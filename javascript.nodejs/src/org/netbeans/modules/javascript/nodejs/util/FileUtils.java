/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.javascript.nodejs.util;

import java.awt.EventQueue;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.project.Project;
import org.netbeans.modules.web.clientproject.api.network.NetworkException;
import org.netbeans.modules.web.clientproject.api.network.NetworkSupport;
import org.netbeans.modules.web.common.api.Version;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.modules.Places;
import org.openide.text.Line;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;
import org.openide.util.Utilities;

// XXX copied from PHP
public final class FileUtils {

    static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    private static final boolean IS_WINDOWS = Utilities.isWindows();
    private static final String JAVASCRIPT_MIME_TYPE = "text/javascript"; // NOI18N
    private static final String NODEJS_DIR_NAME = "nodejs"; // NOI18N
    private static final String NODEJS_SOURCES_URL = "http://nodejs.org/dist/v%s/node-v%s.tar.gz"; // NOI18N


    private FileUtils() {
    }

    public static boolean isJavaScriptFile(FileObject file) {
        assert file != null;
        return JAVASCRIPT_MIME_TYPE.equals(FileUtil.getMIMEType(file, JAVASCRIPT_MIME_TYPE));
    }

    public static boolean isJavaScriptFile(File file) {
        return isJavaScriptFile(FileUtil.toFileObject(file));
    }

    public static String relativizePath(Project project, String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return ""; // NOI18N
        }
        File file = new File(filePath);
        String path = PropertyUtils.relativizeFile(FileUtil.toFile(project.getProjectDirectory()), file);
        if (path == null
                || path.startsWith("../")) { // NOI18N
            // cannot be relativized or outside project
            path = file.getAbsolutePath();
        }
        return path;
    }

    public static String resolvePath(Project project, String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }
        return PropertyUtils.resolveFile(FileUtil.toFile(project.getProjectDirectory()), filePath).getAbsolutePath();
    }

    public static List<File> sortFiles(Collection<File> files) {
        final Collator collator = Collator.getInstance();
        List<File> sortedFiles = new ArrayList<>(files);
        Collections.sort(sortedFiles, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                return collator.compare(file1.getName(), file2.getName());
            }
        });
        return sortedFiles;
    }

    /**
     * Find all the files (absolute path) with the given "filename" on user's PATH.
     * <p>
     * This method is suitable for *nix as well as windows.
     * @param filename the name of a file to find.
     * @return list of absolute paths of found files.
     * @see #findFileOnUsersPath(String[])
     */
    public static List<String> findFileOnUsersPath(String filename) {
        Parameters.notNull("filename", filename); // NOI18N
        return findFileOnUsersPath(new String[]{filename});
    }

    /**
     * Find all the files (absolute path) with the given "filename" on user's PATH.
     * <p>
     * This method is suitable for *nix as well as windows.
     * @param filenames the name of a file to find, more names can be provided.
     * @return list of absolute paths of found files (order preserved according to input names).
     * @see #findFileOnUsersPath(String)
     */
    public static List<String> findFileOnUsersPath(String... filenames) {
        Parameters.notNull("filenames", filenames); // NOI18N

        String path = System.getenv("PATH"); // NOI18N
        LOGGER.log(Level.FINE, "PATH: [{0}]", path);
        if (path == null) {
            return Collections.<String>emptyList();
        }
        // on linux there are usually duplicities in PATH
        Set<String> dirs = new LinkedHashSet<>(Arrays.asList(path.split(File.pathSeparator)));
        LOGGER.log(Level.FINE, "PATH dirs: {0}", dirs);
        List<String> found = new ArrayList<>(dirs.size() * filenames.length);
        for (String filename : filenames) {
            Parameters.notNull("filename", filename); // NOI18N
            for (String dir : dirs) {
                File file = new File(dir, filename);
                if (file.isFile()) {
                    String absolutePath = FileUtil.normalizeFile(file).getAbsolutePath();
                    LOGGER.log(Level.FINE, "File ''{0}'' found", absolutePath);
                    // not optimal but should be ok
                    if (!found.contains(absolutePath)) {
                        LOGGER.log(Level.FINE, "File ''{0}'' added to found files", absolutePath);
                        found.add(absolutePath);
                    }
                }
            }
        }
        LOGGER.log(Level.FINE, "Found files: {0}", found);
        return found;
    }

    /**
     * Validate a file path and return {@code null} if it is valid, otherwise an error.
     * <p>
     * This method simply calls {@link #validateFile(String, String, boolean)} with "File"
     * (localized) as a {@code source}.
     * @param filePath a file path to validate
     * @param writable {@code true} if the file must be writable, {@code false} otherwise
     * @return {@code null} if it is valid, otherwise an error
     * @see #validateFile(String, String, boolean)
     */
    @NbBundle.Messages("FileUtils.validateFile.file=File")
    @CheckForNull
    public static String validateFile(String filePath, boolean writable) {
        return validateFile(Bundle.FileUtils_validateFile_file(), filePath, writable);
    }

    /**
     * Validate a file path and return {@code null} if it is valid, otherwise an error.
     * <p>
     * A valid file means that the <tt>filePath</tt> represents a valid, readable file
     * with absolute file path.
     * @param source source used in error message (e.g. "Script", "Config file")
     * @param filePath a file path to validate
     * @param writable {@code true} if the file must be writable, {@code false} otherwise
     * @return {@code null} if it is valid, otherwise an error
     */
    @NbBundle.Messages({
        "# {0} - source",
        "FileUtils.validateFile.missing={0} must be selected.",
        "# {0} - source",
        "FileUtils.validateFile.notAbsolute={0} must be an absolute path.",
        "# {0} - source",
        "FileUtils.validateFile.notFile={0} must be a valid file.",
        "# {0} - source",
        "FileUtils.validateFile.notReadable={0} is not readable.",
        "# {0} - source",
        "FileUtils.validateFile.notWritable={0} is not writable."
    })
    @CheckForNull
    public static String validateFile(String source, String filePath, boolean writable) {
        if (!StringUtils.hasText(filePath)) {
            return Bundle.FileUtils_validateFile_missing(source);
        }

        File file = new File(filePath);
        if (!file.isAbsolute()) {
            return Bundle.FileUtils_validateFile_notAbsolute(source);
        } else if (!file.isFile()) {
            return Bundle.FileUtils_validateFile_notFile(source);
        } else if (!file.canRead()) {
            return Bundle.FileUtils_validateFile_notReadable(source);
        } else if (writable && !file.canWrite()) {
            return Bundle.FileUtils_validateFile_notWritable(source);
        }
        return null;
    }

    /**
     * Get the OS-dependent script extension.
     * <ul>Currently it returns (for dotted version):
     *   <li><tt>.bat/.cmd</tt> on Windows
     *   <li><tt>.sh</tt> anywhere else
     * </ul>
     * @param withDot return "." as well, e.g. <tt>.sh</tt>
     * @param cmdInsteadBatOnWin if {@code true}, return "cmd" instead of "bat" on Windows
     * @return the OS-dependent script extension
     */
    public static String getScriptExtension(boolean withDot, boolean cmdInsteadBatOnWin) {
        StringBuilder sb = new StringBuilder(4);
        if (withDot) {
            sb.append("."); // NOI18N
        }
        if (IS_WINDOWS) {
            sb.append(cmdInsteadBatOnWin ? "cmd" : "bat"); // NOI18N
        } else {
            sb.append("sh"); // NOI18N
        }
        return sb.toString();
    }

    /**
     * Opens the file and optionally set cursor to the line. This action is always run in AWT thread.
     * @param file path of a file to open
     * @param line line of a file to set cursor to, {@code -1} if no specific line is needed
     */
    public static void openFile(File file, int line) {
        assert file != null;

        FileObject fileObject = FileUtil.toFileObject(FileUtil.normalizeFile(file));
        if (fileObject == null) {
            LOGGER.log(Level.INFO, "FileObject not found for {0}", file);
            return;
        }

        DataObject dataObject;
        try {
            dataObject = DataObject.find(fileObject);
        } catch (DataObjectNotFoundException ex) {
            LOGGER.log(Level.INFO, "DataObject not found for {0}", file);
            return;
        }

        if (line == -1) {
            // simply open file
            EditorCookie ec = dataObject.getLookup().lookup(EditorCookie.class);
            ec.open();
            return;
        }

        // open at specific line
        LineCookie lineCookie = dataObject.getLookup().lookup(LineCookie.class);
        if (lineCookie == null) {
            LOGGER.log(Level.INFO, "LineCookie not found for {0}", file);
            return;
        }
        Line.Set lineSet = lineCookie.getLineSet();
        try {
            final Line currentLine = lineSet.getCurrent(line - 1);
            Mutex.EVENT.readAccess(new Runnable() {
                @Override
                public void run() {
                    currentLine.show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
                }
            });
        } catch (IndexOutOfBoundsException exc) {
            LOGGER.log(Level.FINE, null, exc);
        }
    }

    public static boolean hasNodeSources(Version version) {
        File nodeSources = getNodeSources(version);
        String[] children = nodeSources.list();
        return children != null
                && children.length > 0;
    }

    public static File getNodeSources(Version version) {
        assert version != null;
        return Places.getCacheSubdirectory(NODEJS_DIR_NAME + "/" + version.toString());
    }

    @NbBundle.Messages({
        "# {0} - version",
        "FileUtils.sources.downloading=Downloading sources for node.js version {0}...",
    })
    public static void downloadNodeSources(Version version) throws NetworkException, IOException {
        assert !EventQueue.isDispatchThread();
        if (hasNodeSources(version)) {
            // delete it
            final FileObject fo = FileUtil.toFileObject(getNodeSources(version));
            assert fo != null : version;
            FileUtil.runAtomicAction(new FileSystem.AtomicAction() {
                @Override
                public void run() throws IOException {
                    fo.delete();
                }
            });
        }
        File nodeSources = getNodeSources(version);
        String nodeVersion = version.toString();
        String url = String.format(NODEJS_SOURCES_URL, nodeVersion, nodeVersion);
        File archive = new File(nodeSources, "nodejs-" + nodeVersion + ".tar.gz"); // NOI18N
        try {
            NetworkSupport.downloadWithProgress(url, archive, Bundle.FileUtils_sources_downloading(nodeVersion));
        } catch (InterruptedException ex) {
            // download cancelled
            LOGGER.log(Level.FINE, "Download cancelled for {0}", url);
            return;
        }
        decompressTarGz(archive, nodeSources, true);
        if (!archive.delete()) {
            archive.deleteOnExit();
        }
    }

    public static void decompressTarGz(File archive, File destination, boolean skipArchiveRoot) throws IOException {
        try (TarArchiveInputStream tarInputStream = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(archive)))) {
            int archiveRootLength = -1;
            TarArchiveEntry tarEntry = tarInputStream.getNextTarEntry();
            if (skipArchiveRoot
                    && tarEntry != null) {
                archiveRootLength = tarEntry.getName().length();
                tarEntry = tarInputStream.getNextTarEntry();
            }
            while (tarEntry != null) {
                String name = tarEntry.getName();
                if (skipArchiveRoot) {
                    name = name.substring(archiveRootLength);
                }
                File destPath = new File(destination, name);
                if (tarEntry.isDirectory()) {
                    destPath.mkdirs();
                } else {
                    destPath.createNewFile();
                    try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(destPath))) {
                        FileUtil.copy(tarInputStream, outputStream);
                    }
                }
                tarEntry = tarInputStream.getNextTarEntry();
            }
        }
    }

}
