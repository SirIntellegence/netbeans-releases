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

package org.netbeans.modules.apisupport.project.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tools.ant.module.api.support.ActionUtils;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.apisupport.project.NbModuleProject;
import org.netbeans.modules.apisupport.project.suite.SuiteProject;
import org.netbeans.modules.apisupport.project.ui.customizer.SuiteUtils;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.MoveOperationImplementation;
import org.netbeans.spi.project.support.ant.GeneratedFilesHelper;
import org.openide.filesystems.FileObject;

/**
 * @author Martin Krauskopf
 */
public final class ModuleOperations implements DeleteOperationImplementation, MoveOperationImplementation {
    
    private static final Map/*<String, SuiteProject>*/ TEMPORARY_CACHE = new HashMap();
    
    private final NbModuleProject project;
    private final FileObject projectDir;
    
    public ModuleOperations(final NbModuleProject project) {
        this.project = project;
        this.projectDir = project.getProjectDirectory();
    }
    
    public void notifyDeleting() throws IOException {
        FileObject buildXML = projectDir.getFileObject(GeneratedFilesHelper.BUILD_XML_PATH);
        ActionUtils.runTarget(buildXML, new String[] { ActionProvider.COMMAND_CLEAN }, null).waitFinished();
        
        SuiteProject suite = SuiteUtils.findSuite(project);
        if (suite != null) {
            SuiteUtils.removeModuleFromSuite(project);
        }
        
        project.notifyDeleting();
    }
    
    public void notifyDeleted() throws IOException {
        project.getHelper().notifyDeleted();
    }
    
    public void notifyMoving() throws IOException {
        SuiteProject suite = SuiteUtils.findSuite(project);
        if (suite != null) {
            TEMPORARY_CACHE.put(project.getCodeNameBase(), suite);
        }
        notifyDeleting();
    }
    
    public void notifyMoved(Project original, File originalPath, String nueName) throws IOException {
        if (original == null) { // called on the original project
            project.getHelper().notifyDeleted();
        } else { // called on the new project
            SuiteProject suite = (SuiteProject) TEMPORARY_CACHE.remove(project.getCodeNameBase());
            if (suite != null) {
                SuiteUtils.addModule(suite, project);
            }
        }
    }
    
    public List/*<FileObject>*/ getMetadataFiles() {
        List/*<FileObject>*/ files = new ArrayList();
        addFile(GeneratedFilesHelper.BUILD_XML_PATH, files);
        addFile("manifest.mf", files); // NOI18N
        addFile("nbproject", files); // NOI18N
        addFile(".cvsignore", files); // NOI18N
        return files;
    }
    
    public List/*<FileObject>*/ getDataFiles() {
        List/*<FileObject>*/ files = new ArrayList();
        
        Sources srcs = ProjectUtils.getSources(project);
        SourceGroup[] grps = srcs.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        for (int i = 0; i < grps.length; i++) {
            FileObject srcRoot = grps[i].getRootFolder();
            if (srcRoot.getPath().endsWith("test/unit/src")) { // NOI18N
                addFile("test", files); // NOI18N
            } else {
                files.add(srcRoot);
            }
        }
        
        return files;
    }
    
    private void addFile(String fileName, List/*<FileObject>*/ result) {
        FileObject file = projectDir.getFileObject(fileName);
        if (file != null) {
            result.add(file);
        }
    }
    
}
