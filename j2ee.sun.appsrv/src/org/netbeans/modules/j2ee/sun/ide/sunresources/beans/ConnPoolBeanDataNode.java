/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
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
package org.netbeans.modules.j2ee.sun.ide.sunresources.beans;

import java.beans.PropertyEditor;

import org.openide.util.Utilities;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.nodes.BeanNode;
import org.openide.nodes.PropertySupport;

import org.openide.filesystems.FileObject;

import org.netbeans.modules.j2ee.sun.ide.editors.NameValuePairsPropertyEditor;
import org.netbeans.modules.j2ee.sun.ide.sunresources.resourcesloader.SunResourceDataObject;
import org.netbeans.modules.j2ee.sun.dd.api.serverresources.Resources;

/** A node to represent this object.
 *
 * @author nityad
 */
public class ConnPoolBeanDataNode extends BaseResourceNode implements java.beans.PropertyChangeListener{
    ConnPoolBean resource = null;
    
    public ConnPoolBeanDataNode(SunResourceDataObject obj, ConnPoolBean key) {
        super(obj);
        setIconBaseWithExtension("org/netbeans/modules/j2ee/sun/ide/resources/ResNodeNodeIcon.gif"); //NOI18N
        setShortDescription (NbBundle.getMessage (ConnPoolBeanDataNode.class, "DSC_ConnPoolNode"));//NOI18N
        
        resource = key;
        key.addPropertyChangeListener(this);
        
        Class clazz = key.getClass();
        try{
            createProperties(key, Utilities.getBeanInfo(clazz));
        } catch (Exception e){
            e.printStackTrace();
        }
        
    }
    
   protected ConnPoolBeanDataNode getConnPoolBeanDataNode(){
        return this;
    }
    
    protected ConnPoolBean getConnPoolBean(){
        return resource;
    }
    
    public HelpCtx getHelpCtx() {
        return null; // new HelpCtx ("AS_Res_ConnectionPool");//NOI18N
    }
    
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        FileObject resFile = getConnPoolBeanDataNode().getDataObject().getPrimaryFile();
        ResourceUtils.saveNodeToXml(resFile, resource.getGraph());
    }
    
    public Resources getBeanGraph(){
        return resource.getGraph();
    }
    
    protected void createProperties(Object bean, java.beans.BeanInfo info) {
        BeanNode.Descriptor d = BeanNode.computeProperties(bean, info);
        Node.Property p = new PropertySupport.ReadWrite(
        "extraParams", ConnPoolBeanDataNode.class, //NOI18N
        NbBundle.getMessage(ConnPoolBeanDataNode.class,"LBL_ExtParams"), //NOI18N
        NbBundle.getMessage(ConnPoolBeanDataNode.class,"DSC_ExtParams") //NOI18N
        ) {
            public Object getValue() {
                return resource.getExtraParams();
            }
            
            public void setValue(Object val){
                if (val instanceof Object[])
                    resource.setExtraParams((Object[])val);
            }
            
            public PropertyEditor getPropertyEditor(){
                return new NameValuePairsPropertyEditor(resource.getExtraParams());
            }
        };
        
        Sheet sets = getSheet();
        Sheet.Set pset = Sheet.createPropertiesSet();
        pset.put(d.property);
        pset.put(p);
//        pset.setValue("helpID", "AS_Res_ConnectionPool_Props"); //NOI18N
        sets.put(pset);
     }
    
    
    /* Example of adding Executor / Debugger / Arguments to node:
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set set = sheet.get(ExecSupport.PROP_EXECUTION);
        if (set == null) {
            set = new Sheet.Set();
            set.setName(ExecSupport.PROP_EXECUTION);
            set.setDisplayName(NbBundle.getMessage(SunResourceDataNode.class, "LBL_DataNode_exec_sheet"));
            set.setShortDescription(NbBundle.getMessage(SunResourceDataNode.class, "HINT_DataNode_exec_sheet"));
        }
        ((ExecSupport)getCookie(ExecSupport.class)).addProperties(set);
        // Maybe:
        ((CompilerSupport)getCookie(CompilerSupport.class)).addProperties(set);
        sheet.put(set);
        return sheet;
    }
     */
    
    // Don't use getDefaultAction(); just make that first in the data loader's getActions list
    
}
