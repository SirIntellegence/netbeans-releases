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

package com.netbeans.developer.modules.loaders.properties;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.text.MessageFormat;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.Timer;
import javax.swing.text.StyledDocument;
import javax.swing.text.EditorKit;
import javax.swing.text.BadLocationException;


import org.openide.util.WeakListener;
import org.openide.util.NbBundle;
import org.openide.text.EditorSupport;
import org.openide.cookies.ViewCookie;
import org.openide.cookies.SaveCookie;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.DataObject;
import org.openide.filesystems.FileObject;
import org.openide.windows.CloneableTopComponent;
import org.openide.windows.TopComponent;
import org.openide.nodes.NodeAdapter;
import org.openide.nodes.Node;
import org.openide.TopManager;
import org.openide.NotifyDescriptor;

 
/** Support for viewing porperties files (ViewCookie) by opening them in a text editor */
public class PropertiesEditorSupport extends EditorSupport implements ViewCookie {

  /** Timer which countdowns the auto-reparsing time. */
  javax.swing.Timer timer;

  /** New lines in this file was delimited by '\n' */
  static final byte NEW_LINE_N = 0;

  /** New lines in this file was delimited by '\r' */
  static final byte NEW_LINE_R = 1;

  /** New lines in this file was delimited by '\r\n' */
  static final byte NEW_LINE_RN = 2;
  
  /** The type of new lines */
  byte newLineType;

  /** The flag saying if we should listen to the document modifications */
  private boolean listenToEntryModifs = true;

  /** Listener to the document changes - entry. The superclass holds a saving manager 
  * for the whole dataobject. */
  private EntrySavingManager entryModifL;

  /** Properties Settings */
  static final PropertiesSettings settings = new PropertiesSettings();

  /** Constructor */
  public PropertiesEditorSupport(PropertiesFileEntry entry) {
    super (entry);
    super.setModificationListening(false);
    setMIMEType ("text/plain");
    initTimer();

    // listen to myself so I can add a listener for changes when the document is loaded
    addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        if (isDocumentLoaded()) {
          getDocument().addDocumentListener(getEntryModifL());
        }
      }
    });

    //PENDING
    // set actions
    /*setActions (new SystemAction [] {
      SystemAction.get (CutAction.class),
      SystemAction.get (CopyAction.class),
      SystemAction.get (PasteAction.class),
    });*/
  }      
                                 
  /** Visible view of underlying file entry */
  PropertiesFileEntry myEntry = (PropertiesFileEntry)entry;
  
  /** Focuses existing component to open, or if none exists creates new.
  * @see OpenCookie#open
  */
  public void open () {
    try {
      MessageFormat mf = new MessageFormat (NbBundle.getBundle(PropertiesEditorSupport.class).
        getString ("CTL_PropertiesOpen"));
      
      TopManager.getDefault ().setStatusText (mf.format (
        new Object[] {
          entry.getFile().getName()
        }
      ));
      synchronized (allEditors) {
        try {
          TopComponent editor = (TopComponent)allEditors.getAnyComponent ();
          editor.requestFocus ();
        } catch (java.util.NoSuchElementException ex) {
          // no opened editor
          CloneableTopComponent editor = createCloneableTopComponent ();
          allEditors = editor.getReference ();
          editor.open ();
          editor.requestFocus();
        }
      }
    } finally {
      TopManager.getDefault ().setStatusText (NbBundle.getBundle(PropertiesEditorSupport.class).
        getString ("CTL_PropertiesOpened"));
    }
  }

  /** Launches the timer for autoreparse */              
  private void initTimer() {
    // initialize timer
    timer = new Timer(0, new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent e) {
        myEntry.getHandler().autoParse();
      }
    });
    timer.setInitialDelay(settings.getAutoParsingDelay());
    timer.setRepeats(false);

    // create document listener
    final DocumentListener docListener = new DocumentListener() {
      public void insertUpdate(DocumentEvent e) { change(e); }
      public void changedUpdate(DocumentEvent e) { }
      public void removeUpdate(DocumentEvent e) { change(e); }
      
      private void change(DocumentEvent e) {
        int delay = settings.getAutoParsingDelay();
        myEntry.getHandler().setDirty(true);
        if (delay > 0) {
          timer.setInitialDelay(delay);
          timer.restart();
        }
      }
    };

    // add change listener
    addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent evt) {
        if (isDocumentLoaded()) {
          getDocument().addDocumentListener(docListener);
        }
      }
    });
  }              
  

  public void saveThisEntry() throws IOException {
    super.saveDocument();
    myEntry.setModified(false);
  }

  /** Utility method which enables or disables listening to modifications
  * on asociated document.
  * <P>
  * Could be useful if we have to modify document, but do not want the
  * Save and Save All actions to be enabled/disabled automatically.
  * Initially modifications are listened to.
  * @param listenToModifs whether to listen to modifications
  */
  public void setModificationListening (final boolean listenToModifs) {
    this.listenToEntryModifs = listenToModifs;
    if (getDocument() == null) return;
    if (listenToEntryModifs)
      getDocument().addDocumentListener(getEntryModifL());
    else
      getDocument().removeDocumentListener(getEntryModifL());
  }

  /* A method to create a new component. Overridden in subclasses.
  * @return the {@link Editor} for this support
  */
  protected CloneableTopComponent createCloneableTopComponent () {
    // initializes the document if not initialized
    prepareDocument ();

    DataObject obj = myEntry.getDataObject ();
    Editor editor = new PropertiesEditor (obj, myEntry);
    return editor;
  }
  
  /** Should test whether all data is saved, and if not, prompt the user
  * to save.
  *
  * @return <code>true</code> if everything can be closed
  */
  protected boolean canClose () {
    SaveCookie savec = (SaveCookie) myEntry.getCookie(SaveCookie.class);
    if (savec != null) {
      MessageFormat format = new MessageFormat(NbBundle.getBundle(PropertiesEditorSupport.class).
        getString("MSG_SaveFile"));
      String msg = format.format(new Object[] { entry.getFile().getName()});
      NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg, NotifyDescriptor.YES_NO_CANCEL_OPTION);
      Object ret = TopManager.getDefault().notify(nd);
  
      if (NotifyDescriptor.CANCEL_OPTION.equals(ret))
        return false;
      
      if (NotifyDescriptor.YES_OPTION.equals(ret)) {
        try {
          savec.save();
        }
        catch (IOException e) {
          TopManager.getDefault().notifyException(e);
          return false;
        }
      }
    }
    return true;
  }

  /** Read the file from the stream, filter the guarded section
  * comments, and mark the sections in the editor.
  *
  * @param doc the document to read into
  * @param stream the open stream to read from
  * @param kit the associated editor kit
  * @throws IOException if there was a problem reading the file
  * @throws BadLocationException should not normally be thrown
  * @see #saveFromKitToStream
  */
  protected void loadFromStreamToKit (StyledDocument doc, InputStream stream, EditorKit kit) throws IOException, BadLocationException {
    
    NewLineInputStream is = new NewLineInputStream(stream);
    try {
      kit.read(is, doc, 0);
      newLineType = is.getNewLineType();
    }
    finally {
      is.close();
    }
  }

  /** Store the document and add the special comments signifying
  * guarded sections.
  *
  * @param doc the document to write from
  * @param kit the associated editor kit
  * @param stream the open stream to write to
  * @throws IOException if there was a problem writing the file
  * @throws BadLocationException should not normally be thrown
  * @see #loadFromStreamToKit
  */
  protected void saveFromKitToStream(StyledDocument doc, EditorKit kit, OutputStream stream) throws IOException, BadLocationException {
    OutputStream os = new NewLineOutputStream(stream, newLineType);
    try {
      kit.write(os, doc, 0, doc.getLength());
    }
    finally {
      if (os != null) {
        try {
          os.close();
        }
        catch (IOException e) {
        }
      }
    }
  }

  
  /** Does part of the cleanup - removes a listener.
  */
  private void closeDocumentEntry () {
    // listen to modifs
    if (listenToEntryModifs) {
      getEntryModifL().clearSaveCookie(); // ???
      if (getDocument() != null) {
        getDocument().removeDocumentListener(getEntryModifL());
      }
    }
  }

  /** Returns an entry saving manager. */
  private synchronized EntrySavingManager getEntryModifL () {
    if (entryModifL == null) {
      entryModifL = new EntrySavingManager();
      // listens whether to add or remove SaveCookie
      myEntry.addPropertyChangeListener(entryModifL);
    }
    return entryModifL;
  }
                           
  /** Make modifiedApendix accessible for inner classes. */
  String getModifiedAppendix() {
    return modifiedAppendix;
  }                       

  /** Cloneable top component to hold the editor kit.
  */
  class PropertiesEditor extends EditorSupport.Editor {
                                          
    /** Holds the file being edited */                                                                   
    protected PropertiesFileEntry entry;
                                                                       
    /** Listener for entry's save cookie changes */
    private PropertyChangeListener saveCookieLNode;
    /** Listener for entry's name changes */
    private NodeAdapter nodeL;

    /** Creates new editor */
    public PropertiesEditor(DataObject obj, PropertiesFileEntry entry) {
      super(obj, PropertiesEditorSupport.this);
      this.entry = entry;

      entry.getNodeDelegate().addNodeListener (
        new WeakListener.Node(nodeL = 
        new NodeAdapter () {
          public void propertyChange (PropertyChangeEvent ev) {
            if (ev.getPropertyName ().equals (Node.PROP_DISPLAY_NAME)) {
              updateName();
            }         
          }  
        }
      ));
      Node n = entry.getNodeDelegate ();
      setActivatedNodes (new Node[] { n });
      
      updateName();

      // entry to the set of listeners
      saveCookieLNode = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
          if (PresentableFileEntry.PROP_COOKIE.equals(evt.getPropertyName()) ||
              PresentableFileEntry.PROP_NAME.equals(evt.getPropertyName())) {
            updateName();
          }
        }
      };
      this.entry.addPropertyChangeListener(
        new WeakListener.PropertyChange(saveCookieLNode));
    }

    /** When closing last view, also close the document.
     * @return <code>true</code> if close succeeded
    */
    protected boolean closeLast () {
      boolean canClose = super.closeLast();
      if (!canClose)
        return false;
      PropertiesEditorSupport.this.closeDocumentEntry();
      return true;
    }

    /** Updates the name of this top component according to
    * the existence of the save cookie in ascoiated data object
    */
    protected void updateName () {
      if (entry == null) {
        setName("");
        return;
      }
      else {
        String name = entry.getFile().getName();
        if (entry.getCookie(SaveCookie.class) != null)
          setName(name + PropertiesEditorSupport.this.getModifiedAppendix());
        else
          setName(name);
      }  
    }
  
  } // end of PropertiesEditor inner class
  
  /** EntrySavingManager manages two tasks concerning saving:<P>
  * 1) It tracks changes in document asociated with ther entry and
  *    sets modification flag appropriately.<P>
  * 2) This class also implements functionality of SaveCookie interface
  */
  private final class EntrySavingManager implements DocumentListener, SaveCookie, PropertyChangeListener {

    /*********** Implementation of the DocumentListener *******/

    /** Gives notification that an attribute or set of attributes changed.
    * @param ev event describing the action
    */
    public void changedUpdate(DocumentEvent ev) {
      // do nothing - just an attribute
    }

    /** Gives notification that there was an insert into the document.
    * @param ev event describing the action
    */
    public void insertUpdate(DocumentEvent ev) {
      modified();
    }

    /** Gives notification that a portion of the document has been removed.
    * @param ev event describing the action
    */
    public void removeUpdate(DocumentEvent ev) {
      modified();
    }

    /** Gives notification that the DataObject was changed.
    * @param ev PropertyChangeEvent
    */
    public void propertyChange(PropertyChangeEvent ev) {
      if ((ev.getSource() == myEntry) &&
          (ev.getPropertyName() == PropertiesFileEntry.PROP_MODIFIED)) {
        
        if (((Boolean) ev.getNewValue()).booleanValue()) {
          addSaveCookie();
        } else {
          removeSaveCookie();
        }
      }
    }

    /******* Implementation of the Save Cookie *********/

    public void save () throws IOException {
      // do saving job
      saveThisEntry();
    }

    void clearSaveCookie() {
      // remove save cookie (if save was succesfull)
      myEntry.setModified(false);
    }

    /** Sets modification flag.
    */
    private void modified () {
      myEntry.setModified(true);
    }
    /** Adds save cookie to the DO.
    */
    private void addSaveCookie() {
      // add Save cookie to the entry       
      if (myEntry.getCookie(SaveCookie.class) == null) {
        myEntry.getCookieSet().add(this);
      }
      ((PropertiesDataObject)myEntry.getDataObject()).updateModificationStatus();
    }
    /** Removes save cookie from the DO.
    */
    private void removeSaveCookie() {                   
      // remove Save cookie from the data object
      if (myEntry.getCookie(SaveCookie.class) == this) {
        myEntry.getCookieSet().remove(this);
      }
      ((PropertiesDataObject)myEntry.getDataObject()).updateModificationStatus();
    }
    
  } // end of EntrySavingManager inner class


  /** This stream is able to filter various new line delimiters and replace them by \n.
  */
  static class NewLineInputStream extends InputStream {
    /** Encapsulated input stream */
    BufferedInputStream bufis;

    /** Next character to read. */
    int nextToRead;
    
    /** The count of types new line delimiters used in the file */
    int[] newLineTypes;
    
    /** Creates new stream.
    * @param is encapsulated input stream.
    * @param justFilter The flag determining if this stream should
    *        store the guarded block information. True means just filter,
    *        false means store the information.
    */
    public NewLineInputStream(InputStream is) throws IOException {
      bufis = new BufferedInputStream(is);
      nextToRead = bufis.read();
      newLineTypes = new int[] { 0, 0, 0 };
    }

    /** Reads one character.
    * @return next char or -1 if the end of file was reached.
    * @exception IOException if any problem occured.
    */
    public int read() throws IOException {
      if (nextToRead == -1)
        return -1;
              
      if (nextToRead == '\r') { 
        nextToRead = bufis.read();
        if (nextToRead == '\n') {     
          nextToRead = bufis.read();
          newLineTypes[NEW_LINE_RN]++;
          return '\n';
        }
        else {
          newLineTypes[NEW_LINE_R]++;
          return '\n';
        }
      }            
      if (nextToRead == '\n') {
        nextToRead = bufis.read();
        newLineTypes[NEW_LINE_N]++;
        return '\n';
      }
      int oldNextToRead = nextToRead;
      nextToRead = bufis.read();
      return oldNextToRead;
    }  
      
    public byte getNewLineType() {
      if (newLineTypes[0] > newLineTypes[1]) {
        return (newLineTypes[0] > newLineTypes[2]) ? (byte) 0 : 2;
      }
      else {
        return (newLineTypes[1] > newLineTypes[2]) ? (byte) 1 : 2;
      }
    }
  }


  /** This stream is used for changing the new line delimiters.
  * It replaces the '\n' by '\n', '\r' or "\r\n"
  */
  private static class NewLineOutputStream extends OutputStream {
    /** Underlaying stream. */
    OutputStream stream;
    
    /** The type of new line delimiter */
    byte newLineType;
    
    /** Creates new stream.
    * @param stream Underlaying stream
    * @param newLineType The type of new line delimiter
    */
    public NewLineOutputStream(OutputStream stream, byte newLineType) {
      this.stream = stream;
      this.newLineType = newLineType;
    }

    /** Write one character.
    * @param b char to write.
    */
    public void write(int b) throws IOException {
      if (b == '\n') {
        switch (newLineType) {
        case NEW_LINE_R:
          stream.write('\r');
          break;
        case NEW_LINE_RN:
          stream.write('\r');
        case NEW_LINE_N:
          stream.write('\n');
          break;
        }
      }
      else {
        stream.write(b);
      }
    }

    /** Closes the underlaying stream.
    */
    public void close() throws IOException {
      stream.flush();
      stream.close();
    }
  }
  

}
