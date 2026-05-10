<%--
  Created by IntelliJ IDEA.
  User: hello
  Date: 2019-12-3
  Time: 15:37
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta name="description" content="">
    <meta name="author" content="">
    <meta name="generator" content="">
    <title>Dashboard Template · Bootstrap</title>

    <!-- Bootstrap core CSS -->
    <link href="<%=request.getContextPath()%>/static/bootstrap/css/bootstrap.css" rel="stylesheet">
    <script src="<%=request.getContextPath()%>/static/jquery/jquery-3.4.1.js"></script>
    <script src="<%=request.getContextPath()%>/static/bootstrap/js/bootstrap.bundle.min.js"></script>
    <!-- Custom styles for this template -->
    <link href="<%=request.getContextPath()%>/static/css/app.css" rel="stylesheet">
    <style>
        .bd-placeholder-img {
            font-size: 1.125rem;
            text-anchor: middle;
            -webkit-user-select: none;
            -moz-user-select: none;
            -ms-user-select: none;
            user-select: none;
        }

        @media (min-width: 768px) {
            .bd-placeholder-img-lg {
                font-size: 3.5rem;
            }
        }
    </style>
</head>

<body class="index-page">

<jsp:include page="header.jsp" />

<!-- 改动2：侧边栏和触发区在 row 外面 -->
<div class="sidebar-trigger"></div>
<jsp:include page="nav.jsp" >
    <jsp:param name="active" value="dashboard" />
</jsp:include>

<div class="container-fluid">
    <div class="row">

        <main role="main" class="col-md-12 ml-sm-auto px-0 hero-container">
            <div class="hero-content text-center">
                <h1 class="hero-title">Precision Medicine<br>Matching System</h1>
                <p class="hero-subtitle mt-3">Integrating pharmacogenomics knowledge with clinical decision support</p>
                <a href="<%=request.getContextPath()%>/login" class="btn btn-outline-primary btn-lg mt-4 px-5">Log In</a>
                <p class="hero-footer mt-3">
                    <a href="<%=request.getContextPath()%>/register" class="text-white-50">Don't have an account? Register</a>
                </p>
            </div>
        </main>

    </div>
</div>
</body>
</html>