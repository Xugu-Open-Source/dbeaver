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

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

public class TriggerTest extends BaseTrigger<Schema> implements DBSTrigger, DBPQualifiedObject, 
SourceObject {

	
	/**
	 * 库id
	 */
	private Integer dbId;
	
	/**
	 * 模式id
	 */
	private Integer schemaId;
	
	/**
	 * 用户id
	 */
	private Integer userId;
	
	/**
	 * 触发器名
	 */
	private String trigName;
	
	/**
	 * 触发器事件  1：插入，2：更新，4：删除
	 */
	private String triggeringEvent;
	
	/**
	 * 触发器类型  1：行级触发器（for each row）  2:语句触发器（for statement）
	 */
	private String triggerType;
	
	/**
	 * 触发条件
	 */
	private String trigCond;
	
	/**
	 * 动作类型
	 */
	private String actionType;
	
	/**
	 * 触发器定义
	 */
	private String define;
	
	/**
	 * 触发器是否启用
	 */
	private String isAble;
	
	/**
	 * vaild
	 */
	private String valid;
	
	/**
	 * 触发器作用对象名
	 */
	private String objectName;
	
	/**
	 * 触发器作用对象类型
	 */
	private String objectType;
	
	private  static final int NUM1 = 1; 
	private  static final int NUM2 = 2; 
	private  static final int NUM4 = 4; 
	private  static final int NUM5 = 5; 
	private  static final int NUM9 = 9; 

	
	
	
	public TriggerTest(Schema parent, String name) {
		super(parent, name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public DBSTable getTable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Schema getSchema() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public TriggerTest( DBRProgressMonitor monitor, JDBCSession session, Schema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "trig_name"));
		this.dbId = JDBCUtils.safeGetInt(dbResult, "DB_ID");
		this.schemaId = JDBCUtils.safeGetInt(dbResult, "SCHEMA_ID");
		this.userId = JDBCUtils.safeGetInt(dbResult, "USER_ID");			
		this.trigName = JDBCUtils.safeGetString(dbResult, "trig_name");
		Integer eventInteger = JDBCUtils.safeGetInt(dbResult, "trig_event");
		if(eventInteger == 1) {
			this.triggeringEvent =  "Insert";
		}else if(eventInteger == NUM4) {
			this.triggeringEvent =  "Delete";
		}else if(eventInteger == NUM2) {
			this.triggeringEvent = "Update";
		}else {
			this.triggeringEvent = "Unknown";
		}
		Integer typeInteger = JDBCUtils.safeGetInt(dbResult, "trig_type");
		if(typeInteger == NUM2) {
			this.triggerType = "for each row ";
		}else if(typeInteger == NUM1) {
			this.triggerType = " for statement";   
		}else {
			this.triggerType = "Unknown";
		}
		this.trigCond = JDBCUtils.safeGetString(dbResult, "trig_cond");
		this.actionType = JDBCUtils.safeGetString(dbResult, "language");
		this.define = JDBCUtils.safeGetString(dbResult, "define");
		Boolean isable = JDBCUtils.safeGetBoolean(dbResult, "enable");
		if(isable == true ) {
			this.isAble= "Enable";
		}else {
			this.isAble = "Disable";
		}
		Boolean isValid = JDBCUtils.safeGetBoolean(dbResult, "valid");
		if(isValid) {
			this.valid = "true";
		}else {
			this.valid = "false";
		}
		this.objectName = JDBCUtils.safeGetString(dbResult, "obj_name");
		Integer objType = JDBCUtils.safeGetInt(dbResult, "obj_Type");
		if(objType == NUM5) {
			this.objectType = "Table";
		}else if(objType == NUM9) {
			this.objectType = "View";
		}else {
			this.objectType = "Unkown";
		}

	}
	
	

public Integer getDbId() {
		return dbId;
	}

	public void setDbId(Integer dbId) {
		this.dbId = dbId;
	}

	public Integer getSchemaId() {
		return schemaId;
	}

	public void setSchemaId(Integer schemaId) {
		this.schemaId = schemaId;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	
	public String getTrigName() {
		return super.getName();
	}

	public void setTrigName(String trigName) {
		this.trigName = trigName;
	}
	
	@Property(viewable = true, order = 4)
	public String getTriggeringEvent() {
		return triggeringEvent;
	}

	public void setTrigEvent(String trigEvent) {
		 
		this.triggeringEvent = trigEvent;
	}
	
	@Property(viewable = true, order = 3)
	public String getTriggerType() {
		return triggerType;
	}

	public void setTrigType(String triggerType) {
		 this.triggerType = triggerType;
	}

	public String getTrigCond() {
		return trigCond;
	}

	public void setTrigCond(String trigCond) {
		this.trigCond = trigCond;
	}
	
	@Property(viewable = true, order = 12)
	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

//
//	public String getDefine() {
//		return define;
//	}
//
//	@Property(hidden = false, editable = true, updatable = true, order = -1)
//	public void setDefine(String define) {
//		this.define = define;
//	}

	@Property(editable = true, order = 10)
	public String getIsAble() {
		return isAble;
	}

	public void setIsAble(String isable) {
	 this.isAble = isable;
	}
	
	@Property(viewable = true, order = 11)
	public String getValid() {
		return valid;
	}


	public void setValid(String valid) {
		this.valid = valid;
	}
	@Property(viewable = true, order = 6)
	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}
	
	@Property(viewable = true, order = 7)
	public String getObjectType() {
		return objectType;
	}

	public void setObjType(String objectType) {
		this.objectType = objectType;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options)
			throws DBException {
	        return define;
	}
	
	
	@Override
	public void setObjectDefinitionText(String source) {
		// TODO Auto-generated method stub
		this.define = source;
		
	}

	@Override
	public DBSObjectState getObjectState() {
		return Boolean.valueOf(valid) ? DBSObjectState.ACTIVE : DBSObjectState.INVALID;
	}


	@Override
	public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
		this.valid = (Utils.getObjectStatus(monitor, this, ObjectType.TRIGGER) ?  "true"
				: "false");
		
	}


	@Override
	public SourceType getSourceType() {
		return SourceType.TRIGGER;
	}


	@Override
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		return new DBEPersistAction[] { new ObjectPersistAction(ObjectType.TRIGGER, "Compile trigger",
				"ALTER TRIGGER " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " RECOMPILE") };
	}
	
}
