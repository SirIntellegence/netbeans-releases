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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
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

package org.netbeans.modules.java.navigation;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.swing.SwingUtilities;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CodeStyle;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ui.ElementJavadoc;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 * This task is called every time the caret position changes in a Java editor.
 * <p>
 * The task finds the TreePath of the Tree under the caret, converts it to
 * an Element and then shows the declartion of the element in Declaration window
 * and javadoc in the Javadoc window.
 *
 * @author Sandip V. Chitale (Sandip.Chitale@Sun.Com)
 */
public class CaretListeningTask implements CancellableTask<CompilationInfo> {
    
    private CaretListeningFactory caretListeningFactory;
    private FileObject fileObject;
    private boolean canceled;
    
    private static ElementHandle<Element> lastEh;
    private static ElementHandle<Element> lastEhForNavigator;
    
    private static final Set<JavaTokenId> TOKENS_TO_SKIP = EnumSet.of(JavaTokenId.WHITESPACE, 
                                  JavaTokenId.BLOCK_COMMENT, 
                                  JavaTokenId.LINE_COMMENT, 
                                  JavaTokenId.JAVADOC_COMMENT);
    
    
    CaretListeningTask(CaretListeningFactory whichElementJavaSourceTaskFactory,FileObject fileObject) {
        this.caretListeningFactory = whichElementJavaSourceTaskFactory;
        this.fileObject = fileObject;
    }
    
    static void resetLastEH() {
        lastEh = null;
    }
    
    public void run(CompilationInfo compilationInfo) {
        // System.out.println("running " + fileObject);
        resume();
        
        boolean navigatorShouldUpdate = ClassMemberPanel.getInstance() != null; // XXX set by navigator visible
        boolean javadocShouldUpdate = JavadocTopComponent.shouldUpdate();
        boolean declarationShouldUpdate = DeclarationTopComponent.shouldUpdate();
        
        if ( isCancelled() || ( !navigatorShouldUpdate && !javadocShouldUpdate && !declarationShouldUpdate ) ) {
            return;
        }
                        
        int lastPosition = CaretListeningFactory.getLastPosition(fileObject);
        
        TokenHierarchy tokens = compilationInfo.getTokenHierarchy();
        TokenSequence ts = tokens.tokenSequence();
        boolean inJavadoc = false;
        int offset = ts.move(lastPosition);
        if (ts.moveNext() && ts.token() != null ) {
            
            Token token = ts.token();
            TokenId tid = token.id();
            if ( tid == JavaTokenId.JAVADOC_COMMENT ) {
                inJavadoc = true;                
            }
            
            if ( shouldGoBack( token.toString(), offset < 0 ? 0 : offset ) ) {
                if ( ts.movePrevious() ) {
                    token = ts.token();
                    tid = token.id();
                }
            }
            
            if ( TOKENS_TO_SKIP.contains(tid) ) {
                skipTokens(ts, TOKENS_TO_SKIP);                
            }
            lastPosition = ts.offset();
        }
                
        if (ts.token() != null && ts.token().length() > 1) {
            // it is magic for TreeUtilities.pathFor to proper tree
            ++lastPosition;
        }
                
        // Find the TreePath for the caret position
        TreePath tp =
                compilationInfo.getTreeUtilities().pathFor(lastPosition);        
        // if cancelled, return
        if (isCancelled()) {
            return;
        }
        
        // Update the navigator
        if ( navigatorShouldUpdate ) {
            updateNavigatorSelection(compilationInfo, tp); 
        }
        
        // Get Element
        Element element = compilationInfo.getTrees().getElement(tp);
                       
        // if cancelled or no element, return
        if (isCancelled() ) {
            return;
        }
    
        if ( element == null || inJavadoc ) {
            element = outerElement(compilationInfo, tp);
        }
        
        // if is canceled or no element
        if (isCancelled() || element == null) {            
            return;
        }
        
        // Don't update when element is the same
        if ( lastEh != null && lastEh.signatureEquals(element) && !inJavadoc ) {
            // System.out.println("  stoped because os same eh");
            return;
        }
        else {
            switch (element.getKind()) {
            case PACKAGE:
            case CLASS:
            case INTERFACE:
            case ENUM:
            case ANNOTATION_TYPE:
            case METHOD:
            case CONSTRUCTOR:
            case INSTANCE_INIT:
            case STATIC_INIT:
            case FIELD:
            case ENUM_CONSTANT:
                lastEh = ElementHandle.create(element);
                // Different element clear data
                setDeclaration(""); // NOI18N
                setJavadoc(null); // NOI18N
                break;
            case PARAMETER:
                element = element.getEnclosingElement(); // Take the enclosing method
                lastEh = ElementHandle.create(element);
                setDeclaration(""); // NOI18N
                setJavadoc(null); // NOI18N
                break;
            case LOCAL_VARIABLE:
                lastEh = null; // ElementHandle not supported 
                setDeclaration(Utils.format(compilationInfo, element)); // NOI18N
                setJavadoc(null); // NOI18N
                return;
            default:
                // clear
                setDeclaration(""); // NOI18N
                setJavadoc(null); // NOI18N
                return;
            }
        }
            
        
        // Compute and set javadoc
        if ( javadocShouldUpdate ) {
            // System.out.println("Updating JD");
            computeAndSetJavadoc(compilationInfo, element);
        }
        
        if ( isCancelled() ) {
            return;
        }
        
        // Compute and set declaration
        if ( declarationShouldUpdate ) {
            // System.out.println("Updating DECL");
            computeAndSetDeclaration(compilationInfo, element);
        }
        
    }
    
    private void setDeclaration(final String declaration) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                DeclarationTopComponent declarationTopComponent = DeclarationTopComponent.findInstance();
                if (declarationTopComponent != null && declarationTopComponent.isOpened()) {
                    declarationTopComponent.setDeclaration(declaration);
                }
            }
        });
    }
    
    private void setJavadoc(final ElementJavadoc javadoc) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JavadocTopComponent javadocTopComponent = JavadocTopComponent.findInstance();
                if (javadocTopComponent != null && javadocTopComponent.isOpened()) {
                    javadocTopComponent.setJavadoc(javadoc);
                }
            }
        });
    }
    
    /**
     * After this method is called the task if running should exit the run
     * method immediately.
     */
    public final synchronized void cancel() {
        canceled = true;
    }
    
    protected final synchronized boolean isCancelled() {
        return canceled;
    }
    
    protected final synchronized void resume() {
        canceled = false;
    }
    
   
    private void computeAndSetJavadoc(CompilationInfo compilationInfo, Element element) {
        
        if (isCancelled()) {
            return;
        }
        setJavadoc(ElementJavadoc.create(compilationInfo, element));
    }
    
    private void computeAndSetDeclaration(CompilationInfo compilationInfo, Element element ) {
            
        if ( element.getKind() == ElementKind.PACKAGE ) { 
            setDeclaration("package " + element.toString() + ";");
            return;
        }
            
        if ( isCancelled() ) {
            return;
        }
        
        final TreePath treePath = compilationInfo.getTrees().getPath(element);

        if ( isCancelled()) {
            return;
        }

        if ( treePath != null ) {
            try {
                final CompilationUnitTree cu = treePath.getCompilationUnit();
                final CharSequence text = cu.getSourceFile().getCharContent(true);
                final SourcePositions pos = compilationInfo.getTrees().getSourcePositions();
                long startPos = pos.getStartPosition(treePath.getCompilationUnit(), treePath.getLeaf());
                long endPos = pos.getEndPosition(treePath.getCompilationUnit(), treePath.getLeaf());
                if (startPos < 0 || endPos < 0) {
                    return;
                }
                long shift = cu.getLineMap().getColumnNumber(startPos) - 1;
                int tabSize = CodeStyle.getDefault(fileObject).getTabSize();
                final CharSequence declaration = text.subSequence((int)startPos, (int)endPos);
                setDeclaration(shift(declaration,(int)shift,tabSize));
            } catch(IOException ioe) {
                Exceptions.printStackTrace(ioe);
            }
        }
    }

    private String shift (final CharSequence content, int shift, int tabSize) {
        int j = 0;
        boolean trim = true;
        final StringBuilder result = new StringBuilder();
        for (int i=0; i< content.length(); i++) {
            char c = content.charAt(i);
            if (trim) {
                if (Character.isWhitespace(c)) {
                    j+= c == '\t' ? tabSize : 1;    //NOI18N
                    while (j>shift) {
                        result.append(' ');         //NOI18N
                        j--;
                    }
                    if (j == shift) {
                        trim = false;
                    }
                } else {
                    result.append(c);
                    trim = false;
                }
            } else {
                result.append(c);
                if (c == '\n') {        //NOI18N
                    trim = true;
                    j = 0;
                }
            }
        }
        return result.toString();
    }
    
    private void updateNavigatorSelection(CompilationInfo ci, TreePath tp) {
        
        // Try to find the declaration we are in
        Element e = outerElement(ci, tp);
                
        if ( e != null ) {
            final ElementHandle<Element> eh = ElementHandle.create(e);
            
            if ( lastEhForNavigator != null && eh.signatureEquals(lastEhForNavigator)) {
                return;
            }
            
            lastEhForNavigator = eh;
            
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    final ClassMemberPanel cmp = ClassMemberPanel.getInstance();
                    if (cmp != null) {
                        cmp.selectElement(eh);
                    }                    
                }                
            });
        }
        
    }
       
    private static Element outerElement( CompilationInfo ci, TreePath tp ) {
        
        Element e = null;
        
        while (tp != null) {
            
            switch( tp.getLeaf().getKind()) {
                case METHOD:
                case ANNOTATION_TYPE:
                case CLASS:
                case ENUM:
                case INTERFACE:
                case COMPILATION_UNIT:
                    e = ci.getTrees().getElement(tp);                    
                    break;
                case VARIABLE:
                    e = ci.getTrees().getElement(tp);
                    if (e != null && !e.getKind().isField()) {
                        e = null;
                    }
                    break;                
            }                        
            if ( e != null ) {
                break;
            }
            tp = tp.getParentPath();
        }
    
        return e;
    }
    
    
    private void skipTokens( TokenSequence ts, Set<JavaTokenId> typesToSkip ) {
                  
        while(ts.moveNext()) {
            if ( !typesToSkip.contains(ts.token().id()) ) {
                return;
            }
        }
        
        return;
    }
    
    private boolean shouldGoBack( String s, int offset ) {
        
        int nlBefore = 0;
        int nlAfter = 0;
        
        for( int i = 0; i < s.length(); i++ ) {
            if ( s.charAt(i) == '\n' ) { // NOI18N
                if ( i < offset ) {
                    nlBefore ++; 
                }
                else { 
                    nlAfter++; 
                }
                
                if ( nlAfter > nlBefore ) {
                    return true;
                }                
            }
        }
        
        if ( nlBefore < nlAfter ) {
            return false;
        }
        
        return offset < (s.length() - offset);
        
    }
    
    
    
}
