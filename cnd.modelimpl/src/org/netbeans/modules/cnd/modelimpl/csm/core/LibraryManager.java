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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.cnd.modelimpl.csm.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmModelAccessor;
import org.netbeans.modules.cnd.api.model.CsmProject;
import org.netbeans.modules.cnd.api.model.CsmUID;
import org.netbeans.modules.cnd.apt.support.ResolvedPath;
import org.netbeans.modules.cnd.modelimpl.debug.TraceFlags;
import org.netbeans.modules.cnd.modelimpl.repository.PersistentUtils;
import org.netbeans.modules.cnd.modelimpl.uid.UIDCsmConverter;
import org.netbeans.modules.cnd.repository.spi.RepositoryDataInput;
import org.netbeans.modules.cnd.repository.spi.RepositoryDataOutput;
import org.netbeans.modules.cnd.repository.support.AbstractObjectFactory;
import org.netbeans.modules.cnd.utils.CndPathUtilitities;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.openide.filesystems.FileSystem;

/**
 * Artificial libraries manager.
 * Manage auto created libraries (artificial libraries) for included files.
 *
 *
 * @author Alexander Simon
 */
public final class LibraryManager {

    private static final LibraryManager instance = new LibraryManager();

    public static LibraryManager getInstance() {
        return instance;
    }

    private LibraryManager() {
    }    
    private final Map<LibraryKey, LibraryEntry> librariesEntries = new ConcurrentHashMap<LibraryKey, LibraryEntry>();
    
    private static final class Lock {}
    private final Object lock = new Lock();

    public void shutdown(){
        librariesEntries.clear();
    }
    
    /**
     * Returns collection of artificial libraries used in project
     */
    public List<LibProjectImpl> getLibraries(ProjectImpl project) {
        List<LibProjectImpl> res = new ArrayList<LibProjectImpl>();
        CsmUID<CsmProject> projectUid = project.getUID();
        for (LibraryEntry entry : librariesEntries.values()) {
            if (entry.containsProject(projectUid)) {
                LibProjectImpl lib = (LibProjectImpl) entry.getLibrary().getObject();
                if (lib != null) {
                    res.add(lib);
                }
            }
        }
        return res;
    }

    /**
     * Returns collection uids of artificial libraries used in project
     */
    public Collection<CsmUID<CsmProject>> getLirariesKeys(CsmUID<CsmProject> projectUid) {
        List<CsmUID<CsmProject>> res = new ArrayList<CsmUID<CsmProject>>();
        for (LibraryEntry entry : librariesEntries.values()) {
            if (entry.containsProject(projectUid)) {
                res.add(entry.getLibrary());
            }
        }
        return res;
    }

    private void trace(String where, FileImpl curFile, ResolvedPath resolvedPath, ProjectBase res, ProjectBase start) {
        System.out.println("Resolved Path " + resolvedPath.getPath()); //NOI18N
        System.out.println("    start project " + start); //NOI18N
        System.out.println("    found in " + where + " " + res); //NOI18N
        System.out.println("    included from " + curFile); //NOI18N
        System.out.println("    file from project " + curFile.getProject()); //NOI18N
        for (CsmProject prj : start.getLibraries()) {
            System.out.println("    search lib " + prj); //NOI18N
        }
    }

    /**
     * Find project for resolved file.
     * Search for project in project, dependencies, artificial libraries.
     * If search is false then method creates artificial library or returns base project.
     */
    public ProjectBase resolveFileProjectOnInclude(ProjectBase baseProject, FileImpl curFile, ResolvedPath resolvedPath) {
        String absPath = resolvedPath.getPath().toString();
        Set<ProjectBase> antiLoop = new HashSet<ProjectBase>();
        ProjectBase res = searchInProjectFiles(baseProject, resolvedPath, antiLoop);
        if (res != null) {
            if (TraceFlags.TRACE_RESOLVED_LIBRARY) {
                trace("Projects", curFile, resolvedPath, res, baseProject);//NOI18N
            }
            return res;
        }
        final String folder = resolvedPath.getFolder().toString(); // always normalized
        antiLoop.clear();
        res = searchInProjectRoots(baseProject, resolvedPath.getFileSystem(), getPathToFolder(folder, absPath), antiLoop);
        if (res != null) {
            if (TraceFlags.TRACE_RESOLVED_LIBRARY) {
                trace("Projects roots", curFile, resolvedPath, res, baseProject);//NOI18N
            }
            return res;
        }
        res = searchInProjectFilesArtificial(baseProject, resolvedPath, antiLoop);
        if (res != null) {
            if (TraceFlags.TRACE_RESOLVED_LIBRARY) {
                trace("Libraries", curFile, resolvedPath, res, baseProject);//NOI18N
            }
            return res;
        }
        synchronized (lock) {
            antiLoop.clear();
            res = searchInProjectRootsArtificial(baseProject, resolvedPath.getFileSystem(), getPathToFolder(folder, absPath), antiLoop);
            if (res == null) {
                if (resolvedPath.isDefaultSearchPath()) {
                    res = curFile.getProjectImpl(true);
                    if (TraceFlags.TRACE_RESOLVED_LIBRARY) {
                        trace("Base Project as Default Search Path", curFile, resolvedPath, res, baseProject);//NOI18N
                    }
                } else if (!baseProject.isArtificial()) {
                    res = getLibrary((ProjectImpl) baseProject, resolvedPath.getFileSystem(), folder);
                    if (res == null) {
                        if (CndUtils.isDebugMode()) {
                            trace("Not created library for folder " + folder, curFile, resolvedPath, res, baseProject); //NOI18N
                        }
                        res = baseProject;
                    }
                    if (TraceFlags.TRACE_RESOLVED_LIBRARY) {
                        trace("Library for folder " + folder, curFile, resolvedPath, res, baseProject); //NOI18N
                    }
                } else {
                    res = baseProject;
                    if (TraceFlags.TRACE_RESOLVED_LIBRARY) {
                        trace("Base Project", curFile, resolvedPath, res, baseProject);//NOI18N
                    }
                }
            } else {
                if (TraceFlags.TRACE_RESOLVED_LIBRARY) {
                    trace("Libraries roots", curFile, resolvedPath, res, baseProject);//NOI18N
                }
            }
        }
        return res;
    }

    private List<String> getPathToFolder(String folder, String path) {
        List<String> res = new ArrayList<String>(3);
        res.add(folder);
        if (path.startsWith(folder)) {
            while (true) {
                String dir = CndPathUtilitities.getDirName(path);
                if (dir == null || folder.equals(dir) || !dir.startsWith(folder)) {
                    break;
                }
                res.add(dir);
                if (res.size() == 3) {
                    break;
                }
                path = dir;
            }
        }
        return res;
    }

    private ProjectBase searchInProjectFiles(ProjectBase baseProject, ResolvedPath searchFor, Set<ProjectBase> set) {
        if (set.contains(baseProject)) {
            return null;
        }
        set.add(baseProject);
        if (baseProject.getFileSystem() == searchFor.getFileSystem()) {
            baseProject.ensureFilesCreated();
            CsmUID<CsmFile> file = baseProject.getFileUID(searchFor.getPath(), true);
            if (file != null) {
                return baseProject;
            }
        }
        List<CsmProject> libraries = baseProject.getLibraries();
        for (CsmProject prj : libraries) {
            if (prj.isArtificial()) {
                break;
            }
            ProjectBase res = searchInProjectFiles((ProjectBase) prj, searchFor, set);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    private ProjectBase searchInProjectFilesArtificial(ProjectBase baseProject, ResolvedPath searchFor, Set<ProjectBase> antiLoop) {
        List<CsmProject> libraries = baseProject.getLibraries();
        for (CsmProject prj : libraries) {
            if (prj.isArtificial()) {
                antiLoop.clear();
                ProjectBase res = searchInProjectFiles((ProjectBase) prj, searchFor, antiLoop);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    private ProjectBase searchInProjectRoots(ProjectBase baseProject, FileSystem fs, List<String> folders, Set<ProjectBase> set) {
        if (set.contains(baseProject)) {
            return null;
        }
        set.add(baseProject);
        if (baseProject.getFileSystem() == fs) {
            for (String folder : folders) {
                if (baseProject.isMySource(folder)) {
                    return baseProject;
                }
            }
        }
        List<CsmProject> libraries = baseProject.getLibraries();
        for (CsmProject prj : libraries) {
            if (prj.isArtificial()) {
                break;
            }
            ProjectBase res = searchInProjectRoots((ProjectBase) prj, fs, folders, set);
            if (res != null) {
                return res;
            }
        }
        return null;
    }

    private ProjectBase searchInProjectRootsArtificial(ProjectBase baseProject, FileSystem fs, List<String> folders, Set<ProjectBase> set) {
        List<CsmProject> libraries = baseProject.getLibraries();
        ProjectBase candidate = null;
        for (CsmProject prj : libraries) {
            if (prj.isArtificial()) {
                set.clear();
                ProjectBase res = searchInProjectRoots((ProjectBase) prj, fs, folders, set);
                if (res != null) {
                    if (candidate == null) {
                        candidate = res;
                    } else {
                        CharSequence path1 = ((LibProjectImpl)candidate).getPath();
                        CharSequence path2 = ((LibProjectImpl)res).getPath();
                        if (path2.length() > path1.length()) {
                            candidate = res;
                        }
                    }
                }
            }
        }
        return candidate;
    }

    private LibProjectImpl getLibrary(ProjectImpl project, FileSystem fs, String folder) {
        CsmUID<CsmProject> projectUid = project.getUID();
        LibraryKey libraryKey = new LibraryKey(fs, folder);
        LibraryEntry entry = librariesEntries.get(libraryKey);
        if (entry == null) {
            entry = getOrCreateLibrary(libraryKey);
        }
        if (!entry.containsProject(projectUid)) {
            entry.addProject(projectUid);
        }
        return (LibProjectImpl) entry.getLibrary().getObject();
    }

    private LibraryEntry getOrCreateLibrary(LibraryKey libraryKey) {
        LibraryEntry entry = librariesEntries.get(libraryKey);
        if (entry == null) {
            boolean needFire = false;
            synchronized (lock) {
                entry = librariesEntries.get(libraryKey);
                if (entry == null) {                    
                    entry = new LibraryEntry(libraryKey);
                    librariesEntries.put(libraryKey, entry);
                    needFire = true;
                }
            }
            if (needFire) {
                final LibraryEntry passEntry = entry;
                ModelImpl.instance().enqueueModelTask(new Runnable() {

                    @Override
                    public void run() {
                        ListenersImpl.getImpl().fireProjectOpened((ProjectBase) passEntry.getLibrary().getObject());
                    }
                }, "postponed library opened " + libraryKey.folder); // NOI18N
            }
        }
        return entry;
    }

    public void onProjectPropertyChanged(CsmUID<CsmProject> project) {
        for (LibraryEntry entry : librariesEntries.values()) {
            entry.removeProject(project);
        }
    }

    /**
     * Close unused artificial libraries.
     */
    public void onProjectClose(CsmUID<CsmProject> project) {
        List<LibraryEntry> toClose = new ArrayList<LibraryEntry>();
        for (LibraryEntry entry : librariesEntries.values()) {
            entry.removeProject(project);
            if (entry.isEmpty()) {
                toClose.add(entry);
            }
        }
        if (toClose.size() > 0) {
            for (LibraryEntry entry : toClose) {
                librariesEntries.remove(entry.getKey());
            }
        }
        closeLibraries(toClose);
    }

    /*package*/
    final void cleanLibrariesData(Collection<LibProjectImpl> libs) {
        for (LibProjectImpl entry : libs) {
            librariesEntries.remove(new LibraryKey(entry.getFileSystem(), entry.getPath().toString()));
            entry.dispose(true);
        }
    }

    private void closeLibraries(Collection<LibraryEntry> entries) {
        ModelImpl model = (ModelImpl) CsmModelAccessor.getModel();
        for (LibraryEntry entry : entries) {
            CsmUID<CsmProject> uid = entry.getLibrary();
            ProjectBase lib = (ProjectBase) uid.getObject();
            assert lib != null : "Null project for UID " + uid;
            model.disposeProject(lib);
        }
    }

    /**
     * Write artificial libraries for project
     */
    /*package-local*/ void writeProjectLibraries(CsmUID<CsmProject> project, RepositoryDataOutput aStream) throws IOException {
        assert aStream != null;
        Set<LibraryKey> keys = new HashSet<LibraryKey>();
        for (Map.Entry<LibraryKey, LibraryEntry> entry : librariesEntries.entrySet()) {
            if (entry.getValue().containsProject(project)) {
                keys.add(entry.getKey());
            }
        }        
        aStream.writeInt(keys.size());
        for (LibraryKey libraryKey : keys) {
            libraryKey.write(aStream);
        }
    }

    /**
     * Read artificial libraries for project
     */
    /*package-local*/ void readProjectLibraries(CsmUID<CsmProject> project, RepositoryDataInput input) throws IOException {
        assert input != null;
        int len = input.readInt();
        if (len != AbstractObjectFactory.NULL_POINTER) {
            for (int i = 0; i < len; i++) {
                LibraryKey key =  new LibraryKey(input);
                LibraryEntry entry =  getOrCreateLibrary(key);
                entry.addProject(project);
            }
        }
    }

    private static final class LibraryKey {

        private final FileSystem fileSystem;
        private final String folder;

        public LibraryKey(FileSystem fileSystem, String folder) {
            this.fileSystem = fileSystem;
            this.folder = folder;
        }

        private LibraryKey(RepositoryDataInput input) throws IOException {
            this.fileSystem = PersistentUtils.readFileSystem(input);
            this.folder = input.readUTF();
        }
        
        private void write(RepositoryDataOutput out) throws IOException {
            PersistentUtils.writeFileSystem(fileSystem, out);
            out.writeUTF(folder);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LibraryKey other = (LibraryKey) obj;
            if ((this.folder == null) ? (other.folder != null) : !this.folder.equals(other.folder)) {
                return false;
            }
            if (this.fileSystem != other.fileSystem && (this.fileSystem == null || !this.fileSystem.equals(other.fileSystem))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 83 * hash + (this.folder != null ? this.folder.hashCode() : 0);
            hash = 83 * hash + (this.fileSystem != null ? this.fileSystem.hashCode() : 0);
            return hash;
        }
    }

    private static final class LibraryEntry {

        private final LibraryKey key;
        private CsmUID<CsmProject> libraryUID;
        private final ConcurrentMap<CsmUID<CsmProject>, Boolean> dependentProjects;

        private LibraryEntry(LibraryKey folder) {
            this.key = folder;
            dependentProjects = new ConcurrentHashMap<CsmUID<CsmProject>, Boolean>();
        }

        private String getFolder() {
            return key.folder;
        }

        private FileSystem getFileSystem() {
            return key.fileSystem;
        }

        public LibraryKey getKey() {
            return key;
        }
        
        private CsmUID<CsmProject> getLibrary() {
            if (libraryUID == null) {
                createUID();
            }
            return libraryUID;
        }

        private synchronized void createUID() {
            if (libraryUID == null) {
                ModelImpl model = (ModelImpl) CsmModelAccessor.getModel();
                LibProjectImpl library = LibProjectImpl.createInstance(model, getFileSystem(), getFolder());
                libraryUID = library.getUID();
            }
        }

        private boolean isEmpty() {
            return dependentProjects.size() == 0;
        }

        private boolean containsProject(CsmUID<CsmProject> project) {
            return dependentProjects.containsKey(project);
        }

        private void addProject(CsmUID<CsmProject> project) {
            dependentProjects.put(project, Boolean.TRUE);
        }

        private void removeProject(CsmUID<CsmProject> project) {
            dependentProjects.remove(project);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("folder=").append(key).append(",\nlibraryUID=").append(libraryUID);// NOI18N
            if (dependentProjects.isEmpty()) {
                sb.append(" NO DEPENDENT PROJECTS!");// NOI18N
            } else {
                sb.append("\ndependentProjects=");// NOI18N
                for (CsmUID<CsmProject> csmUID : dependentProjects.keySet()) {
                    sb.append("\n(").append(System.identityHashCode(csmUID)).append(")").append(csmUID);// NOI18N
                }
            }
            return sb.toString();
        }
        
    }
    
    public void dumpInfo(PrintWriter printOut,boolean withContainers) {
        printOut.printf("LibraryManager: libs=%d\n", librariesEntries.size());// NOI18N
        int ind = 1;
        for (Map.Entry<LibraryKey, LibraryEntry> entry : librariesEntries.entrySet()) {
            printOut.printf("Lib[%d] %s with LibEntry %s\n", ind++, entry.getKey(), entry.getValue());// NOI18N
            if (withContainers) {
                CsmProject library = UIDCsmConverter.UIDtoProject(entry.getValue().libraryUID);
                if (library == null) {
                    printOut.printf("Library was NOT restored from repository\n");// NOI18N
                } else if (library instanceof ProjectBase) {
                    printOut.printf("[%d] disposing=%s\n", ind, ((ProjectBase)library).isDisposing());// NOI18N
                    ((ProjectBase)library).traceFileContainer(printOut);
                } else {
                    printOut.printf("Library's project has unexpected class type %s\n", library.getClass().getName());// NOI18N
                }
            }
        }
    }

}
