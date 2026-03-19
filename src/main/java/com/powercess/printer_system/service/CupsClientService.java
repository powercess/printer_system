package com.powercess.printer_system.service;

import com.powercess.printer_system.cups.CupsOperations;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJobAttributes;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface CupsClientService {

    /**
     * 测试 CUPS 连接
     */
    boolean testConnection();

    /**
     * 获取所有CUPS服务器状态
     */
    Map<String, CupsOperations.ConnectionStatus> getAllServerStatus();

    /**
     * 获取所有打印机列表
     */
    List<CupsPrinter> getPrinters() throws Exception;

    /**
     * 根据名称获取打印机
     */
    CupsPrinter getPrinter(String printerName) throws Exception;

    /**
     * 获取打印机详细信息
     */
    Map<String, Object> getPrinterInfo(CupsPrinter printer);

    /**
     * 打印文件
     */
    int printFile(String printerName, File file, String jobName, int copies, String duplex) throws Exception;

    /**
     * 打印文件
     */
    int printFile(CupsPrinter printer, File file, String jobName, int copies, String duplex) throws Exception;

    /**
     * 打印字节数组
     */
    int printBytes(String printerName, byte[] content, String jobName, int copies, String duplex) throws Exception;

    /**
     * 打印输入流
     */
    int printStream(CupsPrinter printer, InputStream inputStream, String jobName, int copies, String duplex) throws Exception;

    /**
     * 打印带自定义选项
     */
    int printWithOptions(CupsPrinter printer, InputStream inputStream, String jobName, Map<String, String> options) throws Exception;

    /**
     * 取消打印任务
     */
    boolean cancelJob(Integer jobId);

    /**
     * 取消打印任务
     */
    boolean cancelJob(CupsPrinter printer, Integer jobId);

    /**
     * 获取打印机的打印任务
     */
    List<Map<String, Object>> getJobs(CupsPrinter printer, String whichJobs) throws Exception;

    /**
     * 获取打印任务属性
     */
    PrintJobAttributes getJobAttributes(Integer jobId) throws Exception;
}