package cn.edu.zju.controller;

import cn.edu.zju.bean.User;
import cn.edu.zju.dao.UserDao;
import cn.edu.zju.servlet.DispatchServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


//处理认证相关的业务逻辑（登录、注册、退出）包括密码验证，session控制
public class AuthController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private UserDao userDao = new UserDao();

    public void register(DispatchServlet.Dispatcher dispatcher) {
        dispatcher.registerGetMapping("/login", this::loginPage);
        dispatcher.registerPostMapping("/login", this::login);
        dispatcher.registerGetMapping("/register", this::registerPage);
        dispatcher.registerPostMapping("/register", this::register);
        dispatcher.registerGetMapping("/logout", this::logout);
    }

    // ==================== 登录相关 ====================

    public void loginPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 已登录用户直接跳转首页
        if (getCurrentUser(request) != null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        request.getRequestDispatcher("/views/login.jsp").forward(request, response);
    }

    public void login(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // 参数校验
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            request.setAttribute("error", "用户名和密码不能为空");
            request.getRequestDispatcher("/views/login.jsp").forward(request, response);
            return;
        }

        // 查询用户
        User user = userDao.findByUsername(username);
        if (user == null) {
            request.setAttribute("error", "用户名不存在");
            request.getRequestDispatcher("/views/login.jsp").forward(request, response);
            return;
        }

        // 验证密码哈希
        String hashedPassword = hashPassword(password);
        if (!hashedPassword.equals(user.getPassword())) {
            request.setAttribute("error", "密码错误");
            request.getRequestDispatcher("/views/login.jsp").forward(request, response);
            return;
        }

        // 登录成功，存入 Session
        HttpSession session = request.getSession();
        session.setAttribute(SESSION_USER_KEY, user);
        log.info("User logged in: {}", username);

        response.sendRedirect(request.getContextPath() + "/");
    }

    // ==================== 注册相关 ====================

    public void registerPage(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 已登录用户直接跳转首页
        if (getCurrentUser(request) != null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        request.getRequestDispatcher("/views/register.jsp").forward(request, response);
    }

    public void register(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // 参数校验
        if (username == null || username.isBlank()) {
            request.setAttribute("error", "用户名不能为空");
            request.getRequestDispatcher("/views/register.jsp").forward(request, response);
            return;
        }
        if (password == null || password.isBlank()) {
            request.setAttribute("error", "密码不能为空");
            request.getRequestDispatcher("/views/register.jsp").forward(request, response);
            return;
        }
        if (!password.equals(confirmPassword)) {
            request.setAttribute("error", "两次输入的密码不一致");
            request.getRequestDispatcher("/views/register.jsp").forward(request, response);
            return;
        }

        // 检查用户名是否已存在
        if (userDao.existsByUsername(username)) {
            request.setAttribute("error", "用户名已存在");
            request.getRequestDispatcher("/views/register.jsp").forward(request, response);
            return;
        }

        // 创建用户（密码哈希存储）
        User user = new User();
        user.setUsername(username);
        user.setPassword(hashPassword(password));

        int userId = userDao.save(user);
        if (userId == -1) {
            request.setAttribute("error", "注册失败，请稍后重试");
            request.getRequestDispatcher("/views/register.jsp").forward(request, response);
            return;
        }

        // 注册成功，自动登录
        user.setId(userId);
        HttpSession session = request.getSession();
        session.setAttribute(SESSION_USER_KEY, user);
        log.info("User registered and logged in: {}", username);

        response.sendRedirect(request.getContextPath() + "/");
    }

    // ==================== 退出 ====================

    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String username = getCurrentUser(request) != null ? getCurrentUser(request).getUsername() : "unknown";
            session.invalidate();
            log.info("User logged out: {}", username);
        }
        response.sendRedirect(request.getContextPath() + "/");
    }

    // ==================== 密码哈希工具 ====================

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            return password; // fallback，不应该发生
        }
    }
}
