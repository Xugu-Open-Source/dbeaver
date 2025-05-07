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
import org.jkiss.dbeaver.DBException;

import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.xugu.conf.OemConfig;
import org.jkiss.dbeaver.ext.xugu.dialect.XuguSqlDialect;
import org.jkiss.dbeaver.ext.xugu.internal.XuguConstants;
import org.jkiss.dbeaver.ext.xugu.internal.XuguUtils;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionResult;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.exec.output.DBCOutputWriter;
import org.jkiss.dbeaver.model.impl.AsyncServerOutputReader;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCStatementImpl;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

/**
 * @author jowee
 */
public class XuguDataSource extends GenericDataSource {

    private final UserCache userCache = new UserCache();

    private final RoleCache roleCache = new RoleCache();

    public XuguDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel) throws DBException {
        super(monitor, container, metaModel, new XuguSqlDialect());
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new XuguExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(DBRProgressMonitor monitor, JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        XuguExecutionContext executionContext = (XuguExecutionContext) context;
        if (initFrom == null) {
            executionContext.refreshDefaults(monitor, true);
            return;
        }
        XuguExecutionContext executionMetaContext = (XuguExecutionContext) initFrom;
        XuguSchema defaultSchema = executionMetaContext.getDefaultSchema();
        if (defaultSchema != null) {
            executionContext.setDefaultSchema(monitor, defaultSchema);
        }
    }



    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, JDBCDatabaseMetaData metaData) {
        return new JDBCDataSourceInfo(metaData);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        return this;
    }

    @Override
    public DBPDataKind resolveDataKind(String typeName, int valueType) {
        return getDataKind(typeName, valueType);
    }

    @NotNull
    public static DBPDataKind getDataKind(@NotNull String typeName, int valueType) {
        if (typeName.equals(XuguConstants.TYPE_NAME_XML) || typeName.equals(XuguConstants.TYPE_NAME_XMLTYPE)) {
            return DBPDataKind.CONTENT;
        }
        return GenericDataSource.getDataKind(typeName, valueType);
    }


    @Nullable
    public XuguUser getUserById(DBRProgressMonitor monitor, long userId) throws DBException {
        return XuguUtils.getObjectById(monitor, userCache, this, userId);
    }



    @Association
    public Collection<XuguUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        return userCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<XuguRole> getRoles(DBRProgressMonitor monitor) throws DBException {
        return roleCache.getAllObjects(monitor, this);
    }


    static class RoleCache extends JDBCObjectCache<XuguDataSource, XuguRole> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataSource XuguDataSource) throws SQLException {
            StringBuilder sql = new StringBuilder();
            String databaseName = session.getDataSource().getContainer().getConnectionConfiguration().getDatabaseName();
            try {
                sql.append("SELECT * FROM ");
                sql.append("ALL");
                sql.append("_USERS WHERE IS_ROLE=true");
                sql.append(" AND DB_ID=");
                sql.append("(SELECT db_id FROM all_databases WHERE db_name = '"+databaseName+"')");
            } catch (Exception e) {
                throw new SQLException("Error in DataSource.RoleCache.prepareObjectsStatement()", e);
            }
            return session.prepareStatement(sql.toString());
        }

        @Override
        protected XuguRole fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource XuguDataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new XuguRole(XuguDataSource, resultSet);
        }
    }

    static class UserCache extends JDBCObjectCache<XuguDataSource, XuguUser> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataSource dataSource) throws SQLException {
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            String databaseName = session.getDataSource().getContainer().getConnectionConfiguration().getDatabaseName();
            try {
                sql.append("ALL");
                sql.append("_USERS");
                sql.append(" WHERE IS_ROLE=FALSE AND DB_ID=");
                sql.append("(SELECT db_id FROM all_databases WHERE db_name = '"+databaseName+"')");
            } catch (Exception e) {
                throw new SQLException("Get database object error: ", e);
            }
            return session.prepareStatement(sql.toString());
        }

        @Override
        protected XuguUser fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new XuguUser(dataSource, resultSet);
        }
    }

//    static class SchemaCache extends JDBCObjectCache<XuguDataSource, XuguSchema> {
//        @NotNull
//        @Override
//        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull XuguDataSource dataSource) throws SQLException {
//            StringBuilder schemasQuery = new StringBuilder();
//
//
//            // 根据owner的用户角色选取不同的语句来查询schema
//            schemasQuery.append("SELECT S.DB_ID,S.SCHEMA_ID,S.SCHEMA_NAME,U.USER_NAME,S.COMMENTS FROM ");
//            schemasQuery.append("ALL");
//            schemasQuery.append("_SCHEMAS S");
//            schemasQuery.append(",");
//            schemasQuery.append("ALL");
//            schemasQuery.append("_USERS U");
//            schemasQuery.append(" WHERE S.USER_ID=U.USER_ID AND S.DB_ID=");
//            schemasQuery.append("(SELECT db_id FROM all_databases WHERE db_name = '"+dataSource.getContainer().getConnectionConfiguration().getDatabaseName()+"')");
////            if (schema != null) {
////                schemasQuery.append(" AND S.SCHEMA_NAME =");
////                schemasQuery.append(SQLUtils.quoteString(schema, schema.getName()));
////            } else if (name != null) {
////                schemasQuery.append(" AND S.SCHEMA_NAME =");
////                schemasQuery.append(SQLUtils.quoteString(owner, name));
////            }
//            schemasQuery.append(" ORDER BY S.SCHEMA_ID ASC");
////            log.debug("schema message ：" + schemasQuery.toString());
//
//            JDBCPreparedStatement dbStat = session.prepareStatement(schemasQuery.toString());
//
//            return dbStat;
//        }
//
//        @Override
//        protected XuguSchema fetchObject(@NotNull JDBCSession session, @NotNull XuguDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
//            return null;
//        }
//    }




}
