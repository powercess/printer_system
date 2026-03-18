<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        社区广场
      </h1>
      <UButton color="primary" icon="i-heroicons-plus" @click="shareModalOpen = true">
        发布分享
      </UButton>
    </div>

    <!-- Posts List -->
    <div v-if="loading" class="flex justify-center py-8">
      <LoadingSpinner />
    </div>

    <div v-else-if="posts.length === 0" class="text-center py-8 text-gray-500">
      <UIcon name="i-heroicons-chat-bubble-left-right" class="w-12 h-12 mx-auto mb-4 opacity-50" />
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
              <span class="font-semibold">{{ post.username }}</span>
              <span class="text-sm text-gray-500">{{ formatDate(post.created_at) }}</span>
            </div>
            <h3 class="font-semibold text-lg mb-2">{{ post.title }}</h3>
            <p class="text-gray-600 dark:text-gray-400 whitespace-pre-wrap">{{ post.content }}</p>

            <div v-if="post.file_name" class="mt-3 p-3 bg-gray-50 dark:bg-gray-800 rounded-lg flex items-center gap-2">
              <UIcon name="i-heroicons-document" class="w-5 h-5 text-primary" />
              <span class="text-sm">{{ post.file_name }}</span>
            </div>

            <div class="flex items-center gap-4 mt-4">
              <button
                class="flex items-center gap-1 text-sm"
                :class="post.is_liked ? 'text-red-500' : 'text-gray-500 hover:text-red-500'"
                @click="toggleLike(post)"
              >
                <UIcon :name="post.is_liked ? 'i-heroicons-heart-solid' : 'i-heroicons-heart'" class="w-5 h-5" />
                <span>{{ post.likes_count }}</span>
              </button>

              <button
                v-if="post.user_id === currentUserId"
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
            <h3 class="text-lg font-semibold">发布分享</h3>
          </template>

          <form @submit.prevent="submitShare" class="space-y-4">
            <UFormField label="标题" required>
              <UInput v-model="shareForm.title" placeholder="请输入标题" />
            </UFormField>

            <UFormField label="内容" required>
              <UTextarea
                v-model="shareForm.content"
                placeholder="分享你的打印经验..."
                :rows="4"
              />
            </UFormField>

            <UFormField label="附件文件（可选）">
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
              <UButton color="primary" :loading="submitting" @click="submitShare">
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
            <UIcon name="i-heroicons-exclamation-triangle" class="w-12 h-12 mx-auto text-red-500 mb-4" />
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
definePageMeta({
  middleware: ["auth"],
});

import type { CommunityPost } from "../../types/community";
import { useUserStore } from "../../stores/user";
import { useAppToast } from "../../composables/useToast";
import { useCommunityApi } from "../../api/community";
import { useFileApi } from "../../api/file";

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
  title: "",
  content: "",
  fileId: null as string | null,
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
  loading.value = true;
  try {
    const result = await communityApi.getList({
      page: currentPage.value,
      page_size: pageSize,
    });
    posts.value = result.items;
    totalPages.value = Math.ceil(result.total / pageSize);
  } catch (error) {
    toast.error("获取分享列表失败");
  } finally {
    loading.value = false;
  }
};

const handleShareFileSelect = async (e: Event) => {
  const target = e.target as HTMLInputElement;
  const files = target.files;
  if (!files?.length || !files[0]) return;

  try {
    const result = await fileApi.upload(files[0]);
    shareForm.fileId = result.file_id;
    toast.success("文件上传成功");
  } catch (error) {
    toast.error("文件上传失败");
  }
};

const submitShare = async () => {
  if (!shareForm.title || !shareForm.content) {
    toast.error("请填写标题和内容");
    return;
  }

  submitting.value = true;
  try {
    await communityApi.share({
      title: shareForm.title,
      content: shareForm.content,
      file_id: shareForm.fileId || undefined,
    });

    toast.success("分享成功");
    shareModalOpen.value = false;
    shareForm.title = "";
    shareForm.content = "";
    shareForm.fileId = null;
    await fetchPosts();
  } catch (error) {
    toast.error("分享失败");
  } finally {
    submitting.value = false;
  }
};

const toggleLike = async (post: CommunityPost) => {
  try {
    if (post.is_liked) {
      await communityApi.unlike(post.id);
    } else {
      await communityApi.like(post.id);
    }
    await fetchPosts();
  } catch (error) {
    toast.error("操作失败");
  }
};

const confirmDelete = (post: CommunityPost) => {
  postToDelete.value = post;
  deleteModalOpen.value = true;
};

const deletePost = async () => {
  if (!postToDelete.value) return;

  deleting.value = true;
  try {
    await communityApi.delete(postToDelete.value.id);
    toast.success("已删除");
    await fetchPosts();
  } catch (error) {
    toast.error("删除失败");
  } finally {
    deleting.value = false;
    deleteModalOpen.value = false;
    postToDelete.value = null;
  }
};

onMounted(() => {
  fetchPosts();
});
</script>