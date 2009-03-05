/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 * 
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.modules.bpel.project.ui.customizer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.TreeSelectionModel;

import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;

import org.openide.util.HelpCtx;
import org.netbeans.modules.compapp.projects.base.ui.customizer.IcanproProjectProperties;

public class Customizer extends javax.swing.JPanel implements HelpCtx.Provider  {
    
    private Component currentCustomizer;
    private GridBagConstraints fillConstraints;
    private IcanproProjectProperties webProperties;

    public Customizer(IcanproProjectProperties webProperties) {
        initComponents();
        this.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(Customizer.class, "ACS_Customize_A11YDesc")); //NOI18N
        this.webProperties = webProperties;
        
        fillConstraints = new GridBagConstraints();
        fillConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        fillConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        fillConstraints.fill = java.awt.GridBagConstraints.BOTH;
        fillConstraints.weightx = 1.0;
        fillConstraints.weighty = 1.0;
        
        categoryPanel.add( new CategoryView( createRootNode(webProperties) ), fillConstraints );
    }
    
    public HelpCtx getHelpCtx() {
        return new HelpCtx(Customizer.class);
    }     
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        categoryPanel = new javax.swing.JPanel();
        customizerPanel = new javax.swing.JPanel();

        setLayout(new java.awt.GridBagLayout());

        setPreferredSize(new java.awt.Dimension(750, 450));
        categoryPanel.setLayout(new java.awt.GridBagLayout());

        categoryPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        categoryPanel.setMinimumSize(new java.awt.Dimension(220, 4));
        categoryPanel.setPreferredSize(new java.awt.Dimension(220, 4));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 8, 8, 8);
        add(categoryPanel, gridBagConstraints);

        customizerPanel.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(8, 4, 8, 8);
        add(customizerPanel, gridBagConstraints);

    }// </editor-fold>//GEN-END:initComponents
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel categoryPanel;
    private javax.swing.JPanel customizerPanel;
    // End of variables declaration//GEN-END:variables
    
    private class CategoryView extends JPanel implements ExplorerManager.Provider, HelpCtx.Provider {
        
        private ExplorerManager manager;
        private BeanTreeView btv;
        
        CategoryView( Node rootNode ) {
            manager = new ExplorerManager();
            setLayout( new BorderLayout() );
            
            Dimension size = new Dimension( 220, 4 );
            btv = new BeanTreeView();
            btv.setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
            btv.setPopupAllowed( false );
            btv.setRootVisible( false );
            btv.setDefaultActionAllowed( false );            
            btv.setMinimumSize( size );
            btv.setPreferredSize( size );
            btv.setMaximumSize( size );
            this.add( btv, BorderLayout.CENTER );                        
            manager.setRootContext( rootNode );
            manager.addPropertyChangeListener( new ManagerChangeListener() );
            selectFirstNode();
            btv.expandAll();
                   
            btv.getAccessibleContext().setAccessibleName(NbBundle.getMessage(Customizer.class, "ACS_CustomizeTree_A11YName")); //NOI18N
            btv.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(Customizer.class, "ACS_CustomizeTree_A11YDesc")); //NOI18N
        }
        
        public HelpCtx getHelpCtx() {
            return new HelpCtx(Customizer.class);
        }         
        public ExplorerManager getExplorerManager() {
            return manager;
        }
        
        public void addNotify() {
            super.addNotify();
            btv.expandAll();
        }
        
        private void selectFirstNode() {
            Children ch = manager.getRootContext().getChildren();

            if ( ch != null ) {
                Node nodes[] = ch.getNodes();
                
                if ( nodes != null && nodes.length > 0 ) {
                    try {                    
                        manager.setSelectedNodes( new Node[] { nodes[0] } );
                    }
                    catch ( PropertyVetoException e ) {
                        // No node will be selected
                    }
                }
            }
        }
        
        private class ManagerChangeListener implements PropertyChangeListener {

            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getSource() != manager) {
                    return;
                }

                if (ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                    Node nodes[] = manager.getSelectedNodes(); 
                    if ( nodes == null || nodes.length <= 0 ) {
                        return;
                    }
                    Node node = nodes[0];

                    if ( currentCustomizer != null ) {
                        customizerPanel.remove( currentCustomizer );
                    }
                    if ( node.hasCustomizer() ) {
                        currentCustomizer = node.getCustomizer();
                        
                        if ( currentCustomizer instanceof Panel ) {
                            ((Panel)currentCustomizer).initValues();
                        }
                        customizerPanel.add( currentCustomizer, fillConstraints );
                        customizerPanel.validate();
                        customizerPanel.repaint();
                    }
                    else {
                        currentCustomizer = null;
                    }
                    return;
                }
            }
        }
    }
             
    private static Node createRootNode(IcanproProjectProperties webProperties) {
        String ICON_FOLDER = "org/netbeans/modules/bpel/project/ui/resources/";
        ResourceBundle bundle = NbBundle.getBundle( Customizer.class );
        
        ConfigurationDescription descriptions[] = new ConfigurationDescription[] {
            new ConfigurationDescription(
                "General",
                bundle.getString( "LBL_Config_General" ), // NOI18N
                ICON_FOLDER + "defaultCategory", // "icanproProjectIcon", // NOI18N
                new CustomizerGeneral(webProperties), null),
        };

        ConfigurationDescription rootDescription = new ConfigurationDescription(
        "InvisibleRoot", "InvisibleRoot", null, null, descriptions );  // NOI18N

        return new ConfigurationNode( rootDescription );
    }

    private static javax.swing.JLabel createEmptyLabel( String text ) {
        JLabel label;
        if ( text == null ) {
            label = new JLabel();
        }
        else {
            label = new JLabel( text );
            label.setHorizontalAlignment( JLabel.CENTER );
        }
        return label;
    }

    private static class ConfigurationDescription {
        private String name;
        private String displayName;
        private String iconBase;
        private Component customizer;
        private ConfigurationDescription[] children;
        
        ConfigurationDescription( String name,
        String displayName,
        String iconBase,
        Component customizer,
        ConfigurationDescription[] children ) {
            
            this.name = name;
            this.displayName = displayName;
            this.iconBase = iconBase;
            this.customizer = customizer;
            this.children = children;
        }
    }
    
    private static class ConfigurationNode extends AbstractNode {
        private Component customizer;
        
        public ConfigurationNode( ConfigurationDescription description ) {
            super( description.children == null ? Children.LEAF : new ConfigurationChildren( description.children ) );
            setName( description.name );
            setDisplayName( description.displayName );
            if ( description.iconBase != null ) {
                setIconBaseWithExtension(description.iconBase);
            }
            this.customizer = description.customizer;
        }
        
        public boolean hasCustomizer() {
            return true;
        }
        
        public Component getCustomizer() {
            return customizer;
        }
    }
    
    private static class ConfigurationChildren extends Children.Keys {
        
        private Collection descriptions;
        
        public ConfigurationChildren( ConfigurationDescription[] descriptions ) {
            this.descriptions = Arrays.asList( descriptions );
        }
        
        public void addNotify() {
            setKeys( descriptions );
        }
        
        public void removeNotify() {
            setKeys( Collections.EMPTY_LIST );
        }
        
        protected Node[] createNodes( Object key ) {
            return new Node[] { new ConfigurationNode( (ConfigurationDescription)key ) };
        }
    }
    
    static interface Panel {
        public void initValues();
    }
}
