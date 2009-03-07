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

/*
 * IzPanel.java
 *
 * Created on Nov 11, 2008, 3:32:39 PM
 */

package org.netbeans.modules.bugtracking.bridge.exportdiff;

import org.netbeans.modules.bugtracking.vcshooks.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import org.netbeans.modules.bugtracking.ui.search.QuickSearchComboBar;
import org.netbeans.modules.bugtracking.spi.Issue;
import org.netbeans.modules.bugtracking.spi.Repository;
import org.netbeans.modules.bugtracking.util.BugtrackingUtil;

/**
 *
 * @author Tomas Stupka
 */
public class AttachPanel extends javax.swing.JPanel implements ItemListener, PropertyChangeListener {
    private QuickSearchComboBar qs;
    private PropertyChangeListener issueListener;
    
    public AttachPanel(Repository[] repos, Repository toSelect, PropertyChangeListener issueListener) {
        initComponents();
        this.issueListener = issueListener;
        qs = new QuickSearchComboBar(this);
        issuePanel.add(qs, BorderLayout.NORTH);
        
        repositoryComboBox.setModel(new DefaultComboBoxModel(repos != null ? repos : new Repository[0]));
        repositoryComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if(value != null) {
                    Repository r = (Repository) value;
                    value = r.getDisplayName();
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });

        repositoryComboBox.addItemListener(this);
        if(toSelect != null) {
            repositoryComboBox.setSelectedItem(toSelect);
            qs.setRepository(toSelect);
        } else {
            if(repositoryComboBox.getItemCount() > 0) {
                Repository repo = (Repository) repositoryComboBox.getItemAt(0);
                repositoryComboBox.setSelectedItem(repo);
                qs.setRepository(repo);
            }
        }
        enableFields();        
        
    }

    Issue getIssue() {
        return qs.getIssue();
    }

    private void enableFields() {
        boolean repoSelected = repositoryComboBox.getSelectedItem() != null;
        boolean enableFields = getIssue() != null && repoSelected;

        descriptionTextField.setEnabled(enableFields);
        descriptionLabel.setEnabled(enableFields);
        
        issueLabel.setEnabled(repoSelected);
        qs.enableFields(repoSelected);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        repositoryLabel = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        issueLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        issuePanel = new javax.swing.JPanel();
        descriptionLabel = new javax.swing.JLabel();

        repositoryLabel.setText(org.openide.util.NbBundle.getMessage(AttachPanel.class, "AttachPanel.repositoryLabel.text")); // NOI18N

        repositoryComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jButton2.setText(org.openide.util.NbBundle.getMessage(AttachPanel.class, "AttachPanel.jButton2.text")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        issueLabel.setText(org.openide.util.NbBundle.getMessage(AttachPanel.class, "AttachPanel.issueLabel.text")); // NOI18N

        jLabel2.setForeground(javax.swing.UIManager.getDefaults().getColor("Button.disabledText"));
        jLabel2.setText(org.openide.util.NbBundle.getMessage(AttachPanel.class, "AttachPanel.jLabel2.text")); // NOI18N

        issuePanel.setLayout(new java.awt.BorderLayout());

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
            .add(issuePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .add(issuePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 9, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        descriptionLabel.setText(org.openide.util.NbBundle.getMessage(AttachPanel.class, "AttachPanel.descriptionLabel.text")); // NOI18N

        descriptionTextField.setText(org.openide.util.NbBundle.getMessage(AttachPanel.class, "AttachPanel.descriptionTextField.text")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(repositoryLabel)
                    .add(descriptionLabel)
                    .add(issueLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                        .add(repositoryComboBox, 0, 321, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 85, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, descriptionTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 412, Short.MAX_VALUE)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(repositoryLabel)
                    .add(repositoryComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jButton2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(issueLabel)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(descriptionLabel)
                    .add(descriptionTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        Repository repo = BugtrackingUtil.createRepository();
        repositoryComboBox.addItem(repo);
        repositoryComboBox.setSelectedItem(repo);
    }//GEN-LAST:event_jButton2ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel descriptionLabel;
    final javax.swing.JTextField descriptionTextField = new javax.swing.JTextField();
    private javax.swing.JLabel issueLabel;
    private javax.swing.JPanel issuePanel;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    final javax.swing.JComboBox repositoryComboBox = new javax.swing.JComboBox();
    private javax.swing.JLabel repositoryLabel;
    // End of variables declaration//GEN-END:variables

    public void itemStateChanged(ItemEvent e) {
        enableFields();
        if(e.getStateChange() == ItemEvent.SELECTED) {
            Repository repo = (Repository) e.getItem();
            if(repo != null) {
                qs.setRepository(repo);
            }
        }
    }

    @Override
    public void addNotify() {
        qs.addPropertyChangeListener(this);
        qs.addPropertyChangeListener(issueListener);
        super.addNotify();
    }

    @Override
    public void removeNotify() {
        qs.removePropertyChangeListener(this);
        qs.removePropertyChangeListener(issueListener);
        super.removeNotify();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals(QuickSearchComboBar.EVT_ISSUE_CHANGED)) {
            enableFields();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        descriptionTextField.setEnabled(enabled);
        descriptionLabel.setEnabled(enabled);

        issueLabel.setEnabled(enabled);
        qs.enableFields(enabled);
        repositoryLabel.setEnabled(enabled);
        repositoryComboBox.setEnabled(enabled);
    }

}
