package com.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.ForumComment;
import com.example.springboot.entity.ForumPost;
import com.example.springboot.service.ForumCommentService;
import com.example.springboot.service.ForumPostService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/forum")
public class ForumController {

    @Resource
    private ForumPostService forumPostService;

    @Resource
    private ForumCommentService forumCommentService;

    @GetMapping("/post/find")
    public Result<?> findPosts(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(defaultValue = "") String search,
                               @RequestParam(defaultValue = "") String category) {
        Page page = forumPostService.find(pageNum, pageSize, search, category);
        return page != null ? Result.success(page) : Result.error("-1", "查询失败");
    }

    @GetMapping("/post/{id}")
    public Result<?> detail(@PathVariable Integer id) {
        Map<String, Object> detail = forumPostService.detail(id);
        return detail != null ? Result.success(detail) : Result.error("-1", "帖子不存在");
    }

    @PostMapping("/post/add")
    public Result<?> addPost(@RequestBody ForumPost post, HttpSession session) {
        int i = forumPostService.addPost(post, session);
        return i == 1 ? Result.success() : Result.error("-1", "发布失败");
    }

    @PutMapping("/post/update")
    public Result<?> updatePost(@RequestBody ForumPost post, HttpSession session) {
        int i = forumPostService.updatePost(post, session);
        return i == 1 ? Result.success() : Result.error("-1", "更新失败");
    }

    @DeleteMapping("/post/delete/{id}")
    public Result<?> deletePost(@PathVariable Integer id, HttpSession session) {
        int i = forumPostService.deletePost(id, session);
        return i == 1 ? Result.success() : Result.error("-1", "删除失败");
    }

    @PostMapping("/comment/add")
    public Result<?> addComment(@RequestBody ForumComment comment, HttpSession session) {
        int i = forumCommentService.addComment(comment, session);
        return i == 1 ? Result.success() : Result.error("-1", "评论失败");
    }

    @PutMapping("/comment/update")
    public Result<?> updateComment(@RequestBody ForumComment comment, HttpSession session) {
        int i = forumCommentService.updateComment(comment, session);
        return i == 1 ? Result.success() : Result.error("-1", "更新失败");
    }

    @DeleteMapping("/comment/delete/{id}")
    public Result<?> deleteComment(@PathVariable Integer id, HttpSession session) {
        int i = forumCommentService.deleteComment(id, session);
        return i == 1 ? Result.success() : Result.error("-1", "删除失败");
    }
}
