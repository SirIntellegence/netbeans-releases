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
/*
 * CreateTestAction.java
 *
 * Created on January 19, 2001, 1:00 PM
 */

package org.netbeans.modules.junit;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import javax.swing.Action;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Message;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.netbeans.modules.javacore.api.JavaModel;
import org.netbeans.jmi.javamodel.*;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileUtil;



/** Action sensitive to some cookie that does something useful.
 *
 * @author  vstejskal, David Konecny
 * @author  Marian Petras
 * @author  Ondrej Rypacek
 */
public class CreateTestAction extends TestAction {
        
        public CreateTestAction() {
            putValue("noIconInMenu", Boolean.TRUE);                     //NOI18N
        }
        
        /* public members */
        public String getName() {
            return NbBundle.getMessage(CreateTestAction.class,
                                       "LBL_Action_CreateTest");        //NOI18N
        }
        
        public HelpCtx getHelpCtx() {
            return new HelpCtx(CreateTestAction.class);
        }
        
        
        
        protected void initialize() {
            super.initialize();
            putProperty(Action.SHORT_DESCRIPTION,
                        NbBundle.getMessage(CreateTestAction.class,
                                            "HINT_Action_CreateTest")); //NOI18N
        }
        
        protected String iconResource() {
            return "org/netbeans/modules/junit/resources/"              //NOI18N
                   + "CreateTestActionIcon.gif";                        //NOI18N
        }
        

        
        private static void noTemplateMessage(String temp) {
            String msg = NbBundle.getMessage(
                    CreateTestAction.class,
                    "MSG_template_not_found",                           //NOI18N
                    temp);
            NotifyDescriptor descr = new Message(
                    msg,
                    NotifyDescriptor.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(descr);
        }
        
        protected void performAction(Node[] nodes) {
            
            // show configuration dialog
            // when dialog is canceled, escape the action
            JUnitCfgOfCreate cfg = new JUnitCfgOfCreate(nodes);
            if (!cfg.configure()) {
                return;
            }
            final boolean singleClass = (nodes.length == 1)
                                        && cfg.isSingleClass();
            String testClassName = singleClass ? cfg.getTestClassName() : null;
            
            final FileObject targetFolder = cfg.getTargetFolder();
            final ClassPath testClassPath = ClassPathSupport.createClassPath(
                                               new FileObject[] {targetFolder});
            
            DataObject doTestTempl;
            if ((doTestTempl = loadTestTemplate("PROP_testClassTemplate"))
                    == null) {
                return;
            }
            
            DataObject doSuiteTempl = null;
            if (!singleClass) {
                if ((doSuiteTempl = loadTestTemplate("PROP_testSuiteTemplate"))
                        == null) {
                    return;
                }
            }
            
            TestCreator.initialize();
            
            ProgressIndicator progress = new ProgressIndicator();
            progress.show();
            
            String msg = NbBundle.getMessage(
                    CreateTestAction.class,
                    "MSG_StatusBar_CreateTest_Begin");                  //NOI18N
            progress.displayStatusText(msg);
            
            // results will be accumulated here
            CreationResults results;
            
            try {
                if (singleClass) {
                    assert testClassName != null;
                    
                    FileObject fo = getTestFileObject(nodes[0]);
                    if (fo != null) {
                        try {
                            results = createSingleTest(
                                    testClassPath,
                                    fo,
                                    testClassName,
                                    doTestTempl,
                                    null,              //parent suite
                                    progress,
                                    false);            //do not skip any classes
                        } catch (CreationError ex) {
                            ErrorManager.getDefault().notify(ex);
                            results = new CreationResults();
                        }
                    } else {
                        results = new CreationResults();
                    }
                } else {
                    results = new CreationResults();
                    
                    // go through all nodes
                    for(int nodeIdx = 0; nodeIdx < nodes.length; nodeIdx++) {
                        if (hasParentAmongNodes(nodes, nodeIdx)) {
                            continue;
                        }
                        FileObject fo = getTestFileObject(nodes[nodeIdx]);
                        if (fo == null) {
                            continue;
                        }
                        try {
                            results.combine(createTests(nodes[nodeIdx],
                                                        testClassPath,
                                                        doTestTempl,
                                                        doSuiteTempl,
                                                        null,
                                                        progress));
                        } catch (CreationError e) {
                            ErrorManager.getDefault().notify(e);
                        }
                    }
                }
            } finally {
                progress.hide();
            }
            
            
            if (!results.getSkipped().isEmpty()) {
                // something was skipped
                String message;
                if (results.getSkipped().size() == 1) {
                    // one class? report it
                    String className =
                            ((JavaClass) results.getSkipped().iterator().next())
                            .getName();
                    message = NbBundle.getMessage(CreateTestAction.class,
                                                  "MSG_skipped_class",  //NOI18N
                                                  className);
                } else {
                    // more classes, report a general error
                    message = NbBundle.getMessage(CreateTestAction.class,
                                                  "MSG_skipped_classes");
                }
                TestUtil.notifyUser(message,
                                    NotifyDescriptor.INFORMATION_MESSAGE);
                
            } else if (results.getCreated().size() == 1) {
                // created exactly one class, highlight it in the explorer
                // and open it in the editor
                DataObject dobj = (DataObject)
                                  results.getCreated().iterator().next();
                EditorCookie ec = (EditorCookie)
                                  dobj.getCookie(EditorCookie.class);
                if (ec != null) {
                    ec.open();
                }
            }
        }
        
        /**
         * Loads a test template.
         * If the template loading fails, displays an error message.
         *
         * @param  templateID  bundle key identifying the template type
         * @return  loaded template, or <code>null</code> if the template
         *          could not be loaded
         */
        private static DataObject loadTestTemplate(String templateID) {
            // get the Test class template
            String path = NbBundle.getMessage(CreateTestAction.class,
                                              templateID);
            try {
                FileObject fo = Repository.getDefault().getDefaultFileSystem()
                                .findResource(path);
                if (fo == null) {
                    noTemplateMessage(path);
                    return null;
                }
                return DataObject.find(fo);
            }
            catch (DataObjectNotFoundException e) {
                noTemplateMessage(path);
                return null;
            }
        }
        
        /**
         * Grabs and checks a <code>FileObject</code> from the given node.
         * If either the file could not be grabbed or the file does not pertain
         * to any project, a message is displayed.
         *
         * @param  node  node to get a <code>FileObject</code> from.
         * @return  the grabbed <code>FileObject</code>,
         *          or <code>null</code> in case of failure
         */
        private static FileObject getTestFileObject(final Node node) {
            final FileObject fo = TestUtil.getFileObjectFromNode(node);
            if (fo == null) {
                TestUtil.notifyUser(NbBundle.getMessage(
                        CreateTestAction.class,
                        "MSG_file_from_node_failed"));                  //NOI18N
                return null;
            }
            ClassPath cp = ClassPath.getClassPath(fo, ClassPath.SOURCE);
            if (cp == null) {
                TestUtil.notifyUser(NbBundle.getMessage(
                        CreateTestAction.class,
                        "MSG_no_project",                               //NOI18N
                        fo));
                return null;
            }
            return fo;
        }
        
        private static void ensureFolder(URL url) throws java.io.IOException {
            if (url.getProtocol().equals("file")) { // NOI18N
                String path = url.getPath();
                ensureFolder(new File(path));
            }
        }
        
        private static FileObject ensureFolder(File file)
                throws java.io.IOException {
            File parent = file.getParentFile();
            String name = file.getName();
            FileObject pfo = FileUtil.toFileObject(parent);
            if (pfo == null) pfo = ensureFolder(parent);
            return pfo.createFolder(name);
        }
        
        public static DataObject createSuiteTest(
                ClassPath testClassPath,
                DataFolder folder,
                String suiteName,
                LinkedList suite,
                DataObject doSuiteT,
                LinkedList parentSuite,
                ProgressIndicator progress) throws CreationError {
            
            // find correct package name
            FileObject fo = folder.getPrimaryFile();
            ClassPath cp = ClassPath.getClassPath(fo, ClassPath.SOURCE);
            assert cp != null : "SOURCE classpath was not found for " + fo;
            if (cp == null) {
                return null;
            }
            String pkg = cp.getResourceName(fo, '/', false);
            String dotPkg = pkg.replace('/', '.');
            String fullSuiteName = (suiteName != null)
                                   ? pkg + '/' + suiteName
                                   : TestUtil.convertPackage2SuiteName(pkg);
            
            try {
                // find the suite class,
                // if it exists or create one from active template
                DataObject doTarget = getTestClass(testClassPath,
                                                   fullSuiteName,
                                                   doSuiteT);
                
                // generate the test suite for all listed test classes
                Collection targetClasses = TestUtil.getAllClassesFromFile(
                                                   doTarget.getPrimaryFile());
                
                Iterator tcit = targetClasses.iterator();
                while (tcit.hasNext()) {
                    JavaClass targetClass = (JavaClass)tcit.next();
                    
                    if (progress != null) {
                        progress.setMessage(
                                getCreatingMsg(targetClass.getName()), false);
                    }
                    
                    try {
                        TestCreator.createTestSuite(suite, dotPkg, targetClass);
                        save(doTarget);
                    } catch (Exception e) {
                        ErrorManager.getDefault().log(ErrorManager.ERROR,
                                                      e.toString());
                        return null;
                    }
                    
                    // add the suite class to the list of members of the parent
                    if (null != parentSuite) {
                        parentSuite.add(targetClass.getName());
                    }
                }
                return doTarget;
            } catch (IOException ioe) {
                throw new CreationError(ioe);
            }
        }
        
        private CreationResults createTests(
                    Node node,
                    ClassPath testClassPath,
                    DataObject doTestT,
                    DataObject doSuiteT,
                    LinkedList parentSuite,
                    ProgressIndicator progress) throws CreationError {
            
            FileObject foSource = TestUtil.getFileObjectFromNode(node);
            if (foSource.isFolder()) {
                // create test for all direct subnodes of the folder
                Node  childs[] = node.getChildren().getNodes(true);
                CreationResults results = new CreationResults();
                
                LinkedList  mySuite = new LinkedList(); // List<String>
                progress.setMessage(getScanningMsg(foSource.getName()), false);
                
                for (int ch = 0; ch < childs.length;ch++) {
                    
                    if (progress.isCanceled()) {
                        results.setAbborted();
                        break;
                    }
                    
                    results.combine(createTests(childs[ch],
                                                testClassPath,
                                                doTestT,
                                                doSuiteT,
                                                mySuite,
                                                progress));
                    if (results.isAbborted()) {
                        break;
                    }
                }
                
                // if everything went ok, and the option is enabled,
                // create a suite for the folder .
                if (!results.isAbborted()
                        && ((0 < mySuite.size())
                            & (JUnitSettings.getDefault()
                               .isGenerateSuiteClasses()))) {
                    createSuiteTest(testClassPath,
                                    DataFolder.findFolder(foSource),
                                    (String) null,
                                    mySuite,
                                    doSuiteT,
                                    parentSuite,
                                    progress);
                }
                
                return results;
            } else {
                // is not folder, create test for the fileObject of the node
                if (foSource.isData()
                        && !("java".equals(foSource.getExt()))) {       //NOI18N
                    return CreationResults.EMPTY;
                } else {
                    return createSingleTest(testClassPath,
                                            foSource,
                                            null,      //use the default clsname
                                            doTestT,
                                            parentSuite,
                                            progress,
                                            true);
                }
            }
        }
        
        public static CreationResults createSingleTest(
                ClassPath testClassPath,
                FileObject foSource,
                String testClassName,
                DataObject doTestT,
                LinkedList parentSuite,
                ProgressIndicator progress,
                boolean skipNonTestable) throws CreationError {
            
            // create tests for all classes in the source
            Resource srcRc = JavaModel.getResource(foSource);
            String packageName = (testClassName == null)
                                 ? srcRc.getPackageName()
                                 : null;            //will be built if necessary
            List srcChildren = srcRc.getChildren();
            CreationResults result = new CreationResults(srcChildren.size());

            /* used only if (testClassName != null): */
            boolean defClassProcessed = false;

            Iterator scit = srcChildren.iterator();
            while (scit.hasNext()) {
                Element el = (Element)scit.next();
                if (!(el instanceof JavaClass)) {
                    continue;
                }
                
                JavaClass theClass = (JavaClass)el;
                
                if (skipNonTestable && !TestCreator.isClassTestable(theClass)) {
                    if (progress != null) {
                        // ignoring because untestable
                        progress.setMessage(
                                getIgnoringMsg(theClass.getName()), false);
                        result.addSkipped(theClass);
                    }
                    continue;
                }
                
                // find the test class, if it exists or create one
                // from active template
                try {
                    String testResourceName;
                    String srcClassNameShort = theClass.getSimpleName();
                    if (testClassName == null) {
                        testResourceName = TestUtil.getTestClassFullName(
                                srcClassNameShort,
                                packageName);
                    } else if (!defClassProcessed
                              && srcClassNameShort.equals(foSource.getName())) {
                        /* main Java class: */
                        testResourceName = testClassName.replace('.', '/');
                        defClassProcessed = true;
                    } else {
                        if (packageName == null) {
                            packageName = packageName(testClassName);
                        }
                        testResourceName = TestUtil.getTestClassFullName(
                                srcClassNameShort,
                                packageName);
                    }
                    
                    /* find or create the test class DataObject: */
                    DataObject doTarget = getTestClass(testClassPath,
                                                       testResourceName,
                                                       doTestT);

                    // generate the test of current node
                    Resource tgtRc = JavaModel.getResource(
                            doTarget.getPrimaryFile());
                    JavaClass targetClass = TestUtil.getMainJavaClass(tgtRc);

                    if (targetClass != null) {
                        if (progress != null) {
                            progress.setMessage(
                                  getCreatingMsg(targetClass.getName()), false);
                        }

                        TestCreator.createTestClass(srcRc,
                                                    theClass,
                                                    tgtRc,
                                                    targetClass);
                        save(doTarget);
                        result.addCreated(doTarget);
                        // add the test class to the parent's suite
                        if (null != parentSuite) {
                            parentSuite.add(targetClass.getName());
                        }
                    }
                } catch (IOException ioe) {
                    throw new CreationError(ioe);
                }
            }
            
            return result;
            
        }
        
        private static String packageName(String fullName) {
            int i = fullName.lastIndexOf('.');
            return fullName.substring(0, i > 0 ? i : 0);
        }
        
        private static DataObject getTestClass(
                ClassPath cp,
                String testClassName,
                DataObject doTemplate) throws DataObjectNotFoundException,
                                              IOException {
            FileObject fo = cp.findResource(testClassName + ".java");   //NOI18N
            if (fo != null) {
                return DataObject.find(fo);
            } else {
                // test file does not exist yet so create it:
                assert cp.getRoots().length == 1;
                FileObject root = cp.getRoots()[0];
                int index = testClassName.lastIndexOf('/');
                String pkg = index > -1 ? testClassName.substring(0, index)
                                        : "";                           //NOI18N
                String clazz = index > -1 ? testClassName.substring(index+1)
                                          : testClassName;
                
                // create package if it does not exist
                if (pkg.length() > 0) {
                    root = FileUtil.createFolder(root, pkg);
                }
                // instantiate template into the package
                return doTemplate.createFromTemplate(
                        DataFolder.findFolder(root),
                        clazz);
            }
        }
        
        private boolean hasParentAmongNodes(Node[] nodes, int idx) {
            Node node;
            
            node = nodes[idx].getParentNode();
            while (null != node) {
                for (int i = 0; i < nodes.length; i++) {
                    if (i == idx) {
                        continue;
                    }
                    if (node == nodes[i]) {
                        return true;
                    }
                }
                node = node.getParentNode();
            }
            return false;
        }
        
        private static void save(DataObject dO) throws IOException {
            SaveCookie sc = (SaveCookie) dO.getCookie(SaveCookie.class);
            if (null != sc)
                sc.save();
        }
        
        private static String getCreatingMsg(String className) {
            String fmt = NbBundle.getMessage(
                    CreateTestAction.class,
                    "FMT_generator_status_creating");                   //NOI18N
            return MessageFormat.format(fmt, new Object[] { className });
        }
        
        private static String getScanningMsg(String sourceName) {
            String fmt = NbBundle.getMessage(
                    CreateTestAction.class,
                    "FMT_generator_status_scanning");                   //NOI18N
            return MessageFormat.format(fmt, new Object[] { sourceName });
        }
        
        private static String getIgnoringMsg(String sourceName) {
            String fmt = NbBundle.getMessage(
                    CreateTestAction.class,
                    "FMT_generator_status_ignoring");                   //NOI18N
            return MessageFormat.format(fmt, new Object[] { sourceName });
        }
        
        
        
        /**
         * Error thrown by failed test creation.
         */
        public static final class CreationError extends Exception {
            public CreationError() {};
            public CreationError(Throwable cause) {
                super(cause);
            }
        };
        
        /**
         * Utility class representing the results of a test creation
         * process. It gatheres all tests (as DataObject) created and all
         * classes (as JavaClasses) for which no test was created.
         */
        public static class CreationResults {
            public static final CreationResults EMPTY = new CreationResults();
            
            Set created; // Set< createdTest : DataObject >
            Set skipped; // Set< sourceClass : JavaClass >
            boolean abborted = false;
            
            public CreationResults() { this(20);}
            
            public CreationResults(int expectedSize) {
                created = new HashSet(expectedSize * 2 , 0.5f);
                skipped = new HashSet(expectedSize * 2 , 0.5f);
            }
            
            public void setAbborted() {
                abborted = true;
            }
            
            /**
             * Returns true if the process of creation was abborted. The
             * result contains the results gathered so far.
             */
            public boolean isAbborted() {
                return abborted;
            }
            
            
            /**
             * Adds a new entry to the set of created tests.
             * @return true if it was added, false if it was present before
             */
            public boolean addCreated(DataObject test) {
                return created.add(test);
            }
            
            /**
             * Adds a new <code>JavaClass</code> to the collection of
             * skipped classes.
             * @return true if it was added, false if it was present before
             */
            public boolean addSkipped(JavaClass c) {
                return skipped.add(c);
            }
            
            /**
             * Returns a set of classes that were skipped in the process.
             * @return Set<JavaClass>
             */
            public Set getSkipped() {
                return skipped;
            }
            
            /**
             * Returns a set of test data objects created.
             * @return Set<DataObject>
             */
            public Set getCreated() {
                return created;
            }
            
            /**
             * Combines two results into one. If any of the results is an
             * abborted result, the combination is also abborted. The
             * collections of created and skipped classes are unified.
             * @param rhs the other CreationResult to combine into this
             */
            public void combine(CreationResults rhs) {
                if (rhs.abborted) {
                    this.abborted = true;
                }
                
                this.created.addAll(rhs.created);
                this.skipped.addAll(rhs.skipped);
            }
            
        }
        
    }
