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

import org.netbeans.api.debugger.DebuggerInfo;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.jpda.AttachingDICookie;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.spi.debugger.SessionProvider;
import org.netbeans.spi.debugger.ContextProvider;


/**
 *
 * @author Jan Jancura
 */
public class AttachingSessionProvider extends SessionProvider {
    
    private ContextProvider contextProvider;
    private AttachingDICookie sadic;
    
    public AttachingSessionProvider (ContextProvider contextProvider) {
        this.contextProvider = contextProvider;
        sadic = (AttachingDICookie) contextProvider.lookupFirst 
            (null, AttachingDICookie.class);
    };
    
    public String getSessionName () {
        String processName = (String) contextProvider.lookupFirst 
            (null, String.class);
        if (processName != null)
            return processName;
        if (sadic.getHostName () != null)
            return sadic.getHostName () + ":" + sadic.getPortNumber ();
        return LaunchingSessionProvider.findUnique 
            (sadic.getSharedMemoryName ());
    };
    
    public String getLocationName () {
        if (sadic.getHostName () != null)
            return sadic.getHostName ();
        return "localhost";
    }
    
    public String getTypeID () {
        return JPDADebugger.SESSION_ID;
    }
    
    public Object[] getServices () {
        return new Object [0];
    }
}

