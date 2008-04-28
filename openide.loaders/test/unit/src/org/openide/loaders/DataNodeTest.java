/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.openide.loaders;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Enumeration;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.junit.NbTestCase;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStatusEvent;
import org.openide.filesystems.FileSystem;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.filesystems.Repository;
import org.openide.util.Enumerations;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.test.MockLookup;

/** Test things about node delegates.
 * Note: if you mess with file status changes in this test, you may effectively
 * break the testLeakAfterStatusChange test.
 *
 * @author Jesse Glick
 */
public class DataNodeTest extends NbTestCase {
    Logger LOG;
    
    public DataNodeTest(String name) {
        super(name);
    }

    @Override
    protected Level logLevel() {
        return Level.FINE;
    }

    @Override
    protected void setUp() throws Exception {
        LOG = Logger.getLogger("test." + getName());
        clearWorkDir();
        MockLookup.setInstances(new Pool());
    }
    
    /** Test that for all examples to be found in the system file system,
     * the node delegate has the same object as a cookie from DataObject.class.
     * (It is fine to have a different object from a more specific cookie, as
     * may happen in the case of a data shadow.)
     * See jglick's message on nbdev as of 22 Jun 2001:
     * assertTrue(dataObject.getNodeDelegate().getCookie(DataObject.class)==dataObject)
     */
    public void testDataNodeHasObjectAsCookie() throws Exception {
        // First make sure some core is installed. This could be run inside or
        // outside a running IDE.
//        TopManager tm = TopManager.getDefault();
        // Now scan SFS for all DO's and check the assertion.
        FileSystem sfs = Repository.getDefault().getDefaultFileSystem();
        DataFolder top = DataFolder.findFolder(sfs.getRoot());
        Enumeration e = top.children(true);
        while (e.hasMoreElements()) {
            DataObject o = (DataObject)e.nextElement();
            if (o.getPrimaryFile().getPath().startsWith("UI/Services")) {
                continue;
            }
            LOG.fine("Testing " + o.getPrimaryFile());
            Node n = o.getNodeDelegate();
            DataObject o2 = n.getCookie(DataObject.class);
            assertEquals("Correct cookie from node delegate", o, o2);
        }
    }

    
    public void testDataNodeGetHtmlNameDoesNotInitializeAllFiles () throws Exception {
        org.openide.filesystems.FileSystem lfs = TestUtilHid.createLocalFileSystem(getWorkDir (), new String[] {
            "F.java", "F.form"
        });
        
        FSWithStatus fs = new FSWithStatus ();
        fs.setRootDirectory(org.openide.filesystems.FileUtil.toFile(lfs.getRoot()));
        
        DataObject obj = DataObject.find (fs.findResource("F.java"));
        
        String n = obj.getNodeDelegate ().getHtmlDisplayName ();
        assertNotNull ("FSWithStatus called", fs.lastFiles);
     
        assertEquals ("Primary entry created", 1, TwoPartLoader.get ().primary);
        if (TwoPartLoader.get ().secondary != 0) {
            try {
                assertEquals ("Secondary entry not", 0, TwoPartLoader.get ().secondary);
            } catch (Error t1) {
                Throwable t2 = TwoPartLoader.get ().whoCreatedSecondary;
                if (t2 != null) {
                    t1.initCause (t2);
                }
                throw t1;
            }
        }
        assertEquals ("Size is two", 2, fs.lastFiles.size ());
        assertEquals ("Now the secondary entry had to be created", 1, TwoPartLoader.get ().secondary);
    }

    
    public void testWhatIsAddedToNodeLookupIsByDefaultVisibleInDataObjectLookup() throws Exception {
        org.openide.filesystems.FileSystem lfs = TestUtilHid.createLocalFileSystem(getWorkDir (), new String[] {
            "F.java", "F.form"
        });
        
        FSWithStatus fs = new FSWithStatus ();
        fs.setRootDirectory(org.openide.filesystems.FileUtil.toFile(lfs.getRoot()));
        
        DataObject obj = DataObject.find (fs.findResource("F.java"));

        OpenCookie fromNode = obj.getNodeDelegate().getLookup().lookup(OpenCookie.class);
        assertNotNull("There is some cookie in node", fromNode);
        
        OpenCookie fromObject = obj.getLookup().lookup(OpenCookie.class);
        assertNotNull("There is a cookie in data object too", fromObject);
        
    }
    
    public void testDataNodeGetDataFromLookup() throws Exception {

        FileSystem sfs = Repository.getDefault().getDefaultFileSystem();
        DataFolder rootFolder = DataFolder.findFolder(sfs.getRoot());
        
        class C implements Node.Cookie {
        }
        
        String objectToLookup = "I like my Lookup";
        C cookieToLookup = new C();
        Lookup lookup = Lookups.fixed(new Object[] { objectToLookup, cookieToLookup });
        
        DataNode node = new DataNode(rootFolder, Children.LEAF, lookup);
        Object objectLookedUp = node.getLookup().lookup(String.class);
        assertEquals("object is going to be found", objectToLookup, objectLookedUp);
        assertEquals("cookie found.", cookieToLookup, node.getLookup().lookup(C.class));

        //assertNull("Cannot return string as cookie", node.getCookie(String.class));
        assertEquals("But C is found", cookieToLookup, node.getCookie(C.class));
        
        assertNull("Data object is not found in lookup", node.getLookup().lookup(DataObject.class));
        assertNull("DataObject not found in cookies as they delegate to lookup", node.getCookie(DataObject.class));
    }
    
    /**
     * Verifues that a DataObject/DataNode is not leaked after firing a status
     * change for one of its files. Note that this test used to fail only
     * for the very first DataNode changed in the JVM run so if there are tests
     * running before this one which fire a status change, this test may not
     * catch a real regression.
     */
    public void testLeakAfterStatusChange() throws Exception {
        org.openide.filesystems.FileSystem lfs = TestUtilHid.createLocalFileSystem(getWorkDir (), new String[] {
            "F.java", "F.form"
        });
        
        FSWithStatus fs = new FSWithStatus ();
        fs.setRootDirectory(org.openide.filesystems.FileUtil.toFile(lfs.getRoot()));
        
        FileObject fo = fs.findResource("F.java");
        DataObject obj = DataObject.find (fo);
        Node n = obj.getNodeDelegate ();
        fs.fireStatusChange(new FileStatusEvent(fs, fo, true, true));
        
        WeakReference refN = new WeakReference<Object>(n);
        WeakReference refD = new WeakReference<Object>(obj);
        n = null;
        obj = null;
        
        assertGC("Node released", refN);
        assertGC("DataObject released", refD);
    }
    
    private static final class FSWithStatus extends org.openide.filesystems.LocalFileSystem 
    implements FileSystem.HtmlStatus {
        public Set lastFiles;
        
        
        @Override
        public FileSystem.Status getStatus () {
            return this;
        }

        private void checkFirst (Set files) {
            lastFiles = files;
            assertNotNull ("There is first file", files.iterator ().next ());
        }
        
        public java.awt.Image annotateIcon(java.awt.Image icon, int iconType, java.util.Set files) {
            checkFirst (files);
            return icon;
        }

        public String annotateName(String name, java.util.Set files) {
            checkFirst (files);
            return name;
        }

        public String annotateNameHtml(String name, java.util.Set files) {
            checkFirst (files);
            return name;
        }
        
        void fireStatusChange(FileStatusEvent fse) {
            fireFileStatusChanged(fse);
        }
    } // end of FSWithStatus
    
    private static final class Pool extends DataLoaderPool {
        protected Enumeration<? extends DataLoader> loaders() {
            return Enumerations.singleton(TwoPartLoader.get());
        }
    }
    
    public static final class TwoPartLoader extends MultiFileLoader {
        public int primary;
        public int secondary;
        public Throwable whoCreatedSecondary;
        
        public static TwoPartLoader get () {
            return TwoPartLoader.findObject(TwoPartLoader.class, true);
        }
        
        public TwoPartLoader() {
            super(TwoPartObject.class.getName ());
        }
        protected String displayName() {
            return "TwoPart";
        }
        protected FileObject findPrimaryFile(FileObject fo) {
            return org.openide.filesystems.FileUtil.findBrother(fo, "java");
        }
        protected MultiDataObject createMultiObject(FileObject primaryFile) throws DataObjectExistsException, IOException {
            return new TwoPartObject(this, primaryFile);
        }
        protected MultiDataObject.Entry createPrimaryEntry(MultiDataObject obj, FileObject primaryFile) {
            primary++;
            return new FileEntry.Folder(obj, primaryFile);
        }
        protected MultiDataObject.Entry createSecondaryEntry(MultiDataObject obj, FileObject secondaryFile) {
            secondary++;
            whoCreatedSecondary = new Throwable ("Secondary should not be created");
            return new FileEntry(obj, secondaryFile);
        }
    }
    public static final class TwoPartObject extends MultiDataObject {
        public TwoPartObject(TwoPartLoader l, FileObject folder) throws DataObjectExistsException {
            super(folder, l);
        }

        @Override
        protected Node createNodeDelegate() {
            return new DN(this);
        }
    }
    
    private static final class DN extends DataNode {
        public DN(TwoPartObject obj) {
            super(obj, Children.LEAF);
        }
        
        @Override
        public <T extends Node.Cookie> T getCookie(Class<T> clazz) {
            if (clazz.isAssignableFrom(OpenCookie.class)) {
                return clazz.cast(new OpenCookie () {
                    public void open() {
                    }
                });
            }
            return super.getCookie(clazz);
        }
    }
    
}
