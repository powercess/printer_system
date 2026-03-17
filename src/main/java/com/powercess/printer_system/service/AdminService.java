package com.powercess.printer_system.service;

import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.admin.AdminUserCreateRequest;
import com.powercess.printer_system.dto.admin.AdminUserUpdateRequest;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.entity.User;

import java.util.Map;

public interface AdminService {

    void createUser(Long adminId, AdminUserCreateRequest request);

    PageResult<User> getUsers(Long adminId, int page, int pageSize, String username, Long groupId);

    void updateUser(Long adminId, Long userId, AdminUserUpdateRequest request);

    void deleteUser(Long adminId, Long userId);

    PageResult<Map<String, Object>> getFiles(Long adminId, int page, int pageSize, Long userId);

    PageResult<Order> getOrders(Long adminId, int page, int pageSize, Integer status, Long userId);

    Map<String, Object> getStats(Long adminId);
}