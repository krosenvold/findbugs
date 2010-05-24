/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.properties;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * @author Andrei Loskutov
 */
public class ReportConfigurationTab extends Composite {

	private final FindbugsPropertyPage propertyPage;
	private List<Button> chkEnableBugCategoryList;
	private Scale minRankSlider;
	private Label rankValueLabel;

	/**
	 * @param parent
	 * @param style
	 */
	public ReportConfigurationTab(TabFolder parent, FindbugsPropertyPage page, int style) {
		super(parent, style);
		this.propertyPage = page;
		setLayout(new GridLayout());

		TabItem tabDetector = new TabItem(parent, SWT.NONE);
		tabDetector.setText(getMessage("property.reportConfigurationTab"));
		tabDetector.setControl(this);
		tabDetector.setToolTipText("Configure bugs reported to the UI");

		createRankGroup(this);
		createBugCategoriesGroup(this, page.getProject());
	}

	private void createRankGroup(ReportConfigurationTab parent) {
		Composite prioGroup = new Composite(parent, SWT.NONE);
		prioGroup.setLayout(new GridLayout(2, false));

		Label minRankLabel = new Label(prioGroup, SWT.NONE);
		minRankLabel.setText(getMessage("property.minRank")
				+ System.getProperty("line.separator")
				+ getMessage("property.minRank.line2"));
		minRankLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		minRankSlider = new Scale(prioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		minRankSlider.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
		minRankSlider.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int rank = minRankSlider.getSelection();
				getCurrentProps().getFilterSettings().setMinRank(rank);
				updateRankValueLabel();
			}
		});
		minRankSlider.setMinimum(0);
		minRankSlider.setMaximum(20);
		minRankSlider.setSelection(getCurrentProps().getFilterSettings().getMinRank());
		minRankSlider.setIncrement(1);
		minRankSlider.setPageIncrement(5);
		Label dummyLabel = new Label(prioGroup, SWT.NONE);
		dummyLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		rankValueLabel = new Label(prioGroup, SWT.NONE);
		rankValueLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		updateRankValueLabel();
	}

	private void updateRankValueLabel() {
		String label;
		int rank = minRankSlider.getSelection();
		if (rank < 5) {
			label = "Scariest";
		} else if (rank < 10) {
			label = "Scary";
		} else if (rank < 15) {
			label = "Troubling";
		} else {
			label = "Possible";
		}
		rankValueLabel.setText(rank + " (" + label + ")");
	}

	/**
	 * Helper method to shorten message access
	 * @param key a message key
	 * @return requested message
	 */
	protected String getMessage(String key) {
		return FindbugsPlugin.getDefault().getMessage(key);
	}


	/**
	 * Build list of bug categories to be enabled or disabled.
	 * Populates chkEnableBugCategoryList and bugCategoryList fields.
	 *
	 * @param parent control checkboxes should be added to
	 * @param project       the project being configured
	 */
	private void createBugCategoriesGroup(Composite parent, final IProject project) {
		Group checkBoxGroup = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		checkBoxGroup.setText(getMessage("property.categoriesGroup"));
		checkBoxGroup.setLayout(new GridLayout(1, true));

		List<String> bugCategoryList = new LinkedList<String>(I18N.instance().getBugCategories());
		chkEnableBugCategoryList = new LinkedList<Button>();
		ProjectFilterSettings origFilterSettings = propertyPage
				.getOriginalUserPreferences().getFilterSettings();
		for (String category: bugCategoryList) {
			Button checkBox = new Button(checkBoxGroup, SWT.CHECK);
			checkBox.setText(I18N.instance().getBugCategoryDescription(category));
			checkBox.setSelection(origFilterSettings.containsCategory(category));
			GridData layoutData = new GridData();
			layoutData.horizontalIndent = 10;
			checkBox.setLayoutData(layoutData);

			// Every time a checkbox is clicked, rebuild the detector factory table
			// to show only relevant entries
			checkBox.addListener(SWT.Selection,
				new Listener(){
					public void handleEvent(Event e){
						syncSelectedCategories();
					}
				}
			);
			checkBox.setData(category);
			chkEnableBugCategoryList.add(checkBox);
		}
	}

	/**
	 * Synchronize selected bug category checkboxes with the current user preferences.
	 */
	protected void syncSelectedCategories() {
		ProjectFilterSettings filterSettings = getCurrentProps().getFilterSettings();
		for (Button checkBox: chkEnableBugCategoryList) {
			String category = (String) checkBox.getData();
			if (checkBox.getSelection()) {
				filterSettings.addCategory(category);
			} else {
				filterSettings.removeCategory(category);
			}
		}
		propertyPage.getVisibleDetectors().clear();
	}

	/**
	 * @return
	 */
	protected UserPreferences getCurrentProps() {
		return propertyPage.getCurrentUserPreferences();
	}

	@Override
	public void setEnabled(boolean enabled) {
		minRankSlider.setEnabled(enabled);
		for (Button checkBox : chkEnableBugCategoryList) {
			checkBox.setEnabled(enabled);
		}
		super.setEnabled(enabled);
	}

	void refreshUI(UserPreferences prefs) {
		ProjectFilterSettings filterSettings = prefs.getFilterSettings();
		minRankSlider.setSelection(filterSettings.getMinRank());
		for (Button checkBox: chkEnableBugCategoryList) {
			checkBox.setSelection(filterSettings.containsCategory((String) checkBox.getData()));
		}
		syncSelectedCategories();
	}

	protected List<Button> getChkEnableBugCategoryList() {
		return chkEnableBugCategoryList;
	}
}


