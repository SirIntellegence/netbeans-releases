/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cnd.remote.ui.proxysettings;

import java.awt.BorderLayout;
import java.util.Iterator;
import javax.accessibility.Accessible;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSeparator;
import org.netbeans.spi.options.OptionsCategory;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Item;
import org.openide.util.Lookup.Result;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author akrasny
 */
public final class ProxySettingsPanel extends javax.swing.JPanel {

    static final String OD_LAYER_FOLDER_NAME = "OptionsDialog"; // NOI18N
    static final String CATEGORY_NAME = "General"; // NOI18N
    private OptionsPanelController controller = null;
    private final JComponent origPanel;
    
    private static final RequestProcessor RP = new RequestProcessor("ProxySettingsPanel", 1); // NOI18N

    /**
     * Creates new form ProxySettingsPanel
     */
    public ProxySettingsPanel() {
        initComponents();
        origPanel = findGeneralOptionsPanel();

        if (origPanel != null) {
            hideUnwantedItems();
            proxySettingPanel.add(origPanel, BorderLayout.CENTER);
        } else {
            proxySettingPanel.add(new JLabel(NbBundle.getMessage(ProxySettingsPanel.class, "ProxySettingsPanel.error.noProxySettings"))); // NOI18N
        }
    }

    public boolean isValidState() {
        if (controller == null) {
            return true;
        }

        return controller.isValid();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        proxySettingPanel = new javax.swing.JPanel();

        proxySettingPanel.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proxySettingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 401, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(proxySettingPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 268, Short.MAX_VALUE)
                .addGap(0, 0, 0))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel proxySettingPanel;
    // End of variables declaration//GEN-END:variables

    private void applyProxyChanges() {
        controller.applyChanges();
        controller.update();
        hideUnwantedItems();
    }

    public void applyProxyChangesIfNeed() {
        if (controller.isChanged() && controller.isValid()) {
            applyProxyChanges();
        }
    }

    private JComponent findGeneralOptionsPanel() {
        Lookup lookup = Lookups.forPath(OD_LAYER_FOLDER_NAME);
        Result<OptionsCategory> result = lookup.lookupResult(OptionsCategory.class);
        Iterator<? extends Item<OptionsCategory>> it = result.allItems().iterator();
        while (it.hasNext()) {
            Item<OptionsCategory> item = it.next();
            String categoryID = item.getId().substring(OD_LAYER_FOLDER_NAME.length() + 1);
            if (CATEGORY_NAME.equals(categoryID)) {
                controller = item.getInstance().create();
                controller.update();
                return controller.getComponent(Lookup.EMPTY);
            }
        }
        return null;
    }

    private void hideUnwantedItems() {
        int cnt = origPanel.getAccessibleContext().getAccessibleChildrenCount();
        boolean visible = false;
        boolean firstLabel = false;
        int button = 0;
        for (int i = 0; i < cnt; i++) {
            Accessible accessibleChild = origPanel.getAccessibleContext().getAccessibleChild(i);
            if (accessibleChild instanceof JComponent) {
                JComponent elem = (JComponent) accessibleChild;
                if (elem instanceof JSeparator) {
                    elem.setVisible(false);
                    visible = !visible;
                    firstLabel = true;
                    continue;
                }
                if (firstLabel && elem instanceof JLabel) {
                    firstLabel = false;
                    elem.setVisible(false);
                    continue;
                }
                if (!visible && elem.isVisible()) {
                    elem.setVisible(false);
                } else {
                    // #253316 - "Test connection" button ... works wrong
                    // I dislike this hack, but it is not worse than the entire method :-/
                    if (elem instanceof JButton) {
                        button++;
                        if (button == 3) {
                            elem.setVisible(false);
                        }
                    }
                }
            }
        }
    }
}
