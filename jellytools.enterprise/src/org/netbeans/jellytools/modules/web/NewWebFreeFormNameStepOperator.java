/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */

package org.netbeans.jellytools.modules.web;

import javax.swing.JTextField;
import org.netbeans.jellytools.Bundle;
import org.netbeans.jellytools.NewProjectWizardOperator;
import org.netbeans.jellytools.WizardOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JCheckBoxOperator;
import org.netbeans.jemmy.operators.JLabelOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;


/** Class implementing all necessary methods for handling "New Web Application
 *  with Existing Ant Script" wizard.
 *
 * @author Martin.Schovanek@sun.com
 * @version 1.0
 */
public class NewWebFreeFormNameStepOperator extends WizardOperator {

    private JLabelOperator _lblBuildScript;
    private JLabelOperator _lblProjectName;
    private JLabelOperator _lblProjectFolder;
    private JTextFieldOperator _txtBuildScript;
    private JTextFieldOperator _txtProjectName;
    private JTextFieldOperator _txtProjectFolder;
    private JButtonOperator _btBrowse;
    private JButtonOperator _btBrowse2;
    private JTextFieldOperator _txtLocation;
    private JLabelOperator _lblLocation;
    private JButtonOperator _btBrowse3;
    
    public NewWebFreeFormNameStepOperator() {
        super(Helper.freeFormWizardTitle());
    }
    
    public static NewWebFreeFormNameStepOperator invoke() {
        NewProjectWizardOperator projectWizard = NewProjectWizardOperator.invoke();
        String web = Bundle.getStringTrimmed(
                "org.netbeans.modules.web.core.Bundle",
                "Templates/JSP_Servlet");
        projectWizard.selectCategory(web);
        String webFreeForm = Bundle.getStringTrimmed(
                "org.netbeans.modules.web.freeform.resources.Bundle",
                "Templates/Project/Web/webfreeform.xml");
        projectWizard.selectProject(webFreeForm);
        projectWizard.next();
        return new NewWebFreeFormNameStepOperator();
    }
    
    
    //******************************
    // Subcomponents definition part
    //******************************
    
    /** Tries to find "Build Script:" JLabel in this dialog.
     * @return JLabelOperator
     */
    public JLabelOperator lblBuildScript() {
        if (_lblBuildScript==null) {
            String buildScript = Bundle.getStringTrimmed(
                    "org.netbeans.modules.ant.freeform.ui.Bundle",
                    "LBL_BasicProjectInfoPanel_jLabel2");
            _lblBuildScript = new JLabelOperator(this, buildScript);
        }
        return _lblBuildScript;
    }
    
    /** Tries to find "Project Name:" JLabel in this dialog.
     * @return JLabelOperator
     */
    public JLabelOperator lblProjectName() {
        if (_lblProjectName==null) {
            String projectName = Bundle.getStringTrimmed(
                    "org.netbeans.modules.ant.freeform.ui.Bundle",
                    "LBL_BasicProjectInfoPanel_jLabel4");
            _lblProjectName = new JLabelOperator(this, projectName);
        }
        return _lblProjectName;
    }
    
    /** Tries to find "Project Folder:" JLabel in this dialog.
     * @return JLabelOperator
     */
    public JLabelOperator lblProjectFolder() {
        if (_lblProjectFolder==null) {
            String projectFolder = Bundle.getStringTrimmed(
                    "org.netbeans.modules.ant.freeform.ui.Bundle",
                    "LBL_BasicProjectInfoPanel_jLabel5");
            _lblProjectFolder = new JLabelOperator(this, projectFolder);
        }
        return _lblProjectFolder;
    }
    
    /** Tries to find null JTextField in this dialog.
     * @return JTextFieldOperator
     */
    public JTextFieldOperator txtBuildScript() {
        if (_txtBuildScript==null) {
            if (lblBuildScript().getLabelFor()!=null) {
                _txtBuildScript = new JTextFieldOperator(
                        (JTextField) lblBuildScript().getLabelFor());
            } else {
                _txtBuildScript = new JTextFieldOperator(this);
            }
        }
        return _txtBuildScript;
    }
    
    /** Tries to find null JTextField in this dialog.
     * @return JTextFieldOperator
     */
    public JTextFieldOperator txtProjectName() {
        if (_txtProjectName==null) {
            if (lblProjectName().getLabelFor()!=null) {
                _txtProjectName = new JTextFieldOperator(
                        (JTextField) lblProjectName().getLabelFor());
            } else {
                _txtProjectName = new JTextFieldOperator(this, 1);
            }
        }
        return _txtProjectName;
    }
    
    /** Tries to find null JTextField in this dialog.
     * @return JTextFieldOperator
     */
    public JTextFieldOperator txtProjectFolder() {
        if (_txtProjectFolder==null) {
            _txtProjectFolder = new JTextFieldOperator(this, 2);
        }
        return _txtProjectFolder;
    }
    
    /** Tries to find "Browse..." JButton in this dialog.
     * @return JButtonOperator
     */
    public JButtonOperator btBrowse() {
        if (_btBrowse==null) {
            String browse = Bundle.getStringTrimmed(
                    "org.netbeans.modules.ant.freeform.ui.Bundle",
                    "BTN_BasicProjectInfoPanel_browseProjectLocation"); //XXX what's xxx?
            _btBrowse = new JButtonOperator(this, browse);
        }
        return _btBrowse;
    }
    
    /** Tries to find "Browse..." JButton in this dialog.
     * @return JButtonOperator
     */
    public JButtonOperator btBrowse2() {
        if (_btBrowse2==null) {
            String browse = Bundle.getStringTrimmed(
                    "org.netbeans.modules.ant.freeform.ui.Bundle",
                    "BTN_BasicProjectInfoPanel_browseProjectLocation"); //XXX what's xxx?
            _btBrowse2 = new JButtonOperator(this, browse, 1);
        }
        return _btBrowse2;
    }
    
    /** Tries to find null JTextField in this dialog.
     * @return JTextFieldOperator
     */
    public JTextFieldOperator txtLocation() {
        if (_txtLocation==null) {
            if (lblLocation().getLabelFor()!=null) {
                _txtLocation = new JTextFieldOperator(
                        (JTextField) lblLocation().getLabelFor());
            } else {
                _txtLocation = new JTextFieldOperator(this, 3);
            }
        }
        return _txtLocation;
    }
    
    /** Tries to find "Location:" JLabel in this dialog.
     * @return JLabelOperator
     */
    public JLabelOperator lblLocation() {
        if (_lblLocation==null) {
            String location = Bundle.getStringTrimmed(
                    "org.netbeans.modules.ant.freeform.ui.Bundle",
                    "LBL_BasicProjectInfoPanel_jLabel6");
            _lblLocation = new JLabelOperator(this, location);
        }
        return _lblLocation;
    }
    
    /** Tries to find "Browse..." JButton in this dialog.
     * @return JButtonOperator
     */
    public JButtonOperator btBrowse3() {
        if (_btBrowse3==null) {
            String browse = Bundle.getStringTrimmed(
                    "org.netbeans.modules.ant.freeform.ui.Bundle",
                    "BTN_BasicProjectInfoPanel_browseProjectLocation"); //XXX what's xxx?
            _btBrowse3 = new JButtonOperator(this, browse, 2);
        }
        return _btBrowse3;
    }
    
    //****************************************
    // Low-level functionality definition part
    //****************************************
    
    /** gets text for txtBuildScript
     * @return String text
     */
    public String getBuildScript() {
        return txtBuildScript().getText();
    }
    
    /** sets text for txtBuildScript
     * @param text String text
     */
    public void setBuildScript(String text) {
        txtBuildScript().setText(text);
    }
    
    /** types text for txtBuildScript
     * @param text String text
     */
    public void typeBuildScript(String text) {
        txtBuildScript().typeText(text);
    }
    
    /** gets text for txtProjectName
     * @return String text
     */
    public String getProjectName() {
        return txtProjectName().getText();
    }
    
    /** sets text for txtProjectName
     * @param text String text
     */
    public void setProjectName(String text) {
        txtProjectName().setText(text);
    }
    
    /** types text for txtProjectName
     * @param text String text
     */
    public void typeProjectName(String text) {
        txtProjectName().typeText(text);
    }
    
    /** gets text for txtProjectFolder
     * @return String text
     */
    public String getProjectFolder() {
        return txtProjectFolder().getText();
    }
    
    /** sets text for txtProjectFolder
     * @param text String text
     */
    public void setProjectFolder(String text) {
        txtProjectFolder().setText(text);
    }
    
    /** types text for txtProjectFolder
     * @param text String text
     */
    public void typeProjectFolder(String text) {
        txtProjectFolder().typeText(text);
    }
    
    /** clicks on "Browse..." JButton
     */
    public void browse() {
        btBrowse().push();
    }
    
    /** clicks on "Browse..." JButton
     */
    public void browse2() {
        btBrowse2().push();
    }
    
    // !!! the ...ProjectLocation instead ...Location is used due conflict with
    // !!! ComponentOperator.getLocation()
    /** gets text for txtLocation
     * @return String text
     */
    public String getProjectLocation() {
        return txtLocation().getText();
    }
    
    /** sets text for txtLocation
     * @param text String text
     */
    public void setProjectLocation(String text) {
        txtLocation().setText(text);
    }
    
    /** types text for txtLocation
     * @param text String text
     */
    public void typeProjectLocation(String text) {
        txtLocation().typeText(text);
    }
    
    /** clicks on "Browse..." JButton
     */
    public void browse3() {
        btBrowse3().push();
    }
    
    //*****************************************
    // High-level functionality definition part
    //*****************************************
    
    /**
     * Performs verification of NewWebFreeFormNameStepOperator by accessing all its components.
     */
    @Override
    public void verify() {
        lblBuildScript();
        lblProjectName();
        lblProjectFolder();
        txtBuildScript();
        txtProjectName();
        txtProjectFolder();
        btBrowse();
        btBrowse2();
        txtLocation();
        lblLocation();
        btBrowse3();
    }
}

