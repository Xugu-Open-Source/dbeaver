package org.jkiss.dbeaver.ext.xugu.tasks;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.SchemaCache;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.UserRoleFlag;
import org.jkiss.dbeaver.ext.xugu.model.Database;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Table;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;

import com.xugu.backup.BackupExecutor;

import static org.eclipse.swt.events.SelectionListener.*;

public class BackupTool implements IUserInterfaceTool {
	private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

	@Override
	public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
			throws DBException {
		if (objects.isEmpty()) {
			return;
		}
		Iterator<DBSObject> it = objects.iterator();
		DBSObject first = it.next();
		DataSource dataSource = (DataSource) first.getDataSource();
		LoggingProgressMonitor monitor = new LoggingProgressMonitor();
		Collection<Database> databases = dataSource.getDatabases(monitor);
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
		selectShell.setText("数据库备份工具");
		selectShell.setLayout(new GridLayout());
		org.eclipse.swt.widgets.Table objectTable = new org.eclipse.swt.widgets.Table(selectShell, SWT.BORDER);
		objectTable.setLinesVisible(true);
		objectTable.setHeaderVisible(true);
		objectTable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		TableColumn c0 = new TableColumn(objectTable, SWT.NONE);
		c0.setWidth(30);
		c0.setText("");
		TableColumn c1 = new TableColumn(objectTable, SWT.CENTER);
		c1.setWidth(80);
		c1.setText("对象类型");
		TableColumn c2 = new TableColumn(objectTable, SWT.CENTER);
		c2.setWidth(300);
		c2.setText("对象名称");
		String dateString = format.format(new Date());
		{
			String fileName = String.format("%s_%s.dump", SelectedObject.Type.SYSTEM, dateString);
			SelectedObject obj = new SelectedObject(SelectedObject.Type.SYSTEM, "", "", "", "", fileName);
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
			editor.minimumWidth = button.getSize().x;
			editor.horizontalAlignment = SWT.CENTER;
			editor.setEditor(button, item, 0);
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
		Button backupButton = new Button(selectShell, SWT.PUSH);
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
				BackupExecutor executor = new BackupExecutor(dataSource);
				Map<SelectedObject, Exception> exceptions = new HashMap<>();
				for (SelectedObject object : selectedObjects) {
					switch (object.getType()) {
					case SYSTEM:
						try {
							executor.forSystemAll(object.getFileName());
						} catch (Exception ex) {
							exceptions.put(object, ex);
						}
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
		selectShell.open();
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
