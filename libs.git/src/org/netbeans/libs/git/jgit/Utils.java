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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.libs.git.jgit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectDatabase;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefComparator;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.NotTreeFilter;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.netbeans.libs.git.GitBranch;
import org.netbeans.libs.git.GitException;
import org.netbeans.libs.git.GitObjectType;
import org.netbeans.libs.git.GitRevisionInfo;
import org.netbeans.libs.git.GitRevisionInfo.GitFileInfo;
import org.netbeans.libs.git.jgit.commands.ListBranchCommand;
import org.netbeans.libs.git.progress.ProgressMonitor;

/**
 *
 * @author ondra
 */
public final class Utils {
    private Utils () {
    }

    public static Repository getRepositoryForWorkingDir (File workDir) throws IOException, IllegalArgumentException {
         Repository repo = new FileRepositoryBuilder().setWorkTree(workDir).build();
         repo.getConfig().setBoolean("pack", null, "buildbitmaps", false);
         return repo;
    }

    public static File getMetadataFolder (File workDir) {
        return new File(workDir, Constants.DOT_GIT);
    }

    public static boolean checkExecutable (Repository repository) {
        return repository.getConfig().getBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_FILEMODE, true);
    }
    
    public static Collection<PathFilter> getPathFilters (File workDir, File[] roots) {
        Collection<String> relativePaths = getRelativePaths(workDir, roots);
        return getPathFilters(relativePaths);
    }

    public static TreeFilter getExcludeExactPathsFilter (File workDir, File[] roots) {
        Collection<String> relativePaths = getRelativePaths(workDir, roots);
        TreeFilter filter = null;
        if (relativePaths.size() > 0) {
            Collection<PathFilter> filters = getPathFilters(relativePaths);
            List<TreeFilter> exactPathFilters = new LinkedList<TreeFilter>();
            for (PathFilter f : filters) {
                exactPathFilters.add(ExactPathFilter.create(f));
            }
            return NotTreeFilter.create(exactPathFilters.size() == 1 ? exactPathFilters.get(0) : OrTreeFilter.create(exactPathFilters));
        }
        return filter;
    }

    public static List<GitFileInfo> getDiffEntries (Repository repository, TreeWalk walk, GitClassFactory fac) throws IOException {
        List<GitFileInfo> result = new ArrayList<GitFileInfo>();
        List<DiffEntry> entries = DiffEntry.scan(walk);
        RenameDetector rd = new RenameDetector(repository);
        rd.addAll(entries);
        entries = rd.compute();
        for (DiffEntry e : entries) {
            GitRevisionInfo.GitFileInfo.Status status;
            File oldFile = null;
            String oldPath = null;
            String path = e.getOldPath();
            if (path == null) {
                path = e.getNewPath();
            }
            switch (e.getChangeType()) {
                case ADD:
                    status = GitRevisionInfo.GitFileInfo.Status.ADDED;
                    path = e.getNewPath();
                    break;
                case COPY:
                    status = GitRevisionInfo.GitFileInfo.Status.COPIED;
                    oldFile = new File(repository.getWorkTree(), e.getOldPath());
                    oldPath = e.getOldPath();
                    path = e.getNewPath();
                    break;
                case DELETE:
                    status = GitRevisionInfo.GitFileInfo.Status.REMOVED;
                    path = e.getOldPath();
                    break;
                case MODIFY:
                    status = GitRevisionInfo.GitFileInfo.Status.MODIFIED;
                    path = e.getOldPath();
                    break;
                case RENAME:
                    status = GitRevisionInfo.GitFileInfo.Status.RENAMED;
                    oldFile = new File(repository.getWorkTree(), e.getOldPath());
                    oldPath = e.getOldPath();
                    path = e.getNewPath();
                    break;
                default:
                    status = GitRevisionInfo.GitFileInfo.Status.UNKNOWN;
            }
            if (status == GitRevisionInfo.GitFileInfo.Status.RENAMED) {
                result.add(fac.createFileInfo(new File(repository.getWorkTree(), e.getOldPath()), e.getOldPath(), GitRevisionInfo.GitFileInfo.Status.REMOVED, null, null));
            }
            result.add(fac.createFileInfo(new File(repository.getWorkTree(), path), path, status, oldFile, oldPath));
        }
        return result;
    }

    public static Collection<PathFilter> getPathFilters (Collection<String> relativePaths) {
        Collection<PathFilter> filters = new ArrayList<>(relativePaths.size());
        for (String path : relativePaths) {
            filters.add(PathFilter.create(path));
        }
        return filters;
    }

    public static List<String> getRelativePaths(File workDir, File[] roots) {
        List<String> paths = new ArrayList<String>(roots.length);
        for (File root : roots) {
            if (workDir.equals(root)) {
                paths.clear();
                break;
            } else {
                paths.add(getRelativePath(workDir, root));
            }
        }
        return paths;
    }

    public static String getRelativePath (File repo, final File file) {
        return getRelativePath(repo, file, false);
    }

    public static Path getLinkPath (final Path p) throws IOException {
        return Files.readSymbolicLink(p);
    }

    private static String getRelativePath (File repo, final File file, boolean canonicalized) {
        StringBuilder relativePath = new StringBuilder("");
        File parent = file;
        if (!parent.equals(repo)) {
            while (parent != null && !parent.equals(repo)) {
                relativePath.insert(0, "/").insert(0, parent.getName()); //NOI18N
                parent = parent.getParentFile();
            }
            if (parent == null) {
                if (!canonicalized) {
                    try {
                        return getRelativePath(repo.getCanonicalFile(), file.getCanonicalFile(), true);
                    } catch (IOException ex) {
                        Logger.getLogger(Utils.class.getName()).log(Level.FINE, null, ex);
                    }
                }
                throw new IllegalArgumentException(file.getPath() + " is not under " + repo.getPath());
            }
            relativePath.deleteCharAt(relativePath.length() - 1);
        }
        return relativePath.toString();
    }

    /**
     * Returns true if the current file/folder specified by the given TreeWalk lies under any of the given filters
     * @param treeWalk
     * @param filters
     * @return
     */
    public static boolean isUnderOrEqual (TreeWalk treeWalk, Collection<PathFilter> filters) {
        boolean retval = filters.isEmpty();
        for (PathFilter filter : filters) {
            if (filter.include(treeWalk) && treeWalk.getPathString().length() >= filter.getPath().length()) {
                retval = true;
                break;
            }
        }
        return retval;
    }

    public static Collection<byte[]> getPaths (Collection<PathFilter> pathFilters) {
        Collection<byte[]> paths = new LinkedList<byte[]>();
        for (PathFilter filter : pathFilters) {
            paths.add(Constants.encode(filter.getPath()));
        }
        return paths;
    }

    public static RevCommit findCommit (Repository repository, String revision) throws GitException.MissingObjectException, GitException {
        return findCommit(repository, revision, null);
    }
    
    public static RevCommit findCommit (Repository repository, String revision, RevWalk walk) throws GitException.MissingObjectException, GitException {
        ObjectId commitId = parseObjectId(repository, revision);
        if (commitId == null) {
            throw new GitException.MissingObjectException(revision, GitObjectType.COMMIT);
        }
        return findCommit(repository, commitId, walk);
    }
    
    public static RevCommit findCommit (Repository repository, ObjectId commitId, RevWalk walk) throws GitException.MissingObjectException, GitException {
        try {
            return (walk == null ? new RevWalk(repository) : walk).parseCommit(commitId);
        } catch (MissingObjectException ex) {
            throw new GitException.MissingObjectException(commitId.name(), GitObjectType.COMMIT, ex);
        } catch (IncorrectObjectTypeException ex) {
            throw new GitException(MessageFormat.format(Utils.getBundle(Utils.class).getString("MSG_Exception_IdNotACommit"), commitId.name())); //NOI18N
        } catch (IOException ex) {
            throw new GitException(ex);
        }
    }

    public static ObjectId parseObjectId (Repository repository, String objectId) throws GitException {
        try {
            return repository.resolve(objectId);
        } catch (RevisionSyntaxException ex) {
            throw new GitException.MissingObjectException(objectId, GitObjectType.COMMIT, ex);
        } catch (AmbiguousObjectException ex) {
            throw new GitException(MessageFormat.format(Utils.getBundle(Utils.class).getString("MSG_Exception_IdNotACommit"), objectId), ex); //NOI18N
        } catch (IOException ex) {
            throw new GitException(ex);
        }
    }

    public static RevObject findObject (Repository repository, String objectId) throws GitException.MissingObjectException, GitException {
        try {
            ObjectId commitId = parseObjectId(repository, objectId);
            if (commitId == null) {
                throw new GitException.MissingObjectException(objectId, GitObjectType.UNKNOWN);
            }
            return new RevWalk(repository).parseAny(commitId);
        } catch (MissingObjectException ex) {
            throw new GitException.MissingObjectException(objectId, GitObjectType.UNKNOWN, ex);
        } catch (IOException ex) {
            throw new GitException(ex);
        }
    }

    /**
     * Recursively deletes the file or directory.
     *
     * @param file file/directory to delete
     */
    public static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File [] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteRecursively(files[i]);
            }
        }
        file.delete();
    }
    
    /**
     * Eliminates part of the ref's name that equals knon prefixes such as refs/heads/, refs/remotes/ etc.
     * @param ref
     * @return 
     */
    public static String getRefName (Ref ref) {
        String name = ref.getName();
        for (String prefix : Arrays.asList(Constants.R_HEADS, Constants.R_REMOTES, Constants.R_TAGS, Constants.R_REFS)) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length());
            }
        }
        return name;
    }

    /**
     * Transforms references into GitBranches
     * @param allRefs all references found
     * @param prefix prefix denoting heads amongst references
     * @return 
     */
    public static Map<String, GitBranch> refsToBranches (Collection<Ref> allRefs, String prefix, GitClassFactory factory) {
        Map<String, GitBranch> branches = new LinkedHashMap<String, GitBranch>();
        
        // try to find the head first - it usually is the active remote branch
        Ref head = null;
        for (final Ref ref : allRefs) {
            if (ref.getLeaf().getName().equals(Constants.HEAD)) {
                head = ref;
                break;
            }
        }
        
        // get all refs/heads
        for (final Ref ref : RefComparator.sort(allRefs)) {
            String refName = ref.getLeaf().getName();
            if (refName.startsWith(prefix)) {
                String name = refName.substring(prefix.length());
                ObjectId id = ref.getLeaf().getObjectId();
                if (id == null) {
                    // can happen, e.g. when the repository has no HEAD yet
                    Logger.getLogger(Utils.class.getName()).log(Level.INFO, "Null object id for ref: {0}, {1}:{2}, {3}", //NOI18N
                            new Object[] { ref.toString(), ref.getName(), ref.getObjectId(), ref.getLeaf() } );
                    continue;
                }
                branches.put(
                    name, 
                    factory.createBranch(
                        name, 
                        false, 
                        head != null && ref.getObjectId().equals(head.getObjectId()), 
                        id));
            }
        }
        return branches;
    }

    /**
     * Transforms references into pairs of tag name/id
     * @param allRefs all references found
     * @return 
     */
    public static Map<String, String> refsToTags (Collection<Ref> allRefs) {
        Map<String, String> tags = new LinkedHashMap<String, String>();
        
        // get all refs/tags
        for (final Ref ref : RefComparator.sort(allRefs)) {
            String refName = ref.getLeaf().getName();
            if (refName.startsWith(Constants.R_TAGS)) {
                String name = refName.substring(Constants.R_TAGS.length());
                tags.put(name, ObjectId.toString(ref.getLeaf().getObjectId()));
            }
        }
        return tags;
    }

    /**
     * Returns a resource bundle contained in the same package the given clazz is.
     * @param clazz
     * @return 
     */
    public static ResourceBundle getBundle (Class clazz) {
        String pref = clazz.getName();
        int last = pref.lastIndexOf('.');

        if (last >= 0) {
            pref = pref.substring(0, last + 1) + "Bundle"; //NOI18N
        } else {
            // base package, search for bundle
            pref = "Bundle"; // NOI18N
        }
        return ResourceBundle.getBundle(pref);
    }

    public static boolean isFromNested (int fm) {
        return fm == FileMode.TYPE_GITLINK;
    }

    public static GitBranch getTrackedBranch (Config config, String branchName, Map<String, GitBranch> allBranches) {
        String remoteName = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE);
        String trackedBranchName = config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE);
        if (trackedBranchName != null) {
            if (trackedBranchName.startsWith(Constants.R_HEADS)) {
                trackedBranchName = trackedBranchName.substring(Constants.R_HEADS.length());
            } else if (trackedBranchName.startsWith(Constants.R_REMOTES)) {
                trackedBranchName = trackedBranchName.substring(Constants.R_REMOTES.length());
            }
        }
        if (trackedBranchName == null) {
            return null;
        } else {
            if (remoteName != null && ".".equals(remoteName)) { //NOI18N
                remoteName = ""; //NOI18N
            } else {
                remoteName = remoteName + "/"; //NOI18N
            }
            return allBranches.get(remoteName + trackedBranchName);
        }
    }

    public static Map getAllBranches (Repository repository, GitClassFactory fac, ProgressMonitor monitor) throws GitException {
        ListBranchCommand cmd = new ListBranchCommand(repository, fac, true, monitor);
        cmd.execute();
        return cmd.getBranches();
    }

    public static RawText getRawText (ObjectId id, ObjectDatabase db) throws IOException {
        if (id.equals(ObjectId.zeroId())) {
            return RawText.EMPTY_TEXT;
        }
        return new RawText(db.open(id, Constants.OBJ_BLOB).getCachedBytes());
    }
}
