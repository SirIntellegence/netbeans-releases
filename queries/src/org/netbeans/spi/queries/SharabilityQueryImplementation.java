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

package org.netbeans.spi.queries;

import java.io.File;

/**
 * Determine whether files should be shared (for example in a VCS) or are intended
 * to be unshared.
 * Could be implemented e.g. by project types which know that certain files or folders in
 * a project (e.g. <samp>src/</samp>) are intended for VCS sharing while others
 * (e.g. <samp>build/</samp>) are not.
 * <p class="nonnormative">
 * Note that the Project API module registers a default implementation of this query
 * which delegates to the project which owns the queried file, if there is one.
 * This is more efficient than searching instances in global lookup, so use that
 * facility wherever possible.
 * </p>
 * @see org.netbeans.api.queries.SharabilityQuery
 * @author Jesse Glick
 */
public interface SharabilityQueryImplementation {
    
    /**
     * Check whether a file or directory should be shared.
     * If it is, it ought to be committed to a VCS if the user is using one.
     * If it is not, it is either a disposable build product, or a per-user
     * private file which is important but should not be shared.
     * @param file a file to check for sharability (may or may not yet exist)
     * @return one of {@link SharabilityQuery}'s constants
     */
    int getSharability(File file);
    
}
