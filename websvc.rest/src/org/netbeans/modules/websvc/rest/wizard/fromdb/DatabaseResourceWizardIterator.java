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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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
package org.netbeans.modules.websvc.rest.wizard.fromdb;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.TypeElement;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.progress.aggregate.AggregateProgressFactory;
import org.netbeans.api.progress.aggregate.AggregateProgressHandle;
import org.netbeans.api.progress.aggregate.ProgressContributor;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.persistence.api.PersistenceLocation;
import org.netbeans.modules.j2ee.persistence.api.metadata.orm.Entity;
import org.netbeans.modules.j2ee.persistence.dd.common.Persistence;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.EntityClassesPanel;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.FacadeGenerator;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.FacadeGeneratorProvider;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.PersistenceGenerator;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.PersistenceGeneratorProvider;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.ProgressPanel;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.RelatedCMPHelper;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.RelatedCMPWizard;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.Table;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.TableClosure;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.Table.DisabledReason;
import org.netbeans.modules.j2ee.persistence.wizard.fromdb.Table.ExistingDisabledReason;
import org.netbeans.modules.websvc.api.support.LogUtils;
import org.netbeans.modules.websvc.rest.RestUtils;
import org.netbeans.modules.websvc.rest.codegen.Constants;
import org.netbeans.modules.websvc.rest.codegen.EntityResourcesGenerator;
import org.netbeans.modules.websvc.rest.codegen.EntityResourcesGeneratorFactory;
import org.netbeans.modules.websvc.rest.codegen.model.EntityClassInfo;
import org.netbeans.modules.websvc.rest.codegen.model.EntityResourceBeanModel;
import org.netbeans.modules.websvc.rest.codegen.model.EntityResourceModelBuilder;
import org.netbeans.modules.websvc.rest.codegen.model.TypeUtil;
import org.netbeans.modules.websvc.rest.support.PersistenceHelper;
import org.netbeans.modules.websvc.rest.support.PersistenceHelper.PersistenceUnit;
import org.netbeans.modules.websvc.rest.support.SourceGroupSupport;
import org.netbeans.modules.websvc.rest.wizard.EntityResourcesIterator;
import org.netbeans.modules.websvc.rest.wizard.EntityResourcesSetupPanel;
import org.netbeans.modules.websvc.rest.wizard.Util;
import org.netbeans.modules.websvc.rest.wizard.WizardProperties;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;

import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;
import org.openide.util.RequestProcessor;

public final class DatabaseResourceWizardIterator implements WizardDescriptor.InstantiatingIterator {

    private int index;
    private WizardDescriptor wizard;
    private WizardDescriptor.Panel<?>[] panels;
    private static final String PROP_CMP = "wizard-is-cmp"; //NOI18N
    private static final String PROP_HELPER = "wizard-helper"; //NOI18N
    private static final Lookup.Result<PersistenceGeneratorProvider> PERSISTENCE_PROVIDERS =
            Lookup.getDefault().lookupResult(PersistenceGeneratorProvider.class);
    private RelatedCMPHelper helper;
    private ProgressPanel progressPanel;
    private PersistenceGenerator generator;

    @Override
    public void initialize(WizardDescriptor wizard) {
        this.wizard = wizard;
        Project project = Templates.getProject(wizard);
        generator = createPersistenceGenerator("jpa");
        FileObject configFilesFolder = PersistenceLocation.getLocation(project);
        helper = new RelatedCMPHelper(project, configFilesFolder, generator);
        wizard.putProperty(PROP_HELPER, helper);
        wizard.putProperty(PROP_CMP, new Boolean(false));

        // Moved to getPanels()
        //String wizardBundleKey = "Templates/Persistence/RelatedCMP"; // NOI18N
        //wizard.putProperty("NewFileWizard_Title", NbBundle.getMessage(RelatedCMPWizard.class, wizardBundleKey)); // NOI18N

        generator.init(wizard);
        SourceGroup[] sourceGroups = SourceGroupSupport.getJavaSourceGroups(project);
        if (Templates.getTargetFolder(wizard) == null && sourceGroups.length > 0) {
            Templates.setTargetFolder(wizard, sourceGroups[0].getRootFolder());
        }

    }

    @Override
    public Set<?> instantiate() throws IOException {
        // create the pu first if needed
        if(helper.isCreatePU()) {
            Project project = Templates.getProject(wizard);
            if ( RestUtils.hasSpringSupport(project) ) {
                org.netbeans.modules.j2ee.persistence.wizard.Util.
                    addPersistenceUnitToProject(project,org.netbeans.modules.j2ee.
                        persistence.wizard.Util.
                        buildPersistenceUnitUsingData(project, null, 
                                helper.getTableSource().getName(), null, null,
                                Persistence.VERSION_1_0));
            }
            else {
                org.netbeans.modules.j2ee.persistence.wizard.Util.
                    addPersistenceUnitToProject(project,org.netbeans.modules.j2ee.
                            persistence.wizard.Util.
                            buildPersistenceUnitUsingData(project, null, 
                            helper.getTableSource().getName(), null, null));
            }
        }

        final String title = NbBundle.getMessage(RelatedCMPWizard.class, "TXT_EntityClassesGeneration");
        final ProgressContributor progressContributor = AggregateProgressFactory.createProgressContributor(title);
        final AggregateProgressHandle aggregateHandle =
                AggregateProgressFactory.createHandle(title, new ProgressContributor[]{progressContributor}, null, null);
        progressPanel = new ProgressPanel();
        final JComponent progressComponent = AggregateProgressFactory.createProgressComponent(aggregateHandle);

        final Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    aggregateHandle.start();
                    generate(progressContributor);
                } catch (IOException ioe) {
                    Logger.getLogger("global").log(Level.INFO, null, ioe);
                    NotifyDescriptor nd = new NotifyDescriptor.Message(ioe.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                    DialogDisplayer.getDefault().notify(nd);
                } finally {
                    generator.uninit();
                    aggregateHandle.finish();
                }
            }
        };

        // Ugly hack ensuring the progress dialog opens after the wizard closes. Needed because:
        // 1) the wizard is not closed in the AWT event in which instantiate() is called.
        //    Instead it is closed in an event scheduled by SwingUtilities.invokeLater().
        // 2) when a modal dialog is created its owner is set to the foremost modal
        //    dialog already displayed (if any). Because of #1 the wizard will be
        //    closed when the progress dialog is already open, and since the wizard
        //    is the owner of the progress dialog, the progress dialog is closed too.
        // The order of the events in the event queue:
        // -  this event
        // -  the first invocation event of our runnable
        // -  the invocation event which closes the wizard
        // -  the second invocation event of our runnable

        SwingUtilities.invokeLater(new Runnable() {

            private boolean first = true;

            public void run() {
                if (!first) {
                    RequestProcessor.getDefault().post(r);
                    progressPanel.open(progressComponent, title);
                } else {
                    first = false;
                    SwingUtilities.invokeLater(this);
                }
            }
        });

        // The commented code below is the ideal state, but since there is not way to request
        // TemplateWizard.Iterator.instantiate() be called asynchronously it
        // would cause the wizard to stay visible until the bean generation process
        // finishes. So for now just returning the package -- not a problem,
        // JavaPersistenceGenerator.createdObjects() returns an empty set anyway.

        // remember to wait for generate() to actually return!
        // Set created = generator.createdObjects();
        // if (created.size() == 0) {
        //     created = Collections.singleton(SourceGroupSupport.getFolderForPackage(helper.getLocation(), helper.getPackageName()));
        // }

        // logging usage of wizard
        Object[] params = new Object[5];
        params[0] = LogUtils.WS_STACK_JAXRS;
        Project project = Templates.getProject(wizard);
        params[1] = project.getClass().getName();
        J2eeModule j2eeModule = RestUtils.getJ2eeModule(project);
        params[2] = j2eeModule == null ? null : j2eeModule.getModuleVersion()+"(WAR)"; //NOI18N
        params[3] = "REST FROM DATABASE"; //NOI18N
        LogUtils.logWsWizard(params);

        return Collections.<DataObject>singleton(DataFolder.findFolder(
                getFolderForPackage(helper.getLocation(), helper.getPackageName())));
    }

    private static PersistenceGenerator createPersistenceGenerator(String type) {
        assert type != null;

        Collection<? extends PersistenceGeneratorProvider> providers = PERSISTENCE_PROVIDERS.allInstances();
        for (PersistenceGeneratorProvider provider : providers) {
            if (type.equals(provider.getGeneratorType())) {
                return provider.createGenerator();
            }
        }
        throw new AssertionError("Could not find a persistence generator of type " + type); // NOI18N

    }

    private void generate(ProgressContributor handle) throws IOException {
        final Project project = Templates.getProject(wizard);
        try {
            handle.start(1); //TODO: need the correct number of work units here 


            handle.progress(NbBundle.getMessage(RelatedCMPWizard.class, "TXT_SavingSchema"));
            progressPanel.setText(NbBundle.getMessage(RelatedCMPWizard.class, "TXT_SavingSchema"));

            FileObject dbschemaFile = helper.getDBSchemaFile();

            String extracting = NbBundle.getMessage(RelatedCMPWizard.class, "TXT_ExtractingEntityClassesAndRelationships");

            handle.progress(extracting);
            progressPanel.setText(extracting);

            helper.buildBeans();

            generator.generateBeans(progressPanel, helper, dbschemaFile, handle);

            Set<FileObject> files = getAffectedFiles(generator, helper );

            RestUtils.ensureRestDevelopmentReady(project);
            Set<String> entities = Util.getEntities(project, files);
            RestUtils.configRestPackages(project, 
                    wizard.getProperty(WizardProperties.RESOURCE_PACKAGE).toString());
            
            if (!RestUtils.hasSpringSupport(project) && RestUtils.isJavaEE6(project)) {
                String targetPackage = null;
                String resourcePackage = null;
                String controllerPackage = null;
                FileObject targetResourceFolder = null;

                FileObject targetFolder = (FileObject) wizard.getProperty(WizardProperties.TARGET_SRC_ROOT);
                if (targetFolder != null) {
                    targetPackage = SourceGroupSupport.packageForFolder(targetFolder);
                    resourcePackage = (String) wizard.getProperty(WizardProperties.RESOURCE_PACKAGE);
                    SourceGroup[] sourceGroups = SourceGroupSupport.getJavaSourceGroups(project);
                    SourceGroup targetSourceGroup = SourceGroupSupport.findSourceGroupForFile(sourceGroups, targetFolder);
                    if (targetSourceGroup != null) {
                        targetResourceFolder = SourceGroupSupport.getFolderForPackage(targetSourceGroup, resourcePackage, true);
                    }
                } else {
                    targetFolder = Templates.getTargetFolder(wizard);
                    SourceGroup targetSourceGroup = null;
                    targetPackage = "";
                    if (targetFolder != null) {
                        SourceGroup[] sourceGroups = SourceGroupSupport.getJavaSourceGroups(project);
                        targetSourceGroup = SourceGroupSupport.findSourceGroupForFile(sourceGroups, targetFolder);
                        if (targetSourceGroup != null) {
                            targetPackage = SourceGroupSupport.getPackageForFolder(targetSourceGroup, targetFolder);

                        }
                    }

                    targetPackage = (targetPackage.length() == 0) ? "" : targetPackage + ".";
                    resourcePackage = targetPackage + EntityResourcesGenerator.RESOURCE_FOLDER;
                    if (targetSourceGroup != null) {
                        targetResourceFolder = SourceGroupSupport.getFolderForPackage(targetSourceGroup, resourcePackage, true);
                    }
                }
                
                if (targetResourceFolder == null) {
                    targetResourceFolder = targetFolder;
                }

                EntityResourceModelBuilder builder = new EntityResourceModelBuilder(project, entities);
                EntityResourceBeanModel model = builder.build();
                Util.generateRESTFacades(project, entities,  model,
                        targetResourceFolder, resourcePackage);

            } else {    
                EntityResourceModelBuilder builder = new EntityResourceModelBuilder(project, entities);
                EntityResourceBeanModel model = builder.build();

                PersistenceUnit pu = new PersistenceHelper(project).getPersistenceUnit();

                FileObject targetFolder = (FileObject) wizard.getProperty(WizardProperties.TARGET_SRC_ROOT);
                String targetPackage = null;
                String resourcePackage = null;
                String controllerPackage = null;

                if (targetFolder != null) {
                    targetPackage = SourceGroupSupport.packageForFolder(targetFolder);
                    resourcePackage = (String) wizard.getProperty(WizardProperties.RESOURCE_PACKAGE);
                    controllerPackage = (String) wizard.getProperty(WizardProperties.CONTROLLER_PACKAGE);
                } else {
                    targetFolder = Templates.getTargetFolder(wizard);
                    SourceGroup targetSourceGroup = null;
                    targetPackage = "";
                    if (targetFolder != null) {
                        SourceGroup[] sourceGroups = SourceGroupSupport.getJavaSourceGroups(project);
                        targetSourceGroup = SourceGroupSupport.findSourceGroupForFile(sourceGroups, targetFolder);
                        if (targetSourceGroup != null) {
                            targetPackage = SourceGroupSupport.getPackageForFolder(targetSourceGroup, targetFolder);
                        }
                    }

                    targetPackage = (targetPackage.length() == 0) ? "" : targetPackage + ".";
                    resourcePackage = targetPackage + EntityResourcesGenerator.RESOURCE_FOLDER;
                    controllerPackage = targetPackage + EntityResourcesGenerator.CONTROLLER_FOLDER;
                }

                final EntityResourcesGenerator gen = EntityResourcesGeneratorFactory.newInstance(project);
                gen.initialize(model, project, targetFolder, targetPackage, 
                        resourcePackage, controllerPackage, pu);

                RequestProcessor.Task transformTask = RequestProcessor.getDefault().create(new Runnable() {

                    public void run() {
                        try {
                            RestUtils.disableRestServicesChangeListner(project);
                            gen.generate(null);

                        } catch (Exception iox) {
                            Exceptions.printStackTrace(iox);
                        } finally {
                            RestUtils.enableRestServicesChangeListner(project);

                        }
                    }
                });
                transformTask.schedule(50);
            }

        } finally {
            handle.finish();
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    progressPanel.close();
                }
            });
        }

    }

    private Set<FileObject> getAffectedFiles( PersistenceGenerator generator,
            RelatedCMPHelper helper )
    {
        Set<FileObject> created = generator.createdObjects();
        TableClosure closure = helper.getTableClosure();
        Set<Table> tables = closure.getSelectedTables();
        Set<FileObject> extension = new HashSet<FileObject>(); 
        for(Table table :tables ){
            if ( table.isDisabled() ){
                DisabledReason reason = table.getDisabledReason();
                if ( reason instanceof ExistingDisabledReason ){
                    String fqnClass = ((ExistingDisabledReason)reason).getFQClassName();
                    try {
                        FileObject fileObject = SourceGroupSupport.
                            getFileObjectFromClassName(fqnClass, helper.getProject());
                        if ( !created.contains( fileObject) ){
                            extension.add( fileObject );
                        }
                    }
                    catch(IOException e){
                        Logger.getLogger("global").log(Level.SEVERE, null, e);
                    }
                }
            }
        }
        if ( extension.size() == 0 ){
            return created;
        }
        else {
            extension.addAll( created );
            return extension;
        }
    }

    /**
     * Initialize panels representing individual wizard's steps and sets
     * various properties for them influencing wizard appearance.
     */
    private WizardDescriptor.Panel<?>[] getPanels() {
        if (panels == null) {

            String wizardBundleKey = "Templates/WebServices/RestServicesFromDatabase"; // NOI18N
            String wizardTitle = NbBundle.getMessage(EntityResourcesIterator.class, wizardBundleKey); // NOI18N
            Project project = Templates.getProject(wizard);
            boolean withoutController = RestUtils.isJavaEE6( project ) || 
                RestUtils.hasSpringSupport(project);
            panels = new WizardDescriptor.Panel[]{
                        //new DatabaseResourceWizardPanel1()
                        new org.netbeans.modules.j2ee.persistence.wizard.fromdb.DatabaseTablesPanel.WizardPanel(wizardTitle),
                        new EntityClassesPanel.WizardPanel(true, withoutController),
                        new EntityResourcesSetupPanel(NbBundle.getMessage(
                                EntityResourcesIterator.class,
                                "LBL_RestResourcesAndClasses"), wizard, 
                                withoutController)
                    };

            String[] steps = createSteps();
            for (int i = 0; i < panels.length; i++) {
                Component c = panels[i].getComponent();
                if (steps[i] == null) {
                    // Default step name to component name of panel. Mainly
                    // useful for getting the name of the target chooser to
                    // appear in the list of steps.
                    steps[i] = c.getName();
                }
                if (c instanceof JComponent) { // assume Swing components

                    JComponent jc = (JComponent) c;
                    // Sets step number of a component
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_SELECTED_INDEX, new Integer(i));
                    // Sets steps names for a panel
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DATA, steps);
                    // Turn on subtitle creation on each step
                    jc.putClientProperty(WizardDescriptor.PROP_AUTO_WIZARD_STYLE, Boolean.TRUE);
                    // Show steps on the left side with the image on the background
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_DISPLAYED, Boolean.TRUE);
                    // Turn on numbering of all steps
                    jc.putClientProperty(WizardDescriptor.PROP_CONTENT_NUMBERED, Boolean.TRUE);
                }
            }
        }
        return panels;
    }

    public void uninitialize(WizardDescriptor wizard) {
        panels = null;
    }

    public WizardDescriptor.Panel current() {
        return getPanels()[index];
    }

    public String name() {
        return index + 1 + ". from " + getPanels().length;
    }

    public boolean hasNext() {
        return index < getPanels().length - 1;
    }

    public boolean hasPrevious() {
        return index > 0;
    }

    public void nextPanel() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        index++;
    }

    public void previousPanel() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        }
        index--;
    }

    // If nothing unusual changes in the middle of the wizard, simply:
    public void addChangeListener(ChangeListener l) {
    }

    public void removeChangeListener(ChangeListener l) {
    }


    // You could safely ignore this method. Is is here to keep steps which were
    // there before this wizard was instantiated. It should be better handled
    // by NetBeans Wizard API itself rather than needed to be implemented by a
    // client code.
    private String[] createSteps() {
        String[] beforeSteps = null;
        Object prop = wizard.getProperty(WizardDescriptor.PROP_CONTENT_DATA);
        if (prop != null && prop instanceof String[]) {
            beforeSteps = (String[]) prop;
        }

        if (beforeSteps == null) {
            beforeSteps = new String[0];
        }

        String[] res = new String[(beforeSteps.length - 1) + panels.length];
        for (int i = 0; i < res.length; i++) {
            if (i < (beforeSteps.length - 1)) {
                res[i] = beforeSteps[i];
            } else {
                res[i] = panels[i - beforeSteps.length + 1].getComponent().getName();
            }
        }
        return res;
    }

    /**
     * Gets the folder representing the given <code>packageName</code>.
     * 
     * @param sourceGroup the source group of the package; must not be null.
     * @param packageName the name of the package; must not be null.
     * @param create specifies whether the folder should be created if it does not exist.
     * @return the folder representing the given package or null if it was not found.
     */
    public static FileObject getFolderForPackage(SourceGroup sourceGroup, String packageName) throws IOException {
        Parameters.notNull("sourceGroup", sourceGroup); //NOI18N

        Parameters.notNull("packageName", packageName); //NOI18N

        String relativePkgName = packageName.replace('.', '/');
        FileObject folder = sourceGroup.getRootFolder().getFileObject(relativePkgName);
        if (folder != null) {
            return folder;
        } else {
            return FileUtil.createFolder(sourceGroup.getRootFolder(), relativePkgName);
        }
    }

}
