/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.j2me.project.ui.customizer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.netbeans.api.project.Project;
import org.netbeans.modules.java.j2seproject.api.J2SEPropertyEvaluator;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 *
 * @author Theofanis Oikonomou
 */
public class J2MECompositeCategoryProvider implements ProjectCustomizer.CompositeCategoryProvider {

    private static final String PLATFORM = "Platform";
    private static final String APPLICATION_DESCRIPTOR = "Application Descriptor";
    private static final String OBFUSCATING = "Obfuscating";
    private static final String SIGNING = "Signing";
    private String name;
    private static final Map<String, J2MEProjectProperties> projectProperties = new HashMap<>();

    private J2MECompositeCategoryProvider(String name) {
        this.name = name;
    }

    @Override
    @NbBundle.Messages({"LBL_Category_Platform=Platform",
        "LBL_Category_Application_Descriptor=Application Descriptor",
        "LBL_Category_Obfuscating=Obfuscating",
        "LBL_Category_Signing=Signing"})
    public ProjectCustomizer.Category createCategory(Lookup context) {
        ProjectCustomizer.Category toReturn = null;
        boolean meEnabled = false;
        final Project project = context.lookup(Project.class);
        if (project != null && name != null) {
            final J2SEPropertyEvaluator j2sepe = project.getLookup().lookup(J2SEPropertyEvaluator.class);
            meEnabled = J2MEProjectProperties.isTrue(j2sepe.evaluator().getProperty(J2MEProjectProperties.JAVAME_ENABLED));
        }
        if (meEnabled) {
            switch (name) {
                case PLATFORM:
                    toReturn = ProjectCustomizer.Category.create(
                            PLATFORM,
                            Bundle.LBL_Category_Platform(),
                            null);
                    break;
                case APPLICATION_DESCRIPTOR:
                    toReturn = ProjectCustomizer.Category.create(
                            APPLICATION_DESCRIPTOR,
                            Bundle.LBL_Category_Application_Descriptor(),
                            null);
                    break;
                case OBFUSCATING:
                    toReturn = ProjectCustomizer.Category.create(
                            OBFUSCATING,
                            Bundle.LBL_Category_Obfuscating(),
                            null);
                    break;
                case SIGNING:
                    toReturn = ProjectCustomizer.Category.create(
                            SIGNING,
                            Bundle.LBL_Category_Signing(),
                            null);
                    break;
            }
            assert toReturn != null : "No category for name:" + name;
            toReturn.setOkButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (project != null) {
                        J2MEProjectProperties prop = J2MEProjectProperties.getInstanceIfExists(project.getLookup());
                        if (prop != null) {
                            projectProperties.put(project.getProjectDirectory().getPath(), prop);
                        }
                    }
                }
            });
            toReturn.setStoreListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (project != null) {
                        J2MEProjectProperties prop = projectProperties.get(project.getProjectDirectory().getPath());
                        if (prop != null) {
                            try {
                                prop.store();
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                        projectProperties.remove(project.getProjectDirectory().getPath());
                    }
                }
            });
            toReturn.setCloseListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (project != null) {
                        J2MEProjectProperties.cleanup(project.getLookup());
                    }
                }
            });
            return toReturn;
        }
        return null;
    }

    @Override
    public JComponent createComponent(ProjectCustomizer.Category category, final Lookup context) {
        String nm = category.getName();
        final J2MEProjectProperties uiProps = J2MEProjectProperties.getInstance(context);
        switch (nm) {
            case PLATFORM:
                return new J2MEPlatformPanel(uiProps);
            case APPLICATION_DESCRIPTOR:
                return new J2MEApplicationDescriptorPanel(uiProps);
            case OBFUSCATING:
                return new J2MEObfuscatingPanel(uiProps);
            case SIGNING:
                return new J2MESigningPanel(uiProps);
        }
        return new JPanel();

    }

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-java-j2seproject",
            position = 305)
    public static J2MECompositeCategoryProvider createPlatform() {
        return new J2MECompositeCategoryProvider(PLATFORM);
    }

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-java-j2seproject",
            position = 310)
    public static J2MECompositeCategoryProvider createApplicationDescriptor() {
        return new J2MECompositeCategoryProvider(APPLICATION_DESCRIPTOR);
    }

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-java-j2seproject",
            category = "BuildCategory",
            position = 230)
    public static J2MECompositeCategoryProvider createObfuscating() {
        return new J2MECompositeCategoryProvider(OBFUSCATING);
    }

    @ProjectCustomizer.CompositeCategoryProvider.Registration(
            projectType = "org-netbeans-modules-java-j2seproject",
            category = "BuildCategory",
            position = 235)
    public static J2MECompositeCategoryProvider createSigning() {
        return new J2MECompositeCategoryProvider(SIGNING);
    }
}
