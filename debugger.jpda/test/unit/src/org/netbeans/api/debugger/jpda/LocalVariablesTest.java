/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2004 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.api.debugger.jpda;

import java.net.URL;
import java.util.*;

/**
 * Tests information about local variables.
 *
 * @author Maros Sandor
 */
public class LocalVariablesTest extends DebuggerJPDAApiTestBase {

    private JPDASupport     support;
    private JPDADebugger    debugger;

    private static final String CLASS_NAME = "org.netbeans.api.debugger.jpda.testapps.LocalVariablesApp";

    public LocalVariablesTest(String s) {
        super(s);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testWatches() throws Exception {
        try {
            ClassLoader cl = this.getClass().getClassLoader();
            URL url = cl.getResource(CLASS_NAME.replace('.', '/') + ".class");
            LineBreakpoint lb = LineBreakpoint.create(url.toString(), 34);
            dm.addBreakpoint(lb);

            support = JPDASupport.attach (CLASS_NAME);
            debugger = support.getDebugger();

            support.waitState (JPDADebugger.STATE_STOPPED);  // breakpoint hit

            CallStackFrame sf = debugger.getCurrentCallStackFrame();
            assertEquals("Debugger stopped at wrong line", lb.getLineNumber(), sf.getLineNumber(null));

            LocalVariable [] vars = sf.getLocalVariables();
            assertEquals("Wrong number of local variables", 4, vars.length);
            Arrays.sort(vars, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((LocalVariable)o1).getName().compareTo(((LocalVariable)o2).getName());
                }
            });
            assertEquals("Wrong info about local variables", "g", vars[0].getName());
            assertEquals("Wrong info about local variables", "20", vars[0].getValue());
            assertEquals("Wrong info about local variables", "int", vars[0].getDeclaredType());
            assertEquals("Wrong info about local variables", "int", vars[0].getType());
            assertEquals("Wrong info about local variables", CLASS_NAME, vars[0].getClassName());

            assertEquals("Wrong info about local variables", "s", vars[1].getName());
            assertEquals("Wrong info about local variables", "\"asdfghjkl\"", vars[1].getValue());
            assertEquals("Wrong info about local variables", "java.lang.Object", vars[1].getDeclaredType());
            assertEquals("Wrong info about local variables", "java.lang.String", vars[1].getType());
            assertEquals("Wrong info about local variables", CLASS_NAME, vars[1].getClassName());

            assertEquals("Wrong info about local variables", "x", vars[2].getName());
            assertEquals("Wrong info about local variables", "40", vars[2].getValue());
            assertEquals("Wrong info about local variables", "int", vars[2].getDeclaredType());
            assertEquals("Wrong info about local variables", "int", vars[2].getType());
            assertEquals("Wrong info about local variables", CLASS_NAME, vars[2].getClassName());

            assertEquals("Wrong info about local variables", "y", vars[3].getName());
            assertEquals("Wrong info about local variables", "50.5", vars[3].getValue());
            assertEquals("Wrong info about local variables", "float", vars[3].getDeclaredType());
            assertEquals("Wrong info about local variables", "float", vars[3].getType());
            assertEquals("Wrong info about local variables", CLASS_NAME, vars[3].getClassName());

            support.stepOver();
            support.stepOver();

            vars = sf.getLocalVariables();
            assertEquals("Wrong number of local variables", 4, vars.length);
            Arrays.sort(vars, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((LocalVariable)o1).getName().compareTo(((LocalVariable)o2).getName());
                }
            });
            assertEquals("Wrong info about local variables", "g", vars[0].getName());
            assertEquals("Wrong info about local variables", "\"ad\"", vars[0].getValue());
            assertEquals("Wrong info about local variables", "java.lang.CharSequence", vars[0].getDeclaredType());
            assertEquals("Wrong info about local variables", "java.lang.String", vars[0].getType());
            assertEquals("Wrong info about local variables", CLASS_NAME, vars[0].getClassName());

        } finally {
            support.doFinish();
        }
    }
}
