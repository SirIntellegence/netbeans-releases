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
package org.netbeans.modules.java.hints;

import com.sun.source.util.TreePath;
import java.util.List;
import java.util.Locale;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.modules.java.hints.infrastructure.TreeRuleTestBase;
import org.netbeans.spi.editor.hints.ErrorDescription;

/**
 *
 * @author Jaroslav Tulach
 */
public class ExportNonAccessibleElementTest extends TreeRuleTestBase {
    
    public ExportNonAccessibleElementTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        Locale.setDefault(Locale.US);
        SourceUtilsTestUtil.setLookup(new Object[0], getClass().getClassLoader());
    }
    
    
    
    public void testMethodReturningNonPublicInnerClass() throws Exception {
        String before = "package test; public class Test extends Object {" +
            " public Inner ";
        String after = " ret() {" +
            "  return null;" +
            " }\n" +
            " private class Inner { }" +
            "}";
        
        performAnalysisTest("test/Test.java", before + after, before.length(), 
            "0:63-0:66:verifier:Exporting non-public type through public API"
        );
    }

    public void testNonPublicClass() throws Exception {
        String before = "package test; class Te";
        String after = "st extends Inner {" +
            " private class Inner { }" +
            "}";
        
        performAnalysisTest("test/Test.java", before + after, before.length());
    }

    public void testMethodTakingNonPublicInnerClassNotInMethod() throws Exception {
        String before = "package test; public class Test extends Object {" +
            " public void";
        String after = " ret(Inner in) {" +
            " }\n" +
            " private class Inner { }" +
            "}";
        
        performAnalysisTest("test/Test.java", before + after, before.length());
    }
    public void testMethodTakingNonPublicInnerClass() throws Exception {
        String before = "package test; public class Test extends Object {" +
            " public void ret(Inner in)";
        String after = " {" +
            " }" +
            " private class Inner { }" +
            "}";
        
        String res = (before + after).replace("public void", "void");
        performFixTest("test/Test.java", before + after, before.length(), 
            "0:61-0:64:verifier:Exporting non-public type through public API",
            "FixExportNonAccessibleElement",
            res);
    }
    public void testNoProblemInNotPublicClasses() throws Exception {
        String before = "package test; class Test extends Object {" +
            " public void";
        String after = " ret(Inner in) {" +
            " }\n" +
            " private class Inner { }" +
            "}";
        
        performAnalysisTest("test/Test.java", before + after, before.length());
    }
    public void testOnFieldsInPrivateClass() throws Exception {
        String before = "package test; class Test extends Object {" +
            " public Inner ";
        String after = "in;" +
            " private class Inner { }" +
            "}";
        
        String res = (before + after).replace("public ", "");
        performAnalysisTest("test/Test.java", before + after, before.length());
    }
    public void testOnFieldsAsWell() throws Exception {
        String first = "package test; public class Test extends ";
        String before = "Object {" +
            " public Inner ";
        String after = "in;" +
            " private class Inner { }" +
            "}";
        
        String res = first + (before + after).replace("public ", "");
        performFixTest("test/Test.java", first + before + after, (first + before).length(), 
            "0:62-0:64:verifier:Exporting non-public type through public API",
            "FixExportNonAccessibleElement",
            res);
    }
    public void testNonPublicSuperClass() throws Exception {
        String before = "package test; " +
            "class TestBase {} " +
            "public class Test e"; String after = "xtends TestBase {" +
            "}";
        
            
        String res = (before + after).replace("public ", "");
        performFixTest("test/Test.java", before + after, before.length(), 
            "0:45-0:49:verifier:Exporting non-public type through public API", 
            "FixExportNonAccessibleElement",
            res);
    }
    public void testWhyEachMethodIsInaccessible() throws Exception {
        String before = "package test; " +
            "public class Test {" +
            "    public void testNonPublicSuperClass()";
        String after = " throws Exception { " +
            "\n}" +
            "";
        performAnalysisTest("test/Test.java", before + after, before.length());       
    }

    public void testWhyStaticFieldIsNotIdentified() throws Exception {
        String  before = "package proxy.test.impl; " +
        "public class IfaceFactory {" +
            "public static I2 xfjd";
        String after = "ksla;" +
        "}" +
        "interface I2 {" +
        "}";
        
        performAnalysisTest("test/Test.java", before + after, before.length(),
            "0:69-0:77:verifier:Exporting non-public type through public API");       
        
    }
    public void testWildCards() throws Exception {
        String  before = "package proxy.test.impl; " +
        "public class IfaceFactory {" +
            "public static java.util.Collection<? extends I2> xfjd";
        String after = "ksla;" +
        "}" +
        "interface I2 {" +
        "}";
        
        performAnalysisTest("test/Test.java", before + after, before.length(),
            "0:101-0:109:verifier:Exporting non-public type through public API");       
        
    }
    public void testWildCardsSuper() throws Exception {
        String  before = "package proxy.test.impl; " +
        "public class IfaceFactory {" +
            "public static java.util.Collection<? super I2> xfjd";
        String after = "ksla;" +
        "}" +
        "interface I2 {" +
        "}";
        
        performAnalysisTest("test/Test.java", before + after, before.length(),
            "0:99-0:107:verifier:Exporting non-public type through public API");       
        
    }
    public void testWildCardsIssue108829() throws Exception {
        String  before = "package proxy.test.impl; import java.util.Comparable;" +
        "public class IfaceFactory {" +
            "public void func";
        String after = "tion (Comparable<?> c) { }" +
        "}";
        
        performAnalysisTest("test/Test.java", before + after, before.length());       
    }
    
    public void testElementHandle() throws Exception {
        String before = "public final class ElementHa";
        String after = "ndle<T extends Element> { }";
        
        performAnalysisTest("test/Test.java", before + after, before.length());       
    }

    public void testNonVisibleMethod() throws Exception {
        String before = "package test; " +
            "public class Test {" +
            "    private C testNonPublicSuperClass()";
        String after = " throws Exception { " +
            "\n}" +
            "class C { }";
        performAnalysisTest("test/Test.java", before + after, before.length());       
    }
    
    public void testVeryBroken() throws Exception {
        String before = "package test; public class Test extends Object {" +
            " public Inner<String> ";
        String after = " () {" +
            "  return null;" +
            " }\n" +
            " private class Inner<T> { }" +
            "}";
        
        performAnalysisTest("test/Test.java", before + after, before.length());
    }
    
    protected List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path) {
        SourceUtilsTestUtil.setSourceLevel(info.getFileObject(), sourceLevel);
        return new ExportNonAccessibleElement().run(info, path);
    }
    
    private String sourceLevel = "1.5";
    
}
