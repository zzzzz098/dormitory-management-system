package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.ForumComment;

import javax.servlet.http.HttpSession;

public interface ForumCommentService extends IService<ForumComment> {
    int addComment(ForumComment comment, HttpSession session);

    int updateComment(ForumComment comment, HttpSession session);

    int deleteComment(Integer id, HttpSession session);
}
