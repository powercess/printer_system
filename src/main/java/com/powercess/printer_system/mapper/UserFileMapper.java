package com.powercess.printer_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powercess.printer_system.entity.UserFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserFileMapper extends BaseMapper<UserFile> {

    /**
     * 根据ID查找未删除的用户文件（包含关联的 blob 信息）
     */
    @Select("""
        SELECT uf.*, fb.storage_path, fb.file_size, fb.file_type
        FROM user_files uf
        LEFT JOIN file_blobs fb ON uf.blob_id = fb.id
        WHERE uf.id = #{id} AND uf.deleted_at IS NULL
        """)
    Optional<UserFile> findByIdNotDeletedWithBlob(Long id);

    /**
     * 根据ID查找未删除的用户文件（不含 blob 信息）
     */
    @Select("SELECT * FROM user_files WHERE id = #{id} AND deleted_at IS NULL")
    Optional<UserFile> findByIdNotDeleted(Long id);
}