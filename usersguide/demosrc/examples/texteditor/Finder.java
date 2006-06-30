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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package examples.texteditor;

/** Finder dialog is used to allow the user to search for given string.
 */
public class Finder extends javax.swing.JDialog {

    /** Finder constructor.
     * It creates modal dialog and displays it.
     */
    public Finder(java.awt.Frame parent, javax.swing.JTextArea textEditor) {
        super(parent, true);
        this.textEditor = textEditor;
        initComponents();
        pack();
        setLocationRelativeTo(parent);
        findField.requestFocus();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the FormEditor.
     */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        findPanel = new javax.swing.JPanel();
        findLabel = new javax.swing.JLabel();
        findField = new javax.swing.JTextField();
        buttonPanel = new javax.swing.JPanel();
        findButton = new javax.swing.JButton();
        closeButton = new javax.swing.JButton();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setTitle("Find");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        getAccessibleContext().setAccessibleName("Find Dialog");
        getAccessibleContext().setAccessibleDescription("Find dialog.");
        findPanel.setLayout(new java.awt.GridBagLayout());

        findLabel.setLabelFor(findField);
        findLabel.setText("Find text:");
        findPanel.add(findLabel, new java.awt.GridBagConstraints());
        findLabel.getAccessibleContext().setAccessibleDescription("Find text.");

        findField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findFieldActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        findPanel.add(findField, gridBagConstraints);
        findField.getAccessibleContext().setAccessibleName("Find Field");
        findField.getAccessibleContext().setAccessibleDescription("Find field.");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(11, 12, 0, 11);
        getContentPane().add(findPanel, gridBagConstraints);

        buttonPanel.setLayout(new java.awt.GridBagLayout());

        findButton.setMnemonic('f');
        findButton.setText("Find");
        findButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(findButton, new java.awt.GridBagConstraints());

        closeButton.setMnemonic('c');
        closeButton.setText("Close");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        buttonPanel.add(closeButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(17, 12, 11, 11);
        getContentPane().add(buttonPanel, gridBagConstraints);

    }//GEN-END:initComponents

    /** This method is called when ENTER is pressed in Find text field.
     * If the field contains some text, it invokes Find button action, otherwise does nothing.
     * @param evt ActionEvent instance passed from actionPerformed event.
     */
    private void findFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findFieldActionPerformed
        if (findField.getText().trim().length() > 0)
            findButton.doClick();
    }//GEN-LAST:event_findFieldActionPerformed

    /** This method is called when Find button is pressed.
     * If the field contains some text, it sets the caret position to the searched word, otherwise does nothing.
     * @param evt ActionEvent instance passed from actionPerformed event.
     */
    private void findButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findButtonActionPerformed
        // Add your handling code here:
        String text = textEditor.getText();
        String textToFind = findField.getText();
        if (!"".equals(textToFind)) {
            int index = text.indexOf(textToFind);
            if (index != -1) {
                textEditor.setCaretPosition(index);
                closeDialog(null);
            } else {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }
    }//GEN-LAST:event_findButtonActionPerformed

    /** This method is called when Close button is pressed.
     * It closes the Finder dialog.
     * @param evt ActionEvent instance passed from actionPerformed event.
     */
    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        closeDialog(null);
    }//GEN-LAST:event_closeButtonActionPerformed

    /** This method is called when the dialog is closed.
     * @param evt WindowEvent instance passed from windowClosing event.
     */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        setVisible(false);
        dispose();
    }//GEN-LAST:event_closeDialog


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JButton findButton;
    private javax.swing.JTextField findField;
    private javax.swing.JLabel findLabel;
    private javax.swing.JPanel findPanel;
    // End of variables declaration//GEN-END:variables

    private javax.swing.JTextArea textEditor;

}
