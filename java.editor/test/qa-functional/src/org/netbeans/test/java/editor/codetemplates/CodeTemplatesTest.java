/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.test.java.editor.codetemplates;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.Test;
import org.netbeans.jellytools.EditorOperator;
import org.netbeans.jemmy.operators.JEditorPaneOperator;
import org.netbeans.junit.NbModuleSuite;
import org.netbeans.test.java.editor.lib.JavaEditorTestCase;

/**
 *
 * @author Jiri Prox Jiri.Prox@Sun.COM
 */
public class CodeTemplatesTest extends JavaEditorTestCase {

    public CodeTemplatesTest(String testMethodName) {
        super(testMethodName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        openProject("java_editor_test");
        //org.netbeans.junit.Log.enableInstances(Logger.getLogger("TIMER"),null, Level.FINEST);
    }

    @Override
    protected void tearDown() throws Exception {
        //org.netbeans.junit.Log.assertInstances("All instances are not removed");
        super.tearDown();
    }
    EditorOperator oper;

    public void testFor() throws IOException {
        openSourceFile("org.netbeans.test.java.editor.codetemplates", "Main");
        oper = new EditorOperator("Main");
        JEditorPaneOperator txtOper = oper.txtEditorPane();
        oper.setCaretPosition(6, 9);
        txtOper.typeText("fori");
        txtOper.pressKey(KeyEvent.VK_TAB);
        compareGoldenFile(oper);
    }

    public static Test suite() {
        return NbModuleSuite.create(
                NbModuleSuite.createConfiguration(CodeTemplatesTest.class).addTest("testFor").enableModules(".*").clusters(".*"));
    }
}
