package com.powercess.printer_system.service.impl;

import com.powercess.printer_system.dto.file.PreviewSession;
import com.powercess.printer_system.entity.FileBlob;
import com.powercess.printer_system.entity.UserFile;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.FileBlobMapper;
import com.powercess.printer_system.mapper.UserFileMapper;
import com.powercess.printer_system.service.PdfConversionService;
import com.powercess.printer_system.service.StorageService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("预览会话服务测试")
class PreviewSessionServiceTest {

    @Mock
    private PdfConversionService pdfConversionService;

    @Mock
    private StorageService storageService;

    @Mock
    private UserFileMapper userFileMapper;

    @Mock
    private FileBlobMapper fileBlobMapper;

    @InjectMocks
    private PreviewSessionServiceImpl previewSessionService;

    @Mock
    private MultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        previewSessionService.cleanupExpiredSessions();
    }

    @Nested
    @DisplayName("创建预览会话测试")
    class CreatePreviewTests {

        @Test
        @DisplayName("应该成功创建PDF文件的预览会话")
        void shouldCreatePreviewForPdfFile() throws IOException {
            byte[] pdfContent = "%PDF-1.4 test content".getBytes();

            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("document.pdf");
            when(multipartFile.getSize()).thenReturn((long) pdfContent.length);
            when(pdfConversionService.getFileType("document.pdf")).thenReturn("pdf");
            when(pdfConversionService.isPdf("document.pdf")).thenReturn(true);
            when(pdfConversionService.isConvertible("document.pdf")).thenReturn(false);
            when(pdfConversionService.convertToPdf(multipartFile)).thenReturn(new ByteArrayInputStream(pdfContent));

            PreviewSession session = previewSessionService.createPreview(1L, multipartFile);

            assertThat(session).isNotNull();
            assertThat(session.getSessionId()).isNotBlank();
            assertThat(session.getUserId()).isEqualTo(1L);
            assertThat(session.getOriginalFilename()).isEqualTo("document.pdf");
            assertThat(session.getOriginalFileType()).isEqualTo("pdf");
            assertThat(session.isConverted()).isFalse();
            assertThat(session.getStatus()).isEqualTo("pending");
            assertThat(session.getPdfData()).isEqualTo(pdfContent);

            verify(pdfConversionService).convertToPdf(multipartFile);
        }

        @Test
        @DisplayName("应该成功创建并转换非PDF文件")
        void shouldCreateAndConvertNonPdfFile() throws IOException {
            byte[] docxContent = "docx content".getBytes();
            byte[] pdfContent = "%PDF-1.4 converted".getBytes();

            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("document.docx");
            when(multipartFile.getSize()).thenReturn((long) docxContent.length);
            when(pdfConversionService.getFileType("document.docx")).thenReturn("docx");
            when(pdfConversionService.isPdf("document.docx")).thenReturn(false);
            when(pdfConversionService.isConvertible("document.docx")).thenReturn(true);
            when(pdfConversionService.convertToPdf(multipartFile)).thenReturn(new ByteArrayInputStream(pdfContent));

            PreviewSession session = previewSessionService.createPreview(1L, multipartFile);

            assertThat(session).isNotNull();
            assertThat(session.isConverted()).isTrue();
            assertThat(session.getPdfData()).isEqualTo(pdfContent);
        }

        @Test
        @DisplayName("空文件应抛出异常")
        void shouldThrowExceptionForEmptyFile() {
            when(multipartFile.isEmpty()).thenReturn(true);

            assertThatThrownBy(() -> previewSessionService.createPreview(1L, multipartFile))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件不能为空")
                .extracting("code").isEqualTo(400);
        }

        @Test
        @DisplayName("不支持的文件类型应抛出异常")
        void shouldThrowExceptionForUnsupportedType() {
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("image.jpg");
            when(pdfConversionService.getFileType("image.jpg")).thenReturn("jpg");
            when(pdfConversionService.isPdf("image.jpg")).thenReturn(false);
            when(pdfConversionService.isConvertible("image.jpg")).thenReturn(false);

            assertThatThrownBy(() -> previewSessionService.createPreview(1L, multipartFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不支持的文件类型")
                .extracting("code").isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("获取预览会话测试")
    class GetPreviewTests {

        @Test
        @DisplayName("应该成功获取存在的预览会话")
        void shouldGetExistingPreview() throws IOException {
            byte[] pdfContent = "%PDF-1.4".getBytes();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getSize()).thenReturn((long) pdfContent.length);
            when(pdfConversionService.getFileType("test.pdf")).thenReturn("pdf");
            when(pdfConversionService.isPdf("test.pdf")).thenReturn(true);
            when(pdfConversionService.isConvertible("test.pdf")).thenReturn(false);
            when(pdfConversionService.convertToPdf(multipartFile)).thenReturn(new ByteArrayInputStream(pdfContent));

            PreviewSession created = previewSessionService.createPreview(1L, multipartFile);

            PreviewSession retrieved = previewSessionService.getPreview(created.getSessionId(), 1L);

            assertThat(retrieved.getSessionId()).isEqualTo(created.getSessionId());
            assertThat(retrieved.getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("不存在的会话应抛出异常")
        void shouldThrowExceptionForNonExistingSession() {
            assertThatThrownBy(() -> previewSessionService.getPreview("nonexistent", 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预览会话不存在或已过期")
                .extracting("code").isEqualTo(404);
        }

        @Test
        @DisplayName("其他用户的会话应拒绝访问")
        void shouldDenyAccessToOtherUserSession() throws IOException {
            byte[] pdfContent = "%PDF-1.4".getBytes();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getSize()).thenReturn((long) pdfContent.length);
            when(pdfConversionService.getFileType("test.pdf")).thenReturn("pdf");
            when(pdfConversionService.isPdf("test.pdf")).thenReturn(true);
            when(pdfConversionService.isConvertible("test.pdf")).thenReturn(false);
            when(pdfConversionService.convertToPdf(multipartFile)).thenReturn(new ByteArrayInputStream(pdfContent));

            PreviewSession created = previewSessionService.createPreview(1L, multipartFile);

            assertThatThrownBy(() -> previewSessionService.getPreview(created.getSessionId(), 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权访问此预览")
                .extracting("code").isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("确认预览测试")
    class ConfirmPreviewTests {

        @Test
        @DisplayName("应该成功确认预览并保存文件")
        void shouldConfirmPreviewAndSaveFile() throws IOException {
            byte[] pdfContent = "%PDF-1.4".getBytes();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("document.docx");
            when(multipartFile.getSize()).thenReturn(1000L);
            when(pdfConversionService.getFileType("document.docx")).thenReturn("docx");
            when(pdfConversionService.isPdf("document.docx")).thenReturn(false);
            when(pdfConversionService.isConvertible("document.docx")).thenReturn(true);
            when(pdfConversionService.convertToPdf(multipartFile)).thenReturn(new ByteArrayInputStream(pdfContent));

            PreviewSession created = previewSessionService.createPreview(1L, multipartFile);

            when(fileBlobMapper.findByContentHash(any())).thenReturn(Optional.empty());
            when(storageService.upload(any(), any(), anyLong(), any())).thenReturn("s3/2024/01/01/test.pdf");
            when(fileBlobMapper.insert(any(FileBlob.class))).thenAnswer(inv -> {
                FileBlob blob = inv.getArgument(0);
                blob.setId(100L);
                return 1;
            });
            when(userFileMapper.insert(any(UserFile.class))).thenAnswer(inv -> {
                UserFile file = inv.getArgument(0);
                file.setId(200L);
                return 1;
            });
            doNothing().when(fileBlobMapper).incrementRefCount(anyLong());

            Long fileId = previewSessionService.confirmPreview(created.getSessionId(), 1L);

            assertThat(fileId).isEqualTo(200L);
            verify(fileBlobMapper).insert(any(FileBlob.class));
            verify(userFileMapper).insert(any(UserFile.class));
            verify(fileBlobMapper).incrementRefCount(100L);
        }

        @Test
        @DisplayName("已确认的会话不能再次确认")
        void shouldNotConfirmTwice() throws IOException {
            byte[] pdfContent = "%PDF-1.4".getBytes();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getSize()).thenReturn((long) pdfContent.length);
            when(pdfConversionService.getFileType("test.pdf")).thenReturn("pdf");
            when(pdfConversionService.isPdf("test.pdf")).thenReturn(true);
            when(pdfConversionService.isConvertible("test.pdf")).thenReturn(false);
            when(pdfConversionService.convertToPdf(multipartFile)).thenReturn(new ByteArrayInputStream(pdfContent));
            when(fileBlobMapper.findByContentHash(any())).thenReturn(Optional.empty());
            when(storageService.upload(any(), any(), anyLong(), any())).thenReturn("s3/test.pdf");
            when(fileBlobMapper.insert(any(FileBlob.class))).thenAnswer(inv -> {
                FileBlob blob = inv.getArgument(0);
                blob.setId(1L);
                return 1;
            });
            when(userFileMapper.insert(any(UserFile.class))).thenAnswer(inv -> {
                UserFile file = inv.getArgument(0);
                file.setId(1L);
                return 1;
            });
            doNothing().when(fileBlobMapper).incrementRefCount(anyLong());

            PreviewSession created = previewSessionService.createPreview(1L, multipartFile);
            previewSessionService.confirmPreview(created.getSessionId(), 1L);

            assertThatThrownBy(() -> previewSessionService.confirmPreview(created.getSessionId(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预览会话不存在或已过期");
        }
    }

    @Nested
    @DisplayName("取消预览测试")
    class CancelPreviewTests {

        @Test
        @DisplayName("应该成功取消预览会话")
        void shouldCancelPreviewSession() throws IOException {
            byte[] pdfContent = "%PDF-1.4".getBytes();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getSize()).thenReturn((long) pdfContent.length);
            when(pdfConversionService.getFileType("test.pdf")).thenReturn("pdf");
            when(pdfConversionService.isPdf("test.pdf")).thenReturn(true);
            when(pdfConversionService.isConvertible("test.pdf")).thenReturn(false);
            when(pdfConversionService.convertToPdf(multipartFile)).thenReturn(new ByteArrayInputStream(pdfContent));

            PreviewSession created = previewSessionService.createPreview(1L, multipartFile);

            previewSessionService.cancelPreview(created.getSessionId(), 1L);

            assertThatThrownBy(() -> previewSessionService.getPreview(created.getSessionId(), 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预览会话不存在或已过期");
        }
    }

    @Nested
    @DisplayName("获取PDF数据测试")
    class GetPdfDataTests {

        @Test
        @DisplayName("应该返回正确的PDF数据")
        void shouldReturnCorrectPdfData() throws IOException {
            byte[] pdfContent = "%PDF-1.4 test pdf content".getBytes();
            when(multipartFile.isEmpty()).thenReturn(false);
            when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
            when(multipartFile.getSize()).thenReturn((long) pdfContent.length);
            when(pdfConversionService.getFileType("test.pdf")).thenReturn("pdf");
            when(pdfConversionService.isPdf("test.pdf")).thenReturn(true);
            when(pdfConversionService.isConvertible("test.pdf")).thenReturn(false);
            when(pdfConversionService.convertToPdf(multipartFile)).thenReturn(new ByteArrayInputStream(pdfContent));

            PreviewSession created = previewSessionService.createPreview(1L, multipartFile);

            byte[] data = previewSessionService.getPdfData(created.getSessionId(), 1L);

            assertThat(data).isEqualTo(pdfContent);
        }
    }
}