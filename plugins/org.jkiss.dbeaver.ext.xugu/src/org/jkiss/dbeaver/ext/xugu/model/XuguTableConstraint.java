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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericUniqueKey;
import org.jkiss.dbeaver.model.DBPObjectWithLongId;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableCheckConstraint;

import java.sql.Timestamp;
import java.util.Map;

/**
 * Dameng Constraint
 *
 * @author Baishengkai
 * @since 2024/10/13
 */
public class XuguTableConstraint extends GenericUniqueKey implements DBPObjectWithLongId, DBSTableCheckConstraint, DBPScriptObject {



    private Status status;


    public XuguTableConstraint(GenericTableBase table, String name, DBSEntityConstraintType constraintType, JDBCResultSet dbResult,
                               boolean persisted) {
        super(table, name, null, constraintType, persisted);
        this.status = "Y".equals(JDBCUtils.safeGetString(dbResult, "VALID")) ? Status.VALID : Status.INVALID;
    }


    @Override
    public long getObjectId() {
        return 0;
    }



    @Property(viewable = true, order = 6)
    public Status getStatus() {
        return status;
    }


    @Override
    public String getCheckConstraintDefinition() {
        return null;
    }

    @Override
    public void setCheckConstraintDefinition(String expression) {
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return null;
    }



    public enum Status {
        VALID,
        INVALID,
    }
}
