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
package org.netbeans.modules.gsf.codecoverage.api;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.openide.util.Exceptions;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

/**
 * This utility class helps with implementations of tasks that providers
 * need, such as project persistence of the enabled- and aggregation- state
 * for a project. (This isn't done automatically by the coverage manager
 * since there may be code coverage frameworks, such as emma for a Java project,
 * where this state should be recorded as the ant attributes directly instead
 * of a separate auxiliary state.
 *
 * @author Tor Norbye
 */
public class CoverageProviderHelper {
    private static final String COVERAGE_NAMESPACE_URI = "http://www.netbeans.org/ns/code-coverage/1"; // NOI18N

    private CoverageProviderHelper() {
        // Utility method class
    }

    public static boolean isEnabled(Project project) {
        AuxiliaryConfiguration config = ProjectUtils.getAuxiliaryConfiguration(project);
        Element configurationFragment = config.getConfigurationFragment("coverage", COVERAGE_NAMESPACE_URI, false); // NOI18N
        if (configurationFragment == null) {
            return false;
        }
        return configurationFragment.getAttribute("enabled").equals("true"); // NOI18N
    }

    public static void setEnabled(Project project, boolean enabled) {
        AuxiliaryConfiguration config = ProjectUtils.getAuxiliaryConfiguration(project);
        if (ProjectManager.getDefault().isValid(project)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document document = builder.newDocument();
                Element configurationFragment = document.createElementNS(
                        COVERAGE_NAMESPACE_URI,
                        "coverage"); // NOI18N
                configurationFragment.setAttribute("enabled", enabled ? "true" : "false"); // NOI18N

                config.putConfigurationFragment(
                        configurationFragment, false);
                ProjectManager.getDefault().saveProject(project);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ParserConfigurationException ex) {
                Exceptions.printStackTrace(ex);
            } catch (DOMException e) {
                Exceptions.printStackTrace(e);
            }
        }
    }

    public static boolean isAggregating(Project project) {
        AuxiliaryConfiguration config = ProjectUtils.getAuxiliaryConfiguration(project);
        Element configurationFragment = config.getConfigurationFragment("coverage", COVERAGE_NAMESPACE_URI, false); // NOI18N
        if (configurationFragment == null) {
            return false;
        }
        return configurationFragment.getAttribute("aggregating").equals("true"); // NOI18N
    }

    public static void setAggregating(Project project, boolean aggregating) {
        AuxiliaryConfiguration config = ProjectUtils.getAuxiliaryConfiguration(project);
        if (ProjectManager.getDefault().isValid(project)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();
                org.w3c.dom.Document document = builder.newDocument();
                Element configurationFragment = document.createElementNS(
                        COVERAGE_NAMESPACE_URI,
                        "coverage"); // NOI18N
                configurationFragment.setAttribute("aggregating", aggregating ? "true" : "false"); // NOI18N

                config.putConfigurationFragment(
                        configurationFragment, false);
                ProjectManager.getDefault().saveProject(project);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException ex) {
                Exceptions.printStackTrace(ex);
            } catch (ParserConfigurationException ex) {
                Exceptions.printStackTrace(ex);
            } catch (DOMException e) {
                Exceptions.printStackTrace(e);
            }
        }
    }
}
