/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.web.debug;

import java.io.File;
import java.io.IOException;
import junit.framework.Test;
import org.netbeans.jellytools.Bundle;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jellytools.MainWindowOperator;
import org.netbeans.jellytools.NbDialogOperator;
import org.netbeans.jellytools.OutputTabOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.actions.ActionNoBlock;
import org.netbeans.jellytools.actions.OpenAction;
import org.netbeans.jellytools.modules.debugger.actions.ApplyCodeChangesAction;
import org.netbeans.jellytools.modules.debugger.actions.ContinueAction;
import org.netbeans.jellytools.modules.debugger.actions.StepIntoAction;
import org.netbeans.jellytools.modules.debugger.actions.StepOutAction;
import org.netbeans.jellytools.modules.debugger.actions.StepOverAction;
import org.netbeans.jellytools.modules.j2ee.J2eeTestCase;
import org.netbeans.jellytools.modules.j2ee.nodes.J2eeServerNode;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jellytools.nodes.SourcePackagesNode;
import org.netbeans.junit.NbModuleSuite;

/** Test of web application debugging. Manual test specification is here:
 * http://wiki.netbeans.org/TS_70_WebEnterpriseDebug
 *
 * @author Jiri Skrivanek
 */
public class ServletDebuggingTest extends J2eeTestCase {

    /** Status bar tracer used to wait for state. */
    private MainWindowOperator.StatusTextTracer stt;
    // name of sample web application project
    private static final String SAMPLE_WEB_PROJECT_NAME = "MainTestApplication";  //NOI18N
    // line number of breakpoint
    private static int line;
    // servlet node in Projects view
    private Node servletNode;
    
    public ServletDebuggingTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return NbModuleSuite.create(addServerTests(Server.GLASSFISH, NbModuleSuite.createConfiguration(ServletDebuggingTest.class),
                "testSetBreakpoint",
                "testDebugProject",
                "testStepInto",
                "testStepOut",
                "testStepOver",
                "testApplyCodeChanges",
                "testStopServer").
                enableModules(".*").clusters(".*"));
    }

    /** Print test name and initialize status bar tracer. */
    @Override
    public void setUp() throws IOException {
        System.out.println("########  " + getName() + "  #######");
        stt = MainWindowOperator.getDefault().getStatusTextTracer();
        // start to track Main Window status bar
        stt.start();
        // increase timeout to 60 seconds when waiting for status bar text
        MainWindowOperator.getDefault().getTimeouts().setTimeout("Waiter.WaitingTime", 60000);
        // find servlet node in Projects view
        openProjects(new File(getDataDir(), SAMPLE_WEB_PROJECT_NAME).getAbsolutePath());
        waitScanFinished();
        servletNode = new Node(new SourcePackagesNode(SAMPLE_WEB_PROJECT_NAME),
                "org.netbeans.test.servlets|DivideServlet.java"); //NOI18N
    }

    /** Stops status bar tracer. */
    @Override
    public void tearDown() {
        stt.stop();
    }

    /** Set breakpoint.
     * - open Source Packages|org.netbeans.test.servlets|DivideServlet.java
     * - select <h1> in editor
     * - toggle breakpoint at selected line
     */
    public void testSetBreakpoint() throws Exception {
        Utils.suppressBrowserOnRun(SAMPLE_WEB_PROJECT_NAME);
        waitScanFinished();
        new OpenAction().performAPI(servletNode);
        // find file in Editor
        EditorOperator eo = new EditorOperator("DivideServlet.java"); // NOI18N
        line = Utils.setBreakpoint(eo, "<h1>"); // NOI18N
    }

    /** Step into in Servlet.
     * - debug project (#199576 - Debug File on servlet doesn't run debugger)
     * - close "Port Selection Notice" dialog
     * - wait until debugger is started
     */
    public void testDebugProject() {
        //Bug 199576 - Debug File on servlet doesn't run debugger 
        Node rootNode = new ProjectsTabOperator().getProjectRootNode(SAMPLE_WEB_PROJECT_NAME);
        rootNode.performPopupActionNoBlock("Debug");
        // close info message "Use 9009 to attach the debugger to the GlassFish Instance"
        new NbDialogOperator("Port Selection Notice").close();
        Utils.waitFinished(this, SAMPLE_WEB_PROJECT_NAME, "debug");
    }
    
    /** Step into in Servlet.
     * - call "Debug File"  popup on servlet's node
     * - wait until debugger stops at previously set breakpoint
     * - call Debug|Step Into from main menu
     * - call Debug|Step Into from main menu to confirm method selection
     * - wait until debugger stops at next line
     * - call Debug|Step Into from main menu again
     * - wait until debugger stops at line in Divider.java
     * - find and close editor tab with Divider.java
     * - continue
     */
    public void testStepInto() {
        debugServlet();
        new StepIntoAction().perform();
        // confirm step into selection
        new StepIntoAction().perform();
        // #201275 - use Debugger Console instead of status bar
        new OutputTabOperator("Debugger Console").waitText("DivideServlet.java:" + (line + 2)); //NOI18N
        //stt.waitText("DivideServlet.java:"+(line+2)); //NOI18N
        new StepIntoAction().perform();
        // org.netbeans.test.freeformlib.Divider should be used in servlet when #199615 is fixed
        stt.waitText("Multiplier.java:"); //NOI18N
        new EditorOperator("Multiplier.java").close(); //NOI18N
        new ContinueAction().perform();
    }

    /** Step out from servlet.
     * - call Debug File popup on servlet's node
     * - wait until debugger stops at previously set breakpoint
     * - call Debug|Step Out from main menu
     * - wait until debugger stops in doGet method
     * - continue
     */
    public void testStepOut() {
        debugServlet();
        stt.clear();
        new StepOutAction().perform();
        // it stops at doGet method
        stt.waitText("DivideServlet.java:"); //NOI18N
        new ContinueAction().perform();
    }

    /** Step over servlet.
     * - call Debug File popup on servlet's node
     * - wait until debugger stops at previously set breakpoint
     * - call Debug|Step Over from main menu
     * - wait until debugger stops at next line
     * - call Debug|Step Over from main menu again
     * - wait until debugger stops at next line
     * - continue
     */
    public void testStepOver() {
        debugServlet();
        new StepOverAction().perform();
        stt.waitText("DivideServlet.java:" + (line + 2)); //NOI18N
        new StepOverAction().perform();
        stt.waitText("DivideServlet.java:" + (line + 4)); //NOI18N
        new ContinueAction().perform();
    }

    /** Apply code changes in servlet.
     * - call Debug File popup on servlet's node
     * - wait until debugger stops at previously set breakpoint
     * - replace "Servlet DIVIDE" by "Servlet DIVIDE Changed" in DivideServlet.java
     * - call Debug|Apply Code Changes from main menu
     * - wait until debugger stops somewhere in DivideServlet.java
     * - finish debugger
     * - open URL connection and wait for changed text
     */
    public void testApplyCodeChanges() {
        debugServlet();
        stt.clear();
        EditorOperator eo = new EditorOperator("DivideServlet.java"); // NOI18N
        eo.replace("Servlet DIVIDE", "Servlet DIVIDE Changed"); //NOI18N
        new ApplyCodeChangesAction().perform();
        Utils.waitFinished(this, SAMPLE_WEB_PROJECT_NAME, "debug-fix");
        stt.waitText("DivideServlet.java:"); //NOI18N
        Utils.finishDebugger();
        Utils.waitText(SAMPLE_WEB_PROJECT_NAME + "/DivideServlet", 240000, "Servlet DIVIDE Changed");
    }

    /** Stop server just for clean-up.
     * - stop server and wait until it finishes
     */
    public void testStopServer() {
        J2eeServerNode serverNode = new J2eeServerNode(Utils.DEFAULT_SERVER);
        JSPDebuggingOverallTest.verifyServerNode(serverNode);
        serverNode.stop();
    }
    
    /**
     * - call Debug File popup on servlet's node
     * - close Set URI dialog
     * - wait until project execution is finished
     * - reload page because browser doesn't open automatically
     * - wait until debugger stops at previously set breakpoint
     */
    private void debugServlet() {
        new ActionNoBlock(null, "Debug File").perform(servletNode);
        String setURITitle = Bundle.getString("org.netbeans.modules.web.project.ui.Bundle", "TTL_setServletExecutionUri");
        new NbDialogOperator(setURITitle).ok();
        Utils.waitFinished(this, SAMPLE_WEB_PROJECT_NAME, "run"); // should be "debug" when #199576 fixed
        // reload page because browser is suppressed
        Utils.reloadPage(SAMPLE_WEB_PROJECT_NAME + "/DivideServlet");
        stt.waitText("DivideServlet.java:" + line); //NOI18N
    }
}
