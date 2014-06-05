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

package org.netbeans.modules.parsing.spi.indexing.support;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.search.BooleanClause;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.impl.RunWhenScanFinishedSupport;
import org.netbeans.modules.parsing.impl.Utilities;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.IndexFactoryImpl;
import org.netbeans.modules.parsing.impl.indexing.IndexingModule;
import org.netbeans.modules.parsing.impl.indexing.PathRecognizerRegistry;
import org.netbeans.modules.parsing.impl.indexing.PathRegistry;
import org.netbeans.modules.parsing.impl.indexing.RepositoryUpdater;
import org.netbeans.modules.parsing.impl.indexing.SPIAccessor;
import org.netbeans.modules.parsing.impl.indexing.TransientUpdateSupport;
import org.netbeans.modules.parsing.impl.indexing.URLCache;
import org.netbeans.modules.parsing.impl.indexing.Util;
import org.netbeans.modules.parsing.impl.indexing.friendapi.IndexingController;
import org.netbeans.modules.parsing.impl.indexing.lucene.LayeredDocumentIndex;
import org.netbeans.modules.parsing.impl.indexing.lucene.LuceneIndexFactory;
import org.netbeans.modules.parsing.lucene.support.Convertor;
import org.netbeans.modules.parsing.lucene.support.DocumentIndex2;
import org.netbeans.modules.parsing.lucene.support.Index;
import org.netbeans.modules.parsing.lucene.support.IndexDocument;
import org.netbeans.modules.parsing.lucene.support.Queries;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.URLMapper;
import org.openide.util.Pair;
import org.openide.util.Parameters;

/**
 *
 * @author Tomas Zezula
 */
public final class QuerySupport {

    /**
     * Gets classpath roots relevant for a file. This method tries to find
     * classpath roots for a given files. It looks at classpaths specified by
     * <code>sourcePathIds</code>, <code>libraryPathIds</code> and
     * <code>binaryLibraryPathIds</code> parameters.
     *
     * <p>The roots collected from <code>binaryLibraryPathIds</code> will be translated
     * by the <code>SourceForBinaryQuery</code> in order to find relevant sources root.
     * The roots collected from <code>libraryPathIds</code> are expected to be
     * libraries in their sources form (ie. no translation).
     *
     * @param f The file to find roots for.
     * @param sourcePathIds The IDs of source classpath to look at.
     * @param libraryPathIds The IDs of library classpath to look at.
     * @param binaryLibraryPathIds The IDs of binary library classpath to look at.
     *
     * @return The collection of roots for a given file. It may be empty, but never <code>null</code>.
     * 
     * @since 1.6
     */
    public static Collection<FileObject> findRoots(
            FileObject f,
            Collection<String> sourcePathIds,
            Collection<String> libraryPathIds,
            Collection<String> binaryLibraryPathIds)
    {
        Collection<FileObject> roots = new HashSet<FileObject>();

        if (sourcePathIds == null) {
            sourcePathIds = PathRecognizerRegistry.getDefault().getSourceIds();
        }

        if (libraryPathIds == null) {
            libraryPathIds = PathRecognizerRegistry.getDefault().getLibraryIds();
        }

        if (binaryLibraryPathIds == null) {
            binaryLibraryPathIds = PathRecognizerRegistry.getDefault().getBinaryLibraryIds();
        }

        collectClasspathRoots(f, sourcePathIds, false, roots);
        collectClasspathRoots(f, libraryPathIds, false, roots);
        collectClasspathRoots(f, binaryLibraryPathIds, true, roots);

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Roots for file " + f //NOI18N
                    + ", sourcePathIds=" + sourcePathIds //NOI18N
                    + ", libraryPathIds=" + libraryPathIds //NOI18N
                    + ", binaryPathIds=" + binaryLibraryPathIds //NOI18N
                    + ": "); //NOI18N
            for(FileObject root : roots) {
                try {
                    LOG.fine("  " + root.getURL()); //NOI18N
                } catch (FileStateInvalidException ex) {
                    //ignore
                }
            }
            LOG.fine("----"); //NOI18N
        }

        return roots != null ? roots : Collections.<FileObject>emptySet();
    }

    /**
     * Returns the dependent source roots for given source root. It returns
     * all the source roots which have either direct or transitive dependency on
     * the given source root.
     *
     * @param root to find the dependent roots for
     * @param filterNonOpenedProjects if true the results contains only roots of
     * opened projects
     * @return {@link Collection} of {@link FileObject}s containing at least the incoming
     * root, never returns null.
     * @since 1.64
     */
    @NonNull
    @org.netbeans.api.annotations.common.SuppressWarnings(value={"DMI_COLLECTION_OF_URLS"}, justification="URLs have never host part")
    public static Collection<FileObject> findDependentRoots(
            @NonNull final FileObject file,
            final boolean filterNonOpenedProjects) {
        Parameters.notNull("file", file);   //NOI18N

        final URL root = findOwnerRoot(file);
        if (root == null) {
            return Collections.<FileObject>emptySet();
        }
        final IndexingController ic = IndexingController.getDefault();
        final Map<URL, List<URL>> binaryDeps = ic.getBinaryRootDependencies();
        final Map<URL, List<URL>> sourceDeps = ic.getRootDependencies();
        final Map<URL, List<URL>> peerDeps = ic.getRootPeers();
        Set<URL> urls;

        if (sourceDeps.containsKey(root)) {
            urls = Util.findReverseSourceRoots(root, sourceDeps, peerDeps);
        } else {
            urls = new HashSet<URL>();
            final FileObject rootFo = URLMapper.findFileObject(root);
            if (rootFo != null) {
                for (URL binary : findBinaryRootsForSourceRoot(rootFo, binaryDeps)) {
                    List<URL> deps = binaryDeps.get(binary);
                    if (deps != null) {
                        urls.addAll(deps);
                    }
                }
            }
        }

        if(filterNonOpenedProjects) {
            final GlobalPathRegistry gpr = GlobalPathRegistry.getDefault();
            final Set<ClassPath> cps = new HashSet<ClassPath>();
            for (String id : PathRecognizerRegistry.getDefault().getSourceIds()) {
                cps.addAll(gpr.getPaths(id));
            }
            Set<URL> toRetain = new HashSet<URL>();
            for (ClassPath cp : cps) {
                for (ClassPath.Entry e : cp.entries()) {
                    toRetain.add(e.getURL());
                }
            }
            urls.retainAll(toRetain);
        }
        return mapToFileObjects(urls);
    }

    /**
     * Gets classpath roots relevant for a project. This method tries to find
     * classpaths with <code>sourcePathIds</code>, <code>libraryPathIds</code> and
     * <code>binaryPathIds</code> supplied by the <code>project</code>.
     *
     * <p>The roots collected from <code>binaryLibraryPathIds</code> will be translated
     * by the <code>SourceForBinaryQuery</code> in order to find relevant sources root.
     * The roots collected from <code>libraryPathIds</code> are expected to be
     * libraries in their sources form (ie. no translation).
     *
     * @param project The project to find the roots for. Can be <code>null</code> in
     *   which case the method searches in all registered classpaths.
     * @param sourcePathIds The IDs of source classpath to look at.
     * @param libraryPathIds The IDs of library classpath to look at.
     * @param binaryLibraryPathIds The IDs of binary library classpath to look at.
     *
     * @return The collection of roots for a given project. It may be empty, but never <code>null</code>.
     * 
     * @since 1.6
     */
    public static Collection<FileObject> findRoots(
            Project project,
            Collection<String> sourcePathIds,
            Collection<String> libraryPathIds,
            Collection<String> binaryLibraryPathIds)
    {
        Set<FileObject> roots = new HashSet<FileObject>();

        if (sourcePathIds == null) {
            sourcePathIds = PathRecognizerRegistry.getDefault().getSourceIds();
        }

        if (libraryPathIds == null) {
            libraryPathIds = PathRecognizerRegistry.getDefault().getLibraryIds();
        }

        if (binaryLibraryPathIds == null) {
            binaryLibraryPathIds = PathRecognizerRegistry.getDefault().getBinaryLibraryIds();
        }

        collectClasspathRoots(null, sourcePathIds, false, roots);
        collectClasspathRoots(null, libraryPathIds, false, roots);
        collectClasspathRoots(null, binaryLibraryPathIds, true, roots);

        if (project != null) {
            Set<FileObject> rootsInProject = new HashSet<FileObject>();
            for(FileObject root : roots) {
                if (FileOwnerQuery.getOwner(root) == project) {
                    rootsInProject.add(root);
                }
            }
            roots = rootsInProject;
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Roots for project " + project //NOI18N
                    + ", sourcePathIds=" + sourcePathIds //NOI18N
                    + ", libraryPathIds=" + libraryPathIds //NOI18N
                    + ", binaryPathIds=" + binaryLibraryPathIds //NOI18N
                    + ": "); //NOI18N
            for(FileObject root : roots) {
                try {
                    LOG.fine("  " + root.getURL()); //NOI18N
                } catch (FileStateInvalidException ex) {
                    //ignore
                }
            }
            LOG.fine("----"); //NOI18N
        }

        return roots;
    }

    public static QuerySupport forRoots (final String indexerName, final int indexerVersion, final URL... roots) throws IOException {
        Parameters.notNull("indexerName", indexerName); //NOI18N
        Parameters.notNull("roots", roots); //NOI18N
        return new QuerySupport(indexerName, indexerVersion, roots);
    }

    public static QuerySupport forRoots (final String indexerName, final int indexerVersion, final FileObject... roots) throws IOException {
        Parameters.notNull("indexerName", indexerName); //NOI18N
        Parameters.notNull("roots", roots); //NOI18N
        final List<URL> rootsURL = new ArrayList<URL>(roots.length);
        for (FileObject root : roots) {
            rootsURL.add(root.getURL());
        }
        return new QuerySupport(indexerName, indexerVersion, rootsURL.toArray(new URL[rootsURL.size()]));
    }

    public Collection<? extends IndexResult> query(
            final String fieldName,
            final String fieldValue,
            final Kind kind,
            final String... fieldsToLoad
    ) throws IOException {
        Parameters.notNull("fieldName", fieldName); //NOI18N
        Parameters.notNull("fieldValue", fieldValue); //NOI18N
        Parameters.notNull("kind", kind); //NOI18N
        return getQueryFactory().field(fieldName, fieldValue, kind).execute(fieldsToLoad);
    }

   /**
    * Returns the factory for creating composite queries.
    * @return the {@link QuerySupport.Query.Factory}
    * @since 1.66
    */
    @NonNull
    public Query.Factory getQueryFactory() {
        return new Query.Factory(this);
    }

    /**
     * Encodes a type of the name kind used by {@link QuerySupport#query}.
     *
     */
    public enum Kind {
        /**
         * The name parameter
         * is an exact simple name of the package or declared type.
         */
        EXACT,
        /**
         * The name parameter
         * is an case sensitive prefix of the package or declared type name.
         */
        PREFIX,
        /**
         * The name parameter is
         * an case insensitive prefix of the declared type name.
         */
        CASE_INSENSITIVE_PREFIX,
        /**
         * The name parameter is
         * an camel case of the declared type name.
         */
        CAMEL_CASE,
        /**
         * The name parameter is
         * an regular expression of the declared type name.
         */
        REGEXP,
        /**
         * The name parameter is
         * an case insensitive regular expression of the declared type name.
         */
        CASE_INSENSITIVE_REGEXP,

        CASE_INSENSITIVE_CAMEL_CASE;
    }

    /**
     * An index query.
     * @since 1.66
     */
    public static final class Query {

        private final QuerySupport qs;
        private final org.apache.lucene.search.Query queryImpl;

        private Query(
            @NonNull final QuerySupport qs,
            @NonNull final org.apache.lucene.search.Query queryImpl) {
            assert qs != null;
            assert queryImpl != null;
            this.qs = qs;
            this.queryImpl = queryImpl;
        }

        /**
         * Factory for an index queries.
         */
        public static final class Factory {

            private final QuerySupport qs;

            private Factory(@NonNull final QuerySupport qs) {
                assert qs != null;
                this.qs = qs;
            }

            /**
             * Creates a query for required field value.
             * @param fieldName the name of the tested field
             * @param fieldValue the required value of the tested field
             * @param kind the kind of the query
             * @return the newly created query
             */
            @NonNull
            public Query field(
                @NonNull final String fieldName,
                @NonNull final String fieldValue,
                @NonNull final Kind kind) {
                Parameters.notNull("fieldName", fieldName); //NOI18N
                Parameters.notNull("fieldValue", fieldValue);   //NOI18N
                Parameters.notNull("kind", kind);   //NOI18N
                return new Query(
                    qs,
                    Queries.createQuery(
                        fieldName,
                        fieldName,
                        fieldValue,
                        translateQueryKind(kind)));
            }

            /**
             * Creates a boolean AND query.
             * @param queries the queries to compose into the AND query
             * @return the newly created AND query
             */
            @NonNull
            public Query and(@NonNull final Query...queries) {
                Parameters.notNull("queries", queries);     //NOI18N
                final org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery();
                for (Query q : queries) {
                    bq.add(new BooleanClause(q.queryImpl, org.apache.lucene.search.BooleanClause.Occur.MUST));
                }
                return new Query(
                    qs,
                    bq);
            }

            /**
             * Creates a boolean OR query.
             * @param queries the queries to compose into the OR query
             * @return the newly created OR query
             */
            @NonNull
            public Query or(@NonNull final Query...queries) {
                Parameters.notNull("queries", queries);     //NOI18N
                final org.apache.lucene.search.BooleanQuery bq = new org.apache.lucene.search.BooleanQuery();
                for (Query q : queries) {
                    bq.add(new BooleanClause(q.queryImpl, org.apache.lucene.search.BooleanClause.Occur.SHOULD));
                }
                return new Query(
                    qs,
                    bq);
            }
        }

        /**
         * Executes the query.
         * @param fieldsToLoad the filter for fields to be loaded into the {@link IndexResult}s
         * @return the {@link Collection} of {@link IndexResult} matching the {@link QuerySupport.Query}
         * @throws IOException in case of IO error.
         */
        @NonNull
        public Collection<? extends IndexResult> execute(@NullAllowed final String... fieldsToLoad) throws IOException {
            try {
            return Utilities.runPriorityIO(new Callable<Collection<? extends IndexResult>>() {
                @Override
                public Collection<? extends IndexResult> call() throws Exception {
                    Iterable<? extends Pair<URL, LayeredDocumentIndex>> indices = qs.indexerQuery.getIndices(qs.roots);
                    // check if there are stale indices
                    for (Pair<URL, LayeredDocumentIndex> pair : indices) {
                        final LayeredDocumentIndex index = pair.second();
                        final Collection<? extends String> staleFiles = index.getDirtyKeys();
                        final boolean scanningThread = RunWhenScanFinishedSupport.isScanningThread();
                        LOG.log(
                            Level.FINE,
                            "Index: {0}, staleFiles: {1}, scanning thread: {2}",  //NOI18N
                            new Object[]{
                                index,
                                staleFiles,
                                scanningThread
                            });
                        if (!staleFiles.isEmpty() && !scanningThread && !TransientUpdateSupport.isTransientUpdate()) {
                            final URL root = pair.first();
                            LinkedList<URL> list = new LinkedList<URL>();
                            for (String staleFile : staleFiles) {
                                try {
                                    list.add(Util.resolveUrl(root, staleFile, false));
                                } catch (MalformedURLException ex) {
                                    LOG.log(Level.WARNING, null, ex);
                                }
                            }
                            TransientUpdateSupport.setTransientUpdate(true);
                            try {
                                RepositoryUpdater.getDefault().enforcedFileListUpdate(root,list);
                            } finally {
                                TransientUpdateSupport.setTransientUpdate(false);
                            }
                        }
                    }
                    final Queue<IndexResult> result = new ArrayDeque<IndexResult>();
                    for (Pair<URL, LayeredDocumentIndex> pair : indices) {
                        final DocumentIndex2 index = pair.second();
                        final URL root = pair.first();
                        final Collection<? extends IndexResult> pr =
                                index.query(
                                    queryImpl,
                                    new DocumentToResultConvertor(root),
                                    fieldsToLoad);
                        result.addAll(pr);  //TODO: Perf: Replace by ProxyCollection
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.log(
                                Level.FINE, "{0} (loading fields {1}) invoked at {2}@{3}[indexer={4}]:",    //NOI18N
                                new Object[]{
                                    this,
                                    printFiledToLoad(fieldsToLoad),
                                    qs.getClass().getSimpleName(),
                                    Integer.toHexString(System.identityHashCode(qs)),
                                    qs.indexerQuery.getIndexerId()});
                            for (IndexResult idi : pr) {
                                LOG.log(Level.FINE, " {0}", idi); //NOI18N
                            }
                            LOG.fine("----"); //NOI18N
                        }                        
                    }
                    return result;
                }
            });
        } catch (Index.IndexClosedException ice) {
            if (IndexingModule.isClosed()) {
                return Collections.<IndexResult>emptySet();
            } else {
                throw ice;
            }
        } catch (IOException ioe) {
            throw ioe;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
            
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(
                "QuerySupport.Query[%s]",   //NOI18N
                queryImpl);
        }        
    }

    // ------------------------------------------------------------------------
    // Private implementation
    // ------------------------------------------------------------------------

    private static final Logger LOG = Logger.getLogger(QuerySupport.class.getName());

    private final IndexerQuery indexerQuery;
    private final List<URL> roots;

    private QuerySupport (final String indexerName, int indexerVersion, final URL... roots) throws IOException {
        this.indexerQuery = IndexerQuery.forIndexer(indexerName, indexerVersion);
        this.roots = new LinkedList<URL>(Arrays.asList(roots));

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) //NOI18N
                    + "[indexer=" + indexerQuery.getIndexerId() + "]:"); //NOI18N
            for(Pair<URL, LayeredDocumentIndex> pair : indexerQuery.getIndices(this.roots)) {
                LOG.fine(" " + pair.first() + " -> index: " + pair.second()); //NOI18N
            }
            LOG.fine("----"); //NOI18N
        }
    }

    private static void collectClasspathRoots(FileObject file, Collection<String> pathIds, boolean binaryPaths, Collection<FileObject> roots) {
        for(String id : pathIds) {
            Collection<FileObject> classpathRoots = getClasspathRoots(file, id);
            if (binaryPaths) {
                // Filter out roots that do not have source files available
                for(FileObject binRoot : classpathRoots) {
                    URL binRootUrl;
                    try {
                        binRootUrl = binRoot.getURL();
                    } catch (FileStateInvalidException fsie) {
                        continue;
                    }

                    URL[] srcRoots = PathRegistry.getDefault().sourceForBinaryQuery(binRootUrl, null, false);
                    if (srcRoots != null) {
                        LOG.log(Level.FINE, "Translating {0} -> {1}", new Object [] { binRootUrl, srcRoots }); //NOI18N
                        for(URL srcRootUrl : srcRoots) {
                            FileObject srcRoot = URLCache.getInstance().findFileObject(srcRootUrl, false);
                            if (srcRoot != null) {
                                roots.add(srcRoot);
                            }
                        }
                    } else {
                        LOG.log(Level.FINE, "No sources for {0}, adding bin root", binRootUrl); //NOI18N
                        roots.add(binRoot);
                    }
                }
            } else {
                roots.addAll(classpathRoots);
            }
        }
    }

    private static Collection<FileObject> getClasspathRoots(FileObject file, String classpathId) {
        Collection<FileObject> roots = Collections.<FileObject>emptySet();

        if (file != null) {
            ClassPath classpath = ClassPath.getClassPath(file, classpathId);
            if (classpath != null) {
                roots = Arrays.asList(classpath.getRoots());
            }
        } else {
            roots = new HashSet<FileObject>();
            Set<URL> urls = PathRegistry.getDefault().getRootsMarkedAs(classpathId);
            for(URL url : urls) {
                FileObject f = URLCache.getInstance().findFileObject(url, false);
                if (f != null) {
                    roots.add(f);
                }
            }
        }

        return roots;
    }

    private static String printFiledToLoad(String... fieldsToLoad) {
        if (fieldsToLoad == null || fieldsToLoad.length == 0) {
            return "<all-fields>"; //NOI18N
        } else {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < fieldsToLoad.length; i++) {
                sb.append("\"").append(fieldsToLoad[i]).append("\""); //NOI18N
                if (i + 1 < fieldsToLoad.length) {
                    sb.append(", "); //NOI18N
                }
            }
            return sb.toString();
        }
    }
    
    private static Queries.QueryKind translateQueryKind(final QuerySupport.Kind kind) {
        switch (kind) {
            case EXACT: return Queries.QueryKind.EXACT;
            case PREFIX: return Queries.QueryKind.PREFIX;                
            case CASE_INSENSITIVE_PREFIX: return Queries.QueryKind.CASE_INSENSITIVE_PREFIX;
            case CAMEL_CASE: return Queries.QueryKind.CAMEL_CASE;
            case CASE_INSENSITIVE_REGEXP: return Queries.QueryKind.CASE_INSENSITIVE_REGEXP;                
            case REGEXP: return Queries.QueryKind.REGEXP;
            case CASE_INSENSITIVE_CAMEL_CASE: return Queries.QueryKind.CASE_INSENSITIVE_CAMEL_CASE;
            default: throw new UnsupportedOperationException (kind.toString());
        }
    }

    @CheckForNull
    private static URL findOwnerRoot(@NonNull final FileObject file) {
        final PathRecognizerRegistry regs = PathRecognizerRegistry.getDefault();
        URL res = findOwnerRoot(file, regs.getSourceIds());
        if (res != null) {
            return res;
        }
        res = findOwnerRoot(file, regs.getBinaryLibraryIds());
        if (res != null) {
            return res;
        }
        res = findOwnerRoot(file, regs.getLibraryIds());
        if (res != null) {
            return res;
        }
        //Fallback for roots with wrong classpath
        return file.isFolder() ? file.toURL() : null;
    }

    @CheckForNull
    private static URL findOwnerRoot(
        @NonNull final FileObject file,
        @NonNull final Collection<? extends String> ids) {
        for (String id : ids) {
            final ClassPath cp = ClassPath.getClassPath(file, id);
            if (cp != null) {
                final FileObject owner = cp.findOwnerRoot(file);
                if (owner != null) {
                    return owner.toURL();
                }
            }
        }
        return null;
    }

    @NonNull
    @org.netbeans.api.annotations.common.SuppressWarnings(value={"DMI_COLLECTION_OF_URLS"}, justification="URLs have never host part")
    private static Set<URL> findBinaryRootsForSourceRoot(
            @NonNull final FileObject sourceRoot,
            @NonNull final Map<URL, List<URL>> binaryDeps) {
        Set<URL> result = new HashSet<URL>();
        for (URL bin : binaryDeps.keySet()) {
            for (FileObject fo : SourceForBinaryQuery.findSourceRoots(bin).getRoots()) {
                if (sourceRoot.equals(fo)) {
                    result.add(bin);
                }
            }
        }
        return result;
    }

    @NonNull
    private static Collection<FileObject> mapToFileObjects(
        @NonNull final Collection<? extends URL> urls) {
        final Collection<FileObject> result = new ArrayList<FileObject>(urls.size());
        for (URL u : urls) {
            final FileObject fo = URLMapper.findFileObject(u);
            if (fo != null) {
                result.add(fo);
            }
        }
        return result;
    }

    /* test */ static final class IndexerQuery {

        public static synchronized IndexerQuery forIndexer(String indexerName, int indexerVersion) {
            String indexerId = SPIAccessor.getInstance().getIndexerPath(indexerName, indexerVersion);
            IndexerQuery q = queries.get(indexerId);
            if (q == null) {
                q = new IndexerQuery(indexerId);
                queries.put(indexerId, q);
            }
            return q;
        }

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public Iterable<? extends Pair<URL, LayeredDocumentIndex>> getIndices(List<? extends URL> roots) {
            synchronized (root2index) {
                List<Pair<URL, LayeredDocumentIndex>> indices = new LinkedList<Pair<URL, LayeredDocumentIndex>>();

                for(URL r : roots) {
                    assert PathRegistry.noHostPart(r) : r;
                    Reference<LayeredDocumentIndex> indexRef = root2index.get(r);
                    LayeredDocumentIndex index = indexRef != null ? indexRef.get() : null;
                    if (index == null) {
                        index = findIndex(r);
                        if (index != null) {
                            root2index.put(r, new SoftReference<LayeredDocumentIndex>(index));
                        } else {
                            root2index.remove(r);
                        }
                    }
                    if (index != null) {
                        indices.add(Pair.of(r, index));
                    }
                }

                return indices;
            }
        }

        public String getIndexerId() {
            return indexerId;
        }

        // ------------------------------------------------------------------------
        // Private implementation
        // ------------------------------------------------------------------------

        private static final Map<String, IndexerQuery> queries = new HashMap<String, IndexerQuery>();
        /* test */ static /* final, but tests need to change it */ IndexFactoryImpl indexFactory = LuceneIndexFactory.getDefault();

        private final String indexerId;
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        private final Map<URL, Reference<LayeredDocumentIndex>> root2index = new HashMap<URL, Reference<LayeredDocumentIndex>>();

        private IndexerQuery(String indexerId) {
            this.indexerId = indexerId;
        }

        private LayeredDocumentIndex findIndex(URL root) {
            try {
                FileObject cacheFolder = CacheFolder.getDataFolder(root);
                assert cacheFolder != null;
                FileObject indexFolder = cacheFolder.getFileObject(indexerId);
                if (indexFolder != null) {
                    return indexFactory.getIndex(indexFolder);
                }
            } catch (IOException ioe) {
                LOG.log(Level.INFO, "Can't create index for " + indexerId + " and " + root, ioe); //NOI18N
            }
            return null;
        }        
    } // End of IndexerQuery class

    private static final class DocumentToResultConvertor implements Convertor<IndexDocument, IndexResult> {

        private final URL root;

        DocumentToResultConvertor(@NonNull final URL root) {
            this.root = root;
        }

        @Override
        @CheckForNull
        public IndexResult convert(@NullAllowed final IndexDocument p) {
            return p == null ?
                null :
                new IndexResult(p, root);
        }

    }
}