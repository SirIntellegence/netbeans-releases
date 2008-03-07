/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.php.project.ui.wizards;

import java.awt.Component;
import java.awt.event.ActionListener;
import javax.swing.ComboBoxEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.MutableComboBoxModel;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;

/**
 * @author  Tomas Mysik
 */
public class SourcesPanelVisual extends JPanel {

    private static final long serialVersionUID = -358263102348820543L;

    private final DefaultComboBoxModel localServerComboBoxModel = new LocalServerComboBoxModel();

    /** Creates new form SourcesPanelVisual */
    public SourcesPanelVisual() {
        initComponents();
        init();
    }

    private void init() {
        localServerComboBox.setModel(localServerComboBoxModel);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        sourcesLabel = new javax.swing.JLabel();
        localServerComboBox = new javax.swing.JComboBox();
        locateButton = new javax.swing.JButton();
        browseButton = new javax.swing.JButton();
        localServerLabel = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(sourcesLabel, org.openide.util.NbBundle.getMessage(SourcesPanelVisual.class, "LBL_Sources")); // NOI18N

        localServerComboBox.setEditable(true);

        org.openide.awt.Mnemonics.setLocalizedText(locateButton, org.openide.util.NbBundle.getMessage(SourcesPanelVisual.class, "LBL_LocateLocalServer")); // NOI18N
        locateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                locateButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(SourcesPanelVisual.class, "LBL_BrowseLocalServer")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(localServerLabel, org.openide.util.NbBundle.getMessage(SourcesPanelVisual.class, "TXT_LocalServer")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(sourcesLabel)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(localServerComboBox, 0, 297, Short.MAX_VALUE)
                            .add(localServerLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(browseButton)
                            .add(locateButton))))
                .add(18, 18, 18))
        );

        layout.linkSize(new java.awt.Component[] {browseButton, locateButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(sourcesLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(localServerLabel)
                    .add(locateButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(localServerComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(browseButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        LocalServer ls = (LocalServer) localServerComboBox.getSelectedItem();
        String newLocation = Utils.browseLocationAction(this, ls.getDocumentRoot());
        if (newLocation == null) {
            return;
        }

        for (int i = 0; i < localServerComboBoxModel.getSize(); i++) {
            LocalServer element = (LocalServer) localServerComboBoxModel.getElementAt(i);
            if (newLocation.equals(element.getDocumentRoot())) {
                localServerComboBox.setSelectedIndex(i);
                return;
            }
        }
        LocalServer localServer = new LocalServer(null, newLocation);
        localServerComboBoxModel.addElement(localServer);
        localServerComboBox.setSelectedItem(localServer);
        Utils.sortComboBoxModel(localServerComboBoxModel);
    }//GEN-LAST:event_browseButtonActionPerformed

    private void locateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_locateButtonActionPerformed
        // XXX
        String message = "Not implemented yet."; // NOI18N
        NotifyDescriptor descriptor = new NotifyDescriptor(
                message,
                message,
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.INFORMATION_MESSAGE,
                new Object[] {NotifyDescriptor.OK_OPTION},
                NotifyDescriptor.OK_OPTION);
        DialogDisplayer.getDefault().notify(descriptor);
    }//GEN-LAST:event_locateButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JComboBox localServerComboBox;
    private javax.swing.JLabel localServerLabel;
    private javax.swing.JButton locateButton;
    private javax.swing.JLabel sourcesLabel;
    // End of variables declaration//GEN-END:variables

    String getSourcesLocation() {
        return (String) localServerComboBox.getSelectedItem();
    }

    MutableComboBoxModel getLocalServerModel() {
        return localServerComboBoxModel;
    }

    void replaceLocalServerLocation(String path) {
        int idx = localServerComboBox.getSelectedIndex();
        if (idx != -1) {
            localServerComboBoxModel.removeElementAt(idx);
            localServerComboBoxModel.insertElementAt(path, idx);
        }
    }

    static class LocalServer implements Comparable<LocalServer> {
        private final String virtualHost;
        private final String documentRoot;

        public LocalServer(String virtualHost, String documentRoot) {
            this.virtualHost = virtualHost;
            this.documentRoot = documentRoot;
        }

        public String getDocumentRoot() {
            return documentRoot;
        }

        public String getVirtualHost() {
            return virtualHost;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append(virtualHost);
            sb.append(" : ");
            sb.append(documentRoot);
            sb.append("]");
            return sb.toString();
        }

        public int compareTo(LocalServer ls) {
            return documentRoot.compareTo(ls.getDocumentRoot());
        }
    }

    private static class LocalServerComboBoxModel extends DefaultComboBoxModel {
        private static final long serialVersionUID = 193082264935872743L;

        public LocalServerComboBoxModel() {
            LocalServer localServer = new LocalServer(null,
                    NbBundle.getMessage(SourcesPanelVisual.class, "LBL_UseProjectFolder"));
            addElement(localServer);
        }
    }
}
