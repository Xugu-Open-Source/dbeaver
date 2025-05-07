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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.xugu.conf.OemConfig;
import org.jkiss.dbeaver.model.DBPObjectStatistics;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintInfo;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Shengkai Bai
 */
public class XuguTable extends GenericTable implements DBPObjectStatistics {

    private long tableSize = -1;

    public XuguTable(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        super(container, tableName, tableType, dbResult);
    }

    public XuguTable(GenericStructContainer container, String tableName, String tableCatalogName, String tableSchemaName) {
        super(container, tableName, tableCatalogName, tableSchemaName);
    }

    @Override
    public List<DBSEntityConstraintInfo> getSupportedConstraints() {
        return List.of(
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.PRIMARY_KEY, GenericTableConstraint.class),
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.UNIQUE_KEY, GenericTableConstraint.class),
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.CHECK, GenericTableConstraint.class)
        );
    }

    @Override
    public boolean hasStatistics() {
        return tableSize != -1;
    }

    @Override
    public long getStatObjectSize() {
        return tableSize;
    }

    void fetchStatistics(JDBCResultSet dbResult) throws SQLException {
        tableSize = dbResult.getLong("DISK_SIZE");
    }

    @Override
    public List<? extends GenericTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        JDBCSession session = DBUtils.openMetaSession(monitor, this.getSchema(), "Read table triggers");
        String databaseName = getDataSource().getContainer().getConnectionConfiguration().getDatabaseName();
        String orleFlag = "all";
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("select * from ");
        sqlBuilder.append(orleFlag);
        sqlBuilder.append("_triggers st join ");
        sqlBuilder.append(orleFlag);
        sqlBuilder.append("_objects so");
        sqlBuilder.append(" on st.obj_id = so.obj_id and st.db_id = so.db_id where st.db_id= ");
        sqlBuilder.append("SELECT db_id FROM ALL_DATABASES WHERE  db_name = "+"'"+databaseName+"'");
        sqlBuilder.append(" and st.schema_id=");
        sqlBuilder.append("SELECT schema_id FROM all_schemas WHERE schema_name  = "+"'"+getSchemaName()+"'");
        List<GenericTrigger> result = new ArrayList<>();
        try {
            JDBCResultSet jdbcResultSet = session.prepareStatement(sqlBuilder.toString()).executeQuery();
            while (jdbcResultSet.next()){
                String name = JDBCUtils.safeGetString(jdbcResultSet, 1);
                result.add(new GenericTableTrigger(this, name, null));
            }
            return result;
        } catch (SQLException e) {

            throw new RuntimeException(e);
        }
    }

    @Override
    public DBPPropertySource getStatProperties() {
        return null;
    }
}
