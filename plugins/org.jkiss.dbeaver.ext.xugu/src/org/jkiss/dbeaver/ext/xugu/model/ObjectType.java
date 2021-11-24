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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.HashMap;
import java.util.Map;

/**
 * 对象类型
 */
public enum ObjectType implements DBSObjectType {
	/**
	 * 对象类型枚举
	 */
	UDT("UDT", null, Udt.class, null),
	TABLESPACE("TABLESPACE", null, Tablespace.class, null),
	ROLE("ROLE", null, Role.class, null),
	CLUSTER("CLUSTER", null, DBSObject.class, null),
	CONSTRAINT("CONSTRAINT", DBIcon.TREE_CONSTRAINT, TableConstraint.class, null),
	CONSUMER_GROUP("CONSUMER GROUP", null, DBSObject.class, null),
	CONTEXT("CONTEXT", null, DBSObject.class, null),
	DIRECTORY("DIRECTORY", null, DBSObject.class, null),
	EVALUATION_CONTEXT("EVALUATION CONTEXT", null, DBSObject.class, null),
	FOREIGN_KEY("FOREIGN KEY", DBIcon.TREE_FOREIGN_KEY, TableForeignKey.class, null),
	FUNCTION("FUNCTION", DBIcon.TREE_PROCEDURE, ProcedureStandalone.class, new ObjectFinder() {
		@Override
		public ProcedureStandalone findObject(DBRProgressMonitor monitor, Schema schema, String objectName)
				throws DBException {
			return schema.proceduresCache.getObject(monitor, schema, objectName);
		}
	}),
	INDEX("INDEX", DBIcon.TREE_INDEX, TableIndex.class, new ObjectFinder() {
		@Override
		public TableIndex findObject(DBRProgressMonitor monitor, Schema schema, String objectName) throws DBException {
			return schema.indexCache.getObject(monitor, schema, objectName);
		}
	}),
	INDEX_PARTITION("INDEX PARTITION", null, DBSObject.class, null),
	INDEXTYPE("INDEXTYPE", null, DBSObject.class, null),
	JAVA_DATA("JAVA DATA", null, DBSObject.class, null),
	JAVA_RESOURCE("JAVA RESOURCE", null, DBSObject.class, null),
	JOB("JOB", null, DBSObject.class, null),
	JOB_CLASS("JOB CLASS", null, DBSObject.class, null),
	LIBRARY("LIBRARY", null, DBSObject.class, null),
	LOB("CONTENT", null, DBSObject.class, null),
	MATERIALIZED_VIEW("MATERIALIZED VIEW", null, DBSObject.class, null),
	OPERATOR("OPERATOR", null, DBSObject.class, null),
	PACKAGE("PACKAGE", DBIcon.TREE_PACKAGE, Package.class, new ObjectFinder() {
		@Override
		public Package findObject(DBRProgressMonitor monitor, Schema schema, String objectName) throws DBException {
			return schema.packageCache.getObject(monitor, schema, objectName);
		}
	}),
	// package body当作package处理
	PACKAGE_BODY("PACKAGE BODY", DBIcon.TREE_PACKAGE, Package.class, new ObjectFinder() {
		@Override
		public Package findObject(DBRProgressMonitor monitor, Schema schema, String objectName) throws DBException {
			return schema.packageCache.getObject(monitor, schema, objectName);
		}
	}),
	// procedure的obj_type为7
	PROCEDURE("PROCEDURE", DBIcon.TREE_PROCEDURE, ProcedureStandalone.class, new ObjectFinder() {
		@Override
		public ProcedureStandalone findObject(DBRProgressMonitor monitor, Schema schema, String objectName)
				throws DBException {
			return schema.proceduresCache.getObject(monitor, schema, objectName);
		}
	}),
	PROGRAM("PROGRAM", null, DBSObject.class, null),
	RULE("RULE", null, DBSObject.class, null),
	RULE_SET("RULE SET", null, DBSObject.class, null),
	SCHEDULE("SCHEDULE", null, DBSObject.class, null),
	// sequence的obj_type为8
	SEQUENCE("SEQUENCE", DBIcon.TREE_SEQUENCE, Sequence.class, new ObjectFinder() {
		@Override
		public Sequence findObject(DBRProgressMonitor monitor, Schema schema, String objectName) throws DBException {
			return schema.sequenceCache.getObject(monitor, schema, objectName);
		}
	}),
	SYNONYM("SYNONYM", DBIcon.TREE_SYNONYM, Synonym.class, new ObjectFinder() {
		@Override
		public Synonym findObject(DBRProgressMonitor monitor, Schema schema, String objectName) throws DBException {
			return schema.synonymCache.getObject(monitor, schema, objectName);
		}
	}),
	// table的OBJ_TYPE为5
	TABLE("TABLE", DBIcon.TREE_TABLE, Table.class, new ObjectFinder() {
		@Override
		public BaseTable findObject(DBRProgressMonitor monitor, Schema schema, String objectName) throws DBException {
			return schema.tableCache.getObject(monitor, schema, objectName);
		}
	}),
	TABLE_PARTITION("TABLE PARTITION", null, DBSObject.class, null),
	// trigger的OBJ_TYPE为11
	TRIGGER("TRIGGER", DBIcon.TREE_TRIGGER, Trigger.class, null),
	// view的OBJ_TYPE为9
	VIEW("VIEW", DBIcon.TREE_VIEW, View.class, new ObjectFinder() {
		@Override
		public View findObject(DBRProgressMonitor monitor, Schema schema, String objectName) throws DBException {
			return schema.tableCache.getObject(monitor, schema, objectName, View.class);
		}
	}),
	WINDOW("WINDOW", null, DBSObject.class, null),
	WINDOW_GROUP("WINDOW GROUP", null, DBSObject.class, null),
	XML_SCHEMA("XML SCHEMA", null, DBSObject.class, null);

	private static final Log log = Log.getLog(ObjectType.class);

	private static Map<String, ObjectType> typeMap = new HashMap<>();

	static {
		for (ObjectType type : values()) {
			typeMap.put(type.getTypeName(), type);
		}
	}

	public static ObjectType getByType(String typeName) {
		return typeMap.get(typeName);
	}

	/**
	 * 对象查找器
	 */
	private static interface ObjectFinder {
		/**
		 * 查找对象
		 * 
		 * @param monitor 进程监视器
		 * @param schema 模式
		 * @param objectName 对象名
		 * @return 查找到的对象
		 * @throws DBException 数据库异常
		 */
		DBSObject findObject(DBRProgressMonitor monitor, Schema schema, String objectName) throws DBException;
	}

	private final String objectType;
	private final DBPImage image;
	private final Class<? extends DBSObject> typeClass;
	private final ObjectFinder finder;

	<OBJECT_TYPE extends DBSObject> ObjectType(String objectType, DBPImage image, Class<OBJECT_TYPE> typeClass,
			ObjectFinder finder) {
		this.objectType = objectType;
		this.image = image;
		this.typeClass = typeClass;
		this.finder = finder;
	}

	public boolean isBrowsable() {
		return finder != null;
	}

	@Override
	public String getTypeName() {
		return objectType;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public DBPImage getImage() {
		return image;
	}

	@Override
	public Class<? extends DBSObject> getTypeClass() {
		return typeClass;
	}

	public DBSObject findObject(DBRProgressMonitor monitor, Schema schema, String objectName) throws DBException {
		if (finder != null) {
			return finder.findObject(monitor, schema, objectName);
		} else {
			return null;
		}
	}

	public static Object resolveObject(DBRProgressMonitor monitor, DataSource dataSource, String dbLink,
			String objectTypeName, String objectOwner, String objectName) throws DBException {
		if (dbLink != null) {
			return objectName;
		}
		ObjectType objectType = ObjectType.getByType(objectTypeName);
		if (objectType == null) {
			log.debug("Unrecognized object type: " + objectTypeName);
			return objectName;
		}
		if (!objectType.isBrowsable()) {
			log.debug("Unsupported object type: " + objectTypeName);
			return objectName;
		}
		final Schema schema = dataSource.getSchema(monitor, objectOwner);
		if (schema == null) {
			log.debug("Schema '" + objectOwner + "' not found");
			return objectName;
		}
		final DBSObject object = objectType.findObject(monitor, schema, objectName);
		if (object == null) {
			log.debug(objectTypeName + " '" + objectName + "' not found in '" + schema.getName() + "'");
			return objectName;
		}
		return object;
	}

	@Override
	public String toString() {
		return objectType;
	}
}
