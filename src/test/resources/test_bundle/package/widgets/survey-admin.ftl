<#assign wp=JspTaglibs["/aps-core"]>
<#if (Session.currentUser.username != "guest") >    
<script crossorigin src="https://unpkg.com/react@16/umd/react.development.js"></script>    
<script crossorigin src="https://unpkg.com/react-dom@16/umd/react-dom.development.js"></script>    
<link href="<@wp.resourceURL />inail/static/css/2.chunk.css" rel="stylesheet">    
<link href="<@wp.resourceURL />inail/static/css/main.chunk.css" rel="stylesheet">   
 <script src="<@wp.resourceURL />inail/static/js/runtime~main.js"></script>    
<script src="<@wp.resourceURL />inail/static/js/2.chunk.js"></script>    
<script src="<@wp.resourceURL />inail/static/js/main.chunk.js"></script>    
<en-survey-admin service-url="/survey" />
<#else>    You have to be logged in to fill the survey</#if>