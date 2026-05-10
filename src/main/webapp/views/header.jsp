<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="cn.edu.zju.bean.User" %>
<%
    User currentUser = (User) session.getAttribute("currentUser");
%>
<nav class="navbar navbar-dark fixed-top bg-dark flex-md-nowrap p-0 shadow">
    <a class="navbar-brand col-sm-3 col-md-2 mr-0" href="<%=request.getContextPath()%>/">Precision Medicine Matching System</a>

    <ul class="navbar-nav navbar-nav-right d-flex flex-row">
        <% if (currentUser == null) { %>
            <li class="nav-item mr-3">
                <a class="nav-link" href="<%=request.getContextPath()%>/login">Login</a>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="<%=request.getContextPath()%>/register">Register</a>
            </li>
        <% } else { %>
            <li class="nav-item">
                <span class="username-text">Welcome, <%= currentUser.getUsername() %></span>
            </li>
            <li class="nav-item">
                <a class="nav-link" href="<%=request.getContextPath()%>/logout">Logout</a>
            </li>
        <% } %>
    </ul>
</nav>