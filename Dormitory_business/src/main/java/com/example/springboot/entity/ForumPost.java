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
@TableName(value = "forum_post")
public class ForumPost {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("title")
    private String title;
    @TableField("content")
    private String content;
    @TableField("category")
    private String category;
    @TableField("image")
    private String image;
    @TableField("author_username")
    private String authorUsername;
    @TableField("author_name")
    private String authorName;
    @TableField("create_time")
    private String createTime;
    @TableField("update_time")
    private String updateTime;
    @TableField("deleted")
    private Integer deleted;
}
