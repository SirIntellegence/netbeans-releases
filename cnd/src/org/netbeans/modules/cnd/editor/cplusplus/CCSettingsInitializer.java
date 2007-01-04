/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.netbeans.org/cddl.html
 * or http://www.netbeans.org/cddl.txt.

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

package org.netbeans.modules.cnd.editor.cplusplus;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import javax.swing.KeyStroke;
import org.netbeans.editor.*;
import org.netbeans.editor.ext.ExtKit;
import org.netbeans.editor.ext.ExtSettingsNames;
import org.netbeans.editor.ext.ExtSettingsInitializer;

// popup menu actions
import org.openide.actions.SaveAction;
import org.openide.actions.CutAction;
import org.openide.actions.CopyAction;
import org.openide.actions.PasteAction;
import org.openide.actions.DeleteAction;
import org.openide.actions.NewAction;
import org.openide.actions.ToolsAction;
import org.openide.actions.PropertiesAction;
import org.openide.windows.TopComponent;

/**
* Extended settings for CC.
*
*/

public class CCSettingsInitializer extends Settings.AbstractInitializer {

    /** Name assigned to initializer */
    public static final String NAME = "cc-settings-initializer";  //NOI18N

    private Class ccKitClass;

    /** Construct new cc-settings-initializer.
    * @param ccKitClass the real kit class for which the settings are created.
    *   It's unknown here so it must be passed to this constructor.
    */
    public CCSettingsInitializer(Class ccKitClass) {
        super(NAME);
        this.ccKitClass = ccKitClass;
    }

    /**
     *  Update map filled with the settings.
     *
     *  @param kitClass kit class for which the settings are being updated. It is always non-null value.
     *  @param settingsMap map holding [setting-name, setting-value] pairs.
     *   The map can be empty if this is the first initializer
     *   that updates it or if no previous initializers updated it.
     */
    public void updateSettingsMap(Class kitClass, Map settingsMap) {

        // Update cc colorings
        if (kitClass == BaseKit.class) {
            new CCSettingsDefaults.CCTokenColoringInitializer().updateSettingsMap(kitClass, settingsMap);
        }
        
        if (kitClass == ExtKit.class) {
	    // set up cut,paste,etc buttons
	    new ExtSettingsInitializer().updateSettingsMap(kitClass, settingsMap);
        }
        
        if (kitClass == ccKitClass) {
            SettingsUtil.updateListSetting(settingsMap, SettingsNames.TOKEN_CONTEXT_LIST,
                new TokenContext[] { CCTokenContext.context });

	    // key bindings in CCSettingsDefaults.java 
	    SettingsUtil.updateListSetting(settingsMap,
					   SettingsNames.KEY_BINDING_LIST, 
					   CCSettingsDefaults.getCCKeyBindings());

	    SettingsUtil.updateListSetting(settingsMap,
                         ExtSettingsNames.POPUP_MENU_ACTION_NAME_LIST,
		         new String [] { BaseKit.formatAction,
					 null,
					 TopComponent.class.getName(),
					 null,
					 //CompileAction.class.getName(),
					 //null,
                                         //org.openide.actions.ToggleBreakpointAction.class.getName(),
                                         //org.openide.actions.AddWatchAction.class.getName(),
					 //null,
					 CutAction.class.getName(),
					 CopyAction.class.getName(),
					 PasteAction.class.getName(),
					 null,
					 DeleteAction.class.getName(),
					 null,
					 NewAction.class.getName(),
					 null,
					 CCKit.gotoHelpAction,
					 null,
					 ToolsAction.class.getName(),
					 BaseKit.generateGutterPopupAction,
					 PropertiesAction.class.getName() }
				  );

            SettingsUtil.updateListSetting(settingsMap, SettingsNames.KEY_BINDING_LIST,
                                           new MultiKeyBinding[] {
                                               new MultiKeyBinding(
                                                   KeyStroke.getKeyStroke(KeyEvent.VK_F1, InputEvent.ALT_MASK),
                                                   CCKit.gotoHelpAction
                                               )
                                           }
                                          );
	    
            settingsMap.put(SettingsNames.ABBREV_MAP, CCSettingsDefaults.getCCAbbrevMap());

            settingsMap.put(ExtSettingsNames.CARET_SIMPLE_MATCH_BRACE,
                            CCSettingsDefaults.defaultCaretSimpleMatchBrace);

            settingsMap.put(ExtSettingsNames.HIGHLIGHT_MATCH_BRACE,
                            CCSettingsDefaults.defaultHighlightMatchBrace);

            settingsMap.put(SettingsNames.IDENTIFIER_ACCEPTOR,
                            CCSettingsDefaults.defaultIdentifierAcceptor);

            settingsMap.put(SettingsNames.ABBREV_RESET_ACCEPTOR,
                            CCSettingsDefaults.defaultAbbrevResetAcceptor);

            settingsMap.put(SettingsNames.WORD_MATCH_MATCH_CASE,
                            CCSettingsDefaults.defaultWordMatchMatchCase);

            settingsMap.put(SettingsNames.WORD_MATCH_STATIC_WORDS,
                            CCSettingsDefaults.defaultWordMatchStaticWords);

            // Formatting settings
            settingsMap.put(CCSettingsNames.FORMAT_SPACE_BEFORE_PARENTHESIS,
                            CCSettingsDefaults.defaultFormatSpaceBeforeParenthesis);

            settingsMap.put(CCSettingsNames.FORMAT_SPACE_AFTER_COMMA,
                            CCSettingsDefaults.defaultFormatSpaceAfterComma);

            settingsMap.put(CCSettingsNames.FORMAT_NEWLINE_BEFORE_BRACE,
                            CCSettingsDefaults.defaultFormatNewlineBeforeBrace);

            settingsMap.put(CCSettingsNames.INDENT_HOT_CHARS_ACCEPTOR,
                            CCSettingsDefaults.defaultIndentHotCharsAcceptor);

	    // URL settings
            settingsMap.put(CCSettingsNames.DOCUMENTATION_URLBASE,
                            CCSettingsDefaults.defaultDocURLbase);

	    // Code folding settings
	    settingsMap.put(SettingsNames.CODE_FOLDING_ENABLE, CCSettingsDefaults.defaultCodeFoldingEnable);
            
	    settingsMap.put(CCSettingsNames.PAIR_CHARACTERS_COMPLETION,
			    CCSettingsDefaults.defaultPairCharactersCompletion);    
            settingsMap.put(ExtSettingsNames.JAVADOC_AUTO_POPUP,
                            CCSettingsDefaults.defaultJavaDocAutoPopup);
        }
    }
}
 
