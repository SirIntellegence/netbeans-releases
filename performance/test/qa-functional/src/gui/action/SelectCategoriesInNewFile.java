/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2004 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package gui.action;

import org.netbeans.jellytools.NewFileWizardOperator;

import org.netbeans.jemmy.operators.ComponentOperator;


/**
 * Test of expanding nodes in the New File Wizard tree.
 *
 * @author  mmirilovic@netbeans.org
 */
public class SelectCategoriesInNewFile extends org.netbeans.performance.test.utilities.PerformanceTestCase {
    
    /** Category name */
    private static String category;
    
    /** Jelly Operator for New Wizard */
    private static NewFileWizardOperator newFile;
    
    /**
     * Creates a new instance of SelectCategoriesInNewFile
     * @param testName the name of the test
     */
    public SelectCategoriesInNewFile(String testName) {
        super(testName);
        expectedTime = WINDOW_OPEN;
    }
    
    /**
     * Creates a new instance of SelectCategoriesInNewFile
     * @param testName the name of the test
     * @param performanceDataName measured values will be saved under this name
     */
    public SelectCategoriesInNewFile(String testName, String performanceDataName) {
        super(testName, performanceDataName);
        expectedTime = WINDOW_OPEN;
    }
    
    
    public void testSelectGUIForms(){
        testCategory("org.netbeans.modules.form.resources.Bundle", "Templates/GUIForms");
    }
    
    public void testSelectXML(){
        testCategory("org.netbeans.api.xml.resources.Bundle", "Templates/XML");
    }
    
    public void testSelectOther(){
        testCategory("org.netbeans.modules.favorites.Bundle", "Templates/Other");
    }
    
    
    protected void testCategory(String bundle, String key) {
        category = org.netbeans.jellytools.Bundle.getStringTrimmed(bundle,key);
        doMeasurement();
    }
   
    protected void initialize(){
    }
    
    public void prepare(){
        newFile = NewFileWizardOperator.invoke();
    }
    
    public ComponentOperator open(){
        newFile.selectCategory(category);
        return null;
    }
    
    public void close(){
        newFile.cancel();        
    }
    
}
