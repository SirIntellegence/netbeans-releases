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

import java.awt.Component;

import org.netbeans.modules.editor.java.JavaKit;
import org.netbeans.modules.editor.options.BaseOptions;
import org.netbeans.modules.editor.options.JavaOptions;
import org.netbeans.modules.java.settings.JavaSettings;

import org.netbeans.jellytools.MainWindowOperator;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jellytools.EditorWindowOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jellytools.actions.OpenAction;

import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.jemmy.ComponentChooser;

/**
 * Test of java completion in opened source editor.
 *
 * @author  anebuzelsky@netbeans.org, mmirilovic@netbeans.org
 */
public class JavaCompletionInEditor extends testUtilities.PerformanceTestCase {
    
    /** Creates a new instance of JavaCompletionInEditor */
    public JavaCompletionInEditor(String testName) {
        super(testName);
        expectedTime = WINDOW_OPEN;
        WAIT_AFTER_OPEN = 1;
    }
    
    /** Creates a new instance of JavaCompletionInEditor */
    public JavaCompletionInEditor(String testName, String performanceDataName) {
        super(testName, performanceDataName);
        expectedTime = WINDOW_OPEN;
    }
    
    private EditorOperator editorOperator;
    private int completionAutoPopupDelay;
    private boolean javaDocAutoPopup;
    private int parsingErrors;
    
    protected void turnOff() {
        // turn off the code completion popup delay and turn off the javadoc popup completely
        Class kitClass = JavaKit.class;
        BaseOptions options = BaseOptions.getOptions (kitClass);
        if (options instanceof JavaOptions) {
            completionAutoPopupDelay = ((JavaOptions)options).getCompletionAutoPopupDelay();
            ((JavaOptions)options).setCompletionAutoPopupDelay(0);
            javaDocAutoPopup = ((JavaOptions)options).getJavaDocAutoPopup();
            ((JavaOptions)options).setJavaDocAutoPopup(false);
        }
        // turn off the error hightlighting feature
        parsingErrors = JavaSettings.getDefault().getParsingErrors();
        JavaSettings.getDefault().setParsingErrors(0);        
    }
    
    protected void turnBack() {
        // set the modified properties back to default
        Class kitClass = JavaKit.class;
        BaseOptions options = BaseOptions.getOptions (kitClass);
        if (options instanceof JavaOptions) {
            ((JavaOptions)options).setCompletionAutoPopupDelay(completionAutoPopupDelay);
            ((JavaOptions)options).setJavaDocAutoPopup(javaDocAutoPopup);
        }
        JavaSettings.getDefault().setParsingErrors(parsingErrors);        
    }
    
    protected void initialize() {
        // open a java file in the editor
        new OpenAction().performAPI(new Node(new ProjectsTabOperator().getProjectRootNode("PerformanceTestData"), gui.Utilities.SOURCE_PACKAGES + "|org.netbeans.test.performance|Main.java"));
        editorOperator = new EditorWindowOperator().getEditor("Main.java");
        turnOff();
    }
    
    public void prepare() {
        /*
        import org.netbeans.editor.BaseCaret;
        
        javax.swing.text.Caret thecaret = editorOperator.txtEditorPane().getCaret();
        if (thecaret instanceof BaseCaret)
        {
            BaseCaret thebasecaret = (BaseCaret) thecaret;
            setAreaToFilter (thebasecaret.x-20, thebasecaret.y-20, thebasecaret.width+40, thebasecaret.height+40);
        }
        */
        addClassNameToLookFor("JDCPopupPanel");
        setPrintClassNames (true);
        setJavaEditorCaretFilteringOn();

        // scroll to the place where we start
        editorOperator.activateWindow();
        editorOperator.setCaretPositionToLine(18);
        // insert the initial text
        editorOperator.insert("{ System");
        // wait
        waitNoEvent(1000);
   }
    
    public ComponentOperator open(){
        // invoke the completion dialog
        editorOperator.pushKey('.');
        // wait for the completion window
        return new ComponentOperator(MainWindowOperator.getDefault(), new CodeCompletionSubchooser());
    }
    
    protected void shutdown() {
        turnBack();
        editorOperator.closeDiscard();
        super.shutdown();
    }
    

    /** SubChooser to determine codecompletion panel
     */
    private static final class CodeCompletionSubchooser implements ComponentChooser {

        public boolean checkComponent(Component comp) {
            return (comp instanceof org.netbeans.editor.ext.JDCPopupPanel);
        }

        public String getDescription() {
            return "org.netbeans.editor.ext.JDCPopupPanel";
        }
    }
}