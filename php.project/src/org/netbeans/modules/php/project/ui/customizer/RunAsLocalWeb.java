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
package org.netbeans.modules.php.project.ui.customizer;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.php.project.ui.Utils;
import org.netbeans.modules.php.project.ui.customizer.PhpProjectProperties.RunAsType;
import org.netbeans.spi.project.ui.support.ProjectCustomizer.Category;
import org.openide.util.NbBundle;

/**
 *
 * @author  Radek Matous
 */
public class RunAsLocalWeb extends RunAsPanel.InsidePanel {
    private static final long serialVersionUID = -5593489817914071L;
    private final JLabel[] labels;
    private final JTextField[] textFields;
    private final String[] propertyNames;
    private String displayName;

    public RunAsLocalWeb(ConfigManager manager, Category category) {
        this(manager, category, NbBundle.getMessage(RunAsScript.class, "RunAsType.localweb"));
    }
    
    /** Creates new form LocalWebPanel */
    private RunAsLocalWeb(ConfigManager manager, Category category, String displayName) {
        super(manager, category);
        initComponents();
        this.displayName = displayName;
        this.labels = new JLabel[] {
            urlLabel, 
            indexFileLabel, 
            argsLabel
        };
        this.textFields = new JTextField[] {
            urlTextField, 
            indexFileTextField, 
            argsTextField
        };
        this.propertyNames = new String[] {
            PhpProjectProperties.URL, 
            PhpProjectProperties.INDEX_FILE, 
            PhpProjectProperties.ARGS
        
        
        
        };
        assert labels.length == textFields.length && labels.length == propertyNames.length;
        for (int i = 0; i < textFields.length; i++) {
            DocumentListener dl = new FieldUpdater(propertyNames[i], labels[i], textFields[i]);
            textFields[i].getDocument().addDocumentListener(dl);
        }        
    }

    @Override
    protected boolean isDefault() {
        return true;
    }
    
    @Override
    protected RunAsType getRunAsType() {
        return PhpProjectProperties.RunAsType.LOCAL;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    protected JLabel getRunAsLabel() {
        return runAsLabel;
    }
    
    @Override
    public JComboBox getRunAsCombo() {
        return runAsCombo;
    }

    protected void loadFields() {
        for (int i = 0; i < textFields.length; i++) {
            textFields[i].setText(getValue(propertyNames[i]));
        }
    }
        
    private String composeHint() {
        String baseURL = urlTextField.getText();
        String indexFile = indexFileTextField.getText();
        String args = argsTextField.getText();
        URL retval = null;

        try {
            if (baseURL != null && baseURL.trim().length() > 0) {
                retval = new URL(baseURL);
            }
            if (retval != null && indexFile != null && indexFile.trim().length() > 0) {
                retval = new URL(retval, indexFile);
            }
            if (retval != null && args != null && args.trim().length() > 0) {
                retval = new URI(retval.getProtocol(), retval.getUserInfo(), retval.getHost(), retval.getPort(), retval.getPath(), args, retval.getRef()).toURL(); //NOI18N

            }
        } catch (MalformedURLException ex) {
            String err = NbBundle.getMessage(RunAsLocalWeb.class, "MSG_InvalidUrl");
            getCategory().setErrorMessage(err);
            getCategory().setValid(false);            
        } catch (URISyntaxException ex) {
            String err = NbBundle.getMessage(RunAsLocalWeb.class, "MSG_InvalidUrl");
            getCategory().setErrorMessage(err);
            getCategory().setValid(false);            
        }
        return (retval != null) ? retval.toExternalForm() : "";//NOI18N

    }

    protected void validateFields() {
        String url = urlTextField.getText();
        String indexFile = indexFileTextField.getText();

        String err = null;
        if (!Utils.isValidUrl(url)) {
            err = NbBundle.getMessage(RunAsLocalWeb.class, "MSG_InvalidUrl");
        } else if (!url.endsWith("/")) { // NOI18N

            err = NbBundle.getMessage(RunAsLocalWeb.class, "MSG_UrlNotTrailingSlash");
        } else if (!Utils.isValidFileName(indexFile)) {
            err = NbBundle.getMessage(RunAsLocalWeb.class, "MSG_IllegalIndexName");
        }
        //TODO: no validation for arguments        
        if (err != null) {
            getCategory().setErrorMessage(err);
            getCategory().setValid(false);
        } else {
            getCategory().setErrorMessage(null);
            getCategory().setValid(true);
        }
    }

    private class FieldUpdater implements DocumentListener {

        private final JLabel label;
        private final JTextField field;
        private final String propName;

        public FieldUpdater(String propName, JLabel label, JTextField field) {
            this.propName = propName;
            this.label = label;
            this.field = field;
        }

        public final void insertUpdate(DocumentEvent e) {
            changed();
            validateFields();
        }

        public final void removeUpdate(DocumentEvent e) {
            insertUpdate(e);
        }

        public final void changedUpdate(DocumentEvent e) {
        }

        final String getPropName() {
            return propName;
        }

        final String getDefaultValue() {
            return RunAsLocalWeb.this.getDefaultValue(getPropName()); //NOI18N

        }

        void changed() {
            putValue(propName, field.getText());
            markAsModified(label, propName, field.getText());
            hintLabel.setText(composeHint());
            validateFields();
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        urlLabel = new javax.swing.JLabel();
        urlTextField = new javax.swing.JTextField();
        indexFileLabel = new javax.swing.JLabel();
        indexFileTextField = new javax.swing.JTextField();
        argsLabel = new javax.swing.JLabel();
        argsTextField = new javax.swing.JTextField();
        hintLabel = new javax.swing.JTextArea();
        runAsLabel = new javax.swing.JLabel();
        runAsCombo = new javax.swing.JComboBox();

        org.openide.awt.Mnemonics.setLocalizedText(urlLabel, org.openide.util.NbBundle.getMessage(RunAsLocalWeb.class, "RunAsLocalWeb.urlLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(indexFileLabel, org.openide.util.NbBundle.getMessage(RunAsLocalWeb.class, "RunAsLocalWeb.indexFileLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(argsLabel, org.openide.util.NbBundle.getMessage(RunAsLocalWeb.class, "RunAsLocalWeb.argsLabel.text")); // NOI18N

        hintLabel.setColumns(20);
        hintLabel.setEditable(false);
        hintLabel.setLineWrap(true);
        hintLabel.setRows(5);
        hintLabel.setWrapStyleWord(true);
        hintLabel.setBorder(null);
        hintLabel.setEnabled(false);
        hintLabel.setOpaque(false);

        org.openide.awt.Mnemonics.setLocalizedText(runAsLabel, org.openide.util.NbBundle.getMessage(RunAsLocalWeb.class, "RunAsLocalWeb.runAsLabel.text")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(runAsLabel)
                .addContainerGap())
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(argsLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 72, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(indexFileLabel)
                    .add(urlLabel))
                .add(29, 29, 29)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, hintLabel, 0, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, argsTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, indexFileTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, runAsCombo, 0, 203, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, urlTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE))
                .add(0, 0, 0))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(runAsLabel)
                    .add(runAsCombo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(urlLabel)
                    .add(urlTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(indexFileLabel)
                    .add(indexFileTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(argsLabel)
                    .add(argsTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(hintLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel argsLabel;
    private javax.swing.JTextField argsTextField;
    private javax.swing.JTextArea hintLabel;
    private javax.swing.JLabel indexFileLabel;
    private javax.swing.JTextField indexFileTextField;
    private javax.swing.JComboBox runAsCombo;
    private javax.swing.JLabel runAsLabel;
    private javax.swing.JLabel urlLabel;
    private javax.swing.JTextField urlTextField;
    // End of variables declaration//GEN-END:variables
}
