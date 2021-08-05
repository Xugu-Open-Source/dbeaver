/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.edit.UserManager;
import org.jkiss.dbeaver.ext.xugu.edit.UserPropertyHandler;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Role;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.ext.xugu.model.UserAuthority;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;

import com.alibaba.druid.sql.visitor.functions.Isnull;

import org.jkiss.dbeaver.model.impl.edit.DBECommandAdapter;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.UIUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 用户编辑器衍生类
 * 
 * @author Xugu
 */
public class UserEditorGeneral extends BaseUserEditor {
	private PageControl pageControl;
	private boolean isLoaded;
	private boolean newUser;
	private Text userNameText;
	private Text passwordText;
	private Text confirmText;

	Collection<UserAuthority> authorities;
	ArrayList<String> databaseAuthoritiesNames;
	ArrayList<String> objectAuthoritiesNames;
	ArrayList<String> subObjectAuthoritiesNames;

	private org.eclipse.swt.widgets.List roleList;
	private Combo roleCombo;
	private Button addRole;
	private Button removeRole;
	private Button lockCheck;
	private Button expireCheck;
	private Text timeText;
	private String userName = "";
	private String password = "";
	private String untilTime = "";
	private boolean lockFlag;
	private boolean expireFlag;
	private CommandListener commandlistener;

	@Override
	public void createPartControl(Composite parent) {
		// 容器及布局
		pageControl = new PageControl(parent);
		Composite container = UIUtils.createPlaceholder(pageControl, 4, 5);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		container.setLayoutData(gd);
		container.setSize(600, 400);

		newUser = !getDatabaseObject().isPersisted();
		CTabFolder cf1 = new CTabFolder(container, 0);
		CTabItem ti1 = new CTabItem(cf1, 1);
		CTabItem ti2 = new CTabItem(cf1, 2);
		CTabItem ti3 = new CTabItem(cf1, 3);
		
		Composite userGroup = UIUtils.createControlGroup(cf1, "", 2,	
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 400);
		userGroup.setSize(400, 400);
		Composite subUserGroupLeft = UIUtils.createControlGroup(userGroup,
				Messages.editors_user_editor_general_label_user_properties_title, 2,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 200);
		Composite subUserGroupRight = UIUtils.createControlGroup(userGroup,
				Messages.editors_user_editor_general_label_role_manage_title, 1,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 200);
		
		Composite userGroup2 = UIUtils.createControlGroup(cf1, Messages.editors_authority_editor_database_title, 1,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 300);
		userGroup2.setSize(200, 200);
		Composite userGroup3 = UIUtils.createControlGroup(cf1, Messages.editors_authority_editor_object_title, 2,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 400);
		userGroup3.setSize(200, 200);
		ti1.setControl(userGroup);
		ti1.setText(Messages.editors_user_editor_general_label_user_properties_title);
		ti2.setControl(userGroup2);
		ti2.setText(Messages.editors_authority_editor_database_title);
		ti3.setControl(userGroup3);
		ti3.setText(Messages.editors_authority_editor_object_title);
		cf1.setSelection(0);
		// 创建新用户时使用默认数据 修改用户时则使用当前用户数据 对密码做特殊处理
		//
		password = newUser ? "" : getDatabaseObject().getPassword();
		userName = newUser ? "" : getDatabaseObject().getName();
		untilTime = newUser ? Constants.DEF_UNTIL_TIME
				: getDatabaseObject().getUntilTime() == null ? "" : getDatabaseObject().getUntilTime().toString();
		lockFlag = newUser ? false : getDatabaseObject().isLocked();
		expireFlag = newUser ? false : getDatabaseObject().isExpired();

		userNameText = UIUtils.createLabelText(subUserGroupLeft, Messages.editors_user_editor_general_label_user_name,
				userName);
		ControlPropertyCommandListener.create(this, userNameText, UserPropertyHandler.NAME);

		passwordText = UIUtils.createLabelText(subUserGroupLeft, Messages.editors_user_editor_general_label_password,
				password, SWT.BORDER | SWT.PASSWORD);
		ControlPropertyCommandListener.create(this, passwordText, UserPropertyHandler.PASSWORD);

		confirmText = UIUtils.createLabelText(subUserGroupLeft, Messages.editors_user_editor_general_label_confirm,
				password, SWT.BORDER | SWT.PASSWORD);
		ControlPropertyCommandListener.create(this, confirmText, UserPropertyHandler.PASSWORD_CONFIRM);

		lockCheck = UIUtils.createLabelCheckbox(subUserGroupLeft, Messages.editors_user_editor_general_label_locked,
				lockFlag);
		ControlPropertyCommandListener.create(this, lockCheck, UserPropertyHandler.LOCKED);

		expireCheck = UIUtils.createLabelCheckbox(subUserGroupLeft,
				Messages.editors_user_editor_general_label_pwd_expired, expireFlag);
		ControlPropertyCommandListener.create(this, expireCheck, UserPropertyHandler.EXPIRED);

		timeText = UIUtils.createLabelText(subUserGroupLeft, Messages.editors_user_editor_general_label_valid_until,
				untilTime);
		ControlPropertyCommandListener.create(this, timeText, UserPropertyHandler.UNTIL_TIME);

		roleCombo = UIUtils.createLabelCombo(subUserGroupRight, Messages.editors_user_editor_general_label_role_list,
				0);
		addRole = UIUtils.createPushButton(subUserGroupRight, Messages.editors_user_editor_general_label_add_role,
				null);
		addRole.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removeRole = UIUtils.createPushButton(subUserGroupRight, Messages.editors_user_editor_general_label_remove_role,
				null);
		removeRole.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// 不允许手动修改角色列表文本框
		roleList = new org.eclipse.swt.widgets.List(subUserGroupRight, SWT.V_SCROLL | SWT.MULTI);
		ControlPropertyCommandListener.create(this, roleList, UserPropertyHandler.ROLE_LIST);
		roleList.setLayoutData(new GridData(370, 150));
		roleList.setEnabled(false);

		List<User> userList = User.users;
		// 加载用户当前的角色信息
		if (userList.size() > 0) {
			String userNameString = getDatabaseObject().getName();
			for (int i = 0; i < userList.size(); i++) {
				if (userList.get(i).getName().equals(userNameString)) {
					String[] roleStrings = userList.get(i).getRoleList().split(",");
					for (String string : roleStrings) {
						if (string != "") {
							roleList.add(string);
						}
					}
				}
			}
		}
		// 加载全部角色信息
		if (User.roleNames.size() > 0) {
			for (String roleName : User.roleNames) {
				roleCombo.add(roleName);
			}
		}
//			String[] choosenRoleList = getDatabaseObject().getRoleList().split(",");
//			for (int i = 0; i < choosenRoleList.length; i++) {
//				roleList.add(choosenRoleList[i]);
//			}
		// 加载全部可选的角色信息
//		if (UserManager.getRolesList() != null) {
//			Collection<Role> rolesList = UserManager.getRolesList();
//			if (roles != null && !"".equals(roles[0])) {
//				for (int i = 0; i < roles.length; i++) {
//					roleCombo.add(roles[i]);
//				}
//			}
//			Iterator<Role> iterator = rolesList.iterator();
//			while(iterator.hasNext()) {
//				roleCombo.add(iterator.next().getName());
//			}
//		}

		addRole.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String newRole = roleCombo.getText();
				// 屏蔽空选项和空串情况
				if (newRole != null && newRole.length() != 0) {
					// 先判断list文本框中是否已有，已有则不添加
					String[] nowItems = roleList.getItems();
					boolean hasItem = false;
					for (int i = 0, l = nowItems.length; i < l; i++) {
						if (nowItems[i].equals(newRole)) {
							hasItem = true;
							break;
						}
					}
					if (!hasItem) {
						roleList.add(newRole);
					}
					// 全部选中
					roleList.selectAll();
					// 激活修改监听
					roleList.notifyListeners(SWT.Modify, null);
					roleList.deselectAll();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		});
		removeRole.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String oldRole = roleCombo.getText();
				// 屏蔽空和空串情况
				if (oldRole != null && oldRole.length() != 0) {
					// 先判断list文本框中是否已有，已有则不添加
					int index = roleList.indexOf(oldRole);
//					if (index != -1) {
//						roleList.remove(index);
//					}
					roleList.remove(oldRole);
					// 全部选中
					roleList.selectAll();
					// 激活修改监听
					roleList.notifyListeners(SWT.Modify, null);
					roleList.deselectAll();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		});

		// 暂时禁止修改用户锁定及口令失效 无论是否新建用户(check类型数据无法被PropertyHandler识别)
		lockCheck.setEnabled(false);
		expireCheck.setEnabled(false);

		// 权限处理
		{
			// 加载用户中的权限信息并分为库级权限和对象级权限两类 对象权限又分为两个级别
			authorities = getDatabaseObject().getUserAuthorities();
			databaseAuthoritiesNames = new ArrayList<>();
			objectAuthoritiesNames = new ArrayList<>();
			subObjectAuthoritiesNames = new ArrayList<>();
			AuthorityEditor baseEditor = new AuthorityEditor(userGroup2, userGroup3, 1);
			baseEditor.setUserEditor(this);
			// 库级权限
			Collection<UserAuthority> dataBaseAuthorities = getDatabaseObject().getUserDatabaseAuthorities();
			// 对象级权限
			Collection<UserAuthority> objectAuthorities = getDatabaseObject().getUserObjectAuthorities();
			// 对象二级权限
			Collection<UserAuthority> subObjectAuthorities = getDatabaseObject().getUserSubObjectAuthorities();

			if (dataBaseAuthorities != null) {
				Iterator<UserAuthority> it = dataBaseAuthorities.iterator();
				UserAuthority authority;
				while (it.hasNext()) {
					authority = it.next();
					databaseAuthoritiesNames.add(authority.getName());
				}
			}
			if (objectAuthorities != null) {
				Iterator<UserAuthority> it = objectAuthorities.iterator();
				UserAuthority authority;
				while (it.hasNext()) {
					authority = it.next();
					objectAuthoritiesNames.add(authority.getName());
				}
			}
			if (subObjectAuthorities != null) {
				Iterator<UserAuthority> it = subObjectAuthorities.iterator();
				UserAuthority authority;
				while (it.hasNext()) {
					authority = it.next();
					subObjectAuthoritiesNames.add(authority.getName());
				}
			}

//			if (authorities != null) {
//				Iterator<UserAuthority> it = authorities.iterator();
//				UserAuthority authority;
//				while (it.hasNext()) {
//					authority = it.next();
//					if (authority.isDatabase()) {
//						databaseAuthorities.add(authority.getName());
//					} else {
//						objectAuthorities.add(authority.getName());
//					}
//				}
//			}
			baseEditor.loadDatabaseAuthorities(databaseAuthoritiesNames, objectAuthoritiesNames,
					subObjectAuthoritiesNames);
			baseEditor.loadDatabaseAuthorityView();
			baseEditor.loadObjectAuthorityView(getDatabaseObject());
		}

		pageControl.createProgressPanel();

		commandlistener = new CommandListener();
		getEditorInput().getCommandContext().addCommandListener(commandlistener);
	}

	@Override
	public void dispose() {
		if (commandlistener != null) {
			getEditorInput().getCommandContext().removeCommandListener(commandlistener);
		}
		super.dispose();
	}

	public void listNotify(org.eclipse.swt.widgets.List target) {
		target.notifyListeners(SWT.Modify, null);
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
				// TODO 数据库加载服务评估
				return null;
			}
		}, pageControl.createLoadVisualizer()).schedule();
	}

	@Override
	protected PageControl getPageControl() {
		return pageControl;
	}

	@Override
	public void refreshPart(Object source, boolean force) {
		// TODO 刷新编辑区
	}

	private class PageControl extends UserPageControl {
		public PageControl(Composite parent) {
			super(parent);
		}

		public ProgressVisualizer<List<String>> createLoadVisualizer() {
			return new ProgressVisualizer<List<String>>() {
				@Override
				public void completeLoading(List<String> privs) {
					super.completeLoading(privs);
				}
			};
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
						userNameText.setEditable(true);
						passwordText.setEditable(true);
					}
				});
			}
		}
	}
}
