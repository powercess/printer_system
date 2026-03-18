// 消息提示 composable - 使用 Nuxt UI 内置 toast

import { useLogger } from "../utils/logger";

const logger = useLogger("Composable:useToast");

export const useAppToast = () => {
  const toast = useToast();

  logger.debug("初始化 useAppToast composable");

  return {
    success: (message: string) => {
      logger.info(`Toast[success]: ${message}`);
      toast.add({
        title: message,
        color: "success",
        icon: "i-heroicons-check-circle",
      });
    },
    error: (message: string) => {
      logger.error(`Toast[error]: ${message}`);
      toast.add({
        title: message,
        color: "error",
        icon: "i-heroicons-x-circle",
      });
    },
    warning: (message: string) => {
      logger.warn(`Toast[warning]: ${message}`);
      toast.add({
        title: message,
        color: "warning",
        icon: "i-heroicons-exclamation-triangle",
      });
    },
    info: (message: string) => {
      logger.info(`Toast[info]: ${message}`);
      toast.add({
        title: message,
        color: "info",
        icon: "i-heroicons-information-circle",
      });
    },
  };
};