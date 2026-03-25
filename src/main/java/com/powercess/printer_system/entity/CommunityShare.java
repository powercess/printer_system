package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("community_shares")
public class CommunityShare {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long fileId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime deletedAt;

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String nickname;

    @TableField(exist = false)
    private String fileName;

    @TableField(exist = false)
    private String filePath;

    @TableField(exist = false)
    private Integer likeCount;

    @TableField(exist = false)
    private Boolean isLiked;
}