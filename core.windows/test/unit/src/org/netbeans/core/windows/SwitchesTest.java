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

package org.netbeans.core.windows;

import java.util.Locale;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.netbeans.core.windows.actions.MaximizeWindowAction;
import org.netbeans.core.windows.actions.UndockWindowAction;
import org.netbeans.core.windows.view.dnd.WindowDnDManager;
import org.netbeans.core.windows.view.ui.MultiSplitPane;
import org.netbeans.core.windows.view.ui.tabcontrol.TabbedAdapter;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.NbTestSuite;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;

/** 
 * Test Mode activation behavior.
 * 
 * @author Marek Slama
 * 
 */
public class SwitchesTest extends NbTestCase {

    public SwitchesTest (String name) {
        super (name);
    }
    
    public static Test suite() {
        TestSuite suite = new NbTestSuite(SwitchesTest.class);
        
        return suite;
    }

    protected boolean runInEQ () {
        return true;
    }

    private Locale defLocale;
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        defLocale = Locale.getDefault();
        Locale.setDefault(new Locale("te_ST"));
    }
    
    @Override
    protected void tearDown() {
        Locale.setDefault(defLocale);
    }
    
    public void testDndDisabled() throws Exception {
        assertFalse(Switches.isTopComponentDragAndDropEnabled());
        assertFalse(WindowDnDManager.isDnDEnabled());
    }

    public void testUndockingDisabled() throws Exception {
        assertFalse(Switches.isTopComponentUndockingEnabled());
        assertFalse( new UndockWindowAction( new TopComponent()).isEnabled() );
    }

    public void testSlidingDisabled() throws Exception {
        assertFalse(Switches.isTopComponentSlidingEnabled());
        assertFalse( new TabbedAdapter(Constants.MODE_KIND_VIEW).getContainerWinsysInfo().isTopComponentSlidingEnabled() );
    }

    public void testMaximizationDisabled() throws Exception {
        assertFalse(Switches.isTopComponentMaximizationEnabled());
        assertFalse( new MaximizeWindowAction(new TopComponent()).isEnabled() );
    }

    public void testViewClosingDisabled() throws Exception {
        assertFalse(Switches.isViewTopComponentClosingEnabled());
        assertFalse( new TabbedAdapter(Constants.MODE_KIND_VIEW).getContainerWinsysInfo().isTopComponentClosingEnabled() );
    }

    public void testEditorClosingDisabled() throws Exception {
        assertFalse(Switches.isEditorTopComponentClosingEnabled());
        assertFalse( new TabbedAdapter(Constants.MODE_KIND_EDITOR).getContainerWinsysInfo().isTopComponentClosingEnabled() );
    }

//    public void testMinimumSizeRespected() throws Exception {
//        assertTrue(Switches.isSplitterRespectMinimumSizeEnabled());
//        MultiSplitPane split = new MultiSplitPane();
//        split.getMinimumSize()
//        assertFalse( new TabbedAdapter(Constants.MODE_KIND_EDITOR).getContainerWinsysInfo().isTopComponentClosingEnabled() );
//    }
}

