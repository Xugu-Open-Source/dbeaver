package org.jkiss.dbeaver.ext.cae.tasks;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cae.model.DataSource;
import org.jkiss.dbeaver.ext.cae.model.Database;
import org.jkiss.dbeaver.ext.cae.model.Schema;
import org.jkiss.dbeaver.ext.cae.model.Table;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;

public class TransactionKiller implements IUserInterfaceTool {
	private final List<TableEditor> editors = new ArrayList<>();

	@Override
	public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
			throws DBException {
		Iterator<DBSObject> it = objects.iterator();
		DBSObject first = it.next();
		DataSource dataSource = (DataSource) first.getDataSource();
		Shell killShell = new Shell(window.getShell());
		killShell.setText("数据库事务查杀");
		killShell.setLayout(new GridLayout());

		ToolBar toolBar = new ToolBar(killShell, SWT.NONE);
		ToolItem refreshToolItem = new ToolItem(toolBar, SWT.PUSH);
		refreshToolItem.setText("刷新");
		Rectangle clientArea = killShell.getClientArea();
		toolBar.setLocation(clientArea.x, clientArea.y);
		toolBar.pack();

		org.eclipse.swt.widgets.Table transactionTable = new org.eclipse.swt.widgets.Table(killShell, SWT.BORDER);
		transactionTable.setLinesVisible(true);
		transactionTable.setHeaderVisible(true);
		transactionTable.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		TableColumn c0 = new TableColumn(transactionTable, SWT.NONE);
		c0.setWidth(100);
		c0.setText("节点ID");
		TableColumn c1 = new TableColumn(transactionTable, SWT.NONE);
		c1.setWidth(100);
		c1.setText("事务号");
		TableColumn c2 = new TableColumn(transactionTable, SWT.NONE);
		c2.setWidth(200);
		c2.setText("开始时间");
		TableColumn c3 = new TableColumn(transactionTable, SWT.NONE);
		c3.setWidth(100);
		c3.setText("根事务代理节点");
		TableColumn c4 = new TableColumn(transactionTable, SWT.NONE);
		c4.setWidth(100);
		c4.setText("是否为代理事务");
		TableColumn c5 = new TableColumn(transactionTable, SWT.NONE);
		c5.setWidth(100);
		c5.setText("根事务节点ID");
		TableColumn c6 = new TableColumn(transactionTable, SWT.NONE);
		c6.setWidth(100);
		c6.setText("根事务号");
		TableColumn c7 = new TableColumn(transactionTable, SWT.NONE);
		c7.setWidth(100);
		c7.setText("待同步数");
		TableColumn c8 = new TableColumn(transactionTable, SWT.NONE);
		c8.setWidth(100);
		c8.setText("已完成同步数");
		TableColumn c9 = new TableColumn(transactionTable, SWT.NONE);
		c9.setWidth(100);
		c9.setText("所属会话ID");
		TableColumn c10 = new TableColumn(transactionTable, SWT.NONE);
		c10.setWidth(100);
		c10.setText("操作");

		fillTransData(dataSource, killShell, transactionTable);

		refreshToolItem.addSelectionListener(widgetSelectedAdapter(e -> {
			clearTransData(transactionTable);
			fillTransData(dataSource, killShell, transactionTable);
			killShell.pack();
		}));

		killShell.pack();
		killShell.open();
	}

	private void clearTransData(org.eclipse.swt.widgets.Table transactionTable) {
		for (TableEditor editor : editors) {
			editor.getEditor().dispose();
		}
		editors.clear();
		transactionTable.removeAll();
	}

	private void fillTransData(DataSource dataSource, Shell killShell, org.eclipse.swt.widgets.Table transactionTable) {
		final String transQuerySql = "SELECT T.*,S.SESSION_ID FROM SYS_ALL_TRANS T JOIN SYS_ALL_SESSIONS S ON T.NODEID=S.NODEID AND T.TRANID=S.CURR_TID";
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(transQuerySql)) {
			while (resultSet.next()) {
				TableItem item = new TableItem(transactionTable, SWT.NONE);
				TableEditor editor = new TableEditor(transactionTable);
				Text nodeIdText = new Text(transactionTable, SWT.NONE);
				nodeIdText.setText(resultSet.getString("NODEID"));
				nodeIdText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(nodeIdText, item, 0);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Text tranIdText = new Text(transactionTable, SWT.NONE);
				tranIdText.setText(resultSet.getString("TRANID"));
				tranIdText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(tranIdText, item, 1);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Text startTimeText = new Text(transactionTable, SWT.NONE);
				startTimeText.setText(resultSet.getString("START_T"));
				startTimeText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(startTimeText, item, 2);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Text rstubsText = new Text(transactionTable, SWT.NONE);
				rstubsText.setText(resultSet.getString("RSTUBS"));
				rstubsText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(rstubsText, item, 3);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Text isProxyText = new Text(transactionTable, SWT.NONE);
				isProxyText.setText(resultSet.getString("IS_PROXY"));
				isProxyText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(isProxyText, item, 4);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Text rootNodeIdText = new Text(transactionTable, SWT.NONE);
				rootNodeIdText.setText(resultSet.getString("R_NODE"));
				rootNodeIdText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(rootNodeIdText, item, 5);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Text rootTransIdText = new Text(transactionTable, SWT.NONE);
				rootTransIdText.setText(resultSet.getString("R_TRANSID"));
				rootTransIdText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(rootTransIdText, item, 6);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Text waitSyncText = new Text(transactionTable, SWT.NONE);
				waitSyncText.setText(resultSet.getString("WANT_SYNC"));
				waitSyncText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(waitSyncText, item, 7);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Text doneSyncText = new Text(transactionTable, SWT.NONE);
				doneSyncText.setText(resultSet.getString("DONE_SYNC"));
				doneSyncText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(doneSyncText, item, 8);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Text sessionIdText = new Text(transactionTable, SWT.NONE);
				sessionIdText.setText(resultSet.getString("SESSION_ID"));
				sessionIdText.setEditable(false);
				editor.grabHorizontal = true;
				editor.setEditor(sessionIdText, item, 9);
				editors.add(editor);

				editor = new TableEditor(transactionTable);
				Button button = new Button(transactionTable, SWT.PUSH);
				button.setText("强制停止");
				button.pack();
				button.addSelectionListener(widgetSelectedAdapter(e -> {
					if (!"SYSTEM".equalsIgnoreCase(dataSource.getDatabase().getName()) || !"SYSDBA".equalsIgnoreCase(dataSource.getContainer().getConnectionConfiguration().getUserName())) {
						MessageDialog.openWarning(killShell, "警告", "事务查杀功能属于高风险操作，仅允许 SYSTEM 库下 SYSDBA 用户使用");
						return;
					}
					if (MessageDialog.openConfirm(killShell, "确认事务强制停止", String.format(
							 "此操作可能造成事务正在处理的数据丢失，请谨慎考虑！\n您确认要强制停止节点（%s）事务（%s）吗？",
							 nodeIdText.getText(), tranIdText.getText()))) {
						try (Connection killConnection = dataSource.getConnection();
								Statement killStatement = killConnection.createStatement()) {
							killStatement.execute(String.format("DBMS_DBA.KILL_TRANS(%s, %s)", nodeIdText.getText(), tranIdText.getText()));
							MessageDialog.openInformation(killShell,
									"事务强制停止成功", String.format("强制停止数据库节点（%s）事务（%s）成功！",
									nodeIdText.getText(), tranIdText.getText()));
							clearTransData(transactionTable);
							fillTransData(dataSource, killShell, transactionTable);
							killShell.pack();
						} catch (SQLException ex) {
							MessageDialog.openError(killShell, "事务强制停止失败", ex.getLocalizedMessage());
							ex.printStackTrace();
						}
					}
				}));
				editor.minimumWidth = button.getSize().x;
				editor.horizontalAlignment = SWT.LEFT;
				editor.setEditor(button, item, 10);
				editors.add(editor);
			}
		} catch (SQLException e) {
			MessageDialog.openError(killShell, "事务信息查询失败", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
}
