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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.internal.Constants;
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 加载表相关物理信息（分区信息、表空间信息）
 */
public abstract class BaseTablePhysical extends BaseTable implements DBSObjectLazy<DataSource>, DBDPseudoAttributeContainer {
	private static final Log log = Log.getLog(BaseTablePhysical.class);

	public static final String CAT_STATISTICS = "Statistics";

	private long rowCount;
	private Long realRowCount;
	private Object tablespace;
	private Integer partitioned;
	public PartitionCache partitionCache = new PartitionCache();
	public SubPartitionCache subPartitionCache = new SubPartitionCache();

	protected BaseTablePhysical(Schema schema, String name) {
		super(schema, name, false);
	}

	protected BaseTablePhysical(Schema schema, ResultSet dbResult) {
		super(schema, dbResult, ObjectType.TABLE);

		// 加载表分区信息
		this.partitioned = JDBCUtils.safeGetInteger(dbResult, "PARTI_TYPE");
	}

	// 复制构造函数
	public BaseTablePhysical(DBRProgressMonitor monitor, Schema schema, DBSEntity source) throws DBException {
		super(monitor, schema, source);
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return tablespace;
	}

	public Object getTablespace() {
		return tablespace;
	}

	public void setTablespace(Tablespace tablespace) {
		this.tablespace = tablespace;
	}

	/**
	 * 使用缓存读取索引
	 */
	@Override
	@Association
	public Collection<TableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		return this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
	}

	public TableIndex getIndex(DBRProgressMonitor monitor, String name) throws DBException {
		return this.getContainer().indexCache.getObject(monitor, getContainer(), this, name);
	}

	@Association
	public Collection<TablePartition> getPartitions(DBRProgressMonitor monitor) throws DBException {
		if (partitionCache == null) {
			return null;
		} else {
			return this.partitionCache.getAllObjects(monitor, this);
		}
	}

	@Association
	public Collection<TableSubPartition> getSubPartitions(DBRProgressMonitor monitor) throws DBException {
		if (subPartitionCache == null) {
			return null;
		} else {
			this.subPartitionCache.getAllObjects(monitor, this);
			return this.subPartitionCache.getAllObjects(monitor, this);
		}
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		getContainer().indexCache.clearObjectCache(this);
		return super.refreshObject(monitor);
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = Utils.getObjectStatus(monitor, this, ObjectType.TABLE);
	}
	
	@Override
	public DBDPseudoAttribute[] getPseudoAttributes() throws DBException {
		return new DBDPseudoAttribute[] { Constants.PSEUDO_ATTR_ROWID };
	}

	public static class PartitionCache extends JDBCObjectCache<BaseTablePhysical, TablePartition> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull BaseTablePhysical table)
				throws SQLException {
			StringBuilder builder = new StringBuilder();
			builder.append("SELECT * FROM ");
			builder.append(table.getSchema().getRoleFlag());
			builder.append(
					"_PARTIS P INNER JOIN (SELECT PARTI_TYPE, PARTI_KEY, AUTO_PARTI_TYPE, AUTO_PARTI_SPAN, TABLE_ID, TABLE_NAME, PARTI_NUM, SUBPARTI_NUM FROM ");
			builder.append(table.getSchema().getRoleFlag());
			builder.append("_TABLES T WHERE TABLE_NAME = '");
			builder.append(table.getName());
			builder.append("' AND SCHEMA_ID = ");
			builder.append(table.getSchema().getId() + "and table_id =" + table.getId());
			builder.append(") ON P.TABLE_ID = T.TABLE_ID");

			final JDBCPreparedStatement dbStat = session.prepareStatement(builder.toString());
			return dbStat;
		}

		@Override
		protected TablePartition fetchObject(JDBCSession session, BaseTablePhysical owner, JDBCResultSet resultSet)
				throws SQLException, DBException {
			return new TablePartition(owner, false, resultSet);
		}
	}

	public static class SubPartitionCache extends JDBCObjectCache<BaseTablePhysical, TableSubPartition> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull BaseTablePhysical table)
				throws SQLException {
			StringBuilder builder = new StringBuilder();
			builder.append("SELECT * FROM ");
			builder.append(table.getSchema().getRoleFlag());
			builder.append("_SUBPARTIS SP INNER JOIN (SELECT SUBPARTI_TYPE, SUBPARTI_KEY, TABLE_ID, TABLE_NAME, PARTI_NUM, SUBPARTI_NUM FROM  ");
			builder.append(table.getSchema().getRoleFlag());
			builder.append("_TABLES T WHERE TABLE_NAME = '");
			builder.append(table.getName());
			builder.append("' AND SCHEMA_ID = ");
			builder.append(table.getSchema().getId() + "and table_id =" + table.getId());
			builder.append(") ON SP.TABLE_ID = T.TABLE_ID");
			final JDBCPreparedStatement dbStat = session.prepareStatement(builder.toString());
			return dbStat;
		}

		@Override
		protected TableSubPartition fetchObject(JDBCSession session, BaseTablePhysical owner, JDBCResultSet resultSet)
				throws SQLException, DBException {
			return new TableSubPartition(owner, true, resultSet);
		}
	}

	public static class TablespaceListProvider implements IPropertyValueListProvider<BaseTablePhysical> {
		@Override
		public boolean allowCustomValue() {
			return false;
		}

		@Override
		public Object[] getPossibleValues(BaseTablePhysical object) {
			final List<Tablespace> tablespaces = new ArrayList<>();
			try {
				tablespaces.addAll(object.getDataSource().getTablespaces(new VoidProgressMonitor()));
			} catch (DBException e) {
				log.error(e);
			}
			tablespaces.sort(DBUtils.<Tablespace>nameComparator());
			return tablespaces.toArray(new Tablespace[tablespaces.size()]);
		}
	}
}
