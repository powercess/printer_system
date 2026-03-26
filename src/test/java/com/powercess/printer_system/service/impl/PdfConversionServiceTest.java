package com.powercess.printer_system.service.impl;

import com.powercess.printer_system.exception.BusinessException;
import dev.gotenberg.GotenbergClient;
import dev.gotenberg.GotenbergClient.LibreOfficeOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PDF转换服务测试")
class PdfConversionServiceImplTest {

    @Mock
    private GotenbergClient gotenbergClient;

    @InjectMocks
    private PdfConversionServiceImpl pdfConversionService;

    @Mock
    private MultipartFile multipartFile;

    @Nested
    @DisplayName("文件类型判断测试")
    class FileTypeTests {

        @Test
        @DisplayName("应该正确识别PDF文件")
        void shouldIdentifyPdfFile() {
            assertThat(pdfConversionService.isPdf("document.pdf")).isTrue();
            assertThat(pdfConversionService.isPdf("document.PDF")).isTrue();
            assertThat(pdfConversionService.isPdf("DOCUMENT.Pdf")).isTrue();
        }

        @Test
        @DisplayName("应该正确识别非PDF文件")
        void shouldIdentifyNonPdfFile() {
            assertThat(pdfConversionService.isPdf("document.docx")).isFalse();
            assertThat(pdfConversionService.isPdf("document.xlsx")).isFalse();
            assertThat(pdfConversionService.isPdf("document.pptx")).isFalse();
        }

        @Test
        @DisplayName("空文件名应返回false")
        void shouldReturnFalseForNullOrEmptyFilename() {
            assertThat(pdfConversionService.isPdf(null)).isFalse();
            assertThat(pdfConversionService.isPdf("")).isFalse();
            assertThat(pdfConversionService.isPdf("noextension")).isFalse();
        }

        @Test
        @DisplayName("应该正确识别可转换的文件类型")
        void shouldIdentifyConvertibleTypes() {
            // Office 文档
            assertThat(pdfConversionService.isConvertible("document.doc")).isTrue();
            assertThat(pdfConversionService.isConvertible("document.docx")).isTrue();
            assertThat(pdfConversionService.isConvertible("spreadsheet.xls")).isTrue();
            assertThat(pdfConversionService.isConvertible("spreadsheet.xlsx")).isTrue();
            assertThat(pdfConversionService.isConvertible("presentation.ppt")).isTrue();
            assertThat(pdfConversionService.isConvertible("presentation.pptx")).isTrue();

            // OpenDocument 格式
            assertThat(pdfConversionService.isConvertible("document.odt")).isTrue();
            assertThat(pdfConversionService.isConvertible("spreadsheet.ods")).isTrue();
            assertThat(pdfConversionService.isConvertible("presentation.odp")).isTrue();

            // 其他格式
            assertThat(pdfConversionService.isConvertible("document.rtf")).isTrue();
            assertThat(pdfConversionService.isConvertible("document.txt")).isTrue();
            assertThat(pdfConversionService.isConvertible("page.html")).isTrue();
            assertThat(pdfConversionService.isConvertible("page.htm")).isTrue();
        }

        @Test
        @DisplayName("PDF文件不应标记为可转换")
        void shouldNotMarkPdfAsConvertible() {
            assertThat(pdfConversionService.isConvertible("document.pdf")).isFalse();
        }

        @Test
        @DisplayName("不支持的格式应返回false")
        void shouldReturnFalseForUnsupportedTypes() {
            assertThat(pdfConversionService.isConvertible("image.jpg")).isFalse();
            assertThat(pdfConversionService.isConvertible("image.png")).isFalse();
            assertThat(pdfConversionService.isConvertible("video.mp4")).isFalse();
            assertThat(pdfConversionService.isConvertible("archive.zip")).isFalse();
        }
    }

    @Nested
    @DisplayName("文件转换测试")
    class ConversionTests {

        @Test
        @DisplayName("PDF文件应直接返回输入流")
        void shouldReturnPdfAsIs() throws IOException {
            byte[] pdfContent = "%PDF-1.4 test content".getBytes();
            when(multipartFile.getOriginalFilename()).thenReturn("document.pdf");
            when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(pdfContent));

            try (InputStream result = pdfConversionService.convertToPdf(multipartFile)) {
                assertThat(result).isNotNull();
                byte[] resultBytes = result.readAllBytes();
                assertThat(resultBytes).isEqualTo(pdfContent);
            }
        }

        @Test
        @DisplayName("不支持的文件类型应抛出异常")
        void shouldThrowExceptionForUnsupportedType() {
            when(multipartFile.getOriginalFilename()).thenReturn("image.jpg");

            assertThatThrownBy(() -> pdfConversionService.convertToPdf(multipartFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型")
                .extracting("code").isEqualTo(400);
        }

        @Test
        @DisplayName("应该成功转换Office文档")
        void shouldConvertOfficeDocument() throws IOException {
            byte[] docxContent = "docx content".getBytes();
            byte[] pdfContent = "%PDF-1.4 converted content".getBytes();

            when(multipartFile.getOriginalFilename()).thenReturn("document.docx");
            when(multipartFile.getBytes()).thenReturn(docxContent);

            ResponseEntity<InputStream> mockResponse = ResponseEntity.ok(new ByteArrayInputStream(pdfContent));
            when(gotenbergClient.convertLibreOffice(any(LibreOfficeOptions.class))).thenReturn(mockResponse);

            try (InputStream result = pdfConversionService.convertToPdf(multipartFile)) {
                assertThat(result).isNotNull();
                byte[] resultBytes = result.readAllBytes();
                assertThat(new String(resultBytes)).contains("PDF");
            }
        }

        @Test
        @DisplayName("Gotenberg返回null应抛出异常")
        void shouldThrowExceptionWhenGotenbergReturnsNull() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("document.docx");
            when(multipartFile.getBytes()).thenReturn("content".getBytes());
            when(gotenbergClient.convertLibreOffice(any(LibreOfficeOptions.class))).thenReturn(null);

            assertThatThrownBy(() -> pdfConversionService.convertToPdf(multipartFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件转换失败")
                .extracting("code").isEqualTo(500);
        }

        @Test
        @DisplayName("Gotenberg响应体为null应抛出异常")
        void shouldThrowExceptionWhenGotenbergReturnsNullBody() throws IOException {
            when(multipartFile.getOriginalFilename()).thenReturn("document.docx");
            when(multipartFile.getBytes()).thenReturn("content".getBytes());

            ResponseEntity<InputStream> mockResponse = ResponseEntity.ok(null);
            when(gotenbergClient.convertLibreOffice(any(LibreOfficeOptions.class))).thenReturn(mockResponse);

            assertThatThrownBy(() -> pdfConversionService.convertToPdf(multipartFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件转换失败")
                .extracting("code").isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("获取文件类型测试")
    class GetFileTypeTests {

        @Test
        @DisplayName("应该正确提取文件扩展名")
        void shouldExtractFileExtension() {
            assertThat(pdfConversionService.getFileType("document.pdf")).isEqualTo("pdf");
            assertThat(pdfConversionService.getFileType("document.DOCX")).isEqualTo("docx");
            assertThat(pdfConversionService.getFileType("file.XLS")).isEqualTo("xls");
        }

        @Test
        @DisplayName("无扩展名应返回unknown")
        void shouldReturnUnknownForNoExtension() {
            assertThat(pdfConversionService.getFileType(null)).isEqualTo("unknown");
            assertThat(pdfConversionService.getFileType("")).isEqualTo("unknown");
            assertThat(pdfConversionService.getFileType("noextension")).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("获取支持的类型测试")
    class SupportedTypesTests {

        @Test
        @DisplayName("应该返回支持转换的类型列表")
        void shouldReturnSupportedTypes() {
            String[] types = pdfConversionService.getSupportedTypes();

            assertThat(types).isNotEmpty();
            assertThat(types).contains("doc", "docx", "xls", "xlsx", "ppt", "pptx");
            assertThat(types).contains("odt", "ods", "odp");
            assertThat(types).contains("rtf", "txt", "html", "htm");
        }
    }
}