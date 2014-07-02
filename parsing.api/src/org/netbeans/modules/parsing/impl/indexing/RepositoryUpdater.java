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

package org.netbeans.modules.parsing.impl.indexing;

import org.netbeans.modules.parsing.impl.indexing.friendapi.IndexDownloader;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.ClassPath.Entry;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.editor.AtomicLockEvent;
import org.netbeans.editor.AtomicLockListener;
import org.netbeans.editor.BaseDocument;
import org.netbeans.lib.editor.util.swing.DocumentUtilities;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.impl.*;
import org.netbeans.modules.parsing.impl.Utilities;
import org.netbeans.modules.parsing.impl.event.EventSupport;
import org.netbeans.modules.parsing.impl.indexing.IndexerCache.IndexerInfo;
import org.netbeans.modules.parsing.impl.indexing.errors.TaskCache;
import org.netbeans.modules.parsing.impl.indexing.friendapi.DownloadedIndexPatcher;
import org.netbeans.modules.parsing.impl.indexing.friendapi.IndexingActivityInterceptor;
import org.netbeans.modules.parsing.impl.indexing.friendapi.IndexingController;
import org.netbeans.modules.parsing.lucene.support.DocumentIndex;
import org.netbeans.modules.parsing.lucene.support.DocumentIndexCache;
import org.netbeans.modules.parsing.lucene.support.Index;
import org.netbeans.modules.parsing.lucene.support.IndexManager;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.indexing.*;
import org.netbeans.modules.project.indexingbridge.IndexingBridge;
import org.netbeans.modules.sampler.Sampler;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Mutex.ExceptionAction;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.util.Pair;
import org.openide.util.Parameters;
import org.openide.util.RequestProcessor;
import org.openide.util.TopologicalSortException;
import org.openide.util.Union2;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tomas Zezula
 */
@SuppressWarnings("ClassWithMultipleLoggers")
public final class RepositoryUpdater implements PathRegistryListener, PropertyChangeListener, DocumentListener, AtomicLockListener {
    /**
     * If the task is delayed longer than this constant from its schedule to execution, the previous task's info
     * will be chained to it. If the user cancels the task, the log will also contain the long-blocking predecessor.
     * In milliseconds.
     */
    private static final int PROFILE_EXECUTION_DELAY_TRESHOLD = 2 * 60 * 1000; 
    
    // -----------------------------------------------------------------------
    // Public implementation
    // -----------------------------------------------------------------------

    public enum IndexingState {
        STARTING,
        PATH_CHANGING,
        WORKING
    }
    
    /**
     * Controls whether the updater will self-profile, if an indexer takes "too long". The supported values are:
     * <ul>
     * <li>[undefined] = do not sample
     * <li>true = turn on sampling always and immediately
     * <li>oneshot = turn on sampling just until this scan cancel is reported, then turn the sampling back off
     * <li>[any other value] = turn on sampling, if an indexer takes longer than estimated
     * </ul>
     */
    static final String PROP_SAMPLING = RepositoryUpdater.class.getName() + ".indexerSampling"; // NOI18N

    public static synchronized RepositoryUpdater getDefault() {
        if (instance == null) {
            instance = new RepositoryUpdater();
        }
        return instance;
    }

    public void start(boolean force) {
        Work work = null;
        synchronized (this) {
            if (state == State.CREATED || state == State.STOPPED) {
                state = State.STARTED;
                getWorker().allCancelled = false;
                LOGGER.fine("Initializing..."); //NOI18N
                this.indexingActivityInterceptors = Lookup.getDefault().lookupResult(IndexingActivityInterceptor.class);
                PathRegistry.getDefault().addPathRegistryListener(this);
                rootsListeners.setListener(sourceRootsListener, binaryRootsListener);
                EditorRegistry.addPropertyChangeListener(this);
                IndexerCache.getCifCache().addPropertyChangeListener(this);
                IndexerCache.getEifCache().addPropertyChangeListener(this);
                visibilitySupport.start();
                if (force) {
                    work = new InitialRootsWork(
                        scannedRoots2Dependencies,
                        scannedBinaries2InvDependencies,
                        scannedRoots2Peers,
                        sourcesForBinaryRoots,
                        false,
                        scannedRoots2DependenciesLamport,
                        suspendSupport.getSuspendStatus(),
                        LogContext.create(LogContext.EventType.PATH, null));
                }
            }
        }

        if (work != null) {
            scheduleWork(work, false);
        }
    }

    public void stop(@NullAllowed final Runnable postCleanTask) throws TimeoutException {
        boolean cancel = false;

        synchronized (this) {
            if (state != State.STOPPED) {
                state = State.STOPPED;
                LOGGER.fine("Closing..."); //NOI18N

                PathRegistry.getDefault().removePathRegistryListener(this);
                rootsListeners.setListener(null, null);
                EditorRegistry.removePropertyChangeListener(this);
                visibilitySupport.stop();
                cancel = true;
            }
        }

        if (cancel) {
            getWorker().cancelAll(postCleanTask);
        }
    }

    public Set<IndexingState> getIndexingState() {
        boolean beforeInitialScanStarted;
        synchronized (this) {
            beforeInitialScanStarted = state == State.CREATED || state == State.STARTED;
        }

        // #168272
        boolean openingProjects;
        try {
            Future<Project []> f = OpenProjects.getDefault().openProjects();
            openingProjects = !f.isDone() || f.get().length > 0;
        } catch (Exception ie) {
            openingProjects = true;
        }
        final Set<IndexingState> result = EnumSet.noneOf(IndexingState.class);
        final boolean starting = (beforeInitialScanStarted && openingProjects);
        final boolean working = getWorker().isWorking();
        final boolean pathChanging = !PathRegistry.getDefault().isFinished();
        if (starting) {
            result.add(IndexingState.STARTING);
        }
        if (pathChanging) {
            result.add(IndexingState.PATH_CHANGING);
        }
        if (working) {
            result.add(IndexingState.WORKING);
        }
        LOGGER.log(Level.FINE,
                "IsScanInProgress: (starting: {0} | working: {1} | path are changing: {2})",  //NOI18N
                new Object[] {
                    starting,
                    working,
                    pathChanging
                });
        return result;
    }

    public boolean isProtectedModeOwner(final Thread thread) {
        return getWorker().isProtectedModeOwner(thread);
    }

    public boolean isIndexer() {
        return inIndexer.get() == Boolean.TRUE;
    }

    public void runIndexer(final Runnable indexer) {
        assert indexer != null;
        inIndexer.set(Boolean.TRUE);
        try {
            indexer.run();
        } finally {
            inIndexer.remove();
        }
    }
    
    // returns false when timed out
    public boolean waitUntilFinished(long timeout) throws InterruptedException {
        long ts1 = System.currentTimeMillis();
        long ts2 = ts1;
        //long tout = timeout > 0 ? timeout : 1000;

        do {
            boolean timedOut = !getWorker().waitUntilFinished(timeout);
            ts2 = System.currentTimeMillis();
            if (timedOut) {
                return false;
            }
        } while (!getIndexingState().isEmpty() && (timeout <= 0 || ts2 - ts1 < timeout));

        return timeout <= 0 || ts2 - ts1 < timeout;
    }

    /**
     * Schedules new job for indexing files under a root. This method forcible
     * reindexes all files in the job without checking timestamps.
     *
     * @param rootUrl The root that should be reindexed.
     * @param fileUrls Files under the root. Files that are not under the <code>rootUrl</code>
     *   are ignored. Can be <code>null</code> in which case all files under the root
     *   will be reindexed.
     * @param followUpJob If <code>true</code> the indexers will be notified that
     *   they are indexing follow up files (ie. files that one of the indexers involved
     *   in earlier indexing job requested to reindex) in contrast to files that are
     *   being reindexed due to ordinary change events (eg. when classpath roots are
     *   added/removed, file is modified, editor tabs are switched, etc).
     */
    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    public void addIndexingJob(
        @NonNull final URL rootUrl,
        @NullAllowed Collection<? extends URL> fileUrls,
        boolean followUpJob,
        boolean checkEditor,
        boolean wait,
        boolean forceRefresh,
        boolean steady,
        @NonNull final LogContext logCtx) {
        
        LOGGER.log(
            Level.FINE,
            "addIndexingJob: rootUrl={0}, fileUrls={1}, followUpJob={2}, checkEditor={3}, wait={4}",    //NOI18N
            new Object[]{
                rootUrl,
                fileUrls,
                followUpJob,
                checkEditor,
                wait}); 

        final FileListWork flw = createFileListWork(rootUrl, fileUrls, followUpJob, checkEditor, forceRefresh, steady, logCtx);        
        if (flw != null) {
            LOGGER.log(
                Level.FINE,
                "Scheduling index refreshing: root={0}, files={1}", //NOI18N
                new Object[]{rootUrl, fileUrls});
            scheduleWork(flw, wait);
        }
    }

    void addDeleteJob(
        @NonNull final URL root,
        @NonNull final Set<String> relativePaths,
        @NonNull final LogContext logCtx) {
        final Work wrk = new DeleteWork(
            root,
            relativePaths,
            suspendSupport.getSuspendStatus(),
            logCtx);
        scheduleWork(wrk, false);
    }

    void addBinaryJob(
        @NonNull final URL root,
        @NonNull final LogContext logCtx) {
        final Work wrk = new BinaryWork(
            root,
            suspendSupport.getSuspendStatus(),
            logCtx);
        scheduleWork(wrk, false);
    }
    
    public void enforcedFileListUpdate(
            @NonNull final URL rootUrl,
            @NonNull final Collection<? extends URL> fileUrls) throws IOException {
        final FileListWork flw = createFileListWork(rootUrl, fileUrls, false, true, true, false, null);
        if (flw != null) {
            LOGGER.log(
                Level.FINE,
                "Transient File List Update {0}",   //NOI18N
                flw);
            class T implements Callable<Void>, Runnable {
                @Override
                public void run() {
                    flw.doTheWork();
                }

                @Override
                public Void call() throws Exception {
                    suspendSupport.runWithNoSuspend(this);
                    return null;
                }
            };
            final T t = new T();
            try {
                Utilities.runPriorityIO(t);
            } catch (Exception ex) {
                throw new IOException(ex);
            }
        }
    }
    
    @CheckForNull
    private FileListWork createFileListWork(
        @NonNull final URL rootUrl,
        @NullAllowed Collection<? extends URL> fileUrls,
        boolean followUpJob,
        boolean checkEditor,
        boolean forceRefresh,
        boolean steady,
        @NullAllowed final LogContext logCtx) {
        
        assert rootUrl != null;
        assert PathRegistry.noHostPart(rootUrl) : rootUrl;

        FileObject root = URLMapper.findFileObject(rootUrl);
        if (root == null) {
            LOGGER.log(Level.INFO, "{0} can''t be translated to FileObject", rootUrl); //NOI18N
            return null;
        }

        FileListWork flw = null;
        if (fileUrls != null && fileUrls.size() > 0) {
            Set<FileObject> files = new HashSet<FileObject>();
            for(URL fileUrl : fileUrls) {
                FileObject file = URLMapper.findFileObject(fileUrl);
                if (file != null) {
                    if (FileUtil.isParentOf(root, file)) {
                        files.add(file);
                    } else {
                        if (LOGGER.isLoggable(Level.WARNING)) {
                            LOGGER.log(Level.WARNING, "{0} does not lie under {1}, not indexing it", new Object[]{file, root}); //NOI18N
                        }
                    }
                }
            }

            if (files.size() > 0) {
                flw = new FileListWork(
                    scannedRoots2Dependencies,
                    rootUrl,
                    files,
                    followUpJob,
                    checkEditor,
                    forceRefresh,
                    sourcesForBinaryRoots.contains(rootUrl),
                    steady,
                    suspendSupport.getSuspendStatus(),
                    logCtx);
            }
        } else {
            flw = new FileListWork(
                scannedRoots2Dependencies,
                rootUrl,
                followUpJob,
                checkEditor,
                forceRefresh,
                sourcesForBinaryRoots.contains(rootUrl),
                suspendSupport.getSuspendStatus(),
                logCtx);
        }
        return flw;
    }

    /**
     * Schedules new job for refreshing all indexes created by the given indexer.
     *
     * @param indexerName The name of the indexer, which indexes should be refreshed.
     */
    public void addIndexingJob(
            @NonNull final String indexerName,
            @NonNull final LogContext logCtx) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("addIndexingJob: indexerName=" + indexerName); //NOI18N
        }

        Collection<? extends IndexerCache.IndexerInfo<CustomIndexerFactory>> cifInfos = IndexerCache.getCifCache().getIndexersByName(indexerName);
        Work w;
        
        if (cifInfos == null) {
            Collection<? extends IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> eifInfos = IndexerCache.getEifCache().getIndexersByName(indexerName);
            if (eifInfos == null) {
                throw new InvalidParameterException("No CustomIndexerFactory or EmbeddingIndexerFactory with name: '" + indexerName + "'"); //NOI18N
            } else {
                w = new RefreshEifIndices(
                        eifInfos,
                        scannedRoots2Dependencies, sourcesForBinaryRoots, suspendSupport.getSuspendStatus(), logCtx
                );
            }
        } else {
            w = new RefreshCifIndices(cifInfos, scannedRoots2Dependencies, sourcesForBinaryRoots, suspendSupport.getSuspendStatus(), logCtx);
        }
        scheduleWork(w, false);
    }

    public void refreshAll(
            final boolean fullRescan,
            final boolean wait,
            final boolean logStatistics,
            @NullAllowed final LogContext logCtx,
            @NullAllowed final Object... filesOrFileObjects) {
        
        boolean ae = false;
        assert ae = true;
        if (ae) {
            for (final Object fileOrFileObject : filesOrFileObjects) {
                if (fileOrFileObject instanceof File) {
                    final File file = (File) fileOrFileObject;
                    assert file.equals(FileUtil.normalizeFile(file)) : String.format("File: %s is not normalized.", file.toString());   //NOI18N
                }
            }
        }
        
        FSRefreshInterceptor fsRefreshInterceptor = null;
        for(IndexingActivityInterceptor iai : indexingActivityInterceptors.allInstances()) {
            if (iai instanceof FSRefreshInterceptor) {
                fsRefreshInterceptor = (FSRefreshInterceptor) iai;
                break;
            }
        }

        scheduleWork(
            new RefreshWork(
                scannedRoots2Dependencies,
                scannedBinaries2InvDependencies,
                scannedRoots2Peers,
                sourcesForBinaryRoots,
                fullRescan,
                logStatistics,
                filesOrFileObjects == null ? Collections.<Object>emptySet() : Arrays.asList(filesOrFileObjects),
                fsRefreshInterceptor,
                suspendSupport.getSuspendStatus(),
                logCtx),
            wait);
    }

    public void suspend() {
        if (notInterruptible) {
            // ignore the request
            return;
        }
        suspendSupport.suspend();
    }

    public void resume() {
        if (notInterruptible) {
            // ignore the request
            return;
        }
        suspendSupport.resume();
    }

    public synchronized IndexingController getController() {
        if (controller == null) {
            controller = new Controller();
        }
        return controller;
    }

    // -----------------------------------------------------------------------
    // PathRegistryListener implementation
    // -----------------------------------------------------------------------

    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part. Already verified by PathRegistry")
    @Override
    public void pathsChanged(PathRegistryEvent event) {
        assert event != null;
        if (LOGGER.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Paths changed:\n"); //NOI18N
            for(PathRegistryEvent.Change c : event.getChanges()) {
                sb.append(" event=").append(c.getEventKind()); //NOI18N
                sb.append(" pathKind=").append(c.getPathKind()); //NOI18N
                sb.append(" pathType=").append(c.getPathId()); //NOI18N
                sb.append(" affected paths:\n"); //NOI18N
                Collection<? extends ClassPath> paths = c.getAffectedPaths();
                if (paths != null) {
                    for(ClassPath cp : paths) {
                        sb.append("  \""); //NOI18N
                        sb.append(cp.toString(ClassPath.PathConversionMode.PRINT));
                        sb.append("\"\n"); //NOI18N
                    }
                }
                sb.append("--\n"); //NOI18N
            }
            sb.append("====\n"); //NOI18N
            LOGGER.fine(sb.toString());
        }

        boolean existingPathsChanged = false;
        boolean containsRelevantChanges = false;
        final LogContext logContext = event.getLogContext();
        List<URL> includesChanged = new ArrayList<URL>();
        for(PathRegistryEvent.Change c : event.getChanges()) {

            if (c.getEventKind() == EventKind.INCLUDES_CHANGED) {
                for (ClassPath cp : c.getAffectedPaths()) {
                    for (Entry e : cp.entries()) {
                        includesChanged.add(e.getURL());
                    }
                }
            } else {
                containsRelevantChanges = true;
                if (c.getEventKind() == EventKind.PATHS_CHANGED) {
                    existingPathsChanged = true;
                }
            }
        }

        if (containsRelevantChanges) {
            scheduleWork(
                    new RootsWork(
                        scannedRoots2Dependencies,
                        scannedBinaries2InvDependencies,
                        scannedRoots2Peers,
                        sourcesForBinaryRoots,
                        !existingPathsChanged,
                        false,
                        scannedRoots2DependenciesLamport,
                        suspendSupport.getSuspendStatus(),
                        logContext),
                    false);
        }
        for (URL rootUrl : includesChanged) {
            scheduleWork(
                new FileListWork(
                    scannedRoots2Dependencies,
                    rootUrl,
                    false,
                    false,
                    false,
                    sourcesForBinaryRoots.contains(rootUrl),
                    suspendSupport.getSuspendStatus(),
                    logContext),
                false);
        }
    }

    // -----------------------------------------------------------------------
    // FileChangeListener implementation
    // -----------------------------------------------------------------------
    
    private final FileEventLog eventQueue = new FileEventLog();

    private void fileFolderCreatedImpl(FileEvent fe, Boolean source) {
        FileObject fo = fe.getFile();
        if (isCacheFile(fo)) {
            return;
        }

        if (!authorize(fe)) {
            return;
        }
        
        //In ideal case this should do nothing,
        //but in Netbeans newlly created folder may
        //already contain files
        boolean processed = false;
        Pair<URL, FileObject> root = null;
        
        if (fo != null && fo.isValid()) {
            if (source == null || source.booleanValue()) {
                root = getOwningSourceRoot(fo);
                if (root != null && visibilitySupport.isVisible(fo, root.second())) {
                    if (root.second() == null) {
                        LOGGER.log(
                            Level.INFO,
                            "Ignoring event from non existing FileObject {0}",  //NOI18N
                            root.first());
                        return;
                    }
                    boolean sourcForBinaryRoot = sourcesForBinaryRoots.contains(root.first());
                    ClassPath.Entry entry = sourcForBinaryRoot ? null : getClassPathEntry(root.second());
                    if (entry == null || entry.includes(fo)) {
                        Work wrk;
                        if (fo.equals(root.second())) {
                            if (scannedRoots2Dependencies.get(root.first()) == NONEXISTENT_ROOT) {
                                //For first time seeing valid root do roots work to recalculate dependencies
                                wrk = new RootsWork(
                                    scannedRoots2Dependencies,
                                    scannedBinaries2InvDependencies,
                                    scannedRoots2Peers,
                                    sourcesForBinaryRoots,
                                    false,
                                    true,
                                    scannedRoots2DependenciesLamport,
                                    suspendSupport.getSuspendStatus(),
                                    LogContext.create(LogContext.EventType.FILE, null).addRoots(Collections.singleton(root.first())));
                            } else {
                                //Already seen files work is enough
                                final FileObject[] children = fo.getChildren();
                                final Collection<FileObject> c = Arrays.asList(children);
                                if (children.length > 0) {
                                    wrk = new FileListWork(
                                        scannedRoots2Dependencies,
                                        root.first(),
                                        c,
                                        false,
                                        false,
                                        true,
                                        sourcForBinaryRoot,
                                        true,
                                        suspendSupport.getSuspendStatus(),
                                        LogContext.create(LogContext.EventType.FILE, null).
                                            withRoot(root.first()).
                                            addFileObjects(c));
                                } else {
                                    //If no children nothing needs to be done - save some CPU time
                                    wrk = null;
                                }
                            }
                        } else {
                            Collection<FileObject> c = Collections.singleton(fo);
                            wrk = new FileListWork(
                                scannedRoots2Dependencies,
                                root.first(),
                                c,
                                false,
                                false,
                                true,
                                sourcForBinaryRoot,
                                true,
                                suspendSupport.getSuspendStatus(),
                                LogContext.create(LogContext.EventType.FILE, null).
                                    withRoot(root.first()).
                                    addFileObjects(c));
                        }
                        if (wrk != null) {
                            eventQueue.record(FileEventLog.FileOp.CREATE, root.first(), FileUtil.getRelativePath(root.second(), fo), fe, wrk);
                        }
                        processed = true;
                    }
                }
            }

            if (!processed && (source == null || !source.booleanValue())) {
                root = getOwningBinaryRoot(fo);
                if (root != null && visibilitySupport.isVisible(fo, root.second())) {
                    final Work wrk = new BinaryWork(
                        root.first(),
                        suspendSupport.getSuspendStatus(),
                        LogContext.create(LogContext.EventType.FILE, null).
                            withRoot(root.first()).
                            addFileObjects(Collections.singleton(fo)));
                    eventQueue.record(FileEventLog.FileOp.CREATE, root.first(), null, fe, wrk);
                    processed = true;
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Folder created (" + (processed ? "processed" : "ignored") + "): " //NOI18N
                    + FileUtil.getFileDisplayName(fo) + " Owner: " + root); //NOI18N
        }
    }

    private void fileChangedImpl (FileEvent fe, Boolean source) {
        FileObject fo = fe.getFile();
        if (isCacheFile(fo)) {
            return;
        }

        if (!authorize(fe)) {
            return;
        }

        boolean processed = false;
        Pair<URL, FileObject> root = null;

        if (fo != null && fo.isValid()) {
            if (source == null || source.booleanValue()) {
                root = getOwningSourceRoot (fo);
                if (root != null && visibilitySupport.isVisible(fo,root.second())) {
                    if (root.second() == null) {
                        LOGGER.log(
                            Level.INFO,
                            "Ignoring event from non existing FileObject {0}",  //NOI18N
                            root.first());
                        return;
                    }
                    boolean sourceForBinaryRoot = sourcesForBinaryRoots.contains(root.first());
                    ClassPath.Entry entry = sourceForBinaryRoot ? null : getClassPathEntry(root.second());
                    if (entry == null || entry.includes(fo)) {
                        final Work wrk = new FileListWork(
                            scannedRoots2Dependencies,
                            root.first(),
                            Collections.singleton(fo),
                            false,
                            false,
                            true,
                            sourceForBinaryRoot,
                            true,
                            suspendSupport.getSuspendStatus(),
                            LogContext.create(LogContext.EventType.FILE, null).
                                withRoot(root.first()).
                                addFiles(Collections.singleton(fo.toURL())));
                        eventQueue.record(FileEventLog.FileOp.CREATE, root.first(), FileUtil.getRelativePath(root.second(), fo), fe, wrk);
                        processed = true;
                    }
                }
            }

            if (!processed && (source == null || !source.booleanValue())) {
                root = getOwningBinaryRoot(fo);
                if (root != null && visibilitySupport.isVisible(fo,root.second())) {
                    final Work wrk = new BinaryWork(
                        root.first(),
                        suspendSupport.getSuspendStatus(),
                        LogContext.create(LogContext.EventType.FILE, null).
                            withRoot(root.first()).
                            addFiles(Collections.singleton(fo.toURL())));
                    eventQueue.record(FileEventLog.FileOp.CREATE, root.first(), null, fe, wrk);
                    processed = true;
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "File modified ({0}): {1} Owner: {2}",      //NOI18N
                    new Object[]{
                        processed ? "processed" : "ignored",    //NOI18N
                        FileUtil.getFileDisplayName(fo),
                        root
                    });
        }
    }

    private void fileDeletedImpl(FileEvent fe, Boolean source) {
        FileObject fo = fe.getFile();
        if (isCacheFile(fo)) {
            return;
        }

        if (!authorize(fe)) {
            return;
        }

        boolean processed = false;
        Pair<URL, FileObject> root = null;

        if (fo != null) {
            if (source == null || source.booleanValue()) {
                root = getOwningSourceRoot (fo);
                if (root != null && fo.isData() && visibilitySupport.isVisible(fo, root.second())) {
                    String relativePath = null;
                    try {
                    //Root may be deleted -> no root.second available
                        if (root.second() != null) {
                            relativePath = FileUtil.getRelativePath(root.second(), fo);
                        } else {
                            relativePath = root.first().toURI().relativize(fo.getURL().toURI()).getPath();
                        }
                        assert relativePath != null : "FileObject not under root: f=" + fo + ", root=" + root; //NOI18N
                        final Work wrk = new DeleteWork(
                            root.first(),
                            Collections.singleton(relativePath),
                            suspendSupport.getSuspendStatus(),
                            LogContext.create(LogContext.EventType.FILE, null).
                                withRoot(root.first()).
                                addFiles(Collections.singleton(fo.toURL())));
                        eventQueue.record(FileEventLog.FileOp.DELETE, root.first(), relativePath, fe, wrk);
                        processed = true;
                    } catch (FileStateInvalidException fse) {
                        Exceptions.printStackTrace(fse);
                    } catch (URISyntaxException use) {
                        Exceptions.printStackTrace(use);
                    }
                }
            }

            if (!processed && (source == null || !source.booleanValue())) {
                root = getOwningBinaryRoot(fo);
                if (root != null && visibilitySupport.isVisible(fo, root.second())) {
                    final Work wrk = new BinaryWork(
                        root.first(),
                        suspendSupport.getSuspendStatus(),
                        LogContext.create(LogContext.EventType.FILE, null).
                            withRoot(root.first()).
                            addFiles(Collections.singleton(fo.toURL())));
                    eventQueue.record(FileEventLog.FileOp.DELETE, root.first(), null, fe, wrk);
                    processed = true;
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "File deleted ({0}): {1} Owner: {2}",   //NOI18N
                new Object[]{
                    processed ? "processed" : "ignored",    //NOI18N
                    FileUtil.getFileDisplayName(fo), root
                });
        }
    }

    private void fileRenamedImpl(FileRenameEvent fe, Boolean  source) {
        FileObject fo = fe.getFile();
        if (isCacheFile(fo)) {
            return;
        }

        if (!authorize(fe)) {
            return;
        }
        
        FileObject newFile = fe.getFile();
        String oldNameExt = fe.getExt().length() == 0 ? fe.getName() : fe.getName() + "." + fe.getExt(); //NOI18N
        Pair<URL, FileObject> root = null;
        boolean processed = false;

        if (newFile != null && newFile.isValid()) {
            if (source == null || source.booleanValue()) {
                root = getOwningSourceRoot(newFile);
                if (root != null) {
                    if (root.second() == null) {
                        LOGGER.log(
                            Level.INFO,
                            "Ignoring event from non existing FileObject {0}",  //NOI18N
                            root.first());
                        return;
                    }
                    FileObject rootFo = root.second();
                    if (rootFo.equals(newFile)) {
                        //Root renamed do nothing, will be fired as ClassPath change.
                        return;
                    }
                    String ownerPath = FileUtil.getRelativePath(rootFo, newFile.getParent());
                    String oldFilePath =  ownerPath.length() == 0 ? oldNameExt : ownerPath + "/" + oldNameExt; //NOI18N
                    if (newFile.isData()) {
                        final Work work = new DeleteWork(
                            root.first(),
                            Collections.singleton(oldFilePath),
                            suspendSupport.getSuspendStatus(),
                            LogContext.create(LogContext.EventType.FILE, null).
                                withRoot(root.first()).
                                addFilePaths(Collections.singleton(oldFilePath)));
                        eventQueue.record(FileEventLog.FileOp.DELETE, root.first(), oldFilePath, fe, work);
                    } else {
                        Set<String> oldFilePaths = new HashSet<String>();
                        collectFilePaths(newFile, oldFilePath, oldFilePaths);
                        final Work work = new DeleteWork(
                            root.first(),
                            oldFilePaths,
                            suspendSupport.getSuspendStatus(),
                            LogContext.create(LogContext.EventType.FILE, null).
                                withRoot(root.first()).
                                addFilePaths(oldFilePaths));
                        for (String path : oldFilePaths) {                            
                            eventQueue.record(FileEventLog.FileOp.DELETE, root.first(), path, fe, work);
                        }
                    }

                    if (visibilitySupport.isVisible(newFile,root.second())) {
                        final boolean sourceForBinaryRoot = sourcesForBinaryRoots.contains(root.first());
                        ClassPath.Entry entry = sourceForBinaryRoot ? null : getClassPathEntry(rootFo);
                        if (entry == null || entry.includes(newFile)) {
                            final FileListWork flw = new FileListWork(
                                scannedRoots2Dependencies,
                                root.first(),
                                Collections.singleton(newFile),
                                false,
                                false,
                                true,
                                sourceForBinaryRoot,
                                true,
                                suspendSupport.getSuspendStatus(),
                                LogContext.create(LogContext.EventType.FILE, null).
                                    withRoot(root.first()).
                                    addFileObjects(Collections.singleton(newFile)));
                            eventQueue.record(FileEventLog.FileOp.CREATE, root.first(), FileUtil.getRelativePath(rootFo, newFile), fe,flw);
                        }
                    }
                    processed = true;
                }
            }

            if (!processed && (source == null || !source.booleanValue())) {
                root = getOwningBinaryRoot(newFile);
                if (root != null) {
                    final File parentFile = FileUtil.toFile(newFile.getParent());
                    if (parentFile != null) {
                        try {
                            URL oldBinaryRoot = org.openide.util.Utilities.toURI(new File (parentFile, oldNameExt)).toURL();
                            eventQueue.record(
                                    FileEventLog.FileOp.DELETE,
                                    oldBinaryRoot,
                                    null,
                                    fe,
                                    new BinaryWork(oldBinaryRoot,
                                        suspendSupport.getSuspendStatus(),
                                        LogContext.create(LogContext.EventType.FILE, null).
                                            addRoots(Collections.singleton(oldBinaryRoot))));    //NOI18N
                        } catch (MalformedURLException mue) {
                            LOGGER.log(Level.WARNING, null, mue);
                        }
                    }

                    eventQueue.record(
                            FileEventLog.FileOp.CREATE,
                            root.first(),
                            null,
                            fe,
                            new BinaryWork(root.first(),
                                suspendSupport.getSuspendStatus(),
                                LogContext.create(LogContext.EventType.FILE, null).
                                    withRoot(root.first()).
                                    addRoots(Collections.singleton(root.first()))));
                    processed = true;
                }
            }
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                Level.FINE,
                "File renamed ({0}): {1} Owner: {2} Original Name: {3}",    //NOI18N
                new Object[]{
                    processed ? "processed" : "ignored",    //NOI18N
                    FileUtil.getFileDisplayName(newFile),
                    root,
                    oldNameExt
                });
        }
    }

    // -----------------------------------------------------------------------
    // PropertyChangeListener implementation
    // -----------------------------------------------------------------------

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName() != null && evt.getPropertyName().equals(CustomIndexerFactory.class.getName())) {
            if (!ignoreIndexerCacheEvents) {
                @SuppressWarnings("unchecked")
                Set<IndexerCache.IndexerInfo<CustomIndexerFactory>> changedIndexers = (Set<IndexerInfo<CustomIndexerFactory>>) evt.getNewValue();
                scheduleWork(new RefreshCifIndices(
                        changedIndexers,
                        scannedRoots2Dependencies,
                        sourcesForBinaryRoots,
                        suspendSupport.getSuspendStatus(),
                        LogContext.create(LogContext.EventType.INDEXER,null)),
                        false);
            }
            return;
        } else if (evt.getPropertyName() != null && evt.getPropertyName().equals(EmbeddingIndexerFactory.class.getName())) {
            if (!ignoreIndexerCacheEvents) {
                @SuppressWarnings("unchecked")
                Set<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> changedIndexers = (Set<IndexerInfo<EmbeddingIndexerFactory>>) evt.getNewValue();
                scheduleWork(new RefreshEifIndices(
                        changedIndexers,
                        scannedRoots2Dependencies,
                        sourcesForBinaryRoots,
                        suspendSupport.getSuspendStatus(),
                        LogContext.create(LogContext.EventType.INDEXER, null)),
                        false);
            }
            return;
        }

        assert SwingUtilities.isEventDispatchThread() : "Changes in focused editor component should be delivered on AWT"; //NOI18N
        
        List<? extends JTextComponent> components = Collections.<JTextComponent>emptyList();

        if (evt.getPropertyName() == null) {
            components = EditorRegistry.componentList();

        } else if (evt.getPropertyName().equals(EditorRegistry.FOCUS_LOST_PROPERTY) || 
                   evt.getPropertyName().equals(EditorRegistry.LAST_FOCUSED_REMOVED_PROPERTY))
        {
            if (evt.getOldValue() instanceof JTextComponent) {
                Object newValue = evt.getNewValue();
                if (!(newValue instanceof JTextComponent) || ((JTextComponent)newValue).getClientProperty("AsTextField") == null) {
                    JTextComponent jtc = (JTextComponent) evt.getOldValue();
                    handleActiveDocumentChange(jtc.getDocument(), null);
                }
            }
            
        } else if (evt.getPropertyName().equals(EditorRegistry.COMPONENT_REMOVED_PROPERTY)) {
            if (evt.getOldValue() instanceof JTextComponent) {
                JTextComponent jtc = (JTextComponent) evt.getOldValue();
                components = Collections.singletonList(jtc);
            }

        } else if (evt.getPropertyName().equals(EditorRegistry.FOCUS_GAINED_PROPERTY)) {
            if (evt.getNewValue() instanceof JTextComponent) {
                JTextComponent jtc = (JTextComponent) evt.getNewValue();
                if (jtc.getClientProperty("AsTextField") == null) {
                    handleActiveDocumentChange(null, jtc.getDocument());
                    JTextComponent activeComponent = activeComponentRef == null ? null : activeComponentRef.get();
                    if (activeComponent != jtc) {
                        if (activeComponent != null)
                            components = Collections.singletonList(activeComponent);
                        activeComponentRef = new WeakReference<JTextComponent>(jtc);
                    }
                }
            }

        } else if (evt.getPropertyName().equals(EditorRegistry.FOCUSED_DOCUMENT_PROPERTY)) {
            JTextComponent jtc = EditorRegistry.focusedComponent();
            if (jtc == null) {
                jtc = EditorRegistry.lastFocusedComponent();
            }
            if (jtc != null) {
                components = Collections.singletonList(jtc);
            }

            handleActiveDocumentChange((Document) evt.getOldValue(), (Document) evt.getNewValue());
        }

        Map<URL, FileListWork> jobs = new HashMap<URL, FileListWork>();
        if (components.size() > 0) {
            for(JTextComponent jtc : components) {
                Document doc = jtc.getDocument();
                Pair<URL, FileObject> root = getOwningSourceRoot(doc);
                if (root != null) {
                    if (root.second() == null) {
                        final FileObject file = Util.getFileObject(doc);
                        assert file == null || !file.isValid() : "Expecting both owningSourceRootUrl=" + root.first() + " and owningSourceRoot=" + root.second(); //NOI18N
                        return;
                    }                    
                    long version = DocumentUtilities.getDocumentVersion(doc);
                    Long lastIndexedVersion = (Long) doc.getProperty(PROP_LAST_INDEXED_VERSION);
                    Long lastDirtyVersion = (Long) doc.getProperty(PROP_LAST_DIRTY_VERSION);
                    boolean reindex = false;

                    boolean openedInEditor = EditorRegistry.componentList().contains(jtc);
                    if (openedInEditor) {
                        if (lastIndexedVersion == null) {
                            reindex = lastDirtyVersion != null;
                        } else {
                            reindex = lastIndexedVersion < version;
                        }
                    } else {
                        // Editor closed. There were possibly discarded changes and
                        // so we have to reindex the contents of the file.
                        // This must not be done too agresively (eg reindex only when there really were
                        // editor changes) otherwise it may cause unneccessary redeployments, etc (see #152222).
                        reindex = lastDirtyVersion != null;
                    }

                    FileObject docFile = Util.getFileObject(doc);
                    LOGGER.log(Level.FINE, "{0}: version={1}, lastIndexerVersion={2}, lastDirtyVersion={3}, openedInEditor={4} => reindex={5}", new Object [] {
                        docFile.getPath(), version, lastIndexedVersion, lastDirtyVersion, openedInEditor, reindex
                    });

                    if (reindex) {
                        // we have already seen the document and it's been modified since the last time
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine("Document modified (reindexing): " + FileUtil.getFileDisplayName(docFile) + " Owner: " + root.first()); //NOI18N
                        }

                        FileListWork job = jobs.get(root.first());
                        if (job == null) {
                            Collection<FileObject> c = Collections.singleton(docFile);
                            job = new FileListWork(
                                    scannedRoots2Dependencies,
                                    root.first(),
                                    c,
                                    false,
                                    openedInEditor,
                                    true,
                                    sourcesForBinaryRoots.contains(root.first()),
                                    true,
                                    suspendSupport.getSuspendStatus(),
                                    LogContext.create(LogContext.EventType.FILE, null).
                                        withRoot(root.first()).
                                        addFileObjects(c));
                            jobs.put(root.first(), job);
                        } else {
                            // XXX: strictly speaking we should set 'checkEditor' for each file separately
                            // and not for each job; in reality we normally do not end up here
                            job.addFile(docFile);
                        }
                    }
                }
            }
        }       
        for(FileListWork job : jobs.values()) {
            scheduleWork(job, false);
        }
    }

    // -----------------------------------------------------------------------
    // DocumentListener implementation
    // -----------------------------------------------------------------------

    @Override
    public void changedUpdate(DocumentEvent e) {
        // no document modification
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        removeUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        Document d = e.getDocument();
        if (d instanceof BaseDocument) {
            d.putProperty(PROP_MODIFIED_UNDER_WRITE_LOCK, true);
        } else {
            handleDocumentModification(d);
        }
    }

    // -----------------------------------------------------------------------
    // AtomicLockListener implementation
    // -----------------------------------------------------------------------

    @Override
    public void atomicLock(AtomicLockEvent e) {
        Document d = (Document) e.getSource();
        d.putProperty(PROP_MODIFIED_UNDER_WRITE_LOCK, null);
    }

    @Override
    public void atomicUnlock(AtomicLockEvent e) {
        Document d = (Document) e.getSource();
        Boolean modified = (Boolean) d.getProperty(PROP_MODIFIED_UNDER_WRITE_LOCK);
        d.putProperty(PROP_MODIFIED_UNDER_WRITE_LOCK, null);
        if (modified != null && modified.booleanValue()) {
            handleDocumentModification(d);
        }
    }

    // -----------------------------------------------------------------------
    // Private implementation
    // -----------------------------------------------------------------------

    private static RepositoryUpdater instance;

    private static final Logger LOGGER = Logger.getLogger(RepositoryUpdater.class.getName());
    private static final Logger TEST_LOGGER = Logger.getLogger(RepositoryUpdater.class.getName() + ".tests"); //NOI18N
    private static final Logger PERF_LOGGER = Logger.getLogger(RepositoryUpdater.class.getName() + ".perf"); //NOI18N
    private static final Logger SFEC_LOGGER = Logger.getLogger("org.netbeans.ui.ScanForExternalChanges"); //NOI18N
    private static final Logger UI_LOGGER = Logger.getLogger("org.netbeans.ui.indexing");   //NOI18N
    private static final RequestProcessor RP = new RequestProcessor("RepositoryUpdater.delay"); //NOI18N
    private static final RequestProcessor WORKER = new RequestProcessor("RepositoryUpdater.worker", 1, false, false);
    private static final boolean notInterruptible = getSystemBoolean("netbeans.indexing.notInterruptible", false); //NOI18N
    private static final boolean useRecursiveListeners = getSystemBoolean("netbeans.indexing.recursiveListeners", true); //NOI18N
    private static final int FILE_LOCKS_DELAY = org.openide.util.Utilities.isWindows() ? 2000 : 1000;
    private static final String PROP_LAST_INDEXED_VERSION = RepositoryUpdater.class.getName() + "-last-indexed-document-version"; //NOI18N
    private static final String PROP_LAST_DIRTY_VERSION = RepositoryUpdater.class.getName() + "-last-dirty-document-version"; //NOI18N
    private static final String PROP_MODIFIED_UNDER_WRITE_LOCK = RepositoryUpdater.class.getName() + "-modified-under-write-lock"; //NOI18N
    private static final String PROP_OWNING_SOURCE_ROOT_URL = RepositoryUpdater.class.getName() + "-owning-source-root-url"; //NOI18N
    private static final String PROP_OWNING_SOURCE_ROOT = RepositoryUpdater.class.getName() + "-owning-source-root"; //NOI18N
    private static final String PROP_OWNING_SOURCE_UNKNOWN_IN = RepositoryUpdater.class.getName() + "-owning-source-root-unknown-in"; //NOI18N
    private static final String INDEX_DOWNLOAD_FOLDER = "index-download";   //NOI18N
    private static final boolean[] FD_NEW_SFB_ROOT = new boolean[1];

    /* test */ static final List<URL> UNKNOWN_ROOT = Collections.unmodifiableList(new LinkedList<URL>());
    /* test */ static final List<URL> NONEXISTENT_ROOT = Collections.unmodifiableList(new LinkedList<URL>());
    /* test */ volatile static Source unitTestActiveSource;

    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    private final Map<URL, List<URL>>scannedRoots2Dependencies = Collections.synchronizedMap(new TreeMap<URL, List<URL>>(new LexicographicComparator(true)));
    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    private final Map<URL, List<URL>>scannedBinaries2InvDependencies = Collections.synchronizedMap(new HashMap<URL,List<URL>>());
    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    private final Map<URL, List<URL>>scannedRoots2Peers = Collections.synchronizedMap(new TreeMap<URL, List<URL>>(new LexicographicComparator(true)));
    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    private final Set<URL>scannedUnknown = Collections.synchronizedSet(new HashSet<URL>());
    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    private final Set<URL>sourcesForBinaryRoots = Collections.synchronizedSet(new HashSet<URL>());

    private volatile State state = State.CREATED;
    private volatile Task worker;

    private volatile Reference<Document> activeDocumentRef = null;
    private volatile Reference<JTextComponent> activeComponentRef = null;
    private Lookup.Result<? extends IndexingActivityInterceptor> indexingActivityInterceptors = null;
    private IndexingController controller;

    @org.netbeans.api.annotations.common.SuppressWarnings("DM_STRING_CTOR")
    private final String lastOwningSourceRootCacheLock = new String("lastOwningSourceRootCacheLock"); //NOI18N

    private boolean ignoreIndexerCacheEvents = false;

    /* test */ final RootsListeners rootsListeners = new RootsListeners();
    private final FileChangeListener sourceRootsListener = new FCL(useRecursiveListeners ? Boolean.TRUE : null);
    private final FileChangeListener binaryRootsListener = new FCL(Boolean.FALSE);
    private final ThreadLocal<Boolean> inIndexer = new ThreadLocal<Boolean>();

   private final VisibilitySupport visibilitySupport = VisibilitySupport.create(this, RP);
   private final SuspendSupport suspendSupport = new SuspendSupport(WORKER);
   private final AtomicLong scannedRoots2DependenciesLamport = new AtomicLong();

    private RepositoryUpdater () {
        LOGGER.log(Level.FINE, "netbeans.indexing.notInterruptible={0}", notInterruptible); //NOI18N
        LOGGER.log(Level.FINE, "netbeans.indexing.recursiveListeners={0}", useRecursiveListeners); //NOI18N
        LOGGER.log(Level.FINE, "FILE_LOCKS_DELAY={0}", FILE_LOCKS_DELAY); //NOI18N
    }

    private void handleActiveDocumentChange(Document deactivated, Document activated) {
        Document activeDocument = activeDocumentRef == null ? null : activeDocumentRef.get();

        if (deactivated != null && deactivated == activeDocument) {
            if (activeDocument instanceof BaseDocument) {
                ((BaseDocument) activeDocument).removeAtomicLockListener(this);
            }
            activeDocument.removeDocumentListener(this);
            activeDocumentRef = null;
            LOGGER.log(Level.FINE, "Unregistering active document listener: activeDocument={0}", activeDocument); //NOI18N
        }

        if (activated != null && activated != activeDocument) {
            if (activeDocument != null) {
                if (activeDocument instanceof BaseDocument) {
                    ((BaseDocument) activeDocument).removeAtomicLockListener(this);
                }
                activeDocument.removeDocumentListener(this);
                LOGGER.log(Level.FINE, "Unregistering active document listener: activeDocument={0}", activeDocument); //NOI18N
            }

            activeDocument = activated;
            activeDocumentRef = new WeakReference<Document>(activeDocument);
            
            if (activeDocument instanceof BaseDocument) {
                ((BaseDocument) activeDocument).addAtomicLockListener(this);
            }
            activeDocument.addDocumentListener(this);
            LOGGER.log(Level.FINE, "Registering active document listener: activeDocument={0}", activeDocument); //NOI18N
        }
    }

    public void handleDocumentModification(Document document) {
        final Reference<Document> ref = activeDocumentRef;
        Document activeDocument = ref == null ? null : ref.get();

        final Pair<URL, FileObject> root = getOwningSourceRoot(document);
        if (root != null) {
            if (root.second() == null) {
                LOGGER.log(
                    Level.INFO,
                    "Ignoring event from non existing FileObject {0}",  //NOI18N
                    root.first());
                return;
            }
            if (activeDocument == document) {
                long version = DocumentUtilities.getDocumentVersion(activeDocument);
                Long lastDirtyVersion = (Long) activeDocument.getProperty(PROP_LAST_DIRTY_VERSION);
                boolean markDirty = false;

                if (lastDirtyVersion == null || lastDirtyVersion < version) {
                    // the document was changed since the last time
                    markDirty = true;
                }

                activeDocument.putProperty(PROP_LAST_DIRTY_VERSION, version);

                if (markDirty) {
                    FileObject docFile = Util.getFileObject(document);
                    
                    // An active document was modified, we've indexed that document berfore,
                    // so mark it dirty
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Active document modified (marking dirty): " + FileUtil.getFileDisplayName(docFile) + " Owner: " + root.first()); //NOI18N
                    }

                    Collection<? extends Indexable> dirty = Collections.singleton(SPIAccessor.getInstance().create(new FileObjectIndexable(root.second(), docFile)));
                    String mimeType = DocumentUtilities.getMimeType(document);

                    TransientUpdateSupport.setTransientUpdate(true);
                    try {
                        final Callable<FileObject> indexFolderFactory =
                            new Callable<FileObject>() {
                                private FileObject cache;
                                @Override
                                public FileObject call() throws Exception {
                                    if (cache == null) {
                                        cache = CacheFolder.getDataFolder(root.first());
                                    }
                                    return cache;
                                }
                            };
                        Collection<? extends IndexerCache.IndexerInfo<CustomIndexerFactory>> cifInfos = IndexerCache.getCifCache().getIndexersFor(mimeType, true);
                        for(IndexerCache.IndexerInfo<CustomIndexerFactory> info : cifInfos) {
                            try {
                                final CustomIndexerFactory factory = info.getIndexerFactory();
                                final Context ctx = SPIAccessor.getInstance().createContext(
                                        indexFolderFactory,
                                        root.first(),
                                        factory.getIndexerName(),
                                        factory.getIndexVersion(),
                                        null,
                                        false,
                                        true,
                                        false,
                                        SuspendSupport.NOP,
                                        null,
                                        null);
                                factory.filesDirty(dirty, ctx);
                            } catch (IOException ex) {
                                LOGGER.log(Level.WARNING, null, ex);
                            }
                        }

                        Collection<? extends IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> eifInfos = collectEmbeddingIndexers(mimeType);
                        for(IndexerCache.IndexerInfo<EmbeddingIndexerFactory> info : eifInfos) {
                            try {
                                final EmbeddingIndexerFactory factory = info.getIndexerFactory();
                                final Context ctx = SPIAccessor.getInstance().createContext(
                                        indexFolderFactory,
                                        root.first(),
                                        factory.getIndexerName(),
                                        factory.getIndexVersion(),
                                        null,
                                        false,
                                        true,
                                        false,
                                        SuspendSupport.NOP,
                                        null,
                                        null);
                                factory.filesDirty(dirty, ctx);
                            } catch (IOException ex) {
                                LOGGER.log(Level.WARNING, null, ex);
                            }
                        }
                    } finally {
                        TransientUpdateSupport.setTransientUpdate(false);
                    }
                }
            } else {
                // an odd event, maybe we could just ignore it
                try {
                    FileObject f = Util.getFileObject(document);
                    Collection<URL> c = Collections.singleton(f.getURL());
                    addIndexingJob(
                        root.first(),
                        c,
                        false,
                        true,
                        false,
                        true,
                        true,
                        LogContext.create(LogContext.EventType.FILE, null).
                            withRoot(root.first()).
                            addFiles(c));
                } catch (FileStateInvalidException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                }
            }
        }
    }

    @NonNull
    private static Collection<? extends IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> collectEmbeddingIndexers(
            @NonNull final String topMimeType){
        final Collection<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> result = new ArrayDeque<IndexerInfo<EmbeddingIndexerFactory>>();
        collectEmbeddingIndexers(topMimeType, result);
        return result;
    }

    private static void collectEmbeddingIndexers(
            @NonNull final String mimeType,
            @NonNull final Collection<? super IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> collector) {
        collector.addAll(IndexerCache.getEifCache().getIndexersFor(mimeType, true));
        for (EmbeddingProviderFactory epf : MimeLookup.getLookup(MimePath.get(mimeType)).lookupAll(EmbeddingProviderFactory.class)) {
            collectEmbeddingIndexers(epf.getTargetMimeType(), collector);
        }
    }

    /* test */ void scheduleWork(Iterable<? extends Work> multipleWork) {
        recordCaller();

        boolean canScheduleMultiple;
        synchronized (this) {
            canScheduleMultiple = state == State.INITIAL_SCAN_RUNNING || state == State.ACTIVE;
        }

        if (canScheduleMultiple) {
            getWorker().schedule(multipleWork);
        } else {
            for(Work w : multipleWork) {
                scheduleWork(w, false);
            }
        }
    }

    /* test */ void scheduleWork(Work work, boolean wait) {
        recordCaller();

        boolean scheduleExtraWork = false;

        synchronized (this) {
            if (state == State.STARTED) {
                state = State.INITIAL_SCAN_RUNNING;
                scheduleExtraWork = !(work instanceof InitialRootsWork);
            }
        }

        if (scheduleExtraWork) {
            getWorker().schedule(new InitialRootsWork(
                scannedRoots2Dependencies,
                scannedBinaries2InvDependencies,
                scannedRoots2Peers,
                sourcesForBinaryRoots,
                true,
                scannedRoots2DependenciesLamport,
                suspendSupport.getSuspendStatus(),
                work == null ?
                    LogContext.create(LogContext.EventType.PATH, null)
                    : work.getLogContext()), false);

            if (work instanceof RootsWork) {
                // if the work is the initial RootsWork it's superseeded
                // by the RootsWork we've just scheduled and so we can quit now.
                return;
            }
        }

        if (work != null) {
            getWorker().schedule(work, wait);
        }
    }

    public void runAsWork(@NonNull final Runnable r) {
        assert r != null;
        final Work work = new Work(false, false, false, true, SuspendSupport.NOP, null) {
            @Override
            protected boolean getDone() {
                r.run();
                return true;
            }
        };
        getWorker().schedule(work, false);
    }

    private Task getWorker () {
        Task t = this.worker;
        if (t == null) {
            synchronized (this) {
                if (this.worker == null) {
                    this.worker = new Task ();
                }
                t = this.worker;
            }
        }
        return t;
    }

    public Pair<URL, FileObject> getOwningSourceRoot(Object fileOrDoc) {        
        FileObject file = null;
        Document doc = null;        
        List<URL> clone;
        final long current = scannedRoots2DependenciesLamport.get();
        synchronized (lastOwningSourceRootCacheLock) {            
            if (fileOrDoc instanceof Document) {
                doc = (Document) fileOrDoc;
                file = Util.getFileObject(doc);
                if (file == null) {
                    return null;
                }
                URL cachedSourceRootUrl = (URL) doc.getProperty(PROP_OWNING_SOURCE_ROOT_URL);
                FileObject cachedSourceRoot = (FileObject) doc.getProperty(PROP_OWNING_SOURCE_ROOT);
                if (cachedSourceRootUrl != null && cachedSourceRoot != null && cachedSourceRoot.isValid() && FileUtil.isParentOf(cachedSourceRoot, file)) {
                    return Pair.of(cachedSourceRootUrl, cachedSourceRoot);
                } else {
                    final Long unknownIn = (Long) doc.getProperty(PROP_OWNING_SOURCE_UNKNOWN_IN);
                    if (unknownIn != null && unknownIn.longValue() == current) {
                        return null;
                    }
                }
            } else if (fileOrDoc instanceof FileObject) {
                file = (FileObject) fileOrDoc;
            } else {
                return null;
            }
            clone = new ArrayList<URL> (this.scannedRoots2Dependencies.keySet());
        }
        
        assert file != null;
        URL owningSourceRootUrl = null;
        FileObject owningSourceRoot = null;

        for (URL root : clone) {
            try {
                FileObject rootFo = URLCache.getInstance().findFileObject(root, false);
                if (rootFo != null) {
                    if (rootFo.equals(file) || FileUtil.isParentOf(rootFo,file)) {
                        owningSourceRootUrl = root;
                        owningSourceRoot = rootFo;
                        break;
                    }
                } else if (file.getURL().toExternalForm().startsWith(root.toExternalForm())) {
                    owningSourceRootUrl = root;
                    owningSourceRoot = rootFo;
                    break;
                }
            } catch (FileStateInvalidException fsi) {
                Exceptions.printStackTrace(fsi);
            }
        }

        synchronized (lastOwningSourceRootCacheLock) {        
            if (owningSourceRootUrl != null) {
                if (doc != null && file.isValid()) {
                    assert owningSourceRoot != null : "Expecting both owningSourceRootUrl=" + owningSourceRootUrl + " and owningSourceRoot=" + owningSourceRoot; //NOI18N
                    doc.putProperty(PROP_OWNING_SOURCE_ROOT_URL, owningSourceRootUrl);
                    doc.putProperty(PROP_OWNING_SOURCE_ROOT, owningSourceRoot);
                }
                return Pair.of(owningSourceRootUrl, owningSourceRoot);
            } else {
                if (doc != null) {
                    doc.putProperty(PROP_OWNING_SOURCE_UNKNOWN_IN, current);
                }
                return null;
            }
        }
    }

    Pair<URL, FileObject> getOwningBinaryRoot(final FileObject fo) {
        if (fo == null) {
            return null;
        }
        String foPath;
        try {
            foPath = fo.getURL().getPath();
        } catch (FileStateInvalidException fsie) {
            LOGGER.log(Level.WARNING, null, fsie);
            return null;
        }

        List<URL> clone = new ArrayList<URL>(this.scannedBinaries2InvDependencies.keySet());
        for (URL root : clone) {
            URL fileURL = FileUtil.getArchiveFile(root);
            boolean archive = true;
            if (fileURL == null) {
                fileURL = root;
                archive = false;
            }
            String filePath = fileURL.getPath();
            if (filePath.equals(foPath)) {
                return Pair.of(root, null);
            }
            if (!archive && foPath.startsWith(filePath)) {
                return Pair.of(root, null);
            }
        }

        return null;
    }

    @org.netbeans.api.annotations.common.SuppressWarnings(
    value="DMI_BLOCKING_METHODS_ON_URL",
    justification="URLs have never host part")
    private static ClassPath.Entry getClassPathEntry (final FileObject root) {
        try {
            if (root != null) {
                Set<String> ids = PathRegistry.getDefault().getSourceIdsFor(root.getURL());
                if (ids != null) {
                    for (String id : ids) {
                        ClassPath cp = ClassPath.getClassPath(root, id);
                        if (cp != null) {
                            URL rootURL = root.getURL();
                            for (ClassPath.Entry e : cp.entries()) {
                                if (rootURL.equals(e.getURL())) {
                                    return e;
                                }
                            }
                        }
                    }
                }
            }
        } catch (FileStateInvalidException fsie) {}
        return null;
    }

    private boolean authorize(FileEvent event) {
        Collection<? extends IndexingActivityInterceptor> interceptors = indexingActivityInterceptors.allInstances();
        for(IndexingActivityInterceptor i : interceptors) {
            if (i.authorizeFileSystemEvent(event) == IndexingActivityInterceptor.Authorization.IGNORE) {
                return false;
            }
        }
        return true;
    }
    
    public boolean isCacheFile(FileObject f) {
        return FileUtil.isParentOf(CacheFolder.getCacheFolder(), f);
    }

    private static void collectFilePaths(FileObject folder, String pathPrefix, Set<String> collectedPaths) {
        assert folder.isFolder() : "Expecting folder: " + folder; //NOI18N

        if (folder.isValid()) {
            for(FileObject kid : folder.getChildren()) {
                if (kid.isValid()) {
                    String kidPath = pathPrefix + "/" + kid.getNameExt(); //NOI18N
                    if (kid.isData()) {
                        collectedPaths.add(kidPath); //NOI18N
                    } else {
                        collectFilePaths(kid, kidPath, collectedPaths);
                    }
                }
            }
        }
    }

    @org.netbeans.api.annotations.common.SuppressWarnings(
        value={"DMI_COLLECTION_OF_URLS","DMI_BLOCKING_METHODS_ON_URL"},
        justification="URLs have never host part")
    private static boolean findDependencies(
            final URL rootURL,
            final DependenciesContext ctx,
            Set<String> sourceIds,
            Set<String> libraryIds,
            Set<String> binaryLibraryIds,
            @NonNull final CancelRequest cancelRequest,
            @NonNull final SuspendStatus suspendStatus)
    {
        try {
            suspendStatus.parkWhileSuspended();
        } catch (InterruptedException ex) {
            //pass - cancelled
        }
        if (cancelRequest.isRaised()) {
            return false;
        }

        if (ctx.useInitialState) {
            final List<URL> deps = ctx.initialRoots2Deps.get(rootURL);
            //If already scanned ignore
            //If deps == EMPTY_DEPS needs to be rescanned (keep it in oldRoots,
            //it will be removed from scannedRoots2Dependencies and readded from
            //scannedRoots
            if (deps != null && deps != UNKNOWN_ROOT &&
                (deps != NONEXISTENT_ROOT || !ctx.refreshNonExistentDeps)) {
                ctx.oldRoots.remove(rootURL);
                return true;
            }
        }
        if (ctx.newRoots2Deps.containsKey(rootURL)) {
            return true;
        }
        final FileObject rootFo = URLMapper.findFileObject(rootURL);
        if (rootFo == null) {
            ctx.newRoots2Deps.put(rootURL, NONEXISTENT_ROOT);
            return true;
        }

        final List<URL> deps = new LinkedList<URL>();
        final List<URL> peers = new LinkedList<URL>();
        ctx.cycleDetector.push(rootURL);
        try {
            if (sourceIds == null || libraryIds == null || binaryLibraryIds == null) {
                Set<String> ids;
                if (null != (ids = PathRegistry.getDefault().getSourceIdsFor(rootURL)) && !ids.isEmpty()) {
                    LOGGER.log(Level.FINER, "Resolving Ids based on sourceIds for {0}: {1}", new Object [] { rootURL, ids }); //NOI18N
                    Set<String> lids = new HashSet<String>();
                    Set<String> blids = new HashSet<String>();
                    for(String id : ids) {
                        lids.addAll(PathRecognizerRegistry.getDefault().getLibraryIdsForSourceId(id));
                        blids.addAll(PathRecognizerRegistry.getDefault().getBinaryLibraryIdsForSourceId(id));
                    }
                    if (sourceIds == null) {
                        sourceIds = ids;
                    }
                    if (libraryIds == null) {
                        libraryIds = lids;
                    }
                    if (binaryLibraryIds == null) {
                        binaryLibraryIds = blids;
                    }                    
                } else if (null != (ids = PathRegistry.getDefault().getLibraryIdsFor(rootURL)) && !ids.isEmpty()) {
                    LOGGER.log(Level.FINER, "Resolving Ids based on libraryIds for {0}: {1}", new Object [] { rootURL, ids }); //NOI18N
                    Set<String> blids = new HashSet<String>();
                    for(String id : ids) {
                        blids.addAll(PathRecognizerRegistry.getDefault().getBinaryLibraryIdsForLibraryId(id));
                    }
                    if (sourceIds == null) {
                        sourceIds = Collections.emptySet();
                    }
                    if (libraryIds == null) {
                        libraryIds = ids;
                    }
                    if (binaryLibraryIds == null) {
                        binaryLibraryIds = blids;
                    }
                } else if (sourceIds == null) {
                    sourceIds = Collections.emptySet();
                }
            }

            if (cancelRequest.isRaised()) {
                return false;
            }

            LOGGER.log(Level.FINER, "SourceIds for {0}: {1}", new Object [] { rootURL, sourceIds }); //NOI18N
            LOGGER.log(Level.FINER, "LibraryIds for {0}: {1}", new Object [] { rootURL, libraryIds }); //NOI18N
            LOGGER.log(Level.FINER, "BinaryLibraryIds for {0}: {1}", new Object [] { rootURL, binaryLibraryIds }); //NOI18N
                        
            { // sources
                for (String id : sourceIds) {
                    if (cancelRequest.isRaised()) {
                        return false;
                    }
                    final ClassPath cp = ClassPath.getClassPath(rootFo, id);
                    if (cp != null) {
                        for (ClassPath.Entry entry : cp.entries()) {
                            if (cancelRequest.isRaised()) {
                                return false;
                            }
                            final URL sourceRoot = entry.getURL();
                            assert PathRegistry.noHostPart(sourceRoot) : sourceRoot;
                            if (!rootURL.equals(sourceRoot)) {
                                peers.add(entry.getURL());
                            }
                        }
                    }
                }
            }
            { // libraries
                if (libraryIds != null) {
                    for (String id : libraryIds) {
                        if (cancelRequest.isRaised()) {
                            return false;
                        }

                        ClassPath cp = ClassPath.getClassPath(rootFo, id);
                        if (cp != null) {
                            for (ClassPath.Entry entry : cp.entries()) {
                                if (cancelRequest.isRaised()) {
                                    return false;
                                }

                                final URL sourceRoot = entry.getURL();
                                if (!sourceRoot.equals(rootURL) && !ctx.cycleDetector.contains(sourceRoot)) {
                                    deps.add(sourceRoot);
    //                                    LOGGER.log(Level.FINEST, "#1- {0}: adding dependency on {1}, from {2} with id {3}", new Object [] {
    //                                        rootURL, sourceRoot, cp, id
    //                                    });
                                    assert PathRegistry.noHostPart(sourceRoot) : sourceRoot;
                                    if (!findDependencies(
                                            sourceRoot,
                                            ctx,
                                            sourceIds,
                                            libraryIds,
                                            binaryLibraryIds,
                                            cancelRequest,
                                            suspendStatus)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            { // binary libraries
                final Set<String> ids = binaryLibraryIds == null ? PathRecognizerRegistry.getDefault().getBinaryLibraryIds() : binaryLibraryIds;
                for (String id : ids) {
                    if (cancelRequest.isRaised()) {
                        return false;
                    }

                    ClassPath cp = ClassPath.getClassPath(rootFo, id);
                    if (cp != null) {
                        for (ClassPath.Entry entry : cp.entries()) {
                            if (cancelRequest.isRaised()) {
                                return false;
                            }

                            final URL binaryRoot = entry.getURL();
                            final URL[] sourceRoots = PathRegistry.getDefault().sourceForBinaryQuery(binaryRoot, cp, false, FD_NEW_SFB_ROOT);
                            final boolean newSFBRoot = FD_NEW_SFB_ROOT[0];
                            if (sourceRoots != null) {                                
                                for (URL sourceRoot : sourceRoots) {
                                    if (cancelRequest.isRaised()) {
                                        return false;
                                    }
                                    if (newSFBRoot) {
                                        ctx.newlySFBTranslated.add(sourceRoot);
                                    }
                                    if (sourceRoot.equals(rootURL)) {
                                        ctx.sourcesForBinaryRoots.add(rootURL);
                                    } else if (!ctx.cycleDetector.contains(sourceRoot)) {
                                        deps.add(sourceRoot);
//                                            LOGGER.log(Level.FINEST, "#2- {0}: adding dependency on {1}, from {2} with id {3}", new Object [] {
//                                                rootURL, sourceRoot, cp, id
//                                            });
                                        if (!findDependencies(
                                                sourceRoot,
                                                ctx,
                                                sourceIds,
                                                libraryIds,
                                                binaryLibraryIds,
                                                cancelRequest,
                                                suspendStatus)) {
                                            return false;
                                        }
                                    }
                                }
                            }
                            else {
                                //What does it mean?
                                if (ctx.useInitialState) {
                                    if (!ctx.initialBinaries2InvDeps.keySet().contains(binaryRoot)) {
                                        ctx.newBinariesToScan.add (binaryRoot);
                                        List<URL> binDeps = ctx.newBinaries2InvDeps.get(binaryRoot);
                                        if (binDeps == null) {
                                            binDeps = new LinkedList<URL>();
                                            ctx.newBinaries2InvDeps.put(binaryRoot, binDeps);
                                        }
                                        binDeps.add(rootURL);
                                    }
                                } else {
                                    ctx.newBinariesToScan.add(binaryRoot);
                                    List<URL> binDeps = ctx.newBinaries2InvDeps.get(binaryRoot);
                                    if (binDeps == null) {
                                        binDeps = new LinkedList<URL>();
                                        ctx.newBinaries2InvDeps.put(binaryRoot, binDeps);
                                    }
                                    binDeps.add(rootURL);
                                }

                                Set<String> srcIdsForBinRoot = PathRegistry.getDefault().getSourceIdsFor(binaryRoot);
                                if (srcIdsForBinRoot == null || srcIdsForBinRoot.isEmpty()) {
// In some cases people have source roots among libraries for some reason. Misconfigured project?
// Maybe. Anyway, just do the regular check for cycles.
//                                        assert !binaryRoot.equals(rootURL) && !ctx.cycleDetector.contains(binaryRoot) :
//                                            "binaryRoot=" + binaryRoot + //NOI18N
//                                            ", rootURL=" + rootURL + //NOI18N
//                                            ", cycleDetector.contains(" + binaryRoot + ")=" + ctx.cycleDetector.contains(binaryRoot); //NOI18N

                                    if (!binaryRoot.equals(rootURL) && !ctx.cycleDetector.contains(binaryRoot)) {
                                        deps.add(binaryRoot);
                                    }
                                } else {
                                    LOGGER.log(Level.INFO, "The root {0} is registered for both {1} and {2}", new Object[] { //NOI18N
                                        binaryRoot, id, srcIdsForBinRoot
                                    });
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            ctx.cycleDetector.pop();
        }

        ctx.newRoots2Deps.put(rootURL, deps);
        ctx.newRoots2Peers.put(rootURL,peers);
        return true;
    }

    // XXX: this should ideally be available directly from EditorRegistry
    private static Map<FileObject, Document> getEditorFiles() {
        Map<FileObject, Document> f2d = new HashMap<FileObject, Document>();
        for(JTextComponent jtc : EditorRegistry.componentList()) {
            Document d = jtc.getDocument();
            FileObject f = Util.getFileObject(d);
            if (f != null) {
                f2d.put(f, d);
            }
        }
        return f2d;
    }

    private static boolean getSystemBoolean(String propertyName, boolean defaultValue) {
        if (defaultValue) {
            String value = System.getProperty(propertyName);
            return value == null || !value.equals("false"); //NOI18N
        } else {
            return Boolean.getBoolean(propertyName);
        }
    }

    private static final Map<List<StackTraceElement>, Long> lastRecordedStackTraces = new HashMap<List<StackTraceElement>, Long>();
    private static long stackTraceId = 0;
    private static void recordCaller() {
        if (!LOGGER.isLoggable(Level.FINE)) {
            return;
        }

        synchronized (lastRecordedStackTraces) {
            StackTraceElement []  stackTrace = Thread.currentThread().getStackTrace();
            List<StackTraceElement> stackTraceList = new ArrayList<StackTraceElement>(stackTrace.length);
            stackTraceList.addAll(Arrays.asList(stackTrace));

            Long id = lastRecordedStackTraces.get(stackTraceList);
            if (id == null) {
                id = stackTraceId++;
                lastRecordedStackTraces.put(stackTraceList, id);
                StringBuilder sb = new StringBuilder();
                sb.append("RepositoryUpdater caller [id=").append(id).append("] :\n"); //NOI18N
                for(StackTraceElement e : stackTraceList) {
                    sb.append(e.toString());
                    sb.append("\n"); //NOI18N
                }
                LOGGER.fine(sb.toString());
            } else {
                StackTraceElement caller = Util.findCaller(stackTrace);
                LOGGER.fine("RepositoryUpdater caller [refid=" + id + "]: " + caller); //NOI18N
            }
        }
    }

    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    private static void printMap(Map<URL, List<URL>> deps, Level level) {
        Set<URL> sortedRoots = new TreeSet<URL>(C);
        sortedRoots.addAll(deps.keySet());
        for(URL url : sortedRoots) {
            LOGGER.log(level, "  {0}:\n", url); //NOI18N
//            for(URL depUrl : deps.get(url)) {
//                LOGGER.log(level, "  -> {0}\n", depUrl); //NOI18N
//            }
        }
    }

    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    private static StringBuilder printMap(Map<URL, List<URL>> deps, StringBuilder sb) {
        Set<URL> sortedRoots = new TreeSet<URL>(C);
        sortedRoots.addAll(deps.keySet());
        for(URL url : sortedRoots) {
            sb.append("  ").append(url).append(":\n"); //NOI18N
//            for(URL depUrl : deps.get(url)) {
//                sb.append("  -> ").append(depUrl).append("\n"); //NOI18N
//            }
        }
        return sb;
    }

    private static void printCollection(Collection<? extends URL> collection, Level level) {
        Set<URL> sortedRoots = new TreeSet<URL>(C);
        sortedRoots.addAll(collection);
        for(URL url : sortedRoots) {
            LOGGER.log(level, "  {0}\n", url); //NOI18N
        }
    }

    private static StringBuilder printCollection(Collection<? extends URL> collection, StringBuilder sb) {
        Set<URL> sortedRoots = new TreeSet<URL>(C);
        sortedRoots.addAll(collection);
        for(URL url : sortedRoots) {
            sb.append("  ").append(url).append("\n"); //NOI18N
        }
        return sb;
    }

    private static StringBuilder printMimeTypes(Collection<? extends String> collection, StringBuilder sb) {
        for(Iterator<? extends String> i = collection.iterator(); i.hasNext(); ) {
            String mimeType = i.next();
            sb.append("'").append(mimeType).append("'"); //NOI18N
            if (i.hasNext()) {
                sb.append(", "); //NOI18N
            }
        }
        return sb;
    }
    

    private static final Comparator<URL> C = new Comparator<URL>() {
        public @Override int compare(URL o1, URL o2) {
            return o1.toString().compareTo(o2.toString());
        }
    };
    
// we have to handle *all* mime types because of eg. tasklist indexer or goto-file indexer
//    private static boolean isMonitoredMimeType(FileObject f, Set<String> mimeTypes) {
//        String mimeType = FileUtil.getMIMEType(f, mimeTypes.toArray(new String[mimeTypes.size()]));
//        return mimeType != null && mimeTypes.contains(mimeType);
//    }

    enum State {CREATED, STARTED, INITIAL_SCAN_RUNNING, ACTIVE, STOPPED};

    /* test */ static abstract class Work {

        //@GuardedBy("org.netbeans.modules.parsing.impl.Taskprocessor.parserLock")
        private static long lastScanEnded = -1L;

        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private final AtomicBoolean externalCancel = new AtomicBoolean(false);
        private final boolean followUpJob;
        private final boolean checkEditor;
        private final boolean steady;
        private final CountDownLatch latch = new CountDownLatch(1);
        private final CancelRequestImpl cancelRequest = new CancelRequestImpl(cancelled);
        private final String progressTitle;
        private final SuspendStatus suspendStatus;
        private volatile LogContext logCtx;
        private final Object progressLock = new Object();
        //@GuardedBy("progressLock")
        private ProgressHandle progressHandle = null;
        //@GuardedBy("progressLock")
        private int progress = -1;
        //Indexer statistics <IndexerName,{InvocationCount,CumulativeTime}>
        //threading: Has to be SynchronizedMap or ConcurrentMap to ensure propper
        //visibility. The Work.scanBinaries modifies the map from multiple threads.
        private final Map<String,int[]> indexerStatistics = Collections.<String, int[]>synchronizedMap(new HashMap<String, int[]>());
        private volatile boolean reportIndexerStatistics;
        
        protected Work(
                final boolean followUpJob,
                final boolean checkEditor,
                final boolean supportsProgress,
                final boolean steady,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext logCtx) {
            this(
                followUpJob,
                checkEditor,
                supportsProgress ? NbBundle.getMessage(RepositoryUpdater.class, "MSG_BackgroundCompileStart") : null, //NOI18N
                steady,
                suspendStatus,
                logCtx
            );
        }

        protected Work(
                final boolean followUpJob,
                final boolean checkEditor,
                final String progressTitle,
                final boolean steady,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext logCtx) {
            assert suspendStatus != null;
            this.followUpJob = followUpJob;
            this.checkEditor = checkEditor;
            this.progressTitle = progressTitle;
            this.steady = steady;
            this.suspendStatus = suspendStatus;
            this.logCtx = logCtx;
        }

        @CheckForNull
        protected final LogContext getLogContext() {
            return this.logCtx;
        }

        protected final boolean isFollowUpJob() {
            return followUpJob;
        }
        
        protected final boolean hasToCheckEditor() {
            return checkEditor;
        }

        protected final boolean isSteady() {
            return this.steady;
        }

        protected final void updateProgress(String message) {
            assert message != null;
            synchronized (progressLock) {
                if (progressHandle == null) {
                    return;
                }
                progressHandle.progress(message);
            }
        }

        protected final void updateProgress(URL currentlyScannedRoot, boolean increment) {
            assert currentlyScannedRoot != null;
            synchronized (progressLock) {
                if (progressHandle == null) {
                    return;
                }
                if (increment && progress != -1) {
                    progressHandle.progress(urlForMessage(currentlyScannedRoot), ++progress);
                } else {
                    progressHandle.progress(urlForMessage(currentlyScannedRoot));
                }
            }
        }

        protected final void switchProgressToDeterminate(final int workunits) {
            synchronized (progressLock) {
                if (progressHandle == null) {
                    return;
                }
                progress = 0;
                progressHandle.switchToDeterminate(workunits);
            }
        }
        
        protected final void suspendProgress(@NonNull final String message) {
            synchronized (progressLock) {
                if (progressHandle == null) {
                    return;
                }
                progressHandle.suspend(message);
            }
        }
        
        /**
         * Number of modified resources found by the crawler. Actually initialized by subclass Work
         */
        protected int modifiedResourceCount;
        
        /**
         * Number of all resources in the source root
         */
        protected int allResourceCount;
        
        private Preferences indexerProfileNode(SourceIndexerFactory srcFactory) {
            String nn = srcFactory.getIndexerName();
            if (nn.length() >= Preferences.MAX_NAME_LENGTH) {
                // such long nodes are constructer e.g. from class names
                int i = nn.lastIndexOf('.');
                if (i >= 0) {
                    nn = nn.substring(i + 1);
                }
                if (nn.length() < 3 || nn.length() >= Preferences.MAX_NAME_LENGTH) {
                    String hashCode = Integer.toHexString(nn.hashCode());
                    // attempt to derive +- unique node name
                    nn = srcFactory.getClass().getSimpleName() + "_" + hashCode; // NOI18N
                }
            }
            return NbPreferences.forModule(srcFactory.getClass()).node("RepositoryUpdater"). // NOI18N
                    node(nn);
        }
        
        // because of multiplexing in CSL, the node path must include mime type or indexer name, so
        // each of the end SPI modules can have its own preferences.
        private int estimateEmbeddingIndexer(SourceIndexerFactory srcFactory) {
            Preferences pref = indexerProfileNode(srcFactory);
            int c1 = pref.getInt("modifiedScanTime", 500) + pref.getInt("modifiedBaseTime", 100); // NOI18N
            return modifiedResourceCount * c1;
        }
        
        /**
         * Makes an estimate how fast a custom indexer is
         * 
         * @param indexerName
         * @return estimate time [ms] for indexer start completion
         */
        private int estimateCustomStartTime(CustomIndexerFactory srcFactory) {
            // PENDING: modify the time, if the source root is a remote or otherwise slow filesystem
            if (modifiedResourceCount == 0 && allResourceCount == 0) {
                return -1;
            }            
            Preferences moduleNode = indexerProfileNode(srcFactory);
            int c1 = moduleNode.getInt("modifiedStartTime", 500); // NOI18N
            int c2 = moduleNode.getInt("fileStartTime", 300); // NOI18N
            int c3 = moduleNode.getInt("startBaseTime", 300); // NOI18N
            moduleNode.putBoolean("hello", true);
            int threshold = Math.max(modifiedResourceCount * c1, allResourceCount * c2) + c3;
            
            return threshold;
        }

        private int estimateCustomIndexingTime(CustomIndexerFactory srcFactory) {
            // PENDING: modify the time, if the source root is a remote or otherwise slow filesystem
            if (modifiedResourceCount == 0 && allResourceCount == 0) {
                return -1;
            }            
            Preferences moduleNode = indexerProfileNode(srcFactory);
            int c1 = moduleNode.getInt("modifiedScanTime", 500); // NOI18N
            int c2 = moduleNode.getInt("fileScanTime", 300); // NOI18N
            int c3 = moduleNode.getInt("indexingBaseTime", 300); // NOI18N
            
            int threshold = Math.max(modifiedResourceCount * c1, allResourceCount * c2) + c3;
            
            return threshold;
        }

        private int estimateSourceEndTime(SourceIndexerFactory srcFactory) {
            // PENDING: modify the time, if the source root is a remote or otherwise slow filesystem
            if (modifiedResourceCount == 0 && allResourceCount == 0) {
                return -1;
            }            
            Preferences moduleNode = indexerProfileNode(srcFactory);
            int c1 = moduleNode.getInt("modifiedEndTime", 500); // NOI18N
            int c2 = moduleNode.getInt("fileEndTime", 300); // NOI18N
            int c3 = moduleNode.getInt("indexingEndTime", 300); // NOI18N
            
            int threshold = Math.max(modifiedResourceCount * c1, allResourceCount * c2) + c3;
            
            return threshold;
        }

        protected final void scanStarted(final URL root, final boolean sourceForBinaryRoot,
                                   final SourceIndexers indexers, final Map<SourceIndexerFactory,Boolean> votes,
                                   final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> ctxToFinish) throws IOException {
            final FileObject cacheRoot = CacheFolder.getDataFolder(root);
            customIndexersScanStarted(root, cacheRoot, sourceForBinaryRoot, indexers.cifInfos, votes, ctxToFinish);
            embeddingIndexersScanStarted(root, cacheRoot, sourceForBinaryRoot, indexers.eifInfosMap.values(), votes, ctxToFinish);
        }

        protected final void customIndexersScanStarted(
            @NonNull final URL root,
            @NonNull final FileObject cacheRoot,
            final boolean sourceForBinaryRoot,
            final Collection<? extends IndexerCache.IndexerInfo<CustomIndexerFactory>> indexers,
            final Map<SourceIndexerFactory,Boolean> votes,
            final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> ctxToFinish) throws IOException {

            for(IndexerCache.IndexerInfo<CustomIndexerFactory> cifInfo : indexers) {
                parkWhileSuspended();
                final CustomIndexerFactory factory = cifInfo.getIndexerFactory();
                final Pair<String,Integer> key = Pair.of(factory.getIndexerName(),factory.getIndexVersion());
                Pair<SourceIndexerFactory,Context> value = ctxToFinish.get(key);
                if (TEST_LOGGER.isLoggable(Level.FINEST)) {
                    TEST_LOGGER.log(Level.FINEST, "scanStarting:{0}:{1}",
                            new Object[] { factory.getIndexerName(), root.toString() });
                }
                if (value == null) {
                    final Context ctx = SPIAccessor.getInstance().createContext(
                            cacheRoot,
                            root,
                            factory.getIndexerName(),
                            factory.getIndexVersion(),
                            null,
                            followUpJob,
                            checkEditor,
                            sourceForBinaryRoot,
                            getSuspendStatus(),
                            getCancelRequest(),
                            logCtx);
                    value = Pair.<SourceIndexerFactory,Context>of(factory,ctx);
                    ctxToFinish.put(key,value);
                }
                logStartIndexer(factory.getIndexerName());
                try {
                    boolean vote = doStartCustomIndexer(factory, value.second());
                    votes.put(factory,vote);
                } catch (Throwable t) {
                    if (t instanceof ThreadDeath) {
                        throw (ThreadDeath) t;
                    }
                    votes.put(factory, false);
                    Exceptions.printStackTrace(t);
                } finally {
                    logFinishIndexer(factory.getIndexerName());
                }
            }
        }
        
        private boolean doStartCustomIndexer(final CustomIndexerFactory factory, Context factoryContext) {
            int estimate = estimateCustomStartTime(factory);
            SamplerInvoker.start(getLogContext(), factory.getIndexerName(), estimate, factoryContext.getRootURI());
            try {
                return factory.scanStarted(factoryContext);
            } finally {
                // cancel the task. If the task is already running, let it be
                SamplerInvoker.stop();
            }
        }

        protected final void embeddingIndexersScanStarted(
            @NonNull final URL root,
            @NonNull final FileObject cacheRoot,
            final boolean sourceForBinaryRoot,
            final Collection<Collection<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>>> indexers,
            final Map<SourceIndexerFactory,Boolean> votes,
            final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> ctxToFinish) throws IOException {

            for(Collection<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> eifInfos : indexers) {
                for(IndexerCache.IndexerInfo<EmbeddingIndexerFactory> eifInfo : eifInfos) {
                    parkWhileSuspended();
                    EmbeddingIndexerFactory eif = eifInfo.getIndexerFactory();
                    final Pair<String,Integer> key = Pair.of(eif.getIndexerName(), eif.getIndexVersion());
                    Pair<SourceIndexerFactory,Context> value = ctxToFinish.get(key);
                    if (value == null) {
                        final Context context = SPIAccessor.getInstance().createContext(
                                cacheRoot,
                                root,
                                eif.getIndexerName(),
                                eif.getIndexVersion(),
                                null,
                                followUpJob,
                                checkEditor,
                                sourceForBinaryRoot,
                                getSuspendStatus(),
                                getCancelRequest(),
                                logCtx);
                        value = Pair.<SourceIndexerFactory,Context>of(eif,context);
                        ctxToFinish.put(key, value);
                    }
                    logStartIndexer(eif.getIndexerName());
                    try {
                        boolean vote = eif.scanStarted(value.second());
                        votes.put(eif, vote);
                    } catch (Throwable t) {
                        if (t instanceof ThreadDeath) {
                            throw (ThreadDeath) t;
                        }
                        votes.put(eif, false);
                        Exceptions.printStackTrace(t);
                    } finally {
                        logFinishIndexer(eif.getIndexerName());
                    }
                }
            }
        }

        protected final void scanFinished(
                @NonNull final Collection<Pair<SourceIndexerFactory,Context>> ctxToFinish,
                @NonNull final UsedIndexables usedIterables,
                final boolean finished) throws IOException {
            try {
                for (Pair<SourceIndexerFactory,Context> entry : ctxToFinish) {
                    parkWhileSuspended();
                    if (TEST_LOGGER.isLoggable(Level.FINEST)) {
                        TEST_LOGGER.log(Level.FINEST, "scanFinishing:{0}:{1}", 
                                new Object[] { entry.first().getIndexerName(), entry.second().getRootURI().toExternalForm() });
                    }
                    logStartIndexer(entry.first().getIndexerName());
                    SPIAccessor.getInstance().putProperty(entry.second(), ClusteredIndexables.DELETE, null);
                    SPIAccessor.getInstance().putProperty(entry.second(), ClusteredIndexables.INDEX, null);
                    cancelRequest.setResult(finished);
                    try {
                        int estimate = estimateSourceEndTime(entry.first());
                        SamplerInvoker.start(getLogContext(), entry.first().getIndexerName(), estimate, entry.second().getRootURI());
                        entry.first().scanFinished(entry.second());
                    }  catch (Throwable t) {
                        if (t instanceof ThreadDeath) {
                            throw (ThreadDeath) t;
                        } else {
                            Exceptions.printStackTrace(t);
                            SamplerInvoker.stop();
                        }
                    } finally {
                        cancelRequest.setResult(null);
                    }
                    logFinishIndexer(entry.first().getIndexerName());
                    if (TEST_LOGGER.isLoggable(Level.FINEST)) {
                        TEST_LOGGER.log(Level.FINEST, "scanFinished:{0}:{1}", 
                                new Object[] { entry.first().getIndexerName(), entry.second().getRootURI().toExternalForm() });
                    }
                }
            } finally {
                try {
                    boolean indexOk = true;
                    Union2<IOException,RuntimeException> exception = null;
                    for(Pair<SourceIndexerFactory,Context> entry : ctxToFinish) {
                        try {
                            indexOk &= storeChanges(
                                    entry.first().getIndexerName(),
                                    entry.second(),
                                    isSteady(),
                                    usedIterables.get(),
                                    finished);
                        } catch (IOException e) {
                            exception = Union2.createFirst(e);
                        } catch (RuntimeException e) {
                            exception = Union2.createSecond(e);
                        }
                    }
                    if (exception != null) {
                        //Do not reschedule scan, the excepion comes from clear()
                        //rescheduling scan will cause infinite scan.
                        if (exception.hasFirst()) {
                            throw exception.first();
                        } else {
                            throw exception.second();
                        }
                    } else if (!indexOk) {
                        final Context ctx = ctxToFinish.iterator().next().second();
                        RepositoryUpdater.getDefault().addIndexingJob(
                            ctx.getRootURI(),
                            null,
                            false,
                            false,
                            false,
                            true,
                            true,
                            LogContext.create(
                                LogContext.EventType.UI,
                                "Broken Index Found."));    //NOI18N
                    }
                } finally {
                    InjectedTasksSupport.clear();
                }
            }
        }

        protected final void delete (
            @NonNull final List<Indexable> deleted,
            @NonNull final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> contexts,
            @NonNull final UsedIndexables usedIterables) throws IOException {
            if (deleted == null || deleted.isEmpty()) {
                return;
            }

            final ClusteredIndexables ci = new ClusteredIndexables(deleted);
            try {
                for (Pair<SourceIndexerFactory,Context> pair : contexts.values()) {
                    parkWhileSuspended();
                    SPIAccessor.getInstance().putProperty(pair.second(), ClusteredIndexables.DELETE, ci);
                    pair.first().filesDeleted(ci.getIndexablesFor(null), pair.second());
                }
            } finally {
                for(Pair<SourceIndexerFactory,Context> pair : contexts.values()) {
                    final Context ctx = pair.second();
                    final FileObject indexFolder = ctx.getIndexFolder();
                    if (indexFolder == null) {
                        throw new IllegalStateException(
                            String.format(
                            "No index folder for context: %s",      //NOI18N
                            ctx));
                    }
                    final DocumentIndex index = SPIAccessor.getInstance().getIndexFactory(ctx).getIndex(indexFolder);
                    if (index != null) {
                        usedIterables.offer(ci.getIndexablesFor(null));
                    }
                }
            }
        }

        protected final boolean index(
                final List<Indexable> resources, // out-of-date (new/modified) files
                final List<Indexable> allResources, // all files
                final URL root,
                final boolean sourceForBinaryRoot,
                final SourceIndexers indexers,
                final Map<SourceIndexerFactory, Boolean> votes,
                @NonNull final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> contexts,
                @NonNull final UsedIndexables usedIterables
        ) throws IOException {
            return TaskCache.getDefault().refreshTransaction(new ExceptionAction<Boolean>() {
                public @Override Boolean run() throws IOException {
                    return doIndex(resources, allResources, root, sourceForBinaryRoot, indexers, votes, contexts, usedIterables);
                }
            });
        }

        private boolean doIndex(
                List<Indexable> resources, // out-of-date (new/modified) files
                List<Indexable> allResources, // all files
                final URL root,
                final boolean sourceForBinaryRoot,
                SourceIndexers indexers,
                Map<SourceIndexerFactory, Boolean> votes,
                @NonNull final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> contexts,
                @NonNull final UsedIndexables usedIterables
        ) throws IOException {

            final LinkedList<Iterable<Indexable>> allIndexblesSentToIndexers = new LinkedList<Iterable<Indexable>>();
            SourceAccessor.getINSTANCE().suppressListening(true, !checkEditor);
                try {
                    final FileObject cacheRoot = CacheFolder.getDataFolder(root);
                    final ClusteredIndexables ci = new ClusteredIndexables(resources);
                    ClusteredIndexables allCi = null;
                    boolean ae = false;
                    assert ae = true;
                    for(IndexerCache.IndexerInfo<CustomIndexerFactory> cifInfo : indexers.cifInfos) {
                        Set<String> rootMimeTypes = PathRegistry.getDefault().getMimeTypesFor(root);
                        if (rootMimeTypes != null && !cifInfo.isAllMimeTypesIndexer() && !Util.containsAny(rootMimeTypes, cifInfo.getMimeTypes())) {
                            // ignore roots that are not marked to be scanned by the cifInfo indexer
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(Level.FINE, "Not using {0} registered for {1} to scan root {2} marked for {3}", new Object [] {
                                    cifInfo.getIndexerFactory().getIndexerName() + "/" + cifInfo.getIndexerFactory().getIndexVersion(),
                                    printMimeTypes(cifInfo.getMimeTypes(), new StringBuilder()),
                                    root,
                                    PathRegistry.getDefault().getMimeTypesFor(root)
                                });
                            }
                            continue;
                        }

                        final CustomIndexerFactory factory = cifInfo.getIndexerFactory();
                        final Pair<String,Integer> key = Pair.of(factory.getIndexerName(),factory.getIndexVersion());
                        Pair<SourceIndexerFactory,Context> value = contexts.get(key);
                        if (value == null) {
                            final Context ctx = SPIAccessor.getInstance().createContext(
                                    cacheRoot,
                                    root,
                                    factory.getIndexerName(),
                                    factory.getIndexVersion(),
                                    null,
                                    followUpJob,
                                    checkEditor,
                                    sourceForBinaryRoot,
                                    getSuspendStatus(),
                                    getCancelRequest(),
                                    logCtx);
                            value = Pair.<SourceIndexerFactory,Context>of(factory,ctx);
                            contexts.put(key,value);
                        }
                        final boolean cifIsChanged = indexers.changedCifs != null && indexers.changedCifs.contains(cifInfo);
                        final boolean forceReindex = votes.get(factory) == Boolean.FALSE && allResources != null;
                        final boolean allFiles = cifIsChanged || forceReindex || (allResources != null && allResources.size() == resources.size());
                        if (forceReindex && resources.size() != allResources.size()) {
                            final LogContext lc = getLogContext();
                            if (lc != null) {
                                lc.reindexForced(root, factory.getIndexerName());
                            }
                        }
                        if (ae && forceReindex && LOGGER.isLoggable(Level.INFO) && resources.size() != allResources.size() && !cifInfo.getMimeTypes().isEmpty()) {
                            LOGGER.log(Level.INFO, "Refresh of custom indexer ({0}) for root: {1} forced by: {2}",    //NOI18N
                                    new Object[]{
                                        cifInfo.getMimeTypes(),
                                        root.toExternalForm(),
                                        factory
                                    });
                        }
                        SPIAccessor.getInstance().setAllFilesJob(value.second(), allFiles);
                        List<Iterable<Indexable>> indexerIndexablesList = new LinkedList<Iterable<Indexable>>();
                        ClusteredIndexables usedCi = null;
                        for(String mimeType : cifInfo.getMimeTypes()) {
                            if ((cifIsChanged || forceReindex) && allResources != null && resources.size() != allResources.size()) {
                                if (allCi == null) {
                                    allCi = new ClusteredIndexables(allResources);
                                }
                                indexerIndexablesList.add(allCi.getIndexablesFor(mimeType));
                                if (usedCi == null) {
                                    usedCi = allCi;
                                }
                            } else {
                                indexerIndexablesList.add(ci.getIndexablesFor(mimeType));
                                if (usedCi == null) {
                                    usedCi = ci;
                                }
                            }
                        }

                        Iterable<Indexable> indexables = new ProxyIterable<Indexable>(indexerIndexablesList);
                        allIndexblesSentToIndexers.addAll(indexerIndexablesList);

                        parkWhileSuspended();
                        if (getCancelRequest().isRaised()) {
                            return false;
                        }

                        final CustomIndexer indexer = factory.createIndexer();
                        long tm1 = -1, tm2 = -1;
                        logStartIndexer(factory.getIndexerName());
                        SPIAccessor.getInstance().putProperty(value.second(), ClusteredIndexables.INDEX, usedCi);
                        int estimate = estimateCustomIndexingTime(factory);
                        try {
                            tm1 = System.currentTimeMillis();
                            SamplerInvoker.start(getLogContext(), factory.getIndexerName(), estimate, root);
                            SPIAccessor.getInstance().index(indexer, indexables, value.second());
                        } catch (ThreadDeath td) {
                            throw td;
                        } catch (Throwable t) {
                            LOGGER.log(Level.WARNING, null, t);
                        } finally {
                            SamplerInvoker.stop();
                        }
                        tm2 = System.currentTimeMillis();
                        logIndexerTime(factory.getIndexerName(), (int)(tm2-tm1));
                        if (LOGGER.isLoggable(Level.FINE)) {
                            StringBuilder sb = printMimeTypes(cifInfo.getMimeTypes(), new StringBuilder());
                            LOGGER.fine("Indexing source root " + root + " using " + indexer
                                + "; mimeTypes=" + sb.toString()
                                + "; took " + (tm1 != -1 && tm2 != -1 ? (tm2 - tm1) + "ms" : "unknown time")); //NOI18N
                        }
                        InjectedTasksSupport.execute();
                    }

                    if (getCancelRequest().isRaised()) {
                        return false;
                    }
                    
                    // now process embedding indexers
                    boolean useAllCi = false;
                    if (allResources != null) {
                        boolean containsNewIndexers = false;
                        boolean forceReindex = false;
                        final Set<EmbeddingIndexerFactory> reindexVoters = new HashSet<EmbeddingIndexerFactory>();
                        final LogContext lc = getLogContext();
                        for(Collection<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> eifInfos : indexers.eifInfosMap.values()) {
                            for(IndexerCache.IndexerInfo<EmbeddingIndexerFactory> eifInfo : eifInfos) {
                                if (indexers.changedEifs != null && indexers.changedEifs.contains(eifInfo)) {
                                    if (lc != null) {
                                        lc.newIndexerSeen(eifInfo.getIndexerFactory().getIndexerName());
                                    }
                                    containsNewIndexers = true;
                                }
                                EmbeddingIndexerFactory eif = eifInfo.getIndexerFactory();
                                boolean indexerVote = votes.get(eif) == Boolean.FALSE;
                                if (indexerVote) {
                                    if (lc != null) {
                                        lc.reindexForced(root, eif.getIndexerName());
                                    }
                                    reindexVoters.add(eif);
                                }
                                forceReindex |= indexerVote;
                            }
                        }
                        if ((containsNewIndexers||forceReindex) && resources.size() != allResources.size()) {
                            if (allCi == null) {
                                allCi = new ClusteredIndexables(allResources);
                            }
                            useAllCi = true;                            
                            if (ae && !reindexVoters.isEmpty() && LOGGER.isLoggable(Level.INFO)) {
                                LOGGER.log(Level.INFO, "Refresh of embedded indexers for root: {0} forced by: {1}",    //NOI18N
                                        new Object[]{
                                            root.toExternalForm(),
                                            reindexVoters.toString()
                                        });
                            }
                        }
                        final boolean allFiles = containsNewIndexers || forceReindex || allResources.size() == resources.size();
                        if (allFiles) {
                            for(Collection<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> eifInfos : indexers.eifInfosMap.values()) {
                                for(IndexerCache.IndexerInfo<EmbeddingIndexerFactory> eifInfo : eifInfos) {
                                    final EmbeddingIndexerFactory factory = eifInfo.getIndexerFactory();
                                    final Pair<String,Integer> key = Pair.of(factory.getIndexerName(),factory.getIndexVersion());
                                    Pair<SourceIndexerFactory,Context> value = contexts.get(key);
                                    if (value == null) {
                                        final Context ctx = SPIAccessor.getInstance().createContext(
                                                cacheRoot,
                                                root,
                                                factory.getIndexerName(),
                                                factory.getIndexVersion(),
                                                null,
                                                followUpJob,
                                                checkEditor,
                                                sourceForBinaryRoot,
                                                getSuspendStatus(),
                                                getCancelRequest(),
                                                logCtx);
                                        value = Pair.<SourceIndexerFactory,Context>of(factory,ctx);
                                        contexts.put(key,value);
                                    }
                                    SPIAccessor.getInstance().setAllFilesJob(value.second(), allFiles);
                                }
                            }
                        }
                    }

                    for(String mimeType : Util.getAllMimeTypes()) {
                        parkWhileSuspended();
                        if (getCancelRequest().isRaised()) {
                            return false;
                        }

                        if (!Util.canBeParsed(mimeType)) {
                            continue;
                        }

                        final ClusteredIndexables usedCi = useAllCi ? allCi : ci;
                        final Iterable<Indexable> indexables = usedCi.getIndexablesFor(mimeType);
                        allIndexblesSentToIndexers.add(indexables);

                        long tm1 = System.currentTimeMillis();
                        boolean f = indexEmbedding(indexers.eifInfosMap, cacheRoot, root, indexables, usedCi, contexts, sourceForBinaryRoot);
                        long tm2 = System.currentTimeMillis();

                        if (!f) {
                            return false;
                        }
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.fine("Indexing " + mimeType + " embeddables under " + root
                                + "; took " + (tm2 - tm1) + "ms"); //NOI18N
                        }
                    }
                    return !getCancelRequest().isRaised();
                } finally {
                    SourceAccessor.getINSTANCE().suppressListening(false, false);
                    usedIterables.offerAll(allIndexblesSentToIndexers);
                }
        }

        protected void invalidateSources (final Iterable<? extends Indexable> toInvalidate) {
            final long st = System.currentTimeMillis();
            for (Indexable indexable : toInvalidate) {
                final FileObject cheapFo = SPIAccessor.getInstance().getFileObject(indexable);
                if (cheapFo != null) {
                    final Source src = SourceAccessor.getINSTANCE().get(cheapFo);
                    if (src != null) {
                        SourceAccessor.getINSTANCE().setFlags(src, EnumSet.of(SourceFlags.INVALID));    //Only invalidate not reschedule                        
                    }
                }
            }
            final long et = System.currentTimeMillis();
            LOGGER.fine("InvalidateSources took: " + (et-st));  //NOI18N
        }

        protected final void binaryScanStarted(
                @NonNull final URL root,
                final boolean upToDate,
                @NonNull final LinkedHashMap<BinaryIndexerFactory, Context> contexts,
                @NonNull final BitSet startedIndexers) throws IOException {
            int index = 0;
            for(Map.Entry<BinaryIndexerFactory,Context> e : contexts.entrySet()) {
                final Context ctx = e.getValue();
                final BinaryIndexerFactory bif = e.getKey();
                SPIAccessor.getInstance().setAllFilesJob(ctx, !upToDate);
                parkWhileSuspended();
                long st = System.currentTimeMillis();
                logStartIndexer(bif.getIndexerName());
                boolean vote;
                try {
                    startedIndexers.set(index);
                    vote = bif.scanStarted(ctx);
                } catch (Throwable t) {
                    if (t instanceof ThreadDeath) {
                        throw (ThreadDeath) t;
                    } else {
                        vote = false;
                        Exceptions.printStackTrace(t);
                    }
                }
                long et = System.currentTimeMillis();
                logIndexerTime(bif.getIndexerName(), (int)(et-st));
                if (!vote) {
                    SPIAccessor.getInstance().setAllFilesJob(ctx, true);
                }
                index++;
            }
        }

        protected final void binaryScanFinished(
                @NonNull final BinaryIndexers indexers,
                @NonNull final LinkedHashMap<BinaryIndexerFactory, Context> contexts,
                @NonNull final BitSet startedIndexers,
                final boolean finished) throws IOException {
            try {
                int index = 0;
                for (Map.Entry<BinaryIndexerFactory, Context> entry : contexts.entrySet()) {
                    if (startedIndexers.get(index++)) {
                        try {
                            final long st = System.currentTimeMillis();
                            try {
                                parkWhileSuspended();
                                logStartIndexer(entry.getKey().getIndexerName());
                            } finally {
                                try {
                                    entry.getKey().scanFinished(entry.getValue());
                                } finally {
                                    long et = System.currentTimeMillis();
                                    logIndexerTime(entry.getKey().getIndexerName(), (int)(et-st));
                                }
                            }
                        } catch (Throwable t) {
                            if (t instanceof ThreadDeath) {
                                throw (ThreadDeath) t;
                            } else {
                                Exceptions.printStackTrace(t);
                            }
                        }
                    }
                }
            } finally {
                boolean indexOk = true;
                Union2<IOException,RuntimeException> exception = null;
                for(Context ctx : contexts.values()) {
                    try {
                        indexOk &= storeChanges(null, ctx, isSteady(), null, finished);
                    } catch (IOException e) {
                        exception = Union2.createFirst(e);
                    } catch (RuntimeException e) {
                        exception = Union2.createSecond(e);
                    }
                }
                if (exception != null) {
                    //Do not reschedule scan, the excepion comes from clear()
                    //rescheduling scan will cause infinite scan.
                    if (exception.hasFirst()) {
                        throw exception.first();
                    } else {
                        throw exception.second();
                    }
                } else if (!indexOk) {
                    RepositoryUpdater.getDefault().addBinaryJob(
                        contexts.values().iterator().next().getRootURI(),
                        LogContext.create(
                            LogContext.EventType.UI,
                            "Broken Index Found."));    //NOI18N);
                }
            }
        }

        protected final void createBinaryContexts(
                @NonNull final URL root,
                @NonNull final BinaryIndexers indexers,
                @NonNull final Map<BinaryIndexerFactory, Context> contexts) throws IOException {
            final FileObject cacheRoot = CacheFolder.getDataFolder(root);
            for(BinaryIndexerFactory bif : indexers.bifs) {
                final Context ctx = SPIAccessor.getInstance().createContext(
                    cacheRoot,
                    root,
                    bif.getIndexerName(),
                    bif.getIndexVersion(),
                    null,
                    false,
                    false,
                    false,
                    getSuspendStatus(),
                    getCancelRequest(),
                    null);
                contexts.put(bif, ctx);
            }
        }

        protected final  boolean checkBinaryIndexers(
                @NullAllowed Pair<Long,Map<Pair<String,Integer>,Integer>> lastState,
                @NonNull Map<BinaryIndexerFactory, Context> contexts) throws IOException {
            if (lastState == null || lastState.first() == 0L) {
                //Nothing known about the last state
                return false;
            }
            if (contexts.size() != lastState.second().size()) {
                //Factories changed
                return false;
            }
            final Map<Pair<String,Integer>,Integer> copy = new HashMap<Pair<String,Integer>,Integer>(lastState.second());
            for (Map.Entry<BinaryIndexerFactory,Context> e : contexts.entrySet()) {
                final BinaryIndexerFactory bif = e.getKey();
                final Integer state = copy.remove(Pair.<String,Integer>of(bif.getIndexerName(),bif.getIndexVersion()));
                if (state == null) {
                    //Factories changed
                    return false;
                }
                ArchiveTimeStamps.setIndexerState(e.getValue(),state);
            }
            return copy.isEmpty();
        }

        protected final Pair<Long,Map<Pair<String,Integer>,Integer>> createBinaryIndexersTimeStamp (
                final long currentTimeStamp,
                @NonNull final Map<BinaryIndexerFactory, Context> contexts) {
            final Map<Pair<String,Integer>,Integer> pairs = new HashMap<Pair<String, Integer>,Integer>();
            for (Map.Entry<BinaryIndexerFactory,Context> e : contexts.entrySet()) {
                final BinaryIndexerFactory bf = e.getKey();
                final Context ctx = e.getValue();
                pairs.put(
                        Pair.<String,Integer>of(bf.getIndexerName(),bf.getIndexVersion()),
                        ArchiveTimeStamps.getIndexerState(ctx));
            }
            return Pair.<Long,Map<Pair<String,Integer>,Integer>>of(currentTimeStamp,pairs);
        }

        protected final boolean indexBinary(
                final URL root,
                final BinaryIndexers indexers,
                final Map<BinaryIndexerFactory,Context> contexts) throws IOException {
            LOGGER.log(Level.FINE, "Scanning binary root: {0}", root); //NOI18N

            if (!RepositoryUpdater.getDefault().rootsListeners.add(root, false, null)) {
                //Exiting
                return false;
            }

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.fine("Using BinaryIndexerFactories: " + indexers.bifs); //NOI18N
            }

            for(BinaryIndexerFactory f : indexers.bifs) {
                parkWhileSuspended();
                if (getCancelRequest().isRaised()) {
                    break;
                }
                final Context ctx = contexts.get(f);
                assert ctx != null;

                final BinaryIndexer indexer = f.createIndexer();
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Indexing binary " + root + " using " + indexer); //NOI18N
                }
                long st = System.currentTimeMillis();
                logStartIndexer(f.getIndexerName());
                try {
                    SPIAccessor.getInstance().index(indexer, ctx);
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t) {
                    LOGGER.log(Level.WARNING, null, t);
                }
                long et = System.currentTimeMillis();
                logIndexerTime(f.getIndexerName(), (int)(et-st));
            }
            return !getCancelRequest().isRaised();
        }

        protected final boolean indexEmbedding(
                final Map<String, Collection<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>>> eifInfosMap,
                final FileObject cache,
                final URL rootURL,
                Iterable<? extends Indexable> files,
                final ClusteredIndexables usedCi,
                final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> transactionContexts,
                final boolean sourceForBinaryRoot
        ) throws IOException {
            for (final Indexable dirty : files) {
                parkWhileSuspended();
                if (getCancelRequest().isRaised()) {
                    return false;
                }

                Collection<? extends IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> infos = eifInfosMap.get(dirty.getMimeType());
                if (infos != null && infos.size() > 0) {
                    final URL url = dirty.getURL();
                    if (url == null) {
                        continue;
                    }

                    final FileObject fileObject = URLMapper.findFileObject(url);
                    if (fileObject == null) {
                        continue;
                    }

                    Source src = Source.create(fileObject);
                    try {
                        // log parsing for the mimetype:
                        logStartIndexer(src.getMimeType());
                        ParserManager.parse(Collections.singleton(src), new UserTask() {
                            @Override
                            public void run(ResultIterator resultIterator) throws Exception {
                                final String mimeType = resultIterator.getSnapshot().getMimeType();
                                final Collection<? extends IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> infos = eifInfosMap.get(mimeType);

                                if (infos != null && infos.size() > 0) {
                                    boolean finished = false;
                                    for (IndexerCache.IndexerInfo<EmbeddingIndexerFactory> info : infos) {
                                        if (getCancelRequest().isRaised()) {
                                            logFinishIndexer(mimeType);
                                            return;
                                        }

                                        EmbeddingIndexerFactory indexerFactory = info.getIndexerFactory();
                                        if (LOGGER.isLoggable(Level.FINE)) {
                                            LOGGER.fine("Indexing file " + fileObject.getPath() + " using " + indexerFactory + "; mimeType='" + mimeType + "'"); //NOI18N
                                        }

                                        final Parser.Result pr;
                                        try {
                                             pr = resultIterator.getParserResult();
                                            // must follow getParserResult(), as resultIterators are lazy
                                        } finally {
                                            if (!finished) {
                                                logFinishIndexer(mimeType);
                                                finished = true;
                                            }
                                        }
                                        if (pr != null) {
                                            final String indexerName = indexerFactory.getIndexerName();
                                            final int indexerVersion = indexerFactory.getIndexVersion();
                                            final Pair<String,Integer> key = Pair.of(indexerName,indexerVersion);
                                            Pair<SourceIndexerFactory,Context> value = transactionContexts.get(key);
                                            if (value == null) {
                                                final Context context = SPIAccessor.getInstance().createContext(
                                                        cache,
                                                        rootURL,
                                                        indexerName,
                                                        indexerVersion,
                                                        null,
                                                        followUpJob,
                                                        checkEditor,
                                                        sourceForBinaryRoot,
                                                        getSuspendStatus(),
                                                        getCancelRequest(),
                                                        logCtx);
                                                value = Pair.<SourceIndexerFactory,Context>of(indexerFactory,context);
                                                transactionContexts.put(key,value);
                                            }

                                            final EmbeddingIndexer indexer = indexerFactory.createIndexer(dirty, pr.getSnapshot());
                                            if (indexer != null) {
                                                SPIAccessor.getInstance().putProperty(value.second(), ClusteredIndexables.INDEX, usedCi);
                                                long st = System.currentTimeMillis();
                                                logStartIndexer(indexerName);
                                                int estimate = estimateEmbeddingIndexer(indexerFactory);
                                                SamplerInvoker.start(getLogContext(), indexerFactory.getIndexerName(), estimate, url);
                                                try {
                                                    SPIAccessor.getInstance().index(indexer, dirty, pr, value.second());
                                                } catch (ThreadDeath td) {
                                                    throw td;
                                                } catch (Throwable t) {
                                                    LOGGER.log(Level.WARNING, null, t);
                                                } finally {
                                                    SamplerInvoker.stop();
                                                }
                                                long et = System.currentTimeMillis();
                                                logIndexerTime(indexerName, (int)(et-st));
                                            }
                                        }
                                    }
                                } else {
                                    logFinishIndexer(mimeType);
                                }

                                for (Embedding embedding : resultIterator.getEmbeddings()) {
                                    if (getCancelRequest().isRaised()) {
                                        return;
                                    }
                                    logStartIndexer(embedding.getMimeType());
                                    run(resultIterator.getResultIterator(embedding));
                                }
                            }
                        });
                    } catch (final ParseException e) {
                        logFinishIndexer(src.getMimeType());
                        LOGGER.log(Level.WARNING, null, e);
                    }
                }
                InjectedTasksSupport.execute();
            }

            return !getCancelRequest().isRaised();
        }

        protected final boolean scanFiles(URL root, Collection<FileObject> files, boolean forceRefresh, boolean sourceForBinaryRoot) {
            final FileObject rootFo = URLMapper.findFileObject(root);
            if (rootFo != null) {
                LogContext lctx = getLogContext();
                try {
                    final ClassPath.Entry entry = sourceForBinaryRoot ? null : getClassPathEntry(rootFo);
                    final Set<Crawler.TimeStampAction> checkTimeStamps = EnumSet.noneOf(Crawler.TimeStampAction.class);
                    final boolean permanentUpdate = !TransientUpdateSupport.isTransientUpdate();
                    assert permanentUpdate || (forceRefresh && !files.isEmpty());
                    if (!forceRefresh) {
                        checkTimeStamps.add(Crawler.TimeStampAction.CHECK);
                    }
                    if (permanentUpdate) {
                        checkTimeStamps.add(Crawler.TimeStampAction.UPDATE);
                    }
                    final Crawler crawler = files.isEmpty() ?
                        new FileObjectCrawler(rootFo, checkTimeStamps, entry, getCancelRequest(), getSuspendStatus()) : // rescan the whole root (no timestamp check)
                        new FileObjectCrawler(rootFo, files.toArray(new FileObject[files.size()]), checkTimeStamps, entry, getCancelRequest(), getSuspendStatus()); // rescan selected files (no timestamp check)
                    if (lctx != null) {
                        lctx.noteRootScanning(root, true);
                    }
                    long t = System.currentTimeMillis();
                    final List<Indexable> resources = crawler.getResources();
                    if (crawler.isFinished()) {
                        
                        logCrawlerTime(crawler, t);
                        final Map<SourceIndexerFactory,Boolean> invalidatedMap = new IdentityHashMap<SourceIndexerFactory, Boolean>();
                        final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> ctxToFinish = new HashMap<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>>();
                        final UsedIndexables usedIterables = new UsedIndexables();
                        final SourceIndexers indexers = SourceIndexers.load(false);
                        invalidateSources(resources);
                        boolean indexResult=false;
                        try {
                            scanStarted (root, sourceForBinaryRoot, indexers, invalidatedMap, ctxToFinish);
                            delete(crawler.getDeletedResources(), ctxToFinish, usedIterables);
                            indexResult=index(resources, crawler.getAllResources(), root, sourceForBinaryRoot, indexers, invalidatedMap, ctxToFinish, usedIterables);
                            if (indexResult) {
                                crawler.storeTimestamps();
                                return true;
                            }
                        } finally {                           
                            scanFinished(ctxToFinish.values(), usedIterables, indexResult);
                        }
                    }

                    return false;
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, null, ioe);
                } finally {
                    if (lctx != null) {
                        lctx.finishScannedRoot(root);
                    }
                }
            }
            return true;
        }

        /**
         * @return <code>true</code> if finished or <code>false</code> if the task
         *   was cancelled and has to be rescheduled again.
         */
        protected abstract boolean getDone();

        protected boolean isCancelledBy(Work newWork, Collection<? super Work> follow) {
            return false;
        }

        public boolean absorb(Work newWork) {
            return false;
        }


        protected final boolean isCancelledExternally() {
            return externalCancel.get();
        }

        protected final CancelRequest getCancelRequest() {
            return cancelRequest;
        }

        @NonNull
        protected final SuspendStatus getSuspendStatus() {
            return suspendStatus;
        }

        protected final void parkWhileSuspended() {
            try {
                this.suspendStatus.parkWhileSuspended();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        protected final void logCrawlerTime(Crawler crawler, long start) throws IOException {
            final LogContext lctx = getLogContext();
            if (lctx == null) {
                return;
            }
            Collection c = crawler.getResources();
            Collection ac = crawler.getAllResources();
            
            lctx.addCrawlerTime(System.currentTimeMillis() - start, 
                    c == null ? -1 : c.size(), 
                    ac == null ? -1 : ac.size());
        }
        
        protected final void logStartIndexer(String iName) {
            final LogContext lc = getLogContext();
            if (lc != null) {
                lc.startIndexer(iName);
            }
        }
        
        protected final void logFinishIndexer(String iName) {
            final LogContext lc = getLogContext();
            if (lc != null) {
                lc.finishIndexer(iName);
            }
        }

        protected final void logIndexerTime(
                final @NonNull String indexerName,
                final int time) {
            final LogContext lc = getLogContext();
            if (lc != null) {
                // a relic, but since time is also counted in indexerStatistic, 
                // it's less code than to finish indexer separately
                lc.addIndexerTime(indexerName, time);
            }
            if (!reportIndexerStatistics) {
                return;
            }
            int[] itime = indexerStatistics.get(indexerName);
            if ( itime == null) {
                itime = new int[] {0,0};
                indexerStatistics.put(indexerName, itime);
            }
            itime[0]++;
            itime[1]+=time;
        }

        public final void doTheWork() {
            try {
                long startTime = -1L;
                if (UI_LOGGER.isLoggable(Level.INFO) ||
                    PERF_LOGGER.isLoggable(Level.FINE)) {
                    reportIndexingStart(UI_LOGGER, Level.INFO, lastScanEnded);
                    if (logCtx != null) {
                        logCtx.recordExecuted();
                    }
                    startTime = System.currentTimeMillis();
                    reportIndexerStatistics = true;
                }
                try {
                    finished.compareAndSet(false, getDone());
                } finally {

                    SamplerInvoker.release();
                    if (reportIndexerStatistics) {
                        lastScanEnded = System.currentTimeMillis();
                        final Object[] stats = createIndexerStatLogData(
                                lastScanEnded - startTime,
                                indexerStatistics);
                        reportIndexerStatistics(UI_LOGGER, Level.INFO, stats);
                        reportIndexerStatistics(PERF_LOGGER, Level.FINE, stats);
                    }
                    if (logCtx != null) {
                        logCtx.recordFinished();
                    }
                }
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, null, t);

                // prevent running the faulty work again
                finished.set(true);

                if (t instanceof ThreadDeath) {
                    throw (ThreadDeath)t;
                }
            } finally {
                latch.countDown();
            }
        }
        
        public final void waitUntilDone() {
            while (latch.getCount() != 0) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    LOGGER.log(Level.FINE, null, e);
                }
            }
        }

        public final void setCancelled(boolean cancelled) {
            this.cancelled.set(cancelled);
            this.externalCancel.set(cancelled);
        }

        public final boolean cancelBy(Work newWork, final Collection<? super Work> follow) {
            if (isCancelledBy(newWork, follow)) {
                LOGGER.log(Level.FINE, "{0} cancelled by {1}", new Object [] { this, newWork }); //NOI18N
                cancelled.set(true);
                finished.set(true); // work cancelled by other work is by default finished
                return true;
            }
            return false;
        }

        public final boolean isFinished() {
            return finished.get();
        }

        public final String getProgressTitle() {
            return progressTitle;
        }

        public final void setProgressHandle(ProgressHandle progressHandle) {
            synchronized (progressLock) {
                this.progressHandle = progressHandle;
            }
        }

        private String urlForMessage(URL currentlyScannedRoot) {
            final File file = FileUtil.archiveOrDirForURL(currentlyScannedRoot);            
            final String msg = file != null?
                file.getAbsolutePath():
                currentlyScannedRoot.toExternalForm();
            return msg;
        }

        public @Override String toString() {
            return getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this)) //NOI18N
                + "[followUpJob=" + followUpJob + ", checkEditor=" + checkEditor; //NOI18N
        }
        
        protected final Source getActiveSource() {
            final Source utActiveSrc = unitTestActiveSource;
            if (utActiveSrc != null) {
                return utActiveSrc;
            }
            final JTextComponent jtc = EditorRegistry.lastFocusedComponent();
            if (jtc == null) {
                return null;
            }
            Document doc = jtc.getDocument();
            assert doc != null;
            return DocumentUtilities.getMimeType(doc) == null ? null : Source.create(doc);
        }

        protected void refreshActiveDocument() {            
            final Source source = getActiveSource();
            if (source != null) {
                LOGGER.fine ("Invalidating source: " + source + " due to RootsWork");   //NOI18N
                final EventSupport support = SourceAccessor.getINSTANCE().getEventSupport(source);
                assert support != null;
                support.resetState(true, false, -1, -1, false);
            }
        }

        private static void reportIndexerStatistics(
                final @NonNull Logger logger,
                final @NonNull Level level,
                final @NonNull Object[] data) {
            if (logger.isLoggable(level)) {
                final LogRecord r = new LogRecord(level, "INDEXING_FINISHED"); //NOI18N
                r.setParameters(data);
                r.setResourceBundle(NbBundle.getBundle(RepositoryUpdater.class));
                r.setResourceBundleName(RepositoryUpdater.class.getPackage().getName() + ".Bundle"); //NOI18N
                r.setLoggerName(logger.getName());
                logger.log(r);
            }
        }

        private static void reportIndexingStart(
                @NonNull final Logger logger,
                @NonNull final Level level,
                final long lastScanEnded) {
            if (logger.isLoggable(level)) {
                final LogRecord r = new LogRecord(level, "INDEXING_STARTED"); //NOI18N
                r.setParameters(new Object [] {lastScanEnded == -1 ? 0 : System.currentTimeMillis()-lastScanEnded});
                r.setResourceBundle(NbBundle.getBundle(RepositoryUpdater.class));
                r.setResourceBundleName(RepositoryUpdater.class.getPackage().getName() + ".Bundle"); //NOI18N
                r.setLoggerName(logger.getName());
                logger.log(r);
            }
        }

        private static Object[] createIndexerStatLogData(
                final long indexingTime,
                final Map<String,int[]> stats) {
            final Object[] result = new Object[3*stats.size()+1];
            result[0] = indexingTime;
            final Iterator<Map.Entry<String,int[]>> it = stats.entrySet().iterator();
            for (int i=1; it.hasNext(); i+=3) {
                final Map.Entry<String,int[]> e = it.next();
                result[i] = e.getKey();
                final int[] countTimePair = e.getValue();
                result[i+1] = countTimePair[0];
                result[i+2] = countTimePair[1];
            }
            return result;
        }

        protected final boolean storeChanges(
                @NullAllowed final String indexerName,
                @NonNull final Context ctx,
                final boolean optimize,
                @NullAllowed final Iterable<? extends Indexable> indexables,
                final boolean finished) throws IOException {
            try {
                final FileObject indexFolder = ctx.getIndexFolder();
                if (indexFolder == null) {
                    throw new IllegalStateException(
                        String.format(
                            "No index folder for context: %s",      //NOI18N
                            ctx));
                }
                final DocumentIndex.Transactional index = SPIAccessor.getInstance().getIndexFactory(ctx).getIndex(indexFolder);
                if (index != null) {
                    TEST_LOGGER.log(
                        Level.FINEST,
                        "indexCommit:{0}:{1}",      //NOI18N
                        new Object[] {
                            indexerName,
                            ctx.getRootURI()
                        });
                    try {
                        if (finished) {
                            storeChanges(index, optimize, indexables);
                        } else {
                            rollBackChanges(index);
                        }
                    } catch (IOException ioe ) {
                        //Broken index, reschedule idexing.
                        LOGGER.log(
                            Level.WARNING,
                            "Broken index for root: {0} reason: {1}, recovering.",  //NOI18N
                            new Object[] {
                                ctx.getRootURI(),
                                ioe.getMessage()
                            });
                        index.clear();
                        return false;
                    }
                }
                return true;
            } finally {
                final DocumentIndexCache cache = SPIAccessor.getInstance().getIndexFactory(ctx).getCache(ctx);
                if (cache instanceof ClusteredIndexables.AttachableDocumentIndexCache) {
                    ((ClusteredIndexables.AttachableDocumentIndexCache)cache).detach();
                }
            }
        }

       private void storeChanges(
            @NonNull final DocumentIndex docIndex,
            final boolean optimize,
            @NullAllowed final Iterable<? extends Indexable> indexables) throws IOException {
            parkWhileSuspended();
            long t = System.currentTimeMillis();
            if (indexables != null) {
                final List<String> keysToRemove = new ArrayList<String>();
                for (Indexable indexable : indexables) {
                    keysToRemove.add(indexable.getRelativePath());
                }
                docIndex.removeDirtyKeys(keysToRemove);
            }
            docIndex.store(optimize);
            long span = System.currentTimeMillis() - t;
            LogContext lc = getLogContext();
            if (lc != null) {
                lc.addStoreTime(span);
            }
        }

       private void rollBackChanges(
               @NonNull final DocumentIndex.Transactional docIndex) throws IOException {
           docIndex.rollback();
       }

        //@NotThreadSafe
        final class UsedIndexables {
            
            private final Collection<Iterable<? extends Indexable>> usedIndexables = new ArrayDeque<Iterable<? extends Indexable>>();
            private  Iterable<? extends Indexable> cache;


            UsedIndexables() {
            }
            
            void offer(@NonNull final Iterable<? extends Indexable> indexables) {
                usedIndexables.add(indexables);
                cache = null;
            }
            
            void offerAll(@NonNull final Collection<? extends Iterable<? extends Indexable>> indexables) {
                usedIndexables.addAll(indexables);
                cache = null;
            }
            
            Iterable<? extends Indexable> get() {
                if (usedIndexables.isEmpty()) {
                    return null;
                }
                if (cache == null) {
                    cache = new ProxyIterable<Indexable>(usedIndexables, false, true);
                }
                return cache;
            }
        }

        private static final class CancelRequestImpl implements CancelRequest {

            private final AtomicBoolean cancelled;
            private Boolean successStatus;

            CancelRequestImpl(@NonNull final AtomicBoolean cancelled) {
                Parameters.notNull("cancelled", cancelled); //NOI18N
                this.cancelled = cancelled;
            }

            void setResult(@NullAllowed final Boolean result) {
                successStatus = result;
            }

            @Override
            public boolean isRaised() {
                return successStatus != null ?
                    !successStatus :
                    cancelled.get();
            }
        }

    } // End of Work class

    /* test */ static final class FileListWork extends Work {

        private final URL root;
        private final Collection<FileObject> files = new HashSet<FileObject>();
        private final boolean forceRefresh;
        private final boolean sourceForBinaryRoot;
        private final Map<URL, List<URL>> scannedRoots2Depencencies;

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public FileListWork (
                Map<URL, List<URL>> scannedRoots2Depencencies,
                URL root, boolean followUpJob,
                boolean checkEditor,
                boolean forceRefresh,
                boolean sourceForBinaryRoot,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext logCtx) {
            super(followUpJob, checkEditor, true, true, suspendStatus, logCtx);

            assert root != null;
            this.root = root;
            this.forceRefresh = forceRefresh;
            this.sourceForBinaryRoot = sourceForBinaryRoot;
            this.scannedRoots2Depencencies = scannedRoots2Depencencies;
        }

        @SuppressWarnings("LeakingThisInConstructor")
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public FileListWork (
                Map<URL, List<URL>> scannedRoots2Depencencies,
                URL root,
                Collection<FileObject> files,
                boolean followUpJob,
                boolean checkEditor,
                boolean forceRefresh,
                boolean sourceForBinaryRoot,
                final boolean steady,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext logCtx) {
            super(followUpJob, checkEditor, followUpJob, steady, suspendStatus, logCtx);

            assert root != null;
            assert files != null && files.size() > 0;
            this.root = root;
            this.files.addAll(files);
            this.forceRefresh = forceRefresh;
            this.sourceForBinaryRoot = sourceForBinaryRoot;
            this.scannedRoots2Depencencies = scannedRoots2Depencencies;
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("FileListWork@" + Integer.toHexString(System.identityHashCode(this)) + ": root=" + root + ", file=" + files); //NOI18N
            }
        }

        public void addFile(FileObject f) {
            assert f != null;
            assert FileUtil.isParentOf(URLMapper.findFileObject(root), f) : "File " + f + " does not belong under the root: " + root; //NOI18N
            files.add(f);
        }

        protected @Override boolean getDone() {
            updateProgress(root, false);
            if (scanFiles(root, files, forceRefresh, sourceForBinaryRoot)) {
                // if we are refreshing a specific set of files, try to update
                // their document versions
                if (!files.isEmpty()) {
                    Map<FileObject, Document> f2d = getEditorFiles();
                    for(FileObject f : files) {
                        Document d = f2d.get(f);
                        if (d != null) {
                            long version = DocumentUtilities.getDocumentVersion(d);
                            d.putProperty(PROP_LAST_INDEXED_VERSION, version);
                            d.putProperty(PROP_LAST_DIRTY_VERSION, null);
                        }
                    }
                }

                //If the root is unknown add it into scannedRoots2Depencencies to allow listening on changes under this root
                if (!scannedRoots2Depencencies.containsKey(root)) {
                    scannedRoots2Depencencies.put(root, UNKNOWN_ROOT);
                }
            }
            TEST_LOGGER.log(Level.FINEST, "filelist"); //NOI18N
            refreshActiveDocument();
            return true;
        }

        @Override
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_BLOCKING_METHODS_ON_URL",
        justification="URLs have never host part")
        public boolean absorb(Work newWork) {
            if (newWork instanceof FileListWork) {
                FileListWork nflw = (FileListWork) newWork;
                if (nflw.root.equals(root)
                    && nflw.isFollowUpJob() == isFollowUpJob()
                    && nflw.hasToCheckEditor() == hasToCheckEditor()
                ) {
                    files.addAll(nflw.files);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(this + ", root=" + root + " absorbed: " + nflw.files); //NOI18N
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void refreshActiveDocument() {
            if (shouldRefresh()) {
                super.refreshActiveDocument();
            }
        }

        @Override
        protected void invalidateSources(Iterable<? extends Indexable> toInvalidate) {
            if (shouldRefresh()) {
                super.invalidateSources(toInvalidate);
            }
        }
        
        private boolean shouldRefresh() {
            return !TransientUpdateSupport.isTransientUpdate();
        }
        
    } // End of FileListWork class


    private static final class BinaryWork extends AbstractRootsWork {

        private final URL root;

        public BinaryWork(
            URL root,
            @NonNull final SuspendStatus suspendStatus,
            @NullAllowed final LogContext logCtx) {
            super(false, suspendStatus, logCtx);
            this.root = root;
        }

        protected @Override boolean getDone() {
            boolean result = scanBinary(root, BinaryIndexers.load(), null);
            TEST_LOGGER.log(Level.FINEST, "binary", Collections.<URL>singleton(root));       //NOI18N
            refreshActiveDocument();
            return result;
        }

        @Override
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_BLOCKING_METHODS_ON_URL",
        justification="URLs have never host part")
        public boolean absorb(Work newWork) {
            if (newWork instanceof BinaryWork) {
                return root.equals(((BinaryWork) newWork).root);
            } else {
                return false;
            }
        }

    } // End of BinaryWork class

    private static final class DeleteWork extends Work {

        private final URL root;
        private final Set<String> relativePaths = new HashSet<String>();

        @SuppressWarnings("LeakingThisInConstructor")
        public DeleteWork (
                URL root,
                Set<String> relativePaths,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext logCtx) {
            super(false, false, false, true, suspendStatus, logCtx);
            
            Parameters.notNull("root", root); //NOI18N
            Parameters.notNull("relativePath", relativePaths); //NOI18N
            
            this.root = root;
            this.relativePaths.addAll(relativePaths);
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("DeleteWork@" + Integer.toHexString(System.identityHashCode(this)) + ": root=" + root + ", files=" + relativePaths); //NOI18N
            }
        }

        public @Override boolean getDone() {
//            updateProgress(root);
            LogContext lctx = getLogContext();
            if (lctx != null) {
                lctx.noteRootScanning(root, false);
            }
            try {
                final List<Indexable> indexables = new ArrayList<Indexable>();
                for(String path : relativePaths) {
                    indexables.add(SPIAccessor.getInstance().create(new DeletedIndexable (root, path)));
                }
                final Map<SourceIndexerFactory,Boolean> votes = new HashMap<SourceIndexerFactory, Boolean>();
                final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> contexts = new HashMap<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>>();
                final UsedIndexables usedIterables = new UsedIndexables();
                final SourceIndexers indexers = SourceIndexers.load(false);
                final TimeStamps ts = TimeStamps.forRoot(root, false);
                try {
                    scanStarted(root, false, indexers, votes, contexts);
                    delete(indexables, contexts, usedIterables);
                    ts.remove(relativePaths);
                } finally {
                    final boolean finished = !getCancelRequest().isRaised();
                    if (finished) {
                        ts.store();
                        scanFinished(contexts.values(), usedIterables, finished);
                    }
                }
                TEST_LOGGER.log(Level.FINEST, "delete"); //NOI18N
            } catch (IOException ioe) {
                LOGGER.log(Level.WARNING, null, ioe);
            } finally {
                if (lctx != null) {
                    lctx.finishScannedRoot(root);
                }
            }
            return true;
        }

        @Override
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_BLOCKING_METHODS_ON_URL",
        justification="URLs have never host part")
        public boolean absorb(Work newWork) {
            if (newWork instanceof DeleteWork) {
                DeleteWork ndw = (DeleteWork) newWork;
                if (ndw.root.equals(root)) {
                    relativePaths.addAll(ndw.relativePaths);
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(this + ", root=" + root + " absorbed: " + ndw.relativePaths); //NOI18N
                    }
                    return true;
                }
            }
            return false;
        }

    } // End of DeleteWork class

    private static class RefreshCifIndices extends Work {

        private final Collection<? extends IndexerCache.IndexerInfo<CustomIndexerFactory>> cifInfos;
        private final Map<URL, List<URL>> scannedRoots2Dependencies;
        private final Set<URL> sourcesForBinaryRoots;

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public RefreshCifIndices(
                Collection<? extends IndexerCache.IndexerInfo<CustomIndexerFactory>> cifInfos,
                Map<URL, List<URL>> scannedRoots2Depencencies,
                Set<URL> sourcesForBinaryRoots,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext logCtx) {
            super(false, false, NbBundle.getMessage(RepositoryUpdater.class, "MSG_RefreshingIndices"),true, suspendStatus, logCtx); //NOI18N
            this.cifInfos = cifInfos;
            this.scannedRoots2Dependencies = scannedRoots2Depencencies;
            this.sourcesForBinaryRoots = sourcesForBinaryRoots;
        }

        @Override
        public  boolean absorb(Work newWork) {
            if (newWork instanceof RefreshCifIndices && cifInfos.equals(((RefreshCifIndices)newWork).cifInfos)) {
                LOGGER.log(Level.FINE, "Absorbing {0}", newWork); //NOI18N
                return true;
            } else {
                return false;
            }
        }

        @Override
        protected boolean isCancelledBy(final Work newWork, final Collection<? super Work> follow) {
            boolean b = (newWork instanceof RootsWork);
            if (b) {
                follow.add(new RefreshCifIndices(cifInfos, scannedRoots2Dependencies, sourcesForBinaryRoots, getSuspendStatus(), LogContext.createAndAbsorb(getLogContext())));
                LOGGER.log(Level.FINE, "Cancelling {0}, because of {1}", new Object[]{this, newWork}); //NOI18N
            }
            return b;
        }

        @Override
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        protected boolean getDone() {
            switchProgressToDeterminate(scannedRoots2Dependencies.size());
            for(URL root : scannedRoots2Dependencies.keySet()) {
                if (getCancelRequest().isRaised()) {
                    return false;
                }                
                LogContext lctx = getLogContext();
                
                if (lctx != null) {
                    lctx.noteRootScanning(root, false);
                }
                this.updateProgress(root, true);
                try {
                    final FileObject rootFo = URLMapper.findFileObject(root);
                    if (rootFo != null) {
                        long time = System.currentTimeMillis();
                        
                        boolean sourceForBinaryRoot = sourcesForBinaryRoots.contains(root);
                        final ClassPath.Entry entry = sourceForBinaryRoot ? null : getClassPathEntry(rootFo);
                        Crawler crawler = new FileObjectCrawler(rootFo, EnumSet.of(Crawler.TimeStampAction.UPDATE), entry, getCancelRequest(), getSuspendStatus());
                        final List<Indexable> resources = crawler.getResources();
                        final List<Indexable> deleted = crawler.getDeletedResources();

                        logCrawlerTime(crawler, time);
                        if (crawler.isFinished()) {
                            final FileObject cacheRoot = CacheFolder.getDataFolder(root);
                            final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> transactionContexts = new HashMap<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>>();
                            final UsedIndexables usedIterables = new UsedIndexables();
                            final Map<SourceIndexerFactory,Boolean> votes = new HashMap<SourceIndexerFactory, Boolean>();
                            try {
                                customIndexersScanStarted(root, cacheRoot, sourceForBinaryRoot, cifInfos, votes, transactionContexts);
                                if (deleted.size() > 0) {
                                    delete(deleted, transactionContexts, usedIterables);
                                }
                                final LinkedList<Iterable<Indexable>> allIndexblesSentToIndexers = new LinkedList<Iterable<Indexable>>();
                                try {
                                    ClusteredIndexables ci = new ClusteredIndexables(resources);
                                    for(IndexerCache.IndexerInfo<CustomIndexerFactory> cifInfo : cifInfos) {
                                        List<Iterable<Indexable>> indexerIndexablesList = new LinkedList<Iterable<Indexable>>();
                                        for(String mimeType : cifInfo.getMimeTypes()) {
                                            indexerIndexablesList.add(ci.getIndexablesFor(mimeType));
                                        }
                                        ProxyIterable<Indexable> indexables = new ProxyIterable<Indexable>(indexerIndexablesList);
                                        allIndexblesSentToIndexers.addAll(indexerIndexablesList);

                                        parkWhileSuspended();
                                        if (getCancelRequest().isRaised()) {
                                            return false;
                                        }

                                        final CustomIndexerFactory factory = cifInfo.getIndexerFactory();
                                        final Pair<String,Integer> indexerKey = Pair.<String,Integer>of(factory.getIndexerName(),factory.getIndexVersion());
                                        final Pair<SourceIndexerFactory, Context> ctx = transactionContexts.get(indexerKey);
                                        if (ctx != null) {
                                            SPIAccessor.getInstance().setAllFilesJob(ctx.second(), true);
                                            final CustomIndexer indexer = factory.createIndexer();
                                            if (LOGGER.isLoggable(Level.FINE)) {
                                                StringBuilder sb = printMimeTypes(cifInfo.getMimeTypes(), new StringBuilder());
                                                LOGGER.log(
                                                    Level.FINE,
                                                    "Reindexing {0} using {1}; mimeTypes={2}",  //NOI18N
                                                    new Object[]{
                                                        root,
                                                        indexer,
                                                        sb
                                                    });
                                            }
                                            SPIAccessor.getInstance().putProperty(ctx.second(), ClusteredIndexables.INDEX, ci);
                                            long st = System.currentTimeMillis();
                                            logStartIndexer(factory.getIndexerName());
                                            try {
                                                SPIAccessor.getInstance().index(indexer, indexables, ctx.second());
                                            } catch (ThreadDeath td) {
                                                throw td;
                                            } catch (Throwable t) {
                                                LOGGER.log(Level.WARNING, null, t);
                                            }
                                            long et = System.currentTimeMillis();
                                            logIndexerTime(factory.getIndexerName(), (int)(et-st));
                                        } else {
                                            LOGGER.log(
                                                Level.WARNING, "RefreshCifIndices ignored recently added factory: {0}", //NOI18N
                                                indexerKey);
                                        }
                                        InjectedTasksSupport.execute();
                                    }
                                } finally {
                                    usedIterables.offerAll(allIndexblesSentToIndexers);
                                }
                            } finally {
                                final boolean commit = !getCancelRequest().isRaised();
                                scanFinished(transactionContexts.values(), usedIterables, commit);
                                if (commit) {
                                    crawler.storeTimestamps();
                                }
                            }
                        }
                    }
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, null, ioe);
                } finally {
                    if (lctx != null) {
                        lctx.finishScannedRoot(root);
                    }
                }
            }
            
            return true;
        }

        public @Override String toString() {
            StringBuilder sb = new StringBuilder();
            for(Iterator<? extends IndexerCache.IndexerInfo<CustomIndexerFactory>> it = cifInfos.iterator(); it.hasNext(); ) {
                IndexerCache.IndexerInfo<CustomIndexerFactory> cifInfo = it.next();
                sb.append(" indexer=").append(cifInfo.getIndexerName()).append('/').append(cifInfo.getIndexerVersion()); //NOI18N
                sb.append(" ("); //NOI18N
                printMimeTypes(cifInfo.getMimeTypes(), sb);
                sb.append(')'); //NOI18N
                if (it.hasNext()) {
                    sb.append(','); //NOI18N
                }
            }
            return super.toString() + sb.toString();
        }
    } // End of RefreshCifIndices class

    private static class RefreshEifIndices extends Work {

        private final Collection<? extends IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> eifInfos;
        private final Map<URL, List<URL>> scannedRoots2Dependencies;
        private final Set<URL> sourcesForBinaryRoots;

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public RefreshEifIndices(
                Collection<? extends IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> eifInfos,
                Map<URL, List<URL>> scannedRoots2Depencencies,
                Set<URL> sourcesForBinaryRoots,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext logCtx) {
            super(false, false, NbBundle.getMessage(RepositoryUpdater.class, "MSG_RefreshingIndices"),true, suspendStatus, logCtx); //NOI18N
            if (eifInfos == null) {
                throw new IllegalArgumentException("eifInfos must not be null");
            }
            this.eifInfos = eifInfos;
            this.scannedRoots2Dependencies = scannedRoots2Depencencies;
            this.sourcesForBinaryRoots = sourcesForBinaryRoots;
        }

        @Override
        protected boolean isCancelledBy(final Work newWork, final Collection<? super Work> follow) {
            boolean b = (newWork instanceof RootsWork);
            if (b) {
                follow.add(new RefreshEifIndices(eifInfos, scannedRoots2Dependencies, sourcesForBinaryRoots, getSuspendStatus(), LogContext.createAndAbsorb(getLogContext())));
                LOGGER.log(Level.FINE, "Cancelling {0}, because of {1}", new Object[]{this, newWork}); //NOI18N
            }
            if (newWork instanceof RefreshEifIndices) {
                boolean b2 = ((RefreshEifIndices)newWork).eifInfos.containsAll(eifInfos);
                if (b2) {
                    LOGGER.log(Level.FINE, "Cancelling {0}, because of {1}", new Object[]{this, newWork}); //NOI18N
                }
                b |= b2;
            }
            return b;
        }

        @Override
        public  boolean absorb(Work newWork) {
            if (newWork instanceof RefreshEifIndices && eifInfos.containsAll(((RefreshEifIndices)newWork).eifInfos)) {
                LOGGER.log(Level.FINE, "Absorbing {0}", newWork); //NOI18N
                return true;
            } else {
                return false;
            }
        }

        @Override
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        protected boolean getDone() {
            switchProgressToDeterminate(scannedRoots2Dependencies.size());
            for(URL root : scannedRoots2Dependencies.keySet()) {
                if (getCancelRequest().isRaised()) {
                    return false;
                }                
                LogContext lctx = getLogContext();
                if (lctx != null) {
                    lctx.noteRootScanning(root, false);
                }
                this.updateProgress(root, true);
                try {
                    final FileObject rootFo = URLMapper.findFileObject(root);
                    if (rootFo != null) {
                        long t = System.currentTimeMillis();
                        
                        boolean sourceForBinaryRoot = sourcesForBinaryRoots.contains(root);
                        final ClassPath.Entry entry = sourceForBinaryRoot ? null : getClassPathEntry(rootFo);
                        Crawler crawler = new FileObjectCrawler(rootFo, EnumSet.of(Crawler.TimeStampAction.UPDATE), entry, getCancelRequest(), getSuspendStatus());
                        final List<Indexable> resources = crawler.getResources();
                        final List<Indexable> deleted = crawler.getDeletedResources();

                        logCrawlerTime(crawler, t);
                        if (crawler.isFinished()) {
                            final FileObject cacheRoot = CacheFolder.getDataFolder(root);
                            final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> transactionContexts = new HashMap<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>>();
                            final UsedIndexables usedIterables = new UsedIndexables();
                            final Map<SourceIndexerFactory,Boolean> votes = new HashMap<SourceIndexerFactory, Boolean>();
                            final Map<String, Collection<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>>> eifInfosMap = new HashMap<String, Collection<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>>>();
                            for(IndexerCache.IndexerInfo<EmbeddingIndexerFactory> eifInfo : eifInfos) {
                                for (String mimeType : eifInfo.getMimeTypes()) {
                                    Collection<IndexerInfo<EmbeddingIndexerFactory>> infos = eifInfosMap.get(mimeType);
                                    if (infos == null) {
                                        infos = new HashSet<IndexerInfo<EmbeddingIndexerFactory>>();
                                        eifInfosMap.put(mimeType, infos);
                                    }
                                    infos.add(eifInfo);
                                }
                            }
                            SourceAccessor.getINSTANCE().suppressListening(true, !hasToCheckEditor());
                            try {
                                embeddingIndexersScanStarted(root, cacheRoot, sourceForBinaryRoot, eifInfosMap.values(), votes, transactionContexts);
                                if (deleted.size() > 0) {
                                    delete(deleted, transactionContexts, usedIterables);
                                }
                                final LinkedList<Iterable<Indexable>> allIndexblesSentToIndexers = new LinkedList<Iterable<Indexable>>();
                                try {
                                    ClusteredIndexables ci = new ClusteredIndexables(resources);
                                    for(String mimeType : Util.getAllMimeTypes()) {
                                        if (getCancelRequest().isRaised()) {
                                            return false;
                                        }

                                        if (!Util.canBeParsed(mimeType)) {
                                            continue;
                                        }

                                        Iterable<Indexable> indexables = ci.getIndexablesFor(mimeType);
                                        allIndexblesSentToIndexers.add(indexables);

                                        long tm1 = System.currentTimeMillis();
                                        boolean f = indexEmbedding(eifInfosMap, cacheRoot, root, indexables, ci, transactionContexts, sourceForBinaryRoot);
                                        long tm2 = System.currentTimeMillis();
                                        if (!f) {
                                            return false;
                                        }
                                        if (LOGGER.isLoggable(Level.FINE)) {
                                            LOGGER.fine("Indexing " + mimeType + " embeddables under " + root
                                                + "; took " + (tm2 - tm1) + "ms"); //NOI18N
                                        }
                                    }
                                } finally {
                                    usedIterables.offerAll(allIndexblesSentToIndexers);
                                }
                            } finally {
                                try  {
                                    final boolean commit = !getCancelRequest().isRaised();
                                    scanFinished(transactionContexts.values(),usedIterables,  commit);
                                    if (commit) {
                                        crawler.storeTimestamps();
                                    }
                                } finally {
                                    SourceAccessor.getINSTANCE().suppressListening(false, false);
                                }
                            }                            
                        }
                    }
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, null, ioe);
                } finally {
                    if (lctx != null) {
                        lctx.finishScannedRoot(root);
                    }
                }
            }

            return true;
        }

        public @Override String toString() {
            StringBuilder sb = new StringBuilder();
            for(Iterator<? extends IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> it = eifInfos.iterator(); it.hasNext(); ) {
                IndexerCache.IndexerInfo<EmbeddingIndexerFactory> eifInfo = it.next();
                sb.append(" indexer=").append(eifInfo.getIndexerName()).append('/').append(eifInfo.getIndexerVersion()); //NOI18N
                sb.append(" ("); //NOI18N
                printMimeTypes(eifInfo.getMimeTypes(), sb);
                sb.append(')'); //NOI18N
                if (it.hasNext()) {
                    sb.append(','); //NOI18N
                }
            }
            return super.toString() + sb.toString();
        }
    } // End of RefreshEifIndices class

    /* test */ static final class RefreshWork extends AbstractRootsWork {

        private final Map<URL, List<URL>> scannedRoots2Dependencies;
        private final Map<URL, List<URL>> scannedBinaries2InvDependencies;
        private final Map<URL, List<URL>> scannedRoots2Peers;
        private final Set<URL> sourcesForBinaryRoots;
        private final Set<Pair<Object, Boolean>> suspectFilesOrFileObjects;
        private final FSRefreshInterceptor interceptor;

        private DependenciesContext depCtx;
        private Map<URL, Set<FileObject>> fullRescanFiles;
        private Map<URL, Set<FileObject>> checkTimestampFiles;

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public RefreshWork(
                Map<URL, List<URL>> scannedRoots2Depencencies,
                Map<URL, List<URL>> scannedBinaries2InvDependencies,
                Map<URL, List<URL>> scannedRoots2Peers,
                Set<URL> sourcesForBinaryRoots,
                boolean fullRescan,
                boolean logStatistics,
                Collection<? extends Object> suspectFilesOrFileObjects,
                FSRefreshInterceptor interceptor,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext logCtx)
        {
            super(logStatistics, suspendStatus, logCtx);

            Parameters.notNull("scannedRoots2Depencencies", scannedRoots2Depencencies); //NOI18N
            Parameters.notNull("scannedBinaries2InvDependencies", scannedBinaries2InvDependencies); //NOI18N
            Parameters.notNull("scannedRoots2Peers", scannedRoots2Peers);
            Parameters.notNull("sourcesForBinaryRoots", sourcesForBinaryRoots); //NOI18N
            Parameters.notNull("interceptor", interceptor); //NOI18N

            this.scannedRoots2Dependencies = scannedRoots2Depencencies;
            this.scannedBinaries2InvDependencies = scannedBinaries2InvDependencies;
            this.scannedRoots2Peers = scannedRoots2Peers;
            this.sourcesForBinaryRoots = sourcesForBinaryRoots;
            this.suspectFilesOrFileObjects = new HashSet<Pair<Object, Boolean>>();
            if (suspectFilesOrFileObjects != null) {
                addSuspects(suspectFilesOrFileObjects, fullRescan);
            }
            this.interceptor = interceptor;
        }

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        protected @Override boolean getDone() {
            if (depCtx == null) {
                depCtx = new DependenciesContext(
                        scannedRoots2Dependencies,
                        scannedBinaries2InvDependencies,
                        scannedRoots2Peers,
                        sourcesForBinaryRoots,
                        false,
                        false);

                if (suspectFilesOrFileObjects.isEmpty()) {
                    depCtx.newBinariesToScan.addAll(scannedBinaries2InvDependencies.keySet());
                    try {
                        depCtx.newRootsToScan.addAll(org.openide.util.Utilities.topologicalSort(scannedRoots2Dependencies.keySet(), scannedRoots2Dependencies));
                    } catch (final TopologicalSortException tse) {
                        LOGGER.log(Level.INFO, "Cycles detected in classpath roots dependencies, using partial ordering", tse); //NOI18N
                        @SuppressWarnings("unchecked") List<URL> partialSort = tse.partialSort(); //NOI18N
                        depCtx.newRootsToScan.addAll(partialSort);
                    }
                    Collections.reverse(depCtx.newRootsToScan);
                } else {
                    Set<Pair<FileObject, Boolean>> suspects = new HashSet<Pair<FileObject, Boolean>>();

                    for(Pair<Object, Boolean> fileOrFileObject : suspectFilesOrFileObjects) {
                        Pair<FileObject, Boolean> fileObject = null;

                        if (fileOrFileObject.first() instanceof File) {
                            FileObject f;
                            try {
                                f = FileUtil.toFileObject((File) fileOrFileObject.first());
                            } catch (IllegalArgumentException e) {
                                throw new IllegalArgumentException(
                                    "Non-normalized file among files to rescan.",   //NOI18N
                                    e) {
                                    {
                                        final LogContext logCtx = getLogContext();
                                        if (logCtx != null) {
                                            setStackTrace(logCtx.getCaller());
                                        }
                                    }
                                };
                            }
                            if (f != null) {
                                fileObject = Pair.<FileObject, Boolean>of(f, fileOrFileObject.second());
                            }
                        } else if (fileOrFileObject.first() instanceof FileObject) {
                            fileObject = Pair.<FileObject, Boolean>of((FileObject) fileOrFileObject.first(), fileOrFileObject.second());
                        } else {
                            LOGGER.fine("Not File or FileObject, ignoring: " + fileOrFileObject); //NOI18N
                        }

                        if (fileObject != null) {
                            suspects.add(fileObject);
                        }
                    }

                    { // <editor-fold defaultstate="collapsed" desc="process binary roots">
                        for(Pair<FileObject, Boolean> f : suspects) {
                            for(URL root : scannedBinaries2InvDependencies.keySet()) {
                                // check roots owned by suspects
                                File rootFile = FileUtil.archiveOrDirForURL(root);
                                if (rootFile != null) {
                                    FileObject rootFo = FileUtil.toFileObject(rootFile);
                                    if (rootFo != null) {
                                        if (f.first() == rootFo || FileUtil.isParentOf(f.first(), rootFo)) {
                                            depCtx.newBinariesToScan.add(root);
                                            break;
                                        }
                                    }
                                }

                                // check roots that own a suspect
                                FileObject rootFo = URLCache.getInstance().findFileObject(root, true);
                                if (rootFo != null) {
                                    if (f.first() == rootFo || FileUtil.isParentOf(rootFo, f.first())) {
                                        depCtx.newBinariesToScan.add(root);
                                        break;
                                    }
                                }
                            }
                        }
                    // </editor-fold>
                    }

                    { // <editor-fold defaultstate="collapsed" desc="process source roots">
                        Set<Pair<FileObject, Boolean>> containers = new HashSet<Pair<FileObject, Boolean>>();
                        Map<URL, Pair<FileObject, Boolean>> sourceRootsToScan = new HashMap<URL, Pair<FileObject, Boolean>>();
                        for(URL root : scannedRoots2Dependencies.keySet()) {
                            FileObject rootFo = URLCache.getInstance().findFileObject(root, true);
                            if (rootFo != null) {
                                for(Pair<FileObject, Boolean> f : suspects) {
                                    if (f.first() == rootFo || FileUtil.isParentOf(f.first(), rootFo)) {
                                        Pair<FileObject, Boolean> pair = sourceRootsToScan.get(root);
                                        if (pair == null) {
                                            pair = Pair.<FileObject, Boolean>of(rootFo, f.second());
                                        } else {
                                            pair = Pair.<FileObject, Boolean>of(rootFo, pair.second() || f.second());
                                        }
                                        sourceRootsToScan.put(root, pair);
                                        containers.add(f);
                                    }
                                }
                            }
                        }

                        suspects.removeAll(containers);
                        for(Map.Entry<URL, Pair<FileObject, Boolean>> entry : sourceRootsToScan.entrySet()) {
                            for(Iterator<Pair<FileObject, Boolean>> it = suspects.iterator(); it.hasNext(); ) {
                                Pair<FileObject, Boolean> f = it.next();
                                Pair<FileObject, Boolean> root = entry.getValue();
                                if (FileUtil.isParentOf(root.first(), f.first()) && (root.second() || !f.second())) { // second means fullRescan
                                    it.remove();
                                }
                            }
                        }

                        for(Map.Entry<URL, Pair<FileObject, Boolean>> entry : sourceRootsToScan.entrySet()) {
                            depCtx.newRootsToScan.add(entry.getKey());
                            if (entry.getValue().second()) {
                                depCtx.fullRescanSourceRoots.add(entry.getKey());
                            }
                        }
                    // </editor-fold>
                    }

                    { // <editor-fold defaultstate="collapsed" desc="process single files and folder">
                        fullRescanFiles = new HashMap<URL, Set<FileObject>>();
                        checkTimestampFiles = new HashMap<URL, Set<FileObject>>();
                        for(Pair<FileObject, Boolean> f : suspects) {
                            for(URL root : scannedRoots2Dependencies.keySet()) {
                                FileObject rootFo = URLCache.getInstance().findFileObject(root, true);
                                if (rootFo != null && (f.first() == rootFo || FileUtil.isParentOf(rootFo, f.first()))) {
                                    Map<URL, Set<FileObject>> map = f.second() ? fullRescanFiles : checkTimestampFiles;
                                    Set<FileObject> files = map.get(root);
                                    if (files == null) {
                                        files = new HashSet<FileObject>();
                                        map.put(root, files);
                                    }
                                    files.add(f.first());
                                    break;
                                }
                            }
                        }
                    // </editor-fold>
                    }
                }

                // refresh filesystems
                FileSystem.AtomicAction aa = new FileSystem.AtomicAction() {
                    public @Override void run() throws IOException {
                        FileUtil.refreshFor(File.listRoots());
                    }
                };
// XXX: nested FS.AA don't seem to work, so just ignore evrything
//      interceptor.setActiveAtomicAction(aa);
//      Probably not needed, the aa calls refreshFor which behaves correctly
//      regarding AtomicAction unlike FU.refreshAll
                interceptor.setIgnoreFsEvents(true);
                try {
                    FileUtil.runAtomicAction(aa);
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, null, ex);
                } finally {
//                    interceptor.setActiveAtomicAction(null);
                    interceptor.setIgnoreFsEvents(false);
                }
            } else {
                depCtx.newRootsToScan.removeAll(depCtx.scannedRoots);
                depCtx.scannedRoots.clear();
                depCtx.newBinariesToScan.removeAll(depCtx.scannedBinaries);
                depCtx.scannedBinaries.clear();
            }

            boolean finished = scanBinaries(depCtx);
            if (finished) {
                finished = scanSources(depCtx, null,null);
                if (finished) {
                    finished = scanRootFiles(fullRescanFiles);
                    if (finished) {
                        finished = scanRootFiles(checkTimestampFiles);
                    }
                }
            }

            final Level logLevel = Level.FINE;
            if (LOGGER.isLoggable(logLevel)) {
                LOGGER.log(logLevel, this + " " + (getCancelRequest().isRaised() ? "cancelled" : "finished") + ": {"); //NOI18N
                LOGGER.log(logLevel, "  scannedRoots2Dependencies(" + scannedRoots2Dependencies.size() + ")="); //NOI18N
                printMap(scannedRoots2Dependencies, logLevel);
                LOGGER.log(logLevel, "  scannedBinaries(" + scannedBinaries2InvDependencies.size() + ")="); //NOI18N
                printCollection(scannedBinaries2InvDependencies.keySet(), logLevel);
                LOGGER.log(logLevel, "} ===="); //NOI18N
            }

            refreshActiveDocument();
            return finished;
        }

        public @Override boolean absorb(Work newWork) {
            if (newWork instanceof RefreshWork) {
                suspectFilesOrFileObjects.addAll(((RefreshWork) newWork).suspectFilesOrFileObjects);
                return true;
            } else if (newWork instanceof FileListWork) {
                FileListWork flw = (FileListWork) newWork;
                if (flw.files.isEmpty()) {
                    suspectFilesOrFileObjects.add(Pair.<Object, Boolean>of(URLCache.getInstance().findFileObject(flw.root, false), flw.forceRefresh));
                } else {
                    addSuspects(flw.files, flw.forceRefresh);
                }
                return true;
            } else if (newWork instanceof DeleteWork) {
                suspectFilesOrFileObjects.add(Pair.<Object, Boolean>of(URLCache.getInstance().findFileObject(((DeleteWork) newWork).root, false), false));
                return true;
            }
            return false;
        }

        public void addSuspects(Collection<? extends Object> filesOrFolders, boolean fullRescan) {
            for(Object o : filesOrFolders) {
                suspectFilesOrFileObjects.add(Pair.<Object, Boolean>of(o, fullRescan));
            }
        }

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        private boolean scanRootFiles(Map<URL, Set<FileObject>> files) {
            if (files != null && files.size() > 0) { // #174887
                for(Iterator<Map.Entry<URL, Set<FileObject>>> it = files.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<URL, Set<FileObject>> entry = it.next();
                    URL root = entry.getKey();
                    if (scanFiles(root, entry.getValue(), true, sourcesForBinaryRoots.contains(root))) {
                        it.remove();
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }

        public @Override String toString() {
            return super.toString() + ", suspectFilesOrFileObjects=" + suspectFilesOrFileObjects; //NOI18N
        }
    } // End of RefreshWork class

    private static class RootsWork extends AbstractRootsWork {

        private final Map<URL, List<URL>> scannedRoots2Dependencies;
        private final Map<URL,List<URL>> scannedBinaries2InvDependencies;
        private final Map<URL,List<URL>> scannedRoots2Peers;
        private final Set<URL> sourcesForBinaryRoots;
        private final AtomicLong scannedRoots2DependenciesLamport;
        private boolean useInitialState;
        private boolean refreshNonExistentDeps;

        private DependenciesContext depCtx;
        protected SourceIndexers indexers = null; // is only ever filled by InitialRootsWork
        
        // flag that no projects are opened, and no real scanning work is expected
        private boolean shouldDoNothing;
        private Level   previousLevel;

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public RootsWork(
                Map<URL, List<URL>> scannedRoots2Depencencies,
                Map<URL,List<URL>> scannedBinaries2InvDependencies,
                Map<URL,List<URL>> scannedRoots2Peers,
                Set<URL> sourcesForBinaryRoots,
                boolean useInitialState,
                boolean refreshNonExistentDeps,
                @NonNull final AtomicLong scannedRoots2DependenciesLamport,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed LogContext logCtx) {
            super(false, suspendStatus, logCtx);
            this.scannedRoots2Dependencies = scannedRoots2Depencencies;
            this.scannedBinaries2InvDependencies = scannedBinaries2InvDependencies;
            this.scannedRoots2Peers = scannedRoots2Peers;
            this.sourcesForBinaryRoots = sourcesForBinaryRoots;
            this.useInitialState = useInitialState;
            this.refreshNonExistentDeps = refreshNonExistentDeps;
            this.scannedRoots2DependenciesLamport = scannedRoots2DependenciesLamport;
        }

        public @Override String toString() {
            return super.toString() + ", useInitialState=" + useInitialState; //NOI18N
        }
        
        private void dumpGlobalRegistry(String n, Collection<String> pathIds) {
            boolean printed = false;
            for (String pathId : pathIds) {
                GlobalPathRegistry gpr = GlobalPathRegistry.getDefault();
                Set<ClassPath> paths = gpr.getPaths(pathId);
                if (!paths.isEmpty() && !printed) {
                    LOGGER.log(Level.FINE, "Dumping: {0}", n);
                    printed = true;
                }
                LOGGER.log(Level.FINE, "Paths ID {0}: {1}", new Object[] { pathId, paths});
            }
        }
        
        private void checkRootCollection(Collection<? extends URL> roots) {
            if (!shouldDoNothing || roots.isEmpty() || getCancelRequest().isRaised()) {
                return;
            }
            int stubCount = 0;
            // ignore all files, which reside in IDE installation (in various clusters)
            // the ignored files must be located 1 subdirectory beneath the cluster 
            // directory
            for (URL u : roots) {
                String path = u.getPath();
                if (path == null || !path.endsWith("stubs.zip!/")) { // NOI18N
                    break;
                }
                FileObject f = URLMapper.findFileObject(u);
                if (f == null) {
                    break;
                }
                // !/ is a root inside archive, get the archive which encapsulates it -> normal FS
                FileObject archive = FileUtil.getArchiveFile(f);
                File archiveFile;
                // quick check - if the archive is not on OS FS, bail out.
                if (archive == null || 
                    ((archiveFile = FileUtil.toFile(archive)) == null)) {
                    break;
                }
                // 1 level up = dir-in-cluster. 2 levels up = cluster dir
                FileObject parent = archive.getParent();
                if (parent == null) {
                    break;
                }
                parent = parent.getParent();
                if (parent == null) {
                    break;
                }
                String clusterPath = FileUtil.getRelativePath(parent, archive);
                File file = InstalledFileLocator.getDefault().locate(clusterPath, null, false);
                if (file == null || !file.equals(archiveFile)) {
                    break;
                }
                stubCount++;
            }
            if (stubCount == roots.size()) {
                return;
            }
            if (previousLevel == null) {
                previousLevel = LOGGER.getLevel() == null ? Level.ALL : LOGGER.getLevel();
                Level toSet;

                try {
                    toSet = Level.parse(System.getProperty("RepositoryUpdate.increasedLogLevel", "FINE"));
                } catch (IllegalArgumentException ex) {
                    toSet = Level.FINE;
                }

                LOGGER.setLevel(toSet);
                LOGGER.warning("Non-empty roots encountered while no projects are opened; loglevel increased");
                
                Collection<? extends PathRecognizer> recogs = Lookup.getDefault().lookupAll(PathRecognizer.class);
                PathRecognizerRegistry reg = PathRecognizerRegistry.getDefault();
                
                dumpGlobalRegistry("Binary Libraries", reg.getBinaryLibraryIds());
                dumpGlobalRegistry("Libraries", reg.getLibraryIds());
                dumpGlobalRegistry("Sources", reg.getSourceIds());
            }
        }

        @Override
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public boolean getDone() {
            TEST_LOGGER.log(Level.FINEST, "RootsWork-started");       //NOI18N
            if (getCancelRequest().isRaised()) {
                return false;
            }

            Project[] openProjects = OpenProjects.getDefault().getOpenProjects();
            
            shouldDoNothing = openProjects.length == 0;
            
            try {
            updateProgress(NbBundle.getMessage(RepositoryUpdater.class, "MSG_ProjectDependencies")); //NOI18N
            long tm1 = System.currentTimeMillis();
            boolean restarted;
            if (depCtx == null) {
                restarted = false;
                depCtx = new DependenciesContext(
                    scannedRoots2Dependencies,
                    scannedBinaries2InvDependencies,
                    scannedRoots2Peers,
                    sourcesForBinaryRoots,
                    useInitialState,
                    refreshNonExistentDeps);
                final Collection<URL> newRoots = new HashSet<URL>();
                Collection<? extends URL> c = PathRegistry.getDefault().getSources();
                checkRootCollection(c);
                LOGGER.log(Level.FINE, "PathRegistry.sources="); printCollection(c, Level.FINE); //NOI18N
                newRoots.addAll(c);

                c = PathRegistry.getDefault().getLibraries();
                checkRootCollection(c);
                LOGGER.log(Level.FINE, "PathRegistry.libraries="); printCollection(c, Level.FINE); //NOI18N
                newRoots.addAll(c);

                checkRootCollection(PathRegistry.getDefault().getBinaryLibraries());
                depCtx.newBinariesToScan.addAll(PathRegistry.getDefault().getBinaryLibraries());                

                if (useInitialState) {
                    c = PathRegistry.getDefault().getUnknownRoots();                    
                    LOGGER.log(Level.FINE, "PathRegistry.unknown="); printCollection(c, Level.FINE); //NOI18N
                    depCtx.unknownRoots.addAll(c);
                    depCtx.preInversedDeps = Util.findTransitiveReverseDependencies(
                        depCtx.initialRoots2Deps,
                        depCtx.initialRoots2Peers);
                    newRoots.addAll(c);
                } // else computing the deps from scratch and so will find the 'unknown' roots
                // by following the dependencies (#166715)
                
                for (URL url : newRoots) {
                    if (!findDependencies(
                            url,
                            depCtx,
                            null,
                            null,
                            null,
                            getCancelRequest(),
                            getSuspendStatus())) {
                        // task cancelled due to IDE shutting down, we should not be called again
                        // throw away depCtx that has not yet been fully initialized
                        depCtx = null;
                        return false;
                    }
                }

                for (Iterator<URL> it = depCtx.newBinariesToScan.iterator(); it.hasNext(); ) {
                    if (depCtx.oldBinaries.remove(it.next())) {
                        it.remove();
                    }
                }

                Controller controller = (Controller)IndexingController.getDefault();
                synchronized (controller) {
                    Map<URL, List<URL>> nextRoots2Deps = new HashMap<URL, List<URL>>();
                    nextRoots2Deps.putAll(depCtx.initialRoots2Deps);
                    nextRoots2Deps.keySet().removeAll(depCtx.oldRoots);
                    nextRoots2Deps.putAll(depCtx.newRoots2Deps);
                    controller.roots2Dependencies = Collections.unmodifiableMap(nextRoots2Deps);
                    Map<URL, List<URL>> nextBinRoots2Deps = new HashMap<URL, List<URL>>();
                    nextBinRoots2Deps.putAll(depCtx.initialBinaries2InvDeps);
                    nextBinRoots2Deps.keySet().removeAll(depCtx.oldBinaries);
                    nextBinRoots2Deps.putAll(depCtx.newBinaries2InvDeps);
                    controller.binRoots2Dependencies = Collections.unmodifiableMap(nextBinRoots2Deps);
                    Map<URL, List<URL>> nextRoots2Peers = new HashMap<URL, List<URL>>();
                    nextRoots2Peers.putAll(depCtx.initialRoots2Peers);
                    nextRoots2Peers.keySet().removeAll(depCtx.oldRoots);
                    nextRoots2Peers.putAll(depCtx.newRoots2Peers);
                    controller.roots2Peers = Collections.unmodifiableMap(nextRoots2Peers);
                }

                try {
                    depCtx.newRootsToScan.addAll(org.openide.util.Utilities.topologicalSort(depCtx.newRoots2Deps.keySet(), depCtx.newRoots2Deps));
                } catch (final TopologicalSortException tse) {
                    LOGGER.log(Level.INFO, "Cycles detected in classpath roots dependencies, using partial ordering", tse); //NOI18N
                    @SuppressWarnings("unchecked") List<URL> partialSort = tse.partialSort(); //NOI18N
                    depCtx.newRootsToScan.addAll(partialSort);
                }
                Collections.reverse(depCtx.newRootsToScan);

                if (!useInitialState) {
                    // check for differencies from the initialState
                    final Map<URL,List<URL>> removed = new HashMap<URL,List<URL>>();
                    final Map<URL,List<URL>> addedOrChanged = new HashMap<URL,List<URL>>();
                    final Map<URL,List<URL>> removedPeers = new HashMap<URL,List<URL>>();
                    final Map<URL,List<URL>> addedOrChangedPeers = new HashMap<URL,List<URL>>();
                    diff(depCtx.initialRoots2Deps, depCtx.newRoots2Deps, addedOrChanged, removed);
                    diff(depCtx.initialRoots2Peers, depCtx.newRoots2Peers, addedOrChangedPeers, removedPeers);


                    final Level logLevel = Level.FINE;
                    if (LOGGER.isLoggable(logLevel) && (addedOrChanged.size() > 0 || removed.size() > 0)) {
                        LOGGER.log(logLevel, "Changes in dependencies detected:"); //NOI18N
                        LOGGER.log(logLevel, "initialRoots2Deps({0})=", depCtx.initialRoots2Deps.size()); //NOI18N
                        printMap(depCtx.initialRoots2Deps, logLevel);
                        LOGGER.log(logLevel, "newRoots2Deps({0})=", depCtx.newRoots2Deps.size()); //NOI18N
                        printMap(depCtx.newRoots2Deps, logLevel);
                        LOGGER.log(logLevel, "addedOrChanged({0})=", addedOrChanged.size()); //NOI18N
                        printMap(addedOrChanged, logLevel);
                        LOGGER.log(logLevel, "removed({0})=", removed.size()); //NOI18N
                        printMap(removed, logLevel);
                    }

                    depCtx.oldRoots.clear();
                    depCtx.oldRoots.addAll(removed.keySet());
                    final Set<URL> toScan = new HashSet<URL>(addedOrChanged.keySet());
                    toScan.addAll(addedOrChangedPeers.keySet());
                    depCtx.newRootsToScan.retainAll(toScan);
                }
            } else {
                restarted = true;
                depCtx.newRootsToScan.removeAll(depCtx.scannedRoots);
                depCtx.scannedRoots.clear();
                depCtx.newBinariesToScan.removeAll(depCtx.scannedBinaries);
                depCtx.scannedBinaries.clear();
                depCtx.oldBinaries.clear();
                depCtx.oldRoots.clear();
                
                if (shouldDoNothing) {
                    LOGGER.warning("restarted while no projects are opened. Roots = " + depCtx.newRootsToScan + " binaries = " + 
                            depCtx.newBinariesToScan);
                    
                }
            }
            
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, "Resolving dependencies took: {0} ms", System.currentTimeMillis() - tm1); //NOI18N
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Running " + this + " on \n" + depCtx.toString()); //NOI18N
                
                if (previousLevel != null && getCancelRequest().isRaised()) {
                    LOGGER.fine("Note: the Work was canceled during dependency-resolve, disregard preceding logs on non-empty paths");
                    LOGGER.setLevel(previousLevel == Level.ALL ? null : previousLevel);
                }
            }
            switchProgressToDeterminate(depCtx.newBinariesToScan.size() + depCtx.newRootsToScan.size());
            boolean finished = scanBinaries(depCtx);
            if (finished) {
                finished = scanSources(depCtx, indexers, scannedRoots2Dependencies);
            }


            if (!finished) {
                final Queue<URL> toUnregister = new ArrayDeque<URL>();
                for (URL translatedRoot : depCtx.newlySFBTranslated) {
                    if (depCtx.newRootsToScan.contains(translatedRoot) &&
                        !depCtx.scannedRoots.contains(translatedRoot)) {
                        toUnregister.offer(translatedRoot);
                    }
                }
                PathRegistry.getDefault().unregisterUnknownSourceRoots(toUnregister);
            }

            final List<URL> missingRoots = new LinkedList<URL>();
            scannedRoots2Dependencies.keySet().removeAll(depCtx.oldRoots);
            scannedRoots2Peers.keySet().removeAll(depCtx.oldRoots);            
            for(URL root : depCtx.scannedRoots) {
                List<URL> deps = depCtx.newRoots2Deps.get(root);
                if (deps == null) {
                    //binDeps not a part of newRoots2Deps, cycle in dependencies?
                    //rescue by EMPTY_DEPS and log
                    deps = UNKNOWN_ROOT;
                    missingRoots.add(root);
                }
                scannedRoots2Dependencies.put(root, deps);
                deps = depCtx.newRoots2Peers.get(root);
                scannedRoots2Peers.put(root,deps);
            }
            final Collection<URL> unknownToRemove = new HashSet<URL>();
            if (!depCtx.unknownRoots.isEmpty()) {
                final Map<URL,Collection<URL>> postInversedDeps =
                    Util.findTransitiveReverseDependencies(scannedRoots2Dependencies, scannedRoots2Peers);
                for (URL ur : depCtx.unknownRoots) {
                    final Collection<URL> postUrInversedDeps = postInversedDeps.get(ur);
                    boolean remove;
                    if (postUrInversedDeps == null) {
                        remove = true;
                    } else  {
                        postUrInversedDeps.removeAll(depCtx.unknownRoots);
                        remove = postUrInversedDeps.isEmpty();
                    }
                    if (remove) {
                        final Collection<URL> preUrInversedDeps = depCtx.preInversedDeps.get(ur);
                        if (preUrInversedDeps != null) {
                            preUrInversedDeps.removeAll(depCtx.unknownRoots);
                            if (!preUrInversedDeps.isEmpty()) {
                                unknownToRemove.add(ur);
                            }
                        }
                    }
                }
            }
            PathRegistry.getDefault().unregisterUnknownSourceRoots(unknownToRemove);
            scannedRoots2DependenciesLamport.incrementAndGet();
            if (!missingRoots.isEmpty()) {
                StringBuilder log = new StringBuilder("Missing dependencies for roots: ");  //NOI18N
                printCollection(missingRoots, log);
                log.append("Context:");    //NOI18N
                log.append(depCtx);
                log.append("Restarted: ");
                log.append(restarted);
                LOGGER.info(log.toString());
            }
            for(URL root : depCtx.scannedBinaries) {
                List<URL> deps = depCtx.newBinaries2InvDeps.get(root);
                if (deps == null) {
                    deps = UNKNOWN_ROOT;
                }
                scannedBinaries2InvDependencies.put(root, deps);
            }
            scannedBinaries2InvDependencies.keySet().removeAll(depCtx.oldBinaries);

            //Needs to be set to the to the scannedRoots2Dependencies.
            //When not finished the scannedRoots2Dependencies != controller.roots2Dependencies
            //as it was set to optimistic value (supposed that all is scanned).
            Controller controller = (Controller)IndexingController.getDefault();
            synchronized (controller) {
                controller.roots2Dependencies = Collections.unmodifiableMap(new HashMap<URL, List<URL>>(scannedRoots2Dependencies));
                controller.binRoots2Dependencies = Collections.unmodifiableMap(new HashMap<URL, List<URL>>(scannedBinaries2InvDependencies));
                controller.roots2Peers = Collections.unmodifiableMap(new HashMap<URL, List<URL>>(scannedRoots2Peers));
            }

            notifyRootsRemoved (depCtx.oldBinaries, depCtx.oldRoots, unknownToRemove);

            final Level logLevel = Level.FINE;
            if (LOGGER.isLoggable(logLevel)) {
                LOGGER.log(logLevel, this + " " + (getCancelRequest().isRaised() ? "cancelled" : "finished") + ": {"); //NOI18N
                LOGGER.log(logLevel, "  scannedRoots2Dependencies(" + scannedRoots2Dependencies.size() + ")="); //NOI18N
                printMap(scannedRoots2Dependencies, logLevel);
                LOGGER.log(logLevel, "  scannedBinaries(" + scannedBinaries2InvDependencies.size() + ")="); //NOI18N
                printCollection(scannedBinaries2InvDependencies.keySet(), logLevel);
                LOGGER.log(logLevel, "  scannedRoots2Peers(" + scannedRoots2Peers.size() + ")="); //NOI18N
                printMap(scannedRoots2Peers, logLevel);
                LOGGER.log(logLevel, "} ===="); //NOI18N
            }
            TEST_LOGGER.log(Level.FINEST, "RootsWork-finished");       //NOI18N
            refreshActiveDocument();
            return finished;
            } finally {
                if (previousLevel != null) {
                    LOGGER.setLevel(previousLevel == Level.ALL ? null : previousLevel);
                    previousLevel = null;
                }
            }
        }

        protected @Override boolean isCancelledBy(final Work newWork, final Collection<? super Work> follow) {
            boolean b = (newWork instanceof RootsWork) && useInitialState;
            if (b && LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Cancelling " + this + ", because of " + newWork); //NOI18N
            }
            return b;
        }

        public @Override boolean absorb(Work newWork) {
            if (newWork.getClass().equals(RootsWork.class)) {
                final RootsWork rw = (RootsWork) newWork;
                if (!rw.useInitialState) {
                    // the new work does not use initial state and so should not we
                    useInitialState = rw.useInitialState;
                    LOGGER.log(
                        Level.FINE,
                        "Absorbing {0}, updating useInitialState to {1}",   //NOI18N
                        new Object[]{
                            rw,
                            useInitialState
                        });
                }
                if (rw.refreshNonExistentDeps) {
                    refreshNonExistentDeps = rw.refreshNonExistentDeps;
                }
                return true;
            } else {
                return false;
            }
        }

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        private void notifyRootsRemoved (
                @NonNull final Collection<? extends URL> binaries,
                @NonNull final Collection<? extends URL> sources,
                @NonNull final Collection<? extends URL> unknown) {
            if (!binaries.isEmpty()) {
                final Collection<? extends BinaryIndexerFactory> binFactories = MimeLookup.getLookup(MimePath.EMPTY).lookupAll(BinaryIndexerFactory.class);
                final Iterable<? extends URL> roots = Collections.unmodifiableCollection(binaries);
                for (BinaryIndexerFactory binFactory : binFactories) {
                    binFactory.rootsRemoved(roots);
                }
                RepositoryUpdater.getDefault().rootsListeners.remove(binaries, false);
            }

            if (!sources.isEmpty() || !unknown.isEmpty()) {
                final Iterable<? extends URL> roots = new ProxyIterable<URL>(Arrays.asList(sources, unknown));
                final Collection<? extends IndexerCache.IndexerInfo<CustomIndexerFactory>> customIndexers = IndexerCache.getCifCache().getIndexers(null);
                for (IndexerCache.IndexerInfo<CustomIndexerFactory> customIndexer : customIndexers) {
                    customIndexer.getIndexerFactory().rootsRemoved(roots);
                }

                final Collection<? extends IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> embeddingIndexers = IndexerCache.getEifCache().getIndexers(null);
                for (IndexerCache.IndexerInfo<EmbeddingIndexerFactory> embeddingIndexer : embeddingIndexers) {
                    embeddingIndexer.getIndexerFactory().rootsRemoved(roots);
                }
                RepositoryUpdater.getDefault().rootsListeners.remove(sources, true);
            }
        }

        private static <A, B> void diff(Map<A, B> oldMap, Map<A, B> newMap, Map<A, B> addedOrChangedEntries, Map<A, B> removedEntries) {
            for(A key : oldMap.keySet()) {
                if (!newMap.containsKey(key)) {
                    removedEntries.put(key, oldMap.get(key));
                } else {
                    if (!org.openide.util.Utilities.compareObjects(oldMap.get(key), newMap.get(key))) {
                        addedOrChangedEntries.put(key, newMap.get(key));
                    }
                }
            }

            for(A key : newMap.keySet()) {
                if (!oldMap.containsKey(key)) {
                    addedOrChangedEntries.put(key, newMap.get(key));
                }
            }
        }

    } // End of RootsScanningWork class

    private static abstract class AbstractRootsWork extends Work {

        private boolean logStatistics;

        protected AbstractRootsWork(
                final boolean logStatistics,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext ctx) {
            super(false, false, true, true, suspendStatus, ctx);
            this.logStatistics = logStatistics;
        }

//        protected AbstractRootsWork(boolean followUpJob, boolean checkEditor, String progressTitle, boolean logStatistics) {
//            super(followUpJob, checkEditor, progressTitle, true);
//            this.logStatistics = logStatistics;
//        }

        protected final boolean scanBinaries(final DependenciesContext ctx) {
            assert ctx != null;
            final AtomicInteger scannedRootsCnt = new AtomicInteger(0);
            final BinaryIndexers binaryIndexers = ctx.newBinariesToScan.isEmpty()?
                    null : BinaryIndexers.load();

            final IndexBinaryWorkPool pool = new IndexBinaryWorkPool(
                    new IndexBinaryWorkPool.Function<URL, Boolean>() {
                        @Override
                        public Boolean apply(URL root) {
                            return scanBinary(
                                    root,
                                    binaryIndexers,
                                    scannedRootsCnt);
                        }
                    },
                    new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            return getCancelRequest().isRaised();
                        }
                    },
                    ctx.newBinariesToScan);
            final long binaryScanStart = System.currentTimeMillis();
            final Pair<Boolean,Collection<? extends URL>> res = pool.execute();
            final long binaryScanEnd = System.currentTimeMillis();
            ctx.scannedBinaries.addAll(res.second());
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(
                    Level.INFO,
                    "Complete indexing of {0} binary roots took: {1} ms",   //NOI18N
                    new Object[] {
                        scannedRootsCnt.get(),
                        binaryScanEnd - binaryScanStart
                    });
            }
            TEST_LOGGER.log(Level.FINEST, "scanBinary", ctx.newBinariesToScan);       //NOI18N
            return res.first();
        }

        protected final boolean scanBinary(URL root, BinaryIndexers binaryIndexers, AtomicInteger scannedRootsCnt) {
            boolean success = false;
            final long tmStart = System.currentTimeMillis();
            final LinkedHashMap<BinaryIndexerFactory, Context> contexts = new LinkedHashMap<BinaryIndexerFactory, Context>();
            final BitSet startedIndexers = new BitSet(contexts.size());
            LogContext lctx = getLogContext();
            if (lctx != null) {
                lctx.noteRootScanning(root, false);
            }
            try {
                createBinaryContexts(root, binaryIndexers, contexts);
                final FileObject rootFo = URLMapper.findFileObject(root);
                final FileObject file = rootFo == null ? null : FileUtil.getArchiveFile(rootFo);
                final boolean upToDate;
                final long currentLastModified;
                if (file != null) {
                    final Pair<Long,Map<Pair<String,Integer>,Integer>> lastState = ArchiveTimeStamps.getLastModified(file.getURL());
                    final boolean indexersUpToDate = checkBinaryIndexers(lastState, contexts);
                    currentLastModified = file.lastModified().getTime();
                    upToDate = indexersUpToDate && lastState.first() ==  currentLastModified;
                } else {
                    currentLastModified = -1L;
                    upToDate = false;
                }                
                try {
                    binaryScanStarted(root, upToDate, contexts, startedIndexers);
                    updateProgress(root, true);
                    success = indexBinary(root, binaryIndexers, contexts);
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, null, ioe);
                } finally {
                    binaryScanFinished(binaryIndexers, contexts, startedIndexers, success);
                    URL archiveFile = FileUtil.getArchiveFile(root);
                    if (success && archiveFile != null && !upToDate) {
                        ArchiveTimeStamps.setLastModified(archiveFile, createBinaryIndexersTimeStamp(currentLastModified,contexts));
                    }
                }
            } catch (IOException ioe) {
                LOGGER.log(Level.WARNING, null, ioe);
            } finally {
                if (lctx != null) {
                    lctx.finishScannedRoot(root);
                }
                final long time = System.currentTimeMillis() - tmStart;
                if (scannedRootsCnt != null) {
                    scannedRootsCnt.incrementAndGet();
                }
                reportRootScan(root, time);
                LOGGER.log(
                    Level.FINE,
                    "Indexing of: {0} took: {1} ms",    //NOI18N
                    new Object[] {
                        root,
                        time
                    });
            }
            return success;
        }

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        protected final boolean scanSources(DependenciesContext ctx, SourceIndexers indexers, Map<URL, List<URL>> preregisterIn) {
            assert ctx != null;
            long scannedRootsCnt = 0;
            long completeTime = 0;
            int totalOutOfDateFiles = 0;
            int totalDeletedFiles = 0;
            long totalRecursiveListenersTime = 0;
            boolean finished = true;

            if (indexers == null) {
                indexers = SourceIndexers.load(false);
            }

            for (URL source : ctx.newRootsToScan) {
                if (getCancelRequest().isRaised()) {
                    finished = false;
                    break;
                }

                final long tmStart = System.currentTimeMillis();
                final int [] outOfDateFiles = new int [] { 0 };
                final int [] deletedFiles = new int [] { 0 };
                final long [] recursiveListenersTime = new long [] { 0 };
                try {
                    updateProgress(source, true);
                    boolean preregistered = false;
                    boolean success = false;
                    if (preregisterIn != null && !preregisterIn.containsKey(source)) {
                        preregisterIn.put(source, UNKNOWN_ROOT);
                        preregistered = true;
                    }
                    LogContext lctx = getLogContext();
                    try {
                        if (lctx != null) {
                            lctx.noteRootScanning(source, false);
                        }
                        if (scanSource (source, ctx.fullRescanSourceRoots.contains(source), ctx.sourcesForBinaryRoots.contains(source), indexers, outOfDateFiles, deletedFiles, recursiveListenersTime)) {
                            ctx.scannedRoots.add(source);
                            success = true;
                        } else {
                            finished = false;
                            break;
                        }
                    } finally {
                        if (lctx != null) {
                            lctx.finishScannedRoot(source);
                        }
                        if (preregistered && !success) {
                            preregisterIn.remove(source);
                        }
                    }
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, null, ioe);
                } finally {
                    final long time = System.currentTimeMillis() - tmStart;
                    completeTime += time;
                    scannedRootsCnt++;
                    totalOutOfDateFiles += outOfDateFiles[0];
                    totalDeletedFiles += deletedFiles[0];
                    totalRecursiveListenersTime += recursiveListenersTime[0];
                    reportRootScan(source, time);
                    if (LOGGER.isLoggable(Level.INFO)) {
                        final File f = FileUtil.archiveOrDirForURL(source);
                        final Object shown = f != null ? f : source;
                        LOGGER.log(
                            Level.INFO,
                            "Indexing of: {0} took: {1} ms (New or modified files: {2}, Deleted files: {3}) [Adding listeners took: {4} ms]", //NOI18N
                            new Object[] {
                                shown,
                                time,
                                outOfDateFiles[0],
                                deletedFiles[0],
                                recursiveListenersTime[0]
                            });
                    }
                }
            }

            LOGGER.log(
                Level.INFO,
                "Complete indexing of {0} source roots took: {1} ms (New or modified files: {2}, Deleted files: {3}) [Adding listeners took: {4} ms]", //NOI18N
                new Object[] {
                    scannedRootsCnt,
                    completeTime,
                    totalOutOfDateFiles,
                    totalDeletedFiles,
                    totalRecursiveListenersTime
                });
            TEST_LOGGER.log(Level.FINEST, "scanSources", ctx.newRootsToScan); //NOI18N
            return finished;
        }

        private static boolean isNoRootsScan() {
            return Boolean.getBoolean("netbeans.indexing.noRootsScan"); //NOI18N
        }

        @CheckForNull
        private static URL getRemoteIndexURL(@NonNull final URL sourceRoot) {
            for (IndexDownloader ld : Lookup.getDefault().lookupAll(IndexDownloader.class)) {
                final URL indexURL = ld.getIndexURL(sourceRoot);
                if (indexURL != null) {
                    return indexURL;
                }
            }
            return null;
        }

        private static boolean patchDownloadedIndex(
                @NonNull final URL sourceRoot,
                @NonNull final URL cacheFolder) {
            boolean vote = true;
            for (DownloadedIndexPatcher patcher : Lookup.getDefault().lookupAll(DownloadedIndexPatcher.class)) {
                vote &= patcher.updateIndex(sourceRoot, cacheFolder);
            }
            return vote;
        }

        @NonNull
        private static String getSimpleName(@NonNull final URL indexURL) throws IllegalArgumentException {
            final String path = indexURL.getPath();
            if (path.length() == 0 || path.charAt(path.length()-1) == '/') {    //NOI18N
                throw new IllegalArgumentException(indexURL.toString());
            }
            final int index = path.lastIndexOf('/');  //NOI18N
            return index < 0 ? path : path.substring(index+1);
        }

        @CheckForNull
        private File download (
                @NonNull final URL indexURL,
                @NonNull final File into) throws IOException {
            try {
                final File packedIndex = new File (into,getSimpleName(indexURL));       //NOI18N
                final InputStream in = new BufferedInputStream(indexURL.openStream());
                try {
                    final OutputStream out = new BufferedOutputStream(new FileOutputStream(packedIndex));
                    try {
                        FileUtil.copy(in, out);
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
                return packedIndex;
            } catch (IOException ioe) {
                return null;
            }
        }

        private boolean unpack (@NonNull final File packedFile, @NonNull File targetFolder) throws IOException {
            final ZipFile zf = new ZipFile(packedFile);
            final Enumeration<? extends ZipEntry> entries = zf.entries();
            try {
                while (entries.hasMoreElements()) {
                    final ZipEntry entry = entries.nextElement();
                    final File target = new File (targetFolder,entry.getName().replace('/', File.separatorChar));   //NOI18N
                    if (entry.isDirectory()) {
                        target.mkdirs();
                    } else {
                        //Some zip files don't have zip entries for folders
                        target.getParentFile().mkdirs();
                        final InputStream in = zf.getInputStream(entry);
                        try {
                            final FileOutputStream out = new FileOutputStream(target);
                            try {
                                FileUtil.copy(in, out);
                            } finally {
                                out.close();
                            }
                        } finally {
                            in.close();
                        }
                    }
                }
            } finally {
                zf.close();
            }
            return true;
        }

        private static void delete (@NullAllowed final File... toDelete) {
            if (toDelete != null) {
                for (File td : toDelete) {
                    if (td.isDirectory()) {
                        delete(td.listFiles());
                    }
                    td.delete();
                }
            }
        }

        private boolean nopCustomIndexers(
            @NonNull final URL root,
            @NonNull final SourceIndexers indexers,
            final boolean sourceForBinaryRoot) throws IOException {
            final FileObject cacheRoot = CacheFolder.getDataFolder(root);
            final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> transactionContexts = new HashMap<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>>();
            final UsedIndexables usedIndexables = new UsedIndexables();
            final Map<SourceIndexerFactory,Boolean> votes = new HashMap<SourceIndexerFactory, Boolean>();
            boolean indexResult = false;
            try {
                customIndexersScanStarted(root, cacheRoot, sourceForBinaryRoot, indexers.cifInfos, votes, transactionContexts);
                for (IndexerCache.IndexerInfo<CustomIndexerFactory> info : indexers.cifInfos) {
                    if (getCancelRequest().isRaised()) {
                        break;
                    }
                    final CustomIndexerFactory factory = info.getIndexerFactory();
                    final CustomIndexer indexer = factory.createIndexer();

                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Fake indexing: indexer=" + indexer); //NOI18N
                    }
                    long start = System.currentTimeMillis();
                    logStartIndexer(info.getIndexerName());
                    try {
                        final Pair<String,Integer> indexerKey = Pair.<String,Integer>of(factory.getIndexerName(),factory.getIndexVersion());
                        final Pair<SourceIndexerFactory,Context> ctx = transactionContexts.get(indexerKey);
                        if (ctx != null) {
                            SPIAccessor.getInstance().index(indexer, Collections.<Indexable>emptySet(), ctx.second());
                        } else {
                            LOGGER.log(
                                Level.WARNING, "RefreshCifIndices ignored recently added factory: {0}", //NOI18N
                                indexerKey);
                        }
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable t) {
                        LOGGER.log(Level.WARNING, null, t);
                    } finally {
                        logIndexerTime(info.getIndexerName(), (int)(System.currentTimeMillis() - start));
                    }
                }
                indexResult = !getCancelRequest().isRaised();
            } finally {
                scanFinished(transactionContexts.values(),usedIndexables, indexResult);
            }
            return indexResult;
        }
        
        private boolean scanSource (URL root, boolean fullRescan, boolean sourceForBinaryRoot, SourceIndexers indexers, int [] outOfDateFiles, int [] deletedFiles, long [] recursiveListenersTime) throws IOException {
            LOGGER.log(Level.FINE, "Scanning sources root: {0}", root); //NOI18N
            final boolean rootSeen = TimeStamps.existForRoot(root);
            if (isNoRootsScan() && !fullRescan && rootSeen) {
                // We've already seen the root at least once and roots scanning is forcibly turned off
                // so just call indexers with no files to let them know about the root, but perform
                // no indexing.
                return nopCustomIndexers(root, indexers, sourceForBinaryRoot);
            } else {
                final FileObject rootFo = URLCache.getInstance().findFileObject(root, true);
                if (rootFo != null) {
                    LogContext lctx = getLogContext();
                    long t = System.currentTimeMillis();
                    URL indexURL;
                    if (!rootSeen && (indexURL=getRemoteIndexURL(root))!=null) {
                        LOGGER.log(
                                Level.FINE,
                                "Downloading index for root: {0} from: {1}",
                                new Object[]{root, indexURL});
                        final FileObject cf = CacheFolder.getCacheFolder();
                        assert cf != null;
                        final File cacheFolder = FileUtil.toFile(cf);
                        assert cacheFolder != null;
                        final File downloadFolder = new File (cacheFolder,INDEX_DOWNLOAD_FOLDER);   //NOI18N
                        if (downloadFolder.exists()) {
                            delete (downloadFolder.listFiles());
                        } else {
                            downloadFolder.mkdir();
                        }
                        final File packedIndex = download(indexURL, downloadFolder);
                        if (packedIndex != null ) {
                            unpack(packedIndex, downloadFolder);
                            packedIndex.delete();
                            if (patchDownloadedIndex(root,org.openide.util.Utilities.toURI(downloadFolder).toURL())) {
                                final FileObject df = CacheFolder.getDataFolder(root);
                                assert df != null;
                                final File dataFolder = FileUtil.toFile(df);
                                assert dataFolder != null;
                                if (dataFolder.exists()) {
                                    //Some features already forced folder creation
                                    //delete it to be able to do renameTo
                                    delete(dataFolder);
                                }
                                downloadFolder.renameTo(dataFolder);
                                final TimeStamps timeStamps = TimeStamps.forRoot(root, false);
                                timeStamps.resetToNow();
                                timeStamps.store();
                                nopCustomIndexers(root, indexers, sourceForBinaryRoot);
                                for (Map.Entry<File,Index> e : IndexManager.getOpenIndexes().entrySet()) {
                                    if (Util.isParentOf(dataFolder, e.getKey())) {
                                        e.getValue().getStatus(true);
                                    }
                                }
                                return true;
                            }
                        }
                    }
                    //todo: optimize for java.io.Files
                    final ClassPath.Entry entry = sourceForBinaryRoot ? null : getClassPathEntry(rootFo);
                    final Set<Crawler.TimeStampAction> checkTimeStamps = EnumSet.of(Crawler.TimeStampAction.UPDATE);
                    if (!fullRescan) {
                        checkTimeStamps.add(Crawler.TimeStampAction.CHECK);
                    }
                    final Crawler crawler = new FileObjectCrawler(rootFo, checkTimeStamps, entry, getCancelRequest(), getSuspendStatus());
                    final List<Indexable> resources = crawler.getResources();
                    final List<Indexable> allResources = crawler.getAllResources();
                    final List<Indexable> deleted = crawler.getDeletedResources();
                    
                    this.modifiedResourceCount = resources.size();
                    this.allResourceCount = allResources.size();

                    logCrawlerTime(crawler, t);
                    if (crawler.isFinished()) {
                        final Map<SourceIndexerFactory,Boolean> invalidatedMap = new IdentityHashMap<SourceIndexerFactory, Boolean>();
                        final Map<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>> ctxToFinish = new HashMap<Pair<String,Integer>,Pair<SourceIndexerFactory,Context>>();
                        final UsedIndexables usedIterables = new UsedIndexables();
                        boolean indexResult = false;
                        try {
                            scanStarted (root, sourceForBinaryRoot, indexers, invalidatedMap, ctxToFinish);
                            delete(deleted, ctxToFinish, usedIterables);
                            invalidateSources(resources);
                            final long tm = System.currentTimeMillis();
                            final boolean rlAdded = RepositoryUpdater.getDefault().rootsListeners.add(root, true, entry);
                            if (recursiveListenersTime != null) {
                                recursiveListenersTime[0] = System.currentTimeMillis() - tm;
                            }
                            if (rlAdded) {
                                indexResult = index(resources, allResources, root, sourceForBinaryRoot, indexers, invalidatedMap, ctxToFinish, usedIterables);
                                if (indexResult) {
                                    crawler.storeTimestamps();
                                    outOfDateFiles[0] = resources.size();
                                    deletedFiles[0] = deleted.size();
                                    if (logStatistics) {
                                        logStatistics = false;
                                        if (SFEC_LOGGER.isLoggable(Level.INFO)) {
                                            LogRecord r = new LogRecord(Level.INFO, "STATS_SCAN_SOURCES"); //NOI18N
                                            r.setParameters(new Object [] { Boolean.valueOf(outOfDateFiles[0] > 0 || deletedFiles[0] > 0)});
                                            r.setResourceBundle(NbBundle.getBundle(RepositoryUpdater.class));
                                            r.setResourceBundleName(RepositoryUpdater.class.getPackage().getName() + ".Bundle"); //NOI18N
                                            r.setLoggerName(SFEC_LOGGER.getName());
                                            SFEC_LOGGER.log(r);
                                        }
                                    }
                                    return true;
                                }
                            }
                        } finally {
                            scanFinished(ctxToFinish.values(), usedIterables, indexResult);
                        }
                    }
                    return false;
                } else {
                    RepositoryUpdater.getDefault().rootsListeners.add(root,true, null);
                    return true;
                }
            }
        }
        
        private static void reportRootScan(URL root, long duration) {
            if (PERF_LOGGER.isLoggable(Level.FINE)) {
                PERF_LOGGER.log (
                    Level.FINE,
                    "reportScanOfFile: {0} {1}", //NOI18N
                    new Object[] {
                        root,
                        duration
                    });
            }
        }
    } // End of AbstractRootsWork class

    private final class InitialRootsWork extends RootsWork {

        private final boolean waitForProjects;

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public InitialRootsWork(
                Map<URL, List<URL>> scannedRoots2Depencencies,
                Map<URL,List<URL>>  scannedBinaries2InvDependencies,
                Map<URL,List<URL>>  scannedRoots2Peers,
                Set<URL> sourcesForBinaryRoots,
                boolean waitForProjects,
                @NonNull final AtomicLong scannedRoots2DependenciesLamport,
                @NonNull final SuspendStatus suspendStatus,
                @NullAllowed final LogContext logCtx) {
            super(scannedRoots2Depencencies,
                scannedBinaries2InvDependencies,
                scannedRoots2Peers,
                sourcesForBinaryRoots,
                true,
                true,
                scannedRoots2DependenciesLamport,
                suspendStatus,
                logCtx);
            this.waitForProjects = waitForProjects;
        }
        
        public @Override boolean getDone() {
            try {                
                if (waitForProjects) {
                    boolean retry = true;
                    suspendProgress(NbBundle.getMessage(RepositoryUpdater.class, "MSG_OpeningProjects"));
                    while (retry) {
                        try {
                            OpenProjects.getDefault().openProjects().get(1000, TimeUnit.MILLISECONDS);
                            retry = false;
                        } catch (TimeoutException ex) {
                            if (isCancelledExternally()) {
                                return false;
                            }
                        } catch (Exception ex) {
                            // ignore
                            retry = false;
                        }
                    }
                }
                
                if (indexers == null) {
                    indexers = SourceIndexers.load(true);
                }

                return super.getDone();
            } finally {
                if (state == State.INITIAL_SCAN_RUNNING) {
                    synchronized (RepositoryUpdater.this) {
                        if (state == State.INITIAL_SCAN_RUNNING) {
                            state = State.ACTIVE;
                        }
                    }
                }
            }
        }
    } // End of InitialRootsWork class

    private static final class Task implements Runnable {

        // -------------------------------------------------------------------
        // Public implementation
        // -------------------------------------------------------------------

        public void schedule (Iterable<? extends Work> multipleWork) {
            synchronized (todo) {
                for(Work w : multipleWork) {
                    schedule(w, false);
                }
            }
        }

        public void schedule (Work work, boolean wait) {
            boolean waitForWork = false;
            if (wait && Utilities.holdsParserLock()) {
                throw new IllegalStateException("Caller holds TaskProcessor.parserLock, which may cause deadlock.");    //NOI18N
            }
            synchronized (todo) {
                assert work != null;
                if (!allCancelled) {
                    boolean canceled = false;
                    final List<Work> follow = new ArrayList<Work>(1);
                    if (workInProgress != null) {
                        if (workInProgress.cancelBy(work,follow)) {
                            // make the WiP absorbed by this follow-up work:
                            if (workInProgress.logCtx != null) {
                                if (work.logCtx != null) {
                                    work.logCtx.absorb(workInProgress.logCtx);
                                } else {
                                    work.logCtx = workInProgress.logCtx;
                                }
                            }
                            canceled = true;
                        }
                    }

                    // coalesce ordinary jobs
                    Work absorbedBy = null;
                    if (!wait) {
                        Work lastDel = null;

                        //XXX (#198565): don't let FileListWork forerun delete works:
                        if (work instanceof FileListWork) {
                            final FileListWork flw = (FileListWork) work;
                            for (Work w : todo) {
                                if (w instanceof DeleteWork &&
                                    ((DeleteWork)w).root.equals(flw.root)) {
                                    lastDel = w;
                                }
                            }
                        }

                        for(Work w : todo) {
                            if (lastDel == null) {
                                if (w.absorb(work)) {
                                    absorbedBy = w;
                                    break;
                                }
                            } else if (w == lastDel) {
                                lastDel = null;
                            }
                        }
                    }

                    if (absorbedBy == null) {
                        LOGGER.log(Level.FINE, "Scheduling {0}", work); //NOI18N
                        if (canceled) {
                            todo.add(0, work);
                            todo.addAll(1,follow);
                        } else {
                            todo.add(work);
                        }
                    } else {
                        if (absorbedBy.logCtx != null) {
                            if (work.logCtx != null) {
                                absorbedBy.logCtx.absorb(work.logCtx);
                            }
                        } else {
                            absorbedBy.logCtx = work.logCtx;
                        }
                        if (canceled) {
                            todo.remove(absorbedBy);
                            todo.add(0, absorbedBy);
                            todo.addAll(1,follow);
                        }
                        LOGGER.log(Level.FINE, "Work absorbed {0}", work); //NOI18N
                    }

                    followUpWorksSorted = false;

                    if (!scheduled && protectedOwners.isEmpty()) {
                        scheduled = true;
                        LOGGER.fine("scheduled = true");    //NOI18N
                        WORKER.submit(this);
                    }
                    waitForWork = wait;
                }
            }
            if (waitForWork) {
                LOGGER.log(Level.FINE, "Waiting for {0}", work); //NOI18N
                work.waitUntilDone();
            }
        }

        public void cancelAll(@NullAllowed final Runnable postCleanTask) throws TimeoutException {
            synchronized (todo) {
                if (!allCancelled) {
                    // stop accepting new work and clean the queue
                    todo.clear();
                    if (postCleanTask != null) {
                        schedule (
                            new Work(false, false, false, true, SuspendSupport.NOP, null) {
                                @Override
                                protected boolean getDone() {
                                    postCleanTask.run();
                                    return true;
                                }
                            },
                            false);
                    }
                    allCancelled = true;
                    // stop the work currently being done
                    final Work work = workInProgress;
                    if (work != null) {
                        work.setCancelled(true);
                    }

                    // wait until the current work is finished
                    int cnt = 10;
                    while (scheduled && cnt-- > 0) {
                        LOGGER.log(Level.FINE, "Waiting for indexing jobs to finish; job in progress: {0}, jobs queue: {1}", new Object[] { work, todo }); //NOI18N
                        try {
                            todo.wait(1000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }

                    if (scheduled && cnt == 0) {
                        LOGGER.log(Level.INFO, "Waiting for indexing jobs to finish timed out; job in progress {0}, jobs queue: {1}", new Object [] { work, todo }); //NOI18N
                        throw new TimeoutException();
                    }
                }
            }
        }

        public boolean isWorking() {
            synchronized (todo) {
                return scheduled;
            }
        }

        public void enterProtectedMode(@NullAllowed Long id) {
            synchronized (todo) {
                protectedOwners.add(id);
                if (LOGGER.isLoggable(Level.FINE)) {
                    // Call toString() now since exitProtectedMode might run before the log handler formats the record:
                    LOGGER.log(Level.FINE, "Entering protected mode: {0}", protectedOwners.toString()); //NOI18N
                }
            }
        }

        public void exitProtectedMode(@NullAllowed Long id, @NullAllowed Runnable followupTask) {
            synchronized (todo) {
                if (protectedOwners.isEmpty()) {
                    throw new IllegalStateException("Calling exitProtectedMode without enterProtectedMode"); //NOI18N
                }

                // stash the followup task, we will run all of them when exiting the protected mode
                if (followupTask != null) {
                    if (followupTasks == null) {
                        followupTasks = new LinkedList<Runnable>();
                    }
                    followupTasks.add(followupTask);
                }
                protectedOwners.remove(id);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Exiting protected mode: {0}", protectedOwners.toString()); //NOI18N
                }

                if (protectedOwners.isEmpty()) {
                    // in normal mode again, restart all delayed jobs
                    final List<Runnable> tasks = followupTasks;

                    // delaying of these tasks was just copied from the old java.source RepositoryUpdater
                    RP.create(new Runnable() {
                        public @Override void run() {
                            schedule(new Work(false, false, false, true, SuspendSupport.NOP, null) {
                                protected @Override boolean getDone() {
                                    if (tasks != null) {
                                        for(Runnable task : tasks) {
                                            try {
                                                task.run();
                                            } catch (ThreadDeath td) {
                                                throw td;
                                            } catch (Throwable t) {
                                                LOGGER.log(Level.WARNING, null, t);
                                            }
                                        }
                                    }
                                    return true;
                                }
                            }, false);
                        }
                    }).schedule(FILE_LOCKS_DELAY);
                    LOGGER.log(Level.FINE, "Protected mode exited, scheduling postprocess tasks: {0}", tasks); //NOI18N
                }
            }
        }

        public boolean isInProtectedMode() {
            synchronized (todo) {
                return !protectedOwners.isEmpty();
            }
        }

        public boolean isProtectedModeOwner (final Thread thread) {
            synchronized (todo) {
                return protectedOwners.contains(thread.getId());
            }
        }

        // returns false when timed out
        public boolean waitUntilFinished(long timeout) throws InterruptedException {
            if (Utilities.holdsParserLock()) {
                throw new IllegalStateException("Can't wait for indexing to finish from inside a running parser task"); //NOI18N
            }

            synchronized (todo) {
                while (scheduled) {
                    if (timeout > 0) {
                        todo.wait(timeout);
                        return !scheduled;
                    } else {
                        todo.wait();
                    }
                }
            }

            return true;
        }

        // -------------------------------------------------------------------
        // ParserResultTask implementation
        // -------------------------------------------------------------------

        @Override
        public void run() {
            try {
                Utilities.runPriorityIO(new Callable<Void>(){
                    @Override
                    public Void call() throws Exception {
                        try {
                            RunWhenScanFinishedSupport.performScan(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        _run();
                                    }
                            });
                        } finally {
                            synchronized (todo) {
                                if (!protectedOwners.isEmpty() || todo.isEmpty()) {
                                    scheduled = false;
                                    LOGGER.fine("scheduled = false");   //NOI18N
                                } else {
                                    WORKER.submit(this);
                                }
                                todo.notifyAll();
                            }
                            RunWhenScanFinishedSupport.performDeferredTasks();
                        }
                        return null;
                    }

                });
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            }
        }

        // -------------------------------------------------------------------
        // private implementation
        // -------------------------------------------------------------------

        private final List<Work> todo = new LinkedList<Work>();
        private boolean followUpWorksSorted = true;
        private Work workInProgress = null;
        private boolean scheduled = false;
        private boolean allCancelled = false;
        private List<Long> protectedOwners = new LinkedList<Long>();
        private List<Runnable> followupTasks = null;

        private void _run() {
            ProgressHandle progressHandle = null;
            try {
                final WorkCancel workCancel = new WorkCancel();
                LogContext prevWorkLctx = null;
                for(Work work = getWork(); work != null; work = getWork()) {
                    LogContext curWorkLctx = work.logCtx;
                    if (curWorkLctx != null && prevWorkLctx != null && prevWorkLctx.getExecutedTime() > 0) {
                        long t = System.currentTimeMillis();
                        // if waiting for > 1 minute because of the just preceding work, chain the previous work
                        // the new work was scheduled during the previous one's execution
                        if (t - prevWorkLctx.getExecutedTime() > PROFILE_EXECUTION_DELAY_TRESHOLD &&
                            t - curWorkLctx.getScheduledTime() > PROFILE_EXECUTION_DELAY_TRESHOLD) {
                            curWorkLctx.setPredecessor(prevWorkLctx);
                        }
                    }
                    prevWorkLctx = curWorkLctx;
                    workCancel.setWork(work);
                    try {
                        if (progressHandle == null) {
                            if (work.getProgressTitle() != null) {
                                progressHandle = ProgressHandleFactory.createHandle(work.getProgressTitle(), workCancel);
                                progressHandle.start();
                            }
                        } else {
                            if (work.getProgressTitle() != null) {
                                progressHandle.setDisplayName(work.getProgressTitle());
                            } else {
                                progressHandle.setDisplayName(NbBundle.getMessage(RepositoryUpdater.class, "MSG_BackgroundCompileStart")); //NOI18N
                            }
                        }

                        long tm = 0;
                        if (LOGGER.isLoggable(Level.FINE)) {
                            tm = System.currentTimeMillis();
                            LOGGER.log(Level.FINE, "Performing {0}", work); //NOI18N
                        }
                        work.setProgressHandle(progressHandle);
                        try {
                            work.doTheWork();
                        } catch (ThreadDeath td) {
                            throw td;
                        } catch (Throwable t) {
                            LOGGER.log(Level.WARNING, null, t);
                        } finally {
                            work.setProgressHandle(null);
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.log(Level.FINE, "Finished {0} in {1} ms with result {2}", new Object[] {  //NOI18N
                                    work,
                                    System.currentTimeMillis() - tm,
                                    work.getCancelRequest().isRaised() ? "Cancelled" : work.isFinished() ? "Done" : "Interrupted" //NOI18N
                                });
                            }
                        }
                    } finally {
                        workCancel.setWork(null);
                    }
                }
            } finally {
                if (progressHandle != null) {
                    progressHandle.finish();
                    progressHandle = null;
                }
            }
        }
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        private Work getWork () {
            synchronized (todo) {
                Work w;
                if (protectedOwners.isEmpty() && todo.size() > 0) {
                    w = todo.remove(0);

                    if (w instanceof FileListWork && ((FileListWork) w).isFollowUpJob() && !followUpWorksSorted) {
                        Map<URL, List<FileListWork>> toSort = new HashMap<URL, List<FileListWork>>();

                        toSort.put(((FileListWork) w).root, new LinkedList<FileListWork>(Arrays.asList((FileListWork) w)));
                        
                        for (Iterator<Work> it = todo.iterator(); it.hasNext(); ) {
                            Work current = it.next();

                            if (current instanceof FileListWork && ((FileListWork) current).isFollowUpJob()) {
                                List<FileListWork> currentWorks = toSort.get(((FileListWork) current).root);

                                if (currentWorks == null) {
                                    toSort.put(((FileListWork) current).root, currentWorks = new LinkedList<FileListWork>());

                                }

                                currentWorks.add((FileListWork) current);
                                it.remove();
                            }
                        }

                        List<URL> sortedRoots;

                        try {
                            sortedRoots = new ArrayList<URL>(org.openide.util.Utilities.topologicalSort(toSort.keySet(), getDefault().scannedRoots2Dependencies));
                        } catch (TopologicalSortException tse) {
                            LOGGER.log(Level.INFO, "Cycles detected in classpath roots dependencies, using partial ordering", tse); //NOI18N
                            @SuppressWarnings("unchecked") List<URL> partialSort = tse.partialSort(); //NOI18N
                            sortedRoots = new ArrayList<URL>(partialSort);
                        }

                        Collections.reverse(sortedRoots);

                        for (URL url : sortedRoots) {
                            final List<FileListWork> flws = toSort.get(url);
                            if (flws != null) {
                                todo.addAll(flws);
                            }
                        }

                        followUpWorksSorted = true;
                        w = todo.remove(0);
                    }
                } else {
                    w = null;
                }
                workInProgress = w;
                return w;
            }
        }

        @ServiceProvider(service=IndexingBridge.Ordering.class)
        public static final class IndexingBridgeImpl extends IndexingBridge.Ordering {
            @Override
            protected void enterProtectedMode() {
                RepositoryUpdater.getDefault().getWorker().enterProtectedMode(null);
            }

            @Override
            protected void exitProtectedMode() {
                RepositoryUpdater.getDefault().getWorker().exitProtectedMode(null, null);
            }

            @Override
            protected void await() throws InterruptedException {
                RepositoryUpdater.getDefault().waitUntilFinished(-1);
            }
        }

    } // End of Task class

    private static final class DependenciesContext {

        private final Map<URL, List<URL>> initialRoots2Deps;
        private final Map<URL, List<URL>> initialBinaries2InvDeps;
        private final Map<URL, List<URL>> initialRoots2Peers;

        private final Set<URL> oldRoots;
        private final Set<URL> oldBinaries;

        private final Map<URL,List<URL>> newRoots2Deps;
        private final Map<URL,List<URL>> newBinaries2InvDeps;
        private final Map<URL,List<URL>> newRoots2Peers;
        private final List<URL> newRootsToScan;
        private final Set<URL> newBinariesToScan;

        private final Set<URL> scannedRoots;
        private final Set<URL> scannedBinaries;

        private final Set<URL> sourcesForBinaryRoots;
        private final Set<URL> unknownRoots;
        private final Set<URL> newlySFBTranslated;
        private Map<URL,Collection<URL>> preInversedDeps;
        private Set<URL> fullRescanSourceRoots;

        private final Stack<URL> cycleDetector;
        private final boolean useInitialState;
        private final boolean refreshNonExistentDeps;

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public DependenciesContext (
                final Map<URL, List<URL>> scannedRoots2Deps,
                final Map<URL,List<URL>>  scannedBinaries2InvDependencies,
                final Map<URL,List<URL>>  scannedRoots2Peers,
                final Set<URL> sourcesForBinaryRoots,
                final boolean useInitialState,
                final boolean refreshNonExistentDeps) {
            assert scannedRoots2Deps != null;
            assert scannedBinaries2InvDependencies != null;
            
            this.initialRoots2Deps = Collections.unmodifiableMap(scannedRoots2Deps);
            this.initialBinaries2InvDeps = Collections.unmodifiableMap(scannedBinaries2InvDependencies);
            this.initialRoots2Peers = Collections.unmodifiableMap(scannedRoots2Peers);

            this.oldRoots = new HashSet<URL> (scannedRoots2Deps.keySet());
            this.oldBinaries = new HashSet<URL> (scannedBinaries2InvDependencies.keySet());

            this.newRoots2Deps = new HashMap<URL,List<URL>>();
            this.newBinaries2InvDeps = new HashMap<URL, List<URL>>();
            this.newRoots2Peers = new HashMap<URL, List<URL>>();
            this.newRootsToScan = new ArrayList<URL>();
            this.newBinariesToScan = new HashSet<URL>();            

            this.scannedRoots = new HashSet<URL>();
            this.scannedBinaries = new HashSet<URL>();

            this.sourcesForBinaryRoots = sourcesForBinaryRoots;
            this.fullRescanSourceRoots = new HashSet<URL>();

            this.useInitialState = useInitialState;
            this.refreshNonExistentDeps = refreshNonExistentDeps;
            this.cycleDetector = new Stack<URL>();
            this.unknownRoots = new HashSet<URL>();
            this.newlySFBTranslated = new HashSet<URL>();
        }

        public @Override String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append(": {\n"); //NOI18N
            sb.append("  useInitialState=").append(useInitialState).append("\n"); //NOI18N
            sb.append("  initialRoots2Deps(").append(initialRoots2Deps.size()).append(")=\n"); //NOI18N
            printMap(initialRoots2Deps, sb);
            sb.append("  initialBinaries(").append(initialBinaries2InvDeps.size()).append(")=\n"); //NOI18N
            printMap(initialBinaries2InvDeps, sb);
            sb.append("  initialRoots2Peers(").append(initialRoots2Peers.size()).append(")=\n"); //NOI18N
            printMap(initialRoots2Peers, sb);
            sb.append("  oldRoots(").append(oldRoots.size()).append(")=\n"); //NOI18N
            printCollection(oldRoots, sb);
            sb.append("  oldBinaries(").append(oldBinaries.size()).append(")=\n"); //NOI18N
            printCollection(oldBinaries, sb);
            sb.append("  newRootsToScan(").append(newRootsToScan.size()).append(")=\n"); //NOI18N
            printCollection(newRootsToScan, sb);
            sb.append("  newBinariesToScan(").append(newBinariesToScan.size()).append(")=\n"); //NOI18N
            printCollection(newBinariesToScan, sb);
            sb.append("  scannedRoots(").append(scannedRoots.size()).append(")=\n"); //NOI18N
            printCollection(scannedRoots, sb);
            sb.append("  scannedBinaries(").append(scannedBinaries.size()).append(")=\n"); //NOI18N
            printCollection(scannedBinaries, sb);
            sb.append("  newRoots2Deps(").append(newRoots2Deps.size()).append(")=\n"); //NOI18N
            printMap(newRoots2Deps, sb);
            sb.append("  newBinaries2InvDeps(").append(newBinaries2InvDeps.size()).append(")=\n"); //NOI18N
            printMap(newBinaries2InvDeps, sb);
            sb.append("  newRoots2Peers(").append(newRoots2Peers.size()).append(")=\n"); //NOI18N
            printMap(newRoots2Peers, sb);
            sb.append("} ----\n"); //NOI18N
            return sb.toString();
        }

    } // End of DependenciesContext class

    private static final class SourceIndexers {

        public static SourceIndexers load(boolean detectChanges) {
            return new SourceIndexers(detectChanges);
        }

        public final Set<IndexerCache.IndexerInfo<CustomIndexerFactory>> changedCifs;
        public final Collection<? extends IndexerCache.IndexerInfo<CustomIndexerFactory>> cifInfos;
        public final Set<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>> changedEifs;
        public final Map<String, Collection<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>>> eifInfosMap;

        private SourceIndexers(boolean detectChanges) {
            final long start = System.currentTimeMillis();
            if (detectChanges) {
                changedCifs = new HashSet<IndexerCache.IndexerInfo<CustomIndexerFactory>>();
                changedEifs = new HashSet<IndexerCache.IndexerInfo<EmbeddingIndexerFactory>>();
            } else {
                changedCifs = null;
                changedEifs = null;
            }
            cifInfos = IndexerCache.getCifCache().getIndexers(changedCifs);
            eifInfosMap = IndexerCache.getEifCache().getIndexersMap(changedEifs);

            final long delta = System.currentTimeMillis() - start;
            LOGGER.log(Level.FINE, "Loading indexers took {0} ms.", delta); // NOI18N
        }
    } // End of SourceIndexers class

    private static final class BinaryIndexers {
        public static BinaryIndexers load() {
            return new BinaryIndexers();
        }

        public final Collection<? extends BinaryIndexerFactory> bifs;

        private BinaryIndexers() {
            bifs = MimeLookup.getLookup(MimePath.EMPTY).lookupAll(BinaryIndexerFactory.class);
        }
    } // End of BinaryIndexers class

    private final class Controller extends IndexingController {

        private Map<URL, List<URL>>roots2Dependencies = Collections.emptyMap();
        private Map<URL, List<URL>>binRoots2Dependencies = Collections.emptyMap();
        private Map<URL, List<URL>>roots2Peers = Collections.emptyMap();

        public Controller() {
            super();
            RepositoryUpdater.this.start(false);
        }

        @Override
        public void enterProtectedMode() {
            getWorker().enterProtectedMode(Thread.currentThread().getId());
        }

        @Override
        public void exitProtectedMode(Runnable followUpTask) {
            getWorker().exitProtectedMode(Thread.currentThread().getId(), followUpTask);
        }

        @Override
        public boolean isInProtectedMode() {
            return getWorker().isInProtectedMode();
        }

        @Override
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public synchronized Map<URL, List<URL>> getRootDependencies() {
            return roots2Dependencies;
        }

        @Override
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public synchronized Map<URL, List<URL>> getBinaryRootDependencies() {
            return binRoots2Dependencies;
        }
        
        @Override
        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public Map<URL, List<URL>> getRootPeers() {
            return roots2Peers;
        }

        @Override
        public int getFileLocksDelay() {
            return FILE_LOCKS_DELAY;
        }

    } // End of Controller class

    @ServiceProvider(service=IndexingActivityInterceptor.class)
    public static final class FSRefreshInterceptor implements IndexingActivityInterceptor {

        private FileSystem.AtomicAction activeAA = null;
        private boolean ignoreFsEvents = false;

        public FSRefreshInterceptor() {
            // no-op
        }

        @Override
        public Authorization authorizeFileSystemEvent(FileEvent event) {
            synchronized (this) {
                if (activeAA != null) {
                    boolean firedFrom = event.firedFrom(activeAA);
                    LOGGER.log(Level.FINE, "{0} fired from {1}: {2}", new Object[] { event, activeAA, firedFrom }); //NOI18N
                    return firedFrom ? Authorization.IGNORE : Authorization.PROCESS;
                } else {
                    LOGGER.log(Level.FINE, "Set to ignore {0}: {1}", new Object[] { event, ignoreFsEvents }); //NOI18N
                    return ignoreFsEvents ? Authorization.IGNORE : Authorization.PROCESS;
                }
            }
        }

        public void setActiveAtomicAction(FileSystem.AtomicAction aa) {
            synchronized (this) {
                LOGGER.log(Level.FINE, "setActiveAtomicAction({0})", aa); //NOI18N
                if (aa != null) {
                    assert activeAA == null : "Expecting no activeAA: " + activeAA; //NOI18N
                    activeAA = aa;
                } else {
                    assert activeAA != null : "Expecting some activeAA"; //NOI18N
                    activeAA = null;
                }
            }
        }

        public void setIgnoreFsEvents(boolean ignore) {
            synchronized (this) {
                LOGGER.log(Level.FINE, "setIgnoreFsEvents({0})", ignore); //NOI18N
                assert activeAA == null : "Expecting no activeAA: " + activeAA; //NOI18N
                ignoreFsEvents = ignore;
            }
        }
    } // End of FSRefreshInterceptor class

    /* test */ static final class LexicographicComparator implements Comparator<URL> {
        private final boolean reverse;

        public LexicographicComparator(boolean reverse) {
            this.reverse = reverse;
        }

        @Override
        public int compare(URL o1, URL o2) {
            int order = o1.toString().compareTo(o2.toString());
            return reverse ? -1 * order : order;
        }
    } // End of LexicographicComparator class

    /* test */ static final class RootsListeners {

        private FileChangeListener sourcesListener = null;
        private FileChangeListener binariesListener = null;
        private final Map<URL, File> sourceRoots = new HashMap<URL, File>();
        private final Map<URL, Pair<File, Boolean>> binaryRoots = new HashMap<URL, Pair<File, Boolean>>();
        private volatile boolean listens;

        public RootsListeners() {
        }

        public void setListener(FileChangeListener sourcesListener, FileChangeListener binariesListener) {
            assert (sourcesListener != null && binariesListener != null) || (sourcesListener == null && binariesListener == null) :
                "Both sourcesListener and binariesListener must either be null or non-null"; //NOI18N

            //todo: remove removeRecursiveListener from synchronized block
            synchronized (this) {
                if (sourcesListener != null) {
                    assert this.sourcesListener == null : "Already using " + this.sourcesListener + "and " + this.binariesListener //NOI18N
                            + ", won't attach " + sourcesListener + " and " + binariesListener; //NOI18N
                    assert sourceRoots.isEmpty() : "Expecting no source roots: " + sourceRoots; //NOI18N
                    assert binaryRoots.isEmpty() : "Expecting no binary roots: " + binaryRoots; //NOI18N

                    this.sourcesListener = sourcesListener;
                    this.binariesListener = binariesListener;
                    if (!useRecursiveListeners) {
                        FileUtil.addFileChangeListener(sourcesListener);
                    }
                    listens = true;
                } else {
                    assert this.sourcesListener != null : "RootsListeners are already dormant"; //NOI18N

                    if (!useRecursiveListeners) {
                        FileUtil.removeFileChangeListener(sourcesListener);
                    }
                    for(Map.Entry<URL, File> entry : sourceRoots.entrySet()) {
                        safeRemoveRecursiveListener(this.sourcesListener, entry.getValue());
                    }
                    sourceRoots.clear();
                    for(Map.Entry<URL, Pair<File, Boolean>> entry : binaryRoots.entrySet()) {
                        if (entry.getValue().second()) {
                            safeRemoveFileChangeListener(this.binariesListener, entry.getValue().first());
                        } else {
                            safeRemoveRecursiveListener(this.binariesListener, entry.getValue().first());
                        }
                    }
                    binaryRoots.clear();
                    this.sourcesListener = null;
                    this.binariesListener = null;
                    listens = false;
                }
            }
        }

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public synchronized boolean add(
                @NonNull final URL root,
                final boolean sourceRoot,
                @NullAllowed ClassPath.Entry entry) {
            if (sourceRoot) {
                if (sourcesListener != null) {
                    if (!sourceRoots.containsKey(root) && root.getProtocol().equals("file")) { //NOI18N
                        try {
                            File f = org.openide.util.Utilities.toFile(root.toURI());
                            safeAddRecursiveListener(sourcesListener, f, entry);
                            sourceRoots.put(root, f);
                        } catch (URISyntaxException use) {
                            LOGGER.log(Level.INFO, null, use);
                        }
                    }
                }
            } else {
                if (binariesListener != null) {
                    if (!binaryRoots.containsKey(root)) {
                        File f = null;
                        URL archiveUrl = FileUtil.getArchiveFile(root);
                        try {
                            URI uri = archiveUrl != null ? archiveUrl.toURI() : root.toURI();
                            if (uri.getScheme().equals("file")) { //NOI18N
                                f = org.openide.util.Utilities.toFile(uri);
                            }
                        } catch (URISyntaxException use) {
                            LOGGER.log(Level.INFO, "Can't convert " + root + " to java.io.File; archiveUrl=" + archiveUrl, use); //NOI18N
                        }

                        if (f != null) {
                            if (archiveUrl != null) {
                                // listening on an archive file
                                safeAddFileChangeListener(binariesListener, f);
                            } else {
                                // listening on a folder
                                safeAddRecursiveListener(binariesListener, f, entry);
                            }
                            binaryRoots.put(root, Pair.of(f, archiveUrl != null));
                        }
                    }
                }
            }
            return listens;
        }

        @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
        public synchronized void remove(final Iterable<? extends URL> roots, final boolean sourceRoot) {
            for (URL root : roots) {
                if (sourceRoot) {
                    if (sourcesListener != null) {
                        File f = sourceRoots.remove(root);
                        if (f != null) {
                            safeRemoveRecursiveListener(sourcesListener, f);
                        }
                    }
                } else {
                    if (binariesListener != null) {
                        Pair<File, Boolean> pair = binaryRoots.remove(root);
                        if (pair != null) {
                            if (pair.second()) {
                                safeRemoveFileChangeListener(binariesListener, pair.first());
                            } else {
                                safeRemoveRecursiveListener(binariesListener, pair.first());
                            }
                        }
                    }
                }
            }
        }

        private void safeAddRecursiveListener(
                @NonNull final FileChangeListener listener,
                @NonNull final File path,
                @NullAllowed final ClassPath.Entry entry) {
            if (useRecursiveListeners) {
                try {
                    final FileFilter filter = entry == null?
                        null:
                        new FileFilter() {
                            @Override
                            public boolean accept(@NonNull final File pathname) {
                                try {
                                    return entry.includes(org.openide.util.Utilities.toURI(pathname).toURL());
                                } catch (MalformedURLException ex) {
                                    Exceptions.printStackTrace(ex);
                                    return true;
                                }
                            }
                        };
                    FileUtil.addRecursiveListener(
                        listener,
                        path, 
                        filter,
                        new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return !listens;
                            }
                        });
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable e) {
                    // ignore
                    LOGGER.log(Level.FINE, null, e);
                }
            }
        }

        private void safeRemoveRecursiveListener(FileChangeListener listener, File path) {
            if (useRecursiveListeners) {
                try {
                    FileUtil.removeRecursiveListener(listener, path);
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable e) {
                    // ignore
                    LOGGER.log(Level.FINE, null, e);
                }
            }
        }

        private void safeAddFileChangeListener(FileChangeListener listener, File path) {
            try {
                FileUtil.addFileChangeListener(listener, path);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable e) {
                // ignore
                LOGGER.log(Level.FINE, null, e);
            }
        }

        private void safeRemoveFileChangeListener(FileChangeListener listener, File path) {
            try {
                FileUtil.removeFileChangeListener(listener, path);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable e) {
                // ignore
                LOGGER.log(Level.FINE, null, e);
            }
        }

    } // End of RootsListeners class

    private final class FCL extends FileChangeAdapter {
        private final Boolean listeningOnSources;

        public FCL(Boolean listeningOnSources) {
            this.listeningOnSources = listeningOnSources;
        }

        public @Override void fileFolderCreated(FileEvent fe) {
            fileFolderCreatedImpl(fe, listeningOnSources);
        }

        public @Override void fileDataCreated(FileEvent fe) {
            fileChangedImpl(fe, listeningOnSources);
        }

        public @Override void fileChanged(FileEvent fe) {
            fileChangedImpl(fe, listeningOnSources);
        }

        public @Override void fileDeleted(FileEvent fe) {
            fileDeletedImpl(fe, listeningOnSources);
        }

        public @Override void fileRenamed(FileRenameEvent fe) {
            fileRenamedImpl(fe, listeningOnSources);
        }
    } // End of FCL class

    private static class WorkCancel implements Cancellable {

        private final AtomicReference<Work> work = new AtomicReference<Work>();

        public void setWork(@NonNull final Work theWork) {
            work.set(theWork);
        }

        @Override
        public boolean cancel() {
            final Work theWork = work.get();
            if (theWork != null) {
                final LogContext logCtx = theWork.getLogContext();
                if (logCtx != null) {
                    logCtx.log();
                }
            }
            return false;
        }

    }


    // -----------------------------------------------------------------------
    // Methods for tests
    // -----------------------------------------------------------------------

    /**
     * Used by unit tests
     * @return
     */
    /* test */ State getState () {
        return state;
    }

    //Unit test method
    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    /* test */ Set<URL> getScannedBinaries () {
        return this.scannedBinaries2InvDependencies.keySet();
    }

    //Unit test method
    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    /* test */ Set<URL> getScannedSources () {
        return this.scannedRoots2Dependencies.keySet();
    }

    //Unit test method
    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    /* test */ Map<URL,List<URL>> getScannedRoots2Dependencies() {
        return this.scannedRoots2Dependencies;
    }

    //Unit test method
    @org.netbeans.api.annotations.common.SuppressWarnings(
        value="DMI_COLLECTION_OF_URLS",
        justification="URLs have never host part")
    /* test */ Set<URL> getScannedUnknowns () {
        return this.scannedUnknown;
    }

    /* test */ void ignoreIndexerCacheEvents(boolean ignore) {
        this.ignoreIndexerCacheEvents = ignore;
    }
    
    private static final RequestProcessor SAMPLER_RP = new RequestProcessor("Repository Updater Sampler"); // NOI18N
    private static volatile SamplerInvoker currentSampler;
    
    /**
     * Spy class that starts sampling after a approximate period of time after indexer starts, to
     * sample the indexer's work. Maintains one SamplerInvoker instance, which is started with a delay
     * and if the delay elapses earlier than the indexer ends, the Sampler starts to profile the IDE.
     * The sampler is terminated at the end of the scan Work, and its data is discarded.
     */
    private static class SamplerInvoker implements Runnable, Callable<byte[]> {
        private volatile Sampler sampler;
        private final String indexerName;
        private RequestProcessor.Task scheduled;
        private URL root;
        private int estimate;
        
       public SamplerInvoker(String indexerName, int delay, URL root) {
            this.estimate = delay;
            this.indexerName = indexerName;
            this.root = root;
        }
        
        public static void start(LogContext ctx, String indexerName, int delay, URL root) {
            if (delay <= 0) {
                return;
            }
            if (currentSampler != null) {
                return;
            }
            String prop = System.getProperty(PROP_SAMPLING); // NOI18N
            if (prop == null) {
                return;
            }
            SamplerInvoker inv = new SamplerInvoker(indexerName, delay, root);
            if (Boolean.TRUE.equals(Boolean.valueOf(prop)) || "oneshot".equals(prop)) { // NO18N
                delay = 0;
            }
            ctx.setProfileSource(inv);
            inv.scheduled = SAMPLER_RP.post(inv, delay);
            currentSampler = inv;
            LOGGER.log(Level.FINE, "Sampler scheduled after {0} + {3} for indexer {1} on {2}", // NOI18N
                    new Object[] { new Date(), indexerName, root, delay });
        }
        
        @Override
        public synchronized void run() {
            Sampler sampler = Sampler.createManualSampler("repoupdater"); // NOI18N
            if (sampler != null && currentSampler == this) {
                sampler.start();
                this.sampler = sampler;
                LOGGER.log(Level.FINE, "Updater profiling started at {0} because of {1} runnint on {2} more than {3})",  // NOI18N
                        new Object[] { new Date(), indexerName, root, estimate });
            
            }
        }

        @Override
        public synchronized byte[] call() throws Exception {
            if (sampler == null) {
                return null;
            }
            LOGGER.log(Level.FINE, "Dumping snapshot for {0}", indexerName); // NOI18N
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(out);
            sampler.stopAndWriteTo(dos);
            dos.close();
            sampler = null;
            return out.toByteArray();
        }
             
        public boolean _stop(boolean release) {
            LOGGER.log(Level.FINE, "Sampler cancelled at {0} for indexer {1} on {2}", new Object[] { // NOI18N
                new Date(), indexerName, root
            });
            if (scheduled != null) {
                if (!scheduled.cancel()) {
                    LOGGER.log(Level.FINE, "Sampling has already started, release = " + release); // NOI18N
                    if (release) {
                        synchronized (this) {
                            if (sampler != null) {
                                sampler.cancel();
                                sampler = null;
                            }
                        }
                       return true;
                    }
                    return false;
                }                                    
            }
            return true;
        }
        
        public static void release() {
            if (currentSampler != null) {
                currentSampler._stop(true);
                currentSampler = null;
            }
        }
        
        public static boolean stop() {
            if (currentSampler == null) {
                return true;
            } else {
                boolean s = currentSampler._stop(false);
                if (s) {
                    currentSampler = null;
                }
                return s;
            }
        }
    }
}