// 消息提示 composable - 使用 Nuxt UI 内置 toast

export const useAppToast = () => {
  const toast = useToast();

  return {
    success: (message: string) =>
      toast.add({
        title: message,
        color: "success",
        icon: "i-heroicons-check-circle",
      }),
    error: (message: string) =>
      toast.add({
        title: message,
        color: "error",
        icon: "i-heroicons-x-circle",
      }),
    warning: (message: string) =>
      toast.add({
        title: message,
        color: "warning",
        icon: "i-heroicons-exclamation-triangle",
      }),
    info: (message: string) =>
      toast.add({
        title: message,
        color: "info",
        icon: "i-heroicons-information-circle",
      }),
  };
};