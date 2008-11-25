/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.performance.visualweb.actions;

import org.netbeans.performance.visualweb.VWPUtilities;
import org.netbeans.performance.visualweb.WebFormDesignerOperator;
import org.netbeans.performance.visualweb.setup.VisualWebSetup;
import org.netbeans.modules.performance.utilities.PerformanceTestCase;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jellytools.ProjectsTabOperator;
import org.netbeans.jellytools.actions.OpenAction;
import org.netbeans.jellytools.nodes.Node;
import org.netbeans.jemmy.operators.ComponentOperator;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.junit.NbModuleSuite;

/**
 *
 * @author mkhramov@netbeans.org
 */
public class PageSwitchTest extends PerformanceTestCase {
    
    private String targetProject = "UltraLargeWA";
    private ProjectsTabOperator pto;
    private String[] pagesToOpen = new String[] {"Page1", "TestPage"};
    private WebFormDesignerOperator page1Op;
    private WebFormDesignerOperator page2Op;

    
    public PageSwitchTest(String testName) {
        super(testName);
        expectedTime = WINDOW_OPEN;
        WAIT_AFTER_OPEN=2000;
    }
    public PageSwitchTest(String testName, String performanceDataName) {
        super(testName,performanceDataName);
        expectedTime = WINDOW_OPEN;
        WAIT_AFTER_OPEN=2000;
    }

    public static NbTestSuite suite() {
        NbTestSuite suite = new NbTestSuite();
        suite.addTest(NbModuleSuite.create(NbModuleSuite.createConfiguration(VisualWebSetup.class)
             .addTest(PageSwitchTest.class)
             .enableModules(".*").clusters(".*")));
        return suite;
    }
    
    @Override
    public void initialize() {
        EditorOperator.closeDiscardAll();
        pto = VWPUtilities.invokePTO();
        for(String namme: pagesToOpen) {
            Node docNode = new Node(pto.getProjectRootNode(targetProject),org.netbeans.performance.visualweb.VWPUtilities.WEB_PAGES + "|"+namme+".jsp");
            new OpenAction().performAPI(docNode);
            WebFormDesignerOperator.findWebFormDesignerOperator(namme);
        }
    }

    public void prepare() {
        page1Op = WebFormDesignerOperator.findWebFormDesignerOperator("Page1");
        page2Op = WebFormDesignerOperator.findWebFormDesignerOperator("TestPage");
    }

    public ComponentOperator open() {
        page1Op.getFocus();
        return null;
    }

    @Override
    public void close() {
    }
    
    @Override
    public void shutdown() {
        EditorOperator.closeDiscardAll();        
    }
    
    public void testPageSwitch() {
        doMeasurement();
    }

}
