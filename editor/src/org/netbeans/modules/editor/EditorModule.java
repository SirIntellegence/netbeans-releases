/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
    

package org.netbeans.modules.editor;


import java.lang.reflect.Field;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.event.ChangeListener;
import javax.swing.JEditorPane;
import javax.swing.text.EditorKit;

import org.netbeans.editor.AnnotationType;
import org.netbeans.editor.BaseKit;
import org.netbeans.editor.DialogSupport;
import org.netbeans.editor.ext.java.JavaSettingsNames;
import org.netbeans.editor.LocaleSupport;
import org.netbeans.modules.editor.options.AllOptions;
import org.netbeans.modules.editor.options.AnnotationTypesFolder;
import org.netbeans.modules.editor.options.JavaPrintOptions;
import org.netbeans.modules.editor.options.HTMLPrintOptions;
import org.netbeans.modules.editor.options.PlainPrintOptions;
import org.openide.TopManager;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.InstanceCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.RepositoryListener;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.modules.ModuleInstall;
import org.openide.nodes.Node;
import org.openide.options.SystemOption;
import org.openide.text.PrintSettings;
import org.openide.util.RequestProcessor;
import org.openide.util.SharedClassObject;
import org.openide.util.WeakListener;
import org.openide.windows.TopComponent;
import org.netbeans.editor.AnnotationTypes;
import org.netbeans.modules.editor.options.BaseOptions;
import org.netbeans.editor.ImplementationProvider;
import org.netbeans.modules.editor.NbImplementationProvider;
import java.util.Iterator;
import org.openide.text.CloneableEditor;
import java.util.HashSet;
import org.netbeans.editor.ext.ExtSettingsNames;
import org.netbeans.modules.editor.java.AutoUpdateQuestionPanel;
import org.netbeans.modules.editor.java.JCStorage;
import org.netbeans.modules.editor.java.JCStorageElement;
import org.netbeans.modules.editor.options.BasePrintOptions;
import org.openide.loaders.OperationEvent;
import org.openide.loaders.OperationListener;
import org.openide.loaders.OperationAdapter;
import org.openide.src.SourceElement;
import org.openide.cookies.SourceCookie;
import org.netbeans.modules.editor.java.JCUpdater;
import org.netbeans.modules.editor.java.JavaKit;
import org.netbeans.modules.editor.java.PDCustomizer;
import org.netbeans.modules.editor.options.JavaOptions;
import org.openide.DialogDescriptor;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.RepositoryEvent;
import org.openide.filesystems.RepositoryReorderedEvent;
import org.openide.loaders.DataFolder;
import org.openide.util.NbBundle;


/**
 * Module installation class for editor.
 *
 * @author Miloslav Metelka
 */

public class EditorModule extends ModuleInstall {

    private static final boolean debug = Boolean.getBoolean("netbeans.debug.editor.kits");
    private RepositListener repoListen;


    /** PrintOptions to be installed */
    Class[] printOpts = new Class[] {
        PlainPrintOptions.class,
        JavaPrintOptions.class,
        HTMLPrintOptions.class
    };

    /** Listener on <code>DataObject.Registry</code>. */
    private DORegistryListener rl;
    
    /** Module installed again. */
    public void restored () {


        LocaleSupport.addLocalizer(new NbLocalizer(AllOptions.class));
        LocaleSupport.addLocalizer(new NbLocalizer(BaseKit.class));
        LocaleSupport.addLocalizer(new NbLocalizer(JavaSettingsNames.class));

        // Initializations
        DialogSupport.setDialogFactory( new NbDialogSupport() );
        
        ImplementationProvider.registerDefault(new NbImplementationProvider());
        
        // register loader for annotation types
        AnnotationTypes.getTypes().registerLoader( new AnnotationTypes.Loader() {
                public void loadTypes() {
                    AnnotationTypesFolder.getAnnotationTypesFolder();
                }
                public void loadSettings() {
                    // AnnotationType properties are stored in BaseOption, so let's read them now
                    BaseOptions bo = (BaseOptions)BaseOptions.findObject(BaseOptions.class, true);

                    Integer i = (Integer)bo.getSettingValue(AnnotationTypes.PROP_BACKGROUND_GLYPH_ALPHA);
                    if (i != null)
                        AnnotationTypes.getTypes().setBackgroundGlyphAlpha(i.intValue());
                    Boolean b = (Boolean)bo.getSettingValue(AnnotationTypes.PROP_BACKGROUND_DRAWING);
                    if (b != null)
                        AnnotationTypes.getTypes().setBackgroundDrawing(b);
                    b = (Boolean)bo.getSettingValue(AnnotationTypes.PROP_COMBINE_GLYPHS);
                    if (b != null)
                        AnnotationTypes.getTypes().setCombineGlyphs(b);
                    b = (Boolean)bo.getSettingValue(AnnotationTypes.PROP_GLYPHS_OVER_LINE_NUMBERS);
                    if (b != null)
                        AnnotationTypes.getTypes().setGlyphsOverLineNumbers(b);
                    b = (Boolean)bo.getSettingValue(AnnotationTypes.PROP_SHOW_GLYPH_GUTTER);
                    if (b != null)
                        AnnotationTypes.getTypes().setShowGlyphGutter(b);
                }
                public void saveType(AnnotationType type) {
                    AnnotationTypesFolder.getAnnotationTypesFolder().saveAnnotationType(type);
                }
                public void saveSetting(String settingName, Object value) {
                    // AnnotationType properties are stored to BaseOption
                    BaseOptions bo = (BaseOptions)BaseOptions.findObject(BaseOptions.class, true);
                    bo.setSettingValue(settingName, value);
                }
            } );

        // Settings
        NbEditorSettingsInitializer.init();

	// defer the rest of initialization, but enable a bit of paralelism
//        org.openide.util.RequestProcessor.postRequest (this, 0, Thread.MIN_PRIORITY);

        // Options
        PrintSettings ps = (PrintSettings) SharedClassObject.findObject(PrintSettings.class, true);

        // Start listening on addition/removal of print options
        BasePrintOptions bpo = (BasePrintOptions) BasePrintOptions.findObject(BasePrintOptions.class, true);
        bpo.init();
        
        for (int i = 0; i < printOpts.length; i++) {
            ps.addOption((SystemOption)SharedClassObject.findObject(printOpts[i], true));
        }

        // Autoregistration
        try {
            Field keyField = JEditorPane.class.getDeclaredField("kitRegistryKey");  // NOI18N
            keyField.setAccessible(true);
            Object key = keyField.get(JEditorPane.class);
            Hashtable kitMapping = (Hashtable)sun.awt.AppContext.getAppContext().get(key);
            sun.awt.AppContext.getAppContext().put(key, new HackMap(kitMapping));
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Registration of the editor kits to JEditorPane
//        for (int i = 0; i < replacements.length; i++) {
//            JEditorPane.registerEditorKitForContentType(
//                replacements[i].contentType,
//                replacements[i].newKitClassName,
//                getClass().getClassLoader()
//            );
//        }

        TopManager.getDefault().getLoaderPool().addOperationListener(new RepositOperations());
        
        
        // Start listening on DataObject.Registry
        if (rl == null) {
            rl = new DORegistryListener();
            DataObject.getRegistry().addChangeListener((ChangeListener)(WeakListener.change(rl, DataObject.getRegistry())));
        }
        
        
        if (repoListen==null){
            repoListen=new RepositListener();
            Repository repo = TopManager.getDefault().getRepository();
            if (repo!=null){
                repo.addRepositoryListener((RepositoryListener)(WeakListener.repository(repoListen, repo)));
            }
        }
        

    }

    /** Called when module is uninstalled. Overrides superclass method. */
    public void uninstalled() {

        // Options
        PrintSettings ps = (PrintSettings) SharedClassObject.findObject(PrintSettings.class, true);

        for (int i = 0; i < printOpts.length; i++) {
            ps.removeOption((SystemOption)SharedClassObject.findObject(printOpts[i], true));
        }

        Node node = TopManager.getDefault().getPlaces().nodes().session();
        Node[] ch = node.getChildren().getNodes();
        Node[] uninstall =new Node[1];
        for (int i=0; i<ch.length; i++){
            if (ch[i].getClass().equals(org.netbeans.modules.editor.options.AllOptionsNode.class)){
                uninstall[0]=ch[i];
            }
        }
        if (uninstall[0]!=null)
            node.getChildren().remove(uninstall);

        // unregister our registry
        try {
            Field keyField = JEditorPane.class.getDeclaredField("kitRegistryKey");  // NOI18N
            keyField.setAccessible(true);
            Object key = keyField.get(JEditorPane.class);
            HackMap kitMapping = (HackMap)sun.awt.AppContext.getAppContext().get(key);
            if (kitMapping.getOriginal() != null) {
                sun.awt.AppContext.getAppContext().put(key, kitMapping.getOriginal());
            } else {
                sun.awt.AppContext.getAppContext().remove(key);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        // issue #16110
        // close all TopComponents which contain editor based on BaseKit
        HashSet set = new HashSet();
        set.addAll(TopComponent.getRegistry().getOpened());

        for (Iterator it = set.iterator(); it.hasNext(); ) {
            TopComponent topComp = (TopComponent)it.next();
            // top components in which we are interested must be of type CloneableEditor
            if (!(topComp instanceof CloneableEditor))
                continue;
            Node[] arr = topComp.getActivatedNodes();
            if (arr == null)
                continue;
            for (int i=0; i<arr.length; i++) {
                EditorCookie ec = (EditorCookie)arr[i].getCookie(EditorCookie.class);
                if (ec == null)
                    continue;
                JEditorPane[] pane = ec.getOpenedPanes();
                if (pane == null) 
                    continue;
                for (int j=0; j<pane.length; j++) {
                    if (pane[j].getEditorKit() instanceof BaseKit) {
                        topComp.setCloseOperation(TopComponent.CLOSE_EACH);
                        topComp.close();
                    }
                }
            }
        }
        
    }

    private static class HackMap extends Hashtable {
        
	private Hashtable delegate;

        HackMap(Hashtable h) {
            delegate = h;
            
            if (debug) {
                if (h != null) {
                    System.err.println("Original kit mappings: " + h);
                }

                try {
                    Field keyField = JEditorPane.class.getDeclaredField("kitTypeRegistryKey");  // NOI18N
                    keyField.setAccessible(true);
                    Object key = keyField.get(JEditorPane.class);
                    Hashtable kitTypeMapping = (Hashtable)sun.awt.AppContext.getAppContext().get(key);
                    sun.awt.AppContext.getAppContext().put(key, new DebugHashtable(kitTypeMapping));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        private Object findKit(String type) {
            FileObject fo = TopManager.getDefault().getRepository().getDefaultFileSystem().findResource("Editors/" + type + "/EditorKit.instance");
            if (fo == null) return null;

            DataObject dobj;
            try {
                dobj = DataObject.find(fo);
                InstanceCookie cookie = (InstanceCookie)dobj.getCookie(InstanceCookie.class);
                Object instance = cookie.instanceCreate();
                if(instance instanceof EditorKit) {
                    return (EditorKit)instance;
                }
            }
            catch (DataObjectNotFoundException e) {}
            catch (IOException e) {}
            catch (ClassNotFoundException e) {}

            return null;
        }
        
        private String getKitClassName(String type) {
            try {
                Field keyField = JEditorPane.class.getDeclaredField("kitTypeRegistryKey");  // NOI18N
                keyField.setAccessible(true);
                Object key = keyField.get(JEditorPane.class);
                Hashtable kitTypeMapping = (Hashtable)sun.awt.AppContext.getAppContext().get(key);
                if (kitTypeMapping != null) {
                    return (String)kitTypeMapping.get(type);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            
            return null;
        }
            
        
        public synchronized Object get(Object key) {
            Object retVal = null;
            
            if (delegate != null) {
                retVal = delegate.get(key);
                if (debug && retVal != null) {
                    System.err.println("Found cached instance kit=" + retVal + " for mimeType=" + key);
                }
            }

	    if ((retVal == null || retVal.getClass().getName().startsWith("javax.swing."))
                && key instanceof String
            ) {
                // first check the type registry
                String kitClassName = getKitClassName((String)key);
                if (debug) {
                    System.err.println("Found kitClassName=" + kitClassName + " for mimeType=" + key);
                }
                
                if (kitClassName == null || kitClassName.startsWith("javax.swing.")) { // prefer layers
                    Object kit = findKit((String)key);
                    if (kit != null) {
                        retVal = kit;
                        if (debug) {
                            System.err.println("Found kit=" + retVal + " in xml layers for mimeType=" + key);
                        }
                    }
                }
	    }

            return retVal;
        }
        
        public synchronized Object put(Object key, Object value) {
            if (delegate == null) {
                delegate = new Hashtable();
            }

            Object ret = delegate.put(key,value);
            
            if (debug) {
                System.err.println("registering mimeType=" + key
                    + " -> kitInstance=" + value
                    + " original was " + ret);
            }
             
            return ret;
        }

        public synchronized Object remove(Object key) {
            Object ret = (delegate != null) ? delegate.remove(key) : null;
            
            if (debug) {
                System.err.println("removing kitInstance=" + ret
                    + " for mimeType=" + key);
            }
            
            return ret;
        }
        
        Hashtable getOriginal() {
            return delegate;
        }

    }
    
    private static final class DebugHashtable extends Hashtable {
        
        DebugHashtable(Hashtable h) {
            if (h != null) {
                putAll(h);
                System.err.println("Existing kit classNames mappings: " + this);
            }
        }
        
        public Object put(Object key, Object value) {
            Object ret = super.put(key, value);
            System.err.println("registering mimeType=" + key
                + " -> kitClassName=" + value
                + " original was " + ret);
            return ret;
        }
        
        public Object remove(Object key) {
            Object ret = super.remove(key);
            System.err.println("removing kitClassName=" + ret
                + " for mimeType=" + key);
            return ret;
        }
        
    }

    /** Listens to repository operations like move, delete and rename of the 
        classes or packages and updates Code Completion DB */
    private static final class RepositOperations extends OperationAdapter {

        public void operationMove(OperationEvent.Move ev){
            DataObject dobj = ev.getObject();
            if (dobj==null){
                return;
            }
            SourceCookie sc = (SourceCookie)dobj.getCookie(SourceCookie.class);
            if (sc == null){
                return;
            }
            removeClass(ev.getOriginalPrimaryFile(),null);
        }
        
        public void operationDelete(OperationEvent ev){
            DataObject dobj = ev.getObject();
            if (dobj==null){
                return;
            }
            SourceCookie sc = (SourceCookie)dobj.getCookie(SourceCookie.class);
            if (sc == null){
                return;
            }
            removeClass(dobj.getPrimaryFile(),null);
        }
        
        
        public void operationRename(OperationEvent.Rename ev){
            DataObject dobj = ev.getObject();
            if (dobj==null) return;
            
            DataFolder df = (DataFolder)dobj.getCookie(DataFolder.class);
            String replacedName = replaceName(dobj.getPrimaryFile().getPackageName('.'), ev.getOriginalName());
            
            if(df!=null){
                inspectFolder(df, replacedName);
                return;
            }
            
            SourceCookie sc = (SourceCookie)dobj.getCookie(SourceCookie.class);
            if (sc == null) return;
            
            removeClass(dobj.getPrimaryFile(),replacedName);
        }

        
        private String replaceName(String newName, String oldName){
            StringBuffer sb = new StringBuffer(newName);
            sb.replace(newName.lastIndexOf(".")+1,newName.length(),oldName); //NOI18N
            return sb.toString();
        }
        
        private void inspectFolder(DataFolder df, String oldFolderName) {
            DataObject[] children = df.getChildren();
            for (int i = 0; i < children.length; i++) {
                DataObject dob = children[i];
                if (dob instanceof DataFolder) {
                    inspectFolder((DataFolder)dob, (oldFolderName+"."+dob.getPrimaryFile().getName())); //NOI18N
                } else if(dob!=null){
                    SourceCookie sc = (SourceCookie)dob.getCookie(SourceCookie.class);
                    if (sc == null) continue;
                    removeClass(dob.getPrimaryFile(), oldFolderName+"."+dob.getPrimaryFile().getName()); //NOI18N
                }
            }
        }
                
        private void removeClass(final FileObject fob, final String oldName){
            final JCUpdater update = new JCUpdater();
            // Update changes in Code Completion DB on background in thread with minPriority
            RequestProcessor.postRequest(new Runnable() {
                public void run() {
                    update.removeClass(fob,oldName);
                }
            }, 0,Thread.MIN_PRIORITY);        
        }
    }
    
    class RepositListener implements org.openide.filesystems.RepositoryListener {
        
        /** Creates new RepositListener */
        public RepositListener() {
        }
        
        public void fileSystemAdded(RepositoryEvent ev){
            if (Boolean.getBoolean("netbeans.full.hack") == true){ //NOI18N
                return; 
            }
            org.openide.windows.WindowManager wm=TopManager.getDefault().getWindowManager();
            if(wm!=null){
                java.awt.Frame frm = wm.getMainWindow();
                if(frm!=null){
                    if(frm.isVisible()){
                        if (ev.getFileSystem() == null) return;
                        FileObject fo = ev.getFileSystem().getRoot();
                        // ignoring JavaDoc (if FS is hidden), ignoring FS already parsed
                        if ((fo!=null) && (!ev.getFileSystem().isHidden()) &&
                        (JCStorage.getStorage().findFileSystemElement(ev.getFileSystem().getSystemName())==null)){
                            
                            JavaOptions jo;
                            
                            if (BaseOptions.getOptions(JavaKit.class) instanceof JavaOptions){
                                // load options
                                jo = (JavaOptions)BaseOptions.getOptions(JavaKit.class);
                            }else{
                                return; //NO JavaKit ...
                            }
                            
                            String updation = jo.getUpdatePDAfterMounting();

                            
                            if (ExtSettingsNames.ASK.equals(updation)){
                                try{
                                    DataObject dataObj = DataObject.find(fo);
                                    Node[] node = new Node[1];
                                    node[0] = dataObj.getNodeDelegate();
                                    AutoUpdateQuestionPanel qPanel = new AutoUpdateQuestionPanel(
                                        MessageFormat.format(NbBundle.getBundle( AutoUpdateQuestionPanel.class ).getString("AUQP_UPDATE_QUESTION"), 
                                        new Object [] { ev.getFileSystem().getSystemName() } )
                                        );

                                    DialogDescriptor dd = new DialogDescriptor(qPanel,
                                    "Question", true, DialogDescriptor.OK_CANCEL_OPTION,
                                    DialogDescriptor.OK_OPTION, null);
                                    java.awt.Dialog d = TopManager.getDefault().createDialog(dd);
                                    d.pack();
                                    d.show();

                                    Object o = dd.getValue();
                                    if (o == DialogDescriptor.OK_OPTION) {
                                        if (qPanel.getDoNowShowNextTime()){
                                            jo.setUpdatePDAfterMounting(qPanel.getYes() ? ExtSettingsNames.ALWAYS : ExtSettingsNames.NEVER);
                                        }
                                        if (qPanel.getYes()){
                                            JCUpdater update = new JCUpdater(node, false);
                                            update.start();
                                        }
                                    }
                                }catch(DataObjectNotFoundException notFound){
                                }
                            }else if (ExtSettingsNames.NEVER.equals(updation)){
                                return;
                            }else if (ExtSettingsNames.ALWAYS.equals(updation)){
                                try{
                                    DataObject dataObj = DataObject.find(fo);
                                    Node[] node = new Node[1];
                                    node[0] = dataObj.getNodeDelegate();
                                    
                                    JCUpdater update = new JCUpdater(node, false, false);
                                    update.start();
                                }catch (DataObjectNotFoundException notFound){
                                }
                            }
                        }
                    }
                }
            }
        }
        
        public void fileSystemRemoved(RepositoryEvent ev){
            if (Boolean.getBoolean("netbeans.full.hack") == true) { //NOI18N
                return; 
            }
            FileSystem fs = ev.getFileSystem();            
            if (fs == null) return;

            FileObject fo = ev.getFileSystem().getRoot();

            if ((fo!=null) && (!ev.getFileSystem().isHidden()) &&
            (JCStorage.getStorage().findFileSystemElement(ev.getFileSystem().getSystemName())!=null)){
                JavaOptions jo;

                if (BaseOptions.getOptions(JavaKit.class) instanceof JavaOptions){
                    // load options
                    jo = (JavaOptions)BaseOptions.getOptions(JavaKit.class);
                }else{
                    return; //NO JavaKit ...
                }

                String delete = jo.getDeletePDAfterUnmounting();

                if (ExtSettingsNames.ASK.equals(delete)){
                    try{
                        DataObject dataObj = DataObject.find(fo);
                        Node[] node = new Node[1];
                        node[0] = dataObj.getNodeDelegate();
                        AutoUpdateQuestionPanel qPanel = new AutoUpdateQuestionPanel(
                            MessageFormat.format(NbBundle.getBundle( AutoUpdateQuestionPanel.class ).getString("AUQP_DELETE_QUESTION"), 
                            new Object [] { ev.getFileSystem().getSystemName() } )
                        );
                        
                        DialogDescriptor dd = new DialogDescriptor(qPanel,
                        "Question", true, DialogDescriptor.OK_CANCEL_OPTION,
                        DialogDescriptor.OK_OPTION, null);
                        java.awt.Dialog d = TopManager.getDefault().createDialog(dd);
                        d.pack();
                        d.show();
                        
                        Object o = dd.getValue();
                        if (o == DialogDescriptor.OK_OPTION) {
                            if (qPanel.getDoNowShowNextTime()){
                                jo.setDeletePDAfterUnmounting(qPanel.getYes() ? ExtSettingsNames.ALWAYS : ExtSettingsNames.NEVER);
                            }
                            if (qPanel.getYes()){
                                JCStorageElement elem = JCStorage.getStorage().findFileSystemElement(fs.getSystemName());
                                if (elem == null) return;
                                String prefixName = elem.getName();
                                PDCustomizer.getDefault().delete(prefixName, false);
                            }else{
                                PDCustomizer.getDefault().refresh();
                            }
                        }
                    }catch(DataObjectNotFoundException notFound){
                        // nothing will be done
                    }

                } else if (ExtSettingsNames.NEVER.equals(delete)){
                    return; // never delete the Code Completion DB file
                } else if (ExtSettingsNames.ALWAYS.equals(delete)){
                    // delete always w/o asking
                    JCStorageElement elem = JCStorage.getStorage().findFileSystemElement(fs.getSystemName());
                    if (elem == null) return;
                    String prefixName = elem.getName();
                    PDCustomizer.getDefault().delete(prefixName, false);
                }
                
            }

        }
        
        public void fileSystemPoolReordered(RepositoryReorderedEvent ev){
        }
    
}
    
    
}
