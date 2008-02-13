/*
 * SpacesPanel.java
 *
 * Created on January 31, 2008, 3:46 PM
 */

package org.netbeans.modules.cnd.editor.options;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import org.netbeans.modules.cnd.editor.api.CodeStyle;
import org.openide.util.NbBundle;

/**
 * was cloned from org.netbeans.modules.java.ui.FmtSpaces
 *
 * @author Alexander Simon
 */
public class SpacesPanel extends JPanel implements TreeCellRenderer, MouseListener, KeyListener {

    private MyCategorySupport controller;
    private DefaultTreeModel model;
    private CodeStyle.Language language;
    private DefaultTreeCellRenderer dr = new DefaultTreeCellRenderer();    
    private JCheckBox renderer = new JCheckBox();
    

    /** Creates new form SpacesPanel */
    public SpacesPanel(CodeStyle.Language language) {
        initComponents();
        this.language = language;
    }

    private void initModel(){
        model = createModel();
        spaceTree.setModel(model);
        spaceTree.setRootVisible(false);
        spaceTree.setShowsRootHandles(true);
        spaceTree.setCellRenderer(this);
        spaceTree.setEditable(false);
        spaceTree.addMouseListener(this);
        spaceTree.addKeyListener(this);
        
        dr.setIcon(null);
        dr.setOpenIcon(null);
        dr.setClosedIcon(null);
        
        DefaultMutableTreeNode root = (DefaultMutableTreeNode)model.getRoot();
        for( int i = root.getChildCount(); i >= 0; i-- ) {
            spaceTree.expandRow(i);
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        spaceTree = new javax.swing.JTree();

        setLayout(new java.awt.BorderLayout());

        jScrollPane1.setViewportView(spaceTree);

        add(jScrollPane1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTree spaceTree;
    // End of variables declaration//GEN-END:variables

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        renderer.setBackground( selected ? dr.getBackgroundSelectionColor() : dr.getBackgroundNonSelectionColor() );
        renderer.setForeground( selected ? dr.getTextSelectionColor() : dr.getTextNonSelectionColor() );
        renderer.setEnabled( true );

        Object data = ((DefaultMutableTreeNode)value).getUserObject();
        if ( data instanceof Item ) {
            Item item = ((Item)data);
            
            if ( ((DefaultMutableTreeNode)value).getAllowsChildren() ) {
                Component c = dr.getTreeCellRendererComponent(tree, value, leaf, expanded, leaf, row, hasFocus);
                return c;
            } else {
                renderer.setText( item.displayName );
                renderer.setSelected( item.value );
            }
        } else {
            Component c = dr.getTreeCellRendererComponent(tree, value, leaf, expanded, leaf, row, hasFocus);             
            return c;
        }
        return renderer;
    }
    
    public void mouseClicked(MouseEvent e) {
        Point p = e.getPoint();
        TreePath path = spaceTree.getPathForLocation(e.getPoint().x, e.getPoint().y);
        if ( path != null ) {
            Rectangle r = spaceTree.getPathBounds(path);
            if (r != null) {
                r.width = r.height;
                if ( r.contains(p)) {
                    toggle( path );
                }
            }
        }
    }
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    
    public void keyTyped(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER ) {
            if ( e.getSource() instanceof JTree ) {
                JTree tree = (JTree) e.getSource();
                TreePath path = tree.getSelectionPath();

                if ( toggle( path )) {
                    e.consume();
                }
            }
        }
    }
    
    private DefaultTreeModel createModel() {
        Item[] categories = new Item[] {
            new Item("BeforeKeywords",                          // NOI18N
                new Item(EditorOptions.spaceBeforeWhile),
                new Item(EditorOptions.spaceBeforeElse),
                new Item(EditorOptions.spaceBeforeCatch) ),
    
            new Item("BeforeParentheses",                       // NOI18N
                new Item(EditorOptions.spaceBeforeMethodDeclParen),
                new Item(EditorOptions.spaceBeforeMethodCallParen),
                new Item(EditorOptions.spaceBeforeIfParen),
                new Item(EditorOptions.spaceBeforeForParen),
                new Item(EditorOptions.spaceBeforeWhileParen),
                new Item(EditorOptions.spaceBeforeCatchParen),
                new Item(EditorOptions.spaceBeforeSwitchParen) ),
    
            new Item("AroundOperators",                         // NOI18N
                new Item(EditorOptions.spaceAroundUnaryOps),
                new Item(EditorOptions.spaceAroundBinaryOps),
                new Item(EditorOptions.spaceAroundTernaryOps),
                new Item(EditorOptions.spaceAroundAssignOps) ),
            
            new Item("BeforeLeftBraces",                        // NOI18N
                new Item(EditorOptions.spaceBeforeClassDeclLeftBrace),
                new Item(EditorOptions.spaceBeforeMethodDeclLeftBrace),
                new Item(EditorOptions.spaceBeforeIfLeftBrace),
                new Item(EditorOptions.spaceBeforeElseLeftBrace),
                new Item(EditorOptions.spaceBeforeWhileLeftBrace),
                new Item(EditorOptions.spaceBeforeForLeftBrace),
                new Item(EditorOptions.spaceBeforeDoLeftBrace),
                new Item(EditorOptions.spaceBeforeSwitchLeftBrace),
                new Item(EditorOptions.spaceBeforeTryLeftBrace),
                new Item(EditorOptions.spaceBeforeCatchLeftBrace),
                new Item(EditorOptions.spaceBeforeArrayInitLeftBrace) ),

            new Item("WithinParentheses",                       // NOI18N
                new Item(EditorOptions.spaceWithinParens),
                new Item(EditorOptions.spaceWithinMethodDeclParens),
                new Item(EditorOptions.spaceWithinMethodCallParens),
                new Item(EditorOptions.spaceWithinIfParens),
                new Item(EditorOptions.spaceWithinForParens),
                new Item(EditorOptions.spaceWithinWhileParens),
                new Item(EditorOptions.spaceWithinSwitchParens),
                new Item(EditorOptions.spaceWithinCatchParens),
                new Item(EditorOptions.spaceWithinTypeCastParens),
                new Item(EditorOptions.spaceWithinBraces),
                new Item(EditorOptions.spaceWithinArrayInitBrackets) ),
                
             new Item("Other",                                  // NOI18N
                new Item(EditorOptions.spaceBeforeComma),
                new Item(EditorOptions.spaceAfterComma),
                new Item(EditorOptions.spaceBeforeSemi),
                new Item(EditorOptions.spaceAfterSemi),
                new Item(EditorOptions.spaceBeforeColon),
                new Item(EditorOptions.spaceAfterColon),
                new Item(EditorOptions.spaceAfterTypeCast) )
        };

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root", true); // NOI18N
        DefaultTreeModel amodel = new DefaultTreeModel( root );
        for( Item item : categories ) {
            DefaultMutableTreeNode cn = new DefaultMutableTreeNode( item, true );
            root.add(cn);
            for ( Item si : item.items ) {
                DefaultMutableTreeNode in = new DefaultMutableTreeNode( si, false );
                cn.add(in);
            }
        }
        return amodel;
    }

    private boolean toggle(TreePath treePath) {
        if( treePath == null ) {
            return false;
        }
        Object o = ((DefaultMutableTreeNode)treePath.getLastPathComponent()).getUserObject();
        DefaultTreeModel amodel = (DefaultTreeModel)spaceTree.getModel();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        if ( o instanceof Item ) {
            Item item = (Item)o;
            if ( node.getAllowsChildren() ) {
                return false;
            }
            item.value = !item.value;            
            amodel.nodeChanged(node); 
            amodel.nodeChanged(node.getParent());
            controller.changed();
        }
        return false;
    }

    public static Category getController(CodeStyle.Language language) {
        Map<String, Object> force = new HashMap<String, Object>();
        SpacesPanel panel = new SpacesPanel(language);
        MyCategorySupport controller = new MyCategorySupport(
                language,
                "LBL_Spaces", // NOI18N
                panel, // NOI18N
                NbBundle.getMessage(SpacesPanel.class, "SAMPLE_Spaces"),
                force);
        panel.controller = controller;
        panel.initModel();
        controller.update();
        return controller;
    }

    private static class Item {
        String id;        
        String displayName;        
        boolean value;        
        Item[] items;
        public Item(String id, Item... items) {
            this.id = id;
            this.items = items;
            this.displayName = NbBundle.getMessage(SpacesPanel.class, "LBL_" + id ); // NOI18N            
        }
        @Override
        public String toString() {
            return displayName;
        }
    }
        
    private static class MyCategorySupport extends CategorySupport {
        SpacesPanel panel;
        public MyCategorySupport(CodeStyle.Language language,
                String nameKey, JPanel panel, String previewText, Map<String, Object> forcedOptions) {
            super(language, nameKey, panel, previewText,forcedOptions );
            this.panel = (SpacesPanel) getComponent(null); 
        }
        @Override
        protected void addListeners() {
            // Should not do anything
        }
        @Override
        public void update() {
            List<Item> items = getAllItems();
            Preferences node = EditorOptions.getPreferences(EditorOptions.getCurrentProfileId(panel.language));
            for (Item item : items) {
                boolean df = (Boolean)EditorOptions.getDefault(item.id);
                item.value = node.getBoolean(item.id, df);
            }
        }
        @Override
        public void applyChanges() {
            storeTo(EditorOptions.getPreferences(EditorOptions.getCurrentProfileId(panel.language)));            
        }

        @Override
        public void storeTo(Preferences preferences) {
            List<Item> items = getAllItems();
            for (Item item : items) {
                preferences.putBoolean(item.id, item.value);
            }
        }
        
        private List<Item> getAllItems() {
            List<Item> result = new LinkedList<SpacesPanel.Item>();
            DefaultMutableTreeNode root = (DefaultMutableTreeNode) panel.model.getRoot();
            Enumeration children = root.depthFirstEnumeration();
            while( children.hasMoreElements() ) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
                Object o = node.getUserObject();
                if (o instanceof Item) {
                    Item item = (Item) o;
                    if ( item.items == null || item.items.length == 0 ) {
                        result.add( item );
                    }
                }
            }
            return result;
        }
    }
}
