/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.parsing.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import javax.swing.text.EditorKit;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.parsing.impl.SourceAccessor;
import org.netbeans.modules.parsing.impl.SourceCache;
import org.netbeans.modules.parsing.impl.SourceFlags;
import org.netbeans.modules.parsing.impl.event.EventSupport;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Parameters;
import org.openide.util.UserQuestionException;


/**
 * Source represents one file or document. There is always at most one Source
 * for one FileObject.
 * 
 * @author Jan Jancura
 * @author Tomas Zezula
 */
public final class Source {
    
    /**
     * Creates Source for given file.
     * 
     * @param fileObject    A file object.
     * @return              Source for given file or null when the given 
     *                      file doesn't exist.
     */
    // XXX: this should really be called 'get'
    public static Source create (
        FileObject          fileObject
    ) {
        Parameters.notNull("fileObject", fileObject); //NOI18N
        if (!fileObject.isValid() || !fileObject.isData()) {
            return null;
        }

        synchronized (Source.class) {
            Reference<Source> ref = instances.get(fileObject);
            Source result = null;
            if (ref != null) {
                result = ref.get();
            }
            if (result == null) {
                result = new Source (
                    fileObject.getMIMEType (),
                    null, 
                    fileObject
                );
                ref = new WeakReference<Source>(result);
                instances.put(fileObject, ref);
            }
            return result;
        }
    }
    
    /**
     * Creates source for given document.
     * 
     * @param document      A document.
     * @return              source for given document.
     */
    // XXX: this should really be called 'get'
    public static Source create (
        Document            document
    ) {
        Parameters.notNull("document", document); //NOI18N

        synchronized (Source.class) {
            Source source = (Source) document.getProperty(Source.class);

            if (source == null) {
                FileObject fileObject = NbEditorUtilities.getFileObject(document);
                if (fileObject != null) {
                    source = Source.create(fileObject);
                } else {
                    // file-less document
                    String mimeType = NbEditorUtilities.getMimeType(document);
                    source = new Source(mimeType, document, null);
                }
                document.putProperty(Source.class, source);
            }

            return source;
        }
    }

    /**
     * Returns source mime type.
     * 
     * @return              this source mime type.
     */
    public String getMimeType() {
        return mimeType;
    }
    
    /**
     * Returns <code>Document</code> this source has been created from or 
     * <code>null</code>.
     * 
     * @return              <code>Document</code> this source has been created 
     *                      from or <code>null</code>
     */
    // XXX: maybe we should add 'boolean forceOpen' parameter and call
    // editorCookie.openDocument() if neccessary
    public Document getDocument () {
        return _getDocument(false);
    }
    
    /**
     * Returns <code>FileObject</code> this source has been created from 
     * or <code>null</code>.
     * 
     * @return              <code>FileObject</code> this source has been 
     *                      created from or <code>null</code>
     */
    public FileObject getFileObject () {
        return fileObject;
    }

    /**
     * Creates snapshot of current content of this source.
     * 
     * @return              snapshot of current content of this source
     */
    public Snapshot createSnapshot () {
        final String [] text = new String [] { "" }; //NOI18N
        Document doc = _getDocument(false);
        if (doc == null) {
            EditorKit kit = CloneableEditorSupport.getEditorKit(mimeType);
            Document customDoc = kit.createDefaultDocument();
            try {
                InputStream is = fileObject.getInputStream();
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, FileEncodingQuery.getEncoding(fileObject)));
                    try {
                        kit.read(reader, customDoc, 0);
                        doc = customDoc;
                    } catch (BadLocationException ble) {
                        LOG.log(Level.WARNING, null, ble);
                    } finally {
                        reader.close();
                    }
                } finally {
                    is.close();
                }
            } catch (IOException ioe) {
                LOG.log(Level.WARNING, null, ioe);
            }
        }
        if (doc != null) {
            final Document d = doc;
            d.render(new Runnable() {
                public void run() {
                    try {
                        text[0] = d.getText(0, d.getLength());
                    } catch (BadLocationException ble) {
                        LOG.log(Level.WARNING, null, ble);
                    }
                }
            });
        }

        return new Snapshot(
            text[0], this, mimeType, new int[][]{new int[]{0, 0}}, new int[][]{new int[]{0, 0}}
        );
    }
    

    // ------------------------------------------------------------------------
    // private implementation
    // ------------------------------------------------------------------------

    private static final Logger LOG = Logger.getLogger(Source.class.getName());
    private static final Map<FileObject, Reference<Source>> instances = new WeakHashMap<FileObject, Reference<Source>>();

    static {
        SourceAccessor.setINSTANCE(new MySourceAccessor());
    }
        
    private final String mimeType;
    private final FileObject fileObject;
    private Document document;

    private final Set<SourceFlags> flags = EnumSet.noneOf(SourceFlags.class);
    
    private int taskCount;
    private volatile Parser cachedParser;
    private SchedulerEvent  schedulerEvent;
    //GuardedBy(this)
    private SourceCache     cache;
    //GuardedBy(this)
    private volatile long eventId;
    //Changes handling
    private final EventSupport support = new EventSupport (this);

    private Source (
        String              mimeType,
        Document            document,
        FileObject          fileObject
    ) {
        this.mimeType =     mimeType;
        this.document =     document;
        this.fileObject =   fileObject;
    }

    private Document _getDocument(boolean forceOpen) {
        Document doc;
        synchronized (this) {
            doc = document;
        }

        if (doc == null) {
            EditorCookie ec = null;

            try {
                DataObject dataObject = DataObject.find(fileObject);
                ec = dataObject.getLookup().lookup(EditorCookie.class);
            } catch (DataObjectNotFoundException ex) {
                LOG.log(Level.WARNING, null, ex);
            }

            if (ec != null) {
                doc = ec.getDocument();
                if (doc == null && forceOpen) {
                    try {
                        try {
                            doc = ec.openDocument();
                        } catch (UserQuestionException uqe) {
                            uqe.confirmed();
                            doc = ec.openDocument();
                        }
                    } catch (IOException ioe) {
                        LOG.log(Level.WARNING, null, ioe);
                    }
                }
            }
        }

        synchronized (this) {
            if (document == null) {
                document = doc;
            }

            return document;
        }
    }

    private void assignListeners () {
        support.init();
    }

    private static class MySourceAccessor extends SourceAccessor {
        
        @Override
        public void setFlags (final Source source, final Set<SourceFlags> flags)  {
            assert source != null;
            assert flags != null;
            synchronized (source) {
                source.flags.addAll(flags);
                source.eventId++;
            }
        }

        @Override
        public boolean testFlag (final Source source, final SourceFlags flag) {
            assert source != null;
            assert flag != null;
            synchronized (source) {
                return source.flags.contains(flag);
            }
        }

        @Override
        public boolean cleanFlag (final Source source, final SourceFlags flag) {
            assert source != null;
            assert flag != null;
            synchronized (source) {        
                return source.flags.remove(flag);
            }
        }

        @Override
        public boolean testAndCleanFlags (final Source source, final SourceFlags test, final Set<SourceFlags> clean) {
            assert source != null;
            assert test != null;
            assert clean != null;
            synchronized (source) {
                boolean res = source.flags.contains(test);
                source.flags.removeAll(clean);
                return res;
            }
        }

        @Override
        public void invalidate (final Source source, final boolean force) {
            assert source != null;
            synchronized (source) {
                if (force || source.flags.remove(SourceFlags.INVALID)) {
                    final SourceCache cache = getCache(source);
                    assert cache != null;
                    cache.invalidate();
                }
            }
        }

        @Override
        public boolean invalidate(Source source, long id, Snapshot snapshot) {
            assert source != null;
            synchronized (source) {
                if (snapshot == null) {
                    return !source.flags.contains(SourceFlags.INVALID);
                }
                else {
                    if (id != source.eventId) {
                        return false;
                    }
                    else {
                        source.flags.remove(SourceFlags.INVALID);
                        final SourceCache cache = getCache(source);
                        assert cache != null;
                        cache.invalidate();
                        return true;
                    }
                }
            }
        }
        
        @Override
        public Parser getParser(Source source) {
            assert source != null;
            return source.cachedParser;
        }

        @Override
        public void setParser(Source source, Parser parser) throws IllegalStateException {
            assert source != null;
            assert parser != null;
            synchronized (source) {
                if (source.cachedParser != null) {
                    throw new IllegalStateException();
                }
                source.cachedParser = parser;
            }
        }
        
        @Override
        public void assignListeners (final Source source) {
            assert source != null;
            source.assignListeners();
        }
        
        @Override
        public EventSupport getEventSupport (final Source source) {
            assert source != null;
            return source.support;
        }

        @Override
        public long getLastEventId (final Source source) {
            assert source != null;
            return source.eventId;
        }

        @Override
        public void setEvent (Source source, SchedulerEvent event) {
            assert source != null;
            assert event != null;
            synchronized (source) {
                if (event == null) {
                    throw new IllegalStateException();
                }
                source.schedulerEvent = event;
            }
        }

        @Override
        public SchedulerEvent getEvent(Source source) {
            return source.schedulerEvent;
        }

        @Override
        public SourceCache getCache (final Source source) {
            assert source != null;
            synchronized (source) {
                if (source.cache == null)
                    source.cache = new SourceCache (source, null);
                return source.cache;
            }
        }

        @Override
        public int taskAdded(final Source source) {
            assert source != null;
            int ret;
            synchronized (source) {
                ret = source.taskCount++;
            }
            return ret;
        }

        @Override
        public int taskRemoved(final Source source) {
            assert source != null;
            synchronized (source) {
                return --source.taskCount;
            }
        }
    } // End of MySourceAccessor class
}
