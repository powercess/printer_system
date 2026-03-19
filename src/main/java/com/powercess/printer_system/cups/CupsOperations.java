package com.powercess.printer_system.cups;

import org.cups4j.CupsPrinter;
import org.cups4j.PrintJobAttributes;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * CUPS 操作抽象接口
 * 支持多种实现：本地CUPS、远程CUPS、模拟实现等
 */
public interface CupsOperations {

    /**
     * 获取CUPS服务器标识
     */
    String getServerId();

    /**
     * 测试连接是否可用
     */
    boolean testConnection();

    /**
     * 获取所有打印机
     */
    List<CupsPrinter> getPrinters() throws CupsException;

    /**
     * 根据名称获取打印机
     */
    CupsPrinter getPrinter(String printerName) throws CupsException;

    /**
     * 获取打印机详细信息
     */
    Map<String, Object> getPrinterInfo(CupsPrinter printer);

    /**
     * 打印文件流
     * @return 打印任务ID
     */
    int print(CupsPrinter printer, InputStream inputStream, String jobName, PrintOptions options) throws CupsException;

    /**
     * 取消打印任务
     */
    boolean cancelJob(CupsPrinter printer, Integer jobId) throws CupsException;

    /**
     * 取消打印任务（自动查找打印机）
     */
    boolean cancelJob(Integer jobId) throws CupsException;

    /**
     * 获取打印机的打印任务列表
     */
    List<Map<String, Object>> getJobs(CupsPrinter printer, String whichJobs) throws CupsException;

    /**
     * 获取打印任务属性
     */
    PrintJobAttributes getJobAttributes(Integer jobId) throws CupsException;

    /**
     * 关闭连接，释放资源
     */
    void close();

    /**
     * 获取连接状态
     */
    ConnectionStatus getConnectionStatus();

    /**
     * 连接状态枚举
     */
    enum ConnectionStatus {
        CONNECTED,
        DISCONNECTED,
        ERROR
    }
}