package edu.umd.cs.findbugs.bluej.test;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;

import bluej.extensions.BClass;
import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.ExtensionException;
import bluej.extensions.MenuGenerator;

public class MenuBuilder extends MenuGenerator
{
	private BlueJ bluej;

	public MenuBuilder(BlueJ bluej)
	{
		this.bluej = bluej;
	}

	/**
	 * @see bluej.extensions.MenuGenerator#getToolsMenuItem(bluej.extensions.BPackage)
	 */
	@Override
	public JMenuItem getToolsMenuItem(BPackage pckg)
	{
		JCheckBoxMenuItem result = new JCheckBoxMenuItem("CheckBox Something");
		//JCheckBoxMenuItem result = new JCheckBoxMenuItem(new MenuAction());
		//result.setText("CheckBox Something");
		return result;
	}

	@SuppressWarnings("serial")
	class MenuAction extends AbstractAction
	{
		/**
		 * Called when our menu entry is clicked. Pop a dialog with a cheery
		 * message, followed by a list of all classes in all packages in all
		 * open projects. (Mostly to see how BlueJ handles weird class
		 * structures.
		 */
		public void actionPerformed(ActionEvent evt)
		{
			try
			{
				StringBuilder result = new StringBuilder();
				
				 for (BProject i : bluej.getOpenProjects())
					for (BPackage j : i.getPackages())
						for (BClass k : j.getClasses())
						{
							result.append(k.getName() + "\n");
							for (Class l : k.getJavaClass()
									.getDeclaredClasses())
								result.append(l.getName() + "\n");
						}
				 
/* Old method: return everything that ends in .class */
//				FilenameFilter filter = new FilenameFilter()
//				{
//					public boolean accept(File dir, String name)
//					{
//						return name.toLowerCase().endsWith(".class");
//					}
//				};
//				for (BProject i : bluej.getOpenProjects())
//					for (BPackage j : i.getPackages())
//						for (File k : j.getDir().listFiles(filter))
//						{
//							String className = k.getName().substring(0, k.getName().length() - ".class".length());
//							result.append(className + "\n");
//						}

				JOptionPane.showMessageDialog(null, "Hello, World!\n\nClasses:\n" + result.toString());
			}
			/* ProjectNotOpenException, PackageNotFoundException, and
			 * ClassNotFoundException are checked exceptions and must be caught,
			 * although none of these should ever be thrown from this code,
			 * since all objects returned by getOpenProjects(), getPackages(),
			 * and getClasses() should be fine.
			 */
			catch (ExtensionException e)
			{
				StringBuilder msg = new StringBuilder();
				msg.append(e.getClass().getName() + ": " + e.getMessage() + "\n");
				for (StackTraceElement i : e.getStackTrace())
					msg.append(i + "\n");
				JOptionPane.showMessageDialog(null, msg);
			}
		}
	}
}
