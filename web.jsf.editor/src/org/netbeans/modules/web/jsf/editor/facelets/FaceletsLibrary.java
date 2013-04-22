/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.

Oracle and Java are registered trademarks of Oracle and/or its affiliates.
Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.netbeans.modules.web.jsf.editor.facelets;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.netbeans.modules.web.jsfapi.api.DefaultLibraryInfo;
import org.netbeans.modules.web.jsfapi.api.LibraryType;
import org.netbeans.modules.web.jsfapi.api.NamespaceUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;

/**
 * Represents a facelets library defined by the facelets library VDL descriptor (.taglib.xml) file.
 * The descriptor must declare the library namespace.
 * 
 * The library may contain both class or composite components
 * 
 * @author marekfukala
 */
public class FaceletsLibrary extends AbstractFaceletsLibrary {

    /** 
     * The namespace declared in the facelets library descriptor
     */
    private final String declaredNamespace;
    
    private final Map<String, NamedComponent> components = new HashMap<String, NamedComponent>();
    private LibraryDescriptor libraryDescriptor, faceletsLibraryDescriptor;
    private String defaultPrefix;
    private final URL libraryDescriptorSource;

    public FaceletsLibrary(FaceletsLibrarySupport support, String namespace, URL libraryDescriptorSourceURL) {
        super(support, namespace);
        declaredNamespace = namespace;
        libraryDescriptorSource = libraryDescriptorSourceURL;
    }
    
    protected synchronized LibraryDescriptor getFaceletsLibraryDescriptor() throws LibraryDescriptorException {
        if(faceletsLibraryDescriptor == null) {
            FileObject libraryDescriptorSourceFile = URLMapper.findFileObject(libraryDescriptorSource);
            faceletsLibraryDescriptor = FaceletsLibraryDescriptor.create(libraryDescriptorSourceFile);
        }
        return faceletsLibraryDescriptor;
    }
    
    @Override
    public Map<String, ? extends NamedComponent> getComponentsMap() {
        return components;
    }

    @Override
    public String getNamespace() {
        return declaredNamespace;
    }

    @Override
    public URL getLibraryDescriptorSource() {
        return libraryDescriptorSource;
    }

    @Override
    public LibraryType getType() {
        return LibraryType.CLASS;
    }

    @Override
    public String getDefaultNamespace() {
        return null;
    }

    @Override
    public synchronized String getDefaultPrefix() {
        if(defaultPrefix == null) {
            try {
                //first try to get the prefix from the facelets library descriptor
                defaultPrefix = getFaceletsLibraryDescriptor().getPrefix();
            } catch (LibraryDescriptorException ex) {
                Exceptions.printStackTrace(ex);
            }
            
            if(defaultPrefix == null) {
                //no prefix defined in the library descriptor
                //if standard library, we have hardcododed prefixes
                defaultPrefix = super.getDefaultPrefix();
            }
            
            if(defaultPrefix == null) {
                //non standard library will use a prefix generated from the library namespace
                defaultPrefix = generateDefaultPrefix();
                
            }
        }
        return defaultPrefix;
    }
    

    @Override
    public synchronized LibraryDescriptor getLibraryDescriptor() {
        if(libraryDescriptor == null) {
            try {
                //create a merged library descriptor from facelets VDL descriptor and the JSP taglib descriptor
                //the reason for this is that often the facelets descriptor doesn't declare the components metadata but the
                //jsp tag library descriptor does
                libraryDescriptor = new TldProxyLibraryDescriptor(getFaceletsLibraryDescriptor(), support.getJsfSupport().getIndex());
            } catch (LibraryDescriptorException ex) {
                //error in parsing the descriptors
                Exceptions.printStackTrace(ex);
            }
        }
        return libraryDescriptor;
    }

    public void putConverter(String name, String id) {
        components.put(name, new Converter(name, id, null));
    }

    public void putConverter(String name, String id, Class handlerClass) {
        components.put(name, new Converter(name, id, handlerClass));
    }

    public void putValidator(String name, String id) {
        components.put(name, new Validator(name, id, null));
    }

    public void putValidator(String name, String id, Class handlerClass) {
        components.put(name, new Validator(name, id, handlerClass));
    }

    public void putBehavior(String name, String id) {
        components.put(name, new Behavior(name, id, null));
    }

    public void putBehavior(String name, String id, Class handlerClass) {
        components.put(name, new Behavior(name, id, handlerClass));
    }

    public void putTagHandler(String name, Class type) {
        components.put(name, new TagHandler(name, type));
    }

    public void putComponent(String name, String componentType,
            String rendererType) {
        components.put(name, new Component(name, componentType, rendererType, null));
    }

    public void putComponent(String name, String componentType,
            String rendererType, Class handlerClass) {
        components.put(name, new Component(name, componentType, rendererType, handlerClass));
    }

    public void putUserTag(String name, URL source) {
        components.put(name, new UserTag(name, source));
    }

    public void putFunction(String name, Method method) {
        components.put(name, new Function(name, method));
    }

    public NamedComponent createNamedComponent(String name) {
        return new NamedComponent(name);
    }

    public Function createFunction(String name, Method method) {
        return new Function(name, method);
    }

    private String generateDefaultPrefix() {
        //generate a default prefix from the namespace
        String ns = getNamespace();
        final String HTTP_PREFIX = "http://"; //NOI18N
        if(ns.startsWith(HTTP_PREFIX)) {
            ns = ns.substring(HTTP_PREFIX.length());
        }
        StringTokenizer st = new StringTokenizer(ns, "/.");
        List<String> tokens = new LinkedList<String>();
        while(st.hasMoreTokens()) {
            String token = st.nextToken();
            if(token.length() > 0) {
                tokens.add(token);
            }
        }
        if(tokens.isEmpty()) {
            //shoult not happen for normal URLs
            return "lib"; //NOI18N
        }

        if(tokens.size() == 1) {
            return tokens.iterator().next();
        } else {
            StringBuilder buf = new StringBuilder();
            for(String token : tokens) {
                buf.append(token.charAt(0));
            }
            return buf.toString();
        }
    }

    @Override
    public String getLegacyNamespace() {
        return NamespaceUtils.NS_MAPPING.get(declaredNamespace);
    }


}
