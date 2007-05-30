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
package org.netbeans.modules.sun.manager.jbi.editors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.management.openmbean.CompositeData;

/**
 *
 * @author jqian
 */
public class EnvironmentVariablesEditor extends SimpleTabularDataEditor {

    public EnvironmentVariablesEditor() {
    }

    @Override
    public java.awt.Component getCustomEditor() {
        return new EnvironmentVariablesCustomEditor(this);
    }
    
    @Override
    protected String getStringForRowData(CompositeData rowData) {
        Collection rowValues = rowData.values();
        
        List<String> visibleRowValues = new ArrayList<String>();
        visibleRowValues.addAll(rowValues);
        
        String type = (String) visibleRowValues.get(
                EnvironmentVariablesCustomEditor.TYPE_COLUMN);
        
        if (type.equals(EnvironmentVariablesCustomEditor.PASSWORD_TYPE)) {
            String password = (String) visibleRowValues.get(
                    EnvironmentVariablesCustomEditor.VALUE_COLUMN);
            password = password.replaceAll(".", "*");
            visibleRowValues.set(
                    EnvironmentVariablesCustomEditor.VALUE_COLUMN, password);
        }
        
        return visibleRowValues.toString();
    }
}
