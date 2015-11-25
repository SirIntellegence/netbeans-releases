/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2015 Sun Microsystems, Inc.
 */
package org.netbeans.modules.docker.remote;

import java.util.EventListener;
import java.util.EventObject;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.docker.DockerInstance;

/**
 *
 * @author Petr Hejl
 */
public class BuildEvent extends EventObject {

    private final DockerInstance instance;

    private final String message;

    private final Error detail;

    private final boolean error;

    private final boolean upload;

    BuildEvent(DockerInstance instance, String message, boolean error, Error detail, boolean upload) {
        super(instance);
        this.instance = instance;
        this.message = message;
        this.detail = detail;
        this.error = error;
        this.upload = upload;
    }

    public String getMessage() {
        return message;
    }

    @CheckForNull
    public Error getDetail() {
        return detail;
    }

    public boolean isError() {
        return error;
    }

    public boolean isUpload() {
        return upload;
    }

    @Override
    public DockerInstance getSource() {
        return instance;
    }

    @Override
    public String toString() {
        return "BuildEvent{" + "instance=" + instance + ", message=" + message + ", detail=" + detail + ", error=" + error + ", upload=" + upload + '}';
    }

    public static class Error {

        private final long code;

        private final String message;

        public Error(long code, String message) {
            this.code = code;
            this.message = message;
        }

        public long getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "Error{" + "code=" + code + ", message=" + message + '}';
        }
    }

    public static interface Listener extends EventListener {

        void onEvent(BuildEvent event);

    }
}
