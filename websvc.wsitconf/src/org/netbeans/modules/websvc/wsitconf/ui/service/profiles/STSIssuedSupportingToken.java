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
 * Software is Sun Microsystems, Inc. Portions Copyright 2006 Sun
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

package org.netbeans.modules.websvc.wsitconf.ui.service.profiles;

import org.netbeans.modules.websvc.wsitconf.spi.SecurityProfile;
import org.netbeans.modules.websvc.wsitconf.spi.features.SecureConversationFeature;
import org.netbeans.modules.websvc.wsitconf.ui.ComboConstants;
import org.netbeans.modules.websvc.wsitconf.wsdlmodelext.AlgoSuiteModelHelper;
import org.netbeans.modules.websvc.wsitconf.wsdlmodelext.SecurityPolicyModelHelper;
import org.netbeans.modules.websvc.wsitconf.wsdlmodelext.SecurityTokensModelHelper;
import org.netbeans.modules.websvc.wsitmodelext.policy.Policy;
import org.netbeans.modules.websvc.wsitmodelext.security.BootstrapPolicy;
import org.netbeans.modules.websvc.wsitmodelext.security.tokens.ProtectionToken;
import org.netbeans.modules.websvc.wsitmodelext.security.tokens.SecureConversationToken;
import org.netbeans.modules.websvc.wsitmodelext.versioning.ConfigVersion;
import org.netbeans.modules.xml.wsdl.model.WSDLComponent;

/**
 *
 * @author  Martin Grebac
 */
public class STSIssuedSupportingToken extends ProfileBaseForm {

    private int suppTokenTypeUsed = SecurityTokensModelHelper.SIGNED_ENCRYPTED;

    /**
     * Creates new form STSIssuedSupporting
     */
    public STSIssuedSupportingToken(WSDLComponent comp, SecurityProfile secProfile) {
        super(comp, secProfile);
        initComponents();

        inSync = true;
        fillLayoutCombo(layoutCombo);
        
        tokenTypeCombo.removeAllItems();
        tokenTypeCombo.addItem(ComboConstants.ISSUED_TOKENTYPE_SAML10);
        tokenTypeCombo.addItem(ComboConstants.ISSUED_TOKENTYPE_SAML11);
        tokenTypeCombo.addItem(ComboConstants.ISSUED_TOKENTYPE_SAML20);

        keyTypeCombo.removeAllItems();
        keyTypeCombo.addItem(ComboConstants.ISSUED_KEYTYPE_SYMMETRIC);
        keyTypeCombo.addItem(ComboConstants.ISSUED_KEYTYPE_PUBLIC);
        keyTypeCombo.addItem(ComboConstants.ISSUED_KEYTYPE_NOPROOF);
       
        fillKeySize(keySizeCombo);
        fillAlgoSuiteCombo(algoSuiteCombo);
        inSync = false;

        if (ConfigVersion.CONFIG_1_0 == cfgVersion) {
            suppTokenTypeUsed = SecurityTokensModelHelper.SIGNED_SUPPORTING;
        }
        
        sync();
    }

    protected void sync() {
        inSync = true;

        WSDLComponent secBinding = null;
        WSDLComponent topSecBinding = SecurityPolicyModelHelper.getSecurityBindingTypeElement(comp);
        WSDLComponent protTokenKind = SecurityTokensModelHelper.getTokenElement(topSecBinding, ProtectionToken.class);
        WSDLComponent protToken = SecurityTokensModelHelper.getTokenTypeElement(protTokenKind);
        
        boolean secConv = (protToken instanceof SecureConversationToken);
        setChBox(secConvChBox, secConv);
        
        if (secConv) {
            WSDLComponent bootPolicy = SecurityTokensModelHelper.getTokenElement(protToken, BootstrapPolicy.class);
            secBinding = SecurityPolicyModelHelper.getSecurityBindingTypeElement(bootPolicy);
            Policy p = (Policy) secBinding.getParent();
            setChBox(derivedKeysChBox, SecurityPolicyModelHelper.isRequireDerivedKeys(protToken));
            setChBox(reqSigConfChBox, SecurityPolicyModelHelper.isRequireSignatureConfirmation(p));
            setChBox(encryptSignatureChBox, SecurityPolicyModelHelper.isEncryptSignature(bootPolicy));
            setChBox(encryptOrderChBox, SecurityPolicyModelHelper.isEncryptBeforeSigning(bootPolicy));
        } else {
            secBinding = SecurityPolicyModelHelper.getSecurityBindingTypeElement(comp);
            setChBox(derivedKeysChBox, false);
            setChBox(reqSigConfChBox, SecurityPolicyModelHelper.isRequireSignatureConfirmation(comp));
            setChBox(encryptSignatureChBox, SecurityPolicyModelHelper.isEncryptSignature(comp));
            setChBox(encryptOrderChBox, SecurityPolicyModelHelper.isEncryptBeforeSigning(comp));
        }            
            
        WSDLComponent tokenKind = SecurityTokensModelHelper.getTokenElement(secBinding, ProtectionToken.class);
        WSDLComponent token = SecurityTokensModelHelper.getTokenTypeElement(tokenKind);

        setChBox(reqDerivedKeys, SecurityPolicyModelHelper.isRequireDerivedKeys(token));

        setCombo(algoSuiteCombo, AlgoSuiteModelHelper.getAlgorithmSuite(secBinding));
        setCombo(layoutCombo, SecurityPolicyModelHelper.getMessageLayout(secBinding));

        if (secConv) {
            tokenKind = SecurityTokensModelHelper.getSupportingToken(secBinding.getParent(), suppTokenTypeUsed);
        } else {
            tokenKind = SecurityTokensModelHelper.getSupportingToken(comp, suppTokenTypeUsed);
        }
        token = SecurityTokensModelHelper.getTokenTypeElement(tokenKind);
        
        setChBox(reqDerivedKeysIssued, SecurityPolicyModelHelper.isRequireDerivedKeys(token));

        setCombo(tokenTypeCombo, SecurityTokensModelHelper.getIssuedTokenType(token));
        setCombo(keyTypeCombo, SecurityTokensModelHelper.getIssuedKeyType(token));
        setCombo(keySizeCombo, SecurityTokensModelHelper.getIssuedKeySize(token));
        
        issuerAddressField.setText(SecurityTokensModelHelper.getIssuedIssuerAddress(token));
        issuerMetadataField.setText(SecurityTokensModelHelper.getIssuedIssuerMetadataAddress(token));
        
        enableDisable();
        
        inSync = false;
    }

    public void setValue(javax.swing.JComponent source) {

        if (inSync) return;

        WSDLComponent secBinding = null;
        WSDLComponent topSecBinding = SecurityPolicyModelHelper.getSecurityBindingTypeElement(comp);
        WSDLComponent protTokenKind = SecurityTokensModelHelper.getTokenElement(topSecBinding, ProtectionToken.class);
        WSDLComponent protToken = SecurityTokensModelHelper.getTokenTypeElement(protTokenKind);
        
        boolean secConv = (protToken instanceof SecureConversationToken);

        if (source.equals(secConvChBox)) {
            ((SecureConversationFeature)secProfile).enableSecureConversation(comp, secConvChBox.isSelected());
            sync();
        }

        SecurityPolicyModelHelper spmh = SecurityPolicyModelHelper.getInstance(cfgVersion);
        SecurityTokensModelHelper stmh = SecurityTokensModelHelper.getInstance(cfgVersion);
        AlgoSuiteModelHelper asmh = AlgoSuiteModelHelper.getInstance(cfgVersion);
        if (secConv) {
            WSDLComponent bootPolicy = SecurityTokensModelHelper.getTokenElement(protToken, BootstrapPolicy.class);
            secBinding = SecurityPolicyModelHelper.getSecurityBindingTypeElement(bootPolicy);
            Policy p = (Policy) secBinding.getParent();
            if (source.equals(derivedKeysChBox)) {
                spmh.enableRequireDerivedKeys(protToken, derivedKeysChBox.isSelected());
            }
            if (source.equals(reqSigConfChBox)) {
                spmh.enableRequireSignatureConfirmation(
                        SecurityPolicyModelHelper.getWss11(p), reqSigConfChBox.isSelected());
            }
        } else {
            secBinding = SecurityPolicyModelHelper.getSecurityBindingTypeElement(comp);
            if (source.equals(reqSigConfChBox)) {
                spmh.enableRequireSignatureConfirmation(SecurityPolicyModelHelper.getWss11(comp), reqSigConfChBox.isSelected());
            }
        }
            
        WSDLComponent tokenKind = SecurityTokensModelHelper.getTokenElement(secBinding, ProtectionToken.class);
        WSDLComponent token = SecurityTokensModelHelper.getTokenTypeElement(tokenKind);
        
        if (source.equals(encryptSignatureChBox)) {
            spmh.enableEncryptSignature(secBinding, encryptSignatureChBox.isSelected());
            if (secConv) {
                spmh.enableEncryptSignature(topSecBinding, encryptSignatureChBox.isSelected());
            }
        }
        if (source.equals(encryptOrderChBox)) {
            spmh.enableEncryptBeforeSigning(secBinding, encryptOrderChBox.isSelected());
            if (secConv) {
                spmh.enableEncryptBeforeSigning(topSecBinding, encryptOrderChBox.isSelected());
            }
        }
        if (source.equals(layoutCombo)) {
            spmh.setLayout(secBinding, (String) layoutCombo.getSelectedItem());
            if (secConv) {
                spmh.setLayout(topSecBinding, (String) layoutCombo.getSelectedItem());
            }
        }
        if (source.equals(algoSuiteCombo)) {
            asmh.setAlgorithmSuite(secBinding, (String) algoSuiteCombo.getSelectedItem());
            if (secConv) {
                asmh.setAlgorithmSuite(topSecBinding, (String) algoSuiteCombo.getSelectedItem());
            }
        }
        if (source.equals(reqDerivedKeys)) {
            spmh.enableRequireDerivedKeys(token, reqDerivedKeys.isSelected());
            return;
        }

        tokenKind = SecurityTokensModelHelper.getSupportingToken(secBinding.getParent(), suppTokenTypeUsed);
        token = SecurityTokensModelHelper.getTokenTypeElement(tokenKind);

        if (source.equals(reqDerivedKeysIssued)) {
            spmh.enableRequireDerivedKeys(token, reqDerivedKeysIssued.isSelected());
            return;
        }
        if (source.equals(tokenTypeCombo) || source.equals(keyTypeCombo) || source.equals(keySizeCombo)) {
            if (ComboConstants.ISSUED_KEYTYPE_NOPROOF.equals(keyTypeCombo.getSelectedItem())) {
                stmh.setIssuedTokenRSTAttributes(token,
                        (String)tokenTypeCombo.getSelectedItem(),
                        (String)keyTypeCombo.getSelectedItem(),
                        null);
            } else {
                stmh.setIssuedTokenRSTAttributes(token,
                        (String)tokenTypeCombo.getSelectedItem(),
                        (String)keyTypeCombo.getSelectedItem(),
                        (String)keySizeCombo.getSelectedItem());
            }
        }

        if (source.equals(issuerAddressField) || source.equals(issuerMetadataField)) {
            stmh.setIssuedTokenAddressAttributes(token, 
                    issuerAddressField.getText(), 
                    issuerMetadataField.getText());
        }

        enableDisable();
    }

    protected void enableDisable() {
        boolean secConvEnabled = secConvChBox.isSelected();
        derivedKeysChBox.setEnabled(secConvEnabled);

        boolean keySizeNeeded = true;
        if (ComboConstants.ISSUED_KEYTYPE_NOPROOF.equals(keyTypeCombo.getSelectedItem())) {
            keySizeNeeded = false;
        }
        keySizeCombo.setEnabled(keySizeNeeded);
        keySizeLabel.setEnabled(keySizeNeeded);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        secConvChBox = new javax.swing.JCheckBox();
        reqSigConfChBox = new javax.swing.JCheckBox();
        derivedKeysChBox = new javax.swing.JCheckBox();
        algoSuiteLabel = new javax.swing.JLabel();
        algoSuiteCombo = new javax.swing.JComboBox();
        layoutLabel = new javax.swing.JLabel();
        layoutCombo = new javax.swing.JComboBox();
        encryptSignatureChBox = new javax.swing.JCheckBox();
        reqDerivedKeysIssued = new javax.swing.JCheckBox();
        encryptOrderChBox = new javax.swing.JCheckBox();
        issuerAddressLabel = new javax.swing.JLabel();
        issuerAddressField = new javax.swing.JTextField();
        issuerMetadataLabel = new javax.swing.JLabel();
        issuerMetadataField = new javax.swing.JTextField();
        tokenTypeLabel = new javax.swing.JLabel();
        keyTypeLabel = new javax.swing.JLabel();
        keySizeLabel = new javax.swing.JLabel();
        tokenTypeCombo = new javax.swing.JComboBox();
        keyTypeCombo = new javax.swing.JComboBox();
        keySizeCombo = new javax.swing.JComboBox();
        reqDerivedKeys = new javax.swing.JCheckBox();

        org.openide.awt.Mnemonics.setLocalizedText(secConvChBox, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_SecConvLabel")); // NOI18N
        secConvChBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        secConvChBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        secConvChBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secConvChBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(reqSigConfChBox, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_RequireSigConfirmation")); // NOI18N
        reqSigConfChBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        reqSigConfChBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        reqSigConfChBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reqSigConfChBoxActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(derivedKeysChBox, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_RequireDerivedKeysForSecConv")); // NOI18N
        derivedKeysChBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        derivedKeysChBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        derivedKeysChBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                derivedKeysChBoxActionPerformed(evt);
            }
        });

        algoSuiteLabel.setLabelFor(algoSuiteCombo);
        org.openide.awt.Mnemonics.setLocalizedText(algoSuiteLabel, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_AlgoSuiteLabel")); // NOI18N

        algoSuiteCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                algoSuiteComboActionPerformed(evt);
            }
        });

        layoutLabel.setLabelFor(layoutCombo);
        org.openide.awt.Mnemonics.setLocalizedText(layoutLabel, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_LayoutLabel")); // NOI18N

        layoutCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                layoutComboActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(encryptSignatureChBox, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_EncryptSignatureLabel")); // NOI18N
        encryptSignatureChBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        encryptSignatureChBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        encryptSignatureChBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                encryptSignatureChBox(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(reqDerivedKeysIssued, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_RequireDerivedKeysIssued")); // NOI18N
        reqDerivedKeysIssued.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        reqDerivedKeysIssued.setMargin(new java.awt.Insets(0, 0, 0, 0));
        reqDerivedKeysIssued.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reqDerivedKeysIssuedActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(encryptOrderChBox, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_EncryptOrderLabel")); // NOI18N
        encryptOrderChBox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        encryptOrderChBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        encryptOrderChBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                encryptOrderChBoxActionPerformed(evt);
            }
        });

        issuerAddressLabel.setLabelFor(issuerAddressField);
        org.openide.awt.Mnemonics.setLocalizedText(issuerAddressLabel, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_IssuerAddress")); // NOI18N

        issuerAddressField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                issuerAddressFieldKeyReleased(evt);
            }
        });

        issuerMetadataLabel.setLabelFor(issuerMetadataField);
        org.openide.awt.Mnemonics.setLocalizedText(issuerMetadataLabel, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_IssuerMetadataAddress")); // NOI18N

        issuerMetadataField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                issuerMetadataFieldKeyReleased(evt);
            }
        });

        tokenTypeLabel.setLabelFor(tokenTypeCombo);
        org.openide.awt.Mnemonics.setLocalizedText(tokenTypeLabel, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_RSTTokenType")); // NOI18N

        keyTypeLabel.setLabelFor(keyTypeCombo);
        org.openide.awt.Mnemonics.setLocalizedText(keyTypeLabel, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_RSTKeyType")); // NOI18N

        keySizeLabel.setLabelFor(keySizeCombo);
        org.openide.awt.Mnemonics.setLocalizedText(keySizeLabel, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_RSTKeySize")); // NOI18N

        tokenTypeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tokenTypeComboActionPerformed(evt);
            }
        });

        keyTypeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keyTypeComboActionPerformed(evt);
            }
        });

        keySizeCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keySizeComboActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(reqDerivedKeys, org.openide.util.NbBundle.getMessage(STSIssuedSupportingToken.class, "LBL_RequireDerivedKeysX509")); // NOI18N
        reqDerivedKeys.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        reqDerivedKeys.setMargin(new java.awt.Insets(0, 0, 0, 0));
        reqDerivedKeys.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reqDerivedKeysActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(reqSigConfChBox)
                    .addComponent(secConvChBox)
                    .addComponent(derivedKeysChBox)
                    .addComponent(encryptSignatureChBox)
                    .addComponent(encryptOrderChBox)
                    .addComponent(reqDerivedKeys)
                    .addComponent(reqDerivedKeysIssued)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(issuerMetadataLabel)
                            .addComponent(layoutLabel)
                            .addComponent(algoSuiteLabel)
                            .addComponent(tokenTypeLabel)
                            .addComponent(keyTypeLabel)
                            .addComponent(keySizeLabel)
                            .addComponent(issuerAddressLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(tokenTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(keySizeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(keyTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(algoSuiteCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(layoutCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(issuerMetadataField, javax.swing.GroupLayout.DEFAULT_SIZE, 159, Short.MAX_VALUE)
                            .addComponent(issuerAddressField))))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {algoSuiteCombo, keySizeCombo, keyTypeCombo, layoutCombo, tokenTypeCombo});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(issuerAddressLabel)
                    .addComponent(issuerAddressField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(issuerMetadataLabel)
                    .addComponent(issuerMetadataField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tokenTypeLabel)
                    .addComponent(tokenTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keyTypeLabel)
                    .addComponent(keyTypeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(keySizeLabel)
                    .addComponent(keySizeCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(algoSuiteLabel)
                    .addComponent(algoSuiteCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(layoutLabel)
                    .addComponent(layoutCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(reqDerivedKeys)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(reqDerivedKeysIssued)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(secConvChBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(derivedKeysChBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(reqSigConfChBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(encryptSignatureChBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(encryptOrderChBox)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {algoSuiteCombo, keySizeCombo, keyTypeCombo, layoutCombo, tokenTypeCombo});

    }// </editor-fold>//GEN-END:initComponents

    private void reqDerivedKeysActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reqDerivedKeysActionPerformed
        setValue(reqDerivedKeys);
    }//GEN-LAST:event_reqDerivedKeysActionPerformed

    private void keySizeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keySizeComboActionPerformed
        setValue(keySizeCombo);
    }//GEN-LAST:event_keySizeComboActionPerformed

    private void keyTypeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keyTypeComboActionPerformed
        setValue(keyTypeCombo);
    }//GEN-LAST:event_keyTypeComboActionPerformed

    private void tokenTypeComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tokenTypeComboActionPerformed
        setValue(tokenTypeCombo);
    }//GEN-LAST:event_tokenTypeComboActionPerformed

    private void issuerMetadataFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_issuerMetadataFieldKeyReleased
        setValue(issuerMetadataField);
    }//GEN-LAST:event_issuerMetadataFieldKeyReleased

    private void issuerAddressFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_issuerAddressFieldKeyReleased
        setValue(issuerAddressField);
    }//GEN-LAST:event_issuerAddressFieldKeyReleased

    private void encryptOrderChBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encryptOrderChBoxActionPerformed
         setValue(encryptOrderChBox);
    }//GEN-LAST:event_encryptOrderChBoxActionPerformed

    private void reqDerivedKeysIssuedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reqDerivedKeysIssuedActionPerformed
         setValue(reqDerivedKeysIssued);
    }//GEN-LAST:event_reqDerivedKeysIssuedActionPerformed

    private void reqSigConfChBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reqSigConfChBoxActionPerformed
         setValue(reqSigConfChBox);
    }//GEN-LAST:event_reqSigConfChBoxActionPerformed

    private void derivedKeysChBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_derivedKeysChBoxActionPerformed
         setValue(derivedKeysChBox);
    }//GEN-LAST:event_derivedKeysChBoxActionPerformed

    private void secConvChBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secConvChBoxActionPerformed
        setValue(secConvChBox);
    }//GEN-LAST:event_secConvChBoxActionPerformed

    private void encryptSignatureChBox(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encryptSignatureChBox
        setValue(encryptSignatureChBox);
    }//GEN-LAST:event_encryptSignatureChBox

    private void layoutComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_layoutComboActionPerformed
        setValue(layoutCombo);
    }//GEN-LAST:event_layoutComboActionPerformed

    private void algoSuiteComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_algoSuiteComboActionPerformed
        setValue(algoSuiteCombo);
    }//GEN-LAST:event_algoSuiteComboActionPerformed
            
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox algoSuiteCombo;
    private javax.swing.JLabel algoSuiteLabel;
    private javax.swing.JCheckBox derivedKeysChBox;
    private javax.swing.JCheckBox encryptOrderChBox;
    private javax.swing.JCheckBox encryptSignatureChBox;
    private javax.swing.JTextField issuerAddressField;
    private javax.swing.JLabel issuerAddressLabel;
    private javax.swing.JTextField issuerMetadataField;
    private javax.swing.JLabel issuerMetadataLabel;
    private javax.swing.JComboBox keySizeCombo;
    private javax.swing.JLabel keySizeLabel;
    private javax.swing.JComboBox keyTypeCombo;
    private javax.swing.JLabel keyTypeLabel;
    private javax.swing.JComboBox layoutCombo;
    private javax.swing.JLabel layoutLabel;
    private javax.swing.JCheckBox reqDerivedKeys;
    private javax.swing.JCheckBox reqDerivedKeysIssued;
    private javax.swing.JCheckBox reqSigConfChBox;
    private javax.swing.JCheckBox secConvChBox;
    private javax.swing.JComboBox tokenTypeCombo;
    private javax.swing.JLabel tokenTypeLabel;
    // End of variables declaration//GEN-END:variables
    
}
