/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Filter;
import edu.umd.cs.findbugs.filter.Matcher;

/**
 * Allows you to make a new Filter by right clicking (control clicking) on a bug in the tree
 */
@SuppressWarnings("serial")
public class NewFilterFromBug extends FBDialog
{
	private HashMap<JCheckBox, Sortables> map = new HashMap<JCheckBox, Sortables>();
	static List<NewFilterFromBug> listOfAllFrames=new ArrayList<NewFilterFromBug>();

	public NewFilterFromBug(final BugInstance bug)
	{
		this.setModal(true);
		listOfAllFrames.add(this);
		setLayout(new BorderLayout());
		add(new JLabel("Filter out all bugs whose..."), BorderLayout.NORTH);

		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
		for (Sortables s : Sortables.values())
		{
			if (s.equals(Sortables.DIVIDER))
				continue;
			JCheckBox radio = new JCheckBox(s.toString() + " is " + s.formatValue(s.getFrom(bug)));
			
			map.put(radio, s);
			center.add(radio);
		}
		add(center, BorderLayout.CENTER);

		JPanel south = new JPanel();
		JButton okButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.ok_btn", "OK"));
		okButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				HashSet<Sortables> set = new HashSet<Sortables>();
				for(Map.Entry<JCheckBox,Sortables> e : map.entrySet()) {
					if (e.getKey().isSelected()) set.add(e.getValue());
				}
				if (!set.isEmpty() )
				{
					Matcher m = FilterFactory.makeMatcher(set, bug);
					Filter f = MainFrame.getInstance().getProject().getSuppressionFilter();
					
					f.addChild(m);

					PreferencesFrame.getInstance().updateFilterPanel();
					FilterActivity.notifyListeners(FilterListener.Action.FILTERING, null);
					NewFilterFromBug.this.dispose();
				}
			}
		});
		JButton cancelButton = new JButton(edu.umd.cs.findbugs.L10N.getLocalString("dlg.cancel_btn", "Cancel"));
		cancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent evt)
			{
				NewFilterFromBug.this.dispose();
			}
		});
		Util.addOkAndCancelButtons(south, okButton, cancelButton);
		add(south, BorderLayout.SOUTH);

		pack();
		setVisible(true);
	}

	static void closeAll()
	{
		for(NewFilterFromBug frame: listOfAllFrames)
			frame.dispose();
		listOfAllFrames.clear();
	}
}
