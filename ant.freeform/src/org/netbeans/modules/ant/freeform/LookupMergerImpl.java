/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.ant.freeform;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.netbeans.modules.ant.freeform.spi.LookupMerger;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.openide.util.Lookup;

/**
 * Merges PrivilegedTemplates - all unique templates are returned.
 * Order is undefined - depends on the lookup.
 *
 * @author David Konecny
 */
public class LookupMergerImpl implements LookupMerger {

    public LookupMergerImpl() {}
    
    public Class[] getMergeableClasses() {
        return new Class[] {
            PrivilegedTemplates.class,
            ActionProvider.class,
        };
    }
    
    public Object merge(Lookup lookup, Class clazz) throws IllegalArgumentException {
        if (clazz == PrivilegedTemplates.class) {
            return new PrivilegedTemplatesImpl(lookup);
        } else if (clazz == ActionProvider.class) {
            return new ActionProviderImpl(lookup);
        } else {
            throw new IllegalArgumentException("merging of " + clazz + " is not supported"); // NOI18N
        }
    }
    
    private static class PrivilegedTemplatesImpl implements PrivilegedTemplates {
        
        private Lookup lkp;
        
        public PrivilegedTemplatesImpl(Lookup lkp) {
            this.lkp = lkp;
        }
        
        public String[] getPrivilegedTemplates() {
            LinkedHashSet templates = new LinkedHashSet();
            Iterator it = lkp.lookup(new Lookup.Template(PrivilegedTemplates.class)).allInstances().iterator();
            while (it.hasNext()) {
                PrivilegedTemplates pt = (PrivilegedTemplates)it.next();
                templates.addAll(Arrays.asList(pt.getPrivilegedTemplates()));
            }
            return (String[])templates.toArray(new String[templates.size()]);
        }
        
    }

    /**
     * Permits any nature to add actions to the project.
     */
    private static class ActionProviderImpl implements ActionProvider {
        
        private final Lookup lkp;
        
        public ActionProviderImpl(Lookup lkp) {
            this.lkp = lkp;
        }
        
        private Iterator/*<ActionProvider>*/ delegates() {
            Collection/*<ActionProvider>*/ all = lkp.lookup(new Lookup.Template(ActionProvider.class)).allInstances();
            assert !all.contains(this) : all;
            return all.iterator();
        }
        
        // XXX delegate directly to single impl if only one

        public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
            boolean supported = false;
            Iterator/*<ActionProvider>*/ it = delegates();
            while (it.hasNext()) {
                ActionProvider ap = (ActionProvider) it.next();
                if (Arrays.asList(ap.getSupportedActions()).contains(command)) {
                    return ap.isActionEnabled(command, context);
                }
            }
            // Not supported by anyone.
            throw new IllegalArgumentException(command);
        }

        public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
            Iterator/*<ActionProvider>*/ it = delegates();
            while (it.hasNext()) {
                ActionProvider ap = (ActionProvider) it.next();
                if (Arrays.asList(ap.getSupportedActions()).contains(command)) {
                    ap.invokeAction(command, context);
                    return;
                }
            }
            throw new IllegalArgumentException(command);
        }

        public String[] getSupportedActions() {
            Set/*<String>*/ actions = new HashSet();
            Iterator/*<ActionProvider>*/ it = delegates();
            while (it.hasNext()) {
                ActionProvider ap = (ActionProvider) it.next();
                actions.addAll(Arrays.asList(ap.getSupportedActions()));
            }
            return (String[]) actions.toArray(new String[actions.size()]);
        }
        
    }
    
}
