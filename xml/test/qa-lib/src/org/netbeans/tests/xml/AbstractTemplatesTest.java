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
package org.netbeans.tests.xml;

import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jellytools.EditorWindowOperator;
import org.netbeans.jellytools.NewWizardOperator;
import org.netbeans.modules.css.CSSObject;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;

/**
 * <P>
 * <P>
 * <FONT COLOR="#CC3333" FACE="Courier New, Monospaced" SIZE="+1">
 * <B>
 * <BR> XML Module Jemmy Test: NewFromTemplate
 * </B>
 * </FONT>
 * <BR><BR><B>What it tests:</B><BR>
 *
 * This test tests New From Template action on all XML's templates.
 *
 * <BR><BR><B>How it works:</B><BR>
 *
 * 1) create new documents from template<BR>
 * 2) write the created documents to output<BR>
 * 3) close source editor<BR>
 *
 * <BR><BR><B>Settings:</B><BR>
 * none<BR>
 *
 * <BR><BR><B>Output (Golden file):</B><BR>
 * Set XML documents.<BR>
 *
 * <BR><B>To Do:</B><BR>
 * none<BR>
 *
 * <P>Created on Januar 09, 2001, 12:33 PM
 * <P>
 */

public abstract class AbstractTemplatesTest extends JXTest {
    /** Creates new TemplatesTest */
    public AbstractTemplatesTest(String testName) {
        super(testName);
    }
    
    // ABSTRACT ////////////////////////////////////////////////////////////////
    
    /**
     *  Returns Sring array with tested templates. The array have to this format:
     *  <code>String [][} {{"Template Name (localized)",  "file extension"}, {...}}</code>
     */
    protected abstract String[][] getTemplateList();
    
    /** Should return TestUtil from Test's package */
    protected abstract AbstractTestUtil testUtil();
    
    // TESTS ///////////////////////////////////////////////////////////////////
    
    public void testNewFromTemplate() throws Exception {
        String templates[][] = getTemplateList();
        
        //FolderNode folder = new FolderNode(findDataNode("templates"), "");
        String folder = getFilesystemName() + DELIM + getDataPackageName(DELIM) + DELIM + "templates";
        // remove old files
        for (int i = 0; i < templates.length; i++) {
            String name = templates[i][0];
            String ext = templates[i][1];
            System.out.println("templates/" + name + "." + ext);
            DataObject dao = testUtil().findData("templates/" + name + "." + ext);
            System.out.println(dao);
            if (dao != null) dao.delete();
            NewWizardOperator.create("XML" + DELIM + name, folder, name);
            new EditorOperator(name);
        }
        
        // write the created documents to output
        for (int i = 0; i < templates.length; i++) {
            String name = templates[i][0];
            String ext = templates[i][1];
            DataObject dataObject = testUtil().findData("templates/" + name + "." + ext);
            ref("\n+++ Document: " + dataObject.getName());
            
            String str = testUtil().dataObjectToString(dataObject);
            if (dataObject instanceof CSSObject) {
                str = testUtil().replaceString(str, "/*", "*/", "/* REMOVED */");
            } else {
                str = testUtil().replaceString(str, "<!--", "-->", "<!-- REMOVED -->");
            }
            ref(str);
        }
        // close source editor
        new EditorWindowOperator().closeDiscard();
        compareReferenceFiles();
    }
}
