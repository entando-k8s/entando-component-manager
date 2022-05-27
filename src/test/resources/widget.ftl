<#assign wp=JspTaglibs["/aps-core"]>

<@wp.info key="systemParam" paramName="applicationBaseURL" var="systemParam_applicationBaseURL" />

<script src="<@wp.resourceURL />/bundles/my-component-77b2b10e/widgets/my-code/static/js/main.js"></script>
<script src="<@wp.resourceURL />/bundles/my-component-77b2b10e/widgets/my-code/static/js/runtime.js"></script>

<link href="<@wp.resourceURL />/bundles/my-component-77b2b10e/widgets/my-code/static/css/style.css" rel="stylesheet">

<#assign mfeSystemConfig>{"systemParams":{"api":{"int-api":{"url":"${systemParam_applicationBaseURL}/service-id-1/path"},"ext-api":{"url":"${systemParam_applicationBaseURL}/service-id-2/path"}}}}</#assign>

<my-code config="${mfeSystemConfig}"/>
