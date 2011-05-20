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
package org.netbeans.modules.j2ee.weblogic9.dd.model;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.j2ee.deployment.common.api.Version;
import org.netbeans.modules.schema2beans.NullEntityResolver;
import org.openide.util.NbBundle;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Petr Hejl
 */
public class MessageModel extends BaseDescriptorModel {

    private final WeblogicJms bean;
    
    public MessageModel(WeblogicJms bean) {
        super(bean);
        this.bean = bean;
    }
    
    public static MessageModel forFile(File file) throws IOException {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        try {
            return forInputStream(is);
        } finally {
            is.close();
        }
    }
    
    public static MessageModel forInputStream(InputStream is) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        
        Document doc;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver(NullEntityResolver.newInstance());
            doc = builder.parse(is);
        } catch (SAXException ex) {
            throw new RuntimeException(NbBundle.getMessage(EarApplicationModel.class, "MSG_CantCreateXMLDOMDocument"), ex);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(NbBundle.getMessage(EarApplicationModel.class, "MSG_CantCreateXMLDOMDocument"), ex);
        }        

        //String ns = doc.getDocumentElement().getNamespaceURI();
        return new MessageModel(org.netbeans.modules.j2ee.weblogic9.dd.jms1031.WeblogicJms.createGraph(doc));  
    }
    
    public static MessageModel generate(@NullAllowed Version serverVersion) {
        return generate1031();
    }

    public List<String> getQueues() {
        List<String> ret = new ArrayList<String>();
        for (QueueType type : bean.getQueue()) {
            ret.add(type.getName());
        }
        
        return ret;
    }
    
    public List<String> getTopics() {
        List<String> ret = new ArrayList<String>();
        for (TopicType type : bean.getTopic()) {
            ret.add(type.getName());
        }
        
        return ret;
    }    
    
    private static MessageModel generate1031() {
        org.netbeans.modules.j2ee.weblogic9.dd.jms1031.WeblogicJms webLogicJms = new org.netbeans.modules.j2ee.weblogic9.dd.jms1031.WeblogicJms();
        webLogicJms.setAttributeValue("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"); // NOI18N
        webLogicJms.setAttributeValue("xsi:schemaLocation", "http://xmlns.oracle.com/weblogic/weblogic-jms http://xmlns.oracle.com/weblogic/weblogic-jms/1.0/weblogic-jms.xsd"); // NOI18N
        return new MessageModel(webLogicJms);
    } 
}
