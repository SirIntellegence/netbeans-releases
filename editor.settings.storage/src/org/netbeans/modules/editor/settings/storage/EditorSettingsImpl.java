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

package org.netbeans.modules.editor.settings.storage;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.AttributeSet;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.modules.editor.settings.storage.api.EditorSettings;
import org.netbeans.modules.editor.settings.storage.api.FontColorSettingsFactory;
import org.netbeans.modules.editor.settings.storage.api.KeyBindingSettingsFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.Repository;

/**
 * This class contains access methods for editor settings like font & colors
 * profiles and keymap profiles. 
 *
 * @author Jan Jancura
 */
public class EditorSettingsImpl extends EditorSettings {

    private static final Logger LOG = Logger.getLogger(EditorSettingsImpl.class.getName());
    
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /** The name of the property change event for 'Highlighting' font and colors. */
    public static final String PROP_HIGHLIGHT_COLORINGS = "editorFontColors"; //NOI18N

    /** The name of the property change event for 'Token' font and colors. */
    public static final String PROP_TOKEN_COLORINGS = "fontColors"; //NOI18N
        
        
    /** The name of the default profile. */
    public static final String DEFAULT_PROFILE = "NetBeans"; //NOI18N
    
    // XXX: rewrite this using NbPreferences
    private static final String FATTR_CURRENT_FONT_COLOR_PROFILE = "currentFontColorProfile"; // NOI18N
    private static final String FATTR_CURRENT_KEYMAP_PROFILE = "currentKeymap";      // NOI18N

    /** Storage folder for the current font & color profile attribute. */
    private static final String EDITORS_FOLDER = "Editors"; //NOI18N
    /** Storage folder for the current keybindings profile attribute. */
    private static final String KEYMAPS_FOLDER = "Keymaps"; // NOI18N

    private static EditorSettingsImpl instance = null;
    
    public static synchronized EditorSettingsImpl getInstance() {
        if (instance == null) {
            instance = new EditorSettingsImpl();
        }
        return instance;
    }

    public Set<String> getAllMimeTypes () {
        FileObject editorsFo = Repository.getDefault().getDefaultFileSystem().findResource(EDITORS_FOLDER);
        HashSet<String> mimeTypes = new HashSet<String>();
        
        if (editorsFo != null) {
            for(FileObject f : editorsFo.getChildren()) {
                if (!f.isFolder()) {
                    continue;
                }

                String firstPart = f.getNameExt();
                for(FileObject ff : f.getChildren()) {
                    if (!ff.isFolder()) {
                        continue;
                    }

                    String mimeType = firstPart + "/" + ff.getNameExt(); //NOI18N
                    mimeTypes.add(mimeType);
                }
            }
        }
        
        return mimeTypes;
    }
    
    /**
     * Returns set of mimetypes.
     *
     * @return set of mimetypes
     */
    public Set<String> getMimeTypes () {
	if (mimeTypesWithColoring == null) {
            init ();
        }
	return mimeTypesWithColoring;
    }
    
    /**
     * Returns name of language for given mime type.
     *
     * @return name of language for given mime type
     */
    public String getLanguageName (String mimeType) {
        FileObject fo = Repository.getDefault().getDefaultFileSystem().findResource("Editors/" + mimeType); //NOI18N
        return fo == null ? mimeType : Utils.getLocalizedName(fo, mimeType, mimeType);
    }

    
    // FontColors ..............................................................

    /* package */ void notifyTokenFontColorChange(MimePath mimePath, String profile) {
        // XXX: this is hack, we should not abuse the event values like that
        pcs.firePropertyChange(PROP_TOKEN_COLORINGS, mimePath, profile);
    }
    
    /**
     * Gets display names of all font & color profiles.
     *
     * @return set of font & colors profiles
     */
    public Set<String> getFontColorProfiles () {
	if (fontColorProfiles == null) {
	    init ();
        }
        
        Set<String> result = new HashSet<String>();
        for(String profile : fontColorProfiles.keySet()) {
            if (!profile.startsWith ("test")) {
                result.add(profile);
            }
        }
        
	return result;
    }
    
    private Set<String> systemFontColorProfiles;
    
    /**
     * Returns true for user defined profile.
     *
     * @param profile a profile name
     * @return true for user defined profile
     */
    public boolean isCustomFontColorProfile(String profile) {
        if (systemFontColorProfiles == null) {
            init ();
        }
        
        return !systemFontColorProfiles.contains(profile);
    }
    
    private String currentFontColorProfile;
    
    /**
     * Returns name of current font & colors profile.
     *
     * @return name of current font & colors profile
     */
    public String getCurrentFontColorProfile () {
        if (currentFontColorProfile == null) {
            FileSystem fs = Repository.getDefault ().getDefaultFileSystem ();
            FileObject fo = fs.findResource (EDITORS_FOLDER);
            if (fo != null) {
                currentFontColorProfile = (String) fo.getAttribute(FATTR_CURRENT_FONT_COLOR_PROFILE);
            }
            if (currentFontColorProfile == null) {
                currentFontColorProfile = DEFAULT_PROFILE;
            }
        }
        if (!getFontColorProfiles ().contains (currentFontColorProfile)) {
            currentFontColorProfile = DEFAULT_PROFILE;
        }
        return currentFontColorProfile;
    }
    
    /**
     * Sets current font & colors profile.
     *
     * @param profile a profile name
     */
    public void setCurrentFontColorProfile (String profile) {
        String oldProfile = getCurrentFontColorProfile ();
        if (oldProfile.equals (profile)) return;

        currentFontColorProfile = profile;
        
        // Persist the change
	FileSystem fs = Repository.getDefault ().getDefaultFileSystem ();
	FileObject fo = fs.findResource (EDITORS_FOLDER);
        if (fo != null) {
            try {
                fo.setAttribute (FATTR_CURRENT_FONT_COLOR_PROFILE, profile);
            } catch (IOException ex) {
                LOG.log(Level.WARNING, "Can't persist change in current font&colors profile.", ex); //NOI18N
            }
        }

        // Notify others
        pcs.firePropertyChange (PROP_CURRENT_FONT_COLOR_PROFILE, oldProfile, currentFontColorProfile);
    }
    
    /**
     * Returns font & color defaults for given profile or null, if the profile
     * is unknown .
     *
     * @param profile a profile name
     * @return font & color defaults for given profile or null
     * 
     * @deprecated Use getFontColorSettings(new String[0]).getAllFontColors(profile) instead.
     */
    public Collection<AttributeSet> getDefaultFontColors(String profile) {
        return getFontColorSettings(new String[0]).getAllFontColors(profile);
    }
    
    /**
     * Returns default values for font & color defaults for given profile 
     * or null, if the profile is unknown.
     *
     * @param profile a profile name
     * @return font & color defaults for given profile or null
     * 
     * @deprecated Use getFontColorSettings(new String[0]).getAllFontColorsDefaults(profile) instead.
     */
    public Collection<AttributeSet> getDefaultFontColorDefaults(String profile) {
        return getFontColorSettings(new String[0]).getAllFontColorDefaults(profile);
    }
    
    /**
     * Sets font & color defaults for given profile.
     *
     * @param profile a profile name
     * @param fontColors font & color defaults to be used
     * 
     * @deprecated Use getFontColorSettings(new String[0]).setAllFontColors(profile, fontColors) instead.
     */
    public void setDefaultFontColors(String profile, Collection<AttributeSet> fontColors) {
        getFontColorSettings(new String[0]).setAllFontColors(profile, fontColors);
    }
    
    // Map (String (profile) > Map (String (category) > AttributeSet)).
    private Map<String, Map<String, AttributeSet>> highlightings = new HashMap<String, Map<String, AttributeSet>>();
    
    /**
     * Returns highlighting properties for given profile or null, if the 
     * profile is not known.
     *
     * @param profile a profile name
     * @return highlighting properties for given profile or null
     */
    public Map<String, AttributeSet> getHighlightings (
	String profile
    ) {
        // 1) translate profile name
	profile = getInternalFontColorProfile (profile);

        if (!highlightings.containsKey (profile)) {
            
            // 2) init profile for test mime types
            if (profile.startsWith ("test")) {
                highlightings.put (
                    profile,
                    getHighlightings (DEFAULT_PROFILE)
                );
            } else {
                
                // 3) read data form disk or cache
                Map<String, AttributeSet> m = ColoringStorage.loadColorings 
                    (MimePath.EMPTY, profile, false, false);
                highlightings.put (profile, m);
            }
        }
        
        if (highlightings.get(profile) == null) {
            return null;
        } else {
            return Collections.unmodifiableMap(highlightings.get(profile));
        }
    }
    
    // Map (String (profile) > Map (String (category) > AttributeSet)).
    private Map<String, Map<String, AttributeSet>> highlightingDefaults = new HashMap<String, Map<String, AttributeSet>>();
    
    /**
     * Returns defaults for highlighting properties for given profile,
     * or null if the profile is not known.
     *
     * @param profile a profile name
     * @return highlighting properties for given profile or null
     */
    public Map<String, AttributeSet> getHighlightingDefaults (
	String profile
    ) {
        // 1) translate profile name
	profile = getInternalFontColorProfile (profile);

        // 2) read data form disk or cache
        if (!highlightingDefaults.containsKey (profile)) {
            Map<String, AttributeSet> m = ColoringStorage.loadColorings 
                (MimePath.EMPTY, profile, false, true);
            highlightingDefaults.put (profile, m);
        }
        
        if (highlightingDefaults.get(profile) == null) {
            return null;
        } else {
            return Collections.unmodifiableMap(highlightingDefaults.get(profile));
        }
    }
    
    /**
     * Sets highlighting properties for given profile.
     *
     * @param profile a profile name
     * @param highlighting a highlighting properties to be used
     */
    public void setHighlightings (
	String  profile,
	Map<String, AttributeSet> fontColors
    ) {
        // 1) translate profile name
	String internalProfile = getInternalFontColorProfile (profile);	
        
        if (fontColors == null) {
            // 2) remove coloring / revert to defaults
            ColoringStorage.deleteColorings
                (MimePath.EMPTY, internalProfile, false, false);
            highlightings.remove (internalProfile);
            init ();
        } else {

            if (fontColors.equals (highlightings.get (internalProfile))) return;

            // 2) save new values to cache
            fontColors = Utils.immutize(fontColors);
            highlightings.put (internalProfile, fontColors);

            // 3) save new values to disk
            if (!internalProfile.startsWith ("test")) {
                ColoringStorage.saveColorings (
                    MimePath.EMPTY, 
                    internalProfile, 
                    false,
                    false,
                    fontColors.values ()
                );
                if (fontColorProfiles.get (profile) == null)
                    fontColorProfiles.put (profile, profile);
            }
        }
        
        pcs.firePropertyChange(PROP_HIGHLIGHT_COLORINGS, MimePath.EMPTY, internalProfile);
    }  
    
    
    // KeyMaps .................................................................

    /**
     * Returns set of keymap profiles.
     *
     * @return set of font & colors profiles
     */
    public Set<String> getKeyMapProfiles () {
	if (keyMapProfiles == null) init ();
	return Collections.unmodifiableSet (keyMapProfiles.keySet ());
    }
    
    private Set<String> systemKeymapProfiles;
    
    /**
     * Returns true for user defined profile.
     *
     * @param profile a profile name
     * @return true for user defined profile
     */
    public boolean isCustomKeymapProfile (String profile) {
        if (systemKeymapProfiles == null) {
            init();
        }
        
        return !systemKeymapProfiles.contains (profile);
    }
    
    private String currentKeyMapProfile;
    
    /**
     * Returns name of current keymap profile.
     *
     * @return name of current keymap profile
     */
    public String getCurrentKeyMapProfile () {
        if (currentKeyMapProfile == null) {
            FileSystem fs = Repository.getDefault ().getDefaultFileSystem ();
            FileObject fo = fs.findResource (KEYMAPS_FOLDER);
            currentKeyMapProfile = fo == null ? null : (String) fo.getAttribute (FATTR_CURRENT_KEYMAP_PROFILE);
            if (currentKeyMapProfile == null)
                currentKeyMapProfile = DEFAULT_PROFILE;
        }
        return currentKeyMapProfile;
    }
    
    /**
     * Sets current keymap profile.
     *
     * @param profile a profile name
     */
    public void setCurrentKeyMapProfile (String keyMapName) {
        String oldKeyMap = getCurrentKeyMapProfile ();
        if (oldKeyMap.equals (keyMapName)) return;

        currentKeyMapProfile = keyMapName;
        
        // Persist the change
        try {
            FileSystem fs = Repository.getDefault ().getDefaultFileSystem ();
            FileObject fo = fs.findResource (KEYMAPS_FOLDER);
            if (fo == null) {
                fo = fs.getRoot ().createFolder (KEYMAPS_FOLDER);
            }
            fo.setAttribute (FATTR_CURRENT_KEYMAP_PROFILE, keyMapName);
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Can't persist change in current keybindings profile.", ex); //NOI18N
        }
        
        // Notify others
        pcs.firePropertyChange (PROP_CURRENT_KEY_MAP_PROFILE, oldKeyMap, currentKeyMapProfile);
    }
    
    /**
     * PropertyChangeListener registration.
     *
     * @param l a PropertyChangeListener to be registerred
     */
    public void addPropertyChangeListener (
        PropertyChangeListener l
    ) {
        pcs.addPropertyChangeListener (l);
    }
    
    /**
     * PropertyChangeListener registration.
     *
     * @param l a PropertyChangeListener to be unregisterred
     */
    public void removePropertyChangeListener (
        PropertyChangeListener l
    ) {
        pcs.removePropertyChangeListener (l);
    }
    
    /**
     * PropertyChangeListener registration.
     *
     * @param propertyName  The name of the property to listen on.
     * @param l a PropertyChangeListener to be registerred
     */
    public void addPropertyChangeListener (
        String propertyName,
        PropertyChangeListener l
    ) {
        pcs.addPropertyChangeListener (propertyName, l);
    }
    
    /**
     * PropertyChangeListener registration.
     *
     * @param propertyName  The name of the property to listen on.
     * @param l a PropertyChangeListener to be unregisterred
     */
    public void removePropertyChangeListener (
        String propertyName,
        PropertyChangeListener l
    ) {
        pcs.removePropertyChangeListener (propertyName, l);
    }
    

    // support methods .........................................................
    
    private Map<String, String> fontColorProfiles;
    private Map<String, String> keyMapProfiles;
    private Set<String> mimeTypesWithColoring;

    private EditorSettingsImpl() {
        
    }
    
    private void init () {
	fontColorProfiles = new HashMap<String, String>();
	keyMapProfiles = new HashMap<String, String>();
	keyMapProfiles.put (DEFAULT_PROFILE, DEFAULT_PROFILE);
	mimeTypesWithColoring = new HashSet<String>();
        systemFontColorProfiles = new HashSet<String>();
        systemKeymapProfiles = new HashSet<String>();
	FileSystem fs = Repository.getDefault ().getDefaultFileSystem ();
	FileObject fo = fs.findResource (EDITORS_FOLDER);
        if (fo != null) {
            Enumeration e = fo.getFolders (false);
            while (e.hasMoreElements()) {
                init1 ((FileObject) e.nextElement ());
            }
        }
        
        mimeTypesWithColoring = Collections.unmodifiableSet(mimeTypesWithColoring);
    }
    
    private void init1 (FileObject fo) {
        Enumeration e = fo.getChildren (false);
        while (e.hasMoreElements ())
            init2 ((FileObject) e.nextElement ());
    }
	
    private void init2 (FileObject fo) {
        if (fo.getNameExt ().equals (ColoringStorage.DEFAULTS_FOLDER) && fo.isFolder () &&
            fo.getFileObject (ColoringStorage.HIGHLIGHTING_FILE_NAME) != null
        )
            addFontColorsProfile (fo, true); // Editors/ProfileName/Defaults/editorColoring.xml
        else
        if (fo.getNameExt ().equals (ColoringStorage.HIGHLIGHTING_FILE_NAME))
            addFontColorsProfile (fo, false); // Editors/ProfileName/editorColoring.xml
        else
        if (fo.getFileObject (DEFAULT_PROFILE + "/" + ColoringStorage.DEFAULTS_FOLDER + "/" + ColoringStorage.COLORING_FILE_NAME) != null) //NOI18N
            addMimeType (fo); // Editors/XXX/YYY/NetBeans/Defaults/coloring.xml
        else
        if (fo.getPath ().endsWith ("text/base") && fo.isFolder ()) { //NOI18N
            if (fo.getFileObject (KeyMapsStorage.DEFAULTS_FOLDER + "/" + KeyMapsStorage.KEYBINDING_FILE_NAME) != null) //NOI18N
                addKeyMapProfile (fo, true); // Editors/text/base/Defaults/keybindings.xml
            else
            if (fo.getFileObject (KeyMapsStorage.KEYBINDING_FILE_NAME) != null)
                addKeyMapProfile (fo, false); // Editors/text/base/keybindings.xml
            Enumeration e = fo.getChildren (false);
            while (e.hasMoreElements ()) {
                FileObject ff = (FileObject) e.nextElement ();
                if (!ff.getNameExt().equals(KeyMapsStorage.DEFAULTS_FOLDER)) {
                    init3 (ff);
                }
            }
        }
    }
        
    private void init3 (FileObject fo) {
        if (fo.getFileObject (KeyMapsStorage.DEFAULTS_FOLDER + "/" + KeyMapsStorage.KEYBINDING_FILE_NAME) != null) //NOI18N
            addKeyMapProfile (fo, true); // Editors/text/base/ProfileName/Defaults/keybindings.xml
        else
        if (fo.getFileObject (KeyMapsStorage.KEYBINDING_FILE_NAME) != null)
            addKeyMapProfile (fo, false); // Editors/text/base/ProfileName/keybindings.xml
    }

    private void addMimeType(FileObject fo) {
        String mimeType = fo.getPath().substring(8);
        mimeTypesWithColoring.add(mimeType);
    }
    
    private void addFontColorsProfile(FileObject fo, boolean systemProfile) {
        String profile = fo.getParent().getNameExt();
        String displayName = Utils.getLocalizedName(fo.getParent(), profile, profile);
        
        if (systemProfile) {
            systemFontColorProfiles.add(displayName);
        }
        
        fontColorProfiles.put(displayName, profile);
    }
    
    private void addKeyMapProfile(FileObject fo, boolean systemProfile) {
        String profile = fo.getNameExt();
        if (profile.equals("base")) { //NOI18N
            profile = DEFAULT_PROFILE;
        }
        
        String displayName = Utils.getLocalizedName(fo, profile, profile);
        
        if (systemProfile) {
            systemKeymapProfiles.add(displayName);
        }
        
        keyMapProfiles.put(displayName, profile);
    }
    
    /**
     * Translates profile's display name to its Id. If the profile's display name
     * can't be translated this method will simply return the profile's display name
     * without translation.
     */
    String getInternalFontColorProfile(String profile) {
	if (fontColorProfiles == null) {
	    init ();
        }
        
	String result = fontColorProfiles.get(profile);
        return result != null ? result : profile;
    }
    
    String getInternalKeymapProfile (String profile) {
	if (keyMapProfiles == null) {
	    init();
        }
        
	String result = keyMapProfiles.get (profile);
        if (result != null) {
            return result;
        } else {
            keyMapProfiles.put(profile, profile);
            return profile;
        }
    }
    
    public KeyBindingSettingsFactory getKeyBindingSettings (String[] mimeTypes) {
        mimeTypes = filter(mimeTypes);
        return KeyBindingSettingsImpl.get(Utils.mimeTypes2mimePath(mimeTypes));
    }

    public FontColorSettingsFactory getFontColorSettings (String[] mimeTypes) {
        mimeTypes = filter(mimeTypes);
        return FontColorSettingsImpl.get(Utils.mimeTypes2mimePath(mimeTypes));
    }
    
    private String [] filter(String [] mimeTypes) {
        if (mimeTypes.length > 0 && mimeTypes[0].startsWith("test")) { //NOI18N
            String [] filtered = new String [mimeTypes.length];
            System.arraycopy(mimeTypes, 0, filtered, 0, mimeTypes.length);
            filtered[0] = mimeTypes[0].substring(mimeTypes[0].indexOf('_') + 1); //NOI18N
            
            LOG.log(Level.INFO, "Don't use 'test' mime type to access settings through the editor/settings/storage API!", new Throwable("Stacktrace"));
            
            return filtered;
        } else {
            return mimeTypes;
        }
    }
}
