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

package org.netbeans.nbbuild;

import java.io.*;
import java.util.*;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.*;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;

/** Setting given property to list of clusters on which module depends
 *
 * @author Michal Zlamal
 */
public class GetDependsClusters extends org.apache.tools.ant.Task {
    private String name = null;
    private String propertiesList = null;
    private String thisModuleName = null;
   
    /** Comma separated list of properties. One of those properties should contain the name of module from what it ran. */
    public void setList( String propertiesList ) {
        this.propertiesList = propertiesList;
    }
    
    /** Name of property to set */
    public void setName(String name) {
        this.name = name;
    }

    public void execute() throws BuildException {
        if (name == null)
            throw new BuildException("Name of property to set have to be specified",this.getLocation());
        if (propertiesList == null)
	    throw new BuildException("List of clusters have to be specified",this.getLocation());

	thisModuleName = this.getOwningTarget().getName();
	if (!thisModuleName.startsWith("all-")) 
	    throw new BuildException("This task could be used only in targets \"all-{modulename}\"",this.getLocation());
	thisModuleName = thisModuleName.substring("all-".length());

        HashSet modules = new HashSet();
        
        StringTokenizer tokens = new StringTokenizer( propertiesList, " \t\n\f\r," );
        while (tokens.hasMoreTokens()) {
            String property = tokens.nextToken().trim();
            String list = this.getProject().getProperty( property );
            if (list == null) throw new BuildException("Property: " + property + " is not defined anywhere",this.getLocation());
            StringTokenizer modTokens = new StringTokenizer(list," \t\n\f\r,");
            while (modTokens.hasMoreTokens()) {
                String module = modTokens.nextToken();
                log( property + " " + module, Project.MSG_VERBOSE );
                if (module.equals(thisModuleName)) {
		    String clusterDepends = this.getProject().getProperty(property + ".depends");
                    if (clusterDepends == null) throw new BuildException( "Property: " + property + ".depends have to be defined", this.getLocation());
		    log( "Property: " + name + " will be set to " + clusterDepends, Project.MSG_VERBOSE);
                    this.getProject().setProperty( name, clusterDepends );
                    return;
                }
            }
        }
	    log("No cluster list with this module: " + thisModuleName + " was found. Assume that this module " + thisModuleName + " depends on all clusters: " + propertiesList, Project.MSG_WARN);
	log( "Property: " + name + " will be set to " + propertiesList, Project.MSG_VERBOSE);
	this.getProject().setProperty( name, propertiesList );
	//	throw new BuildException("No cluster list with this module: " + thisModuleName + " was found.",this.getLocation());
    }
}
