/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package org.netbeans.modules.web.beans.impl.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.netbeans.modules.web.beans.CdiUtil;
import org.netbeans.modules.web.beans.api.model.BeanArchiveType;
import org.netbeans.modules.web.beans.api.model.BeansModel;
import org.netbeans.modules.web.beans.api.model.ModelUnit;
import org.netbeans.modules.web.beans.xml.Alternatives;
import org.netbeans.modules.web.beans.xml.BeanClass;
import org.netbeans.modules.web.beans.xml.BeanClassContainer;
import org.netbeans.modules.web.beans.xml.Beans;
import org.netbeans.modules.web.beans.xml.BeansAttributes;
import org.netbeans.modules.web.beans.xml.Decorators;
import org.netbeans.modules.web.beans.xml.Interceptors;
import org.netbeans.modules.web.beans.xml.Stereotype;
import org.netbeans.modules.web.beans.xml.WebBeansModel;
import org.netbeans.modules.web.beans.xml.WebBeansModelFactory;
import org.netbeans.modules.xml.retriever.catalog.Utilities;
import org.netbeans.modules.xml.xam.ModelSource;
import org.netbeans.modules.xml.xam.locator.CatalogModelException;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;


/**
 * @author ads
 *
 */
public class BeansModelImpl implements BeansModel {
    
    private static final String META_INF = "META-INF";    //NOI18N
    
    private static final String BEANS_XML   ="beans.xml";  //NOI18N
    
    private static final String WEB_INF = "WEB-INF";       //NOI18N
    
    private BeanArchiveType beanArchType = null;
    
    public BeansModelImpl( ModelUnit unit ){
        myUnit = unit;
        myLock = new Object();
        registerChangeListeners();
        initModels();
    }

    /* (non-Javadoc)
     * @see org.netbeans.modules.web.beans.api.model.BeansModel#getAlternativeClasses()
     */
    public Set<String> getAlternativeClasses() {
        Set<String> result = new HashSet<String>();
        for( WebBeansModel model : getModels() ){
            Beans beans = model.getBeans();
            if ( beans == null ){
                // it could happen if model is not well formed xml ( or f.e. empty XML file )
                continue;
            }
            List<Alternatives> alternatives = beans.getChildren(Alternatives.class);
            for (Alternatives alternative : alternatives) {
                List<BeanClass> children = alternative.getChildren(BeanClass.class);
                for (BeanClass beanClass : children) {
                    result.add( beanClass.getBeanClass());
                }
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.netbeans.modules.web.beans.api.model.BeansModel#getAlternativeStereotypes()
     */
    public Set<String> getAlternativeStereotypes() {
        Set<String> result = new HashSet<String>();
        for( WebBeansModel model : getModels() ){
            Beans beans = model.getBeans();
            if ( beans == null ){
                // it could happen if model is not well formed xml ( or f.e. empty XML file )
                continue;
            }
            List<Alternatives> alternatives = beans.getChildren(Alternatives.class);
            for (Alternatives alternative : alternatives) {
                List<Stereotype> children = alternative.getChildren(Stereotype.class);
                for (Stereotype stereotype : children) {
                    result.add( stereotype.getStereotype());
                }
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.netbeans.modules.web.beans.api.model.BeansModel#getDecoratorClasses()
     */
    public LinkedHashSet<String> getDecoratorClasses() {
        return getBeanClasses( Decorators.class ); 
    }

    /* (non-Javadoc)
     * @see org.netbeans.modules.web.beans.api.model.BeansModel#getIntercetorClasses()
     */
    public LinkedHashSet<String> getInterceptorClasses() {
        return getBeanClasses( Interceptors.class );
    }
    
    public LinkedHashSet<String> getBeanClasses( Class<? extends BeanClassContainer> clazz) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (WebBeansModel model : getModels()) {
            Beans beans = model.getBeans();
            if ( beans == null ){
                // it could happen if model is not well formed xml ( or f.e. empty XML file )
                continue;
            }
            List<? extends BeanClassContainer> children = beans.getChildren(clazz);
            for (BeanClassContainer container : children) {
                List<BeanClass> beansClasses = container.getBeansClasses();
                for (BeanClass beanClass : beansClasses) {
                    result.add( beanClass.getBeanClass());
                }
            }
        }
        return result;
    }

    @Override
    public BeanArchiveType getBeanArchiveType() {
        if(beanArchType == null) {
            Project project = getUnit().getProject();
            if(project != null) {
                //
                CdiUtil lookup = project.getLookup().lookup( CdiUtil.class );
                //
                if( lookup == null ) {
                    if (!CdiUtil.isCdiEnabled(project)) {
                        // no CDI
                        beanArchType = BeanArchiveType.NONE;
                    } else if (!CdiUtil.isCdi11OrLater(project)) {
                        // CDI 1.0 behaves like explicit bean archive
                        beanArchType = BeanArchiveType.EXPLICIT;
                    } else {
                        beanArchType = getBeansArchiveType();
                    } 
                } else {
                    if (!lookup.isCdiEnabled()) {
                        // no CDI
                        beanArchType = BeanArchiveType.NONE;
                    } else if (!lookup.isCdi11OrLater()) {
                        // CDI 1.0 behaves like explicit bean archive
                        beanArchType = BeanArchiveType.EXPLICIT;
                    } else {
                        beanArchType = getBeansArchiveType();
                    } 
                }
            } else {
                //there is no perfect solution. may happens in tests and may be in stand alone file opening, default as in cdi1.0
                beanArchType = BeanArchiveType.EXPLICIT;
            }
        }
        return beanArchType;
    }
    
    private void registerChangeListeners() {
        
        ClassPath compile = getUnit().getCompilePath();
        compile.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange( PropertyChangeEvent arg0 ) {
                /*
                 * Synchronization is needed only at initModels() call.
                 */
                synchronized (myLock) {
                    if ( myModels == null ){
                        return;
                    }
                    FileObject[] roots = getUnit().getCompilePath()
                            .getRoots();
                    Set<FileObject> rootsSet = new HashSet<FileObject>(Arrays
                            .asList(roots));
                    Set<FileObject> oldRoots = new HashSet<FileObject>(
                            myCompileRootToModel.keySet());
                    Set<FileObject> intersection = new HashSet<FileObject>(
                            rootsSet);
                    intersection.retainAll(oldRoots);
                    oldRoots.removeAll(rootsSet);
                    for (FileObject fileObject : oldRoots) {
                        List<WebBeansModel> remove = myCompileRootToModel.
                            remove(fileObject);
                        myModels.removeAll(remove);
                    }
                    rootsSet.removeAll(intersection);
                    for (FileObject fileObject : rootsSet) {
                        addCompileModels( fileObject , myModels);
                    }
                }
            }        
        });
        
        myListener = new FileChangeListener(){

            public void fileAttributeChanged( FileAttributeEvent arg0 ) {
            }

            public void fileChanged( FileEvent event ) {
                //TODO, drop beanArchType here? if it's beans.xml
                if ( !checkBeansFile(event.getFile())){//heavy operation?
                    return;
                }
                beanArchType = null;
            }

            public void fileDataCreated( FileEvent event ) {
                FileObject file = event.getFile();
                if ( !checkBeansFile(file)){
                    return;
                }
                ModelSource source=  getModelSource(file,  true );
                beanArchType = null;
                if (  source!= null ){
                    WebBeansModel model = WebBeansModelFactory.getInstance().
                        getModel( source );
                    synchronized( myLock ){
                        if ( myModels == null ){
                            return;
                        }
                        myModels.add( model );
                    }
                }
            }

            public void fileDeleted( FileEvent event ) {
                FileObject file = event.getFile();
                if ( !wasBeansFile(file)){
                    return;
                }
                WebBeansModel model = null;
                beanArchType = null;
                synchronized (myLock) {
                    if ( myModels == null){
                        return;
                    }
                    for (WebBeansModel mod : myModels) {
                        FileObject fileObject = mod.getModelSource()
                                .getLookup().lookup(FileObject.class);
                        if (fileObject.equals(event.getFile())) {
                            model = mod;
                            break;
                        }
                    }
                    if (model != null) {
                        myModels.remove(model);
                    }

                }
            }

            public void fileFolderCreated( FileEvent arg0 ) {
            }

            public void fileRenamed( FileRenameEvent arg0 ) {
            }
            
            private boolean checkBeansFile( FileObject fileObject ){
                if ( fileObject == null){
                    return false;
                }
                FileObject[] roots = getUnit().getSourcePath().getRoots();
                for (FileObject root : roots) {
                    FileObject meta = root.getFileObject(META_INF);
                    if ( meta != null && fileObject.equals( meta.getFileObject(BEANS_XML) )){
                        return true;
                    }
                    FileObject webInf = root.getFileObject( WEB_INF );
                    if ( webInf!=null && fileObject.equals( webInf.getFileObject(BEANS_XML))){
                        return true;
                    }
                }
                return false;
            }
            
            private boolean wasBeansFile( FileObject fileObject ){
                if ( fileObject == null){
                    return false;
                }
                String name = fileObject.getNameExt();
                if ( name.equals( BEANS_XML ))
                {
                    FileObject parent = fileObject.getParent();
                    if ( !parent.getName().equals( META_INF ) && 
                            !parent.getName().equals( WEB_INF ))
                    {
                        return false;
                    }
                    for ( FileObject root : getUnit().getSourcePath().getRoots()){
                        if ( parent.equals(root.getFileObject(META_INF))
                                ||parent.equals(root.getFileObject(WEB_INF)))
                        {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
        
        FileUtil.addFileChangeListener( myListener );
    }
    
    private void initModels() {
        /*
         *  synchronization is needed only at time of "initModels" work.
         *  It prevent simultaneous work initModels and registered listeners .
         *  All subsequent access to myModels could be done without synchronization
         *  because of chosen class for myModels ( it is CopyOnWrite ).
         */
        synchronized ( myLock ) {
            List<WebBeansModel> list = new LinkedList<WebBeansModel>();
            FileObject[] roots = getUnit().getSourcePath().getRoots();
            for (FileObject fileObject : roots) {
                addModels(fileObject,list);
            }
            FileObject[] compileRoots = getUnit().getCompilePath().getRoots();
            for (FileObject root : compileRoots) {
                addCompileModels( root ,list);
            }
            myModels = new CopyOnWriteArrayList<WebBeansModel>( list );
        }
    }

    private void addCompileModels( FileObject root , List<WebBeansModel> list ) {
        FileObject beans = getBeansFile(root);
        if (beans != null){
            addCompileModel(beans, root, list);
        }
    }

    /**
     * is based on cdi 1.1, do not use for 1.0
     * @return 
     */
    private synchronized BeanArchiveType getBeansArchiveType() {
        for (FileObject fileObject : getUnit().getSourcePath().getRoots()) {
            FileObject beans = getBeansFile(fileObject);
            if (beans != null) {
                beanArchiveType = detectArchiveType(beans);
                return beanArchiveType;
            }
        }
        for (FileObject fileObject : getUnit().getCompilePath().getRoots()) {
            FileObject beans = getBeansFile(fileObject);
            if (beans != null) {
                beanArchiveType = detectArchiveType(beans);
                return beanArchiveType;
            }
        }
        beanArchiveType = BeanArchiveType.IMPLICIT;//no beans.xmk is found, in 1.1 it means implicit/annotated
        return beanArchiveType;
    }

    private BeanArchiveType detectArchiveType(FileObject beans) {
        WebBeansModel model = WebBeansModelFactory.getInstance().getModel(getModelSource(beans, true));
        if (model == null) {
            return BeanArchiveType.IMPLICIT;
        }

        String attribute = model.getRootComponent().getAttribute(BeansAttributes.BEAN_DISCOVERY_MODE);
        if(attribute == null) {
            attribute = "all";//NOI18N, got this for cdi 1.0, but there should be a better place for check, CdiUtil.isCdi11OrLater isn't good for ee7 server wih 1.0 beans.xml
        }
        switch(attribute) {
            case "none":        //NOI18N
                return BeanArchiveType.NONE;
            case "all":         //NOI18N
                return BeanArchiveType.EXPLICIT;
            case "annotated":   //NOI18N
            default:
                return BeanArchiveType.IMPLICIT;
        }
    }

    private FileObject getBeansFile(FileObject root) {
        FileObject beans = null;
        FileObject meta = root.getFileObject(META_INF);
        if(meta != null) beans = meta.getFileObject(BEANS_XML);
        if (beans != null) {
            return beans;
        }
        FileObject web = root.getFileObject(WEB_INF);
        return web != null ? web.getFileObject(BEANS_XML) : null;
    }
    
    void addCompileModel(FileObject fileObject, FileObject compileRoot, 
            List<WebBeansModel> modelList )
    {
        WebBeansModel model = WebBeansModelFactory.getInstance().getModel(
                getModelSource(fileObject, false));
        if ( model != null ){
            modelList.add( model );
            List<WebBeansModel> list = myCompileRootToModel.get(compileRoot );
            if ( list == null ){
                list = new ArrayList<>(2);
                myCompileRootToModel.put( compileRoot , list );
            }
            list.add( model );
        }
    }

    private void addModels( FileObject root , List<WebBeansModel> list ) {
        FileObject beans = getBeansFile(root);
        if (beans != null){
            addModel(beans, list );
        }
    }

    void addModel( FileObject beans , List<WebBeansModel> list ) {
        WebBeansModel model = WebBeansModelFactory.getInstance().getModel(
                getModelSource(beans, true));
        if ( model != null ){
            list.add( model );
        }
    }
    
    private ModelSource getModelSource( FileObject fileObject , 
            boolean isEditable )
    {
        try {
            return Utilities.createModelSource( fileObject,isEditable);
        } catch (CatalogModelException ex) {
            Logger.getLogger("global").log(java.util.logging.Level.SEVERE,
                ex.getMessage(), ex);   // NOI18N
        }
        return null;
    }
    
    private List<WebBeansModel> getModels(){
        return myModels;
    }
    
    private ModelUnit getUnit(){
        return myUnit;
    }

    private ModelUnit myUnit;
    private Object myLock;
    private List<WebBeansModel> myModels;
    private Map<FileObject, List<WebBeansModel>> myCompileRootToModel = new HashMap<>();
    private FileChangeListener myListener;
    private BeanArchiveType beanArchiveType;
}
