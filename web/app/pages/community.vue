<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        社区广场
      </h1>
      <UButton color="primary" icon="i-heroicons-solid-plus" @click="shareModalOpen = true">
        发布分享
      </UButton>
    </div>

    <!-- Posts List -->
    <div v-if="loading" class="flex justify-center py-8">
      <LoadingSpinner />
    </div>

    <div v-else-if="posts.length === 0" class="text-center py-8 text-gray-500">
      <UIcon name="i-heroicons-outline-chat-bubble-left-right" class="w-12 h-12 mx-auto mb-4 opacity-50" />
      <p>暂无分享内容</p>
      <p class="text-sm mt-2">成为第一个分享的人吧！</p>
    </div>

    <div v-else class="space-y-4">
      <UCard v-for="post in posts" :key="post.id">
        <div class="flex items-start gap-4">
          <div class="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
            <span class="text-primary font-semibold">{{ post.username.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-semibold">{{ post.nickname || post.username }}</span>
              <span class="text-sm text-gray-500">{{ formatDate(post.createdAt) }}</span>
            </div>

            <div v-if="post.fileName" class="mt-3 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg flex items-center gap-2">
              <UIcon name="i-heroicons-outline-document" class="w-5 h-5 text-primary" />
              <span class="text-sm">{{ post.fileName }}</span>
            </div>

            <div class="flex items-center gap-4 mt-4">
              <button
                class="flex items-center gap-1 text-sm"
                :class="post.isLiked ? 'text-red-500' : 'text-gray-500 hover:text-red-500'"
                @click="toggleLike(post)"
              >
                <UIcon :name="post.isLiked ? 'i-heroicons-solid-heart' : 'i-heroicons-outline-heart'" class="w-5 h-5" />
                <span>{{ post.likeCount }}</span>
              </button>

              <button
                v-if="post.userId === currentUserId"
                class="text-sm text-gray-500 hover:text-red-500"
                @click="confirmDelete(post)"
              >
                删除
              </button>
            </div>
          </div>
        </div>
      </UCard>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex justify-center">
      <Pagination
        v-model:current-page="currentPage"
        :total-pages="totalPages"
        @change="fetchPosts"
      />
    </div>

    <!-- Share Modal -->
    <UModal v-model:open="shareModalOpen">
      <template #content>
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">分享文件</h3>
          </template>

          <form class="space-y-4" @submit.prevent="submitShare">
            <UFormField label="选择文件" required>
              <UInput
                ref="shareFileInput"
                type="file"
                accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg"
                @change="handleShareFileSelect"
              />
            </UFormField>
          </form>

          <template #footer>
            <div class="flex justify-end gap-3">
              <UButton color="neutral" variant="ghost" @click="shareModalOpen = false">
                取消
              </UButton>
              <UButton color="primary" :loading="submitting" :disabled="!shareForm.fileId" @click="submitShare">
                发布
              </UButton>
            </div>
          </template>
        </UCard>
      </template>
    </UModal>

    <!-- Delete Confirmation Modal -->
    <UModal v-model:open="deleteModalOpen">
      <template #content>
        <UCard>
          <div class="text-center">
            <UIcon name="i-heroicons-solid-exclamation-triangle" class="w-12 h-12 mx-auto text-red-500 mb-4" />
            <h3 class="text-lg font-semibold mb-2">确认删除</h3>
            <p class="text-gray-500 mb-6">
              确定要删除这条分享吗？此操作不可撤销。
            </p>
            <div class="flex justify-center gap-3">
              <UButton color="neutral" variant="ghost" @click="deleteModalOpen = false">
                取消
              </UButton>
              <UButton color="error" :loading="deleting" @click="deletePost">
                删除
              </UButton>
            </div>
          </div>
        </UCard>
      </template>
    </UModal>
  </div>
</template>

<script setup lang="ts">
import type { CommunityPost } from "../../types/community";
import { useUserStore } from "../../stores/user";
import { useAppToast } from "../../composables/useToast";
import { useCommunityApi } from "../../api/community";
import { useFileApi } from "../../api/file";
import { createPageLogger } from "../../utils/logger";

definePageMeta({
  middleware: ["auth"],
});

const log = createPageLogger("community");

const toast = useAppToast();
const userStore = useUserStore();
const communityApi = useCommunityApi();
const fileApi = useFileApi();
const currentUserId = computed(() => userStore.user?.id);

const loading = ref(true);
const posts = ref<CommunityPost[]>([]);
const currentPage = ref(1);
const totalPages = ref(1);
const pageSize = 10;

const shareModalOpen = ref(false);
const submitting = ref(false);
const shareForm = reactive({
  fileId: null as number | null,
});
const shareFileInput = ref<HTMLInputElement | null>(null);

const deleteModalOpen = ref(false);
const postToDelete = ref<CommunityPost | null>(null);
const deleting = ref(false);

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes}分钟前`;
  if (hours < 24) return `${hours}小时前`;
  if (days < 7) return `${days}天前`;
  return date.toLocaleDateString("zh-CN");
};

const fetchPosts = async () => {
  log.loadStart("分享列表");
  loading.value = true;
  try {
    const result = await communityApi.getList({
      page: currentPage.value,
      pageSize: pageSize,
    });
    posts.value = result.items;
    totalPages.value = Math.ceil(result.total / pageSize);
    log.loadSuccess("分享列表", { count: result.items.length, total: result.total });
  } catch (error) {
    log.loadError("分享列表", error);
    toast.error("获取分享列表失败");
  } finally {
    loading.value = false;
  }
};

const handleShareFileSelect = async (e: Event) => {
  const target = e.target as HTMLInputElement;
  const files = target.files;
  if (!files?.length || !files[0]) return;

  log.userAction("选择分享文件", { fileName: files[0].name });

  try {
    const result = await fileApi.upload(files[0]);
    shareForm.fileId = result.fileId;
    log.success("分享文件上传成功", { fileId: result.fileId });
    toast.success("文件上传成功");
  } catch (error) {
    log.error("分享文件上传失败", error);
    toast.error("文件上传失败");
  }
};

const submitShare = async () => {
  log.formSubmit("分享表单", { hasFile: !!shareForm.fileId });

  if (!shareForm.fileId) {
    log.warn("表单验证失败: 未选择文件");
    toast.error("请选择要分享的文件");
    return;
  }

  log.userAction("发布分享", { fileId: shareForm.fileId });
  submitting.value = true;
  try {
    await communityApi.share({
      fileId: shareForm.fileId,
    });

    log.success("分享发布成功");
    toast.success("分享成功");
    shareModalOpen.value = false;
    shareForm.fileId = null;
    await fetchPosts();
  } catch (error) {
    log.error("分享发布失败", error);
    toast.error("分享失败");
  } finally {
    submitting.value = false;
  }
};

const toggleLike = async (post: CommunityPost) => {
  const action = post.isLiked ? "取消点赞" : "点赞";
  log.userAction(action, { postId: post.id });

  try {
    if (post.isLiked) {
      await communityApi.unlike(post.id);
    } else {
      await communityApi.like(post.id);
    }
    log.success(`${action}成功`, { postId: post.id });
    await fetchPosts();
  } catch (error) {
    log.error(`${action}失败`, error);
    toast.error("操作失败");
  }
};

const confirmDelete = (post: CommunityPost) => {
  log.debug("确认删除分享", { postId: post.id, fileName: post.fileName });
  postToDelete.value = post;
  deleteModalOpen.value = true;
};

const deletePost = async () => {
  if (!postToDelete.value) return;

  log.userAction("删除分享", { postId: postToDelete.value.id, fileName: postToDelete.value.fileName });
  deleting.value = true;
  try {
    await communityApi.delete(postToDelete.value.id);
    log.success("分享删除成功");
    toast.success("已删除");
    await fetchPosts();
  } catch (error) {
    log.error("分享删除失败", error);
    toast.error("删除失败");
  } finally {
    deleting.value = false;
    deleteModalOpen.value = false;
    postToDelete.value = null;
  }
};

onMounted(() => {
  log.mounted();
  fetchPosts();
});
</script>