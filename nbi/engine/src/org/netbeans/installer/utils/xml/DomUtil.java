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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 * 
 * $Id$
 */
package org.netbeans.installer.utils.xml;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.netbeans.installer.utils.UnexpectedExceptionError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
/**
 *
 * @author Danila_Dugurov
 */
public class DomUtil {
   
   private static final Logger LOG = Logger.getLogger("org.util.DomUtil");
   
   private static final DocumentBuilderFactory BUILDER_FACTORY;
   private static final TransformerFactory TRANSFORMER_FACTORY;
   
   static {
      BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
      TRANSFORMER_FACTORY = TransformerFactory.newInstance();
   }
   
   public static Document parseXmlFile(File xmlFile) throws IOException, ParseException {
      return parseXmlFile(xmlFile, Charset.forName("UTF-8"));
   }
   
   public static Document parseXmlFile(File xmlFile, Charset charset) throws IOException, ParseException {
      final InputStream in = new BufferedInputStream(
              new FileInputStream(xmlFile));
      try {
         return parseXmlFile(in, charset);
      } finally {
         in.close();
      }
   }
   
   public static Document parseXmlFile(CharSequence xmlFile) throws ParseException {
      try {
         final InputStream in = new ByteArrayInputStream(
                 xmlFile.toString().getBytes("UTF-8"));
         return parseXmlFile(in);
      } catch (UnsupportedEncodingException worntHappend) {
         LOG.log(Level.SEVERE, "UTF-8 unsupported encoding on your system");
         throw new RuntimeException(worntHappend);//TODO
      } catch (IOException worntHappend) {
         throw new RuntimeException(worntHappend);
      }
   }
   
   public static Document parseXmlFile(InputStream xmlStream, Charset charset) throws IOException, ParseException {
      try {
         final DocumentBuilder builder = BUILDER_FACTORY.newDocumentBuilder();
         final Reader reader = new InputStreamReader(xmlStream, charset);
         final InputSource inputSource = new InputSource(reader);
         return builder.parse(inputSource);
      } catch (ParserConfigurationException worntHappend) {
         throw new RuntimeException(worntHappend);
      } catch (SAXException ex) {
         LOG.log(Level.SEVERE, "error while parsing xml!", ex);
         throw new ParseException("parsing error occuers!",ex);
      }
   }
   
   public static Document parseXmlFile(InputStream xmlStream) throws IOException, ParseException {
      return parseXmlFile(xmlStream, Charset.forName("UTF-8"));
   }
   
   public static void writeXmlFile(Document document, OutputStream outputStream, Charset charset) throws IOException {
      try {
         final Source domSource = new DOMSource(document);
         final Writer writer = new PrintWriter(new OutputStreamWriter(outputStream, charset));
         final Result output = new StreamResult(writer);
         final Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
         transformer.transform(domSource, output);
         writer.flush();
      } catch(TransformerConfigurationException worntHappend) {
         LOG.log(Level.SEVERE, "error while DOM serializing!");
         throw new UnexpectedExceptionError(worntHappend);
      } catch(TransformerException ex) {
         LOG.log(Level.SEVERE, "error while DOM serializing!", ex);
         throw new IOException(ex.getMessage());
      }
   }
   
   public static void writeXmlFile(Document document, OutputStream outputStream) throws IOException {
      writeXmlFile(document, outputStream, Charset.forName("UTF-8"));
   }
   
   public static void writeXmlFile(Document document, File file) throws IOException {
      writeXmlFile(document, file, Charset.forName("UTF-8"));
   }
   
   public static void writeXmlFile(Document document, File file, Charset charset) throws IOException {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
      try {
         writeXmlFile(document, out, charset);
         out.flush();
      } finally {
         out.close();
      }
   }
   
   public static <T extends DomExternalizable> void addChild(Element parent, T object) {
      parent.appendChild(object.writeXML(parent.getOwnerDocument()));
   }
}
