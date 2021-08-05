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
package org.jkiss.dbeaver.ext.xugu.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Sequence;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 序列管理器，进行序列的创建，修改和删除
 */
public class SequenceManager extends SQLObjectEditor<Sequence, Schema> {
	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		return FEATURE_EDITOR_ON_CREATE;
	}

	protected void validateObjectProperties(ObjectChangeCommand command) throws DBException {
		if (CommonUtils.isEmpty(command.getObject().getName())) {
			throw new DBException("Sequence name cannot be empty");
		}
	}

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, Sequence> getObjectsCache(Sequence object) {
		return object.getSchema().sequenceCache;
	}

	@Override
	protected Sequence createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object copyFrom, Map<String, Object> options) {
		Schema schema = (Schema) container;
		return new UITask<Sequence>() {
			@Override
			protected Sequence runTask() {
				EntityEditPage page = new EntityEditPage(schema.getDataSource(), DBSEntityType.SEQUENCE);
				if (!page.edit()) {
					return null;
				}

				Sequence sequence = new Sequence(schema, page.getEntityName());
				sequence.setSeqName(page.getEntityName());
				return sequence;
			}
		}.execute();
	}

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Sequence, Schema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		Sequence sq = command.getObject();
		BigDecimal curValue = sq.getCurValue();
		BigDecimal minValue = sq.getMinValue();
		BigDecimal maxValue = sq.getMaxValue();
		BigDecimal increamentBy = sq.getIncrementBy();

		if (curValue.scale() > 0) {
			throw new DBException("序列当前值只能为整数");
		}
		if (minValue.scale() > 0) {
			throw new DBException("序列最小值值只能为整数");
		}
		if (maxValue.scale() > 0) {
			throw new DBException("序列最大值只能为整数");
		}
		if (increamentBy.scale() > 0) {
			throw new DBException("序列步长只能为整数");
		}
		if (minValue.longValue() < Long.MIN_VALUE || minValue.longValue() > Long.MAX_VALUE) {
			throw new DBException("序列最小值只能介于 -2(63次方) 至 2(63次方)-1 之间");
		}
		if (maxValue.longValue() < Long.MIN_VALUE || maxValue.longValue() > Long.MAX_VALUE) {
			throw new DBException("序列最大值只能介于 -2(63次方) 至 2(63次方)-1 之间");
		}
		if (minValue.longValue() > maxValue.longValue()) {
			throw new DBException("序列最小值不能大于最大值");
		}
		if (increamentBy.longValue() > (maxValue.longValue() - minValue.longValue())) {
			throw new DBException("序列步长不能大于最大值与最小值的差值");
		}

		String sql = buildStatement(sq, false);

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create sequence sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Create Sequence", sql));
	}

	@Override
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<Sequence, Schema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperties().size() > 1 || command.getProperty(commentKey) == null) {
			String sql = buildStatement(command.getObject(), true);

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct alter sequence sql: " + sql.toString());
			actionList.add(new SQLDatabasePersistAction("Alter Sequence", sql));
		}
	}

	@Override
	protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions,
			NestedObjectCommand<Sequence, SQLObjectEditor<Sequence, Schema>.PropertyHandler> command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperty(commentKey) != null) {
			StringBuilder desc = new StringBuilder(100);
			desc.append("COMMENT ON SEQUENCE ");
			desc.append(command.getObject().getName());
			desc.append(" IS ");
			desc.append(SQLUtils.quoteString(command.getObject(), command.getObject().getComment()));

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add sequence comment sql: " + desc.toString());
			actions.add(new SQLDatabasePersistAction("Comment Sequence", desc.toString()));
		}
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<Sequence, Schema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		String sql = "DROP SEQUENCE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop sequence sql: " + sql);
		DBEPersistAction action = new SQLDatabasePersistAction("Drop Sequence", sql);
		actions.add(action);
	}

	private String buildStatement(Sequence sequence, Boolean forUpdate) {
		StringBuilder sb = new StringBuilder();
		if (forUpdate) {
			sb.append("ALTER SEQUENCE ");
		} else {
			sb.append("CREATE SEQUENCE ");
		}
		sb.append(sequence.getFullyQualifiedName(DBPEvaluationContext.DDL)).append(" ");

		if (sequence.getMinValue() != null) {
			sb.append("MINVALUE ").append(sequence.getMinValue()).append(" ");
		}
		if (sequence.getMaxValue() != null) {
			sb.append("MAXVALUE ").append(sequence.getMaxValue()).append(" ");
		}

		if (sequence.getCurValue() != null) {
			sb.append("START WITH ").append(sequence.getCurValue()).append(" ");
		}
		if (sequence.getIncrementBy() != null) {
			sb.append("INCREMENT BY ").append(sequence.getIncrementBy()).append(" ");
		}
		if (sequence.isCycle()) {
			sb.append("CYCLE ");
		} else {
			sb.append("NOCYCLE ");
		}
		return sb.toString();
	}
}
