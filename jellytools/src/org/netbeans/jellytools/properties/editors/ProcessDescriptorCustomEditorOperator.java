/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2002 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.jellytools.properties.editors;

/*
 * ProcessDescriptorCustomEditorOperator.java
 *
 * Created on 6/12/02 5:00 PM
 */

import javax.swing.JDialog;
import org.netbeans.jellytools.Bundle;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jemmy.operators.*;

/** Class implementing all necessary methods for handling Process Descriptor Custom
 * Editor
 * @author <a href="mailto:adam.sotona@sun.com">Adam Sotona</a>
 * @version 1.0 */
public class ProcessDescriptorCustomEditorOperator extends NbDialogOperator {

    /** Creates new ProcessDescriptorCustomEditorOperator
     * @throws TimeoutExpiredException when NbDialog not found
     * @param title String title of custom editor */
    public ProcessDescriptorCustomEditorOperator(String title) {
        super(title);
    }

    /** creates new ProcessDescriptorCustomEditorOperator
     * @param wrapper JDialogOperator wrapper for custom editor */    
    public ProcessDescriptorCustomEditorOperator(JDialogOperator wrapper) {
        super((JDialog)wrapper.getSource());
    }

    private JTextAreaOperator _txtArgumentKey;
    private JButtonOperator _btSelectProcessExecutable;
    private JTextFieldOperator _txtProcess;
    private JTextAreaOperator _txtArguments;

    /** Tries to find null JTextArea in this dialog.
     * @throws TimeoutExpiredException when component not found
     * @return JTextAreaOperator
     */
    public JTextAreaOperator txtArgumentKey() {
        if (_txtArgumentKey==null) {
            _txtArgumentKey = new JTextAreaOperator( this, null, 1 );
        }
        return _txtArgumentKey;
    }

    /** Tries to find "..." JButton in this dialog.
     * @throws TimeoutExpiredException when component not found
     * @return JButtonOperator
     */
    public JButtonOperator btSelectProcessExecutable() {
        if (_btSelectProcessExecutable==null) {
            _btSelectProcessExecutable = new JButtonOperator( this, Bundle.getString("org.netbeans.beaninfo.editors.Bundle", "CTL_NbProcessDescriptorCustomEditor.jButton1.text"), 0 );
        }
        return _btSelectProcessExecutable;
    }

    /** Tries to find null JTextField in this dialog.
     * @throws TimeoutExpiredException when component not found
     * @return JTextFieldOperator
     */
    public JTextFieldOperator txtProcess() {
        if (_txtProcess==null) {
            _txtProcess = new JTextFieldOperator( this, null, 0 );
        }
        return _txtProcess;
    }

    /** Tries to find null JTextArea in this dialog.
     * @throws TimeoutExpiredException when component not found
     * @return JTextAreaOperator
     */
    public JTextAreaOperator txtArguments() {
        if (_txtArguments==null) {
            _txtArguments = new JTextAreaOperator( this, null, 0 );
        }
        return _txtArguments;
    }

    /** getter for Argument Key text
     * @return String text of Argument Key */    
    public String getArgumentKey() {
        return txtArgumentKey().getText();
    }

    /** clicks on "..." JButton
     * @throws TimeoutExpiredException when JButton not found
     * @return FileCustomEditorOperator */
    public FileCustomEditorOperator selectProcessExecutable() {
        btSelectProcessExecutable().pushNoBlock();
        return new FileCustomEditorOperator(Bundle.getString("org.openide.actions.Bundle", "Open"));
    }

    /** getter of edited process text
     * @return String process text */    
    public String getProcess() {
        return txtProcess().getText();
    }

    /** tries to find and set text of txtProcess
     * @param text String text
     */
    public void setProcess( String text ) {
        txtProcess().setText(text);
    }

    /** getter of edited arguments text
     * @return String argumentstext */    
    public String getArguments() {
        return txtArguments().getText();
    }

    /** tries to find and set text of txtArguments
     * @param text String text
     */
    public void setArguments( String text ) {
        txtArguments().setText(text);
    }
}

