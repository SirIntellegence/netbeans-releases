/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.jellytools.actions;

import org.netbeans.jellytools.Bundle;

/** AddLocaleAction class 
 * @author <a href="mailto:adam.sotona@sun.com">Adam Sotona</a> */
public class AddLocaleAction extends ActionNoBlock {

    private static final String addPopup = Bundle.getStringTrimmed("org.netbeans.modules.properties.Bundle", "CTL_AddLocale");

    /** creates new AddLocaleAction instance */    
    public AddLocaleAction() {
        super(null, addPopup);
    }
}