// 打印机相关类型

export interface PrinterStatus {
  name: string;
  status: "online" | "offline" | "busy" | "error";
  message?: string;
  toner_level?: number;
  paper_level?: number;
}

export interface CupsPrinter {
  name: string;
  description: string;
  location?: string;
  deviceUri?: string;
  state?: "idle" | "printing" | "stopped" | null;
}

export interface PrintRequest {
  file_path: string;
  printer_name: string;
  copies: number;
  color_mode: "bw" | "color";
  duplex: boolean;
  paper_size: string;
  page_range?: string;
}

export interface PrintJob {
  id: number;
  printer_name: string;
  file_name: string;
  status: "pending" | "processing" | "completed" | "cancelled" | "error";
  created_at: string;
  completed_at?: string;
}

export interface PrintJobsParams {
  page?: number;
  pageSize?: number;
  printer_name?: string;
  status?: PrintJob["status"];
}