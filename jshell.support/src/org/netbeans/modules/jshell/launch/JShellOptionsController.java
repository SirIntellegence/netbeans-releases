/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2016 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2016 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jshell.launch;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JComponent;
import org.netbeans.modules.options.java.api.JavaOptions;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author sdedic
 */
@OptionsPanelController.SubRegistration(
    location=JavaOptions.JAVA,
    displayName="#LBL_JavaShellOptions",
    keywords="#KW_JavaShell",
    keywordsCategory = JavaOptions.JAVA + "/JavaShell"
)
@NbBundle.Messages({
    "LBL_JavaShellOptions=Java Shell",
    "KW_JavaShell=JavaShell,Shell,JShell"
})
public class JShellOptionsController extends OptionsPanelController {
    private JShellOptionsPanel ui;
    private PropertyChangeSupport supp = new PropertyChangeSupport(this);
    private boolean changed;
    
    @Override
    public void update() {
        ui.load();
        changed = false;
    }

    @Override
    public void applyChanges() {
        ui.store();
        changed = false;
    }

    @Override
    public void cancel() {
        // no op.
    }

    @Override
    public boolean isValid() {
        return ui.valid();
    }

    @Override
    public boolean isChanged() {
        return ui.isChanged();
    }
    
    private JShellOptionsPanel panel() {
        if (ui == null) {
            ui = new JShellOptionsPanel(this);
        }
        return ui;
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return panel();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx("JShell.Options");
    }
    
    void changed() {
        if (!changed) {
            changed = true;
            supp.firePropertyChange(PROP_CHANGED, false, true);
        }
        supp.firePropertyChange(PROP_VALID, null, null);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
        supp.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
        supp.removePropertyChangeListener(l);
    }
    
}
