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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.test.MockMimeLookup;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.parsing.api.indexing.IndexingManager;
import org.netbeans.modules.parsing.impl.indexing.*;
import org.netbeans.modules.parsing.impl.indexing.lucene.DocumentBasedIndexManager;
import org.netbeans.modules.parsing.impl.indexing.lucene.LayeredDocumentIndex;
import org.netbeans.modules.parsing.impl.indexing.lucene.LuceneIndexFactory;
import org.netbeans.modules.parsing.lucene.support.DocumentIndex;
import org.netbeans.modules.parsing.lucene.support.DocumentIndexCache;
import org.netbeans.modules.parsing.lucene.support.Queries;
import org.netbeans.modules.parsing.spi.indexing.*;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author Tomas Zezula
 */
public class IndexingSupportTest extends NbTestCase {
    
    private static final String MIME = "text/x-foo";    //NOI18N
    private static final String SCP = "FOO-SOURCES"; //NOI18N

    private FileObject root;
    private FileObject cache;
    private FileObject f1;
    private FileObject f2;

    public IndexingSupportTest (final String name) {
        super (name);
    }

    @Override
    public void setUp () throws Exception {
        this.clearWorkDir();
        final File wdf = getWorkDir();
        final FileObject wd = FileUtil.toFileObject(wdf);
        assert wd != null;
        root = FileUtil.createFolder(wd,"src");
        assert root != null;
        cache = FileUtil.createFolder(wd, "cache");
        assert cache != null;
        CacheFolder.setCacheFolder(cache);
        f1 = FileUtil.createData(root,"folder/a.foo");
        assert f1 != null;
        f2 = FileUtil.createData(root,"folder/b.foo");
        assert f2 != null;
        FileUtil.setMIMEType("foo", MIME);  //NOI18N
    }

    public void testIndexingSupportInstances () throws Exception {
        final Context ctx1 = SPIAccessor.getInstance().createContext(
                CacheFolder.getDataFolder(
                root.toURL()),
                root.toURL(),
                "fooIndexer",
                1,
                null,
                false,
                false,
                false,
                SuspendSupport.NOP,
                null,
                null);
        assertNotNull(ctx1);
        final Context ctx2 = SPIAccessor.getInstance().createContext(
                CacheFolder.getDataFolder(root.toURL()),
                root.toURL(),
                "embIndexer",
                1,
                null,
                false,
                false,
                false,
                SuspendSupport.NOP,
                null,
                null);
        assertNotNull(ctx2);

        final IndexingSupport is1 = IndexingSupport.getInstance(ctx1);
        assertNotNull(is1);
        final IndexingSupport is2 = IndexingSupport.getInstance(ctx2);
        assertNotNull(is2);

        assertSame(is1, SPIAccessor.getInstance().context_getAttachedIndexingSupport(ctx1));
        assertSame(is2, SPIAccessor.getInstance().context_getAttachedIndexingSupport(ctx2));
    }

    public void testIndexingQuerySupport () throws Exception {
        // index
        final Context ctx = SPIAccessor.getInstance().createContext(
                CacheFolder.getDataFolder(root.toURL()),
                root.toURL(),
                "fooIndexer",
                1,
                null,
                false,
                false,
                false,
                SuspendSupport.NOP,
                null,
                null);
        assertNotNull(ctx);
        final Indexable i1 = SPIAccessor.getInstance().create(new FileObjectIndexable(root, f1));
        final IndexingSupport is = IndexingSupport.getInstance(ctx);
        assertNotNull(is);
        IndexDocument doc1 = is.createDocument(i1);
        assertNotNull(doc1);
        doc1.addPair("class", "String", true, true);
        doc1.addPair("package", "java.lang", true, true);
        is.addDocument(doc1);
        final Indexable i2 = SPIAccessor.getInstance().create(new FileObjectIndexable(root, f2));
        IndexDocument doc2 = is.createDocument(i2);
        assertNotNull(doc2);
        doc2.addPair("class", "Object", true, true);
        doc2.addPair("package", "java.lang", true, true);
        doc2.addPair("flag", "true", true, true);
        is.addDocument(doc2);
        SPIAccessor.getInstance().getIndexFactory(ctx).getIndex(ctx.getIndexFolder()).store(true);

        // query
        QuerySupport qs = QuerySupport.forRoots("fooIndexer", 1, root);
        Collection<? extends IndexResult> result = qs.query("class", "String", QuerySupport.Kind.EXACT, "class", "package");
        assertEquals(1, result.size());
        assertEquals("String", result.iterator().next().getValue("class"));
        assertEquals("java.lang", result.iterator().next().getValue("package"));
        assertEquals(f1, result.iterator().next().getFile());
        assertEquals(f1.getURL(), result.iterator().next().getUrl());
        result = qs.query("class", "Str", QuerySupport.Kind.PREFIX, "class", "package");
        assertEquals(1, result.size());
        assertEquals("String", result.iterator().next().getValue("class"));
        assertEquals("java.lang", result.iterator().next().getValue("package"));
        result = qs.query("class", "S.*g", QuerySupport.Kind.REGEXP, "class", "package");
        assertEquals(1, result.size());
        assertEquals("String", result.iterator().next().getValue("class"));
        assertEquals("java.lang", result.iterator().next().getValue("package"));
        result = qs.query("class", "S", QuerySupport.Kind.CAMEL_CASE, "class", "package");
        assertEquals(1, result.size());
        assertEquals("String", result.iterator().next().getValue("class"));
        assertEquals("java.lang", result.iterator().next().getValue("package"));
        result = qs.query("class", "", QuerySupport.Kind.PREFIX, "class", "package");
        assertEquals(2, result.size());
        IndexResult[] ir = new IndexResult[2];
        ir = result.toArray(ir);
        assertEquals("String", ir[0].getValue("class"));
        assertEquals("java.lang", ir[0].getValue("package"));
        assertEquals("Object", ir[1].getValue("class"));
        assertEquals("java.lang", ir[1].getValue("package"));
        result = qs.query("class", "F", QuerySupport.Kind.PREFIX, "class", "package");
        assertEquals(0, result.size());

        // search for documents that contain field called 'flag'
        result = qs.query("flag", "", QuerySupport.Kind.PREFIX);
        assertEquals(1, result.size());
        assertEquals("Object", result.iterator().next().getValue("class"));
        assertEquals("java.lang", result.iterator().next().getValue("package"));
        assertEquals("true", result.iterator().next().getValue("flag"));

        // search for all documents
        result = qs.query("", "", QuerySupport.Kind.PREFIX);
        assertEquals(2, result.size());
        ir = new IndexResult[2];
        ir = result.toArray(ir);
        assertEquals("String", ir[0].getValue("class"));
        assertEquals("java.lang", ir[0].getValue("package"));
        assertNull(ir[0].getValue("flag"));
        assertEquals("Object", ir[1].getValue("class"));
        assertEquals("java.lang", ir[1].getValue("package"));
        assertEquals("true", ir[1].getValue("flag"));
    }

    public void testQuerySupportCaching() throws Exception {
        // index
        final Context ctx = SPIAccessor.getInstance().createContext(
                CacheFolder.getDataFolder(root.toURL()),
                root.toURL(),
                "fooIndexer",
                1,
                null,
                false,
                false,
                false,
                SuspendSupport.NOP,
                null,
                null);
        assertNotNull(ctx);
        final Indexable i1 = SPIAccessor.getInstance().create(new FileObjectIndexable(root, f1));
        final IndexingSupport is = IndexingSupport.getInstance(ctx);
        assertNotNull(is);
        IndexDocument doc1 = is.createDocument(i1);
        assertNotNull(doc1);
        doc1.addPair("class", "String", true, true);
        doc1.addPair("package", "java.lang", true, true);
        is.addDocument(doc1);
        final Indexable i2 = SPIAccessor.getInstance().create(new FileObjectIndexable(root, f2));
        IndexDocument doc2 = is.createDocument(i2);
        assertNotNull(doc2);
        doc2.addPair("class", "Object", true, true);
        doc2.addPair("package", "java.lang", true, true);
        is.addDocument(doc2);
        SPIAccessor.getInstance().getIndexFactory(ctx).getIndex(ctx.getIndexFolder()).store(true);

        class LIF implements IndexFactoryImpl {
            
            private final IndexFactoryImpl delegate = LuceneIndexFactory.getDefault();
            
            boolean getIndexCalled = false;

            @Override
            public org.netbeans.modules.parsing.lucene.support.IndexDocument createDocument(Indexable indexable) {
                return delegate.createDocument(indexable);
            }

            @Override
            public LayeredDocumentIndex createIndex(Context ctx) throws IOException {
                return delegate.createIndex(ctx);
            }
            
            @Override
            public LayeredDocumentIndex getIndex(FileObject indexFolder) throws IOException {
                getIndexCalled = true;
                return delegate.getIndex(indexFolder);
            }

            @Override
            public DocumentIndexCache getCache(Context ctx) throws IOException {
                return null;
            }
        }
        final LIF lif = new LIF();
        QuerySupport.IndexerQuery.indexFactory = lif;

        QuerySupport qs1 = QuerySupport.forRoots("fooIndexer", 1, root);
        assertFalse("Expecting getIndex not called", lif.getIndexCalled);
        qs1.query("", "", QuerySupport.Kind.EXACT);
        assertTrue("Expecting getIndex called", lif.getIndexCalled);

        lif.getIndexCalled = false;
        qs1.query("", "", QuerySupport.Kind.EXACT);
        assertFalse("Expecting getIndex not called", lif.getIndexCalled);

        QuerySupport qs2 = QuerySupport.forRoots("fooIndexer", 1, root);
        assertFalse("Expecting getIndex not called", lif.getIndexCalled);
        qs2.query("", "", QuerySupport.Kind.EXACT);
        assertFalse("Expecting getIndex not called", lif.getIndexCalled);
    }
    
    public void testTransientUpdates() throws Exception {
        MockMimeLookup.setInstances(MimePath.get(MIME), new CIF());
        MockServices.setServices(PR.class, CPP.class);
        CPP.scp = ClassPathSupport.createClassPath(root);
        RepositoryUpdaterTest.setMimeTypes(MIME);
        Map<URL,Map<String,Collection<String>>> attrs = new HashMap<URL, Map<String, Collection<String>>>();
        Map<String,Collection<String>> ca = new HashMap<String, Collection<String>>();
        ca.put("name",Collections.<String>singleton(f1.getName()));            //NOI18N
        ca.put("class",Collections.<String>singleton(f1.getName()));           //NOI18N
        attrs.put(f1.toURL(), ca);
        ca = new HashMap<String, Collection<String>>();
        ca.put("name",Collections.<String>singleton(f2.getName()));            //NOI18N
        ca.put("class",Collections.<String>singleton(f2.getName()));           //NOI18N
        attrs.put(f2.toURL(), ca);
        CI.attrs = attrs;
        GlobalPathRegistry.getDefault().register(SCP, new ClassPath[]{CPP.scp});
        try {
            IndexingManager.getDefault().refreshIndexAndWait(root.toURL(), null);
            final QuerySupport qs = QuerySupport.forRoots("fooIndexer", 1, root);   //NOI18N
            Collection<? extends IndexResult> result = qs.query("name", f1.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            assertEquals(1, result.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f1.getName(), result.iterator().next().getValue("class")); //NOI18N
            final FileObject indexFolder = CacheFolder.getDataFolder(root.toURL()).getFileObject("fooIndexer/1/1"); //NOI18N
            assertNotNull(indexFolder);
            final DocumentIndex base = DocumentBasedIndexManager.getDefault().getIndex(
                indexFolder.toURL(),
                DocumentBasedIndexManager.Mode.OPENED);
            assertNotNull(base);
            Collection<? extends org.netbeans.modules.parsing.lucene.support.IndexDocument> baseResult =
                base.query("name", f1.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            assertEquals(1, baseResult.size());
            assertEquals(1, baseResult.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f1.getName(), baseResult.iterator().next().getValue("class")); //NOI18N
            result = qs.query("name", f2.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            assertEquals(1, result.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), result.iterator().next().getValue("class")); //NOI18N
            baseResult = base.query("name", f2.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            assertEquals(1, baseResult.size());
            assertEquals(1, baseResult.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), baseResult.iterator().next().getValue("class")); //NOI18N
            
            //Now add new field into f1 document as transient and verify that disk index
            //did not changed
            attrs = new HashMap<URL, Map<String, Collection<String>>>();
            ca = new HashMap<String, Collection<String>>();
            ca.put("name",Collections.<String>singleton(f1.getName()));            //NOI18N
            ca.put("class",Arrays.asList(f1.getName(),"another_a_class"));         //NOI18N
            attrs.put(f1.toURL(), ca);
            ca = new HashMap<String, Collection<String>>();
            ca.put("name",Collections.<String>singleton(f2.getName()));            //NOI18N
            ca.put("class",Collections.<String>singleton(f2.getName()));           //NOI18N
            attrs.put(f2.toURL(), ca);
            CI.attrs = attrs;
            //Mark index as transiently modified and do query - f1 should have 2 classes
            final LayeredDocumentIndex ldi = LuceneIndexFactory.getDefault().getIndex(indexFolder.getParent());
            assertNotNull(ldi);
            ldi.markKeyDirty(FileUtil.getRelativePath(root, f1));
            result = qs.query("name", f1.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            String[] expected = new String[] {f1.getName(), "another_a_class"}; //NOI18N
            Arrays.sort(expected);
            String[] fields = result.iterator().next().getValues("class"); //NOI18N
            Arrays.sort(fields);
            assertEquals(Arrays.asList(expected),Arrays.asList(fields));
            //Verify that disk index is not changed
            baseResult =
                base.query("name", f1.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            assertEquals(1, baseResult.size());
            assertEquals(1, baseResult.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f1.getName(), baseResult.iterator().next().getValue("class")); //NOI18N
            //Verify that f2 document is not changed
            result = qs.query("name", f2.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            assertEquals(1, result.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), result.iterator().next().getValue("class")); //NOI18N
            baseResult = base.query("name", f2.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            assertEquals(1, baseResult.size());
            assertEquals(1, baseResult.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), baseResult.iterator().next().getValue("class")); //NOI18N
            
            //Make changes persistent -> changes should be visible also in disk index
            IndexingManager.getDefault().refreshIndexAndWait(root.toURL(), null);
            result = qs.query("name", f1.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            fields = result.iterator().next().getValues("class"); //NOI18N
            Arrays.sort(fields);
            assertEquals(Arrays.asList(expected),Arrays.asList(fields));
            baseResult =
                base.query("name", f1.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            assertEquals(1, baseResult.size());            
            fields = baseResult.iterator().next().getValues("class"); //NOI18N
            Arrays.sort(fields);
            assertEquals(Arrays.asList(expected),Arrays.asList(fields));
            //Verify that f2 document is not changed
            result = qs.query("name", f2.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            assertEquals(1, result.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), result.iterator().next().getValue("class")); //NOI18N
            baseResult = base.query("name", f2.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            assertEquals(1, baseResult.size());
            assertEquals(1, baseResult.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), baseResult.iterator().next().getValue("class")); //NOI18N
            
            //Now delete second document from f1 and mark transient - > should not be visible
            //in the query but should stay on disk
            attrs = new HashMap<URL, Map<String, Collection<String>>>();
            ca = new HashMap<String, Collection<String>>();
            ca.put("name",Collections.<String>singleton(f1.getName()));            //NOI18N
            ca.put("class",Collections.<String>singleton(f1.getName()));           //NOI18N
            attrs.put(f1.toURL(), ca);
            ca = new HashMap<String, Collection<String>>();
            ca.put("name",Collections.<String>singleton(f2.getName()));            //NOI18N
            ca.put("class",Collections.<String>singleton(f2.getName()));           //NOI18N
            attrs.put(f2.toURL(), ca);
            CI.attrs = attrs;
            ldi.markKeyDirty(FileUtil.getRelativePath(root, f1));
            result = qs.query("name", f1.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            assertEquals(1, result.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f1.getName(), result.iterator().next().getValue("class")); //NOI18N
            
            baseResult = base.query("name", f1.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            fields = baseResult.iterator().next().getValues("class"); //NOI18N
            Arrays.sort(fields);
            assertEquals(Arrays.asList(expected),Arrays.asList(fields));
            
            result = qs.query("name", f2.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            assertEquals(1, result.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), result.iterator().next().getValue("class")); //NOI18N
            baseResult = base.query("name", f2.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            assertEquals(1, baseResult.size());
            assertEquals(1, baseResult.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), baseResult.iterator().next().getValue("class")); //NOI18N
            
            //Make changes persistent -> changes should be visible also in disk index
            IndexingManager.getDefault().refreshIndexAndWait(root.toURL(), null);
            result = qs.query("name", f1.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            assertEquals(1, result.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f1.getName(), result.iterator().next().getValue("class")); //NOI18N
            baseResult = base.query("name", f1.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            assertEquals(1, baseResult.size());
            assertEquals(1, baseResult.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f1.getName(), baseResult.iterator().next().getValue("class")); //NOI18N
            result = qs.query("name", f2.getName(), QuerySupport.Kind.EXACT, "class");   //NOI18N
            assertEquals(1, result.size());
            assertEquals(1, result.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), result.iterator().next().getValue("class")); //NOI18N
            baseResult = base.query("name", f2.getName(), Queries.QueryKind.EXACT, "class");   //NOI18N
            assertEquals(1, baseResult.size());
            assertEquals(1, baseResult.iterator().next().getValues("class").length);    //NOI18N
            assertEquals(f2.getName(), baseResult.iterator().next().getValue("class")); //NOI18N
            
            
        } finally {
            GlobalPathRegistry.getDefault().unregister(SCP, new ClassPath[]{CPP.scp});
        }
    }
    
    public static class CIF extends CustomIndexerFactory {

        @Override
        public CustomIndexer createIndexer() {
            return new CI();
        }

        @Override
        public boolean supportsEmbeddedIndexers() {
            return false;
        }

        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
            try {
                final IndexingSupport is = IndexingSupport.getInstance(context);
                for (final Indexable indexable : deleted) {
                    is.removeDocuments(indexable);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {
        }

        @Override
        public String getIndexerName() {
            return "fooIndexer";    //NOI18N
        }

        @Override
        public int getIndexVersion() {
            return 1;
        }
    }
    
    private static class CI extends CustomIndexer {
        
        static volatile Map<URL,Map<String,Collection<String>>> attrs;
        
        @Override
        protected void index(Iterable<? extends Indexable> files, Context context) {
            final Map<URL,Map<String,Collection<String>>> ca = attrs;
            if (ca != null) {
                try {
                    final IndexingSupport is = IndexingSupport.getInstance(context);
                    for (Indexable indexable : files) {
                        final Map<String,Collection<String>> as = attrs.get(indexable.getURL());
                        if (as != null)  {
                            final IndexDocument doc = is.createDocument(indexable);
                            doc.addPair("path", indexable.getRelativePath(), true, true);   //NOI18N
                            for (Map.Entry<String,Collection<String>> e : as.entrySet()) {
                                for (String val : e.getValue()) {
                                    doc.addPair(e.getKey(), val, true, true);
                                }
                            }
                            is.addDocument(doc);
                        }
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
    
    public static class PR extends PathRecognizer {

        @Override
        public Set<String> getSourcePathIds() {
            return Collections.<String>singleton(SCP);
        }

        @Override
        public Set<String> getLibraryPathIds() {
            return Collections.<String>emptySet();
        }

        @Override
        public Set<String> getBinaryLibraryPathIds() {
            return Collections.<String>emptySet();
        }

        @Override
        public Set<String> getMimeTypes() {
            return Collections.<String>singleton(MIME);
        }
        
    }
    
    public static class CPP implements ClassPathProvider {
        
        static volatile ClassPath scp;

        @Override
        public ClassPath findClassPath(FileObject file, String type) {
            if (SCP.equals(type) && scp != null && scp.contains(file)) {
                return scp;
            }
            return null;
        }
        
      }
}
