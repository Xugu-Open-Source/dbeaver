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
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.IntKeyMap;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;

/**
 * 基本存储过程
 */
public abstract class BaseProcedure<PARENT extends DBSObjectContainer> extends BaseObject<PARENT>
		implements DBSProcedure {
	static final Log log = Log.getLog(BaseProcedure.class);

	private DBSProcedureType procedureType;
	private final ArgumentsCache argumentsCache = new ArgumentsCache();


	public BaseProcedure(PARENT parent, String name, long objectId, DBSProcedureType procedureType) {
		super(parent, name, objectId, true);
		this.procedureType = procedureType;
	}

	public BaseProcedure(PARENT parent, ProcedureStandalone source) {
		super(parent, null, false);
		this.procedureType = source.getProcedureType();
	}

	@Override
	@Property(viewable = true, editable = true, order = 3)
	public DBSProcedureType getProcedureType() {
		return procedureType;
	}

    public void setProcedureType(DBSProcedureType procedureType) {
        this.procedureType = procedureType;
    }

	@Override
	public DBSObjectContainer getContainer() {
		return getParentObject();
	}

	/**
	 * 获取模式对象
	 * 
	 * @return 模式对象
	 */
	public abstract Schema getSchema();

	/**
	 * 获取过载个数
	 * 
	 * @return 过载个数
	 */
	public abstract Integer getOverloadNumber();

	@Override
	public Collection<ProcedureParameter> getParameters(DBRProgressMonitor monitor) throws DBException {
		return argumentsCache.getAllObjects(monitor, this);
	}

	static class ArgumentsCache extends JDBCObjectCache<BaseProcedure, ProcedureParameter> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull BaseProcedure procedure)
				throws SQLException {
			JDBCPreparedStatement dbStat = session.prepareStatement(
					"SELECT DEFINE FROM ALL_PROCEDURES " + "WHERE PROC_ID=" + procedure.getObjectId());
			return dbStat;
		}

		@Override
		protected ProcedureParameter fetchObject(@NotNull JDBCSession session, @NotNull BaseProcedure procedure,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new ProcedureParameter(session.getProgressMonitor(), procedure, resultSet);
		}

		@Override
		protected void invalidateObjects(DBRProgressMonitor monitor, BaseProcedure owner,
				Iterator<ProcedureParameter> objectIter) {
			IntKeyMap<ProcedureParameter> argStack = new IntKeyMap<>();
			while (objectIter.hasNext()) {
				ProcedureParameter argument = objectIter.next();
				final int curDataLevel = argument.getDataLevel();
				argStack.put(curDataLevel, argument);
				if (curDataLevel > 0) {
					objectIter.remove();
					ProcedureParameter parentArgument = argStack.get(curDataLevel - 1);
					if (parentArgument == null) {
						log.error("Broken arguments structure for '"
								+ argument.getParentObject().getFullyQualifiedName(DBPEvaluationContext.DDL)
								+ "' - no parent argument for argument " + argument.getSequence());
					} else {
						parentArgument.addAttribute(argument);
					}
				}
			}
		}
	}
}
