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
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObjectStatisticsCollector;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author jowee
 */
public class XuguSchema  extends GenericSchema implements DBPQualifiedObject, DBPObjectStatisticsCollector{

    @NotNull
    private String schemaName;

    private boolean persisted;

    private boolean hasStatistics;


    public XuguSchema(GenericDataSource dataSource, String schemaName, boolean persisted) {
        super(dataSource, null, schemaName);
        this.schemaName = schemaName;
        this.persisted = persisted;
    }


    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return schemaName;
    }

    public void setName(@NotNull String schemaName) {
        this.schemaName = schemaName;
    }


    // todo  表空间大小
    @Override
    public boolean isStatisticsCollected() {
        return false;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {

    }


    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), this);
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

}
