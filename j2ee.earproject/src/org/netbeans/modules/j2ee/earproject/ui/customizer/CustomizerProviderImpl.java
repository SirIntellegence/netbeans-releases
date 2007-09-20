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

package org.netbeans.modules.j2ee.earproject.ui.customizer;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.j2ee.earproject.EarProject;
import org.netbeans.spi.project.support.ant.AntBasedProjectType;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.ReferenceHelper;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/** 
 * Customization of Enterprise Application project.
 *
 * @author Petr Hrebejk
 */
public class CustomizerProviderImpl implements CustomizerProvider {
    
    private final Project project;
    private final AntProjectHelper antProjectHelper;   
    private final ReferenceHelper refHelper;
    private final AntBasedProjectType abpt;
    
    private static Map<Project, Dialog> project2Dialog = new HashMap<Project, Dialog>();
    
    public static final String CUSTOMIZER_FOLDER_PATH = "Projects/org-netbeans-modules-j2ee-earproject/Customizer"; //NO18N
     
    public CustomizerProviderImpl(Project project, AntProjectHelper antProjectHelper, ReferenceHelper refHelper, AntBasedProjectType abpt) {
        this.project = project;
        this.antProjectHelper = antProjectHelper;
        this.refHelper = refHelper;
        this.abpt = abpt;
    }
    
    public void showCustomizer() {
        showCustomizer(null);
    }
    
    public void showCustomizer(String preselectedCategory) {
        showCustomizer(preselectedCategory, null);
    }
    
    public void showCustomizer(String preselectedCategory, String preselectedSubCategory) {
        
        Dialog dialog = project2Dialog.get(project);
        if (dialog != null) {
            dialog.setVisible(true);
            return;
        } else {
            EarProjectProperties uiProperties = new EarProjectProperties((EarProject) project, refHelper, abpt);
            Lookup context = Lookups.fixed(new Object[] {
                project,
                uiProperties,
                new SubCategoryProvider(preselectedCategory, preselectedSubCategory)
            });

            OptionListener listener = new OptionListener(project, uiProperties);
            dialog = ProjectCustomizer.createCustomizerDialog(CUSTOMIZER_FOLDER_PATH, context, preselectedCategory, listener, null);
            dialog.addWindowListener(listener);
            dialog.setTitle(MessageFormat.format(
                    NbBundle.getMessage(CustomizerProviderImpl.class, "LBL_Customizer_Title"), // NOI18N
                    new Object[] { ProjectUtils.getInformation(project).getDisplayName() }));

            project2Dialog.put(project, dialog);
            dialog.setVisible(true);
        }
    }
    
    /** Listens to the actions on the Customizer's option buttons */
    private class OptionListener extends WindowAdapter implements ActionListener {
    
        private Project project;
        private EarProjectProperties uiProperties;
        
        OptionListener(Project project, EarProjectProperties uiProperties) {
            this.project = project;
            this.uiProperties = uiProperties;
        }
        
        // Listening to OK button ----------------------------------------------
        
        public void actionPerformed(ActionEvent e) {
            // Store the properties into project 
            
//#95952 some users experience this assertion on a fairly random set of changes in 
// the customizer, that leads me to assume that a project can be already marked
// as modified before the project customizer is shown. 
//            assert !ProjectManager.getDefault().isModified(project) : 
//                "Some of the customizer panels has written the changed data before OK Button was pressed. Please file it as bug."; //NOI18N
            uiProperties.store();
            
            // Close & dispose the the dialog
            Dialog dialog = project2Dialog.get(project);
            if (dialog != null) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        }
        
        // Listening to window events ------------------------------------------
                
        @Override
        public void windowClosed(WindowEvent e) {
            project2Dialog.remove(project);
        }    
        
        @Override
        public void windowClosing(WindowEvent e) {
            //Dispose the dialog otherwsie the {@link WindowAdapter#windowClosed}
            //may not be called
            Dialog dialog = project2Dialog.get(project);
            if (dialog != null) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        }
    }
    
    static final class SubCategoryProvider {

        private String subcategory;
        private String category;

        SubCategoryProvider(String category, String subcategory) {
            this.category = category;
            this.subcategory = subcategory;
        }
        public String getCategory() {
            return category;
        }
        public String getSubcategory() {
            return subcategory;
        }
    }
}
