package com.powercess.printer_system.service.impl;

import com.powercess.printer_system.config.CupsProperties;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.service.CupsClientService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJob;
import org.cups4j.PrintJobAttributes;
import org.cups4j.PrintRequestResult;
import org.cups4j.WhichJobsEnum;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CupsClientServiceImpl implements CupsClientService {

    private final CupsProperties cupsProperties;
    private CupsClient cupsClient;

    @PostConstruct
    public void init() {
        try {
            URL cupsUrl = URI.create(cupsProperties.getIppUrl()).toURL();
            if (cupsProperties.hasAuth()) {
                cupsClient = new CupsClient(
                    cupsUrl.getHost(),
                    cupsUrl.getPort(),
                    cupsProperties.username()
                );
            } else {
                cupsClient = new CupsClient(cupsUrl.getHost(), cupsUrl.getPort());
            }
            log.info("CUPS client initialized: {}:{}", cupsProperties.host(), cupsProperties.port());
        } catch (Exception e) {
            log.error("Failed to initialize CUPS client: {}", e.getMessage());
        }
    }

    private void ensureClientReady() {
        if (cupsClient == null) {
            throw new BusinessException(503, "CUPS服务不可用");
        }
    }

    @Override
    public boolean testConnection() {
        try {
            ensureClientReady();
            getPrinters();
            return true;
        } catch (Exception e) {
            log.error("CUPS connection test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<CupsPrinter> getPrinters() throws Exception {
        ensureClientReady();
        return cupsClient.getPrinters();
    }

    @Override
    public CupsPrinter getPrinter(String printerName) throws Exception {
        ensureClientReady();
        return cupsClient.getPrinter(printerName);
    }

    @Override
    public Map<String, Object> getPrinterInfo(CupsPrinter printer) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", printer.getName());
        info.put("description", printer.getDescription());
        info.put("location", printer.getLocation());
        info.put("state", printer.getState());
        info.put("deviceUri", printer.getDeviceURI());
        info.put("printerUri", printer.getPrinterURL());
        return info;
    }

    @Override
    public int printFile(String printerName, File file, String jobName, int copies, String duplex) throws Exception {
        ensureClientReady();
        CupsPrinter printer = getPrinter(printerName);
        if (printer == null) {
            throw new BusinessException(404, "打印机不存在: " + printerName);
        }
        return printFile(printer, file, jobName, copies, duplex);
    }

    @Override
    public int printFile(CupsPrinter printer, File file, String jobName, int copies, String duplex) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            return printStream(printer, inputStream, jobName, copies, duplex);
        }
    }

    @Override
    public int printBytes(String printerName, byte[] content, String jobName, int copies, String duplex) throws Exception {
        ensureClientReady();
        CupsPrinter printer = getPrinter(printerName);
        if (printer == null) {
            throw new BusinessException(404, "打印机不存在: " + printerName);
        }
        return printStream(printer, new ByteArrayInputStream(content), jobName, copies, duplex);
    }

    @Override
    public int printStream(CupsPrinter printer, InputStream inputStream, String jobName, int copies, String duplex) throws Exception {
        boolean isDuplex = "two-sided-long-edge".equals(duplex) || "two-sided-short-edge".equals(duplex);
        PrintJob printJob = new PrintJob.Builder(inputStream)
            .jobName(jobName)
            .copies(copies)
            .duplex(isDuplex)
            .build();

        PrintRequestResult result = printer.print(printJob);

        if (result.isSuccessfulResult()) {
            log.info("Print job submitted successfully, jobId: {}", result.getJobId());
            return result.getJobId();
        } else {
            throw new BusinessException(500, "打印任务提交失败: " + result.getResultMessage());
        }
    }

    @Override
    public int printWithOptions(CupsPrinter printer, InputStream inputStream, String jobName, Map<String, String> options) throws Exception {
        PrintJob.Builder builder = new PrintJob.Builder(inputStream)
            .jobName(jobName);

        if (options.containsKey("copies")) {
            builder.copies(Integer.parseInt(options.get("copies")));
        }
        if (options.containsKey("sides")) {
            String sides = options.get("sides");
            builder.duplex("two-sided-long-edge".equals(sides) || "two-sided-short-edge".equals(sides));
        }
        if (options.containsKey("media")) {
            builder.pageFormat(options.get("media"));
        }
        if (options.containsKey("ColorModel")) {
            builder.color("RGB".equals(options.get("ColorModel")));
        }

        PrintRequestResult result = printer.print(builder.build());

        if (result.isSuccessfulResult()) {
            log.info("Print job submitted successfully, jobId: {}", result.getJobId());
            return result.getJobId();
        } else {
            throw new BusinessException(500, "打印任务提交失败: " + result.getResultMessage());
        }
    }

    @Override
    public boolean cancelJob(Integer jobId) {
        ensureClientReady();
        try {
            // 遍历所有打印机找到该任务
            List<CupsPrinter> printers = getPrinters();
            for (CupsPrinter printer : printers) {
                List<PrintJobAttributes> jobs = cupsClient.getJobs(printer, WhichJobsEnum.ALL, null, false);
                for (PrintJobAttributes job : jobs) {
                    if (job.getJobID() == jobId) {
                        boolean result = cupsClient.cancelJob(printer, jobId);
                        if (result) {
                            log.info("Job {} cancelled", jobId);
                        }
                        return result;
                    }
                }
            }
            log.warn("Job {} not found", jobId);
            return false;
        } catch (Exception e) {
            log.error("Failed to cancel job {}: {}", jobId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean cancelJob(CupsPrinter printer, Integer jobId) {
        ensureClientReady();
        try {
            boolean result = cupsClient.cancelJob(printer, jobId);
            if (result) {
                log.info("Job {} cancelled", jobId);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to cancel job {}: {}", jobId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getJobs(CupsPrinter printer, String whichJobs) throws Exception {
        ensureClientReady();
        WhichJobsEnum whichJobsEnum = switch (whichJobs) {
            case "completed" -> WhichJobsEnum.COMPLETED;
            case "all" -> WhichJobsEnum.ALL;
            default -> WhichJobsEnum.NOT_COMPLETED;
        };
        List<PrintJobAttributes> jobs = cupsClient.getJobs(printer, whichJobsEnum, null, false);

        // 转换为 Map 列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (PrintJobAttributes job : jobs) {
            Map<String, Object> jobMap = new HashMap<>();
            jobMap.put("id", job.getJobID());
            jobMap.put("name", job.getJobName());
            jobMap.put("user", job.getUserName());
            jobMap.put("state", job.getJobState() != null ? job.getJobState().name() : "UNKNOWN");
            jobMap.put("pagesPrinted", job.getPagesPrinted());
            jobMap.put("size", job.getSize());
            jobMap.put("createTime", job.getJobCreateTime());
            jobMap.put("completeTime", job.getJobCompleteTime());
            jobMap.put("jobUrl", job.getJobURL());
            jobMap.put("printerUrl", job.getPrinterURL());
            result.add(jobMap);
        }
        return result;
    }

    @Override
    public PrintJobAttributes getJobAttributes(Integer jobId) throws Exception {
        ensureClientReady();
        return cupsClient.getJobAttributes(jobId);
    }
}