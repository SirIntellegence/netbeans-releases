/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.jsf.editor.facelets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.netbeans.api.xml.services.UserCatalog;
import org.netbeans.modules.web.jsfapi.api.Attribute;
import org.netbeans.modules.web.jsfapi.api.Tag;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.util.Exceptions;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author marekfukala
 */
public final class FaceletsLibraryDescriptor implements LibraryDescriptor {

    static FaceletsLibraryDescriptor create(FileObject definitionFile) throws LibraryDescriptorException {
        return new FaceletsLibraryDescriptor(definitionFile);
    }
    private FileObject definitionFile;
    private String uri;
    private Map<String, Tag> tags = new HashMap<String, Tag>();

    private FaceletsLibraryDescriptor(FileObject definitionFile) throws LibraryDescriptorException {
        this.definitionFile = definitionFile;
        parseLibrary();
    }

    public FileObject getDefinitionFile() {
        return definitionFile;
    }

    @Override
    public String getNamespace() {
        return uri;
    }

    @Override
    public Map<String, Tag> getTags() {
        return tags;
    }

    public static String parseNamespace(InputStream content) throws IOException {
        return parseNamespace(content, "facelet-taglib", "namespace"); //NOI18N
    }

    protected void parseLibrary() throws LibraryDescriptorException {
        try {
            parseLibrary(getDefinitionFile().getInputStream());
        } catch (FileNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    protected void parseLibrary(InputStream content) throws LibraryDescriptorException {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            InputSource is = new InputSource(content); //the ecoding should be autodetected
            docBuilder.setEntityResolver(UserCatalog.getDefault().getEntityResolver()); //we count on TaglibCatalog from web.core module
            Document doc = docBuilder.parse(is);

            //usually the default taglib prefix
            Node tagLib = getNodeByName(doc, "facelet-taglib"); //NOI18N

            uri = getTextContent(tagLib, "namespace"); //NOI18N
            if (uri == null) {
                throw new IllegalStateException("Missing namespace entry in " + getDefinitionFile().getPath() + " library.", null);
            }

            //scan the <tag> nodes content - the tag descriptions
            NodeList tagNodes = doc.getElementsByTagName("tag"); //NOI18N
            if (tagNodes != null) {
                for (int i = 0; i < tagNodes.getLength(); i++) {
                    Node tag = tagNodes.item(i);
                    String tagName = getTextContent(tag, "tag-name"); //NOI18N
                    String tagDescription = getTextContent(tag, "description"); //NOI18N

                    Map<String, Attribute> attrs = new HashMap<String, Attribute>();
                    //find attributes
                    for (Node attrNode : getNodesByName(tag, "attribute")) { //NOI18N
                        String aName = getTextContent(attrNode, "name"); //NOI18N
                        String aDescription = getTextContent(attrNode, "description"); //NOI18N
                        boolean aRequired = Boolean.parseBoolean(getTextContent(attrNode, "required")); //NOI18N

                        attrs.put(aName, new Attribute.DefaultAttribute(aName, aDescription, aRequired));
                    }

                    tags.put(tagName, new TagImpl(tagName, tagDescription, attrs));

                }
            }

        } catch (ParserConfigurationException ex) {
            throw new LibraryDescriptorException("Error parsing facelets library: ", ex); //NOI18N
        } catch (SAXException ex) {
            throw new LibraryDescriptorException("Error parsing facelets library: ", ex); //NOI18N
        } catch (IOException ex) {
            throw new LibraryDescriptorException("Error parsing facelets library: ", ex); //NOI18N
        }


    }
    private static final String STOP_PARSING_MGS = "regularly_stopped"; //NOI18N

    public static String parseNamespace(InputStream content, final String tagTagName, final String namespaceTagName) throws IOException {
        final String[] ns = new String[1];
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setValidating(false);
            SAXParser parser = factory.newSAXParser();

            class Handler extends DefaultHandler {

                private boolean inTaglib = false;
                private boolean inURI = false;

                @Override
                public void startElement(String uri, String localname, String qname, Attributes attr) throws SAXException {
                    String tagName = qname.toLowerCase();
                    if (tagTagName.equals(tagName)) { //NOI18N
                        inTaglib = true;
                    }
                    if (inTaglib) {
                        if (namespaceTagName.equals(tagName)) { //NOI18N
                            inURI = true;
                        }

                    }
                }

                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (inURI) {
                        ns[0] = new String(ch, start, length).trim();
                        //stop parsing
                        throw new SAXException(STOP_PARSING_MGS);
                    }
                }

                @Override
                public InputSource resolveEntity(String publicId, String systemId) {
                    return new InputSource(new StringReader("")); //prevent the parser to use catalog entity resolver // NOI18N
                }
            }


            parser.parse(content, new Handler());
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        } catch (SAXException ex) {
            if (!STOP_PARSING_MGS.equals(ex.getMessage())) {
                throw new IOException(ex);
            }
        }

        return ns[0];
    }

    @Override
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(getDefinitionFile() != null ? getDefinitionFile().getFileSystem().getRoot().getURL().toString() + ";" + getDefinitionFile().getPath() : ""); //NOI18N
            sb.append("; uri = ").append(getNamespace()).append("; tags={"); //NOI18N
            for (Tag t : getTags().values()) {
                sb.append(t.toString());
            }
            sb.append("}]"); //NOI18N
            return sb.toString();
        } catch (FileStateInvalidException ex) {
            return null;
        }
    }

    protected static String getTextContent(Node parent, String childName) {
        Node found = getNodeByName(parent, childName);
        return found == null ? null : found.getTextContent().trim();
    }

    static Node getNodeByName(Node parent, String childName) {
        Collection<Node> found = getNodesByName(parent, childName);
        if (!found.isEmpty()) {
            return found.iterator().next();
        } else {
            return null;
        }
    }

    static Collection<Node> getNodesByName(Node parent, String childName) {
        Collection<Node> nodes = new ArrayList<Node>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(childName)) {
                nodes.add(n);
            }
        }
        return nodes;
    }

}
