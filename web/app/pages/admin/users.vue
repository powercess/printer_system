<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        用户管理
      </h1>
      <UButton color="primary" icon="i-heroicons-plus" @click="createModalOpen = true">
        添加用户
      </UButton>
    </div>

    <!-- Search -->
    <div class="flex gap-4">
      <UInput
        v-model="searchQuery"
        placeholder="搜索用户..."
        icon="i-heroicons-magnifying-glass"
        class="max-w-xs"
        @keyup.enter="fetchUsers"
      />
      <UButton color="neutral" @click="fetchUsers">搜索</UButton>
    </div>

    <!-- Users Table -->
    <UCard>
      <UTable :data="users" :columns="columns">
        <template #role-cell="{ row }">
          <UBadge :color="row.original.groupId === 0 ? 'primary' : 'neutral'" variant="subtle">
            {{ row.original.groupId === 0 ? "管理员" : row.original.groupName || "用户" }}
          </UBadge>
        </template>

        <template #balance-cell="{ row }">
          ¥{{ (row.original.balance ?? 0).toFixed(2) }}
        </template>

        <template #created_at-cell="{ row }">
          {{ formatDate(row.original.createdAt) }}
        </template>

        <template #actions-cell="{ row }">
          <div class="flex items-center gap-2">
            <UButton
              color="primary"
              variant="ghost"
              size="sm"
              icon="i-heroicons-pencil"
              @click="openEditModal(row.original)"
            />
            <UButton
              color="error"
              variant="ghost"
              size="sm"
              icon="i-heroicons-trash"
              @click="confirmDelete(row.original)"
            />
          </div>
        </template>
      </UTable>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mt-6 flex justify-center">
        <Pagination
          v-model:current-page="currentPage"
          :total-pages="totalPages"
          @change="fetchUsers"
        />
      </div>
    </UCard>

    <!-- Create User Modal -->
    <UModal v-model:open="createModalOpen">
      <template #content>
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">添加用户</h3>
          </template>

          <form @submit.prevent="createUser" class="space-y-4">
            <UFormField label="用户名" required>
              <UInput v-model="createForm.username" placeholder="请输入用户名" />
            </UFormField>

            <UFormField label="邮箱" required>
              <UInput v-model="createForm.email" type="email" placeholder="请输入邮箱" />
            </UFormField>

            <UFormField label="密码" required>
              <UInput v-model="createForm.password" type="password" placeholder="请输入密码" />
            </UFormField>

            <UFormField label="角色">
              <USelect v-model="createForm.role" :items="roleOptions" />
            </UFormField>
          </form>

          <template #footer>
            <div class="flex justify-end gap-3">
              <UButton color="neutral" variant="ghost" @click="createModalOpen = false">
                取消
              </UButton>
              <UButton color="primary" :loading="submitting" @click="createUser">
                创建
              </UButton>
            </div>
          </template>
        </UCard>
      </template>
    </UModal>

    <!-- Edit User Modal -->
    <UModal v-model:open="editModalOpen">
      <template #content>
        <UCard>
          <template #header>
            <h3 class="text-lg font-semibold">编辑用户</h3>
          </template>

          <form class="space-y-4">
            <UFormField label="用户名">
              <UInput :model-value="editForm.username" disabled />
            </UFormField>

            <UFormField label="邮箱">
              <UInput v-model="editForm.email" type="email" />
            </UFormField>

            <UFormField label="新密码（留空不修改）">
              <UInput v-model="editForm.password" type="password" placeholder="请输入新密码" />
            </UFormField>
          </form>

          <template #footer>
            <div class="flex justify-end gap-3">
              <UButton color="neutral" variant="ghost" @click="editModalOpen = false">
                取消
              </UButton>
              <UButton color="primary" :loading="submitting" @click="updateUser">
                保存
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
              确定要删除用户 "{{ userToDelete?.username }}" 吗？此操作不可撤销。
            </p>
            <div class="flex justify-center gap-3">
              <UButton color="neutral" variant="ghost" @click="deleteModalOpen = false">
                取消
              </UButton>
              <UButton color="error" :loading="deleting" @click="deleteUser">
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
import type { User } from "../../../types/user";
import { useAppToast } from "../../../composables/useToast";
import { useAdminApi } from "../../../api/admin";

definePageMeta({
  middleware: ["auth", "admin"],
});

const toast = useAppToast();
const adminApi = useAdminApi();
const users = ref<User[]>([]);
const currentPage = ref(1);
const totalPages = ref(1);
const pageSize = 10;
const searchQuery = ref("");

const createModalOpen = ref(false);
const editModalOpen = ref(false);
const deleteModalOpen = ref(false);
const submitting = ref(false);
const deleting = ref(false);

const userToEdit = ref<User | null>(null);
const userToDelete = ref<User | null>(null);

const createForm = reactive({
  username: "",
  email: "",
  password: "",
  role: "user" as "user" | "admin",
});

const editForm = reactive({
  username: "",
  email: "",
  password: "",
});

const roleOptions = [
  { value: "user", label: "普通用户" },
  { value: "admin", label: "管理员" },
];

const columns = [
  { accessorKey: "id", header: "ID" },
  { accessorKey: "username", header: "用户名" },
  { accessorKey: "email", header: "邮箱" },
  { accessorKey: "role", header: "角色" },
  { accessorKey: "balance", header: "余额" },
  { accessorKey: "created_at", header: "注册时间" },
  {
    id: "actions",
    header: "操作",
    cell: () => null,
  },
];

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN");
};

const fetchUsers = async () => {
  try {
    const result = await adminApi.getUserList({
      page: currentPage.value,
      page_size: pageSize,
      search: searchQuery.value || undefined,
    });
    users.value = result.items;
    totalPages.value = Math.ceil(result.total / pageSize);
  } catch (error) {
    toast.error("获取用户列表失败");
  }
};

const createUser = async () => {
  if (!createForm.username || !createForm.email || !createForm.password) {
    toast.error("请填写所有必填项");
    return;
  }

  submitting.value = true;
  try {
    await adminApi.createUser(createForm);
    toast.success("用户创建成功");
    createModalOpen.value = false;
    createForm.username = "";
    createForm.email = "";
    createForm.password = "";
    createForm.role = "user";
    await fetchUsers();
  } catch (error) {
    toast.error("创建用户失败");
  } finally {
    submitting.value = false;
  }
};

const openEditModal = (user: User) => {
  userToEdit.value = user;
  editForm.username = user.username;
  editForm.email = user.email;
  editForm.password = "";
  editModalOpen.value = true;
};

const updateUser = async () => {
  if (!userToEdit.value) return;

  submitting.value = true;
  try {
    await adminApi.updateUser({
      user_id: userToEdit.value.id,
      email: editForm.email,
      password: editForm.password || undefined,
    });
    toast.success("用户更新成功");
    editModalOpen.value = false;
    await fetchUsers();
  } catch (error) {
    toast.error("更新用户失败");
  } finally {
    submitting.value = false;
  }
};

const confirmDelete = (user: User) => {
  userToDelete.value = user;
  deleteModalOpen.value = true;
};

const deleteUser = async () => {
  if (!userToDelete.value) return;

  deleting.value = true;
  try {
    await adminApi.deleteUser(userToDelete.value.id);
    toast.success("用户已删除");
    await fetchUsers();
  } catch (error) {
    toast.error("删除用户失败");
  } finally {
    deleting.value = false;
    deleteModalOpen.value = false;
    userToDelete.value = null;
  }
};

onMounted(() => {
  fetchUsers();
});
</script>