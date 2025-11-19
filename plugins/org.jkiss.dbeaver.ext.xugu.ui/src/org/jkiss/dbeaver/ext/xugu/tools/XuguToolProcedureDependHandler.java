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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.ext.xugu.tasks.XuguToolAbstractHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Vector;

/**
 * 分析 XuguDB 中表对象的依赖关系
 */
public class XuguToolProcedureDependHandler extends XuguToolAbstractHandler {

    private static final String APPLICATION_PLUGIN_ID = "org.jkiss.dbeaver.ui.app.standalone";

    /**
     * 此表所有依赖的对象
     */
    private static final String DEPEND_OBJECT1 =
        "select schema_id,obj_id,obj_type from {2}_objects where obj_id in(select d.obj_id1 from {2}_objects o inner join {2}_depends d on o.user_id=d.owner_id2 and o.obj_id=d.obj_id2 and o.db_id=d.db_id inner join {2}_schemas s on o.schema_id=s.schema_id and o.db_id=s.db_id where o.obj_id={1} and o.obj_type=7 and s.schema_id={0} and o.db_id=current_db_id) and db_id = current_db_id";
    /**
     * 所有依赖于此表的对象
     */
    private static final String DEPEND_OBJECT2 =
        "select schema_id,obj_id,obj_type from {2}_objects where obj_id in(select d.obj_id2 from {2}_objects o inner join {2}_depends d on o.user_id=d.owner_id1 and o.obj_id=d.obj_id1 and o.db_id=d.db_id inner join {2}_schemas s on o.schema_id=s.schema_id and o.db_id=s.db_id where o.obj_id={1} and o.obj_type=7 and s.schema_id={0} and o.db_id=current_db_id) and db_id = current_db_id";
    private static final String DEPEND_TABLE =
        "select s.schema_name,t.table_name,''Table'' from {2}_tables t join {2}_schemas s on s.db_id = t.db_id  where t.schema_id=s.schema_id and s.schema_id={0} and t.table_id={1} and s.db_id= current_db_id and s.db_id=current_db_id";
    private static final String DEPEND_PF_INFO =
        "select s.schema_name,p.proc_name,''Procedure'' from {4}_procedures p,{4}_schemas s where p.schema_id=s.schema_id and s.schema_id={0} and p.proc_id={1} and p.ret_type isnull union select s.schema_name,p.proc_name,''Function'' from {4}_procedures p,{4}_schemas s where p.schema_id=s.schema_id and s.schema_id={2} and p.proc_id={3} and p.ret_type notnull and s.db_id=current_db_id";
    private static final String DEPEND_VIEW =
        "select s.schema_name,v.view_name,''View'' from {2}_views v,{2}_schemas s where v.schema_id=s.schema_id and s.schema_id={0} and v.view_id={1} and s.db_id=current_db_id";
    private static final String DEPEND_TRIGGER =
        "select s.schema_name,t.trig_name,''Trigger'' from {2}_triggers t,{2}_schemas s where t.schema_id=s.schema_id and s.schema_id={0} and t.trig_id={1} and s.db_id=current_db_id";
    private static final String DEPEND_PACKAGE =
        "select s.schema_name,p.pack_name,''Package'' from {2}_packages p,{2}_schemas s where p.schema_id=s.schema_id and s.schema_id={0} and p.pack_id={1} and s.db_id=current_db_id";

    @Override
    public void XuguToolSubclassReach(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) {
        org.jkiss.dbeaver.ext.xugu.model.ProcedureStandalone firstProcedure =
            (org.jkiss.dbeaver.ext.xugu.model.ProcedureStandalone) (((ArrayList<?>) objects).getFirst());
        String schemaName = firstProcedure.getSchema().getName();
        String name = firstProcedure.getName();
        Display display = window.getShell().getDisplay();
        Shell shell = new Shell(window.getShell(), SWT.SHELL_TRIM);
        shell.setMinimumSize(450, 300);
        shell.setText("查看依赖关系 - " + schemaName + "." + name);
        shell.setLayout(new GridLayout(1, true));
        Image dbeaverIcon = AbstractUIPlugin.imageDescriptorFromPlugin(APPLICATION_PLUGIN_ID, "icons/dbeaver32.png").createImage();
        shell.setImage(dbeaverIcon);
        dbeaverIcon.dispose();

        CTabFolder folder = new CTabFolder(shell, SWT.BORDER);
        folder.setSimple(false);
        folder.setTabPosition(SWT.TOP);
        folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // tab页1
        CTabItem tabItem1 = new CTabItem(folder, SWT.NONE);
        Composite content1 = new Composite(folder, SWT.NONE);
        content1.setLayout(new GridLayout(1, false));
        tabItem1.setText(name + "依赖的对象");
        Table table1 = createTable(content1);
        tabItem1.setControl(content1);

        // tab页2
        CTabItem tabItem2 = new CTabItem(folder, SWT.NONE);
        tabItem2.setText("依赖于" + name + "的对象");
        Composite content2 = new Composite(folder, SWT.NONE);
        content2.setLayout(new GridLayout(1, false));
        Table table2 = createTable(content2);
        tabItem2.setControl(content2);

        try (Connection currentConn = firstProcedure.getDataSource().getConnection()) {
            setData(currentConn, firstProcedure, table1, DEPEND_OBJECT1);
            setData(currentConn, firstProcedure, table2, DEPEND_OBJECT2);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        folder.setSelection(0);
        shell.open();

        while (!shell.isDisposed()) {
            while (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        shell.dispose();
    }

    /**
     *
     * @param firstProcedure  当前数据
     * @param table           tab页表格选择
     * @param dependObjectSQL 查询对象之间依赖关系sql
     */
    private static void setData(Connection currentConn, org.jkiss.dbeaver.ext.xugu.model.ProcedureStandalone firstProcedure, Table table,
                                String dependObjectSQL) {

        Vector<Vector<Integer>> allDependObjects = new Vector<>();
        long schemaId = firstProcedure.getSchema().getId();
        long id = firstProcedure.getObjectId();
        String roleFlag = firstProcedure.getSchema().getRoleFlag().toLowerCase(Locale.ENGLISH);
        String[] parameters = {Long.toString(schemaId), Long.toString(id), roleFlag};
        MessageFormat messageFormat = new MessageFormat(dependObjectSQL);
        String sql = messageFormat.format(parameters);
        try (Statement stmt = currentConn.createStatement();
             ResultSet resultSet = stmt.executeQuery(sql)) {
            while (resultSet.next()) {
                Vector<Integer> singleDependObjects = new Vector<>();
                singleDependObjects.add(resultSet.getInt(1));
                singleDependObjects.add(resultSet.getInt(2));
                singleDependObjects.add(resultSet.getInt(3));
                allDependObjects.add(singleDependObjects);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Get all depend Objects fault.", e);
        }

        for (int i = 0; i < allDependObjects.size(); i++) {
            sql = "";
            parameters =
                new String[] {Integer.toString(allDependObjects.get(i).get(0)),
                    Integer.toString(allDependObjects.get(i).get(1)), roleFlag};
            switch (allDependObjects.get(i).get(2)) {
                // 表
                case 5:
                    messageFormat = new MessageFormat(DEPEND_TABLE);
                    sql = messageFormat.format(parameters);
                    break;
                // 函数或过程
                case 7:
                    parameters = new String[] {Integer.toString(allDependObjects.get(i).get(0)),
                        Integer.toString(allDependObjects.get(i).get(1)), Integer.toString(allDependObjects.get(i).get(0)),
                        Integer.toString(allDependObjects.get(i).get(1)), roleFlag};
                    messageFormat = new MessageFormat(DEPEND_PF_INFO);
                    sql = messageFormat.format(parameters);
                    break;
                // 视图
                case 9:
                    messageFormat = new MessageFormat(DEPEND_VIEW);
                    sql = messageFormat.format(parameters);
                    break;
                // 触发器
                case 11:
                    messageFormat = new MessageFormat(DEPEND_TRIGGER);
                    sql = messageFormat.format(parameters);
                    break;
                // 包
                case 18:
                    messageFormat = new MessageFormat(DEPEND_PACKAGE);
                    sql = messageFormat.format(parameters);
                    break;
                default:
                    break;
            }
            if (!sql.isEmpty()) {
                try (Statement stmt = currentConn.createStatement();
                     ResultSet resultSet = stmt.executeQuery(sql)) {
                    if (resultSet.next()) {
                        TableItem item = new TableItem(table, SWT.NONE);
                        item.setText(new String[] {
                            resultSet.getString(1),
                            resultSet.getString(2),
                            resultSet.getString(3)});
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Get all depend objects fault.", e);
                }
            }
        }
    }

    /**
     * 创建表格
     *
     * @param parent CTabFolder
     * @return 表格
     */
    private Table createTable(Composite parent) {
        Table table = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        // 设置表格填充并抓取额外空间
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        String[] columnNames = {"所属模式", "对象名", "对象类型"};
        for (int i = 0; i < columnNames.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(columnNames[i]);
        }

        table.addListener(SWT.Resize, event -> {
            int width = table.getClientArea().width;
            TableColumn[] columns = table.getColumns();
            int colWidth = width / columns.length;
            for (TableColumn col : columns) {
                col.setWidth(colWidth);
            }
        });

        // 表格内容支持复制
        table.addListener(SWT.KeyDown, e -> {
            if ((e.stateMask & SWT.CTRL) != 0 && e.keyCode == 'c') {
                TableItem[] selection = table.getSelection();
                if (selection.length == 0) {
                    return;
                }

                StringBuilder clipboardData = new StringBuilder();
                int columnCount = table.getColumnCount();
                // 添加选中的数据
                for (TableItem item : selection) {
                    for (int i = 0; i < columnCount; i++) {
                        clipboardData.append(item.getText(i)).append("\t");
                    }
                    clipboardData.append("\n");

                    // 设置到剪贴板
                    Clipboard clipboard = new Clipboard(table.getDisplay());
                    clipboard.setContents(
                        new Object[] {clipboardData.toString()},
                        new Transfer[] {TextTransfer.getInstance()}
                    );
                    clipboard.dispose();
                }
            }
        });


        return table;
    }
}
