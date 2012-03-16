/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.ws.qaf.rest;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.swing.JDialog;
import junit.framework.Test;
import org.netbeans.jellytools.Bundle;
import org.netbeans.jellytools.NewFileWizardOperator;
import org.netbeans.jellytools.WizardOperator;
import org.netbeans.jemmy.EventTool;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JComboBoxOperator;
import org.netbeans.jemmy.operators.JDialogOperator;
import org.netbeans.junit.NbModuleSuite;

/**
 * Tests for New REST web services from Database wizard
 *
 * @author lukas
 */
public class FromDBTest extends CRUDTest {

    protected static Server server = Server.GLASSFISH;

    public FromDBTest(String name) {
        super(name, server);
    }

    public FromDBTest(String name, Server server) {
        super(name, server);
    }

    @Override
    protected String getProjectName() {
        return "FromDB"; //NOI18N
    }

    @Override
    protected String getRestPackage() {
        return "o.n.m.ws.qaf.rest.fromdb"; //NOI18N
    }

    public void testFromDB() throws IOException {
        createPU();
        copyDBSchema();
        //RESTful Web Services from Database
        String restLabel = Bundle.getStringTrimmed("org.netbeans.modules.websvc.rest.wizard.Bundle", "Templates/WebServices/RestServicesFromDatabase");
        NewFileWizardOperator nfwo = createNewWSFile(getProject(), restLabel);
        //Entity Classes from Database
        WizardOperator wo = prepareEntityClasses(nfwo, false);
        wo.next();
        JComboBoxOperator jcbo = new JComboBoxOperator(wo, 1);
        jcbo.clearText();
        jcbo.typeText(getRestPackage() + ".service"); //NOI18N
        wo.finish();
        Runnable r = new Runnable() {

            private boolean found = false;

            @Override
            public void run() {
                while (!found) {
                    String dlgLbl = "REST Resources Configuration";
                    JDialog dlg = JDialogOperator.findJDialog(dlgLbl, true, true);
                    if (null != dlg) {
                        found = true;
                        new JButtonOperator(new JDialogOperator(dlg), "OK").push();
                    }
                }
            }
        };
        new Thread(r).start();
        String generationTitle = Bundle.getStringTrimmed("org.netbeans.modules.j2ee.persistence.wizard.fromdb.Bundle", "TXT_EntityClassesGeneration");
        waitDialogClosed(generationTitle);
        new EventTool().waitNoEvent(1500);
        waitScanFinished();
        Set<File> files = getFiles(getRestPackage() + ".service"); //NOI18N
        if (getJavaEEversion().equals(JavaEEVersion.JAVAEE5)) { // see http://netbeans.org/bugzilla/show_bug.cgi?id=189723
            files.addAll(getFiles("controller")); //NOI18N
            files.addAll(getFiles("controller.exceptions")); //NOI18N
        }
        if (JavaEEVersion.JAVAEE6.equals(getJavaEEversion())) {
            assertEquals("Some files were not generated", 8, files.size()); //NOI18N
        } else {
            assertEquals("Some files were not generated", 18, files.size()); //NOI18N
        }
        //make sure all REST services nodes are visible in project log. view
        assertEquals("missing nodes?", 7, getRestNode().getChildren().length);
    }

    /**
     * Creates suite from particular test cases. You can define order of testcases here.
     */
    public static Test suite() {
        return NbModuleSuite.create(addServerTests(Server.GLASSFISH, NbModuleSuite.createConfiguration(FromDBTest.class),
                "testFromDB", //NOI18N
                "testDeploy", //NOI18N
                "testUndeploy").enableModules(".*").clusters(".*")); //NOI18N
    }
}