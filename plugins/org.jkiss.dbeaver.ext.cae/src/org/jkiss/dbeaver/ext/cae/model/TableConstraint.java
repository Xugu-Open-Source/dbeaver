/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.cae.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.ext.cae.Constants;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 约束信息类，包含约束相关的基本信息
 */
public class TableConstraint extends BaseTableConstraint {
	private static final Log log = Log.getLog(TableConstraint.class);

	private String searchCondition;
	private String consName;
	private char matchType;
	private boolean deferrable;
	private boolean initdeferred;
	private String define;
	private char updateAct;
	private char deleteAct;
	private boolean sys;

	private int dbId;
	private int tableId;
	private int refTableId;
	private char consType;
	private boolean enable;
	private boolean valid;

	public TableConstraint(BaseTable table, String name, DBSEntityConstraintType constraintType, String searchCondition,
			ObjectStatus status) {
		super(table, name, constraintType, status, false);
		this.searchCondition = searchCondition;
	}

	public TableConstraint(BaseTable table, ResultSet dbResult) {
		super(table, JDBCUtils.safeGetString(dbResult, "CONS_NAME"),
				getConstraintType(JDBCUtils.safeGetString(dbResult, "CONS_TYPE")),
				JDBCUtils.safeGetBoolean(dbResult, "ENABLE") && JDBCUtils.safeGetBoolean(dbResult, "VALID")
						? ObjectStatus.ENABLED
						: ObjectStatus.DISABLED,
				true);
		if (dbResult != null) {
			this.dbId = JDBCUtils.safeGetInt(dbResult, "DB_ID");
			this.tableId = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
			this.refTableId = JDBCUtils.safeGetInt(dbResult, "REF_TABLE_ID");
			this.consType = JDBCUtils.safeGetString(dbResult, "CONS_TYPE").charAt(0);
			this.enable = JDBCUtils.safeGetBoolean(dbResult, "ENABLE");
			this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
			this.consName = JDBCUtils.safeGetString(dbResult, "CONS_NAME");

			String s1 = JDBCUtils.safeGetString(dbResult, "MATCH_TYPE");
			if (s1 != null) {
				this.matchType = s1.charAt(0);
			}
			this.deferrable = JDBCUtils.safeGetBoolean(dbResult, "DEFERRABLE");
			this.initdeferred = JDBCUtils.safeGetBoolean(dbResult, "INITDEFERRED");
			this.define = JDBCUtils.safeGetString(dbResult, "DEFINE");
			this.searchCondition = this.define;
			String s2 = JDBCUtils.safeGetString(dbResult, "UPDATE_ACTION");
			if (s2 != null) {
				this.updateAct = s2.charAt(0);
			}
			String s3 = JDBCUtils.safeGetString(dbResult, "DELETE_ACTION");
			if (s3 != null) {
				this.deleteAct = s3.charAt(0);
			}
			this.sys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
		}
	}

	public TableConstraint(DBRProgressMonitor monitor, Table table, TableConstraint source) throws DBException {
		super(table, source);
		this.searchCondition = source.getSearchCondition();
		this.define = source.getDefine();
		this.enable = source.isEnable();
        if (source instanceof DBSEntityReferrer) {
            List<? extends DBSEntityAttributeRef> columns = ((DBSEntityReferrer) source).getAttributeReferences(monitor);
            if (columns != null) {
                for (DBSEntityAttributeRef col : columns) {
                    if (col.getAttribute() != null) {
                        TableColumn ownCol = table.getAttribute(monitor, col.getAttribute().getName());
                        this.addColumn(new TableConstraintColumn(this, ownCol, col.getAttribute().getOrdinalPosition()));
                    }
                }
            }
        }
	}

	@Property(viewable = true, editable = false, updatable = false, order = 3)
	public String getSearchCondition() {
		return this.searchCondition;
	}

	public String getConstraintName() {
		return this.consName;
	}

	public char getMatchType() {
		return this.matchType;
	}

	public boolean getDeferrable() {
		return this.deferrable;
	}

	public boolean getInitDeferrable() {
		return this.initdeferred;
	}

	public String getDefine() {
		return this.define;
	}

	public char getUpdateAction() {
		return this.updateAct;
	}

	public char getDeleteAction() {
		return this.deleteAct;
	}

	public boolean isSys() {
		return this.sys;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 4)
	public boolean isEnable() {
		return this.enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
	}

	public static DBSEntityConstraintType getConstraintType(String code) {
		switch (code) {
		case "C":
			return DBSEntityConstraintType.CHECK;
		case "P":
			return DBSEntityConstraintType.PRIMARY_KEY;
		case "U":
			return DBSEntityConstraintType.UNIQUE_KEY;
		case "F":
			return DBSEntityConstraintType.FOREIGN_KEY;
		case "N":
			return DBSEntityConstraintType.NOT_NULL;
		case "D":
			return Constants.CONSTRAINT_DEFAULT;
		case "R":
			return Constants.CONSTRAINT_REF_COLUMN;
		default:
			log.debug("Unsupported constraint type: " + code);
			return DBSEntityConstraintType.CHECK;
		}
	}
}
