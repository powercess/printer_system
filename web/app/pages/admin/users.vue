<template>
  <NuxtLayout name="admin">
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
          用户管理
        </h1>
        <div class="flex gap-2">
          <UButton
            v-if="selectedIds.length > 0"
            color="error"
            variant="outline"
            icon="i-heroicons-outline-trash"
            @click="confirmBatchDelete"
          >
            批量删除 ({{ selectedIds.length }})
          </UButton>
          <UButton color="primary" icon="i-heroicons-solid-plus" @click="openCreateModal">
            添加用户
          </UButton>
        </div>
      </div>

      <!-- Search & Filter -->
      <UCard>
        <div class="flex flex-wrap gap-4">
          <UInput
            v-model="searchQuery"
            placeholder="搜索用户名或邮箱..."
            icon="i-heroicons-outline-magnifying-glass"
            class="flex-1 min-w-[200px]"
            @keyup.enter="fetchUsers"
          />
          <USelect
            v-model="groupFilter"
            :items="groupOptions"
            placeholder="用户组"
            class="w-40"
            @update:model-value="fetchUsers"
          />
          <UButton color="primary" @click="fetchUsers">搜索</UButton>
        </div>
      </UCard>

      <!-- Batch Actions Bar -->
      <div v-if="selectedIds.length > 0" class="flex items-center gap-4 p-3 bg-primary/10 rounded-lg">
        <UCheckbox
          :model-value="selectedIds.length === users.length && users.length > 0"
          :indeterminate="selectedIds.length > 0 && selectedIds.length < users.length"
          @update:model-value="(val: boolean | 'indeterminate') => toggleSelectAll(val === true)"
        />
        <span class="text-sm">已选择 {{ selectedIds.length }} 项</span>
        <UButton color="neutral" variant="ghost" size="sm" @click="selectedIds = []">
          取消选择
        </UButton>
        <div class="flex-1" />
        <UButton color="error" variant="outline" size="sm" icon="i-heroicons-outline-trash" @click="confirmBatchDelete">
          批量删除
        </UButton>
      </div>

      <!-- Users Table -->
      <UCard>
        <UTable :data="users" :columns="columns" v-model:sort="sort">
          <template #select-header>
            <UCheckbox
              :model-value="selectedIds.length === users.length && users.length > 0"
              :indeterminate="selectedIds.length > 0 && selectedIds.length < users.length"
              @update:model-value="(val: boolean | 'indeterminate') => toggleSelectAll(val === true)"
            />
          </template>

          <template #select-cell="{ row }">
            <UCheckbox
              :model-value="selectedIds.includes(row.original.id)"
              @update:model-value="(val: boolean | 'indeterminate') => toggleSelect(row.original.id, val === true)"
            />
          </template>

          <template #user-cell="{ row }">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                <span class="font-medium">{{ (row.original.nickname || row.original.username).charAt(0).toUpperCase() }}</span>
              </div>
              <div>
                <p class="font-medium">{{ row.original.nickname || row.original.username }}</p>
                <p class="text-sm text-gray-500">@{{ row.original.username }}</p>
              </div>
            </div>
          </template>

          <template #role-cell="{ row }">
            <UBadge :color="row.original.groupId === 0 ? 'primary' : 'neutral'" variant="subtle">
              {{ row.original.groupId === 0 ? "管理员" : row.original.groupName || "普通用户" }}
            </UBadge>
          </template>

          <template #balance-cell="{ row }">
            <span class="font-medium">¥{{ (row.original.walletBalance ?? 0).toFixed(2) }}</span>
          </template>

          <template #status-cell="{ row }">
            <UBadge :color="row.original.deletedAt ? 'error' : 'success'" variant="subtle">
              {{ row.original.deletedAt ? "已删除" : "正常" }}
            </UBadge>
          </template>

          <template #created_at-cell="{ row }">
            {{ formatDate(row.original.createdAt) }}
          </template>

          <template #actions-cell="{ row }">
            <div class="flex items-center gap-1">
              <UButton
                color="primary"
                variant="ghost"
                size="xs"
                icon="i-heroicons-outline-eye"
                @click="openDetailModal(row.original)"
              />
              <UButton
                color="primary"
                variant="ghost"
                size="xs"
                icon="i-heroicons-outline-pencil"
                @click="openEditModal(row.original)"
              />
              <UButton
                color="error"
                variant="ghost"
                size="xs"
                icon="i-heroicons-outline-trash"
                :disabled="row.original.groupId === 0"
                @click="confirmDelete(row.original)"
              />
            </div>
          </template>
        </UTable>

        <!-- Pagination -->
        <div class="flex items-center justify-between mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
          <div class="text-sm text-gray-500">
            共 {{ total }} 条记录，当前第 {{ currentPage }}/{{ totalPages }} 页
          </div>
          <UPagination
            v-model:page="currentPage"
            :total="total"
            :items-per-page="pageSize"
            show-controls
            @update:page="fetchUsers"
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

            <form class="space-y-4" @submit.prevent="createUser">
              <UFormField label="用户名" required>
                <UInput v-model="createForm.username" placeholder="请输入用户名" />
              </UFormField>

              <UFormField label="邮箱" required>
                <UInput v-model="createForm.email" type="email" placeholder="请输入邮箱" />
              </UFormField>

              <UFormField label="密码" required>
                <UInput v-model="createForm.password" type="password" placeholder="请输入密码" />
              </UFormField>

              <UFormField label="昵称">
                <UInput v-model="createForm.nickname" placeholder="请输入昵称" />
              </UFormField>

              <UFormField label="用户组">
                <USelect v-model="createForm.groupId" :items="groupSelectOptions" />
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

              <UFormField label="昵称">
                <UInput v-model="editForm.nickname" placeholder="请输入昵称" />
              </UFormField>

              <UFormField label="邮箱">
                <UInput v-model="editForm.email" type="email" placeholder="请输入邮箱" />
              </UFormField>

              <UFormField label="新密码（留空不修改）">
                <UInput v-model="editForm.password" type="password" placeholder="请输入新密码" />
              </UFormField>

              <UFormField label="用户组">
                <USelect v-model="editForm.groupId" :items="groupSelectOptions" />
              </UFormField>

              <UFormField label="钱包余额">
                <UInput v-model.number="editForm.walletBalance" type="number" step="0.01" placeholder="请输入余额" />
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

      <!-- User Detail Modal -->
      <UModal v-model:open="detailModalOpen">
        <template #content>
          <UCard>
            <template #header>
              <h3 class="text-lg font-semibold">用户详情</h3>
            </template>

            <div v-if="selectedUser" class="space-y-4">
              <div class="flex items-center gap-4">
                <div class="w-16 h-16 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                  <span class="text-2xl font-medium">{{ (selectedUser.nickname || selectedUser.username).charAt(0).toUpperCase() }}</span>
                </div>
                <div>
                  <p class="text-xl font-semibold">{{ selectedUser.nickname || selectedUser.username }}</p>
                  <p class="text-gray-500">@{{ selectedUser.username }}</p>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p class="text-gray-500">用户ID</p>
                  <p class="font-medium">{{ selectedUser.id }}</p>
                </div>
                <div>
                  <p class="text-gray-500">邮箱</p>
                  <p class="font-medium">{{ selectedUser.email }}</p>
                </div>
                <div>
                  <p class="text-gray-500">用户组</p>
                  <p class="font-medium">{{ selectedUser.groupId === 0 ? "管理员" : selectedUser.groupName || "普通用户" }}</p>
                </div>
                <div>
                  <p class="text-gray-500">钱包余额</p>
                  <p class="font-medium">¥{{ (selectedUser.walletBalance ?? 0).toFixed(2) }}</p>
                </div>
                <div>
                  <p class="text-gray-500">注册时间</p>
                  <p class="font-medium">{{ formatDate(selectedUser.createdAt) }}</p>
                </div>
                <div>
                  <p class="text-gray-500">更新时间</p>
                  <p class="font-medium">{{ formatDate(selectedUser.updatedAt) }}</p>
                </div>
              </div>
            </div>

            <template #footer>
              <div class="flex justify-end gap-3">
                <UButton color="neutral" variant="ghost" @click="detailModalOpen = false">
                  关闭
                </UButton>
                <UButton color="primary" @click="openEditModal(selectedUser!); detailModalOpen = false">
                  编辑
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

      <!-- Batch Delete Confirmation Modal -->
      <UModal v-model:open="batchDeleteModalOpen">
        <template #content>
          <UCard>
            <div class="text-center">
              <UIcon name="i-heroicons-solid-exclamation-triangle" class="w-12 h-12 mx-auto text-red-500 mb-4" />
              <h3 class="text-lg font-semibold mb-2">确认批量删除</h3>
              <p class="text-gray-500 mb-6">
                确定要删除选中的 {{ selectedIds.length }} 个用户吗？此操作不可撤销。
              </p>
              <div class="flex justify-center gap-3">
                <UButton color="neutral" variant="ghost" @click="batchDeleteModalOpen = false">
                  取消
                </UButton>
                <UButton color="error" :loading="batchDeleting" @click="batchDeleteUsers">
                  删除
                </UButton>
              </div>
            </div>
          </UCard>
        </template>
      </UModal>
    </div>
  </NuxtLayout>
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
const total = ref(0);
const pageSize = 10;
const searchQuery = ref("");
const groupFilter = ref<string>("all");
const sort = ref({ column: "createdAt", direction: "desc" as const });

// Selection
const selectedIds = ref<number[]>([]);

const createModalOpen = ref(false);
const editModalOpen = ref(false);
const detailModalOpen = ref(false);
const deleteModalOpen = ref(false);
const batchDeleteModalOpen = ref(false);
const submitting = ref(false);
const deleting = ref(false);
const batchDeleting = ref(false);

const selectedUser = ref<User | null>(null);
const userToEdit = ref<User | null>(null);
const userToDelete = ref<User | null>(null);

const createForm = reactive({
  username: "",
  email: "",
  password: "",
  nickname: "",
  groupId: 1,
});

const editForm = reactive({
  username: "",
  email: "",
  password: "",
  nickname: "",
  groupId: 1,
  walletBalance: 0,
});

const groupOptions = [
  { value: "all", label: "全部用户组" },
  { value: "0", label: "管理员" },
  { value: "1", label: "普通用户" },
];

const groupSelectOptions = [
  { value: 0, label: "管理员" },
  { value: 1, label: "普通用户" },
];

const columns = [
  { id: "select", header: "" },
  { id: "user", header: "用户", sortable: true },
  { accessorKey: "email", header: "邮箱", sortable: true },
  { id: "role", header: "角色" },
  { id: "balance", header: "余额", sortable: true },
  { id: "status", header: "状态" },
  { accessorKey: "createdAt", header: "注册时间", sortable: true },
  { id: "actions", header: "操作" },
];

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN");
};

const toggleSelect = (id: number, selected: boolean) => {
  if (selected) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id);
    }
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id);
  }
};

const toggleSelectAll = (selected: boolean) => {
  if (selected) {
    selectedIds.value = users.value.map(u => u.id);
  } else {
    selectedIds.value = [];
  }
};

const fetchUsers = async () => {
  try {
    const result = await adminApi.getUserList({
      page: currentPage.value,
      pageSize: pageSize,
      username: searchQuery.value || undefined,
      groupId: groupFilter.value === "all" ? undefined : Number(groupFilter.value),
    });
    users.value = result.items;
    total.value = result.total;
    totalPages.value = Math.ceil(result.total / pageSize);
    // Clear selection when page changes
    selectedIds.value = [];
  } catch (error) {
    toast.error("获取用户列表失败");
  }
};

const openCreateModal = () => {
  createForm.username = "";
  createForm.email = "";
  createForm.password = "";
  createForm.nickname = "";
  createForm.groupId = 1;
  createModalOpen.value = true;
};

const createUser = async () => {
  if (!createForm.username || !createForm.email || !createForm.password) {
    toast.error("请填写所有必填项");
    return;
  }

  submitting.value = true;
  try {
    await adminApi.createUser({
      username: createForm.username,
      email: createForm.email,
      password: createForm.password,
      nickname: createForm.nickname || undefined,
      groupId: createForm.groupId,
    });
    toast.success("用户创建成功");
    createModalOpen.value = false;
    await fetchUsers();
  } catch (error) {
    toast.error("创建用户失败");
  } finally {
    submitting.value = false;
  }
};

const openDetailModal = (user: User) => {
  selectedUser.value = user;
  detailModalOpen.value = true;
};

const openEditModal = (user: User) => {
  userToEdit.value = user;
  editForm.username = user.username;
  editForm.email = user.email;
  editForm.password = "";
  editForm.nickname = user.nickname || "";
  editForm.groupId = user.groupId;
  editForm.walletBalance = user.walletBalance ?? 0;
  editModalOpen.value = true;
};

const updateUser = async () => {
  if (!userToEdit.value) return;

  submitting.value = true;
  try {
    await adminApi.updateUser({
      userId: userToEdit.value.id,
      email: editForm.email,
      password: editForm.password || undefined,
      nickname: editForm.nickname || undefined,
      groupId: editForm.groupId,
      walletBalance: editForm.walletBalance,
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

const confirmBatchDelete = () => {
  if (selectedIds.value.length === 0) return;
  batchDeleteModalOpen.value = true;
};

const batchDeleteUsers = async () => {
  batchDeleting.value = true;
  try {
    let successCount = 0;
    let failCount = 0;
    for (const userId of selectedIds.value) {
      try {
        await adminApi.deleteUser(userId);
        successCount++;
      } catch {
        failCount++;
      }
    }
    if (successCount > 0) {
      toast.success(`成功删除 ${ successCount } 个用户${ failCount > 0 ? `，${ failCount } 个失败` : "" }`);
    } else {
      toast.error("删除失败");
    }
    selectedIds.value = [];
    batchDeleteModalOpen.value = false;
    await fetchUsers();
  } finally {
    batchDeleting.value = false;
  }
};

onMounted(() => {
  fetchUsers();
});
</script>