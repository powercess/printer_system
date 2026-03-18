// API 请求 composable

import { useApiRequest } from "../api/index";
import { useLogger } from "../utils/logger";

const logger = useLogger("Composable:useApi");

export const useApi = () => {
  const apiRequest = useApiRequest();
  const toast = useAppToast();

  logger.debug("初始化 useApi composable");

  const handleApiCall = async <T>(
    apiCall: () => Promise<T>,
    options: {
      showError?: boolean;
      errorMessage?: string;
      showSuccess?: boolean;
      successMessage?: string;
    } = {},
  ): Promise<{ data: T | null; error: string | null }> => {
    const {
      showError = true,
      errorMessage,
      showSuccess = false,
      successMessage,
    } = options;

    logger.debug("执行 API 调用", {
      showError,
      hasErrorMessage: !!errorMessage,
      showSuccess,
      hasSuccessMessage: !!successMessage,
    });

    try {
      logger.trace("开始 API 调用");
      const data = await apiCall();

      if (showSuccess && successMessage) {
        logger.debug("显示成功消息", { successMessage });
        toast.success(successMessage);
      }

      logger.success("API 调用成功");
      return { data, error: null };
    } catch (error) {
      const message =
        errorMessage ||
        (error as { message?: string })?.message ||
        "操作失败";

      logger.error("API 调用失败", { error, message });

      if (showError) {
        logger.debug("显示错误消息", { message });
        toast.error(message);
      }

      return { data: null, error: message };
    }
  };

  return {
    ...apiRequest,
    handleApiCall,
  };
};