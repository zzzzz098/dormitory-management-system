package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.springboot.entity.ForumComment;
import com.example.springboot.entity.ForumPost;
import com.example.springboot.entity.Student;
import com.example.springboot.mapper.ForumCommentMapper;
import com.example.springboot.mapper.ForumPostMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForumServiceImplTest {

    @Test
    void addPostUsesSessionStudentAsAuthor() {
        ForumPostServiceImpl service = newPostService();
        ForumPostMapper postMapper = mapper(service, "forumPostMapper");
        when(postMapper.insert(any(ForumPost.class))).thenReturn(1);

        ForumPost post = new ForumPost();
        post.setTitle("Lost umbrella");
        post.setContent("Blue umbrella near Building 3");
        post.setCategory("失物");
        post.setAuthorUsername("hacker");
        post.setAuthorName("Fake");

        int result = service.addPost(post, studentSession("stu001", "张三"));

        assertThat(result).isEqualTo(1);
        verify(postMapper).insert(org.mockito.ArgumentMatchers.argThat(saved ->
                "stu001".equals(saved.getAuthorUsername())
                        && "张三".equals(saved.getAuthorName())
                        && saved.getDeleted() == 0
                        && saved.getCreateTime() != null
                        && saved.getUpdateTime() != null));
    }

    @Test
    void addPostRejectsUnknownCategory() {
        ForumPostServiceImpl service = newPostService();
        ForumPostMapper postMapper = mapper(service, "forumPostMapper");

        ForumPost post = new ForumPost();
        post.setTitle("Hello");
        post.setContent("World");
        post.setCategory("其他");

        int result = service.addPost(post, studentSession("stu001", "张三"));

        assertThat(result).isEqualTo(0);
        verify(postMapper, never()).insert(any(ForumPost.class));
    }

    @Test
    void deletePostAllowsAuthorAndManagerOnly() {
        ForumPostServiceImpl service = newPostService();
        ForumPostMapper postMapper = mapper(service, "forumPostMapper");
        ForumCommentMapper commentMapper = mapper(service, "forumCommentMapper");

        ForumPost post = new ForumPost();
        post.setId(8);
        post.setAuthorUsername("stu001");
        post.setDeleted(0);
        when(postMapper.selectById(8)).thenReturn(post);
        when(postMapper.updateById(any(ForumPost.class))).thenReturn(1);
        when(commentMapper.update(any(ForumComment.class), any(Wrapper.class))).thenReturn(1);

        assertThat(service.deletePost(8, studentSession("stu001", "张三"))).isEqualTo(1);
        post.setDeleted(0);
        assertThat(service.deletePost(8, studentSession("stu002", "李四"))).isEqualTo(0);
        post.setDeleted(0);
        assertThat(service.deletePost(8, managerSession())).isEqualTo(1);
    }

    @Test
    void addCommentRequiresExistingPostAndUsesSessionUser() {
        ForumCommentServiceImpl service = newCommentService();
        ForumCommentMapper commentMapper = mapper(service, "forumCommentMapper");
        ForumPostMapper postMapper = mapper(service, "forumPostMapper");
        ForumPost post = new ForumPost();
        post.setId(9);
        post.setDeleted(0);
        when(postMapper.selectById(9)).thenReturn(post);
        when(commentMapper.insert(any(ForumComment.class))).thenReturn(1);

        ForumComment comment = new ForumComment();
        comment.setPostId(9);
        comment.setContent("I saw it near the gate.");

        int result = service.addComment(comment, studentSession("stu001", "张三"));

        assertThat(result).isEqualTo(1);
        verify(commentMapper).insert(org.mockito.ArgumentMatchers.argThat(saved ->
                "stu001".equals(saved.getAuthorUsername())
                        && "张三".equals(saved.getAuthorName())
                        && saved.getDeleted() == 0
                        && saved.getCreateTime() != null));
    }

    @Test
    void updateCommentAllowsAuthorOnly() {
        ForumCommentServiceImpl service = newCommentService();
        ForumCommentMapper commentMapper = mapper(service, "forumCommentMapper");
        ForumComment current = new ForumComment();
        current.setId(5);
        current.setContent("old");
        current.setAuthorUsername("stu001");
        current.setDeleted(0);
        when(commentMapper.selectById(5)).thenReturn(current);
        when(commentMapper.updateById(any(ForumComment.class))).thenReturn(1);

        ForumComment update = new ForumComment();
        update.setId(5);
        update.setContent("new");

        assertThat(service.updateComment(update, studentSession("stu002", "李四"))).isEqualTo(0);
        assertThat(service.updateComment(update, studentSession("stu001", "张三"))).isEqualTo(1);
        verify(commentMapper).updateById(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getId() == 5 && "new".equals(saved.getContent())));
    }

    @SuppressWarnings("unchecked")
    private <T> T mapper(Object service, String fieldName) {
        return (T) ReflectionTestUtils.getField(service, fieldName);
    }

    private ForumPostServiceImpl newPostService() {
        ForumPostServiceImpl service = new ForumPostServiceImpl();
        ReflectionTestUtils.setField(service, "forumPostMapper", mock(ForumPostMapper.class));
        ReflectionTestUtils.setField(service, "forumCommentMapper", mock(ForumCommentMapper.class));
        return service;
    }

    private ForumCommentServiceImpl newCommentService() {
        ForumCommentServiceImpl service = new ForumCommentServiceImpl();
        ReflectionTestUtils.setField(service, "forumCommentMapper", mock(ForumCommentMapper.class));
        ReflectionTestUtils.setField(service, "forumPostMapper", mock(ForumPostMapper.class));
        return service;
    }

    private MockHttpSession studentSession(String username, String name) {
        Student student = new Student();
        student.setUsername(username);
        student.setName(name);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("Identity", "stu");
        session.setAttribute("User", student);
        return session;
    }

    private MockHttpSession managerSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("Identity", "dormManager");
        session.setAttribute("User", new Object());
        return session;
    }
}
