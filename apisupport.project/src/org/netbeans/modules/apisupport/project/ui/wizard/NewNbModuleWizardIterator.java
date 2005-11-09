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

package org.netbeans.modules.apisupport.project.ui.wizard;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.apisupport.project.NbModuleProjectGenerator;
import org.netbeans.modules.apisupport.project.suite.SuiteProjectGenerator;
import org.netbeans.modules.apisupport.project.ui.ModuleUISettings;
import org.netbeans.modules.apisupport.project.ui.UIUtil;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;

/**
 * Wizard to create a new NetBeans Module project.
 *
 * @author mkrauskopf
 */
public class NewNbModuleWizardIterator implements WizardDescriptor.InstantiatingIterator {
    
    private static final long serialVersionUID = 1L;
    
    final static int TYPE_MODULE = 1;
    final static int TYPE_SUITE = 2;
    final static int TYPE_LIBRARY_MODULE = 3;
    
    static final String PROP_NAME_INDEX = "nameIndex"; //NOI18N
    
    private transient int position;
    private transient WizardDescriptor.Panel[] panels;
    private transient WizardDescriptor settings;
    
    private transient int type;
    
    /** Create a new wizard iterator. */
    private NewNbModuleWizardIterator(int type) {
        this.type = type;
    }
    
    public static NewNbModuleWizardIterator createModuleIterator() {
        return new NewNbModuleWizardIterator(TYPE_MODULE);
    }
    
    public static NewNbModuleWizardIterator createSuiteIterator() {
        return new NewNbModuleWizardIterator(TYPE_SUITE);
    }
    
    public static NewNbModuleWizardIterator createLibraryModuleIterator() {
        return new NewNbModuleWizardIterator(TYPE_LIBRARY_MODULE);
    }
    
    
    public Set instantiate() throws IOException {
        final NewModuleProjectData data = NewModuleProjectData.getData(settings);
        final File projectFolder = new File(data.getProjectFolder());
        ModuleUISettings.getDefault().setLastUsedModuleLocation(data.getProjectLocation());
        if (type == TYPE_MODULE) {
            ModuleUISettings.getDefault().setNewModuleCounter(data.getModuleCounter());
            if (data.isNetBeansOrg()) {
                // create module within the netbeans.org CVS tree
                NbModuleProjectGenerator.createNetBeansOrgModule(projectFolder,
                        data.getCodeNameBase(), data.getProjectDisplayName(),
                        data.getBundle(), data.getLayer());
            } else if (data.isStandalone()) {
                // create standalone module
                NbModuleProjectGenerator.createStandAloneModule(projectFolder,
                        data.getCodeNameBase(), data.getProjectDisplayName(),
                        data.getBundle(), data.getLayer(), data.getPlatform());
            } else {
                // create suite-component module
                NbModuleProjectGenerator.createSuiteComponentModule(projectFolder,
                        data.getCodeNameBase(), data.getProjectDisplayName(),
                        data.getBundle(), data.getLayer(), new File(data.getSuiteRoot()));
            }
        } else if (type == TYPE_LIBRARY_MODULE) {
                // create suite-component module
                File[] jars = LibraryStartVisualPanel.convertStringToFiles((String)settings.getProperty(LibraryStartVisualPanel.PROP_LIBRARY_PATH));
                
                File license = null;
                String licPath = (String)settings.getProperty(LibraryStartVisualPanel.PROP_LICENSE_PATH);
                if (licPath != null && licPath.length() > 0) {
                    license = new File(licPath);
                } 
                NbModuleProjectGenerator.createSuiteLibraryModule(projectFolder,
                        data.getCodeNameBase(), data.getProjectDisplayName(),
                        data.getBundle(), new File(data.getSuiteRoot()), 
                        license, jars);
            
        } else if (this.type == TYPE_SUITE) {
            ModuleUISettings.getDefault().setNewSuiteCounter(data.getSuiteCounter());
            SuiteProjectGenerator.createSuiteProject(projectFolder, data.getPlatform());
        } else {
            throw new IllegalStateException("Uknown wizard type: " + this.type); // NOI18N
        }
        
        FileObject projectFolderFO = FileUtil.toFileObject(FileUtil.normalizeFile(projectFolder));
        
        Set/*<FileObject>*/ resultSet = new HashSet();
        resultSet.add(projectFolderFO);
        
        UIUtil.setProjectChooserDirParent(projectFolder);
        
        // XXX this constant should be defined somewhere!
        settings.putProperty("setAsMain", Boolean.valueOf(data.isMainProject())); // NOI18N
        
        return resultSet;
    }
    
    public void initialize(WizardDescriptor wiz) {
        this.settings = wiz;
        position = 0;
        String[] steps = null;
        switch (type) {
            case TYPE_MODULE:
                steps = initModuleWizard();
                break;
            case TYPE_SUITE:
                steps = initSuiteModuleWizard();
                break;
            case TYPE_LIBRARY_MODULE:
                steps = initLibraryModuleWizard();
                break;
            default:
                assert false : "Should never get here. type: "  + type; // NOI18N
        }
        for (int i = 0; i < panels.length; i++) {
            Component c = panels[i].getComponent();
            if (c instanceof JComponent) { // assume Swing components
                JComponent jc = (JComponent)c;
                // step number
                jc.putClientProperty("WizardPanel_contentSelectedIndex", new Integer(i)); // NOI18N
                // names of currently used steps
                jc.putClientProperty("WizardPanel_contentData", steps); // NOI18N
            }
        }
    }
    
    public void uninitialize(WizardDescriptor wiz) {
        this.settings = null;
        panels = null;
    }
    
    private String[] initModuleWizard() {
        panels = new WizardDescriptor.Panel[] {
            new BasicInfoWizardPanel(settings, TYPE_MODULE),
            new BasicConfWizardPanel(settings)
        };
        String[] steps = new String[] {
            getMessage("LBL_BasicInfoPanel_Title"), // NOI18N
            getMessage("LBL_BasicConfigPanel_Title") // NOI18N
        };
        return steps;
    }
    
    private String[] initSuiteModuleWizard() {
        panels = new WizardDescriptor.Panel[] {
            new BasicInfoWizardPanel(settings, TYPE_SUITE),
        };
        String[] steps = new String[] {
            getMessage("LBL_BasicInfoPanel_Title"), // NOI18N
        };
        return steps;
    }
    
    private String[] initLibraryModuleWizard() {
        panels = new WizardDescriptor.Panel[] {
            new LibraryStartWizardPanel(settings),
            new BasicInfoWizardPanel(settings, TYPE_LIBRARY_MODULE),
            new LibraryConfWizardPanel(settings)
        };
        String[] steps = new String[] {
            getMessage("LBL_LibraryStartPanel_Title"), //NOi18N
            getMessage("LBL_BasicInfoPanel_Title"), // NOI18N
            getMessage("LBL_PlatformSelectionPanel_Title") // NOI18N
        };
        return steps;
    }
    
    public String name() {
        return null; // XXX is this used somewhere actually
    }
    
    public boolean hasNext() {
        return position < (panels.length - 1);
    }
    
    public boolean hasPrevious() {
        return position > 0;
    }
    
    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        position++;
    }
    
    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        position--;
    }
    
    public WizardDescriptor.Panel current() {
        return panels[position];
    }
    
    /**
     * Convenience method for accessing Bundle resources from this package.
     */
    static String getMessage(String key) {
        return NbBundle.getMessage(NewNbModuleWizardIterator.class, key);
    }
    
    // If nothing unusual changes in the middle of the wizard, simply:
    public final void addChangeListener(ChangeListener l) {}
    public final void removeChangeListener(ChangeListener l) {}
}
