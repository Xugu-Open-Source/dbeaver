/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.xugu.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.model.Role;
import org.jkiss.dbeaver.ext.xugu.model.RoleAuthority;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;

/**
 * 角色编辑器
 */
public class RoleEditor extends AbstractDatabaseObjectEditor<Role> {

	private PageControl pageControl;
	private boolean isLoaded;
	private boolean newUser;
	private Text roleNameText;

	Collection<RoleAuthority> dataAuthorities;
	Collection<RoleAuthority> objAuthorities;
	Collection<RoleAuthority> subObjAuthorities;
	ArrayList<String> databaseAuthorities;
	ArrayList<String> objectAuthorities;
	ArrayList<String> subObjectAuthorities;
	private CommandListener commandlistener;

	@Override
	public void createPartControl(Composite parent) {

		pageControl = new PageControl(parent);
		Composite container = UIUtils.createPlaceholder(pageControl, 4, 5);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		container.setLayoutData(gd);
		container.setSize(400, 300);

		CTabFolder cf1 = new CTabFolder(container, 0);
		CTabItem ti1 = new CTabItem(cf1, 1);
		CTabItem ti2 = new CTabItem(cf1, 2);
		CTabItem ti3 = new CTabItem(cf1, 3);
		Composite roleGroup = UIUtils.createControlGroup(cf1,
				Messages.editors_role_editor_general_label_role_properties_title, 2,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 400);
		roleGroup.setSize(200, 200);
		Composite roleGroup2 = UIUtils.createControlGroup(cf1, Messages.editors_authority_editor_database_title, 1,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 400);
		roleGroup2.setSize(200, 200);
		Composite roleGroup3 = UIUtils.createControlGroup(cf1, Messages.editors_authority_editor_object_title, 2,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 400);
		roleGroup3.setSize(860, 250);

		ti1.setControl(roleGroup);
		ti1.setText(Messages.editors_role_editor_general_label_role_properties_title);
		ti2.setControl(roleGroup2);
		ti2.setText(Messages.editors_authority_editor_database_title);
		ti3.setControl(roleGroup3);
		ti3.setText(Messages.editors_authority_editor_object_title);
		cf1.setSelection(0);

		roleNameText = UIUtils.createLabelText(roleGroup, Messages.dialog_role_name, getDatabaseObject().getName());
		roleNameText.setEditable(false);

		// 权限处理
		{
			// 加载用户中的权限信息并分为库级权限和对象级权限两类 对象权限又分为两个级别
			dataAuthorities = getDatabaseObject().getRoleDatabaseAuthorities();
			objAuthorities = getDatabaseObject().getRoleObjectAuthorities();
			subObjAuthorities = getDatabaseObject().getRoleSubObjectAuthorities();
			databaseAuthorities = new ArrayList<>();
			objectAuthorities = new ArrayList<>();
			subObjectAuthorities = new ArrayList<>();

			AuthorityEditor baseEditor = new AuthorityEditor(roleGroup2, roleGroup3, 2);
			baseEditor.setRoleEditor(this);
			if (dataAuthorities != null) {
				Iterator<RoleAuthority> it = dataAuthorities.iterator();
				RoleAuthority authority;
				while (it.hasNext()) {
					authority = it.next();
					databaseAuthorities.add(authority.getName());
				}
			}
			
			if (objAuthorities != null) {
				Iterator<RoleAuthority> it = objAuthorities.iterator();
				RoleAuthority authority;
				while (it.hasNext()) {
					authority = it.next();
					objectAuthorities.add(authority.getName());
				}
			}
			
			if (subObjAuthorities != null) {
				Iterator<RoleAuthority> it = subObjAuthorities.iterator();
				RoleAuthority authority;
				while (it.hasNext()) {
					authority = it.next();
					subObjectAuthorities.add(authority.getName());
				}
			}
			
			baseEditor.loadDatabaseAuthorities(databaseAuthorities, objectAuthorities, subObjectAuthorities);
			baseEditor.loadDatabaseAuthorityView();
			baseEditor.loadObjectAuthorityView(getDatabaseObject());
		}

		pageControl.createProgressPanel();

		commandlistener = new CommandListener();
		getEditorInput().getCommandContext().addCommandListener(commandlistener);
	}

	public void listNotify(org.eclipse.swt.widgets.List target) {
		target.notifyListeners(SWT.Modify, null);
	}

	@Override
	public void dispose() {
		if (commandlistener != null) {
			getEditorInput().getCommandContext().removeCommandListener(commandlistener);
		}
		super.dispose();
	}

	@Override
	public void activatePart() {
		if (isLoaded) {
			return;
		}
		isLoaded = true;

		LoadingJob.createService(new DatabaseLoadService<List<String>>("test", getExecutionContext()) {
			@Override
			public List<String> evaluate(DBRProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
				return null;
			}
		}, pageControl.createLoadVisualizer()).schedule();
	}

	protected PageControl getPageControl() {
		return pageControl;
	}

	@Override
	public RefreshResult refreshPart(Object source, boolean force) {
		// do nothing
		return RefreshResult.IGNORED;
	}

	private class PageControl extends ObjectEditorPageControl {
		public PageControl(Composite parent) {
			super(parent, SWT.NONE, RoleEditor.this);
		}

		public ProgressVisualizer<List<String>> createLoadVisualizer() {
			return new ProgressVisualizer<List<String>>() {
				@Override
				public void completeLoading(List<String> privs) {
					super.completeLoading(privs);
				}
			};
		}

		@Override
		public void fillCustomActions(IContributionManager contributionManager) {
			super.fillCustomActions(contributionManager);
			DatabaseEditorUtils.contributeStandardEditorActions(getSite(), contributionManager);
		}
	}

	private class CommandListener extends DBECommandAdapter {
		@Override
		public void onSave() {
			if (newUser && getDatabaseObject().isPersisted()) {
				newUser = false;
				UIUtils.asyncExec(new Runnable() {
					@Override
					public void run() {
					}
				});
			}
		}
	}

	@Override
	public void setFocus() {
		// TODO 设置聚焦
	}

}
