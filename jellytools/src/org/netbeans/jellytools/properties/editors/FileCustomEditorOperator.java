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
 * FileCustomEditorOperator.java
 *
 * Created on June 13, 2002, 4:01 PM
 */

import java.io.File;
import javax.swing.JDialog;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jemmy.operators.*;

/** Class implementing all necessary methods for handling File Custom Editor
 * @author <a href="mailto:adam.sotona@sun.com">Adam Sotona</a>
 * @version 1.0 */
public class FileCustomEditorOperator extends NbDialogOperator {
    
    private JFileChooserOperator _fileChooser=null;
    
    /** Creates a new instance of FileCustomEditorOperator
     * @param title String title of custom editor */
    public FileCustomEditorOperator(String title) {
        super(title);
    }
    
    /** Creates a new instance of FileCustomEditorOperator
     * @param wrapper JDialogOperator wrapper for custom editor */    
    public FileCustomEditorOperator(JDialogOperator wrapper) {
        super((JDialog)wrapper.getSource());
    }

    /** getter for JFileChooserOperator
     * @return JFileChooserOperator */    
    public JFileChooserOperator fileChooser() {
        if (_fileChooser==null) {
            _fileChooser=new JFileChooserOperator(this);
        }
        return _fileChooser;
    }
    
    /** returns edited file
     * @return File */    
    public File getFileValue() {
        return fileChooser().getSelectedFile();
    }
    
    /** sets edited file
     * @param file File */    
    public void setFileValue(File file) {
        fileChooser().setSelectedFile(file);
    }
    
    /** sets edited file
     * @param fileName String file name */    
    public void setFileValue(String fileName) {
        setFileValue(new File(fileName));
    }
}
