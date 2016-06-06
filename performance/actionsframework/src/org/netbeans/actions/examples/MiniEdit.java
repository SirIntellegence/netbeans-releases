/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2004, 2016 Oracle and/or its affiliates. All rights reserved.
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
 */
/*
 * MiniEdit.java
 *
 * Created on January 25, 2004, 7:55 PM
 */

package org.netbeans.actions.examples;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.UndoManager;
import org.netbeans.actions.api.ContextProvider;
import org.netbeans.actions.api.Engine;
import org.netbeans.actions.spi.ProxyContextProvider;
import org.netbeans.actions.simple.SimpleEngine;
import org.netbeans.actions.spi.ContextProviderSupport;
import org.openide.util.Utilities;
import org.openide.util.enum.AlterEnumeration;
import org.openide.util.enum.ArrayEnumeration;

/**
 *
 * @author  tim
 */
public class MiniEdit extends javax.swing.JFrame implements ContextProvider, FocusListener, MouseListener {
    private Engine engine;
    /** Creates new form MiniEdit */
    public MiniEdit() {
        initComponents();
        
        //Hopefully avoid the floppy drive on Windows by setting index to 1.
        int rootIdx = Utilities.isWindows() ? 1 : 0;
        //Aim our file tree at some filesystem root
        fileTree.setModel(new FileTreeModel(
            File.listRoots()[rootIdx]));

        //Get a URL for our actions definition URL file
        URL url = MiniEdit.class.getClassLoader().getResource(
            "org/netbeans/actions/examples/actiondefs.xml");
        
        //Get a resource bundle with localized names for our actions
        ResourceBundle bundle = ResourceBundle.getBundle(
            "org/netbeans/actions/examples/actions");
        
        //Pass the XML file and the resource bundle to create an instance of
        //Engine which can create menus, toolbars, keymaps, etc., from it
        engine = SimpleEngine.createEngine(url, bundle);
        engine.setContextProvider(this);
        
        //Install the menu bar the engine supplies
        setJMenuBar(engine.createMenuBar());
        
        //Get an array of toolbars to install
        JToolBar[] toolbars = engine.createToolbars();

        //Install the toolbars
        for (int i=0; i < toolbars.length; i++) {
            toolbars[i].setFloatable(false);
            toolbarPanel.add(toolbars[i]);
        }

        //Very simply set up keyboard mappings for the entire app window from
        //those defined in the engine's XML file
        getRootPane().setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, 
            engine.createInputMap(getRootPane()));
        getRootPane().setActionMap(engine.createActionMap());
        
        
        //Add focus listeners that will advise the engine to check enablement
        //of actions
        toolbarPanel.setLayout (new FlowLayout());
        fileTree.setLargeModel(true);
        fileTree.addFocusListener(this);
        toolbarPanel.addFocusListener(this);
        //Misc ui stuff
        fileTree.setAutoscrolls(true);
        fileTree.addMouseListener(this);
        pack();
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        jSplitPane1 = new javax.swing.JSplitPane();
        documentTabs = new javax.swing.JTabbedPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        fileTree = new javax.swing.JTree();
        toolbarPanel = new javax.swing.JPanel();

        setTitle("MiniEdit");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        documentTabs.setMinimumSize(new java.awt.Dimension(400, 200));
        jSplitPane1.setRightComponent(documentTabs);

        jScrollPane1.setMinimumSize(new java.awt.Dimension(200, 200));
        fileTree.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                fileTreeKeyReleased(evt);
            }
        });
        fileTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fileTreeMouseClicked(evt);
            }
        });
        fileTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                fileTreeValueChanged(evt);
            }
        });

        jScrollPane1.setViewportView(fileTree);

        jSplitPane1.setLeftComponent(jScrollPane1);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(toolbarPanel, java.awt.BorderLayout.NORTH);

    }//GEN-END:initComponents

    private void fileTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_fileTreeValueChanged
        engine.recommendUpdate();
        
    }//GEN-LAST:event_fileTreeValueChanged

    private void fileTreeKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_fileTreeKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            open();
        }
    }//GEN-LAST:event_fileTreeKeyReleased

    private void fileTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fileTreeMouseClicked
        if (evt.getClickCount() == 2  && !evt.isPopupTrigger()) {
            open();
        }
    }//GEN-LAST:event_fileTreeMouseClicked
    
    /** Method listed as the invoker for the open action in the XML file */
    public void open() {
        Enumeration files = getSelectedFilesEnumeration();
        while (files.hasMoreElements()) {
            openFile((File) files.nextElement());
        }
    }
    
    public void delete() {
        Enumeration files = getSelectedFilesEnumeration();
        StringBuffer sb = new StringBuffer("Really delete ");
        while (files.hasMoreElements()) {
            sb.append(files.nextElement().toString());
            if (files.hasMoreElements()) {
                sb.append (", ");
            }
        }
        sb.append ("?");
        int opt = JOptionPane.showConfirmDialog(this, sb.toString(), "Delete", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            files = getSelectedFilesEnumeration();
            while (files.hasMoreElements()) {
                File f = (File) files.nextElement();
                f.delete();
            }
        }
        
    }
    
    /** Utility method to open a file */
    private void openFile(File file) {
        Doc doc = (Doc) openDocs.get(file);
        if (doc == null) {
            doc = new Doc(file);
            JTextPane pane = doc.getTextPane();
            if (pane != null) {
                //otherwise there was an exception
                pane.addFocusListener(this);
                synchronized (getTreeLock()) {
                    Component c = documentTabs.add(new JScrollPane(pane));
                    openDocs.put(file, doc);
                    documentTabs.setTitleAt(documentTabs.getTabCount()-1, doc.getTitle());
                    documentTabs.setToolTipTextAt(documentTabs.getTabCount()-1, file.toString());
                    pane.requestFocus();
                    documentTabs.setSelectedComponent(c);
                }
            }
        } else {
            doc.getTextPane().requestFocus();
        }
    }
    
    /** Method invoked by the declarative action "new" */
    public void newFile() {
        openFile (new NewFile());
    }
    
    /** A map of the open documents */
    private HashMap openDocs = new HashMap();
    
    /** Wraps a file, its associated editor component and undo support for it */
    public class Doc implements UndoableEditListener, DocumentListener, ContextProvider, CaretListener {
        private JTextPane pane = null;
        private File file;
        private boolean modified = false;
        public Doc (File file) {
            this.file = file;
        }
        
        public JTextPane getTextPane() {
            try {
            if (pane == null) {
                pane = new JTextPane();
                if (!isNewFile()) {
                    pane.read(new FileInputStream(file), "foo");
                }
                pane.getDocument().addUndoableEditListener(this);
                pane.getDocument().addDocumentListener(this);
                pane.addCaretListener(this);
                pane.putClientProperty("doc", this);
                pane.addMouseListener(MiniEdit.this);
            }
            return pane;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        /** Invoker for the revert action */
        public void revert() {
            if (pane == null) {
                return;
            }
            try {
                pane.read (new FileInputStream(file), "foo");
                setModified (false);
                if (um != null) {
                    um.discardAllEdits();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        /** Supplies a title including a * if the file is modified */
        public String getTitle () {
            String result = file.getName();
            if (isModified()) {
                result = result + "*";
            }
            return result;
        }
        
        /** Check if the current file is modified */
        public boolean isModified() {
            boolean result = modified;
            if (result & um != null) {
                if (!um.canUndo()) {
                    return false;
                }
            }
            return result;
        }
        
        /** Invoker for the saveas action */
        public void saveAs() {
            Doc doc = getSelectedDoc();
            if (doc == null) {
                throw new NullPointerException ("no doc");
            }
            
        
            FileDialog fd = new FileDialog (MiniEdit.this, "Save as");
            fd.setMode(fd.SAVE);
            fd.show();
            if (fd.getFile() != null) {
                File nue = new File (fd.getDirectory() + fd.getFile());
                try {
                    boolean success = nue.createNewFile();
                    if (success) {
                        FileWriter w = new FileWriter (nue);
                        doc.getTextPane().write(w);
                        file = nue;
                        documentTabs.setTitleAt(documentTabs.getSelectedIndex(), nue.getName());
                        documentTabs.setToolTipTextAt(documentTabs.getSelectedIndex(), nue.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        public boolean isNewFile () {
            return file instanceof NewFile;
        }
        
        /** Invoked method by the save action */
        public void save() {
            if (isNewFile()) {
                saveAs();
                return;
            }
            try {
                Writer w = new FileWriter (file);
                pane.write(w);
                modified = false;
                documentTabs.setTitleAt(documentTabs.getSelectedIndex(), getTitle());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } 
        
        UndoManager um = null;
        private UndoManager getUndoSupport() {
            if (um == null) {
                um = new UndoManager();
                um.setLimit(50);
            }
            return um;
        }
        
        public boolean canUndo() {
            if (um != null) {
                return um.canUndo();
            }
            return false;
        }
        
        public boolean canRedo() {
            if (um != null) {
                return um.canRedo();
            }
            return false;
        }
        
        public boolean canUndoOrRedo() {
            if (um != null) {
                return um.canUndoOrRedo();
            }
            return false;
        }
        
        public void undo() {
            if (um != null) {
                try {
                    um.undo();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void redo() {
            if (um != null) {
                try {
                    um.undo();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void setModified (boolean val) {
            if (modified != val) {
                modified = val;
                if (pane != null) {
                    documentTabs.setTitleAt(documentTabs.getSelectedIndex(), getTitle());
                }
                engine.recommendUpdate();
            }
        }
        
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            setModified(true);
        }
        
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            setModified(true);
        }
        
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            setModified(true);
        }
        
        public void undoableEditHappened(javax.swing.event.UndoableEditEvent e) {
            e.getEdit();
            getUndoSupport().addEdit(e.getEdit());
        }
        
        public Map getContext() {
            HashMap result = new HashMap();
            result.put (getClass(), this);
            if (isModified()) {
                result.put ("save", Boolean.TRUE);
                result.put ("modified", Boolean.TRUE);
            }
            if (isNewFile()) {
                result.put ("isNewFile", Boolean.TRUE);
            }
            if (canUndo()) {
                result.put ("undo", Boolean.TRUE);
            }
            if (canRedo()) {
                result.put ("redo", Boolean.TRUE);
            }
            result.put ("doc", Boolean.TRUE);
            
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
            if (documentTabs.isAncestorOf(owner)) {
                if (canPaste()) {
                    result.put ("clipboardcontents", clipboard);
                }
                if (hasSelection()) {
                    result.put ("selection", Boolean.TRUE);
                }
            }
            return result;
        }
        
        public boolean hadSel = false;
        public void caretUpdate(javax.swing.event.CaretEvent e) {
            boolean hasSel = e.getDot() != e.getMark();
            if (hasSel != hadSel) {
                engine.recommendUpdate();
            }
            hadSel = hasSel;
        }
        
    }
    
    private Doc getSelectedDoc() {
        JComponent comp = (JComponent) documentTabs.getSelectedComponent();
        Doc result = null;
        if (comp != null) {
            JTextPane jtp = (JTextPane) ((JScrollPane) comp).getViewport().getView();
            
            result = (Doc) jtp.getClientProperty("doc");
        }

        return result;
    }
    
    private Doc[] getModifiedDocs() {
        ArrayList result = new ArrayList();
        Iterator i = openDocs.keySet().iterator();
        while (i.hasNext()) {
            Doc doc = (Doc) openDocs.get(i.next());
            if (doc.isModified()) {
                result.add(doc);
            }
        }
        Doc[] docs = new Doc[result.size()];
        docs = (Doc[]) result.toArray(docs);
        return docs;
    }
    
    public void saveAll() {
        Doc[] docs = getModifiedDocs();
        for (int i=0; i < docs.length; i++) {
            docs[i].save();
        }
    }
    
    private Enumeration getSelectedFilesEnumeration() {
        TreePath paths[] = fileTree.getSelectionPaths();
        if (paths != null) {
            Enumeration files = new PathFileEnumeration (
                new ArrayEnumeration(fileTree.getSelectionPaths()));
            return files;
        } else {
            return new ArrayEnumeration(new TreePath[0]);
        }
    }
    
    private class PathFileEnumeration extends AlterEnumeration {
        public PathFileEnumeration (Enumeration en) {
            super (en);
        }
        
        protected Object alter(Object o) {
            TreePath path = (TreePath) o;
            File f = (File) path.getLastPathComponent();
            return f;
        }
    }
    
    private String clipboard = null;
    public void cut() {
        if (doCopy()) {
            Doc doc = getSelectedDoc();
            JTextPane pane = doc.getTextPane();
            int start = pane.getSelectionStart();
            int end = pane.getSelectionEnd();
            if (start == end) {
                throw new IllegalStateException ("no selection");
            }
            try {
                pane.getDocument().remove(start, end-start);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new NullPointerException ("no document");
        }
    }
    
    private boolean hasSelection() {
        Doc doc = getSelectedDoc();
        if (doc == null) {
            return false;
        }
        JTextPane pane = doc.getTextPane();
        return pane.getSelectionStart() != pane.getSelectionEnd();
    }
    
    public boolean canPaste() {
        return clipboard != null;
    }
    
    public void paste() {
        Doc doc = getSelectedDoc();
        if (doc != null) {
            JTextPane pane = doc.getTextPane();
            int start = pane.getSelectionStart();
            if (start == -1) {
                throw new IllegalStateException("no selection");
            }
            try {
            int end = pane.getSelectionEnd();
            if (start != end) {
                pane.getDocument().remove(start, end-start); 
            }
            pane.getDocument().insertString(start, clipboard, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new NullPointerException ("no document");
        }
    }
    
    public void copy() {
        if (!doCopy()) {
            throw new NullPointerException ("no document");
        }
    }
    
    private boolean doCopy() {
        Doc doc = getSelectedDoc();
        if (doc != null) {
            JTextPane pane = doc.getTextPane();
            clipboard = pane.getSelectedText();
            return true;
        } else {
            return false;
        }
    }
    
    /** Exit the Application */
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        System.exit(0);
    }//GEN-LAST:event_exitForm
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        System.setProperty ("apple.laf.useScreenMenuBar", "true");
        new MiniEdit().show();
    }
    
    public void exit() {
        System.exit(0);
    }
    
    public Map getContext() {
        HashMap result = new HashMap();
        result.put (getClass(), this);
        Doc[] mods = getModifiedDocs();
        if (mods.length > 1) {
            result.put ("anymodified", Boolean.TRUE);
        }
        
        Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        result.put ("activecomponent", fileTree.hasFocus() ? "filetree" : "editor");
        
        if (owner == fileTree || fileTree.isAncestorOf(owner)) {
            Enumeration en = getSelectedFilesEnumeration();
            if (en != null && en.hasMoreElements()) {
                boolean fileSelected = false;
                boolean dirSelected = false;
                boolean allFilesOpen = true;
                while (en.hasMoreElements()) {
                    File f = (File) en.nextElement();
                    fileSelected |= !f.isDirectory();
                    dirSelected |= f.isDirectory();
                    allFilesOpen &= openDocs.keySet().contains(f);
                }
                if (fileSelected) {
                    result.put("fileSelected", Boolean.TRUE);
                }
                if (!allFilesOpen && fileSelected) {
                    result.put ("closedFileSelected", Boolean.TRUE);
                }
                if (dirSelected) {
                    result.put("dirSelected", Boolean.TRUE);
                }
            }
        }
        
        Doc doc = getSelectedDoc();
        if (doc != null) {
            result.put ("close", Boolean.TRUE);
            result.putAll(doc.getContext());
        }
        return result;
    }    
    
    public void close() {
        Doc doc = getSelectedDoc();
        if (doc != null) {
            if (doc.isModified()) {
                /*
                int opt = JOptionPane.showOptionDialog(this, 
                    "Document is modified. Save?", "Unsaved changes", null,
                    //JOptionPane.YES_NO_CANCEL_OPTION,  
                    JOptionPane.QUESTION_MESSAGE, (Icon) null, 
                    new String[] {"OK", "Cancel"}, 
                    "Close");
                 */
                int opt = JOptionPane.showConfirmDialog(this, 
                    "Document is modified. Save?", "Unsaved changes",
                    JOptionPane.YES_NO_CANCEL_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);
                if (opt == JOptionPane.CANCEL_OPTION) {
                    return;
                }
                if (opt == JOptionPane.YES_OPTION) {
                    doc.save();
                }
            }
            documentTabs.remove(doc.getTextPane().getParent().getParent());
            openDocs.remove(doc);
        }
    }
    
    public void focusGained(java.awt.event.FocusEvent e) {
        System.err.println("Telling engine to update");
        engine.recommendUpdate();
    }
    
    public void focusLost(java.awt.event.FocusEvent e) {
    }
    
    public Map getContribution() {
        return getContext();
    }
    
    public void mouseClicked(java.awt.event.MouseEvent e) {
    }
    
    public void mouseEntered(java.awt.event.MouseEvent e) {
    }
    
    public void mouseExited(java.awt.event.MouseEvent e) {
    }
    
    public void mousePressed(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
            JPopupMenu menu = engine.createPopupMenu();
            menu.show((Component) e.getSource(), e.getX(), e.getY());
        }
    }
    
    public void mouseReleased(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
            JPopupMenu menu = engine.createPopupMenu();
            menu.show((Component) e.getSource(), e.getX(), e.getY());
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane documentTabs;
    private javax.swing.JTree fileTree;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JPanel toolbarPanel;
    // End of variables declaration//GEN-END:variables
    
    private static class FileTreeModel implements TreeModel {
        private File root;
        public FileTreeModel (File root) {
            this.root = root;
        }
        
        public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
            //do nothing
        }
        
        public Object getChild(Object parent, int index) {
            File f = (File) parent;
            return f.listFiles()[index];
        }
        
        public int getChildCount(Object parent) {
            File f = (File) parent;
            if (!f.isDirectory()) {
                return 0;
            } else {
                return f.list().length;
            }
        }
        
        public int getIndexOfChild(Object parent, Object child) {
            File par = (File) parent;
            File ch = (File) child;
            return Arrays.asList(par.listFiles()).indexOf(ch);
        }
        
        public Object getRoot() {
            return root;
        }
        
        public boolean isLeaf(Object node) {
            File f = (File) node;
            return !f.isDirectory();
        }
        
        public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
            //do nothing
        }
        
        public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
            //do nothing
        }
    }
    
    private int newCount = 0;
    private class NewFile extends File {
        int index;
        public NewFile () {
            super ("Untitled " + (newCount++));
            index = newCount;
        }
        
        public String getName() {
            return "Untitled " + index;
        }
    }
}
