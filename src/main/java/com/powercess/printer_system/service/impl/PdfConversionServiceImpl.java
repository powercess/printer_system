package com.powercess.printer_system.service.impl;

import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.service.PdfConversionService;
import dev.gotenberg.GotenbergClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * PDF 转换服务实现
 * 使用 Gotenberg 进行文件转换
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfConversionServiceImpl implements PdfConversionService {

    private final GotenbergClient gotenbergClient;

    // 支持转换为 PDF 的文件类型
    private static final String[] SUPPORTED_TYPES = {
        "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "odt", "ods", "odp", "rtf", "txt", "html", "htm"
    };

    @Override
    public boolean isPdf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return "pdf".equals(extension);
    }

    @Override
    public boolean isConvertible(String filename) {
        if (filename == null || !filename.contains(".")) {
            return false;
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        for (String type : SUPPORTED_TYPES) {
            if (type.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InputStream convertToPdf(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        log.info("Converting file to PDF: {}", originalFilename);

        if (isPdf(originalFilename)) {
            log.debug("File is already PDF, returning as-is");
            try {
                return file.getInputStream();
            } catch (IOException e) {
                log.error("Failed to read PDF file: {}", originalFilename, e);
                throw new BusinessException(500, "文件读取失败");
            }
        }

        if (!isConvertible(originalFilename)) {
            log.warn("File type not supported for conversion: {}", originalFilename);
            throw new BusinessException(400, "不支持的文件类型，请上传 PDF 或 Office 文档");
        }

        try {
            // 使用 Gotenberg 的 LibreOffice 转换功能
            byte[] fileBytes = file.getBytes();

            ResponseEntity<InputStream> response = gotenbergClient.convertLibreOffice(
                GotenbergClient.libreOfficeOptions()
                    .file(new org.springframework.core.io.ByteArrayResource(fileBytes) {
                        @Override
                        public String getFilename() {
                            return originalFilename;
                        }
                    })
            );

            if (response == null || response.getBody() == null) {
                log.error("Gotenberg returned null response for file: {}", originalFilename);
                throw new BusinessException(500, "文件转换失败：服务无响应");
            }

            // 将响应流读取到内存中，以便后续可以多次读取
            InputStream pdfStream = response.getBody();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = pdfStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            log.info("File converted successfully: {} (size: {} bytes)", originalFilename, buffer.size());
            return new ByteArrayInputStream(buffer.toByteArray());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert file to PDF: {}", originalFilename, e);
            throw new BusinessException(500, "文件转换失败: " + e.getMessage());
        }
    }

    @Override
    public String getFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    @Override
    public String[] getSupportedTypes() {
        return SUPPORTED_TYPES.clone();
    }

    @Override
    public byte[] convertToPdf(byte[] content, String filename) {
        log.info("Converting file to PDF for printing: {} ({} bytes)", filename, content.length);

        // 如果已经是 PDF，直接返回
        if (isPdf(filename)) {
            log.debug("File is already PDF, returning as-is");
            return content;
        }

        // 检查是否支持转换
        if (!isConvertible(filename)) {
            log.warn("File type not supported for conversion: {}", filename);
            throw new BusinessException(400, "不支持的文件类型，请上传 PDF 或 Office 文档");
        }

        try {
            // 使用 Gotenberg 的 LibreOffice 转换功能
            ResponseEntity<InputStream> response = gotenbergClient.convertLibreOffice(
                GotenbergClient.libreOfficeOptions()
                    .file(new org.springframework.core.io.ByteArrayResource(content) {
                        @Override
                        public String getFilename() {
                            return filename;
                        }
                    })
            );

            if (response == null || response.getBody() == null) {
                log.error("Gotenberg returned null response for file: {}", filename);
                throw new BusinessException(500, "文件转换失败：服务无响应");
            }

            // 将响应流读取到字节数组
            InputStream pdfStream = response.getBody();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = pdfStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            byte[] pdfBytes = buffer.toByteArray();
            log.info("File converted successfully for printing: {} (PDF size: {} bytes)", filename, pdfBytes.length);
            return pdfBytes;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to convert file to PDF for printing: {}", filename, e);
            throw new BusinessException(500, "文件转换失败: " + e.getMessage());
        }
    }
}