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
/*
 * J2EEBaseVersion.java
 *
 * Created on February 25, 2004, 2:36 PM
 */

package org.netbeans.modules.j2ee.sun.share.configbean;

/**
 *  Base class to relate enumerated types of various J2EE versions.
 *
 * @author Peter Williams
 */
public abstract class J2EEBaseVersion implements Comparable {	

	/** -----------------------------------------------------------------------
	 *  Implementation
	 */
	private final String j2eeModuleVersion;
	private final int numericVersion;
	
	private final String publicId;
	private final String systemId;
	
	/** Creates a new instance of J2EEBaseVersion 
	 */
	protected J2EEBaseVersion(String moduleVersion, int nv, String pubId, String sysId) {
		j2eeModuleVersion = moduleVersion;
		numericVersion = nv;
		publicId = pubId;
		systemId = sysId;
	}
	
	/** The string representation of this version.
	 *
	 * @return String representing the numeric version, e.g. "1.4"
	 */
	public String toString() {
		return j2eeModuleVersion;
	}
	
	/** The Sun public id for this J2EE module type
	 *
	 * @return String representing the Sun public id for this J2EE module type.
	 */
	public String getSunPublicId() {
		return publicId;
	}

	/** The Sun system id for this J2EE module type
	 *
	 * @return String representing the Sun system id for this J2EE module type.
	 */
	public String getSunSystemId() {
		return systemId;
	}

	/** For use by derived class to compare numeric versions.  Derived class
	 *  should ensure target is the appropriate type before invoking this method
	 *  to compare the version numbers themselves.
	 *
	 * @param target Version object to compare with
	 * @return -1, 0, 1 if this version is less than, equal to, or greater than
	 *   the target version.
	 */
	protected int numericCompare(J2EEBaseVersion target) {
		if(numericVersion < target.numericVersion) {
			return -1;
		} else if(numericVersion > target.numericVersion) {
			return 1;
		} else {
			return 0;
		}
	}
	
/*
	public static J2EEBaseVersion getJ2EEVersion(String version) {
		J2EEBaseVersion result = null;
		
		
		if(J2EE_1_3.toString().equals(version)) {
			result = J2EE_1_3;
		} else if(J2EE_1_4.toString().equals(version)) {
			result = J2EE_1_4;
		}
		
		return result;
	}
 */
}
