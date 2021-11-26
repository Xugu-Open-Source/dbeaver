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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.edit.TablePartitionManager.WarningDialog;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.StandardErrorDialog;

import com.alibaba.druid.sql.dialect.xugu.api.XuguParserApi;
import com.alibaba.druid.sql.dialect.xugu.api.bean.CreateFunctionBean;
import com.alibaba.druid.sql.dialect.xugu.api.bean.CreateProcedureBean;
import com.alibaba.druid.sql.dialect.xugu.api.bean.Param;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 存储过程衍生类，包括存储过程名、定义、参数等具体信息
 */
public class ProcedureStandalone extends BaseProcedure<Schema> implements SourceObject, DBPRefreshableObject {
	private static final Log log = Log.getLog(ProcedureStandalone.class);
	private boolean valid;
	private String comment;
	private Timestamp createTime;
	private String sourceDeclaration;

	private String procedureName;
	private List<ProcedureParameter> procParams;

//	
//	
//	/**
//	 * 获取参数位置
//	 * @return
//	 */
//	public List getPositions() {
//		return positions;
//	}

	public ProcedureStandalone(DBRProgressMonitor monitor, Schema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "PROC_NAME"), JDBCUtils.safeGetLong(dbResult, "PROC_ID"),
				DBSProcedureType
						.valueOf(JDBCUtils.safeGetString(dbResult, "RET_TYPE") == null ? "PROCEDURE" : "FUNCTION"));
		this.procedureName = JDBCUtils.safeGetString(dbResult, "PROC_NAME");
		this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
		this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
		this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
		// 通过 define 字段手动解析参数列表（仅支持查看）
		this.sourceDeclaration = JDBCUtils.safeGetString(dbResult, "DEFINE");

		String paraName;
		String paraType;
		String dataType;
		Integer paraPosition;
		String paraDefault;
		ProcedureParameter procedureParameter;
		procParams = new ArrayList<ProcedureParameter>();
//		System.out.println(sourceDeclaration);
		CreateProcedureBean createProcedureBean = null;
		if (JDBCUtils.safeGetString(dbResult, "RET_TYPE") == null) {
			// 通过parser解析包解析存储过程参数。
			List<CreateProcedureBean> procedureBeans = null;
			try {
				procedureBeans = XuguParserApi.parseCreateProcedure(sourceDeclaration);
			} catch (Exception e) {
				procedureBeans = new  ArrayList<CreateProcedureBean>();
				CreateProcedureBean createProcedureBean2 = new CreateProcedureBean();
				Param param = new Param();
				param.setName("Procedure existing parser does not support syntax objects");
				param.setIndex(1);
				param.setDataType("VARCHAR");
			 
		
				List paramsList = new ArrayList<Param>();
				paramsList.add(param);
				createProcedureBean2.setParams(paramsList);
				createProcedureBean2.setParamSize(1);
				procedureBeans.add(createProcedureBean2);
				
//			       Runnable runnable = () -> {
//			            // Display the dialog
//			            StandardErrorDialog dialog = new StandardErrorDialog(UIUtils.getActiveWorkbenchShell(),
//			                    "parser error", e.getMessage(),  new Status(IStatus.ERROR, DBeaverCore.PLUGIN_ID,  e.getMessage()), IStatus.ERROR);
//			            dialog.open();
//			        };
//			        UIUtils.syncExec(runnable);
//				void showNotification(String id, String title, String text, DBPMessageType messageType, Runnable feedback) {
				
//				public static void showNotification(String id, String title, String text, DBPMessageType messageType, Runnable feedback) {
//			        notificationHandler.sendNotification(id, title, text, messageType, feedback);
//			    }

				
			    DBeaverNotifications.showNotification(
                        DBeaverNotifications.NT_RECONNECT,
                        procedureName,
                         e.getMessage(),
                        DBPMessageType.INFORMATION,new Runnable() {
			
							@Override
							public void run() {
								// TODO Auto-generated method stub
								
							}
						});
				
			}
			createProcedureBean = procedureBeans.get(0);
			for (int i = 0; i < createProcedureBean.getParamSize(); i++) {
				paraName = createProcedureBean.getParams().get(i).getName();
				paraType = createProcedureBean.getParams().get(i).getParamType();
				dataType = createProcedureBean.getParams().get(i).getDataType();
				paraPosition = Integer.valueOf(createProcedureBean.getParams().get(i).getIndex());
				paraDefault = createProcedureBean.getParams().get(i).getDefaultValue();
				Integer precision = createProcedureBean.getParams().get(i).getPrecision();
				Integer scale = createProcedureBean.getParams().get(i).getScale();
				if (paraDefault == null) {
					paraDefault = "";
				}
				procedureParameter = new ProcedureParameter(monitor, this, paraName, dataType, paraType,
						paraPosition, paraDefault, precision, scale);
				// monitor,procedure实例，参数名，数据类型，参数模式，参数位置，默认值。
				procParams.add(procedureParameter);
			}
		} else {
			// 通过parser解析包解析存储过程参数。
			List<CreateFunctionBean> functionBeans = null;
			try {
				functionBeans = XuguParserApi.parseCreateFunction(sourceDeclaration);
			} catch (Exception e) {
				functionBeans = new  ArrayList<CreateFunctionBean>();
				CreateFunctionBean createFunctionBean = new CreateFunctionBean();
				Param param = new Param();
				param.setName("Function existing parser does not support syntax objects");
				param.setIndex(1);
				param.setDataType("VARCHAR");
		 
				List paramsList = new ArrayList<Param>();
				paramsList.add(param);
				createFunctionBean.setParams(paramsList);
				createFunctionBean.setParamSize(1);
				functionBeans.add(createFunctionBean);
				
			    DBeaverNotifications.showNotification(
                        DBeaverNotifications.NT_RECONNECT,
                        procedureName,
                         e.getMessage(),
                        DBPMessageType.INFORMATION,new Runnable() {
			
							@Override
							public void run() {
								// TODO Auto-generated method stub
								
							}
						});
			}
			CreateFunctionBean	createFunctionBean = functionBeans.get(0);
			for (int i = 0; i < createFunctionBean.getParamSize(); i++) {
				paraName = createFunctionBean.getParams().get(i).getName();
				paraType = createFunctionBean.getParams().get(i).getParamType();
				dataType = createFunctionBean.getParams().get(i).getDataType();
				paraPosition = Integer.valueOf(createFunctionBean.getParams().get(i).getIndex());
				paraDefault = createFunctionBean.getParams().get(i).getDefaultValue();
				Integer precision = createFunctionBean.getParams().get(i).getPrecision();
				Integer scale = createFunctionBean.getParams().get(i).getScale();
				if (paraDefault == null) {
					paraDefault = "";
				}
				procedureParameter = new ProcedureParameter(monitor, this, paraName, dataType, paraType,
						paraPosition, paraDefault, precision, scale);
				// monitor,procedure实例，参数名，数据类型，参数模式，参数位置，默认值。
				procParams.add(procedureParameter);
			}
			
//			List<CreateFunctionBean> functionBeans = null;
//			try {
//				functionBeans = XuguParserApi.parseCreateFunction(sourceDeclaration);
//			}catch (Exception e) {
//				functionBeans = null;
//			}
//			if(functionBeans!=null) {
//				CreateFunctionBean createFunctionBean = functionBeans.get(0);
//				for (int i = 0; i < createFunctionBean.getParamSize(); i++) {
//					paraName = createFunctionBean.getParams().get(i).getName();
//					paraType = createFunctionBean.getParams().get(i).getParamType();
//					dataType = createFunctionBean.getParams().get(i).getDataType();
//					paraPosition = Integer.valueOf(createFunctionBean.getParams().get(i).getIndex());
//					paraDefault = createFunctionBean.getParams().get(i).getDefaultValue();
//					Integer precision = createFunctionBean.getParams().get(i).getPrecision();
//					Integer scale = createFunctionBean.getParams().get(i).getScale();
//					if (paraDefault == null) {
//						paraDefault = "";
//					}
//					procedureParameter = new ProcedureParameter(monitor, this, paraName, dataType, paraType, paraPosition,
//							paraDefault,precision,scale);
//					// monitor,procedure实例，参数名，数据类型，参数模式，参数位置，默认值。
//					procParams.add(procedureParameter);
//				}
//			}
		}

//		if(JDBCUtils.safeGetString(dbResult, "RET_TYPE")==null) {
//			//通过parser解析包解析存储过程参数。
//			XuguParserApi xuguParserApi = new XuguParserApi();
//			CreateProcedureBean createProcedureBean = xuguParserApi.parseCreateProcedure(sourceDeclaration);
//			for(int i = 0; i<createProcedureBean.getParamSize();i++) {
//				paraName = createProcedureBean.getParams().get(i).get(0);
//				paraType = createProcedureBean.getParams().get(i).get(2);
//				dataType = createProcedureBean.getParams().get(i).get(1);
//				paraPosition = Integer.valueOf(createProcedureBean.getParams().get(i).get(3));
//				paraDefault = createProcedureBean.getParams().get(i).get(4);
//				if(paraDefault == null) {
//					paraDefault = "";
//				}
//				procedureParameter =  new ProcedureParameter(monitor, this,paraName,paraType,dataType,paraPosition,paraDefault);
//				//monitor,procedure实例，参数名，数据类型，参数模式，参数位置，默认值。
//				procParams.add(procedureParameter);
//			}
//		}else {
//			XuguParserApi  xuguParserApi = new XuguParserApi();
//			CreateFunctionBean createFunctionBean = xuguParserApi.parseCreateFunction(sourceDeclaration);
//			for (int i = 0; i < createFunctionBean.getParamSize(); i++) {
//				 paraName = createFunctionBean.getParams().get(i).get(0);
//					paraType = createFunctionBean.getParams().get(i).get(2);
//					dataType = createFunctionBean.getParams().get(i).get(1);
//					paraPosition = Integer.valueOf(createFunctionBean.getParams().get(i).get(3));
//					paraDefault = createFunctionBean.getParams().get(i).get(4);
//					if(paraDefault == null) {
//						paraDefault = "";
//					}
//					procedureParameter =  new ProcedureParameter(monitor, this,paraName,paraType,dataType,paraPosition,paraDefault);
//					//monitor,procedure实例，参数名，数据类型，参数模式，参数位置，默认值。
//					procParams.add(procedureParameter);
//			}
//		}

//		if (this.sourceDeclaration != null) {
//			String reg = "";
//			final String keywordIs = "IS";
//			final String keywordAs = "AS";
//			if (this.sourceDeclaration.toUpperCase().indexOf(keywordIs) != -1) {
//				reg = keywordIs;
//			} else if (this.sourceDeclaration.toUpperCase().indexOf(keywordAs) != -1) {
//				reg = keywordAs;
//			} else {
//				reg = null;
//			}
//			if (reg != null) {
//				String param = getParamString(this.sourceDeclaration).trim();
//				if (!"".equals(param)) {
//					String[] params = param.split(",");
//					this.procParams = new ArrayList<ProcedureParameter>();
//					int position = 0;
//					for (int i = 0; i < params.length; i++) {
//						params[i] = params[i].trim();
//						
//						//解析参数模式
//						String mode = "IN";
//						int modeNum = 0;
//						Pattern pattern = Pattern.compile("\\s+IN\\s+OUT\\s+", Pattern.CASE_INSENSITIVE);
//						if (pattern.matcher(params[i]).find()) {
//							mode = "IN OUT";
//							modeNum = 2;
//						}
//						
//						pattern = Pattern.compile("\\s+OUT\\s+", Pattern.CASE_INSENSITIVE);
//						if (pattern.matcher(params[i]).find()) {
//							mode = "OUT";
//							modeNum = 1;
//						}
//
//						pattern = Pattern.compile("\\s+IN\\s+", Pattern.CASE_INSENSITIVE);
//						if (pattern.matcher(params[i]).find()) {
//							mode = "IN";
//							modeNum = 1;
//						}
//						
//						//获取除去参数名和参数模式的字符串
//						pattern = Pattern.compile("\\s+");
//						String[] items = pattern.split(params[i]);
//						String remainPart = "";
//						for (int index = 1 + modeNum; index < items.length; ++index) {
//							remainPart += items[index] + " ";
//						}
//						
//						//解析默认值
//						String defaultValue;
//						String dataType;
//						pattern = Pattern.compile("\\s+DEFAULT\\s+|\\s*:=\\s*", Pattern.CASE_INSENSITIVE);
//						String[] parts = pattern.split(remainPart);
//						if (parts.length > 1) {
//							defaultValue = parts[1];
//							dataType = parts[0];
//						} else {
//							defaultValue = "";
//							dataType = remainPart;
//						}
//
//			            //记录参数位置
//			        	++position;
//						procParams.add(new ProcedureParameter(monitor, this, items[0],
//								dataType.trim(), mode, position, defaultValue.trim()));
//					}
//				}
//			}
//		}

	}

	private String getParamString(String define) {
		int startIndex = -1;
		int endIndex = -1;
		int innerSingleLeftBracket = 0;
		int commentFlagCount = 0;
		char[] chars = define.toCharArray();
		for (int i = 0; i < chars.length; ++i) {
			// 判断是否是注释状态，当commentFlagCount为2时为注释状态
			// 当为注释状态时，若遇到换行，则清除注释状态
			if (commentFlagCount == 2) {
				if (chars[i] == '\n' || chars[i] == '\r') {
					commentFlagCount = 0;
					continue;
				}
				continue;
			} else {
				// 当不是注释状态时，若遇到注释符-，则注释符计数+1
				// 若不是注释符-，则重置注释符计数
				if (chars[i] == '-') {
					++commentFlagCount;
					continue;
				} else {
					commentFlagCount = 0;
				}

				// 如果是第一个左括号，则记录为参数串开始位置
				// 如果已记录开始位置，则为左内单括号，左内单括号计数+1
				if (startIndex == -1 && chars[i] == '(') {
					String remainString = define.substring(i);
					Pattern pattern = Pattern.compile("\\s(IS|AS)\\s", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(remainString);
					if (matcher.find()) {
						startIndex = i + 1;
						continue;
					} else {
						return "";
					}

				} else if (startIndex != -1 && chars[i] == '(') {
					++innerSingleLeftBracket;
					continue;
				}
				// 如果内部括号对为0，且为右括号，则记录为参数串结束位置
				// 如果左内单括号计数不为零，则当前右括号为左内单括号的配对，左内单括号-1
				if (innerSingleLeftBracket == 0 && chars[i] == ')') {

					endIndex = i;
					break;
				} else if (innerSingleLeftBracket != 0 && chars[i] == ')') {
					--innerSingleLeftBracket;
					continue;
				}
			}
		}

		if (startIndex == -1 || endIndex == -1) {
			return "";
		} else {
			return define.substring(startIndex, endIndex);
		}
	}

	public ProcedureStandalone(Schema schema, String name, DBSProcedureType procedureType) {
		super(schema, name, 0L, procedureType);
	}

	@Override
	public Collection<ProcedureParameter> getParameters(DBRProgressMonitor monitor) throws DBException {
		return this.procParams;
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
	public Schema getSchema() {
		return getParentObject();
	}

	@Override
	public SourceType getSourceType() {
		return getProcedureType() == DBSProcedureType.PROCEDURE ? SourceType.PROCEDURE : SourceType.FUNCTION;
	}

	@Override
	public Integer getOverloadNumber() {
		return null;
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
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
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		return new DBEPersistAction[] { new ObjectPersistAction(
				getProcedureType() == DBSProcedureType.PROCEDURE ? ObjectType.PROCEDURE : ObjectType.FUNCTION,
				"Compile procedure",
				"ALTER PROCEDURE " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " RECOMPILE") };
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = Utils.getObjectStatus(monitor, this,
				getProcedureType() == DBSProcedureType.PROCEDURE ? ObjectType.PROCEDURE : ObjectType.FUNCTION);
	}

	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		getSchema().proceduresCache.clearCache();
		return getSchema().proceduresCache.refreshObject(monitor, getSchema(), this);
	}
}
