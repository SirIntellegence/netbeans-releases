<?xml version="1.0" encoding="UTF-8"?>
<!--
  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

  Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.

  The contents of this file are subject to the terms of either the GNU
  General Public License Version 2 only ("GPL") or the Common
  Development and Distribution License("CDDL") (collectively, the
  "License"). You may not use this file except in compliance with the
  License. You can obtain a copy of the License at
  http://www.netbeans.org/cddl-gplv2.html
  or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
  specific language governing permissions and limitations under the
  License.  When distributing the software, include this License Header
  Notice in each file and include the License file at
  nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
  particular file as subject to the "Classpath" exception as provided
  by Sun in the GPL Version 2 section of the License file that
  accompanied this code. If applicable, add the following below the
  License Header, with the fields enclosed by brackets [] replaced by
  your own identifying information:
  "Portions Copyrighted [year] [name of copyright owner]"

  Contributor(s):

  The Original Software is NetBeans. The Initial Developer of the Original
  Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
  Microsystems, Inc. All Rights Reserved.

  If you wish your version of this file to be governed by only the CDDL
  or only the GPL Version 2, indicate your decision by adding
  "[Contributor] elects to include this software in this distribution
  under the [CDDL or GPL Version 2] license." If you do not indicate a
  single choice of license, a recipient has the option to distribute
  your version of this file under either the CDDL, the GPL Version 2 or
  to extend the choice of license to its licensees as provided above.
  However, if you add GPL Version 2 code and therefore, elected the GPL
  Version 2 license, then the option applies only if the new code is
  made subject to such option by the copyright holder.
-->
<project name="properties" default="none" basedir=".">
    <property environment="env"/>

    <!-- dev -->
    <property name="netbeans.host" value="http://deadlock.netbeans.org"/>
    <property name="netbeans.path" value="hudson/job/trunk/lastSuccessfulBuild/artifact/nbbuild/dist/zip"/>
    <property name="netbeans.dir" value=".netbeans/dev"/>

    <property name="env.NB_USER" value="anoncvs"/>
    <property name="sierra.dir"  value=".netbeans/sierra"/>

    <property name="sources.cvs.root" value=":pserver:${env.NB_USER}@cvs.netbeans.org:/cvs"/>
    <property name="sources.cvs.modules" value="
        ant
        apisupport
        classfile
        core
        db
        debuggercore
        debuggerjpda
        diff
        editor
        enterprise
        graph
        j2ee
        j2eeserver
        java
        lexer
        libs
        nbbuild
        openide
        openidex
        print
        projects
        refactoring
        schema2beans
        serverplugins
        utilities
        web
        websvc
        xml
    "/>
    <property name="sources.cvs.branch" value="-A"/>
    <property name="sources.cvs.date" value="-D now"/>

    <property name="test.cvs.root" value=":pserver:guest@cvs.dev.java.net:/cvs"/>
    <property name="test.cvs.path" value="open-jbi-components/driver-tests/bpelse"/>
    <property name="test.cvs.branch" value="-A"/>
    <property name="test.cvs.modules" value="
        ${test.cvs.path}/assign
        ${test.cvs.path}/benchmark
        ${test.cvs.path}/dynamicpartnerlink
        ${test.cvs.path}/MessageExchange
        ${test.cvs.path}/PartnerLinks
        ${test.cvs.path}/SchemaElemDecl
        ${test.cvs.path}/ScopeTermination
        ${test.cvs.path}/xslt
    "/>
    <property name="home" value="../.."/>
    <property name="cache" value="${home}/_cache"/>
    <property name="dist" value="${cache}/dist"/>
    <property name="lock" value="${cache}/lock"/>
    <property name="test" value="${cache}/test"/>
    <property name="jbi" location="${test}/${test.cvs.path}"/>
    <property name="latest" value="${cache}/latest"/>
    <property name="netbeans" value="${cache}/netbeans"/>
    <property name="enterprise" value="${home}/enterprise"/>
    <property name="soa" value="${home}/enterprise/bpel/samples/resources"/>
    <property name="build.number" value="${home}/nbbuild/netbeans/platform7/build_number"/>
</project>
