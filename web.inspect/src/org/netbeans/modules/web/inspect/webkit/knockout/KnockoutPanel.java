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
package org.netbeans.modules.web.inspect.webkit.knockout;

import java.awt.dnd.DnDConstants;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JComponent;
import org.netbeans.modules.web.inspect.PageModel;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

/**
 * A panel for Knockout-related information about the inspected page.
 *
 * @author Jan Stola
 */
public class KnockoutPanel extends javax.swing.JPanel {
    /** Page model for this panel. */
    private final PageModel pageModel;
    /** Tree view that displays Knockout context of the selected node. */
    private BeanTreeView treeView;

    /**
     * Creates a new {@code KnockoutPanel}.
     */
    public KnockoutPanel(PageModel pageModel) {
        this.pageModel = pageModel;

        initTreeView();
        initComponents();
        if (pageModel == null) {
            messageLabel.setText(NbBundle.getMessage(KnockoutPanel.class, "KnockoutPanel.messageLabel.noInspection")); // NOI18N
            add(messageLabel);
        } else {
            pageModel.addPropertyChangeListener(new Listener());
            update();
        }
    }

    /**
     * Initializes the tree view.
     */
    private void initTreeView() {
        treeView = new BeanTreeView();
        treeView.setAllowedDragActions(DnDConstants.ACTION_NONE);
        treeView.setAllowedDropActions(DnDConstants.ACTION_NONE);
        treeView.setRootVisible(false);
    }

    /**
     * Updates the panel (according to the current selection).
     */
    final void update() {
        List<? extends Node> selection = pageModel.getSelectedNodes();
        JComponent componentToShow;
        if (selection.isEmpty()) {
            messageLabel.setText(NbBundle.getMessage(KnockoutPanel.class, "KnockoutPanel.messageLabel.noSelection")); // NOI18N
            componentToShow = messageLabel;
        } else if (selection.size() > 1) {
            messageLabel.setText(NbBundle.getMessage(KnockoutPanel.class, "KnockoutPanel.messageLabel.noSingleSelection")); // NOI18N
            componentToShow = messageLabel;
        } else {
            // Node selectedNode = selection.get(0);
            // componentToShow = treeView;
            messageLabel.setText("Coming soon ... ;-)");
            componentToShow = messageLabel;
        }
        if (componentToShow.getParent() == null) {
            removeAll();
            add(componentToShow);
        }
        repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        messageLabel = new javax.swing.JLabel();

        messageLabel.setBackground(treeView.getViewport().getView().getBackground());
        messageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        messageLabel.setEnabled(false);
        messageLabel.setOpaque(true);

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel messageLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Listener for the changes of the page model.
     */
    final class Listener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (PageModel.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                update();
            }
        }
        
    }

}
