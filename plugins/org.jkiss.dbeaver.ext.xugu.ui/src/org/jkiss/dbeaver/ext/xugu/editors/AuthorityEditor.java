/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.edit.RolePropertyHandler;
import org.jkiss.dbeaver.ext.xugu.edit.UserPropertyHandler;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.ext.xugu.model.BaseGlobalObject;
import org.jkiss.dbeaver.ext.xugu.model.Role;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.ControlPropertyCommandListener;

/**
 * 权限编辑器，被用于设置用户或角色的权限界面
 */
public class AuthorityEditor {
	private UserEditorGeneral userEditor;
	private RoleEditor roleEditor;
	private int editorType;
	private Composite parent1;
	private Composite parent2;
	Composite subloginGroupLeft;
	Composite subloginGroupRight;
	private org.eclipse.swt.widgets.List databaseAuthorityList;
	private org.eclipse.swt.widgets.List objectAuthorityList;
	private org.eclipse.swt.widgets.List subObjectAuthorityList;

	private Combo databaseAuthorityCombo;

	private Combo objectTypeCombo;
	private Combo schemaCombo;
	private Combo objectCombo;
	private Combo objectAuthorityCombo;
	private Combo subObjectTypeCombo;
	private Combo subObjectCombo;
	private Button addDatabaseAuthority;
	private Button removeDatabaseAuthority;
	private Button addObjectAuthority;
	private Button removeObjectAuthority;

	ArrayList<String> databaseAuthorities;
	ArrayList<String> objectAuthorities;
	ArrayList<String> subObjectAuthorities;

	public AuthorityEditor(Composite parent1, Composite parent2, int type) {
		this.parent1 = parent1;
		this.parent2 = parent2;
		this.editorType = type;
		databaseAuthorities = new ArrayList<>();
		objectAuthorities = new ArrayList<>();
		subObjectAuthorities = new ArrayList<>();
	}

	public void setRoleEditor(RoleEditor editor) {
		this.roleEditor = editor;
	}

	public void setUserEditor(UserEditorGeneral editor) {
		this.userEditor = editor;
	}

	/**
	 * 加载权限
	 * 
	 * @param databaseAuthorities 数据库权限集
	 * @param objectAuthorities   对象权限集
	 */
	public void loadDatabaseAuthorities(ArrayList<String> databaseAuthorities, ArrayList<String> objectAuthorities,ArrayList<String> subObjectAuthorities) {
		this.databaseAuthorities = databaseAuthorities;
		this.objectAuthorities = objectAuthorities;
		this.subObjectAuthorities = subObjectAuthorities;
	}

	/**
	 * 加载库级权限视图
	 */
	public void loadDatabaseAuthorityView() {
		// 加载组件
		//库级权限下拉框
		databaseAuthorityCombo = UIUtils.createLabelCombo(parent1, Messages.editors_authority_editor_db_combo, 0);
		databaseAuthorityCombo.setLayoutData(new GridData(375, 28));
		//授予按钮
		addDatabaseAuthority = UIUtils.createPushButton(parent1, Messages.editors_authority_editor_grant, null);
		addDatabaseAuthority.setLayoutData(new GridData(400, 28));
		//回收按钮
		removeDatabaseAuthority = UIUtils.createPushButton(parent1, Messages.editors_authority_editor_revoke, null);
		removeDatabaseAuthority.setLayoutData(new GridData(400, 28));
		//授予库级权限列表
		databaseAuthorityList = new org.eclipse.swt.widgets.List(parent1, SWT.V_SCROLL | SWT.MULTI);
		databaseAuthorityList.setLayoutData(new GridData(379, 200));
		databaseAuthorityList.setEnabled(false);
		if (editorType == 1) {
			ControlPropertyCommandListener.create(userEditor, databaseAuthorityList,
					UserPropertyHandler.DATABASE_AUTHORITY);
		} else {
			ControlPropertyCommandListener.create(roleEditor, databaseAuthorityList,
					RolePropertyHandler.DATABASE_AUTHORITY);
		}
		// 加载库级和对象级权限到组件中
		for (int i = 0; i < Constants.DEF_DATABASE_AUTHORITY_LIST.length; i++) {
			databaseAuthorityCombo.add(Constants.DEF_DATABASE_AUTHORITY_LIST[i]);
		}
		if (databaseAuthorities != null) {
			for (int i = 0, l = databaseAuthorities.size(); i < l; i++) {
				databaseAuthorityList.add(databaseAuthorities.get(i));
			}
		}
		// 对按钮添加监听事件
		addDatabaseAuthority.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String authority = databaseAuthorityCombo.getText();
				if (authority != null && authority.length() != 0) {
					// 先判断list文本框中是否已有，已有则不添加
					String[] nowItems = databaseAuthorityList.getItems();
					boolean hasItem = false;
					for (int i = 0, l = nowItems.length; i < l; i++) {
						if (nowItems[i].equals(authority)) {
							hasItem = true;
							break;
						}
					}
					if (!hasItem) {
						databaseAuthorityList.add(authority);
					}
					// 全部选中
					databaseAuthorityList.selectAll();
					// 激活相关组件修改监听
					databaseAuthorityList.notifyListeners(SWT.Modify, new Event());
					databaseAuthorityList.deselectAll();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		});
		removeDatabaseAuthority.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String authority = databaseAuthorityCombo.getText();
				if (authority != null && authority.length() != 0) {
					// 将下拉框中选中的权限从列表框中删除
					int index = databaseAuthorityList.indexOf(authority);
					if (index != -1) {
						databaseAuthorityList.remove(index);
					}
					// 全部选中
					databaseAuthorityList.selectAll();
					// 激活相关组件的修改监听
					databaseAuthorityList.notifyListeners(SWT.Modify, new Event());
					databaseAuthorityList.deselectAll();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		});
	}

	/**
	 * 加载对象级权限视图
	 * 
	 * @param owner 对象属主
	 */
	public void loadObjectAuthorityView(BaseGlobalObject owner) {
		// 加载组件
		subloginGroupLeft = UIUtils.createControlGroup(parent2, Messages.editors_authority_editor_subTitle1, 1,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 400);
		subloginGroupRight = UIUtils.createControlGroup(parent2, Messages.editors_authority_editor_subTitle2, 1,
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 400);
		subObjectTypeCombo = UIUtils.createLabelCombo(subloginGroupRight,
				Messages.editors_authority_editor_subObj_type_combo, 0);
		subObjectTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		subObjectCombo = UIUtils.createLabelCombo(subloginGroupRight,
				Messages.editors_authority_editor_subObj_list_combo, 0);
		subObjectCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		//
		subObjectAuthorityList = new org.eclipse.swt.widgets.List(subloginGroupRight, SWT.V_SCROLL | SWT.MULTI);
		subObjectAuthorityList.setLayoutData(new GridData(370, 190));
		subObjectAuthorityList.setEnabled(false);
		subObjectAuthorityList.setParent(subloginGroupRight);
		// 加载监听
		if (editorType == 1) {
			ControlPropertyCommandListener.create(userEditor, subObjectTypeCombo, UserPropertyHandler.SUB_TARGET_TYPE);
			ControlPropertyCommandListener.create(userEditor, subObjectCombo, UserPropertyHandler.SUB_TARGET_OBJECT);
			ControlPropertyCommandListener.create(userEditor, subObjectAuthorityList,
					UserPropertyHandler.SUB_OBJECT_AUTHORITY);
		} else {
			ControlPropertyCommandListener.create(roleEditor, subObjectTypeCombo, RolePropertyHandler.SUB_TARGET_TYPE);
			ControlPropertyCommandListener.create(roleEditor, subObjectCombo, RolePropertyHandler.SUB_TARGET_OBJECT);
			ControlPropertyCommandListener.create(roleEditor, subObjectAuthorityList,
					RolePropertyHandler.SUB_OBJECT_AUTHORITY);
		}
		//添加已有二级权限至列表
		if(subObjectAuthorities!=null) {
			for (int i = 0; i < subObjectAuthorities.size(); i++) {
				subObjectAuthorityList.add(subObjectAuthorities.get(i));
			}
		}
		
//		if (databaseAuthorities != null) {
//			for (int i = 0, l = databaseAuthorities.size(); i < l; i++) {
//				databaseAuthorityList.add(databaseAuthorities.get(i));
//			}
//		}

		subObjectTypeCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String schema = schemaCombo.getText();
				String type = subObjectTypeCombo.getText();
				String object = objectCombo.getText();
				String[] authorityList = null;
				// 加载对象信息
				subObjectCombo.removeAll();
				String objectList = "";
				if (editorType == 1) {
					objectList = ((User) owner).getObjectList(schema, type, object);
				} else {
					objectList = ((Role) owner).getObjectList(schema, type, object);
				}
				String[] objects = objectList.split(",");
				for (int i = 0, l = objects.length; i < l; i++) {
					subObjectCombo.add(objects[i]);
				}
				switch (type) {
				case "TRIGGER":
					authorityList = Constants.DEF_TRIGGER_AUTHORITY_LIST;
					break;
				case "COLUMN":
					authorityList = Constants.DEF_COLUMN_AUTHORITY_LIST;
					break;
				default:
					break;
				}
				if (authorityList != null) {
					objectAuthorityCombo.removeAll();
					for (int i = 0, l = authorityList.length; i < l; i++) {
						objectAuthorityCombo.add(authorityList[i]);
					}
				}	
				// 清空二级权限列表
//				subObjectAuthorityList.removeAll();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		});
		// 二级对象权限监听
		subObjectCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String subType = subObjectTypeCombo.getText();
				String schema = schemaCombo.getText();
				String object = objectCombo.getText();
				String subObject = subObjectCombo.getText();
				String keyWord = "";
				switch (subType) {
				case "TRIGGER":
					keyWord = "触发器";
					break;
				case "COLUMN":
					keyWord = "列";
					break;
				default:
					break;
				}
				// 从全部对象权限中加载符合条件的已有二级对象权限
//				subObjectAuthorityList.removeAll();
				Iterator<String> it = objectAuthorities.iterator();
				while (it.hasNext()) {
					String temp = it.next();
					if (temp.contains(keyWord)
							&& temp.contains("\"" + schema + "\".\"" + object + "\".\"" + subObject + "\"")) {
						subObjectAuthorityList.add(temp.substring(0, temp.indexOf(":")));
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		});

		// 一级对象级权限处理
		// 模式下拉框
		schemaCombo = UIUtils.createLabelCombo(subloginGroupLeft, Messages.editors_authority_editor_schema_combo, 0);
		Collection<Schema> schemaList;
		schemaList = owner.getDataSource().schemaCache.getCachedObjects();
		Iterator<Schema> it = schemaList.iterator();
		while (it.hasNext()) {
			schemaCombo.add(it.next().getName());
		}
		// 对象类型下拉框
		objectTypeCombo = UIUtils.createLabelCombo(subloginGroupLeft, Messages.editors_authority_editor_obj_type_combo,
				0);
		for (int i = 0, l = Constants.DEF_OBJECT_TYPE_LIST.length; i < l; i++) {
			objectTypeCombo.add(Constants.DEF_OBJECT_TYPE_LIST[i]);
		}
		// 对象下拉框
		objectCombo = UIUtils.createLabelCombo(subloginGroupLeft, Messages.editors_authority_editor_obj_list_combo, 0);
		// 可选对象权限下拉框(包括全部一二级权限)
		objectAuthorityCombo = UIUtils.createLabelCombo(parent2, Messages.editors_authority_editor_authority_combo, 0);
		// 已选对象权限列表框
		objectAuthorityList = new org.eclipse.swt.widgets.List(subloginGroupLeft, SWT.V_SCROLL | SWT.MULTI);
		objectAuthorityList.setLayoutData(new GridData(370, 135));
		objectAuthorityList.setEnabled(false);
		objectAuthorityList.setParent(subloginGroupLeft);
		// 加载监听
		if (editorType == 1) {
			ControlPropertyCommandListener.create(userEditor, objectAuthorityList,
					UserPropertyHandler.OBJECT_AUTHORITY);
			ControlPropertyCommandListener.create(userEditor, schemaCombo, UserPropertyHandler.TARGET_SCHEMA);
			ControlPropertyCommandListener.create(userEditor, objectCombo, UserPropertyHandler.TARGET_OBJECT);
			ControlPropertyCommandListener.create(userEditor, objectTypeCombo, UserPropertyHandler.TARGET_TYPE);
		} else {
			ControlPropertyCommandListener.create(roleEditor, objectAuthorityList,
					RolePropertyHandler.OBJECT_AUTHORITY);
			ControlPropertyCommandListener.create(roleEditor, schemaCombo, RolePropertyHandler.TARGET_SCHEMA);
			ControlPropertyCommandListener.create(roleEditor, objectCombo, RolePropertyHandler.TARGET_OBJECT);
			ControlPropertyCommandListener.create(roleEditor, objectTypeCombo, RolePropertyHandler.TARGET_TYPE);
		}
		addObjectAuthority = UIUtils.createPushButton(parent2, Messages.editors_authority_editor_grant, null);
		addObjectAuthority.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removeObjectAuthority = UIUtils.createPushButton(parent2, Messages.editors_authority_editor_revoke, null);
		removeObjectAuthority.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		//添加已有一级对象级权限至列表
		if(objectAuthorities!=null) {
			for (int i = 0; i < objectAuthorities.size(); i++) {
				objectAuthorityList.add(objectAuthorities.get(i));
			}
		}
		
		
		
		SelectionListener itemChangeListener = new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String type = objectTypeCombo.getText();
				String schema = schemaCombo.getText();
				// 加载对象信息
				objectCombo.removeAll();
				String objectList = "";
				if (editorType == 1) {
					objectList = ((User) owner).getObjectList(schema, type, "");
				} else {
					objectList = ((Role) owner).getObjectList(schema, type, "");
				}
				String[] objects = objectList.split(",");

				for (int i = 0, l = objects.length; i < l; i++) {
					objectCombo.add(objects[i]);
				}
				// 加载权限信息
				String[] authorityList = null;
				switch (type) {
				case "TABLE":
					authorityList = Constants.DEF_TABLE_AUTHORITY_LIST;
					break;
				case "VIEW":
					authorityList = Constants.DEF_VIEW_AUTHORITY_LIST;
					break;
				case "SEQUENCE":
					authorityList = Constants.DEF_SEQUENCE_AUTHORITY_LIST;
					break;
				case "TRIGGER":
					authorityList = Constants.DEF_TRIGGER_AUTHORITY_LIST;
					break;
				case "PACKAGE":
					authorityList = Constants.DEF_PACKAGE_AUTHORITY_LIST;
					break;
				case "PROCEDURE":
					authorityList = Constants.DEF_PROCEDURE_AUTHORITY_LIST;
					break;
				default:
					break;
				}
				if (authorityList != null) {
					objectAuthorityCombo.removeAll();
					for (int i = 0, l = authorityList.length; i < l; i++) {
						objectAuthorityCombo.add(authorityList[i]);
					}
				}
				// 清空一级权限列表
//				objectAuthorityList.removeAll();
				// 当一级对象改变时二级对象权限随之清空
				subObjectCombo.removeAll();
				subObjectTypeCombo.removeAll();
//				subObjectAuthorityList.removeAll();
				// 然后根据一级对象类型重新加载二级对象权限类型
				final String tableType = "TABLE";
				final String viewType = "VIEW";
				if (tableType.equals(type) || viewType.equals(type)) {
					subObjectTypeCombo.add("COLUMN");
					//subObjectTypeCombo.add("TRIGGER");
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		};
		objectTypeCombo.addSelectionListener(itemChangeListener);
		schemaCombo.addSelectionListener(itemChangeListener);
		// 一级对象权限监听
		objectCombo.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String type = objectTypeCombo.getText();
				String schema = schemaCombo.getText();
				String object = objectCombo.getText();
				String keyWord = "";
				switch (type) {
				case "TABLE":
					keyWord = "表";
					break;
				case "VIEW":
					keyWord = "视图";
					break;
				case "SEQUENCE":
					keyWord = "序列值";
					break;
				case "TRIGGER":
					keyWord = "触发器";
					break;
				case "PACKAGE":
					keyWord = "包";
					break;
				case "PROCEDURE":
					keyWord = "存储过程或函数";
					break;
				default:
					break;
				}
				// 重新加载权限下拉框 选定为一级对象权限
				objectAuthorityCombo.removeAll();
				String[] authorityList = null;
				switch (type) {
				case "TABLE":
					authorityList = Constants.DEF_TABLE_AUTHORITY_LIST;
					break;
				case "VIEW":
					authorityList = Constants.DEF_VIEW_AUTHORITY_LIST;
					break;
				case "SEQUENCE":
					authorityList = Constants.DEF_SEQUENCE_AUTHORITY_LIST;
					break;
				case "TRIGGER":
					authorityList = Constants.DEF_TRIGGER_AUTHORITY_LIST;
					break;
				case "PACKAGE":
					authorityList = Constants.DEF_PACKAGE_AUTHORITY_LIST;
					break;
				case "PROCEDURE":
					authorityList = Constants.DEF_PROCEDURE_AUTHORITY_LIST;
					break;
				default:
					break;
				}
				if (authorityList != null) {
					objectAuthorityCombo.removeAll();
					for (int i = 0, l = authorityList.length; i < l; i++) {
						objectAuthorityCombo.add(authorityList[i]);
					}
				}
				// 加载符合条件的已有权限
//				objectAuthorityList.removeAll();
//				Iterator<String> it = objectAuthorities.iterator();
//				while (it.hasNext()) {
//					String temp = it.next();
//					if (temp.contains(keyWord) && temp.contains("\"" + schema + "\".\"" + object + "\"")) {
//						objectAuthorityList.add(temp.substring(0, temp.indexOf(":")));
//					}
//				}
				// 当一级对象改变时二级对象权限随之清空
				subObjectCombo.removeAll();
				subObjectTypeCombo.removeAll();
//				subObjectAuthorityList.removeAll();
				// 然后根据一级对象重新加载二级对象权限类型
				final String tableType = "TABLE";
				final String viewType = "VIEW";
				if (tableType.equals(type) || viewType.equals(type)) {
					subObjectTypeCombo.add("COLUMN");
					//subObjectTypeCombo.add("TRIGGER");
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		});
		addObjectAuthority.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String schemaNameString = schemaCombo.getText();
				String objectTypeString = objectTypeCombo.getText();
				String objectNameString = objectCombo.getText();
				String tableColumnString = subObjectCombo.getText();
				String authorityText = objectAuthorityCombo.getText();
				if (schemaNameString.isEmpty()) {
					new WarningDialog(UIUtils.getActiveWorkbenchShell(), "请选择待授予权限的模式名称").open();
					return;
				}
				if (objectTypeString.isEmpty()) {
					new WarningDialog(UIUtils.getActiveWorkbenchShell(), "请选择待授予权限的对象类型").open();
					return;
				}
				if (objectNameString.isEmpty()) {
					new WarningDialog(UIUtils.getActiveWorkbenchShell(), "请选择待授予权限的对象名称").open();
					return;
				}
				if (authorityText.isEmpty()) {
					new WarningDialog(UIUtils.getActiveWorkbenchShell(), "请选择授予的权限名称").open();
					return;
				}
				String authority = authorityText + ":" + "\"" + schemaNameString + "\"" + "." + "\"" + objectNameString + "\"";
				boolean isContainColumn = false;
				if (!tableColumnString.isEmpty()) {
					authority += "." + "\"" + tableColumnString + "\"";
					isContainColumn = true;
				}
				if (authority != null && authority.length() != 0) {
					String[] authorityList = null;
					boolean isFirstLevel = true;
					if (authority != null && isContainColumn) {
						authorityList = subObjectAuthorityList.getItems();
						isFirstLevel = false;
					} else {
						authorityList = objectAuthorityList.getItems();
					}
					boolean hasAuthority = false;
					for (int i = 0, l = authorityList.length; i < l; i++) {
						if (authority.equals(authorityList[i])) {
							hasAuthority = true;
							break;
						}
					}
					if (isFirstLevel) {
						if (!hasAuthority) {
							objectAuthorityList.add(authority);
						}
						objectAuthorityList.selectAll();
						schemaCombo.notifyListeners(SWT.Modify, null);
						objectCombo.notifyListeners(SWT.Modify, null);
						objectTypeCombo.notifyListeners(SWT.Modify, null);
						objectAuthorityList.notifyListeners(SWT.Modify, null);
						objectAuthorityList.deselectAll();
					} else {
						if (!hasAuthority) {
							subObjectAuthorityList.add(authority);
						}
						subObjectAuthorityList.selectAll();
						schemaCombo.notifyListeners(SWT.Modify, null);
						objectCombo.notifyListeners(SWT.Modify, null);
						objectTypeCombo.notifyListeners(SWT.Modify, null);
						subObjectCombo.notifyListeners(SWT.Modify, null);
						subObjectTypeCombo.notifyListeners(SWT.Modify, null);
						subObjectAuthorityList.notifyListeners(SWT.Modify, null);
						subObjectAuthorityList.deselectAll();
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		});
		removeObjectAuthority.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String schemaNameString = schemaCombo.getText();
				String objectNameString = objectCombo.getText();
				String tableColumnString = subObjectCombo.getText();
				String authority = objectAuthorityCombo.getText()+":"+"\""+schemaNameString+"\""+"."+"\""+objectNameString+"\"";
				boolean isContainColumn = false;
				if(!tableColumnString.isEmpty()) {
					authority +="."+"\""+tableColumnString+"\"";
					isContainColumn = true;
				}
				if (authority != null && authority.length() != 0) {
					String[] authorityList = null;
					boolean isFirstLevel = true;
					if (authority != null && isContainColumn) {
						authorityList = subObjectAuthorityList.getItems();
						isFirstLevel = false;
					} else {
						authorityList = objectAuthorityList.getItems();
					}
					if (isFirstLevel) {
						boolean hasAuthority = false;
						for (int i = 0, l = authorityList.length; i < l; i++) {
							if (authority.equals(authorityList[i])) {
								hasAuthority = true;
								break;
							}
						}
						if (hasAuthority) {
							objectAuthorityList.remove(authority);
						}
						objectAuthorityList.selectAll();
						schemaCombo.notifyListeners(SWT.Modify, null);
						objectCombo.notifyListeners(SWT.Modify, null);
						objectTypeCombo.notifyListeners(SWT.Modify, null);
						objectAuthorityList.notifyListeners(SWT.Modify, null);
						objectAuthorityList.deselectAll();
					} else {
						boolean hasAuthority = false;
						for (int i = 0, l = authorityList.length; i < l; i++) {
							if (authority.equals(authorityList[i])) {
								hasAuthority = true;
								break;
							}
						}
						if (hasAuthority) {
							subObjectAuthorityList.remove(authority);
						}
						subObjectAuthorityList.selectAll();
						schemaCombo.notifyListeners(SWT.Modify, null);
						objectCombo.notifyListeners(SWT.Modify, null);
						objectTypeCombo.notifyListeners(SWT.Modify, null);
						subObjectCombo.notifyListeners(SWT.Modify, null);
						subObjectTypeCombo.notifyListeners(SWT.Modify, null);
						subObjectAuthorityList.notifyListeners(SWT.Modify, null);
						subObjectAuthorityList.deselectAll();
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO 小部件默认已选择事件
			}
		});
	}
	
	public static class WarningDialog extends Dialog {
		private String warningInfo;

		public WarningDialog(Shell parentShell, String info) {
			super(parentShell);
			this.warningInfo = info;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			getShell().setText("授予权限");

			Control container = super.createDialogArea(parent);
			Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			Label infoText = UIUtils.createLabel(composite, this.warningInfo);
			infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			return parent;
		}
	}
}
