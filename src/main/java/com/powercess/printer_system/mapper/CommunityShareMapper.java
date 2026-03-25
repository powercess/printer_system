package com.powercess.printer_system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.powercess.printer_system.entity.CommunityShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CommunityShareMapper extends BaseMapper<CommunityShare> {

    @Select("""
        SELECT cs.*, u.username, u.nickname, uf.display_name as file_name, fb.storage_path as file_path,
               (SELECT COUNT(*) FROM likes WHERE share_id = cs.id) as like_count,
               (SELECT COUNT(*) FROM likes WHERE share_id = cs.id AND user_id = #{userId}) > 0 as is_liked
        FROM community_shares cs
        LEFT JOIN users u ON cs.user_id = u.id
        LEFT JOIN user_files uf ON cs.file_id = uf.id
        LEFT JOIN file_blobs fb ON uf.blob_id = fb.id
        WHERE cs.deleted_at IS NULL
        ORDER BY cs.created_at DESC
        """)
    List<CommunityShare> findAllWithDetails(@Param("userId") Long userId);

    @Update("UPDATE community_shares SET deleted_at = #{deletedAt} WHERE id = #{id} AND deleted_at IS NULL")
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}