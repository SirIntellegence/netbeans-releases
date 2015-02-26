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
package org.netbeans.modules.extbrowser.plugins.chrome;

import java.awt.Dialog;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;

import org.json.simple.JSONObject;
import org.netbeans.modules.extbrowser.plugins.ExtensionManager;
import org.netbeans.modules.extbrowser.plugins.ExtensionManager.ExtensitionStatus;
import org.netbeans.modules.extbrowser.plugins.ExtensionManagerAccessor;
import org.netbeans.modules.extbrowser.plugins.ExternalBrowserPlugin;
import org.netbeans.modules.extbrowser.plugins.Message;
import org.netbeans.modules.extbrowser.plugins.Message.MessageType;
import org.netbeans.modules.extbrowser.plugins.MessageListener;
import org.netbeans.modules.extbrowser.plugins.Utils;
import org.netbeans.modules.web.browser.api.BrowserFamilyId;
import org.netbeans.modules.web.browser.api.WebBrowser;
import org.netbeans.modules.web.browser.api.WebBrowsers;


import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.HtmlBrowser;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;



/**
 * @author ads
 *
 */
public class ChromeManagerAccessor implements ExtensionManagerAccessor {

    private static final String NO_WEB_STORE_SWITCH=
            "netbeans.extbrowser.manual_chrome_plugin_install"; // NOI18N

    private static final String PLUGIN_PAGE=
            "https://chrome.google.com/webstore/detail/netbeans-connector/hafdlehgocfcodbgjnpecfajgkeejnaa"; //NOI18N

    /* (non-Javadoc)
     * @see org.netbeans.modules.web.plugins.ExtensionManagerAccessor#getManager()
     */
    @Override
    public BrowserExtensionManager getManager() {
        return new ChromeExtensionManager();
    }


    static class ChromeExtensionManager extends AbstractBrowserExtensionManager {

        private static final String LAST_USED = "\"last_used\":";               // NOI18N

        private static final String VERSION = "\"version\":";                   // NOI18N

        private static final String STATE = "\"state\":";                       // NOI18N

        private static final String PLUGIN_PUBLIC_KEY =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDgo89CrO8f/2srD2BGUP9+dG4I" +
            "kTC2D3gzEjXITaMaQgy8B4ObVpkA3bc27qc7HB9jhVj8P51aAETr89u+AvFgCeFt" +
            "vtva0h0oodKRC3dCkQLnEWPGi7mEKB98cRhZmQ1Wa9A9tg3plKsujwwWskaFEL/h" +
            "O7uu7myF0qLIeuiG6wIDAQAB";// NOI18N

        private static final String EXTENSION_PATH = "modules/lib/netbeans-chrome-connector.crx"; // NOI18N

        @Override
        public BrowserFamilyId getBrowserFamilyId() {
            return BrowserFamilyId.CHROME;
        }

        /* (non-Javadoc)
         * @see org.netbeans.modules.web.plugins.ExtensionManagerAccessor.BrowserExtensionManager#isInstalled()
         */
        @Override
        public ExtensionManager.ExtensitionStatus isInstalled() {
            while (true) {
                ExtensionManager.ExtensitionStatus result = isInstalledImpl();
                if (result == ExtensionManager.ExtensitionStatus.DISABLED) {
                    NotifyDescriptor descriptor = new NotifyDescriptor.Message(
                            NbBundle.getMessage(ChromeExtensionManager.class,
                                    "LBL_ChromePluginIsDisabled"),                   // NOI18N
                                        NotifyDescriptor.ERROR_MESSAGE);
                    descriptor.setTitle(NbBundle.getMessage(ChromeExtensionManager.class,
                            "TTL_ChromePluginIsDisabled"));                             // NOI18N
                    if (DialogDisplayer.getDefault().notify(descriptor) != DialogDescriptor.OK_OPTION) {
                        return result;
                    }
                    continue;
                }
                return result;
            }
        }

        private ExtensionManager.ExtensitionStatus isInstalledImpl() {
            JSONObject preferences = findPreferences();
            if (preferences == null) {
                return ExtensionManager.ExtensitionStatus.MISSING;
            }
            JSONObject extensions = (JSONObject)preferences.get("extensions");
            if (extensions == null) {
                return ExtensionManager.ExtensitionStatus.MISSING;
            }
            JSONObject settings = (JSONObject)extensions.get("settings");
            if (settings == null) {
                return ExtensionManager.ExtensitionStatus.MISSING;
            }
            for (Object item : settings.entrySet()) {
                Map.Entry e = (Map.Entry)item;
                JSONObject extension = (JSONObject)e.getValue();
                if (extension != null) {
                    String path = (String)extension.get("path");
                    if (path != null && (path.indexOf("/extbrowser.chrome/plugins/chrome") != -1
                            || path.indexOf("\\extbrowser.chrome\\plugins\\chrome") != -1))
                    {
                        return ExtensionManager.ExtensitionStatus.INSTALLED;
                    }
                    JSONObject manifest = (JSONObject)extension.get("manifest");
                    if (manifest != null && PLUGIN_PUBLIC_KEY.equals((String)manifest.get("key"))) {
                        String version = (String)manifest.get("version");
                        if (isUpdateRequired( version )){
                            return ExtensionManager.ExtensitionStatus.NEEDS_UPGRADE;
                        }
                        Number n = (Number)extension.get("state");
                        if (n != null && n.intValue() != 1) {
                            return ExtensionManager.ExtensitionStatus.DISABLED;
                        }
                        return ExtensionManager.ExtensitionStatus.INSTALLED;
                    }
                }
            }
            return ExtensionManager.ExtensitionStatus.MISSING;
        }

        private JSONObject findPreferences() {
            File defaultProfile = getDefaultProfile();
            if (defaultProfile == null) {
                return null;
            }

            String[] prefFiles = new String[]{"secure preferences", "protected preferences", "preferences"}; // NOI18N
            for (String prefFile : prefFiles) {
                File[] prefs = defaultProfile.listFiles(new FileFinder(prefFile));
                if (prefs != null
                        && prefs.length > 0) {
                    JSONObject preferences = Utils.readFile(prefs[0]);
                    if (preferences != null
                            && preferences.get("extensions") != null) { // NOI18N
                        return preferences;
                    }
                }
            }
            return null;
        }

        @Override
        public boolean install( ExtensionManager.ExtensitionStatus currentStatus )
        {
            File extensionFile = InstalledFileLocator.getDefault().locate(
                    EXTENSION_PATH,PLUGIN_MODULE_NAME, false);

            if ( extensionFile == null ){
                Logger.getLogger(ChromeExtensionManager.class.getCanonicalName()).
                    severe("Could not find chrome extension in installation directory");   // NOI18N
                return false;
            }

            String useManualInstallation = System
                    .getProperty(NO_WEB_STORE_SWITCH);
            if (useManualInstallation != null) {
                return manualInstallPluginDialog(currentStatus, extensionFile);
            }
            else {
                return alertGoogleWebStore(currentStatus);
            }


           /* NotifyDescriptor installDesc = new NotifyDescriptor.Confirmation(
                    NbBundle.getMessage(ChromeExtensionManager.class,
                            currentStatus == ExtensionManager.ExtensitionStatus.MISSING ?
                        "LBL_InstallMsg" : "LBL_UpgradeMsg"),                                  // NOI18N
                    NbBundle.getMessage(ChromeExtensionManager.class,
                            "TTL_InstallExtension"),                            // NOI18N
                    NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.QUESTION_MESSAGE);
            Object result = DialogDisplayer.getDefault().notify(installDesc);
            if (result != NotifyDescriptor.OK_OPTION) {
                return false;
            }

            try {
                loader.requestPluginLoad( new URL("file:///"+extensionFile.getCanonicalPath()));
            }
            catch( IOException e ){
                Logger.getLogger( ChromeExtensionManager.class.getCanonicalName()).
                    log(Level.INFO , null ,e );
                return false;
            }
            return true;*/
        }

        @Override
        protected String getCurrentPluginVersion(){
            File extensionFile = InstalledFileLocator.getDefault().locate(
                    EXTENSION_PATH,PLUGIN_MODULE_NAME, false);
            if (extensionFile == null) {
                Logger.getLogger(ChromeExtensionManager.class.getCanonicalName()).
                    info("Could not find chrome extension in installation directory!"); // NOI18N
                return null;
            }
            String content = Utils.readZip( extensionFile, "manifest.json");    // NOI18N
            int index = content.indexOf(VERSION);
            if ( index == -1){
                return null;
            }
            index = content.indexOf(',',index);
            return getValue(content, 0, index , VERSION);
        }

        private String getValue(String content, int start , int end , String key){
            String part = content.substring( start , end );
            int index = part.indexOf(key);
            if ( index == -1 ){
                return null;
            }
            String value = part.substring( index +key.length() ).trim();
            return Utils.unquote(value);
        }

        private File getDefaultProfile() {
            String[] userData = getUserData();
            if ( userData != null ){
                for (String dataDir : userData) {
                    File dir = new File(dataDir);
                    if (dir.isDirectory() && dir.exists()) {
                        File[] localState = dir.listFiles(
                                new FileFinder("local state"));         // NOI18N
                        boolean guessDefault = localState == null ||
                                    localState.length == 0;

                        if ( !guessDefault ) {
                            JSONObject localStateContent = Utils.readFile( localState[0]);
                            if (localStateContent != null) {
                                JSONObject profile = (JSONObject)localStateContent.get("profile");
                                if (profile != null) {
                                    String prof = (String)profile.get("last_used");
                                    if (prof != null) {
                                        prof = Utils.unquote(prof);
                                        File[] listFiles = dir.listFiles( new FileFinder(
                                                prof , true));
                                        if ( listFiles != null && listFiles.length >0 ){
                                            return listFiles[0];
                                        } else {
                                            guessDefault = true;
                                        }
                                    }
                                }
                            }
                        }

                        if( guessDefault ) {
                            File[] listFiles = dir.listFiles(
                                    new FileFinder("default"));  // NOI18N
                            if ( listFiles!= null && listFiles.length >0 ) {
                                    return listFiles[0];
                            }
                        }

                    }
                }
            }
            return null;
        }

        protected String[] getUserData(){
            // see http://www.chromium.org/user-experience/user-data-directory
            // TODO - this will not work for Chromium on Windows and Mac
            if (Utilities.isWindows()) {
                ArrayList<String> result = new ArrayList<String>();
                String localAppData = System.getenv("LOCALAPPDATA");                // NOI18N
                if (localAppData != null) {
                    result.add(localAppData+"\\Google\\Chrome\\User Data");
                } else {
                    localAppData = Utils.getLOCALAPPDATAonWinXP();
                    if (localAppData != null) {
                        result.add(localAppData+"\\Google\\Chrome\\User Data");
                    }
                }
                String appData = System.getenv("APPDATA");                // NOI18N
                if (appData != null) {
                    // we are in C:\Documents and Settings\<username>\Application Data\ on XP
                    File f = new File(appData);
                    if (f.exists()) {
                        String fName = f.getName();
                        // #219824 - below code will not work on some localized WinXP where
                        //    "Local Settings" name might be "Lokale Einstellungen";
                        //     no harm if we try though:
                        f = new File(f.getParentFile(),"Local Settings");
                        f = new File(f, fName);
                        if (f.exists()) {
                            result.add(f.getPath()+"\\Google\\Chrome\\User Data");
                        }
                    }
                }
                return result.toArray(new String[result.size()]);
            }
            else if (Utilities.isMac()) {
                return Utils.getUserPaths("/Library/Application Support/Google/Chrome");// NOI18N
            }
            else {
                return Utils.getUserPaths("/.config/google-chrome", "/.config/chrome");// NOI18N
            }
        }

        private boolean manualInstallPluginDialog(
                ExtensionManager.ExtensitionStatus currentStatus,
                File extensionFile )
        {
            String path = null;
            try {
                path = extensionFile.getCanonicalPath();
            }
            catch( IOException e ){
                Logger.getLogger( ChromeExtensionManager.class.getCanonicalName()).
                    log(Level.INFO , null ,e );
                return false;
            }
            JButton continueButton = new JButton(NbBundle.getMessage(
                    ChromeExtensionManager.class,
                    currentStatus == ExtensionManager.ExtensitionStatus.NEEDS_UPGRADE ?
                    "LBL_ContinueUpdate" : "LBL_Continue"));                // NOI18N
            continueButton.getAccessibleContext().setAccessibleName(NbBundle.
                    getMessage(ChromeExtensionManager.class, "ACSN_Continue"));    // NOI18N
            continueButton.getAccessibleContext().setAccessibleDescription(NbBundle.
                    getMessage(ChromeExtensionManager.class, "ACSD_Continue"));    // NOI18N
            DialogDescriptor descriptor = new DialogDescriptor(
                    new ChromeInfoPanel(path, currentStatus),
                    NbBundle.getMessage(ChromeExtensionManager.class,
                            currentStatus == ExtensionManager.ExtensitionStatus.NEEDS_UPGRADE ?
                    "TTL_UpdateExtension" : "TTL_InstallExtension"), true,
                    new Object[]{continueButton,
                            DialogDescriptor.CANCEL_OPTION}, continueButton,
                            DialogDescriptor.DEFAULT_ALIGN, null, null);
            InstallInfoReceiver receiver = new InstallInfoReceiver();
            ExternalBrowserPlugin.getInstance().addMessageListener(receiver);
            while (true) {
                Object result = DialogDisplayer.getDefault().notify(descriptor);
                if (result == continueButton) {
                    if ( receiver.isInstalled() ){
                        return true;
                    }
                    ExtensitionStatus status = isInstalled();
                    if ( status!= ExtensitionStatus.INSTALLED){
                        continue;
                    } else {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }

        private boolean alertGoogleWebStore(
                ExtensionManager.ExtensitionStatus currentStatus)
        {
            // #221325
            if (currentStatus == ExtensionManager.ExtensitionStatus.MISSING) {
                return alertGoogleWebStoreInstall(currentStatus);
            }
            // update
            return alertGoogleWebStoreUpdate(currentStatus);
        }

        private boolean alertGoogleWebStoreInstall(final
                ExtensionManager.ExtensitionStatus currentStatus)
        {
            final File extensionFile = InstalledFileLocator.getDefault().locate(
                    EXTENSION_PATH,PLUGIN_MODULE_NAME, false);
            String path="";
            try {
                path = extensionFile.getParentFile().toURI().toURL().toExternalForm();
            }
            catch( MalformedURLException e ){
                Logger.getLogger(ChromeExtensionManager.class.getName()).log(
                        Level.WARNING, null, e);
            }
            final Dialog[] dialogs = new Dialog[1];
            final boolean result[] = new boolean[1];
            DialogDescriptor descriptor = new DialogDescriptor(
                    new WebStorePanel(false, path, new Runnable() {

                        @Override
                        public void run() {
                            InstallInfoReceiver receiver = new InstallInfoReceiver();
                            ExternalBrowserPlugin.getInstance().
                                addMessageListener(receiver);
                            try {
                                // #228605
                                openInProperBrowser(URI.create(PLUGIN_PAGE).toURL());
                            }
                            catch( MalformedURLException e ){
                                Logger.getLogger(ChromeExtensionManager.class.getName()).log(
                                        Level.WARNING, null, e);
                            }
                            dialogs[0].setVisible(false);
                            dialogs[0].dispose();
                            result[0] = createReRun(currentStatus, extensionFile,
                                    receiver);
                        }

                        private void openInProperBrowser(URL url) {
                            for (WebBrowser browser : WebBrowsers.getInstance().getAll(true, true, true)) {
                                if (browser.hasNetBeansIntegration()) {
                                    // ignore it otherwise it will check the extension status and reopen just the same dialog
                                    continue;
                                }
                                if (browser.getBrowserFamily() == getBrowserFamilyId()) {
                                    browser.createNewBrowserPane().showURL(url);
                                    return;
                                }
                            }
                            // fallback
                            HtmlBrowser.URLDisplayer.getDefault().showURL(url);
                        }

                    }, new Runnable() {

                        @Override
                        public void run() {
                            dialogs[0].setVisible(false);
                            dialogs[0].dispose();
                            result[0] = manualInstallPluginDialog(currentStatus,
                                    extensionFile);
                        }
                    }),
                    NbBundle.getMessage(ChromeExtensionManager.class,
                            "TTL_InstallExtension"), true,
                    new Object[]{DialogDescriptor.CANCEL_OPTION},
                        DialogDescriptor.CANCEL_OPTION,
                            DialogDescriptor.DEFAULT_ALIGN, null, null);
            Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
            dialogs[0] = dialog;
            dialog.setVisible(true);
            return result[0];
        }

        private boolean createReRun(final
                ExtensionManager.ExtensitionStatus currentStatus,
                final File extensionFile, final InstallInfoReceiver receiver)
        {
            final Dialog[] dialogs = new Dialog[1];
            final boolean result[] = new boolean[1];
            DialogDescriptor descriptor = new DialogDescriptor(
                    new WebStorePanel(true, null, new Runnable() {

                        @Override
                        public void run() {
                            ExtensitionStatus status = isInstalled();
                            if ( receiver.isInstalled() ||
                                    status== ExtensitionStatus.INSTALLED)
                            {
                                result[0] = true;
                                dialogs[0].setVisible(false);
                                dialogs[0].dispose();
                            }
                        }
                    }, new Runnable() {

                        @Override
                        public void run() {
                            dialogs[0].setVisible(false);
                            dialogs[0].dispose();
                            result[0] = manualInstallPluginDialog(currentStatus, extensionFile);
                        }
                    }),
                    NbBundle.getMessage(ChromeExtensionManager.class,
                            "TTL_InstallExtension"), true,
                    new Object[]{DialogDescriptor.CANCEL_OPTION},
                        DialogDescriptor.CANCEL_OPTION,
                            DialogDescriptor.DEFAULT_ALIGN, null, null);
            Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
            dialogs[0] = dialog;
            dialog.setVisible(true);
            return result[0];
        }

        private boolean alertGoogleWebStoreUpdate(
                final ExtensionManager.ExtensitionStatus currentStatus)
        {
            final Dialog[] dialogs = new Dialog[1];
            final boolean[] result = new boolean[1];
            DialogDescriptor descriptor = new DialogDescriptor(
                    new WebStorePanel( new Runnable() {

                        @Override
                        public void run() {
                            ExtensitionStatus status = isInstalled();
                            if ( status!= ExtensitionStatus.INSTALLED){
                                return;
                            }
                            result[0] = true;
                            dialogs[0].setVisible(false);
                            dialogs[0].dispose();
                        }
                    } ),
                    NbBundle.getMessage(ChromeExtensionManager.class, "TTL_UpdateExtension"),
                    true,
                    new Object[]{DialogDescriptor.CANCEL_OPTION},
                    DialogDescriptor.CANCEL_OPTION,
                            DialogDescriptor.DEFAULT_ALIGN, null, null);
            Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
            dialogs[0] = dialog;
            dialog.setVisible( true);
            return result[0];
        }

        static private class FileFinder implements FileFilter {
            FileFinder(String name){
                this( name, false );
            }

            FileFinder(String name , boolean caseSensitive ){
                myName = name;
                isCaseSensitive = caseSensitive;
            }

            /* (non-Javadoc)
             * @see java.io.FileFilter#accept(java.io.File)
             */
            @Override
            public boolean accept( File file ) {
                if ( isCaseSensitive ){
                    return file.getName().equals( myName);
                }
                else {
                    return file.getName().toLowerCase(Locale.US).equals( myName);
                }
            }
            private String myName;
            private boolean isCaseSensitive;
        }
    }

    static class InstallInfoReceiver implements MessageListener {

        /* (non-Javadoc)
         * @see org.netbeans.modules.extbrowser.plugins.MessageListener#messageReceived(org.netbeans.modules.extbrowser.plugins.Message)
         */
        @Override
        public void messageReceived( Message message ) {
            if ( message.getType()==MessageType.READY){
                isInstalled = true;
            }
        }

        public boolean isInstalled(){
            return isInstalled;
        }

        private volatile boolean isInstalled;
    }
}
