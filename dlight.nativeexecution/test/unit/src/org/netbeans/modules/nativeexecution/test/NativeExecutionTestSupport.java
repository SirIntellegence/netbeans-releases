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

package org.netbeans.modules.nativeexecution.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.TestSuite;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.util.ConnectionManager;
import org.netbeans.modules.nativeexecution.api.util.PasswordManager;
import org.netbeans.modules.nativeexecution.test.RcFile.FormatException;

/**
 *
 * @author vk155633
 */
public class NativeExecutionTestSupport {

    private static ExecutionEnvironment defaultTestExecutionEnvironment;
    private static RcFile rcFile;
    private static final Map<String, ExecutionEnvironment> spec2env = new LinkedHashMap<String, ExecutionEnvironment>();
    private static final Map<ExecutionEnvironment, String> env2spec = new LinkedHashMap<ExecutionEnvironment, String>();

    private NativeExecutionTestSupport() {
    }

    public static synchronized RcFile getRcFile() throws IOException, RcFile.FormatException {
        if (rcFile == null) {
            String rcFileName = System.getProperty("cnd.remote.rcfile"); // NOI18N
            if (rcFileName == null) {
                String homePath = System.getProperty("user.home");
                if (homePath != null) {
                    File homeDir = new File(homePath);
                    rcFile = new RcFile(new File(homeDir, ".cndtestrc"));
                }
            } else {
                rcFile = new RcFile(new File(rcFileName));
            }
        }
        return rcFile;
    }

    /**
     * Gets old-style default test execution environment -
     * i.e. the one that is set via -J-Dcnd.remote.testuserinfo
     * or CND_REMOTE_TESTUSERINFO environment variable
     */
    public static ExecutionEnvironment getDefaultTestExecutionEnvironment(boolean connect) throws IOException, CancellationException {
        synchronized(NativeExecutionBaseTestCase.class) {
            if (defaultTestExecutionEnvironment == null) {
                String ui = System.getProperty("cnd.remote.testuserinfo"); // NOI18N
                char[] passwd = null;
                if( ui == null ) {
                    ui = System.getenv("CND_REMOTE_TESTUSERINFO"); // NOI18N
                }
                if (ui != null) {
                    int m = ui.indexOf(':');
                    if (m>-1) {
                        int n = ui.indexOf('@');
                        String strPwd = ui.substring(m+1, n);
                        String remoteHKey = ui.substring(0,m) + ui.substring(n);
                        defaultTestExecutionEnvironment = ExecutionEnvironmentFactory.fromUniqueID(remoteHKey);
                        passwd = strPwd.toCharArray();                        
                    } else {
                        String remoteHKey = ui;
                        defaultTestExecutionEnvironment = ExecutionEnvironmentFactory.fromUniqueID(remoteHKey);
                    }
                } else {
                    defaultTestExecutionEnvironment = ExecutionEnvironmentFactory.createNew(System.getProperty("user.name"), "127.0.0.1"); // NOI18N
                }
                if (defaultTestExecutionEnvironment != null) {
                    if(passwd != null && passwd.length > 0) {
                        PasswordManager.getInstance().storePassword(defaultTestExecutionEnvironment, passwd, false);
                    }
                    
                    if (connect) {
                        ConnectionManager.getInstance().connectTo(defaultTestExecutionEnvironment);
                    } 
                }
            }
        }
        return defaultTestExecutionEnvironment;
    }

    private interface UsetrInfoProcessor {
        /** @return true to proceed, false to cancel */
        boolean processLine(String spec, ExecutionEnvironment env, char[] passwd);
    }

    private static void processTestUserInfo(UsetrInfoProcessor processor) throws IOException {
        String rcFileName = System.getProperty("cnd.remote.testuserinfo.rcfile"); // NOI18N
        File userInfoFile = null;

        if (rcFileName == null) {
            String homePath = System.getProperty("user.home");
            if (homePath != null) {
                File homeDir = new File(homePath);
                userInfoFile = new File(homeDir, ".testuserinfo");
            }
        } else {
            userInfoFile = new File(rcFileName);
        }

        if (userInfoFile == null || ! userInfoFile.exists()) {
            return;
        }

        BufferedReader rcReader = new BufferedReader(new FileReader(userInfoFile));
        String str;
        Pattern infoPattern = Pattern.compile("^([^#].*)[ \t]+(.*)"); // NOI18N
        Pattern pwdPattern = Pattern.compile("([^:]+):(.*)@(.*)"); // NOI18N
        char[] passwd = null;

        while ((str = rcReader.readLine()) != null) {
            Matcher m = infoPattern.matcher(str);
            String spec = null;
            String loginInfo;

            if (m.matches()) {
                spec = m.group(1).trim();
                loginInfo = m.group(2).trim();
            } else {
                continue;
            }

            m = pwdPattern.matcher(loginInfo);
            String remoteHKey = null;

            if (m.matches()) {
                passwd = m.group(2).toCharArray();
                remoteHKey = m.group(1) + "@" + m.group(3); // NOI18N
            } else {
                remoteHKey = loginInfo;
            }

            ExecutionEnvironment env = ExecutionEnvironmentFactory.fromUniqueID(remoteHKey);
            if (!processor.processLine(spec, env, passwd)) {
                break;
            }
        }
    }

    public static ExecutionEnvironment[] getTestExecutionEnvironmentsFromSection(String section) throws IOException {
        String[] platforms = getPlatforms(section, null);
        ExecutionEnvironment[] environments = new ExecutionEnvironment[platforms.length];
        for (int i = 0; i < platforms.length; i++) {
            environments[i] = NativeExecutionTestSupport.getTestExecutionEnvironment(platforms[i]);
        }
        return environments;
    }

    /*package*/ static String[] getPlatforms(String section, NbTestSuite suite) {
        try {
            try {
                RcFile rcFile = NativeExecutionTestSupport.getRcFile();
                List<String> result = new ArrayList<String>();
                // We specify environments as just keys in the given section - without values.
                // We also allow specifying some other parameters in the same sections.
                // So we treat a key=value pair as another parameter, not an execution environment
                for (String key : rcFile.getKeys(section)) {
                    String value = rcFile.get(section, key, null);
                    if (value == null) {
                        result.add(key);
                    }
                }
                Collections.sort(result);
                return result.toArray(new String[result.size()]);
            } catch (FileNotFoundException ex) {
                // rcfile does not exists - no tests to run
            }
        } catch (IOException ex) {
            if (suite != null) {
                suite.addTest(TestSuite.warning("Cannot get execution environment: " + exceptionToString(ex)));
            }
        } catch (FormatException ex) {
            if (suite != null) {
                suite.addTest(TestSuite.warning("Cannot get execution environment: " + exceptionToString(ex)));
            }
        }
        return new String[0];
    }

    protected static String exceptionToString(Throwable t) {
            StringWriter stringWriter= new StringWriter();
            PrintWriter writer= new PrintWriter(stringWriter);
            t.printStackTrace(writer);
            return stringWriter.toString();
    }

    public static ExecutionEnvironment getTestExecutionEnvironment(final String mspec) throws IOException {
        if (mspec == null) {
            return null;
        }
        final AtomicReference<ExecutionEnvironment> result = new AtomicReference<ExecutionEnvironment>();
        final AtomicReference<char[]> passwd = new AtomicReference<char[]>();
        processTestUserInfo(new UsetrInfoProcessor() {
            @Override
            public boolean processLine(String spec, ExecutionEnvironment e, char[] p) {
                if (mspec.equals(spec)) {
                    result.set(e);
                    passwd.set(p);
                    return false;
                }
                return true;
            }
        });
        if (result.get() != null) {
            if (passwd.get() != null) {
                PasswordManager.getInstance().storePassword(result.get(), passwd.get(), false);
            }
        }

        spec2env.put(mspec, result.get());
        env2spec.put(result.get(), mspec);
        return result.get();
    }

    public static char[] getTestPassword(final ExecutionEnvironment env) throws IOException {
        if (env == null) {
            return null;
        }
        final AtomicReference<char[]> passwd = new AtomicReference<char[]>();
        processTestUserInfo(new UsetrInfoProcessor() {
            @Override
            public boolean processLine(String spec, ExecutionEnvironment e, char[] p) {
                if (env.equals(e)) {
                    passwd.set(p);
                    return false;
                }
                return true;
            }
        });
        return passwd.get();
    }


    /**
     * Gets an MSpec string, which was used for getting the given environment
     * (i.e. it's an inverse of getTestExecutionEnvironment(String))
     */
    public static String getMspec(ExecutionEnvironment execEnv) {
        return env2spec.get(execEnv);
    }

    public static boolean getBoolean(String condSection, String condKey) {
        return getBoolean(condSection, condKey, false);
    }

    public static boolean getBoolean(String condSection, String condKey, boolean defaultValue) {
        try {
            String value = getRcFile().get(condSection, condKey);
            return (value == null) ? defaultValue : Boolean.parseBoolean(value);
        } catch (FileNotFoundException ex) {
            // silently: just no file => condition is false, that's it
            return defaultValue;
        } catch (IOException ex) {
            return defaultValue;
        } catch (RcFile.FormatException ex) {
            return defaultValue;
        }
    }
}
