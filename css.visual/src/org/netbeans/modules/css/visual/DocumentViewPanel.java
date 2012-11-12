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
package org.netbeans.modules.css.visual;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.dnd.DnDConstants;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import org.netbeans.modules.css.lib.api.CssParserResult;
import org.netbeans.modules.css.model.api.Model;
import org.netbeans.modules.css.model.api.ModelUtils;
import org.netbeans.modules.css.model.api.Rule;
import org.netbeans.modules.css.model.api.StyleSheet;
import org.netbeans.modules.css.visual.actions.CreateRuleAction;
import org.netbeans.modules.css.visual.api.RuleEditorController;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.web.common.api.WebUtils;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.ExplorerUtils;
import org.openide.explorer.view.BeanTreeView;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author marekfukala
 */
@NbBundle.Messages({})
public class DocumentViewPanel extends javax.swing.JPanel implements ExplorerManager.Provider {

    private static RequestProcessor RP = new RequestProcessor();
    /**
     * Tree view showing the style sheet information.
     */
    private BeanTreeView treeView;
    /**
     * Explorer manager provided by this panel.
     */
    private ExplorerManager manager = new ExplorerManager();
    /**
     * Lookup of this panel.
     */
    private final Lookup lookup;
    private final Lookup cssStylesLookup;
    /**
     * Filter for the tree displayed in this panel.
     */
    private Filter filter = new Filter();
    private DocumentViewModel documentModel;
    private DocumentNode documentNode;
    private final CreateRuleAction createRuleAction;
    private final PropertyChangeListener RULE_EDITOR_CONTROLLER_LISTENER = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            if (RuleEditorController.PropertyNames.RULE_SET.name().equals(pce.getPropertyName())) {
                final Rule rule = (Rule) pce.getNewValue();
                if (rule == null) {
                    setSelectedStyleSheet();
                    return ;
                }
                final Model model = rule.getModel();
                model.runReadTask(new Model.ModelTask() {
                    @Override
                    public void run(StyleSheet styleSheet) {
                        setSelectedRule(RuleHandle.createRuleHandle(rule));
                    }
                });
            }
        }
    };

    /**
     * Creates new form DocumentViewPanel
     */
    public DocumentViewPanel(Lookup cssStylesLookup) {
        this.cssStylesLookup = cssStylesLookup;

        createRuleAction = new CreateRuleAction();

        Result<FileObject> result = cssStylesLookup.lookupResult(FileObject.class);
        result.addLookupListener(new LookupListener() {
            @Override
            public void resultChanged(LookupEvent ev) {
                //current stylesheet changed
                contextChanged();
            }
        });

        //listen on selected rule in the rule editor and set selected rule in the 
        //document view accordingly
        RuleEditorController controller = cssStylesLookup.lookup(RuleEditorController.class);
        controller.addRuleEditorListener(RULE_EDITOR_CONTROLLER_LISTENER);

        lookup = ExplorerUtils.createLookup(getExplorerManager(), getActionMap());
        Result<Node> lookupResult = lookup.lookupResult(Node.class);
        lookupResult.addLookupListener(new LookupListener() {
            @Override
            public void resultChanged(LookupEvent ev) {
                //selected node changed
                Node[] selectedNodes = manager.getSelectedNodes();
                Node selected = selectedNodes.length > 0 ? selectedNodes[0] : null;
                if (selected != null) {
                    RuleHandle ruleHandle = selected.getLookup().lookup(RuleHandle.class);
                    if (ruleHandle != null) {
                        selectRuleInRuleEditor(ruleHandle);
                        CssStylesListenerSupport.fireRuleSelected(ruleHandle.getRule());
                    }
                    Location location = selected.getLookup().lookup(Location.class);
                    if (location != null) {
                        createRuleAction.setStyleSheet(location.getFile());
                    }

                }
            }
        });

        initComponents();

        initTreeView();

        //create toolbar
        CustomToolbar toolbar = new CustomToolbar();
        toolbar.addButton(filterToggleButton);
        toolbar.addLineSeparator();
        toolbar.addButton(createRuleToggleButton);

        northPanel.add(toolbar, BorderLayout.EAST);

        //add document listener to the filter text field 
        filterTextField.getDocument().addDocumentListener(new DocumentListener() {
            private void contentChanged() {
                filter.setPattern(filterTextField.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                contentChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                contentChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });

        setFilterVisible(true);
        filterToggleButton.setSelected(true);

        initializeNodes();

        contextChanged();
    }

    /**
     * Select corresponding node in the document view tree upon change of the
     * rule editor's content.
     *
     * A. The RuleNode holds instances of Rule-s from the model instance which
     * was created as setContext(file) was called on the view panel. B. The
     * 'rule' argument here is from an up-to-date model.
     *
     */
    private void setSelectedRule(RuleHandle handle) {
        try {
            Node foundRuleNode = findLocation(manager.getRootContext(), handle);
            Node[] toSelect = foundRuleNode != null ? new Node[]{foundRuleNode} : new Node[0];
            manager.setSelectedNodes(toSelect);
        } catch (PropertyVetoException ex) {
            //no-op
        }
    }

    private void setSelectedStyleSheet() {
        try {
            Node styleSheetNode = findLocation(manager.getRootContext(), new Location(getContext()));
            //assert styleSheetNode != null;
            Node[] toSelect = styleSheetNode != null ? new Node[]{styleSheetNode} : new Node[0];
            manager.setSelectedNodes(toSelect);
        } catch (PropertyVetoException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Select rule in rule editor upon user action in the document view.
     */
    private void selectRuleInRuleEditor(RuleHandle handle) {
        RuleEditorController rec = cssStylesLookup.lookup(RuleEditorController.class);
        final Rule rule = handle.getRule();
        final AtomicReference<Rule> matched_rule_ref = new AtomicReference<Rule>();

        FileObject file = handle.getFile();
        Source source = Source.create(file);
        try {
            ParserManager.parse(Collections.singleton(source), new UserTask() {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    ResultIterator ri = WebUtils.getResultIterator(resultIterator, "text/css"); //NOI18N
                    if (ri != null) {
                        final CssParserResult result = (CssParserResult) ri.getParserResult();
                        final Model model = Model.getModel(result);

                        model.runReadTask(new Model.ModelTask() {
                            @Override
                            public void run(StyleSheet styleSheet) {
                                ModelUtils utils = new ModelUtils(model);
                                Rule match = utils.findMatchingRule(rule.getModel(), rule);
                                matched_rule_ref.set(match);
                            }
                        });
                    }
                }
            });
        } catch (ParseException ex) {
            Exceptions.printStackTrace(ex);
        }

        Rule match = matched_rule_ref.get();
        if (match != null) {
            rec.setModel(match.getModel());
            rec.setRule(match);
        }
    }

    @Override
    public final ExplorerManager getExplorerManager() {
        return manager;
    }

    private FileObject getContext() {
        return cssStylesLookup.lookup(FileObject.class);
    }

    /**
     * Called when the CssStylesPanel is activated for different file.
     */
    private void contextChanged() {
        final FileObject context = getContext();

        //update the action context
        createRuleAction.setStyleSheet(context);

        //dispose old model
        if (documentModel != null) {
            documentModel.dispose();
        }

        if (context == null) {
            documentModel = null;
        } else {
            documentModel = new DocumentViewModel(context);
        }

        RP.post(new Runnable() {
            @Override
            public void run() {
                documentNode.setModel(documentModel);
                setSelectedStyleSheet();
            }
        });

    }

    /**
     * Initializes the tree view.
     */
    private void initTreeView() {
        treeView = new BeanTreeView() {
            {
                MouseAdapter listener = createTreeMouseListener();
                tree.addMouseListener(listener);
                tree.addMouseMotionListener(listener);
                tree.setCellRenderer(createTreeCellRenderer(tree.getCellRenderer()));
            }

            @Override
            public void expandAll() {
                // The original expandAll() doesn't work for us as it doesn't
                // seem to wait for the calculation of sub-nodes.
                Node root = manager.getRootContext();
                expandAll(root);
                // The view attempts to scroll to the expanded node
                // and it does it with a delay. Hence, simple calls like
                // tree.scrollRowToVisible(0) have no effect (are overriden
                // later) => the dummy collapse and expansion attempts
                // to work around that and keep the root node visible.
                collapseNode(root);
                expandNode(root);
            }

            /**
             * Expands the whole sub-tree under the specified node.
             *
             * @param node root node of the sub-tree that should be expanded.
             */
            private void expandAll(Node node) {
                treeView.expandNode(node);
                for (Node subNode : node.getChildren().getNodes(true)) {
                    if (!subNode.isLeaf()) {
                        expandAll(subNode);
                    }
                }
            }
        };
        treeView.setAllowedDragActions(DnDConstants.ACTION_NONE);
        treeView.setAllowedDropActions(DnDConstants.ACTION_NONE);
        treeView.setRootVisible(false);
        add(treeView, BorderLayout.CENTER);
    }

//    private void refreshNodes() {
//        refresh(manager.getRootContext());
//    }
//    
//    private void refresh(Node node) {
//        Children children = node.getChildren();
//        if(children instanceof Refreshable) {
//            ((Refreshable)children).refreshKeys();
//        }
//        for(Node child : children.getNodes()) {
//            refresh(child);
//        }
//    }
    private void initializeNodes() {
        documentNode = new DocumentNode(documentModel, filter);
        Node root = new FakeRootNode<DocumentNode>(documentNode,
                new Action[]{});
        manager.setRootContext(root);
        treeView.expandAll();
    }

    /**
     * Finds a node that represents the specified location in a tree represented
     * by the given root node.
     *
     * @param root root of a tree to search.
     * @param rule rule to find.
     * @return node that represents the rule or {@code null}.
     */
    public static Node findLocation(Node root, Location location) {
        Location candidate = root.getLookup().lookup(Location.class);
        if (candidate != null && location.equals(candidate)) {
            return root;
        }
        for (Node node : root.getChildren().getNodes()) {
            Node result = findLocation(node, location);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    // The last node we were hovering over.
    Object lastHover = null;

    /**
     * Creates a mouse listener for the tree view.
     *
     * @return mouse listener for the tree view.
     */
    public MouseAdapter createTreeMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                processEvent(e);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                processEvent(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                processEvent(null);
                // Make sure that lastHover != <any potential value>
                // i.e., make sure that change in hover is triggered when
                // mouse returns into this component
                lastHover = new Object();
            }

            /**
             * Processes the specified mouse event.
             *
             * @param e mouse event to process.
             */
            private void processEvent(MouseEvent e) {
//                Object hover = null;
//                if (e != null) {
//                    JTree tree = (JTree)e.getSource();
//                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
//                    if (path != null) {
//                        hover = path.getLastPathComponent();
//                    }
//                }
//                if (hover != lastHover) {
//                    lastHover = hover;
//                    final String selector;
//                    if (hover != null) {
//                        Node node = Visualizer.findNode(hover);
//                        Rule rule = node.getLookup().lookup(Rule.class);
//                        if (rule != null) {
//                            selector = rule.getSelector();
//                        } else {
//                            selector = null;
//                        }
//                    } else {
//                        selector = null;
//                    }
//                    treeView.repaint();
//                    final PageModel pageModel = currentPageModel();
//                    if (pageModel != null) {
//                        RP.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                pageModel.setHighlightedSelector(selector);
//                            }
//                        });
//                    }
//                }
            }
        };
    }

    /**
     * Creates a cell renderer for the tree view.
     *
     * @param delegate delegating/original tree renderer.
     * @return call renderer for the tree view.
     */
    private TreeCellRenderer createTreeCellRenderer(final TreeCellRenderer delegate) {
        Color origColor = UIManager.getColor("Tree.selectionBackground"); // NOI18N
        Color color = origColor.brighter().brighter();
        if (color.equals(Color.WHITE)) { // Issue 217127
            color = origColor.darker();
        }
        // Color used for hovering highlight
        final Color hoverColor = color;
        return new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                JLabel component;
                if (!selected && (value == lastHover)) {
                    component = (JLabel) delegate.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, hasFocus);
                    component.setBackground(hoverColor);
                    component.setOpaque(true);
                } else {
                    component = (JLabel) delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                }
                return component;
            }
        };
    }

    private void setFilterVisible(boolean visible) {
        northPanel.remove(filterTextField);
        if (visible) {
            //update the UI
            northPanel.add(filterTextField, BorderLayout.CENTER);
            //set the filter text to the node
            filter.setPattern(filterTextField.getText());

            filterTextField.requestFocus();
        } else {
            //just remove the filter text from the node, but keep it in the field
            //so next time it is opened it will contain the old value
            filter.setPattern(null);
        }
        northPanel.revalidate();
        northPanel.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        createRuleToggleButton = new javax.swing.JToggleButton();
        filterToggleButton = new javax.swing.JToggleButton();
        filterTextField = new javax.swing.JTextField();
        northPanel = new javax.swing.JPanel();

        createRuleToggleButton.setAction(createRuleAction);
        createRuleToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/css/visual/resources/newRule.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(createRuleToggleButton, null);
        createRuleToggleButton.setToolTipText(org.openide.util.NbBundle.getMessage(DocumentViewPanel.class, "DocumentViewPanel.createRuleToggleButton.toolTipText")); // NOI18N
        createRuleToggleButton.setFocusable(false);
        createRuleToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createRuleToggleButtonActionPerformed(evt);
            }
        });

        filterToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/css/visual/resources/find.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(filterToggleButton, null);
        filterToggleButton.setToolTipText(org.openide.util.NbBundle.getMessage(DocumentViewPanel.class, "DocumentViewPanel.filterToggleButton.toolTipText")); // NOI18N
        filterToggleButton.setFocusable(false);
        filterToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterToggleButtonActionPerformed(evt);
            }
        });

        filterTextField.setText(org.openide.util.NbBundle.getMessage(DocumentViewPanel.class, "DocumentViewPanel.filterTextField.text")); // NOI18N
        filterTextField.setToolTipText(org.openide.util.NbBundle.getMessage(DocumentViewPanel.class, "DocumentViewPanel.filterToggleButton.toolTipText")); // NOI18N

        setLayout(new java.awt.BorderLayout());

        northPanel.setLayout(new java.awt.BorderLayout());
        add(northPanel, java.awt.BorderLayout.NORTH);
    }// </editor-fold>//GEN-END:initComponents

    private void filterToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterToggleButtonActionPerformed
        setFilterVisible(filterToggleButton.isSelected());
    }//GEN-LAST:event_filterToggleButtonActionPerformed

    private void createRuleToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createRuleToggleButtonActionPerformed
        createRuleToggleButton.setSelected(false); //disable selected as it's a toggle button
    }//GEN-LAST:event_createRuleToggleButtonActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton createRuleToggleButton;
    private javax.swing.JTextField filterTextField;
    private javax.swing.JToggleButton filterToggleButton;
    private javax.swing.JPanel northPanel;
    // End of variables declaration//GEN-END:variables
}
