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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

/*
 * ProvidesNode.java
 *
 * Created on November 2, 2006, 8:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.netbeans.modules.compapp.casaeditor.nodes;

import java.awt.Image;
import org.netbeans.modules.compapp.casaeditor.model.casa.CasaComponent;
import org.netbeans.modules.compapp.casaeditor.model.casa.CasaEndpointRef;
import org.netbeans.modules.compapp.casaeditor.model.casa.CasaProvides;
import org.netbeans.modules.compapp.casaeditor.model.jbi.impl.JBIAttributes;
import org.netbeans.modules.compapp.casaeditor.properties.PropertyUtils;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Josh Sandusky
 */
public class ProvidesNode extends CasaNode {
    
    private static final Image ICON = Utilities.loadImage(
            "org/netbeans/modules/compapp/casaeditor/nodes/resources/ProvidesNode.png");
    
    public ProvidesNode(CasaComponent component, Children children, Lookup lookup) {
        super(component, children, lookup);
    }
    
    public ProvidesNode(CasaComponent component, Lookup lookup) {
        super(component, Children.LEAF, lookup);
    }
    
    
    protected void setupPropertySheet(Sheet sheet) {
        final CasaProvides provides = (CasaProvides) getData();
        if (provides == null) {
            return;
        }
        
        Sheet.Set mainPropertySet =
                getPropertySet(sheet, PropertyUtils.PropertiesGroups.MAIN_SET);
        
        PropertyUtils.installEndpointInterfaceQNameProperty(
                mainPropertySet, this, provides,
                JBIAttributes.INTERFACE_NAME.getName(), 
                "interfaceQName",
                NbBundle.getMessage(getClass(), "PROP_InterfaceName"),
                NbBundle.getMessage(getClass(), "PROP_InterfaceName"));
        
        PropertyUtils.installEndpointServiceQNameProperty(
                mainPropertySet, this, provides,
                JBIAttributes.SERVICE_NAME.getName(), 
                "serviceQName",
                NbBundle.getMessage(getClass(), "PROP_ServiceName"),
                NbBundle.getMessage(getClass(), "PROP_ServiceName"));
        
        PropertyUtils.installEndpointEndpointNameProperty(
                mainPropertySet, this, provides,
                JBIAttributes.ENDPOINT_NAME.getName(), 
                "endpointName",
                NbBundle.getMessage(getClass(), "PROP_EndpointName"),
                NbBundle.getMessage(getClass(), "PROP_EndpointName"));
    }

    public String getName() {
        CasaProvides provides = (CasaProvides) getData();
        if (provides != null) {
            try {
                return provides.getEndpoint().get().getEndpointName();
            } catch (Throwable t) {
                // getName MUST recover gracefully.
                return getBadName();
            }
        }
        return super.getName();
    }
    
    public Image getIcon(int type) {
        return ICON;
    }
    
    public Image getOpenedIcon(int type) {
        return ICON;
    }
    
    public boolean isEditable(String propertyType) {
        CasaEndpointRef endpoint = (CasaEndpointRef) getData();
        if (endpoint != null) {
            return getModel().isEditable(endpoint, propertyType);
        }
        return false;
    }
    
    public boolean isDeletable() {
        CasaEndpointRef endpoint = (CasaEndpointRef) getData();
        if (endpoint != null) {
            return getModel().isDeletable(endpoint);
        }
        return false;
    }
}
