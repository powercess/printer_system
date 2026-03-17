package com.powercess.printer_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powercess.printer_system.entity.FileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface FileMapper extends BaseMapper<FileEntity> {

    @Select("SELECT * FROM files WHERE id = #{id} AND deleted_at IS NULL")
    Optional<FileEntity> findByIdNotDeleted(Long id);
}