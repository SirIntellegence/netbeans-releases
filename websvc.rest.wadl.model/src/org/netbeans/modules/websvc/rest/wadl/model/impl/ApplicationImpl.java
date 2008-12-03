//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-558 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.11.07 at 12:36:44 PM PST 
//


package org.netbeans.modules.websvc.rest.wadl.model.impl;

import java.util.Collection;
import java.util.Map;
import org.netbeans.modules.websvc.rest.wadl.model.*;
import org.netbeans.modules.websvc.rest.wadl.model.visitor.WadlVisitor;
import org.w3c.dom.Element;

public class ApplicationImpl extends NamedImpl implements Application {
    
    public static final String TNS = "tns"; //NOI18N
    
    /** Creates a new instance of ApplicationImpl */
    public ApplicationImpl(WadlModel model, Element e) {
        super(model, e);
    }
    
    public ApplicationImpl(WadlModel model){
        this(model, createNewElement(WadlQNames.APPLICATION.getQName(), model));
    }
    
    public String getTargetNamespace() {
        return getAttribute(WadlAttribute.TARGET_NAMESPACE);
    }

    public void setTargetNamespace(String uri) {
        String currentTargetNamespace = getTargetNamespace();
        setAttribute(TARGET_NAMESPACE_PROPERTY, WadlAttribute.TARGET_NAMESPACE, uri);
        ensureValueNamespaceDeclared(uri, currentTargetNamespace, TNS);
    }

    public Collection<Grammars> getGrammars() {
        return getChildren(Grammars.class);
    }

    public void addGrammars(Grammars grammars) {
        addAfter(GRAMMARS_PROPERTY, grammars, TypeCollection.FOR_GRAMMARS.types());
    }

    public void removeGrammars(Grammars grammars) {
        removeChild(GRAMMARS_PROPERTY, grammars);
    }

    public Collection<Resources> getResources() {
        return getChildren(Resources.class);
    }
    public void addResources(Resources resources) {
        addAfter(GRAMMARS_PROPERTY, resources, TypeCollection.FOR_RESOURCES.types());
    }

    public void removeResources(Resources resources) {
        removeChild(GRAMMARS_PROPERTY, resources);
    }
    
    public Collection<ResourceType> getResourceType() {
        return getChildren(ResourceType.class);
    }

    public void addResourceType(ResourceType rType) {
        addAfter(RESOURCE_TYPE_PROPERTY, rType, TypeCollection.FOR_RESOURCE_TYPE.types());
    }

    public void removeResourceType(ResourceType rType) {
        removeChild(RESOURCE_TYPE_PROPERTY, rType);
    }
    
    public Collection<Method> getMethod() {
        return getChildren(Method.class);
    }

    public void addMethod(Method method) {
        addAfter(METHOD_PROPERTY, method, TypeCollection.FOR_METHOD.types());
    }

    public void removeMethod(Method method) {
        removeChild(METHOD_PROPERTY, method);
    }
    
    public Collection<Representation> getRepresentation() {
        return getChildren(Representation.class);
    }

    public void addRepresentation(Representation rep) {
        addAfter(REPRESENTATION_PROPERTY, rep, TypeCollection.FOR_REPRESENTATION.types());
    }

    public void removeRepresentation(Representation rep) {
        removeChild(REPRESENTATION_PROPERTY, rep);
    }

    public Collection<Fault> getFault() {
        return getChildren(Fault.class);
    }

    public void addFault(Fault fault) {
        addAfter(FAULT_PROPERTY, fault, TypeCollection.FOR_FAULT.types());
    }

    public void removeFault(Fault fault) {
        removeChild(FAULT_PROPERTY, fault);
    }
    
    public void accept(WadlVisitor visitor) {
        visitor.visit(this);
    }

    public String getSchemaNamespacePrefix() {
        Map<String, String> ns = this.getPrefixes();
        for(Map.Entry e:ns.entrySet()) {
            if(e.getValue() != null && e.getValue().equals(WadlModel.XML_SCHEMA_NS))
                return e.getKey() != null ? (String)e.getKey() : "";
        }
        return "xsd";
    }

}
