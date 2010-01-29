/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

package org.netbeans.modules.cnd.toolchain.compilers.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.cnd.toolchain.api.CompilerFlavor;
import org.netbeans.modules.cnd.toolchain.api.CompilerProvider;
import org.netbeans.modules.cnd.toolchain.api.CompilerSet;
import org.netbeans.modules.cnd.toolchain.api.PlatformTypes;
import org.netbeans.modules.cnd.toolchain.api.Tool;
import org.netbeans.modules.cnd.toolchain.api.ToolchainManager.BaseFolder;
import org.netbeans.modules.cnd.toolchain.api.ToolchainManager.CMakeDescriptor;
import org.netbeans.modules.cnd.toolchain.api.ToolchainManager.CompilerDescriptor;
import org.netbeans.modules.cnd.toolchain.api.ToolchainManager.DebuggerDescriptor;
import org.netbeans.modules.cnd.toolchain.api.ToolchainManager.LinkerDescriptor;
import org.netbeans.modules.cnd.toolchain.api.ToolchainManager.MakeDescriptor;
import org.netbeans.modules.cnd.toolchain.api.ToolchainManager.QMakeDescriptor;
import org.netbeans.modules.cnd.toolchain.api.ToolchainManager.ScannerDescriptor;
import org.netbeans.modules.cnd.toolchain.api.ToolchainManager.ToolchainDescriptor;
import org.netbeans.modules.cnd.utils.CndUtils;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironmentFactory;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.openide.util.NbBundle;

/**
 * A container for information about a set of related compilers, typicaly from a vendor or
 * redistributor.
 */
public class CompilerSetImpl implements CompilerSet {

    @Override
    public boolean isAutoGenerated() {
        return autoGenerated;
    }

    @Override
    public void setAutoGenerated(boolean autoGenerated) {
        this.autoGenerated = autoGenerated;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean isUrlPointer(){
        if (getDirectory() == null || getDirectory().length() == 0){
            return flavor.getToolchainDescriptor().getUpdateCenterUrl() != null && flavor.getToolchainDescriptor().getModuleID() != null;
        }
        return false;
    }

    void setAsDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public void unsetDefault() {
        this.isDefault = false;  // to set to true use CompilerSetManager.setDefault()
    }

    private CompilerFlavor flavor;
    private String name;
    private String displayName;
    private boolean autoGenerated;
    private boolean isDefault;
    private StringBuilder directory = new StringBuilder(256);
    private final ArrayList<Tool> tools = new ArrayList<Tool>();
    private String librarySearchOption;
    private String dynamicLibrarySearchOption;
    private String libraryOption;
    private CompilerProvider compilerProvider;
    private String driveLetterPrefix = "/"; // NOI18N
    private Map<Integer,String> pathSearch;
    private boolean isSunStudioDefault;

    /** Creates a new instance of CompilerSet */
    public CompilerSetImpl(CompilerFlavor flavor, String directory, String name) {
        addDirectory(directory);

        compilerProvider = CompilerProvider.getInstance();
        driveLetterPrefix = flavor.getToolchainDescriptor().getDriveLetterPrefix();

        if (name != null) {
            this.name = name;
        } else {
            this.name = flavor.toString();
        }
        //displayName = mapNameToDisplayName(flavor);
        displayName = flavor.getToolchainDescriptor().getDisplayName();
        librarySearchOption = flavor.getToolchainDescriptor().getLinker().getLibrarySearchFlag();
        dynamicLibrarySearchOption = flavor.getToolchainDescriptor().getLinker().getDynamicLibrarySearchFlag();
        libraryOption = flavor.getToolchainDescriptor().getLinker().getLibraryFlag();
        this.flavor = flavor;
        this.autoGenerated = true;
        this.isDefault = false;
    }

    public CompilerSetImpl(int platform) {
        this.name = None;
        this.flavor = CompilerFlavorImpl.getUnknown(platform);
        this.displayName = NbBundle.getMessage(CompilerSet.class, "LBL_EmptyCompilerSetDisplayName"); // NOI18N

        compilerProvider = CompilerProvider.getInstance();
        this.autoGenerated = true;
        this.isDefault = false;
    }

    @Override
    public CompilerSet createCopy() {
        CompilerSetImpl copy = new CompilerSetImpl(flavor, getDirectory(), name);
        copy.setAutoGenerated(isAutoGenerated());
        copy.setAsDefault(isDefault());

        for (Tool tool : getTools()) {
            copy.addTool(tool.createCopy());
        }

        return copy;
    }

    /**
     * If no compilers are found an empty compiler set is created so we don't have an empty list.
     * Too many places in CND expect a non-empty list and throw NPEs if it is empty!
     */
    protected static CompilerSet createEmptyCompilerSet(int platform) {
        return new CompilerSetImpl(platform);
    }

    @Override
    public boolean isGnuCompiler() {
        return flavor.isGnuCompiler();
    }

    @Override
    public boolean isSunCompiler() {
        return flavor.isSunStudioCompiler();
    }

    @Override
    public CompilerFlavor getCompilerFlavor() {
        return flavor;
    }

    /*package-local*/ void setFlavor(CompilerFlavor flavor) {
        this.flavor = flavor;
    }

    private void addDirectory(String path) {
        if (path != null) {
            if (directory.length() == 0) {
                directory.append(path);
            } else {
                directory.append(File.pathSeparator);
                directory.append(path);
            }
        }
    }

    @Override
    public String getDirectory() {
        return directory.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDisplayName() {
        // TODO: this thing is never used although it's being set to informative values by personality
        return displayName;
    }

    /*package-local*/ Tool addTool(ExecutionEnvironment env, String name, String path, int kind) {
        if (findTool(kind) != null) {
            return null;
        }
        Tool tool = compilerProvider.createCompiler(env, flavor, kind, name, Tool.getToolDisplayName(kind), path);
        if (!tools.contains(tool)) {
            tools.add(tool);
        }
        tool.setCompilerSet(this);
        return tool;
    }

    /*package-local*/ void addTool(Tool tool) {
        tools.add(tool);
        tool.setCompilerSet(this);
    }

    /*package-local*/ Tool addNewTool(ExecutionEnvironment env, String name, String path, int kind) {
        Tool tool = compilerProvider.createCompiler(env, flavor, kind, name, Tool.getToolDisplayName(kind), path);
        tools.add(tool);
        tool.setCompilerSet(this);
        return tool;
    }

    /**
     * Get the first tool of its kind.
     *
     * @param kind The type of tool to get
     * @return The Tool or null
     */
    @Override
    public Tool getTool(int kind) {
        for (Tool tool : tools) {
            if (tool.getKind() == kind) {
                return tool;
            }
        }
        CndUtils.assertFalse(true, "Should not be here, cuz we should create empty tools in CompilerSetManager"); //NOI18N
        //TODO: remove this code, empty tools should be created in CompilerSetManager
        Tool t;
        // Fixup: all tools should go here ....
        t = compilerProvider.createCompiler(ExecutionEnvironmentFactory.getLocal(),
                getCompilerFlavor(), kind, "", Tool.getToolDisplayName(kind), ""); // NOI18N
        t.setCompilerSet(this);
        synchronized( tools ) { // synchronize this only unpredictable tools modification
            tools.add(t);
        }
        return t;
    }


    /**
     * Get the first tool of its kind.
     *
     * @param kind The type of tool to get
     * @return The Tool or null
     */
    @Override
    public Tool findTool(int kind) {
        for (Tool tool : tools) {
            if (tool.getKind() == kind) {
                return tool;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Tool> getTools() {
        synchronized (tools) {
            return (List<Tool>)tools.clone();
        }
    }

    @Override
    public String getDynamicLibrarySearchOption() {
        return dynamicLibrarySearchOption;
    }

    @Override
    public String getLibrarySearchOption() {
        return librarySearchOption;
    }

    @Override
    public String getLibraryOption() {
        return libraryOption;
    }

    private String getDriveLetterPrefix() {
        return driveLetterPrefix;
    }

    @Override
    public String getQmakeSpec(int platform) {
        String qmakespec = flavor.getToolchainDescriptor().getQmakeSpec();
        if (qmakespec != null && 0 <= qmakespec.indexOf("${os}")) { // NOI18N
            String os = null;
            switch (platform) {
                case PlatformTypes.PLATFORM_LINUX:
                    os = "linux"; // NOI18N
                    break;
                case PlatformTypes.PLATFORM_MACOSX:
                    os = "macx"; // NOI18N
                    break;
                case PlatformTypes.PLATFORM_SOLARIS_INTEL:
                case PlatformTypes.PLATFORM_SOLARIS_SPARC:
                    os = "solaris"; // NOI18N
                    break;
                case PlatformTypes.PLATFORM_WINDOWS:
                    os = "win32"; // NOI18N
                    break;
            }
            if (os == null) {
                qmakespec = null;
            } else {
                qmakespec = qmakespec.replaceAll("\\$\\{os\\}", os); // NOI18N
            }
        }
        return qmakespec;
    }

    /**
     * Converts absolute Windows paths to paths without the ':'.
     * Example: C:/abc/def.c -> /cygdrive/c/def/c
     */
    @Override
    public String normalizeDriveLetter(String path) {
        if (path.length() > 1 && path.charAt(1) == ':') { // NOI18N
            return getDriveLetterPrefix() + path.charAt(0) + path.substring(2); // NOI18N
        }
        return path;
    }

    /*package-local*/ void addPathCandidate(int tool, String path) {
        if (pathSearch == null){
            pathSearch = new HashMap<Integer, String>();
        }
        pathSearch.put(tool, path);
    }

    /*package-local*/String getPathCandidate(int tool){
        if (pathSearch == null){
            return null;
        }
        return pathSearch.get(tool);
    }

    /*package-local*/void setSunStudioDefault(boolean isSunStudioDefault){
        this.isSunStudioDefault = isSunStudioDefault;
    }

    /*package-local*/boolean isSunStudioDefault(){
        return isSunStudioDefault;
    }

    @Override
    public String toString() {
        return name;
    }

    public static class UnknownToolchainDescriptor implements ToolchainDescriptor {

        @Override
        public String getFileName() {
            return ""; // NOI18N
        }

        @Override
        public String getName() {
            return ""; // NOI18N
        }

        @Override
        public String getDisplayName() {
            return ""; // NOI18N
        }

        @Override
        public String[] getFamily() {
            return new String[]{};
        }

        @Override
        public String[] getPlatforms() {
            return new String[]{};
        }

        @Override
        public String getUpdateCenterUrl() {
            return null;
        }

        @Override
        public String getUpdateCenterDisplayName() {
            return null;
        }

        @Override
        public String getUpgradeUrl() {
            return null;
        }

        @Override
        public String getModuleID() {
            return null;
        }

        @Override
        public boolean isAbstract() {
            return true;
        }

        @Override
        public String getDriveLetterPrefix() {
            return ""; // NOI18N
        }

        @Override
        public List<BaseFolder> getBaseFolders() {
            return Collections.<BaseFolder>emptyList();
        }

        @Override
        public List<BaseFolder> getCommandFolders() {
            return Collections.<BaseFolder>emptyList();
        }

        @Override
        public String getQmakeSpec() {
            return ""; // NOI18N
        }

        @Override
        public CompilerDescriptor getC() {
            return null;
        }

        @Override
        public CompilerDescriptor getCpp() {
            return null;
        }

        @Override
        public CompilerDescriptor getFortran() {
            return null;
        }

        @Override
        public CompilerDescriptor getAssembler() {
            return null;
        }

        @Override
        public ScannerDescriptor getScanner() {
            return null;
        }

        @Override
        public LinkerDescriptor getLinker() {
            return new LinkerDescriptor(){

                @Override
                public String getLibraryPrefix() {
                    return ""; // NOI18N
                }

                @Override
                public String getLibrarySearchFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getDynamicLibrarySearchFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getLibraryFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getPICFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getStaticLibraryFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getDynamicLibraryFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getDynamicLibraryBasicFlag() {
                    return ""; // NOI18N
                }

                @Override
                public String getOutputFileFlag() {
                    return ""; // NOI18N
                }
            };
        }

        @Override
        public MakeDescriptor getMake() {
            return null;
        }

        @Override
        public Map<String, List<String>> getDefaultLocations() {
            return Collections.<String, List<String>>emptyMap();
        }

        @Override
        public DebuggerDescriptor getDebugger() {
            return null;
        }

        @Override
        public String getMakefileWriter() {
            return null;
        }

        @Override
        public QMakeDescriptor getQMake() {
            return null;
        }

        @Override
        public CMakeDescriptor getCMake() {
            return null;
        }

    }
}
