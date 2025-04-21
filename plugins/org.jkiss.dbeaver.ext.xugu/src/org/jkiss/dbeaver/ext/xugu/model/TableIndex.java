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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.internal.Constants;

import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 索引信息类，包含索引相关的基本信息
 */
public class TableIndex extends JDBCTableIndex<Schema, BaseTablePhysical> {
	private List<TableIndexColumn> columns;

	private int indexId;
	private String indexName;
	private int indexTypeNum;
	private boolean primary;
	private boolean unique;
	private boolean local;
	private int partiType;
	private int partiNum;
	private String partiKey;
	private int gstoNo;
	private int copyNum;
	private int blockSize;
	private int chunkSize;
	private int fieldNum;
	private String keys;
	private String filter;
	private String vocable;
	private String lexer;
	private int wordLen;
	private boolean enableTrans;
	private Date createTime;
	private boolean sys;
	private boolean keepinCache;
	private boolean nologging;
	private boolean valid;

	public TableIndex(Schema schema, BaseTablePhysical table, String indexName, ResultSet dbResult) {
		super(schema, table, indexName, null, true);
		if (dbResult != null) {
			this.indexTypeNum = JDBCUtils.safeGetInteger(dbResult, "INDEX_TYPE");

			switch (this.indexTypeNum) {
			case 0:
				indexType = Constants.INDEX_TYPE_BTREE;
				break;
			case 1:
				indexType = Constants.INDEX_TYPE_RTREE;
				break;
//			case 2:
//				indexType = Constants.INDEX_TYPE_FULL_TEXT;
//				break;
			case 3:
				indexType = Constants.INDEX_TYPE_BITMAP;
				break;
			default:
				indexType = Constants.INDEX_TYPE_BTREE;
			}

			this.indexId = JDBCUtils.safeGetInt(dbResult, "INDEX_ID");
			this.indexName = JDBCUtils.safeGetString(dbResult, "INDEX_NAME");
			this.primary = JDBCUtils.safeGetBoolean(dbResult, "IS_PRIMARY");
			this.unique = JDBCUtils.safeGetBoolean(dbResult, "IS_UNIQUE");
			this.local = JDBCUtils.safeGetBoolean(dbResult, "IS_LOCAL");
			this.partiType = JDBCUtils.safeGetInt(dbResult, "PARTI_TYPE");
			this.partiNum = JDBCUtils.safeGetInt(dbResult, "PARTI_NUM");
			this.partiKey = JDBCUtils.safeGetString(dbResult, "PARTI_KEY");
			this.gstoNo = JDBCUtils.safeGetInt(dbResult, "GSTO_NO");
			this.copyNum = JDBCUtils.safeGetInt(dbResult, "COPY_NUM");
			this.blockSize = JDBCUtils.safeGetInt(dbResult, "BLOCK_SIZE");
			this.chunkSize = JDBCUtils.safeGetInt(dbResult, "CHUNK_SIZE");
			this.fieldNum = JDBCUtils.safeGetInt(dbResult, "FIELD_NUM");
			this.keys = JDBCUtils.safeGetString(dbResult, "KEYS");
			this.filter = JDBCUtils.safeGetString(dbResult, "FILTER");
			this.vocable = JDBCUtils.safeGetString(dbResult, "VOCABLE");
			this.lexer = JDBCUtils.safeGetString(dbResult, "LEXER");
			this.wordLen = JDBCUtils.safeGetInt(dbResult, "WORD_LEN");
			this.enableTrans = JDBCUtils.safeGetBoolean(dbResult, "ENABLE_TRANS");
			this.createTime = JDBCUtils.safeGetDate(dbResult, "CREATE_TIME");
			this.sys = JDBCUtils.safeGetBoolean(dbResult, "IS_SYS");
			this.keepinCache = JDBCUtils.safeGetBoolean(dbResult, "KEEPIN_CACHE");
			this.nologging = JDBCUtils.safeGetBoolean(dbResult, "INDEX_ID");
			this.valid = JDBCUtils.safeGetBoolean(dbResult, "INDEX_ID");
		}
	}

	public TableIndex(Schema schema, BaseTablePhysical parent, String name, boolean unique, DBSIndexType indexType) {
		super(schema, parent, name, indexType, false);
		this.unique = unique;
	}

	public TableIndex(DBRProgressMonitor monitor, Table table, DBSTableIndex source) throws DBException {
		super(table.getSchema(), table, source, false);
		this.setIndexType(source.getIndexType());
		this.setUnique(source.isUnique());
        List<? extends DBSTableIndexColumn> columns = source.getAttributeReferences(monitor);
        if (columns != null) {
            this.columns = new ArrayList<>(columns.size());
            for (DBSTableIndexColumn sourceColumn : columns) {
                this.columns.add(new TableIndexColumn(monitor, this, (TableIndexColumn) sourceColumn));
            }
        }
		
	}

	@NotNull
	@Override
	public DataSource getDataSource() {
		return getTable().getDataSource();
	}

	@Override
	@Property(viewable = true, order = 5)
	public boolean isUnique() {
		return unique;
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public List<TableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor) {
		return columns;
	}

	void setColumns(List<TableIndexColumn> columns) {
		this.columns = columns;
	}

	public void addColumn(TableIndexColumn column) {
		if (columns == null) {
			columns = new ArrayList<>();
		}
		columns.add(column);
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), this);
	}

	@Override
	public String toString() {
		return getFullyQualifiedName(DBPEvaluationContext.UI);
	}

	public List<TableIndexColumn> getColumns() {
		return columns;
	}

	public int getIndexId() {
		return indexId;
	}

	public String getIndexName() {
		return indexName;
	}

	public int getIndexTypeNum() {
		return indexTypeNum;
	}

	@Override
	public boolean isPrimary() {
		return primary;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	@Property(viewable = true, order = 6)
	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
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

	public int getFieldNum() {
		return fieldNum;
	}

	public String getKeys() {
		return keys;
	}

	public String getFilter() {
		return filter;
	}

	public String getVocable() {
		return vocable;
	}

	public String getLexer() {
		return lexer;
	}

	public int getWordLen() {
		return wordLen;
	}

	public boolean isEnableTrans() {
		return enableTrans;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public boolean isSys() {
		return sys;
	}

	public boolean isKeepinCache() {
		return keepinCache;
	}

	public boolean isNologging() {
		return nologging;
	}

	public boolean isValid() {
		return valid;
	}
}
