<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Register · Precision Medicine Matching System</title>

    <link href="<%=request.getContextPath()%>/static/bootstrap/css/bootstrap.css" rel="stylesheet">
    <script src="<%=request.getContextPath()%>/static/jquery/jquery-3.4.1.js"></script>
    <script src="<%=request.getContextPath()%>/static/bootstrap/js/bootstrap.bundle.min.js"></script>
    <link href="<%=request.getContextPath()%>/static/css/app.css" rel="stylesheet">
    <style>
        .register-container {
            max-width: 400px;
            margin: 100px auto;
        }
    </style>
</head>
<body>
<nav class="navbar navbar-dark fixed-top bg-dark flex-md-nowrap p-0 shadow">
    <a class="navbar-brand col-sm-3 col-md-2 mr-0" href="<%=request.getContextPath()%>/">Precision Medicine Matching System</a>
</nav>

<div class="container-fluid">
    <div class="row">
        <main role="main" class="col-md-12 ml-sm-auto px-4">
            <div class="register-container">
                <h2 class="text-center mb-4">Register</h2>

                <%-- 错误提示 --%>
                <% if (request.getAttribute("error") != null) { %>
                    <div class="alert alert-danger" role="alert">
                        ${error}
                    </div>
                <% } %>

                <form method="post" action="<%=request.getContextPath()%>/register">
                    <div class="form-group">
                        <label for="username">Username</label>
                        <input type="text" class="form-control" id="username" name="username"
                               placeholder="Choose a username" value="${param.username}" required autofocus>
                    </div>
                    <div class="form-group">
                        <label for="password">Password</label>
                        <input type="password" class="form-control" id="password" name="password"
                               placeholder="Password" required>
                    </div>
                    <div class="form-group">
                        <label for="confirmPassword">Confirm Password</label>
                        <input type="password" class="form-control" id="confirmPassword" name="confirmPassword"
                               placeholder="Re-enter password" required>
                    </div>
                    <button type="submit" class="btn btn-primary btn-block">Register</button>
                </form>

                <div class="text-center mt-3">
                    <a href="<%=request.getContextPath()%>/login">Already have an account? Login</a>
                </div>
            </div>
        </main>
    </div>
</div>
</body>
</html>