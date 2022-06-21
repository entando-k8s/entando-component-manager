<#assign wp=JspTaglibs["/aps-core"]>

<@wp.info key="systemParam" paramName="applicationBaseURL" var="systemParam_applicationBaseURL" />

<script src="<@wp.resourceURL />bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/js-res-1.js"></script>
<script src="<@wp.resourceURL />bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/static/js/js-res-2.js"></script>

<link href="<@wp.resourceURL />bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/assets/css-res.css" rel="stylesheet">

<#assign mfeSystemConfig>{'systemParams':{'api':{'int-api':{'url':'${systemParam_applicationBaseURL}/entando/todomvcv3'},'ext-api':{'url':'${systemParam_applicationBaseURL}my-path'}}}}</#assign>

<my-widget config="${mfeSystemConfig}"/>
