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

package org.netbeans.api.editor.fold;

/**
 * Information about state changes made in a particular fold.
 * <br>
 * Zero or more of the state change instances can be part of a particular
 * {@link FoldHierarchyEvent}.
 *
 * <p>
 * It can be extended to carry additional information specific to particular fold
 * types.
 *
 * @author Miloslav Metelka
 * @version 1.00
 */

public final class FoldStateChange {
    
    private static final int COLLAPSED_CHANGED_BIT = 1;

    private static final int START_OFFSET_CHANGED_BIT = 2;
    
    private static final int END_OFFSET_CHANGED_BIT = 4;
    
    private static final int DESCRIPTION_CHANGED_BIT = 8;


    private Fold fold;

    private int stateChangeBits;
    
    private int originalStartOffset = -1;

    private int originalEndOffset = -1;
    
    /**
     * Construct state change.
     * @param fold fold being changed.
     */
    FoldStateChange(Fold fold) {
        this.fold = fold;
    }
    
    /**
     * Get the fold that has changed its state.
     */
    public Fold getFold() {
        return fold;
    }

    /**
     * Has the collapsed flag of the fold
     * (returned by <code>getFold()</code>) changed?
     *
     * @return true if the collapsed flag has changed in the fold
     *  or false otherwise.
     */
    public boolean isCollapsedChanged() {
        return ((stateChangeBits & COLLAPSED_CHANGED_BIT) != 0);
    }
    
    /**
     * Has the start offset of the fold
     * (returned by <code>getFold()</code>) changed?
     *
     * @return true if the start offset has changed in the fold
     *  or false otherwise.
     */
    public boolean isStartOffsetChanged() {
        return ((stateChangeBits & START_OFFSET_CHANGED_BIT) != 0);
    }
    
    /**
     * Return the original start offset of the fold prior
     * to change to the current start offset that the fold has now.
     * <br>
     * @return original start offset or -1 if the start offset was not changed
     *  for the fold.
     */
    public int getOriginalStartOffset() {
        return originalStartOffset;
    }

    /**
     * Has the end offset of the fold
     * (returned by <code>getFold()</code>) changed?
     *
     * @return true if the end offset has changed in the fold
     *  or false otherwise.
     */
    public boolean isEndOffsetChanged() {
        return ((stateChangeBits & END_OFFSET_CHANGED_BIT) != 0);
    }
    
    /**
     * Return the original end offset of the fold prior
     * to change to the current end offset that the fold has now.
     * <br>
     * @return original end offset or -1 if the end offset was not changed
     *  for the fold.
     */
    public int getOriginalEndOffset() {
        return originalEndOffset;
    }

    /**
     * Has the text description of the collapsed fold
     * (returned by <code>getFold()</code>) changed?
     *
     * @return true if the collapsed text description has changed in the fold
     *  or false otherwise.
     */
    public boolean isDescriptionChanged() {
        return ((stateChangeBits & DESCRIPTION_CHANGED_BIT) != 0);
    }

    /**
     * Mark that collapsed flag has changed
     * for the fold.
     */
    void collapsedChanged() {
        stateChangeBits |= COLLAPSED_CHANGED_BIT;
    }
    
    /**
     * Mark that start offset has changed
     * for the fold.
     */
    void startOffsetChanged(int originalStartOffset) {
        stateChangeBits |= START_OFFSET_CHANGED_BIT;
        this.originalStartOffset = originalStartOffset;
    }
    
    /**
     * Subclasses can mark that end offset has changed
     * for the fold.
     */
    void endOffsetChanged(int originalEndOffset) {
        stateChangeBits |= END_OFFSET_CHANGED_BIT;
        this.originalEndOffset = originalEndOffset;
    }
    
    /**
     * Subclasses can mark that collapsed flag has changed
     * for the fold.
     */
    void descriptionChanged() {
        stateChangeBits |= DESCRIPTION_CHANGED_BIT;
    }
    
    public String toString() {
        return org.netbeans.modules.editor.fold.FoldUtilitiesImpl.foldStateChangeToString(this);
    }

}
