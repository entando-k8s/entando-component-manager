<#ftl output_format="undefined">
<#assign wp=JspTaglibs["/aps-core"]>

<@wp.currentPage param="code" var="page_code" />
<@wp.info key="startLang" var="info_startLang" />
<@wp.info key="systemParam" paramName="applicationBaseURL" var="systemParam_applicationBaseURL" />

<@wp.currentWidget param="config" configParam="url" var="widget_url" />
<@wp.currentWidget param="config" configParam="title" var="widget_title" />

<script src="<@wp.resourceURL />bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/js-res-1.js"></script>
<script src="<@wp.resourceURL />bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/static/js/js-res-2.js"></script>

<link href="<@wp.resourceURL />bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0/assets/css-res.css" rel="stylesheet">

<script>
window.entando = {
  ...(window.entando || {}),
};
window.entando.widgets = {
  ...(window.entando.widgets || {}),
};
window.entando.widgets["todomvc_widget"]={
  "basePath": "<@wp.resourceURL />bundles/something-ece8f6f0/widgets/my_widget_descriptor_v5-ece8f6f0"
}
</script>

<#assign mfeConfig>{"systemParams":{"api":{"int-api":{"url":"my-path3"},"ext-api":{"url":"my-path"}}},"contextParams":{"info_startLang":"${info_startLang}","page_code":"${page_code}"},"params":{"title":"${widget_title}","url":"${widget_url}"}}</#assign>

<my-widget config="<#outputformat 'HTML'>${mfeConfig}</#outputformat>"/>