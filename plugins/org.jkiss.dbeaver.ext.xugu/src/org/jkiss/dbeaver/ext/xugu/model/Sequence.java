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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.UserRoleFlag;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSequence;

import com.xugu.parser.DatabaseParsing;
import com.xugu.parser.Parsing;
import com.xugu.parser.Parsing.TableType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

/**
 * 序列信息类，包含序列相关的基本信息
 */
public class Sequence extends BaseSchemaObject implements DBSSequence, DBPScriptObject, DBPRefreshableObject {
	private int seqId;
	private String seqName;
	private BigDecimal curVal;
	private BigDecimal minValue;
	private BigDecimal maxValue;
	private BigDecimal incrementBy;
	private int cacheVal;

	private boolean flagCycle;
	private boolean flagOrder;
	private boolean isSys;
	private boolean valid;
	private Timestamp createTime;
	private String comment;

	public Sequence(Schema schema, String name) {
		super(schema, name, false);
		this.curVal = new BigDecimal(1);
		this.minValue = new BigDecimal(1);
		this.maxValue = new BigDecimal(9223372036854775807L);
		this.incrementBy = new BigDecimal(1);
		this.flagCycle = false;
		this.flagOrder = false;
	}

	public Sequence(Schema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "SEQ_NAME"), true);

		this.seqId = JDBCUtils.safeGetInt(dbResult, "SEQ_ID");
		this.seqName = JDBCUtils.safeGetString(dbResult, "SEQ_NAME");

		this.curVal = JDBCUtils.safeGetBigDecimal(dbResult, "CURR_VAL");
		this.minValue = JDBCUtils.safeGetBigDecimal(dbResult, "MIN_VAL");
		this.maxValue = JDBCUtils.safeGetBigDecimal(dbResult, "MAX_VAL");
		this.incrementBy = JDBCUtils.safeGetBigDecimal(dbResult, "STEP_VAL");
		this.cacheVal = JDBCUtils.safeGetInt(dbResult, "CACHE_VAL");

		this.flagCycle = JDBCUtils.safeGetBoolean(dbResult, "IS_CYCLE");
		this.flagOrder = JDBCUtils.safeGetBoolean(dbResult, "IS_ORDER");
		this.isSys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
		this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");

		this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
		this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	public String getName() {
		return seqName;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 2)
	public BigDecimal getCurValue() {
		return curVal;
	}

	@Override
	@Property(viewable = true, editable = true, updatable = true, order = 3)
	public BigDecimal getMinValue() {
		return minValue;
	}

	@Override
	@Property(viewable = true, editable = true, updatable = true, order = 4)
	public BigDecimal getMaxValue() {
		return maxValue;
	}

	@Override
	@Property(viewable = true, editable = true, updatable = true, order = 5)
	public BigDecimal getIncrementBy() {
		return incrementBy;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 6)
	public String getComment() {
		return comment;
	}

	@Property(viewable = true, editable = false, order = 7)
	public int getSeqId() {
		return seqId;
	}

	@Property(viewable = true, editable = false, order = 8)
	public Timestamp getCreateTime() {
		return createTime;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 9)
	public boolean isCycle() {
		return flagCycle;
	}

	@Property(hidden = true, viewable = false, editable = false, order = 10)
	public boolean isValid() {
		return valid;
	}

	@Property(hidden = true, editable = false, order = 11)
	public int getCacheValue() {
		return cacheVal;
	}

	/**
	 * TODO 由于测试时，有序设置在服务端没有效果，暂时隐藏有序属性的展示
	 * 
	 * @return 是否有序
	 */
	@Property(hidden = true, editable = false, order = 12)
	public boolean isOrder() {
		return flagOrder;
	}

	public void setCurValue(BigDecimal curVal) {
		this.curVal = curVal;
	}

	public void setMinValue(BigDecimal minValue) {
		this.minValue = minValue;
	}

	public void setMaxValue(BigDecimal maxValue) {
		this.maxValue = maxValue;
	}

	public void setIncrementBy(BigDecimal incrementBy) {
		this.incrementBy = incrementBy;
	}

	public void setCacheValue(int cacheVal) {
		this.cacheVal = cacheVal;
	}

	public void setCycle(boolean flagCycle) {
		this.flagCycle = flagCycle;
	}

	public void setOrder(boolean flagOrder) {
		this.flagOrder = flagOrder;
	}

	public void setSeqId(int seqId) {
		this.seqId = seqId;
	}

	public String getSeqName() {
		return seqName;
	}

	public void setSeqName(String seqName) {
		this.seqName = seqName;
	}

	public boolean isSys() {
		return isSys;
	}

	public void setSys(boolean isSys) {
		this.isSys = isSys;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public Number getLastValue() {
		return getCurValue();
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		String objectFullName = DBUtils.getObjectFullName(this, DBPEvaluationContext.DDL);
		monitor.beginTask("Load sources for schduler job '" + objectFullName + "'...", 1);
		try (Connection conn = DBUtils.openUtilSession(monitor, this, "Get " + this.name + "DDL")) {
			String roleFlag = getDataSource().getRoleFlag();
			TableType tableType;

			if (UserRoleFlag.SYS.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.SYS;
			} else if (UserRoleFlag.DBA.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.DBA;
			} else {
				tableType = TableType.ALL;
			}

			Parsing parsing = new Parsing();
			return parsing.getObjectDDL(conn, getSchema().getName(), getName(), "SEQUENCE", tableType);
		} catch (SQLException e) {
			throw new DBException("Close connection of DDL failed", e);
		}
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		Schema schema = this.getSchema();
		schema.sequenceCache.clearCache();
		return schema.sequenceCache.refreshObject(monitor, schema, this);
	}
}
