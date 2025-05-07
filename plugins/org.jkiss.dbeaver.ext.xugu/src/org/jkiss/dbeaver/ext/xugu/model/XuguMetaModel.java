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

package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.ext.xugu.internal.XuguConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Shengkai Bai
 */
public class XuguMetaModel extends GenericMetaModel {

    private static final Log log = Log.getLog(XuguMetaModel.class);

    public XuguMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new XuguDataSource(monitor, container, this);
    }

//    @Override
//    public JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> createDataTypeCache(@NotNull GenericStructContainer container) {
//        return new DamengDataTypeCache(container);
//    }

    @Override
    public XuguSchema createSchemaImpl(@NotNull GenericDataSource dataSource, GenericCatalog catalog, @NotNull String schemaName) throws DBException {
        return new XuguSchema(dataSource, schemaName, true);
    }

    @Override
    public GenericTableBase createTableOrViewImpl(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new GenericView(
                    container,
                    tableName,
                    tableType,
                    dbResult);
        }
        return new XuguTable(container, tableName, tableType, dbResult);
    }

    @Override
    public GenericTableBase createTableImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @NotNull GenericMetaObject tableObject, @NotNull JDBCResultSet dbResult) {
        return super.createTableImpl(session, owner, tableObject, dbResult);
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }



    @Override
    public boolean supportsDatabaseTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }




    @Override
    public boolean supportsUniqueKeys() {
        return true;
    }

    @Override
    public boolean supportsCheckConstraints() {
        return true;
    }

//    @Override
//    public GenericUniqueKey createConstraintImpl(GenericTableBase table, String constraintName, DBSEntityConstraintType constraintType, JDBCResultSet dbResult, boolean persisted) {
//        return new DamengTableConstraint(table, constraintName, constraintType, dbResult, persisted);
//    }

    @Override
    public GenericTableConstraintColumn[] createConstraintColumnsImpl(JDBCSession session, GenericTableBase parent, GenericUniqueKey object, GenericMetaObject pkObject, JDBCResultSet dbResult) throws DBException {
        String columnListStr = JDBCUtils.safeGetString(dbResult, "COLUMN_LIST");
        List<String> columnNameList = CommonUtils.splitString(columnListStr, ',');
        List<GenericTableConstraintColumn> columns = new ArrayList<>(columnNameList.size());
        for (String columnName : columnNameList) {
            GenericTableColumn column = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (column == null) {
                throw new DBException("Column '" + columnName + "' not found in table " + parent.getName());
            }
            columns.add(new GenericTableConstraintColumn(object, column, 0));
        }
        return ArrayUtils.toArray(GenericTableConstraintColumn.class, columns);
    }


    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "AUTO_INCREMENT";
    }

    @Override
    public boolean isTableCommentEditable() {
        return true;
    }

    @Override
    public boolean isTableColumnCommentEditable() {
        return true;
    }

    @Override
    public boolean isColumnNotNullByDefault() {
        return true;
    }

}
