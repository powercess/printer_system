package com.powercess.printer_system.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * PDF 转换服务接口
 */
public interface PdfConversionService {

    /**
     * 判断文件是否为 PDF
     */
    boolean isPdf(String filename);

    /**
     * 判断文件是否支持转换为 PDF
     */
    boolean isConvertible(String filename);

    /**
     * 将文件转换为 PDF
     * @param file 源文件
     * @return 转换后的 PDF 输入流
     */
    InputStream convertToPdf(MultipartFile file);

    /**
     * 获取文件类型
     */
    String getFileType(String filename);

    /**
     * 获取支持的转换类型列表
     */
    String[] getSupportedTypes();
}