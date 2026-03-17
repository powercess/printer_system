package com.powercess.printer_system.config;

import cn.dev33.satoken.stp.StpInterface;
import com.powercess.printer_system.entity.User;
import com.powercess.printer_system.entity.UserGroup;
import com.powercess.printer_system.mapper.UserGroupMapper;
import com.powercess.printer_system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserMapper userMapper;
    private final UserGroupMapper userGroupMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.parseLong(loginId.toString());
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Collections.emptyList();
        }

        UserGroup group = userGroupMapper.selectById(user.getGroupId());
        if (group == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(group.getGroupName());
    }
}