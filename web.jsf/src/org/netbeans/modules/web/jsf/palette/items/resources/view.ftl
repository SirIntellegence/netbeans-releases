<#if comment>

  TEMPLATE DESCRIPTION:

  This is XHTML template for 'JSF ReadOnly Form From Entity' action. Templating
  is performed using FreeMaker (http://freemarker.org/) - see its documentation
  for full syntax. Variables available for templating are:

    managedBeanProperty - name of managed bean property choosen in UI (type: String)
    item - name of property used for dataTable iteration (type: String)
    comment - always set to "false" (type: boolean)
    entityDescriptors - list of beans describing individual entities. Bean has following properties:
        label - field label (type: String)
        name - field property name (type: String)
        dateTimeFormat - date/time/datetime formatting (type: String)
        blob - does field represents a large block of text? (type: boolean)
        relationshipOne - does field represent one to one or many to one relationship (type: boolean)
        relationshipMany - does field represent one to many relationship (type: boolean)
        id - field id name (type: String)
        required - is field optional and nullable or it is not? (type: boolean)

</#if>
<h:form>
    <h1><h:outputText value="View"/></h1>
    <h:panelGrid columns="2">
<#list entityDescriptors as entityDescriptor>
        <h:outputText value="${entityDescriptor.label}:"/>
<#if entityDescriptor.dateTimeFormat?? && entityDescriptor.dateTimeFormat != "">
        <h:outputText value="${r"#{"}${entityDescriptor.name}${r"}"}" title="${entityDescriptor.label}">
            <f:convertDateTime pattern="${entityDescriptor.dateTimeFormat}" />
        </h:outputText>
<#else>
        <h:outputText value="${r"#{"}${entityDescriptor.name}${r"}"}" title="${entityDescriptor.label}"/>
</#if>
</#list>
    </h:panelGrid>
</h:form>
