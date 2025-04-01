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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cae.model.BaseTable.TriggerCache;
import org.jkiss.dbeaver.ext.cae.model.source.SourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 触发器衍生类，包含触发器具体信息
 */
public class Trigger extends BaseTrigger<Schema> implements DBPRefreshableObject {
	private Schema ownerSchema;
	private List<String> includeCols;
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
	
	public Trigger(Table table, Trigger source) throws DBException {
		super(table.getSchema(), source.getName());
		this.baseTable = table;
		this.ownerSchema = table.getSchema();
		includeCols = new ArrayList<String>();
		this.setObjectType(source.getObjectType());
		this.setTriggerType(source.getTriggerType());
		this.setTriggeringEvent(source.getTriggeringEvent());
		this.setTriggerTime(source.getTriggerTime());
		this.setTriggerCondition(source.getTriggerCondition());
		this.setObjectDefinitionText(source.getObjectDefinitionText(new VoidProgressMonitor(), null));
		this.setExtendedDefinitionText(source.getExtendedDefinitionText(new VoidProgressMonitor()));
		this.setComment(source.getDescription());
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
		Collection<? extends DBSEntityAttribute> tCols = baseTable.getAttributes(monitor);
		if (this.isPersisted() == false && this.includeCols != null) {
			if (this.includeCols.size() != 0) {
				Iterator<? extends DBSEntityAttribute> it = tCols.iterator();
				while (it.hasNext()) {
					TableColumn tempCol = (TableColumn) it.next();
					tempCol.setPersisted(true);
					if (includeCols.contains(tempCol.getName())) {
						res.add(new TriggerColumn(tempCol.getName(), this, tempCol));
					}
				}
			} else {
				Iterator<? extends DBSEntityAttribute> it = tCols.iterator();
				while (it.hasNext()) {
					TableColumn tempCol = (TableColumn) it.next();
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

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		Schema schema = this.getSchema();
		schema.triggerCache.clearCache();
		return schema.triggerCache.refreshObject(monitor, schema, this);
	}
}
