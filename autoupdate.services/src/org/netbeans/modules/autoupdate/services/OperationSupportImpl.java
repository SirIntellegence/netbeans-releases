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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.autoupdate.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.InvalidException;
import org.netbeans.Module;
import org.netbeans.ModuleManager;
import org.netbeans.api.autoupdate.*;
import org.netbeans.api.autoupdate.OperationContainer.OperationInfo;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.spi.autoupdate.CustomInstaller;
import org.openide.modules.ModuleInfo;

/**
 * @author Radek Matous
 */
public abstract class OperationSupportImpl {
    private static final OperationSupportImpl FOR_INSTALL = new ForInstall();
    private static final OperationSupportImpl FOR_UPDATE = new ForUpdate();
    private static final OperationSupportImpl FOR_ENABLE = new ForEnable();
    private static final OperationSupportImpl FOR_DISABLE = new ForDisable();
    private static final OperationSupportImpl FOR_UNINSTALL = new ForUninstall();
    private static final OperationSupportImpl FOR_CUSTOM_INSTALL = new ForCustomInstall ();
    
    public static OperationSupportImpl forInstall() {
        return FOR_INSTALL;
    }
    public static OperationSupportImpl forUpdate() {
        return FOR_UPDATE;
    }
    public static OperationSupportImpl forUninstall() {
        return FOR_UNINSTALL;
    }
    public static OperationSupportImpl forEnable() {
        return FOR_ENABLE;
    }
    public static OperationSupportImpl forDisable() {
        return FOR_DISABLE;
    }
    public static OperationSupportImpl forCustomInstall () {
        return FOR_CUSTOM_INSTALL;
    }
    
    public abstract void doOperation(ProgressHandle progress/*or null*/, OperationContainer<?> container) throws OperationException;
    public abstract void doCancel () throws OperationException;
    
    /** Creates a new instance of OperationContainer */
    private OperationSupportImpl() {
    }
    
    private static class ForEnable extends OperationSupportImpl {
        public void doOperation(ProgressHandle progress,
                OperationContainer<?> container) throws OperationException {
            try {
                if (progress != null) {
                    progress.start();
                }
                
                ModuleManager mm = null;
                List<? extends OperationInfo> elements = container.listAll();
                Set<ModuleInfo> moduleInfos = new HashSet<ModuleInfo> ();
                for (OperationInfo operationInfo : elements) {
                    UpdateElementImpl impl = Trampoline.API.impl (operationInfo.getUpdateElement ());
                    moduleInfos.addAll (impl.getModuleInfos ());
                }
                Set<Module> modules = new HashSet<Module>();
                for (ModuleInfo info : moduleInfos) {
                    Module m = Utilities.toModule (info);
                    if (Utilities.canEnable (m)) {
                        modules.add(m);
                    }
                    if (mm == null) {
                        mm = m.getManager();
                    }
                }
                assert mm != null;
                enable(mm, modules);
            } finally {
                if (progress != null) {
                    progress.finish();
                }
            }            
        }
        public void doCancel () throws OperationException {
            assert false : "Not supported yet";
        }
        
        private static boolean enable(ModuleManager mm, Set<Module> toRun) throws OperationException {
            boolean retval = false;
            try {
                mm.enable(toRun);
                retval = true;
            } catch(IllegalArgumentException ilae) {
                throw new OperationException(OperationException.ERROR_TYPE.ENABLE);
            } catch(InvalidException ie) {
                throw new OperationException(OperationException.ERROR_TYPE.ENABLE);
            }
            return retval;
        }
        
    }
    private static class ForDisable extends OperationSupportImpl {
        public void doOperation(ProgressHandle progress,
                OperationContainer<?> container) throws OperationException {
            try {
                if (progress != null) {
                    progress.start();
                }
                ModuleManager mm = null;
                List<? extends OperationInfo> elements = container.listAll();
                Set<ModuleInfo> moduleInfos = new HashSet<ModuleInfo> ();
                for (OperationInfo operationInfo : elements) {
                    UpdateElementImpl impl = Trampoline.API.impl (operationInfo.getUpdateElement ());
                    moduleInfos.addAll (impl.getModuleInfos ());
                }
                Set<Module> modules = new HashSet<Module>();
                for (ModuleInfo info : moduleInfos) {
                    Module m = Utilities.toModule (info);
                    if (Utilities.canDisable (m)) {
                        modules.add(m);
                    }
                    if (mm == null) {
                        mm = m.getManager();
                    }
                }
                assert mm != null;
                mm.disable(modules);
            } finally {
                if (progress != null) {
                    progress.finish();
                }
            }
        }
        public void doCancel () throws OperationException {
            assert false : "Not supported yet";
        }
        
    }
    private static class ForUninstall extends OperationSupportImpl {
        public void doOperation(ProgressHandle progress,
                OperationContainer<?> container) throws OperationException {
            try {
                if (progress != null) {
                    progress.start();
                }
                ModuleDeleterImpl deleter = new ModuleDeleterImpl();
                
                List<? extends OperationInfo> infos = container.listAll ();
                Set<ModuleInfo> moduleInfos = new HashSet<ModuleInfo> ();
                Set<UpdateUnit> affectedModules = new HashSet<UpdateUnit> ();
                Set<UpdateUnit> affectedFeatures = new HashSet<UpdateUnit> ();
                for (OperationInfo operationInfo : infos) {
                    UpdateElement updateElement = operationInfo.getUpdateElement ();
                    UpdateElementImpl updateElementImpl = Trampoline.API.impl (updateElement);
                    switch (updateElementImpl.getType ()) {
                    case MODULE :
                        moduleInfos.add (((ModuleUpdateElementImpl) updateElementImpl).getModuleInfo ());
                        affectedModules.add (updateElementImpl.getUpdateUnit ());
                        break;
                    case FEATURE :
                        for (ModuleUpdateElementImpl moduleImpl : ((FeatureUpdateElementImpl) updateElementImpl).getContainedModuleElements ()) {
                            moduleInfos.add (moduleImpl.getModuleInfo ());
                            affectedModules.add (moduleImpl.getUpdateUnit ());
                        }
                        affectedFeatures.add (updateElement.getUpdateUnit ());
                        break;
                    default:
                        assert false : "Not supported for impl " + updateElementImpl;
                    }
                }
                try {
                    deleter.delete (moduleInfos.toArray (new ModuleInfo[0]));
                } catch(IOException iex) {
                    throw new OperationException(OperationException.ERROR_TYPE.UNINSTALL);
                }
                

                for (UpdateUnit unit : affectedModules) {
                    assert unit.getInstalled () != null;
                    UpdateUnitImpl impl = Trampoline.API.impl (unit);
                    impl.setAsUninstalled();
                }
                for (UpdateUnit unit : affectedFeatures) {
                    assert unit.getInstalled () != null;
                    UpdateUnitImpl impl = Trampoline.API.impl (unit);
                    impl.setAsUninstalled();
                }
            } finally {
                if (progress != null) {
                    progress.finish();
                }
            }
        }
        public void doCancel () throws OperationException {
            assert false : "Not supported yet";
        }
        
    }
    private static class ForInstall extends OperationSupportImpl {
        public void doOperation(ProgressHandle progress,
                OperationContainer container) throws OperationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        public void doCancel () throws OperationException {
            assert false : "Not supported yet";
        }
        
    }
    private static class ForUpdate extends OperationSupportImpl {
        public void doOperation(ProgressHandle progress,
                OperationContainer container) throws OperationException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        public void doCancel () throws OperationException {
            assert false : "Not supported yet";
        }
        
    }
    
    private static class ForCustomInstall extends OperationSupportImpl {
        public void doOperation(ProgressHandle progress,
                OperationContainer<?> container) throws OperationException {
            try {
                
                // XXX: do you want to start ProgressHandle for custom install?
                if (progress != null) {
                    progress.start();
                }
                List<? extends OperationInfo> infos = container.listAll ();
                List<NativeComponentUpdateElementImpl> customElements = new ArrayList<NativeComponentUpdateElementImpl> ();
                for (OperationInfo operationInfo : infos) {
                    UpdateElementImpl impl = Trampoline.API.impl (operationInfo.getUpdateElement ());
                    assert impl instanceof NativeComponentUpdateElementImpl : "Impl of " + operationInfo.getUpdateElement () + " instanceof NativeComponentUpdateElementImpl.";
                    customElements.add ((NativeComponentUpdateElementImpl) impl);
                }
                assert customElements != null : "Some elements with custom installer found.";
                for (NativeComponentUpdateElementImpl impl : customElements) {
                    CustomInstaller installer = impl.getInstallInfo ().getCustomInstaller ();
                    assert installer != null : "CustomInstaller must found for " + impl.getUpdateElement ();
                    installer.install (impl.getCodeName (), impl.getSpecificationVersion ().toString (), progress);
                }
            } finally {
                if (progress != null) {
                    progress.finish();
                }
            }
        }
        public void doCancel () throws OperationException {
            assert false : "Not supported yet";
        }
        
    }
}
