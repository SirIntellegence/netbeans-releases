/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2000 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package org.netbeans.modules.debugger.jpda;

import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.spi.debugger.DebuggerEngineProvider;
import org.netbeans.spi.debugger.ContextProvider;


/**
 * Represents one debugger plug-in - one Debugger Implementation.
 * Each Debugger Implementation can add support for debugging of some
 * language or environment to the IDE.
 *
 * @author Jan Jancura
 */
public class JavaEngineProvider extends DebuggerEngineProvider {

    private DebuggerEngine.Destructor   desctuctor;
    private Session                     session;  
    
    public JavaEngineProvider (ContextProvider contextProvider) {
        session = (Session) contextProvider.lookupFirst (null, Session.class);
    }
    
    public String[] getLanguages () {
        return new String[] {"Java"};
    }

    public String getEngineTypeID () {
        return JPDADebugger.ENGINE_ID;
    }
    
    public Object[] getServices () {
        return new Object [0];
    }
    
    public void setDestructor (DebuggerEngine.Destructor desctuctor) {
        this.desctuctor = desctuctor;
    }
    
    public DebuggerEngine.Destructor getDestructor () {
        return desctuctor;
    }
    
    public Session getSession () {
        return session;
    }
}

