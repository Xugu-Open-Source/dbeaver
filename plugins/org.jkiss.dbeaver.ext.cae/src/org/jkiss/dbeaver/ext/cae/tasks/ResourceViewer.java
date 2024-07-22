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
import java.util.stream.Stream;

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

public class ResourceViewer implements IUserInterfaceTool {
	private final String[] resourceVars = new String[] {
			"nio_timeout",          "login_timeout",       "max_idle_time",
			"min_pass_len",         "conn_fail_cnt",       "max_act_conn_num",
			"session_per_user",     "task_sched_grp",      "tcp_thd_num",
			"rsync_thd_num",        "rtran_thd_num",       "max_parallel",
			"cata_parti_num",       "cata_hash_size",      "lock_hash_size",
			"data_buff_mem",        "swap_buff_mem",       "system_sga_mem",
			"xlog_buff_mem",        "max_hash_size",       "max_hash_mem",
			"hash_parti_num",       "max_malloc_once",     "max_task_mem",
			"max_prepare_num",      "max_cursor_num",      "g_max_loop_num",
			"proc_reuse_cnt",       "auto_eje_cast",       "auto_eje_parallel",
			"para_eje_seqscan_num", "ddl_timeout",         "tab_rebuild_limit",
			"idx_delay_del_limit",  "select_table_num",    "default_copy_num",
			"safely_copy_num",      "enable_read_copy2",   "max_hotspot_num",
			"size_per_chunk",       "block_size",          "block_pctfree",
			"init_data_space_num",  "init_temp_space_num", "init_undo_space_num",
			"def_data_space_size",  "def_temp_space_size", "def_undo_space_size"
	};

	@Override
	public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
			throws DBException {
		Iterator<DBSObject> it = objects.iterator();
		DBSObject first = it.next();
		DataSource dataSource = (DataSource) first.getDataSource();
		Shell shell = new Shell(window.getShell(), SWT.SHELL_TRIM);
		shell.setSize(800, 320);
		shell.setText("查看数据库资源");
		shell.setLayout(new GridLayout());

		ToolBar toolBar = new ToolBar(shell, SWT.NONE);
		ToolItem refreshToolItem = new ToolItem(toolBar, SWT.PUSH);
		refreshToolItem.setText("刷新");
		Rectangle clientArea = shell.getClientArea();
		toolBar.setLocation(clientArea.x, clientArea.y);
		toolBar.pack();

		org.eclipse.swt.widgets.Table resourceTable = new org.eclipse.swt.widgets.Table(shell, SWT.BORDER);
		resourceTable.setHeaderVisible(true);
		resourceTable.setLayoutData(new GridData(GridData.FILL_BOTH));
		TableColumn c0 = new TableColumn(resourceTable, SWT.NONE);
		c0.setWidth(70);
		c0.setText("节点ID");
		TableColumn c1 = new TableColumn(resourceTable, SWT.NONE);
		c1.setWidth(150);
		c1.setText("资源名称");
		TableColumn c2 = new TableColumn(resourceTable, SWT.NONE);
		c2.setWidth(70);
		c2.setText("限制值");
		TableColumn c3 = new TableColumn(resourceTable, SWT.CENTER);
		c3.setWidth(80);
		c3.setText("是否全局");
		TableColumn c4 = new TableColumn(resourceTable, SWT.CENTER);
		c4.setWidth(80);
		c4.setText("读写权限");
		TableColumn c5 = new TableColumn(resourceTable, SWT.NONE);
		c5.setWidth(300);
		c5.setText("资源描述");

		fillResourcesData(dataSource, shell, resourceTable);

		refreshToolItem.addSelectionListener(widgetSelectedAdapter(e -> {
			resourceTable.removeAll();
			fillResourcesData(dataSource, shell, resourceTable);
		}));

		shell.open();
	}

	private void fillResourcesData(DataSource dataSource, Shell shell, org.eclipse.swt.widgets.Table table) {
		List<String> vars = Stream.of(resourceVars).map(i -> "'"+i+"'").collect(Collectors.toList());
		final String transQuerySql = String.format("SELECT * FROM SYS_VARS WHERE VAR_NAME IN (%s)", String.join(",", vars));
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(transQuerySql)) {
			while (resultSet.next()) {
				TableItem item = new TableItem(table, SWT.NONE);
				if (connection.getMetaData().getDatabaseMajorVersion() == 11) {
					item.setText(new String[] {
							"",
							resultSet.getString("VAR_NAME"),
							resultSet.getString("VAR_VALUE"),
							resultSet.getString("IS_GLOBAL"),
							resultSet.getString("ACCESS"),
							resultSet.getString("DESCRI")
					});
				} else {
					item.setText(new String[] {
							resultSet.getString("NODEID"),
							resultSet.getString("VAR_NAME"),
							resultSet.getString("VAR_VALUE"),
							resultSet.getString("IS_GLOBAL"),
							resultSet.getString("ACCESS"),
							resultSet.getString("DESCRI")
					});
				}
			}
		} catch (SQLException e) {
			MessageDialog.openError(shell, "资源信息查询失败", e.getLocalizedMessage());
			e.printStackTrace();
		}
	}
}
