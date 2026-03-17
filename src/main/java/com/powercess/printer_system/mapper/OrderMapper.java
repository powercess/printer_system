package com.powercess.printer_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powercess.printer_system.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT o.*, f.name as file_name, u.username FROM orders o " +
            "LEFT JOIN files f ON o.file_id = f.id " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.id = #{id}")
    Optional<Order> findOrderWithDetails(Long id);
}