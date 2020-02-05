<#assign wp=JspTaglibs["/aps-core"]>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8" pageEncoding="UTF-8"/>
	<title><@wp.currentPage param="title" /></title>

	<link rel="stylesheet" href="<@wp.resourceURL />inail/css/bootstrap-italia.min.css">
	<link rel="stylesheet" href="<@wp.resourceURL />inail/css/custom.css">

	<link rel="shortcut icon" href="<@wp.resourceURL />inail/img/favicon.ico" type="image/x-icon"/>
</head>

<body>
	<@wp.show frame=0 />

	<div class="container">
		<@wp.show frame=1 />
		<div class="row">
			<div class="col-lg-3">
				<@wp.show frame=2 />
				<@wp.show frame=3 />
			</div>
			<div class="col-lg-9">
				<@wp.show frame=4 />
				<@wp.show frame=5 />
				<@wp.show frame=6 />
				<@wp.show frame=7 />
			</div>
			<div class="col-lg-12">
				<@wp.show frame=8 />
			</div>
		</div>
	</div>
	<script type="application/javascript" src="<@wp.resourceURL />inail/js/bootstrap-italia.bundle.min.js"></script>
</body>
</html>