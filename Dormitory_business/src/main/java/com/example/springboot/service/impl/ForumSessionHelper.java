package com.example.springboot.service.impl;

import com.example.springboot.entity.Admin;
import com.example.springboot.entity.DormManager;
import com.example.springboot.entity.Student;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class ForumSessionHelper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ForumSessionHelper() {
    }

    static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    static String identity(HttpSession session) {
        Object identity = session.getAttribute("Identity");
        return identity == null ? "" : identity.toString();
    }

    static boolean isManager(HttpSession session) {
        String identity = identity(session);
        return "admin".equals(identity) || "dormManager".equals(identity);
    }

    static String username(HttpSession session) {
        Object user = session.getAttribute("User");
        if (user instanceof Student) {
            return ((Student) user).getUsername();
        }
        if (user instanceof Admin) {
            return ((Admin) user).getUsername();
        }
        if (user instanceof DormManager) {
            return ((DormManager) user).getUsername();
        }
        return "";
    }

    static String name(HttpSession session) {
        Object user = session.getAttribute("User");
        if (user instanceof Student) {
            return ((Student) user).getName();
        }
        if (user instanceof Admin) {
            return ((Admin) user).getName();
        }
        if (user instanceof DormManager) {
            return ((DormManager) user).getName();
        }
        return username(session);
    }
}
