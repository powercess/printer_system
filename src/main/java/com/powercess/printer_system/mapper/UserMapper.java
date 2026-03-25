package com.powercess.printer_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powercess.printer_system.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username} AND deleted_at IS NULL")
    Optional<User> findByUsername(String username);

    @Select("SELECT * FROM users WHERE email = #{email} AND deleted_at IS NULL")
    Optional<User> findByEmail(String email);

    @Select("SELECT * FROM users WHERE id = #{id} AND deleted_at IS NULL")
    Optional<User> findByIdNotDeleted(Long id);

    @Update("UPDATE users SET deleted_at = #{deletedAt} WHERE id = #{id} AND deleted_at IS NULL")
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}