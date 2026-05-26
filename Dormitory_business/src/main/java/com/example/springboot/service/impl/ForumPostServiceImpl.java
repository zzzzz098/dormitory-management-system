package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.ForumComment;
import com.example.springboot.entity.ForumPost;
import com.example.springboot.mapper.ForumCommentMapper;
import com.example.springboot.mapper.ForumPostMapper;
import com.example.springboot.service.ForumPostService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ForumPostServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost> implements ForumPostService {

    private static final List<String> CATEGORIES = Arrays.asList("失物", "招领", "分享", "求助");

    @Resource
    private ForumPostMapper forumPostMapper;

    @Resource
    private ForumCommentMapper forumCommentMapper;

    @Override
    public Page find(Integer pageNum, Integer pageSize, String search, String category) {
        Page<ForumPost> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ForumPost> qw = new QueryWrapper<>();
        qw.eq("deleted", 0);
        if (category != null && !category.isEmpty()) {
            qw.eq("category", category);
        }
        if (search != null && !search.isEmpty()) {
            qw.and(wrapper -> wrapper.like("title", search).or().like("content", search));
        }
        qw.orderByDesc("create_time");
        return forumPostMapper.selectPage(page, qw);
    }

    @Override
    public Map<String, Object> detail(Integer id) {
        Map<String, Object> result = new HashMap<>();
        ForumPost post = forumPostMapper.selectById(id);
        if (post == null || post.getDeleted() == null || post.getDeleted() != 0) {
            return null;
        }
        QueryWrapper<ForumComment> qw = new QueryWrapper<>();
        qw.eq("post_id", id).eq("deleted", 0).orderByAsc("create_time");
        result.put("post", post);
        result.put("comments", forumCommentMapper.selectList(qw));
        return result;
    }

    @Override
    public int addPost(ForumPost post, HttpSession session) {
        if (!"stu".equals(ForumSessionHelper.identity(session)) || !validPost(post)) {
            return 0;
        }
        String now = ForumSessionHelper.now();
        post.setId(null);
        post.setAuthorUsername(ForumSessionHelper.username(session));
        post.setAuthorName(ForumSessionHelper.name(session));
        post.setCreateTime(now);
        post.setUpdateTime(now);
        post.setDeleted(0);
        return forumPostMapper.insert(post);
    }

    @Override
    public int updatePost(ForumPost post, HttpSession session) {
        if (post.getId() == null || !validPost(post)) {
            return 0;
        }
        ForumPost current = forumPostMapper.selectById(post.getId());
        if (current == null || current.getDeleted() == null || current.getDeleted() != 0) {
            return 0;
        }
        if (!ForumSessionHelper.username(session).equals(current.getAuthorUsername())) {
            return 0;
        }
        current.setTitle(post.getTitle());
        current.setContent(post.getContent());
        current.setCategory(post.getCategory());
        current.setImage(post.getImage());
        current.setUpdateTime(ForumSessionHelper.now());
        return forumPostMapper.updateById(current);
    }

    @Override
    public int deletePost(Integer id, HttpSession session) {
        ForumPost current = forumPostMapper.selectById(id);
        if (current == null || current.getDeleted() == null || current.getDeleted() != 0) {
            return 0;
        }
        boolean owner = ForumSessionHelper.username(session).equals(current.getAuthorUsername());
        if (!owner && !ForumSessionHelper.isManager(session)) {
            return 0;
        }
        current.setDeleted(1);
        current.setUpdateTime(ForumSessionHelper.now());
        int updated = forumPostMapper.updateById(current);
        if (updated == 1) {
            ForumComment comment = new ForumComment();
            comment.setDeleted(1);
            UpdateWrapper<ForumComment> wrapper = new UpdateWrapper<>();
            wrapper.eq("post_id", id).eq("deleted", 0);
            forumCommentMapper.update(comment, wrapper);
        }
        return updated;
    }

    private boolean validPost(ForumPost post) {
        return post != null
                && post.getTitle() != null && !post.getTitle().trim().isEmpty()
                && post.getContent() != null && !post.getContent().trim().isEmpty()
                && CATEGORIES.contains(post.getCategory())
                && validImage(post.getImage());
    }

    private boolean validImage(String image) {
        return image == null || image.isEmpty()
                || (!image.contains("..") && !image.contains("/") && !image.contains("\\"));
    }
}
