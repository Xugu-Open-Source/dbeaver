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

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

/**
 * 存储过程打包
 */
public class ProcedurePackaged extends BaseProcedure<Package> implements DBPUniqueObject {
 
	private Integer overload;
	
	private boolean valid;
	private String comment;
	private Timestamp createTime;
	private String sourceDeclaration;
	private List<ProcedureParameter> procParams;
	
	public  ProcedurePackaged(Package ownerPackage,ResultSet dResultSet) {
		super(ownerPackage, JDBCUtils.safeGetString(dResultSet, "PACK_NAME"), 0L,
				DBSProcedureType.valueOf(JDBCUtils.safeGetString(dResultSet, "RET_TYPE")==null?DBSProcedureType.PROCEDURE.toString():DBSProcedureType.FUNCTION.toString()));

		String head = JDBCUtils.safeGetString(dResultSet, "SPEC");
		String  body = JDBCUtils.safeGetString(dResultSet, "BODY");
		String sql = head+body;
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), getParentObject(), this);
	}

	@Override
	public Schema getSchema() {
		return getParentObject().getSchema();
	}

	@Override
	public Integer getOverloadNumber() {
		return overload;
	}

	public void setOverload(int overload) {
		this.overload = overload;
	}

	@NotNull
	@Override
	public String getUniqueName() {
		return overload == null || overload <= 1 ? getName() : getName() + "#" + overload;
	}
}
