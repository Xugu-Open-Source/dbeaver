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
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.edit.TablePartitionManager;
import org.jkiss.dbeaver.ext.xugu.edit.TablePartitionManager.WarningDialog;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import com.alibaba.druid.sql.dialect.xugu.api.XuguParserApi;
import com.alibaba.druid.sql.dialect.xugu.api.bean.CreateFunctionBean;
import com.alibaba.druid.sql.dialect.xugu.api.bean.CreatePackageBean;
import com.alibaba.druid.sql.dialect.xugu.api.bean.CreateProcedureBean;
import com.alibaba.druid.sql.dialect.xugu.api.exception.ParserBusinessException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * 基本模式对象
 */
public class Package extends BaseSchemaObject implements SourceObject, DBPScriptObjectExt, DBSObjectContainer,
		DBSPackage, DBPRefreshableObject, DBSProcedureContainer  {
	private final ProceduresCache proceduresCache = new ProceduresCache();
	private Boolean valid;
	private String comment;
	private Timestamp createTime;
	private String sourceDeclaration;
	private String sourceDefinition;
	private String packageName;
	private XuguParserApi xuguParserApi;
	private CreatePackageBean createPackageBean;
	private List<CreateProcedureBean> procedureBeans;
	private List<CreateFunctionBean> functionBeans;
	private List<NewProcedureParameter> parametersList;
	private final  Schema schema;
	
	public Package(Schema schema, String name) {
		super(schema, name, false);
		this.schema = schema;
	}

	public Package(Schema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "PACK_NAME"), true);
		this.schema = schema;
		this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
		this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
		this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
		this.sourceDeclaration = JDBCUtils.safeGetString(dbResult, "SPEC");
		this.sourceDefinition = JDBCUtils.safeGetString(dbResult, "BODY");
		this.packageName =  JDBCUtils.safeGetString(dbResult, "PACK_NAME");
		parseProduceAndFuntionBean();

		if (this.sourceDeclaration == null) {
			this.sourceDeclaration = "--Null Package header";
		}
		if (this.sourceDefinition == null) {
			this.sourceDefinition = "-- Null Package header";
		}
	}
	
	private void parseProduceAndFuntionBean() {
		try {
			this.createPackageBean = XuguParserApi.parseCreatePackage(this.sourceDeclaration);
			this.procedureBeans = createPackageBean.getCreateProcedureBeans();
			this.functionBeans = createPackageBean.getCreateFunctionBeans();
			parametersList = new ArrayList<NewProcedureParameter>();
			for (int i = 0; i < procedureBeans.size(); i++) {
				CreateProcedureBean createProcedureBean = procedureBeans.get(i);
				String procedureName = createProcedureBean.getProcedureName();
				NewProcedurePackaged newProcedurePackaged = null;
				for (int j = 0; j < createProcedureBean.getParamSize(); j++) {
					String paraName = createProcedureBean.getParams().get(j).getName();
					String paraType = createProcedureBean.getParams().get(j).getParamType();
					String dataType = createProcedureBean.getParams().get(j).getDataType();
					Integer paraPosition = Integer.valueOf(createProcedureBean.getParams().get(j).getIndex());
					String paraDefault = createProcedureBean.getParams().get(j).getDefaultValue();
					Integer precision = createProcedureBean.getParams().get(j).getPrecision();
					Integer scale = createProcedureBean.getParams().get(j).getScale();
					if (paraDefault == null) {
						paraDefault = "";
					}
					newProcedurePackaged = new NewProcedurePackaged(this, procedureName);
					NewProcedureParameter newProcedureParameter = new NewProcedureParameter(newProcedurePackaged,
							paraName, dataType, paraType, paraPosition, paraDefault, precision, scale);
					parametersList.add(newProcedureParameter);
				}
			}
			for (int i = 0; i < functionBeans.size(); i++) {
				CreateFunctionBean createFunctionBean = functionBeans.get(i);
				String functionName = createFunctionBean.getFunctionName();
				NewProcedurePackaged newProcedurePackaged = null;
				for (int j = 0; j < createFunctionBean.getParamSize(); j++) {
					String paraName = createFunctionBean.getParams().get(j).getName();
					String paraType = createFunctionBean.getParams().get(j).getParamType();
					String dataType = createFunctionBean.getParams().get(j).getDataType();
					Integer paraPosition = Integer.valueOf(createFunctionBean.getParams().get(j).getIndex());
					String paraDefault = createFunctionBean.getParams().get(j).getDefaultValue();
					Integer precision = createFunctionBean.getParams().get(j).getPrecision();
					Integer scale = createFunctionBean.getParams().get(j).getScale();
					if (paraDefault == null) {
						paraDefault = "";
					}
					newProcedurePackaged = new NewProcedurePackaged(this, functionName, parametersList);
					NewProcedureParameter newProcedureParameter = new NewProcedureParameter(newProcedurePackaged,
							paraName, dataType, paraType, paraPosition, paraDefault, precision, scale);
					parametersList.add(newProcedureParameter);
					newProcedurePackaged.setProcParams(parametersList);
				}
			}
		} catch (ParserBusinessException e) {
			this.createPackageBean = new CreatePackageBean();
			this.createPackageBean.setCreateFunctionBeans(new ArrayList<>());
			this.createPackageBean.setCreateProcedureBeans(new ArrayList<>());

			this.procedureBeans = new ArrayList<CreateProcedureBean>();
			CreateProcedureBean createProcedureBean = new CreateProcedureBean();
			createProcedureBean.setProcedureName("Package existing parser does not support syntax objects");
			createProcedureBean.setParamSize(0);
	        this.procedureBeans.add(createProcedureBean);
	        
	    	this.functionBeans = new ArrayList<CreateFunctionBean>();
			CreateFunctionBean createFunctionBean = new CreateFunctionBean();
			createFunctionBean.setFunctionName("Package existing parser does not support syntax objects");
			createFunctionBean.setParamSize(0);
	        this.functionBeans.add(createFunctionBean);

		    DBeaverNotifications.showNotification(
                    DBeaverNotifications.NT_RECONNECT_FAILURE,
                    packageName,
                     e.getMessage(),
                    DBPMessageType.INFORMATION, ()->{});
		}
	}
	
	
	public  DataSource getDataSource() {
		
		return (DataSource) schema.getDataSource() ;
	}
	
 
	public List<NewProcedureParameter> getParameters(){
		return this.parametersList;
	}

	@Override
	@Property(viewable = true, editable = false, updatable = false, order = 1)
	public String getName() {
		return this.name;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 2)
	public String getComment() {
		return comment;
	}

	@Property(viewable = true, editable = false, updatable = false, order = 3)
	public Timestamp getCreateTime() {
		return createTime;
	}

	@Property(viewable = true, order = 4)
	public boolean isValid() {
		return valid;
	}

	public CreatePackageBean getCreatePackageBean() {
		return createPackageBean;
	}

	public void setCreatePackageBean(CreatePackageBean createPackageBean) {
		this.createPackageBean = createPackageBean;
	}

	public List<CreateProcedureBean> getProcedureBeans() {
		return procedureBeans;
	}

	public void setProcedureBeans(List<CreateProcedureBean> procedureBeans) {
		this.procedureBeans = procedureBeans;
	}

	public List<CreateFunctionBean> getFunctionBeans() {
		return functionBeans;
	}

	public void setFunctionBeans(List<CreateFunctionBean> functionBeans) {
		this.functionBeans = functionBeans;
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

	@Override
	public SourceType getSourceType() {
		return SourceType.PACKAGE;
	}

	public String getSourceDeclaration() {
		return sourceDeclaration;
	}

	public void setSourceDeclaration(String sourceDeclaration) {
		this.sourceDeclaration = sourceDeclaration;
	}

	public String getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(String sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}

	public ProceduresCache getProceduresCache() {
		return proceduresCache;
	}

	public Boolean getValid() {
		return valid;
	}

	public XuguParserApi getXuguParserApi() {
		return xuguParserApi;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException {
		return sourceDeclaration;
	}

	@Override
	public void setObjectDefinitionText(String sourceDeclaration) {
		this.sourceDeclaration = sourceDeclaration;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		return sourceDefinition;
	}

	public void setExtendedDefinitionText(String source) {
		this.sourceDefinition = source;
	}

	@Override
	@Association
	public Collection<NewProcedurePackaged> getNewProcedures(DBRProgressMonitor monitor) throws DBException {
		
		List<CreateProcedureBean> createProcedureBeans = this.getProcedureBeans();
		List<NewProcedurePackaged> proceduresList  = new ArrayList<NewProcedurePackaged>();
		
		if(createProcedureBeans==null) {
			return proceduresList;
		}
		for(int i =0;i<createProcedureBeans.size();i++) {
			CreateProcedureBean createProcedureBean = createProcedureBeans.get(i);
			String procedureName  = createProcedureBean.getProcedureName();
			List<NewProcedureParameter> parametersList = new ArrayList<NewProcedureParameter>();
			NewProcedurePackaged newProcedurePackaged = new NewProcedurePackaged(this,procedureName,parametersList); 
			for(int j = 0; j<createProcedureBean.getParamSize();j++) {
				String  paraName = createProcedureBean.getParams().get(j).getName();
				String	paraType = createProcedureBean.getParams().get(j).getParamType();
				String  dataType = createProcedureBean.getParams().get(j).getDataType();
				Integer paraPosition = Integer.valueOf(createProcedureBean.getParams().get(j).getIndex());
				String  paraDefault = createProcedureBean.getParams().get(j).getDefaultValue();
				Integer precision = createProcedureBean.getParams().get(j).getPrecision();
				Integer scale = createProcedureBean.getParams().get(j).getScale();
				if(paraDefault == null) {
					paraDefault = "";
				}
				NewProcedureParameter	newProcedureParameter =  new NewProcedureParameter(newProcedurePackaged,monitor,paraName,dataType,paraType,paraPosition,paraDefault,precision,scale);
				parametersList.add(newProcedureParameter);
			}
			newProcedurePackaged.setProcParams(parametersList);
			proceduresList.add(newProcedurePackaged);
		}
		return proceduresList;
	}
	
	
	@Override
	@Association
	public Collection<NewProcedurePackaged> getNewFunctions(DBRProgressMonitor monitor) throws DBException {
		List<CreateFunctionBean> createFunctionBeans = this.getFunctionBeans();
		List<NewProcedurePackaged> functionsList  = new ArrayList<NewProcedurePackaged>();
		if(createFunctionBeans==null) {
			return functionsList;
		}
		for(int i =0;i<createFunctionBeans.size();i++) {
			CreateFunctionBean createFunctionBean = createFunctionBeans.get(i);
			String functionName  = createFunctionBean.getFunctionName();
			List<NewProcedureParameter> parametersList = new ArrayList<NewProcedureParameter>();
			NewProcedurePackaged newProcedurePackaged = new NewProcedurePackaged(this,functionName,parametersList);
			for(int j = 0; j<createFunctionBean.getParamSize();j++) {
				String  paraName = createFunctionBean.getParams().get(j).getName();
				String	paraType = createFunctionBean.getParams().get(j).getParamType();
				String  dataType = createFunctionBean.getParams().get(j).getDataType();
				Integer paraPosition = Integer.valueOf(createFunctionBean.getParams().get(j).getIndex());
				String  paraDefault = createFunctionBean.getParams().get(j).getDefaultValue();
				Integer precision = createFunctionBean.getParams().get(j).getPrecision();
				Integer scale = createFunctionBean.getParams().get(j).getScale();
				if(paraDefault == null) {
					paraDefault = "";
				}
			
				NewProcedureParameter	newProcedureParameter =  new NewProcedureParameter(newProcedurePackaged,monitor,paraName,dataType,paraType,paraPosition,paraDefault,precision,scale);
				parametersList.add(newProcedureParameter);
			}
			newProcedurePackaged.setProcParams(parametersList);
			functionsList.add(newProcedurePackaged); 
		}
		return functionsList;
	}
	

	@Override
	public ProcedurePackaged getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
		return proceduresCache.getObject(monitor, this, uniqueName);
	}

	@Override
	public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
		return proceduresCache.getAllObjects(monitor, this);
	}

	@Override
	public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
		return proceduresCache.getObject(monitor, this, childName);
	}

	@Override
	public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
		return ProcedurePackaged.class;
	}

	@Override
	public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
		proceduresCache.getAllObjects(monitor, this);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		this.proceduresCache.clearCache();
		this.proceduresCache.getAllObjects(monitor, this);
		parseProduceAndFuntionBean();

		Schema schema = this.getSchema();
		schema.packageCache.clearCache();
		return schema.packageCache.refreshObject(monitor, schema, this);
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = Utils.getObjectStatus(monitor, this, ObjectType.PACKAGE);
	}

	@Override
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		List<DBEPersistAction> actions = new ArrayList<>();
		actions.add(new ObjectPersistAction(ObjectType.PACKAGE, "Compile package",
				"ALTER PACKAGE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " RECOMPILE"));
		return actions.toArray(new DBEPersistAction[actions.size()]);
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	static class ProceduresCache extends JDBCObjectCache<Package, ProcedurePackaged> {

		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull Package owner)
				throws SQLException {
			final String roleFlag = ((DataSource)session.getDataSource()).getRoleFlag();
			JDBCPreparedStatement dbStat = session.prepareStatement("SELECT * FROM " + roleFlag + "_PACKAGES WHERE SCHEMA_ID=? AND PACK_NAME=? ");
			
			dbStat.setInt(1, Integer.parseInt(String.valueOf(owner.getSchema().getId())));
			dbStat.setString(2, owner.getName());
			return dbStat;
		}

		@Override
		protected ProcedurePackaged fetchObject(@NotNull JDBCSession session, @NotNull Package owner,
				@NotNull JDBCResultSet dbResult) throws SQLException, DBException {
			
			return new ProcedurePackaged(owner, dbResult);
		}

		@Override
		protected void invalidateObjects(DBRProgressMonitor monitor, Package owner,
				Iterator<ProcedurePackaged> objectIter) {
			Map<String, ProcedurePackaged> overloads = new HashMap<>(16);
			while (objectIter.hasNext()) {
				final ProcedurePackaged proc = objectIter.next();
				if (CommonUtils.isEmpty(proc.getName())) {
					objectIter.remove();
					continue;
				}
				final ProcedurePackaged overload = overloads.get(proc.getName());
				if (overload == null) {
					overloads.put(proc.getName(), proc);
				} else {
					if (overload.getOverloadNumber() == null) {
						overload.setOverload(1);
					}
					proc.setOverload(overload.getOverloadNumber() + 1);
					overloads.put(proc.getName(), proc);
				}
			}
		}
	}

	@Override
	public Collection<? extends DBSProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}
}
