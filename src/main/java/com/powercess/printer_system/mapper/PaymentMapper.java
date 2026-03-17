package com.powercess.printer_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powercess.printer_system.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}