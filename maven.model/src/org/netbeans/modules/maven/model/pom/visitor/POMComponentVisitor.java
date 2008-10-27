/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.maven.model.pom.visitor;

import org.netbeans.modules.maven.model.pom.*;

/**
 *
 * @author mkleint
 */
public interface POMComponentVisitor {
    
    void visit(Project target);
    void visit(Parent target);
    void visit(Organization target);
    void visit(DistributionManagement target);
    void visit(Site target);
    void visit(DeploymentRepository target);
    void visit(Prerequisites target);
    void visit(Contributor target);
    void visit(Scm target);
    void visit(IssueManagement target);
    void visit(CiManagement target);
    void visit(Notifier target);
    void visit(Repository target);
    void visit(RepositoryPolicy target);
    void visit(Profile target);
    void visit(BuildBase target);
    void visit(Plugin target);
    void visit(Dependency target);
    void visit(Exclusion target);
    void visit(PluginExecution target);
    void visit(Resource target);
    void visit(PluginManagement target);
    void visit(Reporting target);
    void visit(ReportPlugin target);
    void visit(ReportSet target);
    void visit(Activation target);
    void visit(ActivationProperty target);
    void visit(ActivationOS target);
    void visit(ActivationFile target);
    void visit(ActivationCustom target);
    void visit(DependencyManagement target);
    void visit(Build target);
    void visit(Extension target);
    void visit(License target);
    void visit(MailingList target);
    void visit(Developer target);
    void visit(POMExtensibilityElement target);
    void visit(ModelList target);
    void visit(Configuration target);
    void visit(Properties target);

}