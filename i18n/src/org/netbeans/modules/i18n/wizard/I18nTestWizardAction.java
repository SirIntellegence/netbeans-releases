/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */


package org.netbeans.modules.i18n.wizard;


import java.awt.Dialog;
import java.awt.Dimension;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.TreeMap;
import javax.swing.SwingUtilities;

import org.netbeans.modules.i18n.I18nUtil;

import org.openide.loaders.DataObject;
import org.openide.TopManager;
import org.openide.util.actions.CallableSystemAction;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.WizardDescriptor;


/**
 * Action which runs i18n test wizard.
 *
 * @author  Peter Zavadsky
 */
public class I18nTestWizardAction extends CallableSystemAction {

    /** Generated serial version UID. */
    static final long serialVersionUID = -3265587506739081248L;

    
    /** Actually performs action. Implements superclass abstract method. */
    public void performAction() {
        
        ArrayList panels = new ArrayList(3);
        panels.add(new SourceWizardPanel.Panel(true));
        panels.add(new ResourceWizardPanel.Panel(true));
        panels.add(new TestStringWizardPanel.Panel());
        
        WizardDescriptor wizardDesc = new I18nWizardDescriptor(
            new WizardDescriptor.ArrayIterator((WizardDescriptor.Panel[])panels.toArray(new WizardDescriptor.Panel[panels.size()])),
            new TreeMap(new SourceData.DataObjectComparator())
        );

        // Init properties.
        wizardDesc.putProperty("WizardPanel_autoWizardStyle", Boolean.TRUE); // NOI18N
        wizardDesc.putProperty("WizardPanel_contentDisplayed", Boolean.TRUE); // NOI18N
        wizardDesc.putProperty("WizardPanel_contentNumbered", Boolean.TRUE); // NOI18N

        ArrayList contents = new ArrayList(3);
        contents.add(NbBundle.getBundle(getClass()).getString("TXT_SelectTestSources"));
        contents.add(NbBundle.getBundle(getClass()).getString("TXT_SelectTestResources"));
        contents.add(NbBundle.getBundle(getClass()).getString("TXT_FoundMissingResources"));
        
        wizardDesc.putProperty("WizardPanel_contentData", (String[])contents.toArray(new String[contents.size()])); // NOI18N
        
        wizardDesc.setTitle(NbBundle.getBundle(getClass()).getString("LBL_TestWizardTitle"));
        wizardDesc.setTitleFormat(new MessageFormat("{0} ({1})")); // NOI18N

        wizardDesc.setModal(false);
        
        Dialog dialog = TopManager.getDefault().createDialog(wizardDesc);
        
        dialog.show();
    }

    /** Gets localized name of action. Overrides superclass method. */
    public String getName() {
        return NbBundle.getBundle(getClass()).getString("LBL_TestWizardActionName");
    }

    /** Gets the action's icon location.
     * @return the action's icon location */
    protected String iconResource () {
        return "/org/netbeans/modules/i18n/i18nAction.gif"; // NOI18N
    }
    
    /** Gets the action's help context. Implemenst superclass abstract method. */
    public HelpCtx getHelpCtx() {
        return new HelpCtx(I18nWizardAction.class);
    }
}
