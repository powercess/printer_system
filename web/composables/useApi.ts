// API 请求 composable

import { useApiRequest } from "../api/index";

export const useApi = () => {
  const apiRequest = useApiRequest();
  const toast = useAppToast();

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

    try {
      const data = await apiCall();

      if (showSuccess && successMessage) {
        toast.success(successMessage);
      }

      return { data, error: null };
    } catch (error) {
      const message =
        errorMessage ||
        (error as { message?: string })?.message ||
        "操作失败";

      if (showError) {
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