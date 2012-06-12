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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.apisupport.project.spi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.project.Project;
import org.netbeans.modules.apisupport.project.api.ManifestManager;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbCollections;
import org.openide.util.Parameters;
import org.openide.util.Utilities;

/**
 * Locates brandable modules.
 * @author Radek Matous
 */
public abstract class BrandingSupport {

    protected interface BrandableModule {
        String getCodeNameBase();
        File getJarLocation();
        String getRelativePath();
    }
    
    private final Project project;
    private Set<BrandableModule> brandedModules;
    private Set<BundleKey> brandedBundleKeys;
    private Set<BrandedFile> brandedFiles;

    private final String brandingPath;
    private final File brandingDir;
    
    private static final String BUNDLE_NAME = "Bundle.properties"; //NOI18N

    /**
     * @param p Project to be branded.
     * @param brandingPath Path relative to project's dir where branded resources are stored in.
     * @throws IOException
     */
    protected BrandingSupport(Project p, String brandingPath) {
        this.project = p;
        this.brandingPath = brandingPath;
        File suiteDir = FileUtil.toFile(project.getProjectDirectory());
        assert suiteDir != null && suiteDir.exists();
        brandingDir = new File(suiteDir, brandingPath);//NOI18N
    }        
    
    /**
     * @return the project directory beneath which everything in the project lies
     */
    private File getProjectDirectory() {
        return FileUtil.toFile(project.getProjectDirectory());
    }
    
    /**
     * @return the top-level branding directory
     */
    File getBrandingRoot() {
        return new File(getProjectDirectory(), brandingPath);
    }
    
    /**
     * @return the branding directory for NetBeans module represented as
     * <code>ModuleEntry</code>
     */
    File getModuleEntryDirectory(BrandableModule mEntry) {
        String relativePath;
        relativePath = mEntry.getRelativePath();
        return new File(getBrandingRoot(),relativePath);
    }
    
    /**
     * @return the file representing localizing bundle for NetBeans module
     */
    private File getLocalizingBundle(final BrandableModule mEntry) {
        ManifestManager mfm = ManifestManager.getInstanceFromJAR(mEntry.getJarLocation());
        File bundle = null;
        if (mfm != null) {
            String bundlePath = mfm.getLocalizingBundle();
            if (bundlePath != null) {
                bundle = new File(getModuleEntryDirectory(mEntry),bundlePath);
            }
        }
        return bundle;
    }
    
    boolean isBranded(final BundleKey key) {
        boolean retval = getBrandedBundleKeys().contains(key);
        return retval;
        
    }
    
    boolean isBranded(final BrandedFile bFile) {
        return getBrandedFiles().contains(bFile);
    }
    
    /**
     * @return true if NetBeans module is already branded
     */
    boolean isBranded(final BrandableModule entry) {
        boolean retval = getBrandedModules().contains(entry);
        assert (retval == getModuleEntryDirectory(entry).exists());
        return retval;
    }
    
    private Set<BrandableModule> getBrandedModules() {
        return brandedModules;
    }
    
    Set<BundleKey> getBrandedBundleKeys() {
        return brandedBundleKeys;
    }
    
    Set<BrandedFile> getBrandedFiles() {
        return brandedFiles;
    }
    
    Set<BundleKey> getLocalizingBundleKeys(final String moduleCodeNameBase, final Set<String> keys) {
        BrandableModule foundEntry = findBrandableModule(moduleCodeNameBase);
        return (foundEntry != null) ? getLocalizingBundleKeys(foundEntry, keys) : null;
    }
    
    private Set<BundleKey> getLocalizingBundleKeys(final BrandableModule moduleEntry, final Set<String> keys) {
        Set<BundleKey> retval = new HashSet<BundleKey>();
        for (Iterator<BundleKey> it = getBrandedBundleKeys().iterator();
        it.hasNext() && retval.size() != keys.size();) {
            BundleKey bKey = it.next();
            if (keys.contains(bKey.getKey())) {
                retval.add(bKey);
            }
        }
        
        if (retval.size() != keys.size()) {
            loadLocalizedBundlesFromPlatform(moduleEntry, keys, retval);
        }
        return (retval.size() != keys.size()) ? null : retval;
    }
    
    BrandedFile getBrandedFile(final String moduleCodeNameBase, final String entryPath) {
        BrandableModule foundEntry = findBrandableModule(moduleCodeNameBase);
        return (foundEntry != null) ? getBrandedFile(foundEntry,entryPath) : null;
    }
    
    private BrandedFile getBrandedFile(final BrandableModule moduleEntry, final String entryPath) {
        BrandedFile retval = null;
        try {
            retval = new BrandedFile(moduleEntry, entryPath);
            for (Iterator it = getBrandedFiles().iterator();it.hasNext() ;) {
                BrandedFile bFile = (BrandedFile)it.next();
                
                if (retval.equals(bFile)) {
                    retval = bFile;
                    
                }
            }
        } catch (MalformedURLException ex) {
            retval = null;
        }
        return retval;
    }
    
    BundleKey getBundleKey(final String moduleCodeNameBase,
            final String bundleEntry,final String key) {
        Set<BundleKey> keys = getBundleKeys(moduleCodeNameBase, bundleEntry, Collections.singleton(key));
        return (keys == null) ? null : (BrandingSupport.BundleKey) keys.toArray()[0];
    }
    
    Set<BundleKey> getBundleKeys(final String moduleCodeNameBase, final String bundleEntry, final Set<String> keys) {
        BrandableModule foundEntry = findBrandableModule(moduleCodeNameBase);
        return (foundEntry != null) ? getBundleKeys(foundEntry, bundleEntry, keys) : null;
    }
    
    private Set<BundleKey> getBundleKeys(final BrandableModule moduleEntry, final String bundleEntry, final Set<String> keys) {
        Set<BundleKey> retval = new HashSet<BundleKey>();
        for (Iterator<BundleKey> it = getBrandedBundleKeys().iterator();
        it.hasNext() && retval.size() != keys.size();) {
            BundleKey bKey = it.next();
            if (keys.contains(bKey.getKey())) {
                retval.add(bKey);
            } 
        }
        
        if (retval.size() != keys.size()) {
            try {
                loadLocalizedBundlesFromPlatform(moduleEntry, bundleEntry, keys, retval);
            } catch (IOException ex) {
                Logger.getLogger(BrandingSupport.class.getName()).log(Level.WARNING, "#211911", ex);
                return null;
            }
        }
                    
        return (retval.size() != keys.size()) ? null : retval;
    }
    
    protected abstract BrandableModule findBrandableModule(String moduleCodeNameBase);

    protected abstract Set<File> getBrandableJars();
    
    void brandFile(final BrandedFile bFile) throws IOException {
        if (!bFile.isModified()) return;

        File target = bFile.getFileLocation();
        if (!target.exists()) {
            target.getParentFile().mkdirs();
            target.createNewFile();
        }
        
        assert target.exists();
        FileObject fo = FileUtil.toFileObject(target);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = bFile.getBrandingSource().openStream();
            os = fo.getOutputStream();
            FileUtil.copy(is, os);
        } finally {
            if (is != null) {
                is.close();
            }
            
            if (os != null) {
                os.close();
            }
            
            brandedFiles.add(bFile);
            bFile.modified = false;
        }
    }
    
    void brandFile(final BrandedFile bFile, final Runnable saveTask) throws IOException {
        if (!bFile.isModified()) return;
        
        saveTask.run();
        brandedFiles.add(bFile);
        bFile.modified = false;
    }
    
    void brandBundleKey(final BundleKey bundleKey) throws IOException {
        if (bundleKey == null) {
            return;
        }
        brandBundleKeys(Collections.singleton(bundleKey));
    }
    
    void brandBundleKeys(final Set<BundleKey> bundleKeys) throws IOException {
        init();
        Map<File,EditableProperties> mentryToEditProp = new HashMap<File,EditableProperties>();
        for (BundleKey bKey : bundleKeys) {
            if (bKey.isModified()) {
                EditableProperties ep = mentryToEditProp.get(bKey.getBrandingBundle());
                if (ep == null) {
                    File bundle = bKey.getBrandingBundle();
                    if (!bundle.exists()) {
                        bundle.getParentFile().mkdirs();
                        bundle.createNewFile();
                    }
                    ep = getEditableProperties(bundle);
                    mentryToEditProp.put(bKey.getBrandingBundle(), ep);
                }
                ep.setProperty(bKey.getKey(), bKey.getValue());
            }
        }
        
        for (Map.Entry<File,EditableProperties> entry : mentryToEditProp.entrySet()) {
            File bundle = entry.getKey();
            assert bundle.exists();
            storeEditableProperties(entry.getValue(), bundle);
            for (BundleKey bKey: bundleKeys) {
                File bundle2 = bKey.getBrandingBundle();
                if (bundle2.equals(bundle)) {
                    brandedBundleKeys.add(bKey);
                    bKey.modified = false;
                    brandedModules.add(bKey.getModuleEntry());
                }
            }
        }
    }

    /** return null in case nothing has changed since last call */
    protected abstract @CheckForNull Set<BrandableModule> loadModules() throws IOException;
    
    void init() throws IOException {
        Set<BrandableModule> loaded = loadModules();
        if (brandedModules == null || loaded != null) {
            brandedModules = new HashSet<BrandableModule>();
            brandedBundleKeys = new HashSet<BundleKey>();
            brandedFiles = new HashSet<BrandedFile>();
            
            if (brandingDir.exists()) {
                assert brandingDir.isDirectory();
                scanModulesInBrandingDir(brandingDir, loaded);
            }
        }
    }
    
    private  void scanModulesInBrandingDir(final File srcDir, final Set<BrandableModule> platformModules) throws IOException  {
        if (srcDir.getName().endsWith(".jar")) {//NOI18N
            BrandableModule foundEntry = null;
            for (BrandableModule platformModule : platformModules) {
                if (isBrandingForModuleEntry(srcDir, platformModule)) {
                    scanBrandedFiles(srcDir, platformModule);
                    
                    foundEntry = platformModule;
                    break;
                }
            }
            if (foundEntry != null) {
                brandedModules.add(foundEntry);
            }
        } else {
            String[] kids = srcDir.list();
            assert (kids != null);
            
            for (String kidName : kids) {
                File kid = new File(srcDir, kidName);
                if (!kid.isDirectory()) {
                    continue;
                }
                scanModulesInBrandingDir(kid, platformModules);
            }
        }
    }
    
    private void scanBrandedFiles(final File srcDir, final BrandableModule mEntry) throws IOException {
        String[] kids = srcDir.list();
        assert (kids != null);
        
        for (String kidName : kids) {
            File kid = new File(srcDir, kidName);
            if (!kid.isDirectory()) {
                if (kid.getName().endsWith(BUNDLE_NAME)) {
                    loadBundleKeys(mEntry, kid);
                } else {
                    loadBrandedFiles(mEntry, kid);
                }
                
                continue;
            }
            scanBrandedFiles(kid, mEntry);
        }
    }
    
    private void loadBundleKeys(final BrandableModule mEntry,
            final File bundle) throws IOException {
        EditableProperties p = getEditableProperties(bundle);
        for (Map.Entry<String,String> entry : p.entrySet()) {
            brandedBundleKeys.add(new BundleKey(mEntry, bundle, entry.getKey(), entry.getValue()));
        }
    }
    
    private void loadBrandedFiles(final BrandableModule mEntry,
            final File file) throws IOException {
        String entryPath = PropertyUtils.relativizeFile(getModuleEntryDirectory(mEntry),file);
        BrandedFile bf = new BrandedFile(mEntry, Utilities.toURI(file).toURL(), entryPath);
        brandedFiles.add(bf);
    }
    
    
    private static EditableProperties getEditableProperties(final File bundle) throws IOException {
        EditableProperties p = new EditableProperties(true);
        InputStream is = new FileInputStream(bundle);
        try {
            p.load(is);
        } finally {
            is.close();
        }
        return p;
    }
    
    private static void storeEditableProperties(final EditableProperties p, final File bundle) throws IOException {
        FileObject fo = FileUtil.toFileObject(bundle);
        OutputStream os = null == fo ? new FileOutputStream(bundle) : fo.getOutputStream();
        try {
            p.store(os);
        } finally {
            os.close();
        }
    }

    protected abstract Map<String,String> localizingBundle(BrandableModule moduleEntry);
    
    private void loadLocalizedBundlesFromPlatform(final BrandableModule moduleEntry, final Set<String> keys, final Set<BundleKey> bundleKeys) {
        Map<String,String> p = localizingBundle(moduleEntry);
        for (String key : p.keySet()) {
            if (keys.contains(key)) {
                String value = p.get(key);
                bundleKeys.add(new BundleKey(moduleEntry, key, value));
            }
        }
    }
    
    private void loadLocalizedBundlesFromPlatform(final BrandableModule moduleEntry,
            final String bundleEntry, final Set<String> keys, final Set<BundleKey> bundleKeys) throws IOException {
        Properties p = new Properties();
        JarFile module = new JarFile(moduleEntry.getJarLocation());
        JarEntry je = module.getJarEntry(bundleEntry);
        InputStream is = module.getInputStream(je);
        File bundle = new File(getModuleEntryDirectory(moduleEntry),bundleEntry);
        try {
            
            p.load(is);
        } finally {
            is.close();
        }
        for (String key : NbCollections.checkedMapByFilter(p, String.class, String.class, true).keySet()) {
            if (keys.contains(key)) {
                String value = p.getProperty(key);
                bundleKeys.add(new BundleKey(moduleEntry, bundle, key, value));
            } 
        }
    }
    
    
    private boolean isBrandingForModuleEntry(final File srcDir, final BrandableModule mEntry) {
        return mEntry.getRelativePath().equals(PropertyUtils.relativizeFile(brandingDir, srcDir));
    }
    
    public final class BundleKey {
        private final File brandingBundle;
        private final BrandableModule moduleEntry;
        private final @NonNull String key;
        private @NonNull String value;
        private boolean modified = false;
        
        private BundleKey(final BrandableModule moduleEntry, final File brandingBundle, final String key, final String value) {
            this.moduleEntry = moduleEntry;
            assert key != null && value != null;
            this.key = key;
            this.value = value;
            this.brandingBundle = brandingBundle;
        }
        
        private BundleKey(final BrandableModule mEntry, final String key, final String value) {
            this(mEntry, getLocalizingBundle(mEntry), key,value);
        }
        
        BrandableModule getModuleEntry() {
            return moduleEntry;
        }
        
        @NonNull String getKey() {
            return key;
        }
        
        public @NonNull String getValue() {
            return value;
        }
        
        public void setValue(@NonNull String value) {
            assert value != null;
            if (!this.value.equals(value)) {
                modified = true;
            }
            this.value = value;
        }
        
        @Override
        public boolean equals(Object obj) {
            boolean retval = false;
            
            if (obj instanceof BundleKey) {
                BundleKey bKey = (BundleKey)obj;
                retval = getKey().equals(bKey.getKey())
                && getModuleEntry().equals(bKey.getModuleEntry())
                && getBrandingBundle().equals(bKey.getBrandingBundle());
            }
            
            return  retval;
        }
        
        @Override
        public int hashCode() {
            return 0;
        }
        
        boolean isModified() {
            return modified;
        }
        
        private File getBrandingBundle() {
            return brandingBundle;
        }

        String getBundleFilePath() {
            return  brandingBundle.getPath();
        }
    }
    
    public final class BrandedFile {
        private final BrandableModule moduleEntry;
        private final String entryPath;
        private @NonNull URL brandingSource;
        private boolean modified = false;
        
        private BrandedFile(final BrandableModule moduleEntry, final String entry) throws MalformedURLException {
            this(moduleEntry, null, entry);
        }
        
        private BrandedFile(final BrandableModule moduleEntry, final URL source, final String entry) throws MalformedURLException {
            this.moduleEntry = moduleEntry;
            this.entryPath = entry;
            if (source == null) {
                brandingSource = Utilities.toURI(moduleEntry.getJarLocation()).toURL();
                brandingSource =  new URL("jar:" + brandingSource + "!/" + entryPath); // NOI18N
            } else {
                brandingSource = source;
            }
            
        }
        
        File getFileLocation() {
            return new File(getModuleEntryDirectory(moduleEntry), entryPath);
        }
        
        public @NonNull URL getBrandingSource()  {
            return brandingSource;
        }
        
        public void setBrandingSource(@NonNull URL brandingSource) {
            Parameters.notNull("brandingSource", brandingSource);
            if (!Utilities.compareObjects(brandingSource, this.brandingSource)) {
                modified = true;
            }
            this.brandingSource = brandingSource;
        }
        
        boolean isModified() {
            return modified;
        }
        
        @Override
        public boolean equals(Object obj) {
            boolean retval = false;
            
            if (obj instanceof BrandedFile) {
                BrandedFile bFile = (BrandedFile)obj;
                retval = moduleEntry.equals(bFile.moduleEntry)
                && entryPath.equals(bFile.entryPath);
            }
            
            //if ()
            return  retval;
        }

        @Override
        public int hashCode() {
            return 0;
        }
        
    }
}
