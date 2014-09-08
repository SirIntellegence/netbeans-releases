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

package org.netbeans.modules.php.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.php.api.util.StringUtils;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.PropertyEvaluator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

/**
 * Represents a helper for manipulation source roots.
 * Based on SourceRoot class (common.java.api)
 * which was copied to all non java projecs.
 * <p>
 * For PHP project, it's simplified because there's no need
 * to store source roots in project.xml (they don't need
 * to be propagated to any build.xml or so). In fact, project.xml
 * is not interesting for PHP project at all.
 * @author Tomas Zezula, Tomas Mysik
 */
public final class SourceRoots {

    /**
     * Property name of a event that is fired when project properties change.
     */
    public static final String PROP_ROOTS = SourceRoots.class.getName() + ".roots"; //NOI18N

    private final UpdateHelper helper;
    private final PropertyEvaluator evaluator;
    private final String displayName;
    private final String propertyNumericPrefix;
    private final PropertyChangeSupport support;
    private final ProjectMetadataListener listener;
    private final boolean tests;
    // #196060 - help to diagnose
    private final AtomicLong firedChanges = new AtomicLong();

    // @GuardedBy("this")
    private List<FileObject> sourceRoots;
    // @GuardedBy("this")
    private List<URL> sourceRootUrls;
    // @GuardedBy("this")
    private List<String> sourceRootProperties;
    // @GuardedBy("this")
    private List<String> sourceRootNames;


    private SourceRoots(Builder builder) {
        assert builder.helper != null;
        assert builder.evaluator != null;
        assert builder.displayName != null;

        helper = builder.helper;
        evaluator = builder.evaluator;
        displayName = builder.displayName;
        propertyNumericPrefix = builder.propertyNumericPrefix;
        tests = builder.tests;

        sourceRootProperties = builder.properties;

        support = new PropertyChangeSupport(this);
        listener = new ProjectMetadataListener();
    }

    static SourceRoots create(Builder builder) {
        SourceRoots roots = new SourceRoots(builder);
        roots.evaluator.addPropertyChangeListener(WeakListeners.propertyChange(roots.listener, roots.evaluator));
        return roots;
    }

    /**
     * Returns the display names of source roots.
     * The returned array has the same length as an array returned by the {@link #getRootProperties()}.
     * It may contain empty {@link String}s but not <code>null</code>.
     * @return an array of source roots names.
     */
    public synchronized String[] getRootNames() {
        return ProjectManager.mutex().readAccess(new Mutex.Action<String[]>() {

            @Override
            public String[] run() {
                synchronized (SourceRoots.this) {
                    assert Thread.holdsLock(SourceRoots.this);
                    if (sourceRootNames == null) {
                        List<String> dirPaths = new ArrayList<>();
                        for (String property : getRootProperties()) {
                            String path = evaluator.getProperty(property);
                            if (path == null) {
                                dirPaths.add(null);
                            } else {
                                dirPaths.add(helper.getAntProjectHelper().resolvePath(path));
                            }
                        }
                        sourceRootNames = getSourceRootsNames(dirPaths, displayName);
                    }
                    return sourceRootNames.toArray(new String[sourceRootNames.size()]);
                }
            }

        });
    }

    /**
     * Returns names of Ant properties in the <i>project.properties</i> file holding the source roots.
     * @return an array of String.
     */
    public String[] getRootProperties() {
        return ProjectManager.mutex().readAccess(new Mutex.Action<String[]>() {

            @Override
            public String[] run() {
                synchronized (SourceRoots.this) {
                    assert Thread.holdsLock(SourceRoots.this);
                    if (sourceRootProperties == null) {
                        assert propertyNumericPrefix != null : displayName;
                        sourceRootProperties = new ArrayList<>();
                        EditableProperties projectProperties = helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
                        // #246368
                        if (projectProperties.containsKey(propertyNumericPrefix)) {
                            sourceRootProperties.add(propertyNumericPrefix);
                        }
                        int i = 1;
                        while (true) {
                            String key = propertyNumericPrefix + i;
                            if (projectProperties.containsKey(key)) {
                                sourceRootProperties.add(key);
                            } else if (i > 1) {
                                break;
                            }
                            i++;
                        }
                    }
                    return sourceRootProperties.toArray(new String[sourceRootProperties.size()]);
                }
            }

        });
    }

    /**
     * Returns the source roots in the form of absolute paths.
     * @return an array of {@link FileObject}s.
     */
    public FileObject[] getRoots() {
        return ProjectManager.mutex().readAccess(new Mutex.Action<FileObject[]>() {
                @Override
                public FileObject[] run() {
                    synchronized (SourceRoots.this) {
                        // local caching
                        assert Thread.holdsLock(SourceRoots.this);
                        if (sourceRoots == null) {
                            String[] srcProps = getRootProperties();
                            List<FileObject> result = new ArrayList<>();
                            for (String p : srcProps) {
                                String prop = evaluator.getProperty(p);
                                if (prop != null) {
                                    FileObject f = helper.getAntProjectHelper().resolveFileObject(prop);
                                    if (f == null) {
                                        continue;
                                    }
                                    if (FileUtil.isArchiveFile(f)) {
                                        f = FileUtil.getArchiveRoot(f);
                                    }
                                    result.add(f);
                                }
                            }
                            sourceRoots = Collections.unmodifiableList(result);
                        }
                        return sourceRoots.toArray(new FileObject[sourceRoots.size()]);
                    }
                }
        });
    }

    /**
     * Returns the source roots as {@link URL}s.
     * @return an array of {@link URL}.
     */
    public URL[] getRootURLs() {
        return ProjectManager.mutex().readAccess(new Mutex.Action<URL[]>() {
            @Override
            public URL[] run() {
                synchronized (SourceRoots.this) {
                    assert Thread.holdsLock(SourceRoots.this);
                    // local caching
                    if (sourceRootUrls == null) {
                        List<URL> result = new ArrayList<>();
                        for (String srcProp : getRootProperties()) {
                            String prop = evaluator.getProperty(srcProp);
                            if (prop != null) {
                                File f = helper.getAntProjectHelper().resolveFile(prop);
                                try {
                                    URL url = Utilities.toURI(f).toURL();
                                    if (!f.exists()) {
                                        url = new URL(url.toExternalForm() + "/"); // NOI18N
                                    } else if (f.isFile()) {
                                        // file cannot be a source root (archives are not supported as source roots).
                                        continue;
                                    }
                                    assert url.toExternalForm().endsWith("/") : "#90639 violation for " + url + "; "
                                            + f + " exists? " + f.exists() + " dir? " + f.isDirectory()
                                            + " file? " + f.isFile();
                                    result.add(url);
                                } catch (MalformedURLException e) {
                                    Exceptions.printStackTrace(e);
                                }
                            }
                        }
                        sourceRootUrls = Collections.unmodifiableList(result);
                    }
                    return sourceRootUrls.toArray(new URL[sourceRootUrls.size()]);
                }
            }
        });
    }

    /**
     * Adds {@link PropertyChangeListener}, see class description for more information
     * about listening to the source roots changes.
     * @param listener a listener to add.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     * Removes {@link PropertyChangeListener}, see class description for more information
     * about listening to the source roots changes.
     * @param listener a listener to remove.
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    /**
     * Translates root name into display name of source/test root.
     * @param rootName the name of root got from {@link SourceRoots#getRootNames()}.
     * @param propName the name of a property the root is stored in.
     * @return the label to be displayed.
     */
    public String getRootDisplayName(String rootName, String propName) {
        assert StringUtils.hasText(rootName) : "No name for " + propName; // NOI18N
        return rootName;
    }

    /**
     * Returns <code>true</code> if the current {@link SourceRoots} instance represents source roots belonging to
     * the test compilation unit.
     * @return boolean <code>true</code> if the instance belongs to the test compilation unit, false otherwise.
     */
    public boolean isTest() {
        return tests;
    }

    private void resetCache(String propName) {
        boolean fire = false;
        synchronized (this) {
            assert Thread.holdsLock(this);
            // in case of change reset local cache
            if (propName == null
                    || (sourceRootProperties != null && sourceRootProperties.contains(propName))
                    || (propertyNumericPrefix != null && propName.startsWith(propertyNumericPrefix))) {
                sourceRoots = null;
                sourceRootUrls = null;
                if (propertyNumericPrefix != null) {
                    sourceRootProperties = null;
                }
                sourceRootNames = null;
                fire = true;
            }
        }
        if (fire) {
            firedChanges.incrementAndGet();
            support.firePropertyChange(PROP_ROOTS, null, null);
        }
    }

    public void refresh() {
        resetCache(null);
    }

    public long getFiredChanges() {
        return firedChanges.get();
    }

    @NbBundle.Messages({
        "# {0} - display name of the source root",
        "# {1} - directory of the source root",
        "SourceRoots.displayName={0} ({1})",
    })
    static List<String> getSourceRootsNames(List<String> dirPaths, String displayName) {
        if (dirPaths.isEmpty()) {
            return Collections.emptyList();
        }
        if (dirPaths.size() == 1) {
            return Collections.singletonList(displayName);
        }
        if (checkIncorrectValues(dirPaths)) {
            // incorrect, duplicated values (should not happen)
            List<String> names = new ArrayList<>(dirPaths.size());
            for (String path : dirPaths) {
                if (path == null) {
                    names.add(displayName);
                } else {
                    names.add(Bundle.SourceRoots_displayName(displayName, path));
                }
            }
            return names;
        }
        String[] names = new String[dirPaths.size()];
        int lastIndex = 0;
        List<Integer> duplicated = new ArrayList<>(dirPaths.size());
        for (;;) {
            duplicated.clear();
            for (int i = 0; i < dirPaths.size(); i++) {
                if (names[i] != null) {
                    // already set
                    continue;
                }
                String path = dirPaths.get(i);
                if (path == null) {
                    names[i] = displayName;
                } else {
                    List<String> segments = StringUtils.explode(path, File.separator);
                    int index = segments.size() - 1 - lastIndex;
                    if (index < 0) {
                        index = 0;
                    }
                    String name = Bundle.SourceRoots_displayName(displayName, segments.get(index));
                    int indexOf = Arrays.asList(names).indexOf(name);
                    if (indexOf != -1
                            && indexOf != i) {
                        duplicated.add(indexOf);
                    } else {
                        names[i] = name;
                    }
                }
            }
            for (Integer index : duplicated) {
                names[index] = null;
            }
            boolean finished = true;
            for (String name : names) {
                if (name == null) {
                    finished = false;
                    break;
                }
            }
            if (finished) {
                break;
            }
            lastIndex++;
        }
        return Arrays.asList(names);
    }

    private static boolean checkIncorrectValues(List<String> dirPaths) {
        List<String> copy = new ArrayList<>(dirPaths);
        copy.removeAll(Collections.singleton(null));
        return new HashSet<>(copy).size() != copy.size();
    }


    //~ Inner classes

    public static final class Builder {

        final UpdateHelper helper;
        final PropertyEvaluator evaluator;
        final String displayName;

        List<String> properties;
        String propertyNumericPrefix;
        boolean tests;


        Builder(UpdateHelper helper, PropertyEvaluator evaluator, String displayName) {
            this.helper = helper;
            this.evaluator = evaluator;
            this.displayName = displayName;
        }

        public Builder setProperties(List<String> properties) {
            this.properties = properties;
            return this;
        }

        public Builder setProperties(String... properties) {
            this.properties = Arrays.asList(properties);
            return this;
        }

        public Builder setPropertyNumericPrefix(String propertyNumericPrefix) {
            this.propertyNumericPrefix = propertyNumericPrefix;
            return this;
        }

        public Builder setTests(boolean tests) {
            this.tests = tests;
            return this;
        }

        public SourceRoots build() {
            assert properties != null || propertyNumericPrefix != null;
            return SourceRoots.create(this);
        }

        //~ Factories

        public static Builder create(UpdateHelper helper, PropertyEvaluator evaluator, String displayName) {
            assert helper != null;
            assert evaluator != null;
            return new Builder(helper, evaluator, displayName);
        }

    }

    private final class ProjectMetadataListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            resetCache(evt.getPropertyName());
        }

    }

}
