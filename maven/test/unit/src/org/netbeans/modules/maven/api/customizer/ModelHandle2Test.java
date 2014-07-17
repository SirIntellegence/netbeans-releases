/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of
 * the License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include
 * the License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the GPL Version 2 section of the License file that accompanied
 * this code. If applicable, add the following below the License Header, with
 * the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you do not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to its
 * licensees as provided above. However, if you add GPL Version 2 code and
 * therefore, elected the GPL Version 2 license, then the option applies only
 * if the new code is made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.maven.api.customizer;

import java.util.Map;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.maven.api.execute.RunConfig;
import org.netbeans.modules.maven.configurations.M2ConfigProvider;
import org.netbeans.modules.maven.configurations.M2Configuration;
import org.netbeans.modules.maven.execute.model.NetbeansActionMapping;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.test.TestFileUtils;

public class ModelHandle2Test extends NbTestCase {

    public ModelHandle2Test(String name) {
        super(name);
    }

    private FileObject d;

    protected @Override void setUp() throws Exception {
        clearWorkDir();
        d = FileUtil.toFileObject(getWorkDir());
    }

    public void testModifyActiveConfig() throws Exception { // #200772
        TestFileUtils.writeFile(d, "pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>g</groupId><artifactId>a</artifactId><version>0</version><profiles><profile><id>jetty</id></profile></profiles></project>");
        TestFileUtils.writeFile(d, "nbactions.xml", "<actions><action><actionName>run</actionName><goals><goal>package</goal></goals></action></actions>");
        FileObject nbactionsJetty = TestFileUtils.writeFile(d, "nbactions-jetty.xml", "<actions><action><displayName>Jetty</displayName><actionName>run</actionName><goals><goal>jetty:run</goal></goals><properties><someprop>v</someprop></properties></action></actions>");
        Project project = ProjectManager.getDefault().findProject(d);
        M2ConfigProvider cp = project.getLookup().lookup(M2ConfigProvider.class);
        for (M2Configuration c : cp.getConfigurations()) {
            if (c.getId().equals("jetty")) {
                cp.setActiveConfiguration(c);
            }
        }
        assertEquals("jetty", cp.getActiveConfiguration().getId());
        NetbeansActionMapping mapp = ModelHandle2.getMapping("run", project, cp.getActiveConfiguration());
        assertNotNull(mapp);
        assertEquals("Jetty", mapp.getDisplayName());
        Map<String,String> props = mapp.getProperties();
        assertNotNull(props);
        assertEquals("{someprop=v}", props.toString());
        props.remove("someprop");
        ModelHandle2.putMapping(mapp, project, cp.getActiveConfiguration());
        assertEquals("jetty", cp.getActiveConfiguration().getId());
        assertFalse(nbactionsJetty.asText().contains("someprop"));
    }
    
    public void testModifyActiveConfigInOneFile() throws Exception { // #200772
        TestFileUtils.writeFile(d, "pom.xml", "<project><modelVersion>4.0.0</modelVersion><groupId>g</groupId><artifactId>a</artifactId><version>0</version><profiles><profile><id>jetty</id></profile></profiles></project>");
        TestFileUtils.writeFile(d, "nbactions.xml", "<actions><action><actionName>run</actionName><goals><goal>package</goal></goals></action>" +
            "<profiles><profile><id>jetty</id><actions><action><displayName>Jetty</displayName>"
                + "<actionName>run</actionName><goals><goal>jetty:run</goal></goals>"
                + "<properties><someprop>v</someprop></properties></action>"
                + "</actions></profile></profiles>"
                + "</actions>");
        Project project = ProjectManager.getDefault().findProject(d);
        M2ConfigProvider cp = project.getLookup().lookup(M2ConfigProvider.class);
        M2Configuration conf = null;
        for (M2Configuration c : cp.getConfigurations()) {
            if (c.getId().equals("jetty")) {
                cp.setActiveConfiguration(c);
                conf = c;
                break;
            }
        }
        assertNotNull("Configuration found", conf);
        assertEquals("jetty", cp.getActiveConfiguration().getId());
        NetbeansActionMapping mapp = ModelHandle2.getMapping("run", project, cp.getActiveConfiguration());
        assertNotNull(mapp);
        assertEquals("Jetty", mapp.getDisplayName());
        Map<String,String> props = mapp.getProperties();
        assertNotNull(props);
        assertEquals("{someprop=v}", props.toString());
        
        RunConfig run = conf.createConfigForDefaultAction("run", project, project.getLookup());
        assertEquals("One goal", 1, run.getGoals().size());
        assertEquals("jetty:run", run.getGoals().get(0));
    }

}
