/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.openide.loaders;

import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.cookies.InstanceCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.Repository;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeEvent;
import org.openide.nodes.NodeListener;
import org.openide.nodes.NodeMemberEvent;
import org.openide.nodes.NodeReorderEvent;
import org.openide.nodes.NodeTransfer;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.datatransfer.NewType;

/** A folder containing data objects.
* Is actually itself a data object, whose primary (and only) file object
* is a file folder.
* <p>Has special support for determining the sorting of the folder,
* or even explicit ordering of the children.
*
* @author Jaroslav Tulach, Petr Hamernik
*/
public class DataFolder extends MultiDataObject implements DataObject.Container {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -8244904281845488751L;

    /** Name of property that holds children of this node. */
    public static final String PROP_CHILDREN = DataObject.Container.PROP_CHILDREN;

    /** Name of property which decides sorting mode. */
    public static final String PROP_SORT_MODE = "sortMode"; // NOI18N

    /** name of extended attribute for order of children */
    static final String EA_SORT_MODE = "OpenIDE-Folder-SortMode"; // NOI18N
    /** name of extended attribute for order of children */
    static final String EA_ORDER = "OpenIDE-Folder-Order"; // NOI18N

    /** Name of property for order of children. */
    public static final String PROP_ORDER = "order"; // NOI18N
    /** Name of set with sorting options. */
    public static final String SET_SORTING = "sorting"; // NOI18N

    /** Icon resource string for folder node */
    private static final String FOLDER_ICON_BASE =
        "org/openide/loaders/defaultFolder.gif"; // NOI18N

    /** name of a shadow file for a root */
    private static final String ROOT_SHADOW_NAME = "Root"; // NOI18N

    /** listener that contains array of children
    * Also represents the folder as the node delegate.
    */
    private FolderList list;
    
    /** Listener for changes in FolderList */
    private PropertyChangeListener pcl;

    private DataTransferSupport dataTransferSupport = new Paste ();
    
    /** Create a data folder from a folder file object.

    * @deprecated This method should not be used in client code.
    * If you are searching for a <code>DataFolder</code> for
    * a FileObject use {@link DataFolder#findFolder} factory method.
    *
    * @param fo file folder to work on
    * @exception DataObjectExistsException if there is one already
    * @exception IllegalArgumentException if <code>fo</code> is not folder
    */
    public DataFolder (FileObject fo)
    throws DataObjectExistsException, IllegalArgumentException {
        this(fo, DataLoaderPool.getFolderLoader ());
    }

    /** Create a data folder from a folder file object.
    *
    * @param fo file folder to work on
    * @param loader data loader for this data object
    * @exception DataObjectExistsException if there is one already
    * @exception IllegalArgumentException if <code>fo</code> is not folder
    */
    protected DataFolder (FileObject fo, MultiFileLoader loader)
    throws DataObjectExistsException, IllegalArgumentException {
        this (fo, loader, true);
    }

    /** Create a data folder from a folder file object.
    * @param fo file folder to work on
    * @param loader data loader for this data object
    * @exception DataObjectExistsException if there is one already
    * @exception IllegalArgumentException if <code>fo</code> is not folder
    * @deprecated Since 1.13 do not use this constructor, it is for backward compatibility only.
    */
    protected DataFolder (FileObject fo, DataLoader loader)
    throws DataObjectExistsException, IllegalArgumentException {
        super (fo, loader);
        init(fo, true);
    }
    
    /** Create a data folder from a folder file object.
    * @param fo file folder to work on
    * @param loader data loader for this data object
    * @param attach listen to changes?
    * @exception DataObjectExistsException if there is one already
    * @exception IllegalArgumentException if <code>fo</code> is not folder
    */
    private DataFolder (FileObject fo, MultiFileLoader loader, boolean attach)
    throws DataObjectExistsException, IllegalArgumentException {
        super (fo, loader);
        init(fo, attach);
    }

    /** Perform initialization after construction.
    * @param fo file folder to work on
    * @param attach listen to changes?
    */
    private void init(FileObject fo, boolean attach) throws IllegalArgumentException {
        if (!fo.isFolder ()) {
            // not folder => throw an exception
            throw new IllegalArgumentException ("Not folder: " + fo); // NOI18N
        }
        list = reassignList (fo, attach);
    }
    
    /** Attaches a listener to the folder list, removes any previous one if registered.
     * @param fo the new primary file we should listen on
     * @param attach really attache listener
     */
    private FolderList reassignList (FileObject fo, boolean attach) {
        // creates object that handles all elements in array and
        // assignes it to the
        FolderList list = FolderList.find (fo, true);

        if (attach) {
            pcl = new ListPCL ();
            list.addPropertyChangeListener (org.openide.util.WeakListeners.propertyChange (pcl, list));
        }
        
        return list;
    }
        

    /** Helper method to find or create a folder of a given path.
    * Tries to find such a subfolder, or creates it if it needs to.
    *
    * @param folder the folder to start in
    * @param name a subfolder path (e.g. <code>com/mycom/testfolder</code>)
    * @return a folder with the given name
    * @exception IOException if the I/O fails
    */
    public static DataFolder create (DataFolder folder, String name) throws IOException {
        StringTokenizer tok = new StringTokenizer (name, "/"); // NOI18N
        while (tok.hasMoreTokens ()) {
            String piece = tok.nextToken ();
            if (! confirmName (piece)) {
                throw new IOException (NbBundle.getMessage (DataFolder.class, "EXC_WrongName", piece));
            }
        }
        return DataFolder.findFolder (FileUtil.createFolder (folder.getPrimaryFile (), name));
    }

    /** Set the sort mode for the folder.
    * @param mode an constant from {@link DataFolder.SortMode}
    * @exception IOException if the mode cannot be set
    */
    public synchronized final void setSortMode (SortMode mode) throws IOException {
        SortMode old = getOrder ().getSortMode ();
        getOrder ().setSortMode (mode);
        firePropertyChange (PROP_SORT_MODE, old, getOrder ().getSortMode ());
    }

    /** Get the sort mode of the folder.
    * @return the sort mode
    */
    public final SortMode getSortMode () {
        return getOrder ().getSortMode ();
    }

    /** Set the order of the children.
     * The provided array defines
    * the order of some children for the folder. Such children
    * will be returned at the beginning of the array returned from
    * {@link #getChildren}. If there are any other children, they
    * will be appended to the array.
    *
    * @param arr array of data objects (children of this
    *   folder) to define the order; or <code>null</code> if any particular ordering should
    *   be cancelled
    *
    * @exception IOException if the order cannot be set
    *
    */
    public synchronized final void setOrder (DataObject[] arr) throws IOException {
        getOrder ().setOrder (arr);
        firePropertyChange (PROP_ORDER, null, null);
    }

    /** Getter for order object.
    * @return order of children
    */
    private FolderOrder getOrder () {
        return FolderOrder.findFor (getPrimaryFile ());
    }

    /** Get the name of the data folder.
    * <p>This implementation uses the name and extension of the primary file.
    * @return the name
    */
    public String getName () {
        return getPrimaryFile ().getNameExt ();
    }
    
    /** Get the children of this folder.
    * @return array of children
    */
    public DataObject[] getChildren () {
        return list.getChildren ();
    }

    /** Getter for list of children.
    * @param filter filter to notify about addition of new objects
    */
    final List getChildrenList () {
        return list.getChildrenList ();
    }

    /** Computes list of children asynchronously
    * @param l listener to notify about the progress
    * @return task that will handle the computation
    */
    final RequestProcessor.Task computeChildrenList (FolderListListener l) {
        return list.computeChildrenList (l);
    }

    /** Get enumeration of children of this folder.
    * @return enumeration of {@link DataObject}s
    */
    public Enumeration children () {
        return Collections.enumeration (getChildrenList ());
    }

    /** Enumerate all children of this folder. If the children should be enumerated
    * recursively, first all direct children are listed; then children of direct subfolders; and so on.
    *
    * @param rec whether to enumerate recursively
    * @return enumeration of type <code>DataObject</code>
    */
    public Enumeration children (final boolean rec) {
        if (!rec) {
            return children();
        }
        
        class Processor implements org.openide.util.Enumerations.Processor {
            /** @param o processes object by adding its children to the queue */
            public Object process (Object o, Collection toAdd) {
                DataObject dataObj = (DataObject)o;
                if (rec && dataObj instanceof DataFolder) {
                    toAdd.addAll (Arrays.asList (((DataFolder)dataObj).getChildren()));
                }
                return o;
            }
        }
        Enumeration en = org.openide.util.Enumerations.queue (
            org.openide.util.Enumerations.array (getChildren ()),
            new Processor ()
        );
        return en;
    }

    /** Create node representative for this folder.
    */
    protected synchronized Node createNodeDelegate () {
        return new FolderNode();
    }

    private final class ClonedFilter extends FilterNode {
        private DataFilter filter;
        private int hashCode = -1; // We need to remember the hash code in 
                              // order to keep it constant fix for
        
        public ClonedFilter (Node n, DataFilter filter) {
            super (n, DataFolder.this.createNodeChildren (filter));
            this.filter = filter;
        }
        public ClonedFilter (DataFilter filter) {
            this (DataFolder.this.getNodeDelegate (), filter);
        }
        public Node cloneNode () {
            if (isValid()) {
                return new ClonedFilter (filter);
            } else {
                return super.cloneNode();
            }
        }
        public Node.Handle getHandle () {
            return new ClonedFilterHandle (DataFolder.this, filter);
        }
        public boolean equals (Object o) {
            if (o == null) {
                return false;
            } else if (o == this) {
                return true;
            } else if (o instanceof FolderNode) {
                FolderNode fn = (FolderNode) o;
                if (fn.getCookie (DataFolder.class) != DataFolder.this) return false;
                org.openide.nodes.Children ch = fn.getChildren ();
                return (ch instanceof FolderChildren) &&
                    ((FolderChildren) ch).getFilter ().equals (filter);
            } else if (o instanceof ClonedFilter) {
                ClonedFilter cf = (ClonedFilter) o;
                return cf.getCookie (DataFolder.class) == DataFolder.this &&
                    cf.filter.equals (filter);
            } else {
                return false;
            }
        }
        public int hashCode () {
            if ( hashCode == -1 ) {
                if ( isValid() ) {
                    hashCode = getNodeDelegate().hashCode();
                }
                else {
                    hashCode = super.hashCode();
                }
                
                if ( hashCode == -1 ) {
                    hashCode = -2;
                }
                
            }            
            return hashCode;
            
        }
    }
    private final static class ClonedFilterHandle implements Node.Handle {
        private final static long serialVersionUID = 24234097765186L;
        private DataObject folder;
        private DataFilter filter;
        public ClonedFilterHandle (DataFolder folder, DataFilter filter) {
            this.folder = folder;
            this.filter = filter;
        }
        public Node getNode () throws IOException {
            if (folder instanceof DataFolder) {
                return ((DataFolder)folder).new ClonedFilter (filter);
            } else {
                throw new java.io.InvalidObjectException(
                    folder == null ? "" : folder.toString() // NOI18N
                );
            }
        }
    }
    /** This method allows DataFolder to filter its nodes.
    *
    * @param filter filter for subdata objects
    * @return the node delegate (without parent) for this data object
    */
    Node getClonedNodeDelegate (DataFilter filter) {
        Node n = getNodeDelegate ();
        Children c = n.getChildren ();
        // #7362: relying on subclassers to override createNodeChildren is ugly...
        if (c.getClass () == FolderChildren.class) {
            DataFilter f = ((FolderChildren) c).getFilter ();
            if (f == DataFilter.ALL) {
                // Either createNodeDelegate was not overridden; or
                // it provided some node with the same children as
                // DataFolder would have anyway. Filter the children.
                return new ClonedFilter (n, filter);
            } else if (filter != DataFilter.ALL && filter != f) {
                // Tricky. createNodeDelegate was overridden, and it is
                // producing FolderChildren with some special filter.
                // Apply both the subclass's filter and this additional one.
                return new ClonedFilter (n, filterCompose (f, filter));
            } else {
                // Subclass provided FolderChildren with some special filter,
                // and we are not trying to filter specially. Let the subclass
                // display as usual.
                return n.cloneNode ();
            }
        } else {
            // We have some DataFolder subclass with idiosyncratic children.
            // Play it safe and let it display what it wants.
            return n.cloneNode ();
        }
    }
    
    /** Logically compose two filters: accept the intersection. */
    private static DataFilter filterCompose (final DataFilter f1, final DataFilter f2) {
        if (f1.equals (f2)) {
            return f1;
        } else {
            return new DataFilter () {
                public boolean acceptDataObject (DataObject obj) {
                    return f1.acceptDataObject (obj) && f2.acceptDataObject (obj);
                }
            };
        }
    }

    /** Support method to obtain a children object that
    * can be added to any {@link Node}. The provided filter can be
    * used to exclude some objects from the list.
    * <p><strong>Overriding this method is deprecated!</strong>
    * @param filter filter of data objects
    * @return children object representing content of this folder
    */
    public /* XXX final */ Children createNodeChildren (DataFilter filter) {
        return new FolderChildren (this, filter);
    }

    /* Getter for delete action.
    * @return true if the object can be deleted
    */
    public boolean isDeleteAllowed () {
        return isRenameAllowed ();
    }

    /* Getter for copy action.
    * @return true if the object can be copied
    */
    public boolean isCopyAllowed () {
        return true;
    }

    /* Getter for move action.
    * @return true if the object can be moved
    */
    public boolean isMoveAllowed () {
        return isRenameAllowed ();
    }

    /* Getter for rename action.
    * @return true if the object can be renamed
    */
    public boolean isRenameAllowed () {
        FileObject fo = getPrimaryFile ();
        return !fo.isRoot() && fo.canWrite();
    }

    /* Help context for this object.
    * @return help context
    */
    public HelpCtx getHelpCtx () {
        return null;
    }

    /** Create a folder for a specified file object.
    * @param fo file object
    * @return folder for the file object
    * @exception IllegalArgumentException if the file object is not folder
    */
    public static DataFolder findFolder (FileObject fo) {
        DataObject d;
        try {
            d = DataObject.find(fo);
        } catch (DataObjectNotFoundException e) {
            throw (IllegalArgumentException)new IllegalArgumentException(e.toString()).initCause(e);
        }
        if (!(d instanceof DataFolder)) {
            throw new IllegalArgumentException("Not a DataFolder: " + fo + " (was a " + d.getClass().getName() + ") (file is folder? " + fo.isFolder() + ")"); // NOI18N
        }
        return (DataFolder)d;
    }

    /** Finds a DataObject.Container representing given folder.
    * @param fo file object (must be folder)
    * @return the container for the file object
    * @exception IllegalArgumentException if the file object is not folder
    *
    * @since 1.11
    */
    public static DataObject.Container findContainer (FileObject fo) {
        if (fo.isFolder ()) {
            return FolderList.find (fo, true);
        } else {
            throw new IllegalArgumentException ("Not a folder: " + fo); // NOI18N
        }
    }
    
    /* Copy this object to a folder.
     * The copy of the object is required to
    * be deletable and movable.
    *
    * @param f the folder to copy object to
    * @exception IOException if something went wrong
    * @return the new object
    */
    protected DataObject handleCopy (DataFolder f) throws IOException {
        testNesting(this, f);
        
        Enumeration en = children ();

        DataFolder newFolder = (DataFolder)super.handleCopy (f);

        while (en.hasMoreElements ()) {
            try {
                DataObject obj = (DataObject)en.nextElement ();
                if (obj.isCopyAllowed()) { 
                    obj.copy (newFolder);
                } else {
                    // data object can not be copied, inform user
                    ErrorManager.getDefault().log(ErrorManager.USER,
                        NbBundle.getMessage(DataFolder.class,
			    "FMT_CannotCopyDo", obj.getName() )
                    );
                }
            } catch (IOException ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }

        return newFolder;
    }

    /**
     * Ensure that given folder is not parent of targetFolder. Also
     * ensure that they are not equal.
     */
    static void testNesting(DataFolder folder, DataFolder targetFolder) throws IOException {
        if (targetFolder.equals(folder)) {
            throw (IOException) ErrorManager.getDefault().annotate(
                new IOException("Error Copying File or Folder"), //NOI18N
                ErrorManager.WARNING, null, NbBundle.getMessage(DataFolder.class, "EXC_CannotCopyTheSame", folder.getName()) //NOI18N
                , null, null);
        } else {
            DataFolder testFolder = targetFolder.getFolder();
            while (testFolder != null) {
                if (testFolder.equals(folder)) {
                    throw (IOException) ErrorManager.getDefault().annotate(
                        new IOException("Error copying file or folder: " + 
                        folder.getPrimaryFile() + " cannot be copied to its subfolder " +
                        targetFolder.getPrimaryFile()), //NOI18N
                        ErrorManager.WARNING, null, NbBundle.getMessage(DataFolder.class, "EXC_CannotCopySubfolder", folder.getName()) //NOI18N
                        , null, null);
                }
                testFolder = testFolder.getFolder();
            }
        }
    }
    
    /* Deals with deleting of the object. Must be overriden in children.
    * @exception IOException if an error occures
    */
    protected void handleDelete () throws IOException {
        Enumeration en = children ();
        FileLock lightWeightLock = null;//#43278
        try {
            lightWeightLock = createLightWeightLock(this);
            while (en.hasMoreElements ()) {
                DataObject obj = (DataObject)en.nextElement ();
                if (obj.isValid ()) {
                    obj.delete ();
                }
            }
        } catch (IOException iex) {
            /** Annotates exception and throws again*/
            FileObject fo = getPrimaryFile();
            String message = NbBundle.getMessage(DataFolder.class, "EXC_CannotDelete2", FileUtil.getFileDisplayName(fo));
            ErrorManager.getDefault().annotate(iex, message);
            throw iex;
        } finally {
            if (lightWeightLock != null) {
                lightWeightLock.releaseLock();
            }
        }
        
        super.handleDelete ();        
    }
    
    private static FileLock createLightWeightLock(DataFolder df) {//#43278
        FileObject fo = df.getPrimaryFile();
        assert fo != null;
        Object o = fo.getAttribute("LIGHTWEIGHT_LOCK_SET");//NOI18N
        assert o == null || (o instanceof FileLock) : fo.toString();    
        return (FileLock)o;
    }

    /* Handles renaming of the object.
    * Must be overriden in children.
    *
    * @param name name to rename the object to
    * @return new primary file of the object
    * @exception IOException if an error occures
    */
    protected FileObject handleRename (final String name) throws IOException {
        if (! confirmName (name)) {
            throw new IOException ("bad name: " + name) { // NOI18N
                    public String getLocalizedMessage () {
                        return NbBundle.getMessage (DataFolder.class, "EXC_WrongName", name);
                    }
                };
        }
        return super.handleRename (name);
    }
    
    /* Handles move of the object. Must be overriden in children. Since 1.13 move operation
    * behaves similar like copy, it merges folders whith existing folders in target location.
    * @param df target data folder
    * @return new primary file of the object
    * @exception IOException if an error occures
    */
    protected FileObject handleMove (DataFolder df) throws IOException {
        FileObject originalFolder = getPrimaryFile ();
        FileLock lock = originalFolder.lock();
        List backup = saveEntries();
        
        try {
            // move entries (FolderEntry creates new folder when moved)

            FileObject newFile = super.handleMove (df);
            
            DataFolder newFolder = null;
            boolean dispose = false;
            boolean keepAlive = false;

            /* 
             * The following code is a partial bugfix of the issue #8705.
             * Please note that this problem is hardly reproducible by users,
             * but only by unit test. 
             *
             * The root of the problem is that it is not possible to disable 
             * recognizing of DataObjects for some time. Couple of lines above 
             * the file object (destination folder) is created using 
             * super.handleMove(df) and couple of lines below DataFolder if created
             * for this file object using createMultiObject.
             * The problems are:
             * 1) Temporary DataFolder created as destination folder is used only
             *    during copying the original (this) DataFolder content.
             *    Then is is marked as not valid using setValid(false). The original
             *    datafolder switches its primary file to the destination file object.
             *    The problem occurs, when some other thread takes the node representing
             *    the temporary folder.
             *    Solution: Special DataFolder that delegates nodeDelegate and 
             *          clonedNodeDelegate to the original folder.
             *
             * 2) There is still some sort time between creating of fileobject 
             *    and its datafolder. Another thread can ask for parent folder's
             *    dataobjects and it forces creation of "normal" datafolder, 
             *    not the special one (with delegating nodes). Then it is necessary
             *    to dispose the normal DataFolder and try to create our one.
             *    To prevent infinite look there is a count down initialy set
             *    to 20 repeats. Acording to results of DataFolderMoveTest it should
             *    help. When this solution fails it only means that in some rare
             *    cases some DataNode might represent invalid DataFolder. It is 
             *    not possible to delete such a node in explorer for instance.
             * 
             * This is really strange hack (especially the 2nd part), and it is 
             * necessary to think about better solution for NetBeans 4.0 
             * data system architecture changes.
             *
             */
            final int COUNT_DOWN_INIT = 20;
            int countDown = COUNT_DOWN_INIT;
            while (countDown >= 0) {
                countDown--;
                try {
                    // resolve temporary object for moving into
                    Object loader = getMultiFileLoader ();
                    assert loader instanceof DataLoaderPool.FolderLoader : "This has to be FolderLoader: " + loader + " for " + getPrimaryFile (); // NOI18N
                    DataLoaderPool.FolderLoader folderLoader = (DataLoaderPool.FolderLoader) loader;
                    newFolder = (DataFolder) DataObjectPool.createMultiObject (folderLoader, newFile, this);
                    dispose = false;
                    break;
                } catch (DataObjectExistsException e) {
                    // object already exists, get it and remember we should be discarded
                    newFolder = (DataFolder)e.getDataObject ();
                    newFolder.dispose();
                    dispose = true;
                }
            }

            // move all children
            Enumeration en = children ();

            while (en.hasMoreElements ()) {
                try {
                    DataObject obj = (DataObject)en.nextElement ();
                    if (obj.isMoveAllowed ()) {
                        obj.move (newFolder);
                    } else {
                        keepAlive = true;
                        
                        // data object can not be moved, inform user
                        ErrorManager.getDefault().log(ErrorManager.USER,
                            NbBundle.getMessage (DataFolder.class,
				    "FMT_CannotMoveDo", obj.getName ())
                        );
                    }
                } catch (IOException ex) {
                    keepAlive = true;
                    ErrorManager.getDefault().notify(ex);
                }
            }

            if (keepAlive) {
                // some children couldn't be moved -> folder shouldn't be moved
                restoreEntries (backup);
                list.refresh ();
                return originalFolder;
            }

            // remove original folder
            try {
                originalFolder.delete (lock);
            } catch (IOException e) {
                Throwable t = ErrorManager.getDefault ().annotate (e, DataObject.getString ("EXC_folder_delete_failed")); // NOI18N
                ErrorManager.getDefault ().notify (t);
            }

            if (dispose) {
                // current object will be discarded, target already existed
                try {
                    setValid (false);
                    newFile = originalFolder;
                } catch (PropertyVetoException e) {
                    // ignore, just repair entries
                    restoreEntries (backup);
                    newFile = getPrimaryEntry ().getFile ();
                }
            } else {
                // dispose temporary folder and place itself instead of it
                // call of changePrimaryFile and dispose must be in this order 
                // to silently change DataFolders in the DataObjectPool
                item.changePrimaryFile (newFile);
                newFolder.dispose ();
                list = reassignList (newFile, true);
            }

            return newFile;
        } finally {
            lock.releaseLock();
        }
    }

    /* Creates new object from template.
    * @param f folder to create object in
    * @return new data object
    * @exception IOException if an error occured
    */
    protected DataObject handleCreateFromTemplate (
        DataFolder f, String name
    ) throws IOException {
        DataFolder newFolder = (DataFolder)super.handleCreateFromTemplate (f, name);
        Enumeration en = children ();

        while (en.hasMoreElements ()) {
            try {
                DataObject obj = (DataObject)en.nextElement ();
                obj.createFromTemplate (newFolder);
            } catch (IOException ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }

        return newFolder;
    }

    /** Creates shadow for this object in specified folder (overridable in subclasses).
     * <p>The default
    * implementation creates a reference data shadow and pastes it into
    * the specified folder.
    *
    * @param f the folder to create a shortcut in
    * @return the shadow
    */
    protected DataShadow handleCreateShadow (DataFolder f) throws IOException {
        // #33871 - prevent creation of recursive folder structure
        testNesting(this, f);
        
        String name;
        if (getPrimaryFile ().isRoot ()) {
            name = FileUtil.findFreeFileName (
                       f.getPrimaryFile (), ROOT_SHADOW_NAME, DataShadow.SHADOW_EXTENSION
                   );
        } else {
            name = null;
        }

        return DataShadow.create (f, name, this);
    }

    /** Merge folder on move or copy when it exists in target location.
     * @returns <code>true</code>
     * @since 1.13
     */
    boolean isMergingFolders(FileObject who, FileObject targetFolder) {
        return !targetFolder.equals (who.getParent ());
    }
    
    /** Support for index cookie for folder nodes.
    */
    public static class Index extends org.openide.nodes.Index.Support {

        /** Asociated data folder */
        private DataFolder df;
        /** node to be associated with */
        private Node node;
        /** change listener */
        private Listener listener;

        /** Create an index cookie associated with a data folder.
         * @param df the data folder
         * @deprecated Please explicitly specify a node to be safe.
        */
        public Index(final DataFolder df) {
            this (df, df.getNodeDelegate ());
        }

        /** Create an index cookie associated with a data folder.
        * @param df the data folder
        * @param node node to be associated with. subnodes of this node will be returned, etc.
        */
        public Index(final DataFolder df, Node node) {
            this.df = df;
            this.node = node;
            listener = new Listener ();
            node.addNodeListener (org.openide.nodes.NodeOp.weakNodeListener (listener, node));
        }

        /* Returns count of the nodes.
        */
        public int getNodesCount () {
            return node.getChildren().getNodes(FolderChildren.checkChildrenMutex()).length;
        }

        /* Returns array of subnodes
        * @return array of subnodes
        */
        public Node[] getNodes () {
            return node.getChildren().getNodes(FolderChildren.checkChildrenMutex());
        }

        /* Reorders all children with given permutation.
        * @param perm permutation with the length of current nodes
        * @exception IllegalArgumentException if the perm is not
        *  valid permutation
        */
        public void reorder (int[] perm) {
            // #11809: the children of the node may not directly match the data folder
            // children. Specifically, it is legal to reorder a set of nodes where
            // each node has a distinct data object cookie, each object being a child of
            // this folder, but there are some objects missing. In such a case, the
            // specified objects are permuted according to the node permutation, while
            // other objects in the folder are left in their original positions and order.
            DataObject[] curObjs = df.getChildren();
            DataObject[] newObjs = new DataObject[curObjs.length];
            Node[] nodes = getNodes ();
            if (nodes.length != perm.length) {
                throw new IllegalArgumentException ("permutation of incorrect length: " + perm.length + " rather than " + nodes.length); // NOI18N
            }
            
            // hashtable from names of nodes to their data objects for
            // nodes that do not express their data object as their cookie
            HashMap names = new HashMap (2 * curObjs.length);
            for (int i = 0; i < curObjs.length; i++) {
                Node del = curObjs[i].getNodeDelegate ();
                if (del.getCookie (DataObject.class) == null) {
                    names.put (del.getName (), curObjs[i]);
                }
            }
            
            DataObject[] dperm = new DataObject[perm.length];
            for (int i = 0; i < perm.length; i++) {
                DataObject d = (DataObject) nodes[i].getCookie (DataObject.class);
                
                if (d == null) {
                    // try to scan the names table too
                    d = (DataObject)names.get (nodes[i].getName ());
                }
                
                
                if (d == null) {
                    throw new IllegalArgumentException ("cannot reorder node with no DataObject: " + nodes[i]); // NOI18N
                }
                if (d.getFolder () != df) {
                    throw new IllegalArgumentException ("wrong folder for: " + d.getPrimaryFile () + " rather than " + df.getPrimaryFile ()); // NOI18N
                }
                dperm[perm[i]] = d;
            }
            Set dpermSet = new HashSet (Arrays.asList (dperm)); // Set<DataObject>
            if (dpermSet.size () != dperm.length) {
                throw new IllegalArgumentException ("duplicate DataObject's among reordered childen"); // NOI18N
            }
            int dindex = 0;
            for (int i = 0; i < curObjs.length; i++) {
                if (dpermSet.remove (curObjs[i])) {
                    newObjs[i] = dperm[dindex++];
                } else {
                    // Not reordered, leave where it was.
                    newObjs[i] = curObjs[i];
                }
            }
            try {
                df.setOrder(newObjs);
            } catch (IOException ex) {
                ErrorManager.getDefault ().annotate (ex, DataObject.getString ("EXC_ReorderFailed")); // NOI18N
                ErrorManager.getDefault ().notify (ex);
            }
        }

        /* Invokes a dialog for reordering subnodes.
        */
        public void reorder () {
            Index.Support.showIndexedCustomizer(this);
        }

        /** Fires notification about reordering to all
        * registered listeners.
        */
        void fireChangeEventAccess () {
            fireChangeEvent (new ChangeEvent (this));
        }

        /** Listener to change of children of the folder.
        */
        private final class Listener extends Object implements NodeListener {
            Listener() {}
            /** Change of children?
            */
            public void propertyChange (PropertyChangeEvent ev) {
            }
            /** Fired when the node is deleted.
            * @param ev event describing the node
            */
            public void nodeDestroyed(NodeEvent ev) {
            }

            /** Fired when the order of children is changed.
            * @param ev event describing the change
            */
            public void childrenReordered(NodeReorderEvent ev) {
                fireChangeEventAccess ();
            }
            /** Fired when a set of children is removed.
            * @param ev event describing the action
            */
            public void childrenRemoved(NodeMemberEvent ev) {
                fireChangeEventAccess ();
            }
            /** Fired when a set of new children is added.
            * @param ev event describing the action
            */
            public void childrenAdded(NodeMemberEvent ev) {
                fireChangeEventAccess ();
            }
        } // end of Listener

    } // end of Index inner class


    /** Type-safe enumeration of sort modes for data folders.
    */
    public abstract static class SortMode extends Object implements Comparator {
        /** Objects are unsorted. */
        public static final SortMode NONE = new FolderComparator (FolderComparator.NONE);

        /** Objects are sorted by their names. */
        public static final SortMode NAMES = new FolderComparator (FolderComparator.NAMES);

        /** Objects are sorted by their types and then by names. */
        public static final SortMode CLASS = new FolderComparator (FolderComparator.CLASS);

        /** Folders go first (sorted by name) followed by non-folder
        * objects sorted by name.
        */
        public static final SortMode FOLDER_NAMES = new FolderComparator (FolderComparator.FOLDER_NAMES);
        
        /**
         * Folders go first (sorted by name) followed by files sorted by decreasing
         * last modification time.
         * @since org.openide.loaders 4.10
         */
        public static final SortMode LAST_MODIFIED = new FolderComparator(FolderComparator.LAST_MODIFIED);

        /**
         * Folders go first (sorted by name) followed by files sorted by decreasing size.
         * @since org.openide.loaders 4.10
         */
        public static final SortMode SIZE = new FolderComparator(FolderComparator.SIZE);

        /** Method to write the sort mode to a folder's attributes.
        * @param folder folder write this mode to
        */
        void write (FileObject f) throws IOException {
            // Let it throw the IOException:
            //if (f.getPrimaryFile ().getFileSystem ().isReadOnly ()) return; // cannot write to read-only FS

            String x;
            if (this == FOLDER_NAMES) x = "F"; // NOI18N
            else if (this == NAMES) x = "N"; // NOI18N
            else if (this == CLASS) x = "C"; // NOI18N
            else if (this == LAST_MODIFIED) x = "M"; // NOI18N
            else if (this == SIZE) x = "S"; // NOI18N
            else x = "O"; // NOI18N

            f.setAttribute (EA_SORT_MODE, x);
        }

        /** Reads sort mode for given folder.
        */
        static SortMode read (FileObject f) {
            String x = (String)f.getAttribute (EA_SORT_MODE);
            if (x == null || x.length () != 1) return FOLDER_NAMES;

            char c = x.charAt (0);
            switch (c) {
            case 'N': return NAMES;
            case 'C': return CLASS;
            case 'O': return NONE;
            case 'M': return LAST_MODIFIED;
            case 'S': return SIZE;
            case 'F':
            default:
                return FOLDER_NAMES;
            }
        }
    }

    /** true if the new folder name is acceptable */
    private static boolean confirmName (String folderName) {
        return folderName.indexOf ('/') == -1 && folderName.indexOf ('\\') == -1;
    }
    
    /** Gets an icon from UIManager and converts it to Image
     */
    private static Image icon2image(String key) {
        Object obj = UIManager.get(key);
        if (obj instanceof Image) {
            return (Image)obj;
        }
        
        if (obj instanceof Icon) {
            Icon icon = (Icon)obj;
            
            int height = icon.getIconHeight();
            int width = icon.getIconWidth();

            BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            icon.paintIcon(new JPanel(), bImage.getGraphics(), 0, 0);

            return bImage;
        }
        
        return null;
    }  

    /** array to cache images in */
    private static Image[] IMGS = new Image[2];

    static Image findIcon(int index, String k1, String k2) {
        if (IMGS[index] != null) {
            return IMGS[index];
        }

        Image i1 = icon2image(k1);
        if (i1 == null) {
            i1 = icon2image(k2);
        }

        IMGS[index] = i1;
        return i1;
    }
    
    /** Node for a folder.
    */
    public class FolderNode extends DataNode {
        /** Create a folder node with some children.
        * @param ch children to use for the node
        */
        public FolderNode (Children ch) {
            super (DataFolder.this, ch);
            setIconBaseWithExtension(FOLDER_ICON_BASE);
        }

        /** Create a folder node with default folder children.
        */
        protected FolderNode () {
            super (DataFolder.this, new FolderChildren (DataFolder.this));
            setIconBaseWithExtension(FOLDER_ICON_BASE);
        }
        

        /** Overrides folder icon to search for icon in UIManager table for
         * BeanInfo.ICON_COLOR_16x16 type, to allow for different icons
         * across Look and Feels.
         * Keeps possibility of icon annotations.
         */
        public Image getIcon (int type) {
            Image img = null;
            if (type == BeanInfo.ICON_COLOR_16x16) {
                // search for proper folder icon installed by core/windows module
                img = findIcon(0, "Nb.Explorer.Folder.icon", "Tree.closedIcon"); // NOI18N
            }
            if (img == null) {
                img = super.getIcon(type);
            } else {
                // give chance to annotate icon returned from UIManeger
                // copied from DataNode to keep the contract
                try {
                    DataObject obj = getDataObject();
                    img = obj.getPrimaryFile().getFileSystem().
                          getStatus().annotateIcon(img, type, obj.files());
                } catch (FileStateInvalidException e) {
                    // no fs, do nothing
                }
            }
            return img;
        }
        
        /** Overrides folder icon to search for icon in UIManager table for
         * BeanInfo.ICON_COLOR_16x16 type, to allow for different icons
         * across Look and Feels.
         * Keeps possibility of icon annotations.
         */
        public Image getOpenedIcon (int type) {
            Image img = null;
            if (type == BeanInfo.ICON_COLOR_16x16) {
                // search for proper folder icon installed by core/windows module
                img = findIcon(1, "Nb.Explorer.Folder.openedIcon", "Tree.openIcon"); // NOI18N
            }
            if (img == null) {
                img = super.getOpenedIcon(type);
            } else {
                // give chance to annotate icon returned from UIManeger
                // copied from DataNode to keep the contract
                try {
                    DataObject obj = getDataObject();
                    img = obj.getPrimaryFile().getFileSystem().
                    getStatus().annotateIcon(img, type, obj.files());
                } catch (FileStateInvalidException e) {
                    // no fs, do nothing
                }
            }
            return img;
        }
        

        public Node.Cookie getCookie (Class clazz) {
            if (clazz == org.openide.nodes.Index.class || clazz == Index.class) {
                //#33130 - enable IndexCookie only on SystemFileSystem
                // (also on apisupport layers...)
                try {
                    if (DataFolder.this.getPrimaryFile().getFileSystem() == 
                                Repository.getDefault().getDefaultFileSystem() ||
                            Boolean.TRUE.equals(DataFolder.this.getPrimaryFile().getAttribute("DataFolder.Index.reorderable"))) { // NOI18N
                        return new Index (DataFolder.this, this);
                    }
                } catch (FileStateInvalidException ex) {
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ex);
                }
            }
            return super.getCookie (clazz);
        }

        /* Adds properties for sorting.
         * @return the augmented property sheet
        */
        protected Sheet createSheet () {
            Sheet s = super.createSheet ();

            Sheet.Set ss = new Sheet.Set ();
            ss.setName (SET_SORTING);
            ss.setDisplayName (DataObject.getString ("PROP_sorting"));
            ss.setShortDescription (DataObject.getString ("HINT_sorting"));

            Node.Property p;

            p = new PropertySupport.ReadWrite (
                    PROP_SORT_MODE, SortMode.class,
                    DataObject.getString("PROP_sort"),
                    DataObject.getString("HINT_sort")
                ) {
                    public Object getValue () {
                        return DataFolder.this.getSortMode ();
                    }

                    public void setValue (Object o) throws InvocationTargetException {
                        try {
                            DataFolder.this.setSortMode ((SortMode)o);
                        } catch (IOException ex) {
                            throw new InvocationTargetException (ex);
                        }
                    }

                    public java.beans.PropertyEditor getPropertyEditor () {
                        return new SortModeEditor ();
                    }
                };
            ss.put (p);

            s.put (ss);
            return s;
        }
        
        /* No default action on data folder node.
         * @return null
         */
        public Action getPreferredAction() {
            return null;
        }
        

        /* New type for creating new subfolder.
        * @return array with one element
        */
        public NewType[] getNewTypes () {
            return new NewType[0];
/*          Commented out. Folder is now created via template.

            if (getPrimaryFile ().isReadOnly ()) {
                // no new types
                return new NewType[0];
            } else {
                return new NewType[] { new NewFolder () };
            }
 */
        }
        private synchronized FolderRenameHandler getRenameHandler() {
            Lookup.Result renameImplementations = Lookup.getDefault().lookup(new Lookup.Template(FolderRenameHandler.class));
            Collection handlers = renameImplementations.allInstances();
            if (handlers.size()==0)
                return null;
            if (handlers.size()>1)
                ErrorManager.getDefault().log(ErrorManager.WARNING, "Multiple instances of FolderRenameHandler found in Lookup; only using first one: " + handlers); //NOI18N
            return (FolderRenameHandler) handlers.iterator().next();
        }

        public void setName(String name) {
            FolderRenameHandler handler = getRenameHandler();
            if (handler == null) {
                super.setName(name);
            } else {
                handler.handleRename(DataFolder.this, name);
            }
        }
        
        /* May add some paste types for objects being added to folders.
        * May move data objects; copy them; create links for them; instantiate
        * them as templates; serialize instances; or create instance data objects
        * from instances, according to the abilities of the transferable.
        *
        * @param t transferable to use
        * @param s list of {@link PasteType}s
        */
        protected void createPasteTypes (Transferable t, java.util.List s) {
            super.createPasteTypes (t, s);
            if (getPrimaryFile().canWrite()) {
                dataTransferSupport.createPasteTypes (t, s);
            }
        }
    } // end of FolderNode

    /** New type for creation of new folder.
    */
    private final class NewFolder extends NewType {
        NewFolder() {}
        
        /** Display name for the creation action. This should be
        * presented as an item in a menu.
        *
        * @return the name of the action
        */
        public String getName() {
            return DataObject.getString ("CTL_NewFolder");
        }

        /** Help context for the creation action.
        * @return the help context
        */
        public HelpCtx getHelpCtx() {
            return HelpCtx.DEFAULT_HELP;
        }

        /** Create the object.
        * @exception IOException if something fails
        */
        public void create () throws IOException {
            NotifyDescriptor.InputLine input = new NotifyDescriptor.InputLine (
                                                   DataObject.getString ("CTL_NewFolderName"), DataObject.getString ("CTL_NewFolderTitle")
                                               );
            input.setInputText (DataObject.getString ("CTL_NewFolderValue"));
            if (DialogDisplayer.getDefault ().notify (input) == NotifyDescriptor.OK_OPTION) {
                String folderName = input.getInputText ();
                if ("".equals (folderName)) return; // empty name = cancel // NOI18N

                FileObject folder = getPrimaryFile ();
                int dotPos = -1;

                while ((dotPos = folderName.indexOf (".")) != -1) { // NOI18N
                    String subFolder = folderName.substring (0, dotPos);
                    folderName = folderName.substring (dotPos + 1);


                    FileObject existingFile = folder.getFileObject (subFolder);
                    if (existingFile != null) {
                        if (!existingFile.isFolder ()) {
                            DialogDisplayer.getDefault ().notify (
                                new NotifyDescriptor.Message (
                                    NbBundle.getMessage (DataObject.class,
					    "MSG_FMT_FileExists",
					     subFolder, folder.getName ()),
                                    NotifyDescriptor.WARNING_MESSAGE
                                )
                            );
                            return;
                        }
                        folder = existingFile;
                    } else {
                        if (! confirmName (subFolder)) {
                            throw new IOException(
				NbBundle.getMessage(DataObject.class,
					"EXC_WrongName", subFolder)
                            );
                        }
                        folder = folder.createFolder (subFolder);
                    }
                }
                if (!"".equals (folderName)) { // NOI18N
                    FileObject existingFile = folder.getFileObject (folderName);
                    if (existingFile != null) {
                        if (existingFile.isFolder ()) {
                            DialogDisplayer.getDefault ().notify (
                                new NotifyDescriptor.Message (
                                    NbBundle.getMessage (DataObject.class,
					    "MSG_FMT_FolderExists",
					    folderName, folder.getName ()),
                                    NotifyDescriptor.INFORMATION_MESSAGE
                                )
                            );
                        } else {
                            DialogDisplayer.getDefault ().notify (
                                new NotifyDescriptor.Message (
                                    NbBundle.getMessage (DataObject.class,
					    "MSG_FMT_FileExists",
					    folderName, folder.getName ()),
                                    NotifyDescriptor.WARNING_MESSAGE
                                )
                            );
                        }
                        return;
                    }

                    if (! confirmName (folderName)) {
                        throw new IOException(
                            NbBundle.getMessage(DataObject.class,
				    "EXC_WrongName", folderName )
                        );
                    }

                    DataObject created = DataObject.find(folder.createFolder (folderName));
                    if (created != null) {
                        DataLoaderPool.getDefault().fireOperationEvent(
                            new OperationEvent.Copy (created, DataFolder.this), OperationEvent.TEMPL
                        );
                    }
                }
            }
        }
    }

    private class Paste extends DataTransferSupport {
        Paste() {}
        
        /** Defines array of classes implementing paste for specified clipboard operation.
        * @param op clopboard operation to specify paste types for
        * @return array of classes extending PasteTypeExt class
        */
        protected DataTransferSupport.PasteTypeExt[] definePasteTypes (int op) {
            switch (op) {
                case LoaderTransfer.CLIPBOARD_CUT:
                return new DataTransferSupport.PasteTypeExt [] {
                    new DataTransferSupport.PasteTypeExt() {
                        public String getName () {
                            return DataObject.getString ("PT_move"); // NOI18N
                        }
                        public HelpCtx getHelpCtx () {
                            return new HelpCtx (Paste.class.getName () + ".move"); // NOI18N
                        }
                        protected boolean handleCanPaste (DataObject obj) {
                            return obj.isMoveAllowed () && !isParent (getPrimaryFile (), obj.getPrimaryFile ());
                        }
                        protected void handlePaste (DataObject obj) throws IOException {
                            obj.move (DataFolder.this);
                        }

                        /** Cleans clipboard after paste. Overrides superclass method. */
                        protected boolean cleanClipboard() {
                            return true;
                        }
                        
                        /** Check if one file object has another as a parent.
                         * @param fo the file object to check
                         * @param parent 
                         * @return true if parent is fo's (indirect) parent 
                         */
                        /*not private called from FolderNode*/ 
                        private boolean isParent (FileObject fo, FileObject parent) {
                            File parentFile = FileUtil.toFile(parent);
                            File foFile = FileUtil.toFile(fo);
                            
                            if (foFile != null && parentFile != null) {
                                return isParentFile(foFile, parentFile);
                            }
                            
                            try {
                                if (fo.getFileSystem () != parent.getFileSystem ()) {
                                    return false;
                                }
                            } catch (IOException ex) {
                            }

                            while (fo != null) {
                                if (fo.equals (parent)) {
                                    return true;
                                }

                                fo = fo.getParent ();
                            }

                            return false;
                        }
                    }
                };

                case LoaderTransfer.CLIPBOARD_COPY:
                return new DataTransferSupport.PasteTypeExt[] {
                    new DataTransferSupport.PasteTypeExt() {
                        public String getName () {
                            return DataObject.getString ("PT_copy"); // NOI18N
                        }
                        public HelpCtx getHelpCtx () {
                            return new HelpCtx (Paste.class.getName () + ".copy"); // NOI18N
                        }
                        protected boolean handleCanPaste (DataObject obj) {
                            return obj.isCopyAllowed ();
                        }
                        protected void handlePaste (DataObject obj) throws IOException {
                            saveIfModified(obj);
                            obj.copy (DataFolder.this);
                        }

                        private void saveIfModified(DataObject obj) throws IOException {
                            if (obj.isModified()) {
                                SaveCookie sc = (SaveCookie)obj.getCookie(SaveCookie.class);
                                if (sc != null) {
                                    sc.save();
                                }
                            }
                        }                        
                    },
                    new DataTransferSupport.PasteTypeExt() {
                        public String getName () {
                            return DataObject.getString ("PT_instantiate"); // NOI18N
                        }
                        public HelpCtx getHelpCtx () {
                            return new HelpCtx (Paste.class.getName () + ".instantiate"); // NOI18N
                        }
                        protected boolean handleCanPaste (DataObject obj) {
                            return obj.isTemplate ();
                        }
                        protected void handlePaste (DataObject obj) throws IOException {
                            obj.createFromTemplate (DataFolder.this);
                        }
                    },
                    new DataTransferSupport.PasteTypeExt() {
                        public String getName () {
                            return DataObject.getString ("PT_shadow"); // NOI18N
                        }
                        public HelpCtx getHelpCtx () {
                            return new HelpCtx (Paste.class.getName () + ".shadow"); // NOI18N
                        }
                        protected boolean handleCanPaste (DataObject obj) {
                            // #42888 - disable "Create as Link" action on non-SystemFileSystem
                            try {
                                if (!DataFolder.this.getPrimaryFile().getFileSystem().equals(
                                        Repository.getDefault().getDefaultFileSystem())) {
                                    return false;
                                }
                            } catch (FileStateInvalidException ex) {
                                // something wrong. disable.
                                return false;
                            }
                            return obj.isShadowAllowed ();
                        }
                        protected void handlePaste (DataObject obj) throws IOException {
                            obj.createShadow (DataFolder.this);
                        }
                    }
                };
            }
            return new DataTransferSupport.PasteTypeExt[0];
        }

        private boolean isParentFile(File foFile, File parentFile) {
            boolean retVal = false;
            while (foFile != null) {
                if (foFile.equals (parentFile)) {
                    retVal = true;
                    break;
                }
                foFile = foFile.getParentFile ();
            }
            return retVal;
        }

        /** Defines array of data clipboard operations recognized by this paste support.
        * @return array of DataFlavors
        */
        protected int [] defineOperations () {
            return new int [] {
                LoaderTransfer.CLIPBOARD_CUT,
                LoaderTransfer.CLIPBOARD_COPY
            };
        }
        protected void handleCreatePasteTypes (Transferable t, java.util.List s) {
            // These should only accept single-node transfers, since they require dialogs.
            Node node = NodeTransfer.node (t, NodeTransfer.CLIPBOARD_COPY);

            // lastly try special cookies
            if (node != null) {
                try {
                    InstanceCookie cookie = (InstanceCookie)node.getCookie (InstanceCookie.class);
                    if (cookie != null && java.io.Serializable.class.isAssignableFrom (cookie.instanceClass ())) {
                        s.add (new DataTransferSupport.SerializePaste (DataFolder.this, cookie));
                        s.add (new DataTransferSupport.InstantiatePaste (DataFolder.this, cookie));
                    }
                } catch (IOException e) {
                } catch (ClassNotFoundException e) {
                }
            }
        }
    }
    
    /** Listener on changes in FolderList that delegates to our PCL.
     */
    private final class ListPCL extends Object implements PropertyChangeListener {
        ListPCL() {}
        public void propertyChange(java.beans.PropertyChangeEvent ev) {
            if (this == DataFolder.this.pcl) {
                // if I am still folder's correct listener
                DataFolder.this.firePropertyChange (PROP_CHILDREN, null, null);
            } 
        }
        
    }
}
