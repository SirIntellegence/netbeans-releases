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

package org.netbeans.modules.cnd.discovery.wizard;

import java.awt.Component;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 *
 * @author Alexander Simon
 */
public class SelectObjectFilesWizard implements WizardDescriptor.Panel, ChangeListener {
    
    private WizardDescriptor wizardDescriptor;
    private SelectObjectFilesPanel component;
    private String name;
    private boolean initialized = false;
    
    /** Create the wizard panel descriptor. */
    public SelectObjectFilesWizard() {
	name = NbBundle.getMessage(SelectObjectFilesPanel.class, "SelectObjectRootName"); // NOI18N
    }
    
    public Component getComponent() {
        if (component == null) {
            component = new SelectObjectFilesPanel(this);
	    component.setName(name);
        }
        return component;
    }

    public WizardDescriptor getWizardDescriptor() {
	return wizardDescriptor;
    } 
    
    public HelpCtx getHelp() {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
        // If you have context help:
        // return new HelpCtx(SampleWizardPanel1.class);
    }
    
    public boolean isValid() {
	boolean valid = ((SelectObjectFilesPanel)getComponent()).valid( wizardDescriptor);
	if (valid) {
	    wizardDescriptor.putProperty("WizardPanel_errorMessage", ""); // NOI18N
        }
	return valid;
    }
    
    private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1);
    public final void addChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }
    public final void removeChangeListener(ChangeListener l) {
        synchronized (listeners) {
            listeners.remove(l);
        }
    }
    
    protected final void fireChangeEvent() {
        Iterator<ChangeListener> it;
        synchronized (listeners) {
            it = new HashSet<ChangeListener>(listeners).iterator();
        }
        ChangeEvent ev = new ChangeEvent(this);
        while (it.hasNext()) {
            it.next().stateChanged(ev);
        }
    }

    public void stateChanged(ChangeEvent e) {
	fireChangeEvent();
    }
    
    // You can use a settings object to keep track of state. Normally the
    // settings object will be the WizardDescriptor, so you can use
    // WizardDescriptor.getProperty & putProperty to store information entered
    // by the user.
    public void readSettings(Object settings) {
        if (initialized)
            return;
        wizardDescriptor = (WizardDescriptor)settings;        
        component.read(wizardDescriptor);
        initialized = true;
    }
    
    public void storeSettings(Object settings) {
        WizardDescriptor d = (WizardDescriptor)settings;
        component.store(d);
    }
    
}

