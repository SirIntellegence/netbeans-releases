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
package org.netbeans.modules.css.prep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.css.indexing.api.CssIndex;
import org.netbeans.modules.css.prep.model.CPElementHandle;
import org.netbeans.modules.css.prep.model.CPElementType;
import org.netbeans.modules.web.common.api.DependenciesGraph;
import org.netbeans.modules.web.common.api.DependencyType;
import org.openide.filesystems.FileObject;

/**
 *
 * @author marekfukala
 */
public class CPUtils {

    public static String SCSS_FILE_EXTENSION = "scss"; //NOI18N
    public static String SASS_FILE_EXTENSION = "sass"; //NOI18N
    public static String LESS_FILE_EXTENSION = "less"; //NOI18N
    public static String SCSS_FILE_MIMETYPE = "text/scss"; //NOI18N
    public static String LESS_FILE_MIMETYPE = "text/less"; //NOI18N

    public static boolean isCPFile(FileObject file) {
        String mt = file.getMIMEType();
        return SCSS_FILE_MIMETYPE.equals(mt) || LESS_FILE_MIMETYPE.equals(mt);
    }

    /**
     * Gets {@link CPCssIndexModel}s for all referred or related files (transitionally)
     *
     * @param allRelatedFiles if true the map will takes into account file relations regardless the direction of the imports. 
     * if false then also referred files are taken into account.
     * @param file the base file
     * @param excludeTheBaseFile if true, model for the file passed as argument is not added to the result map.
     * @return
     */
    public static Map<FileObject, CPCssIndexModel> getIndexModels(FileObject file, DependencyType dependencyType, boolean excludeTheBaseFile) throws IOException {
        Map<FileObject, CPCssIndexModel> models = new LinkedHashMap<FileObject, CPCssIndexModel>();
        Project project = FileOwnerQuery.getOwner(file);
        if (project != null) {
            CssIndex index = CssIndex.get(project);
        DependenciesGraph dependencies = index.getDependencies(file);
        Collection<FileObject> referred = dependencies.getFiles(dependencyType);
        for (FileObject reff : referred) {
            if (excludeTheBaseFile && reff.equals(file)) {
                //skip current file (it is included to the referred files list)
                continue;
            }
            CPCssIndexModel cpIndexModel = (CPCssIndexModel) index.getIndexModel(CPCssIndexModel.Factory.class, reff);
            if (cpIndexModel != null) {
                models.put(reff, cpIndexModel);
            }

        }
        }
        return models;
    }

    /**
     * Gets a new collection of {@link CPElementHandle}s containing just handles of the specified types.
     *
     * @param handles handles to filter
     * @param type required handle types
     * @return non null collection of filtered handles.
     */
    public static Collection<CPElementHandle> filter(Collection<CPElementHandle> handles, CPElementType... types) {
        Set<CPElementType> typesSet = EnumSet.copyOf(Arrays.asList(types));
        Collection<CPElementHandle> filtered = new ArrayList<CPElementHandle>();
        for (CPElementHandle handle : handles) {
            if (typesSet.contains(handle.getType())) {
                filtered.add(handle);
            }
        }
        return filtered;
    }
}
