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
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Wizard to create a new NetBeans Module project.
 *
 * @author mkrauskopf
 */
public class NewNbModuleWizardIterator implements WizardDescriptor.InstantiatingIterator {
    
    private static final long serialVersionUID = 1L;
    
    static final String PROP_NAME_INDEX = "nameIndex"; //NOI18N
    
    private transient int position;
    private transient WizardDescriptor.Panel[] panels;
    private transient WizardDescriptor settings;
    
    /** Create a new wizard iterator. */
    public NewNbModuleWizardIterator() {}
    
    public Set instantiate() throws IOException {
        final NewModuleProjectData data = (NewModuleProjectData) settings.
                getProperty("moduleProjectData"); // XXX should be constant
        
        // create project
        final File projectFolder = new File(data.getProjectFolder());
        NbModuleProjectGenerator.createStandAloneModule(projectFolder,
                data.getProjectDisplayName(), data.getCodeNameBase(),
                data.getBundle(), data.getLayer(), data.getPlatform());
        
        FileObject projectFolderFO = FileUtil.toFileObject(FileUtil.normalizeFile(projectFolder));
        
        Set/*<FileObject>*/ resultSet = new HashSet();
        resultSet.add(projectFolderFO);
        
        final File chooserFolder = (projectFolder != null) ?
            projectFolder.getParentFile() : null;
        if (chooserFolder != null && chooserFolder.exists()) {
            ProjectChooser.setProjectsFolder(chooserFolder);
        }
        
        return resultSet;
    }
    
    public void initialize(WizardDescriptor wiz) {
        this.settings = wiz;
        settings.putProperty("moduleProjectData", new NewModuleProjectData()); // NOI18N
        position = 0;
        panels = new WizardDescriptor.Panel[] {
            new BasicInfoWizardPanel(settings),
                    new BasicConfWizardPanel(settings)
        };
        String[] steps = new String[] {
            "Name and Location", "Basic Module Configuration"
        };
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
    
    public String name() {
        return "NewNbModuleWizardIterator.name()";
//        return MessageFormat.format(
//                NbBundle.getMessage(NewNbModuleWizardIterator.class, "LBL_WizardStepsCount"),
//                new String[] {(new Integer(position + 1)).toString(),
//                        (new Integer(panels.length)).toString()}); //NOI18N
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
    
    // If nothing unusual changes in the middle of the wizard, simply:
    public final void addChangeListener(ChangeListener l) {}
    public final void removeChangeListener(ChangeListener l) {}
}
