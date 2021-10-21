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
import org.jkiss.dbeaver.ext.xugu.model.BaseTable.TriggerCache;
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 触发器衍生类，包含触发器具体信息
 */
public class Trigger extends BaseTrigger<Schema> {
	private Schema ownerSchema;
	private List<String> includeCols;
	private final TriggerCache triggerCache = new TriggerCache();
	
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
	private Integer trigEvent;
	/**
	 * 触发器类型  1：行级触发器（for each row）  2:语句触发器（for statement）
	 */
	private Integer trigType;
	/**
	 * 触发条件
	 */
	private String trigCond;
	/**
	 * 动作类型
	 */
	private String language;
	/**
	 * 触发器定义
	 */
	private String define;
	/**
	 * 触发器是否启用
	 */
	private Boolean isable;
	/**
	 * 触发器作用对象名
	 */
	private String objectName;
	/**
	 * 触发器作用对象类型
	 */
	private Integer objType;
	
	private BaseTable baseTable;
	 


	public Trigger(BaseTable table, String name) {
		super(table.getSchema(), name);
		this.baseTable = table;
		this.ownerSchema = table.getSchema();
		includeCols = new ArrayList<String>();
	}

	public Trigger(BaseTable table, ResultSet dbResult) {
		super(table, dbResult);
		this.baseTable = table;
		this.ownerSchema = table.getSchema();
		includeCols = new ArrayList<String>();
	}


	@Property(viewable = true, order = 3)
	public String getObjName() {
		return baseTable.getFullyQualifiedName(DBPEvaluationContext.DDL);
	}

	@Override
	public BaseTable getTable() {
		return baseTable;
	}

	@Override
	public Schema getSchema() {
		return this.ownerSchema;
	}

	@Association
	public Collection<TriggerColumn> getColumns(DBRProgressMonitor monitor) throws DBException {
		Collection<TriggerColumn> res = new ArrayList<>();
		Collection<TableColumn> tCols = baseTable.getAttributes(monitor);
		if (this.isPersisted() == false && this.includeCols != null) {
			if (this.includeCols.size() != 0) {
				Iterator<TableColumn> it = tCols.iterator();
				while (it.hasNext()) {
					TableColumn tempCol = it.next();
					tempCol.setPersisted(true);
					if (includeCols.contains(tempCol.getName())) {
						res.add(new TriggerColumn(tempCol.getName(), this, tempCol));
					}
				}
			} else {
				Iterator<TableColumn> it = tCols.iterator();
				while (it.hasNext()) {
					TableColumn tempCol = it.next();
					tempCol.setPersisted(true);
					res.add(new TriggerColumn(tempCol.getName(), this, tempCol));
				}
			}
			return res;
		} else {
			return baseTable.triggerCache.getChildren(monitor, baseTable, this);
		}
	}

	public List<String> getIncludeColumns() {
		return includeCols;
	}

	public void setIncludeColumns(List<String> cols) {
		this.includeCols = cols;
	}

	@Override
	public void setObjectDefinitionText(String source) {
		super.setObjectDefinitionText(source);
	}

	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		return defineBody;
	}

	public void setExtendedDefinitionText(String source) {
		this.defineBody = source;
	}
}
