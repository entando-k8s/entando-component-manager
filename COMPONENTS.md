# Components
Digital exchange allows registration of several components.

## Bundle
In order to install components, Digital Exchange will require a bundle with a descriptor file inside and component descriptors.

## Descriptor File
The descriptor file will aggregate all components inside and has the following structure. The extension is YAML.

```yaml
code: inail_bundle # The bundle ID
description: This is the inail bundle # The description of the bundle

components: # All components will be here

  # Optional. Use if the component requires a deployment
  plugins:
    - folder/you/want/my_plugin_descriptor.yaml
    - folder/you/want/another_plugin_descriptor.yaml

  # To create widgets you will need to add references to the descriptor files
  widgets:
    - widgets/my_widget_descriptor.yaml
    - widgets/another_widget_descriptor.yaml

  # To create Page Models you will need to add references to the descriptor files
  pageModels:
    - pagemodels/my_page_model_descriptor.yaml
    - pagemodels/another_page_model_descriptor.yaml

  # To create Content Types you will need to add references to the descriptor files
  contentTypes:
    - contenttypes/my_content_type_descriptor.yaml

  # To create Content Models you will need to add references to the descriptor files
  contentModels:
    - contentmodels/my_content_model_descriptor.yaml
    - contentmodels/another_content_model_descriptor.yaml

  # Labels on the system
  labels:
    - key: HELLO # This is the label identifier
      titles: # The titles on the label
        it: Mio Titolo # The title in Italian
        en: My Title # The title in English
```

## Plugin Descriptor
A plugin should be described using the EntandoPlugin CustomResource format.
```yaml
kind: "EntandoPlugin"
apiVersion: "entando.org/v1alpha1"
metadata:
  name: "my-plugin"
spec:
  image: "my-image"
  replicas: 1
  dbms: "postgresql"
  roles:
    - name: "task-list"
      code: "task-list"
    - name: "task-get"
      code: "task-get"
    - name: "connection-list"
      code: "connection-list"
    - name: "connection-get"
      code: "connection-get"
    - name: "connection-create"
      code: "connection-create"
    - name: "connection-delete"
      code: "connection-delete"
    - name: "connection-edit"
      code: "connection-edit"
  permissions: []
  parameters: {}
  ingressPath: "/myPath"
  healthCheckPath: "/actuator/health"
  securityLevel: "strict"
  connectionConfigNames: []
```



## Widget Descriptor
```yaml
code: another_todomvc_widget # The Widget identification

titles: # Widget's Titles
  en: TODO MVC Widget # Title in English
  it: TODO MVC Widget # Title in Italian

group: free # The owner group of the widget

# Optional. The UI Path, the widget.ftl file will have the customUi content
customUiPath: widget.ftl

# Optional. The Custom UI
customUi: >-
    <h1>My custom widget UI</h1>
```

## Page Model Descriptor
```yaml
code: todomvc_page_model # The Page Model identification
description: TODO MVC basic page model # The Page Model description

titles: # Page Model's Titles
  en: TODO MVC PageModel # Title in English
  it: TODO MVC PageModel # Title in Italian

group: free # The owner group of the widget

# The Page Model configuration
configuration:
  frames: # All frames
    - pos: 0 # Frame position
      description: Header # Frame description
      sketch: # Frame sketch configuration
        x1: 0
        y1: 0
        x2: 11
        y2: 0

    # A simplified way to define a Frame
    - pos: 1
      description: Breadcrumb
      sketch: { x1: 0, y1: 1, x2: 11, y2: 1 }

# Optional. Define the Page Model Template in a separate file or inside descriptor file with `template`
templatePath: page.ftl

# Optional. Define the Page Model Template this way or in a separate file with `templatePath`
template: >-
  <#assign wp=JspTaglibs[\"/aps-core\"]>
  <!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">
  <html>
      <head>
          <title><@wp.currentPage param=\"title\" /></title>
      </head>
      <body>
          <h1><@wp.currentPage param=\"title\" /></h1>
          <a href=\"<@wp.url page=\"homepage\"/>\">Home</a><br>
          <div>
              <h1>Bundle 1 Page Model</h1>
              <@wp.show frame=0 />
          </div>
      </body>
  </html>
```

### Content Type Descriptor
For more details on the properties, refer to the Content Type documentation.

```yaml
code: CNG
name: Demo
status: 0

attributes:
  - code: title
    type: Text
    name: Title
    roles:
      - code: jacms:title
        descr: The main title of a Content
    disablingCodes: []
    mandatory: true
    listFilter: false
    indexable: false

    enumeratorStaticItems: string
    enumeratorStaticItemsSeparator: string
    enumeratorExtractorBean: string

    validationRules:
      minLength: 0
      maxLength: 100
      regex: string
      rangeStartString: string
      rangeEndString: string
      rangeStartStringAttribute: string
      rangeEndStringAttribute: string
      equalString: string
      equalStringAttribute: string
      rangeStartDate: string
      rangeEndDate: string
      rangeStartDateAttribute: string
      rangeEndDateAttribute: string
      equalDate: string
      equalDateAttribute: string
      rangeStartNumber: 0
      rangeStartNumberAttribute: string
      rangeEndNumber: 0
      rangeEndNumberAttribute: string
      equalNumber: 0
      equalNumberAttribute:
      ognlValidation:
        applyOnlyToFilledAttr: false
        errorMessage: Something
        keyForErrorMessage: some
        keyForHelpMessage: thing
        ognlExpression: string
```


### Content Model Descriptor
```yaml
id: 8880003
contentType: CNG
description: Demo Content Model

# Optional. Define the Content Model Shape in a separate file or inside descriptor file with `contentShape`
contentShapePath:

# Optional. Define the Content Model Shape this way or in a separate file with `contentShapePath`
contentShape: >-
  <article>
  	<h1>$content.Title.text</h1>
  	<h2>Demo content model</h2>
  	#if ( $content.MainBody.text != "" )
  	$content.MainBody.text
  	#end
  </article>
```

### Static Files
In order to upload static files, you will need to create a folder called `resources/`, all files inside this folder will be uploaded in the same folder strucure that is inside.

```
resources/
├ css/
│ └ styles.css
├ js/
│ └ script.css
├ images/
│ ├ favicon.ico
│ └ logo.png
└ page.html
```

On the structure above-mentioned, the resulting files on the entando architecture will be:

```
yourbundleid/
├ css/
│ └ styles.css
├ js/
│ └ script.js
├ images/
│ ├ favicon.ico
│ └ logo.png
└ page.html
```

Note: `yourbundleid` is the `code` property inside `descriptor.yaml`

So if you need to use the the file on a widget or page model use this FTL tag `<@wp.resourceURL />`:

```html
<img src="<@wp.resourceURL />yourbundleid/images/logo.png">
<link rel="stylesheet" href="<@wp.resourceURL />yourbundleid/css/styles.css">
<link rel="shortcut icon" href="<@wp.resourceURL />yourbundleid/images/favicon.ico" type="image/x-icon"/>
<script type="application/javascript" src="<@wp.resourceURL />yourbundleid/js/script.js"></script>
```
