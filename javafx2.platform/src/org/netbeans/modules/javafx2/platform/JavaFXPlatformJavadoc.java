/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL") (collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://www.netbeans.org/cddl-gplv2.html or
 * nbbuild/licenses/CDDL-GPL-2-CP. See the License for the specific language
 * governing permissions and limitations under the License. When distributing
 * the software, include this License Header Notice in each file and include the
 * License file at nbbuild/licenses/CDDL-GPL-2-CP. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided by Oracle
 * in the GPL Version 2 section of the License file that accompanied this code.
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL or only
 * the GPL Version 2, indicate your decision by adding "[Contributor] elects to
 * include this software in this distribution under the [CDDL or GPL Version 2]
 * license." If you do not indicate a single choice of license, a recipient has
 * the option to distribute your version of this file under either the CDDL, the
 * GPL Version 2 or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javafx2.platform;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.api.java.queries.JavadocForBinaryQuery;
import org.netbeans.api.java.queries.JavadocForBinaryQuery.Result;
import org.netbeans.modules.javafx2.platform.api.JavaFxRuntimeInclusion;
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.Parameters;
import org.openide.util.WeakListeners;
import org.openide.util.lookup.ServiceProvider;

/**
 * JavadocForBinaryQuery implementation for JFX platform
 * @author Tomas Zezula
 */
@ServiceProvider(service=JavadocForBinaryQueryImplementation.class, position=11000)
public class JavaFXPlatformJavadoc implements JavadocForBinaryQueryImplementation {

    private static final Logger LOG = Logger.getLogger(JavaFXPlatformJavadoc.class.getName());    
    private final ResultCache cache;

    public JavaFXPlatformJavadoc() {
        cache = new ResultCache();
    }



    @Override
    public Result findJavadoc(@NonNull final URL binaryRoot) {
        final long st = System.currentTimeMillis();
        try {
            Parameters.notNull("binaryRoot", binaryRoot);   //NOI18N
            final URL archiveURL = FileUtil.getArchiveFile(binaryRoot);
            if (archiveURL == null) {
                LOG.log(
                    Level.FINE,
                    "Ignoring {0}, not an archvive.",   //NOI18N
                    binaryRoot);
                return null;
            }
            if (!"file".equals(archiveURL.getProtocol())) {    //NOI18N
                LOG.log(
                    Level.FINE,
                    "Ignoring {0}, not a local file.",   //NOI18N
                    binaryRoot);
                return null;
            }
            try {
                final File archiveFile = new File (archiveURL.toURI());
                if (!Utils.getJavaFxRuntimeArchiveName().equals(archiveFile.getName())) {
                    LOG.log(
                        Level.FINE,
                        "Ignoring {0}, not an JavaFX runtime.",   //NOI18N
                        binaryRoot);
                    return  null;
                }
                return cache.getResult(archiveFile);
            } catch (URISyntaxException e) {
                Exceptions.printStackTrace(e);
                return null;
            }
        } finally {
            final long et = System.currentTimeMillis();
            LOG.log(
                Level.FINER,
                "findJavadoc({0}) took {1}ms.", //NOI18N
                new Object[]{
                    binaryRoot,
                    et-st
                });
        }
    }    
    
    //@ThreadSafe
    private static final class ResultCache implements PropertyChangeListener {

        private static final Result UNKNOWN = new Result() {
            @Override
            public URL[] getRoots() {
                return new URL[0];
            }
            @Override
            public void addChangeListener(ChangeListener l) {
            }
            @Override
            public void removeChangeListener(ChangeListener l) {
            }
        };

        //@GuaredBy("results")
        private final Map<File, Result> results =
                Collections.synchronizedMap(new HashMap<File, Result>());



        ResultCache() {
            final JavaPlatformManager jpm = JavaPlatformManager.getDefault();
            jpm.addPropertyChangeListener(WeakListeners.propertyChange(this, jpm));
        }

        @CheckForNull
        Result getResult(@NonNull final File file) {
            Parameters.notNull("file", file);   //NOI18N
            Result res = results.get(file);
            if (res == null) {
                final Collection<? extends JavaPlatform> jps = findJavaPlatforms(file);
                if (!jps.isEmpty() &&
                    !JavaFxRuntimeInclusion.forPlatform(jps.iterator().next()).isIncludedOnClassPath()) {
                    res = new ResultImpl(jps);
                } else {
                    res = UNKNOWN;
                }
                synchronized (results) {
                    final Result tmp = results.get(file);
                    if (tmp == null) {
                        results.put(file, res);
                    } else {
                        res = tmp;
                    }
                }
            }
            return res == UNKNOWN ? null : res;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (JavaPlatformManager.PROP_INSTALLED_PLATFORMS.equals(evt.getPropertyName())) {
                results.clear();
            }
        }

        @NonNull
        private Collection<? extends JavaPlatform> findJavaPlatforms(@NonNull final File jfxrt) {
            final JavaPlatform[] jps = JavaPlatformManager.getDefault().getPlatforms(
                    null,
                    new Specification(
                        "j2se", //NOI18N
                        null));
            final Collection<JavaPlatform> res = new ArrayList<JavaPlatform>(jps.length);
            final FileObject jfxrfFo = FileUtil.toFileObject(jfxrt);
            if (jfxrfFo != null) {
                for (JavaPlatform jp : jps) {
                    for (FileObject installFolder : jp.getInstallFolders()) {
                        if (FileUtil.isParentOf(installFolder, jfxrfFo)) {
                            res.add(jp);
                        }
                    }
                }
            }
            return res;
        }

    }

    private static final class ResultImpl implements JavadocForBinaryQuery.Result, PropertyChangeListener {

        private final Collection<? extends JavaPlatform> plaforms;
        private final ChangeSupport support;

        ResultImpl(@NonNull final Collection<? extends JavaPlatform> platforms) {
            Parameters.notNull("platforms", platforms); //NOI18N
            this.plaforms = platforms;
            for (JavaPlatform jp : this.plaforms) {
                jp.addPropertyChangeListener(WeakListeners.propertyChange(this, jp));
            }
            this.support = new ChangeSupport(this);
        }

        @Override
        public URL[] getRoots() {
            try {
                final long st = System.currentTimeMillis();
                final Set<URI> collector = new LinkedHashSet<URI>();
                for (JavaPlatform jp : plaforms) {
                    for (URL jdoc : jp.getJavadocFolders()) {
                        collector.add(jdoc.toURI());
                    }
                }
                final URL[] res = new URL[collector.size()];
                int i = 0;
                for (URI uri : collector) {
                    res[i++] = uri.toURL();
                }
                final long et = System.currentTimeMillis();
                LOG.log(
                    Level.FINER,
                    "getRoots() -> {0} took: {1}ms",    //NOI18N
                    new Object[]{
                        collector,
                        et-st
                    });
                return res;
            } catch (URISyntaxException e) {
                Exceptions.printStackTrace(e);
            } catch (MalformedURLException e) {
                Exceptions.printStackTrace(e);
            }
            return new URL[0];
        }

        @Override
        public void addChangeListener(@NonNull final ChangeListener l) {
            Parameters.notNull("l", l); //NOI18N
            support.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(@NonNull final ChangeListener l) {
            Parameters.notNull("l", l);     //NOI18N
            support.removeChangeListener(l);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (JavaPlatform.PROP_JAVADOC_FOLDER.equals(evt.getPropertyName())) {
                support.fireChange();
            }
        }

    }

}
