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
package org.netbeans.modules.j2ee.persistence.editor.completion;

import java.io.IOException;
import java.util.*;
import javax.lang.model.element.*;
import javax.swing.text.Document;
import org.netbeans.api.java.source.ClassIndex.NameKind;
import org.netbeans.api.java.source.ClassIndex.SearchScope;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.*;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.j2ee.persistence.dd.common.Persistence;
import org.netbeans.modules.j2ee.persistence.editor.CompletionContext;
import org.netbeans.modules.j2ee.persistence.editor.JPAEditorUtil;
import org.netbeans.modules.j2ee.persistence.provider.Provider;
import org.netbeans.modules.j2ee.persistence.provider.ProviderUtil;
import org.openide.util.Exceptions;
import org.w3c.dom.Node;

/**
 * Various completor for code completing XML tags and attributes in Hibername 
 * configuration and mapping file
 * 
 * @author Dongmei Cao
 */
public abstract class Completor {

    static class JtaDatasourceCompletor extends Completor{

        public JtaDatasourceCompletor() {
        }

        @Override
        public List<JPACompletionItem> doCompletion(CompletionContext context) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    static class ProviderCompletor extends Completor {

        public ProviderCompletor() {
        }

        @Override
        public List<JPACompletionItem> doCompletion(CompletionContext context) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }


    private int anchorOffset = -1;

    public abstract List<JPACompletionItem> doCompletion(CompletionContext context);

    protected void setAnchorOffset(int anchorOffset) {
        this.anchorOffset = anchorOffset;
    }

    public int getAnchorOffset() {
        return anchorOffset;
    }

    /**
     * A simple completor for general attribute value items
     * 
     * Takes an array of strings, the even elements being the display text of the items
     * and the odd ones being the corresponding documentation of the items
     * 
     */
    public static class AttributeValueCompletor extends Completor {

        private String[] itemTexts;

        public AttributeValueCompletor(String[] itemTextAndDocs) {
            this.itemTexts = itemTextAndDocs;
        }

        @Override
        public List<JPACompletionItem> doCompletion(CompletionContext context) {
            List<JPACompletionItem> results = new ArrayList<JPACompletionItem>();
            int caretOffset = context.getCaretOffset();
            String typedChars = context.getTypedPrefix();

            for (int i = 0; i < itemTexts.length; i += 1) {
                if (itemTexts[i].startsWith(typedChars.trim())) {
                    JPACompletionItem item = JPACompletionItem.createAttribValueItem(caretOffset - typedChars.length(),
                            itemTexts[i]);
                    results.add(item);
                }
            }

            setAnchorOffset(context.getCurrentToken().getOffset() + 1);
            return results;
        }
    }

    /**
     * A  completor for completing the cascade attribute with cascade styles
     * 
     */
    public static class CascadeStyleCompletor extends Completor {

        private String[] itemTextAndDocs;

        public CascadeStyleCompletor(String[] itemTextAndDocs) {
            this.itemTextAndDocs = itemTextAndDocs;
        }

        @Override
        public List<JPACompletionItem> doCompletion(CompletionContext context) {
            List<JPACompletionItem> results = new ArrayList<JPACompletionItem>();
            int caretOffset = context.getCaretOffset();
            String typedChars = context.getTypedPrefix();

            String styleName = null;
            if (typedChars.contains(",")) {
                int index = typedChars.lastIndexOf(",");
                styleName = typedChars.substring(index + 1);
            } else {
                styleName = typedChars;
            }

            for (int i = 0; i < itemTextAndDocs.length; i += 2) {
                if (itemTextAndDocs[i].startsWith(styleName.trim())) {
                    JPACompletionItem item = JPACompletionItem.createCascadeStyleItem(caretOffset - styleName.length(),
                            itemTextAndDocs[i], itemTextAndDocs[i + 1]);
                    results.add(item);
                }
            }

            setAnchorOffset(context.getCurrentToken().getOffset() + 1);
            return results;
        }
    }

    /**
     * A completor for completing the Java class attributes
     */
    public static class JavaClassCompletor extends Completor {

        private boolean packageOnly = false;

        public JavaClassCompletor(boolean packageOnly) {
            this.packageOnly = packageOnly;
        }

        @Override
        public List<JPACompletionItem> doCompletion(final CompletionContext context) {
            final List<JPACompletionItem> results = new ArrayList<JPACompletionItem>();
            try {
                Document doc = context.getDocument();
                final String typedChars = context.getTypedPrefix();

                JavaSource js = JPAEditorUtil.getJavaSource(doc);
                if (js == null) {
                    return Collections.emptyList();
                }

                if (typedChars.contains(".") || typedChars.equals("")) { // Switch to normal completion
                    doNormalJavaCompletion(js, results, typedChars, context.getCurrentToken().getOffset() + 1);
                } else { // Switch to smart class path completion
                    doSmartJavaCompletion(js, results, typedChars, context.getCurrentToken().getOffset() + 1);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            return results;
        }

        private void doNormalJavaCompletion(JavaSource js, final List<JPACompletionItem> results,
                final String typedPrefix, final int substitutionOffset) throws IOException {
            js.runUserActionTask(new Task<CompilationController>() {

                @Override
                public void run(CompilationController cc) throws Exception {
                    cc.toPhase(Phase.ELEMENTS_RESOLVED);
                    ClassIndex ci = cc.getClasspathInfo().getClassIndex();
                    int index = substitutionOffset;
                    String packName = typedPrefix;
                    String classPrefix = "";
                    int dotIndex = typedPrefix.lastIndexOf('.'); // NOI18N
                    if (dotIndex != -1) {
                        index += (dotIndex + 1);  // NOI18N
                        packName = typedPrefix.substring(0, dotIndex);
                        classPrefix = (dotIndex + 1 < typedPrefix.length()) ? typedPrefix.substring(dotIndex + 1) : "";
                    }
                    addPackages(ci, results, typedPrefix, index);

                    PackageElement pkgElem = cc.getElements().getPackageElement(packName);
                    if (pkgElem == null) {
                        return;
                    }

                    if (!packageOnly) {
                        List<? extends Element> pkgChildren = pkgElem.getEnclosedElements();
                        for (Element pkgChild : pkgChildren) {
                            if ((pkgChild.getKind() == ElementKind.CLASS) && pkgChild.getSimpleName().toString().startsWith(classPrefix)) {
                                TypeElement typeElement = (TypeElement) pkgChild;
                                JPACompletionItem item = JPACompletionItem.createTypeItem(substitutionOffset,
                                        typeElement, ElementHandle.create(typeElement),
                                        cc.getElements().isDeprecated(pkgChild), false);
                                results.add(item);
                            }
                        }
                    }

                    setAnchorOffset(index);
                }
            }, true);
        }

        private void doSmartJavaCompletion(final JavaSource js, final List<JPACompletionItem> results,
                final String typedPrefix, final int substitutionOffset) throws IOException {
            js.runUserActionTask(new Task<CompilationController>() {

                @Override
                public void run(CompilationController cc) throws Exception {
                    cc.toPhase(Phase.ELEMENTS_RESOLVED);
                    ClassIndex ci = cc.getClasspathInfo().getClassIndex();
                    // add packages
                    addPackages(ci, results, typedPrefix, substitutionOffset);

                    if (!packageOnly) {
                        // add classes 
                        Set<ElementHandle<TypeElement>> matchingTypes = ci.getDeclaredTypes(typedPrefix,
                                NameKind.CASE_INSENSITIVE_PREFIX, EnumSet.allOf(SearchScope.class));
                        for (ElementHandle<TypeElement> eh : matchingTypes) {
                            if (eh.getKind() == ElementKind.CLASS) {
                                if (eh.getKind() == ElementKind.CLASS) {
                                    JPACompletionItem item = null;
                                    //LazyTypeCompletionItem item = LazyTypeCompletionItem.create(substitutionOffset, eh, js);
                                    results.add(item);
                                }
                            }
                        }
                    }
                }
            }, true);

            setAnchorOffset(substitutionOffset);
        }

        private void addPackages(ClassIndex ci, List<JPACompletionItem> results, String typedPrefix, int substitutionOffset) {
            Set<String> packages = ci.getPackageNames(typedPrefix, true, EnumSet.allOf(SearchScope.class));
            for (String pkg : packages) {
                if (pkg.length() > 0) {
                    JPACompletionItem item = JPACompletionItem.createPackageItem(substitutionOffset, pkg, false);
                    results.add(item);
                }
            }
        }
    }

    /**
     * A completor for completing Java properties/fields attributes
     */
    public static class PropertyCompletor extends Completor {

        public PropertyCompletor() {
        }

        @Override
        public List<JPACompletionItem> doCompletion(final CompletionContext context) {

            final List<JPACompletionItem> results = new ArrayList<JPACompletionItem>();
            final int caretOffset = context.getCaretOffset();
            final String typedChars = context.getTypedPrefix();

            final String className = JPAEditorUtil.getClassName(context.getTag());
            if (className == null) {
                return Collections.emptyList();
            }

            try {
                // Compile the class and find the fiels
                JavaSource classJavaSrc = JPAEditorUtil.getJavaSource(context.getDocument());
                classJavaSrc.runUserActionTask(new Task<CompilationController>() {

                    @Override
                    public void run(CompilationController cc) throws Exception {
                        cc.toPhase(Phase.ELEMENTS_RESOLVED);
                        TypeElement typeElem = cc.getElements().getTypeElement(className);

                        if (typeElem == null) {
                            return;
                        }

                        List<? extends Element> clsChildren = typeElem.getEnclosedElements();
                        for (Element clsChild : clsChildren) {
                            if (clsChild.getKind() == ElementKind.FIELD) {
                                VariableElement elem = (VariableElement) clsChild;
                                JPACompletionItem item = JPACompletionItem.createClassPropertyItem(caretOffset - typedChars.length(), elem, ElementHandle.create(elem), cc.getElements().isDeprecated(clsChild));
                                results.add(item);
                            }
                        }
                    }
                }, true);


            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            setAnchorOffset(context.getCurrentToken().getOffset() + 1);

            return results;
        }
    }

    /**
     * A completor for completing the persistence property names in persistence.xml file
     * 
     */
    public static class PersistencePropertyNameCompletor extends Completor {

        private Map<Provider, Map<String, String[]>> allKeyAndValues;

        PersistencePropertyNameCompletor(Map<Provider, Map<String, String[]>> allKeyAndValues) {
            this.allKeyAndValues = allKeyAndValues;
        }

        @Override
        public List<JPACompletionItem> doCompletion(CompletionContext context) {
            List<JPACompletionItem> results = new ArrayList<JPACompletionItem>();
            int caretOffset = context.getCaretOffset();
            String typedChars = context.getTypedPrefix();
            String providerClass = getProviderClass(context.getTag());
            Project enclosingProject = FileOwnerQuery.getOwner(
                    NbEditorUtilities.getFileObject(context.getDocument())
                    );
            Provider provider = ProviderUtil.getProvider(providerClass, enclosingProject);
            ArrayList<String> keys = new ArrayList<String>();
            if(provider == null || Persistence.VERSION_2_0.equals(ProviderUtil.getVersion(provider)))keys.addAll(allKeyAndValues.get(null).keySet());
            if(provider != null)keys.addAll(allKeyAndValues.get(provider).keySet());
            String itemTexts[] = keys.toArray(new String[]{});//TODO: get proper provider
            for (int i = 0; i < itemTexts.length; i ++) {
                if (itemTexts[i].startsWith(typedChars.trim()) 
                        || itemTexts[i].startsWith( "javax.persistence." + typedChars.trim()) ) { // NOI18N
                    JPACompletionItem item = JPACompletionItem.createAttribValueItem(caretOffset - typedChars.length(),
                            itemTexts[i]);
                    results.add(item);
                }
            }

            setAnchorOffset(context.getCurrentToken().getOffset() + 1);
            return results;
        }
    }
    /**
     * A completor for completing the persistence property value in persistence.xml file
     * 
     */
    public static class PersistencePropertyValueCompletor extends Completor {

        private Map<Provider, Map<String, String[]>> allKeyAndValues;

        PersistencePropertyValueCompletor(Map<Provider, Map<String, String[]>> allKeyAndValues) {
            this.allKeyAndValues = allKeyAndValues;
        }

        @Override
        public List<JPACompletionItem> doCompletion(CompletionContext context) {
            List<JPACompletionItem> results = new ArrayList<JPACompletionItem>();
            int caretOffset = context.getCaretOffset();
            String typedChars = context.getTypedPrefix();
            String propertyName = getPropertyName(context.getTag());
            if(propertyName == null || propertyName.equals("")) return results;
            String providerClass = getProviderClass(context.getTag());
            Project enclosingProject = FileOwnerQuery.getOwner(
                    NbEditorUtilities.getFileObject(context.getDocument())
                    );
            Provider provider = ProviderUtil.getProvider(providerClass, enclosingProject);
            String[] values = null;
            if(provider == null || Persistence.VERSION_2_0.equals(ProviderUtil.getVersion(provider))){
                values = allKeyAndValues.get(null).get(propertyName);
            }
            if(values == null && provider != null){
                values = allKeyAndValues.get(provider).get(propertyName);
            }
            if(values != null)for (int i = 0; i < values.length; i ++) {
                    JPACompletionItem item = JPACompletionItem.createAttribValueItem(caretOffset - typedChars.length(),
                            values[i]);
                    results.add(item);
            }

            setAnchorOffset(context.getCurrentToken().getOffset() + 1);
            return results;
        }
    }

    /**
     * A completor for completing orm mapping files
     */
    public static class PersistenceMappingFileCompletor extends Completor {

        public PersistenceMappingFileCompletor() {
        }

        @Override
        public List<JPACompletionItem> doCompletion(CompletionContext context) {
            List<JPACompletionItem> results = new ArrayList<JPACompletionItem>();
            int caretOffset = context.getCaretOffset();
            String typedChars = context.getTypedPrefix();

            String[] mappingFiles = getMappingFilesFromProject(context);

            for (int i = 0; i < mappingFiles.length; i++) {
                if (mappingFiles[i].startsWith(typedChars.trim())) {
                    JPACompletionItem item =
                            JPACompletionItem.createMappingFileItem(caretOffset - typedChars.length(),
                            mappingFiles[i]);
                    results.add(item);
                }
            }

            setAnchorOffset(context.getCurrentToken().getOffset() + 1);
            return results;
        }
        
        // Gets the list of mapping files.
        private String[] getMappingFilesFromProject(CompletionContext context) {
            Project enclosingProject = FileOwnerQuery.getOwner(
                    NbEditorUtilities.getFileObject(context.getDocument())
                    );
            //HibernateEnvironment env = enclosingProject.getLookup().lookup(HibernateEnvironment.class);
            if(null != null) {
                return null;//env.getAllHibernateMappings().toArray(new String[]{});
            } else {
                return new String[0];
            }
                
        }
    }
    
    private static String getProviderClass(Node tag){
        String name = null;
        while(tag!=null && !"persistence-unit".equals(tag.getNodeName()))tag = tag.getParentNode();//NOI18N
        if(tag != null){
            for(Node ch = tag.getFirstChild(); ch != null;ch = ch.getNextSibling()){
                if("provider".equals(ch.getNodeName())){//NOI18N
                    name = ch.getFirstChild().getNodeValue();
                }
            }
        }
        return name;
    }
    private static String getPropertyName(Node tag){
        String name = null;
        while(tag!=null && !"property".equals(tag.getNodeName()))tag = tag.getParentNode();//NOI18N
        if(tag != null){
             Node nmN = tag.getAttributes().getNamedItem("name");
             if(nmN != null){
                 name = nmN.getNodeValue();
             }
        }
        return name;
    }
}
