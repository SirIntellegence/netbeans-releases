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
package org.netbeans.modules.web.beans.analysis.analyzer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.netbeans.api.java.source.CompilationInfo;


/**
 * @author ads
 *
 */
public final class AnnotationUtil {
    
    public static final String VALUE = "value";                                     // NOI18N
    
    public static final String INJECT = "Inject";                                   // NOI18N
    
    public static final String INJECT_FQN = "javax.inject."+INJECT;                 // NOI18N

    public static final String DECORATOR = "javax.decorator.Decorator";             // NOI18N
    
    public static final String PRODUCES = "Produces";
    
    public static final String PRODUCES_FQN = "javax.enterprise.inject."+           // NOI18N
        PRODUCES;     
    
    public static final String INTERCEPTOR_BINDING = "InterceptorBinding";          // NOI18N
    
    public static final String INTERCEPTOR_BINDING_FQN 
                = "javax.interceptor."+INTERCEPTOR_BINDING;                         // NOI18N
    
    public static final String INTERCEPTOR = "javax.interceptor.Interceptor";       // NOI18N
    
    public static final String NORMAL_SCOPE = "NormalScope";                        // NOI18N
    
    public static final String NORMAL_SCOPE_FQN 
                                          = "javax.enterprise.context."+NORMAL_SCOPE;// NOI18N
    
    public static String SCOPE =    "Scope";                                        // NOI18N
    
    public static String SCOPE_FQN = "javax.inject."+SCOPE;                         // NOI18N
    
    public static final String DISPOSES = "Disposes";                               // NOI18N
    
    public static final String DISPOSES_FQN  = "javax.enterprise.inject."+          // NOI18N
                                        DISPOSES;
    
    public static final String OBSERVES = "Observes";                               // NOI18N
    
    public static final String OBSERVES_FQN = "javax.enterprise.event."+            // NOI18N
                                            OBSERVES;        
    
    public static final String STATELESS = "javax.ejb.Stateless";                   // NOI18N

    public static final String STATEFUL = "javax.ejb.Stateful";                     // NOI18N 
    
    public static final String  SINGLETON   = "javax.ejb.Singleton";                // NOI18N
    
    public static final String APPLICATION_SCOPED 
                    = "javax.enterprise.context.ApplicationScoped";                 // NOI18N
    
    public static final String DEPENDENT 
                            = "javax.enterprise.context.Dependent";                 // NOI18N
    
    public static final String STEREOTYPE = "Stereotype";                           // NOI18N
    
    public static final String STEREOTYPE_FQN = 
                    "javax.enterprise.inject."+STEREOTYPE;                          // NOI18N
    
    public static final String NAMED = "javax.inject.Named";                        // NOI18N
    
    public static final String QUALIFIER = "Qualifier";                             // NOI18N
    
    public static final String QUALIFIER_FQN=
                            "javax.inject."+QUALIFIER;                              // NOI18N
    
    public static final String DELEGATE_FQN =
                                        "javax.decorator.Delegate";                 // NOI18N
    
    public static final String SPECIALIZES = "javax.enterprise.inject.Specializes"; // NOI18N
    
    public static final String INJECTION_POINT = 
                            "javax.enterprise.inject.spi.InjectionPoint";           // NOI18N
    
    public static final String DEFAULT_FQN = "javax.enterprise.inject.Default";     // NOI18N
    
    private AnnotationUtil(){
    }
    
    public static boolean hasAnnotation(Element element, String annotation,
            CompilationInfo info )
    {
        return getAnnotationMirror(element, annotation, info)!=null;
    }

    public static AnnotationMirror getAnnotationMirror(Element element, 
            String annotation,CompilationInfo info )
    {
        return getAnnotationMirror(element, info, annotation); 
    }
    
    public static AnnotationMirror getAnnotationMirror(Element element, 
            CompilationInfo info , String... annotationFqns)
    {
        Set<TypeElement> set = new HashSet<TypeElement>();
        for( String annotation : annotationFqns){
            TypeElement annotationElement = info.getElements().getTypeElement(
                    annotation);
            if ( annotationElement != null ){
                set.add( annotationElement );
            }
        }
        
        List<? extends AnnotationMirror> annotations = 
            info.getElements().getAllAnnotationMirrors( element );
        for (AnnotationMirror annotationMirror : annotations) {
            Element declaredAnnotation = info.getTypes().asElement( 
                    annotationMirror.getAnnotationType());
            if ( set.contains( declaredAnnotation ) ){
                return annotationMirror;
            }
        }
        return null;
    }
    
    public static boolean isSessionBean(TypeElement element , 
            CompilationInfo compInfo )
    {
        return getAnnotationMirror(element, compInfo, STATEFUL, STATELESS, 
                SINGLETON)!= null;
    }
    
}
