// 打印机相关 API

import type {
  PrinterStatus,
  CupsPrinter,
  PrintJob,
  PrintJobsParams,
} from "../types/printer";
import type { PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";

export const usePrinterApi = () => {
  const { get, post } = useApiRequest();

  return {
    // 获取打印机状态
    getStatus: () => get<PrinterStatus[]>("/api/printer/status"),

    // 获取CUPS打印机列表
    getCupsList: () => get<CupsPrinter[]>("/api/printer/cups/list"),

    // 打印文件
    print: (data: {
      file_path: string;
      printer_name: string;
      copies: number;
      color_mode: "bw" | "color";
      duplex: boolean;
      paper_size: string;
      page_range?: string;
    }) => post<{ job_id: number }>("/api/printer/cups/print", data),

    // 获取打印任务列表
    getJobs: (params?: PrintJobsParams) =>
      get<PaginatedResponse<PrintJob>>("/api/printer/cups/jobs", params),
  };
};