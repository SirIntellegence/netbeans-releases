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


package org.netbeans.modules.i18n;


import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.openide.util.NbBundle;
import org.openide.util.SharedClassObject;


/**
 * Utilities class for I18N module.
 *
 * @author  Peter Zavadsky
 */
public abstract class I18nUtil {

    /** Items for init format customizer. */
    private static List initFormatItems;

    /** Help description for init format customizer. */
    private static List initHelpItems;

    /** Items for replace format customizer. */
    private static List replaceFormatItems;

    /** Help description for replace format customizer. */
    private static List replaceHelpItems;

    /** Items for regular expression customizer. */
    private static List regExpItems;

    /** Help description for regular expression customizer. */
    private static List regExpHelpItems;
    
    /** Items for i18n regular expression customizer. */
    private static List i18nRegExpItems;

    /** Resource bundle used in i18n module. */
    private static ResourceBundle bundle;
    
    
    /** Gets <code>initFormatItems</code>. */
    public static List getInitFormatItems() { 
        if(initFormatItems == null) {
            initFormatItems = new ArrayList(2);
            initFormatItems.add("java.util.ResourceBundle.getBundle(\"{bundleNameSlashes}\")"); // NOI18N
            initFormatItems.add("org.openide.util.NbBundle.getBundle({sourceFileName}.class)"); // NOI18N
        }
              
        return initFormatItems;
    }

    /** Gets <code>InitHelpFormats</code>. */
    public static List getInitHelpItems() {
        if(initHelpItems == null) {
            initHelpItems = new ArrayList(3);
            initHelpItems.add("{bundleNameSlashes} - "+ getBundle().getString("TXT_PackageNameSlashes")); // NOI18N
            initHelpItems.add("{bundleNameDots} - " + getBundle().getString("TXT_PackageNameDots")); // NOI18N
            initHelpItems.add("{sourceFileName} - " + getBundle().getString("TXT_SourceDataObjectName")); // NOI18N
        }
         
        return initHelpItems;
    }

    /** Gets <code>replaceFormatItems</code>. */
    public static List getReplaceFormatItems() {
        if(replaceFormatItems == null) {
            replaceFormatItems = new ArrayList(6);
            replaceFormatItems.add("{identifier}.getString(\"{key}\")"); // NOI18N
            replaceFormatItems.add("Utilities.getString(\"{key}\")"); // NOI18N
            replaceFormatItems.add("java.util.ResourceBundle.getBundle(\"{bundleNameSlashes}\").getString(\"{key}\")"); // NOI18N
            replaceFormatItems.add("org.openide.util.NbBundle.getBundle({sourceFileName}.class).getString(\"{key}\")"); // NOI18N
            replaceFormatItems.add("java.text.MessageFormat(java.util.ResourceBundle.getBundle(\"{bundleNameSlashes}\").getString(\"{key}\"), {arguments})"); // NOI18N
            replaceFormatItems.add("org.openide.NbBundle.getMessage({sourceFileName}.class, \"{key}\", {arguments})"); // NOI18N
        }
            
        return replaceFormatItems;
    }

    /** Gets <code>replaceHeplItems</code>.*/
    public static List getReplaceHelpItems() {
        if(replaceHelpItems == null) {
            replaceHelpItems = new ArrayList(5);
            replaceHelpItems.add("{identifier} - " + getBundle().getString("TXT_FieldIdentifier")); // NOI18N
            replaceHelpItems.add("{key} - " + getBundle().getString("TXT_KeyHelp")); // NOI18N
            replaceHelpItems.add("{bundleNameSlashes} - " + getBundle().getString("TXT_PackageNameSlashes")); // NOI18N
            replaceHelpItems.add("{bundleNameDots} - " + getBundle().getString("TXT_PackageNameDots")); // NOI18N
            replaceHelpItems.add("{sourceFileName} - " + getBundle().getString("TXT_SourceDataObjectName")); // NOI18N
            replaceHelpItems.add("{arguments} - " + getBundle().getString("TXT_Arguments")); // NOI18N
        }
            
        return replaceHelpItems;
    }

    /** Gets <code>regExpItems</code>. */
    public static List getRegExpItems() {
        if(regExpItems == null) {
            regExpItems = new ArrayList(3);
            regExpItems.add("(getString|getBundle)([:space:]*)\\(([:space:])*{hardString}"); // NOI18N
            regExpItems.add("// NOI18N"); // NOI18N
            regExpItems.add("((getString|getBundle)([:space:]*)\\(([:space:])*{hardString})|(// NOI18N)"); // NOI18N
        }
            
        return regExpItems;
    }
    
    /** Gets <code>i18nRegExpItems</code>. */
    public static List getI18nRegExpItems() {
        if(i18nRegExpItems == null) {
            i18nRegExpItems = new ArrayList(2);
            i18nRegExpItems.add("(getString)([:space:]*)\\(([:space:])*{hardString}"); // NOI18N
            i18nRegExpItems.add("(getString|getMessage)([:space:]*)\\(([:space:])*{hardString}"); // NOI18N
        }
            
        return i18nRegExpItems;
    }
    
    /** Gets <code>regExpHelpItems</code>. */
    public static List getRegExpHelpItems() {
        if(regExpHelpItems == null) {
            regExpHelpItems = new ArrayList(15);
            regExpHelpItems.add("{hardString} - " + getBundle().getString("TXT_HardString")); // NOI18N
            regExpHelpItems.add("[:alnum:] - " + getBundle().getString("TXT_Alnum")); // NOI18N
            regExpHelpItems.add("[:alpha:] - " + getBundle().getString("TXT_Alpha")); // NOI18N
            regExpHelpItems.add("[:blank:] - " + getBundle().getString("TXT_Blank")); // NOI18N
            regExpHelpItems.add("[:cntrl:] - " + getBundle().getString("TXT_Cntrl")); // NOI18N
            regExpHelpItems.add("[:digit:] - " + getBundle().getString("TXT_Digit")); // NOI18N
            regExpHelpItems.add("[:graph:] - " + getBundle().getString("TXT_Graph")); // NOI18N
            regExpHelpItems.add("[:lower:] - " + getBundle().getString("TXT_Lower")); // NOI18N
            regExpHelpItems.add("[:print:] - " + getBundle().getString("TXT_Print")); // NOI18N
            regExpHelpItems.add("[:punct:] - " + getBundle().getString("TXT_Punct")); // NOI18N
            regExpHelpItems.add("[:space:] - " + getBundle().getString("TXT_Space")); // NOI18N
            regExpHelpItems.add("[:upper:] - " + getBundle().getString("TXT_Upper")); // NOI18N
            regExpHelpItems.add("[:xdigit:] - " + getBundle().getString("TXT_Xdigit")); // NOI18N
            regExpHelpItems.add("[:javastart:] - " + getBundle().getString("TXT_Javastart")); // NOI18N
            regExpHelpItems.add("[:javapart:] - " + getBundle().getString("TXT_Javapart")); // NOI18N
        }
        
        return regExpHelpItems;
    }

    /** Gets resource bundle for i18n module. */
    public static ResourceBundle getBundle() {
        if(bundle == null)
            bundle = NbBundle.getBundle(I18nModule.class);
        
        return bundle;
    }
    
    /** Gets i18n options. */
    public static I18nOptions getOptions() {
        return (I18nOptions)SharedClassObject.findObject(I18nOptions.class, true);
    }
    
}
