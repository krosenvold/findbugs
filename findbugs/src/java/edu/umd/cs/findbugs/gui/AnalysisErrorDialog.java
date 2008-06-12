/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * AnalysisErrorDialog.java
 *
 * Created on June 5, 2003, 3:20 PM
 */

package edu.umd.cs.findbugs.gui;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.L10N;

/**
 * A dialog to report errors that occured during analysis.
 *
 * @author David Hovemeyer
 */
public class AnalysisErrorDialog extends javax.swing.JDialog {

	private BugReporter reporter;

	/**
	 * Creates new form AnalysisErrorDialog
	 */
	public AnalysisErrorDialog(java.awt.Frame parent, boolean modal, BugReporter reporter) {
		super(parent, modal);
		this.reporter = reporter;
		initComponents();
	}

	public void generateContents() {
		reporter.reportQueuedErrors();
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	private void initComponents() {//GEN-BEGIN:initComponents
		java.awt.GridBagConstraints gridBagConstraints;

		errorLabel = new javax.swing.JLabel();
		errorMessageScrollPane = new javax.swing.JScrollPane();
		errorMessageTextArea = new javax.swing.JTextPane();
		jSeparator1 = new javax.swing.JSeparator();
		leftSpacer = new javax.swing.JLabel();
		rightSpacer = new javax.swing.JLabel();
		okButton = new javax.swing.JButton();
		analysisMenuBar = new javax.swing.JMenuBar();
		editMenu = new javax.swing.JMenu();
		selectAllMenuItem = new javax.swing.JMenuItem();
		copyMenuItem = new javax.swing.JMenuItem();

		getContentPane().setLayout(new java.awt.GridBagLayout());

		setTitle("Analysis Errors");
		setTitle(L10N.getLocalString("dlg.analysiserrors_ttl", "Analysis Errors"));
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent evt) {
				closeDialog(evt);
			}
		});

		errorLabel.setFont(new java.awt.Font("Dialog", 0, 12));
		errorLabel.setText("Errors occured during the analysis:");
		errorLabel.setText(L10N.getLocalString("dlg.analysiserror_lbl", "Errors occurred during analysis:"));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 3;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(6, 6, 3, 0);
		getContentPane().add(errorLabel, gridBagConstraints);

		errorMessageTextArea.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
		errorMessageTextArea.setEditable(false);
		errorMessageScrollPane.setViewportView(errorMessageTextArea);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(4, 6, 4, 6);
		getContentPane().add(errorMessageScrollPane, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.gridwidth = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		getContentPane().add(jSeparator1, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 0.5;
		getContentPane().add(leftSpacer, gridBagConstraints);

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.weightx = 0.5;
		getContentPane().add(rightSpacer, gridBagConstraints);

		okButton.setMnemonic('O');
		okButton.setText("OK");
		okButton.setText(L10N.getLocalString("dlg.ok_btn", "OK"));
		okButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				okButtonActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
		getContentPane().add(okButton, gridBagConstraints);

		analysisMenuBar.setFont(new java.awt.Font("Dialog", 0, 12));
		editMenu.setText("Edit");
		editMenu.setFont(new java.awt.Font("Dialog", 0, 12));
		L10N.localiseButton(editMenu, "menu.edit_menu", "&Edit", true);
		editMenu.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				editMenuActionPerformed(evt);
			}
		});

		selectAllMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
		selectAllMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
		selectAllMenuItem.setText("Select All");
		L10N.localiseButton(selectAllMenuItem, "menu.selectall_item", "Select &All", true);
		selectAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				selectAllItemActionListener(evt);
			}
		});

		editMenu.add(selectAllMenuItem);

		copyMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
		copyMenuItem.setFont(new java.awt.Font("Dialog", 0, 12));
		copyMenuItem.setText("Copy");
		L10N.localiseButton(copyMenuItem, "menu.copy_item", "Copy", true);
		copyMenuItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				copyMenuItemActionPerformed(evt);
			}
		});

		editMenu.add(copyMenuItem);

		analysisMenuBar.add(editMenu);

		setJMenuBar(analysisMenuBar);

		pack();
	}//GEN-END:initComponents

	private void copyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyMenuItemActionPerformed
		errorMessageTextArea.copy();
	}//GEN-LAST:event_copyMenuItemActionPerformed

	private void selectAllItemActionListener(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllItemActionListener
		errorMessageTextArea.selectAll();
	}//GEN-LAST:event_selectAllItemActionListener

	private void editMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editMenuActionPerformed
		// TODO add your handling code here:
	}//GEN-LAST:event_editMenuActionPerformed

	private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
		closeDialog(null);
	}//GEN-LAST:event_okButtonActionPerformed

	/**
	 * Closes the dialog
	 */
	private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	private StringBuilder buf = new StringBuilder();

	public void addLine(String line) {
		//System.out.println("Appending: " + line);
		buf.append(line);
		buf.append('\n');
	}

	public void clear() {
		errorMessageTextArea.setText("");
	}

	public void finish() {
		errorMessageTextArea.setText(buf.toString());
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JMenuBar analysisMenuBar;
	private javax.swing.JMenuItem copyMenuItem;
	private javax.swing.JMenu editMenu;
	private javax.swing.JLabel errorLabel;
	private javax.swing.JScrollPane errorMessageScrollPane;
	private javax.swing.JTextPane errorMessageTextArea;
	private javax.swing.JSeparator jSeparator1;
	private javax.swing.JLabel leftSpacer;
	private javax.swing.JButton okButton;
	private javax.swing.JLabel rightSpacer;
	private javax.swing.JMenuItem selectAllMenuItem;
	// End of variables declaration//GEN-END:variables

}
