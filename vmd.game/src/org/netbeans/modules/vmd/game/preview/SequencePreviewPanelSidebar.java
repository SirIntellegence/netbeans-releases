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
package org.netbeans.modules.vmd.game.preview;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Timer;
import java.util.TimerTask;
import org.netbeans.modules.vmd.game.model.Sequence;
import org.netbeans.modules.vmd.game.model.SequenceListener;
import org.netbeans.modules.vmd.game.model.StaticTile;
import org.netbeans.modules.vmd.game.view.ImagePreviewComponent;

/**
 *
 * @author  kherink
 */
public class SequencePreviewPanelSidebar extends javax.swing.JPanel implements 
		SequenceListener, ComponentListener, 
		PropertyChangeListener, ActionListener {
    
	public static final boolean DEBUG = false;

	
	private boolean isPlaying;
	private boolean isPlayingForward = true;
	
	private int sequenceIndex;

	private Sequence sequence;
	private Timer timer;

	private ImagePreviewComponent imagePreview;

    public SequencePreviewPanelSidebar(Sequence sequence) {
		this.imagePreview = new ImagePreviewComponent(false, true, false);
        this.initComponents();
		this.addComponentListener(this);
		
		this.setSequence(sequence);
    }
    
	private Dimension getFrameSize() {
		if (this.sequence == null)
			return new Dimension(10, 10);
		return this.sequence.getFrameSize();
	}
	
	/**
	 * Method used to show that this preview is for a "special" sequence (e.g. the default sequence in a container).
	 * Currently only makes the name label use bold text.
	 */
	public void setImportant(boolean isImportant) {
		if (isImportant) {
			this.labelName.setFont(this.labelName.getFont().deriveFont(Font.BOLD));
		}
		else {
			this.labelName.setFont(this.labelName.getFont().deriveFont(Font.PLAIN));
		}
	}
	
	public void setSequence(Sequence sequence) {
		if (this.sequence == null) {
		}
		else {
			this.sequence.removeSequenceListener(this);
			this.sequence.removePropertyChangeListener(this);
			this.timer.cancel();
			int max = sequence.getFrameCount() -1;
		}
        this.sequence = sequence;
		this.labelName.setText(this.sequence.getName());
		
		this.spinnerPanel.add(new SequenceTimeSpinner(this.sequence), BorderLayout.CENTER);
		
		this.timer = new Timer();
		StaticTile frame = this.sequence.getFrame(0);
		this.imagePreview.setPreviewable(frame);
		this.sequence.addSequenceListener(this);
		this.timer.schedule(new AnimationTimerTask(), 0, this.sequence.getFrameMs());
		this.sequence.addPropertyChangeListener(this);
	}
	
	public void requestPreviewFrame(int frameIndex) {
		if (!this.isPlaying && this.sequence.getFrame(frameIndex) != null)
			this.imagePreview.setPreviewable(this.sequence.getFrame(frameIndex));
	}
	
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        jPanel1 = new javax.swing.JPanel();
        buttonPause = new javax.swing.JButton();
        buttonDirection = new javax.swing.JButton();
        panelSequenceAnimatedPreview = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        labelName = new javax.swing.JLabel();
        spinnerPanel = new javax.swing.JPanel();

        setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        buttonPause.setText(">");
        this.buttonPause.addActionListener(this);

        buttonDirection.setText("<>");
        this.buttonDirection.addActionListener(this);

        panelSequenceAnimatedPreview.setLayout(new java.awt.BorderLayout(10, 10));

        panelSequenceAnimatedPreview.setBackground(new java.awt.Color(255, 255, 255));
        panelSequenceAnimatedPreview.setMinimumSize(this.getFrameSize());
        this.panelSequenceAnimatedPreview.add(this.imagePreview);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(buttonDirection, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(buttonPause, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 69, Short.MAX_VALUE))
                .add(18, 18, 18)
                .add(panelSequenceAnimatedPreview, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(panelSequenceAnimatedPreview, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 56, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel1Layout.createSequentialGroup()
                        .add(buttonPause)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(buttonDirection)))
                .addContainerGap())
        );

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));
        labelName.setText("<None>");

        spinnerPanel.setLayout(new java.awt.BorderLayout());

        spinnerPanel.setBackground(new java.awt.Color(255, 255, 255));

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(labelName, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 79, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(spinnerPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 74, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(labelName)
                    .add(spinnerPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 15, Short.MAX_VALUE))
                .add(12, 12, 12))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonDirection;
    private javax.swing.JButton buttonPause;
    public javax.swing.JPanel jPanel1;
    public javax.swing.JPanel jPanel2;
    public javax.swing.JLabel labelName;
    private javax.swing.JPanel panelSequenceAnimatedPreview;
    public javax.swing.JPanel spinnerPanel;
    // End of variables declaration//GEN-END:variables
   
	private void sequenceChanged() {
		this.setSequence(this.sequence);
	}
	
	private void switchPlayDirection() {
		this.isPlayingForward = !this.isPlayingForward;
		this.setPlaying(true);
	}	
	
	private void setPlaying(boolean play) {
		if (play) {
			if (this.isPlayingForward)
				this.buttonPause.setText(">||");
			else {
				this.buttonPause.setText("||<");
			}
		}
		else {
			if (this.isPlayingForward) {
				this.buttonPause.setText(">");
			}
			else {
				this.buttonPause.setText("<");
			}
		}
		this.isPlaying = play;
	}
	
	private void incrementSequenceIndex() {
		int index = this.sequenceIndex + 1;
		if (index >= this.sequence.getFrameCount()) {
			this.sequenceIndex = 0;
		}
		else {
			this.sequenceIndex = index;
		}
	}
	private void decrementSequenceIndex() {
		int index = this.sequenceIndex - 1;
		if (index < 0) {
			this.sequenceIndex = this.sequence.getFrameCount() -1;
		}
		else {
			this.sequenceIndex = index;
		}
	}
	
	private void currentFrameChanged() {
		//this.slider.setValue(this.sequenceIndex);
	}
	
	private class AnimationTimerTask extends TimerTask {
		public void run() {
			if (!SequencePreviewPanelSidebar.this.isPlaying || !SequencePreviewPanelSidebar.this.isShowing())
				return;
			SequencePreviewPanelSidebar.this.setCurrentFrameIndex(SequencePreviewPanelSidebar.this.sequenceIndex);
			SequencePreviewPanelSidebar.this.currentFrameChanged();
			if (SequencePreviewPanelSidebar.this.isPlayingForward) {
				SequencePreviewPanelSidebar.this.incrementSequenceIndex();
			}
			else {
				SequencePreviewPanelSidebar.this.decrementSequenceIndex();
			}
//			System.out.println(System.nanoTime());
		}
	}

	private void setCurrentFrameIndex(int frameIndex) {
			StaticTile frame = null;
			do {
				try {
					frame = SequencePreviewPanelSidebar.this.sequence.getFrame(frameIndex);
				}
				catch (Exception e) {
					//e.printStackTrace();
					frameIndex--;
				}
			} while (frame == null);
			this.sequenceIndex = frameIndex;
			this.imagePreview.setPreviewable(frame);
	}
	
	//PropertyChangeListener-------------------------------------------------------------
	//Listen for changes in frame delay
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getSource() == this.sequence) {
			if (evt.getPropertyName().equals(Sequence.PROPERTY_FRAME_MS)) {
				this.timer.cancel();
				this.timer = new Timer();
				this.timer.schedule(new AnimationTimerTask(), 0, this.sequence.getFrameMs());
			}
			if (evt.getPropertyName().equals(Sequence.PROPERTY_NAME)) {
				this.labelName.setText(this.sequence.getName());
			}
		}
	}

	//ComponentListener-------------------------------------------------------------------
	public void componentShown(ComponentEvent e) {
	}
	public void componentHidden(ComponentEvent e) {
	}
	public void componentResized(ComponentEvent e) {
		if (DEBUG) System.out.println("SequenceAnimatedPreview Resized..."); // NOI18N
		//TODO : here i will recalculate cached images
	}
	public void componentMoved(ComponentEvent e) {
	}

	
	//ActionListener----------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.buttonDirection) {
			this.switchPlayDirection();
		}
		else {
			this.setPlaying(!this.isPlaying);
		}
	}

	//SequenceListener
	public void frameAdded(Sequence sequence, int index) {
		this.sequenceChanged();
	}
	public void frameRemoved(Sequence sequence, int index) {
		this.sequenceChanged();
	}
	public void frameModified(Sequence sequence, int index) {
		//TODO : here i would recache the modified frame image i think
	}	
}
