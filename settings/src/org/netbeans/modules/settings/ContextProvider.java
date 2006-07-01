/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.settings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import org.openide.filesystems.*;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/** Factory producing proxy objects allowing to provide own context via Lookup.Provider.
 *
 * @author  Jan Pokorsky
 */
public final class ContextProvider {

    /** Creates a new instance of ContextProvider */
    private ContextProvider() {
    }

    /** create a proxy which delegates to original Writer w and provides
     * source FileObject via Lookup.Provider
     */
    public static Writer createWriterContextProvider(Writer w, FileObject src) {
        return new WriterProvider(w, src);
    }
    
    /** create a proxy which delegates to original Reader r and provides
     * source FileObject via Lookup.Provider
     */
    public static Reader createReaderContextProvider(Reader r, FileObject src) {
        return new ReaderProvider(r, src);
    }
        
    private static final class WriterProvider extends Writer implements Lookup.Provider {
        private final Writer orig;
        private final FileObject src;
        private Lookup lookup;

        public WriterProvider(Writer w, FileObject src) {
            this.orig = w;
            this.src = src;
        }

        public void close() throws IOException {
            orig.close();
        }

        public void flush() throws IOException {
            orig.flush();
        }

        public void write(char[] cbuf, int off, int len) throws IOException {
            orig.write(cbuf, off, len);
        }

        public Lookup getLookup() {
            if (lookup == null) {
                lookup = Lookups.singleton(new FileObjectContext(src));
            }
            return lookup;
        }
        
    }
    
    private static final class ReaderProvider extends Reader implements Lookup.Provider {
        private final Reader orig;
        private final FileObject src;
        private Lookup lookup;
        
        public ReaderProvider(Reader r, FileObject src) {
            this.orig = r;
            this.src = src;
        }
        
        public void close() throws IOException {
            orig.close();
        }
        
        public int read(char[] cbuf, int off, int len) throws IOException {
            return orig.read(cbuf, off, len);
        }
        
        public Lookup getLookup() {
            if (lookup == null) {
                lookup = Lookups.singleton(new FileObjectContext(src));
            }
            return lookup;
        }
        
    }
    
    /** The Restricted FileObject implementation allowing to get just
     * read-only informations about name and location. It should prevent
     * any manipulation with file or its content.
     */
    private static final class FileObjectContext extends FileObject {
        private static final String UNSUPPORTED = "The Restricted FileObject" + //NOI18N
            " implementation allowing to get just read-only informations about" + //NOI18N
            " name and location. It should prevent any manipulation with file" + //NOI18N
            " or its content."; //NOI18N
        private final FileObject fo;
        
        public FileObjectContext(FileObject fo) {
            this.fo = fo;
        }
        
        public void addFileChangeListener(FileChangeListener fcl) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public FileObject createData(String name, String ext) throws IOException {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public FileObject createFolder(String name) throws IOException {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public void delete(FileLock lock) throws IOException {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public Object getAttribute(String attrName) {
            return fo.getAttribute(attrName);
        }
        
        public java.util.Enumeration getAttributes() {
            return fo.getAttributes();
        }
        
        public FileObject[] getChildren() {
            return new FileObject[0];
        }
        
        public String getExt() {
            return fo.getExt(); //NOI18N
        }
        
        public FileObject getFileObject(String name, String ext) {
            return null;
        }
        
        public FileSystem getFileSystem() throws FileStateInvalidException {
            return fo.getFileSystem();
        }
        
        public java.io.InputStream getInputStream() throws java.io.FileNotFoundException {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public String getName() {
            return fo.getName();
        }
        
        public java.io.OutputStream getOutputStream(FileLock lock) throws java.io.IOException {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public FileObject getParent() {
            return fo.getParent();
        }
        
        public long getSize() {
            return fo.getSize();
        }
        
        public boolean isData() {
            return true;
        }
        
        public boolean isFolder() {
            return false;
        }
        
        public boolean isReadOnly() {
            return fo.isReadOnly();
        }
        
        public boolean isRoot() {
            return false;
        }
        
        public boolean isValid() {
            return fo.isValid();
        }
        
        public java.util.Date lastModified() {
            return fo.lastModified();
        }
        
        public FileLock lock() throws IOException {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public void removeFileChangeListener(FileChangeListener fcl) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public void rename(FileLock lock, String name, String ext) throws IOException {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public void setAttribute(String attrName, Object value) throws IOException {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
        public void setImportant(boolean b) {
            throw new UnsupportedOperationException(UNSUPPORTED);
        }
        
    }
    
}
