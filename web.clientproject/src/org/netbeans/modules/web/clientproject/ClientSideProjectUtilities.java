/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.clientproject;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.ProjectGenerator;
import org.openide.filesystems.FileObject;
import org.openide.util.Mutex;
import org.openide.util.MutexException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author david
 */
public final class ClientSideProjectUtilities {

    private ClientSideProjectUtilities() {
    }

    public static AntProjectHelper setupProject(FileObject dirFO, String name) throws IOException {
        AntProjectHelper projectHelper = ProjectGenerator.createProject(dirFO, ClientSideProjectType.TYPE);
        setProjectName(projectHelper, name);
        saveProjectProperties(projectHelper, Collections.<String, String>emptyMap());
        return projectHelper;
    }

    public static void initializeProject(AntProjectHelper projectHelper) throws IOException {
        initializeProject(projectHelper, ClientSideProjectConstants.DEFAULT_SITE_ROOT_FOLDER,
                ClientSideProjectConstants.DEFAULT_TEST_FOLDER, ClientSideProjectConstants.DEFAULT_CONFIG_FOLDER);
    }

    public static void initializeProject(AntProjectHelper projectHelper, String siteRoot, String test, String config) throws IOException {
        // create dirs
        projectHelper.getProjectDirectory().createFolder(siteRoot);
        projectHelper.getProjectDirectory().createFolder(test);
        projectHelper.getProjectDirectory().createFolder(config);
        // save project
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(ClientSideProjectConstants.PROJECT_SITE_ROOT_FOLDER, siteRoot);
        properties.put(ClientSideProjectConstants.PROJECT_TEST_FOLDER, test);
        properties.put(ClientSideProjectConstants.PROJECT_CONFIG_FOLDER, config);
        saveProjectProperties(projectHelper, properties);
    }

    // XXX "merge" with the method above
    public static void initializeProject(AntProjectHelper projectHelper, String siteRoot) throws IOException {
        Map<String, String> properties = Collections.singletonMap(ClientSideProjectConstants.PROJECT_SITE_ROOT_FOLDER, siteRoot);
        saveProjectProperties(projectHelper, properties);
    }

    // XXX performance (also perhaps incorrect place, should be directly on ClientSideProject (and cached))
    public static FileObject getSiteRootFolder(AntProjectHelper projectHelper) throws IOException {
        EditableProperties properties = projectHelper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
        String siteRoot = properties.getProperty(ClientSideProjectConstants.PROJECT_SITE_ROOT_FOLDER);
        if (siteRoot == null || siteRoot.length() == 0) {
            return null;
        }
        return projectHelper.getProjectDirectory().getFileObject(siteRoot);
    }

    public static void setProjectName(final AntProjectHelper projectHelper, final String name) {
        ProjectManager.mutex().writeAccess(new Runnable() {
            @Override
            public void run() {
                Element data = projectHelper.getPrimaryConfigurationData(true);
                Document document = data.getOwnerDocument();
                NodeList nameList = data.getElementsByTagNameNS(ClientSideProjectType.PROJECT_CONFIGURATION_NAMESPACE, "name"); // NOI18N
                Element nameElement;
                if (nameList.getLength() == 1) {
                    nameElement = (Element) nameList.item(0);
                    NodeList deadKids = nameElement.getChildNodes();
                    while (deadKids.getLength() > 0) {
                        nameElement.removeChild(deadKids.item(0));
                    }
                } else {
                    nameElement = document.createElementNS(
                            ClientSideProjectType.PROJECT_CONFIGURATION_NAMESPACE, "name"); // NOI18N
                    data.insertBefore(nameElement, data.getChildNodes().item(0));
                }
                nameElement.appendChild(document.createTextNode(name));
                projectHelper.putPrimaryConfigurationData(data, true);
            }
        });
    }

    private static void saveProjectProperties(final AntProjectHelper projectHelper, final Map<String, String> properties) throws IOException {
        try {
            ProjectManager.mutex().writeAccess(new Mutex.ExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    if (!properties.isEmpty()) {
                        EditableProperties projectProperties = projectHelper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
                        for (Map.Entry<String, String> entry : properties.entrySet()) {
                            projectProperties.setProperty(entry.getKey(), entry.getValue());
                        }
                        projectHelper.putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, projectProperties);
                    }
                    Project project = ProjectManager.getDefault().findProject(projectHelper.getProjectDirectory());
                    ProjectManager.getDefault().saveProject(project);
                    return null;
                }
            });
        } catch (MutexException e) {
            throw (IOException) e.getException();
        }
    }

}
