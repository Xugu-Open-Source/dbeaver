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
package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;

/**
 * 约束管理器，进行约束的增加删除和修改
 */
public class ConstraintManager extends SQLConstraintManager<TableConstraint, BaseTable> {
	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, TableConstraint> getObjectsCache(TableConstraint object) {
		return object.getParentObject().getSchema().constraintCache;
	}

	@Override
	protected TableConstraint createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {

		BaseTable parent = (BaseTable) container;


		return new UITask<TableConstraint>() {
			@Override
			protected TableConstraint runTask() {
				EditConstraintPage editPage = new EditConstraintPage(Messages.edit_constraint_manager_dialog_title,
						null);
				if (!editPage.edit()) {
					return null;
				}

				final TableConstraint constraint = new TableConstraint(parent, editPage.getConstraintName(),
						editPage.getConstraintType(), editPage.getConstraintExpression(),
						editPage.isEnableConstraint() ? ObjectStatus.ENABLED : ObjectStatus.DISABLED);
				constraint.setEnable(constraint.getStatus() == ObjectStatus.ENABLED);
				int colIndex = 1;
				for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
					constraint.addColumn(new TableConstraintColumn(constraint, (TableColumn) tableColumn, colIndex++));
				}
				return constraint;
			}
		}.execute();
	}

	@NotNull
	@Override
	protected String getAddConstraintTypeClause(TableConstraint constraint) {
		if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
			return "UNIQUE";
		}
		return super.getAddConstraintTypeClause(constraint);
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
		TableConstraint constraint = (TableConstraint) command.getObject();
		// 当为单列主键时，已在列中添加 PRIMARY KEY 以生成主键，此处更改表添加主键需跳过
		if (constraint.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
			List<TableConstraintColumn> columns = constraint.getAttributeReferences(monitor);
			if (columns.size() == 1) {
				return;
			}
		}
		// 当此唯一列约束关联列为自增列时，已在列中添加 IDENTITY 以生成唯一约束，此处更改表添加唯一约束需跳过
		if (constraint.getConstraintType() == DBSEntityConstraintType.UNIQUE_KEY) {
			List<TableConstraintColumn> columns = constraint.getAttributeReferences(monitor);
			if (columns.size() == 1) {
				TableColumn column = columns.get(0).getAttribute();
				if (column.getMinInteger() != null && column.getStepInteger() != null) {
					return;
				}
			}
		}
		BaseTable table = constraint.getTable();
		StringBuilder decl = new StringBuilder(100);
		decl.append("ALTER TABLE ");
		decl.append(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
		decl.append(" ADD ");
		decl.append(getNestedDeclaration(monitor, table, command, options));
		decl.append(";");
		decl.append(CommonUtils.getLineSeparator());

		decl.append("ALTER TABLE ");
		decl.append(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
		decl.append(constraint.isEnable() ? " ENABLE" : " DISABLE");
		decl.append(" CONSTRAINT ");
		decl.append(constraint.getName());

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create constraint sql: " + decl.toString());
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_constraint, decl.toString()));
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<TableConstraint, BaseTable>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		TableConstraint constraint = (TableConstraint) command.getObject();
		BaseTable table = constraint.getTable();
		String sql = "ALTER TABLE " + table.getFullyQualifiedName(DBPEvaluationContext.DDL)
				+ (constraint.isEnable() ? " ENABLE" : " DISABLE") + " CONSTRAINT " + constraint.getName();

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter constraint sql: " + sql);
		actionList.add(new SQLDatabasePersistAction("Alter constraint", sql));
	}

	/**
	 * 重写组装 check 条件的语句
	 */
	@Override
	protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<TableConstraint> command) {
		String[] type = decl.toString().split(" ");
		decl.append(" (");
		final String typeCheck = "CHECK";
		final int typeIndex = 2;
		if (typeCheck.equals(type[typeIndex])) {
			decl.append(command.getObject().getSearchCondition());
		} else {
			List<? extends DBSEntityAttributeRef> attrs = command.getObject()
					.getAttributeReferences(new VoidProgressMonitor());
			if (attrs != null) {
				boolean firstColumn = true;
				for (DBSEntityAttributeRef constraintColumn : attrs) {
					final DBSEntityAttribute attribute = constraintColumn.getAttribute();
					if (attribute == null) {
						continue;
					}
					if (!firstColumn) {
						decl.append(",");
					}
					firstColumn = false;
					decl.append(DBUtils.getQuotedIdentifier(attribute));
				}
			}
		}
		decl.append(")");
	}
}
