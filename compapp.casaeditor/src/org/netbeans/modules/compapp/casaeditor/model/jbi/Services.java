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
package org.netbeans.modules.compapp.casaeditor.model.jbi;

import java.util.List;

/**
 *
 * @author jqian
 */
public interface Services extends JBIComponent {
    public static final String BINDING_COMPONENT_PROPERTY = "binding-component";  // NOI18N //JBIDescriptorAttributes.BINDING_COMPONENT.getName();
    public static final String PROVIDES_PROPERTY = "provides";                    // NOI18N
    public static final String CONSUMES_PROPERTY = "consumes";                    // NOI18N
    
    Boolean getBindingComponent();
    void setBindingComponent(Boolean bindingComponent);
    
    List<Provides> getProvidesList();
    void removeProvides(Provides provides);
    void addProvides(int index, Provides provides);
    
    List<Consumes> getConsumesList();
    void removeConsumes(Consumes consumes);    
    void addConsumes(int index, Consumes consumes);    
}
