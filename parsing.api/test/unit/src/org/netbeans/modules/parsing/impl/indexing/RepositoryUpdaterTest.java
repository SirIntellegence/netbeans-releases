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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.test.MockMimeLookup;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.project.Project;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.junit.RandomlyFails;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.modules.parsing.impl.SourceAccessor;
import org.netbeans.modules.parsing.impl.SourceFlags;
import org.netbeans.modules.parsing.impl.TaskProcessor;
import org.netbeans.modules.parsing.impl.event.EventSupport;
import org.netbeans.modules.parsing.impl.indexing.friendapi.IndexingController;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.Parser.Result;
import org.netbeans.modules.parsing.spi.ParserFactory;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;
import org.netbeans.modules.parsing.spi.indexing.BinaryIndexer;
import org.netbeans.modules.parsing.spi.indexing.BinaryIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexer;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexer;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.netbeans.modules.parsing.spi.indexing.PathRecognizer;
import org.netbeans.modules.project.uiapi.OpenProjectsTrampoline;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.ClassPathImplementation;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.FilteringPathResourceImplementation;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.java.classpath.support.PathResourceBase;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.netbeans.spi.queries.VisibilityQueryImplementation2;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * TODO:
 * - test that modifying .zip/.jar triggeres rescan of this binary
 *
 * @author Tomas Zezula
 */
public class RepositoryUpdaterTest extends NbTestCase {


    private static final int TIME = 5000;
    private static final String SOURCES = "FOO_SOURCES";
    private static final String PLATFORM = "FOO_PLATFORM";
    private static final String LIBS = "FOO_LIBS";
    private static final String MIME = "text/foo";
    private static final String EMIME = "text/emb";
    private static final String JARMIME = "application/java-archive";

    private FileObject srcRoot1;
    private FileObject srcRoot2;
    private FileObject srcRoot3;
    private FileObject compRoot1;
    private FileObject compRoot2;
    private FileObject bootRoot1;
    private FileObject bootRoot2;
    private FileObject bootRoot3;
    private FileObject compSrc1;
    private FileObject compSrc2;
    private FileObject bootSrc1;
    private FileObject unknown1;
    private FileObject unknown2;
    private FileObject unknownSrc2;
    private FileObject srcRootWithFiles1;

    FileObject f1;
    FileObject f3;

    private URL[] customFiles;
    private URL[] embeddedFiles;

    private final BinIndexerFactory binIndexerFactory = new BinIndexerFactory();
// Binary indexer have to be registered for MimePath.EMPTY, no mime-type specific binary indexers
//    private final BinIndexerFactory jarIndexerFactory = new BinIndexerFactory();
    private final FooIndexerFactory indexerFactory = new FooIndexerFactory();
    private final EmbIndexerFactory eindexerFactory = new EmbIndexerFactory();

    private final Map<String, Set<ClassPath>> registeredClasspaths = new HashMap<String, Set<ClassPath>>();

    public RepositoryUpdaterTest (String name) {
        super (name);
    }

    public static Test suite() {
        TestSuite suite = new NbTestSuite(RepositoryUpdaterTest.class);
//        TestSuite suite = new NbTestSuite ();
//        suite.addTest(new RepositoryUpdaterTest("testPeerRootsCalculation"));
        return suite;
    }

    @Override
    protected void setUp() throws Exception {
//        TopLogging.initializeQuietly();
        super.setUp();
        this.clearWorkDir();
        final File _wd = this.getWorkDir();
        final FileObject wd = FileUtil.toFileObject(_wd);
        final FileObject cache = wd.createFolder("cache");
        CacheFolder.setCacheFolder(cache);

        MockServices.setServices(
                FooPathRecognizer.class,
                EmbPathRecognizer.class,
                SFBQImpl.class,
                OpenProject.class,
                ClassPathProviderImpl.class,
                Visibility.class);
        MockMimeLookup.setInstances(MimePath.EMPTY, binIndexerFactory);
//        MockMimeLookup.setInstances(MimePath.get(JARMIME), jarIndexerFactory);
        MockMimeLookup.setInstances(MimePath.get(MIME), indexerFactory);
        MockMimeLookup.setInstances(MimePath.get(EMIME), eindexerFactory, new EmbParserFactory());
        Set<String> mt = new HashSet<String>();
        mt.add(EMIME);
        mt.add(MIME);
        Util.allMimeTypes = mt;

        assertNotNull("No masterfs",wd);
        srcRoot1 = wd.createFolder("src1");
        assertNotNull(srcRoot1);
        srcRoot2 = wd.createFolder("src2");
        assertNotNull(srcRoot2);
        srcRoot3 = wd.createFolder("src3");
        assertNotNull (srcRoot3);
        compRoot1 = wd.createFolder("comp1");
        assertNotNull (compRoot1);
        compRoot2 = wd.createFolder("comp2");
        assertNotNull (compRoot2);
        bootRoot1 = wd.createFolder("boot1");
        assertNotNull (bootRoot1);
        bootRoot2 = wd.createFolder("boot2");
        assertNotNull (bootRoot2);
        FileUtil.setMIMEType("jar", JARMIME);
        FileObject jarFile = FileUtil.toFileObject(getDataDir()).getFileObject("JavaApplication1.jar");
        assertNotNull(jarFile);
        assertTrue(FileUtil.isArchiveFile(jarFile));
        bootRoot3 = FileUtil.getArchiveRoot(jarFile);
        assertNotNull (bootRoot3);
        compSrc1 = wd.createFolder("cs1");
        assertNotNull (compSrc1);
        compSrc2 = wd.createFolder("cs2");
        assertNotNull (compSrc2);
        bootSrc1 = wd.createFolder("bs1");
        assertNotNull (bootSrc1);
        unknown1 = wd.createFolder("uknw1");
        assertNotNull (unknown1);
        unknown2 = wd.createFolder("uknw2");
        assertNotNull (unknown2);
        unknownSrc2 = wd.createFolder("uknwSrc2");
        assertNotNull(unknownSrc2);
        SFBQImpl.register (bootRoot1,bootSrc1);
        SFBQImpl.register (compRoot1,compSrc1);
        SFBQImpl.register (compRoot2,compSrc2);
        SFBQImpl.register (unknown2,unknownSrc2);

        srcRootWithFiles1 = wd.createFolder("srcwf1");
        assertNotNull(srcRootWithFiles1);
        FileUtil.setMIMEType("foo", MIME);
        f1 = FileUtil.createData(srcRootWithFiles1,"folder/a.foo");
        assertNotNull(f1);
        assertEquals(MIME, f1.getMIMEType());
        FileObject f2 = FileUtil.createData(srcRootWithFiles1,"folder/b.foo");
        assertNotNull(f2);
        assertEquals(MIME, f2.getMIMEType());
        customFiles = new URL[] {f1.getURL(), f2.getURL()};

        FileUtil.setMIMEType("emb", EMIME);
        f3 = FileUtil.createData(srcRootWithFiles1,"folder/a.emb");
        assertNotNull(f3);
        assertEquals(EMIME, f3.getMIMEType());
        FileObject f4 = FileUtil.createData(srcRootWithFiles1,"folder/b.emb");
        assertNotNull(f4);
        assertEquals(EMIME, f4.getMIMEType());
        embeddedFiles = new URL[] {f3.getURL(), f4.getURL()};


        waitForRepositoryUpdaterInit();
    }

    @Override
    protected void tearDown() throws Exception {
        for(String id : registeredClasspaths.keySet()) {
            Set<ClassPath> classpaths = registeredClasspaths.get(id);
            GlobalPathRegistry.getDefault().unregister(id, classpaths.toArray(new ClassPath[classpaths.size()]));
        }

        super.tearDown();
    }

    protected final void globalPathRegistry_register(String id, ClassPath [] classpaths) {
        Set<ClassPath> set = registeredClasspaths.get(id);
        if (set == null) {
            set = new HashSet<ClassPath>();
            registeredClasspaths.put(id, set);
        }
        set.addAll(Arrays.asList(classpaths));
        GlobalPathRegistry.getDefault().register(id, classpaths);
    }

    protected final void globalPathRegistry_unregister(String id, ClassPath [] classpaths) {
        GlobalPathRegistry.getDefault().unregister(id, classpaths);
        Set<ClassPath> set = registeredClasspaths.get(id);
        if (set != null) {
            set.removeAll(Arrays.asList(classpaths));
        }
    }

    /* package */ static void waitForRepositoryUpdaterInit() throws Exception {
        RepositoryUpdater.getDefault().ignoreIndexerCacheEvents(true);
        RepositoryUpdater.getDefault().start(true);
        RepositoryUpdater.State state;
        long time = System.currentTimeMillis();
        do {
            if (System.currentTimeMillis() - time > 60000) {
                fail("Waiting for RepositoryUpdater.init() timed out");
            }
            Thread.sleep(100);
            state = RepositoryUpdater.getDefault().getState();
        } while (state != RepositoryUpdater.State.ACTIVE);
        
        // clear all data from previous test runs
        RepositoryUpdater.getDefault().getScannedBinaries().clear();
        RepositoryUpdater.getDefault().getScannedSources().clear();
        RepositoryUpdater.getDefault().getScannedUnknowns().clear();
    }

    @RandomlyFails
    public void testPathAddedRemovedChanged () throws Exception {
        //Empty regs test
        RepositoryUpdater ru = RepositoryUpdater.getDefault();
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedUnknowns().size());

        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);

        //Testing classpath registration
        MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRoot1);
        ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRoot1.getURL(), handler.getSources().get(0));

        //Nothing should be scanned if the same cp is registered again
        handler.reset();
        ClassPath cp1clone = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1clone});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());

        //Nothing should be scanned if the cp is unregistered
        handler.reset();        
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1clone});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());

        //Nothing should be scanned after classpath remove
        handler.reset();
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());        
        

        //Testing changes in registered classpath - add cp root
        handler.reset();
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRoot1.getURL(), handler.getSources().get(0));

        handler.reset();
        mcpi1.addResource(srcRoot2);
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRoot2.getURL(), handler.getSources().get(0));

        //Testing changes in registered classpath - remove cp root
        handler.reset();
        mcpi1.removeResource(srcRoot1);
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());

        //Testing adding new ClassPath
        handler.reset();
        MutableClassPathImplementation mcpi2 = new MutableClassPathImplementation ();
        mcpi2.addResource(srcRoot1);
        ClassPath cp2 = ClassPathFactory.createClassPath(mcpi2);
        globalPathRegistry_register (SOURCES, new ClassPath[] {cp2});
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRoot1.getURL(), handler.getSources().get(0));

        //Testing changes in newly registered classpath - add cp root
        handler.reset();
        mcpi2.addResource(srcRoot3);
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRoot3.getURL(), handler.getSources().get(0));

        //Testing removing ClassPath
        handler.reset();
        globalPathRegistry_unregister(SOURCES,new ClassPath[] {cp2});
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());

        //Testing registering classpath with SFBQ - register PLATFROM
        handler.reset();
        ClassPath cp3 = ClassPathSupport.createClassPath(new FileObject[] {bootRoot1,bootRoot2});
        globalPathRegistry_register(PLATFORM,new ClassPath[] {cp3});
        assertTrue(handler.await());
        assertEquals(1, handler.getBinaries().size());
        assertEquals(this.bootRoot2.getURL(), handler.getBinaries().iterator().next());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.bootSrc1.getURL(), handler.getSources().get(0));

        //Testing registering classpath with SFBQ - register LIBS
        handler.reset();
        MutableClassPathImplementation mcpi4 = new MutableClassPathImplementation ();
        mcpi4.addResource (compRoot1);
        ClassPath cp4 = ClassPathFactory.createClassPath(mcpi4);
        globalPathRegistry_register(LIBS,new ClassPath[] {cp4});
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.compSrc1.getURL(), handler.getSources().get(0));

        //Testing registering classpath with SFBQ - add into LIBS
        handler.reset();
        mcpi4.addResource(compRoot2);
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.compSrc2.getURL(), handler.getSources().get(0));


        //Testing registering classpath with SFBQ - remove from LIBS
        handler.reset();
        mcpi4.removeResource(compRoot1);
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());

        //Testing registering classpath with SFBQ - unregister PLATFORM
        handler.reset();
        globalPathRegistry_unregister(PLATFORM,new ClassPath[] {cp3});
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());

        //Testing listening on SFBQ.Results - bind source
        handler.reset();
        SFBQImpl.register(compRoot2,compSrc1);
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.compSrc1.getURL(), handler.getSources().get(0));

        //Testing listening on SFBQ.Results - rebind (change) source
        handler.reset();
        SFBQImpl.register(compRoot2,compSrc2);
        assertTrue(handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.compSrc2.getURL(), handler.getSources().get(0));
    }

    @RandomlyFails
    public void testIndexers () throws Exception {
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        indexerFactory.indexer.setExpectedFile(customFiles, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);
        MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRootWithFiles1);
        ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());

        handler.reset();
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());

        handler.reset();
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0],new URL[0], new URL[0]);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertEquals(0, indexerFactory.indexer.getIndexCount());
        assertEquals(0, eindexerFactory.indexer.getIndexCount());

        handler.reset();
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());

        Thread.sleep(5000); //Wait for file system time
        handler.reset();
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0],new URL[0], new URL[0]);
        File file = new File (embeddedFiles[0].toURI());
        file.setLastModified(System.currentTimeMillis());
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertEquals(0, indexerFactory.indexer.getIndexCount());
        assertEquals(1, eindexerFactory.indexer.getIndexCount());

        handler.reset();
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());

        Thread.sleep(5000); //Wait for file system time
        handler.reset();
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0],new URL[0], new URL[0]);
        file = new File (embeddedFiles[0].toURI());
        file.setLastModified(System.currentTimeMillis());
        file = new File (embeddedFiles[1].toURI());
        file.delete();
        srcRootWithFiles1.getFileSystem().refresh(true);
        
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertEquals(0, indexerFactory.indexer.getIndexCount());
        assertEquals(1, eindexerFactory.indexer.getIndexCount());
        assertEquals(0, eindexerFactory.indexer.expectedDeleted.size());
        assertEquals(0, eindexerFactory.indexer.expectedDirty.size());
    }

    public void testBinaryIndexers() throws Exception {
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);

        binIndexerFactory.indexer.setExpectedRoots(bootRoot2.getURL(), bootRoot3.getURL());
//        jarIndexerFactory.indexer.setExpectedRoots(bootRoot3.getURL());
        ClassPath cp = ClassPathSupport.createClassPath(new FileObject[] {bootRoot2, bootRoot3});
        globalPathRegistry_register(PLATFORM,new ClassPath[] {cp});
        assertTrue(handler.await());
        assertEquals(2, handler.getBinaries().size());
//        assertEquals(1, jarIndexerFactory.indexer.getCount());
        assertEquals(2, binIndexerFactory.indexer.getCount());
    }

    @RandomlyFails
    public void testFileChanges() throws Exception {
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        indexerFactory.indexer.setExpectedFile(customFiles, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);
        MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRootWithFiles1);
        ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());

        //Test modifications
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[]{f3.getURL()}, new URL[0], new URL[0]);
        final OutputStream out = f3.getOutputStream();
        try {
            out.write(0);
        } finally {
            out.close();
        }
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        assertEquals(1, eindexerFactory.indexer.indexCounter);

        //Test file creation
        File f = FileUtil.toFile(f3);
        final File container = f.getParentFile();
        File newFile = new File (container,"c.emb");
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[]{newFile.toURI().toURL()}, new URL[0], new URL[0]);
        assertNotNull(FileUtil.createData(newFile));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        assertEquals(1, eindexerFactory.indexer.indexCounter);

        //Test folder creation
        final FileObject containerFo = FileUtil.toFileObject(container);
        containerFo.getChildren();
        File newFolder = new File (container,"subfolder");
        newFile = new File (newFolder,"d.emb");
        File newFile2 = new File (newFolder,"e.emb");
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[]{newFile.toURI().toURL(), newFile2.toURI().toURL()}, new URL[0], new URL[0]);
        newFolder.mkdirs();
        touchFile (newFile);
        touchFile (newFile2);
        assertEquals(2,newFolder.list().length);
        FileUtil.toFileObject(newFolder);   //Refresh fs 
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        assertEquals(2, eindexerFactory.indexer.indexCounter);

        //Test file deleted
        handler.reset(TestHandler.Type.DELETE);
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0], new URL[]{f3.getURL()}, new URL[0]);
        f3.delete();
        assertTrue (handler.await());
        assertTrue(indexerFactory.indexer.awaitDeleted());
        assertTrue(eindexerFactory.indexer.awaitDeleted());
        assertEquals(0, eindexerFactory.indexer.indexCounter);
        assertEquals(0,eindexerFactory.indexer.expectedDeleted.size());
        assertEquals(0, eindexerFactory.indexer.expectedDirty.size());

        // test file created and immediatelly deleted in an AtomicAction
        handler.reset(TestHandler.Type.DELETE);
        containerFo.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {
            public void run() throws IOException {
                File newFile = new File (container, "xyz.emb");
                indexerFactory.indexer.setExpectedFile(new URL[0], new URL[] {newFile.toURL()}, new URL[0]);
                eindexerFactory.indexer.setExpectedFile(new URL[0], new URL[] {newFile.toURL()}, new URL[0]);

                FileObject newFileFo = FileUtil.createData(newFile);
                assertNotNull(newFileFo);
                newFileFo.delete();
                assertFalse(newFileFo.isValid());
            }
        });
        assertTrue(indexerFactory.indexer.awaitDeleted());
        assertTrue(eindexerFactory.indexer.awaitDeleted());
        assertEquals(0, indexerFactory.indexer.getIndexCount());
        assertEquals(0, indexerFactory.indexer.getDirtyCount());
        assertEquals(1, indexerFactory.indexer.getDeletedCount());
        assertEquals(0, eindexerFactory.indexer.getIndexCount());
        assertEquals(0, eindexerFactory.indexer.getDirtyCount());
        assertEquals(1, eindexerFactory.indexer.getDeletedCount());
    }

    @RandomlyFails
    public void testFileRenamed() throws Exception {
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRootWithFiles1);
        ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());

        final URL newURL = new URL(f3.getParent().getURL(), "newName.emb");
        final URL oldURL = f3.getURL();

        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[]{oldURL}, new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[]{newURL}, new URL[]{oldURL}, new URL[0]);
        FileLock lock = f3.lock();
        try {
            FileObject f = f3.move(lock, f3.getParent(), "newName", "emb");
        } finally {
            lock.releaseLock();
        }
        assertTrue(indexerFactory.indexer.awaitDeleted());
        assertTrue(eindexerFactory.indexer.awaitDeleted());
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        assertEquals(1, eindexerFactory.indexer.getIndexCount());
        assertEquals(1, eindexerFactory.indexer.getDeletedCount());
        assertEquals(0, eindexerFactory.indexer.getDirtyCount());
        assertEquals(0, indexerFactory.indexer.getIndexCount());
        assertEquals(1, indexerFactory.indexer.getDeletedCount());
        assertEquals(0, indexerFactory.indexer.getDirtyCount());
    }

    private void touchFile (final File file) throws IOException {
        OutputStream out = new FileOutputStream (file);
        out.close();
    }

    public void testFileListWork164622() throws FileStateInvalidException {
        final RepositoryUpdater ru = RepositoryUpdater.getDefault();
        RepositoryUpdater.FileListWork flw1 = new RepositoryUpdater.FileListWork(ru.getScannedRoots2Dependencies(),srcRootWithFiles1.getURL(), false, false, true, false);
        RepositoryUpdater.FileListWork flw2 = new RepositoryUpdater.FileListWork(ru.getScannedRoots2Dependencies(),srcRootWithFiles1.getURL(), false, false, true, false);
        assertTrue("The flw2 job was not absorbed", flw1.absorb(flw2));

        FileObject [] children = srcRootWithFiles1.getChildren();
        assertTrue(children.length > 0);
        RepositoryUpdater.FileListWork flw3 = new RepositoryUpdater.FileListWork(ru.getScannedRoots2Dependencies(),srcRootWithFiles1.getURL(), Collections.singleton(children[0]), false, false, true, false, true);
        assertTrue("The flw3 job was not absorbed", flw1.absorb(flw3));

        RepositoryUpdater.FileListWork flw4 = new RepositoryUpdater.FileListWork(ru.getScannedRoots2Dependencies(),srcRoot1.getURL(), false, false, true, false);
        assertFalse("The flw4 job should not have been absorbed", flw1.absorb(flw4));
    }
    
    public void testScanStartScanFinishedCalled() throws Exception {
        indexerFactory.scanStartedFor.clear();
        indexerFactory.scanFinishedFor.clear();
        eindexerFactory.scanStartedFor.clear();
        eindexerFactory.scanFinishedFor.clear();
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        indexerFactory.indexer.setExpectedFile(customFiles, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);
        MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRootWithFiles1);
        ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        assertEquals(1, indexerFactory.scanStartedFor.size());
        assertEquals(srcRootWithFiles1.getURL(), indexerFactory.scanStartedFor.get(0));
        assertEquals(1, indexerFactory.scanFinishedFor.size());
        assertEquals(srcRootWithFiles1.getURL(), indexerFactory.scanFinishedFor.get(0));
        assertEquals(1, eindexerFactory.scanStartedFor.size());
        assertEquals(srcRootWithFiles1.getURL(), eindexerFactory.scanStartedFor.get(0));
        assertEquals(1, eindexerFactory.scanFinishedFor.size());
        assertEquals(srcRootWithFiles1.getURL(), eindexerFactory.scanFinishedFor.get(0));


        indexerFactory.scanStartedFor.clear();
        indexerFactory.scanFinishedFor.clear();
        eindexerFactory.scanStartedFor.clear();
        eindexerFactory.scanFinishedFor.clear();
        handler.reset();
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0],new URL[0], new URL[0]);
        mcpi1.addResource(srcRoot1);
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRoot1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        assertEquals(1, indexerFactory.scanStartedFor.size());
        assertEquals(srcRoot1.getURL(), indexerFactory.scanStartedFor.get(0));
        assertEquals(1, indexerFactory.scanFinishedFor.size());
        assertEquals(srcRoot1.getURL(), indexerFactory.scanFinishedFor.get(0));
        assertEquals(1, eindexerFactory.scanStartedFor.size());
        assertEquals(srcRoot1.getURL(), eindexerFactory.scanStartedFor.get(0));
        assertEquals(1, eindexerFactory.scanFinishedFor.size());
        assertEquals(srcRoot1.getURL(), eindexerFactory.scanFinishedFor.get(0));



        indexerFactory.scanStartedFor.clear();
        indexerFactory.scanFinishedFor.clear();
        eindexerFactory.scanStartedFor.clear();
        eindexerFactory.scanFinishedFor.clear();
        handler.reset();
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0],new URL[0], new URL[0]);
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1});
        //Give RU a time for refresh - nothing to wait for
        Thread.sleep(2000);
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());
        assertEquals(0, indexerFactory.indexer.getIndexCount());
        assertEquals(0, eindexerFactory.indexer.getIndexCount());
        assertEquals(0, indexerFactory.scanStartedFor.size());
        assertEquals(0, indexerFactory.scanFinishedFor.size());
        assertEquals(0, eindexerFactory.scanStartedFor.size());
        assertEquals(0, eindexerFactory.scanFinishedFor.size());

        handler.reset();
        indexerFactory.scanStartedFor.clear();
        indexerFactory.scanFinishedFor.clear();
        eindexerFactory.scanStartedFor.clear();
        eindexerFactory.scanFinishedFor.clear();
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(2, handler.getSources().size());
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        assertEquals(2, indexerFactory.scanStartedFor.size());
        assertEquals(new URL[] {srcRoot1.getURL(), srcRootWithFiles1.getURL()}, indexerFactory.scanStartedFor);
        assertEquals(2, indexerFactory.scanFinishedFor.size());
        assertEquals(new URL[] {srcRoot1.getURL(), srcRootWithFiles1.getURL()}, indexerFactory.scanFinishedFor);
        assertEquals(2, eindexerFactory.scanStartedFor.size());
        assertEquals(new URL[] {srcRoot1.getURL(), srcRootWithFiles1.getURL()}, eindexerFactory.scanStartedFor);
        assertEquals(2, eindexerFactory.scanFinishedFor.size());
        assertEquals(new URL[] {srcRoot1.getURL(), srcRootWithFiles1.getURL()}, eindexerFactory.scanFinishedFor);
    }

    public void testBinaryScanStartFinishedCalled() throws Exception {
        binIndexerFactory.scanStartedFor.clear();
        binIndexerFactory.scanFinishedFor.clear();
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        binIndexerFactory.indexer.setExpectedRoots(bootRoot2.getURL());
        MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(bootRoot2);
        ClassPath cp = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(PLATFORM,new ClassPath[] {cp});
        assertTrue (handler.await());
        assertEquals(1, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());
        assertEquals(bootRoot2.getURL(), handler.getBinaries().iterator().next());
        assertTrue(binIndexerFactory.indexer.await());
        assertEquals(1, binIndexerFactory.scanStartedFor.size());
        assertEquals(bootRoot2.getURL(), binIndexerFactory.scanStartedFor.get(0));
        assertEquals(1, binIndexerFactory.scanFinishedFor.size());
        assertEquals(bootRoot2.getURL(), binIndexerFactory.scanFinishedFor.get(0));

        binIndexerFactory.scanStartedFor.clear();
        binIndexerFactory.scanFinishedFor.clear();
        handler.reset();
        binIndexerFactory.indexer.setExpectedRoots(bootRoot3.getURL());
        mcpi1.addResource(bootRoot3);
        assertTrue (handler.await());
        assertEquals(1, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());
        assertEquals(bootRoot3.getURL(), handler.getBinaries().iterator().next());
        assertTrue(binIndexerFactory.indexer.await());
        assertEquals(1, binIndexerFactory.scanStartedFor.size());
        assertEquals(bootRoot3.getURL(), binIndexerFactory.scanStartedFor.get(0));
        assertEquals(1, binIndexerFactory.scanFinishedFor.size());
        assertEquals(bootRoot3.getURL(), binIndexerFactory.scanFinishedFor.get(0));

        binIndexerFactory.scanStartedFor.clear();
        binIndexerFactory.scanFinishedFor.clear();
        handler.reset();
        binIndexerFactory.indexer.setExpectedRoots(new URL[0]);
        globalPathRegistry_unregister(PLATFORM,new ClassPath[]{cp});
        //Give RU a time for refresh - nothing to wait for
        Thread.sleep(2000);
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());
        assertEquals(0, binIndexerFactory.indexer.getCount());
        assertEquals(0, binIndexerFactory.scanStartedFor.size());
        assertEquals(0, binIndexerFactory.scanFinishedFor.size());

        binIndexerFactory.scanStartedFor.clear();
        binIndexerFactory.scanFinishedFor.clear();
        handler.reset();
        binIndexerFactory.indexer.setExpectedRoots(bootRoot2.getURL(), bootRoot3.getURL());
        globalPathRegistry_register(PLATFORM,new ClassPath[]{cp});
        assertTrue (handler.await());
        assertEquals(2, handler.getBinaries().size());
        assertEquals(0, handler.getSources().size());
        assertTrue(binIndexerFactory.indexer.await());
        assertEquals(2, binIndexerFactory.scanStartedFor.size());
        assertEquals(new URL[] {bootRoot2.getURL(), bootRoot3.getURL()}, binIndexerFactory.scanStartedFor);
        assertEquals(2, binIndexerFactory.scanFinishedFor.size());
        assertEquals(new URL[] {bootRoot2.getURL(), bootRoot3.getURL()}, binIndexerFactory.scanFinishedFor);
    }

    //where
    private void assertEquals(final URL[] expected, final Collection<URL> data) throws AssertionError {
        assertEquals(expected.length, data.size());
        final Set<URL> expectedSet = new HashSet<URL>(Arrays.asList(expected));
        final Set<URL> dataSet = new HashSet<URL>(data);
        assertEquals(expectedSet.size(), dataSet.size());
        for (URL url : dataSet) {
            assertTrue(expectedSet.remove(url));
        }
    }

    public void testIssue171719() throws Exception {
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        indexerFactory.indexer.setExpectedFile(customFiles, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);
        MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRootWithFiles1);
        ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        
        File root = FileUtil.toFile(srcRootWithFiles1);
        File fdf = new File (root, "direct.emb");   //NOI18N
        eindexerFactory.indexer.setExpectedFile(new URL[]{fdf.toURI().toURL()}, new URL[0], new URL[0]);
        FileObject df = FileUtil.createData(fdf);
        assertNotNull(df);
        assertEquals(EMIME, df.getMIMEType());
        eindexerFactory.indexer.awaitIndex();

        File newfdf = new File (root, "new_direct.emb");   //NOI18N
        eindexerFactory.indexer.setExpectedFile(new URL[]{newfdf.toURI().toURL()}, new URL[]{fdf.toURI().toURL()}, new URL[0]);
        FileLock lock = df.lock();
        try {
            df.rename(lock, "new_direct", "emb");
        } finally {
            lock.releaseLock();
        }
        eindexerFactory.indexer.awaitIndex();
        eindexerFactory.indexer.awaitDeleted();
        assertFalse(eindexerFactory.indexer.broken);
    }

    public void testAWTIndexAndWaitDeadlock() throws Exception {
        final Class<EventSupport.EditorRegistryListener> erlc = EventSupport.EditorRegistryListener.class;        
        final Field k24Field = erlc.getDeclaredField("k24");   //NOI18N
        assertNotNull (k24Field);
        k24Field.setAccessible(true);
        final AtomicBoolean cond = (AtomicBoolean) k24Field.get(null);
        
        final Source source = Source.create(f3);
        assertNotNull(source);

        Runnable action = new Runnable() {
            public void run() {
                try {
                    TaskProcessor.resetState(source, false, true);
                    cond.set(true);
                } catch (/*ReflectiveOperation*/Exception e) {
                    Exceptions.printStackTrace(e);
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        }
        else {
            SwingUtilities.invokeAndWait(action);
        }

        action = new Runnable() {
            public void run() {
                try {
                    IndexingManager.getDefault().refreshIndexAndWait(srcRootWithFiles1.getURL(), null);
                } catch (FileStateInvalidException e) {
                    Exceptions.printStackTrace(e);
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        }
        else {
            SwingUtilities.invokeAndWait(action);
        }


        action = new Runnable() {
            public void run() {
                try {
                    cond.set(false);
                    TaskProcessor.resetStateImpl(source);
                } catch (/*ReflectiveOperation*/Exception e) {
                    Exceptions.printStackTrace(e);
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        }
        else {
            SwingUtilities.invokeAndWait(action);
        }

    }

    public void testFileListWorkVsRefreshWork() throws IOException {
        File root1 = new File(getWorkDir(), "root1");
        {
        RepositoryUpdater.FileListWork flw = new RepositoryUpdater.FileListWork(Collections.<URL, List<URL>>emptyMap(), root1.toURL(), false, false, false, true);
        RepositoryUpdater.RefreshWork rw = new RepositoryUpdater.RefreshWork(
                Collections.<URL, List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL>emptySet(),
                false,
                false,
                null,
                new RepositoryUpdater.FSRefreshInterceptor());
        assertTrue("RefreshWork didn't absorb FileListWork", rw.absorb(flw));
        }
        {
        RepositoryUpdater.FileListWork flw = new RepositoryUpdater.FileListWork(Collections.<URL, List<URL>>emptyMap(), root1.toURL(), false, false, true, true);
        RepositoryUpdater.RefreshWork rw = new RepositoryUpdater.RefreshWork(
                Collections.<URL, List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL>emptySet(),
                true,
                false,
                null,
                new RepositoryUpdater.FSRefreshInterceptor());
        assertTrue("RefreshWork didn't absorb FileListWork", rw.absorb(flw));
        }
        {
        RepositoryUpdater.FileListWork flw = new RepositoryUpdater.FileListWork(Collections.<URL, List<URL>>emptyMap(), root1.toURL(), false, false, false, true);
        RepositoryUpdater.RefreshWork rw = new RepositoryUpdater.RefreshWork(
                Collections.<URL, List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL>emptySet(),
                true,
                false,
                null,
                new RepositoryUpdater.FSRefreshInterceptor());
        assertTrue("RefreshWork didn't absorb FileListWork", rw.absorb(flw));
        }
        {
        RepositoryUpdater.FileListWork flw = new RepositoryUpdater.FileListWork(Collections.<URL, List<URL>>emptyMap(), root1.toURL(), false, false, true, true);
        RepositoryUpdater.RefreshWork rw = new RepositoryUpdater.RefreshWork(
                Collections.<URL, List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL>emptySet(),
                false,
                false,
                null,
                new RepositoryUpdater.FSRefreshInterceptor());
        assertTrue("RefreshWork didn't absorb FileListWork", rw.absorb(flw));
        }
    }

    public void testRefreshWork() throws IOException {
        File root1 = new File(getWorkDir(), "root1");
        File root2 = new File(getWorkDir(), "root2");
        {
        RepositoryUpdater.RefreshWork rw1 = new RepositoryUpdater.RefreshWork(
                Collections.<URL, List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL>emptySet(),
                false,
                false,
                Collections.singleton(root1),
                new RepositoryUpdater.FSRefreshInterceptor());
        RepositoryUpdater.RefreshWork rw2 = new RepositoryUpdater.RefreshWork(
                Collections.<URL, List<URL>>emptyMap(),
                Collections.<URL,List<URL>>emptyMap(),
                Collections.<URL, List<URL>>emptyMap(),
                Collections.<URL>emptySet(),
                false,
                false,
                Collections.singleton(root2),
                new RepositoryUpdater.FSRefreshInterceptor());
        assertFalse("RefreshWork should not be cancelled by other RefreshWork", rw1.isCancelledBy(rw2, new ArrayList<RepositoryUpdater.Work>()));
        assertTrue("RefreshWork should absorb other RefreshWork", rw1.absorb(rw2));
        }
    }

    @RandomlyFails
    public void testMissedChanges() throws Exception {
        
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        indexerFactory.indexer.setExpectedFile(customFiles, new URL[0], new URL[0]);
        indexerFactory.indexer.setCallBack(new Runnable() {
            public void run () {
                try {
                    final OutputStream out = f1.getOutputStream();
                    try {
                        out.write("Buena Suerte".getBytes());
                    } finally {
                        out.close();
                    }
                } catch (IOException e) {
                    Exceptions.printStackTrace(e);
                }
            }
        });
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);


        MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();        
        mcpi1.addResource(srcRootWithFiles1);
        ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});

        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());

        indexerFactory.indexer.setExpectedFile(new URL[]{f1.getURL()}, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
    }
    
    public void testFileChangedInEditorReparsedOnce191885() throws Exception {
        //Prepare
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRootWithFiles1);
        ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        final Source src = Source.create(f1);
        assertNotNull(src);
        assertFalse ("Created source should be valid", SourceAccessor.getINSTANCE().testFlag(src, SourceFlags.INVALID));
        RepositoryUpdater.unitTestActiveSource = src;
        try {
            IndexingManager.getDefault().refreshIndexAndWait(this.srcRootWithFiles1.getURL(),
                    Collections.singleton(f1.getURL()));
            assertFalse("Active shource should not be invalidated",SourceAccessor.getINSTANCE().testFlag(src, SourceFlags.INVALID));
            
        } finally {
            RepositoryUpdater.unitTestActiveSource=null;
        }
        IndexingManager.getDefault().refreshIndexAndWait(this.srcRootWithFiles1.getURL(),
                    Collections.singleton(f1.getURL()));
        assertTrue("Non active shource should be invalidated",SourceAccessor.getINSTANCE().testFlag(src, SourceFlags.INVALID));
    }
    
    public void testFilesScannedAfterRenameOfFolder193243() throws Exception {
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        final FileObject testFo = FileUtil.createData(this.srcRootWithFiles1, "rename/A.foo");
        final MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRootWithFiles1);
        final ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());       
        indexerFactory.indexer.setExpectedFile(new URL[]{new URL(this.srcRootWithFiles1.getURL()+"renamed/A.foo")}, new URL[]{testFo.getURL()}, new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0], new URL[]{testFo.getURL()}, new URL[0]);
        final FileObject parent = testFo.getParent();
        final FileLock lock = parent.lock();
        try {
            parent.rename(lock, "renamed", null);
        } finally {
            lock.releaseLock();
        }
        assertTrue(indexerFactory.indexer.awaitDeleted());
        assertTrue(eindexerFactory.indexer.awaitDeleted());
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        assertEquals(0, eindexerFactory.indexer.getIndexCount());
        assertEquals(1, eindexerFactory.indexer.getDeletedCount());
        assertEquals(0, eindexerFactory.indexer.getDirtyCount());
        assertEquals(1, indexerFactory.indexer.getIndexCount());
        assertEquals(1, indexerFactory.indexer.getDeletedCount());
        assertEquals(0, indexerFactory.indexer.getDirtyCount());
    }
    
    public void testFilesScannedAferSourceRootCreatedDeleted() throws Exception {
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        final File srcRoot1File = FileUtil.toFile(srcRoot1);                
        final ClassPath cp1 = ClassPathSupport.createClassPath(FileUtil.urlForArchiveOrDir(srcRoot1File));
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());       
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);        
        assertTrue(indexerFactory.indexer.awaitDeleted());
        assertTrue(eindexerFactory.indexer.awaitDeleted());
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        srcRoot1.delete();
        
        final File a = new File(srcRoot1File,"folder/a.foo");
        final File b = new File(srcRoot1File,"folder/b.emb");
        indexerFactory.indexer.setExpectedFile(new URL[]{a.toURI().toURL()}, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[]{b.toURI().toURL()}, new URL[0], new URL[0]);
        FileUtil.runAtomicAction(new FileSystem.AtomicAction() {
            @Override
            public void run() throws IOException {
                FileUtil.createFolder(srcRoot1File);
                FileUtil.createData(a);
                FileUtil.createData(b);
            }
        });
        assertTrue(indexerFactory.indexer.awaitDeleted());
        assertTrue(eindexerFactory.indexer.awaitDeleted());
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
    }
    
    public void testIncludesChanges() throws Exception {
        //Prepare
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        MutableFilteringResourceImpl r = new MutableFilteringResourceImpl(srcRootWithFiles1.getURL());
        ClassPath cp1 = ClassPathSupport.createClassPath(Collections.singletonList(r));
        ClassPathProviderImpl.register(srcRootWithFiles1, SOURCES, cp1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());

        //exclude a file:
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[] {f1.getURL()}, new URL[0]);
        r.setExcludes(".*/a\\.foo");

        indexerFactory.indexer.awaitDeleted();
        indexerFactory.indexer.awaitIndex();

        assertEquals(1, indexerFactory.indexer.deletedCounter);
        assertEquals(Collections.emptySet(), indexerFactory.indexer.expectedDeleted);
        assertEquals(0, indexerFactory.indexer.indexCounter);

        //include the file back:
        indexerFactory.indexer.setExpectedFile(new URL[] {f1.getURL()}, new URL[0], new URL[0]);
        r.setExcludes();

        indexerFactory.indexer.awaitDeleted();
        indexerFactory.indexer.awaitIndex();

        assertEquals(0, indexerFactory.indexer.deletedCounter);
        assertEquals(1, indexerFactory.indexer.indexCounter);
        assertEquals(Collections.emptySet(), indexerFactory.indexer.expectedIndex);
    }
    
    public void testPeerRootsCalculation() throws Exception {
        //Prepare
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        ClassPathProviderImpl.reset();
        
        //Register src path {srcRoot1, srcRootWithFiles1}
        handler.reset(TestHandler.Type.ROOTS_WORK_FINISHED);
        final MutableClassPathImplementation mcpi = new MutableClassPathImplementation ();
        mcpi.addResource(this.srcRoot1);
        mcpi.addResource(this.srcRootWithFiles1);
        final ClassPath cp = ClassPathFactory.createClassPath(mcpi);
        ClassPathProviderImpl.register(srcRoot1, SOURCES, cp);
        ClassPathProviderImpl.register(srcRoot2, SOURCES, cp);
        ClassPathProviderImpl.register(srcRootWithFiles1, SOURCES, cp);        
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp});
        assertTrue (handler.await());
        Map<URL,List<URL>> peersMap = IndexingController.getDefault().getRootPeers();
        assertEquals(2, peersMap.size());
        List<URL> peers = peersMap.get(srcRoot1.getURL());       
        assertEquals(Collections.singletonList(srcRootWithFiles1.getURL()), peers);
        peers = peersMap.get(srcRootWithFiles1.getURL());       
        assertEquals(Collections.singletonList(srcRoot1.getURL()), peers);
        
        //Remove srcRootWithFiles1 from src path
        handler.reset(TestHandler.Type.ROOTS_WORK_FINISHED);
        mcpi.removeResource(this.srcRootWithFiles1);
        assertTrue (handler.await());
        peersMap = IndexingController.getDefault().getRootPeers();
        assertEquals(1, peersMap.size());
        peers = peersMap.get(srcRoot1.getURL());       
        assertEquals(Collections.<URL>emptyList(), peers);        
        
        //Readd the srcRootWithFiles1 to src path
        handler.reset(TestHandler.Type.ROOTS_WORK_FINISHED);
        mcpi.addResource(this.srcRootWithFiles1);
        assertTrue (handler.await());
        peersMap = IndexingController.getDefault().getRootPeers();
        assertEquals(2, peersMap.size());
        peers = peersMap.get(srcRoot1.getURL());       
        assertEquals(Collections.singletonList(srcRootWithFiles1.getURL()), peers);
        peers = peersMap.get(srcRootWithFiles1.getURL());       
        assertEquals(Collections.singletonList(srcRoot1.getURL()), peers);
                
        //Add srcRoot2 to src path
        handler.reset(TestHandler.Type.ROOTS_WORK_FINISHED);
        mcpi.addResource(this.srcRoot2);
        assertTrue (handler.await());
        peersMap = IndexingController.getDefault().getRootPeers();
        assertEquals(3, peersMap.size());
        List<URL> returnedPeers = new ArrayList<URL> (peersMap.get(srcRoot1.getURL()));
        Collections.sort(returnedPeers, new URLComparator());
        List<URL> expectedPeers = new ArrayList<URL> () {{add(srcRootWithFiles1.getURL()); add(srcRoot2.getURL());}};
        Collections.sort(expectedPeers, new URLComparator());
        assertEquals(expectedPeers, returnedPeers);        
        returnedPeers = new ArrayList<URL> (peersMap.get(srcRoot2.getURL()));
        Collections.sort(returnedPeers, new URLComparator());
        expectedPeers = new ArrayList<URL> () {{add(srcRootWithFiles1.getURL()); add(srcRoot1.getURL());}};
        Collections.sort(expectedPeers, new URLComparator());
        assertEquals(expectedPeers, returnedPeers);        
        returnedPeers = new ArrayList<URL> (peersMap.get(srcRootWithFiles1.getURL()));
        Collections.sort(returnedPeers, new URLComparator());
        expectedPeers = new ArrayList<URL> () {{add(srcRoot1.getURL()); add(srcRoot2.getURL());}};
        Collections.sort(expectedPeers, new URLComparator());
        assertEquals(expectedPeers, returnedPeers);
        
        //Remove srcRoot2 from src path
        handler.reset(TestHandler.Type.ROOTS_WORK_FINISHED);
        mcpi.removeResource(this.srcRoot2);
        assertTrue (handler.await());
        peersMap = IndexingController.getDefault().getRootPeers();
        assertEquals(2, peersMap.size());
        peers = peersMap.get(srcRoot1.getURL());       
        assertEquals(Collections.singletonList(srcRootWithFiles1.getURL()), peers);
        peers = peersMap.get(srcRootWithFiles1.getURL());       
        assertEquals(Collections.singletonList(srcRoot1.getURL()), peers);
        
        //Remove srcRootWithFiles1
        handler.reset(TestHandler.Type.ROOTS_WORK_FINISHED);
        mcpi.removeResource(this.srcRootWithFiles1);
        assertTrue (handler.await());
        peersMap = IndexingController.getDefault().getRootPeers();
        assertEquals(1, peersMap.size());
        peers = peersMap.get(srcRoot1.getURL());       
        assertEquals(Collections.<URL>emptyList(), peers);        
               
    }
    
    public void testCheckAllFiles() throws Exception {
        RepositoryUpdater ru = RepositoryUpdater.getDefault();
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedUnknowns().size());

        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);

        //1st) Source root seen for first time (allFiles should be true)
        indexerFactory.indexer.setExpectedFile(customFiles, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);
        final MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRootWithFiles1);
        final ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        Map<URL,Pair<Boolean,Boolean>> contextState = indexerFactory.indexer.getContextState();
        assertEquals(1, contextState.size());
        Pair<Boolean,Boolean> state = contextState.get(this.srcRootWithFiles1.getURL());
        assertNotNull(state);
        assertTrue(state.first);
        assertFalse(state.second);
        contextState = eindexerFactory.indexer.getContextState();
        assertEquals(embeddedFiles.length, contextState.size());
        for (URL url : embeddedFiles) {
            state = contextState.get(url);
            assertNotNull(state);
            assertTrue(state.first);
            assertFalse(state.second);
        }

        //2nd) Clean up - unregister
        handler.reset();
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedUnknowns().size());


        //3rd) Source root seen for second time (allFiles should be false)
        indexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[0], new URL[0], new URL[0]);
        handler.reset();
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        contextState = indexerFactory.indexer.getContextState();
        assertEquals(1, contextState.size());
        state = contextState.get(this.srcRootWithFiles1.getURL());
        assertNotNull(state);
        assertFalse(state.first);
        assertFalse(state.second);
        contextState = eindexerFactory.indexer.getContextState();
        assertEquals(0, contextState.size());

        //4th) Clean up - unregister
        handler.reset();
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedUnknowns().size());


        //5th Do some modification and reopen the project (allFiles should be false)
        fsWait();
        touch(customFiles[0]);
        touch(embeddedFiles[0]);
        indexerFactory.indexer.setExpectedFile(new URL[] {customFiles[0]}, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[] {embeddedFiles[0]}, new URL[0], new URL[0]);
        handler.reset();
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        contextState = indexerFactory.indexer.getContextState();
        assertEquals(1, contextState.size());
        state = contextState.get(this.srcRootWithFiles1.getURL());
        assertNotNull(state);
        assertFalse(state.first);
        assertFalse(state.second);
        contextState = eindexerFactory.indexer.getContextState();
        assertEquals(1, contextState.size());
        state = contextState.get(embeddedFiles[0]);
        assertNotNull(state);
        assertFalse(state.first);
        assertFalse(state.second);

        //6th Do some modification when source are registered (allFiles should be false)
        indexerFactory.indexer.setExpectedFile(new URL[] {customFiles[0]}, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[] {embeddedFiles[0]}, new URL[0], new URL[0]);
        fsWait();
        touch(customFiles[0]);
        touch(embeddedFiles[0]);
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        contextState = indexerFactory.indexer.getContextState();
        assertEquals(1, contextState.size());
        state = contextState.get(this.srcRootWithFiles1.getURL());
        assertNotNull(state);
        assertFalse(state.first);
        assertFalse(state.second);
        contextState = eindexerFactory.indexer.getContextState();
        assertEquals(1, contextState.size());
        state = contextState.get(embeddedFiles[0]);
        assertNotNull(state);
        assertFalse(state.first);
        assertFalse(state.second);
        
        //7th IndexingManager.refreshIndex(root, all_files, fullRescan==true) (allFiles should be true)
        indexerFactory.indexer.setExpectedFile(customFiles, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);
        IndexingManager.getDefault().refreshIndex(srcRootWithFiles1.getURL(), Collections.<URL>emptySet(), true);
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        contextState = indexerFactory.indexer.getContextState();
        assertEquals(1, contextState.size());
        state = contextState.get(this.srcRootWithFiles1.getURL());
        assertNotNull(state);
        assertTrue(state.first);
        assertFalse(state.second);
        contextState = eindexerFactory.indexer.getContextState();
        assertEquals(embeddedFiles.length, contextState.size());
        for (URL url : embeddedFiles) {
            state = contextState.get(url);
            assertNotNull(state);
            assertTrue(state.first);
            assertFalse(state.second);
        }

        //8th IndexingManager.refreshIndex(root, specifoc_file, fullRescan==true, checkEditor==true) (allFiles should be false, checkForEditorModifications should be true)
        indexerFactory.indexer.setExpectedFile(new URL[] {customFiles[0]}, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(new URL[] {embeddedFiles[0]}, new URL[0], new URL[0]);
        IndexingManager.getDefault().refreshIndex(srcRootWithFiles1.getURL(), Arrays.asList(new URL[] {customFiles[0], embeddedFiles[0]}), true, true);
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        contextState = indexerFactory.indexer.getContextState();
        assertEquals(1, contextState.size());
        state = contextState.get(this.srcRootWithFiles1.getURL());
        assertNotNull(state);
        assertFalse(state.first);
        assertTrue(state.second);
        contextState = eindexerFactory.indexer.getContextState();
        assertEquals(1, contextState.size());
        state = contextState.get(embeddedFiles[0]);
        assertNotNull(state);
        assertFalse(state.first);
        assertTrue(state.second);        
    }

    public void testVisibilityQueryAmongIDERestarts() throws Exception {
        //1st) Default visibility everything should be scanned
        final RepositoryUpdater ru = RepositoryUpdater.getDefault();
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedUnknowns().size());

        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);

        //2nd) Source root seen for first time (allFiles should be true)
        indexerFactory.indexer.setExpectedFile(customFiles, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);
        final MutableClassPathImplementation mcpi1 = new MutableClassPathImplementation ();
        mcpi1.addResource(this.srcRootWithFiles1);
        final ClassPath cp1 = ClassPathFactory.createClassPath(mcpi1);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());

        //3rd) Unregister - IDE closed
        handler.reset();
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedUnknowns().size());

        //4th) Making customFiles[1] invisible & touching the files to be indexed
        fsWait();
        Thread.sleep(5000);     //wait for filesystem
        touch (customFiles);
        touch (embeddedFiles);
        final Visibility visibility = Lookup.getDefault().lookup(Visibility.class);
        assertNotNull(visibility);
        visibility.registerInvisibles(Collections.singleton(URLMapper.findFileObject(customFiles[1])));
        handler.reset();
        indexerFactory.indexer.setExpectedFile(new URL[] {customFiles[0]}, new URL[] {customFiles[1]}, new URL[0]);
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
        assertTrue(indexerFactory.indexer.awaitDeleted());

        //5th) Unregister - IDE closed
        handler.reset();
        globalPathRegistry_unregister(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedBinaries().size());
        assertEquals(0, ru.getScannedUnknowns().size());
        
        //6th) Making customFiles[1] visible again & touching the files to be indexed
        fsWait();
        touch (customFiles);
        touch (embeddedFiles);
        visibility.registerInvisibles(Collections.<FileObject>emptySet());
        handler.reset();
        indexerFactory.indexer.setExpectedFile(customFiles, new URL[0], new URL[0]);
        eindexerFactory.indexer.setExpectedFile(embeddedFiles, new URL[0], new URL[0]);
        globalPathRegistry_register(SOURCES,new ClassPath[]{cp1});
        assertTrue (handler.await());
        assertEquals(0, handler.getBinaries().size());
        assertEquals(1, handler.getSources().size());
        assertEquals(this.srcRootWithFiles1.getURL(), handler.getSources().get(0));
        assertTrue(indexerFactory.indexer.awaitIndex());
        assertTrue(eindexerFactory.indexer.awaitIndex());
    }

    /**
     * Test that unknown source roots are registered in the scannedRoots2Dependencies with EMPTY_DEPS
     * and when the unknown root registers it's changed to regular dependencies.
     * When the root is unregistered it's removed from scannedRoots2Dependencies
     *
     * The test was also extended to test Issue 196985. Unknown root which becomes known was lost
     * @throws Exception
     */
    public void testIndexManagerRefreshIndexListensOnChanges_And_Issue196985() throws Exception {
        final File _wd = this.getWorkDir();
        final FileObject wd = FileUtil.toFileObject(_wd);
        final FileObject refreshedRoot = wd.createFolder("refreshedRoot");
        final RepositoryUpdater ru = RepositoryUpdater.getDefault();
        assertNotNull(refreshedRoot);
        assertFalse (ru.getScannedRoots2Dependencies().containsKey(refreshedRoot.getURL()));
        IndexingManager.getDefault().refreshIndexAndWait(refreshedRoot.getURL(), Collections.<URL>emptyList());
        assertSame(RepositoryUpdater.EMPTY_DEPS, ru.getScannedRoots2Dependencies().get(refreshedRoot.getURL()));
        //Register the root => EMPTY_DEPS changes to regular deps
        final TestHandler handler = new TestHandler();
        final Logger logger = Logger.getLogger(RepositoryUpdater.class.getName()+".tests");
        logger.setLevel (Level.FINEST);
        logger.addHandler(handler);
        final ClassPath cp = ClassPathSupport.createClassPath(refreshedRoot);
        handler.reset(RepositoryUpdaterTest.TestHandler.Type.ROOTS_WORK_FINISHED);
        globalPathRegistry_register(SOURCES, new ClassPath[]{cp});
        handler.await();
        assertNotNull(ru.getScannedRoots2Dependencies().get(refreshedRoot.getURL()));
        assertNotSame(RepositoryUpdater.EMPTY_DEPS, ru.getScannedRoots2Dependencies().get(refreshedRoot.getURL()));
        handler.reset(RepositoryUpdaterTest.TestHandler.Type.ROOTS_WORK_FINISHED);
        globalPathRegistry_unregister(SOURCES, new ClassPath[]{cp});
        handler.await();
        assertFalse(ru.getScannedRoots2Dependencies().containsKey(refreshedRoot.getURL()));
    }

    // <editor-fold defaultstate="collapsed" desc="Mock Services">
    public static class TestHandler extends Handler {

        public static enum Type {BATCH, DELETE, FILELIST, ROOTS_WORK_FINISHED};

        private Type type;
        private CountDownLatch latch;
        private List<URL> sources;
        private Set<URL> binaries;

        public TestHandler () {
            reset();
        }

        public void reset () {
            reset (Type.BATCH);
        }

        public void reset(final Type t) {
            sources = null;
            binaries = null;
            type = t;
            if (t == Type.BATCH) {
                latch = new CountDownLatch(2);
            }
            else {
                latch = new CountDownLatch(1);
            }
        }

        public void reset(final Type t, int initialCount) {
            sources = null;
            binaries = null;
            type = t;
            latch = new CountDownLatch(initialCount);
        }

        public boolean await () throws InterruptedException {
            return latch.await(TIME, TimeUnit.MILLISECONDS);
        }

        public Set<URL> getBinaries () {
            return this.binaries;
        }

        public List<URL> getSources() {
            return this.sources;
        }

        @Override
        public void publish(LogRecord record) {
            String msg = record.getMessage();
            if (type == Type.BATCH) {
                if ("scanBinary".equals(msg)) {
                    @SuppressWarnings("unchecked")
                    Set<URL> b = (Set<URL>) record.getParameters()[0];
                    binaries = b;
                    latch.countDown();
                }
                else if ("scanSources".equals(msg)) {
                    @SuppressWarnings("unchecked")
                    List<URL> s =(List<URL>) record.getParameters()[0];
                    sources = s;
                    latch.countDown();
                }
            } else if (type == Type.DELETE) {
                if ("delete".equals(msg)) {
                    latch.countDown();
                }
            } else if (type == Type.FILELIST) {
                if ("filelist".equals(msg)) {
                    latch.countDown();
                }
            } else if (type == Type.ROOTS_WORK_FINISHED) {
                if ("RootsWork-finished".equals(msg)) { //NOI18N
                    latch.countDown();
                }
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    public static class PRI implements FilteringPathResourceImplementation {

        private final URL root;
        private final PropertyChangeSupport support;

        public PRI (URL root) {
            this.root = root;
            this.support = new PropertyChangeSupport (this);
        }

        public boolean includes(URL root, String resource) {
            return true;
        }

        public URL[] getRoots() {
            return new URL[] {root};
        }

        public ClassPathImplementation getContent() {
            return null;
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            this.support.addPropertyChangeListener(listener);
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            this.support.removePropertyChangeListener(listener);
        }

        public void firePropertyChange (final Object propId) {
            PropertyChangeEvent event = new PropertyChangeEvent (this,FilteringPathResourceImplementation.PROP_INCLUDES,null,null);
            event.setPropagationId(propId);
            this.support.firePropertyChange(event);
        }
    }        
    

    public static class MutableClassPathImplementation implements ClassPathImplementation {

        private final List<PathResourceImplementation> res;
        private final PropertyChangeSupport support;

        public MutableClassPathImplementation () {
            res = new ArrayList<PathResourceImplementation> ();
            support = new PropertyChangeSupport (this);
        }

        public void addResource (FileObject... fos) throws IOException {
            synchronized (res) {
                for(FileObject f : fos) {
                    res.add(ClassPathSupport.createResource(f.getURL()));
                }
            }
            this.support.firePropertyChange(PROP_RESOURCES,null,null);
        }

        public void addFilteredResource (FileObject root, String... excludes) throws IOException {
            synchronized (res) {
                res.add(new SimpleFilteringResourceImpl(root.getURL(), excludes));
            }
        }

        public void removeResource (FileObject... fos) throws IOException {
            boolean fire = false;

            synchronized (res) {
                for(FileObject f : fos) {
                    URL url = f.getURL();
                    for (Iterator<PathResourceImplementation> it = res.iterator(); it.hasNext(); ) {
                        PathResourceImplementation r = it.next();
                        if (url.equals(r.getRoots()[0])) {
                            it.remove();
                            fire = true;
                        }
                    }
                }
            }

            if (fire) {
                this.support.firePropertyChange(PROP_RESOURCES, null, null);
            }
        }

        public void removePropertyChangeListener(PropertyChangeListener listener) {
            support.removePropertyChangeListener(listener);
        }

        public void addPropertyChangeListener(PropertyChangeListener listener) {
            support.addPropertyChangeListener(listener);
        }

        public List<? extends PathResourceImplementation> getResources() {
            synchronized (res) {
                return new LinkedList<PathResourceImplementation>(res);
            }
        }

    }

    private static final class SimpleFilteringResourceImpl extends PathResourceBase implements FilteringPathResourceImplementation {

        private final URL[] url;
        private final Pattern[] excludes;

        public SimpleFilteringResourceImpl(URL root, String... excludes) {
            this.url = new URL[] {root};
            this.excludes = new Pattern[excludes.length];
            for(int i = 0; i < excludes.length; i++) {
                this.excludes[i] = Pattern.compile(excludes[i]);
            }
        }

        public URL[] getRoots() {
            return this.url;
        }

        public ClassPathImplementation getContent() {
            return null;
        }

        public boolean includes(URL root, String resource) {
            for(Pattern e : excludes) {
                if (e.matcher(resource).matches()) {
                    System.out.println("'" + resource + "' matches exclude pattern " + e.pattern());
                    return false;
                }
            }
            System.out.println("'" + resource + "' matches no exclude pattern");
            return true;
        }

        @Override
        public String toString () {
            return "FilteringResourceImpl{" + this.getRoots()[0] + "}";   //NOI18N
        }

        @Override
        public int hashCode () {
            int hash = this.url[0].hashCode();
            for(Pattern e : excludes) {
                hash += 7 * e.pattern().hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals (Object other) {
            if (other instanceof SimpleFilteringResourceImpl) {
                SimpleFilteringResourceImpl opr = (SimpleFilteringResourceImpl) other;
                if (this.url[0].equals(opr.url[0])) {
                    if (excludes.length == opr.excludes.length) {
                        for(int i = 0; i < excludes.length; i++) {
                            if (!excludes[i].pattern().equals(opr.excludes[i].pattern())) {
                                return false;
                            }
                        }

                        return true;
                    }
                }
            }

            return false;
        }
    }

    private static final class MutableFilteringResourceImpl extends PathResourceBase implements FilteringPathResourceImplementation {

        private final URL[] url;
        private final List<Pattern> excludes;

        public MutableFilteringResourceImpl(URL root) {
            this.url = new URL[] {root};
            this.excludes = new ArrayList<Pattern>();
        }

        public synchronized void setExcludes(String... patterns) {
            excludes.clear();

            for (String p : patterns) {
                excludes.add(Pattern.compile(p));
            }

            firePropertyChange(PROP_INCLUDES, null, null);
        }
        
        public URL[] getRoots() {
            return this.url;
        }

        public ClassPathImplementation getContent() {
            return null;
        }

        public synchronized boolean includes(URL root, String resource) {
            for(Pattern e : excludes) {
                if (e.matcher(resource).matches()) {
                    System.out.println("'" + resource + "' matches exclude pattern " + e.pattern());
                    return false;
                }
            }
            System.out.println("'" + resource + "' matches no exclude pattern");
            return true;
        }

        @Override
        public String toString () {
            return "FilteringResourceImpl{" + this.getRoots()[0] + "}";   //NOI18N
        }

        @Override
        public int hashCode () {
            int hash = this.url[0].hashCode();
            for(Pattern e : excludes) {
                hash += 7 * e.pattern().hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals (Object other) {
            if (other instanceof MutableFilteringResourceImpl) {
                MutableFilteringResourceImpl opr = (MutableFilteringResourceImpl) other;
                if (!this.url[0].equals(opr.url[0]) || !excludes.equals(opr.excludes)) {
                    return false;
                }
            }

            return false;
        }
    }

    public static class SFBQImpl implements SourceForBinaryQueryImplementation {

        final static Map<URL,FileObject> map = new HashMap<URL,FileObject> ();
        final static Map<URL,Result> results = new HashMap<URL,Result> ();

        public SFBQImpl () {

        }

        public static void register (FileObject binRoot, FileObject sourceRoot) throws IOException {
            URL url = binRoot.getURL();
            map.put (url,sourceRoot);
            Result r = results.get (url);
            if (r != null) {
                r.update (sourceRoot);
            }
        }

        public static void unregister (FileObject binRoot) throws IOException {
            URL url = binRoot.getURL();
            map.remove(url);
            Result r = results.get (url);
            if (r != null) {
                r.update (null);
            }
        }

        public static void clean () {
            map.clear();
            results.clear();
        }

        public SourceForBinaryQuery.Result findSourceRoots(URL binaryRoot) {
            FileObject srcRoot = map.get(binaryRoot);
            if (srcRoot == null) {
                return null;
            }
            Result r = results.get (binaryRoot);
            if (r == null) {
                r = new Result (srcRoot);
                results.put(binaryRoot, r);
            }
            return r;
        }
        
        public static class Result implements SourceForBinaryQuery.Result {

            private FileObject root;
            private final List<ChangeListener> listeners;

            public Result (FileObject root) {
                this.root = root;
                this.listeners = new LinkedList<ChangeListener> ();
            }

            public void update (FileObject root) {
                this.root = root;
                fireChange ();
            }

            public synchronized void addChangeListener(ChangeListener l) {
                this.listeners.add(l);
            }

            public synchronized void removeChangeListener(ChangeListener l) {
                this.listeners.remove(l);
            }

            public @Override FileObject[] getRoots() {
                if (this.root == null) {
                    return new FileObject[0];
                }
                else {
                    return new FileObject[] {this.root};
                }
            }

            private void fireChange () {
                ChangeListener[] _listeners;
                synchronized (this) {
                    _listeners = this.listeners.toArray(new ChangeListener[this.listeners.size()]);
                }
                ChangeEvent event = new ChangeEvent (this);
                for (ChangeListener l : _listeners) {
                    l.stateChanged (event);
                }
            }
        }

    }

    
    public static class FooPathRecognizer extends PathRecognizer {
        
        @Override
        public Set<String> getSourcePathIds() {
            return Collections.singleton(SOURCES);
        }

        @Override
        public Set<String> getBinaryLibraryPathIds() {
            final Set<String> res = new HashSet<String>();
            res.add(PLATFORM);
            res.add(LIBS);
            return res;
        }

        @Override
        public Set<String> getLibraryPathIds() {
            return null;
        }

        @Override
        public Set<String> getMimeTypes() {
            return Collections.singleton(MIME);
        }        

    }

    public static class EmbPathRecognizer extends PathRecognizer {

        @Override
        public Set<String> getSourcePathIds() {
            return Collections.singleton(SOURCES);
        }

        @Override
        public Set<String> getBinaryLibraryPathIds() {
            final Set<String> res = new HashSet<String>();
            res.add(PLATFORM);
            res.add(LIBS);
            return res;
        }

        @Override
        public Set<String> getLibraryPathIds() {
            return null;
        }

        @Override
        public Set<String> getMimeTypes() {
            return Collections.singleton(EMIME);
        }

    }

    public static class OpenProject implements  OpenProjectsTrampoline {

        public @Override Project[] getOpenProjectsAPI() {
            return new Project[0];
        }

        public @Override void openAPI(Project[] projects, boolean openRequiredProjects, boolean showProgress) {

        }

        public @Override void closeAPI(Project[] projects) {

        }

        public void addPropertyChangeListenerAPI(PropertyChangeListener listener, Object source) {
            
        }

        public Future<Project[]> openProjectsAPI() {
            return new Future<Project[]>() {

                public boolean cancel(boolean mayInterruptIfRunning) {
                    return true;
                }

                public boolean isCancelled() {
                    return false;
                }

                public boolean isDone() {
                    return true;
                }

                public Project[] get() throws InterruptedException, ExecutionException {
                    return new Project[0];
                }

                public Project[] get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                    return new Project[0];
                }
            };
        }

        public void removePropertyChangeListenerAPI(PropertyChangeListener listener) {
            
        }

        public @Override Project getMainProject() {
            return null;
        }

        public @Override void setMainProject(Project project) {
            
        }

    }

    private static class BinIndexerFactory extends BinaryIndexerFactory {

        private final BinIndexer indexer = new BinIndexer();
        final List<URL> scanStartedFor = new LinkedList<URL>();
        final List<URL> scanFinishedFor = new LinkedList<URL>();

        @Override
        public BinaryIndexer createIndexer() {
            return this.indexer;
        }

        @Override
        public void rootsRemoved(final Iterable<? extends URL> rr) {
            
        }

        @Override
        public String getIndexerName() {
            return "jar";
        }

        @Override
        public int getIndexVersion() {
            return 1;
        }

        @Override
        public boolean scanStarted(final Context ctx) {
            scanStartedFor.add(ctx.getRootURI());
            return true;
        }

        @Override
        public void scanFinished(final Context ctx) {
            scanFinishedFor.add(ctx.getRootURI());
        }
    }

    private static class BinIndexer extends BinaryIndexer {

        private Set<URL> expectedRoots = new HashSet<URL>();
        private CountDownLatch latch;
        private volatile int counter;

        public void setExpectedRoots (URL... roots) {
            expectedRoots.clear();
            expectedRoots.addAll(Arrays.asList(roots));
            counter = 0;
            latch = new CountDownLatch(expectedRoots.size());
        }

        public boolean await () throws InterruptedException {
            return this.latch.await(TIME, TimeUnit.MILLISECONDS);
        }

        public int getCount () {
            return this.counter;
        }

        @Override
        protected void index(Context context) {
            if (expectedRoots.remove(context.getRootURI())) {
                counter++;
                latch.countDown();
            }
        }
    }

    private static class FooIndexerFactory extends CustomIndexerFactory {

        final List<URL> scanStartedFor = new LinkedList<URL>();
        final List<URL> scanFinishedFor = new LinkedList<URL>();

        private final FooIndexer indexer = new FooIndexer();

        @Override
        public CustomIndexer createIndexer() {
            return this.indexer;
        }

        @Override
        public String getIndexerName() {
            return "foo";
        }

        @Override
        public int getIndexVersion() {
            return 1;
        }
        
        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
            for (Indexable i : deleted) {
                //System.out.println("FooIndexerFactory.filesDeleted: " + i.getURL());
                indexer.deletedCounter++;
                if (indexer.expectedDeleted.remove(i.getURL())) {
                    indexer.deletedFilesLatch.countDown();
                }
            }
        }

        @Override
        public void rootsRemoved(final Iterable<? extends URL> rr) {
            
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {
            for (Indexable i : dirty) {
                //System.out.println("FooIndexerFactory.filesDirty: " + i.getURL());
                indexer.dirtyCounter++;
                if (indexer.expectedDirty.remove(i.getURL())) {
                    indexer.dirtyFilesLatch.countDown();
                }
            }
        }

        @Override
        public boolean supportsEmbeddedIndexers() {
            return false;
        }

        @Override
        public boolean scanStarted(final Context ctx) {
            scanStartedFor.add(ctx.getRootURI());
            return true;
        }

        @Override
        public void scanFinished(final Context ctx) {
            scanFinishedFor.add(ctx.getRootURI());
        }
    }

    private static class FooIndexer extends CustomIndexer {

        private Set<URL> expectedIndex = new HashSet<URL>();
        private CountDownLatch indexFilesLatch;
        private CountDownLatch deletedFilesLatch;
        private CountDownLatch dirtyFilesLatch;
        private volatile int indexCounter;
        private volatile int deletedCounter;
        private volatile int dirtyCounter;
        private final Set<URL> expectedDeleted = new HashSet<URL>();
        private final Set<URL> expectedDirty = new HashSet<URL>();
        private final Map<URL,Pair<Boolean,Boolean>> contextState = new HashMap<URL, Pair<Boolean, Boolean>>();
        private Runnable callBack;

        public void setExpectedFile (URL[] files, URL[] deleted, URL[] dirty) {
            expectedIndex.clear();
            expectedIndex.addAll(Arrays.asList(files));
            expectedDeleted.clear();
            expectedDeleted.addAll(Arrays.asList(deleted));
            expectedDirty.clear();
            expectedDirty.addAll(Arrays.asList(dirty));
            contextState.clear();
            indexCounter = 0;
            deletedCounter = 0;
            dirtyCounter = 0;
            indexFilesLatch = new CountDownLatch(expectedIndex.size());
            deletedFilesLatch = new CountDownLatch(expectedDeleted.size());
            dirtyFilesLatch = new CountDownLatch(expectedDirty.size());
        }

        public void setCallBack (final Runnable callBack) {
            this.callBack = callBack;
        }

        public boolean awaitIndex() throws InterruptedException {
            return this.indexFilesLatch.await(TIME, TimeUnit.MILLISECONDS);
        }

        public boolean awaitDeleted() throws InterruptedException {
            return this.deletedFilesLatch.await(TIME, TimeUnit.MILLISECONDS);
        }

        public boolean awaitDirty() throws InterruptedException {
            return this.dirtyFilesLatch.await(TIME, TimeUnit.MILLISECONDS);
        }

        public int getIndexCount() {
            return this.indexCounter;
        }

        public int getDeletedCount() {
            return this.deletedCounter;
        }

        public int getDirtyCount() {
            return this.dirtyCounter;
        }

        public Map<URL,Pair<Boolean,Boolean>> getContextState() {
            return this.contextState;
        }

        @Override
        protected void index(Iterable<? extends Indexable> files, Context context) {
            contextState.put(context.getRootURI(),Pair.<Boolean,Boolean>of(context.isAllFilesIndexing(),context.checkForEditorModifications()));
            for (Indexable i : files) {
                indexCounter++;
                if (expectedIndex.remove(i.getURL())) {
                    indexFilesLatch.countDown();
                }
            }            
            if (callBack != null) {
                callBack.run();
                callBack = null;
            }
        }
    }

    private static class EmbIndexerFactory extends EmbeddingIndexerFactory {

        private EmbIndexer indexer = new EmbIndexer ();

        final List<URL> scanStartedFor = new LinkedList<URL>();
        final List<URL> scanFinishedFor = new LinkedList<URL>();

        @Override
        public EmbeddingIndexer createIndexer(final Indexable indexable, final Snapshot snapshot) {
            return indexer;
        }

        @Override
        public String getIndexerName() {
            return "emb";
        }

        @Override
        public int getIndexVersion() {
            return 1;
        }

        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
            for (Indexable i : deleted) {
                //System.out.println("EmbIndexerFactory.filesDeleted: " + i.getURL());
                indexer.deletedCounter++;
                if (indexer.expectedDeleted.remove(i.getURL())) {
                    final String relPath = i.getRelativePath();
                    if (relPath.charAt(0) == '/') {
                        indexer.broken = true;
                    }
                    indexer.deletedFilesLatch.countDown();
                }
            }
        }

        @Override
        public void rootsRemoved(final Iterable<? extends URL> removedRoots) {
            
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {
            for (Indexable i : dirty) {
                //System.out.println("EmbIndexerFactory.filesDirty: " + i.getURL());
                indexer.dirtyCounter++;
                if (indexer.expectedDirty.remove(i.getURL())) {
                    indexer.dirtyFilesLatch.countDown();
                }
            }
        }

        @Override
        public boolean scanStarted(final Context ctx) {
            scanStartedFor.add(ctx.getRootURI());
            return true;
        }

        @Override
        public void scanFinished(final Context ctx) {
            scanFinishedFor.add(ctx.getRootURI());
        }
    }

    private static class EmbIndexer extends EmbeddingIndexer {

        private Set<URL> expectedIndex = new HashSet<URL>();
        private CountDownLatch indexFilesLatch;
        private CountDownLatch deletedFilesLatch;
        private CountDownLatch dirtyFilesLatch;
        private volatile int indexCounter;
        private volatile int deletedCounter;
        private volatile int dirtyCounter;
        private Set<URL> expectedDeleted = new HashSet<URL>();
        private Set<URL> expectedDirty = new HashSet<URL>();
        private Map<URL,Pair<Boolean,Boolean>> contextState = new HashMap<URL, Pair<Boolean, Boolean>>();
        private boolean broken;

        public void setExpectedFile (URL[] files, URL[] deleted, URL[] dirty) {
            broken = false;
            expectedIndex.clear();
            expectedIndex.addAll(Arrays.asList(files));
            expectedDeleted.clear();
            expectedDeleted.addAll(Arrays.asList(deleted));
            expectedDirty.clear();
            expectedDirty.addAll(Arrays.asList(dirty));
            contextState.clear();
            indexCounter = 0;
            deletedCounter = 0;
            dirtyCounter = 0;
            indexFilesLatch = new CountDownLatch(expectedIndex.size());
            deletedFilesLatch = new CountDownLatch(expectedDeleted.size());
            dirtyFilesLatch = new CountDownLatch(expectedDirty.size());
        }

        public boolean awaitIndex() throws InterruptedException {
            return this.indexFilesLatch.await(TIME, TimeUnit.MILLISECONDS);
        }

        public boolean awaitDeleted() throws InterruptedException {
            return this.deletedFilesLatch.await(TIME, TimeUnit.MILLISECONDS);
        }

        public boolean awaitDirty() throws InterruptedException {
            return this.dirtyFilesLatch.await(TIME, TimeUnit.MILLISECONDS);
        }

        public int getIndexCount() {
            return this.indexCounter;
        }

        public int getDeletedCount() {
            return this.deletedCounter;
        }

        public int getDirtyCount() {
            return this.dirtyCounter;
        }

        public Map<URL,Pair<Boolean,Boolean>> getContextState() {
            return contextState;
        }

        @Override
        protected void index(Indexable indexable, Result parserResult, Context context) {
            try {
                final URL url = parserResult.getSnapshot().getSource().getFileObject().getURL();
                //System.out.println("EmbIndexer.index: " + url);
                indexCounter++;
                if (expectedIndex.remove(url)) {
                    contextState.put(url, Pair.<Boolean,Boolean>of(context.isAllFilesIndexing(),context.checkForEditorModifications()));
                    indexFilesLatch.countDown();
                }
            } catch (FileStateInvalidException ex) {
                Exceptions.printStackTrace(ex);
            }
        }        

    }

    private static class EmbParserFactory extends ParserFactory {

        @Override
        public Parser createParser(Collection<Snapshot> snapshots) {
            return new EmbParser();
        }

    }

    private static class EmbParser extends Parser {

        private EmbResult result;

        @Override
        public void parse(Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {            
            result = new EmbResult(snapshot);
        }

        @Override
        public Result getResult(Task task) throws ParseException {
            return result;
        }

        @Override
        public void cancel() {

        }

        @Override
        public void addChangeListener(ChangeListener changeListener) {

        }

        @Override
        public void removeChangeListener(ChangeListener changeListener) {

        }

    }

    private static class EmbResult extends Parser.Result {

        public EmbResult(final Snapshot snapshot) {
            super(snapshot);
        }


        @Override
        protected void invalidate() {
        }

    }

    public static final class ClassPathProviderImpl implements ClassPathProvider {

        private static final Map<Entry<FileObject, String>, ClassPath> classPathSpec = new HashMap<Entry<FileObject, String>, ClassPath>();
        
        public static void reset() {
            classPathSpec.clear();
        }

        public static void register(FileObject root, String type, ClassPath cp) {
            classPathSpec.put(new SimpleEntry<FileObject, String>(root, type), cp);
        }

        public static void unregister(FileObject root, String type) {
            classPathSpec.remove(new SimpleEntry<FileObject, String>(root, type));
        }

        @Override
        public ClassPath findClassPath(FileObject file, String type) {
            while (!file.isRoot()) {
                for (Entry<Entry<FileObject, String>, ClassPath> e : classPathSpec.entrySet()) {
                    if (e.getKey().getKey() == file && e.getKey().getValue().equals(type)) {
                        return e.getValue();
                    }
                }
                file = file.getParent();
            }

            return null;
        }

    }
    
    private static class URLComparator implements Comparator<URL> {

        @Override
        public int compare(URL o1, URL o2) {
            return o1.toExternalForm().compareTo(o2.toExternalForm());
        }
        
    }

    public static class Visibility implements VisibilityQueryImplementation2 {

        private final Collection<FileObject> invisible =
                Collections.synchronizedList( new ArrayList<FileObject>());

        private final ChangeSupport changeSupport = new ChangeSupport(this);

        public Visibility() {            
        }

        public void registerInvisibles (final Collection<? extends FileObject> invisibles) {
            synchronized (invisible) {
                invisible.clear();
                invisible.addAll(invisibles);
            }
            changeSupport.fireChange();
        }

        @Override
        public boolean isVisible(File file) {
            return isVisible(FileUtil.toFileObject(file));
        }

        @Override
        public boolean isVisible(FileObject file) {
            return ! invisible.contains(file);
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            changeSupport.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            changeSupport.removeChangeListener(l);
        }
    }

    private static void touch (final URL... urls) throws IOException {
        for(URL url : urls) {
            final FileObject fo = URLMapper.findFileObject(url);
            if (fo != null) {
                final OutputStream out = fo.getOutputStream();
                try {
                } finally {
                    out.close();
                }
            }
        }
    }

    /**
     * Waits for file system mtime changes
     */
    private static void fsWait() throws InterruptedException {
        Thread.sleep(3000);
    }

    //</editor-fold>
}
