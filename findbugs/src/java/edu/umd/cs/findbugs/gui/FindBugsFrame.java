/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
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
 * FindBugsFrame.java
 *
 * Created on March 30, 2003, 12:05 PM
 */

package edu.umd.cs.findbugs.gui;

import java.lang.reflect.Method;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.*;
import javax.swing.tree.*;
import java.text.MessageFormat;
import java.awt.event.KeyEvent;
import java.awt.datatransfer.*;


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.SourceFinder;

/**
 * The main GUI frame for FindBugs.
 *
 * @author David Hovemeyer
 */
public class FindBugsFrame extends javax.swing.JFrame {

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static final Color HIGH_PRIORITY_COLOR = new Color(0xff0000);
	private static final Color NORMAL_PRIORITY_COLOR = new Color(0x9f0000);
	private static final Color LOW_PRIORITY_COLOR = Color.BLACK;

	/**
	 * Custom cell renderer for the bug tree.
	 * We use this to select the tree icons, and to set the
	 * text color based on the bug priority.
	 */
	private static class BugCellRenderer extends DefaultTreeCellRenderer {
		private ImageIcon bugGroupIcon;
		private ImageIcon packageIcon;
		private ImageIcon bugIcon;
		private ImageIcon classIcon;
		private ImageIcon methodIcon;
		private ImageIcon fieldIcon;
		private ImageIcon sourceFileIcon;
		private Object value;

		public BugCellRenderer() {
			ClassLoader classLoader = this.getClass().getClassLoader();
			bugGroupIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/bug.png"));
			packageIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/package.png"));
			bugIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/bug2.png"));
			classIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/class.png"));
			methodIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/method.png"));
			fieldIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/field.png"));
			sourceFileIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/sourcefile.png"));
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
		                                              boolean expanded, boolean leaf, int row, boolean hasFocus) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object obj = node.getUserObject();

			this.value = obj;

			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

			// Set the icon, depending on what kind of node it is
			if (obj instanceof BugInstance) {
				setIcon(bugIcon);
			} else if (obj instanceof ClassAnnotation) {
				setIcon(classIcon);
			} else if (obj instanceof MethodAnnotation) {
				setIcon(methodIcon);
			} else if (obj instanceof FieldAnnotation) {
				setIcon(fieldIcon);
			} else if (obj instanceof SourceLineAnnotation) {
				setIcon(sourceFileIcon);
			} else if (obj instanceof BugInstanceGroup) {
				// This is a "group" node
				BugInstanceGroup groupNode = (BugInstanceGroup) obj;
				String groupType = groupNode.getGroupType();
				if (groupType == GROUP_BY_CLASS) {
					setIcon(classIcon);
				} else if (groupType == GROUP_BY_PACKAGE) {
					setIcon(packageIcon);
				} else if (groupType == GROUP_BY_BUG_TYPE) {
					setIcon(bugGroupIcon);
				}
			} else {
				setIcon(null);
			}

			return this;
		}

		public Color getTextNonSelectionColor() {
			return getCellTextColor();
		}

		private Color getCellTextColor() {
			// Based on the priority, color-code the bug instance.
			Color color = Color.BLACK;
			if (value instanceof BugInstance) {
				BugInstance bugInstance = (BugInstance) value;
				switch (bugInstance.getPriority()) {
				case Detector.LOW_PRIORITY:
					color = LOW_PRIORITY_COLOR;
					break;
				case Detector.NORMAL_PRIORITY:
					color = NORMAL_PRIORITY_COLOR;
					break;
				case Detector.HIGH_PRIORITY:
					color = HIGH_PRIORITY_COLOR;
					break;
				}
			}
			return color;
		}
	}

	/**
	 * The instance of BugCellRenderer.
	 */
	private static final FindBugsFrame.BugCellRenderer bugCellRenderer = new FindBugsFrame.BugCellRenderer();

	/**
	 * Tree node type for BugInstances.
	 * We use this instead of plain DefaultMutableTreeNodes in order to
	 * get more control over the exact text that is shown in the tree.
	 */
	private class BugTreeNode extends DefaultMutableTreeNode {
		private int count;

		public BugTreeNode(BugInstance bugInstance) {
			super(bugInstance);
			count = -1;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public String toString() {
			try {
				BugInstance bugInstance = (BugInstance) getUserObject();
				StringBuffer result = new StringBuffer();

				if (count >= 0) {
					result.append(count);
					result.append(": ");
				}

				if (bugInstance.isExperimental())
					result.append(L10N.getLocalString("msg.exp_txt", "EXP: "));

				result.append(fullDescriptionsItem.isSelected() ? bugInstance.getMessage() : bugInstance.toString());

				return result.toString();
			} catch (Exception e) {
				return MessageFormat.format(L10N.getLocalString("msg.errorformatting_txt", "Error formatting message for bug: "), new Object[]{e.toString()});
			}
		}
	}

	/**
	 * Compare BugInstance class names.
	 * This is useful for grouping bug instances by class.
	 * Note that all instances with the same class name will compare
	 * as equal.
	 */
	private static class BugInstanceClassComparator implements Comparator<BugInstance> {
		public int compare(BugInstance lhs, BugInstance rhs) {
			return lhs.getPrimaryClass().compareTo(rhs.getPrimaryClass());
		}
	}

	/**
	 * The instance of BugInstanceClassComparator.
	 */
	private static final Comparator<BugInstance> bugInstanceClassComparator = new BugInstanceClassComparator();

	/**
	 * Compare BugInstance package names.
	 * This is useful for grouping bug instances by package.
	 * Note that all instances with the same package name will compare
	 * as equal.
	 */
	private static class BugInstancePackageComparator implements Comparator<BugInstance> {
		public int compare(BugInstance lhs, BugInstance rhs) {
			return lhs.getPrimaryClass().getPackageName().compareTo(rhs.getPrimaryClass().getPackageName());
		}
	}

	/**
	 * The instance of BugInstancePackageComparator.
	 */
	private static final Comparator<BugInstance> bugInstancePackageComparator = new BugInstancePackageComparator();

	/**
	 * Compare BugInstance bug types.
	 * This is useful for grouping bug instances by bug type.
	 * Note that all instances with the same bug type will compare
	 * as equal.
	 */
	private static class BugInstanceTypeComparator implements Comparator<BugInstance> {
		public int compare(BugInstance lhs, BugInstance rhs) {
			String lhsString = lhs.toString();
			String rhsString = rhs.toString();
			return lhsString.substring(0, lhsString.indexOf(':')).compareTo(rhsString.substring(0, rhsString.indexOf(':')));
		}
	}

	/**
	 * The instance of BugInstanceTypeComparator.
	 */
	private static final Comparator<BugInstance> bugInstanceTypeComparator = new BugInstanceTypeComparator();

	/**
	 * Two-level comparison of bug instances by class name and
	 * BugInstance natural ordering.
	 */
	private static class BugInstanceByClassComparator implements Comparator<BugInstance> {
		public int compare(BugInstance a, BugInstance b) {
			int cmp = bugInstanceClassComparator.compare(a, b);
			if (cmp != 0)
				return cmp;
			return a.compareTo(b);
		}
	}

	/**
	 * The instance of BugInstanceByClassComparator.
	 */
	private static final Comparator<BugInstance> bugInstanceByClassComparator = new FindBugsFrame.BugInstanceByClassComparator();

	/**
	 * Two-level comparison of bug instances by package and
	 * BugInstance natural ordering.
	 */
	private static class BugInstanceByPackageComparator implements Comparator<BugInstance> {
		public int compare(BugInstance a, BugInstance b) {
			int cmp = bugInstancePackageComparator.compare(a, b);
			if (cmp != 0)
				return cmp;
			return a.compareTo(b);
		}
	}

	/**
	 * The instance of BugInstanceByPackageComparator.
	 */
	private static final Comparator<BugInstance> bugInstanceByPackageComparator = new FindBugsFrame.BugInstanceByPackageComparator();

	/**
	 * Two-level comparison of bug instances by bug type and
	 * BugInstance natural ordering.
	 */
	private static class BugInstanceByTypeComparator implements Comparator<BugInstance> {
		public int compare(BugInstance a, BugInstance b) {
			int cmp = bugInstanceTypeComparator.compare(a, b);
			if (cmp != 0)
				return cmp;
			return a.compareTo(b);
		}
	}

	/**
	 * The instance of BugTypeByTypeComparator.
	 */
	private static final Comparator<BugInstance> bugInstanceByTypeComparator = new FindBugsFrame.BugInstanceByTypeComparator();

	/**
	 * Swing FileFilter class for file selection dialogs for FindBugs project files.
	 */
	private static class ProjectFileFilter extends FileFilter {
		public boolean accept(File file) {
			return file.isDirectory() || file.getName().endsWith(".fb");
		}

		public String getDescription() {
			return L10N.getLocalString("dlg.findbugsprojects_lbl", "FindBugs projects (*.fb)");
		}
	}

	/**
	 * The instance of ProjectFileFilter.
	 */
	private static final FileFilter projectFileFilter = new ProjectFileFilter();

	/**
	 * Swing FileFilter for choosing an auxiliary classpath entry.
	 * Both Jar files and directories can be chosen.
	 */
	private static class AuxClasspathEntryFileFilter extends FileFilter {
		public boolean accept(File file) {
			return file.isDirectory() || file.getName().endsWith(".jar");
		}

		public String getDescription() {
			return L10N.getLocalString("dlg.jarsanddirectories_lbl", "Jar files and directories");
		}
	}

	/**
	 * The instance of AuxClasspathEntryFileFilter.
	 */
	private static final FileFilter auxClasspathEntryFileFilter = new AuxClasspathEntryFileFilter();

	/**
	 * Swing FileFilter for choosing XML saved bug files.
	 */
	private static class XMLFileFilter extends FileFilter {
		public boolean accept(File file) {
			return file.isDirectory() || file.getName().endsWith(".xml");
		}

		public String getDescription() {
			return L10N.getLocalString("dlg.xmlsavedbugs_lbl", "XML saved bug files");
		}
	}

	/**
	 * The instance of XMLFileFilter.
	 */
	private static final FileFilter xmlFileFilter = new XMLFileFilter();

	/**
	 * Set of archive file extensions.
	 */
	private static final HashSet<String> archiveExtensionSet = new HashSet<String>();

	static {
		archiveExtensionSet.add(".jar");
		archiveExtensionSet.add(".zip");
		archiveExtensionSet.add(".ear");
		archiveExtensionSet.add(".war");
	}

	/**
	 * File filter for choosing archives and directories.
	 */
	private static class ArchiveAndDirectoryFilter extends FileFilter {
		public boolean accept(File file) {
			if (file.isDirectory())
				return true;

			String fileName = file.getName();
			int dot = fileName.lastIndexOf('.');
			if (dot < 0)
				return false;
			String extension = fileName.substring(dot);
			return archiveExtensionSet.contains(extension);
		}

		public String getDescription() {
			return L10N.getLocalString("dlg.javaarchives_lbl", "Java archives (*.jar,*.zip,*.ear,*.war)");
		}
	};

	/**
	 * The instance of ArchiveAndDirectoryFilter.
	 */
	private static final FileFilter archiveAndDirectoryFilter = new ArchiveAndDirectoryFilter();

	/* ----------------------------------------------------------------------
	 * Constants
	 * ---------------------------------------------------------------------- */

	private static final String GROUP_BY_CLASS = "By class";
	private static final String GROUP_BY_PACKAGE = "By package";
	private static final String GROUP_BY_BUG_TYPE = "By bug type";
	private static final String[] GROUP_BY_ORDER_LIST = {
		GROUP_BY_CLASS, GROUP_BY_PACKAGE, GROUP_BY_BUG_TYPE
	};

	/**
	 * A fudge value required in our hack to get the REAL maximum
	 * divider location for a JSplitPane.  Experience suggests that
	 * the value "1" would work here, but setting it a little higher
	 * makes the code a bit more robust.
	 */
	private static final int DIVIDER_FUDGE = 3;

	private static final boolean BUG_COUNT = Boolean.getBoolean("findbugs.gui.bugCount");

        /* ----------------------------------------------------------------------
         * Member fields
         * ---------------------------------------------------------------------- */
        Component selectedComponent = null;
	/* ----------------------------------------------------------------------
	 * Constructor
	 * ---------------------------------------------------------------------- */

	/**
	 * Creates new form FindBugsFrame.
	 */
	public FindBugsFrame() {
		String dirProp = System.getProperty("user.dir");

		if (dirProp != null) {
			currentDirectory = new File(dirProp);
		}

		UserPreferences prefs = UserPreferences.getUserPreferences();
		prefs.read();

		prefs.loadUserDetectorPreferences();
		priorityThreshold = prefs.getUserDetectorThreshold();

		initComponents();
		postInitComponents();
        }

	/* ----------------------------------------------------------------------
	 * Component initialization and event handlers
	 * ---------------------------------------------------------------------- */

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        consoleSplitter = new javax.swing.JSplitPane();
        viewPanel = new javax.swing.JPanel();
        emptyPanel = new javax.swing.JPanel();
        reportPanel = new javax.swing.JPanel();
        editProjectPanel = new javax.swing.JPanel();
        jarFileLabel = new javax.swing.JLabel();
        jarNameTextField = new javax.swing.JTextField();
        addJarButton = new javax.swing.JButton();
        jarFileListLabel = new javax.swing.JLabel();
        sourceDirLabel = new javax.swing.JLabel();
        srcDirTextField = new javax.swing.JTextField();
        addSourceDirButton = new javax.swing.JButton();
        sourceDirListLabel = new javax.swing.JLabel();
        removeJarButton = new javax.swing.JButton();
        removeSrcDirButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        browseJarButton = new javax.swing.JButton();
        browseSrcDirButton = new javax.swing.JButton();
        editProjectLabel = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        findBugsButton = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jarFileListScrollPane = new javax.swing.JScrollPane();
        jarFileList = new javax.swing.JList();
        sourceDirListScrollPane = new javax.swing.JScrollPane();
        sourceDirList = new javax.swing.JList();
        classpathEntryLabel = new javax.swing.JLabel();
        classpathEntryListLabel = new javax.swing.JLabel();
        classpathEntryTextField = new javax.swing.JTextField();
        browseClasspathEntryButton = new javax.swing.JButton();
        addClasspathEntryButton = new javax.swing.JButton();
        removeClasspathEntryButton = new javax.swing.JButton();
        classpathEntryListScrollPane = new javax.swing.JScrollPane();
        classpathEntryList = new javax.swing.JList();
        jSeparator5 = new javax.swing.JSeparator();
        sourceUpButton = new javax.swing.JButton();
        sourceDownButton = new javax.swing.JButton();
        classpathUpButton = new javax.swing.JButton();
        classpathDownButton = new javax.swing.JButton();
        bugTreePanel = new javax.swing.JPanel();
        bugTreeBugDetailsSplitter = new javax.swing.JSplitPane();
        groupByTabbedPane = new javax.swing.JTabbedPane();
        byClassScrollPane = new javax.swing.JScrollPane();
        byClassBugTree = new javax.swing.JTree();
        byPackageScrollPane = new javax.swing.JScrollPane();
        byPackageBugTree = new javax.swing.JTree();
        byBugTypeScrollPane = new javax.swing.JScrollPane();
        byBugTypeBugTree = new javax.swing.JTree();
        bySummary = new javax.swing.JScrollPane();
        bugSummaryEditorPane = new javax.swing.JEditorPane();
        bugDetailsTabbedPane = new javax.swing.JTabbedPane();
        bugDescriptionScrollPane = new javax.swing.JScrollPane();
        bugDescriptionEditorPane = new javax.swing.JEditorPane();
        sourceTextAreaScrollPane = new javax.swing.JScrollPane();
        sourceTextArea = new javax.swing.JTextArea();
        annotationTextAreaScrollPane = new javax.swing.JScrollPane();
        annotationTextArea = new javax.swing.JTextArea();
        consoleScrollPane = new javax.swing.JScrollPane();
        consoleMessageArea = new javax.swing.JTextArea();
        urlLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        logoLabel = new javax.swing.JLabel();
        growBoxSpacer = new javax.swing.JLabel();
        theMenuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newProjectItem = new javax.swing.JMenuItem();
        openProjectItem = new javax.swing.JMenuItem();
        recentProjectsMenu = new javax.swing.JMenu();
        jSeparator9 = new javax.swing.JSeparator();
        closeProjectItem = new javax.swing.JMenuItem();
        saveProjectItem = new javax.swing.JMenuItem();
        saveProjectAsItem = new javax.swing.JMenuItem();
        reloadProjectItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        loadBugsItem = new javax.swing.JMenuItem();
        saveBugsItem = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        exitItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        cutItem = new javax.swing.JMenuItem();
        copyItem = new javax.swing.JMenuItem();
        pasteItem = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JSeparator();
        selectAllItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        viewConsoleItem = new javax.swing.JCheckBoxMenuItem();
        viewBugDetailsItem = new javax.swing.JCheckBoxMenuItem();
        fullDescriptionsItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator7 = new javax.swing.JSeparator();
        lowPriorityButton = new javax.swing.JRadioButtonMenuItem();
        mediumPriorityButton = new javax.swing.JRadioButtonMenuItem();
        highPriorityButton = new javax.swing.JRadioButtonMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        viewProjectItem = new javax.swing.JRadioButtonMenuItem();
        viewBugsItem = new javax.swing.JRadioButtonMenuItem();
        settingsMenu = new javax.swing.JMenu();
        configureDetectorsItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutItem = new javax.swing.JMenuItem();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        consoleSplitter.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        consoleSplitter.setResizeWeight(1.0);
        consoleSplitter.setOneTouchExpandable(true);
        consoleSplitter.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                consoleSplitterPropertyChange(evt);
            }
        });

        viewPanel.setLayout(new java.awt.CardLayout());

        viewPanel.add(emptyPanel, "EmptyPanel");

        viewPanel.add(reportPanel, "ReportPanel");

        editProjectPanel.setLayout(new java.awt.GridBagLayout());

        jarFileLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        jarFileLabel.setText("Archive or directory:");
        jarFileLabel.setText(L10N.getLocalString("dlg.jarfile_lbl", "Archive or Directory:"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(jarFileLabel, gridBagConstraints);

        jarNameTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jarNameTextFieldActionPerformed(evt);
            }
        });
        jarNameTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(jarNameTextField, gridBagConstraints);

        addJarButton.setFont(new java.awt.Font("Dialog", 0, 12));
        addJarButton.setText("Add");
        addJarButton.setMaximumSize(new java.awt.Dimension(90, 25));
        addJarButton.setMinimumSize(new java.awt.Dimension(90, 25));
        addJarButton.setPreferredSize(new java.awt.Dimension(90, 25));
        addJarButton.setText(L10N.getLocalString("dlg.add_btn", "Add"));
        addJarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addJarButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(addJarButton, gridBagConstraints);

        jarFileListLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        jarFileListLabel.setText("Archives/directories:");
        jarFileListLabel.setText(L10N.getLocalString("dlg.jarlist_lbl", "Archives/Directories:"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(jarFileListLabel, gridBagConstraints);

        sourceDirLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        sourceDirLabel.setText("Source directory:");
        sourceDirLabel.setText(L10N.getLocalString("dlg.srcfile_lbl", "Source directory:"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(sourceDirLabel, gridBagConstraints);

        srcDirTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                srcDirTextFieldActionPerformed(evt);
            }
        });
        srcDirTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(srcDirTextField, gridBagConstraints);

        addSourceDirButton.setFont(new java.awt.Font("Dialog", 0, 12));
        addSourceDirButton.setText("Add");
        addSourceDirButton.setMaximumSize(new java.awt.Dimension(90, 25));
        addSourceDirButton.setMinimumSize(new java.awt.Dimension(90, 25));
        addSourceDirButton.setPreferredSize(new java.awt.Dimension(90, 25));
        addSourceDirButton.setText(L10N.getLocalString("dlg.add_btn", "Add"));
        addSourceDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSourceDirButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(addSourceDirButton, gridBagConstraints);

        sourceDirListLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        sourceDirListLabel.setText("Source directories:");
        sourceDirListLabel.setText(L10N.getLocalString("dlg.srclist_lbl", "Source directories:"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(sourceDirListLabel, gridBagConstraints);

        removeJarButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeJarButton.setText("Remove");
        removeJarButton.setMaximumSize(new java.awt.Dimension(90, 25));
        removeJarButton.setMinimumSize(new java.awt.Dimension(90, 25));
        removeJarButton.setPreferredSize(new java.awt.Dimension(90, 25));
        removeJarButton.setText(L10N.getLocalString("dlg.remove_btn", "Remove"));
        removeJarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeJarButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(removeJarButton, gridBagConstraints);

        removeSrcDirButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeSrcDirButton.setText("Remove");
        removeSrcDirButton.setMaximumSize(new java.awt.Dimension(90, 25));
        removeSrcDirButton.setMinimumSize(new java.awt.Dimension(90, 25));
        removeSrcDirButton.setPreferredSize(new java.awt.Dimension(90, 25));
        removeSrcDirButton.setText(L10N.getLocalString("dlg.remove_btn", "Remove"));
        removeSrcDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSrcDirButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(removeSrcDirButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        editProjectPanel.add(jSeparator1, gridBagConstraints);

        browseJarButton.setFont(new java.awt.Font("Dialog", 0, 12));
        browseJarButton.setText("Browse");
        browseJarButton.setMaximumSize(new java.awt.Dimension(90, 25));
        browseJarButton.setMinimumSize(new java.awt.Dimension(90, 25));
        browseJarButton.setPreferredSize(new java.awt.Dimension(90, 25));
        browseJarButton.setText(L10N.getLocalString("dlg.browse_btn", "Browse"));
        browseJarButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseJarButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(browseJarButton, gridBagConstraints);

        browseSrcDirButton.setFont(new java.awt.Font("Dialog", 0, 12));
        browseSrcDirButton.setText("Browse");
        browseSrcDirButton.setMaximumSize(new java.awt.Dimension(90, 25));
        browseSrcDirButton.setMinimumSize(new java.awt.Dimension(90, 25));
        browseSrcDirButton.setPreferredSize(new java.awt.Dimension(90, 25));
        browseSrcDirButton.setText(L10N.getLocalString("dlg.browse_btn", "Browse"));
        browseSrcDirButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseSrcDirButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(browseSrcDirButton, gridBagConstraints);

        editProjectLabel.setBackground(new java.awt.Color(0, 0, 204));
        editProjectLabel.setFont(new java.awt.Font("Dialog", 1, 24));
        editProjectLabel.setForeground(new java.awt.Color(255, 255, 255));
        editProjectLabel.setText("Project");
        editProjectLabel.setOpaque(true);
        editProjectLabel.setText(L10N.getLocalString("dlg.project_lbl", "Project"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        editProjectPanel.add(editProjectLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        editProjectPanel.add(jSeparator2, gridBagConstraints);

        findBugsButton.setMnemonic('B');
        findBugsButton.setText("Find Bugs!");
        findBugsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                findBugsButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 21;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        editProjectPanel.add(findBugsButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 3, 3, 3);
        editProjectPanel.add(jSeparator4, gridBagConstraints);

        jarFileListScrollPane.setPreferredSize(new java.awt.Dimension(259, 1));
        jarFileList.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        jarFileList.setFont(new java.awt.Font("Dialog", 0, 12));
        disableEditKeyBindings(jarFileList);

        jarFileList.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        jarFileListScrollPane.setViewportView(jarFileList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.4;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(jarFileListScrollPane, gridBagConstraints);

        sourceDirListScrollPane.setPreferredSize(new java.awt.Dimension(259, 1));
        sourceDirList.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        sourceDirList.setFont(new java.awt.Font("Dialog", 0, 12));
        disableEditKeyBindings(sourceDirList);
        sourceDirList.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        sourceDirListScrollPane.setViewportView(sourceDirList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(sourceDirListScrollPane, gridBagConstraints);

        classpathEntryLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        classpathEntryLabel.setText("Classpath entry:");
        classpathEntryLabel.setText(L10N.getLocalString("dlg.classpathfile_lbl", "Classpath entry:"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 3);
        editProjectPanel.add(classpathEntryLabel, gridBagConstraints);

        classpathEntryListLabel.setFont(new java.awt.Font("Dialog", 0, 12));
        classpathEntryListLabel.setText("Classpath entries:");
        classpathEntryListLabel.setText(L10N.getLocalString("dlg.classpathlist_lbl", "Classpath entries:"));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 3);
        editProjectPanel.add(classpathEntryListLabel, gridBagConstraints);

        classpathEntryTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(classpathEntryTextField, gridBagConstraints);

        browseClasspathEntryButton.setFont(new java.awt.Font("Dialog", 0, 12));
        browseClasspathEntryButton.setText("Browse");
        browseClasspathEntryButton.setMaximumSize(new java.awt.Dimension(90, 25));
        browseClasspathEntryButton.setMinimumSize(new java.awt.Dimension(90, 25));
        browseClasspathEntryButton.setPreferredSize(new java.awt.Dimension(90, 25));
        browseClasspathEntryButton.setText(L10N.getLocalString("dlg.browse_btn", "Browse"));
        browseClasspathEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseClasspathEntryButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 15;
        editProjectPanel.add(browseClasspathEntryButton, gridBagConstraints);

        addClasspathEntryButton.setFont(new java.awt.Font("Dialog", 0, 12));
        addClasspathEntryButton.setText("Add");
        addClasspathEntryButton.setMaximumSize(new java.awt.Dimension(90, 25));
        addClasspathEntryButton.setMinimumSize(new java.awt.Dimension(90, 25));
        addClasspathEntryButton.setPreferredSize(new java.awt.Dimension(90, 25));
        addClasspathEntryButton.setText(L10N.getLocalString("dlg.add_btn", "Add"));
        addClasspathEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addClasspathEntryButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(addClasspathEntryButton, gridBagConstraints);

        removeClasspathEntryButton.setFont(new java.awt.Font("Dialog", 0, 12));
        removeClasspathEntryButton.setText("Remove");
        removeClasspathEntryButton.setMaximumSize(new java.awt.Dimension(90, 25));
        removeClasspathEntryButton.setMinimumSize(new java.awt.Dimension(90, 25));
        removeClasspathEntryButton.setPreferredSize(new java.awt.Dimension(90, 25));
        removeClasspathEntryButton.setText(L10N.getLocalString("dlg.remove_btn", "Remove"));
        removeClasspathEntryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeClasspathEntryButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 0);
        editProjectPanel.add(removeClasspathEntryButton, gridBagConstraints);

        classpathEntryListScrollPane.setPreferredSize(new java.awt.Dimension(259, 1));
        classpathEntryList.setBorder(new javax.swing.border.BevelBorder(javax.swing.border.BevelBorder.LOWERED));
        classpathEntryList.setFont(new java.awt.Font("Dialog", 0, 12));
        disableEditKeyBindings(classpathEntryList);
        classpathEntryList.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        classpathEntryListScrollPane.setViewportView(classpathEntryList);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 3, 0, 3);
        editProjectPanel.add(classpathEntryListScrollPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(3, 0, 3, 0);
        editProjectPanel.add(jSeparator5, gridBagConstraints);

        sourceUpButton.setFont(new java.awt.Font("Dialog", 0, 12));
        sourceUpButton.setText("Up");
        sourceUpButton.setMaximumSize(new java.awt.Dimension(90, 25));
        sourceUpButton.setMinimumSize(new java.awt.Dimension(90, 25));
        sourceUpButton.setPreferredSize(new java.awt.Dimension(90, 25));
        sourceUpButton.setText(L10N.getLocalString("dlg.up_btn", "Up"));
        sourceUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sourceUpButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.weighty = 0.2;
        editProjectPanel.add(sourceUpButton, gridBagConstraints);

        sourceDownButton.setFont(new java.awt.Font("Dialog", 0, 12));
        sourceDownButton.setText("Down");
        sourceDownButton.setMaximumSize(new java.awt.Dimension(90, 25));
        sourceDownButton.setMinimumSize(new java.awt.Dimension(90, 25));
        sourceDownButton.setPreferredSize(new java.awt.Dimension(90, 25));
        sourceDownButton.setText(L10N.getLocalString("dlg.down_btn", "Down"));
        sourceDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sourceDownButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 11;
        editProjectPanel.add(sourceDownButton, gridBagConstraints);

        classpathUpButton.setFont(new java.awt.Font("Dialog", 0, 12));
        classpathUpButton.setText("Up");
        classpathUpButton.setMaximumSize(new java.awt.Dimension(90, 25));
        classpathUpButton.setMinimumSize(new java.awt.Dimension(90, 25));
        classpathUpButton.setPreferredSize(new java.awt.Dimension(90, 25));
        classpathUpButton.setText(L10N.getLocalString("dlg.up_btn", "Up"));
        classpathUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classpathUpButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 17;
        gridBagConstraints.weighty = 0.2;
        editProjectPanel.add(classpathUpButton, gridBagConstraints);

        classpathDownButton.setFont(new java.awt.Font("Dialog", 0, 12));
        classpathDownButton.setText("Down");
        classpathDownButton.setMaximumSize(new java.awt.Dimension(90, 25));
        classpathDownButton.setMinimumSize(new java.awt.Dimension(90, 25));
        classpathDownButton.setPreferredSize(new java.awt.Dimension(90, 25));
        classpathDownButton.setText(L10N.getLocalString("dlg.down_btn", "Down"));
        classpathDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                classpathDownButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 18;
        editProjectPanel.add(classpathDownButton, gridBagConstraints);

        viewPanel.add(editProjectPanel, "EditProjectPanel");

        bugTreePanel.setLayout(new java.awt.GridBagLayout());

        bugTreeBugDetailsSplitter.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        bugTreeBugDetailsSplitter.setResizeWeight(1.0);
        bugTreeBugDetailsSplitter.setOneTouchExpandable(true);
        bugTreeBugDetailsSplitter.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                bugTreeBugDetailsSplitterPropertyChange(evt);
            }
        });

        byClassBugTree.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        byClassScrollPane.setViewportView(byClassBugTree);

        groupByTabbedPane.addTab("By Class", byClassScrollPane);

        byPackageBugTree.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        byPackageScrollPane.setViewportView(byPackageBugTree);

        groupByTabbedPane.addTab("By Package", byPackageScrollPane);

        byBugTypeBugTree.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        byBugTypeScrollPane.setViewportView(byBugTypeBugTree);

        groupByTabbedPane.addTab("By Bug Type", byBugTypeScrollPane);

        bugSummaryEditorPane.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        bySummary.setViewportView(bugSummaryEditorPane);

        groupByTabbedPane.addTab("Summary", bySummary);

        bugTreeBugDetailsSplitter.setTopComponent(groupByTabbedPane);

        bugDescriptionEditorPane.setEditable(false);
        bugDescriptionEditorPane.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        bugDescriptionScrollPane.setViewportView(bugDescriptionEditorPane);

        bugDetailsTabbedPane.addTab("Details", bugDescriptionScrollPane);

        sourceTextAreaScrollPane.setMinimumSize(new java.awt.Dimension(22, 180));
        sourceTextAreaScrollPane.setPreferredSize(new java.awt.Dimension(0, 100));
        sourceTextArea.setEditable(false);
        sourceTextArea.setFont(new java.awt.Font("Lucida Sans Typewriter", 0, 12));
        sourceTextArea.setEnabled(false);
        sourceTextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        sourceTextAreaScrollPane.setViewportView(sourceTextArea);

        bugDetailsTabbedPane.addTab("Source code", sourceTextAreaScrollPane);

        annotationTextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                focusGainedHandler(evt);
            }
        });

        annotationTextAreaScrollPane.setViewportView(annotationTextArea);

        bugDetailsTabbedPane.addTab("Annotations", annotationTextAreaScrollPane);

        bugTreeBugDetailsSplitter.setBottomComponent(bugDetailsTabbedPane);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        bugTreePanel.add(bugTreeBugDetailsSplitter, gridBagConstraints);

        viewPanel.add(bugTreePanel, "BugTree");

        consoleSplitter.setTopComponent(viewPanel);

        {
            equalizeControls( new JComponent[]
                {
                    addJarButton,
                    addSourceDirButton,
                    addClasspathEntryButton,
                    removeJarButton,
                    removeSrcDirButton,
                    removeClasspathEntryButton,
                    browseJarButton,
                    browseSrcDirButton,
                    browseClasspathEntryButton,
                    sourceUpButton,
                    sourceDownButton,
                    classpathUpButton,
                    classpathDownButton
                });

                groupByTabbedPane.setTitleAt(0, L10N.getLocalString( "dlg.byclass_tab", "By Class"));
                groupByTabbedPane.setTitleAt(1, L10N.getLocalString( "dlg.bypackage_tab", "By Package"));
                groupByTabbedPane.setTitleAt(2, L10N.getLocalString( "dlg.bybugtype_tab", "By Bug Type"));
                groupByTabbedPane.setTitleAt(3, L10N.getLocalString( "dlg.summary_tab", "Summary"));
                bugDetailsTabbedPane.setTitleAt(0, L10N.getLocalString( "dlg.details_tab", "Details"));
                bugDetailsTabbedPane.setTitleAt(1, L10N.getLocalString( "dlg.sourcecode_tab", "Source Code"));
                bugDetailsTabbedPane.setTitleAt(2, L10N.getLocalString( "dlg.annotations_tab", "Annotations"));
            }
            consoleScrollPane.setMinimumSize(new java.awt.Dimension(22, 100));
            consoleScrollPane.setPreferredSize(new java.awt.Dimension(0, 100));
            consoleMessageArea.setBackground(new java.awt.Color(204, 204, 204));
            consoleMessageArea.setEditable(false);
            consoleMessageArea.setFont(new java.awt.Font("Lucida Sans Typewriter", 0, 12));
            consoleMessageArea.setMinimumSize(new java.awt.Dimension(0, 0));
            consoleMessageArea.setAutoscrolls(false);
            consoleMessageArea.addFocusListener(new java.awt.event.FocusAdapter() {
                public void focusGained(java.awt.event.FocusEvent evt) {
                    focusGainedHandler(evt);
                }
            });

            consoleScrollPane.setViewportView(consoleMessageArea);

            consoleSplitter.setBottomComponent(consoleScrollPane);

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            getContentPane().add(consoleSplitter, gridBagConstraints);

            urlLabel.setFont(new java.awt.Font("Dialog", 0, 12));
            urlLabel.setText("FindBugs - http://findbugs.sourceforge.net/");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
            gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 0);
            getContentPane().add(urlLabel, gridBagConstraints);

            jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.X_AXIS));

            jPanel1.add(logoLabel);

            growBoxSpacer.setMaximumSize(new java.awt.Dimension(16, 16));
            jPanel1.add(growBoxSpacer);

            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = 1;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHEAST;
            gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 2);
            getContentPane().add(jPanel1, gridBagConstraints);

            theMenuBar.setFont(new java.awt.Font("Dialog", 0, 12));
            fileMenu.setText("File");
            fileMenu.setFont(new java.awt.Font("Dialog", 0, 12));
            localiseButton(fileMenu, "menu.file_menu", "&File", true);
            fileMenu.addMenuListener(new javax.swing.event.MenuListener() {
                public void menuCanceled(javax.swing.event.MenuEvent evt) {
                }
                public void menuDeselected(javax.swing.event.MenuEvent evt) {
                }
                public void menuSelected(javax.swing.event.MenuEvent evt) {
                    fileMenuMenuSelected(evt);
                }
            });

            newProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
            newProjectItem.setText("New Project");
            localiseButton(newProjectItem, "menu.new_item", "&New Project", true);
            newProjectItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    newProjectItemActionPerformed(evt);
                }
            });

            fileMenu.add(newProjectItem);

            openProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
            openProjectItem.setText("Open Project...");
            localiseButton(openProjectItem, "menu.open_item", "&Open Project", true);
            openProjectItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    openProjectItemActionPerformed(evt);
                }
            });

            fileMenu.add(openProjectItem);

            recentProjectsMenu.setText("Recent Projects");
            recentProjectsMenu.setFont(new java.awt.Font("Dialog", 0, 12));
            localiseButton(recentProjectsMenu, "menu.recent_menu", "R&ecent Projects", true);
            rebuildRecentProjectsMenu();
            fileMenu.add(recentProjectsMenu);

            fileMenu.add(jSeparator9);

            closeProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
            closeProjectItem.setText("Close Project");
            localiseButton(closeProjectItem, "menu.close_item", "&Close", true);
            closeProjectItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    closeProjectItemActionPerformed(evt);
                }
            });

            fileMenu.add(closeProjectItem);

            saveProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
            saveProjectItem.setText("Save Project");
            localiseButton(saveProjectItem, "menu.save_item", "&Save Project", true);
            saveProjectItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveProjectItemActionPerformed(evt);
                }
            });

            fileMenu.add(saveProjectItem);

            saveProjectAsItem.setFont(new java.awt.Font("Dialog", 0, 12));
            saveProjectAsItem.setText("Save Project As...");
            localiseButton(saveProjectAsItem, "menu.saveas_item", "Save Project &As...", true);
            saveProjectAsItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveProjectAsItemActionPerformed(evt);
                }
            });

            fileMenu.add(saveProjectAsItem);

            reloadProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
            reloadProjectItem.setText("Reload Project");
            localiseButton(reloadProjectItem, "menu.reload_item", "&Reload Project", true);
            reloadProjectItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    reloadProjectItemActionPerformed(evt);
                }
            });

            fileMenu.add(reloadProjectItem);

            fileMenu.add(jSeparator3);

            loadBugsItem.setFont(new java.awt.Font("Dialog", 0, 12));
            loadBugsItem.setText("Load Bugs...");
            localiseButton(loadBugsItem, "menu.loadbugs_item", "&Load Bugs...", true);
            loadBugsItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    loadBugsItemActionPerformed(evt);
                }
            });

            fileMenu.add(loadBugsItem);

            saveBugsItem.setFont(new java.awt.Font("Dialog", 0, 12));
            saveBugsItem.setText("Save Bugs");
            localiseButton(saveBugsItem, "menu.savebugs_item", "Save &Bugs...", true);
            saveBugsItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    saveBugsItemActionPerformed(evt);
                }
            });

            fileMenu.add(saveBugsItem);

            fileMenu.add(jSeparator6);

            exitItem.setFont(new java.awt.Font("Dialog", 0, 12));
            exitItem.setText("Exit");
            localiseButton(exitItem, "menu.exit_item", "E&xit", true);
            exitItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    exitItemActionPerformed(evt);
                }
            });

            fileMenu.add(exitItem);

            theMenuBar.add(fileMenu);

            editMenu.setText("Edit");
            editMenu.setFont(new java.awt.Font("Dialog", 0, 12));
            editMenu.setEnabled(false);
            localiseButton(editMenu, "menu.edit_menu", "&Edit", true);
            cutItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
            cutItem.setFont(new java.awt.Font("Dialog", 0, 12));
            cutItem.setText("Cut");
            localiseButton(cutItem, "menu.cut_item", "Cut", true);
            cutItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    cutActionPerformed(evt);
                }
            });

            editMenu.add(cutItem);

            copyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
            copyItem.setFont(new java.awt.Font("Dialog", 0, 12));
            copyItem.setText("Copy");
            localiseButton(copyItem, "menu.copy_item", "Copy", true);
            copyItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    copyActionPerformed(evt);
                }
            });

            editMenu.add(copyItem);

            pasteItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
            pasteItem.setFont(new java.awt.Font("Dialog", 0, 12));
            pasteItem.setText("Paste");
            localiseButton(pasteItem, "menu.paste_item", "Paste", true);
            pasteItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    pasteActionPerformed(evt);
                }
            });

            editMenu.add(pasteItem);

            editMenu.add(jSeparator10);

            selectAllItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
            selectAllItem.setFont(new java.awt.Font("Dialog", 0, 12));
            selectAllItem.setText("Select All");
            localiseButton(selectAllItem, "menu.selectall_item", "Select &All", true);
            selectAllItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    selectAllActionPerformed(evt);
                }
            });

            editMenu.add(selectAllItem);

            theMenuBar.add(editMenu);

            viewMenu.setText("View");
            viewMenu.setFont(new java.awt.Font("Dialog", 0, 12));
            localiseButton(viewMenu, "menu.view_menu", "&View", true);
            viewMenu.addMenuListener(new javax.swing.event.MenuListener() {
                public void menuCanceled(javax.swing.event.MenuEvent evt) {
                }
                public void menuDeselected(javax.swing.event.MenuEvent evt) {
                }
                public void menuSelected(javax.swing.event.MenuEvent evt) {
                    viewMenuMenuSelected(evt);
                }
            });

            viewConsoleItem.setFont(new java.awt.Font("Dialog", 0, 12));
            viewConsoleItem.setText("Console");
            localiseButton(viewConsoleItem, "menu.console_item", "&Console", true);
            viewConsoleItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    viewConsoleItemActionPerformed(evt);
                }
            });

            viewMenu.add(viewConsoleItem);

            viewBugDetailsItem.setFont(new java.awt.Font("Dialog", 0, 12));
            viewBugDetailsItem.setSelected(true);
            viewBugDetailsItem.setText("Bug Details");
            localiseButton(viewBugDetailsItem, "menu.bugdetails_item", "Bug &Details", true);
            viewBugDetailsItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    viewBugDetailsItemActionPerformed(evt);
                }
            });

            viewMenu.add(viewBugDetailsItem);

            fullDescriptionsItem.setFont(new java.awt.Font("Dialog", 0, 12));
            fullDescriptionsItem.setSelected(true);
            fullDescriptionsItem.setText("Full Descriptions");
            localiseButton(fullDescriptionsItem, "menu.fulldescriptions_item", "&Full Descriptions", true);
            fullDescriptionsItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    fullDescriptionsItemActionPerformed(evt);
                }
            });

            viewMenu.add(fullDescriptionsItem);

            viewMenu.add(jSeparator7);

            lowPriorityButton.setFont(new java.awt.Font("Dialog", 0, 12));
            lowPriorityButton.setText("Low Priority");
            localiseButton(lowPriorityButton, "menu.lowpriority_item", "&Low Priority", true);
            lowPriorityButton.setSelected(priorityThreshold == Detector.LOW_PRIORITY);
            lowPriorityButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    lowPriorityButtonActionPerformed(evt);
                }
            });

            viewMenu.add(lowPriorityButton);

            mediumPriorityButton.setFont(new java.awt.Font("Dialog", 0, 12));
            mediumPriorityButton.setText("Medium Priority");
            localiseButton(mediumPriorityButton, "menu.mediumpriority_item", "&Medium Priority", true);
            mediumPriorityButton.setSelected(priorityThreshold == Detector.NORMAL_PRIORITY);
            mediumPriorityButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    mediumPriorityButtonActionPerformed(evt);
                }
            });

            viewMenu.add(mediumPriorityButton);

            highPriorityButton.setFont(new java.awt.Font("Dialog", 0, 12));
            highPriorityButton.setText("High Priority");
            localiseButton(highPriorityButton, "menu.highpriority_item", "&High Priority", true);
            highPriorityButton.setSelected(priorityThreshold == Detector.HIGH_PRIORITY);
            highPriorityButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    highPriorityButtonActionPerformed(evt);
                }
            });

            viewMenu.add(highPriorityButton);

            viewMenu.add(jSeparator8);

            viewProjectItem.setFont(new java.awt.Font("Dialog", 0, 12));
            viewProjectItem.setText("View Project Details");
            viewProjectItem.setEnabled(false);
            localiseButton(viewProjectItem, "menu.viewprojectdetails_item", "View Project Details", true);
            viewProjectItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    viewProjectItemActionPerformed(evt);
                }
            });

            viewMenu.add(viewProjectItem);

            viewBugsItem.setFont(new java.awt.Font("Dialog", 0, 12));
            viewBugsItem.setText("View Bugs");
            viewBugsItem.setEnabled(false);
            localiseButton(viewBugsItem, "menu.viewbugs_item", "View Bugs", true);
            viewBugsItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    viewBugsItemActionPerformed(evt);
                }
            });

            viewMenu.add(viewBugsItem);

            theMenuBar.add(viewMenu);

            settingsMenu.setText("Settings");
            settingsMenu.setFont(new java.awt.Font("Dialog", 0, 12));
            localiseButton(settingsMenu, "menu.settings_menu", "&Settings", true);
            configureDetectorsItem.setFont(new java.awt.Font("Dialog", 0, 12));
            configureDetectorsItem.setText("Configure Detectors...");
            localiseButton(configureDetectorsItem, "menu.configure_item", "&Configure detectors", true);
            configureDetectorsItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    configureDetectorsItemActionPerformed(evt);
                }
            });

            settingsMenu.add(configureDetectorsItem);

            theMenuBar.add(settingsMenu);

            helpMenu.setText("Help");
            helpMenu.setFont(new java.awt.Font("Dialog", 0, 12));
            localiseButton(helpMenu, "menu.help_menu", "&Help", true);
            aboutItem.setFont(new java.awt.Font("Dialog", 0, 12));
            aboutItem.setText("About...");
            localiseButton(aboutItem, "menu.about_item", "&About", true);
            aboutItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    aboutItemActionPerformed(evt);
                }
            });

            helpMenu.add(aboutItem);

            theMenuBar.add(helpMenu);

            setJMenuBar(theMenuBar);

            pack();
        }//GEN-END:initComponents

    private void selectAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectAllActionPerformed
        if (selectedComponent == null)
            return;
        
        if (selectedComponent instanceof JTextComponent)
            ((JTextComponent)selectedComponent).selectAll();
        else if (selectedComponent instanceof JList) {
            JList list = (JList)selectedComponent;
            list.setSelectionInterval(0, list.getModel().getSize());
        }
    }//GEN-LAST:event_selectAllActionPerformed

    private void disableEditKeyBindings(JList list) {
        list.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK), "none");
        list.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK), "none");
        list.getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK), "none");
    }
    
    private String buildSelectPath(JList list) {
        StringBuffer path = new StringBuffer();
        int[] indices = list.getSelectedIndices();
        String separatorStr = System.getProperty("path.separator");
        String sep = "";
        ListModel m = list.getModel();
        for (int i = 0; i < indices.length; i++) {
            path.append(sep);
            sep = separatorStr;
            path.append(m.getElementAt(indices[i]));
        }
        return path.toString();
    }
    
    private void pasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteActionPerformed
        if (selectedComponent == null)
            return;
        
        if (selectedComponent instanceof JTextComponent)
            ((JTextComponent)selectedComponent).paste();
        else if (selectedComponent instanceof JList) {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable transfer = cb.getContents(this);
            if (transfer.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String path = (String)transfer.getTransferData(DataFlavor.stringFlavor);

                    if (selectedComponent == jarFileList) {
                        jarNameTextField.setText(path);
                        addJarButtonActionPerformed(evt);
                    }
                    else if (selectedComponent == sourceDirList) {
                        srcDirTextField.setText(path);
                        this.addSourceDirButtonActionPerformed(evt);
                    }
                    else if (selectedComponent == classpathEntryList) {
                        classpathEntryTextField.setText(path);
                        addClasspathEntryButtonActionPerformed(evt);
                    }
                } catch (Exception e) {
                }
            }
        }
    }//GEN-LAST:event_pasteActionPerformed

    private void copyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyActionPerformed
        if (selectedComponent == null)
            return;
        
        if (selectedComponent instanceof JTextComponent)
            ((JTextComponent)selectedComponent).copy();
        else if (selectedComponent instanceof JTree) {
            TreePath path = ((JTree)selectedComponent).getSelectionPath();
            StringSelection data = new StringSelection(path.getLastPathComponent().toString());
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(data, data);
       }
        else if (selectedComponent instanceof JList) {
            StringSelection path = new StringSelection(buildSelectPath((JList)selectedComponent));
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(path, path);
        }        
    }//GEN-LAST:event_copyActionPerformed

    private void cutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cutActionPerformed
        if (selectedComponent == null)
            return;
        
        if (selectedComponent instanceof JTextComponent)
            ((JTextComponent)selectedComponent).cut();
        else if (selectedComponent instanceof JList) {
            StringSelection path = new StringSelection(buildSelectPath((JList)selectedComponent));
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(path, path);
            if (selectedComponent == jarFileList)
                removeJarButtonActionPerformed(evt);
            else if (selectedComponent == sourceDirList)
                removeSrcDirButtonActionPerformed(evt);
            else if (selectedComponent == classpathEntryList)
                removeClasspathEntryButtonActionPerformed(evt);
        }
    }//GEN-LAST:event_cutActionPerformed

    private void focusGainedHandler(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_focusGainedHandler
        Component old = evt.getOppositeComponent();
        if (old instanceof JList)
            ((JList) old).clearSelection();
        selectedComponent = evt.getComponent();
        ableEditMenu();
    }//GEN-LAST:event_focusGainedHandler

	private void classpathUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classpathUpButtonActionPerformed
		if (moveEntriesUp(classpathEntryList))
			resyncAuxClasspathEntries();
	}//GEN-LAST:event_classpathUpButtonActionPerformed

	private void sourceDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sourceDownButtonActionPerformed
		if (moveEntriesDown(sourceDirList))
			resyncSourceEntries();
	}//GEN-LAST:event_sourceDownButtonActionPerformed

	private void sourceUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sourceUpButtonActionPerformed
		if (moveEntriesUp(sourceDirList))
			resyncSourceEntries();
	}//GEN-LAST:event_sourceUpButtonActionPerformed

	private void classpathDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_classpathDownButtonActionPerformed
		if (moveEntriesDown(classpathEntryList))
			resyncAuxClasspathEntries();
	}//GEN-LAST:event_classpathDownButtonActionPerformed

	private void viewBugsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewBugsItemActionPerformed
		setView("BugTree");
	}//GEN-LAST:event_viewBugsItemActionPerformed

	private void viewProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewProjectItemActionPerformed

		setView("EditProjectPanel");
	}//GEN-LAST:event_viewProjectItemActionPerformed

	private void highPriorityButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_highPriorityButtonActionPerformed
		mediumPriorityButton.setSelected(false);
		lowPriorityButton.setSelected(false);
		setPriorityThreshold(Detector.HIGH_PRIORITY);
	}//GEN-LAST:event_highPriorityButtonActionPerformed

	private void mediumPriorityButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mediumPriorityButtonActionPerformed
		highPriorityButton.setSelected(false);
		lowPriorityButton.setSelected(false);
		setPriorityThreshold(Detector.NORMAL_PRIORITY);
	}//GEN-LAST:event_mediumPriorityButtonActionPerformed

	private void lowPriorityButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lowPriorityButtonActionPerformed
		highPriorityButton.setSelected(false);
		mediumPriorityButton.setSelected(false);
		setPriorityThreshold(Detector.LOW_PRIORITY);
	}//GEN-LAST:event_lowPriorityButtonActionPerformed

	private void saveBugsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveBugsItemActionPerformed

		try {
			if (currentAnalysisRun == null) {
				logger.logMessage(ConsoleLogger.ERROR, "No bugs are loaded!");
				return;
			}

			JFileChooser chooser = createFileChooser();
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setFileFilter(xmlFileFilter);

			int result = chooseFile(chooser, L10N.getLocalString("dlg.savebugs_ttl", "Save Bugs"));	

			if (result != JFileChooser.CANCEL_OPTION) {
				// Make sure current annotation text is up to date with its
				// corresponding bug instance
				if (currentBugInstance != null)
					synchBugAnnotation(currentBugInstance);

				// Save bugs to file
				File selectedFile = chooser.getSelectedFile();
				currentAnalysisRun.saveBugsToFile(selectedFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.logMessage(ConsoleLogger.ERROR, "Could not save bugs: " + e.toString());
		}
	}//GEN-LAST:event_saveBugsItemActionPerformed

	private void loadBugsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadBugsItemActionPerformed
		// FIXME: offer to save current project and bugs

		try {

			JFileChooser chooser = createFileChooser();
			chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			chooser.setFileFilter(xmlFileFilter);

			int result = chooseFile(chooser, L10N.getLocalString("dlg.loadbugs_ttl", "Load Bugs..."));

			if (result != JFileChooser.CANCEL_OPTION) {
				File selectedFile = chooser.getSelectedFile();

				Project project = new Project();
				AnalysisRun analysisRun = new AnalysisRun(project, this);

				analysisRun.loadBugsFromFile(selectedFile);

				setProject(project);
				synchAnalysisRun(analysisRun);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.logMessage(ConsoleLogger.ERROR, "Could not load bugs: " + e.toString());
		}

	}//GEN-LAST:event_loadBugsItemActionPerformed

	private void configureDetectorsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configureDetectorsItemActionPerformed
		ConfigureDetectorsDialog dialog = new ConfigureDetectorsDialog(this, true);
		dialog.setSize(700, 520);
		dialog.setLocationRelativeTo(null); // center the dialog
		dialog.setVisible(true);
	}//GEN-LAST:event_configureDetectorsItemActionPerformed

	private void reloadProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reloadProjectItemActionPerformed
		Project current = getCurrentProject();

		if (current == null)
			return;

		try {
			String filename = current.getFileName();
			Project project = new Project();
			project.read(filename);
			setProject(null);
			setProject(project);
			findBugsButtonActionPerformed(evt);
		} catch (IOException e) {
			logger.logMessage(ConsoleLogger.ERROR, "Could not reload project: " + e.getMessage());
		}

	}//GEN-LAST:event_reloadProjectItemActionPerformed

	private void saveProjectAsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectAsItemActionPerformed
		saveProject(getCurrentProject(), L10N.getLocalString("dlg.saveprojectas_ttl", "Save Project As..."), true);
	}//GEN-LAST:event_saveProjectAsItemActionPerformed

	private void viewMenuMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_viewMenuMenuSelected
		// View bug details and full descriptions items,
		// are only enabled if there is a project open.
		boolean hasProject = getCurrentProject() != null;
		viewBugDetailsItem.setEnabled(hasProject);
		fullDescriptionsItem.setEnabled(hasProject);
	}//GEN-LAST:event_viewMenuMenuSelected

	private void fileMenuMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_fileMenuMenuSelected
		// Save and close project items are only enabled if there is a project open.
		boolean hasProject = getCurrentProject() != null;
		saveProjectItem.setEnabled(hasProject);
		saveProjectAsItem.setEnabled(hasProject);
		reloadProjectItem.setEnabled(hasProject && !getCurrentProject().getFileName().equals(Project.UNNAMED_PROJECT));
		closeProjectItem.setEnabled(hasProject);

		// Save bugs is only enabled if there is a current analysis run
		boolean hasAnalysisRun = currentAnalysisRun != null;
		saveBugsItem.setEnabled(hasAnalysisRun);

	}//GEN-LAST:event_fileMenuMenuSelected

	private void closeProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeProjectItemActionPerformed
		if (closeProjectHook(getCurrentProject(), L10N.getLocalString("dlg.closeproject_lbl", "Close Project"))) {
			setProject(null);
		}
	}//GEN-LAST:event_closeProjectItemActionPerformed

	private void removeClasspathEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeClasspathEntryButtonActionPerformed
		Project project = getCurrentProject();
		DefaultListModel listModel = (DefaultListModel) classpathEntryList.getModel();

		int[] selIndices = classpathEntryList.getSelectedIndices();
		for (int i = selIndices.length - 1; i >= 0; i--) {
			int sel = selIndices[i];
			project.removeAuxClasspathEntry(sel);
			listModel.remove(sel);
		}
	}//GEN-LAST:event_removeClasspathEntryButtonActionPerformed

	private void addClasspathEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addClasspathEntryButtonActionPerformed
		addClasspathEntryToList();
	}//GEN-LAST:event_addClasspathEntryButtonActionPerformed

	private void browseClasspathEntryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseClasspathEntryButtonActionPerformed
		JFileChooser chooser = createFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setFileFilter(auxClasspathEntryFileFilter);
		chooser.setMultiSelectionEnabled(true);

		int result = chooseFile(chooser, "Add Entry");

		if (result != JFileChooser.CANCEL_OPTION) {
			File[] selectedFileList = chooser.getSelectedFiles();
			for (int i = 0; i < selectedFileList.length; ++i) {
				selectedFileList[i] = verifyFileSelection(selectedFileList[i]);
				String entry = selectedFileList[i].getPath();
				addClasspathEntryToProject(entry);
			}
		}
	}//GEN-LAST:event_browseClasspathEntryButtonActionPerformed

	private void fullDescriptionsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fullDescriptionsItemActionPerformed
		for (int j = 0; j < bugTreeList.length; ++j) {
			JTree bugTree = bugTreeList[j];

			// Redisplay the displayed bug instance nodes
			DefaultTreeModel bugTreeModel = (DefaultTreeModel) bugTree.getModel();
			int numRows = bugTree.getRowCount();

			for (int i = 0; i < numRows; ++i) {
				//System.out.println("Getting path for row " + i);
				TreePath path = bugTree.getPathForRow(i);
				if (path == null)
					continue;
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				if (node instanceof BugTreeNode)
					bugTreeModel.valueForPathChanged(path, node.getUserObject());
			}
		}
	}//GEN-LAST:event_fullDescriptionsItemActionPerformed

	private void viewBugDetailsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewBugDetailsItemActionPerformed
		String view = getView();
		if (view.equals("BugTree")) {
			checkBugDetailsVisibility();
		}

	}//GEN-LAST:event_viewBugDetailsItemActionPerformed

	private void bugTreeBugDetailsSplitterPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_bugTreeBugDetailsSplitterPropertyChange
		// Here we want to
		//   (1) Keep the View:Bug details checkbox item up to date, and
		//   (2) keep the details window synchronized with the current bug instance
		String propertyName = evt.getPropertyName();
		if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
			boolean isMaximized = isSplitterMaximized(bugTreeBugDetailsSplitter, evt);
			viewBugDetailsItem.setSelected(!isMaximized);
			if (!isMaximized) {
				// Details window is shown, so make sure it is populated
				// with bug detail information
				synchBugInstance();
			}
		}
	}//GEN-LAST:event_bugTreeBugDetailsSplitterPropertyChange

	private void openProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openProjectItemActionPerformed

		if (!closeProjectHook(getCurrentProject(), L10N.getLocalString("msg.openproject_txt", "Open Project")))
			return;

		JFileChooser chooser = createFileChooser();
		chooser.setFileFilter(projectFileFilter);
		int result = chooseFileToOpen(chooser);

		if (result == JFileChooser.CANCEL_OPTION)
			return;
		try {
			File file = chooser.getSelectedFile();
			Project project = new Project();
			project.read(file.getPath());
			setProject(project);
			UserPreferences.getUserPreferences().useProject(file.getPath());
			rebuildRecentProjectsMenu();

		} catch (IOException e) {
			logger.logMessage(ConsoleLogger.ERROR, MessageFormat.format( L10N.getLocalString("msg.couldnotopenproject_txt", "Could not open project: {0}"), new Object[]{e.getMessage()}));
		}
	}//GEN-LAST:event_openProjectItemActionPerformed

	private void saveProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectItemActionPerformed
		saveProject(getCurrentProject(), L10N.getLocalString("msg.saveproject_txt", "Save Project"));
	}//GEN-LAST:event_saveProjectItemActionPerformed

	private void aboutItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutItemActionPerformed
		about();
	}//GEN-LAST:event_aboutItemActionPerformed

	private void consoleSplitterPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_consoleSplitterPropertyChange
		// The idea here is to keep the View:Console checkbox up to date with
		// the real location of the divider of the consoleSplitter.
		// What we want is if any part of the console window is visible,
		// then the checkbox should be checked.
		String propertyName = evt.getPropertyName();
		if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
			boolean isMaximized = isSplitterMaximized(consoleSplitter, evt);
			viewConsoleItem.setSelected(!isMaximized);
		}
	}//GEN-LAST:event_consoleSplitterPropertyChange

	private void viewConsoleItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_viewConsoleItemActionPerformed
		if (viewConsoleItem.isSelected()) {
			consoleSplitter.resetToPreferredSizes();
		} else {
			consoleSplitter.setDividerLocation(1.0);
		}
	}//GEN-LAST:event_viewConsoleItemActionPerformed

	private void findBugsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_findBugsButtonActionPerformed
		Project project = getCurrentProject();

		if (project.getNumJarFiles() == 0) {
			logger.logMessage(ConsoleLogger.ERROR, MessageFormat.format(L10N.getLocalString("msg.projectnojars_txt", "Project {0} has no Jar files selected"), new Object[]{project}));
			return;
		}

		bugDescriptionEditorPane.setText("");
		sourceTextArea.setText("");
		AnalysisRun analysisRun = new AnalysisRun(project, this);

		logger.logMessage(ConsoleLogger.INFO, MessageFormat.format(L10N.getLocalString("msg.beginninganalysis_txt", "Beginning analysis of {0}"), new Object[]{project}));

		// Run the analysis!
		RunAnalysisDialog dialog = new RunAnalysisDialog(this, analysisRun);
		dialog.setSize(400, 300);
		dialog.setLocationRelativeTo(null); // center the dialog
		dialog.setVisible(true);

		if (dialog.isCompleted()) {
			logger.logMessage(ConsoleLogger.INFO, MessageFormat.format(L10N.getLocalString("msg.analysiscompleted_txt", "Analysis {0} completed"), new Object[]{project}));

			// Report any errors that might have occurred during analysis
			analysisRun.reportAnalysisErrors();

			// Now we have an analysis run to look at
			synchAnalysisRun(analysisRun);
		} else {
			if (dialog.exceptionOccurred()) {
				// The analysis was killed by an unexpected exception
				Exception e = dialog.getException();
				AnalysisErrorDialog err = new AnalysisErrorDialog(this, true);
				err.addLine(MessageFormat.format(L10N.getLocalString("msg.fatalanalysisexception_txt", "Fatal analysis exception: {0}"),  new Object[]{e.toString()}));
				StackTraceElement[] callList = e.getStackTrace();
				for (int i = 0; i < callList.length; ++i)
					err.addLine("\t" + callList[i]);
				err.finish();
				err.setSize(650, 500);
				err.setLocationRelativeTo(null); // center the dialog
				err.setVisible(true);
			} else {
				// Cancelled by user
				logger.logMessage(ConsoleLogger.INFO, MessageFormat.format(L10N.getLocalString("msg.analysiscancelled_txt", "Analysis of {0} cancelled by user"), new Object[]{project}));
			}
		}
	}//GEN-LAST:event_findBugsButtonActionPerformed

	private void browseSrcDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseSrcDirButtonActionPerformed
		JFileChooser chooser = createFileChooser();
		chooser.setFileFilter(archiveAndDirectoryFilter);
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		int rc = chooseFile(chooser, L10N.getLocalString("msg_addsource_lbl", "Add source directory or archive"));
		if (rc == JFileChooser.APPROVE_OPTION) {
			File[] selectedFileList = chooser.getSelectedFiles();
			for (int i = 0; i < selectedFileList.length; ++i) {
				selectedFileList[i] = verifyFileSelection(selectedFileList[i]);
				String entry = selectedFileList[i].getPath();
				addSrcToProject(entry);
			}
		}
	}//GEN-LAST:event_browseSrcDirButtonActionPerformed

	private void srcDirTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_srcDirTextFieldActionPerformed
		addSourceDirToList();
	}//GEN-LAST:event_srcDirTextFieldActionPerformed

	private void jarNameTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jarNameTextFieldActionPerformed
		addJarToList();
	}//GEN-LAST:event_jarNameTextFieldActionPerformed

	private void browseJarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseJarButtonActionPerformed
		JFileChooser chooser = createFileChooser();
		chooser.setFileFilter(archiveAndDirectoryFilter);
		chooser.setMultiSelectionEnabled(true);
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		int rc = chooseFile(chooser, L10N.getLocalString("msg.addarchiveordirectory_txt", "Add archive or directory"));
		if (rc == JFileChooser.APPROVE_OPTION) {
			File[] selectedFileList = chooser.getSelectedFiles();
			for (int i = 0; i < selectedFileList.length; ++i) {
				selectedFileList[i] = verifyFileSelection(selectedFileList[i]);
				String entry = selectedFileList[i].getPath();
				addJarToProject(entry);
			}
		}
	}//GEN-LAST:event_browseJarButtonActionPerformed

	private void newProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newProjectItemActionPerformed

		if (!closeProjectHook(getCurrentProject(), L10N.getLocalString("msg.newproject_txt", "New Project")))
			return;

		Project project = new Project();
		setProject(project);
	}//GEN-LAST:event_newProjectItemActionPerformed

	private void exitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitItemActionPerformed
		exitFindBugs();
	}//GEN-LAST:event_exitItemActionPerformed

	private void removeSrcDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeSrcDirButtonActionPerformed
		Project project = getCurrentProject();
		DefaultListModel listModel = (DefaultListModel) sourceDirList.getModel();

		int[] selIndices = sourceDirList.getSelectedIndices();
		for (int i = selIndices.length - 1; i >= 0; i--) {
			int sel = selIndices[i];
			project.removeSourceDir(sel);
			listModel.remove(sel);
		}
	}//GEN-LAST:event_removeSrcDirButtonActionPerformed

	private void removeJarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeJarButtonActionPerformed
		Project project = getCurrentProject();
		DefaultListModel listModel = (DefaultListModel) jarFileList.getModel();

		int[] selIndices = jarFileList.getSelectedIndices();
		for (int i = selIndices.length - 1; i >= 0; i--) {
			int sel = selIndices[i];
			project.removeJarFile(sel);
			listModel.remove(sel);
		}
	}//GEN-LAST:event_removeJarButtonActionPerformed

	private void addSourceDirButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSourceDirButtonActionPerformed
		addSourceDirToList();
	}//GEN-LAST:event_addSourceDirButtonActionPerformed

	private void addJarButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addJarButtonActionPerformed
		addJarToList();
	}//GEN-LAST:event_addJarButtonActionPerformed

	/**
	 * Exit the Application
	 */
	private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
		exitFindBugs();
	}//GEN-LAST:event_exitForm

	/**
	 * This makes the set of controls passed in all the same size, equal to
	 * the minimum needed of the largest control.
	 */
	private void equalizeControls(JComponent[] components) {
		Dimension d;

		int minX = 0, minY = 0;
		for (int i = 0; i < components.length; i++) {
			JComponent comp = components[i];
			comp.setMaximumSize(null);
			comp.setMinimumSize(null);
			comp.setPreferredSize(null);
			d = comp.getPreferredSize();
			if (d.width > minX)
				minX = d.width;
			if (d.height > minY)
				minY = d.height;
		}

		d = new Dimension(minX, minY);
		for (int i = 0; i < components.length; i++) {
			JComponent comp = components[i];
			comp.setMinimumSize(d);
			comp.setMaximumSize(d);
			comp.setPreferredSize(d);
		}
	}

	/**
	 * This is called whenever the selection is changed in the bug tree.
	 *
	 * @param e the TreeSelectionEvent
	 */
	private void bugTreeSelectionChanged(TreeSelectionEvent e) {

		BugInstance selected = getCurrentBugInstance();
		if (selected != null) {
			synchBugInstance();
		}
	}

	private void openRecentProjectItemActionPerformed(java.awt.event.ActionEvent evt) {
		if (!closeProjectHook(getCurrentProject(), L10N.getLocalString("msg.openproject_txt", "Open Project")))
			return;

		JMenuItem recentProjectItem = (JMenuItem) evt.getSource();
		File file = new File(recentProjectItem.getText());
		try {
			System.setProperty("user.dir", file.getParent());
			Project project = new Project();
			project.read(file.getPath());
			setProject(project);
			UserPreferences.getUserPreferences().useProject(file.getPath());
		} catch (IOException e) {
			UserPreferences.getUserPreferences().removeProject(file.getPath());
			logger.logMessage(ConsoleLogger.ERROR, MessageFormat.format(L10N.getLocalString("msg.couldnotopenproject_txt", "Could not open project: {0}"), new Object[]{e.getMessage()}));
		} finally {
			rebuildRecentProjectsMenu();
		}
	}

	private boolean moveEntriesUp(JList entryList) {
		int[] selIndices = entryList.getSelectedIndices();
		if (selIndices.length == 0)
			return false;

		boolean changed = false;
		int lastInsertPos = -1;
		DefaultListModel model = (DefaultListModel) entryList.getModel();
		for (int i = 0; i < selIndices.length; i++) {
			int sel = selIndices[i];
			if ((sel - 1) > lastInsertPos) {
				model.add(sel - 1, model.remove(sel));
				selIndices[i] = sel - 1;
				changed = true;
			}
			lastInsertPos = selIndices[i];
		}

		entryList.setSelectedIndices(selIndices);
		return changed;
	}

	private boolean moveEntriesDown(JList entryList) {
		int[] selIndices = entryList.getSelectedIndices();
		if (selIndices.length == 0)
			return false;

		boolean changed = false;
		DefaultListModel model = (DefaultListModel) entryList.getModel();
		int lastInsertPos = model.getSize();
		for (int i = selIndices.length - 1; i >= 0; i--) {
			int sel = selIndices[i];
			if ((sel + 1) < lastInsertPos) {
				model.add(sel + 1, model.remove(sel));
				selIndices[i] = sel + 1;
				changed = true;
			}
			lastInsertPos = selIndices[i];
		}

		entryList.setSelectedIndices(selIndices);
		return changed;
	}

	private void resyncAuxClasspathEntries() {
		Project project = getCurrentProject();
		int numEntries = project.getNumAuxClasspathEntries();
		while (numEntries-- > 0)
			project.removeAuxClasspathEntry(0);

		DefaultListModel model = (DefaultListModel) classpathEntryList.getModel();
		for (int i = 0; i < model.size(); i++)
			project.addAuxClasspathEntry((String) model.get(i));
	}

	private void resyncSourceEntries() {
		Project project = getCurrentProject();
		int numEntries = project.getNumSourceDirs();
		while (numEntries-- > 0)
			project.removeSourceDir(0);

		DefaultListModel model = (DefaultListModel) sourceDirList.getModel();
		for (int i = 0; i < model.size(); i++)
			project.addSourceDir((String) model.get(i));
	}

	/**
	 * Localise the given AbstractButton, setting the text and optionally mnemonic
	 * Note that AbstractButton includes menus and menu items.
	 * @param button        The button to localise
	 * @param key           The key to look up in resource bundle
	 * @param defaultString default String to use if key not found
	 * @param setMnemonic   whether or not to set the mnemonic. According to Sun's
	 *                      guidelines, default/cancel buttons should not have mnemonics
	 *                      but instead should use Return/Escape
	 */
	private void localiseButton(AbstractButton button, String key, String defaultString,
	                            boolean setMnemonic) {
		AnnotatedString as = new AnnotatedString(L10N.getLocalString(key, defaultString));
		button.setText(as.toString());
		int mnemonic;
		if (setMnemonic &&
		    (mnemonic = as.getMnemonic()) != KeyEvent.VK_UNDEFINED) {
			button.setMnemonic(mnemonic);
			button.setDisplayedMnemonicIndex(as.getMnemonicIndex());
		}
	}

	/* ----------------------------------------------------------------------
	 * Component initialization support
	 * ---------------------------------------------------------------------- */

	/**
	 * This is called from the constructor to perform post-initialization
	 * of the components in the form.
	 */
	private void postInitComponents() {
		logger = new ConsoleLogger(this);

		viewPanelLayout = (CardLayout) viewPanel.getLayout();

		// Console starts out disabled
		consoleSplitter.setDividerLocation(1.0);

		// List of bug group tabs.
		// This must be in the same order as GROUP_BY_ORDER_LIST!
		bugTreeList = new JTree[]{byClassBugTree, byPackageBugTree, byBugTypeBugTree};

		// Configure bug trees
		for (int i = 0; i < bugTreeList.length; ++i) {
			JTree bugTree = bugTreeList[i];
			bugTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			bugTree.setCellRenderer(bugCellRenderer);
			bugTree.setRootVisible(false);
			bugTree.setShowsRootHandles(true);
			bugTree.addTreeSelectionListener(new TreeSelectionListener() {
				public void valueChanged(TreeSelectionEvent e) {
					bugTreeSelectionChanged(e);
				}
			});
		}

		jarFileList.setModel(new DefaultListModel());
		sourceDirList.setModel(new DefaultListModel());
		classpathEntryList.setModel(new DefaultListModel());

		// We use a special highlight painter to ensure that the highlights cover
		// complete source lines, even though the source text doesn't
		// fill the lines completely.
		final Highlighter.HighlightPainter painter =
		        new DefaultHighlighter.DefaultHighlightPainter(sourceTextArea.getSelectionColor()) {
			        public Shape paintLayer(Graphics g, int offs0, int offs1,
			                                Shape bounds, JTextComponent c, View view) {
				        try {
					        Shape extent = view.modelToView(offs0, Position.Bias.Forward, offs1, Position.Bias.Backward, bounds);
					        Rectangle rect = extent.getBounds();
					        rect.x = 0;
					        rect.width = bounds.getBounds().width;
					        g.setColor(getColor());
					        g.fillRect(rect.x, rect.y, rect.width, rect.height);
					        return rect;
				        } catch (BadLocationException e) {
					        return null;
				        }
			        }
		        };
		Highlighter sourceHighlighter = new DefaultHighlighter() {
			public Object addHighlight(int p0, int p1, Highlighter.HighlightPainter p)
			        throws BadLocationException {
				return super.addHighlight(p0, p1, painter);
			}
		};
		sourceTextArea.setHighlighter(sourceHighlighter);

		updateTitle(getCurrentProject());

		// Load the icon for the UMD logo
		ClassLoader classLoader = this.getClass().getClassLoader();
		ImageIcon logoIcon = new ImageIcon(classLoader.getResource("edu/umd/cs/findbugs/gui/logo_umd.png"));
		logoLabel.setIcon(logoIcon);

		// Set common Menu Accelerators
		newProjectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, MENU_MASK));
		openProjectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK));
		saveProjectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK));
		closeProjectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, MENU_MASK));
		reloadProjectItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, MENU_MASK));
		
		if (MAC_OS_X) {
			// Some more accelerators that use modifiers. Other platforms
			// tend not to use modifiers for menu accelerators
			saveProjectAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK | Event.SHIFT_MASK));
			loadBugsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, MENU_MASK | Event.ALT_MASK));
			saveBugsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, MENU_MASK | Event.ALT_MASK));

			// Leave room for the growBox on Mac
			growBoxSpacer.setMinimumSize(new java.awt.Dimension(16,16));

			// Remove Unnecessary/Redundant menu items.
			fileMenu.remove(exitItem);
			fileMenu.remove(jSeparator6);
			theMenuBar.remove(helpMenu);

			// Set up listeners for Quit and About menu items using
			// Apple's EAWT API.
			// We use reflection here, so there is no posible chance that the
			// class loader will try to load OSXAdapter on a non Mac system
			try {
				Class osxAdapter = Class.forName("edu.umd.cs.findbugs.gui.OSXAdapter");
				Class[] defArgs = {FindBugsFrame.class};
				Method registerMethod = osxAdapter.getDeclaredMethod("registerMacOSXApplication", defArgs);
				if (registerMethod != null) {
					Object[] args = {this};
					registerMethod.invoke(osxAdapter, args);
				}
			}
			catch (NoClassDefFoundError e) {
				// This will be thrown first if the OSXAdapter is loaded on a system without the EAWT
				// because OSXAdapter extends ApplicationAdapter in its def
				System.err.println("This version of Mac OS X does not support the Apple EAWT. Application Menu handling has been disabled (" + e + ")");
			}
			catch (ClassNotFoundException e) {
				// This shouldn't be reached; if there's a problem with the OSXAdapter we should get the
				// above NoClassDefFoundError first.
				System.err.println("This version of Mac OS X does not support the Apple EAWT. Application Menu handling has been disabled (" + e + ")");
			}
			catch (Exception e) {
				System.err.println("Exception while loading the OSXAdapter:");
				e.printStackTrace();
			}
		}

	}

	private void rebuildRecentProjectsMenu() {
		UserPreferences prefs = UserPreferences.getUserPreferences();
		final List<String> recentProjects = prefs.getRecentProjects();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				recentProjectsMenu.removeAll();
				java.awt.Font ft = new java.awt.Font("Dialog", 0, 12);
				if (recentProjects.size() == 0) {
					JMenuItem emptyItem = new JMenuItem(L10N.getLocalString("menu.empty_item", "Empty"));
					emptyItem.setFont(ft);
					emptyItem.setEnabled(false);
					recentProjectsMenu.add(emptyItem);
				} else {
					for (int i = 0; i < recentProjects.size(); i++) {
						JMenuItem projectItem = new JMenuItem(recentProjects.get(i));
						projectItem.setFont(ft);
						projectItem.addActionListener(new java.awt.event.ActionListener() {
							public void actionPerformed(java.awt.event.ActionEvent evt) {
								openRecentProjectItemActionPerformed(evt);
							}
						});
						recentProjectsMenu.add(projectItem);
					}
				}
			}
		});
	}

	/* ----------------------------------------------------------------------
	 * Helpers for accessing and modifying UI components
	 * ---------------------------------------------------------------------- */

	/**
	 * Based on the current tree selection path, get a user object
	 * whose class is the same as the given class.
	 *
	 * @param tree the tree
	 * @param c    the class
	 * @return an instance of the given kind of object which is in the
	 *         current selection, or null if there is no matching object
	 */
	private static Object getTreeSelectionOf(JTree tree, Class c) {
		TreePath selPath = tree.getSelectionPath();

		// There may not be anything selected at the moment
		if (selPath == null)
			return null;

		// Work backwards from end until we get to the kind of
		// object we're looking for.
		Object[] nodeList = selPath.getPath();
		for (int i = nodeList.length - 1; i >= 0; --i) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodeList[i];
			Object nodeInfo = node.getUserObject();
			if (nodeInfo != null && nodeInfo.getClass() == c)
				return nodeInfo;
		}
		return null;
	}

	/**
	 * Get the current project.
	 */
	private Project getCurrentProject() {
		return currentProject;
	}

	/**
	 * Get the current analysis run.
	 */
	private AnalysisRun getCurrentAnalysisRun() {
		return currentAnalysisRun;
	}

	/**
	 * Get the bug instance currently selected in the bug tree.
	 */
	private BugInstance getCurrentBugInstance() {
		JTree bugTree = getCurrentBugTree();
		if (bugTree != null) {
			return (BugInstance) getTreeSelectionOf(bugTree, BugInstance.class);
		}
		return null;
	}

	/**
	 * Return whether or not the given splitter is "maximized", meaning that
	 * the top window of the split has been given all of the space.
	 * Note that this window assumes that the split is vertical (meaning
	 * that we have top and bottom components).
	 *
	 * @param splitter the JSplitPane
	 * @param evt      the event that is changing the splitter value
	 */
	private boolean isSplitterMaximized(JSplitPane splitter, java.beans.PropertyChangeEvent evt) {
		Integer location = (Integer) evt.getNewValue();

		int height = splitter.getHeight();
		int hopefullyMaxDivider = height - (splitter.getDividerSize() + DIVIDER_FUDGE);
		//System.out.println("Splitter: "+(splitter==consoleSplitter?"consoleSplitter":"bugTreeBugDetailsSplitter")+
		//    ": height="+height+",location="+location+
		//    ",hopefullyMax="+hopefullyMaxDivider);
		boolean isMaximized = location.intValue() >= hopefullyMaxDivider;
		return isMaximized;
	}

	private void checkBugDetailsVisibility() {
		if (viewBugDetailsItem.isSelected()) {
			bugTreeBugDetailsSplitter.resetToPreferredSizes();
		} else {
			bugTreeBugDetailsSplitter.setDividerLocation(1.0);
		}
		//System.out.("New bug detail splitter location " + bugTreeBugDetailsSplitter.getDividerLocation());
	}

	private JTree getCurrentBugTree() {
		JScrollPane selected = (JScrollPane) groupByTabbedPane.getSelectedComponent();
		Object view = selected.getViewport().getView();
		if (view instanceof JTree) {
			return (JTree) view;
		}
		return null;
	}

	/* ----------------------------------------------------------------------
	 * Synchronization of data model and UI
	 * ---------------------------------------------------------------------- */

	/**
	 * Set the priority threshold for display of bugs in the bug tree.
	 *
	 * @param threshold the threshold
	 */
	private void setPriorityThreshold(int threshold) {
		if (threshold != priorityThreshold) {
			priorityThreshold = threshold;
			if (currentAnalysisRun != null)
				synchAnalysisRun(currentAnalysisRun);
		}
	}

        private void ableEditMenu() {
            String view = getView();
            if ((view != null) && view.equals("EditProjectPanel")) {
                if (selectedComponent != null) {
                    boolean hasSelection = false;
                    if (selectedComponent instanceof JList) {
                        JList list = (JList)selectedComponent;
                        hasSelection = list.getSelectedIndices().length > 0;
                    } else if (selectedComponent instanceof JTextField) {
                        JTextField tf = (JTextField)selectedComponent;
                        hasSelection = tf.getSelectedText().length() > 0;
                    }
                                    
                    cutItem.setEnabled(hasSelection);
                    copyItem.setEnabled(hasSelection);   
                    selectAllItem.setEnabled(true);                  
                }
//            } else if (view.equals("BugTree")) {           
//            } else if (view.equals("ReportPanel")) {
                
            } else {
                cutItem.setEnabled(false);
                copyItem.setEnabled(true);
                pasteItem.setEnabled(false);
                selectAllItem.setEnabled(false);
            }            
        }
        
	private void setProject(Project project) {
		currentProject = project;
		if (project != null) {
			synchProject(project);
			setView("EditProjectPanel");
                        editMenu.setEnabled(true);
			viewProjectItem.setEnabled(true);
			viewProjectItem.setSelected(true);
			viewBugsItem.setEnabled(false);
			viewBugsItem.setSelected(false);
		} else {
			editMenu.setEnabled(false);
                        viewProjectItem.setEnabled(false);
			viewProjectItem.setSelected(false);
			viewBugsItem.setEnabled(false);
			viewBugsItem.setSelected(false);
			setView("EmptyPanel");
		}
		updateTitle(project);
                ableEditMenu();
	}

	private void updateTitle(Project project) {
		if (project == null)
			this.setTitle(L10N.getLocalString("dlg.noproject_lbl", "FindBugs - no project"));
		else
			this.setTitle("FindBugs - " + project.toString());
	}

	/**
	 * Save given project.
	 * If the project already has a valid filename, use that filename.
	 * Otherwise, prompt for one.
	 *
	 * @param project     the Project to save
	 * @param dialogTitle the title for the save dialog (if needed)
	 */
	private boolean saveProject(Project project, String dialogTitle) {
		return saveProject(project, dialogTitle, false);
	}

	/**
	 * Offer to save the current Project to a file.
	 *
	 * @param project        the Project to save
	 * @param dialogTitle    the title for the save dialog (if needed)
	 * @param chooseFilename if true, force a dialog to prompt the user
	 *                       for a filename
	 * @return true if the project is saved successfully, false if the user
	 *         cancels or an error occurs
	 */
	private boolean saveProject(Project project, String dialogTitle, boolean chooseFilename) {
		boolean useRelativePaths;
		try {
			if (project == null)
				return true;

			File file;
			String fileName = project.getFileName();

			if (!fileName.startsWith("<") && !chooseFilename) {
				file = new File(fileName);
				useRelativePaths = project.getOption( Project.RELATIVE_PATHS );
			} else {
				JRadioButton relativePaths = new JRadioButton(L10N.getLocalString("msg.userelativepaths_txt", "Use Relative Paths"));
				relativePaths.setSelected(project.getOption(Project.RELATIVE_PATHS));
				JFileChooser chooser = createFileChooser(relativePaths);
				chooser.setFileFilter(projectFileFilter);
				int result = chooseFile(chooser, dialogTitle);
				if (result == JFileChooser.CANCEL_OPTION)
					return false;
				file = chooser.getSelectedFile();
				fileName = Project.transformFilename(file.getPath());
				file = new File(fileName);
				useRelativePaths = relativePaths.isSelected();
			}

			project.write(file.getPath(), useRelativePaths, file.getParent());
			logger.logMessage(ConsoleLogger.INFO, "Project saved");
			project.setFileName(file.getPath());

			UserPreferences prefs = UserPreferences.getUserPreferences();
			prefs.useProject(file.getPath());
			prefs.read();
			rebuildRecentProjectsMenu();

			updateTitle(project);

			return true;
		} catch (IOException e) {
			logger.logMessage(ConsoleLogger.ERROR, "Could not save project: " + e.toString());
			JOptionPane.showMessageDialog(this, "Error saving project: " + e.toString(),
			        "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	/**
	 * Hook to call before closing a project.
	 *
	 * @param project         the project being closed
	 * @param savePromptTitle title to use for the "Save project?" dialog
	 * @return true if user has confirmed that the project should be closed,
	 *         false if the close is cancelled
	 */
	private boolean closeProjectHook(Project project, String savePromptTitle) {
		if (project == null || !project.isModified())
			return true;

		// Confirm that the project should be closed.
		int option = JOptionPane.showConfirmDialog(this, L10N.getLocalString("msg.saveprojectquery_txt", "Save Project?"), savePromptTitle,
		        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

		if (option == JOptionPane.CANCEL_OPTION)
			return false;
		else if (option == JOptionPane.YES_OPTION) {
			boolean result = saveProject(project, "Save Project");
			if (result)
				JOptionPane.showMessageDialog(this, "Project was successfully saved.");
			return result;
		} else
			return true;
	}

	/**
	 * Synchronize the edit project dialog with given project.
	 *
	 * @param project the selected project
	 */
	private void synchProject(Project project) {
		// Clear text fields
		jarNameTextField.setText("");
		srcDirTextField.setText("");
		classpathEntryTextField.setText("");

		// Populate jar file, source directory, and aux classpath entry lists

		DefaultListModel jarListModel = (DefaultListModel) jarFileList.getModel();
		jarListModel.clear();
		for (int i = 0; i < project.getNumJarFiles(); ++i) {
			jarListModel.addElement(project.getJarFile(i));
		}

		DefaultListModel srcDirListModel = (DefaultListModel) sourceDirList.getModel();
		srcDirListModel.clear();
		for (int i = 0; i < project.getNumSourceDirs(); ++i) {
			srcDirListModel.addElement(project.getSourceDir(i));
		}

		DefaultListModel classpathEntryListModel = (DefaultListModel) classpathEntryList.getModel();
		classpathEntryListModel.clear();
		for (int i = 0; i < project.getNumAuxClasspathEntries(); ++i) {
			classpathEntryListModel.addElement(project.getAuxClasspathEntry(i));
		}
	}

	/**
	 * Synchronize the bug trees with the given analysisRun object.
	 *
	 * @param analysisRun the selected analysis run
	 */
	private void synchAnalysisRun(AnalysisRun analysisRun) {
		// Create and populate tree models
		for (int i = 0; i < GROUP_BY_ORDER_LIST.length; ++i) {
			DefaultMutableTreeNode bugRootNode = new DefaultMutableTreeNode();
			DefaultTreeModel bugTreeModel = new DefaultTreeModel(bugRootNode);

			String groupByOrder = GROUP_BY_ORDER_LIST[i];
			analysisRun.setTreeModel(groupByOrder, bugTreeModel);
			populateAnalysisRunTreeModel(analysisRun, groupByOrder);
			if (i < bugTreeList.length)
				bugTreeList[i].setModel(bugTreeModel);
		}

		currentAnalysisRun = analysisRun;

		//set the summary output
		setSummary(analysisRun.getSummary());
		setView("BugTree");
	}

	private void setSummary(String summaryXML) {
		bugSummaryEditorPane.setContentType("text/html");
		bugSummaryEditorPane.setText(summaryXML);

		// FIXME: unfortunately, using setText() on the editor pane
		// results in the contents being scrolled to the bottom of the pane.
		// An immediate inline call to set the scroll position does nothing.
		// So, use invokeLater(), even though this results in flashing.
		// [What we really need is a way to set the text WITHOUT changing
		// the caret position.  Need to investigate.]
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				bySummary.getViewport().setViewPosition(new Point(0, 0));
			}
		});
	}

	/**
	 * Populate an analysis run's tree model for given sort order.
	 */
	private void populateAnalysisRunTreeModel(AnalysisRun analysisRun, final String groupBy) {
		//System.out.println("Populating bug tree for order " + groupBy);

		// Set busy cursor - this is potentially a time-consuming operation
		Cursor orig = this.getCursor();
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		final DefaultTreeModel bugTreeModel = analysisRun.getTreeModel(groupBy);
		final DefaultMutableTreeNode bugRootNode = (DefaultMutableTreeNode) bugTreeModel.getRoot();

		// Delete all children from root node
		bugRootNode.removeAllChildren();

		// Sort the instances (considering only those that meet the
		// priority threshold)
		TreeSet<BugInstance> sortedCollection = new TreeSet<BugInstance>(getBugInstanceComparator(groupBy));
		for (Iterator<BugInstance> i = analysisRun.getBugInstances().iterator(); i.hasNext();) {
			BugInstance bugInstance = i.next();
			if (bugInstance.getPriority() <= priorityThreshold)
				sortedCollection.add(bugInstance);
		}

		// The grouper callback is what actually adds the group and bug
		// nodes to the tree.
		Grouper.Callback<BugInstance> callback = new Grouper.Callback<BugInstance>() {
			private BugInstanceGroup currentGroup;
			private DefaultMutableTreeNode currentGroupNode;

			public void startGroup(BugInstance member) {
				String groupName;
				if (groupBy == GROUP_BY_CLASS)
					groupName = member.getPrimaryClass().getClassName();
				else if (groupBy == GROUP_BY_PACKAGE) {
					groupName = member.getPrimaryClass().getPackageName();
					if (groupName.equals(""))
						groupName = "Unnamed package";
				} else if (groupBy == GROUP_BY_BUG_TYPE) {
					String desc = member.toString();
					String shortBugType = desc.substring(0, desc.indexOf(':'));
					String bugTypeDescription = I18N.instance().getBugTypeDescription(shortBugType);
					groupName = shortBugType + ": " + bugTypeDescription;
				} else
					throw new IllegalStateException("Unknown sort order: " + groupBy);
				currentGroup = new BugInstanceGroup(groupBy, groupName);
				currentGroupNode = new DefaultMutableTreeNode(currentGroup);
				bugTreeModel.insertNodeInto(currentGroupNode, bugRootNode, bugRootNode.getChildCount());

				insertIntoGroup(member);
			}

			public void addToGroup(BugInstance member) {
				insertIntoGroup(member);
			}

			private void insertIntoGroup(BugInstance member) {
				int count = currentGroup.getMemberCount();
				currentGroup.incrementMemberCount();
				BugTreeNode bugNode = new BugTreeNode(member);
				if (BUG_COUNT)
					bugNode.setCount(count);
				bugTreeModel.insertNodeInto(bugNode, currentGroupNode, currentGroupNode.getChildCount());

				// Insert annotations
				Iterator j = member.annotationIterator();
				while (j.hasNext()) {
					BugAnnotation annotation = (BugAnnotation) j.next();
					DefaultMutableTreeNode annotationNode = new DefaultMutableTreeNode(annotation);
					bugTreeModel.insertNodeInto(annotationNode, bugNode, bugNode.getChildCount());
				}

			}
		};

		// Create the grouper, and execute it to populate the bug tree
		Grouper<BugInstance> grouper = new Grouper<BugInstance>(callback);
		Comparator<BugInstance> groupComparator = getGroupComparator(groupBy);
		grouper.group(sortedCollection, groupComparator);

		// Let the tree know it needs to update itself
		bugTreeModel.nodeStructureChanged(bugRootNode);

		// Now we're done
		this.setCursor(orig);
	}

	/**
	 * Get a BugInstance Comparator for given sort order.
	 */
	private Comparator<BugInstance> getBugInstanceComparator(String sortOrder) {
		if (sortOrder.equals(GROUP_BY_CLASS))
			return bugInstanceByClassComparator;
		else if (sortOrder.equals(GROUP_BY_PACKAGE))
			return bugInstanceByPackageComparator;
		else if (sortOrder.equals(GROUP_BY_BUG_TYPE))
			return bugInstanceByTypeComparator;
		else
			throw new IllegalArgumentException("Bad sort order: " + sortOrder);
	}

	/**
	 * Get a Grouper for a given sort order.
	 */
	private Comparator<BugInstance> getGroupComparator(String groupBy) {
		if (groupBy.equals(GROUP_BY_CLASS)) {
			return bugInstanceClassComparator;
		} else if (groupBy.equals(GROUP_BY_PACKAGE)) {
			return bugInstancePackageComparator;
		} else if (groupBy.equals(GROUP_BY_BUG_TYPE)) {
			return bugInstanceTypeComparator;
		} else
			throw new IllegalArgumentException("Bad sort order: " + groupBy);
	}

	/**
	 * Set the view panel to display the named view.
	 */
	private void setView(String viewName) {
		//System.out.println("Showing view " + viewName);
		viewPanelLayout.show(viewPanel, viewName);
		boolean viewingBugs = viewName.equals("BugTree");
		if (viewingBugs)
			checkBugDetailsVisibility();

		viewProjectItem.setSelected(!viewingBugs);
		if (viewingBugs)
			viewBugsItem.setEnabled(true);
		viewBugsItem.setSelected(viewingBugs);

		currentView = viewName;
                ableEditMenu();
	}

	/**
	 * Get which view is displayed currently.
	 */
	private String getView() {
		return currentView;
	}

	/**
	 * Called to add the jar file in the jarNameTextField to the
	 * Jar file list (and the project it represents).
	 */
	private void addJarToList() {
            String dirs = jarNameTextField.getText();
            String[] jarDirs = parsePaths(dirs);
            for (int i = 0; i < jarDirs.length; i++) {
                String jarFile = jarDirs[i];
		if (!jarFile.equals("")) {
			addJarToProject(jarFile);
		}
            }
	    jarNameTextField.setText("");
	}

	/**
	 * Add a Src file to the current project.
	 *
	 * @param srcFile the jar file to add to the project
	 */
	private void addSrcToProject(String srcFile) {
		Project project = getCurrentProject();
		if (project.addSourceDir(srcFile)) {
			DefaultListModel listModel = (DefaultListModel) sourceDirList.getModel();
			listModel.addElement(srcFile);
		}
	}

	/**
	 * Add a Jar file to the current project.
	 *
	 * @param jarFile the jar file to add to the project
	 */
	private void addJarToProject(String jarFile) {
		Project project = getCurrentProject();
		if (project.addJar(jarFile)) {
			DefaultListModel listModel = (DefaultListModel) jarFileList.getModel();
			listModel.addElement(jarFile);
		}
	}
        
        /**
         * Parses a classpath into it's sub paths
         *
         * @param path the classpath
         * @return an array of paths
         */
        private String[] parsePaths(String paths) {
            return paths.split(System.getProperty("path.separator"));
        }

	/**
	 * Called to add the source directory in the sourceDirTextField
	 * to the source directory list (and the project it represents).
	 */
	private void addSourceDirToList() {
            String dirs = srcDirTextField.getText();
            String[] sourceDirs = parsePaths(dirs);
            for (int i = 0; i < sourceDirs.length; i++) {
                String sourceDir = sourceDirs[i];
                if (!sourceDir.equals("")) {
                    Project project = getCurrentProject();
                    if (project.addSourceDir(sourceDir)) {
                        DefaultListModel listModel = (DefaultListModel) sourceDirList.getModel();
                        listModel.addElement(sourceDir);
                    }
                }
            }
            srcDirTextField.setText("");
	}

	/**
	 * Called to add the classpath entry in the classpathEntryTextField
	 * to the classpath entry list (and the project it represents).
	 */
	private void addClasspathEntryToList() {
            String dirs = classpathEntryTextField.getText();
            String[] classDirs = parsePaths(dirs);
            for (int i = 0; i < classDirs.length; i++) {
                String classpathEntry = classDirs[i];
		if (!classpathEntry.equals("")) {
			addClasspathEntryToProject(classpathEntry);
		}
            }
            classpathEntryTextField.setText("");
	}

	/**
	 * Add a classpath entry to the current project.
	 *
	 * @param classpathEntry the classpath entry to add
	 */
	private void addClasspathEntryToProject(String classpathEntry) {
		Project project = getCurrentProject();
		if (project.addAuxClasspathEntry(classpathEntry)) {
			DefaultListModel listModel = (DefaultListModel) classpathEntryList.getModel();
			listModel.addElement(classpathEntry);
		}
	}

	/**
	 * Synchronize current bug instance with the bug detail
	 * window (source view, details window, etc.)
	 */
	private void synchBugInstance() {
		// Get current bug instance
		BugInstance selected = getCurrentBugInstance();
		if (selected == null)
			return;

		// If the details window is minimized, then the user can't see
		// it and there is no point in updating it.
		if (!viewBugDetailsItem.isSelected())
			return;

		// Get the current source line annotation.
		// If the current leaf selected is not a source line annotation,
		// use the default source line annotation from the current bug instance
		// (if any).
		JTree bugTree = getCurrentBugTree();

		// if the summary window is shown then skip it
		if (bugTree == null) {
			return;
		}
		SourceLineAnnotation srcLine = null;
		TreePath selPath = bugTree.getSelectionPath();
		if (selPath != null) {
			Object leaf = ((DefaultMutableTreeNode) selPath.getLastPathComponent()).getUserObject();
			if (leaf instanceof SourceLineAnnotation)
				srcLine = (SourceLineAnnotation) leaf;
			else
				srcLine = selected.getPrimarySourceLineAnnotation();
		}

		// Show source code.
		if (srcLine == null || srcLine != currentSourceLineAnnotation) {
			Project project = getCurrentProject();
			AnalysisRun analysisRun = getCurrentAnalysisRun();
			if (project == null) throw new IllegalStateException("null project!");
			if (analysisRun == null) throw new IllegalStateException("null analysis run!");
			try {
				boolean success = viewSource(project, analysisRun, srcLine);
				sourceTextArea.setEnabled(success);
				if (!success)
					sourceTextArea.setText("No source line information for this bug");
			} catch (IOException e) {
				sourceTextArea.setText("Could not find source: " + e.getMessage());
				logger.logMessage(ConsoleLogger.WARNING, e.getMessage());
			}

			currentSourceLineAnnotation = srcLine;
		}

		// Show bug info.
		showBugInfo(selected);

		// Synch annotation text.
		synchBugAnnotation(selected);

		// Now the bug details are up to date.
		currentBugInstance = selected;
	}

	private static final int SELECTION_VOFFSET = 2;

	/**
	 * Update the source view window.
	 *
	 * @param project     the project (containing the source directories to search)
	 * @param analysisRun the analysis run (containing the mapping of classes to source files)
	 * @param srcLine     the source line annotation (specifying source file to load and
	 *                    which lines to highlight)
	 * @return true if the source was shown successfully, false otherwise
	 */
	private boolean viewSource(Project project, AnalysisRun analysisRun, final SourceLineAnnotation srcLine)
	        throws IOException {
		// Get rid of old source code text
		sourceTextArea.setText("");

		// There is nothing to do without a source annotation
		// TODO: actually, might want to put a message in the source window
		// explaining that we don't have the source file, and that
		// they might want to recompile with debugging info turned on.
		if (srcLine == null)
			return false;

		// Look up the source file for this class.
		sourceFinder.setSourceBaseList(project.getSourceDirList());
		String sourceFile = srcLine.getSourceFile();
		if (sourceFile == null || sourceFile.equals("<Unknown>")) {
			logger.logMessage(ConsoleLogger.WARNING, "No source file for class " + srcLine.getClassName());
			return false;
		}

		// Try to open the source file and display its contents
		// in the source text area.
		InputStream in = sourceFinder.openSource(srcLine.getPackageName(), sourceFile);
		BufferedReader reader = null;

		try {
			reader = new BufferedReader(new InputStreamReader(in));

			String line;
			while ((line = reader.readLine()) != null) {
				sourceTextArea.append(line + "\n");
			}
		} finally {
			if (reader != null)
				reader.close();
		}

		if (srcLine.isUnknown()) {
			// No line number information, so can't highlight anything
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					sourceTextAreaScrollPane.getViewport().setViewPosition(new Point(0, 0));
				}
			});
			return true;
		}

		// Highlight the annotation.
		// There seems to be some bug in Swing that sometimes prevents this code
		// from working when executed immediately after populating the
		// text in the text area.  My guess is that when a large amount of text
		// is added, Swing defers some UI update work until "later" that is needed
		// to compute the visibility of text in the text area.
		// So, post some code to do the update to the Swing event queue.
		// Not really an ideal solution, but it seems to work.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Highlight the lines from the source annotation.
				// Note that the source lines start at 1, while the line numbers
				// in the text area start at 0.
				try {
					int startLine = srcLine.getStartLine() - 1;
					int endLine = srcLine.getEndLine();

					// Compute number of rows visible.
					// What a colossal pain in the ass this is.
					// You'd think there would be a convenient method to
					// return this information, but no.
					JViewport viewport = sourceTextAreaScrollPane.getViewport();
					Rectangle viewportRect = viewport.getViewRect();
					int topRow = sourceTextArea.getLineOfOffset(sourceTextArea.viewToModel(viewportRect.getLocation()));
					int bottomRow = sourceTextArea.getLineOfOffset(sourceTextArea.viewToModel(new Point(viewportRect.x, (viewportRect.y + viewportRect.height) - 1)));
					int numRowsVisible = bottomRow - topRow;

					// Scroll the window so the beginning of the
					// annotation text will be (approximately) centered.
					int viewLine = Math.max(startLine - (numRowsVisible > 0 ? numRowsVisible / 2 : 0), 0);
					int viewBegin = sourceTextArea.getLineStartOffset(viewLine);
					Rectangle viewRect = sourceTextArea.modelToView(viewBegin);
					viewport.setViewPosition(new Point(viewRect.x, viewRect.y));

					// Select (and highlight) the annotation.
					int selBegin = sourceTextArea.getLineStartOffset(startLine);
					int selEnd = sourceTextArea.getLineStartOffset(endLine);
					sourceTextArea.select(selBegin, selEnd);
					sourceTextArea.getCaret().setSelectionVisible(true);
				} catch (javax.swing.text.BadLocationException e) {
					logger.logMessage(ConsoleLogger.ERROR, e.toString());
				}
			}
		});

		return true;
	}

	/**
	 * Show descriptive text about the type of bug
	 *
	 * @param bugInstance the bug instance
	 */
	private void showBugInfo(BugInstance bugInstance) {
		// Are we already showing details for this kind of bug?
		String bugDetailsKey = bugInstance.getType();
		if (bugDetailsKey.equals(currentBugDetailsKey))
			return;

		// Display the details
		String html = I18N.instance().getDetailHTML(bugDetailsKey);
		bugDescriptionEditorPane.setContentType("text/html");
		bugDescriptionEditorPane.setText(html);
		currentBugDetailsKey = bugDetailsKey;

		// FIXME: unfortunately, using setText() on the editor pane
		// results in the contents being scrolled to the bottom of the pane.
		// An immediate inline call to set the scroll position does nothing.
		// So, use invokeLater(), even though this results in flashing.
		// [What we really need is a way to set the text WITHOUT changing
		// the caret position.  Need to investigate.]
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				bugDescriptionScrollPane.getViewport().setViewPosition(new Point(0, 0));
			}
		});
	}

	/**
	 * Synchronize the bug annotation text with the current bug instance,
	 * and update the annotation text with the new bug instance.
	 *
	 * @param selected the new BugInstance
	 */
	private void synchBugAnnotation(BugInstance selected) {
		if (currentBugInstance != null) {
			String text = annotationTextArea.getText();
			currentBugInstance.setAnnotationText(text);
		}

		annotationTextArea.setText(selected.getAnnotationText());
	}

	/* ----------------------------------------------------------------------
	 * Misc. helpers
	 * ---------------------------------------------------------------------- */

	/**
	 * Show About
	 */
	void about() {
		AboutDialog dialog = new AboutDialog(this, true);
		dialog.setSize(500, 354);
		dialog.setLocationRelativeTo(null); // center the dialog
		dialog.setVisible(true);
	}

	/**
	 * Exit the application.
	 */
	void exitFindBugs() {
		// TODO: offer to save work, etc.
		UserPreferences.getUserPreferences().storeUserDetectorPreferences();
		UserPreferences.getUserPreferences().setUserDetectorThreshold(priorityThreshold);
		UserPreferences.getUserPreferences().write();
		System.exit(0);
	}

	/**
	 * Create a file chooser dialog.
	 * Ensures that the dialog will start in the current directory.
	 *
	 * @return the file chooser
	 */
	private JFileChooser createFileChooser() {
		return new JFileChooser(currentDirectory);
	}

	/**
	 * Create a file chooser dialog.
	 * Ensures that the dialog will start in the current directory.
	 *
	 * @param extraComp The extra component to append to the dialog
	 * @return the file chooser
	 */
	private JFileChooser createFileChooser(final JComponent extraComp) {
		return new JFileChooser(currentDirectory) {
			protected JDialog createDialog(Component parent) throws HeadlessException {
				JDialog dialog = super.createDialog(parent);
				dialog.getContentPane().add(extraComp, BorderLayout.SOUTH);
				dialog.setLocation(300, 200);
				dialog.setResizable(false);
				return dialog;
			}
		};
	}

	/**
	 * Run a file chooser dialog.
	 * If a file is chosen, then the current directory is updated.
	 *
	 * @param dialog      the file chooser dialog
	 * @param dialogTitle the dialog title
	 * @return the outcome
	 */
	private int chooseFile(JFileChooser dialog, String dialogTitle) {
		int outcome = dialog.showDialog(this, dialogTitle);
		return updateCurrentDirectoryFromDialog(dialog, outcome);
	}

	/**
	 * Run a file chooser dialog to choose a file to open.
	 * If a file is chosen, then the current directory is updated.
	 *
	 * @param dialog the file chooser dialog
	 * @return the outcome
	 */
	private int chooseFileToOpen(JFileChooser dialog) {
		int outcome = dialog.showOpenDialog(this);
		return updateCurrentDirectoryFromDialog(dialog, outcome);
	}

	private int updateCurrentDirectoryFromDialog(JFileChooser dialog, int outcome) {
		if (outcome != JFileChooser.CANCEL_OPTION) {
			File selectedFile = dialog.getSelectedFile();
			currentDirectory = selectedFile.getParentFile();
		}
		return outcome;
	}

	/**
	 * Get the ConsoleLogger.
	 */
	public ConsoleLogger getLogger() {
		return logger;
	}

	/**
	 * Show an error dialog.
	 */
	public void error(String message) {
		JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Write a message to the console window.
	 */
	public void writeToConsole(String message) {
		consoleMessageArea.append(message);
		consoleMessageArea.append("\n");
	}

	/**
	 * Fix up the path that is received from JFileChooser, if necessary
	 * Double clicking a directory causes a repeated name, for some reason
	 * such as a:\b\c\c when a:\b\c was chosen
	 */
	public File verifyFileSelection(File pickedFile) {
		if (pickedFile.exists())
			return pickedFile;

		File parent = pickedFile.getParentFile();
		if ((parent != null) && parent.getName().equals(pickedFile.getName()))
			return parent;

		//Something bad has happened
		return pickedFile;
	}

	/* ----------------------------------------------------------------------
	 * main() method
	 * ---------------------------------------------------------------------- */

	/**
	 * Invoke from the command line.
	 *
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		Project project = null;

		for (int i = 0; i < args.length; ++i) {
			String arg = args[i];

			if (arg.equals("-debug")) {
				System.out.println("Setting findbugs.debug=true");
				System.setProperty("findbugs.debug", "true");
			} else if (arg.startsWith("-look:")) {
				arg = arg.substring(6);

				String theme = null;
				if (arg.equals("plastic")) {
					// You can get the Plastic look and feel from jgoodies.com:
					//    http://www.jgoodies.com/downloads/libraries.html
					// Just put "plastic.jar" in the lib directory, right next
					// to the other jar files.
					theme = "com.jgoodies.plaf.plastic.PlasticXPLookAndFeel";
				} else if (arg.equals("gtk")) {
					theme = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
				} else if (arg.equals("native")) {
					theme = UIManager.getSystemLookAndFeelClassName();
				} else {
					System.err.println("Style '" + arg + "' not supported");
				}

				if (theme != null) {
					try {
						UIManager.setLookAndFeel(theme);
					} catch (Exception e) {
						System.err.println("Couldn't load " + arg +
						        " look and feel: " + e.toString());
					}
				}
			} else if (arg.equals("-project")) {
				++i;
				if (i == args.length)
					throw new IllegalArgumentException(arg + " option requires argument");

				String projectFile = args[i];

				try {
					project = new Project();
					project.read(projectFile);
				} catch (IOException e) {
					System.err.println("Couldn't load project: " + e.getMessage());
				}
			}
		}

//      Uncomment one of these to test I18N    
//	    Locale.setDefault( Locale.FRENCH );
//	    Locale.setDefault( Locale.GERMAN );
//	    Locale.setDefault( Locale.JAPANESE );
//	    Locale.setDefault( new Locale( "et" ));
//	    Locale.setDefault( new Locale( "fi" ));
//	    Locale.setDefault( new Locale( "es" ));
//	    Locale.setDefault( new Locale( "pl" ));

		// Load plugins!
		DetectorFactoryCollection.instance();

		FindBugsFrame frame = new FindBugsFrame();

		if (project != null) {
			frame.setProject(project);
		}
		frame.setSize(800, 600);
		frame.setLocationRelativeTo(null); // center the frame
		frame.setVisible(true);
	}

	/* ----------------------------------------------------------------------
	 * Instance variables
	 * ---------------------------------------------------------------------- */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JButton addClasspathEntryButton;
    private javax.swing.JButton addJarButton;
    private javax.swing.JButton addSourceDirButton;
    private javax.swing.JTextArea annotationTextArea;
    private javax.swing.JScrollPane annotationTextAreaScrollPane;
    private javax.swing.JButton browseClasspathEntryButton;
    private javax.swing.JButton browseJarButton;
    private javax.swing.JButton browseSrcDirButton;
    private javax.swing.JEditorPane bugDescriptionEditorPane;
    private javax.swing.JScrollPane bugDescriptionScrollPane;
    private javax.swing.JTabbedPane bugDetailsTabbedPane;
    private javax.swing.JEditorPane bugSummaryEditorPane;
    private javax.swing.JSplitPane bugTreeBugDetailsSplitter;
    private javax.swing.JPanel bugTreePanel;
    private javax.swing.JTree byBugTypeBugTree;
    private javax.swing.JScrollPane byBugTypeScrollPane;
    private javax.swing.JTree byClassBugTree;
    private javax.swing.JScrollPane byClassScrollPane;
    private javax.swing.JTree byPackageBugTree;
    private javax.swing.JScrollPane byPackageScrollPane;
    private javax.swing.JScrollPane bySummary;
    private javax.swing.JButton classpathDownButton;
    private javax.swing.JLabel classpathEntryLabel;
    private javax.swing.JList classpathEntryList;
    private javax.swing.JLabel classpathEntryListLabel;
    private javax.swing.JScrollPane classpathEntryListScrollPane;
    private javax.swing.JTextField classpathEntryTextField;
    private javax.swing.JButton classpathUpButton;
    private javax.swing.JMenuItem closeProjectItem;
    private javax.swing.JMenuItem configureDetectorsItem;
    private javax.swing.JTextArea consoleMessageArea;
    private javax.swing.JScrollPane consoleScrollPane;
    private javax.swing.JSplitPane consoleSplitter;
    private javax.swing.JMenuItem copyItem;
    private javax.swing.JMenuItem cutItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JLabel editProjectLabel;
    private javax.swing.JPanel editProjectPanel;
    private javax.swing.JPanel emptyPanel;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JButton findBugsButton;
    private javax.swing.JCheckBoxMenuItem fullDescriptionsItem;
    private javax.swing.JTabbedPane groupByTabbedPane;
    private javax.swing.JLabel growBoxSpacer;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JRadioButtonMenuItem highPriorityButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JSeparator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSeparator jSeparator9;
    private javax.swing.JLabel jarFileLabel;
    private javax.swing.JList jarFileList;
    private javax.swing.JLabel jarFileListLabel;
    private javax.swing.JScrollPane jarFileListScrollPane;
    private javax.swing.JTextField jarNameTextField;
    private javax.swing.JMenuItem loadBugsItem;
    private javax.swing.JLabel logoLabel;
    private javax.swing.JRadioButtonMenuItem lowPriorityButton;
    private javax.swing.JRadioButtonMenuItem mediumPriorityButton;
    private javax.swing.JMenuItem newProjectItem;
    private javax.swing.JMenuItem openProjectItem;
    private javax.swing.JMenuItem pasteItem;
    private javax.swing.JMenu recentProjectsMenu;
    private javax.swing.JMenuItem reloadProjectItem;
    private javax.swing.JButton removeClasspathEntryButton;
    private javax.swing.JButton removeJarButton;
    private javax.swing.JButton removeSrcDirButton;
    private javax.swing.JPanel reportPanel;
    private javax.swing.JMenuItem saveBugsItem;
    private javax.swing.JMenuItem saveProjectAsItem;
    private javax.swing.JMenuItem saveProjectItem;
    private javax.swing.JMenuItem selectAllItem;
    private javax.swing.JMenu settingsMenu;
    private javax.swing.JLabel sourceDirLabel;
    private javax.swing.JList sourceDirList;
    private javax.swing.JLabel sourceDirListLabel;
    private javax.swing.JScrollPane sourceDirListScrollPane;
    private javax.swing.JButton sourceDownButton;
    private javax.swing.JTextArea sourceTextArea;
    private javax.swing.JScrollPane sourceTextAreaScrollPane;
    private javax.swing.JButton sourceUpButton;
    private javax.swing.JTextField srcDirTextField;
    private javax.swing.JMenuBar theMenuBar;
    private javax.swing.JLabel urlLabel;
    private javax.swing.JCheckBoxMenuItem viewBugDetailsItem;
    private javax.swing.JRadioButtonMenuItem viewBugsItem;
    private javax.swing.JCheckBoxMenuItem viewConsoleItem;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JPanel viewPanel;
    private javax.swing.JRadioButtonMenuItem viewProjectItem;
    // End of variables declaration//GEN-END:variables

	// My variable declarations
	private ConsoleLogger logger;
	private CardLayout viewPanelLayout;
	private String currentView;
	private File currentDirectory;
	private Project currentProject;
	private JTree[] bugTreeList;
	private AnalysisRun currentAnalysisRun;
	private SourceFinder sourceFinder = new SourceFinder();
	private BugInstance currentBugInstance; // be lazy in switching bug instance details
	private SourceLineAnnotation currentSourceLineAnnotation; // as above
	private String currentBugDetailsKey;
	private int priorityThreshold;

	// My constant declarations
	private final static boolean MAC_OS_X = System.getProperty("os.name").toLowerCase().startsWith("mac os x");
	private final static int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

}
