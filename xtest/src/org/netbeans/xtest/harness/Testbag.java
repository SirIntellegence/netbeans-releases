/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2003 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

/*
 * Testbag.java
 *
 * Created on 25th March 2003, 15:46
 */

package org.netbeans.xtest.harness;

import org.netbeans.xtest.xmlserializer.*;

/**
 *
 * @author  lm97939
 */

public class Testbag implements XMLSerializable {

    private String name;
    private String testattribs;
    private String executor;
    private String compiler;
    private String resultsprocessor;
    private int prio = Integer.MAX_VALUE;
    private int timeout = -1;
    
    private Testset testsets[];
    
    private MTestConfig.AntExecType ant_executor;
    private MTestConfig.AntExecType ant_compiler;
    private MTestConfig.AntExecType ant_resultsprocessor;
    
    static ClassMappingRegistry classMappingRegistry = new ClassMappingRegistry(Testbag.class);
    static {
        try {
            // register this class
            classMappingRegistry.registerSimpleField("name",ClassMappingRegistry.ATTRIBUTE,"name");
            classMappingRegistry.registerSimpleField("testattribs",ClassMappingRegistry.ATTRIBUTE,"testattribs");
            classMappingRegistry.registerSimpleField("executor",ClassMappingRegistry.ATTRIBUTE,"executor");
            classMappingRegistry.registerSimpleField("compiler",ClassMappingRegistry.ATTRIBUTE,"compiler");
            classMappingRegistry.registerSimpleField("resultsprocessor",ClassMappingRegistry.ATTRIBUTE,"resultsprocessor");
            classMappingRegistry.registerSimpleField("prio",ClassMappingRegistry.ATTRIBUTE,"prio");
            classMappingRegistry.registerSimpleField("timeout",ClassMappingRegistry.ATTRIBUTE,"timeout");
            classMappingRegistry.registerContainerField("testsets","testset",ClassMappingRegistry.DIRECT);
            
        } catch (MappingException me) {
            me.printStackTrace();
            classMappingRegistry = null;
        }
    }
    
    public ClassMappingRegistry registerXMLMapping() {
        return classMappingRegistry;
    }

    public String getName() {
        return name;
    }
    
    public int getPrio() {
        return prio;
    }

    public String getExecutorName() {
        return executor;
    }

    public String getCompilerName() {
        return compiler;
    }

    public String getResultsprocessorName() {
        return resultsprocessor;
    }
    
    protected void setAntExecutor(MTestConfig.AntExecType type) {
        ant_executor = type;
    }

    protected void setAntCompiler(MTestConfig.AntExecType type) {
        ant_compiler = type;
    }

    protected void setAntResultsprocessor(MTestConfig.AntExecType type) {
        ant_resultsprocessor = type;
    }
    
    protected void validate() throws XMLSerializeException {
        if (name == null)
            throw new XMLSerializeException("Attribute name is required for element testbag.");
        if (testattribs == null)
            throw new XMLSerializeException("Attribute testattribs is required for element testbag.");
        if (testsets == null || testsets.length ==0)
            throw new XMLSerializeException("Al least one element testset is required under element testbag.");
        for (int i=0; i<testsets.length; i++) {
            testsets[i].validate();   
        }
    }

    
    // public inner classes    
    
    public static class Testset implements XMLSerializable {
        private String dir;
        private Patternset patternsets[];

        static ClassMappingRegistry classMappingRegistry = new ClassMappingRegistry(Testbag.Testset.class);
        static {
            try {
                // register this class
                classMappingRegistry.registerSimpleField("dir",ClassMappingRegistry.ATTRIBUTE,"dir");
                classMappingRegistry.registerContainerField("patternsets","patternset",ClassMappingRegistry.DIRECT);
            } catch (MappingException me) {
                me.printStackTrace();
                classMappingRegistry = null;
            }
        }

        public ClassMappingRegistry registerXMLMapping() {
            return classMappingRegistry;
        }
        
        protected void validate() throws XMLSerializeException {
            if (dir == null)
                throw new XMLSerializeException("Attribute dir is required for element testset");
            if (patternsets != null) 
                for (int i=0; i<patternsets.length; i++) 
                    patternsets[i].validate();
        }
    }
    
    
    public static class Patternset implements XMLSerializable {
        private InExclude includes[];
        private InExclude excludes[];
        private String patternattribs;

        static ClassMappingRegistry classMappingRegistry = new ClassMappingRegistry(Testbag.Patternset.class);
        static {
            try {
                // register this class
                classMappingRegistry.registerSimpleField("patternattribs",ClassMappingRegistry.ATTRIBUTE,"patternattribs");
                classMappingRegistry.registerContainerField("includes","include",ClassMappingRegistry.DIRECT);
                classMappingRegistry.registerContainerField("excludes","exclude",ClassMappingRegistry.DIRECT);
            } catch (MappingException me) {
                me.printStackTrace();
                classMappingRegistry = null;
            }
        }

        public ClassMappingRegistry registerXMLMapping() {
            return classMappingRegistry;
        }
        
        protected void validate() throws XMLSerializeException {
            if (includes != null) 
               for (int i=0; i<includes.length; i++) 
                    includes[i].validate();
            if (excludes != null) 
               for (int i=0; i<excludes.length; i++) 
                    excludes[i].validate();
        }
    }
    
    public static class InExclude implements XMLSerializable {
        private String name;
        private String expectedFail;

        static ClassMappingRegistry classMappingRegistry = new ClassMappingRegistry(Testbag.InExclude.class);
        static {
            try {
                // register this class
                classMappingRegistry.registerSimpleField("expectedFail",ClassMappingRegistry.ATTRIBUTE,"expectedFail");
                classMappingRegistry.registerSimpleField("name",ClassMappingRegistry.ATTRIBUTE,"name");
            } catch (MappingException me) {
                me.printStackTrace();
                classMappingRegistry = null;
            }
        }

        public ClassMappingRegistry registerXMLMapping() {
            return classMappingRegistry;
        }
        
        protected void validate() throws XMLSerializeException {
            if (name == null)
                throw new XMLSerializeException("Attribute name is required for element include/exclude.");
        }
    }

}
