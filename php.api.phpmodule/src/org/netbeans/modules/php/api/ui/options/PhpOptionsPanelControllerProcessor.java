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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.php.api.ui.options;

import java.lang.annotation.Annotation;
import java.util.Set;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.netbeans.modules.php.api.util.UiUtils.PhpOptionsPanelRegistration;
import org.netbeans.spi.options.AdvancedOption;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.filesystems.annotations.LayerBuilder.File;
import org.openide.filesystems.annotations.LayerGeneratingProcessor;
import org.openide.filesystems.annotations.LayerGenerationException;
import org.openide.util.lookup.ServiceProvider;

/**
 * @author S. Aubrecht
 */
@ServiceProvider(service = Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes("org.netbeans.modules.php.api.util.UiUtils.PhpOptionsPanelRegistration")
public class PhpOptionsPanelControllerProcessor extends LayerGeneratingProcessor {

    @Override
    protected boolean handleProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) throws LayerGenerationException {
        if (roundEnv.processingOver()) {
            return false;
        }
        for (Element element : roundEnv.getElementsAnnotatedWith(PhpOptionsPanelRegistration.class)) {
            PhpOptionsPanelRegistration registration = element.getAnnotation(PhpOptionsPanelRegistration.class);
            if (registration.id().isEmpty()) {
                throw new LayerGenerationException("Registration id cannot be empty", element);
            }
            File file = layer(element)
                    .instanceFile(FrameworksOptionsPanelController.FRAMEWORKS_AND_TOOLS_OPTIONS_PATH, registration.id(), registration, null)
                    .methodvalue("instanceCreate", AdvancedOption.class.getName(), "createSubCategory") // NOI18N
                    .instanceAttribute("controller", OptionsPanelController.class) // NOI18N
                    .bundlevalue("displayName", registration.displayName()) // NOI18N
                    .position(registration.position());
            keywords(element, registration.keywords(), registration.keywordsCategory(), registration, file);
            file.write();
        }

        return true;
    }

    private void keywords(Element element, String keywords, String keywordsCategory, Annotation r, File file) throws LayerGenerationException {
        if (keywords.length() > 0) {
            if (keywordsCategory.length() == 0) {
                throw new LayerGenerationException("Must specify both keywords and keywordsCategory", element, processingEnv, r, "keywordsCategory");
            }
            file.bundlevalue("keywords", keywords, r, "keywords") // NOI18N
                    .bundlevalue("keywordsCategory", keywordsCategory, r, "keywordsCategory"); // NOI18N
        } else if (keywordsCategory.length() > 0) {
            throw new LayerGenerationException("Must specify both keywords and keywordsCategory", element, processingEnv, r, "keywords");
        }
    }

}
