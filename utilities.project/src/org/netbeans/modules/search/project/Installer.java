/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 2004 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.search.project;

import org.openide.modules.ModuleInstall;
import org.openide.util.actions.SystemAction;

/**
 *
 * @author  Marian Petras
 */
public class Installer extends ModuleInstall {
    
    public void restored() {
        ((ProjectsSearchAction) SystemAction.get(ProjectsSearchAction.class))
                .link();
    }
    
    public void uninstalled() {
        ((ProjectsSearchAction) SystemAction.get(ProjectsSearchAction.class))
                .unlink();
    }
    
}
