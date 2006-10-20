/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.modules.bpel.nodes;

import java.awt.Component;
import org.netbeans.modules.bpel.model.api.MessageExchangeReference;
import org.netbeans.modules.bpel.model.api.Receive;
import org.netbeans.modules.bpel.design.nodes.DiagramExtInfo;
import org.netbeans.modules.bpel.model.api.support.TBoolean;
import org.netbeans.modules.bpel.properties.Constants;
import org.netbeans.modules.bpel.design.nodes.NodeType;
import org.netbeans.modules.bpel.model.api.BpelEntity;
import org.netbeans.modules.bpel.model.api.CorrelationContainer;
import org.netbeans.modules.bpel.model.api.CreateInstanceActivity;
import org.netbeans.modules.bpel.model.api.NamedElement;
import org.netbeans.modules.bpel.model.api.OperationReference;
import org.netbeans.modules.bpel.model.api.PartnerLinkReference;
import org.netbeans.modules.bpel.model.api.PortTypeReference;
import org.netbeans.modules.bpel.model.api.VariableReference;
import org.netbeans.modules.bpel.model.api.events.ChangeEvent;
import org.netbeans.modules.bpel.properties.props.PropertyUtils;
import org.openide.nodes.Sheet;
import static org.netbeans.modules.bpel.properties.PropertyType.*;
import org.netbeans.modules.bpel.properties.Util;
import org.netbeans.modules.bpel.properties.editors.ReceiveCustomEditor;
import org.netbeans.modules.bpel.properties.editors.controls.CustomNodeEditor.EditingMode;
import org.netbeans.modules.bpel.nodes.actions.ActionType;
import org.netbeans.modules.bpel.properties.props.CustomEditorProperty;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author nk160297
 */
public class ReceiveNode extends DiagramBpelNode<Receive, DiagramExtInfo> {
    
    public ReceiveNode(Receive reference, Children children, Lookup lookup) {
        super(reference, children, lookup);
    }
    
    public ReceiveNode(Receive reference, Lookup lookup) {
        super(reference, lookup);
    }
    
    public NodeType getNodeType() {
        return NodeType.RECEIVE;
    }
    
    @Override
    protected boolean isEventRequreUpdate(ChangeEvent event) {
        if (super.isEventRequreUpdate(event)) {
            return true;
        }
        
        //CorrelationContainer
        BpelEntity entity = event.getParent();
        if (entity == null) {
            return false;
        }
        Object ref = getReference();
        return  ref != null && ref == entity.getParent()
        && entity.getElementType() == CorrelationContainer.class;
    }
    
    protected String getImplHtmlDisplayName() {
        Receive receive = getReference();
        if (receive == null) {
            return super.getImplHtmlDisplayName();
        }
        
        return Util.getGrayString(super.getImplHtmlDisplayName(), EMPTY_STRING);
    }
    
//    protected String getImplShortDescription() {
//        Receive receive = getReference();
//        if (receive == null) {
//            return super.getImplShortDescription();
//        }
//        
//        StringBuffer result = new StringBuffer();
//        result.append(getName());
//        result.append(receive.getVariable() == null ? EMPTY_STRING : VARIABLE_EQ+receive.getVariable().getRefString());
//        result.append(receive.getMessageExchange() == null ? EMPTY_STRING : MESSAGE_EXCHANGE_EQ+receive.getMessageExchange().getRefString());
//        result.append(receive.getPartnerLink() == null ? EMPTY_STRING : PARTNER_LINK_EQ+receive.getPartnerLink().getRefString());
//        result.append(receive.getOperation() == null ? EMPTY_STRING : OPERATION_EQ+receive.getOperation().getRefString());
//        TBoolean createInstance = receive.getCreateInstance();
//        if (createInstance != null
//                && !(createInstance.equals(TBoolean.INVALID))) {
//            result.append(CREATE_INSTANCE_EQ).append(createInstance.toString());
//        }
//        
//        return NbBundle.getMessage(ReceiveNode.class,
//                "LBL_RECEIVE_NODE_TOOLTIP", // NOI18N
//                result.toString()
//                );
//    }
    
    public String getHelpId() {
        return getNodeType().getHelpId();
    }
    
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        if (getReference() == null) {
            // The related object has been removed!
            return sheet;
        }
        //
        DiagramExtInfo diagramReference = getDiagramReference();
        //
        Sheet.Set mainPropertySet =
                getPropertySet(sheet, Constants.PropertiesGroups.MAIN_SET);
        //
        Node.Property property;
        //
        CustomEditorProperty customizer = new CustomEditorProperty(this);
        mainPropertySet.put(customizer);
        //
        PropertyUtils.registerAttributeProperty(this, mainPropertySet,
                NamedElement.NAME, NAME, "getName", "setName", null); // NOI18N
        //
        InstanceRef myInstanceRef = new InstanceRef() {
            public Object getReference() {
                return ReceiveNode.this;
            }
            public Object getAlternativeReference() {
                return null;
            }
        };
        //
        PropertyUtils.registerAttributeProperty(myInstanceRef, mainPropertySet,
                CreateInstanceActivity.CREATE_INSTANCE, CREATE_INSTANCE,
                "getCreateInstance", "setCreateInstance",   // NOI18N
                null); // NOI18N
        //

// Issue 85553 start.        
//        property = PropertyUtils.registerAttributeProperty(this, mainPropertySet,
//                MessageExchangeReference.MESSAGE_EXCHANGE, MESSAGE_EXCHANGE,
//                "getMessageExchange", "setMessageExchange",  // NOI18N
//                "removeMessageExchange"); // NOI18N
//        property.setValue("canEditAsText", Boolean.FALSE); // NOI18N
//        //
// Issue 85553 end.    
        
        Sheet.Set messagePropertySet =
                getPropertySet(sheet, Constants.PropertiesGroups.MESSAGE_SET);
        
        //
        property = PropertyUtils.registerAttributeProperty(this,
                messagePropertySet,
                PartnerLinkReference.PARTNER_LINK, PARTNER_LINK,
                "getPartnerLink", "setPartnerLink", null); // NOI18N
        property.setValue("suppressCustomEditor", Boolean.TRUE); // NOI18N
        property.setValue("canEditAsText", Boolean.FALSE); // NOI18N
        //
        property = PropertyUtils.registerAttributeProperty(this,
                messagePropertySet,
                PortTypeReference.PORT_TYPE, PORT_TYPE,
                "getPortType", "setPortType", "removePortType"); // NOI18N
        property.setValue("suppressCustomEditor", Boolean.TRUE); // NOI18N
        property.setValue("canEditAsText", Boolean.FALSE); // NOI18N
        //
        property = PropertyUtils.registerAttributeProperty(this,
                messagePropertySet,
                OperationReference.OPERATION, OPERATION,
                "getOperation", "setOperation", null); // NOI18N
        property.setValue("suppressCustomEditor", Boolean.TRUE); // NOI18N
        property.setValue("canEditAsText", Boolean.FALSE); // NOI18N
        //
        property = PropertyUtils.registerAttributeProperty(this,
                messagePropertySet,
                VariableReference.VARIABLE, INPUT,
                "getVariable", "setVariable", "removeVariable"); // NOI18N
        property.setValue("suppressCustomEditor", Boolean.TRUE); // NOI18N
        property.setValue("canEditAsText", Boolean.FALSE); // NOI18N
        
        return sheet;
    }
    
    public Boolean getCreateInstance() {
        Receive receive = getReference();
        if (receive != null) {
            TBoolean isCreateInstance = receive.getCreateInstance();
            if (TBoolean.YES.equals(isCreateInstance)) {
                return Boolean.TRUE;
            } 
        }
        //
        return Boolean.FALSE;
    }
    
    public void setCreateInstance(Boolean newValue) {
        Receive receive = getReference();
        if (receive != null) {
            if (Boolean.TRUE.equals(newValue)) {
                receive.setCreateInstance(TBoolean.YES);
            } else {
                receive.setCreateInstance(TBoolean.NO);
            }
        }
    }
    
    public Component getCustomizer() {
        return new ReceiveCustomEditor(this, EditingMode.EDIT_INSTANCE);
    }
    
    protected ActionType[] getActionsArray() {
        return new ActionType[] {
            ActionType.GO_TO_SOURCE,
//            ActionType.CYCLE_MEX, // Issue 85553
            ActionType.SEPARATOR,
            ActionType.SHOW_POPERTY_EDITOR,
            ActionType.SEPARATOR,
            ActionType.TOGGLE_BREAKPOINT,
            ActionType.SEPARATOR,
            ActionType.REMOVE,
            ActionType.SEPARATOR,
            ActionType.PROPERTIES
        };
    }
}
