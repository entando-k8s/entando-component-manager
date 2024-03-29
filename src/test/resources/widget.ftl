<#ftl output_format="undefined">
<#assign wp=JspTaglibs["/aps-core"]>

<@wp.currentPage param="code" var="page_code" />
<@wp.info key="startLang" var="info_startLang" />
<@wp.info key="systemParam" paramName="applicationBaseURL" var="systemParam_applicationBaseURL" />

<@wp.currentWidget param="config" configParam="paramA" var="widget_paramA" />
<#assign widget_paramA>${(widget_paramA)!""}</#assign>
<@wp.currentWidget param="config" configParam="paramB" var="widget_paramB" />
<#assign widget_paramB>${(widget_paramB)!""}</#assign>

<#assign PLACEHOLDER_FOR_API_URL_EXTRACTION></#assign>

<script>
window.entando = {
  ...(window.entando || {}),
};
window.entando.widgets = {
  ...(window.entando.widgets || {}),
};
window.entando.widgets["my-name"]={
  "basePath": "<@wp.resourceURL />bundles/my-component-77b2b10e/widgets/any-path-77b2b10e"
}
</script>

<script src="<@wp.resourceURL />bundles/my-component-77b2b10e/widgets/my-code-77b2b10e/static/js/main.js"></script>
<script src="<@wp.resourceURL />bundles/my-component-77b2b10e/widgets/my-code-77b2b10e/static/js/runtime.js"></script>
<link href="<@wp.resourceURL />bundles/my-component-77b2b10e/widgets/my-code-77b2b10e/static/css/style.css" rel="stylesheet">

<#assign mfeConfig>{"systemParams":{"api":{"int-api":{"url":"${apiClaim_int_DASH_api}"},"ext-api":{"url":"${apiClaim_ext_DASH_api}"}}},"contextParams":{"info_startLang":"${info_startLang}","page_code":"${page_code}","systemParam_applicationBaseURL":"${systemParam_applicationBaseURL}"},"params":{"paramA":"${widget_paramA}","paramB":"${widget_paramB}"}}</#assign>

<my-code config="<#outputformat 'HTML'>${mfeConfig}</#outputformat>"/>