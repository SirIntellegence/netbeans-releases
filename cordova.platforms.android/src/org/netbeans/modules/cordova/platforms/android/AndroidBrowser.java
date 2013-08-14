/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.cordova.platforms.android;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.api.project.Project;
import org.netbeans.modules.cordova.platforms.spi.Device;
import org.netbeans.modules.cordova.platforms.api.WebKitDebuggingSupport;
import org.netbeans.modules.web.browser.api.BrowserFamilyId;
import org.netbeans.modules.web.browser.api.BrowserSupport;
import org.netbeans.modules.web.browser.api.WebBrowserFeatures;
import org.netbeans.modules.web.browser.spi.EnhancedBrowser;
import static org.netbeans.spi.project.ActionProvider.COMMAND_RUN;
import static org.netbeans.spi.project.ActionProvider.COMMAND_RUN_SINGLE;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.HtmlBrowser;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.windows.WindowManager;

/**
 *
 * @author Jan Becicka
 */
public class AndroidBrowser extends HtmlBrowser.Impl implements EnhancedBrowser{
    private final Kind kind;
    
    private static final Logger LOGGER = Logger.getLogger(AndroidBrowser.class.getName());
    
    private Lookup context;

    @Override
    public void initialize(WebBrowserFeatures browserFeatures) {
    }

    @Override
    public void close(boolean closeTab) {
        RequestProcessor.getDefault().post(new Runnable() {
            @Override
            public void run() {
                WebKitDebuggingSupport.getDefault().stopDebugging(true);
            }
        });
    }

    @Override
    public void setProjectContext(Lookup projectContext) {
        context = projectContext;
    }

    @Override
    public boolean canReloadPage() {
        return true;
    }

    @Override
    public boolean ignoreChange(FileObject fo) {
        return BrowserSupport.ignoreChangeDefaultImpl(fo);
    }
    
    public static enum Kind {
        ANDROID_DEVICE_DEFAULT,
        ANDROID_DEVICE_CHROME,
        ANDROID_EMULATOR_DEFAULT
    }
    

    public AndroidBrowser(Kind kind) {
        this.kind = kind;
    }
    
    private URL url;

    @Override
    public Component getComponent() {
        return null;
    }

    @Override
    public void reloadDocument() {
        WebKitDebuggingSupport.getDefault().reload();
    }

    @Override
    public void stopLoading() {
    }

    @Override
    public void setURL(final URL url) {
        final WebKitDebuggingSupport build = WebKitDebuggingSupport.getDefault();

        final String checkAndroid = AndroidActionProvider.checkAndroid();
        if (checkAndroid != null) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    NotifyDescriptor not = new NotifyDescriptor(
                            checkAndroid,
                            Bundle.ERR_Title(),
                            NotifyDescriptor.OK_CANCEL_OPTION,
                            NotifyDescriptor.ERROR_MESSAGE,
                            null,
                            null);
                    Object value = DialogDisplayer.getDefault().notify(not);
                    if (NotifyDescriptor.CANCEL_OPTION != value) {
                        OptionsDisplayer.getDefault().open("Advanced/MobilePlatforms"); // NOI18N
                    }
                }
            });
            return;
        }

        ProgressUtils.runOffEventDispatchThread(new Runnable() {
            @Override
            public void run() {
                String checkDevices = checkDevices();
                while (checkDevices != null) {
                    NotifyDescriptor not = new NotifyDescriptor(
                            checkDevices,
                            Bundle.ERR_Title(),
                            NotifyDescriptor.DEFAULT_OPTION,
                            NotifyDescriptor.ERROR_MESSAGE,
                            null,
                            null);
                    Object value = DialogDisplayer.getDefault().notify(not);
                    if (NotifyDescriptor.CANCEL_OPTION == value) {
                        return;
                    } else {
                        checkDevices = checkDevices();
                    }
                }

                Browser b;
                boolean emulator;
                if (kind.equals(AndroidBrowser.Kind.ANDROID_DEVICE_DEFAULT)) {
                    b = Browser.DEFAULT;
                    emulator = false;
                } else if (kind.equals(AndroidBrowser.Kind.ANDROID_DEVICE_CHROME)) {
                    b = Browser.CHROME;
                    emulator = false;
                } else {
                    b = Browser.DEFAULT;
                    emulator = true;
                }
                Device device = new AndroidDevice("android", b, emulator); // NOI18N

                device.openUrl(url.toExternalForm());

                final Project project = context.lookup(Project.class);
                if (Browser.CHROME.getName().equals(b.getName()) && project != null) {
                    try {
                        build.startDebugging(device, project, new ProxyLookup(context, Lookups.fixed(BrowserFamilyId.ANDROID, url)), false);
                    } catch (IllegalStateException ex) {
                        LOGGER.log(Level.INFO, ex.getMessage(), ex);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(
                                        WindowManager.getDefault().getMainWindow(),
                                        Bundle.ERR_WebDebug());
                            }
                        });
                    }
                }

            }
        }, Bundle.LBL_CheckingDevice(), new AtomicBoolean(), false);
        this.url = url;
    }
    
     public static void openBrowser(String command, final Lookup context, final AndroidBrowser.Kind kind, final Project project, final BrowserSupport support) throws IllegalArgumentException {
        final WebKitDebuggingSupport build = WebKitDebuggingSupport.getDefault();
         if (COMMAND_RUN.equals(command) || COMMAND_RUN_SINGLE.equals(command)) {
            try {
                final URL urL = new URL(build.getUrl(project, context));
                FileObject f = build.getFile(project, context);
                support.load(urL, f);
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            }
         }
    }

    private String checkDevices() {
        try {
            if (kind.equals(AndroidBrowser.Kind.ANDROID_EMULATOR_DEFAULT)) { //NOI18N
                for (Device dev : AndroidPlatform.getDefault().getConnectedDevices()) {
                    if (dev.isEmulator()) {
                        return null;
                    }
                }
                return Bundle.ERR_RunAndroidEmulator();
            } else {
                for (Device dev : AndroidPlatform.getDefault().getConnectedDevices()) {
                    if (!dev.isEmulator()) {
                        return null;
                    }
                }
                return Bundle.ERR_ConnectAndroidDevice();
            }
        } catch (IOException iOException) {
            Exceptions.printStackTrace(iOException);
        }
        return Bundle.ERR_Unknown();
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public String getStatusMessage() {
        return "";
    }

    @Override
    @NbBundle.Messages("LBL_BrowserTitle=Android Browser")
    public String getTitle() {
        return Bundle.LBL_BrowserTitle();
    }

    @Override
    public boolean isForward() {
        return false;
    }

    @Override
    public void forward() {
    }

    @Override
    public boolean isBackward() {
        return false;
    }

    @Override
    public void backward() {
    }

    @Override
    public boolean isHistory() {
        return false;
    }

    @Override
    public void showHistory() {
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }
    
}
