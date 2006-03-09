/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.openide.util.lookup;

import junit.framework.TestCase;
import junit.framework.*;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup.ReferenceIterator;
import org.openide.util.lookup.AbstractLookup.ReferenceToResult;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 *
 * @author Jaroslav Tulach
 */
public class InheritanceTreeTest extends TestCase {
    
    public InheritanceTreeTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testDeserOfNode() {
        InheritanceTree inh = new InheritanceTree();
        InheritanceTree.Node n = new InheritanceTree.Node(String.class);
        n.markDeserialized();
        n.markDeserialized();

        n.assignItem(inh, new InstanceContent.SimpleItem("Ahoj"));
    }
    
}
