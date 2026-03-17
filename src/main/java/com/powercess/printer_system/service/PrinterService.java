package com.powercess.printer_system.service;

import java.util.Map;

public interface PrinterService {

    Map<String, Object> healthCheck();

    Map<String, Object> getPrintersStatus();

    Map<String, Object> getCupsPrinters();

    Map<String, Object> getPrinterDetail(String printerName);

    Map<String, Object> getPrintJobs(String printerName, String whichJobs);

    Map<String, Object> getPrintJobDetail(Integer jobId);

    void cancelPrintJob(Integer jobId);

    Map<String, Object> print(Long userId, String printerName, String filePath, String title, Map<String, String> options);

    Map<String, Object> executePrint(Long orderId, String printerName, Long fileId, Integer colorMode, Integer duplex, String paperSize, Integer copies);
}