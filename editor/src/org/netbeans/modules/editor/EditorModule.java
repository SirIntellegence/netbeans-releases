/*
 *                 Sun Public License Notice
 * 
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 * 
 * The Original Code is Forte for Java, Community Edition. The Initial
 * Developer of the Original Code is Sun Microsystems, Inc. Portions
 * Copyright 1997-2000 Sun Microsystems, Inc. All Rights Reserved.
 */

package com.netbeans.developer.modules.text;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.Writer;
import java.io.File;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.JEditorPane;
import com.netbeans.editor.BaseDocument;
import com.netbeans.editor.BaseKit;
import com.netbeans.editor.Settings;
import com.netbeans.editor.ext.JavaKit;
import com.netbeans.editor.Formatter;
import com.netbeans.editor.view.DialogSupport;
import com.netbeans.editor.ext.ExtSettings;
import org.openide.modules.ModuleInstall;
import org.openide.text.IndentEngine;
import org.openide.TopManager;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.LocalFileSystem;
import com.netbeans.developer.modules.text.java.JCStorage;

/**
* Module installation class for editor
*
* @author Miloslav Metelka
*/
public class EditorModule implements ModuleInstall {

  private static final String DB_DIR = "ParserDB";

  private static final String MIME_PLAIN = "text/plain";
  private static final String MIME_JAVA = "text/x-java";
  private static final String MIME_HTML = "text/html";
  private static final String MIME_IDL = "text/x-idl";

  /** Kit replacements that will be installed into JEditorPane */
  KitInfo[] replacements = new KitInfo[] {
    new KitInfo(MIME_PLAIN, "com.netbeans.developer.modules.text.NbEditorPlainKit"),
    new KitInfo(MIME_JAVA, "com.netbeans.developer.modules.text.NbEditorJavaKit"),
    new KitInfo(MIME_HTML, "com.netbeans.developer.modules.text.NbEditorHTMLKit"),
    new KitInfo(MIME_IDL, "com.netbeans.developer.modules.text.NbEditorIDLKit")
  };

  private static PropertyChangeListener settingsListener;

  static {
    settingsListener = new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        registerIndents();
      }
    };
    Settings.addPropertyChangeListener(settingsListener);
  }

  private static void registerIndents() {
    IndentEngine.register(MIME_JAVA,
        new FilterIndentEngine(Formatter.getFormatter(JavaKit.class)));
  }

  /** Module installed for the first time. */
  public void installed () {
    restored ();
  }

  /** Module installed again. */
  public void restored () {

    NbEditorSettings.initLocale();

    // initializations
    FileSystem rfs = TopManager.getDefault().getRepository().getDefaultFileSystem();
    File rootDir = ((LocalFileSystem)rfs).getRootDirectory();

    DialogSupport.setDialogCreator(new NbDialogCreator());
    ExtSettings.init();
    NbEditorSettings.init();
    KitSupport.init();
    File dbDir = new File(rootDir.getAbsolutePath() + File.separator + DB_DIR);
    if (!dbDir.exists()) {
      dbDir.mkdir();
    }
    JCStorage.init(dbDir.getAbsolutePath());

    // preload some classes for faster editor opening
    BaseKit.getKit(NbEditorJavaKit.class).createDefaultDocument();

    // install new kits
    for (int i = 0; i < replacements.length; i++) {
      JEditorPane.registerEditorKitForContentType(
        replacements[i].contentType,
        replacements[i].newKitClassName,
        getClass().getClassLoader()
      );
    }

  }

  /** Module was uninstalled. */
  public void uninstalled () {
  }

  /** Module is being closed. */
  public boolean closing () {
    return true; // agree to close
  }

  static class KitInfo {

    /** Content type for which the kits will be switched */
    String contentType;

    /** Class name of the kit that will be registered */
    String newKitClassName;

    KitInfo(String contentType, String newKitClassName) {
      this.contentType = contentType;
      this.newKitClassName = newKitClassName;
    }

  }

  static class FilterIndentEngine extends IndentEngine {

    Formatter formatter;

    FilterIndentEngine(Formatter formatter) { 
      this.formatter = formatter;
    }

    public int indentLine (Document doc, int offset) {
      return formatter.indentLine(doc, offset);
    }
    
    public int indentNewLine (Document doc, int offset) {
      return formatter.indentNewLine(doc, offset);
    }
    
    public Writer createWriter (Document doc, int offset, Writer writer) {
      return formatter.createWriter(doc, offset, writer);
    }

  }

}

/*
 * Log
 *  19   Gandalf   1.18        7/20/99  Miloslav Metelka Creation of ParserDB dir
 *       if necessary
 *  18   Gandalf   1.17        7/20/99  Miloslav Metelka 
 *  17   Gandalf   1.16        7/9/99   Miloslav Metelka 
 *  16   Gandalf   1.15        6/9/99   Miloslav Metelka 
 *  15   Gandalf   1.14        6/9/99   Ian Formanek    ---- Package Change To 
 *       org.openide ----
 *  14   Gandalf   1.13        6/8/99   Miloslav Metelka 
 *  13   Gandalf   1.12        6/1/99   Miloslav Metelka 
 *  12   Gandalf   1.11        6/1/99   Miloslav Metelka 
 *  11   Gandalf   1.10        5/5/99   Miloslav Metelka 
 *  10   Gandalf   1.9         4/23/99  Miloslav Metelka Differrent document 
 *       constructor
 *  9    Gandalf   1.8         4/13/99  Ian Formanek    Fixed bug #1518 - 
 *       java.lang.NoClassDefFoundError: 
 *       com/netbeans/developer/modules/text/NbEditorBaseKit thrown on startup.
 *  8    Gandalf   1.7         4/8/99   Miloslav Metelka 
 *  7    Gandalf   1.6         3/18/99  Miloslav Metelka 
 *  6    Gandalf   1.5         3/11/99  Jaroslav Tulach Works with plain 
 *       document.
 *  5    Gandalf   1.4         3/10/99  Jaroslav Tulach body of install moved to
 *       restored.
 *  4    Gandalf   1.3         3/9/99   Ian Formanek    Fixed last change
 *  3    Gandalf   1.2         3/9/99   Ian Formanek    Removed obsoleted import
 *  2    Gandalf   1.1         3/8/99   Jesse Glick     For clarity: Module -> 
 *       ModuleInstall; NetBeans-Module-Main -> NetBeans-Module-Install.
 *  1    Gandalf   1.0         2/4/99   Miloslav Metelka 
 * $
 */
