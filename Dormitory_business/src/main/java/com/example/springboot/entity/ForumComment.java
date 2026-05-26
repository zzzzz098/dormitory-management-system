package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName(value = "forum_comment")
public class ForumComment {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("post_id")
    private Integer postId;
    @TableField("content")
    private String content;
    @TableField("author_username")
    private String authorUsername;
    @TableField("author_name")
    private String authorName;
    @TableField("create_time")
    private String createTime;
    @TableField("deleted")
    private Integer deleted;
}
