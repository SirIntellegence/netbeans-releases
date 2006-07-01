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


package org.netbeans.spi.palette;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import org.netbeans.modules.palette.DefaultModel;
import org.openide.ErrorManager;
import org.openide.nodes.Index;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.datatransfer.ExTransferable;
import org.openide.util.datatransfer.PasteType;
/**
 * <p>An abstract class implemented by palette clients to implement drag and drop
 * of new items into the palette window and to customize the default Transferable 
 * instance of items being dragged from the palette window to editor area.</p>
 *
 * <p>Client's can support multiple DataFlavors that may help to enable/disable the drop
 * when dragging an item over different editor area parts that allow only certain
 * item types to be dropped into them.</p>
 *
 * @author S. Aubrecht
 */
public abstract class DragAndDropHandler {

    private static DragAndDropHandler defaultHandler;
    
    static DragAndDropHandler getDefault() {
        if( null == defaultHandler )
            defaultHandler = new DefaultDragAndDropHandler();
        return defaultHandler;
    }
    
    /**
     * Add your own custom DataFlavor as need to suppor drag-over a different
     * parts of editor area.
     *
     * @param t Item's default Transferable.
     * @param item Palette item's Lookup.
     *
     */
    public abstract void customize( ExTransferable t, Lookup item );
    
    /**
     * @param targetCategory Lookup of the category under the drop cursor.
     * @param flavors Supported DataFlavors.
     * @param dndAction Drop action type.
     *
     * @return True if the given category can accept the item being dragged.
     */
    public boolean canDrop( Lookup targetCategory, DataFlavor[] flavors, int dndAction ) {
        for( int i=0; i<flavors.length; i++ ) {
            if( PaletteController.ITEM_DATA_FLAVOR.equals( flavors[i] ) ) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Perform the drop operation and add the dragged item into the given category.
     *
     * @param targetCategory Lookup of the category that accepts the drop.
     * @param item Transferable holding the item being dragged.
     * @param dndAction Drag'n'drop action type.
     * @param dropIndex Zero-based position where the dragged item should be dropped.
     *
     * @return True if the drop has been successful, false otherwise.
     */
    public boolean doDrop( Lookup targetCategory, Transferable item, int dndAction, int dropIndex ) {
        Node categoryNode = (Node)targetCategory.lookup( Node.class );
        try {
            //first check if we're reordering items within the same category
            if( item.isDataFlavorSupported( PaletteController.ITEM_DATA_FLAVOR ) ) {
                Lookup itemLookup = (Lookup)item.getTransferData( PaletteController.ITEM_DATA_FLAVOR );
                if( null != itemLookup ) {
                    Node itemNode = (Node)itemLookup.lookup( Node.class );
                    if( null != itemNode ) {
                        Index order = (Index)categoryNode.getCookie( Index.class );
                        if( null != order && order.indexOf( itemNode ) >= 0 ) {
                            //the drop item comes from the targetCategory so let's 
                            //just change the order of items
                            return moveItem( targetCategory, itemLookup, dropIndex );
                        }
                    }
                }
            }
            PasteType paste = categoryNode.getDropType( item, dndAction, dropIndex );
            if( null != paste ) {
                Node[] itemsBefore = categoryNode.getChildren().getNodes( DefaultModel.canBlock() );
                paste.paste();
                Node[] itemsAfter = categoryNode.getChildren().getNodes( DefaultModel.canBlock() );
                
                if( itemsAfter.length == itemsBefore.length+1 ) {
                    int currentIndex = -1;
                    Node newItem = null;
                    for( int i=itemsAfter.length-1; i>=0; i-- ) {
                        newItem = itemsAfter[i];
                        currentIndex = i;
                        for( int j=0; j<itemsBefore.length; j++ ) {
                            if( newItem.equals( itemsBefore[j] ) ) {
                                newItem = null;
                                break;
                            }
                        }
                        if( null != newItem ) {
                            break;
                        }
                    }
                    if( null != newItem && dropIndex >= 0 ) {
                        if( currentIndex < dropIndex )
                            dropIndex++;
                        moveItem( targetCategory, newItem.getLookup(), dropIndex );
                    }
                }
                return true;
            }
        } catch( IOException ioE ) {
            ErrorManager.getDefault().notify( ErrorManager.INFORMATIONAL, ioE );
        } catch( UnsupportedFlavorException e ) {
            ErrorManager.getDefault().notify( ErrorManager.INFORMATIONAL, e );
        }
        return false;
    }
    
    /**
     * Move palette item to a new position in its current category.
     *
     * @param category Lookup of the category that contains the dragged item.
     * @param itemToMove Lookup of the item that is going to be moved to a new position.
     * @param moveToIndex Zero-based index to category's children where the item should move to.
     *
     * @return True if the move operation was successful.
     */
    private boolean moveItem( Lookup category, Lookup itemToMove, int moveToIndex ) {
        Node categoryNode = (Node)category.lookup( Node.class );
        if( null == categoryNode )
            return false;
        Node itemNode = (Node)itemToMove.lookup( Node.class );
        if( null == itemNode )
            return false;
        
        Index order = (Index)categoryNode.getCookie( Index.class );
        if( null == order ) {
            return false;
        }
        
        int sourceIndex = order.indexOf( itemNode );
        if( sourceIndex < moveToIndex ) {
            moveToIndex--;
        }
        order.move( sourceIndex, moveToIndex );
        return true;
    }
    
    /**
     * @param paletteRoot Lookup of palette's root node.
     * @return True if it is possible to reorder categories by drag and drop operations.
     */
    public boolean canReorderCategories( Lookup paletteRoot ) {
        Node rootNode = (Node)paletteRoot.lookup( Node.class );
        if( null != rootNode ) {
            return null != rootNode.getCookie( Index.class );
        }
        return false;
    }
    
    /**
     * Move the given category to a new position.
     *
     * @param category The lookup of category that is being dragged.
     * @param moveToIndex Zero-based index to palette's root children Nodes 
     * where the category should move to.
     * @return True if the move operation was successful.
     */
    public boolean moveCategory( Lookup category, int moveToIndex ) {
        Node categoryNode = (Node)category.lookup( Node.class );
        if( null == categoryNode )
            return false;
        Node rootNode = categoryNode.getParentNode();
        if( null == rootNode )
            return false;
        
        Index order = (Index)rootNode.getCookie( Index.class );
        if( null == order ) {
            return false;
        }
        
        int sourceIndex = order.indexOf( categoryNode );
        if( sourceIndex < moveToIndex ) {
            moveToIndex--;
        }
        order.move( sourceIndex, moveToIndex );
        return true;
    }
    
    private static final class DefaultDragAndDropHandler extends DragAndDropHandler {
        public void customize(ExTransferable t, Lookup item) {
            //do nothing
        }
    }
}
