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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.php.api.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;
import org.openide.util.Utilities;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Miscellaneous file utilities.
 * @author Tomas Mysik
 */
public final class FileUtils {

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());

    /**
     * Constant for PHP MIME type.
     * @see #isPhpFile(FileObject)
     */
    public static final String  PHP_MIME_TYPE = "text/x-php5"; // NOI18N

    private static final boolean IS_UNIX = Utilities.isUnix();
    private static final boolean IS_MAC = Utilities.isMac();
    private static final boolean IS_WINDOWS = Utilities.isWindows();
    private static final ZipEntryFilter DUMMY_ZIP_ENTRY_FILTER = new ZipEntryFilter() {
        @Override
        public boolean accept(ZipEntry zipEntry) {
            return true;
        }
        @Override
        public String getName(ZipEntry zipEntry) {
            return zipEntry.getName();
        }
    };


    private FileUtils() {
    }

    /**
     * Returns <code>true</code> if the file is a PHP file.
     * @param file file to check
     * @return <code>true</code> if the file is a PHP file
     * @see #PHP_MIME_TYPE
     */
    public static boolean isPhpFile(FileObject file) {
        Parameters.notNull("file", file); // NOI18N
        return PHP_MIME_TYPE.equals(FileUtil.getMIMEType(file, PHP_MIME_TYPE));
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
        return findFileOnUsersPath(new String[]{filename});
    }

    /**
     * Find all the files (absolute path) with the given "filename" on user's PATH.
     * <p>
     * This method is suitable for *nix as well as windows.
     * @param filename the name of a file to find, more names can be provided.
     * @return list of absolute paths of found files (order preserved according to input names).
     * @see #findFileOnUsersPath(String)
     */
    public static List<String> findFileOnUsersPath(String... filename) {
        Parameters.notNull("filename", filename); // NOI18N

        String path = System.getenv("PATH"); // NOI18N
        LOGGER.log(Level.FINE, "PATH: [{0}]", path);
        if (path == null) {
            return Collections.<String>emptyList();
        }
        // on linux there are usually duplicities in PATH
        Set<String> dirs = new LinkedHashSet<String>(Arrays.asList(path.split(File.pathSeparator)));
        LOGGER.log(Level.FINE, "PATH dirs: {0}", dirs);
        List<String> found = new ArrayList<String>(dirs.size() * filename.length);
        for (String f : filename) {
            for (String d : dirs) {
                File file = new File(d, f);
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
     * Get the OS-dependent script extension.
     * <ul>Currently it returns (for dotted version):
     *   <li><tt>.bat</tt> on Windows
     *   <li><tt>.sh</tt> anywhere else
     * </ul>
     * @param withDot return "." as well, e.g. <tt>.sh</tt>
     * @return the OS-dependent script extension
     */
    public static String getScriptExtension(boolean withDot) {
        StringBuilder sb = new StringBuilder(4);
        if (withDot) {
            sb.append("."); // NOI18N
        }
        if (IS_WINDOWS) {
            sb.append("bat"); // NOI18N
        } else {
            sb.append("sh"); // NOI18N
        }
        return sb.toString();
    }

    /**
     * Get {@link FileObject} for the given {@link Lookup context}.
     * @param context {@link Lookup context} where the {@link FileObject} is searched for
     * @return {@link FileObject} for the given {@link Lookup context} or <code>null</code> if not found
     */
    public static FileObject getFileObject(Lookup context) {
        FileObject fo = context.lookup(FileObject.class);
        if (fo != null) {
            return fo;
        }
        DataObject d = context.lookup(DataObject.class);
        if (d != null) {
            return d.getPrimaryFile();
        }
        return null;
    }

    /**
     * Create {@link org.xml.sax.XMLReader} from {javax.xml.parsers.SAXParser}.
     * @return {@link org.xml.sax.XMLReader} from {javax.xml.parsers.SAXParser}
     * @throws SAXException if the parser cannot be created
     */
    public static XMLReader createXmlReader() throws SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        try {
            return factory.newSAXParser().getXMLReader();
        } catch (ParserConfigurationException ex) {
            throw new SAXException("Cannot create SAX parser", ex);
        }
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
     * @see #validateDirectory(String, String, boolean)
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
     * Validate a directory path and return {@code null} if it is valid, otherwise an error.
     * <p>
     * This method simply calls {@link #validateDirectory(String, String, boolean)} with "Directory"
     * (localized) as a {@code source}.
     * @param dirPath a file path to validate
     * @param writable {@code true} if the directory must be writable, {@code false} otherwise
     * @return {@code null} if it is valid, otherwise an error
     * @see #validateDirectory(String, String, boolean)
     * @see #isDirectoryWritable(File)
     */
    @NbBundle.Messages("FileUtils.validateDirectory.directory=Directory")
    public static String validateDirectory(String dirPath, boolean writable) {
        return validateDirectory(Bundle.FileUtils_validateDirectory_directory(), dirPath, writable);
    }

    /**
     * Validate a directory path and return {@code null} if it is valid, otherwise an error.
     * <p>
     * A valid directory means that the <tt>dirPath</tt> represents an existing, readable, optionally
     * writable directory with absolute file path.
     * @param source source used in error message (e.g. "Project directory", "Working directory")
     * @param dirPath a file path to validate
     * @param writable {@code true} if the directory must be writable, {@code false} otherwise
     * @return {@code null} if it is valid, otherwise an error
     * @see #isDirectoryWritable(File)
     */
    @NbBundle.Messages({
        "# {0} - source",
        "FileUtils.validateDirectory.missing={0} must be selected.",
        "# {0} - source",
        "FileUtils.validateDirectory.notAbsolute={0} must be an absolute path.",
        "# {0} - source",
        "FileUtils.validateDirectory.notDir={0} must be a valid directory.",
        "# {0} - source",
        "FileUtils.validateDirectory.notReadable={0} is not readable.",
        "# {0} - source",
        "FileUtils.validateDirectory.notWritable={0} is not writable."
    })
    public static String validateDirectory(String source, String dirPath, boolean writable) {
        if (!StringUtils.hasText(dirPath)) {
            return Bundle.FileUtils_validateDirectory_missing(source);
        }

        File dir = new File(dirPath);
        if (!dir.isAbsolute()) {
            return Bundle.FileUtils_validateDirectory_notAbsolute(source);
        } else if (!dir.isDirectory()) {
            return Bundle.FileUtils_validateDirectory_notDir(source);
        } else if (!dir.canRead()) {
            return Bundle.FileUtils_validateDirectory_notReadable(source);
        } else if (writable && !isDirectoryWritable(dir)) {
            return Bundle.FileUtils_validateDirectory_notWritable(source);
        }
        return null;
    }

    // #144928, #157417
    /**
     * Handles correctly 'feature' of Windows (read-only flag, "Program Files" directory on Windows Vista).
     * @param directory a directory to check
     * @return <code>true</code> if directory is writable
     */
    public static boolean isDirectoryWritable(File directory) {
        if (!directory.isDirectory()) {
            // #157591
            LOGGER.log(Level.FINE, "{0} is not a folder", directory);
            return false;
        }
        boolean windows = IS_WINDOWS;
        LOGGER.log(Level.FINE, "On Windows: {0}", windows);

        boolean canWrite = directory.canWrite();
        LOGGER.log(Level.FINE, "Folder {0} is writable: {1}", new Object[] {directory, canWrite});
        if (!windows) {
            // we are not on windows => result is ok
            return canWrite;
        }

        // on windows
        LOGGER.fine("Trying to create temp file");
        try {
            File tmpFile = File.createTempFile("netbeans", null, directory);
            LOGGER.log(Level.FINE, "Temp file {0} created", tmpFile);
            if (!tmpFile.delete()) {
                tmpFile.deleteOnExit();
            }
            LOGGER.log(Level.FINE, "Temp file {0} deleted", tmpFile);
        } catch (IOException exc) {
            LOGGER.log(Level.FINE, exc.getMessage(), exc);
            return false;
        }
        return true;
    }

    /**
     * Test whether the given file is a folder and is a symlink.
     * <p>
     * In other words, a file is never considered to be a symlink. Directory is checked
     * and correct result is returned.
     * @param directory file to be checked, cannot be {@code null}
     * @return {@code true} if the file is a folder and a symlink, {@code false} otherwise
     */
    public static boolean isDirectoryLink(File directory) {
        Parameters.notNull("directory", directory); // NOI18N
        if (!IS_UNIX && !IS_MAC) {
            return false;
        }
        if (!directory.isDirectory()) {
            return false;
        }
        final File canDirectory;
        try {
            canDirectory = directory.getCanonicalFile();
        } catch (IOException ioe) {
            return false;
        }
        final String dirPath = directory.getAbsolutePath();
        final String canDirPath = canDirectory.getAbsolutePath();
        return IS_MAC ? !dirPath.equalsIgnoreCase(canDirPath) : !dirPath.equals(canDirPath);
    }

    /**
     * Recursively unzip the given ZIP archive to the given target directory.
     * @param zipPath path of ZIP archive to be extracted
     * @param targetDirectory target directory
     * @param zipEntryFilter {@link ZipEntryFilter}, can be {@code null} (in such case, all entries are accepted with their original names)
     * @throws IOException if any error occurs
     */
    public static void unzip(String zipPath, File targetDirectory, ZipEntryFilter zipEntryFilter) throws IOException {
        Parameters.notEmpty("zipPath", zipPath); // NOI18N
        Parameters.notNull("targetDirectory", targetDirectory); // NOI18N

        if (zipEntryFilter == null) {
            zipEntryFilter = DUMMY_ZIP_ENTRY_FILTER;
        }
        ZipFile zipFile = new ZipFile(zipPath);
        try {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (!zipEntryFilter.accept(zipEntry)) {
                    continue;
                }
                File destinationFile = new File(targetDirectory, zipEntryFilter.getName(zipEntry));
                ensureParentExists(destinationFile);
                copyZipEntry(zipFile, zipEntry, destinationFile);
            }
        } finally {
            zipFile.close();
        }
    }

    private static void ensureParentExists(File file) throws IOException {
        File parent = file.getParentFile();
        if (!parent.isDirectory()) {
            if (!parent.mkdirs()) {
                throw new IOException("Cannot create parent directories for " + file.getAbsolutePath());
            }
        }
    }

    private static void copyZipEntry(ZipFile zipFile, ZipEntry zipEntry, File destinationFile) throws IOException {
        if (zipEntry.isDirectory()) {
            return;
        }
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        try {
            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            try {
                FileUtil.copy(inputStream, outputStream);
            } finally {
                outputStream.close();
            }
        } finally {
            inputStream.close();
        }
    }

    //~ Inner classes

    /**
     * Filter for {@link ZipEntry}s.
     * <p>
     * Instances of this interface may be passed to the {@link #unzip(String, File, ZipEntryFilter)}code> method.
     * @see #unzip(String, File, ZipEntryFilter)
     */
    public interface ZipEntryFilter {

        /**
         * Test whether or not the specified {@link ZipEntry} should be
         * included in a list.
         *
         * @param zipEntry the {@link ZipEntry} to be tested
         * @return {@ code true} if {@link ZipEntry} should be included, {@code false} otherwise
         */
        boolean accept(ZipEntry zipEntry);

        /**
         * Get the name of the specified {@link ZipEntry}; in other words, this method allows
         * to rename the specified {@link ZipEntry}.
         * <p>
         * Typical implementation simply returns {@link ZipEntry#getName() original name}.
         * @param zipEntry the {@link ZipEntry} to be got name of
         * @return the name of the specified {@link ZipEntry}
         */
        String getName(ZipEntry zipEntry);

    }

}
