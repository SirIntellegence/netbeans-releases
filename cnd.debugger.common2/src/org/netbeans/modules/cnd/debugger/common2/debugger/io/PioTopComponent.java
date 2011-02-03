/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.modules.cnd.debugger.common2.debugger.io;

import org.netbeans.modules.terminal.api.IOTopComponent;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.openide.util.ImageUtilities;
import org.netbeans.api.settings.ConvertAsProperties;
import org.netbeans.modules.terminal.api.TerminalContainer;
import org.openide.windows.IOContainer;
import org.openide.windows.Mode;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(dtd = "-//org.netbeans.modules.cnd.debugger.common2.debugger.io//Pio//EN",
autostore = false)
public final class PioTopComponent extends TopComponent implements IOTopComponent {

    private static PioTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "org/netbeans/modules/cnd/debugger/common2/icons/process_io.png"; // NOI18N
    private static final String PREFERRED_ID = "PioTopComponent"; // NOI18N

    public PioTopComponent() {
	initComponents();
	setName(NbBundle.getMessage(PioTopComponent.class, "CTL_PioTopComponent"));
	setToolTipText(NbBundle.getMessage(PioTopComponent.class, "HINT_PioTopComponent"));
	setIcon(ImageUtilities.loadImage(ICON_PATH, true));
	initComponents2();

    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
        // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
        private void initComponents() {

                setLayout(new java.awt.BorderLayout());
        }// </editor-fold>//GEN-END:initComponents

        // Variables declaration - do not modify//GEN-BEGIN:variables
        // End of variables declaration//GEN-END:variables
    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    private static synchronized PioTopComponent getDefault() {
	if (instance == null) {
            assert SwingUtilities.isEventDispatchThread();
	    instance = new PioTopComponent();
	}
	return instance;
    }

    /**
     * Obtain the PioTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized PioTopComponent findInstance() {
	TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
	if (win == null) {
	    Logger.getLogger(PioTopComponent.class.getName()).warning(
		    "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system."); // NOI18N
	    return getDefault();
	}
	if (win instanceof PioTopComponent) {
	    return (PioTopComponent) win;
	}
	Logger.getLogger(PioTopComponent.class.getName()).warning(
		"There seem to be multiple components with the '" + PREFERRED_ID // NOI18N
		+ "' ID. That is a potential source of errors and unexpected behavior."); // NOI18N
	return getDefault();
    }

    @Override
    public int getPersistenceType() {
	return TopComponent.PERSISTENCE_ALWAYS;
    }

    // interface TopComponent
    @Override
    public void open() {
	if (isOpened() && isShowing())
	    return;

	// Workaround per http://wiki.netbeans.org/DevFaqWindowsOpenInMode
	WindowManager wm = WindowManager.getDefault();
	Mode mode = wm.findMode(this);        // mode to which we belong
	if (mode == null) {
	    // if not in any mode, dock us into the default mode
	    mode = wm.findMode("output"); // NOI18N
	}
	mode.dockInto(this);

	super.open();
    }

    @Override
    public void componentOpened() {
	// TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
	// TODO add custom code on component closing
    }

    @Override
    protected void componentActivated() {
        super.componentActivated();
        tc.componentActivated();
    }

    @Override
    protected void componentDeactivated() {
        super.componentDeactivated();
        tc.componentDeactivated();
    }

    void writeProperties(java.util.Properties p) {
	// better to version settings since initial version as advocated at
	// http://wiki.apidesign.org/wiki/PropertyFiles
	p.setProperty("version", "1.0"); // NOI18N
	// TODO store your settings
    }

    Object readProperties(java.util.Properties p) {
	if (instance == null) {
	    instance = this;
	}
	instance.readPropertiesImpl(p);
	return instance;
    }

    private void readPropertiesImpl(java.util.Properties p) {
	String version = p.getProperty("version");
	// TODO read your settings according to their version
    }

    @Override
    protected String preferredID() {
	return PREFERRED_ID;
    }

    //
    // Implementation of IOTopComponent
    //
    private TerminalContainer tc;

    public IOContainer ioContainer() {
        return tc.ioContainer();
    }

    public TopComponent topComponent() {
        return this;
    }

    private void initComponents2() {
        tc = TerminalContainer.createMuxable(this, getName());
        add(tc);
    }
}
