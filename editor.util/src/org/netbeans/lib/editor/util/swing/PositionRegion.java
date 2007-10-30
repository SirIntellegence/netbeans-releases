/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.lib.editor.util.swing;

import java.util.Comparator;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;

/**
 * A pair of positions delimiting a text region in a swing document.
 * <br/>
 * At all times it should be satisfied that
 * {@link #getStartOffset()} &lt;= {@link #getEndOffset()}.
 *
 * @author Miloslav Metelka
 * @since 1.6
 */

public class PositionRegion {

    /** Copmarator for position regions */
    private static Comparator<PositionRegion> comparator;
    
    /**
     * Get comparator for position regions comparing start offsets
     * of the two given regions.
     *
     * @return non-null comparator comparing the start offsets of the two given
     *  regions.
     */
    public static final Comparator<PositionRegion> getComparator() {
        if (comparator == null) {
            comparator = new Comparator<PositionRegion>() {
                public int compare(PositionRegion pr1, PositionRegion pr2) {
                    return pr1.getStartOffset() - pr2.getStartOffset();
                }
            };
        }
        return comparator;
    }

    /**
     * Create a fixed position instance that just wraps a given integer offset.
     * <br/>
     * This may be useful for situations where a position needs to be used
     * but the document is not available yet. Once the document becomes
     * available the regular position instance (over an existing document)
     * may be used instead.
     *
     * @param offset &gt;=0 offset at which the position should be created.
     * @since 1.10
     */
    public static Position createFixedPosition(final int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset < 0");
        }
        return new Position() {
            public int getOffset() {
                return offset;
            }
        };
    }

    /**
     * Check whether a list of position regions is sorted
     * according the start offsets of the regions.
     *
     * @param positionRegionList list of the regions to be compared.
     * @return true if the regions are sorted according to the starting offset
     *  of the given regions or false otherwise.
     */
    public static boolean isRegionsSorted(List<? extends PositionRegion> positionRegionList) {
        for (int i = positionRegionList.size() - 2; i >= 0; i--) {
            if (getComparator().compare(positionRegionList.get(i),
                    positionRegionList.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }

    private Position startPosition;
    
    private Position endPosition;
    
    /**
     * Construct new position region.
     *
     * @param startPosition non-null start position of the region &lt;= end position.
     * @param endPosition non-null end position of the region &gt;= start position.
     */
    public PositionRegion(Position startPosition, Position endPosition) {
        assertPositionsValid(startPosition, endPosition);
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
    
    /**
     * Construct new position region based on the knowledge
     * of the document and starting and ending offset.
     */
    public PositionRegion(Document doc, int startOffset, int endOffset) throws BadLocationException {
        this(doc.createPosition(startOffset), doc.createPosition(endOffset));
    }
    
    /**
     * Get starting offset of this region.
     *
     * @return &gt;=0 starting offset of this region.
     */
    public final int getStartOffset() {
        return startPosition.getOffset();
    }
    
    /**
     * Get starting position of this region.
     *
     * @return non-null starting position of this region.
     */
    public final Position getStartPosition() {
        return startPosition;
    }
    
    /**
     * Get ending offset of this region.
     *
     * @return &gt;=0 ending offset of this region.
     */
    public final int getEndOffset() {
        return endPosition.getOffset();
    }
    
    /**
     * Get ending position of this region.
     *
     * @return non-null ending position of this region.
     */
    public final Position getEndPosition() {
        return endPosition;
    }
    
    /**
     * Get length of this region.
     *
     * @return &gt;=0 length of this region
     *  computed as <code>getEndOffset() - getStartOffset()</code>.
     */
    public final int getLength() {
        return getEndOffset() - getStartOffset();
    }

    /**
     * {@link MutablePositionRegion} uses this package private method
     * to set a new start position of this region.
     */
    void resetImpl(Position startPosition, Position endPosition) {
        assertPositionsValid(startPosition, endPosition);
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
    
    /**
     * {@link MutablePositionRegion} uses this package private method
     * to set a new start position of this region.
     */
    void setStartPositionImpl(Position startPosition) {
        assertPositionsValid(startPosition, endPosition);
        this.startPosition = startPosition;
    }

    /**
     * {@link MutablePositionRegion} uses this package private method
     * to set a new start position of this region.
     */
    void setEndPositionImpl(Position endPosition) {
        assertPositionsValid(startPosition, endPosition);
        this.endPosition = endPosition;
    }

    private static void assertPositionsValid(Position startPos, Position endPos) {
        assert (startPos.getOffset() <= endPos.getOffset())
            : "startPosition=" + startPos.getOffset() + " > endPosition=" // NOI18N
                + endPos.getOffset();
    }

}
