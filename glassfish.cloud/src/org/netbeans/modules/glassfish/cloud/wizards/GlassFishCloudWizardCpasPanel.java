/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.glassfish.cloud.wizards;

import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.HelpCtx;
import static org.openide.util.NbBundle.getMessage;
import org.openide.util.Utilities;

/**
 * GlassFish Cloud Wizard CPAS Panel.
 * <p>
 * Allows user to register a new GlassFish Cloud (CPAS).
 * <p/>
 * @author Tomas Kraus, Peter Benedikovic
 */
public class GlassFishCloudWizardCpasPanel extends GlassFishWizardPanel {

    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////

    /** CPAS host property name. */
    static final String PROPERTY_CPAS_HOST = "host";

    /** CPAS port property name. */
    static final String PROPERTY_CPAS_PORT = "port";

    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////

    /** Panel component containing CPAS attributes. */
    GlassFishCloudWizardCpasComponent component;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Constructs an instance of CPAS panel.
     */
    public GlassFishCloudWizardCpasPanel() {
        super();
    }

    ////////////////////////////////////////////////////////////////////////////
    // Implemented Interface Methods                                          //
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public void prepareValidation() {
        // Lock form before validation.
        getComponent().setCursor(Utilities.createProgressCursor(getComponent()));
        component.disableModifications();
    }

    /**
     * Get panel component containing CPAS attributes.
     * <p/>
     * @return Panel component containing CPAS attributes.
     */
    @Override
    public Component getComponent() {
        if (component == null) {
            component = new GlassFishCloudWizardCpasComponent();
            component.setChangeListener(this);
        }
        return component;
    }

    /**
     * Help for this panel.
     * <p/>
     * When the panel is active, this is used as the help for the wizard dialog.
     * <p/>
     * @return The help or <code>null</code> if no help is supplied.
     */
    @Override
    public HelpCtx getHelp() {
        return new HelpCtx(GlassFishCloudWizardCpasPanel.class.getName());
    }


    /**
     * Provides the wizard panel with the opportunity to update the settings
     * with its current customized state.
     * <p/>
     * Rather than updating its settings with every change in the GUI, it should
     * collect them, and then only save them when requested to by this method.
     * Also, the original settings passed to {@link #readSettings} should not be
     * modified (mutated). Rather, the object passed in here should be mutated
     * according to the collected changes, in case it is a copy. This method can
     * be called multiple times on one instance of wizard panel.
     * <p/>
     * The settings object is originally supplied to
     * {@link
     * WizardDescriptor#WizardDescriptor(WizardDescriptor.Iterator,Object)}.
     * <p/>
     * @param settings the object representing wizard panel state
     */
    @Override
    public void storeSettings(WizardDescriptor settings) {
        if (component != null) {
            settings.putProperty(PROPERTY_CPAS_HOST, component.getHost());
            settings.putProperty(PROPERTY_CPAS_PORT, component.getPortText());
        }
    }

    /**
     * Test whether the panel is finished and it is safe to proceed to the next
     * one.
     * <p/>
     * If the panel is valid, the "Next" (or "Finish") button will be
     * enabled.
     * <p/>
     * @retrn <code>true</code> if the user has entered satisfactory information
     *        or <code>false</code> otherwise.
     */
    @Override
    public boolean isValid() {
        if (component == null || wizardDescriptor == null || asynchError != null) {
            return false;
        }
        return component.valid();
    }

    /**
     * Is called in separate thread when Next of Finish buttons are clicked and
     * allows deeper check to find out that panel is in valid state and it is ok
     * to leave it.
     * <p/>
     * @throws WizardValidationException when validation fails
     */
    @Override
    public void validate() throws WizardValidationException {
        try {
            // This should not happen at this stage but let's make sure.
            if (component == null || wizardDescriptor == null) {
                throw new WizardValidationException((JComponent)getComponent(), 
                        getMessage(GlassFishCloudWizardCpasPanel.class,
                        Bundle.CLOUD_PANEL_VALIDATION_FAILED),
                        getMessage(GlassFishCloudWizardCpasPanel.class,
                        Bundle.CLOUD_PANEL_ERROR_COMPONENT_UNINITIALIZED));
            }
            // Form fields validation.
            String error = performValidation();
            if (error != null) {
                throw new WizardValidationException((JComponent)getComponent(), 
                        getMessage(GlassFishCloudWizardCpasPanel.class,
                        Bundle.CLOUD_PANEL_VALIDATION_FAILED),
                        error);
            }
        // Unlock form after validation.
        } finally {
            component.setCursor(null);
            component.enableModifications();
        }
    }

    /**
     * Invoked when the target of the listener has changed its state.
     * <p/>
     * @param e a ChangeEvent object
     */
    @Override
    public void stateChanged(ChangeEvent e) {
        super.stateChanged(e);
        Object source = e.getSource();
        if (source instanceof GlassFishWizardComponent.ValidationResult) {
            GlassFishWizardComponent.ValidationResult result
                    = (GlassFishWizardComponent.ValidationResult)source;
            setErrorMessage(result.getErrorMessage());
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Methods                                                                //
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Validate form elements and return error message when any error was found.
     * <p/>
     * Validate all panel component fields and return error message related to
     * first error found during validation.
     * <p/>
     * @return Error message or <code>null</code> when no error was found.
     */
    public String performValidation() {
        if (component == null || wizardDescriptor == null) {
            return null;
        }
        GlassFishWizardComponent.ValidationResult result
                = component.hostValid();
        if (!result.isValid()) {
            return result.getErrorMessage();
        }
        result = component.portValid();
        if (!result.isValid()) {
            return result.getErrorMessage();
        }
        return null;
    }

}
