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
package org.netbeans.core;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

/**
 *
 * @author Jirka Rechtacek
 */
public class ProxyAutoConfig {

    private static final Map<String, ProxyAutoConfig> file2pac = new HashMap<String, ProxyAutoConfig>(2);
    private static RequestProcessor RP = new RequestProcessor(ProxyAutoConfig.class);
    private static final String NS_PROXY_AUTO_CONFIG_URL = "nbinst://org.netbeans.core/modules/ext/nsProxyAutoConfig.js"; // NOI18N

    public static synchronized ProxyAutoConfig get(String pacFile) {
        if (file2pac.get(pacFile) == null) {
            LOGGER.fine("Init ProxyAutoConfig for " + pacFile);
            file2pac.put(pacFile, new ProxyAutoConfig(pacFile));
        }
        return file2pac.get(pacFile);
    }
    private static final Logger LOGGER = Logger.getLogger(ProxyAutoConfig.class.getName());
    private Invocable inv = null;
    private final Task initTask;
    private final URI pacURI;

    private ProxyAutoConfig(final String pacURL) {
        assert file2pac.get(pacURL) == null : "Only once object for " + pacURL + " must exist.";
        try {
            pacURI = new URI(pacURL);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(pacURL + " throws " + ex, ex);
        }
        initTask = RP.post(new Runnable() {

            @Override
            public void run() {
                initEngine(pacURL);
            }
        });
    }

    public URI getPacURI() {
        return pacURI;
    }

    private void initEngine(String pacURL) {
        InputStream pacIS;
        try {
            pacIS = downloadPAC(pacURL);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "InputStream for " + pacURL + " throws " + ex, ex);
            return;
        }
        assert pacIS != null : "No InputStream for " + pacURL;
        if (pacIS == null) {
            return ;
        }
        String utils = downloadUtils();
        ScriptEngine eng;
        try {
            eng = evalPAC(pacIS, utils);
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.FINE, "While constructing ProxyAutoConfig thrown " + ex, ex);
            throw new IllegalArgumentException(ex);
        } catch (ScriptException ex) {
            LOGGER.log(Level.FINE, "While constructing ProxyAutoConfig thrown " + ex, ex);
            throw new IllegalArgumentException(ex);
        } finally {
            if (pacIS != null) {
                try {
                    pacIS.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.FINE, "While closing PAC input stream thrown " + ex, ex);
                }
            }
        }
        assert eng != null : "JavaScri5pt engine cannot be null";
        if (eng == null) {
            throw new IllegalArgumentException("JavaScript engine cannot be null");
        }
        inv = (Invocable) eng;
    }

    @SuppressWarnings("unchecked")
    public List<Proxy> findProxyForURL(URI u) {
        assert initTask != null : "initTask has be to posted.";
        if (!initTask.isFinished()) {
            while (!initTask.isFinished()) {
                try {
                    RP.post(new Runnable() {

                        @Override
                        public void run() {
                        }
                    }).waitFinished(100);
                } catch (InterruptedException ex) {
                    LOGGER.log(Level.FINEST, ex.getMessage(), ex);
                }
            }
        }
        if (inv == null) {
            return Collections.singletonList(Proxy.NO_PROXY);
        }
        Object proxies = null;
        try {
            proxies = inv.invokeFunction("FindProxyForURL", u.toString(), u.getHost()); // NOI18N
        } catch (ScriptException ex) {
            LOGGER.log(Level.FINE, "While invoking FindProxyForURL(" + u + ", " + u.getHost() + " thrown " + ex, ex);
        } catch (NoSuchMethodException ex) {
            LOGGER.log(Level.FINE, "While invoking FindProxyForURL(" + u + ", " + u.getHost() + " thrown " + ex, ex);
        }
        List<Proxy> res = analyzeResult(u, proxies);
        if (res == null) {
            LOGGER.info("findProxyForURL(" + u + ") returns null.");
            res = Collections.emptyList();
        }
        LOGGER.fine("findProxyForURL(" + u + ") returns " + Arrays.asList(res));
        return res;
    }

    private static InputStream downloadPAC(String pacURL) throws IOException {
        InputStream is = null;
        URL url = null;
        try {
            url = new URL(pacURL);
        } catch (MalformedURLException ex) {
            LOGGER.log(Level.INFO, "Malformed " + pacURL, ex);
            return null;
        }
        URLConnection conn = url.openConnection(Proxy.NO_PROXY);
        is = conn.getInputStream();
        return is;
    }

    private static ScriptEngine evalPAC(InputStream is, String utils) throws FileNotFoundException, ScriptException {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        Reader pacReader = new InputStreamReader(is);
        engine.eval(pacReader);
        engine.eval(utils);
        return engine;
    }

    private List<Proxy> analyzeResult(URI uri, Object proxiesString) {
        if (proxiesString == null) {
            LOGGER.fine("Null result for " + uri);
            return null;
        }
        Proxy.Type proxyType;
        String protocol = uri.getScheme();
        assert protocol != null : "Invalid scheme of uri " + uri + ". Scheme cannot be null!";
        if (protocol == null) {
            return null;
        } else {
            if ("http".equals(protocol)) { // NOI18N
                proxyType = Proxy.Type.HTTP;
            } else {
                proxyType = Proxy.Type.SOCKS;
            }
        }
        StringTokenizer st = new StringTokenizer(proxiesString.toString(), ";"); //NOI18N
        List<Proxy> proxies = new LinkedList<Proxy>();
        while (st.hasMoreTokens()) {
            String proxy = st.nextToken();
            if (ProxySettings.DIRECT.equals(proxy.trim())) {
                proxies.add(Proxy.NO_PROXY);
            } else {
                String host = getHost(proxy);
                Integer port = getPort(proxy);
                if (host != null && port != null) {
                    proxies.add(new Proxy(proxyType, new InetSocketAddress(host, port)));
                }
            }
        }
        return proxies;
    }

    private static String getHost(String proxy) {
        if (proxy.startsWith("PROXY ")) {
            proxy = proxy.substring(6);
        }
        int i = proxy.lastIndexOf(":"); // NOI18N
        if (i <= 0 || i >= proxy.length() - 1) {
            LOGGER.info("No port in " + proxy);
            return null;
        }

        String host = proxy.substring(0, i);

        return ProxySettings.normalizeProxyHost(host);
    }

    private static Integer getPort(String proxy) {
        if (proxy.startsWith("PROXY ")) {
            proxy = proxy.substring(6);
        }
        int i = proxy.lastIndexOf(":"); // NOI18N
        if (i <= 0 || i >= proxy.length() - 1) {
            LOGGER.info("No port in " + proxy);
            return null;
        }

        String port = proxy.substring(i + 1);
        if (port.indexOf('/') >= 0) {
            port = port.substring(0, port.indexOf('/'));
        }

        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            LOGGER.log(Level.INFO, ex.getLocalizedMessage(), ex);
            return null;
        }
    }

    private static String downloadUtils() {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = null;
        // XXX why is the below not more simply:
        // reader = new BufferedReader(new URL(NS_PROXY_AUTO_CONFIG_URL).openStream());
        FileObject fo = null;
        try {
            try {
                fo = URLMapper.findFileObject(new URL(NS_PROXY_AUTO_CONFIG_URL));
            } catch (MalformedURLException ex) {
                LOGGER.log(Level.INFO, ex.getMessage(), ex);
            }
            reader = new BufferedReader(new java.io.FileReader(FileUtil.toFile(fo)));
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.INFO, ex.getMessage(), ex);
        }
        try {
            String line = null;
            boolean doAppend = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if( line.startsWith("var pacUtils =") ) { //NOI18N
                    doAppend = true;
                }
                if( !doAppend )
                    continue;
                if (line.endsWith("+")) { // NOI18N
                    line = line.substring(0, line.length() - 1);
                }
                builder.append(line.replaceAll("\"", "").replaceAll("\\\\n", "").replaceAll("\\\\\\\\", "\\\\")); // NOI18N
                builder.append(System.getProperty("line.separator")); // NOI18N
            }
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "While downloading nsProxyAutoConfig.js thrown " + ex.getMessage(), ex);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    LOGGER.log(Level.FINE, ex.getMessage(), ex);
                }
            }
        }
        return builder.toString();
    }
}
