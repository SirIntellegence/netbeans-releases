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

package org.netbeans.modules.groovy.grailsproject;

import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;
import java.io.File;

/**
 *
 * @author schmidtm
 */
public class GrailsProjectSettings {
    
    private static final GrailsProjectSettings INSTANCE = new GrailsProjectSettings();
    
    private static final String LAST_USED_ARTIFACT_FOLDER = "lastUsedArtifactFolder"; //NOI18N
    private static final String NEW_PROJECT_COUNT = "newProjectCount"; //NOI18N
    
    public static GrailsProjectSettings getDefault () {
        return INSTANCE;
    }
    
    private static Preferences getPreferences() {
        return NbPreferences.forModule(GrailsProjectSettings.class);
    }

    public int getNewProjectCount () {
        return getPreferences().getInt(NEW_PROJECT_COUNT, 0);
    }

    public void setNewProjectCount (int count) {
        getPreferences().putInt(NEW_PROJECT_COUNT, count);
    }    
    
    public File getLastUsedArtifactFolder () {
        return new File (getPreferences().get(LAST_USED_ARTIFACT_FOLDER, System.getProperty("user.home")));
    }

    public void setLastUsedArtifactFolder (File folder) {
        assert folder != null : "Folder can not be null";
        String path = folder.getAbsolutePath();
        getPreferences().put(LAST_USED_ARTIFACT_FOLDER, path);
    }   
    
}
