<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page isELIgnored="false" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Matching</title>
    <link href="<%=request.getContextPath()%>/static/bootstrap/css/bootstrap.css" rel="stylesheet">
    <script src="<%=request.getContextPath()%>/static/jquery/jquery-3.4.1.js"></script>
    <script src="<%=request.getContextPath()%>/static/bootstrap/js/bootstrap.bundle.min.js"></script>
    <link href="<%=request.getContextPath()%>/static/css/app.css" rel="stylesheet">
</head>
<body>
<jsp:include page="head.jsp" />

<div class="container-fluid">
    <div class="row">
        <jsp:include page="nav.jsp">
            <jsp:param name="active" value="matching" />
        </jsp:include>

        <main role="main" class="col-md-9 ml-sm-auto col-lg-10 px-4">
            <div class="pt-3 pb-2 mb-3 border-bottom">
                <h1>Matching</h1>
            </div>

            <div class="row">
                <div class="col-md-6">
                    <h3>Upload VCF</h3>
                    <form method="post" action="<%=request.getContextPath()%>/uploadVcf" enctype="multipart/form-data">
                        <div class="form-group">
                            <label>VCF File</label>
                            <input type="file" class="form-control-file" name="vcf" accept=".vcf" required>
                        </div>
                        <div class="form-group">
                            <label>Uploaded By</label>
                            <input type="text" class="form-control" name="uploaded_by" required>
                        </div>
                        <button type="submit" class="btn btn-primary">Upload and Annotate</button>
                    </form>
                </div>

                <div class="col-md-6">
                    <h3>Upload ANNOVAR Output</h3>
                    <form method="post" action="<%=request.getContextPath()%>/uploadAnnovar" enctype="multipart/form-data">
                        <div class="form-group">
                            <label>ANNOVAR Output</label>
                            <input type="file" class="form-control-file" name="annovar" required>
                        </div>
                        <div class="form-group">
                            <label>Uploaded By</label>
                            <input type="text" class="form-control" name="uploaded_by" required>
                        </div>
                        <button type="submit" class="btn btn-success">Upload Directly</button>
                    </form>
                </div>
            </div>
        </main>
    </div>
</div>
</body>
</html>