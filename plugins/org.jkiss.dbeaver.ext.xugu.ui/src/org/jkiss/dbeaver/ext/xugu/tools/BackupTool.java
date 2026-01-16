/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.xugu.tools;

import com.xugu.backup.AbstractExecutor;
import com.xugu.backup.BackupExecutor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Database;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Table;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

public class BackupTool extends XuguToolAbstractHandler {
	private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");


	@Override
	public void XuguToolSubclassReach(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) {
		if (objects.isEmpty()) {
			return;
		}
		int databaseMajorVersion;
		Iterator<DBSObject> it = objects.iterator();
		DBSObject first = it.next();
		DataSource dataSource = (DataSource) first.getDataSource();
		try (Connection connection = dataSource.getConnection()) {
			databaseMajorVersion = connection.getMetaData().getDatabaseMajorVersion();
		} catch (SQLException ex) {
			throw new IllegalStateException(ex);
		}
		LoggingProgressMonitor monitor = new LoggingProgressMonitor();
		Collection<Database> databases = null;
		try {
			databases = dataSource.getDatabases(monitor);
		} catch (DBException e) {
			throw new RuntimeException(e);
		}
		Collection<Schema> schemas = databases.stream().flatMap(database -> {
			try {
				return database.getSchemas(monitor).stream();
			} catch (DBException e) {
				throw new IllegalStateException(String.format("获取数据库（%s）的模式列表失败", database.getName()), e);
			}
		}).collect(Collectors.toSet());
		Collection<Table> tables = schemas.stream().flatMap(schema -> {
			try {
				return schema.getTables(monitor).stream();
			} catch (DBException e) {
				throw new IllegalStateException(
						String.format("获取库（%s）模式（%s）的表列表失败", schema.getParent().getName(), schema.getName()), e);
			}
		}).collect(Collectors.toSet());
		Collection<SelectedObject> selectedObjects = new ArrayList<>();
		Shell selectShell = new Shell(window.getShell());
		BackupExecutor executor = new BackupExecutor(dataSource);
		selectShell.setText("数据库备份工具");
		selectShell.setLayout(new GridLayout(1, false));

		// 创建滚动容器
		ScrolledComposite scrolledComposite = new ScrolledComposite(selectShell, SWT.V_SCROLL | SWT.BORDER);
		scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		// 内容容器
		Composite contentComposite = new Composite(scrolledComposite, SWT.NONE);
		contentComposite.setLayout(new GridLayout(1, false));

		// 提示标签
		Label tipsLabel = new Label(contentComposite, SWT.NONE);
		tipsLabel.setText("提示：备份文件存储于数据库部署目录下 " + AbstractExecutor.DATABASE_PATH_PREFIX + executor.getBackupPathPrefix());

		// 表格
		org.eclipse.swt.widgets.Table objectTable = new org.eclipse.swt.widgets.Table(contentComposite, SWT.BORDER | SWT.FULL_SELECTION);
		objectTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		objectTable.setLinesVisible(true);
		objectTable.setHeaderVisible(true);

		TableColumn c0 = new TableColumn(objectTable, SWT.NONE);
		c0.setWidth(30);
		c0.setText("");
		TableColumn c1 = new TableColumn(objectTable, SWT.CENTER);
		c1.setWidth(80);
		c1.setText("对象类型");
		TableColumn c2 = new TableColumn(objectTable, SWT.CENTER);
		c2.setWidth(720);
		c2.setText("对象名称");

		List<Button> selectButtons = new ArrayList<>(1 + databases.size() + schemas.size() + tables.size());
		String dateString = format.format(new Date());
		Button systemSelectButton = new Button(objectTable, SWT.CHECK);
		{
			String fileName = String.format("%s_%s.dump", SelectedObject.Type.SYSTEM, dateString);
			SelectedObject obj = new SelectedObject(SelectedObject.Type.SYSTEM, "", "", "", "", fileName);
			TableItem item = new TableItem(objectTable, SWT.NONE);
			TableEditor editor = new TableEditor(objectTable);
			systemSelectButton.pack();
			systemSelectButton.addSelectionListener(widgetSelectedAdapter(e -> {
				Button btn = (Button) e.getSource();
				if (btn.getSelection()) {
					if (confirmSelectSystemButton(selectShell)) {
						selectedObjects.add(obj);
					} else {
						btn.setSelection(false);
					}
				} else {
					selectedObjects.remove(obj);
				}
			}));
			selectButtons.add(systemSelectButton);
			editor.minimumWidth = systemSelectButton.getSize().x;
			editor.horizontalAlignment = SWT.CENTER;
			editor.setEditor(systemSelectButton, item, 0);
			editor = new TableEditor(objectTable);
			Text objectTypeText = new Text(objectTable, SWT.CENTER);
			objectTypeText.setText(SelectedObject.Type.SYSTEM.getName());
			objectTypeText.setEditable(false);
			editor.grabHorizontal = true;
			editor.setEditor(objectTypeText, item, 1);
			editor = new TableEditor(objectTable);
			Text objectNameText = new Text(objectTable, SWT.NONE);
			objectNameText.setText("");
			objectNameText.setEditable(false);
			editor.grabHorizontal = true;
			editor.setEditor(objectNameText, item, 2);
		}
		for (Database database : databases) {
			String objectName = String.format("<%s>", database.getName());
			String fileName = String.format("%s_%s_%s.dump", SelectedObject.Type.DATABASE, database.getName(),
					dateString);
			SelectedObject obj = new SelectedObject(SelectedObject.Type.DATABASE, database.getName(), "", "",
					objectName, fileName);
			TableItem item = new TableItem(objectTable, SWT.NONE);
			TableEditor editor = new TableEditor(objectTable);
			Button button = new Button(objectTable, SWT.CHECK);
			button.pack();
			button.addSelectionListener(widgetSelectedAdapter(e -> {
				Button btn = (Button) e.getSource();
				if (btn.getSelection()) {
					selectedObjects.add(obj);
				} else {
					selectedObjects.remove(obj);
				}
			}));
			selectButtons.add(button);
			editor.minimumWidth = button.getSize().x;
			editor.horizontalAlignment = SWT.CENTER;
			editor.setEditor(button, item, 0);
			editor = new TableEditor(objectTable);
			Text objectTypeText = new Text(objectTable, SWT.CENTER);
			objectTypeText.setText(SelectedObject.Type.DATABASE.getName());
			objectTypeText.setEditable(false);
			editor.grabHorizontal = true;
			editor.setEditor(objectTypeText, item, 1);
			editor = new TableEditor(objectTable);
			Text objectNameText = new Text(objectTable, SWT.NONE);
			objectNameText.setText(objectName);
			objectNameText.setEditable(false);
			editor.grabHorizontal = true;
			editor.setEditor(objectNameText, item, 2);
		}
		if (databaseMajorVersion > 11) {
			for (Schema schema : schemas) {
				String objectName = String.format("<%s>%s", schema.getParent().getName(), schema.getName());
				String fileName = String.format("%s_%s_%s_%s.dump", SelectedObject.Type.SCHEMA,
						schema.getParent().getName(), schema.getName(), dateString);
				SelectedObject obj = new SelectedObject(SelectedObject.Type.SCHEMA, schema.getParent().getName(),
						schema.getName(), "", objectName, fileName);
				TableItem item = new TableItem(objectTable, SWT.NONE);
				TableEditor editor = new TableEditor(objectTable);
				Button button = new Button(objectTable, SWT.CHECK);
				button.pack();
				button.addSelectionListener(widgetSelectedAdapter(e -> {
					Button btn = (Button) e.getSource();
					if (btn.getSelection()) {
						selectedObjects.add(obj);
					} else {
						selectedObjects.remove(obj);
					}
				}));
				selectButtons.add(button);
				editor.minimumWidth = button.getSize().x;
				editor.horizontalAlignment = SWT.CENTER;
				editor.setEditor(button, item, 0);
				editor = new TableEditor(objectTable);
				Text objectTypeText = new Text(objectTable, SWT.CENTER);
				objectTypeText.setText(SelectedObject.Type.SCHEMA.getName());
				objectTypeText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(objectTypeText, item, 1);
				editor = new TableEditor(objectTable);
				Text objectNameText = new Text(objectTable, SWT.NONE);
				objectNameText.setText(objectName);
				objectNameText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(objectNameText, item, 2);
			}
		}
		for (Table table : tables) {
			String objectName = String.format("<%s>%s.%s", table.getSchema().getParent().getName(),
					table.getSchema().getName(), table.getName());
			String fileName = String.format("%s_%s_%s_%s_%s.dump", SelectedObject.Type.TABLE,
					table.getSchema().getParent().getName(), table.getSchema().getName(), table.getName(), dateString);
			SelectedObject obj = new SelectedObject(SelectedObject.Type.TABLE, table.getSchema().getParent().getName(),
					table.getSchema().getName(), table.getName(), objectName, fileName);
			TableItem item = new TableItem(objectTable, SWT.NONE);
			TableEditor editor = new TableEditor(objectTable);
			Button button = new Button(objectTable, SWT.CHECK);
			button.pack();
			button.addSelectionListener(widgetSelectedAdapter(e -> {
				Button btn = (Button) e.getSource();
				if (btn.getSelection()) {
					selectedObjects.add(obj);
				} else {
					selectedObjects.remove(obj);
				}
			}));
			selectButtons.add(button);
			editor.minimumWidth = button.getSize().x;
			editor.horizontalAlignment = SWT.CENTER;
			editor.setEditor(button, item, 0);
			editor = new TableEditor(objectTable);
			Text objectTypeText = new Text(objectTable, SWT.CENTER);
			objectTypeText.setText(SelectedObject.Type.TABLE.getName());
			objectTypeText.setEditable(false);
			editor.grabHorizontal = true;
			editor.setEditor(objectTypeText, item, 1);
			editor = new TableEditor(objectTable);
			Text objectNameText = new Text(objectTable, SWT.NONE);
			objectNameText.setText(objectName);
			objectNameText.setEditable(false);
			editor.grabHorizontal = true;
			editor.setEditor(objectNameText, item, 2);
		}

		// 设置滚动内容
		scrolledComposite.setContent(contentComposite);
		scrolledComposite.setMinSize(contentComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		// 按钮区域（固定在底部）
		Composite buttonComposite = new Composite(selectShell, SWT.NONE);
		buttonComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		buttonComposite.setLayout(new RowLayout(SWT.HORIZONTAL));

		Button allSelectButton = new Button(buttonComposite, SWT.PUSH);
		allSelectButton.setText("全选");
		allSelectButton.addSelectionListener(widgetSelectedAdapter(e -> {
			for (Button button : selectButtons) {
				button.setSelection(true);
			}
			systemSelectButton.setSelection(confirmSelectSystemButton(selectShell));
		}));

		Button reverseSelectButton = new Button(buttonComposite, SWT.PUSH);
		reverseSelectButton.setText("反选");
		reverseSelectButton.addSelectionListener(widgetSelectedAdapter(e -> {
			for (Button button : selectButtons) {
				button.setSelection(!button.getSelection());
			}
			if (systemSelectButton.getSelection()) {
				systemSelectButton.setSelection(confirmSelectSystemButton(selectShell));
			}
		}));

		Button backupButton = new Button(buttonComposite, SWT.PUSH);
		backupButton.setText("开始备份");
		backupButton.addSelectionListener(widgetSelectedAdapter(e -> {
			Shell comfirmShell = new Shell(selectShell);
			comfirmShell.setText("确认备份文件名");
			comfirmShell.setLayout(new GridLayout());
			org.eclipse.swt.widgets.Table comfirmTable = new org.eclipse.swt.widgets.Table(comfirmShell, SWT.BORDER);
			comfirmTable.setLinesVisible(true);
			comfirmTable.setHeaderVisible(true);
			comfirmTable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			TableColumn objectTypeColumn = new TableColumn(comfirmTable, SWT.NONE);
			objectTypeColumn.setWidth(80);
			objectTypeColumn.setText("对象类型");
			TableColumn objectNameColumn = new TableColumn(comfirmTable, SWT.CENTER);
			objectNameColumn.setWidth(300);
			objectNameColumn.setText("对象名称");
			TableColumn fileNameColumn = new TableColumn(comfirmTable, SWT.CENTER);
			fileNameColumn.setWidth(300);
			fileNameColumn.setText("文件名称");
			if (selectedObjects.isEmpty()) {
				throw new RuntimeException("请先选择需要备份的对象");
			}
			for (SelectedObject object : selectedObjects) {
				TableItem item = new TableItem(comfirmTable, SWT.NONE);
				TableEditor editor = new TableEditor(comfirmTable);
				Text objectTypeText = new Text(comfirmTable, SWT.NONE);
				objectTypeText.setText(object.getType().getName());
				objectTypeText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(objectTypeText, item, 0);
				editor = new TableEditor(comfirmTable);
				Text objectNameText = new Text(comfirmTable, SWT.NONE);
				objectNameText.setText(object.getObjectName());
				objectNameText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(objectNameText, item, 1);
				editor = new TableEditor(comfirmTable);
				Text fileNameText = new Text(comfirmTable, SWT.NONE);
				fileNameText.setText(object.getFileName());
				fileNameText.addModifyListener(event -> {
					Text text = (Text) event.getSource();
					object.setFileName(text.getText());
				});
				editor.grabHorizontal = true;
				editor.setEditor(fileNameText, item, 2);
			}
			Button comfirmButton = new Button(comfirmShell, SWT.PUSH);
			comfirmButton.setText("已确认，立即开始");
			comfirmButton.addSelectionListener(widgetSelectedAdapter(event -> {
				Map<SelectedObject, Exception> exceptions = new HashMap<>();
				boolean isSelectedSystem = false;
				SelectedObject systemObject = null;
				for (SelectedObject object : selectedObjects) {
					switch (object.getType()) {
						case SYSTEM:
							isSelectedSystem = true;
							systemObject = object;
							break;
						case DATABASE:
							try {
								executor.forCatalog(object.getDatabaseName(), object.getFileName());
							} catch (Exception ex) {
								exceptions.put(object, ex);
							}
							break;
						case SCHEMA:
							try {
								executor.forSchema(object.getDatabaseName(), object.getSchemaName(), object.getFileName());
							} catch (Exception ex) {
								exceptions.put(object, ex);
							}
							break;
						case TABLE:
							try {
								executor.forTable(object.getDatabaseName(), object.getSchemaName(), object.getTableName(),
										object.getFileName());
							} catch (Exception ex) {
								exceptions.put(object, ex);
							}
							break;
						default:
							exceptions.put(object, new UnsupportedOperationException("未支持的对象：" + object));
					}
				}
				if (isSelectedSystem) {
					try {
						executor.forSystemAll(systemObject.getFileName());
					} catch (Exception ex) {
						exceptions.put(systemObject, ex);
					}
				}
				if (exceptions.isEmpty()) {
					MessageDialog.openInformation(comfirmShell, "备份成功", "执行数据库对象备份完成！");
					comfirmShell.dispose();
				} else {
					StringBuilder builder = new StringBuilder();
					exceptions.forEach((key, value) -> {
						builder.append("[");
						builder.append(key.getType().getName());
						builder.append("]");
						builder.append(key.getObjectName());
						builder.append("\n");
						String msg = value.getLocalizedMessage();
						if (msg.charAt(msg.length() - 1) == '\0') {
							builder.append(msg.substring(0, msg.length() - 1));
						} else {
							builder.append(msg);
						}
						builder.append("\n\n");
					});
					MessageDialog.openError(comfirmShell, "备份失败", "下列对象备份失败：\n" + builder);
					return;
				}
			}));
			comfirmShell.pack();
			comfirmShell.open();
		}));

		selectShell.pack();
		selectShell.setSize(800, 600); // 设置窗口初始大小
		selectShell.open();
		if (databaseMajorVersion == 11) {
			MessageDialog.openWarning(selectShell, "模式级对象备份未支持", "当前连接的服务器版本为 11，暂未支持模式级对象备份！");
		}
	}

	private static boolean confirmSelectSystemButton(Shell shell) {
		return MessageDialog.openConfirm(shell,
				"确认选择系统级备份",
				"执行系统级备份将关闭所有数据库连接，请谨慎考虑！\n您确认要选择系统级备份吗？");
	}

	private static class SelectedObject {
		private final Type type;
		private final String databaseName;
		private final String schemaName;
		private final String tableName;
		private final String objectName;
		private String fileName;

		public SelectedObject(Type type, String databaseName, String schemaName, String tableName, String objectName,
							  String fileName) {
			super();
			this.type = type;
			this.databaseName = databaseName;
			this.schemaName = schemaName;
			this.tableName = tableName;
			this.objectName = objectName;
			this.fileName = fileName;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public Type getType() {
			return type;
		}

		public String getDatabaseName() {
			return databaseName;
		}

		public String getSchemaName() {
			return schemaName;
		}

		public String getTableName() {
			return tableName;
		}

		public String getObjectName() {
			return objectName;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((objectName == null) ? 0 : objectName.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SelectedObject other = (SelectedObject) obj;
			if (objectName == null) {
				if (other.objectName != null)
					return false;
			} else if (!objectName.equals(other.objectName))
				return false;
			if (type != other.type)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "SelectedObject [type=" + type + ", objectName=" + objectName + ", fileName=" + fileName + "]";
		}

		private static enum Type {
			SYSTEM("系统"), DATABASE("库"), SCHEMA("模式"), TABLE("表");

			private final String name;

			Type(String name) {
				this.name = name;
			}

			public String getName() {
				return name;
			}
		}
	}
}
