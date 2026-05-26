package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.ForumComment;
import com.example.springboot.entity.ForumPost;
import com.example.springboot.mapper.ForumCommentMapper;
import com.example.springboot.mapper.ForumPostMapper;
import com.example.springboot.service.ForumCommentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

@Service
public class ForumCommentServiceImpl extends ServiceImpl<ForumCommentMapper, ForumComment> implements ForumCommentService {

    @Resource
    private ForumCommentMapper forumCommentMapper;

    @Resource
    private ForumPostMapper forumPostMapper;

    @Override
    public int addComment(ForumComment comment, HttpSession session) {
        if (comment == null || comment.getPostId() == null
                || comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return 0;
        }
        ForumPost post = forumPostMapper.selectById(comment.getPostId());
        if (post == null || post.getDeleted() == null || post.getDeleted() != 0) {
            return 0;
        }
        comment.setId(null);
        comment.setAuthorUsername(ForumSessionHelper.username(session));
        comment.setAuthorName(ForumSessionHelper.name(session));
        comment.setCreateTime(ForumSessionHelper.now());
        comment.setDeleted(0);
        return forumCommentMapper.insert(comment);
    }

    @Override
    public int updateComment(ForumComment comment, HttpSession session) {
        if (comment == null || comment.getId() == null
                || comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            return 0;
        }
        ForumComment current = forumCommentMapper.selectById(comment.getId());
        if (current == null || current.getDeleted() == null || current.getDeleted() != 0) {
            return 0;
        }
        if (!ForumSessionHelper.username(session).equals(current.getAuthorUsername())) {
            return 0;
        }
        current.setContent(comment.getContent());
        return forumCommentMapper.updateById(current);
    }

    @Override
    public int deleteComment(Integer id, HttpSession session) {
        ForumComment current = forumCommentMapper.selectById(id);
        if (current == null || current.getDeleted() == null || current.getDeleted() != 0) {
            return 0;
        }
        boolean owner = ForumSessionHelper.username(session).equals(current.getAuthorUsername());
        if (!owner && !ForumSessionHelper.isManager(session)) {
            return 0;
        }
        current.setDeleted(1);
        return forumCommentMapper.updateById(current);
    }
}
