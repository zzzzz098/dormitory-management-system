package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.ForumPost;

import javax.servlet.http.HttpSession;
import java.util.Map;

public interface ForumPostService extends IService<ForumPost> {
    Page find(Integer pageNum, Integer pageSize, String search, String category);

    Map<String, Object> detail(Integer id);

    int addPost(ForumPost post, HttpSession session);

    int updatePost(ForumPost post, HttpSession session);

    int deletePost(Integer id, HttpSession session);
}
