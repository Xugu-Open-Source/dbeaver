package org.jkiss.dbeaver.ext.xugu.tasks;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.nio.charset.Charset;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;

import com.xugu.backup.RestoreExecutor;

public class RestoreTool implements IUserInterfaceTool {

	@Override
	public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
			throws DBException {
		Iterator<DBSObject> it = objects.iterator();
		DBSObject first = it.next();
		DataSource dataSource = (DataSource) first.getDataSource();
		Shell infoShell = new Shell(window.getShell());
		infoShell.setText("数据库恢复工具");
		infoShell.setLayout(new GridLayout());
		Composite sourceComp = new Composite(infoShell, SWT.NONE);
		sourceComp.setLayout(new GridLayout(3, false));
		Combo typeCombo = new Combo(sourceComp, SWT.READ_ONLY);
		Text pathText = new Text(sourceComp, SWT.BORDER);
		pathText.setLayoutData(new GridData(200, 19));
		pathText.setMessage("请输入备份文件名称");
		Button comfirmButton = new Button(sourceComp, SWT.PUSH);
		comfirmButton.setText("开始恢复");
		typeCombo.setItems("系统", "库", "模式", "表");
		Composite stackComp = new Composite(infoShell, SWT.NONE);
		StackLayout stackLayout = new StackLayout();
		stackComp.setLayout(stackLayout);
		// 系统恢复
		Composite systemArgsComp = new Composite(stackComp, SWT.NONE);
		systemArgsComp.setLayout(new GridLayout(2, false));
		Label systemSysdbaPasswordLabel = new Label(systemArgsComp, SWT.NONE);
		systemSysdbaPasswordLabel.setText("密码");
		Text systemSysdbaPasswordText = new Text (systemArgsComp, SWT.SINGLE | SWT.BORDER);
		systemSysdbaPasswordText.setLayoutData(new GridData(294, 19));
		systemSysdbaPasswordText.setMessage("请输入 SYSTEM 库 SYSDBA 用户密码");
		systemSysdbaPasswordText.setEchoChar('*');
		// 库恢复
		Composite databaseArgsComp = new Composite(stackComp, SWT.NONE);
		databaseArgsComp.setLayout(new GridLayout(2, false));
		Label databaseSourceDatabaseNameLabel = new Label(databaseArgsComp, SWT.NONE);
		databaseSourceDatabaseNameLabel.setText("源库名称");
		Text databaseSourceDatabaseNameText = new Text (databaseArgsComp, SWT.SINGLE | SWT.BORDER);
		databaseSourceDatabaseNameText.setLayoutData(new GridData(258, 19));
		databaseSourceDatabaseNameText.setMessage("请输入源库名称");
		Label databaseTargetDatabaseNameLabel = new Label(databaseArgsComp, SWT.NONE);
		databaseTargetDatabaseNameLabel.setText("目标库名称");
		Text databaseTargetDatabaseNameText = new Text (databaseArgsComp, SWT.SINGLE | SWT.BORDER);
		databaseTargetDatabaseNameText.setLayoutData(new GridData(258, 19));
		databaseTargetDatabaseNameText.setMessage("请输入目标库名称");

		Group databaseTargetDatabaseGroup = new Group(databaseArgsComp, SWT.NONE);
		databaseTargetDatabaseGroup.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, 2, 1));
		databaseTargetDatabaseGroup.setLayout(new GridLayout(2, false));
		databaseTargetDatabaseGroup.setText("新库创建参数");
		Label databaseTargetDatabaseCharsetLabel = new Label(databaseTargetDatabaseGroup, SWT.NONE);
		databaseTargetDatabaseCharsetLabel.setText("目标库字符集");
		Text databaseTargetDatabaseCharsetText = new Text (databaseTargetDatabaseGroup, SWT.SINGLE | SWT.BORDER);
		databaseTargetDatabaseCharsetText.setLayoutData(new GridData(216, 19));
		databaseTargetDatabaseCharsetText.setMessage("请输入目标库字符集");
		databaseTargetDatabaseCharsetText.setText("UTF-8");
		Label databaseTargetDatabaseZoneOffsetLabel = new Label(databaseTargetDatabaseGroup, SWT.NONE);
		databaseTargetDatabaseZoneOffsetLabel.setText("目标库时间偏移");
		Text databaseTargetDatabaseZoneOffsetText = new Text (databaseTargetDatabaseGroup, SWT.SINGLE | SWT.BORDER);
		databaseTargetDatabaseZoneOffsetText.setLayoutData(new GridData(216, 19));
		databaseTargetDatabaseZoneOffsetText.setMessage("请输入目标库时间偏移");
		databaseTargetDatabaseZoneOffsetText.setText("+08:00");
		// 模式恢复
		Composite schemaArgsComp = new Composite(stackComp, SWT.NONE);
		schemaArgsComp.setLayout(new GridLayout(2, false));
		Label schemaSourceSchemaNameLabel = new Label(schemaArgsComp, SWT.NONE);
		schemaSourceSchemaNameLabel.setText("源模式名称");
		Text schemaSourceSchemaNameText = new Text (schemaArgsComp, SWT.SINGLE | SWT.BORDER);
		schemaSourceSchemaNameText.setLayoutData(new GridData(246, 19));
		schemaSourceSchemaNameText.setMessage("请输入源模式名称");
		Label schemaTargetDatabaseNameLabel = new Label(schemaArgsComp, SWT.NONE);
		schemaTargetDatabaseNameLabel.setText("目标库名称");
		Text schemaTargetDatabaseNameText = new Text (schemaArgsComp, SWT.SINGLE | SWT.BORDER);
		schemaTargetDatabaseNameText.setLayoutData(new GridData(246, 19));
		schemaTargetDatabaseNameText.setMessage("请输入目标库名称");
		Label schemaTargetSchemaNameLabel = new Label(schemaArgsComp, SWT.NONE);
		schemaTargetSchemaNameLabel.setText("目标模式名称");
		Text schemaTargetSchemaNameText = new Text (schemaArgsComp, SWT.SINGLE | SWT.BORDER);
		schemaTargetSchemaNameText.setLayoutData(new GridData(246, 19));
		schemaTargetSchemaNameText.setMessage("请输入目标模式名称");

		Group schemaTargetDatabaseGroup = new Group(schemaArgsComp, SWT.NONE);
		schemaTargetDatabaseGroup.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, 2, 1));
		schemaTargetDatabaseGroup.setLayout(new GridLayout(2, false));
		schemaTargetDatabaseGroup.setText("新库创建参数");
		Label schemaTargetDatabaseCharsetLabel = new Label(schemaTargetDatabaseGroup, SWT.NONE);
		schemaTargetDatabaseCharsetLabel.setText("目标库字符集");
		Text schemaTargetDatabaseCharsetText = new Text (schemaTargetDatabaseGroup, SWT.SINGLE | SWT.BORDER);
		schemaTargetDatabaseCharsetText.setLayoutData(new GridData(216, 19));
		schemaTargetDatabaseCharsetText.setMessage("请输入目标库字符集");
		schemaTargetDatabaseCharsetText.setText("UTF-8");
		Label schemaTargetDatabaseZoneOffsetLabel = new Label(schemaTargetDatabaseGroup, SWT.NONE);
		schemaTargetDatabaseZoneOffsetLabel.setText("目标库时间偏移");
		Text schemaTargetDatabaseZoneOffsetText = new Text (schemaTargetDatabaseGroup, SWT.SINGLE | SWT.BORDER);
		schemaTargetDatabaseZoneOffsetText.setLayoutData(new GridData(216, 19));
		schemaTargetDatabaseZoneOffsetText.setMessage("请输入目标库时间偏移");
		schemaTargetDatabaseZoneOffsetText.setText("+08:00");
		// 表恢复
		Composite tableArgsComp = new Composite(stackComp, SWT.NONE);
		tableArgsComp.setLayout(new GridLayout(2, false));
		Label tableSourceSchemaNameLabel = new Label(tableArgsComp, SWT.NONE);
		tableSourceSchemaNameLabel.setText("源模式名称");
		Text tableSourceSchemaNameText = new Text (tableArgsComp, SWT.SINGLE | SWT.BORDER);
		tableSourceSchemaNameText.setLayoutData(new GridData(246, 19));
		tableSourceSchemaNameText.setMessage("请输入源模式名称");
		Label tableSourceTableNameLabel = new Label(tableArgsComp, SWT.NONE);
		tableSourceTableNameLabel.setText("源表名称");
		Text tableSourceTableNameText = new Text (tableArgsComp, SWT.SINGLE | SWT.BORDER);
		tableSourceTableNameText.setLayoutData(new GridData(246, 19));
		tableSourceTableNameText.setMessage("请输入源表名称");
		Label tableTargetDatabaseNameLabel = new Label(tableArgsComp, SWT.NONE);
		tableTargetDatabaseNameLabel.setText("目标库名称");
		Text tableTargetDatabaseNameText = new Text (tableArgsComp, SWT.SINGLE | SWT.BORDER);
		tableTargetDatabaseNameText.setLayoutData(new GridData(246, 19));
		tableTargetDatabaseNameText.setMessage("请输入目标库名称");
		Label tableTargetSchemaNameLabel = new Label(tableArgsComp, SWT.NONE);
		tableTargetSchemaNameLabel.setText("目标模式名称");
		Text tableTargetSchemaNameText = new Text (tableArgsComp, SWT.SINGLE | SWT.BORDER);
		tableTargetSchemaNameText.setLayoutData(new GridData(246, 19));
		tableTargetSchemaNameText.setMessage("请输入目标模式名称");
		Label tableTargetTableNameLabel = new Label(tableArgsComp, SWT.NONE);
		tableTargetTableNameLabel.setText("目标表名称");
		Text tableTargetTableNameText = new Text (tableArgsComp, SWT.SINGLE | SWT.BORDER);
		tableTargetTableNameText.setLayoutData(new GridData(246, 19));
		tableTargetTableNameText.setMessage("请输入目标表名称");

		Group tableTargetDatabaseGroup = new Group(tableArgsComp, SWT.NONE);
		tableTargetDatabaseGroup.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, 2, 1));
		tableTargetDatabaseGroup.setLayout(new GridLayout(2, false));
		tableTargetDatabaseGroup.setText("新库创建参数");
		Label tableTargetDatabaseCharsetLabel = new Label(tableTargetDatabaseGroup, SWT.NONE);
		tableTargetDatabaseCharsetLabel.setText("目标库字符集");
		Text tableTargetDatabaseCharsetText = new Text (tableTargetDatabaseGroup, SWT.SINGLE | SWT.BORDER);
		tableTargetDatabaseCharsetText.setLayoutData(new GridData(216, 19));
		tableTargetDatabaseCharsetText.setMessage("请输入目标库字符集");
		tableTargetDatabaseCharsetText.setText("UTF-8");
		Label tableTargetDatabaseZoneOffsetLabel = new Label(tableTargetDatabaseGroup, SWT.NONE);
		tableTargetDatabaseZoneOffsetLabel.setText("目标库时间偏移");
		Text tableTargetDatabaseZoneOffsetText = new Text (tableTargetDatabaseGroup, SWT.SINGLE | SWT.BORDER);
		tableTargetDatabaseZoneOffsetText.setLayoutData(new GridData(216, 19));
		tableTargetDatabaseZoneOffsetText.setMessage("请输入目标库时间偏移");
		tableTargetDatabaseZoneOffsetText.setText("+08:00");

		Group tableTargetSchemaGroup = new Group(tableArgsComp, SWT.NONE);
		tableTargetSchemaGroup.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, true, 2, 1));
		tableTargetSchemaGroup.setLayout(new GridLayout(2, false));
		tableTargetSchemaGroup.setText("新模式创建参数");
		Label tableTargetSchemaOwnerNameLabel = new Label(tableTargetSchemaGroup, SWT.NONE);
		tableTargetSchemaOwnerNameLabel.setText("目标模式属主名称");
		Text tableTargetSchemaOwnerNameText = new Text (tableTargetSchemaGroup, SWT.SINGLE | SWT.BORDER);
		tableTargetSchemaOwnerNameText.setLayoutData(new GridData(204, 19));
		tableTargetSchemaOwnerNameText.setMessage("请输入目标模式属主名称");
		tableTargetSchemaOwnerNameText.setText(dataSource.getContainer().getConnectionConfiguration().getUserName());

		typeCombo.addSelectionListener(widgetSelectedAdapter(event -> {
			Combo combo = (Combo) event.getSource();
			String selectedType = combo.getText();
			switch (selectedType) {
				case "系统":
					stackLayout.topControl = systemArgsComp;
					break;
				case "库":
					stackLayout.topControl = databaseArgsComp;
					break;
				case "模式":
					stackLayout.topControl = schemaArgsComp;
					break;
				case "表":
					stackLayout.topControl = tableArgsComp;
					break;
				default:
					throw new IllegalStateException("未支持的类型：" + selectedType);
			}
			stackComp.layout();
		}));
		comfirmButton.addSelectionListener(widgetSelectedAdapter(event -> {
			RestoreExecutor executor = new RestoreExecutor(dataSource);
			String selectedType = typeCombo.getText();
			try {
				switch (selectedType) {
					case "系统":
						executor.forSystem(systemSysdbaPasswordText.getText(), pathText.getText());
						break;
					case "库":
						executor.forCatalog(databaseSourceDatabaseNameText.getText(),
								databaseTargetDatabaseNameText.getText(),
								Charset.forName(databaseTargetDatabaseCharsetText.getText()),
								ZoneOffset.of(databaseTargetDatabaseZoneOffsetText.getText()),
								pathText.getText());
						break;
					case "模式":
						executor.forSchema(schemaSourceSchemaNameText.getText(),
								schemaTargetDatabaseNameText.getText(),
								Charset.forName(schemaTargetDatabaseCharsetText.getText()),
								ZoneOffset.of(schemaTargetDatabaseZoneOffsetText.getText()),
								schemaTargetSchemaNameText.getText(),
								pathText.getText());
						break;
					case "表":
						executor.forTable(tableSourceSchemaNameText.getText(),
								tableSourceTableNameText.getText(),
								tableTargetDatabaseNameText.getText(),
								Charset.forName(tableTargetDatabaseCharsetText.getText()),
								ZoneOffset.of(tableTargetDatabaseZoneOffsetText.getText()),
								tableTargetSchemaNameText.getText(),
								tableTargetSchemaOwnerNameText.getText(),
								tableTargetTableNameText.getText(),
								pathText.getText());
						break;
					default:
						throw new IllegalStateException("未支持的类型：" + selectedType);
				}
			} catch (Exception e) {
				MessageDialog.openError(infoShell, "恢复失败", e.getLocalizedMessage());
				e.printStackTrace();
			}
			MessageDialog.openInformation(infoShell, "恢复成功", "执行数据库对象恢复完成！");
		}));

		typeCombo.select(3);
		stackLayout.topControl = tableArgsComp;
		stackComp.layout();

		infoShell.pack();
		infoShell.open();
	}
}
