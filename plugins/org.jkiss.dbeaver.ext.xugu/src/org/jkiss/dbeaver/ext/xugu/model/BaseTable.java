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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.source.StatefulObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 表信息基类，包含触发器缓存
 */
public abstract class BaseTable extends JDBCTable<DataSource, Schema>
		implements DBPNamedObject2, DBPRefreshableObject, StatefulObject {
	private static final Log log = Log.getLog(BaseTable.class);

	private int id;
	protected boolean valid;
	private String comment;
	private Timestamp createTime;
	private ObjectType tableType;

	public final TriggerCache triggerCache = new TriggerCache();

	/**
	 * 获取表类型名称
	 * 
	 * @return 表类型名称
	 */
	protected abstract String getTableTypeName();

	public static class TableAdditionalInfo {
		volatile boolean loaded = false;

		boolean isLoaded() {
			return loaded;
		}
	}

	public static class CommentsValidator implements IPropertyCacheValidator<BaseTable> {
		@Override
		public boolean isPropertyCached(BaseTable object, Object propertyId) {
			return object.comment != null;
		}
	}

	protected BaseTable(Schema schema, String name, boolean persisted) {
		super(schema, name, persisted);
	}

	protected BaseTable(Schema schema, ResultSet dbResult, ObjectType type) {
		super(schema, true);
		if (type.getTypeName().equals(ObjectType.TABLE.getTypeName())) {
			setName(JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
			this.id = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
		} else {
			setName(JDBCUtils.safeGetString(dbResult, "VIEW_NAME"));
			this.id = JDBCUtils.safeGetInt(dbResult, "VIEW_ID");
		}
		this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
		this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
		this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
		this.tableType = type;
	}

	// 复制构造函数
	public BaseTable(DBRProgressMonitor monitor, Schema schema, DBSEntity source) throws DBException {
		super(schema, source, false);
		
        DBSObjectCache<BaseTable, TableColumn> colCache = getContainer().tableCache.getChildrenCache(this);
        // 复制列
        for (DBSEntityAttribute srcColumn : CommonUtils.safeCollection(source.getAttributes(monitor))) {
            if (DBUtils.isHiddenObject(srcColumn)) {
                continue;
            }
            TableColumn column = new TableColumn(monitor, this, srcColumn);
            colCache.cacheObject(column);
        }
	}

	/**
	 * 对象类型（表或试图）
	 * 
	 * @return 对象类型
	 */
	public ObjectType getType() {
		return tableType;
	}

	@Override
	public JDBCStructCache<Schema, BaseTable, TableColumn> getCache() {
		return getContainer().tableCache;
	}

	@Override
	@NotNull
	public Schema getSchema() {
		return super.getContainer();
	}

	@NotNull
	@Property(viewable = false, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = -1)
	public int getId() {
		return id;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	public String getName() {
		return super.getName();
	}

	@NotNull
	@Property(viewable = true, editable = true, updatable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 2)
	public String getComment() {
		return comment;
	}

	@NotNull
	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
	public Timestamp getCreateTime() {
		return createTime;
	}

	@NotNull
	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 4)
	public boolean isValid() {
		return valid;
	}

	@Nullable
	@Override
	public String getDescription() {
		return getComment();
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getContainer(), this);
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * 获取表注释信息
	 * 
	 * @param monitor 进程监视器
	 * @return 表注释
	 * @throws DBException 数据库异常
	 */
	@Property(viewable = true, editable = true, updatable = true, order = 100)
	@LazyProperty(cacheValidator = CommentsValidator.class)
	public String getComment(DBRProgressMonitor monitor) throws DBException {
		if (comment == null) {
			int tableType = 0;
			final String viewType = "VIEW";
			if (viewType.equals(getTableTypeName())) {
				tableType = 1;
			}
			try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table comments")) {
				comment = JDBCUtils.queryString(session,
						"SELECT COMMENTS FROM " + this.getDataSource().getRoleFlag()
								+ "_TABLES WHERE TABLE_ID=? AND TABLE_TYPE=? AND DB_ID=?",
						getId(), tableType, this.getSchema().getDbId(this.getSchema(), session));
				if (comment == null) {
					comment = "";
				}
			} catch (SQLException e) {
				log.warn("Can't fetch table '" + getName() + "' comment", e);
			}
		}
		return comment;
	}

	/**
	 * 获取列名及注释信息
	 * 
	 * @param monitor 进程监视器
	 */
	void loadColumnComments(DBRProgressMonitor monitor) {
		try {
			try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table column comments")) {
				try (JDBCPreparedStatement stat = session.prepareStatement("SELECT COL_NAME,COMMENTS FROM "
						+ this.getDataSource().getRoleFlag() + "_COLUMNS CC WHERE CC.TABLE_ID=? AND DB_ID=?")) {
					stat.setInt(1, getId());
					stat.setInt(2, this.getSchema().getDbId(this.getSchema(), session));
					try (JDBCResultSet resultSet = stat.executeQuery()) {
						while (resultSet.next()) {
							String colName = resultSet.getString(1);
							String colComment = resultSet.getString(2);
							TableColumn col = getAttribute(monitor, colName);
							if (col == null) {
								log.warn("Column '" + colName + "' not found in table '"
										+ getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
							} else {
								col.setComment(CommonUtils.notEmpty(colComment));
							}
						}
					}
				}
			}
			for (DBSEntityAttribute col : getAttributes(monitor)) {
				((TableColumn)col).cacheComment();
			}
		} catch (Exception e) {
			log.warn("Error fetching table '" + getName() + "' column comments", e);
		}
	}

	@Override
	public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().tableCache.getChildren(monitor, getContainer(), this);
	}

	@Override
	public TableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
			throws DBException {
		return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		getContainer().constraintCache.clearObjectCache(this);
		return getContainer().tableCache.refreshObject(monitor, getContainer(), this);
	}

	@Association
	public List<Trigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
		if (this.isPersisted()) {
			return triggerCache.getAllObjects(monitor, this);
		}
		return null;
	}

	@Override
	public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		// TODO 获取索引
		return null;
	}

	@Nullable
	@Override
	@Association
	public Collection<TableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().constraintCache.getObjects(monitor, getContainer(), this);
	}

	public TableConstraint getConstraint(DBRProgressMonitor monitor, String ukName) throws DBException {
		return getContainer().constraintCache.getObject(monitor, getContainer(), this, ukName);
	}

	public DBSTableForeignKey getForeignKey(DBRProgressMonitor monitor, String ukName) throws DBException {
		return DBUtils.findObject(getAssociations(monitor), ukName);
	}

	@Override
	public Collection<TableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().foreignKeyCache.getAllObjects(monitor, this.getSchema());
	}

	@Override
	public Collection<TableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().foreignKeyCache.getAllObjects(monitor, this.getSchema());
	}

	public String getDdl(DBRProgressMonitor monitor, DDLFormat ddlFormat, Map<String, Object> options)
			throws DBException {
		return Utils.getDdl(monitor, getTableTypeName(), this, ddlFormat, options);
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	public static BaseTable findTable(DBRProgressMonitor monitor, DataSource dataSource, String ownerName,
			String tableName) throws DBException {
		Schema refSchema = dataSource.getSchema(monitor, ownerName);
		if (refSchema == null) {
			log.warn("Referenced schema '" + ownerName + "' not found");
			return null;
		} else {
			BaseTable refTable = refSchema.tableCache.getObject(monitor, refSchema, tableName);
			if (refTable == null) {
				log.warn("Referenced table '" + tableName + "' not found in schema '" + ownerName + "'");
			}
			return refTable;
		}
	}

	public static BaseTable findView(DBRProgressMonitor monitor, DataSource dataSource, String ownerName,
			String viewName) throws DBException {
		Schema refSchema = dataSource.getSchema(monitor, ownerName);
		if (refSchema == null) {
			log.warn("Referenced schema '" + ownerName + "' not found");
			return null;
		} else {
			BaseTable refView = refSchema.viewCache.getObject(monitor, refSchema, viewName);
			if (refView == null) {
				log.warn("Referenced view '" + viewName + "' not found in schema '" + ownerName + "'");
			}
			return refView;
		}
	}

	static class TriggerCache extends JDBCStructCache<BaseTable, Trigger, TriggerColumn> {
		TriggerCache() {
			super("TRIGGER_NAME");
		}

		// 获取触发器信息
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull BaseTable owner)
				throws SQLException {
			StringBuilder builder = new StringBuilder();
			// 对象类型为table
			if (ObjectType.TABLE.equals(owner.getType())) {
				builder.append("SELECT *, TR.OBJ_ID AS TABLE_ID\nFROM ");
				builder.append(owner.getDataSource().getRoleFlag());
				builder.append("_TRIGGERS TR WHERE SCHEMA_ID=");
				builder.append(owner.getSchema().getId());
				builder.append(" AND TABLE_ID=");
				builder.append(owner.getId());
				builder.append("\n ORDER BY TRIG_NAME");
			}
			// 对象类型为view
			else {
				builder.append("SELECT *, TR.OBJ_ID AS VIEW_ID\nFROM ");
				builder.append(owner.getDataSource().getRoleFlag());
				builder.append("_TRIGGERS TR WHERE SCHEMA_ID=");
				builder.append(owner.getSchema().getId());
				builder.append(" AND VIEW_ID=");
				builder.append(owner.getId());
				builder.append("\n ORDER BY TRIG_NAME");
			}

			log.debug("[" + OemConfig.OEM_NAME_EN+ "] Construct select triggers sql: " + builder.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(builder.toString());
			return dbStat;
		}

		@Override
		protected Trigger fetchObject(@NotNull JDBCSession session, @NotNull BaseTable owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new Trigger(owner, resultSet);
		}

		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull BaseTable owner,
				@Nullable Trigger forObject) throws SQLException {
			// 通过获取description中的字段名来查询触发器的列信息
			String cols = forObject.getDescription();
			int updateKeyIndex = cols == null ? -1 : cols.toUpperCase().indexOf("UPDATE");
			int columnOfKeyIndex = cols == null ? -1 : cols.toUpperCase().indexOf("OF", updateKeyIndex);
			int onKeyIndex = cols == null ? -1 : cols.toUpperCase().indexOf(" ON ");
			boolean isSetColumn = updateKeyIndex == -1 ? false : columnOfKeyIndex != -1;
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM ");
			sql.append(owner.getDataSource().getRoleFlag());
			// 对象类型为table
			if (ObjectType.TABLE.equals(owner.getType())) {
				sql.append("_COLUMNS WHERE TABLE_ID=");
			}
			// 对象类型为view
			else {
				sql.append("_VIEW_COLUMNS WHERE VIEW_ID=");
			}
			sql.append(owner.getId());

			// 指定了特殊字段则仅查询指定字段，若没有则直接查该表的所有列
			if (isSetColumn && onKeyIndex != -1) {
				cols = cols.substring(columnOfKeyIndex + 2, onKeyIndex).trim();
				String[] col = cols.split(",");
				sql.append(" AND COL_NAME IN (");
				for (int i = 0; i < col.length; i++) {
					sql.append("'");
					sql.append(col[i].replaceAll("\"", ""));
					sql.append("'");
					if (i != col.length - 1) {
						sql.append(",");
					}
				}
				sql.append(")");
			}

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct select trigger columns sql: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected TriggerColumn fetchChild(@NotNull JDBCSession session, @NotNull BaseTable owner,
				@NotNull Trigger parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			if (owner instanceof Table) {
				BaseTable refTable = BaseTable.findTable(session.getProgressMonitor(), owner.getDataSource(),
						owner.getSchema().getName(), owner.getName());
				if (refTable != null) {
					final String columnName = JDBCUtils.safeGetString(dbResult, "COL_NAME");
					TableColumn tableColumn = refTable.getAttribute(session.getProgressMonitor(), columnName);
					if (tableColumn == null) {
						log.debug("Column '" + columnName + "' not found in table '"
								+ refTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "' for trigger '"
								+ parent.getName() + "'");
					}
					return new TriggerColumn(session.getProgressMonitor(), parent, tableColumn, dbResult);
				}
				return null;
			} else {
				BaseTable refView = BaseTable.findView(session.getProgressMonitor(), owner.getDataSource(),
						owner.getSchema().getName(), owner.getName());
				if (refView != null) {
					final String columnName = JDBCUtils.safeGetString(dbResult, "COL_NAME");
					TableColumn tableColumn = refView.getAttribute(session.getProgressMonitor(), columnName);
					if (tableColumn == null) {
						log.debug("Column '" + columnName + "' not found in view '"
								+ refView.getFullyQualifiedName(DBPEvaluationContext.DDL) + "' for trigger '"
								+ parent.getName() + "'");
					}
					return new TriggerColumn(session.getProgressMonitor(), parent, tableColumn, dbResult);
				}
				return null;
			}
		}
	}
}
