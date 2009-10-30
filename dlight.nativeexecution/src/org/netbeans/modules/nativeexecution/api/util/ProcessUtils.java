/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
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
package org.netbeans.modules.nativeexecution.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProcessUtils {

    private ProcessUtils() {
    }

    public static List<String> readProcessError(final Process p) throws IOException {
        if (p == null) {
            return Collections.<String>emptyList();
        }

        return readProcessStream(p.getErrorStream());
    }

    public static String readProcessErrorLine(final Process p) throws IOException {
        if (p == null) {
            return ""; // NOI18N
        }

        return readProcessStreamLine(p.getErrorStream());
    }

    public static List<String> readProcessOutput(final Process p) throws IOException {
        if (p == null) {
            return Collections.<String>emptyList();
        }

        return readProcessStream(p.getInputStream());
    }

    public static String readProcessOutputLine(final Process p) throws IOException {
        if (p == null) {
            return ""; // NOI18N
        }

        return readProcessStreamLine(p.getInputStream());
    }

    public static void logError(final Level logLevel, final Logger log, final Process p) throws IOException {
        if (log == null || !log.isLoggable(logLevel)) {
            return;
        }
        List<String> err = readProcessError(p);
        for (String line : err) {
            log.log(logLevel, "ERROR: " + line); // NOI18N
        }
    }

    private static List<String> readProcessStream(final InputStream stream) throws IOException {
        if (stream == null) {
            return Collections.<String>emptyList();
        }

        final List<String> result = new LinkedList<String>();
        final BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        try {
            String line;
            while ((line = br.readLine()) != null) {
                result.add(line);
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return result;
    }

    private static String readProcessStreamLine(final InputStream stream) throws IOException {
        if (stream == null) {
            return ""; // NOI18N
        }

        final StringBuilder result = new StringBuilder();
        final BufferedReader br = new BufferedReader(new InputStreamReader(stream));

        try {
            String line;

            while ((line = br.readLine()) != null) {
                result.append(line);
            }
        } finally {
            if (br != null) {
                br.close();
            }
        }

        return result.toString();
    }

    public static void writeError(Writer error, Process p) throws IOException {
        List<String> err = readProcessError(p);
        for (String line : err) {
            error.write(line);
        }
    }
}
