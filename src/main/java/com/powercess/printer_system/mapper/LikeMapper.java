package com.powercess.printer_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powercess.printer_system.entity.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    @Select("SELECT COUNT(*) FROM likes WHERE share_id = #{shareId} AND user_id = #{userId}")
    int countByShareIdAndUserId(@Param("shareId") Long shareId, @Param("userId") Long userId);
}