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
package org.netbeans.modules.java.hints.infrastructure;

import com.sun.source.util.TreePath;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.swing.text.Document;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.api.lexer.Language;
import org.netbeans.junit.NbTestCase;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.LifecycleManager;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/**
 * @author Jan Lahoda
 */
public abstract class TreeRuleTestBase extends NbTestCase {
    
    public TreeRuleTestBase(String name) {
        super(name);
        
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SourceUtilsTestUtil.prepareTest(new String[] {"org/netbeans/modules/java/editor/resources/layer.xml"}, new Object[0]);
    }
    
    private void prepareTest(String fileName, String code) throws Exception {
        clearWorkDir();
        
        FileObject workFO = FileUtil.toFileObject(getWorkDir());
        
        assertNotNull(workFO);
        
        sourceRoot = workFO.createFolder("src");
        FileObject buildRoot  = workFO.createFolder("build");
        FileObject cache = workFO.createFolder("cache");
        
        FileObject data = FileUtil.createData(sourceRoot, fileName);
        File dataFile = FileUtil.toFile(data);
        
        assertNotNull(dataFile);
        
        TestUtilities.copyStringToFile(dataFile, code);
        
        SourceUtilsTestUtil.prepareTest(sourceRoot, buildRoot, cache, extraClassPath());
        
        DataObject od = DataObject.find(data);
        EditorCookie ec = od.getCookie(EditorCookie.class);
        
        assertNotNull(ec);
        
        doc = ec.openDocument();
        doc.putProperty(Language.class, JavaTokenId.language());
        
        JavaSource js = JavaSource.forFileObject(data);
        
        assertNotNull(js);
        
        info = SourceUtilsTestUtil.getCompilationInfo(js, Phase.RESOLVED);
        
        assertNotNull(info);
    }
    
    private FileObject sourceRoot;
    private CompilationInfo info;
    private Document doc;
    
    protected abstract List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path);
    
    protected String toDebugString(CompilationInfo info, Fix f) {
        return f.toString();
    }
    
    protected void performAnalysisTest(String fileName, String code, int pos, String... golden) throws Exception {
        prepareTest(fileName, code);
        
        TreePath path = info.getTreeUtilities().pathFor(pos);
        
        List<ErrorDescription> errors = computeErrors(info, path);
        List<String> errorsNames = new LinkedList<String>();
        
        errors = errors != null ? errors : Collections.<ErrorDescription>emptyList();
        
        for (ErrorDescription e : errors) {
            errorsNames.add(e.toString());
        }
        
        assertTrue(errorsNames.toString(), Arrays.equals(golden, errorsNames.toArray(new String[0])));
    }
    
    protected void performFixTest(String fileName, String code, int pos, String errorDescriptionToString, String fixDebugString, String golden) throws Exception {
        performFixTest(fileName, code, pos, errorDescriptionToString, fixDebugString, fileName, golden);
    }
    
    protected void performFixTest(String fileName, String code, int pos, String errorDescriptionToString, String fixDebugString, String goldenFileName, String golden) throws Exception {
        prepareTest(fileName, code);
        
        TreePath path = info.getTreeUtilities().pathFor(pos);
        
        List<ErrorDescription> errors = computeErrors(info, path);
        
        ErrorDescription toFix = null;
        
        for (ErrorDescription d : errors) {
            if (errorDescriptionToString.equals(d.toString())) {
                toFix = d;
                break;
            }
        }
        
        assertNotNull("Error: \"" + errorDescriptionToString + "\" not found.", toFix);
        
        assertTrue("Must be computed", toFix.getFixes().isComputed());
        
        List<Fix> fixes = toFix.getFixes().getFixes();
        Fix toApply = null;
        
        for (Fix f : fixes) {
            if (fixDebugString.equals(toDebugString(info, f))) {
                toApply = f;
            }
        }
        
        toApply.implement();
        
        FileObject toCheck = sourceRoot.getFileObject(goldenFileName);
        
        assertNotNull(toCheck);
        
        DataObject toCheckDO = DataObject.find(toCheck);
        EditorCookie ec = toCheckDO.getLookup().lookup(EditorCookie.class);
        Document toCheckDocument = ec.openDocument();
        
        String realCode = toCheckDocument.getText(0, toCheckDocument.getLength());
        
        //ignore whitespaces:
        realCode = realCode.replaceAll("[ \t\n]+", " ");
        
        assertEquals(golden, realCode);
        
        LifecycleManager.getDefault().saveAll();
    }
    
    protected FileObject[] extraClassPath() {
        return new FileObject[0];
    }
    
}
