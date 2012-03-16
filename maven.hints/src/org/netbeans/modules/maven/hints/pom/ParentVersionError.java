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

package org.netbeans.modules.maven.hints.pom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.project.MavenProject;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.maven.api.NbMavenProject;
import static org.netbeans.modules.maven.hints.pom.Bundle.*;
import org.netbeans.modules.maven.hints.pom.spi.Configuration;
import org.netbeans.modules.maven.hints.pom.spi.POMErrorFixProvider;
import org.netbeans.modules.maven.indexer.api.NBVersionInfo;
import org.netbeans.modules.maven.indexer.api.RepositoryQueries;
import org.netbeans.modules.maven.indexer.api.RepositoryQueries.Result;
import org.netbeans.modules.maven.model.pom.POMModel;
import org.netbeans.modules.maven.model.pom.Parent;
import org.netbeans.modules.xml.xam.Model;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author mkleint
 */
public class ParentVersionError implements POMErrorFixProvider {
    private Configuration configuration;
    static final String PROP_SOURCES = "sources";//NOI18N
    static final String PROP_SNAPSHOT = "snapshot";//NOI18N

    @Messages({
        "TIT_ParentVersionError=Use latest version of parent POM", 
        "DESC_ParentVersionError=Make sure the current project is using the version of parent that resides in current source base or the latest known version based on the repository index."})
    public ParentVersionError() {
        configuration = new Configuration("ParentVersionError", //NOI18N
                TIT_ParentVersionError(),
                DESC_ParentVersionError(),
                true, Configuration.HintSeverity.WARNING);
    }


    @Override
    @Messages({
        "# {0} - parent artifact version",
        "TXT_ParentVersionError=Use current parent version - {0}", 
        "# {0} - parent artifact version",
        "TXT_ParentVersionError2=Use the latest known parent version - {0}"})
    public List<ErrorDescription> getErrorsForDocument(POMModel model, Project prj) {
        assert model != null;
        List<ErrorDescription> toRet = new ArrayList<ErrorDescription>();
        if (prj == null) {
            return null;
        }
        Parent par = model.getProject().getPomParent();
        if (par == null) {
            return toRet;
        }
        boolean useSources = getConfiguration().getPreferences().getBoolean(PROP_SOURCES, true);
        boolean useSnapshot = getConfiguration().getPreferences().getBoolean(PROP_SNAPSHOT, false);
        String declaredVersion = par.getVersion();
        String relPath = par.getRelativePath();
        if (relPath == null) {
            relPath = "../pom.xml"; //NOI18N
        }
        FileObject relPathFO = prj.getProjectDirectory().getFileObject(relPath);
        String currentVersion = null;
        boolean usedSources = false;
        if (useSources && relPathFO != null && relPathFO.getNameExt().equals("pom.xml")) {
            //#172839
            String parGr = par.getGroupId();
            String parArt = par.getArtifactId();
            if (parArt != null && parGr != null) {
                try {
                    Project parentPrj = ProjectManager.getDefault().findProject(relPathFO.getParent());
                    if (parentPrj != null) {
                        NbMavenProject nbprj = parentPrj.getLookup().lookup(NbMavenProject.class);
                        if (nbprj != null) { //do we have some non-maven project maybe?
                            MavenProject mav = nbprj.getMavenProject();
                            //#167711 check the coordinates to filter out parents in non-default location without relative-path elemnt
                            if (parGr.equals(mav.getGroupId()) &&
                                parArt.equals(mav.getArtifactId())) {
                                currentVersion = mav.getVersion();
                                usedSources = true;
                            }
                        }
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        if ((!useSources || currentVersion == null) && declaredVersion != null) {
            ArtifactVersion currentAV = new DefaultArtifactVersion(declaredVersion);
            Result<NBVersionInfo> result = RepositoryQueries.getVersionsResult(par.getGroupId(), par.getArtifactId(), null);
            if (!result.isPartial()) {
                for (NBVersionInfo info : result.getResults()) {
                    if (!useSnapshot && info.getVersion().contains("SNAPSHOT")) { //NOI18N
                        continue;
                    }
                    // XXX can probably just pick the first one, since RQ now sorts
                    ArtifactVersion av = new DefaultArtifactVersion(info.getVersion());
                    if (currentAV.compareTo(av) < 0) {
                        currentAV = av;
                    }
                }
                currentVersion = currentAV.toString();
            }
        }

        if (currentVersion != null && !currentVersion.equals(declaredVersion)) {
            int position = par.findChildElementPosition(model.getPOMQNames().VERSION.getQName());
            Line line = NbEditorUtilities.getLine(model.getBaseDocument(), position, false);
            String message = usedSources ?
                TXT_ParentVersionError(currentVersion) :
                TXT_ParentVersionError2(currentVersion);

            toRet.add(ErrorDescriptionFactory.createErrorDescription(
                    configuration.getSeverity(configuration.getPreferences()).toEditorSeverity(),
                    message,
                    Collections.<Fix>singletonList(new SynchronizeFix(par, currentVersion, usedSources)),
                    model.getBaseDocument(), line.getLineNumber() + 1));

        }
        return toRet;

    }

    @Override
    public JComponent getCustomizer(Preferences preferences) {
        return new ParentVersionErrorCustomizer(preferences);
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    private static class SynchronizeFix implements Fix {
        private Parent parent;
        private String version;
        private String message;

        @Messages({
            "# {0} - parent artifact version",
            "TEXT_ParentVersionFix=Use version {0} as defined in current parent project sources.", 
            "# {0} - parent artifact version",
            "TEXT_ParentVersionFix2=Use the latest known parent version - {0}"})
        SynchronizeFix(Parent par, String version, boolean usedSources) {
            parent = par;
            this.version = version;
            message = usedSources ?
                TEXT_ParentVersionFix(version) :
                TEXT_ParentVersionFix2(version);
        }

        @Override
        public String getText() {
            return message;
        }

        @Override
        public ChangeInfo implement() throws Exception {
            ChangeInfo info = new ChangeInfo();
            POMModel mdl = parent.getModel();
            if (!mdl.getState().equals(Model.State.VALID)) {
                return info;
            }
            mdl.startTransaction();
            try {
                parent.setVersion(version);
            } finally {
                mdl.endTransaction();
            }
            return info;
        }
    }

}
