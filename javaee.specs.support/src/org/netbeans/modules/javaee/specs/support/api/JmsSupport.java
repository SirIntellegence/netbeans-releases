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
package org.netbeans.modules.javaee.specs.support.api;

import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eePlatform;
import org.netbeans.modules.javaee.specs.support.DefaultJmsSupportImpl;
import org.netbeans.modules.javaee.specs.support.spi.JmsSupportImplementation;

/**
 *
 * @author Martin Fousek <marfous@netbeans.org>
 * @since 1.13
 */
public final class JmsSupport {

    private static final JmsSupport DEFAULT = new JmsSupport(new DefaultJmsSupportImpl());
    private final JmsSupportImplementation impl;

    private JmsSupport(JmsSupportImplementation impl) {
        this.impl = impl;
    }

    /**
     * Returns instance for given j2eePlatform.
     * @param j2eePlatform platform, can be null for default support retrieval
     * @return platform specific instance, if no such exist default one is provided
     */
    @NonNull
    public static JmsSupport getInstance(J2eePlatform j2eePlatform) {
        if (j2eePlatform == null) {
            return DEFAULT;
        }

        JmsSupportImplementation supportImpl = j2eePlatform.getLookup().lookup(JmsSupportImplementation.class);
        if (supportImpl != null) {
            return new JmsSupport(supportImpl);
        }
        return DEFAULT;
    }

    /**
     * Whether should be used 'mappedName' attribute of the @MessageDriven in projects targeting Java EE6
     * and lower to specify the destination. In another words, whether server supports mappedName attribute.
     *
     * @return {@code true} if the mappedName can be used and generated in EE6- projects, {@code false} otherwise
     */
    public boolean useMappedName() {
        return impl.useMappedName();
    }

    /**
     * Whether can be used EE7 specific 'destinationLookup' Activation Config Property. Tells that the
     * server is Java EE7 platform complied.
     *
     * @return {@code true} if the 'destinationLookup' ACP can be used in destination specification, {@code false}
     * otherwise
     */
    public boolean useDestinationLookup() {
        return impl.useDestinationLookup();
    }

    /**
     * Activation config property to generate into the 'activationConfig' attribute of the @MessageDriven
     * annotation. Can be {@code null} in case that no @ActivationConfigProperty is supported.
     * <p>
     * This property will be used for projects targeting JavaEE6 or lower EE platform.
     *
     * @return property name if any, {@code null} otherwise
     */
    @CheckForNull
    public String activationConfigProperty() {
        return impl.activationConfigProperty();
    }

}
