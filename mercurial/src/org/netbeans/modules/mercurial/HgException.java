/*
 * HgException.java
 *
 * Created on March 15, 2007, 6:39 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.netbeans.modules.mercurial;

/**
 *
 * @author jr140578
 */
public class HgException extends Exception {
    
    /** Creates a new instance of HgException */
    public HgException(String msg) {
        super(msg);
    }

    public static class HgTooLongArgListException extends HgException {
        public HgTooLongArgListException (String message) {
            super(message);
        }
    }

    public static class HgCommandCanceledException extends HgException {
        public HgCommandCanceledException (String message) {
            super(message);
        }
    }
}
