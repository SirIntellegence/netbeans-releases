/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.cnd.makeproject.api.configurations;

import org.netbeans.modules.cnd.makeproject.configurations.ui.IntNodeProp;
import org.netbeans.modules.cnd.makeproject.configurations.ui.OptionsNodeProp;
import org.netbeans.modules.cnd.makeproject.configurations.ui.StringNodeProp;
import org.netbeans.modules.cnd.api.utils.CppUtils;
import org.netbeans.modules.cnd.makeproject.api.compilers.BasicCompiler;
import org.netbeans.modules.cnd.makeproject.api.compilers.CCCCompiler;
import org.netbeans.modules.cnd.makeproject.api.compilers.CompilerSet;
import org.netbeans.modules.cnd.makeproject.api.compilers.CompilerSets;
import org.netbeans.modules.cnd.makeproject.api.compilers.Tool;
import org.openide.nodes.Sheet;
import org.openide.util.NbBundle;

public class CCompilerConfiguration extends CCCCompilerConfiguration implements AllOptionsProvider {
    // Constructors
    public CCompilerConfiguration(String baseDir, CCompilerConfiguration master) {
        super(baseDir, master);
    }
    
    // Clone and assign
    public void assign(CCompilerConfiguration conf) {
        // From XCompiler
        super.assign(conf);
    }
    
    public Object clone() {
        CCompilerConfiguration clone = new CCompilerConfiguration(getBaseDir(), (CCompilerConfiguration)getMaster());
        // BasicCompilerConfiguration
        clone.setDevelopmentMode((IntConfiguration)getDevelopmentMode().clone());
        clone.setWarningLevel((IntConfiguration)getWarningLevel().clone());
        clone.setMTLevel((IntConfiguration)getMTLevel().clone());
        clone.setSixtyfourBits((BooleanConfiguration)getSixtyfourBits().clone());
        clone.setStrip((BooleanConfiguration)getStrip().clone());
        clone.setAdditionalDependencies((StringConfiguration)getAdditionalDependencies().clone());
        clone.setTool((StringConfiguration)getTool().clone());
        clone.setCommandLineConfiguration((OptionsConfiguration)getCommandLineConfiguration().clone());
        // From CCCCompiler
        clone.setMTLevel((IntConfiguration)getMTLevel().clone());
        clone.setLibraryLevel((IntConfiguration)getLibraryLevel().clone());
        clone.setStandardsEvolution((IntConfiguration)getStandardsEvolution().clone());
        clone.setLanguageExt((IntConfiguration)getLanguageExt().clone());
        clone.setIncludeDirectories((VectorConfiguration)getIncludeDirectories().clone());
        clone.setInheritIncludes((BooleanConfiguration)getInheritIncludes().clone());
        clone.setPreprocessorConfiguration((OptionsConfiguration)getPreprocessorConfiguration().clone());
        clone.setInheritPreprocessor((BooleanConfiguration)getInheritPreprocessor().clone());
        return clone;
    }
    
    // Interface OptionsProvider
    public String getOptions(BasicCompiler compiler) {
        String options = "$(COMPILE.c) "; // NOI18N
        options += getAllOptions2(compiler) + " "; // NOI18N
        options += getCommandLineConfiguration().getValue() + " "; // NOI18N
        return CppUtils.reformatWhitespaces(options);
    }
    
    public String getCFlagsBasic(BasicCompiler compiler) {
        CCCCompiler cccCompiler = (CCCCompiler)compiler;
        String options = ""; // NOI18N
        options += cccCompiler.getMTLevelOptions(getMTLevel().getValue()) + " "; // NOI18N
        options += cccCompiler.getStandardsEvolutionOptions(getStandardsEvolution().getValue()) + " "; // NOI18N
        options += cccCompiler.getLanguageExtOptions(getLanguageExt().getValue()) + " "; // NOI18N
        //options += compiler.getStripOption(getStrip().getValue()) + " "; // NOI18N
        options += compiler.getSixtyfourBitsOption(getSixtyfourBits().getValue()) + " "; // NOI18N
        if (getDevelopmentMode().getValue() == DEVELOPMENT_MODE_TEST)
            options += compiler.getDevelopmentModeOptions(DEVELOPMENT_MODE_TEST);
        return CppUtils.reformatWhitespaces(options);
    }
    
    public String getCFlags(BasicCompiler compiler) {
        String options = getCFlagsBasic(compiler) + " "; // NOI18N
        options += getCommandLineConfiguration().getValue() + " "; // NOI18N
        return CppUtils.reformatWhitespaces(options);
    }
    
    public String getAllOptions(BasicCompiler compiler) {
        CCompilerConfiguration master = (CCompilerConfiguration)getMaster();
        
        String options = ""; // NOI18N
        options += getCFlagsBasic(compiler) + " "; // NOI18N
        if (master != null)
            options += master.getCommandLineConfiguration().getValue() + " "; // NOI18N
        options += getAllOptions2(compiler) + " "; // NOI18N
        return CppUtils.reformatWhitespaces(options);
    }
    
    public String getAllOptions2(BasicCompiler compiler) {
        CCompilerConfiguration master = (CCompilerConfiguration)getMaster();
        
        String options = ""; // NOI18N
        options += compiler.getDevelopmentModeOptions(getDevelopmentMode().getValue()) + " "; // NOI18N
        options += compiler.getWarningLevelOptions(getWarningLevel().getValue()) + " "; // NOI18N
        options += compiler.getStripOption(getStrip().getValue()) + " "; // NOI18N
        if (master != null && getInheritPreprocessor().getValue())
            options += master.getPreprocessorConfiguration().getOptions("-D") + " "; // NOI18N
        options += getPreprocessorConfiguration().getOptions("-D") + " " ; // NOI18N
        if (master != null && getInheritIncludes().getValue())
            options += master.getIncludeDirectories().getOption("-I") + " "; // NOI18N
        options += getIncludeDirectories().getOption("-I") + " "; // NOI18N
        return CppUtils.reformatWhitespaces(options);
    }
    
    // Sheet
    public Sheet getGeneralSheet(MakeConfiguration conf) {
        Sheet sheet = new Sheet();
        CompilerSet compilerSet = CompilerSets.getCompilerSet(conf.getCompilerSet().getValue());
        BasicCompiler cCompiler = (BasicCompiler)compilerSet.getTool(Tool.CCompiler);
        
        sheet.put(getSet());
        if (conf.isCompileConfiguration()) {
            sheet.put(getBasicSet());
            if (conf.getCompilerSet().getValue() == CompilerSets.SUN_COMPILER_SET) { // FIXUP: should be moved to SunCCompiler
                Sheet.Set set2 = new Sheet.Set();
                set2.setName("OtherOptions"); // NOI18N
                set2.setDisplayName(getString("OtherOptionsTxt"));
                set2.setShortDescription(getString("OtherOptionsHint"));
                set2.put(new IntNodeProp(getMTLevel(), getMaster() != null ? false : true, "MultithreadingLevel", getString("MultithreadingLevelTxt"), getString("MultithreadingLevelHint"))); // NOI18N
                set2.put(new IntNodeProp(getStandardsEvolution(), getMaster() != null ? false : true, "StandardsEvolution", getString("StandardsEvolutionTxt"), getString("StandardsEvolutionHint"))); // NOI18N
                set2.put(new IntNodeProp(getLanguageExt(), getMaster() != null ? false : true, "LanguageExtensions", getString("LanguageExtensionsTxt"), getString("LanguageExtensionsHint"))); // NOI18N
                sheet.put(set2);
            }
            if (getMaster() != null)
                sheet.put(getInputSet());
            Sheet.Set set4 = new Sheet.Set();
            set4.setName("Tool"); // NOI18N
            set4.setDisplayName(getString("ToolTxt1"));
            set4.setShortDescription(getString("ToolHint1"));
            set4.put(new StringNodeProp(getTool(), cCompiler.getName(), "Tool", getString("ToolTxt2"), getString("ToolHint2"))); // NOI18N
            sheet.put(set4);
        }
        
        return sheet;
    }
    
    public Sheet getCommandLineSheet(Configuration conf) {
        Sheet sheet = new Sheet();
        String[] texts = new String[] {getString("AdditionalOptionsTxt1"), getString("AdditionalOptionsHint"), getString("AdditionalOptionsTxt2"), getString("AllOptionsTxt")};
        CompilerSet compilerSet = CompilerSets.getCompilerSet(((MakeConfiguration)conf).getCompilerSet().getValue());
        BasicCompiler cCompiler = (BasicCompiler)compilerSet.getTool(Tool.CCompiler);
        
        Sheet.Set set2 = new Sheet.Set();
        set2.setName("CommandLine"); // NOI18N
        set2.setDisplayName(getString("CommandLineTxt"));
        set2.setShortDescription(getString("CommandLineHint"));
        set2.put(new OptionsNodeProp(getCommandLineConfiguration(), null, this, cCompiler, null, texts));
        sheet.put(set2);
        
        return sheet;
    }
    
    /** Look up i18n strings here */
    private static String getString(String s) {
        return NbBundle.getMessage(CCompilerConfiguration.class, s);
    }
}
