/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.primefaces;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.classpath.ProjectClassPathModifier;
import org.netbeans.api.project.libraries.Library;
import org.netbeans.api.project.libraries.LibraryManager;
import org.netbeans.modules.j2ee.common.ClasspathUtil;
import org.netbeans.modules.web.api.webmodule.WebModule;
import org.netbeans.modules.web.jsf.api.JsfComponentUtils;
import org.netbeans.modules.web.jsf.api.facesmodel.JSFVersion;
import org.netbeans.modules.web.jsf.spi.components.JsfComponentCustomizer;
import org.netbeans.modules.web.jsf.spi.components.JsfComponentImplementation;
import org.netbeans.modules.web.primefaces.ui.PrimefacesCustomizerPanel;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.JarFileSystem;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 * Represents support for PrimeFaces component libraries.
 *
 * @author Martin Fousek <marfous@netbeans.org>
 */
public class PrimefacesImplementation implements JsfComponentImplementation {

    private PrimefacesCustomizer customizer;

    /** Framework name used also for statistics. */
    public static final String PRIMEFACES_NAME = "PrimeFaces"; //NOI18N

    // PrimeFaces Maven resources
    private static final String MAVEN_PF_REPO ="default:http://repository.primefaces.org/"; //NOI18N
    private static final String MAVEN_PF_DEP = "org.primefaces:primefaces:5.0:jar"; //NOI18N

    private static final Logger LOGGER = Logger.getLogger(PrimefacesImplementation.class.getName());
    private static final String PRIMEFACES_SPECIFIC_PRIME_RESOURCE = "org.primefaces.application.PrimeResource"; //NOI18N
    private static final String PRIMEFACES_SPECIFIC_PRIME_RESOURCE_HANDLER = "org.primefaces.application.PrimeResourceHandler"; //NOI18N
    private static final String PREFERENCES_NODE = "primefaces"; //NOI18N
    private static final String POM_PROPERTIES_PATH = "META-INF/maven/org.primefaces/primefaces/pom.properties"; //NOI18N

    /** Preferences property name used for getting lastly used PrimeFaces library. */
    public static final String PROP_PREFERRED_LIBRARY = "preferred-library"; //NOI18N

    public PrimefacesImplementation() {
    }

    @Override
    public String getName() {
        return PRIMEFACES_NAME;
    }

    @NbBundle.Messages({
        "PrimefacesImplementation.primefaces.display.name=PrimeFaces"
    })
    @Override
    public String getDisplayName() {
        return Bundle.PrimefacesImplementation_primefaces_display_name();
    }

    @Override
    public String getDescription() {
        return NbBundle.getMessage(PrimefacesProvider.class, "LBL_PrimeFaces_Description"); //NOI18N
    }

    @Override
    public Set<FileObject> extend(WebModule webModule, JsfComponentCustomizer jsfComponentCustomizer) {
        // Add PrimeFaces library to WebModule classpath
        extendClasspath(webModule, jsfComponentCustomizer);

        // generate PrimeFaces welcome page
        try {
            FileObject welcomePage = generateWelcomePage(webModule);
            return Collections.singleton(welcomePage);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Exception during creating welcome page and extending index", ex); //NOI18N
        }

        return Collections.<FileObject>emptySet();
    }

    @Override
    public Set<JSFVersion> getJsfVersion() {
        return EnumSet.of(JSFVersion.JSF_2_0, JSFVersion.JSF_2_1, JSFVersion.JSF_2_2);
    }

    @Override
    public boolean isInWebModule(WebModule webModule) {
        ClassPath classpath = ClassPath.getClassPath(webModule.getDocumentBase(), ClassPath.COMPILE);
        return hasPrimeFacesResource(classpath);
    }

    private boolean hasPrimeFacesResource(ClassPath classPath) {
        // PRIMEFACES_SPECIFIC_PRIME_RESOURCE - PF 3.2- backward compatibility
        return classPath.findResource(PRIMEFACES_SPECIFIC_PRIME_RESOURCE.replace('.', '/') + ".class") != null //NOI18N
                // PRIMEFACES_SPECIFIC_PRIME_RESOURCE_HANDLER - PF 3.3+
                || classPath.findResource(PRIMEFACES_SPECIFIC_PRIME_RESOURCE_HANDLER.replace('.', '/') + ".class") != null; //NOI18N
    }

    @Override
    public JsfComponentCustomizer createJsfComponentCustomizer(WebModule webModule) {
        if (customizer == null) {
            customizer = new PrimefacesCustomizer();
        }
        return customizer;
    }

    @Override
    public void remove(WebModule webModule) {
        try {
            List<Library> primefacesLibraries;
            if (JsfComponentUtils.isMavenBased(webModule)) {
                primefacesLibraries = Arrays.asList(getMavenLibrary());
            } else {
                primefacesLibraries = getAllRegisteredPrimefaces();
            }
            ProjectClassPathModifier.removeLibraries(primefacesLibraries.toArray(
                    new Library[primefacesLibraries.size()]), webModule.getJavaSources()[0], ClassPath.COMPILE);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Exception during removing JSF suite from an web project", ex); //NOI18N
        } catch (UnsupportedOperationException ex) {
            LOGGER.log(Level.WARNING, "Exception during removing JSF suite from an web project", ex); //NOI18N
        }
    }

    private static void extendClasspath(WebModule webModule, JsfComponentCustomizer jsfComponentCustomizer) {
        try {
            Library primefacesLibrary;
            if (JsfComponentUtils.isMavenBased(webModule)) {
                primefacesLibrary = getMavenLibrary();
            } else {
                primefacesLibrary = getPreferredLibrary(jsfComponentCustomizer);
                if (primefacesLibrary == null) {
                    LOGGER.log(Level.SEVERE, "No PrimeFaces library found.");
                }
            }
            FileObject[] javaSources = webModule.getJavaSources();
                ProjectClassPathModifier.addLibraries(
                        new Library[] {primefacesLibrary},
                        javaSources[0],
                        ClassPath.COMPILE);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "Exception during extending an web project", ex); //NOI18N
        } catch (UnsupportedOperationException ex) {
            LOGGER.log(Level.WARNING, "Exception during extending an web project", ex); //NOI18N
        }
    }

    private static Library getMavenLibrary() {
        return JsfComponentUtils.createMavenDependencyLibrary(
                PRIMEFACES_NAME + "-maven-lib", //NOI18N
                new String[]{MAVEN_PF_DEP},
                new String[]{MAVEN_PF_REPO});
    }

     /**
     * Gets {@code List} of all Primefaces libraries registered in the IDE.
     * @return list of libraries
     */
    public static List<Library> getAllRegisteredPrimefaces() {
        List<Library> libraries = new ArrayList<Library>();
        List<URL> content;
        for (Library library : LibraryManager.getDefault().getLibraries()) {
            if (!"j2se".equals(library.getType())) { //NOI18N
                continue;
            }

            content = library.getContent("classpath"); //NOI18N
            if (isValidPrimefacesLibrary(content)) {
                libraries.add(library);
            }
        }
        return libraries;
    }

    /**
     * Checks if given library content contains mandatory Primefaces classes.
     * @param libraryContent library content
     * @return {@code true} if the given content contains required Primefaces class, {@code false} otherwise
     */
    public static boolean isValidPrimefacesLibrary(List<URL> libraryContent) {
        try {
            return ClasspathUtil.containsClass(libraryContent, PRIMEFACES_SPECIFIC_PRIME_RESOURCE)
                    || ClasspathUtil.containsClass(libraryContent, PRIMEFACES_SPECIFIC_PRIME_RESOURCE_HANDLER);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, null, ex);
            return false;
        }
    }

    /**
     * Gets {@code NbPreferences} for Primefaces plugin.
     * @return Preferences of the Primefaces
     */
    public static Preferences getPrimefacesPreferences() {
        return NbPreferences.forModule(PrimefacesImplementation.class).node(PrimefacesImplementation.PREFERENCES_NODE);
    }

    private static List<URI> getPomURIs(Library library) {
        String baseUri = "http://repository.primefaces.org/org/primefaces/primefaces/<VERSION>/primefaces-<VERSION>.pom"; //NOI18N
        String versionItem = "version="; //NOI18N
        List<URI> poms = new LinkedList<URI>();
        for (URI uri : library.getURIContent("classpath")) { //NOI18N
            try {
                URL archiveFileURL = FileUtil.getArchiveFile(uri.toURL());
                JarFileSystem jsf = new JarFileSystem(new File(archiveFileURL.toURI()));
                FileObject resource = jsf.findResource(POM_PROPERTIES_PATH);
                if (resource != null) {
                    String propertiesText = resource.asText();
                    int indexOfVersion = propertiesText.indexOf(versionItem); //NOI18N
                    String version = propertiesText.substring(indexOfVersion + versionItem.length());
                    version = version.substring(0, version.indexOf("\n")); //NOI18N
                    poms.add(new URI(baseUri.replaceAll("<VERSION>", version.trim()))); //NOI18N
                }
            } catch (URISyntaxException ex) {
                LOGGER.log(Level.WARNING, "Primefaces version wasn't parsed", ex);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Primefaces version wasn't parsed", ex);
            }
        }
        return poms;
    }

    /**
     * Gets library selected in the customizer if was at least opened, otherwise it looks into preferences for
     * lastly used library, otherwise it choose any registered PrimeFaces library.
     */
    private static Library getPreferredLibrary(JsfComponentCustomizer jsfComponentCustomizer) {
        // get the PrimeFaces library from customizer
        if (jsfComponentCustomizer != null) {
            PrimefacesCustomizerPanel panel = ((PrimefacesCustomizerPanel) jsfComponentCustomizer.getComponent());
            Library libarary = panel.getPrimefacesLibrary();
            if (libarary != null) {
                return LibraryManager.getDefault().getLibrary(libarary.getName());
            }
        }

        // search for library stored in PrimeFaces preferences
        Library primefacesLibrary = LibraryManager.getDefault().getLibrary(
                getPrimefacesPreferences().get(PROP_PREFERRED_LIBRARY, "")); //NOI18N
        if (primefacesLibrary != null) {
            return primefacesLibrary;
        }

        // otherwise search for any registered PrimeFaces library in IDE
        return getAllRegisteredPrimefaces().get(0);
    }

    /**
     * Generates PrimeFaces's welcome page in the webmodule.
     */
    private static FileObject generateWelcomePage(WebModule webModule) throws IOException {
        FileObject templateFO = FileUtil.getConfigFile("Templates/Other/welcomePrimefaces.xhtml"); //NOI18N
        DataObject templateDO = DataObject.find(templateFO);
        DataObject generated = templateDO.createFromTemplate(
                DataFolder.findFolder(webModule.getDocumentBase()),
                "welcomePrimefaces"); //NOI18N
        JsfComponentUtils.reformat(generated);

        // update and reformat index page
        updateIndexPage(webModule);

        return generated.getPrimaryFile();
    }

    /**
     * Updates index page of the webmodule - includes link to PrimeFaces's welcome page.
     */
    @NbBundle.Messages({
        "PrimefacesImplementation.index.welcome.primefaces.lbl=Primefaces welcome page"
    })
    private static void updateIndexPage(WebModule webModule) throws DataObjectNotFoundException {
        FileObject indexFO = webModule.getDocumentBase().getFileObject("index.xhtml"); //NOI18N
        if (indexFO == null || !indexFO.isValid() || !indexFO.canWrite()) {
            return;
        }

        DataObject indexDO = DataObject.find(indexFO);
        JsfComponentUtils.enhanceFileBody(
                indexDO,
                "</h:body>", //NOI18N
                "<br />\n<h:link outcome=\"welcomePrimefaces\" value=\"" + Bundle.PrimefacesImplementation_index_welcome_primefaces_lbl() + "\" />"); //NOI18N
        JsfComponentUtils.reformat(indexDO);
    }
}
