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
 */
/*
 * ResourceConfiguration.java
 *
 * Created on August 22, 2005, 12:43 PM
 */

package org.netbeans.modules.j2ee.sun.ide.sunresources.beans;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import javax.enterprise.deploy.spi.DeploymentManager;
import org.netbeans.modules.j2ee.deployment.common.api.DatasourceAlreadyExistsException;
import org.netbeans.modules.j2ee.deployment.common.api.Datasource;

import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.ErrorManager;
import org.openide.util.RequestProcessor;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.modules.j2ee.deployment.common.api.MessageDestination;

import org.netbeans.modules.j2ee.sun.api.ResourceConfiguratorInterface;
import org.netbeans.modules.j2ee.sun.dd.api.DDProvider;
import org.netbeans.modules.j2ee.sun.dd.api.serverresources.AdminObjectResource;
import org.netbeans.modules.j2ee.sun.dd.api.serverresources.ConnectorConnectionPool;
import org.netbeans.modules.j2ee.sun.dd.api.serverresources.ConnectorResource;
import org.netbeans.modules.j2ee.sun.dd.api.serverresources.Resources;
import org.netbeans.modules.j2ee.sun.dd.api.serverresources.JdbcResource;
import org.netbeans.modules.j2ee.sun.dd.api.serverresources.ConnectorResource;
import org.netbeans.modules.j2ee.sun.dd.api.serverresources.PropertyElement;
import org.netbeans.modules.j2ee.sun.dd.api.serverresources.JdbcConnectionPool;
import org.netbeans.modules.j2ee.sun.share.serverresources.SunDatasource;
import org.netbeans.modules.j2ee.sun.share.serverresources.SunMessageDestination;
import org.netbeans.modules.j2ee.sun.sunresources.beans.DatabaseUtils;

import org.netbeans.modules.j2ee.sun.sunresources.beans.Field;
import org.netbeans.modules.j2ee.sun.sunresources.beans.FieldGroup;
import org.netbeans.modules.j2ee.sun.sunresources.beans.FieldGroupHelper;
import org.netbeans.modules.j2ee.sun.sunresources.beans.FieldHelper;
import org.netbeans.modules.j2ee.sun.sunresources.beans.Wizard;
import org.netbeans.modules.j2ee.sun.sunresources.beans.WizardConstants;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Nitya Doraisamy
 */
public class ResourceConfigurator implements ResourceConfiguratorInterface {
    
    public static final String __JMSResource = "jms"; // NOI18N
    public static final String __JMSConnectionFactory = "jms_CF"; // NOI18N
    public static final String __JdbcConnectionPool = "connection-pool"; // NOI18N
   
    public static final String __SunResourceExt = "sun-resource"; // NOI18N
    
    private final static char BLANK = ' ';
    private final static char DOT   = '.';
    private final static char[]	ILLEGAL_FILENAME_CHARS	= {'/', '\\', ':', '*', '?', '"', '<', '>', '|', ',', '=', ';' };
    // private final static char[]	ILLEGAL_RESOURCE_NAME_CHARS	= {':', '*', '?', '"', '<', '>', '|', ',' };
    private final static char REPLACEMENT_CHAR = '_';
    private final static char DASH = '-';
    
    public static final String __ConnectionPool = "ConnectionPool"; // NOI18N
    private static final String DATAFILE = "org/netbeans/modules/j2ee/sun/sunresources/beans/CPWizard.xml";  // NOI18N

    private boolean showMsg = false;
    private DeploymentManager currentDM = null;
    
    ResourceBundle bundle = ResourceBundle.getBundle("org.netbeans.modules.j2ee.sun.ide.sunresources.beans.Bundle");// NOI18N
    
    /**
     * Creates a new instance of ResourceConfigurator
     */
    public ResourceConfigurator() {
    }
    
    public void setDeploymentManager(DeploymentManager dm) {
        this.currentDM = dm;
    }
    
    /** Returns whether or not the specific JMS resource alread exists.
     *
     * @param jndiName Jndi name identifying the JMS resource.
     * @param dir Folder where the resources should be stored.  Can be null,
     *   in which case this method will return false.
     *
     * @returns true if the resource exists in the resource, or false for all
     *  other possibilities.
     */
    public boolean isJMSResourceDefined(String jndiName, File dir) {
        return resourceAlreadyDefined(jndiName, dir, __JMSResource);
    }

    public MessageDestination createJMSResource(String jndiName, MessageDestination.Type type, String ejbName, File dir) {
        SunMessageDestination msgDest = null;
        FileObject location = FileUtil.toFileObject(dir);
        Resources resources = ResourceUtils.getServerResourcesGraph(location);
        AdminObjectResource aoresource = resources.newAdminObjectResource();
        aoresource.setJndiName(jndiName);
        if (MessageDestination.Type.QUEUE.equals(type)) {
            aoresource.setResType(WizardConstants.__QUEUE);
        } else if (MessageDestination.Type.TOPIC.equals(type)) {
            aoresource.setResType(WizardConstants.__TOPIC);
        }
        aoresource.setResAdapter(WizardConstants.__JmsResAdapter);
        aoresource.setEnabled("true"); // NOI18N
        aoresource.setDescription(""); // NOI18N
        PropertyElement prop = aoresource.newPropertyElement();
        prop.setName("Name"); // NOI18N
        prop.setValue(ejbName);
        aoresource.addPropertyElement(prop);
        resources.addAdminObjectResource(aoresource);
        
        ConnectorResource connresource = resources.newConnectorResource();
        ConnectorConnectionPool connpoolresource = resources.newConnectorConnectionPool();
        
        String connectionFactoryJndiName= "jms/" + jndiName + "Factory"; // NOI18N
        connresource.setJndiName(connectionFactoryJndiName);
        connresource.setDescription("");
        connresource.setEnabled("true");
        connresource.setPoolName(connectionFactoryJndiName);
        
        connpoolresource.setName(connectionFactoryJndiName);
        connpoolresource.setResourceAdapterName(WizardConstants.__JmsResAdapter);
        
        if(type.equals(MessageDestination.Type.QUEUE)) {
            connpoolresource.setConnectionDefinitionName(WizardConstants.__QUEUE_CNTN_FACTORY);
        } else {
            if(type.equals(MessageDestination.Type.TOPIC)) {
                connpoolresource.setConnectionDefinitionName(WizardConstants.__TOPIC_CNTN_FACTORY);
            } else {
                assert false; //control should never reach here
            }
        }
        resources.addConnectorResource(connresource);
        resources.addConnectorConnectionPool(connpoolresource);
        ResourceUtils.createFile(location, resources);
        msgDest = new SunMessageDestination(jndiName, type);
        return msgDest;
    }
    /** Creates a new JMS resource with the specified values.
     *
     * @param jndiName jndi-name that identifies this JMS resource.
     * @param msgDstnType type of message destination.
     * @param msgDstnName name of the message destination.
     * @param ejbName name of ejb.
     * @param dir Folder where the resource should be stored.  Should not be null.
     */
    public void createJMSResource(String jndiName, String msgDstnType, String msgDstnName, String ejbName, File dir) {
        FileObject location = FileUtil.toFileObject(dir);
        Resources resources = ResourceUtils.getServerResourcesGraph(location);
        AdminObjectResource aoresource = resources.newAdminObjectResource();
        aoresource.setJndiName(jndiName);
        aoresource.setResType(msgDstnType);
        aoresource.setResAdapter(WizardConstants.__JmsResAdapter);
        aoresource.setEnabled("true"); // NOI18N
        aoresource.setDescription(""); // NOI18N
        PropertyElement prop = aoresource.newPropertyElement();
        prop.setName("Name"); // NOI18N
        prop.setValue(ejbName);
        aoresource.addPropertyElement(prop);
        resources.addAdminObjectResource(aoresource);
        
        ConnectorResource connresource = resources.newConnectorResource();
        ConnectorConnectionPool connpoolresource = resources.newConnectorConnectionPool();
        
        String connectionFactoryJndiName= "jms/" + msgDstnName + "Factory"; // NOI18N
        connresource.setJndiName(connectionFactoryJndiName);
        connresource.setDescription("");
        connresource.setEnabled("true");
        connresource.setPoolName(connectionFactoryJndiName);
        
        connpoolresource.setName(connectionFactoryJndiName);
        connpoolresource.setResourceAdapterName(WizardConstants.__JmsResAdapter);
        
        if(msgDstnType.equals(WizardConstants.__QUEUE)) {
            connpoolresource.setConnectionDefinitionName(WizardConstants.__QUEUE_CNTN_FACTORY);
        } else {
            if(msgDstnType.equals(WizardConstants.__TOPIC)) {
                connpoolresource.setConnectionDefinitionName(WizardConstants.__TOPIC_CNTN_FACTORY);
            } else {
                assert false; //control should never reach here
            }
        }
        resources.addConnectorResource(connresource);
        resources.addConnectorConnectionPool(connpoolresource);
        
        ResourceUtils.createFile(location, resources);
    }
    
    public void createJDBCDataSourceFromRef(String refName, String databaseInfo, File dir) {
        /*try {
            String name = refName;
            if(databaseInfo != null) {
                String vendorName = convertToValidName(databaseInfo);
                if(vendorName != null) {
                    name = vendorName;
                }
                
                if(vendorName.equals("derby_embedded")){ //NOI18N
                    NotifyDescriptor d = new NotifyDescriptor.Message(bundle.getString("Err_UnSupportedDerby"), NotifyDescriptor.WARNING_MESSAGE); // NOI18N
                    DialogDisplayer.getDefault().notify(d);
                    return;
                }
                
                // Is connection pool already defined
                String poolName = generatePoolName(name, dir, databaseInfo);
                if(poolName == null) {
                    if(resourceAlreadyDefined(refName, dir, __JdbcResource)) {
                        return;
                    } else {
                        createJDBCResource(name, refName, databaseInfo, dir);
                    }
                } else {
                    name = poolName;
                    createCPPoolResource(name, refName, databaseInfo, dir);
                    createJDBCResource(name, refName, databaseInfo, dir);
                }
            }
        } catch(IOException ex) {
            // XXX Report I/O Exception to the user.  We should do a nicely formatted
            // message identifying the problem.
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
        }*/
    }
    
    public String createJDBCDataSourceForCmp(String beanName, String databaseInfo, File dir) {
        /*String name = "jdbc/" + beanName; // NOI18N
        String jndiName = name;
        try {
            if(databaseInfo != null) {
                String vendorName = convertToValidName(databaseInfo);
                if(vendorName != null) {
                    name = vendorName;
                }
                
                if(vendorName.equals("derby_embedded")){  //NOI18N
                    NotifyDescriptor d = new NotifyDescriptor.Message(bundle.getString("Err_UnSupportedDerby"), NotifyDescriptor.WARNING_MESSAGE); // NOI18N
                    DialogDisplayer.getDefault().notify(d);
                    return null;
                }
                // Return if resource already defined
                String poolName = generatePoolName(name, dir, databaseInfo);
                if(poolName == null) {
                    return null;
                } else {
                    name = poolName;
                }

                jndiName = "jdbc/" + name;

                createCPPoolResource(name, jndiName, databaseInfo, dir);
                createJDBCResource(name, jndiName, databaseInfo, dir);

                if(this.showMsg) {
                    String mess = MessageFormat.format(bundle.getString("LBL_UnSupportedDriver"), new Object [] { jndiName }); // NOI18N
                    showInformation(mess);
                    this.showMsg = false;
                }
            }
        } catch(IOException ex) {
            // XXX Report I/O Exception to the user.  We should do a nicely formatted
            // message identifying the problem.
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
        }
        
        return jndiName;
         */
        return null;
    }
    
    private boolean isLegalFilename(String filename) {
        for(int i = 0; i < ILLEGAL_FILENAME_CHARS.length; i++) {
            if(filename.indexOf(ILLEGAL_FILENAME_CHARS[i]) >= 0) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isFriendlyFilename(String filename) {
        if(filename.indexOf(BLANK) >= 0 || filename.indexOf(DOT) >= 0) {
            return false;
        }
        
        return isLegalFilename(filename);
    }
    
    private String makeLegalFilename(String filename) {
        for(int i = 0; i < ILLEGAL_FILENAME_CHARS.length; i++) {
            filename = filename.replace(ILLEGAL_FILENAME_CHARS[i], REPLACEMENT_CHAR);
        }
        
        return filename;
    }
    
    private String makeShorterLegalFilename(String filename) {
        //To clean up the default generation a little
        if(filename.indexOf("://") != -1) { // NOI18N
            filename = filename.substring(0, filename.indexOf("://")) + "_" +  // NOI18N
                    filename.substring(filename.indexOf("://")+3, filename.length()); // NOI18N
        }
        if(filename.indexOf("//") != -1) { // NOI18N
            filename = filename.substring(0, filename.indexOf("//")) + "_" +  // NOI18N
                    filename.substring(filename.indexOf("//")+2, filename.length()); // NOI18N
        }
        filename = makeLegalFilename(filename);
        
        return filename;
    }
    
    private void ensureFolderExists(File folder) throws IOException {
        if(!folder.exists()) {
            folder.mkdir();
        }
    }
    
    private String getFileName(String beanName, String resourceType) {
        
        assert (beanName != null);
        assert (beanName.length() != 0);
        
        assert (resourceType != null);
        assert (resourceType.length() != 0);
        
        String fileName = resourceType;
        
        if(!isFriendlyFilename(beanName)) {
            beanName = makeLegalFilename(beanName);
        }
        
        if(!isFriendlyFilename(fileName)) {
            fileName = makeLegalFilename(fileName);
        }
        
        fileName = fileName + DASH + beanName + DOT + __SunResourceExt;
        return fileName;
    }
    
    private JdbcConnectionPool setDerbyProps(String vendorName, String url, JdbcConnectionPool jdbcConnectionPool){
        url = stripExtraDBInfo(url);
        String workingUrl = url.substring(url.indexOf("//") + 2, url.length()); //NOI18N
        String hostName = getDerbyServerName(workingUrl);
        PropertyElement servName = jdbcConnectionPool.newPropertyElement();
        servName.setName(WizardConstants.__ServerName);
        servName.setValue(hostName);
        
        String portNumber = getDerbyPortNo(workingUrl);
        PropertyElement portno = jdbcConnectionPool.newPropertyElement();
        portno.setName(WizardConstants.__DerbyPortNumber);
        portno.setValue(portNumber);
        
        String databaseName = getDerbyDatabaseName(workingUrl);
        PropertyElement dbName = jdbcConnectionPool.newPropertyElement();
        dbName.setName(WizardConstants.__DerbyDatabaseName);
        dbName.setValue(databaseName);
        
        String connectionAttr = getDerbyConnAttrs(workingUrl);
        if(! connectionAttr.equals("")) { //NOI18N
            PropertyElement connAttr = jdbcConnectionPool.newPropertyElement();
            connAttr.setName(WizardConstants.__DerbyConnAttr);
            connAttr.setValue(connectionAttr);
            jdbcConnectionPool.addPropertyElement(connAttr);
        }
        jdbcConnectionPool.addPropertyElement(servName);
        jdbcConnectionPool.addPropertyElement(portno);
        jdbcConnectionPool.addPropertyElement(dbName);
        return jdbcConnectionPool;
    }
     
    
    /**
     * Parses incoming url to create additional properties required by server
     * example of url : jdbc:sun:db2://serverName:portNumber;databaseName=databaseName
     * jdbc:sun:sqlserver://serverName[:portNumber]
     */
    private JdbcConnectionPool setAdditionalProps(String vendorName, String url, JdbcConnectionPool jdbcConnectionPool){
        url = stripExtraDBInfo(url);
        String workingUrl = url;
        if(vendorName.equals("sybase2")){ //NOI18N
            int index = url.indexOf("Tds:"); //NOI18N
            if(index != -1){
                workingUrl = url.substring(index + 4, url.length()); //NOI18N
            } else {
                return jdbcConnectionPool;
            }
        }else {
            workingUrl = url.substring(url.indexOf("//") + 2, url.length()); //NOI18N
        }
        String hostName = getUrlServerName(workingUrl);
        PropertyElement servName = jdbcConnectionPool.newPropertyElement();
        servName.setName(WizardConstants.__ServerName);
        servName.setValue(hostName);
        
        String portNumber = getUrlPortNo(workingUrl);
        PropertyElement portno = jdbcConnectionPool.newPropertyElement();
        portno.setName(WizardConstants.__PortNumber);
        portno.setValue(portNumber);
        
        if(Arrays.asList(WizardConstants.VendorsDBNameProp).contains(vendorName)) {  //NOI18N
            PropertyElement dbName = jdbcConnectionPool.newPropertyElement();
            String databaseName = "";
            if(vendorName.equals("sun_oracle") || vendorName.equals("datadirect_oracle")) {  //NOI18N
                databaseName = getUrlSIDName(workingUrl);
                dbName.setName(WizardConstants.__SID);
            }else{
                databaseName = getUrlDatabaseName(workingUrl);
                dbName.setName(WizardConstants.__DatabaseName);
                if(databaseName.equals("")) { //NOI18N
                    databaseName = getUrlDbName(workingUrl);
                }
            }
            dbName.setValue(databaseName);
            jdbcConnectionPool.addPropertyElement(dbName);
        }
        jdbcConnectionPool.addPropertyElement(servName);
        jdbcConnectionPool.addPropertyElement(portno);
        return jdbcConnectionPool;
    }
    
    private JdbcConnectionPool setDBProp(String vendorName, String url, JdbcConnectionPool jdbcConnectionPool){
        url = stripExtraDBInfo(url);
        String workingUrl = url.substring(url.indexOf("//") + 2, url.length()); //NOI18N
        String databaseName = getUrlDatabaseName(workingUrl);
        PropertyElement dbName = jdbcConnectionPool.newPropertyElement();
        dbName.setName(WizardConstants.__DerbyDatabaseName);
        dbName.setValue(databaseName);
        
        jdbcConnectionPool.addPropertyElement(dbName);
        return jdbcConnectionPool;
    }
    
    private String getDatasourceClassName(String vendorName, boolean isXA, Wizard wizard) {
        if(vendorName == null) {
            return null;
        }
        
        try {
            FieldGroup generalGroup = FieldGroupHelper.getFieldGroup(wizard, WizardConstants.__General);
            
            Field dsField = null;
            if (isXA) {
                dsField = FieldHelper.getField(generalGroup, WizardConstants.__XADatasourceClassname);
            } else {
                dsField = FieldHelper.getField(generalGroup, WizardConstants.__DatasourceClassname);
            }
            return FieldHelper.getConditionalFieldValue(dsField, vendorName);
        } catch(Exception ex) {
            // This should really a Schema2BeansException, but for classloader and dependency
            // purposes we're catching Exception instead.
            
            // XXX why do we suppress this?
            // Unable to create Wizard object -- supppress.
        }
        
        return null;
    }
    
    
    public static String getDatabaseVendorName(String url, Wizard wizard) {
        String vendorName = "";
        try {
            if(wizard == null) {
               wizard = getWizardInfo();
            }   
            FieldGroup propGroup = FieldGroupHelper.getFieldGroup(wizard, WizardConstants.__PropertiesURL);
            Field urlField = FieldHelper.getField(propGroup, "vendorUrls"); // NOI18N
            vendorName = FieldHelper.getOptionNameFromValue(urlField, url);
        } catch(Exception ex) {
            // This should really a Schema2BeansException, but for classloader and dependency
            // purposes we're catching Exception instead.
            
            // XXX why do we suppress this?
            // Unable to create Wizard object -- supppress.
        }
        
        return vendorName;
    }
    
    private String convertToValidName(String database) {
        database = stripExtraDBInfo(database);
        String vendorName = getDatabaseVendorName(database, null);
        if(vendorName != null) {
            if(!vendorName.equals("")) { // NOI18N
                if(!isFriendlyFilename(vendorName)) {
                    vendorName = makeLegalFilename(vendorName);
                }
                this.showMsg = false;
            } else {
                this.showMsg = true;
                vendorName = makeShorterLegalFilename(database);
            }
        }
        return vendorName;
    }
    
    private String getDatabaseName(String database) {
        String result = null;
        int index = database.lastIndexOf('/') + 1;
        if(index > 0) {
            result = database.substring(index);
        }
        return result;
    }
    
    private String getResourceType(boolean isXA) {
        if(isXA) {
            return "javax.sql.XADataSource";  // NOI18N
        } else {
            return "javax.sql.DataSource";  // NOI18N
        }
    }
    
    private boolean resourceAlreadyDefined(String resName, File dir, String resType) {
        boolean result = false;
        if(dir != null && dir.exists()) {
            String filename = getFileName(resName, resType);
            File resourceFile = new File(dir, filename);
            if(resourceFile.exists()) {
                result = true;
            }
        }
        
        return result;
    }
    
    private String isSameDatabaseConnection(File resourceFile, String databaseUrl, String username, String password) {
        String poolJndiName = null;
        try {
            FileInputStream in = new FileInputStream(resourceFile);
            Resources resources = DDProvider.getDefault().getResourcesGraph(in);
            
            // identify JDBC Resources xml
            JdbcConnectionPool[] pools = (JdbcConnectionPool[])resources.getJdbcConnectionPool();
            if(pools.length != 0) {
                JdbcConnectionPool connPool = pools[0];
                PropertyElement[] pl = (PropertyElement[])connPool.getPropertyElement();
                if(databaseUrl.startsWith("jdbc:derby:")){ //NOI18N
                    databaseUrl = stripExtraDBInfo(databaseUrl);
                    String workingUrl = databaseUrl.substring(databaseUrl.indexOf("//") + 2, databaseUrl.length());
                    String hostName = getDerbyServerName(workingUrl);
                    String portNumber = getDerbyPortNo(workingUrl);
                    String databaseName = getDerbyDatabaseName(workingUrl);
                    String hostProp = null;
                    String portProp = null;
                    String dbProp = null;
                    String dbUser = null;
                    String dbPwd = null;
                    for(int i=0; i<pl.length; i++) {
                        String prop = pl[i].getName();
                        if(prop.equalsIgnoreCase(WizardConstants.__ServerName)) { 
                            hostProp = pl[i].getValue();
                        }else if(prop.equals(WizardConstants.__DerbyPortNumber)){
                            portProp = pl[i].getValue();
                        }else if(prop.equals(WizardConstants.__DerbyDatabaseName)){
                            dbProp = pl[i].getValue();
                        }else if(prop.equals(WizardConstants.__User)){
                            dbUser = pl[i].getValue();
                        }else if(prop.equals(WizardConstants.__Password)){
                            dbPwd = pl[i].getValue();
                        }
                    }
                    if(hostName.equals(hostProp) && portNumber.equals(portProp) && 
                            databaseName.equals(dbProp)){
                        if(dbUser != null && dbPwd != null && dbUser.equals(username) && dbPwd.equals(password)){
                            poolJndiName = connPool.getName();
                        }
                    }   
                }else{
                    String hostName = ""; //NOI18N
                    String portNumber = ""; //NOI18N
                    String databaseName = ""; //NOI18N
                    String sid = ""; //NOI18N
                    String user = ""; //NOI18N
                    String pwd = ""; //NOI18N
                    for(int i=0; i<pl.length; i++) {
                        String prop = pl[i].getName();
                        if(prop.equalsIgnoreCase(WizardConstants.__ServerName)) {
                            hostName = pl[i].getValue();
                        }else if(prop.equals(WizardConstants.__PortNumber)){
                            portNumber = pl[i].getValue();
                        }else if(prop.equals(WizardConstants.__DatabaseName)){
                            databaseName = pl[i].getValue();
                        }else if(prop.equals(WizardConstants.__SID)){
                            sid = pl[i].getValue();
                        }else if(prop.equals(WizardConstants.__User)){
                            user = pl[i].getValue();
                        }else if(prop.equals(WizardConstants.__Password)){
                            pwd = pl[i].getValue();
                        }
                    }
                    String serverPort = hostName;
                    if (null != portNumber && portNumber.length() > 0) {
                        serverPort += ":" + portNumber; //NOI18N
                    }
                    if((databaseUrl.indexOf(serverPort) != -1 ) && 
                       ((databaseUrl.indexOf(databaseName) != -1) || (databaseUrl.indexOf(sid) != -1))){
                            if((username != null && user.equals(username)) && (password != null && pwd.equals(password))){
                                poolJndiName = connPool.getName();
                            }
                    }
                        
                    for(int i=0; i<pl.length; i++) {
                        String prop = pl[i].getName();
                        if(prop.equals("URL") || prop.equals("databaseName")) { // NOI18N
                            String urlValue = pl[i].getValue();
                            if(urlValue.equals(databaseUrl)) {
                                if((username != null && user.equals(username)) && (password != null && pwd.equals(password))){
                                    poolJndiName = connPool.getName();
                                    break;
                                }
                            }
                        }
                    } //for
                }
            }
            in.close();
        } catch(IOException ex) {
            // Could not check local file
        }
        return poolJndiName;
    }
    
    private String stripExtraDBInfo(String dbConnectionString) {
        if(dbConnectionString.indexOf("[") != -1) { //NOI18N
            dbConnectionString = dbConnectionString.substring(0, dbConnectionString.indexOf("[")).trim(); // NOI18N
        }
        return dbConnectionString;
    }
    
    public static void showInformation(final String msg) {
        // Asynchronous message posting.  Placed on AWT thread automatically by DialogDescriptor.
        RequestProcessor.getDefault().post(new Runnable() {
            public void run() {
                NotifyDescriptor d = new NotifyDescriptor.Message(msg, NotifyDescriptor.WARNING_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        });
    }
    
    private static Wizard getWizardInfo(){
        Wizard wizard = null;        
        try {
            InputStream in = Wizard.class.getClassLoader().getResourceAsStream(DATAFILE);
            wizard = Wizard.createGraph(in);
            in.close();
        } catch(Exception ex) {
            // XXX Report I/O Exception to the user.  We should do a nicely formatted
            // message identifying the problem.
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
        }
        return wizard;
    }
    
    private String getDerbyServerName(String url){
        String hostName = ""; //NOI18N
        int index = url.indexOf(":"); //NOI18N
        if(index != -1) {
            hostName = url.substring(0, index); 
        }else{
            index = url.indexOf("/"); //NOI18N
            if(index != -1){
                hostName = url.substring(0, index); 
            }    
        }    
        return hostName;
    }
    
    private String getDerbyPortNo(String url){
        String portNumber = "1527";  //NOI18N
        int index = url.indexOf(":"); //NOI18N
        if(index != -1) {
            portNumber = url.substring(index + 1, url.indexOf("/")); //NOI18N
        }    
        return portNumber;
    }
    
    private String getDerbyDatabaseName(String url){
        String databaseName = ""; //NOI18N
        int index = url.indexOf("/"); //NOI18N
        if(index != -1){
            int colonIndex = url.indexOf(";"); //NOI18N
            if(colonIndex != -1) {
                databaseName = url.substring(index + 1, colonIndex);        
            } else {
                databaseName = url.substring(index + 1, url.length());        
            }    
        }
        return databaseName;
    }
    
    private String getDerbyConnAttrs(String url){
        String connAttr = ""; //NOI18N
        int colonIndex = url.indexOf(";"); //NOI18N
        if(colonIndex != -1) {
            connAttr = url.substring(colonIndex,  url.length());         
        }    
        return connAttr;
    }
    
    private String getUrlServerName(String url){
        String hostName = ""; //NOI18N
        int index = url.indexOf(":"); //NOI18N
        if(index != -1) {
            hostName = url.substring(0, index);
        }else{
            index = url.indexOf("/"); //NOI18N
            if(index != -1) {
                hostName = url.substring(0, index);
            }else{
                index= url.indexOf(";"); //NOI18N
                if(index != -1) {
                    hostName = url.substring(0, index);
                }
            }
        }
        return hostName;
    }
    
    private String getUrlPortNo(String url){
        String portNumber = "";  //NOI18N
        int index = url.indexOf(":"); //NOI18N
        if(index != -1){
            int slashIndex = url.indexOf("/"); //NOI18N
            int colonIndex = url.indexOf(";"); //NOI18N
            if(slashIndex != -1)
                portNumber = url.substring(index + 1, slashIndex); 
            else{
                if(colonIndex != -1)
                    portNumber = url.substring(index + 1, colonIndex); 
                else
                    portNumber = url.substring(index + 1, url.length()); 
            }    
        }    
        return portNumber;
    }
    
    /**
     * Parses incoming url to create DatabaseName additional properties required by server
     * example of url : jdbc:sun:db2://serverName:portNumber;databaseName=databaseName
     * "jdbc:sun:sqlserver://sunsqlserverHost:3333;databaseName=sunsqlserverdb;selectMethod=cursor"
     */
    private String getUrlDatabaseName(String url){
        String databaseName = ""; //NOI18N
        int dbIndex = url.indexOf(";databaseName="); //NOI18N
        if(dbIndex != -1){
            int eqIndex = url.indexOf("=", dbIndex); //NOI18N
            int lenIndex = url.indexOf(";", eqIndex); //NOI18N
            if(lenIndex != -1){
                databaseName = url.substring(eqIndex + 1, lenIndex);
            }else{
                databaseName = url.substring(eqIndex + 1, url.length());
            }
        }
        return databaseName;
    }
    /**
     * Parses incoming url(sun:oracle) to create SID additional properties required by server
     * example of url : jdbc:sun:oracle://serverName[:portNumber][;SID=databaseName]
     */
    private String getUrlSIDName(String url){
        String databaseName = ""; //NOI18N
        int sidIndex = url.indexOf(";SID="); //NOI18N
        if(sidIndex != -1){
            int eqIndex = url.indexOf("=", sidIndex); //NOI18N
            databaseName = url.substring(eqIndex + 1, url.length());
        }
        return databaseName;
    }
    /**
     * Parses incoming url. to create SID additional properties required by server
     * examples of url 
     *   - jdbc:derby://serverName:portNumber/databaseName;create=true
     *   - jdbc:mysql://host:port/database?relaxAutoCommit="true"
     *   - jdbc:vendor://host:port/database
     */
    private String getUrlDbName(String url){
        String databaseName = ""; //NOI18N
        int slashIndex = url.indexOf("/"); //NOI18N
        int clIndex = url.indexOf(";", slashIndex); //NOI18N
        int scIndex = url.indexOf(":", slashIndex); //NOI18N
        int qIndex = url.indexOf("?", slashIndex); //NOI18N
        if(slashIndex != -1){
            if(clIndex != -1)
                databaseName = url.substring(slashIndex + 1, clIndex);
            else if(scIndex != -1)
                databaseName = url.substring(slashIndex + 1, scIndex);
            else if(qIndex != -1)
                databaseName = url.substring(slashIndex + 1, qIndex);
            else
                databaseName = url.substring(slashIndex + 1, url.length());
                
        }
        return databaseName;
    }
    
    /***************************************** DS Management API *****************************************************************************/
    
    /**
     * Returns Set of SunDataSource's(JDBC Resources) that are deployed on the server. 
     * Called from SunDataSourceManager.
     * SunDataSource is a combination of JDBC & JDBC Connection Pool Resources.
     * @return Set containing SunDataSource
     */
    public HashSet getServerDataSources() {
        return ResourceUtils.getServerDataSources(this.currentDM);
    }

    /**
     * Implementation of DS Management API in ConfigurationSupport
     * SunDataSource is a combination of JDBC & JDBC Connection Pool Resources.
     * Called through ConfigurationSupportImpl
     * @return Returns Set of SunDataSource's(JDBC Resources) present in this J2EE project
     * @param dir File providing location of the project's server resource directory
     */
    public HashSet getResources(File resourceDir) {
        HashSet serverresources = getServerResourceFiles(resourceDir);
        if (serverresources.size() == 0) {
            return serverresources;
        }    

        HashSet dsources = new HashSet();
        HashMap connPools = getConnectionPools(serverresources);
        List dataSources = getJdbcResources(serverresources);
        for(int i=0; i<dataSources.size(); i++){
            JdbcResource datasourceBean = (JdbcResource)dataSources.get(i);
            String poolName = datasourceBean.getPoolName();
            try{
                JdbcConnectionPool connectionPoolBean =(JdbcConnectionPool)connPools.get(poolName);
                String url = "";
                String username = "";
                String password = "";
                String driverClass = "";
                String serverName = "";
                String portNo = "";
                String dbName = "";
                String sid = "";
                if(connectionPoolBean != null){
                    PropertyElement[] props = connectionPoolBean.getPropertyElement();
                    driverClass = connectionPoolBean.getDatasourceClassname();
                    HashMap properties = new HashMap();
                    for (int j = 0; j < props.length; j++) {
                        Object val = props[j].getValue();
                        String propValue = "";
                        if(val != null) {
                            propValue = val.toString();
                        }    
                        String propName = props[j].getName();
                        if(propName.equalsIgnoreCase(WizardConstants.__DatabaseName)){
                            if(driverClass.indexOf("pointbase") != -1) { //NOI18N
                                url = propValue;
                            } else if(driverClass.indexOf("derby") != -1) { //NOI18N
                                dbName = propValue;
                            } else {
                                dbName = propValue;
                            }    
                        }else if(propName.equalsIgnoreCase(WizardConstants.__User)) {
                            username = propValue;
                        }else if(propName.equalsIgnoreCase(WizardConstants.__Password)){
                            password = propValue;
                        }else if(propName.equalsIgnoreCase(WizardConstants.__Url)){
                            url = propValue;
                        }else if(propName.equalsIgnoreCase(WizardConstants.__ServerName)){
                            serverName = propValue;
                        }else if(propName.equalsIgnoreCase(WizardConstants.__DerbyPortNumber)){
                            portNo = propValue;
                        }else if(propName.equalsIgnoreCase(WizardConstants.__SID)){
                            sid = propValue;
                        }
                    }
                    
                    if(driverClass.indexOf("derby") != -1){ //NOI18N
                        url = "jdbc:derby://";
                        if(serverName != null){
                            url = url + serverName;
                            if(portNo != null  && portNo.length() > 0) {
                                url = url + ":" + portNo; //NOI18N
                            }    
                            url = url + "/" + dbName ; //NOI8N
                        }
                    }else if(url.equals("")) { //NOI18N
                        String urlPrefix = DatabaseUtils.getUrlPrefix(driverClass);
                        String vName = getDatabaseVendorName(urlPrefix, null);
                        if(serverName != null){
                            if(vName.equals("sybase2")){ //NOI18N
                                url = urlPrefix + serverName; 
                            } else {
                                url = urlPrefix + "//" + serverName; //NOI18N
                            }
                            if(portNo != null  && portNo.length() > 0) {
                                url = url + ":" + portNo; //NOI18N
                            }    
                        }
                        if(vName.equals("sun_oracle") || vName.equals("datadirect_oracle")) { //NOI18N
                            url = url + ";SID=" + sid; //NOI18N
                        }else if(Arrays.asList(WizardConstants.Reqd_DBName).contains(vName)) {
                            url = url + ";databaseName=" + dbName; //NOI18N
                        }else if(Arrays.asList(WizardConstants.VendorsDBNameProp).contains(vName)) {
                            url = url + "/" + dbName ; //NOI8N
                        }    
                    }    
                    
                    DatabaseConnection databaseConnection = ResourceUtils.getDatabaseConnection(url);
                    if(databaseConnection != null) {
                        driverClass = databaseConnection.getDriverClass();
                    }else{
                        //Fix Issue 78212 - NB required driver classname
                        String drivername = DatabaseUtils.getDriverName(url);
                        if(drivername != null) {
                            driverClass = drivername;
                        }    
                    }
                    
                    SunDatasource sunResource = new SunDatasource(datasourceBean.getJndiName(), url, username, password, driverClass);
                    sunResource.setResourceDir(resourceDir);
                    dsources.add(sunResource);
                }else{
                    //Get Pool From Server
                    HashMap poolValues = ResourceUtils.getConnPoolValues(resourceDir, poolName);
                    if(! poolValues.isEmpty()){
                        username = (String)poolValues.get(WizardConstants.__User);
                        password = (String)poolValues.get(WizardConstants.__Password);
                        url = (String)poolValues.get(WizardConstants.__Url);
                        driverClass = (String)poolValues.get(WizardConstants.__DriverClassName);
                        if((url != null) && (! url.equals (""))) { //NOI18N
                            SunDatasource sunResource = new SunDatasource (datasourceBean.getJndiName (), url, username, password, driverClass);
                            sunResource.setResourceDir (resourceDir);
                            dsources.add (sunResource);
                        }
                    }
                }
            }catch(Exception ex){
                //Should never happen
                ErrorManager.getDefault().log(ErrorManager.INFORMATIONAL, "Cannot construct SunDatasource for jdbc resource : " + datasourceBean.getJndiName()
                    + "with pool " + poolName); // NOI18N
            }
        }
        return dsources;
    }
    
    /**
     * Create SunDataSource object's defined. Called from impl of
     * ConfigurationSupport API (ConfigurationSupportImpl).
     * SunDataSource is a combination of JDBC & JDBC Connection Pool
     * Resources.
     * @return Set containing SunDataSource
     * @param jndiName JNDI Name of JDBC Resource
     * @param url Url for database referred to by this JDBC Resource's Connection Pool
     * @param username UserName for database referred to by this JDBC Resource's Connection Pool
     * @param password Password for database referred to by this JDBC Resource's Connection Pool
     * @param driver Driver ClassName for database referred to by this JDBC Resource's Connection Pool
     * @param dir File providing location of the project's server resource directory
     */
    public Datasource createDataSource(String jndiName, String url, String username, String password, String driver, File dir) throws DatasourceAlreadyExistsException {
        SunDatasource ds = null;
        try {
            if(isDataSourcePresent(jndiName, dir)){
                throw new DatasourceAlreadyExistsException(new SunDatasource(jndiName, url, username, password, driver));
            }
            if(url != null){
                String vendorName = convertToValidName(url);
                if(vendorName == null) {
                    vendorName = jndiName;
                }else{
                    if(vendorName.equals("derby_embedded")){ //NOI18N
                        NotifyDescriptor d = new NotifyDescriptor.Message(bundle.getString("Err_UnSupportedDerby"), NotifyDescriptor.WARNING_MESSAGE); // NOI18N
                        DialogDisplayer.getDefault().notify(d);
                        return null;
                    }
                }
                ensureFolderExists(dir);
                // Is connection pool already defined
                String poolName = vendorName + WizardConstants.__ConnPoolSuffix;
                HashMap poolMap = updatePoolName(jndiName, poolName, dir, url, username, password);
                Object[] pools = poolMap.keySet().toArray();
                String newPoolName = (String)pools[0]; 
                Object resFile = poolMap.get(pools[0]);
                if(resFile != null) {
                    if(resourceFileExists(jndiName, dir)) {
                        ds = null;
                    } else {
                        createJDBCResource(jndiName, newPoolName, dir);
                        ds = new SunDatasource(jndiName, url, username, password, driver);
                    }    
                } else {
                    createCPPoolResource(newPoolName, url, username, password, driver, dir);
                    createJDBCResource(jndiName, newPoolName, dir);
                    ds = new SunDatasource(jndiName, url, username, password, driver);
                }
            }
        } catch(IOException ex) {
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ex);
        }
        return ds;
    }    
    
    private void createCPPoolResource(String name, String databaseUrl, String username, String password, String driver, File resourceDir) throws IOException {
        FileObject location = FileUtil.toFileObject(resourceDir);
        Resources resources = ResourceUtils.getServerResourcesGraph(location);
        
        JdbcConnectionPool jdbcConnectionPool = resources.newJdbcConnectionPool();
        jdbcConnectionPool.setName(name);
        jdbcConnectionPool.setResType(getResourceType(false));
                      
        // XXX Refactor database abstractions into own object.  For example,
        // due to lack of member data, we're parsing CPWizard.xml twice here,
        // once in getDatabaseVendorName() and again in getDatasourceClassName()
        Wizard wizard = getWizardInfo();
        String vendorName = getDatabaseVendorName(databaseUrl, wizard);
        String datasourceClassName = ""; // NOI18N
        if(!vendorName.equals("")) { // NOI18N
            datasourceClassName = getDatasourceClassName(vendorName, false, wizard);
        }
        
        if(datasourceClassName.equals("")) { // NOI18N
            datasourceClassName = DatabaseUtils.getDSClassName(databaseUrl);
            if(datasourceClassName == null || datasourceClassName.equals("")) { //NOI18N
                //String mess = MessageFormat.format(bundle.getString("LBL_NoDSClassName"), new Object [] { name }); // NOI18N
                //showInformation(mess);
                datasourceClassName = driver;
            } 
        }
        if(datasourceClassName != null) {
            jdbcConnectionPool.setDatasourceClassname(datasourceClassName);
        }
        PropertyElement user = jdbcConnectionPool.newPropertyElement();
        user.setName(WizardConstants.__User); // NOI18N
        PropertyElement passElement = jdbcConnectionPool.newPropertyElement();
        passElement.setName(WizardConstants.__Password); // NOI18N
        String dbUser = username;
        String dbPassword = password;
        if(vendorName.equals("derby_net")) {  //NOI18N)
            jdbcConnectionPool = setDerbyProps(vendorName, databaseUrl, jdbcConnectionPool);
            if(dbUser == null || dbUser.trim().length() == 0) {
                dbUser = "app"; //NOI18N
            }    
            if(dbPassword == null || dbPassword.trim().length() == 0) {
                dbPassword = "app"; //NOI18N
            }    
        }else {
            if(Arrays.asList(WizardConstants.VendorsExtraProps).contains(vendorName)) {
               jdbcConnectionPool = setAdditionalProps(vendorName, databaseUrl, jdbcConnectionPool);
            }else{
                PropertyElement databaseOrUrl = jdbcConnectionPool.newPropertyElement();
                if(vendorName.equals("pointbase")) { // NOI18N
                    databaseOrUrl.setName(WizardConstants.__DatabaseName); // NOI18N
                } else {
                    databaseOrUrl.setName(WizardConstants.__Url); // NOI18N
                }
                databaseOrUrl.setValue(databaseUrl);
                jdbcConnectionPool.addPropertyElement(databaseOrUrl);
            }
        }
        user.setValue(dbUser);
        jdbcConnectionPool.addPropertyElement(user);
        passElement.setValue(dbPassword);
        jdbcConnectionPool.addPropertyElement(passElement);
        resources.addJdbcConnectionPool(jdbcConnectionPool);
        
        ResourceUtils.createFile(location, resources);
        try{
            Thread.sleep(1000);
        }catch(Exception ex){}
    }
    
    private void createJDBCResource(String jndiName, String poolName, File resourceDir) throws IOException {
        FileObject location = FileUtil.toFileObject(resourceDir);
        Resources resources = ResourceUtils.getServerResourcesGraph(location);
        JdbcResource jdbcResource = resources.newJdbcResource();
        jdbcResource.setPoolName(poolName);
        jdbcResource.setJndiName(jndiName);
        resources.addJdbcResource(jdbcResource);
        ResourceUtils.createFile(location, resources);
    }
    
    private HashSet getServerResourceFiles(File resourceDir) {
        HashSet serverresources = new HashSet();
        if(resourceDir.exists()){
            FileObject resDir = FileUtil.toFileObject(resourceDir);
            Enumeration files = resDir.getChildren(true);
            while (files.hasMoreElements()) {
                FileObject file = (FileObject) files.nextElement();
                if (!file.isFolder() && file.getNameExt().endsWith(".sun-resource") && file.canRead()) { //NOI18N
                    serverresources.add(file);
                }
            }
        }
        return serverresources;
    }
    
    private List getJdbcResources(HashSet serverresources) {
        List dataSources = new ArrayList();
        for (Iterator it = serverresources.iterator(); it.hasNext();) {
            try {
                FileObject dsObj = (FileObject)it.next();
                File dsFile = FileUtil.toFile(dsObj);
                if(! dsFile.isDirectory()){
                    FileInputStream in = new FileInputStream(dsFile);
                    
                    Resources resources = DDProvider.getDefault().getResourcesGraph(in);
                    
                    // identify JDBC Resources xml
                    JdbcResource[] dSources = resources.getJdbcResource();
                    if(dSources.length != 0){
                        dataSources.add(dSources[0]);
                    }
                }
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }
        return dataSources;
    }
    
    private HashMap getConnectionPools(HashSet serverresources) {
        HashMap connPools = new HashMap();
        for (Iterator it = serverresources.iterator(); it.hasNext();) {
            try {
                FileObject dsObj = (FileObject)it.next();
                File dsFile = FileUtil.toFile(dsObj);
                if(! dsFile.isDirectory()){
                    FileInputStream in = new FileInputStream(dsFile);
                    
                    Resources resources = DDProvider.getDefault().getResourcesGraph(in);
                    
                    // identify JDBC Connection Pool xml
                    JdbcConnectionPool[] pools = resources.getJdbcConnectionPool();
                    if(pools.length != 0){
                        JdbcConnectionPool cp = pools[0];
                        connPools.put(cp.getName(), cp);
                    }
                }
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }
        return connPools;
    }
    
    private HashMap getPoolFiles(HashSet serverresources) {
        HashMap connPools = new HashMap();
        for (Iterator it = serverresources.iterator(); it.hasNext();) {
            try {
                FileObject dsObj = (FileObject)it.next();
                File dsFile = FileUtil.toFile(dsObj);
                
                if(! dsFile.isDirectory()){
                    FileInputStream in = new FileInputStream(dsFile);
                    
                    Resources resources = DDProvider.getDefault().getResourcesGraph(in);
                    
                    // identify JDBC Connection Pool xml
                    JdbcConnectionPool[] pools = resources.getJdbcConnectionPool();
                    if(pools.length != 0){
                        connPools.put(dsObj.getName(), dsFile);
                    }
                }
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }
        return connPools;
    }
    
    private boolean isDataSourcePresent(String jndiName, File dir){
        boolean exists = false;
        HashMap serverResources = getDataSourceMap(getResources(dir));
        if(serverResources.containsKey(jndiName)) {
            exists = true;
        }
        return exists;
    }
    
    private HashMap getDataSourceMap(HashSet resources){
        HashMap dSources = new HashMap();
        for (Iterator it = resources.iterator(); it.hasNext();) {
            SunDatasource ds = (SunDatasource)it.next();
            dSources.put(ds.getJndiName(), ds);
        }
        return dSources;
    }
    
    /**
     * 
     * @param dsJndiName JNDI Name of JDBC Datasource that uses/needs this Connection Pool
     * @param poolName Connection Pool for this JDBC Datasource
     * @param dir Resource Directory
     * @param url Database URL
     * @return Returns null if Connection Pool already exists for this database else return 
     * unique Connection PoolName.
     *    
     */
    private HashMap updatePoolName(String dsJndiName, String poolName, File dir, String url, String username, String password){
        HashMap poolAndFile = new HashMap();
        String cpName = poolName;
        HashSet resourceFiles = getServerResourceFiles(dir);
        HashMap poolFiles = getPoolFiles(resourceFiles);
        for(Iterator itr=poolFiles.values().iterator(); itr.hasNext();){
            File resourceFile = (File)itr.next();
            if(resourceFile != null && resourceFile.exists()) {
                String poolJndiName = isSameDatabaseConnection(resourceFile, url, username, password);
                if(poolJndiName != null){
                    cpName = poolJndiName;
                    poolAndFile.put(cpName, resourceFile);
                    break;
                }    
            }
        }
        if(poolAndFile.size() == 0){
            cpName = FileUtil.findFreeFileName(FileUtil.toFileObject(dir), poolName, __SunResourceExt);
            poolAndFile.put(cpName, null);
        }    
        return poolAndFile;
    }
    
    /**
     * Implementation of Message Destination API in ConfigurationSupport
     * @return returns Set of SunMessageDestination's(JMS Resources) present in this J2EE project
     * @param dir File providing location of the project's server resource directory
     */
    public HashSet getMessageDestinations(File resourceDir) {
        HashSet serverresources = getServerResourceFiles(resourceDir);
        if (serverresources.size() == 0) {
            return serverresources;
        }    

        HashSet destinations = new HashSet();
        List jmsResources = getJmsResources(serverresources);
        for(int i=0; i<jmsResources.size(); i++){
            AdminObjectResource aoBean = (AdminObjectResource)jmsResources.get(i);
            String jmsName = aoBean.getJndiName();
            String type = aoBean.getResType();
            SunMessageDestination sunMessage = null;
            if(type.equals(WizardConstants.__QUEUE)){
                sunMessage = new SunMessageDestination(jmsName, MessageDestination.Type.QUEUE);
            } else {
                sunMessage = new SunMessageDestination(jmsName, MessageDestination.Type.TOPIC);
            }
            sunMessage.setResourceDir(resourceDir);
            destinations.add(sunMessage);
        }   
        return destinations;
    }
    
    private List getJmsResources(HashSet serverresources) {
        List jmsResources = new ArrayList();
        for (Iterator it = serverresources.iterator(); it.hasNext();) {
            try {
                FileObject connObj = (FileObject)it.next();
                File connFile = FileUtil.toFile(connObj);
                if(! connFile.isDirectory()){
                    FileInputStream in = new FileInputStream(connFile);
                    
                    Resources resources = DDProvider.getDefault().getResourcesGraph(in);
                    
                    // identify AdminObjectResource xml
                    AdminObjectResource[] adminResources = resources.getAdminObjectResource();
                    if(adminResources.length != 0){
                        jmsResources.add(adminResources[0]);
                    }
                }
            } catch (Exception ex) {
                ErrorManager.getDefault().notify(ex);
            }
        }
        return jmsResources;
    }
    
    public HashSet getServerDestinations() {
        return ResourceUtils.getServerDestinations(this.currentDM);
    }
    
    private File getResourceFile(String fileName, File dir){
        File resourceFile = null;
        if(dir != null && dir.exists()) {
            String filename = fileName + DOT + __SunResourceExt; 
            resourceFile = new File(dir, filename);
        }
        return resourceFile;
    }
    
    private boolean resourceFileExists(String resName, File dir) {
        boolean result = false;
        if(dir != null && dir.exists()) {
            String filename = resName + DOT + __SunResourceExt; 
            File resourceFile = new File(dir, filename);
            if(resourceFile.exists()) {
                result = true;
            }
        }
        return result;
    } 
    
}

