/*
 *                         Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License"). You may not use this file except in compliance with 
 * the License. A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the Ant module
 * The Initial Developer of the Original Code is Jayme C. Edwards.
 * Portions created by Jayme C. Edwards are Copyright (c) 2000.
 * All Rights Reserved.
 *
 * Contributor(s): Jesse Glick.
 */

package org.apache.tools.ant.module.wizards.shortcut;

import java.awt.Component;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.loaders.TemplateWizard;
import javax.swing.KeyStroke;
import org.openide.util.Utilities;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class SelectKeyboardShortcutPanel extends javax.swing.JPanel implements WizardDescriptor.Panel, KeyListener {

    private KeyStroke stroke = null;
    
    /** Create the wizard panel and set up some basic properties. */
    public SelectKeyboardShortcutPanel () {
        initComponents ();
        // Provide a name in the title bar.
        setName (NbBundle.getMessage (SelectKeyboardShortcutPanel.class, "SKSP_LBL_select_shortcut_to_add"));
        testField.addKeyListener (this);
    }

    // --- VISUAL DESIGN OF PANEL ---

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents () {//GEN-BEGIN:initComponents
        hintsArea = new javax.swing.JTextArea ();
        mainPanel = new javax.swing.JPanel ();
        testField = new javax.swing.JTextField ();
        
        setLayout (new java.awt.BorderLayout ());
        
        hintsArea.setWrapStyleWord (true);
        hintsArea.setLineWrap (true);
        hintsArea.setEditable (false);
        hintsArea.setForeground (new java.awt.Color (102, 102, 153));
        hintsArea.setText (NbBundle.getMessage (SelectKeyboardShortcutPanel.class, "SKSP_TEXT_press_any_key_seq"));
        hintsArea.setBackground (new java.awt.Color (204, 204, 204));
        add (hintsArea, java.awt.BorderLayout.NORTH);
        
        testField.setColumns (15);
        testField.setText (NbBundle.getMessage (SelectKeyboardShortcutPanel.class, "SKSP_LBL_type_here"));
        testField.setHorizontalAlignment (javax.swing.JTextField.CENTER);
        mainPanel.add (testField);
        
        add (mainPanel, java.awt.BorderLayout.CENTER);
        
    }//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea hintsArea;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JTextField testField;
    // End of variables declaration//GEN-END:variables

    // --- WizardDescriptor.Panel METHODS ---

    // Get the visual component for the panel. In this template, the same class
    // serves as the component and the Panel interface, but you could keep
    // them separate if you wished.
    public Component getComponent () {
        return this;
    }

    public HelpCtx getHelp () {
        return HelpCtx.DEFAULT_HELP; // XXX
    }

    public boolean isValid () {
        return stroke != null;
    }

    private final Set listeners = new HashSet (1); // Set<ChangeListener>
    public final void addChangeListener (ChangeListener l) {
        synchronized (listeners) {
            listeners.add (l);
        }
    }
    public final void removeChangeListener (ChangeListener l) {
        synchronized (listeners) {
            listeners.remove (l);
        }
    }
    protected final void fireChangeEvent () {
        Iterator it;
        synchronized (listeners) {
            it = new HashSet (listeners).iterator ();
        }
        ChangeEvent ev = new ChangeEvent (this);
        while (it.hasNext ()) {
            ((ChangeListener) it.next ()).stateChanged (ev);
        }
    }

    public void readSettings (Object settings) {
        // XXX later...
    }
    public void storeSettings (Object settings) {
        TemplateWizard wiz = (TemplateWizard) settings;
        wiz.putProperty (ShortcutIterator.PROP_STROKE, stroke);
    }
    
    // KeyListener:

    public void keyPressed (KeyEvent e) {
        // XXX ideally make TAB switch focus, rather than be handled...
        stroke = KeyStroke.getKeyStroke (e.getKeyCode (), e.getModifiers ());
        testField.setText (Utilities.keyToString (stroke));
        fireChangeEvent ();
        e.consume ();
    }
    public void keyReleased (KeyEvent e) {
        e.consume ();
    }
    public void keyTyped (KeyEvent e) {
        e.consume ();
    }
    
}
