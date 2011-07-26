/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.maven.webframeworks;

import java.awt.Component;
import java.util.LinkedList;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.netbeans.modules.web.api.webmodule.WebFrameworks;
import org.netbeans.modules.web.spi.webmodule.WebFrameworkProvider;

/**
 * Coied from netbeans.org's web.project
 * @author  Radko Najman
 * @author  Milos Kleint
 */
public class AddFrameworkPanel extends javax.swing.JPanel {

    /** Creates new form AddFrameworkPanel */
    public AddFrameworkPanel(List<WebFrameworkProvider> usedFrameworks) {
        initComponents();
        jListFrameworks.setCellRenderer(new FrameworksListCellRenderer());
        createFrameworksList(usedFrameworks);
        jListFrameworks.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    private void createFrameworksList(List<WebFrameworkProvider> usedFrameworks) {
        List<WebFrameworkProvider> frameworks = WebFrameworks.getFrameworks();
        DefaultListModel model = new DefaultListModel();
        jListFrameworks.setModel(model);
        for (WebFrameworkProvider framework : frameworks) {
            if (usedFrameworks.size() == 0) {
                model.addElement(framework);
            } else {
                boolean isUsed = false;
                for (int j = 0; j < usedFrameworks.size(); j++) {
                    if (usedFrameworks.get(j).getName().equals(framework.getName())) {
                        isUsed = true;
                        break;
                    }
                }
                if (!isUsed) {
                    model.addElement(framework);
                }
            }
        }


    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jListFrameworks = new javax.swing.JList();
        jLabel1 = new javax.swing.JLabel();

        jScrollPane1.setViewportView(jListFrameworks);

        jLabel1.setLabelFor(jListFrameworks);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(AddFrameworkPanel.class, "LBL_Select_Framework")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
            .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 163, Short.MAX_VALUE))
        );

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/maven/webframeworks/Bundle"); // NOI18N
        getAccessibleContext().setAccessibleDescription(bundle.getString("ACSD_AddFramework")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JLabel jLabel1;
    public javax.swing.JList jListFrameworks;
    public javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    public List<WebFrameworkProvider> getSelectedFrameworks() {
        List<WebFrameworkProvider> selectedFrameworks = new LinkedList<WebFrameworkProvider>();
        DefaultListModel model = (DefaultListModel) jListFrameworks.getModel();
        int[] indexes = jListFrameworks.getSelectedIndices();
        for (int i = 0; i < indexes.length; i++) {
            selectedFrameworks.add((WebFrameworkProvider)model.get(indexes[i]));
        }
        return selectedFrameworks;
    }

    public static class FrameworksListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof WebFrameworkProvider) {
                WebFrameworkProvider item = (WebFrameworkProvider) value;
                return super.getListCellRendererComponent(list, item.getName(), index, isSelected, cellHasFocus);
            } else {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        }
    }
}
