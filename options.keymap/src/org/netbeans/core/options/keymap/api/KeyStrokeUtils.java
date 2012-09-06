/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.netbeans.core.options.keymap.api;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.KeyStroke;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.openide.util.Lookup;
import org.openide.util.Utilities;


/**
 * @since 1.21
 * @author Jan Jancura, Jaroslav Havlin
 */
public class KeyStrokeUtils {

    private static final Logger LOG = Logger.getLogger(
            KeyStrokeUtils.class.getName());

    /**
     * Convert an array of {@link KeyStroke key stroke} to a string composed of
     * human-readable names of these key strokes, delimited by {@code delim}.
     */
    public static String getKeyStrokesAsText(
            @NullAllowed KeyStroke[] keyStrokes, @NonNull String delim) {
        if (keyStrokes == null) {
            return "";                                                  //NOI18N
        }
        if (keyStrokes.length == 0) {
            return "";                                                  //NOI18N
        }
        StringBuilder sb = new StringBuilder(getKeyStrokeAsText(keyStrokes[0]));
        int i, k = keyStrokes.length;
        for (i = 1; i < k; i++) {
            sb.append(delim).append(getKeyStrokeAsText(keyStrokes[i]));
        }
        return new String(sb);
    }

    // Important: keep in sync with Editor Settings Storage StorageSupport
    // until Keymap Options provides a proper API

    private static final String EMACS_CTRL = "Ctrl+"; //NOI18N
    private static final String EMACS_ALT = "Alt+"; //NOI18N
    private static final String EMACS_SHIFT = "Shift+"; //NOI18N
    private static final String EMACS_META = "Meta+"; //NOI18N
    
    /**
     * Platform - dependent value for Alt or Meta presentation
     */
    private static final String STRING_META; // NOI18N
    private static final String STRING_ALT; // NOI18N
    
    static {
        if (Utilities.isMac()) {
            STRING_META = KeyEvent.getKeyText(KeyEvent.VK_META).concat("+");
            STRING_ALT = KeyEvent.getKeyText(KeyEvent.VK_ALT).concat("+");
        } else {
            STRING_META = EMACS_META;
            STRING_ALT = EMACS_ALT;
        }
    }
    
    /**
     * Convert human-readable keystroke name to {@link KeyStroke} object.
     */
    public static @CheckForNull KeyStroke getKeyStroke(
            @NonNull String keyStroke) {

        int modifiers = 0;
        while (true) {
            if (keyStroke.startsWith(EMACS_CTRL)) {
                modifiers |= InputEvent.CTRL_DOWN_MASK;
                keyStroke = keyStroke.substring(EMACS_CTRL.length());
            } else if (keyStroke.startsWith(EMACS_ALT)) {
                modifiers |= InputEvent.ALT_DOWN_MASK;
                keyStroke = keyStroke.substring(EMACS_ALT.length());
            } else if (keyStroke.startsWith(EMACS_SHIFT)) {
                modifiers |= InputEvent.SHIFT_DOWN_MASK;
                keyStroke = keyStroke.substring(EMACS_SHIFT.length());
            } else if (keyStroke.startsWith(EMACS_META)) {
                modifiers |= InputEvent.META_DOWN_MASK;
                keyStroke = keyStroke.substring(EMACS_META.length());
            } else if (keyStroke.startsWith(STRING_ALT)) {
                modifiers |= InputEvent.ALT_DOWN_MASK;
                keyStroke = keyStroke.substring(STRING_ALT.length());
            } else if (keyStroke.startsWith(STRING_META)) {
                modifiers |= InputEvent.META_DOWN_MASK;
                keyStroke = keyStroke.substring(STRING_META.length());
            } else {
                break;
            }
        }
        KeyStroke ks = Utilities.stringToKey (keyStroke);
        if (ks == null) { // Return null to indicate an invalid keystroke
            return null;
        } else {
            KeyStroke result = KeyStroke.getKeyStroke (ks.getKeyCode (), modifiers);
            return result;
        }
    }
    
    /**
     * Get human-readable name for a {@link KeyStroke}.
     */
    public static String getKeyStrokeAsText(@NonNull KeyStroke keyStroke) {
        int modifiers = keyStroke.getModifiers ();
        StringBuilder sb = new StringBuilder ();
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) > 0) {
            sb.append(EMACS_CTRL);
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) > 0) {
            sb.append(STRING_ALT);
        }
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) > 0) {
            sb.append (EMACS_SHIFT);
        }
        if ((modifiers & InputEvent.META_DOWN_MASK) > 0) {
            sb.append(STRING_META);
        }
        if (keyStroke.getKeyCode () != KeyEvent.VK_SHIFT &&
            keyStroke.getKeyCode () != KeyEvent.VK_CONTROL &&
            keyStroke.getKeyCode () != KeyEvent.VK_META &&
            keyStroke.getKeyCode () != KeyEvent.VK_ALT &&
            keyStroke.getKeyCode () != KeyEvent.VK_ALT_GRAPH) {
            sb.append (Utilities.keyToString (
                KeyStroke.getKeyStroke (keyStroke.getKeyCode (), 0)
            ));
        }
        return sb.toString ();
    }

    /**
     * Converts a textual representation of key strokes to an array of <code>KeyStroke</code>
     * objects. Please see {@link #keyStrokesToString(Collection<KeyStroke>, boolean)}
     * ror details about the available formats.
     *
     * @param key The textual representation of keystorkes to convert. Its format
     *   depends on the value of <code>emacsStyle</code> parameter.
     *
     * @return The <code>KeyStroke</code>s that were represented by the <code>key</code>
     *   text or <code>null</code> if the textual representation was malformed.
     * @since 1.16
     */
    public static @CheckForNull KeyStroke[] getKeyStrokes(@NonNull String key) {
        assert key != null : "The parameter key must not be null"; //NOI18N

        List<KeyStroke> result = new ArrayList<KeyStroke>();
        String delimiter = " "; //NOI18N

        for(StringTokenizer st = new StringTokenizer(key, delimiter); st.hasMoreTokens();) { //NOI18N
            String ks = st.nextToken().trim();
            KeyStroke keyStroke = getKeyStroke(ks);

            if (keyStroke != null) {
                result.add(keyStroke);
            } else {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE,
                            "Invalid keystroke string: ''{0}''", ks);   //NOI18N
                }
                return null;
            }
        }

        return result.toArray(new KeyStroke[result.size()]);
    }

    /**
     * Find key strokes for an action. The currently selected profile is used.
     * If there are more than one key strokes, the most convenient one is the
     * first one in the array (index 0). If no key stroke is found and
     * {@code defaultKeyStroke} is not null, an array containing only
     * {@code defaultKeyStroke} is returned. If no key stroke is found and
     * {@code defaultKeyStroke} is null, an empty array is returned.
     *
     * @param actionId ID of action.
     * @param defaultKeyStroke Default key stroke, used in case no key stroke is
     * found for the action. Can be null.
     * @return Array of key strokes, or an empty array if no key stroke is
     * available.
     */
    public static List<KeyStroke[]> getKeyStrokesForAction(@NonNull String actionId,
            @NullAllowed KeyStroke defaultKeyStroke) {
        for (ShortcutsFinder sf : Lookup.getDefault().lookupAll(
                ShortcutsFinder.class)) {
            ShortcutAction sa = sf.findActionForId(actionId);
            if (sa != null) {
                String[] shortcuts = sf.getShortcuts(sa);
                if (shortcuts != null && shortcuts.length > 0) {
                    List<KeyStroke[]> ks = new LinkedList<KeyStroke[]>();
                    for (int i = 0; i < shortcuts.length; i++) {
                        if (shortcuts[i] != null) {
                            KeyStroke s[] = getKeyStrokes(shortcuts[i]);
                            if (s != null) {
                                ks.add(s);
                            }
                        }
                    }
                    return sortKeyStrokesByPreference(ks);
                }
            }
        }
        return defaultKeyStroke == null
                ? Collections.<KeyStroke[]>emptyList()
                : Collections.singletonList(new KeyStroke[]{defaultKeyStroke});
    }

    /**
     * Sort the list, so that the most appropriate accelerator is at index 0.
     */
    private static List<KeyStroke[]> sortKeyStrokesByPreference(
            List<KeyStroke[]> keystrokes) {
        if (keystrokes.size() < 2) {
            return keystrokes;
        }
        KeyStroke best[] = null;
        boolean isSolaris =
                Utilities.getOperatingSystem() == Utilities.OS_SOLARIS;
        for (int i = 0; i < keystrokes.size(); i++) {
            KeyStroke[] ks = keystrokes.get(i);
            if (ks.length > 1) {
                continue;
            }
            boolean solarisKey = ks[0].getKeyCode() >= KeyEvent.VK_STOP
                    && ks[0].getKeyCode() <= KeyEvent.VK_CUT;
            if (isSolaris == solarisKey
                    && (best == null
                    || best[0].getKeyCode() > ks[0].getKeyCode())) {
                //Solaris key on solaris OS or other key on other OS.
                best = ks;
            }
        }
        if (best != null) {
            keystrokes.remove(best);
            keystrokes.add(0, best);
        }
        return keystrokes;
    }

    /**
     * Force caches to be refreshed, so that
     * {@link #getKeyStrokesForAction(String, KeyStroke)} returns correct and
     * up-to-date results.
     */
    public static void refreshActionCache() {
        for (ShortcutsFinder sf :
                Lookup.getDefault().lookupAll(ShortcutsFinder.class)) {
            sf.refreshActions();
        }
    }
}
