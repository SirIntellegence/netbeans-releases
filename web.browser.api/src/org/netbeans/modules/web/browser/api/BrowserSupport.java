/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.web.browser.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.web.browser.api.WebBrowserPane.WebBrowserPaneEvent;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Helper class to be added to project's lookup and to be used to open URLs from
 * project. It keeps association between URL opened in browser and project's file.
 * According to results of DependentFileQuery it can answer whether project's file
 * change should result into refresh of URL in the browser. It also keeps 
 * WebBrowserPane instance to open URLs in. Such browser pane can be single global one
 * shared with HtmlBrowser.URLDisaplyer (ie. getGlobalSharedOne method) or single
 * global one owned by BrowserSupport (ie. getDefault method) or per project
 * pane (ie. getProjectScoped method).
 */
@NbBundle.Messages({
    "AttemptToDebug=Attempting to debug {0}",
    "AlreadyRunning=Reusing active debugging session",
    "StartingDebugger=Starting new debugging session",
    "BrowserReady=Browser is up and running",
    "BrowserUnknownState=Cannot talk to browser via NetBeans browser plugin. Debugging aborted.",
    "WaitingForBrowser=Waiting for browser to start"
    })
public final class BrowserSupport {
    
    private WebBrowserPane pane;
    
    private URL currentURL;
    private WebBrowserPane.WebBrowserPaneListener paneListener;
    private WebBrowser browser;
    private PropertyChangeListener listener;
    private FileObject file;
    private boolean activeDebuggingSession = false;

    private static BrowserSupport INSTANCE = create();
    
    /**
     * Returns instance of BrowserSupport which shares WebBrowserPane
     * with HtmlSupport.URLDisplayer. That means that opening a URL via
     * BrowserSupport.load() or via HtmlSupport.URLDisaplayer.show() will
     * results into URL being opened in the same browser pane.
     */
    public static synchronized BrowserSupport getGlobalSharedOne() {
        // XXX: to implement this I need to hack in NbDisplayerURL
        throw new UnsupportedOperationException("not implemented yet");
    }
    
    /**
     * Returns singleton instance of BrowserSupport with its own WebBrowserPane.
     * Using this instance means that all URLs opened with BrowserSupport will 
     * have its own browser pane and all URLs opened via HtmlSupport.URLDisplayer
     * will have its own browser pane as well. The browser used to open URLs is
     * always the one configured in IDE Options.
     */
    public static BrowserSupport getDefault() {
        return INSTANCE;
    }

    /**
     * Creates a new instance of BrowserSupport which will always use browser
     * according to IDE Options.
     */
    public static BrowserSupport create() {
        return new BrowserSupport();
    }
    
    /**
     * Creates a new instance of BrowserSupport for given browser.
     */
    public static BrowserSupport create(WebBrowser browser) {
        return new BrowserSupport(browser);
    }
    /**
     * Use browser from IDE settings and change browser pane whenever default
     * browser changes in IDE options.
     */
    private BrowserSupport() {
        this(null);
    }
    
    private BrowserSupport(WebBrowser browser) {
        this.browser = browser;
    }
    
    private synchronized WebBrowserPane getWebBrowserPane() {
        if (pane == null) {
            if (browser == null) {
                browser = WebBrowsers.getInstance().getPreferred();
                listener = new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        synchronized ( BrowserSupport.this ) {
                            if (WebBrowsers.PROP_DEFAULT_BROWSER.equals(evt
                                    .getPropertyName()))
                            {
                                if (!WebBrowsers.getInstance().getPreferred()
                                        .getId().equals(browser.getId()))
                                {
                                    // update browser pane
                                    browser = WebBrowsers.getInstance()
                                            .getPreferred();
                                    if ( pane!= null ){
                                        // pane could be null because of the following code
                                        pane.removeListener(paneListener);
                                        if (activeDebuggingSession) {
                                            activeDebuggingSession = false;
                                            BrowserDebugger
                                                    .stopDebuggingSession(pane);
                                        }
                                        pane = null;
                                    }
                                }
                            }
                        }
                    }
                };
                WebBrowsers.getInstance().addPropertyChangeListener(listener);
            }
            pane = browser.createNewBrowserPane();
            paneListener = new ListenerImpl();
            pane.addListener(paneListener);
        }
        return pane;
    }
    
    /**
     * Opens URL in a browser pane associated with this BrowserSupport.
     * FileObject param is "context" associated with URL being opened. It can be 
     * file which is being opened in the browser or  project folder in case of URL being result
     * of project execution. If browser pane does not support concept of reloading it will simply 
     * open a tab with this URL on each execution.
     * 
     */
    public void load(URL url, FileObject context) {
        WebBrowserPane wbp = getWebBrowserPane();
        file = context;
        currentURL = url;
        wbp.showURL(url);
        if (BrowserDebugger.isDebuggingEnabled(pane)) {
            BrowserDebugger.getOutputLogger().getOut().println(Bundle.AttemptToDebug(url.toExternalForm()));
            startDebugger(url, FileOwnerQuery.getOwner(context));
        }
    }
    
    private void startDebugger(final URL url, final Project p) {
        assert BrowserDebugger.isDebuggingEnabled(pane);
        final String tabToDebug = url.toExternalForm();
        if (!activeDebuggingSession) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    BrowserDebugger.getOutputLogger().getOut().println(Bundle.StartingDebugger());
                    boolean res = BrowserDebugger.startDebuggingSession(p, pane, tabToDebug);
                    if (res) {
                        activeDebuggingSession = true;
                    }
                }
            };

            final WebBrowserPane.WebBrowserPaneListener l = new WebBrowserPane.WebBrowserPaneListener() {
                @Override
                public void browserEvent(WebBrowserPaneEvent event) {
                    if (event instanceof WebBrowserPane.WebBrowserRunningStateChangedEvent) {
                        pane.removeListener(this);
                        if (Boolean.TRUE.equals(pane.isRunning())) {
                            // try to start debugger again; will be harmless if debugger is already running
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    startDebugger(url, p);
                                }
                            });
                        }
                    }
                }
            };
            pane.addListener(l);

            Boolean browserRuns = getWebBrowserPane().isRunning();
            if (browserRuns == Boolean.TRUE) {
                BrowserDebugger.getOutputLogger().getOut().println(Bundle.BrowserReady());
                r.run();
            } else if (browserRuns == null) {
                BrowserDebugger.getOutputLogger().getOut().println(Bundle.BrowserUnknownState());
                // we do know know anything about browser;
                // perhaps I could just wait two seconds and try to connect to
                // browser and repeat this five times before giving up.
                // there might be many causes of the failure, eg. browser is still starting;
                // browser is not running with debugging port; etc.
            } else {
                BrowserDebugger.getOutputLogger().getOut().println(Bundle.WaitingForBrowser());
                // listener defined above will take care about this case
                // and start debugger once the browser has started
            }
        } else {
            BrowserDebugger.getOutputLogger().getOut().println(Bundle.AlreadyRunning());
        }
    }
    
    /**
     * The same behaviour as load() method but file object context is not necessary
     * to be passed again.
     */
    public boolean reload(URL url) {
        if (!canReload(url)) {
            return false;
        }
        getWebBrowserPane().reload();
        return true;
    }
    
    /**
     * Has this URL being previous opened via load() method or not? BrowserSupport
     * remember last URL opened.
     */
    public boolean canReload(URL url) {
        return currentURL != null && currentURL.equals(url);
    }
    
         /**
     * Returns URL which was opened in the browser and which was associated with
     * given FileObject. That is calling load(URL, FileObject) creates mapping 
     * between FileObject in IDE side and URL in browser side and this method 
     * allows to use the mapping to retrieve URL.
     * 
     * If checkDependentFiles parameter is set to true then 
     * DependentFileQuery.isDependent will be consulted to check whether URL opened
     * in the browser does not depend on given FileObject. If answer is yes than
     * any change in this FileObject should be reflected in browser and URL
     * should be refreshed in browser.
     */
    public URL getBrowserURL(FileObject fo, boolean checkDependentFiles) {
        Project project = FileOwnerQuery.getOwner(fo);
        if (file == null || currentURL == null) {
            return null;
        }
        if (checkDependentFiles) {
            if ( file.equals( project ) || DependentFileQuery.isDependent(file, fo)) {
                // Two cases :
                // - a project was "Run" and we have no idea which exact project's 
                //   file was opened in browser;
                //   because "fo" belongs to the project we could say
                //   that URL corresponding for this fo is project's URL;
                //   let's first check other opened browsers for better match and
                //   if nothing better is found we can return project's URL
                // - <code>file</code> depends on <code>fo</code>
                return currentURL;
            }
        } 
        if (fo.equals(file)) {
            return currentURL;
        }
        return null;
    }
    
    private class ListenerImpl implements WebBrowserPane.WebBrowserPaneListener {

        @Override
        public void browserEvent(WebBrowserPane.WebBrowserPaneEvent event) {
            if (event instanceof WebBrowserPane.WebBrowserPaneWasClosedEvent) {
                currentURL = null;
                file = null;
                if (activeDebuggingSession) {
                    activeDebuggingSession = false;
                    BrowserDebugger.stopDebuggingSession(pane);
                }
            }
        }
        
    }
    
}
