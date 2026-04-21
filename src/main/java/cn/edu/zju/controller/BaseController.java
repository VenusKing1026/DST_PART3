package cn.edu.zju.controller;

import cn.edu.zju.bean.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class BaseController {

    protected static final String SESSION_USER_KEY = "currentUser";

    /**
     * 获取当前登录用户
     */
    protected User getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(SESSION_USER_KEY);
    }

    /**
     * 获取当前登录用户ID
     * 未登录返回 -1
     */
    protected int getCurrentUserId(HttpServletRequest request) {
        User user = getCurrentUser(request);
        return user != null ? user.getId() : -1;
    }

    /**
     * 【临时方法】开发阶段使用，固定返回用户ID=1
     * 等登录功能完成后删除此方法
     */
    protected int getCurrentUserIdForDev() {
        return 1;
    }
}