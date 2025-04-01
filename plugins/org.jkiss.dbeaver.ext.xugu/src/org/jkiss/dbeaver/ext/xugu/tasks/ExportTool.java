package org.jkiss.dbeaver.ext.xugu.tasks;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
import static org.eclipse.swt.events.MouseListener.mouseDownAdapter;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Database;
import org.jkiss.dbeaver.ext.xugu.model.Trigger;
import org.jkiss.dbeaver.ext.xugu.model.ProcedureStandalone;
import org.jkiss.dbeaver.ext.xugu.model.Role;
import org.jkiss.dbeaver.ext.xugu.model.SchedulerJob;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Sequence;
import org.jkiss.dbeaver.ext.xugu.model.Synonym;
import org.jkiss.dbeaver.ext.xugu.model.Table;
import org.jkiss.dbeaver.ext.xugu.model.Trigger;
import org.jkiss.dbeaver.ext.xugu.model.Package;
import org.jkiss.dbeaver.ext.xugu.model.Udt;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.ext.xugu.model.View;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import com.xugu.parser.DatabaseParsing;
import com.xugu.parser.Parsing;
import com.xugu.parser.Parsing.TableType;

public class ExportTool implements IUserInterfaceTool {
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
		Shell fileSaveShell = new Shell(window.getShell());
		fileSaveShell.setText("数据库定义导出工具");
		fileSaveShell.setLayout(new GridLayout());
		org.eclipse.swt.widgets.Table objectTable = new org.eclipse.swt.widgets.Table(fileSaveShell, SWT.BORDER);
		objectTable.setLinesVisible(true);
		objectTable.setHeaderVisible(true);
		objectTable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		TableColumn c0 = new TableColumn(objectTable, SWT.CENTER);
		c0.setWidth(100);
		c0.setText("对象类型");
		TableColumn c1 = new TableColumn(objectTable, SWT.CENTER);
		c1.setWidth(300);
		c1.setText("对象名称");
		TableColumn c2 = new TableColumn(objectTable, SWT.CENTER);
		c2.setWidth(500);
		c2.setText("保存路径");
		List<ExportObject<? extends DBSObject>> expObjs = new ArrayList<>();
		String dateString = format.format(new Date());
		for (DBSObject object : objects) {
			if (object instanceof DataSourceDescriptor) {
				Database database = dataSource.getDatabase();
				ExportObject<Database> expObj = new ExportObject<>(ExportObject.Type.DATABASE, database);
				expObjs.add(expObj);
				String objectName = String.format("<%s>", database.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s.sql",
						expObj.getType(),
						expObj.getObject().getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof Schema) {
				Schema schema = (Schema) object;
				ExportObject<Schema> expObj = new ExportObject<>(ExportObject.Type.SCHEMA, schema);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s", schema.getParent().getName(), schema.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s.sql",
						expObj.getType(),
						expObj.getObject().getParent().getName(),
						expObj.getObject().getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof Role) {
				Role role = (Role) object;
				ExportObject<Role> expObj = new ExportObject<>(ExportObject.Type.ROLE, role);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s", role.getParent().getName(), role.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s.sql",
						expObj.getType(),
						expObj.getObject().getParent().getName(),
						expObj.getObject().getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof User) {
				User user = (User) object;
				ExportObject<User> expObj = new ExportObject<>(ExportObject.Type.USER, user);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s", user.getParent().getName(), user.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s.sql",
						expObj.getType(),
						expObj.getObject().getParent().getName(),
						expObj.getObject().getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof SchedulerJob) {
				SchedulerJob user = (SchedulerJob) object;
				ExportObject<SchedulerJob> expObj = new ExportObject<>(ExportObject.Type.JOB, user);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s", user.getParent().getName(), user.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s.sql",
						expObj.getType(),
						expObj.getObject().getParent().getName(),
						expObj.getObject().getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof Table) {
				Table table = (Table) object;
				ExportObject<Table> expObj = new ExportObject<>(ExportObject.Type.TABLE, table);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s.%s",
						table.getSchema().getParent().getName(),
						table.getSchema().getName(),
						table.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s_%s.sql",
						ExportObject.Type.TABLE,
						table.getSchema().getParent().getName(),
						table.getSchema().getName(),
						table.getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof View) {
				View view = (View) object;
				ExportObject<View> expObj = new ExportObject<>(ExportObject.Type.VIEW, view);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s.%s",
						view.getSchema().getParent().getName(),
						view.getSchema().getName(),
						view.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s_%s.sql",
						ExportObject.Type.VIEW,
						view.getSchema().getParent().getName(),
						view.getSchema().getName(),
						view.getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof Sequence) {
				Sequence sequence = (Sequence) object;
				ExportObject<Sequence> expObj = new ExportObject<>(ExportObject.Type.SEQUENCE, sequence);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s.%s",
						sequence.getSchema().getParent().getName(),
						sequence.getSchema().getName(),
						sequence.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s_%s.sql",
						ExportObject.Type.SEQUENCE,
						sequence.getSchema().getParent().getName(),
						sequence.getSchema().getName(),
						sequence.getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof Package) {
				Package pkg = (Package) object;
				ExportObject<Package> expObj = new ExportObject<>(ExportObject.Type.PACKAGE, pkg);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s.%s",
						pkg.getSchema().getParent().getName(),
						pkg.getSchema().getName(),
						pkg.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s_%s.sql",
						ExportObject.Type.PACKAGE,
						pkg.getSchema().getParent().getName(),
						pkg.getSchema().getName(),
						pkg.getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof ProcedureStandalone) {
				ProcedureStandalone procedure = (ProcedureStandalone) object;
				DBSProcedureType type = procedure.getProcedureType();
				ProcedureStandalone procedureStandalone = (ProcedureStandalone) object;
				ExportObject<ProcedureStandalone> expObj;
				String objectName;
				Path directoryPath;
				String fileName;
				Path filePath;
				switch (type) {
					case PROCEDURE:
						expObj = new ExportObject<>(ExportObject.Type.PROCEDURE, procedureStandalone);
						expObjs.add(expObj);
						objectName = String.format("<%s>%s.%s",
								procedureStandalone.getSchema().getParent().getName(),
								procedureStandalone.getSchema().getName(),
								procedureStandalone.getName());
						directoryPath = RuntimeUtils.getUserHomeDir().toPath();
						fileName = String.format("%s_%s_%s_%s_%s.sql",
								ExportObject.Type.PACKAGE,
								procedureStandalone.getSchema().getParent().getName(),
								procedureStandalone.getSchema().getName(),
								procedureStandalone.getName(),
								dateString);
						filePath = directoryPath.resolve(fileName);
						expObj.setPath(filePath);
						setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
						break;
					case FUNCTION:
						expObj = new ExportObject<>(ExportObject.Type.FUNCTION, procedureStandalone);
						expObjs.add(expObj);
						objectName = String.format("<%s>%s.%s",
								procedureStandalone.getSchema().getParent().getName(),
								procedureStandalone.getSchema().getName(),
								procedureStandalone.getName());
						directoryPath = RuntimeUtils.getUserHomeDir().toPath();
						fileName = String.format("%s_%s_%s_%s_%s.sql",
								ExportObject.Type.FUNCTION,
								procedureStandalone.getSchema().getParent().getName(),
								procedureStandalone.getSchema().getName(),
								procedureStandalone.getName(),
								dateString);
						filePath = directoryPath.resolve(fileName);
						expObj.setPath(filePath);
						setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
						break;
					default:
						throw new IllegalStateException("未知存储类型：" + type);
				}
			} else if (object instanceof Trigger) {
				Trigger trigger = (Trigger) object;
				ExportObject<Trigger> expObj = new ExportObject<>(ExportObject.Type.TRIGGER, trigger);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s.%s",
						trigger.getSchema().getParent().getName(),
						trigger.getSchema().getName(),
						trigger.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s_%s.sql",
						ExportObject.Type.TRIGGER,
						trigger.getSchema().getParent().getName(),
						trigger.getSchema().getName(),
						trigger.getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof Synonym) {
				Synonym synonym = (Synonym) object;
				ExportObject<Synonym> expObj = new ExportObject<>(ExportObject.Type.SYNONYM, synonym);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s.%s",
						synonym.getSchema().getParent().getName(),
						synonym.getSchema().getName(),
						synonym.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s_%s.sql",
						ExportObject.Type.SYNONYM,
						synonym.getSchema().getParent().getName(),
						synonym.getSchema().getName(),
						synonym.getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			} else if (object instanceof Udt) {
				Udt udt = (Udt) object;
				ExportObject<Udt> expObj = new ExportObject<>(ExportObject.Type.UDT, udt);
				expObjs.add(expObj);
				String objectName = String.format("<%s>%s.%s",
						udt.getSchema().getParent().getName(),
						udt.getSchema().getName(),
						udt.getName());
				Path directoryPath = RuntimeUtils.getUserHomeDir().toPath();
				String fileName = String.format("%s_%s_%s_%s_%s.sql",
						ExportObject.Type.UDT,
						udt.getSchema().getParent().getName(),
						udt.getSchema().getName(),
						udt.getName(),
						dateString);
				Path filePath = directoryPath.resolve(fileName);
				expObj.setPath(filePath);
				setupDataAndUi(fileSaveShell, objectTable, expObj, objectName, filePath);
			}
		}

		Button exportButton = new Button(fileSaveShell, SWT.PUSH);
		exportButton.setText("开始导出");
		exportButton.addSelectionListener(widgetSelectedAdapter(event -> {
			Parsing parsing = new Parsing();
			TableType tableType = TableType.valueOf(dataSource.getRoleFlag());
			Map<ExportObject<? extends DBSObject>, Exception> exceptions = new HashMap<>();
			try (Connection connection = dataSource.getConnection()) {
				for (ExportObject<? extends DBSObject> object : expObjs) {
					try(Writer writer = Files.newBufferedWriter(object.getPath(), StandardCharsets.UTF_8)) {
						String ddl;
						switch (object.getType()) {
						case DATABASE:
							StringBuilder builder = new StringBuilder();
							builder.append(parsing.loadTheDatabaseDDL(connection, object.getObject().getName(), tableType));
							Database database = (Database) object.getObject();
							database.getSchemas(new LoggingProgressMonitor()).forEach(schema -> {
								builder.append(parsing.loadTheSchemaDDL(connection, schema.getName(), tableType));

								try (Statement statement = connection.createStatement()) {
									String sql =String.format("SELECT JOB_NAME FROM %s_JOBS WHERE DB_ID=%d",
											tableType, database.getId(), schema.getId());
									ResultSet resultSet = statement.executeQuery(sql);
									while (resultSet.next()) {
										builder.append(new DatabaseParsing().loadJobDdl(connection,
												(int) schema.getId(),
												resultSet.getString(1),
												tableType));
									}
								} catch (SQLException e) {
									throw new IllegalStateException(e);
								}
								
								try (Statement statement = connection.createStatement()) {
									String sql =String.format("SELECT USER_NAME FROM %s_USERS WHERE DB_ID=%d AND IS_ROLE=TRUE",
											tableType, database.getId(), schema.getId());
									ResultSet resultSet = statement.executeQuery(sql);
									while (resultSet.next()) {
										builder.append(parsing.loadTheRoleDDL(connection, resultSet.getString(1)));
									}
								} catch (SQLException e) {
									throw new IllegalStateException(e);
								}
								
								try (Statement statement = connection.createStatement()) {
									String sql =String.format("SELECT USER_NAME FROM %s_USERS WHERE DB_ID=%d AND IS_ROLE=FALSE",
											tableType, database.getId(), schema.getId());
									ResultSet resultSet = statement.executeQuery(sql);
									while (resultSet.next()) {
										builder.append(parsing.loadTheUserDDL(connection,
												resultSet.getString(1),
												"CHANGE_PASSWORD",
												tableType));
									}
								} catch (SQLException e) {
									throw new IllegalStateException(e);
								}
								
								builder.append(parsing.getSchemaDDL(connection, schema.getName(), tableType));
							});
							ddl = builder.toString();
							break;
						case SCHEMA:
							builder = new StringBuilder();
							builder.append(parsing.loadTheSchemaDDL(connection, object.getObject().getName(), tableType));
							builder.append(parsing.getSchemaDDL(connection, object.getObject().getName(), tableType));
							ddl = builder.toString();
							break;
						case ROLE:
							switch (tableType) {
							case DBA:
							case SYS:
								ddl = parsing.loadTheRoleDDL(connection, object.getObject().getName());
								break;
							default:
								throw new IllegalStateException("角色导出必须以 DBA 或 SYSDBA 角色登录");
							}
							break;
						case USER:
							switch (tableType) {
							case DBA:
							case SYS:
								ddl = parsing.loadTheUserDDL(connection,
										object.getObject().getName(),
										"CHANGE_PASSWORD",
										tableType);
								break;
							default:
								throw new IllegalStateException("用户导出必须以 DBA 或 SYSDBA 角色登录");
							}
							break;
						case JOB:
							SchedulerJob job = (SchedulerJob) object.getObject();
							ddl = new DatabaseParsing().loadJobDdl(connection,
									job.getParent().getId(),
									job.getName(),
									tableType);
							break;
						case TABLE:
							Table table = (Table) object.getObject();
							ddl = parsing.loadTableDDL(connection,
									table.getSchema().getName(),
									table.getName(),
									tableType);
							break;
						case VIEW:
							View view = (View) object.getObject();
							ddl = parsing.loadViewDDL(connection,
									view.getSchema().getName(),
									view.getName(),
									tableType);
							break;
						case SEQUENCE:
							Sequence sequence = (Sequence) object.getObject();
							ddl = parsing.loadSequenceDDL(connection,
									sequence.getSchema().getName(),
									sequence.getName(),
									tableType);
							break;
						case PACKAGE:
							Package pkg = (Package) object.getObject();
							ddl = parsing.loadPackageDDL(connection,
									pkg.getSchema().getName(),
									pkg.getName(),
									tableType);
							break;
						case PROCEDURE:
							ProcedureStandalone procedure = (ProcedureStandalone) object.getObject();
							ddl = parsing.loadProcedureDDL(connection,
									procedure.getSchema().getName(),
									procedure.getName(),
									tableType);
							break;
						case FUNCTION:
							ProcedureStandalone func = (ProcedureStandalone) object.getObject();
							ddl = parsing.loadFunctionDDL(connection,
									func.getSchema().getName(),
									func.getName(),
									tableType);
							break;
						case SYNONYM:
							Synonym synonym = (Synonym) object.getObject();
							ddl = parsing.loadSynonymDDL(connection,
									synonym.getSchema().getName(),
									synonym.getName(),
									tableType);
							break;
						case TRIGGER:
							Trigger trigger = (Trigger) object.getObject();
							ddl = parsing.loadTriggerDDL(connection,
									trigger.getSchema().getName(),
									trigger.getName(),
									tableType);
							break;
						case UDT:
							Udt udt = (Udt) object.getObject();
							ddl = parsing.loadUDTDDL(connection,
									udt.getSchema().getName(),
									udt.getName(),
									tableType);
							break;
						default:
							throw new IllegalStateException("暂未支持的对象类型：" + object.getType());
						}
						writer.write(ddl);
					} catch (Exception ex) {
						exceptions.put(object, ex);
					}
				}
				if (exceptions.isEmpty()) {
					MessageDialog.openInformation(fileSaveShell, "导出成功", "执行数据库对象导出完成！");
				} else {
					StringBuilder builder = new StringBuilder();
					exceptions.forEach((key, value) -> {
						value.printStackTrace();
						builder.append("[");
						builder.append(key.getType().getName());
						builder.append("]");
						builder.append(key.getObject().getName());
						builder.append("\n");
						String msg = value.getLocalizedMessage();
						if (msg.charAt(msg.length() - 1) == '\0') {
							builder.append(msg.substring(0, msg.length() - 1));
						} else {
							builder.append(msg);
						}
						builder.append("\n\n");
					});
					MessageDialog.openError(fileSaveShell, "导出失败", "下列对象导出失败：\n" + builder);
					return;
				}
			} catch (SQLException ex) {
				MessageDialog.openError(fileSaveShell, "导出失败", "连接获取失败：" + ex.getLocalizedMessage());
				return;
			} catch (Exception ex) {
				MessageDialog.openError(fileSaveShell, "导出失败", "导出时出现异常：" + ex.getLocalizedMessage());
				return;
			}
		}));

		fileSaveShell.pack();
		fileSaveShell.open();
	}

	private static <T extends DBSObject> void setupDataAndUi(Shell fileSaveShell, org.eclipse.swt.widgets.Table objectTable, ExportObject<T> expObj,
			String objectName, Path filePath) {
		TableItem item = new TableItem(objectTable, SWT.NONE);
		TableEditor editor = new TableEditor(objectTable);
		Text objectTypeText = new Text(objectTable, SWT.CENTER);
		objectTypeText.setText(expObj.getType().getName());
		objectTypeText.setEditable(false);
		editor.grabHorizontal = true;
		editor.setEditor(objectTypeText, item, 0);
		editor = new TableEditor(objectTable);
		Text objectNameText = new Text(objectTable, SWT.NONE);
		objectNameText.setText(objectName);
		objectNameText.setEditable(false);
		editor.grabHorizontal = true;
		editor.setEditor(objectNameText, item, 1);
		editor = new TableEditor(objectTable);
		Text filePathText = new Text(objectTable, SWT.LEFT);
		filePathText.setText(filePath.toString());
		filePathText.addMouseListener(mouseDownAdapter(event -> {
			FileDialog dialog = new FileDialog(fileSaveShell, SWT.SAVE);
			Path path = expObj.getPath();
			dialog.setFilterPath(path.getParent().toString());
			dialog.setFilterExtensions(new String[] { "*.sql" });
			dialog.setOverwrite(true);
			dialog.setFileName(path.getFileName().toString());
			String newFilePath = dialog.open();
			if (newFilePath != null) {
				expObj.setPath(Paths.get(newFilePath));
				filePathText.setText(newFilePath);
			}
		}));
		editor.grabHorizontal = true;
		editor.setEditor(filePathText, item, 2);
	}

	private static class ExportObject<T> {
		private final Type type;
		private final T object;
		private Path path;

		public ExportObject(Type type, T object) {
			this.type = type;
			this.object = object;
		}

		public Type getType() {
			return type;
		}

		public T getObject() {
			return object;
		}

		public Path getPath() {
			return path;
		}

		public void setPath(Path path) {
			this.path = path;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((object == null) ? 0 : object.hashCode());
			result = prime * result + ((path == null) ? 0 : path.hashCode());
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
			ExportObject<?> other = (ExportObject<?>) obj;
			if (object == null) {
				if (other.object != null)
					return false;
			} else if (!object.equals(other.object))
				return false;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			if (type != other.type)
				return false;
			return true;
		}

		private static enum Type {
			DATABASE("库"), SCHEMA("模式"), ROLE("角色"), USER("用户"), JOB("定时作业"), TABLE("表"), VIEW("视图"), SEQUENCE("序列"), PACKAGE("包"),
			PROCEDURE("存储过程"), FUNCTION("存储函数"), TRIGGER("触发器"), SYNONYM("同义词"), UDT("自定义数据类型");

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
