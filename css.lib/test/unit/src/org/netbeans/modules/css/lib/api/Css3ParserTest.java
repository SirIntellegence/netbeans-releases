/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.css.lib.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import org.netbeans.modules.csl.api.test.CslTestBase;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.api.UserTask;
import org.netbeans.modules.parsing.spi.ParseException;
import org.openide.filesystems.FileObject;

/**
 *
 * @author marekfukala
 */
public class Css3ParserTest extends CslTestBase {

    private static final String bodysetPath = "styleSheet/bodylist/bodyset/";
    
    public Css3ParserTest(String testName) {
        super(testName);
    }
    
//     public static Test suite() throws IOException, BadLocationException {
//        System.err.println("Beware, only selected tests runs!!!");
//        TestSuite suite = new TestSuite();
//        suite.addTest(new Css3ParserTest("testErrors"));
//        return suite;
//    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CssParserResult.IN_UNIT_TESTS = true;
    }
    
    public void testErrorRecoveryInRule() throws ParseException, BadLocationException {
        //resync the parser to the last right curly bracket
        String code = "myns|h1  color: red; } h2 { color: blue; }";
        
        CssParserResult res = parse(code);
//        dumpResult(res);
        
        //this case recovers badly so far - the myns|h1 and h2 are joined into a single ruleset
    }

    public void testErrorRecoveryInsideDeclaration() throws ParseException, BadLocationException {
        //recovery inside declaration rule, resyncing to next semicolon or right curly brace
        String code = "a {\n"
                        + " s  red; \n"
                        + " background: red; \n"
                      + "}";
        
        CssParserResult res = parse(code);
        //dumpResult(res);
        
        //the background: red; declaration is properly parsed even if the previous declaration is broken
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + "ruleSet/declarations/declaration|1/property/background"));
        
//        dumpResult(res);
    }
    
    public void testErrorRecoveryGargabeBeforeDeclaration() throws ParseException, BadLocationException {
        //recovery before entering declaration rule, the Parser.syncToIdent() is used to skip until ident is found
        String code = "a {\n"
                        + " @ color: red; \n"
                        + " background: red; \n"
                      + "}";
        
        CssParserResult res = parse(code);
        //dumpResult(res);
        
        //the garbage char @ is skipped by Parser.syncToIdent()
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + "ruleSet/declarations/declaration|0/property/color"));
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + "ruleSet/declarations/declaration|1/property/background"));
        
    }
    
    public void testValidCode() throws ParseException, BadLocationException {
        String code = "a {\n"
                        + "color : black; \n"
                        + "background: red; \n"
                      + "}\n\n"
                +      ".class { }\n"
                +      "#id { }";
        
        CssParserResult res = parse(code);
//        dumpResult(res);
        
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + "ruleSet/declarations/declaration|0/property/color"));
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + "ruleSet/declarations/declaration|1/property/background"));
    }
   
    public void testNamespacesInSelector() throws ParseException, BadLocationException {
        CssParserResult res = assertResultOK(parse("myns|h1 { color: red; }"));
        //dumpResult(res);
        
        String typeSelectorPath = "ruleSet/selectorsGroup/selector/simpleSelectorSequence/typeSelector/";
        
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + typeSelectorPath + "namespacePrefix/myns"));
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + typeSelectorPath + "elementName/h1"));        
        
        res = assertResultOK(parse("*|h1 { color: red; }"));
        //dumpResult(res);
        
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + typeSelectorPath + "namespacePrefix/*"));
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + typeSelectorPath + "elementName/h1"));
        
        res = assertResultOK(parse("*|* { color: red; }"));
        //dumpResult(res);
        
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + typeSelectorPath + "namespacePrefix/*"));
        assertNotNull(NodeUtil.query(res.getParseTree(), 
                bodysetPath + typeSelectorPath + "elementName/*"));
    }
        
    public void testNetbeans_Css() throws ParseException, BadLocationException, IOException {
        assertResult(parse(getTestFile("testfiles/netbeans.css")), 2);
    }

    private CssParserResult assertResultOK(CssParserResult result) {
        return assertResult(result, 0);
    }
    
    private CssParserResult assertResult(CssParserResult result, int problems) {
        assertNotNull(result);
        assertNotNull(result.getParseTree());
        assertEquals(problems, result.getDiagnostics().size());
        
        return result;
    }
    
    private CssParserResult parse(String code) throws ParseException, BadLocationException {
        Document doc = new PlainDocument();
        doc.putProperty("mimeType", "text/x-css");
        doc.insertString(0, code, null);
        Source source = Source.create(doc);
        return parse(source);
    }
    
    private CssParserResult parse(FileObject file) throws ParseException, BadLocationException, IOException {
        //no loader here so we need to create the swing document
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), "UTF-8"));
        StringBuilder builder = new StringBuilder();
        
        char[] buffer = new char[8096];
        int read = 0;
        while((read = reader.read(buffer)) > 0) {
            builder.append(buffer, 0, read);
        }
        reader.close();
        return parse(builder.toString());
    }
   
    private CssParserResult parse(Source source) throws ParseException {
        final AtomicReference<CssParserResult> resultRef = new AtomicReference<CssParserResult>();
        ParserManager.parse(Collections.singleton(source), new UserTask() {

            @Override
            public void run(ResultIterator resultIterator) throws Exception {
                CssParserResult result = (CssParserResult) resultIterator.getParserResult();
                resultRef.set(result);
            }
        });
        
        return resultRef.get();
    }
    
    private void dumpResult(CssParserResult result) {
        NodeUtil.dumpTree(result.getParseTree());
        Collection<ProblemDescription> problems = result.getDiagnostics();
        if(!problems.isEmpty()) {
            System.out.println(String.format("Found %s problems while parsing:", problems.size()));
            for(ProblemDescription pp : problems) {
                System.out.println(pp);
            }
        }
        
    }
    
}
