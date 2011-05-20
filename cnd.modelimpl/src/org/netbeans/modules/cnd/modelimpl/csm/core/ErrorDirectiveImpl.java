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

package org.netbeans.modules.cnd.modelimpl.csm.core;

import java.io.IOException;
import org.netbeans.modules.cnd.api.model.CsmErrorDirective;
import org.netbeans.modules.cnd.api.model.CsmFile;
import org.netbeans.modules.cnd.api.model.CsmOffsetable;
import org.netbeans.modules.cnd.modelimpl.repository.PersistentUtils;
import org.netbeans.modules.cnd.modelimpl.textcache.DefaultCache;
import org.netbeans.modules.cnd.repository.spi.RepositoryDataInput;
import org.netbeans.modules.cnd.repository.spi.RepositoryDataOutput;

/**
 *
 * @author Vladimir Voskresensky
 */
public final class ErrorDirectiveImpl extends OffsetableBase implements CsmErrorDirective {
    private final CharSequence msg;
    private ErrorDirectiveImpl(CsmFile file, CharSequence text, CsmOffsetable offs) {
        super(file, offs);
        this.msg = DefaultCache.getManager().getString(text);
    }

    public static ErrorDirectiveImpl create(CsmFile file, CharSequence msg, CsmOffsetable offs) {
        return new ErrorDirectiveImpl(file, msg, offs);
    }

    @Override
    public CharSequence getText() {
        return msg;
    }

    @Override
    public String toString() {
        return super.toString() + msg;
    }

    @Override
    public int hashCode() {
        return 47 * super.hashCode() + msg.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final ErrorDirectiveImpl other = (ErrorDirectiveImpl) obj;
        return this.msg.equals(other.msg);
    }


    ///////////////////////////////////////////////////////////////////////
    // serialization
    
    @SuppressWarnings("unchecked")
    public ErrorDirectiveImpl(RepositoryDataInput input) throws IOException {
        super(input);
        this.msg = PersistentUtils.readUTF(input, DefaultCache.getManager());
    }

    @Override
    public void write(RepositoryDataOutput output) throws IOException {
        super.write(output);
        PersistentUtils.writeUTF(msg, output);
    }
}
