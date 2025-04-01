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
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableForeignKey;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKeyColumn;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 外键信息类，包含外键相关的基本信息
 */
public class TableForeignKey extends BaseTableConstraint implements DBSTableForeignKey {
	private static final Log log = Log.getLog(TableForeignKey.class);

	private TableConstraint referencedKey;
	private DBSForeignKeyModifyRule deleteRule;
	private DBSForeignKeyModifyRule updateRule;
	private boolean enable;

	public TableForeignKey(@NotNull BaseTable table, @Nullable String name, @Nullable ObjectStatus status,
			@NotNull TableConstraint referencedKey, @NotNull DBSForeignKeyModifyRule deleteRule,
			@NotNull DBSForeignKeyModifyRule updateRule) {
		super(table, name, DBSEntityConstraintType.FOREIGN_KEY, status, false);
		this.referencedKey = referencedKey;
		this.deleteRule = deleteRule;
		this.updateRule = updateRule;
	}

	public TableForeignKey(DBRProgressMonitor monitor, Table table, ResultSet dbResult) throws DBException {
		super(table, JDBCUtils.safeGetString(dbResult, "CONS_NAME"), DBSEntityConstraintType.FOREIGN_KEY,
				JDBCUtils.safeGetBoolean(dbResult, "ENABLE") && JDBCUtils.safeGetBoolean(dbResult, "VALID")
						? ObjectStatus.ENABLED
						: ObjectStatus.DISABLED,
				true);
		String refName;
		String refOwnerName;
		String refTableName;
		try {
			refName = dbResult.getString("REF_NAME");
			refOwnerName = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
			refTableName = dbResult.getString("REF_TABLE_NAME");
		} catch (SQLException e) {
			throw new DBException("访问结果集失败", e);
		}

		this.enable = JDBCUtils.safeGetBoolean(dbResult, "ENABLE");

		log.debug(OemConfig.OEM_NAME_EN + " can get alias filed? " + refTableName + " " + refName);
		BaseTable refTable = BaseTable.findTable(monitor, table.getDataSource(), refOwnerName, refTableName);
		if (refTable == null) {
			log.warn("Referenced table '" + DBUtils.getSimpleQualifiedName(refOwnerName, refTableName) + "' not found");
		} else {
			referencedKey = refTable.getConstraint(monitor, refName);
			if (referencedKey == null) {
				log.warn("Referenced constraint '" + refName + "' not found in table '"
						+ refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
				referencedKey = new TableConstraint(refTable, "refName", DBSEntityConstraintType.UNIQUE_KEY, null,
						ObjectStatus.ERROR);
			}
		}

		String updateAction = JDBCUtils.safeGetString(dbResult, "UPDATE_ACTION");
		switch (updateAction) {
		case "n":
			this.updateRule = DBSForeignKeyModifyRule.NO_ACTION;
			break;
		case "u":
			this.updateRule = DBSForeignKeyModifyRule.SET_NULL;
			break;
		case "d":
			this.updateRule = DBSForeignKeyModifyRule.SET_DEFAULT;
			break;
		case "c":
			this.updateRule = DBSForeignKeyModifyRule.CASCADE;
			break;
		default:
			this.updateRule = DBSForeignKeyModifyRule.NO_ACTION;
			break;
		}

		String deleteAction = JDBCUtils.safeGetString(dbResult, "DELETE_ACTION");
		switch (deleteAction) {
		case "n":
			this.deleteRule = DBSForeignKeyModifyRule.NO_ACTION;
			break;
		case "u":
			this.deleteRule = DBSForeignKeyModifyRule.SET_NULL;
			break;
		case "d":
			this.deleteRule = DBSForeignKeyModifyRule.SET_DEFAULT;
			break;
		case "c":
			this.deleteRule = DBSForeignKeyModifyRule.CASCADE;
			break;
		default:
			this.deleteRule = DBSForeignKeyModifyRule.NO_ACTION;
			break;
		}
	}
	
	public TableForeignKey(DBRProgressMonitor monitor, Table table, TableForeignKey source) throws DBException {
		super(table, source);
		this.enable = source.enable;
		this.updateRule = source.getUpdateRule();
		this.deleteRule = source.getDeleteRule();
		TableConstraint srcRefConstraint = source.getReferencedConstraint();
        if (srcRefConstraint != null) {
            DBSEntity refEntity = srcRefConstraint.getParentObject();
            if (refEntity != null) {
                if (srcRefConstraint instanceof JDBCTableConstraint && refEntity.getParentObject() == table.getParentObject()) {
                    this.referencedKey = srcRefConstraint;
                } else {
                    // Try to find table with the same name as referenced constraint owner
                    DBSObject tableContainer = table.getContainer();
                    if (tableContainer instanceof DBSObjectContainer) {
                        DBSObject refTable = ((DBSObjectContainer)tableContainer).getChild(monitor, refEntity.getName());
                        if (refTable instanceof DBSEntity && referencedKey instanceof DBSEntityReferrer) {
                            List<DBSEntityAttribute> refAttrs = DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) referencedKey);
                            this.referencedKey = (TableConstraint) DBUtils.findEntityConstraint(monitor, (DBSEntity) refTable, refAttrs);
                        }
                    }
                }
            }
        }
        if (source instanceof DBSEntityReferrer) {
            List<? extends DBSEntityAttributeRef> columns = ((DBSEntityReferrer) source).getAttributeReferences(monitor);
            if (columns != null) {
                this.setColumns(new ArrayList<>());
                for (DBSEntityAttributeRef srcCol : columns) {
                    if (srcCol instanceof DBSTableForeignKeyColumn) {
                        DBSTableForeignKeyColumn fkCol = (DBSTableForeignKeyColumn) srcCol;
                        this.addColumn(new TableForeignKeyColumn(this,
                            table.getAttribute(monitor, fkCol.getName()),
                            this.getColumns().size(),
                            table.getAttribute(monitor, fkCol.getReferencedColumn().getName())));
                    }
                }
            }
        }
	}

	@Property(viewable = true, order = 3)
	public BaseTable getReferencedTable() {
		return referencedKey == null ? null : referencedKey.getTable();
	}

	@Nullable
	@Override
	@Property(id = "reference", viewable = true, order = 4)
	public TableConstraint getReferencedConstraint() {
		return referencedKey;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = false, listProvider = ConstraintModifyRuleListProvider.class, order = 5)
	public DBSForeignKeyModifyRule getDeleteRule() {
		return deleteRule;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = false, listProvider = ConstraintModifyRuleListProvider.class, order = 6)
	public DBSForeignKeyModifyRule getUpdateRule() {
		return updateRule;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 4)
	public boolean isEnable() {
		return this.enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	@Override
	public BaseTable getAssociatedEntity() {
		return getReferencedTable();
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
	}

	public static class ConstraintModifyRuleListProvider implements IPropertyValueListProvider<JDBCTableForeignKey> {
		@Override
		public boolean allowCustomValue() {
			return false;
		}

		@Override
		public Object[] getPossibleValues(JDBCTableForeignKey foreignKey) {
			return new DBSForeignKeyModifyRule[] { DBSForeignKeyModifyRule.NO_ACTION, DBSForeignKeyModifyRule.CASCADE,
					DBSForeignKeyModifyRule.SET_NULL, DBSForeignKeyModifyRule.SET_DEFAULT };
		}
	}
}
