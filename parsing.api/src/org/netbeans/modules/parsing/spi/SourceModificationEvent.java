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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.parsing.spi;

import java.util.EventObject;
import org.netbeans.modules.parsing.api.Source;

/**
 *
 * @author hanz
 */
public class SourceModificationEvent extends EventObject {
    
    private final boolean sourceChanged;

    
    /**
     * Creates a new {@link SourceModificationEvent}
     * @param source the {@link Source} in which the event occurred
     * @deprecated use {@link SourceModificationEvent#SourceModificationEvent(java.lang.Object, boolean)}
     */
    @Deprecated
    protected SourceModificationEvent (
        Object              source
    ) {
        this(source, true);
    }
    
    /**
     * Creates a new {@link SourceModificationEvent}
     * @param source the {@link Source} in which the event occurred
     * @param sourceChanged true if the change caused a modification of the text being parsed.
     * @since 1.36
     */
    protected SourceModificationEvent (
        Object              source,
        boolean             sourceChanged
    ) {
        super (source);
        this.sourceChanged = sourceChanged;
    }

    public Source getModifiedSource () {
        return (Source) getSource ();
    }
    
    /**
     * Returns true when the change causing this event affected the source.
     * @return true if the source was changed
     * @since 1.36
     */
    public boolean sourceChanged() {
        return sourceChanged;
    }

    @Override
    public String toString () {
        return "SourceModificationEvent " + hashCode () + "(source: " + source + ")";
    }
}



