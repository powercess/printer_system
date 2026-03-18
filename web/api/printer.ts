// 打印机相关 API

import type {
  PrinterStatus,
  CupsPrinter,
  PrintJob,
  PrintJobsParams,
} from "../types/printer";
import type { PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("Printer");

export const usePrinterApi = () => {
  const { get, post } = useApiRequest();

  return {
    // 获取打印机状态
    getStatus: () => {
      apiLog.requestStart("GET", "/api/printer/status");
      return get<PrinterStatus[]>("/api/printer/status");
    },

    // 获取CUPS打印机列表
    getCupsList: () => {
      apiLog.requestStart("GET", "/api/printer/cups/list");
      return get<CupsPrinter[]>("/api/printer/cups/list");
    },

    // 打印文件
    print: (data: {
      file_path: string;
      printer_name: string;
      copies: number;
      color_mode: "bw" | "color";
      duplex: boolean;
      paper_size: string;
      page_range?: string;
    }) => {
      apiLog.requestStart("POST", "/api/printer/cups/print", {
        filePath: data.file_path,
        printerName: data.printer_name,
        copies: data.copies,
        colorMode: data.color_mode,
        duplex: data.duplex,
        paperSize: data.paper_size,
        pageRange: data.page_range || "全部",
      });
      return post<{ job_id: number }>("/api/printer/cups/print", data);
    },

    // 获取打印任务列表
    getJobs: (params?: PrintJobsParams) => {
      apiLog.requestStart("GET", "/api/printer/cups/jobs", params);
      return get<PaginatedResponse<PrintJob>>("/api/printer/cups/jobs", params);
    },
  };
};