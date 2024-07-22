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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cae.Utils;
import org.jkiss.dbeaver.ext.cae.model.Schema.UdtCache;
import org.jkiss.dbeaver.ext.cae.model.source.SourceObject;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;

import com.alibaba.druid.sql.dialect.xugu.api.XuguParserApi;
import com.alibaba.druid.sql.dialect.xugu.api.bean.CreateTypeBean;

/**
 * 自定义类型信息类
 */
public class Udt extends BaseSchemaObject
		implements SourceObject, DBPScriptObjectExt, DBSObjectContainer, DBSDataType, DBPRefreshableObject,DBSObject {

	private static final Log LOG = Log.getLog(Udt.class);
	private final UdtCache udtCache = new UdtCache();
	private int typeId;
	private String typeName;
	private String objectSchemaName;
	protected boolean valid;
	private String comment;
	private Timestamp createTime;
	private String typeHead;
	private String typeBody;
	Map<String,String> attributesHashMap;
	Map<String, String> methodsMap ;
	Boolean hasAttributes  ;
	Boolean hasMethods  ;
	
	

	public Udt(Schema schema, String name) {
		super(schema, name, false);
	}

	public Udt(Schema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "TYPE_NAME"), true);
		this.typeId = JDBCUtils.safeGetInt(dbResult, "TYPE_ID");
		this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
		this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
		this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
		this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
		this.objectSchemaName = JDBCUtils.safeGetString(dbResult, "SCHEMA_NAME");
		this.typeHead = JDBCUtils.safeGetString(dbResult, "SPEC");
		this.typeBody = JDBCUtils.safeGetString(dbResult, "BODY");
		String headString = JDBCUtils.safeGetString(dbResult, "SPEC");
		String bodyString = JDBCUtils.safeGetString(dbResult, "BODY");
		String createSql=null;
		if(headString!=null&&bodyString!=null) {
			  createSql = headString+bodyString;
		}
		if(headString!=null&& bodyString==null) {
			  createSql  = headString;
		}	
		CreateTypeBean  createTypeBean  = null;
		try {
			if(createSql!=null) {
				createTypeBean = XuguParserApi.parseCreateType(createSql);
				if(createTypeBean != null) {
					attributesHashMap =  createTypeBean.getAttributeMap();
					methodsMap = createTypeBean.getMethodMap();
				}
			}
		
		}catch (Exception e) {
			createTypeBean = new CreateTypeBean();
			this.attributesHashMap = new HashMap<String, String>();
			attributesHashMap.put("UDT existing parser does not support syntax objects", "UDT existing parser does not support syntax objects");
			createTypeBean.setAttributeMap(attributesHashMap);
			this.methodsMap = new HashMap<String, String>();
			methodsMap.put("UDT existing parser does not support syntax objects", "UDT existing parser does not support syntax objects");
			createTypeBean.setMethodMap(methodsMap);
		    DBeaverNotifications.showNotification(
                    DBeaverNotifications.NT_RECONNECT_FAILURE,
                    typeName,
                     e.getMessage(),
                    DBPMessageType.INFORMATION,new Runnable() {
		
						@Override
						public void run() {
							// TODO Auto-generated method stub
							
						}
					});
		 
		}
		if(attributesHashMap!=null) {
			this.hasAttributes = true;
		}else {
			this.hasAttributes = false;
		}
		if(hasMethods !=null) {
			this.hasMethods = true;
		}
	
	}

	public Collection<NewDataTypeAttribute> getAttributes(){
		List<NewDataTypeAttribute> dataTypeAttributes = new ArrayList<NewDataTypeAttribute>(); 
		if( this.getAttributesHashMap()  != null) { 
		for (Map.Entry<String, String> entry : this.getAttributesHashMap().entrySet()){ 
			       String name = entry.getKey();
			       String typeName = entry.getValue();
			       if(name == null) {
			    	   name = "";
			       }
			       if(typeName == null) {
			    	   typeName = "";
			       }
			       NewDataTypeAttribute newDataTypeAttribute = new NewDataTypeAttribute(name, typeName);
			       dataTypeAttributes.add(newDataTypeAttribute);
				}
				return dataTypeAttributes ;
		}else {
			return null;
		}	
	}
	
	
	public Collection<NewDataTypeMethod> getMethods(){
		List<NewDataTypeMethod> newDataTypeMethods = new ArrayList<NewDataTypeMethod>();
		if(this.getMethodsMap()  != null) {
			for (Map.Entry<String, String> entry : this.getMethodsMap().entrySet()){
			     String name = entry.getKey();
			       String methodType = entry.getValue();
			       if(name == null) {
			    	   name = "";
			       }
			       if(methodType == null) {
			    	   methodType= "";
			       }
			       NewDataTypeMethod newDataTypeMethod = new  NewDataTypeMethod(name, methodType);
			       newDataTypeMethods.add(newDataTypeMethod);		  
			}
			return newDataTypeMethods ;
		}else {
			return null;
		}
	
	}
	
	
	
	public Map<String, String> getAttributesHashMap() {
		return attributesHashMap;
	}

	public void setAttributesHashMap(Map<String, String> attributesHashMap) {
		this.attributesHashMap = attributesHashMap;
	}

	public Map<String, String> getMethodsMap() {
		return methodsMap;
	}

	public void setMethodsMap(Map<String, String> methodsMap) {
		this.methodsMap = methodsMap;
	}
	
	
	
    /**
     *  Use by tree navigator thru reflection
     * @return
     */
    public boolean hasMethods()
    {
        return hasMethods;
    }
    /**
     *  Use by tree navigator thru reflection
     * @return
     */
    public boolean hasAttributes()
    {
        return  hasAttributes;
    }
    
    
    
    


	@Property(viewable = true, editable = false, order = 1)
	public int getTypeId() {
		return typeId;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 2)
	public String getName() {
		return this.typeName;
	}

	@Property(viewable = true, editable = false, order = 3)
	public String getObjectSchemaName() {
		return objectSchemaName;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 18)
	public String getComment() {
		return comment;
	}

	@Property(viewable = true, editable = false, order = 5)
	public Timestamp getCreateTime() {
		return createTime;
	}

	@Property(viewable = true, editable = false, order = 6)
	public boolean isValid() {
		return valid;
	}

	public Object getObjectOwner() {
		final Schema schema = getDataSource().schemaCache.getCachedObject(objectSchemaName);
		return schema == null ? objectSchemaName : schema;
	}

	@Override
	public void setName(String name) {
		this.name = name;
		this.typeName = name;
	}

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public void setObjectSchemaName(String objectSchemaName) {
		this.objectSchemaName = objectSchemaName;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	@Override
	public SourceType getSourceType() {
		return SourceType.TYPE;
	}

	public Object getObject(DBRProgressMonitor monitor) throws DBException {
		return ObjectType.resolveObject(monitor, getDataSource(), null, "UDT", objectSchemaName, typeName);
	}

	@Override
	public String getTypeName() {
		if (name != null) {
			final String sepearator = ".";
			if (this.name.indexOf(sepearator) != -1) {
				return this.name.split(sepearator)[1];
			} else {
				return this.name;
			}
		} else {
			return this.name;
		}
	}

	@Override
	public String getFullTypeName() {
		return typeName;
	}

	@Override
	public int getTypeID() {
		return typeId;
	}

	@Override
	public DBPDataKind getDataKind() {
		// TODO Auto-generated method stub
		return DBPDataKind.OBJECT;
	}

	@Override
	public Integer getScale() {
		return -1;
	}

	@Override
	public Integer getPrecision() {
		return -1;
	}

	@Override
	public long getMaxLength() {
		return -1;
	}

	@Override
	public Object geTypeExtension() {
		return null;
	}

	@Override
	public DBSDataType getComponentType(DBRProgressMonitor monitor) throws DBException {
		return null;
	}

	@Override
	public int getMinScale() {
		return -1;
	}

	@Override
	public int getMaxScale() {
		return -1;
	}

	@Override
	public DBCLogicalOperator[] getSupportedOperators(DBSTypedObject attribute) {
		return null;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = 15)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		return typeHead;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = 16)
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		return typeBody;
	}

	@Override
	public void setObjectDefinitionText(String typeHead) {
		this.typeHead = typeHead;
	}

	public void setExtendedDefinitionText(String typeBody) {
		this.typeBody = typeBody;
	}

	@Override
	public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
		return udtCache.getAllObjects(monitor, this.parent);
	}

	@Override
	public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
		return udtCache.getObject(monitor, this.parent, childName);
	}

	@Override
	public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
		return DataType.class;
	}

	@Override
	public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
		udtCache.getAllObjects(monitor, this.getSchema());
	}

	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
		this.valid = Utils.getObjectStatus(monitor, this, ObjectType.UDT);
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		this.udtCache.clearCache();
		this.udtCache.refreshObject(monitor, parent, this);
		Schema schema = this.getSchema();
		schema.udtCache.clearCache();
		return schema.udtCache.refreshObject(monitor, schema, this);
	}

	@Override
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		List<DBEPersistAction> actions = new ArrayList<>();
		actions.add(new ObjectPersistAction(ObjectType.UDT, "Compile UDT",
				"ALTER TYPE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " RECOMPILE"));
		return actions.toArray(new DBEPersistAction[actions.size()]);
	}

	@Override
	public long getTypeModifiers() {
		return 0;
	}
}
