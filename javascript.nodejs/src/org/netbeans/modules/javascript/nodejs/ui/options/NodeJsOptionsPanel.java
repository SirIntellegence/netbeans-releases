/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2014 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript.nodejs.ui.options;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.javascript.nodejs.exec.NodeExecutable;
import org.netbeans.modules.javascript.nodejs.util.FileUtils;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;

public final class NodeJsOptionsPanel extends JPanel {

    private final ChangeSupport changeSupport = new ChangeSupport(this);


    public NodeJsOptionsPanel() {
        initComponents();
        init();
    }

    @NbBundle.Messages({
        "# {0} - node.js file name",
        "NodeJsOptionsPanel.node.hint=Full path of node file (typically {0}).",
    })
    private void init() {
        errorLabel.setText(" "); // NOI18N
        nodeHintLabel.setText(Bundle.NodeJsOptionsPanel_node_hint(NodeExecutable.NODE_NAME));

        DocumentListener defaultDocumentListener = new DefaultDocumentListener();
        nodeTextField.getDocument().addDocumentListener(defaultDocumentListener);
    }

    public String getNode() {
        return nodeTextField.getText();
    }

    public void setNode(String node) {
        nodeTextField.setText(node);
    }

    public void setError(String message) {
        errorLabel.setText(" "); // NOI18N
        errorLabel.setForeground(UIManager.getColor("nb.errorForeground")); // NOI18N
        errorLabel.setText(message);
    }

    public void setWarning(String message) {
        errorLabel.setText(" "); // NOI18N
        errorLabel.setForeground(UIManager.getColor("nb.warningForeground")); // NOI18N
        errorLabel.setText(message);
    }

    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    void fireChange() {
        changeSupport.fireChange();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this method is always regenerated by the Form
     * Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nodeLabel = new javax.swing.JLabel();
        nodeTextField = new javax.swing.JTextField();
        nodeBrowseButton = new javax.swing.JButton();
        nodeSearchButton = new javax.swing.JButton();
        nodeHintLabel = new javax.swing.JLabel();
        errorLabel = new javax.swing.JLabel();

        org.openide.awt.Mnemonics.setLocalizedText(nodeLabel, org.openide.util.NbBundle.getMessage(NodeJsOptionsPanel.class, "NodeJsOptionsPanel.nodeLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(nodeBrowseButton, org.openide.util.NbBundle.getMessage(NodeJsOptionsPanel.class, "NodeJsOptionsPanel.nodeBrowseButton.text")); // NOI18N
        nodeBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nodeBrowseButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(nodeSearchButton, org.openide.util.NbBundle.getMessage(NodeJsOptionsPanel.class, "NodeJsOptionsPanel.nodeSearchButton.text")); // NOI18N
        nodeSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nodeSearchButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(nodeHintLabel, "HINT"); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(errorLabel, "ERROR"); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(nodeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(nodeHintLabel)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(nodeTextField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nodeBrowseButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nodeSearchButton))))
            .addGroup(layout.createSequentialGroup()
                .addComponent(errorLabel)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nodeLabel)
                    .addComponent(nodeTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nodeBrowseButton)
                    .addComponent(nodeSearchButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(nodeHintLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(errorLabel))
        );
    }// </editor-fold>//GEN-END:initComponents

    @NbBundle.Messages("NodeJsOptionsPanel.node.browse.title=Select node")
    private void nodeBrowseButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_nodeBrowseButtonActionPerformed
        assert EventQueue.isDispatchThread();
        File file = new FileChooserBuilder(NodeJsOptionsPanel.class)
                .setFilesOnly(true)
                .setTitle(Bundle.NodeJsOptionsPanel_node_browse_title())
                .showOpenDialog();
        if (file != null) {
            nodeTextField.setText(file.getAbsolutePath());
        }
    }//GEN-LAST:event_nodeBrowseButtonActionPerformed

    @NbBundle.Messages("NodeJsOptionsPanel.node.none=No node executable was found.")
    private void nodeSearchButtonActionPerformed(ActionEvent evt) {//GEN-FIRST:event_nodeSearchButtonActionPerformed
        assert EventQueue.isDispatchThread();
        for (String node : FileUtils.findFileOnUsersPath(NodeExecutable.NODE_NAME)) {
            nodeTextField.setText(new File(node).getAbsolutePath());
            return;
        }
        // no node found
        StatusDisplayer.getDefault().setStatusText(Bundle.NodeJsOptionsPanel_node_none());
    }//GEN-LAST:event_nodeSearchButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel errorLabel;
    private javax.swing.JButton nodeBrowseButton;
    private javax.swing.JLabel nodeHintLabel;
    private javax.swing.JLabel nodeLabel;
    private javax.swing.JButton nodeSearchButton;
    private javax.swing.JTextField nodeTextField;
    // End of variables declaration//GEN-END:variables

    //~ Inner classes

    private final class DefaultDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            processUpdate();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            processUpdate();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            processUpdate();
        }

        private void processUpdate() {
            fireChange();
        }

    }

}
