package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powercess.printer_system.dto.file.PreviewSession;
import com.powercess.printer_system.service.FileService;
import com.powercess.printer_system.service.PreviewSessionService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("文件控制器预览接口测试")
class FileControllerPreviewTest {

    private MockMvc mockMvc;

    @Mock
    private FileService fileService;

    @Mock
    private PreviewSessionService previewSessionService;

    @InjectMocks
    private FileController fileController;

    private ObjectMapper objectMapper;

    private MockedStatic<StpUtil> stpUtilMock;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileController).build();
        objectMapper = new ObjectMapper();
        stpUtilMock = mockStatic(StpUtil.class);
        stpUtilMock.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        stpUtilMock.close();
    }

    @Nested
    @DisplayName("上传预览接口测试")
    class UploadPreviewTests {

        @Test
        @DisplayName("应该成功上传文件并创建预览会话")
        void shouldUploadAndCreatePreviewSession() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx content".getBytes()
            );

            PreviewSession mockSession = PreviewSession.builder()
                .sessionId("abc123def456")
                .userId(1L)
                .originalFilename("document.docx")
                .originalFileType("docx")
                .originalFileSize(1000L)
                .pdfSize(2000L)
                .converted(true)
                .pageCount(3)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .status("pending")
                .build();

            when(previewSessionService.createPreview(eq(1L), any())).thenReturn(mockSession);

            mockMvc.perform(multipart("/api/file/preview/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").value("abc123def456"))
                .andExpect(jsonPath("$.data.originalFilename").value("document.docx"))
                .andExpect(jsonPath("$.data.converted").value(true));

            verify(previewSessionService).createPreview(eq(1L), any());
        }

        @Test
        @DisplayName("PDF文件上传应标记为未转换")
        void shouldMarkPdfAsNotConverted() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf",
                "%PDF-1.4 content".getBytes()
            );

            PreviewSession mockSession = PreviewSession.builder()
                .sessionId("pdf123session")
                .userId(1L)
                .originalFilename("document.pdf")
                .originalFileType("pdf")
                .originalFileSize(500L)
                .pdfSize(500L)
                .converted(false)
                .pageCount(5)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .status("pending")
                .build();

            when(previewSessionService.createPreview(eq(1L), any())).thenReturn(mockSession);

            mockMvc.perform(multipart("/api/file/preview/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.converted").value(false));
        }
    }

    @Nested
    @DisplayName("获取预览PDF接口测试")
    class GetPreviewPdfTests {

        @Test
        @DisplayName("应该返回PDF文件内容")
        void shouldReturnPdfContent() throws Exception {
            byte[] pdfContent = "%PDF-1.4 test content".getBytes();

            PreviewSession mockSession = PreviewSession.builder()
                .sessionId("test-session-id")
                .userId(1L)
                .originalFilename("document.docx")
                .converted(true)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .status("pending")
                .build();

            when(previewSessionService.getPdfData("test-session-id", 1L)).thenReturn(pdfContent);
            when(previewSessionService.getPreview("test-session-id", 1L)).thenReturn(mockSession);

            mockMvc.perform(get("/api/file/preview/pdf/test-session-id"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("document.pdf")));
        }

        @Test
        @DisplayName("PDF文件应保持原始文件名")
        void shouldKeepOriginalPdfFilename() throws Exception {
            byte[] pdfContent = "%PDF-1.4 content".getBytes();

            PreviewSession mockSession = PreviewSession.builder()
                .sessionId("pdf-session")
                .userId(1L)
                .originalFilename("original.pdf")
                .converted(false)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .status("pending")
                .build();

            when(previewSessionService.getPdfData("pdf-session", 1L)).thenReturn(pdfContent);
            when(previewSessionService.getPreview("pdf-session", 1L)).thenReturn(mockSession);

            mockMvc.perform(get("/api/file/preview/pdf/pdf-session"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("original.pdf")));
        }
    }

    @Nested
    @DisplayName("获取预览状态接口测试")
    class GetPreviewStatusTests {

        @Test
        @DisplayName("应该返回预览会话状态")
        void shouldReturnPreviewStatus() throws Exception {
            PreviewSession mockSession = PreviewSession.builder()
                .sessionId("status-session")
                .userId(1L)
                .originalFilename("test.docx")
                .converted(true)
                .pageCount(3)
                .status("pending")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

            when(previewSessionService.getPreview("status-session", 1L)).thenReturn(mockSession);

            mockMvc.perform(get("/api/file/preview/status").param("sessionId", "status-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").value("status-session"))
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.converted").value(true))
                .andExpect(jsonPath("$.data.pageCount").value(3));
        }
    }

    @Nested
    @DisplayName("确认预览接口测试")
    class ConfirmPreviewTests {

        @Test
        @DisplayName("应该成功确认预览并返回文件ID")
        void shouldConfirmPreviewAndReturnFileId() throws Exception {
            when(previewSessionService.confirmPreview("confirm-session", 1L)).thenReturn(123L);

            mockMvc.perform(post("/api/file/preview/confirm").param("sessionId", "confirm-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileId").value(123))
                .andExpect(jsonPath("$.data.message").value("文件已保存"));

            verify(previewSessionService).confirmPreview("confirm-session", 1L);
        }
    }

    @Nested
    @DisplayName("取消预览接口测试")
    class CancelPreviewTests {

        @Test
        @DisplayName("应该成功取消预览会话")
        void shouldCancelPreviewSession() throws Exception {
            doNothing().when(previewSessionService).cancelPreview("cancel-session", 1L);

            mockMvc.perform(delete("/api/file/preview/cancel").param("sessionId", "cancel-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已取消"));

            verify(previewSessionService).cancelPreview("cancel-session", 1L);
        }
    }
}