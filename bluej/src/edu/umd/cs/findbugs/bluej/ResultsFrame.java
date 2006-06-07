package edu.umd.cs.findbugs.bluej;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import bluej.extensions.BProject;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.extensions.editor.Editor;
import bluej.extensions.editor.TextLocation;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;

@SuppressWarnings("serial")
public class ResultsFrame extends JFrame
{
	private static final String[] columnNames = {"File", "Line", "Description"};
	private static final int[] columnWidths = {150, 50, 475};
	
	private JEditorPane description;
	
	private BProject currProject;
	
	public ResultsFrame(final SortedBugCollection bugs, BProject project)
	{
		currProject = project;
		final ArrayList<BugInstance> bugList = new ArrayList<BugInstance>(bugs.getCollection());
		
		final JTable table = new JTable(new MyTableModel(bugList));
		
		for (int i = 0; i < columnNames.length; i++)
			table.getColumn(columnNames[i]).setPreferredWidth(columnWidths[i]);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent evt)
			{
				description.setText(bugList.get(table.getSelectedRow()).getBugPattern().getDetailHTML());
			}	
		});
		
		JScrollPane topScroll = new JScrollPane(table);
		topScroll.setPreferredSize(new Dimension(675, 275));
			
		
		description = new JEditorPane();
		description.setContentType("text/html");
		JScrollPane bottomScroll = new JScrollPane(description);
		bottomScroll.setPreferredSize(new Dimension(675, 100));
		
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topScroll, bottomScroll);
		setContentPane(splitPane);
		
		JButton showCode = new JButton("Show Code");
		add(showCode);
		showCode.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				showEditorAndHighlight(bugList.get(table.getSelectedRow()));
			}
		});
		
		pack();
		setDefaultCloseOperation(HIDE_ON_CLOSE);
	}
	
	/*
	 * Finds the editor that corresponds with the class the bug in the BugInstance
	 * is in. Opens the editor and then highlights the bug and places the cursor 
	 * at the beginning.
	 */
	private void showEditorAndHighlight(BugInstance instance) {
		SourceLineAnnotation srcLine = instance.getPrimarySourceLineAnnotation();
		try {
			Editor srcEditor = currProject.getPackage(srcLine.getPackageName()).getBClass(getClassName(srcLine)).getEditor();
			srcEditor.setVisible(true);
			
			//srcStartLine and srcEndLine created in case returned -1
			int srcStartLine = srcLine.getStartLine();
			int srcEndLine = srcLine.getEndLine();
			
			if(srcStartLine < 0)
				srcStartLine++;
			
			if(srcEndLine < 0)
				srcEndLine++;
			
			srcEditor.setCaretLocation(new TextLocation(srcStartLine, 0));
			srcEditor.setSelection(new TextLocation(srcStartLine, 0), new TextLocation(srcEndLine, 0));
		} catch (ProjectNotOpenException e) {
			Log.recordBug(e);
		} catch (PackageNotFoundException e) {
			Log.recordBug(e);
		}
	}
	
	/*
	 * Gets the source file and gets the name of the class from that.
	 */
	private String getClassName(SourceLineAnnotation srcLine) {
		String str = srcLine.getSourceFile();
		return str.substring(0, str.indexOf("."));
	}
	
//	private ArrayList<String[]> parseBugInstances(SortedBugCollection bugs)
//	{
//		ArrayList<String[]> result = new ArrayList<String[]>();
//		Iterator<BugInstance> i = bugs.iterator();
//		while (i.hasNext())
//		{
//			BugInstance bug = i.next();
//			result.add(new String[3]);
//			result.get(result.size() - 1)[0] = bug.getPrimarySourceLineAnnotation().getSourceFile();
//			result.get(result.size() - 1)[1] = String.valueOf(bug.getPrimarySourceLineAnnotation().getStartLine());
//			result.get(result.size() - 1)[2] = bug.getMessageWithoutPrefix();
//		}
//		return result;
//	}
	
	private class MyTableModel extends AbstractTableModel
	{
		private ArrayList<BugInstance> bugList;
		
		public MyTableModel(ArrayList<BugInstance> bugList)
		{
			this.bugList = bugList;
		}
		
		public int getRowCount()
		{
			return bugList.size();
		}

		public int getColumnCount()
		{
			return 3;
		}

		@Override
		public String getColumnName(int column)
		{
			return columnNames[column];
		}
		
		public Object getValueAt(int row, int column)
		{
			switch (column)
			{
			case 0:
				return bugList.get(row).getPrimarySourceLineAnnotation().getSourceFile();
			case 1:
				int line = bugList.get(row).getPrimarySourceLineAnnotation().getStartLine();
				return (line != -1 ? String.valueOf(line) : "");
			case 2:
				return bugList.get(row).getMessageWithoutPrefix();
			default:
				throw new ArrayIndexOutOfBoundsException("Column " + column + " must be < 3");
			}
		}
		
	}
}
