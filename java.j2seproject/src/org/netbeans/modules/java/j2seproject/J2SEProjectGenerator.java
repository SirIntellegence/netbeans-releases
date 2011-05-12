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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2010 Sun
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

package org.netbeans.modules.java.j2seproject;

import java.io.File;
import java.io.IOException;
import org.netbeans.modules.java.j2seproject.api.J2SEProjectBuilder;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Parameters;

/**
 * Creates a J2SEProject from scratch according to some initial configuration.
 */
public class J2SEProjectGenerator {
    
    private J2SEProjectGenerator() {}
    
    /**
     * Create a new empty J2SE project.
     * @param dir the top-level directory (need not yet exist but if it does it must be empty)
     * @param name the name for the project
     * @param librariesDefinition project relative or absolute OS path to libraries definition; can be null
     * @return the helper object permitting it to be further customized
     * @throws IOException in case something went wrong
     */
    public static AntProjectHelper createProject(final File dir, final String name, final String mainClass, 
            final String manifestFile, final String librariesDefinition, boolean skipTests) throws IOException {
        Parameters.notNull("dir", dir); //NOI18N
        Parameters.notNull("name", name);   //NOI18N
        return new J2SEProjectBuilder (dir, name).
                addDefaultSourceRoots().
                skipTests(skipTests).
                setMainClass(mainClass).
                setManifest(manifestFile).
                setLibrariesDefinitionFile(librariesDefinition).
                setSourceLevel(defaultSourceLevel).
                build();
    }

    public static AntProjectHelper createProject(final File dir, final String name,
                                                  final File[] sourceFolders, final File[] testFolders, 
                                                  final String manifestFile, final String librariesDefinition,
                                                  final String buildXmlName) throws IOException {
        Parameters.notNull("dir", dir); //NOI18N
        Parameters.notNull("name", name);   //NOI8N
        Parameters.notNull("sourceFolders", sourceFolders); //NOI18N
        Parameters.notNull("testFolders", testFolders); //NOI18N
        return new J2SEProjectBuilder(dir, name).
                addSourceRoots(sourceFolders).
                addTestRoots(testFolders).
                skipTests(testFolders.length == 0).
                setManifest(manifestFile).
                setLibrariesDefinitionFile(librariesDefinition).
                setBuildXmlName(buildXmlName).
                setSourceLevel(defaultSourceLevel).
                build();
    }
                    
    //------------ Used by unit tests -------------------
    private static SpecificationVersion defaultSourceLevel;
                
    /**
     * Unit test only method. Sets the default source level for tests
     * where the default platform is not available.
     * @param version the default source level set to project when it is created
     *
     */
    public static void setDefaultSourceLevel (SpecificationVersion version) {
        defaultSourceLevel = version;
    }
}


