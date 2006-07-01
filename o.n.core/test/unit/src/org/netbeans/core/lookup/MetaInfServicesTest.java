/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
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

package org.netbeans.core.lookup;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.print.PrintServiceLookup;
import org.netbeans.Module;
import org.netbeans.ModuleManager;
import org.netbeans.core.startup.ModuleHistory;
import org.netbeans.junit.NbTestCase;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Mutex;
import org.openide.util.MutexException;

/** Test whether modules can really register things in their META-INF/services/class.Name
 * files, and whether this behaves correctly when the modules are disabled/enabled.
 * Note that Plain loads its classpath modules as soon as you ask for it, so these
 * tests do not check what happens on the NetBeans startup classpath.
 * @author Jesse Glick
 */
public class MetaInfServicesTest extends NbTestCase {

    public MetaInfServicesTest(String name) {
        super(name);
    }
    
    private ModuleManager mgr;
    private Module m1, m2;
    protected void setUp() throws Exception {
        //System.err.println("setUp");
        //Thread.dumpStack();
        clearWorkDir();
        // Load Plain.
        // Make a couple of modules.
        mgr = org.netbeans.core.startup.Main.getModuleSystem().getManager();
        try {
            mgr.mutex().writeAccess(new Mutex.ExceptionAction() {
                public Object run() throws Exception {
                    File jar1 = InstanceDataObjectModuleTestHid.toFile(MetaInfServicesTest.class.getResource("data/services-jar-1.jar"));
                    File jar2 = InstanceDataObjectModuleTestHid.toFile(MetaInfServicesTest.class.getResource("data/services-jar-2.jar"));
                    m1 = mgr.create(jar1, new ModuleHistory(jar1.getAbsolutePath()), false, false, false);
                    m2 = mgr.create(jar2, new ModuleHistory(jar2.getAbsolutePath()), false, false, false);
                    return null;
                }
            });
        } catch (MutexException me) {
            throw me.getException();
        }
        assertEquals(Collections.EMPTY_SET, m1.getProblems());
    }
    protected void tearDown() throws Exception {
        try {
            mgr.mutex().writeAccess(new Mutex.ExceptionAction() {
                public Object run() throws Exception {
                    if (m2.isEnabled()) mgr.disable(m2);
                    mgr.delete(m2);
                    if (m1.isEnabled()) mgr.disable(m1);
                    mgr.delete(m1);
                    return null;
                }
            });
        } catch (MutexException me) {
            throw me.getException();
        }
        m1 = null;
        m2 = null;
        mgr = null;
    }
    protected static final int TWIDDLE_ENABLE = 0;
    protected static final int TWIDDLE_DISABLE = 1;
    protected void twiddle(final Module m, final int action) throws Exception {
        try {
            mgr.mutex().writeAccess(new Mutex.ExceptionAction() {
                public Object run() throws Exception {
                    switch (action) {
                    case TWIDDLE_ENABLE:
                        mgr.enable(m);
                        break;
                    case TWIDDLE_DISABLE:
                        mgr.disable(m);
                        break;
                    default:
                        throw new IllegalArgumentException("bad action: " + action);
                    }
                    return null;
                }
            });
        } catch (MutexException me) {
            throw me.getException();
        }
    }
    
    /** Fails to work if you have >1 method per class, because setUp gets run more
     * than once (XTest bug I suppose).
     */
    public void testEverything() throws Exception {
        twiddle(m1, TWIDDLE_ENABLE);
        ClassLoader systemClassLoader = Lookup.getDefault().lookup(ClassLoader.class);
        Class xface = systemClassLoader.loadClass("org.foo.Interface");
        Lookup.Result r = Lookup.getDefault().lookupResult(xface);
        List instances = new ArrayList(r.allInstances());
        // Expect to get Impl1 from first JAR.
        assertEquals(1, instances.size());
        Object instance1 = instances.get(0);
        assertTrue(xface.isInstance(instance1));
        assertEquals("org.foo.impl.Implementation1", instance1.getClass().getName());
        // Expect to have (same) Impl1 + Impl2.
        LookupL l = new LookupL();
        r.addLookupListener(l);
        twiddle(m2, TWIDDLE_ENABLE);
        assertTrue("Turning on a second module with a manifest service fires a lookup change", l.gotSomething());
        instances = new ArrayList(r.allInstances());
        assertEquals(2, instances.size());
        assertEquals(instance1, instances.get(0));
        assertEquals("org.bar.Implementation2", instances.get(1).getClass().getName());
        // Expect to lose Impl2.
        l.count = 0;
        twiddle(m2, TWIDDLE_DISABLE);
        assertTrue(l.gotSomething());
        instances = new ArrayList(r.allInstances());
        assertEquals(1, instances.size());
        assertEquals(instance1, instances.get(0));
        // Expect to lose Impl1 too.
        l.count = 0;
        twiddle(m1, TWIDDLE_DISABLE);
        assertTrue(l.gotSomething());
        instances = new ArrayList(r.allInstances());
        assertEquals(0, instances.size());
        // Expect to not get anything: wrong xface version
        l.count = 0;
        twiddle(m1, TWIDDLE_ENABLE);
        // not really important: assertFalse(l.gotSomething());
        instances = new ArrayList(r.allInstances());
        assertEquals(0, instances.size());
        systemClassLoader = Lookup.getDefault().lookup(ClassLoader.class);
        Class xface2 = systemClassLoader.loadClass("org.foo.Interface");
        assertTrue(xface != xface2);
        Lookup.Result r2 = Lookup.getDefault().lookupResult(xface2);
        instances = new ArrayList(r2.allInstances());
        assertEquals(1, instances.size());
        // Let's also check up on some standard JDK services.
        PrintServiceLookup psl = Lookup.getDefault().lookup(PrintServiceLookup.class);
        assertNotNull("Some META-INF/services/javax.print.PrintServiceLookup was found in " + Lookup.getDefault(), psl);
    }
    
    protected static final class LookupL implements LookupListener {
        public int count = 0;
        public synchronized void resultChanged(LookupEvent ev) {
            count++;
            notifyAll();
        }
        public synchronized boolean gotSomething() throws InterruptedException {
            if (count > 0) return true;
            wait(9999);
            return count > 0;
        }
    }
    
}
