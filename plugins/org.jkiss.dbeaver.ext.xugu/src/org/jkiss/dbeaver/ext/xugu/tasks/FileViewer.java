//package org.jkiss.dbeaver.ext.xugu.tasks;
//
//import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;
//
//import java.nio.file.Paths;
//import java.sql.Connection;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.List;
//
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.dnd.Clipboard;
//import org.eclipse.swt.dnd.TextTransfer;
//import org.eclipse.swt.dnd.Transfer;
//import org.eclipse.swt.layout.FillLayout;
//import org.eclipse.swt.layout.GridData;
//import org.eclipse.swt.widgets.Display;
//import org.eclipse.swt.widgets.Menu;
//import org.eclipse.swt.widgets.MenuItem;
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.swt.widgets.Tree;
//import org.eclipse.swt.widgets.TreeColumn;
//import org.eclipse.swt.widgets.TreeItem;
//import org.eclipse.ui.IWorkbenchPart;
//import org.eclipse.ui.IWorkbenchWindow;
//import org.jkiss.dbeaver.DBException;
//import org.jkiss.dbeaver.ext.xugu.model.DataSource;
//import org.jkiss.dbeaver.model.struct.DBSObject;
//import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
//
//public class FileViewer implements IUserInterfaceTool {
//	@Override
//	public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
//			throws DBException {
//		DBSObject first = objects.iterator().next();
//		DataSource dataSource = (DataSource) first.getDataSource();
//		Display display = window.getShell().getDisplay();
//		Shell shell = new Shell(window.getShell(), SWT.SHELL_TRIM);
//		shell.setMinimumSize(800, 320);
//		shell.setText("查看数据库文件");
//		shell.setLayout(new FillLayout());
//		Tree tree = new Tree(shell, SWT.H_SCROLL | SWT.V_SCROLL);
//		tree.setHeaderVisible(true);
//		tree.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		TreeColumn column1 = new TreeColumn(tree, SWT.NONE);
//		column1.setText("实体名称");
//		column1.setWidth(400);
//		TreeColumn column2 = new TreeColumn(tree, SWT.NONE);
//		column2.setText("操作系统路径");
//		column2.setWidth(300);
//		TreeColumn column3 = new TreeColumn(tree, SWT.CENTER);
//		column3.setText("路径类型");
//		column3.setWidth(100);
//
//		List<FileEntity> rootEntitis;
//		try (Connection connection = dataSource.getConnection();
//			Statement statement = connection.createStatement()) {
//			rootEntitis = queryFilesRecursive(statement, "/");
//		} catch (SQLException e) {
//			throw new IllegalStateException(e);
//		}
//
//		Menu menu = new Menu(tree);
//		MenuItem copyNameMenu = new MenuItem(menu, SWT.NONE);
//		copyNameMenu.setText("复制实体名称");
//		copyNameMenu.addSelectionListener(widgetSelectedAdapter(event -> {
//			TreeItem selection = tree.getSelection()[0];
//            Clipboard clipboard = new Clipboard(display);
//            clipboard.setContents(
//            		new String[]{selection.getText()},
//            		new Transfer[]{TextTransfer.getInstance()});
//            clipboard.dispose();
//        }));
//		MenuItem copyDbPathMenu = new MenuItem(menu, SWT.NONE);
//		copyDbPathMenu.setText("复制数据库路径");
//		copyDbPathMenu.addSelectionListener(widgetSelectedAdapter(event -> {
//			TreeItem selection = tree.getSelection()[0];
//			FileEntity entity = (FileEntity) selection.getData();
//            Clipboard clipboard = new Clipboard(display);
//            clipboard.setContents(
//            		new String[]{entity.getDbPath()},
//            		new Transfer[]{TextTransfer.getInstance()});
//            clipboard.dispose();
//        }));
//		MenuItem copyOsPathMenu = new MenuItem(menu, SWT.NONE);
//		copyOsPathMenu.setText("复制操作系统路径");
//		copyOsPathMenu.addSelectionListener(widgetSelectedAdapter(event -> {
//			TreeItem selection = tree.getSelection()[0];
//			FileEntity entity = (FileEntity) selection.getData();
//            Clipboard clipboard = new Clipboard(display);
//            clipboard.setContents(
//            		new String[]{entity.getOsPath()},
//            		new Transfer[]{TextTransfer.getInstance()});
//            clipboard.dispose();
//        }));
//		tree.setMenu(menu);
//
//		for (FileEntity entity : rootEntitis) {
//			TreeItem item = new TreeItem(tree, SWT.NONE);
//			item.setData(entity);
//			item.setText(entity.getViewData());
//			List<FileEntity> itemChildren = entity.getChildren();
//			if (itemChildren.size() > 0) {
//				buildTree(display, item, itemChildren);
//			}
//		}
//
//		shell.pack();
//		shell.open();
//	}
//
//	private void buildTree(Display display, TreeItem root, List<FileEntity> children) {
//		for (FileEntity entity : children) {
//			TreeItem item = new TreeItem(root, SWT.NONE);
//			item.setData(entity);
//			item.setText(entity.getViewData());
//			List<FileEntity> itemChildren = entity.getChildren();
//			if (itemChildren.size() > 0) {
//				buildTree(display, item, itemChildren);
//			}
//		}
//	}
//
//	private List<FileEntity> queryFilesRecursive(Statement statement, String dbDirPath) {
//		try (ResultSet resultSet = statement.executeQuery("SHOW DIR '" + dbDirPath + "'")) {
//			List<FileEntity> result = new ArrayList<>();
//			while (resultSet.next()) {
//				String dbPath = resultSet.getString("DB_PATH");
//				String osPath = resultSet.getString("OS_PATH");
//				boolean isDir = resultSet.getBoolean("IS_DIR");
//				List<FileEntity> children = new ArrayList<>();
//				if (isDir) { children.addAll(queryFilesRecursive(statement, dbPath)); }
//				FileEntity fileEntity = new FileEntity(dbPath, osPath, isDir, children);
//				result.add(fileEntity);
//			}
//			return result;
//		} catch (SQLException e) {
//			throw new IllegalStateException(e);
//		}
//	}
//
//	private static class FileEntity {
//		private final String dbPath;
//		private final String osPath;
//		private final boolean isDir;
//		private final String[] viewData;
//		private final List<FileEntity> children;
//
//		public FileEntity(String dbPath, String osPath, boolean isDir, List<FileEntity> children) {
//			this.dbPath = dbPath;
//			this.osPath = osPath;
//			this.isDir = isDir;
//			this.viewData = new String[] {
//					Paths.get(dbPath).getFileName().toString(),
//					osPath,
//					isDir ? "目录" : "文件"
//			};
//			this.children = children;
//		}
//
//		public String getDbPath() {
//			return dbPath;
//		}
//
//		public String getOsPath() {
//			return osPath;
//		}
//
//		public boolean isDir() {
//			return isDir;
//		}
//
//		public String[] getViewData() {
//			return viewData;
//		}
//
//		public List<FileEntity> getChildren() {
//			return children;
//		}
//	}
//}
