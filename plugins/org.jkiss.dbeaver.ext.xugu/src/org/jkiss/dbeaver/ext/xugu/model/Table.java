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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 表信息衍生类，包含表相关的基本信息
 */
public class Table extends BaseTablePhysical implements DBPScriptObject {
	private static final Log log = Log.getLog(Table.class);

	/**
	 * 修改了用户信息的字段
	 */
	private int dbId;
	private int userId;
	private int schemaId;
	private int tableId;
	private String tableName;
	private int tempType;
	private int fieldNum;

	private int partiType;
	private int partiNum;
	private String partiKey;
	private int autoPartiType;
	private int autoPartiSpan;

	private int subpartiType;
	private int subpartiNum;
	private String subpartiKey;

	private int gstoNo;
	private int copyNum;
	private int blockSize;
	private int chunkSize;
	private long recordNum;
	private int pctfree;
	private String fileType;
	private String filePath;
	private String rowDelimiter;
	private String colDelimiter;
	private String badFile;
	private String missingVal;
	private boolean useCache;
	private String online;
	private boolean sys;
	private boolean encr;
	private boolean havePolicy;
	private boolean onCommitDel;
	private boolean enaTrans;
	private boolean enaLogging;
	private int aclMask;

	public Table(Schema schema, String name) {
		super(schema, name);
	}

	public Table(DBRProgressMonitor monitor, Schema schema, ResultSet dbResult) {
		super(schema, dbResult);
		// xfc 修改了模式名获取的方式
		if (dbResult != null) {
			// 结果集不为空时，persisted 设为 true
			this.setPersisted(true);

			this.tableName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
			this.dbId = JDBCUtils.safeGetInt(dbResult, "DB_ID");
			this.userId = JDBCUtils.safeGetInt(dbResult, "USER_ID");
			this.schemaId = JDBCUtils.safeGetInt(dbResult, "SCHEMA_ID");
			this.tableId = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
			this.tableName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
			this.tempType = JDBCUtils.safeGetInt(dbResult, "TEMP_TYPE");
			this.fieldNum = JDBCUtils.safeGetInt(dbResult, "FIELD_NUM");

			this.partiType = JDBCUtils.safeGetInt(dbResult, "PARTI_TYPE");
			this.partiNum = JDBCUtils.safeGetInt(dbResult, "PARTI_NUM");
			this.partiKey = JDBCUtils.safeGetString(dbResult, "PARTI_KEY");
			this.autoPartiType = JDBCUtils.safeGetInt(dbResult, "AUTO_PARTI_TYPE");
			this.autoPartiSpan = JDBCUtils.safeGetInt(dbResult, "AUTO_PARTI_SPAN");

			this.subpartiType = JDBCUtils.safeGetInt(dbResult, "SUBPARTI_TYPE");
			this.subpartiNum = JDBCUtils.safeGetInt(dbResult, "SUBPARTI_NUM");
			this.subpartiKey = JDBCUtils.safeGetString(dbResult, "SUBPARTI_KEY");

			this.gstoNo = JDBCUtils.safeGetInt(dbResult, "GSTO_NO");
			this.copyNum = JDBCUtils.safeGetInt(dbResult, "COPY_NUM");
			this.blockSize = JDBCUtils.safeGetInt(dbResult, "BLOCK_SIZE");
			this.chunkSize = JDBCUtils.safeGetInt(dbResult, "CHUNK_SIZE");
			this.recordNum = JDBCUtils.safeGetLong(dbResult, "RECORD_NUM");
			this.pctfree = JDBCUtils.safeGetInt(dbResult, "PCTFREE");
			this.useCache = JDBCUtils.safeGetBoolean(dbResult, "USE_CACHE");
			this.online = JDBCUtils.safeGetString(dbResult, "ONLINE");
			this.sys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
			this.onCommitDel = JDBCUtils.safeGetBoolean(dbResult, "ON_COMMIT_DEL");
			this.enaTrans = JDBCUtils.safeGetBoolean(dbResult, "ENA_TRANS");
			this.enaLogging = JDBCUtils.safeGetBoolean(dbResult, "ENA_LOGGING");
			this.aclMask = JDBCUtils.safeGetInt(dbResult, "ACL_MASK");
		}
	}

	// 复制构造函数
	public Table(DBRProgressMonitor monitor, Schema schema, DBSEntity source) throws DBException {
		super(monitor, schema, source);
		if (source instanceof Table) {
			this.partiNum = ((Table) source).partiNum;
			this.subpartiNum = ((Table) source).subpartiNum;

			// 复制触发器
			for (Trigger srcTrigger : ((Table) source).getTriggers(monitor)) {
				Trigger trigger = new Trigger(this, srcTrigger);
				getContainer().triggerCache.cacheObject(trigger);
			}

			// 复制一级分区
			for (TablePartition srcPartition : ((Table) source).getPartitions(monitor)) {
				TablePartition partition = new TablePartition(this, false, srcPartition);
				this.partitionCache.cacheObject(partition);
			}
			this.partitionCache.setFullCache(true);

			// 复制二级分区
			for (TableSubPartition srcSubPartition : ((Table) source).getSubPartitions(monitor)) {
				TableSubPartition subPartition = new TableSubPartition(this, true, srcSubPartition);
				this.subPartitionCache.cacheObject(subPartition);
			}
			this.subPartitionCache.setFullCache(true);
		}
        if (source instanceof DBSTable) {
            // 复制索引
            for (DBSTableIndex srcIndex : CommonUtils.safeCollection(((DBSTable)source).getIndexes(monitor))) {
                if (srcIndex instanceof TableIndex && srcIndex.isPrimary()) {
                    // Skip primary key index (it will be created implicitly)
                    continue;
                }
                TableIndex index = new TableIndex(monitor, this, srcIndex);
                this.getContainer().indexCache.cacheObject(index);
            }
        }
        
        // 复制约束
        for (DBSEntityConstraint srcConstr : CommonUtils.safeCollection(source.getConstraints(monitor))) {
            TableConstraint constr = new TableConstraint(monitor, this, (TableConstraint) srcConstr);
            this.getContainer().constraintCache.cacheObject(constr);
        }
        
        // 复制外键
        for (DBSEntityAssociation srcFK : CommonUtils.safeCollection(source.getAssociations(monitor))) {
            TableForeignKey fk = new TableForeignKey(monitor, this, (TableForeignKey) srcFK);
            this.getContainer().foreignKeyCache.cacheObject(fk);
        }
	}

	@Override
	protected String getTableTypeName() {
		return ObjectType.TABLE.getTypeName();
	}

	@Override
	public boolean isView() {
		return false;
	}

	public static Log getLog() {
		return log;
	}

	public int getDbId() {
		return dbId;
	}

	public int getUserId() {
		return userId;
	}

	public int getSchemaId() {
		return schemaId;
	}

	public int getTableId() {
		return tableId;
	}

	public String getTableName() {
		return super.getFullyQualifiedName(DBPEvaluationContext.DML);
	}

	public int getTempType() {
		return tempType;
	}

	public int getFieldNum() {
		return fieldNum;
	}

	public int getPartiType() {
		return partiType;
	}

	public int getPartiNum() {
		return partiNum;
	}

	public String getPartiKey() {
		return partiKey;
	}

	public int getAutoPartiType() {
		return autoPartiType;
	}

	public int getAutoPartiSpan() {
		return autoPartiSpan;
	}

	public int getSubpartiType() {
		return subpartiType;
	}

	public int getSubpartiNum() {
		return subpartiNum;
	}

	public String getSubpartiKey() {
		return subpartiKey;
	}

	public int getGstoNo() {
		return gstoNo;
	}

	public int getCopyNum() {
		return copyNum;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public int getChunkSize() {
		return chunkSize;
	}

	public long getRecordNum() {
		return recordNum;
	}

	public int getPctfree() {
		return pctfree;
	}

	public String getFileType() {
		return fileType;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getRowDelimiter() {
		return rowDelimiter;
	}

	public String getColDelimiter() {
		return colDelimiter;
	}

	public String getBadFile() {
		return badFile;
	}

	public String getMissingVal() {
		return missingVal;
	}

	public boolean isUseCache() {
		return useCache;
	}

	public String getOnline() {
		return online;
	}

	public boolean isSys() {
		return sys;
	}

	public boolean isEncr() {
		return encr;
	}

	public boolean isHavePolicy() {
		return havePolicy;
	}

	public boolean isOnCommitDel() {
		return onCommitDel;
	}

	public boolean isEnaTrans() {
		return enaTrans;
	}

	public boolean isEnaLogging() {
		return enaLogging;
	}

	public int getAclMask() {
		return aclMask;
	}

	@Override
	public TableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
			throws DBException {
		return super.getAttribute(monitor, attributeName);
	}

	@Override
	public Collection<TableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
		List<TableForeignKey> refs = new ArrayList<>();
		// This is dummy implementation
		// Get references from this schema only
		final Collection<TableForeignKey> allForeignKeys = getContainer().foreignKeyCache.getObjects(monitor,
				getContainer(), this);
		for (TableForeignKey constraint : allForeignKeys) {
			if (constraint.getReferencedTable() == this) {
				refs.add(constraint);
			}
		}
		return refs;
	}

	@Override
	@Association
	public Collection<TableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().foreignKeyCache.getObjects(monitor, getContainer(), this);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		getContainer().tableCache.clearCache();
		return super.refreshObject(monitor);
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		return getDdl(monitor, DDLFormat.getCurrentFormat(getDataSource()), options);
	}
}
