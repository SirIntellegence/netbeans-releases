/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.versioning.system.cvss.ui.actions.diff;

import org.netbeans.api.diff.StreamSource;
import org.netbeans.api.diff.Difference;
import org.netbeans.modules.versioning.system.cvss.VersionsCache;
import org.netbeans.modules.versioning.system.cvss.CvsVersioningSystem;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import java.io.*;

/**
 * Stream source for diffing CVS managed files.
 * 
 * @author Maros Sandor
 */
public class DiffStreamSource extends StreamSource {
        
    private final File      baseFile;
    private final String    revision;
    private final String    title;
    private String          mimeType;

    private IOException     failure;
    /**
     * Null is a valid value if base file does not exist in this revision. 
     */ 
    private File            remoteFile;
    private boolean         binary;

    /**
     * Creates a new StreamSource implementation for Diff engine.
     * 
     * @param baseFile
     * @param revision file revision, may be null if the revision does not exist (ie for new files)
     * @param title title to use in diff panel
     */ 
    public DiffStreamSource(File baseFile, String revision, String title) {
        this.baseFile = baseFile;
        this.revision = revision;
        this.title = title;
    }

    public String getName() {
        return baseFile.getName();
    }

    public String getTitle() {
        return title;
    }

    public String getMIMEType() {
        try {
            init();
        } catch (IOException e) {
            return null;
        }
        return mimeType;
    }

    public Reader createReader() throws IOException {
        init();        
        if (revision == null || remoteFile == null) return null;
        if (binary) {
            return new StringReader("[Binary File " + getTitle() + "]");
        } else {
            return new FileReader(remoteFile);
        }
    }

    public Writer createWriter(Difference[] conflicts) throws IOException {
        throw new IOException("Operation not supported");
    }

    /**
     * Loads data over network.
     */
    synchronized void init() throws IOException {
        if (remoteFile != null || revision == null) return;
        binary = !CvsVersioningSystem.getInstance().isText(baseFile);
        try {
            remoteFile = VersionsCache.getInstance().getRemoteFile(baseFile, revision);
            failure = null;
        } catch (Exception e) {
            failure = new IOException("Cannot initialize stream source");
            failure.initCause(e);
            throw failure;
        }
        FileObject fo = remoteFile != null ? FileUtil.toFileObject(remoteFile) : null;
        if (fo != null) {
            mimeType = fo.getMIMEType();
        } else if (binary) {
            mimeType = "application/octet-stream";
        } else {
            mimeType = "text/plain";
        }
    }
}
