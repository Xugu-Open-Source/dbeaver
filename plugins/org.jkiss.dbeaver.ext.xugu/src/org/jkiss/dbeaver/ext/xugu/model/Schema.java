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
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 模式信息类，包含模式相关的基本信息，以及表级对象缓存（表、视图、约束、外键、索引、序列、包、存储过程、作业、同义词、自定义类型）
 * 
 * @author Xugu
 */
public class Schema extends BaseGlobalObject
		implements DBSSchema, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer ,DBPNamedObject2 {
	private static final Log log = Log.getLog(Schema.class);

	final public TableCache tableCache = new TableCache();
	final public ViewCache viewCache = new ViewCache();
	final public ConstraintCache constraintCache = new ConstraintCache();
	final public ForeignKeyCache foreignKeyCache = new ForeignKeyCache();
	final public IndexCache indexCache = new IndexCache();
	final public SequenceCache sequenceCache = new SequenceCache();
	final public PackageCache packageCache = new PackageCache();
	final public SynonymCache synonymCache = new SynonymCache();
	final public UdtCache udtCache = new UdtCache();
	final public ProceduresCache proceduresCache = new ProceduresCache();
	final public FunctionsCache functionsCache = new FunctionsCache();
	final public SchedulerJobCache schedulerJobCache = new SchedulerJobCache();
	final public TriggerCache triggerCache = new TriggerCache();
 	final public ProcedurePackagedCache procedurePackagedCache = new ProcedurePackagedCache();
 	
	private long id;
	private String name;
	private String owner;
	private String comment;
	private String roleFlag;
	private Database parent;
	private transient User user;
	
	private DataSource dataSource;

	/**
	 * 通过指定模式ID和模式名称构造一个新的模式对象
	 * 
	 * @param dataSource 数据源
	 * @param id         模式ID
	 * @param name       模式名称
	 */
	public Schema(DataSource dataSource, long id, String name) {
		super(dataSource, id > 0);
		this.id = id;
		this.name = name;
		this.roleFlag = dataSource.getRoleFlag();
		this.dataSource= dataSource;
	}

	/**
	 * 通过结果集构造一个新的模式对象，同时指定其所属数据库
	 * 
	 * @param dataSource 数据源
	 * @param parent     所属数据库
	 * @param dbResult   查询结果集
	 */
	public Schema(@NotNull DataSource dataSource, Database parent, ResultSet dbResult) {
		super(dataSource, true);
		this.id = JDBCUtils.safeGetLong(dbResult, "SCHEMA_ID");
		this.name = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
		this.owner = JDBCUtils.safeGetString(dbResult, "USER_NAME");
		this.comment = JDBCUtils.safeGetString(dbResult, "COMMENT");
		this.dataSource= dataSource;
		this.roleFlag = dataSource.getRoleFlag();
		this.parent = parent;
		if (CommonUtils.isEmpty(this.name)) {
			log.warn("Empty schema name fetched");
			this.name = "? " + super.hashCode();
		}
	}

	/**
	 * 通过结果集构造一个新的模式对象
	 * 
	 * @param dataSource 数据源
	 * @param dbResult   查询结果集
	 */
	public Schema(@NotNull DataSource dataSource, @NotNull ResultSet dbResult) {
		super(dataSource, true);
		this.id = JDBCUtils.safeGetLong(dbResult, "SCHEMA_ID");
		this.name = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
		this.owner = JDBCUtils.safeGetString(dbResult, "USER_NAME");
		this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
		this.roleFlag = dataSource.getRoleFlag();
		this.dataSource= dataSource;
		if (CommonUtils.isEmpty(this.name)) {
			log.warn("Empty schema name fetched");
			this.name = "? " + super.hashCode();
		}
	}

	public boolean isPublic() {
		return Constants.USER_PUBLIC.equals(this.name);
	}

	@NotNull
	@Property(viewable = true, editable = false, order = 200)
	public long getId() {
		return id;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, order = 1)
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@NotNull
	@Property(viewable = true, editable = true, updatable = true, order = 2)
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 3)
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getDbId(Schema schema, JDBCSession session) {
		try {
			String dbName = schema.getName();
			int dbId = schema.getDataSource().databaseCache
					.getObject(session.getProgressMonitor(), schema.getDataSource(), dbName).getId();
			return dbId;
		} catch (DBException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public Database getParent() {
		return parent;
	}

	public void setParent(Database parent) {
		this.parent = parent;
	}

	@Nullable
	@Override
	public String getDescription() {
		return this.comment;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getRoleFlag() {
		return roleFlag;
	}

	public Collection<? extends DBSDataType> getLocalDataTypes() {
		return udtCache.getCachedObjects();
	}

	/**
	 * 从索引缓存中获取模式包含的所有索引信息（提供给界面展示）
	 * 
	 * @param monitor 监控
	 * @return 索引列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<TableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
		Collection<TableIndex> list = indexCache.getObjects(monitor, this, null);
		return list;
	}

	/**
	 * 从表缓存中获取模式包含的所有表信息（提供给界面展示）
	 * 
	 * @param monitor 监控
	 * @return 表列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<Table> getTables(DBRProgressMonitor monitor) throws DBException {
		Collection<Table> list = tableCache.getTypedObjects(monitor, this, Table.class);
		return list;
	}

	/**
	 * 根据表名从缓存中获取指定的表信息
	 * 
	 * @param monitor 监控
	 * @param name    表名
	 * @return 表对象
	 * @throws DBException 数据库异常
	 */
	public Table getTable(DBRProgressMonitor monitor, String name) throws DBException {
		Table table = tableCache.getObject(monitor, this, name, Table.class);
		return table;
	}

	/**
	 * 从视图缓存中获取所有视图信息（提供给界面展示）
	 * 
	 * @param monitor 监控
	 * @return list 视图列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<View> getViews(DBRProgressMonitor monitor) throws DBException {
		Collection<View> list = viewCache.getAllObjects(monitor, this);
		return list;
	}

	/**
	 * 根据视图名从缓存中获取指定的视图信息
	 * 
	 * @param monitor 监控
	 * @param name    视图名
	 * @return view 视图对象
	 * @throws DBException 数据库异常
	 */
	public View getView(DBRProgressMonitor monitor, String name) throws DBException {
		View view = viewCache.getObject(monitor, this, name, View.class);
		return view;
	}

	/**
	 * 从序列缓存中获取全部的缓存信息（提供给界面展示）
	 * 
	 * @param monitor 监控
	 * @return list 序列列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<Sequence> getSequences(DBRProgressMonitor monitor) throws DBException {
		Collection<Sequence> list = sequenceCache.getAllObjects(monitor, this);
		return list;
	}

	/**
	 * 从包缓存中获取全部的包信息（提供给界面展示）
	 * 
	 * @param monitor 监控
	 * @return list 包列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<Package> getPackages(DBRProgressMonitor monitor) throws DBException {
		Collection<Package> list = packageCache.getAllObjects(monitor, this);
		return list;
	}

	/**
	 * 从存储过程缓存中获取全部的存储过程信息（提供给界面展示）
	 * 
	 * @param monitor 监控
	 * @return list 存储过程列表
	 * @throws DBException 数据库异常
	 */
	@Override
	@Association
	public Collection<ProcedureStandalone> getProcedures(DBRProgressMonitor monitor) throws DBException {
		Collection<ProcedureStandalone> list = proceduresCache.getAllObjects(monitor, this);
		return list;
	}

	/**
	 * 根据存储过程名从缓存中获取指定的存储过程信息
	 * 
	 * @param monitor    监控
	 * @param uniqueName 存储过程名
	 * @return procedure 存储过程对象
	 * @throws DBException 数据库异常
	 */
	@Override
	public ProcedureStandalone getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
		ProcedureStandalone procedure = proceduresCache.getObject(monitor, this, uniqueName);
		return procedure;
	}
	
	/**
	 * 从存储函数缓存中获取全部的存储函数信息（提供给界面展示）
	 * 
	 * @param monitor 监控
	 * @return list 存储函数列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<ProcedureStandalone> getFunctions(DBRProgressMonitor monitor) throws DBException {
		Collection<ProcedureStandalone> list = functionsCache.getAllObjects(monitor, this);
		return list;
	}

	
	
	
	
	/**
	 * 根据存储函数名从缓存中获取指定的存储函数信息
	 * 
	 * @param monitor    监控
	 * @param uniqueName 存储函数名
	 * @return procedure 存储函数对象
	 * @throws DBException 数据库异常
	 */
	public ProcedureStandalone getFunction(DBRProgressMonitor monitor, String uniqueName) throws DBException {
		ProcedureStandalone procedure = functionsCache.getObject(monitor, this, uniqueName);
		return procedure;
	}

	/**
	 * 从同义词缓存中获取全部的同义词信息
	 * 
	 * @param monitor 监控
	 * @return list 同义词列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<Synonym> getSynonyms(DBRProgressMonitor monitor) throws DBException {
		Collection<Synonym> list = synonymCache.getAllObjects(monitor, this);
		return list;
	}
	
	
	/**
	 * 从缓存触发器中获取全部触发器信息
	 */
	@Association
	public Collection<NewTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
		Collection<NewTrigger> list = triggerCache.getAllObjects(monitor, this);
		return list;
	}
	
	
	/**
	 * 从缓存触发器中获取全部触发器信息
	 */
//	@Association
//	public Collection<TriggerTest> getTriggers(DBRProgressMonitor monitor) throws DBException {
//		Collection<TriggerTest> list = triggerCache.getAllObjects(monitor, this);
//		return list;
//	}
	

	/**
	 * 根据指定的同义词名从缓存中获取指定的同义词信息
	 * 
	 * @param monitor 监控
	 * @param name    同义词名
	 * @return synonym 同义词对象
	 * @throws DBException 数据库异常
	 */
	public Synonym getSynonym(DBRProgressMonitor monitor, String name) throws DBException {
		Synonym synonym = synonymCache.getObject(monitor, this, name, Synonym.class);
		return synonym;
	}
	
	/**
	 * 根据指定的触发器名从缓存中获取指定的触发器信息
	 * 
	 * @param monitor 监控
	 * @param name    触发器名
	 * @return synonym 触发器对象
	 * @throws DBException 数据库异常
	 */
	public NewTrigger getTrigger(DBRProgressMonitor monitor, String name) throws DBException {
		NewTrigger trigger = triggerCache.getObject(monitor, this, name, NewTrigger.class);
		return trigger;
	}
//	public TriggerTest getTrigger(DBRProgressMonitor monitor, String name) throws DBException {
//		TriggerTest trigger = triggerCache.getObject(monitor, this, name, TriggerTest.class);
//		return trigger;
//	}

	/**
	 * 从用户自定义数据类型缓存中获取全部的自定义数据类型信息
	 * 
	 * @param monitor 监控
	 * @return list 数据类型列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<Udt> getUdts(DBRProgressMonitor monitor) throws DBException {
		Collection<Udt> list = udtCache.getAllObjects(monitor, this);
		return list;
	}

	/**
	 * 根据指定的用户自定义数据类型名从缓存中获取指定的数据类型信息
	 * 
	 * @param monitor 监控
	 * @param name    数据类型名
	 * @return udt 数据类型对象
	 * @throws DBException 数据库异常
	 */
	public Udt getUdt(DBRProgressMonitor monitor, String name) throws DBException {
		Udt udt = udtCache.getObject(monitor, this, name, Udt.class);
		return udt;
	}

	/**
	 * 从作业缓存中获取全部的作业信息
	 * 
	 * @param monitor 监控
	 * @return list 作业列表
	 * @throws DBException 数据库异常
	 */
	@Association
	public Collection<SchedulerJob> getSchedulerJobs(DBRProgressMonitor monitor) throws DBException {
		Collection<SchedulerJob> list = schedulerJobCache.getAllObjects(monitor, this);
		return list;
	}

	public User getSchemaUser(DBRProgressMonitor monitor) throws DBException {
		return getDataSource().getUser(monitor, name);
	}

	@Override
	public Collection<DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
		List<DBSObject> children = new ArrayList<DBSObject>();
		children.addAll(tableCache.getAllObjects(monitor, this));
		children.addAll(viewCache.getAllObjects(monitor, this));
		children.addAll(sequenceCache.getAllObjects(monitor, this));
		children.addAll(packageCache.getAllObjects(monitor, this));
		children.addAll(synonymCache.getAllObjects(monitor, this));
		children.addAll(triggerCache.getAllObjects(monitor, this));
		children.addAll(udtCache.getAllObjects(monitor, this));
		children.addAll(schedulerJobCache.getAllObjects(monitor, this));
		return children;
	}

	@Override
	public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
		BaseTable table = tableCache.getObject(monitor, this, childName);
		if (table != null) {
			return table;
		}
		Synonym synonym = synonymCache.getObject(monitor, this, childName);
		if (synonym != null) {
			return synonym;
		}
		
		NewTrigger trigger = triggerCache.getObject(monitor, this, childName);
		if (trigger != null) {
			return trigger;
		}
//		TriggerTest trigger = triggerCache.getObject(monitor, this, childName);
//		if (trigger != null) {
//			return trigger;
//		}	

		return packageCache.getObject(monitor, this, childName);
	}

	@Override
	public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
		return DBSEntity.class;
	}

	@Override
	public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
		monitor.subTask("Cache tables");
		tableCache.getAllObjects(monitor, this);
		monitor.subTask("Cache views");
		viewCache.getAllObjects(monitor, this);
		monitor.subTask("Cache udts");
		udtCache.getAllObjects(monitor, this);
		if ((scope & STRUCT_ATTRIBUTES) != 0) {
			monitor.subTask("Cache table columns");
			tableCache.loadChildren(monitor, this, null);
			monitor.subTask("Cache view columns");
			viewCache.loadChildren(monitor, this, null);
		}
		if ((scope & STRUCT_ASSOCIATIONS) != 0) {
			monitor.subTask("Cache table indexes");
			indexCache.getObjects(monitor, this, null);
			monitor.subTask("Cache table constraints");
			constraintCache.getObjects(monitor, this, null);
			foreignKeyCache.getObjects(monitor, this, null);
			monitor.subTask("Cache indexes");
			indexCache.getAllObjects(monitor, this);
			monitor.subTask("Cache sequences");
			sequenceCache.getAllObjects(monitor, this);
			monitor.subTask("Cache packages");
			packageCache.getAllObjects(monitor, this);
			monitor.subTask("Cache synonyms");
			synonymCache.getAllObjects(monitor, this);
			monitor.subTask("Cache triggers");
			triggerCache.getAllObjects(monitor, this);
			monitor.subTask("Cache job");
			schedulerJobCache.getAllObjects(monitor, this);
		}
	}

	@Override
	public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		tableCache.clearCache();
		viewCache.clearCache();
		foreignKeyCache.clearCache();
		constraintCache.clearCache();
		indexCache.clearCache();
		packageCache.clearCache();
		proceduresCache.clearCache();
		functionsCache.clearCache();
		sequenceCache.clearCache();
		synonymCache.clearCache();
		triggerCache.clearCache();
		udtCache.clearCache();
		schedulerJobCache.clearCache();
		return this.getDataSource().schemaCache.refreshObject(monitor, this.getDataSource(), this);
	}

	@Override
	public boolean isSystem() {
		return ArrayUtils.contains(Constants.SYSTEM_SCHEMAS, getName());
	}

	@Override
	public String toString() {
		return "Schema " + name;
	}

	private static TableColumn getTableColumn(JDBCSession session, BaseTable parent, ResultSet dbResult)
			throws DBException {
		String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COL_NAME");
		return getTableColumn(session, parent, columnName);
	}

	private static TableColumn getTableColumn(JDBCSession session, BaseTable parent, String columnName)
			throws DBException {
		// 将keys字段中的引号去掉（是否可支持多列？）
		if (columnName == null) {
			return null;
		}
		columnName = columnName.replaceAll("\"", "");
		// que 获取到的列为空？
		TableColumn tableColumn = columnName == null ? null
				: parent.getAttribute(session.getProgressMonitor(), columnName);
		if (tableColumn == null) {
			log.debug("GetTableColumn Column '" + columnName + "' not found in table '" + parent.getName() + "'");
		}
		return tableColumn;
	}

	/**
	 * 表缓存
	 */
	public static class TableCache extends JDBCStructLookupCache<Schema, BaseTable, TableColumn> {

		TableCache() {
			super("TABLE_NAME");
			setListOrderComparator(DBUtils.nameComparator());
		}

		@NotNull
		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull Schema owner,
				@Nullable BaseTable object, @Nullable String objectName) throws SQLException {
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
 
//	 sql.append("select  db_id , null as  view_id , null as col_name , null as col_no ,null as  type_name , null as scale   ,"
//	 		+ "user_id , schema_id ,  table_id , table_name , null as define , null as option, null as create_time, null as valid, null as is_sys, null as comments, table_type, temp_type , field_num , parti_type , parti_num ,\r\n" + 
//	 		"parti_key , auto_parti_type , auto_parti_span , subparti_type , subparti_num , subparti_key ,"
//	 		+ "gsto_no , copy_num , block_size , chunk_size , record_num , pctfree , hotspot_num , use_cache , "
//	 		+ "online , on_commit_del , ena_trans , ena_logging , acl_mask from ");
//		sql.append(roleFlag);
//		sql.append("_TABLES  WHERE DB_ID=");
//		sql.append(owner.getDbId(owner, session));
//		sql.append(" AND SCHEMA_ID=");
//		sql.append(owner.id);
//		sql.append(" union all ");
//		sql.append("select  db_id , null as  view_id , null as col_name , null as col_no ,null as  type_name , null as scale  , "
//				+ " user_id , schema_id ,view_id as table_id ,view_name as table_name ,define ,option ,create_time , valid , is_sys , comments , null as table_type, null as temp_type , null as field_num, null parti_type, null as parti_num, "
//				+ "null as parti_key, null as auto_parti_type, null as auto_parti_span, null as subparti_type, null as subparti_num , null as subparti_key , \r\n" 
//				+ " null as gsto_no , null as copy_num , null as block_size , null as chunk_size , null as record_num , null as pctfree , null as hotspot_num , null as use_cache ,"
//				+ " null as online , null as on_commit_del , null as ena_trans , null as ena_logging , null as acl_mask from ");
//		sql.append(roleFlag);
//		sql.append("_views  WHERE DB_ID=");
//		sql.append(owner.getDbId(owner, session));
//		sql.append(" AND SCHEMA_ID=");
//		sql.append(owner.id);
//		sql.append(" union all ");
//		sql.append("select  db_id , view_id , col_name , col_no , type_name , scale ,   "
//				+ "null as user_id , null as schema_id ,null as table_id ,null as table_name ,null as define ,null as option ,null as create_time ,null as  valid ,"
//				+ "null as  is_sys , comments , null as table_type, null as temp_type , null as field_num, null parti_type, null as parti_num, \r\n" 
//				+ "null as parti_key, null as auto_parti_type, null as auto_parti_span, null as subparti_type, null as subparti_num , null as subparti_key ,\r\n" 
//				+ " null as gsto_no , null as copy_num , null as block_size , null as chunk_size , null as record_num , null as pctfree , null as hotspot_num , null as use_cache ,\r\n" 
//				+ " null as online , null as on_commit_del , null as ena_trans , null as ena_logging , null as acl_mask from ");
//		sql.append(roleFlag);
//		sql.append("_view_columns  WHERE DB_ID=");
//		sql.append(owner.getDbId(owner, session));
//		sql.append(" AND view_id =");
//		if(object!=null) {
//			sql.append(object.getId());
//		}
//		else {
//			sql.append("null");
//		}


//			sql.append("select ta.*, se.min_val,se.step_val,co.col_name from ");
//			sql.append(roleFlag);
//			sql.append("_tables ta,");
//			sql.append(roleFlag);
//			sql.append("_columns co,");
//			sql.append(roleFlag);
//			sql.append("_sequences se where ta.db_id = co.db_id and co.db_id = se.db_id and ta.schema_id = se.schema_id ");
//			sql.append(" and ta.table_id = co.table_id and co.serial_id = se.seq_id ");
//			sql.append(" and ta.db_id = ");
//			sql.append(owner.getDbId(owner, session));
//			sql.append(" and schema_id = ");
//			sql.append(owner.id);
//			sql.append(" and co.is_serial = true ");
//			// 当有检索条件时 只查询指定表 用于新建表之后的刷新工作
//			if (object != null) {
//				sql.append(" and ta.table_id = ");
//				sql.append(object.getId());
//			} else if (objectName != null) {
//				sql.append(" and ta.table_name ='");
//				sql.append(objectName);
//				sql.append("'");
//			}
			

			// xfc 根据schema name 查询所有表信息
			sql.append("SELECT * FROM ");
			sql.append(roleFlag);
			sql.append("_TABLES  WHERE DB_ID=");
			sql.append(owner.getDbId(owner, session));
			sql.append(" AND SCHEMA_ID=");
			sql.append(owner.id);
		// 当有检索条件时 只查询指定表 用于新建表之后的刷新工作
		if (object != null) {
			sql.append(" AND TABLE_ID = ");
			sql.append(object.getId());
		} else if (objectName != null) {
			sql.append(" AND TABLE_NAME ='");
			sql.append(objectName);
			sql.append("'");
		}
			log.debug("" + OemConfig.OEM_NAME_EN + " table metadata: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected BaseTable fetchObject(@NotNull JDBCSession session, @NotNull Schema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			// xfc 修改object_type字段为table_type 并修改为int类型
			int tableType = JDBCUtils.safeGetInt(dbResult, "TABLE_TYPE");
			if (tableType == 0) {
				return new Table(session.getProgressMonitor(), owner, dbResult);
			} else {
				return new View(session.getProgressMonitor(), session, owner, dbResult);
			}
		}

		// 获取列信息
		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull Schema owner,
				@Nullable BaseTable forTable) throws SQLException {
			// xfc 修改了获取列信息的sql
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder(500);
			
//			sql.append("SELECT COL.*,TAB.TABLE_NAME FROM ");
//			sql.append(roleFlag);
//			sql.append("_COLUMNS COL");
//			sql.append(" LEFT JOIN ");
//			sql.append(roleFlag);
//			sql.append("_TABLES TAB ON TAB.TABLE_ID=COL.TABLE_ID");
//			sql.append(" WHERE COL.DB_ID=");
//			sql.append(owner.getDbId(owner, session));
//			if (forTable != null) {
//				sql.append(" AND COL.TABLE_ID=");
//				sql.append(forTable.getId());
//				sql.append(" AND COL.DB_ID=");
//				sql.append(owner.getDbId(owner, session));
//			}

			 
			sql.append("select co.*,c.min_val as min,c.max_val as max ,c.step_val as step from (select  b.table_name ,a.* from ");
			sql.append(roleFlag);
			sql.append("_columns a ,");
			sql.append(roleFlag);
			sql.append("_tables b where a.db_id= b.db_id and a.table_id = b.table_id and b.table_id = ");
			sql.append(forTable.getId());
			sql.append(" and a.db_id = ");
			sql.append(owner.getDbId(owner, session));
			sql.append(" ) as co left join ");
			sql.append(roleFlag);
			sql.append("_sequences c on co.serial_id = c.seq_id ");
			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct select table columns sql: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected TableColumn fetchChild(@NotNull JDBCSession session, @NotNull Schema owner, @NotNull BaseTable table,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new TableColumn(session.getProgressMonitor(), table, dbResult);
		}

		@Override
		protected void cacheChildren(BaseTable parent, List<TableColumn> tableColumns) {
			tableColumns.sort(DBUtils.orderComparator());
			super.cacheChildren(parent, tableColumns);
		}
	}
	

	/**
	 * 约束缓存
	 */
	class ConstraintCache extends JDBCCompositeCache<Schema, BaseTable, TableConstraint, TableConstraintColumn> {
		ConstraintCache() {
			// 修改约束名字段为 CONS_NAME
			super(tableCache, BaseTable.class, "TABLE_NAME", "CONS_NAME");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, Schema owner, BaseTable forTable)
				throws SQLException {
			// 修改了获取约束信息的sql
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder(500);
			sql.append("SELECT DISTINCT *, DEFINE AS COL_NAME, TABLE_NAME FROM ");
			sql.append(roleFlag);
			sql.append("_CONSTRAINTS INNER JOIN (SELECT S.SCHEMA_NAME, T.TABLE_ID, T.TABLE_NAME FROM ");
			sql.append(roleFlag);
			sql.append("_SCHEMAS S INNER JOIN ");
			sql.append(roleFlag);
			sql.append("_TABLES T USING(SCHEMA_ID) ");
			if (forTable != null) {
				sql.append("WHERE TABLE_ID=");
				sql.append(forTable.getId());
			}
			sql.append(") USING(TABLE_ID)");
			sql.append(" WHERE DB_ID=");
			sql.append(owner.getDbId(owner, session));
			sql.append(" AND CONS_TYPE != 'F'");

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct select constraints sql: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Nullable
		@Override
		protected TableConstraint fetchObject(JDBCSession session, Schema owner, BaseTable parent, String indexName,
				JDBCResultSet dbResult) throws SQLException, DBException {
			return new TableConstraint(parent, dbResult);
		}

		@Nullable
		@Override
		protected TableConstraintColumn[] fetchObjectRow(JDBCSession session, BaseTable parent, TableConstraint object,
				JDBCResultSet dbResult) throws SQLException, DBException {
			// 处理多列的情况
			String colName = JDBCUtils.safeGetStringTrimmed(dbResult, "DEFINE");
			if (TableConstraint.getConstraintType(
					JDBCUtils.safeGetString(dbResult, "CONS_TYPE")) == DBSEntityConstraintType.CHECK) {
				return null;
			}
			if (colName != null && !colName.isEmpty()) {
				final String comma = ",";
				final String leftBracket = "(";
				final String rightBracket = ")";

				if (colName.indexOf(comma) != -1) {
					if (colName.indexOf(leftBracket) != -1) {
						colName = colName.substring(colName.indexOf(leftBracket) + 1, colName.indexOf(rightBracket));
					}
					String[] colNames = colName.split(",");
					TableConstraintColumn[] conCols = new TableConstraintColumn[colNames.length];
					for (int i = 0; i < colNames.length; i++) {
						TableColumn tableColumn = getTableColumn(session, parent,
								colNames[i].replace(" DESC", "").trim());
						conCols[i] = new TableConstraintColumn(object, tableColumn, tableColumn.getOrdinalPosition());
					}
					return conCols;
				}
				// 处理单列但是带括号情况
				else if (colName.indexOf(leftBracket) != -1) {
					String realColName = colName.substring(colName.indexOf(leftBracket) + 1,
							colName.indexOf(rightBracket));
					TableColumn tableColumn = getTableColumn(session, parent, realColName);
					return tableColumn == null ? null
							: new TableConstraintColumn[] {
									new TableConstraintColumn(object, tableColumn, tableColumn.getOrdinalPosition()) };
				}
				// 正常的单列情况
				else {
					colName = colName.replaceAll("\"", "");
					TableColumn tableColumn = getTableColumn(session, parent, colName);
					return tableColumn == null ? null
							: new TableConstraintColumn[] {
									new TableConstraintColumn(object, tableColumn, tableColumn.getOrdinalPosition()) };
				}
			}
			TableColumn tableColumn = getTableColumn(session, parent, dbResult);
			// COL_NO无法从结果集直接获取 选择从column中调用get方法
			return tableColumn == null ? null
					: new TableConstraintColumn[] {
							new TableConstraintColumn(object, tableColumn, tableColumn.getOrdinalPosition()) };
		}

		@Override
		protected void cacheChildren(DBRProgressMonitor monitor, TableConstraint constraint,
				List<TableConstraintColumn> rows) {
			constraint.setColumns(rows);
		}
	}

	/**
	 * 外键缓存
	 */
	class ForeignKeyCache extends JDBCCompositeCache<Schema, Table, TableForeignKey, TableForeignKeyColumn> {
		ForeignKeyCache() {
			// 修改约束名字段为 CONS_NAME
			super(tableCache, Table.class, "TABLE_NAME", "CONS_NAME");
		}

		@Override
		protected void loadObjects(DBRProgressMonitor monitor, Schema schema, Table forParent) throws DBException {
			// Cache schema constraints if not table specified
			if (forParent == null) {
				constraintCache.getAllObjects(monitor, schema);
			}
			super.loadObjects(monitor, schema, forParent);
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, Schema owner, Table forTable)
				throws SQLException {
			// 修改了获取外键信息的sql
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder(500);
			sql.append("SELECT DISTINCT F.*,S.SCHEMA_NAME,T.TABLE_NAME, F.DEFINE AS COL_NAME,"
					+ " T2.TABLE_NAME AS REF_TABLE_NAME, F2.CONS_NAME AS REF_NAME FROM ");
			sql.append(roleFlag);
			sql.append("_CONSTRAINTS F INNER JOIN (SELECT S.SCHEMA_NAME, T.TABLE_ID, T.TABLE_NAME FROM ");
			sql.append(roleFlag);
			sql.append("_SCHEMAS S INNER JOIN ");
			sql.append(roleFlag);
			sql.append("_TABLES T USING(SCHEMA_ID) ");
			if (forTable != null) {
				sql.append("WHERE TABLE_ID=");
				sql.append(forTable.getId());
			}
			sql.append(") USING(TABLE_ID) INNER JOIN (SELECT T2.TABLE_NAME, T2.TABLE_ID FROM ");
			sql.append(roleFlag);
			sql.append("_TABLES T2) ON F.REF_TABLE_ID=T2.TABLE_ID JOIN ");
			sql.append(roleFlag);
			sql.append("_CONSTRAINTS F2 ON f.REF_TABLE_ID=F2.TABLE_ID ");
			sql.append("WHERE CONS_TYPE='F' AND DB_ID=");
			sql.append(owner.getDbId(owner, session));

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct select foreign keys sql: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());

			return dbStat;
		}

		@Nullable
		@Override
		protected TableForeignKey fetchObject(JDBCSession session, Schema owner, Table parent, String indexName,
				JDBCResultSet dbResult) throws SQLException, DBException {
			return new TableForeignKey(session.getProgressMonitor(), parent, dbResult);
		}

		@Nullable
		@Override
		protected TableForeignKeyColumn[] fetchObjectRow(JDBCSession session, Table parent, TableForeignKey object,
				JDBCResultSet dbResult) throws SQLException, DBException {
			if (dbResult != null) {
				String colName = JDBCUtils.safeGetStringTrimmed(dbResult, "DEFINE");
				// 处理多列的情况
				final String comma = ",";
				final String leftBracket = "(";
				final String rightBracket = ")";
				if (colName.indexOf(leftBracket) != -1) {
					String refColName = colName.substring(colName.lastIndexOf(leftBracket) + 1,
							colName.lastIndexOf(rightBracket));
					colName = colName.substring(colName.indexOf(leftBracket) + 1, colName.indexOf(rightBracket));
					String[] colNames = colName.split(comma);
					String[] refColNames = refColName.split(comma);
					TableForeignKeyColumn[] conCols = new TableForeignKeyColumn[colNames.length];
					for (int i = 0; i < colNames.length; i++) {
						TableColumn tableColumn = getTableColumn(session, parent, colNames[i]);
						TableColumn fkRefColumn = getTableColumn(session, object.getReferencedConstraint().getTable(),
								refColNames[i]);
						conCols[i] = new TableForeignKeyColumn(object, tableColumn, tableColumn.getOrdinalPosition(),
								fkRefColumn);
					}
					return conCols;
				}
			}
			TableColumn column = getTableColumn(session, parent, dbResult);
			TableColumn refColumn = getTableColumn(session, object.getReferencedConstraint().getTable(), dbResult);
			return column == null ? null
					: new TableForeignKeyColumn[] { new TableForeignKeyColumn(object, column,
							JDBCUtils.safeGetInt(dbResult, "POSITION"), refColumn) };
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void cacheChildren(DBRProgressMonitor monitor, TableForeignKey foreignKey,
				List<TableForeignKeyColumn> rows) {
			foreignKey.setColumns((List) rows);
		}
	}

	/**
	 * 索引缓存
	 */
	class IndexCache extends JDBCCompositeCache<Schema, BaseTablePhysical, TableIndex, TableIndexColumn> {
		IndexCache() {
			super(tableCache, BaseTablePhysical.class, "TABLE_NAME", "INDEX_NAME");
		}

		@NotNull
		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, Schema owner, BaseTablePhysical forTable)
				throws SQLException {
			// 修改了获取索引信息的sql
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT IDX.*,TAB.TABLE_NAME FROM ");
			sql.append(roleFlag);
			sql.append("_INDEXES IDX ");
			sql.append(" LEFT JOIN ");
			sql.append(roleFlag);
			sql.append("_TABLES TAB ON IDX.TABLE_ID=TAB.TABLE_ID ");
			sql.append(" WHERE IDX.DB_ID=");
			sql.append(owner.getDbId(owner, session));
			if (forTable != null) {
				sql.append(" AND IDX.TABLE_ID=");
				sql.append(forTable.getId());
			}

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct select indexes sql: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Nullable
		@Override
		protected TableIndex fetchObject(JDBCSession session, Schema owner, BaseTablePhysical parent, String indexName,
				JDBCResultSet dbResult) throws SQLException, DBException {
			return new TableIndex(owner, parent, indexName, dbResult);
		}

		@Nullable
		@Override
		protected TableIndexColumn[] fetchObjectRow(JDBCSession session, BaseTablePhysical parent, TableIndex object,
				JDBCResultSet dbResult) throws SQLException, DBException {
			// 处理别名情况
			String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "KEYS");
			columnName = columnName.replaceAll("\"", "");
			// 处理多字段情况
			final String comma = ",";
			if (columnName.indexOf(comma) != -1) {
				String[] cols = columnName.split(comma);
				TableIndexColumn[] res = new TableIndexColumn[cols.length];
				for (int i = 0; i < cols.length; i++) {
					boolean ascending = cols[i].toUpperCase().contains(" DESC") ? false : true;
					TableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(),
							cols[i].replace(" DESC", "").trim());
					if (tableColumn == null) {
						log.debug("Column '" + columnName + "' not found in table '" + parent.getName()
								+ "' for index '" + object.getName() + "'");
						return null;
					}
					TableIndexColumn tempIndexCol = new TableIndexColumn(object, tableColumn,
							tableColumn.getOrdinalPosition(), ascending, null);
					res[i] = tempIndexCol;
				}
				return res;
			} else {
				boolean ascending = columnName.toUpperCase().contains(" DESC") ? false : true;
				columnName = columnName.replace(" DESC", "").trim();
				TableColumn tableColumn = columnName == null ? null
						: parent.getAttribute(session.getProgressMonitor(), columnName);
				if (tableColumn == null) {
					log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '"
							+ object.getName() + "'");
					return null;
				}
				return new TableIndexColumn[] {
						new TableIndexColumn(object, tableColumn, tableColumn.getOrdinalPosition(), ascending, null) };
			}
		}

		@Override
		protected void cacheChildren(DBRProgressMonitor monitor, TableIndex index, List<TableIndexColumn> rows) {
			index.setColumns(rows);
		}
	}

	/**
	 * 数据类型缓存
	 */
	static class DataTypeCache extends JDBCObjectCache<Schema, DataType> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull Schema owner)
				throws SQLException {
			// TODO 准备数据类型缓存声明
			return null;
		}

		@Override
		protected DataType fetchObject(@NotNull JDBCSession session, @NotNull Schema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException {
			// TODO 获取数据类型缓存对象
			return null;
		}
	}

	/**
	 * 序列缓存
	 */
	static class SequenceCache extends JDBCObjectCache<Schema, Sequence> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull Schema owner)
				throws SQLException {
			// 修改了获取sequence信息的sql
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM ");
			sql.append(roleFlag);
			sql.append("_SEQUENCES");
			sql.append(" WHERE DB_ID=");
			sql.append(owner.getDbId(owner, session));
			sql.append(" AND SCHEMA_ID=");
			sql.append(owner.getId());
			sql.append(" AND IS_SYS=FALSE");
			sql.append(" ORDER BY SEQ_NAME");

			log.debug("" + OemConfig.OEM_NAME_EN + " sequence metadata: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected Sequence fetchObject(@NotNull JDBCSession session, @NotNull Schema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new Sequence(owner, resultSet);
		}
	}

	/**
	 * 存储过程缓存
	 */
	static class ProceduresCache extends JDBCObjectLookupCache<Schema, ProcedureStandalone> {
		@NotNull
		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull Schema owner,
				@Nullable ProcedureStandalone object, @Nullable String objectName) throws SQLException {
			// xfc 修改了获取存储过程信息的sql语句
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM ");
			sql.append(roleFlag);
			sql.append("_PROCEDURES WHERE DB_ID=");
			sql.append(owner.getDbId(owner, session));
			sql.append(" AND SCHEMA_ID=");
			sql.append(owner.id);
			sql.append(" AND RET_TYPE IS NULL");
			// 当有检索条件时 只查询指定表 用于新建表之后的刷新工作
			if (object != null) {
				sql.append(" AND PROC_ID = ");
				sql.append(object.getObjectId());
				if (DBSProcedureType.FUNCTION == object.getProcedureType()) {
					
				} else {
					sql.append(" AND RET_TYPE IS NOT NULL");
				}
			}

			log.debug("" + OemConfig.OEM_NAME_EN + " procedure metadata: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected ProcedureStandalone fetchObject(@NotNull JDBCSession session, @NotNull Schema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new ProcedureStandalone(session.getProgressMonitor(), owner, dbResult);
		}
	}
	
	/**
	 * 函数缓存
	 */
	static class FunctionsCache extends ProceduresCache {
		@NotNull
		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull Schema owner,
				@Nullable ProcedureStandalone object, @Nullable String objectName) throws SQLException {
			// xfc 修改了获取存储函数信息的sql语句
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM ");
			sql.append(roleFlag);
			sql.append("_PROCEDURES WHERE DB_ID=");
			sql.append(owner.getDbId(owner, session));
			sql.append(" AND SCHEMA_ID=");
			sql.append(owner.id);
			sql.append(" AND RET_TYPE IS NOT NULL");
			// 当有检索条件时 只查询指定表 用于新建表之后的刷新工作
			if (object != null) {
				sql.append(" AND PROC_ID = ");
				sql.append(object.getObjectId());
			}

			log.debug("" + OemConfig.OEM_NAME_EN + " function metadata: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected ProcedureStandalone fetchObject(@NotNull JDBCSession session, @NotNull Schema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new ProcedureStandalone(session.getProgressMonitor(), owner, dbResult);
		}
	}

	/**
	 * 包缓存
	 */
	static class PackageCache extends JDBCObjectCache<Schema, Package> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull Schema owner)
				throws SQLException {
			// xfc 修改了获取所有包信息的sql语句
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM ");
			sql.append(roleFlag);
			sql.append("_PACKAGES WHERE DB_ID=");
			sql.append(owner.getDbId(owner, session));
			sql.append(" AND SCHEMA_ID=");
			sql.append(owner.id);
			log.debug("" + OemConfig.OEM_NAME_EN + " package metadata: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected Package fetchObject(@NotNull JDBCSession session, @NotNull Schema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new Package(owner, dbResult);
		}
	}
	
	
	/**
	 *  包内存储过程缓存 
	 */
	class ProcedurePackagedCache extends JDBCObjectCache<Schema,ProcedurePackaged>{

		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, Schema owner)
				throws SQLException {
			return null; 
		}

		@Override
		protected ProcedurePackaged fetchObject(JDBCSession session, Schema owner, JDBCResultSet resultSet)
				throws SQLException, DBException {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	

	/**
	 * 同义词缓存
	 */
	static class SynonymCache extends JDBCObjectCache<Schema, Synonym> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull Schema owner)
				throws SQLException {
			
			// xfc 修改了获取同义词信息的语句
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			if(owner.getName()==Constants.USER_PUBLIC) {
				sql.append("select s3.schema_name TARG_SC, s1.*  from ");
				sql.append(roleFlag);
				sql.append("_synonyms s1 left join ");
				sql.append(roleFlag);
				sql.append("_schemas s3  ON s3.schema_id=s1.targ_sche_id AND s3.db_id=current_db_id  ");
				sql.append( " where s1.is_public = true ");
			}else {	
				sql.append("select s2.schema_name CURR_SC,s3.schema_name TARG_SC, s1.*  from ");
				sql.append(roleFlag);
				sql.append("_synonyms s1 left join ");
				sql.append(roleFlag);
				sql.append("_schemas s2 ON s2.schema_id=s1.schema_id AND s2.db_id=current_db_id  left join ");
				sql.append(roleFlag);
				sql.append("_schemas s3  ON s3.schema_id=s1.targ_sche_id AND s3.db_id=current_db_id  ");
				sql.append(" where s1.schema_id = ");
				sql.append(owner.getId());
				sql.append( " and s1.is_public = false ");
			}
			log.debug("" + OemConfig.OEM_NAME_EN + " synonyms metadata: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected Synonym fetchObject(@NotNull JDBCSession session, @NotNull Schema owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new Synonym(session.getProgressMonitor(), session, owner, resultSet);
		}
	}
	
	/**
	 * 触发器缓存
	 * @author zkun
	 *
	 */
	 static class TriggerCache extends JDBCObjectCache<Schema,NewTrigger>{

		@Override
		protected JDBCStatement prepareObjectsStatement(JDBCSession session, Schema owner)
				throws SQLException {
			String orleFlag = owner.getRoleFlag();
			StringBuilder sqlBuilder = new StringBuilder();
			sqlBuilder.append("select st.db_id,st.schema_id,st.user_id, st.trig_name, st.trig_event,st.trig_type,st.trig_cond,st.Language,st.define,st.enable,st.valid,st.comments,so.obj_name,so.obj_type from ");
			sqlBuilder.append(orleFlag);
			sqlBuilder.append("_triggers st join ");
			sqlBuilder.append(orleFlag);
			sqlBuilder.append("_objects so");
			sqlBuilder.append(" on st.obj_id = so.obj_id and st.db_id = so.db_id where st.db_id= ");
			sqlBuilder.append(owner.getDbId(owner, session));
			sqlBuilder.append(" and st.schema_id=");
			sqlBuilder.append(owner.id);
			log.debug("" + OemConfig.OEM_NAME_EN + " triggers metadata: " + sqlBuilder.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sqlBuilder.toString());
			return dbStat;
		}

		@Override
		protected NewTrigger fetchObject(JDBCSession session, Schema owner, JDBCResultSet resultSet)
				throws SQLException, DBException {
			return new NewTrigger(session.getProgressMonitor(), session, owner, resultSet);
		}
	}
	 
//	 static class TriggerCache extends JDBCObjectCache<Schema,TriggerTest>{
//
//		@Override
//		protected JDBCStatement prepareObjectsStatement(JDBCSession session, Schema owner)
//				throws SQLException {
//			String orleFlag = owner.getRoleFlag();
//			StringBuilder sqlBuilder = new StringBuilder();
//			sqlBuilder.append("select st.db_id,st.schema_id,st.user_id, st.trig_name, st.trig_event,st.trig_type,st.trig_cond,st.Language,st.define,st.enable,st.valid,so.obj_name,so.obj_type from ");
//			sqlBuilder.append(orleFlag);
//			sqlBuilder.append("_triggers st join ");
//			sqlBuilder.append(orleFlag);
//			sqlBuilder.append("_objects so");
//			sqlBuilder.append(" on st.obj_id = so.obj_id and st.db_id = so.db_id where st.db_id= ");
//			sqlBuilder.append(owner.getDbId(owner, session));
//			sqlBuilder.append(" and st.schema_id=");
//			sqlBuilder.append(owner.id);
//			log.debug("" + OemConfig.COMPANY_NAME + " triggers metadata: " + sqlBuilder.toString());
//			JDBCPreparedStatement dbStat = session.prepareStatement(sqlBuilder.toString());
//			return dbStat;
//		}
//
//		@Override
//		protected TriggerTest fetchObject(JDBCSession session, Schema owner, JDBCResultSet resultSet)
//				throws SQLException, DBException {
//			return new TriggerTest(session.getProgressMonitor(), session, owner, resultSet);
//		}
//		
//	}
	
	
	

	/**
	 * 用户自定义数据类型缓存
	 */
	static class UdtCache extends JDBCObjectCache<Schema, Udt> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull Schema owner)
				throws SQLException {
			// xfc 修改了获取同义词信息的语句
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT T.*,S.SCHEMA_NAME FROM ");
			sql.append(roleFlag);
			sql.append("_TYPES T LEFT JOIN ");
			sql.append(roleFlag);
			sql.append("_SCHEMAS S ON T.SCHEMA_ID=S.SCHEMA_ID AND T.DB_ID = S.DB_ID ");
			sql.append("WHERE T.SCHEMA_ID=");
			sql.append(owner.id);
			sql.append(" AND T.DB_ID=");
			sql.append(owner.getDbId(owner, session));

			log.debug("" + OemConfig.OEM_NAME_EN + " udt metadata: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected Udt fetchObject(@NotNull JDBCSession session, @NotNull Schema owner, @NotNull JDBCResultSet resultSet)
				throws SQLException, DBException {
			return new Udt(owner, resultSet);
		}
	}

	/**
	 * 视图缓存
	 */
	public static class ViewCache extends JDBCStructLookupCache<Schema, View, TableColumn> {
		ViewCache() {
			super("VIEW_NAME");
			setListOrderComparator(DBUtils.nameComparator());
		}

		@Override
		public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull Schema owner, View object,
				String objectName) throws SQLException {
			// xfc 修改了获取所有视图信息的sql
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM ");
			sql.append(roleFlag);
			sql.append("_VIEWS WHERE DB_ID=");
			sql.append(owner.getDbId(owner, session));
			sql.append(" AND SCHEMA_ID=");
			sql.append(owner.getId());
			if (object != null) {
				sql.append(" AND VIEW_ID=");
				sql.append(object.getId());
			}

			log.debug("" + OemConfig.OEM_NAME_EN + " view metadata: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected View fetchObject(@NotNull JDBCSession session, @NotNull Schema owner, @NotNull JDBCResultSet dbResult)
				throws SQLException, DBException {
			return new View(session.getProgressMonitor(), session, owner, dbResult);
		}

		// 获取视图列信息
		@Override
		protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull Schema owner,
				@Nullable View forView) throws SQLException {
			// xfc 修改了获取列信息的sql
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder(500);
			sql.append("SELECT COL.*,VW.VIEW_NAME FROM ");
			sql.append(roleFlag);
			sql.append("_VIEW_COLUMNS COL ");
			sql.append(" LEFT JOIN ");
			sql.append(roleFlag);
			sql.append("_VIEWS VW ON VW.VIEW_ID=COL.VIEW_ID ");
			sql.append(" WHERE VW.DB_ID=");
			sql.append(owner.getDbId(owner, session));
			if (forView != null) {
				sql.append(" AND VW.VIEW_ID=");
				sql.append(forView.getId());
			}

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct select view columns sql: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected TableColumn fetchChild(@NotNull JDBCSession session, @NotNull Schema owner, @NotNull View view,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new TableColumn(session.getProgressMonitor(), view, dbResult);
//			return new ViewColumn(session.getProgressMonitor(), view, dbResult);
		}

		@Override
		protected void cacheChildren(View parent, List<TableColumn> tableColumns) {
			tableColumns.sort(DBUtils.orderComparator());
			super.cacheChildren(parent, tableColumns);
		}
	}

	/**
	 * 作业缓存
	 */
	static class SchedulerJobCache extends JDBCObjectCache<Schema, SchedulerJob> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull Schema owner)
				throws SQLException {
			// xfc 修改了获取所有job信息的sql语句
			String roleFlag = owner.getRoleFlag();
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM ");
			sql.append(roleFlag);
			sql.append("_JOBS WHERE DB_ID=");
			sql.append(owner.getDbId(owner, session));

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct select jobs sql: " + sql.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
			return dbStat;
		}

		@Override
		protected SchedulerJob fetchObject(@NotNull JDBCSession session, @NotNull Schema owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			return new SchedulerJob(session.getProgressMonitor(), session, owner, dbResult);
		}
	}
	

}
