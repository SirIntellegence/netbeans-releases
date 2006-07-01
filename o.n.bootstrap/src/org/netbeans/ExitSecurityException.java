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

package org.netbeans;

/**
 * Thrown during exit of the task, not reported as exception
 *
 * @author Ales Novak
 */
class ExitSecurityException extends SecurityException {
    /** generated Serialized Version UID */
    static final long serialVersionUID = -8973677308554045785L;

    /** Creates new exception ExitSecurityException
    */
    public ExitSecurityException () {
        super ();
    }

    /** Creates new exception ExitSecurityException with text specified
    * string s.
    * @param s the text describing the exception
    */
    public ExitSecurityException (String s) {
        super (s);
    }
}
