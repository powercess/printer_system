package com.powercess.printer_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powercess.printer_system.entity.FileBlob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface FileBlobMapper extends BaseMapper<FileBlob> {

    /**
     * 根据内容哈希查找 FileBlob
     */
    @Select("SELECT * FROM file_blobs WHERE content_hash = #{contentHash}")
    Optional<FileBlob> findByContentHash(String contentHash);

    /**
     * 增加引用计数
     */
    @Select("UPDATE file_blobs SET ref_count = ref_count + 1 WHERE id = #{id}")
    void incrementRefCount(Long id);

    /**
     * 减少引用计数
     */
    @Select("UPDATE file_blobs SET ref_count = ref_count - 1 WHERE id = #{id} AND ref_count > 0")
    void decrementRefCount(Long id);
}