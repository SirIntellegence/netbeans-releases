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
package org.netbeans.modules.masterfs.filebasedfs.fileobjects;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Test support for detection and reading of symbolic links.
 *
 * @author jhavlin
 */
public class FileObjSymlinkTest extends NbTestCase {

    public FileObjSymlinkTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        deleteSymlinks(getWorkDir());
        clearWorkDir();
    }

    public void testIsSymbolicLink() throws IOException {
        if (!checkSymlinksSupported()) {
            return;
        }
        File dir = getWorkDir();
        Path p = dir.toPath();
        Path data = p.resolve("data.dat");
        Path link = p.resolve("link.lk");
        data.toFile().createNewFile();
        Files.createSymbolicLink(link, data);
        FileObject dirFO = FileUtil.toFileObject(dir);
        dirFO.refresh();
        FileObject dataFO = dirFO.getFileObject("data.dat");
        FileObject linkFO = dirFO.getFileObject("link.lk");
        assertFalse(dataFO.isSymbolicLink());
        assertTrue(linkFO.isSymbolicLink());
    }

    /**
     * Test isRecursiveSymbolicLink method. Use this folder tree:
     * <pre>
     * - workdir
     *   - a
     *     - b
     *       - c (symlink to a)
     *       - d (symlink to e)
     *   - e
     * </pre>
     *
     * @throws java.io.IOException
     */
    public void testIsRecursiveSymbolicLink() throws IOException {
        if (!checkSymlinksSupported()) {
            return;
        }
        File dir = getWorkDir();
        File a = new File(dir, "a");
        File b = new File(a, "b");
        File c = new File(b, "c");
        File d = new File(b, "d");
        File e = new File(dir, "e");
        b.mkdirs();
        e.mkdirs();
        Files.createSymbolicLink(c.toPath(), a.toPath());
        Files.createSymbolicLink(d.toPath(), e.toPath());
        FileObject dirFO = FileUtil.toFileObject(dir);
        dirFO.refresh();
        FileObject cFO = dirFO.getFileObject("a/b/c");
        FileObject dFO = dirFO.getFileObject("a/b/d");
        assertTrue(FileUtil.isRecursiveSymbolicLink(cFO));
        assertFalse(FileUtil.isRecursiveSymbolicLink(dFO));
        assertFalse(FileUtil.isRecursiveSymbolicLink(dirFO));
    }

    /**
     * Test isRecursiveSymbolicLink method. Use this folder tree:
     * <pre>
     * - workdir
     *   - a
     *     - b
     *       - c (symlink to d)
     *   - d (symlink to a)
     * </pre>
     *
     * @throws java.io.IOException
     */
    public void testIsRecursiveSymbolicLinkIndirect() throws IOException {
        if (!checkSymlinksSupported()) {
            return;
        }
        File dir = getWorkDir();
        File a = new File(dir, "a");
        File b = new File(a, "b");
        File c = new File(b, "c");
        File d = new File(dir, "d");
        b.mkdirs();
        Files.createSymbolicLink(d.toPath(), a.toPath());
        Files.createSymbolicLink(c.toPath(), d.toPath());
        FileObject dirFO = FileUtil.toFileObject(dir);
        FileObject cFO = dirFO.getFileObject("a/b/c");
        assertTrue(FileUtil.isRecursiveSymbolicLink(cFO));
    }

    public void testReadSymbolicLinkAbsolute() throws IOException {
        if (!checkSymlinksSupported()) {
            return;
        }
        File dir = getWorkDir();
        File data = new File(dir, "data.dat");
        File link = new File(dir, "link.lnk");
        data.createNewFile();
        Files.createSymbolicLink(link.toPath(), data.toPath());
        FileObject linkFO = FileUtil.toFileObject(link);
        FileObject dataFO = linkFO.readSymbolicLink();
        assertNotSame(linkFO, dataFO);
        assertNotNull(dataFO);
        assertEquals("data.dat", dataFO.getNameExt());
    }

    public void testReadSymbolicLinkRelative() throws IOException {
        if (!checkSymlinksSupported()) {
            return;
        }
        File dir = getWorkDir();
        File folder = new File(dir, "folder");
        File link = new File(dir, "link");
        folder.mkdir();
        Path lp = Files.createSymbolicLink(link.toPath(), Paths.get("folder"));
        assertNotNull(lp);
        FileObject linkFO = FileUtil.toFileObject(link);
        assertNotNull(linkFO);
        FileObject dataFO = linkFO.readSymbolicLink();
        assertNotSame(linkFO, dataFO);
        assertNotNull(dataFO);
        assertEquals("folder", dataFO.getNameExt());
    }

    public void testReadSymbolicLinkPath() throws IOException {
        if (!checkSymlinksSupported()) {
            return;
        }
        File dir = getWorkDir();
        File data = new File(dir, "data.dat");
        File link = new File(dir, "link.lnk");
        data.createNewFile();
        Files.createSymbolicLink(link.toPath(), data.toPath());
        FileObject linkFO = FileUtil.toFileObject(link);
        assertTrue(linkFO.readSymbolicLinkPath().endsWith("data.dat"));
    }

    /**
     * Test getCanonicalFileObject method.
     *
     * Use this directory tree:
     * <pre>
     * - workdir
     *   - a
     *     - data.dat
     *   - b
     *     - link.lnk (symlink to data.dat)
     *   - c
     *     - link2.lnk (symlink to link.link)
     *   - d
     *     - folderLink (symlink to c)
     * </pre>
     * @throws java.io.IOException
     */
    public void testGetRealFileObject() throws IOException {
        if (!checkSymlinksSupported()) {
            return;
        }
        File dir = getWorkDir();
        File a = new File(dir, "a");
        File dataDat = new File(a, "data.dat");
        File b = new File(dir, "b");
        File linkLnk = new File(b, "link.lnk");
        File c = new File(dir, "c");
        File link2Lnk = new File(c, "link2.lnk");
        File d = new File(dir, "d");
        File folderLink = new File(d, "folderLink");
        a.mkdir();
        dataDat.createNewFile();
        b.mkdir();
        Files.createSymbolicLink(linkLnk.toPath(), dataDat.toPath());
        c.mkdir();
        Files.createSymbolicLink(link2Lnk.toPath(), linkLnk.toPath());
        d.mkdir();
        Files.createSymbolicLink(folderLink.toPath(), c.toPath());
        FileObject dirFO = FileUtil.toFileObject(dir);
        dirFO.refresh();
        FileObject fo = dirFO.getFileObject("d/folderLink/link2.lnk");
        assertNotNull(fo);
        FileObject realFO = fo.getCanonicalFileObject();
        assertNotNull(realFO);
        assertTrue(realFO.getPath().endsWith("a/data.dat"));
    }

    /**
     * Recursively delete all symlinks in a directory.
     */
    private void deleteSymlinks(File root) {
        for (File f : root.listFiles()) {
            if (Files.isSymbolicLink(f.toPath())) {
                f.delete();
            } else if (f.isDirectory()) {
                deleteSymlinks(f);
            }
        }
    }

    private boolean checkSymlinksSupported() {
        File dir;
        try {
            dir = getWorkDir();
        } catch (IOException ex) {
            printSkipping();
            return false;
        }
        File a = new File(dir, "a");
        try {
            File lk = new File(dir, "lk");
            Files.createSymbolicLink(lk.toPath(), a.toPath());
            lk.delete();
            return true;
        } catch (RuntimeException e) {
        } catch (IOException ex) {
        }
        printSkipping();
        return false;
    }

    private void printSkipping() {
        System.out.println(
                "Symbolic links are not supported, skipping test " + getName());
    }
}
