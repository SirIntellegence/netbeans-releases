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


package org.netbeans.modules.properties;


import java.util.Locale;

import org.openide.filesystems.FileObject;
import org.openide.loaders.MultiDataObject;
import org.openide.util.NbBundle;


/**
 * Miscellaneous utilities for properties(reosurce bundles) module.
 * @author Petr Jiricka
 */
public final class Util extends Object {
    
    /** Help ID for properties module in general. */
    public static final String HELP_ID_PROPERTIES = "propfiles.prop"; // NOI18N
    /** Help ID for properties new from template. */
    public static final String HELP_ID_CREATING = "propfiles.creating"; // NOI18N
    /** Help ID for new property dialog. */
    public static final String HELP_ID_ADDING = "propfiles.adding"; // NOI18N
    /** Help ID for table view of properties. */
    public static final String HELP_ID_MODIFYING = "propfiles.modifying"; // NOI18N
    /** Help ID for new locale dialog. */
    public static final String HELP_ID_ADDLOCALE = "propfiles.addlocale"; // NOI18N
    /** Help ID for source editor of .properties file. */
    public static final String HELP_ID_EDITLOCALE = "propfiles.editlocale"; // NOI18N

    /** Character used to separate parts of bundle properties file name */
    public static final char PRB_SEPARATOR_CHAR = PropertiesDataLoader.PRB_SEPARATOR_CHAR;
    /** Default length for the first part of node label */
    public static final int LABEL_FIRST_PART_LENGTH = 10;

    /** Converts a string to a string suitable for a resource bundle key */
    public static String stringToKey(String source) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < source.length(); i++) {
            char x = source.charAt(i);
            switch (x) {
            case '=':
            case ':':
            case '\t':
            case '\r':
            case '\n':
            case '\f':
            case ' ':
                result.append('_'); break;
            default:
                result.append(x);
            }
        }
        return result.toString();
    }

    /** Gets the file for the primary entry.
     * @param <code>FileObject</code> of primary entry
     */
    private static FileObject getPrimaryFileObject(MultiDataObject.Entry fe) {
        return fe.getDataObject().getPrimaryFile();
    }

    /** Assembles a file name for a properties file from its base name and language.
     * @return assembled name */
    public static String assembleName (String baseName, String lang) {
        if (lang.length() == 0)
            return baseName;
        else {
            if (lang.charAt(0) != PRB_SEPARATOR_CHAR) {
                StringBuffer res = new StringBuffer().append(baseName).append(PRB_SEPARATOR_CHAR).append(lang);
                return res.toString();
            }
            else
                return baseName + lang;
        }
    }
    
    /** Gets a locale part of file name based on the primary file entry for a properties data object,
     * e.g. for file <code>Bundle_en_US.properties</code> returns <code>_en_US</code>, if Bundle.properties exists.
     */
    public static String getLocalePartOfFileName(MultiDataObject.Entry fe) {
        String myName   = fe.getFile().getName();
        String baseName = getPrimaryFileObject(fe).getName();
        
        if (!myName.startsWith(baseName))
            throw new IllegalStateException("Resource Bundle: Should never happen - error in Properties loader"); // NOI18N
        
        return myName.substring(baseName.length());
    }

    /** Gets a language from a file name based on the primary file entry for a properties data object,
     * e.g. for file <code>Bundle_en_US.properties</code> returns <code>en</code> (if Bundle.properties exists).
     * @return language for this locale or <code>null</code> if no language is present
     */
    public static String getLanguage(MultiDataObject.Entry fe) {
        String part = getLocalePartOfFileName(fe);
        return getFirstPart(part);
    }

    /** Gets a country from a file name based on the primary file entry for a properties data object,
     * e.g. for file <code>Bundle_en_US.properties</code> returns <code>US</code> (if Bundle.properties exists).
     * @return language for this locale or <code>null</code> if no country is present
     */
    public static String getCountry(MultiDataObject.Entry fe) {
        try {
            String part = getLocalePartOfFileName(fe);
            int start = part.indexOf(PRB_SEPARATOR_CHAR, 1);
            if (start == -1)
                return null;
            return getFirstPart(part.substring(start));
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            return null;
        }
    }

    /** Gets a variant from a file name based on the primary file entry for a properties data object,
     * e.g. for file <code>Bundle_en_US_POSIX.properties</code> returns <code>POSIX</code> (if Bundle.properties exists).
     * @return language for this locale or <code>null</code> if no variant is present
     */
    public static String getVariant(MultiDataObject.Entry fe) {
        try {
            String part = getLocalePartOfFileName(fe);
            int start = part.indexOf(PRB_SEPARATOR_CHAR, 1);
            if (start == -1)
                return null;
            start = part.indexOf(PRB_SEPARATOR_CHAR, start + 1);
            if (start == -1)
                return null;
            return getFirstPart(part.substring(start));
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            return null;
        }
    }

    /** Gets first substring enclosed between the leading underscore and the next underscore. */
    private static String getFirstPart(String part) {
        try {
            if(part.length() == 0)
                return null;
            if(part.charAt(0) != PRB_SEPARATOR_CHAR)
                throw new IllegalStateException("Resource Bundle: Should never happen - error in Properties loader (" + part + ")"); // NOI18N
            
            int end = part.indexOf(PRB_SEPARATOR_CHAR, 1);
            String result;
            result = (end == -1) ? part.substring(1) : part.substring(1, end);

            return (result.length() == 0) ? "" : result;
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            return null;
        }
    }

    /** Gets a label for properties nodes for individual locales. */
    public static String getLocaleLabel(MultiDataObject.Entry fe) {
        // locale-specific part of the file name
        String temp = getLocalePartOfFileName(fe);
        
        if (temp.length() > 0)
            if (temp.charAt(0) == PRB_SEPARATOR_CHAR)
                temp = temp.substring(1);

        // start constructing the result
        StringBuffer result = new StringBuffer(temp);
        if (temp.length() > 0)
            result.append(" - "); // NOI18N

        // Append language.
        String lang = getLanguage(fe);
        if (lang == null)
            temp = NbBundle.getBundle(Util.class).getString("LAB_DefaultBundle_Label");
        else {
            temp = (new Locale(lang, "")).getDisplayLanguage(); // NOI18N
            if (temp.length() == 0)
                temp = lang;
        }
        result.append(temp);

        // Append country.
        String coun = getCountry(fe);
        if(coun == null)
            temp = ""; // NOI18N
        else {
            temp = (new Locale(lang, coun)).getDisplayCountry();
            if (temp.length() == 0)
                temp = coun;
        }
        if(temp.length() != 0) {
            result.append("/"); // NOI18N
            result.append(temp);
        }

        // Append variant.
        String variant = getVariant(fe);
        if(variant == null)
            temp = ""; // NOI18N
        else {
            temp = (new Locale(lang, coun, variant)).getDisplayVariant();
            if (temp.length() == 0)
                temp = variant;
        }
        if (temp.length() != 0) {
            result.append("/"); // NOI18N
            result.append(temp);
        }

        return result.toString();
    }

}
