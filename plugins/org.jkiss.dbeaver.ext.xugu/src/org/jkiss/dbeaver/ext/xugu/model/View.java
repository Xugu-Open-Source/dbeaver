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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ext.xugu.Utils;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

/**
 * 视图信息类
 * 
 * @author Xugu
 */
public class View extends BaseTable implements SourceObject {
	private String viewText;

	public View(Schema schema, String name) {
		super(schema, name, false);
	}

	public View(DBRProgressMonitor monitor, JDBCSession session, Schema schema, ResultSet dbResult) {
		super(schema, dbResult, ObjectType.VIEW);
		this.viewText = JDBCUtils.safeGetString(dbResult, "DEFINE");
	}

	@Override
	@Association
	public Collection<TableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
		return getContainer().viewCache.getChildren(monitor, getContainer(), this);
	}

	@Override
	public TableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
			throws DBException {
		return getContainer().viewCache.getChild(monitor, getContainer(), this, attributeName);
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = 15)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		return viewText;
	}

	@Override
	public SourceType getSourceType() {
		return SourceType.VIEW;
	}

	@Override
	public void setObjectDefinitionText(String source) {
		this.viewText = source;
	}

	@Override
	public boolean isView() {
		return true;
	}

	@Override
	protected String getTableTypeName() {
		return ObjectType.VIEW.getTypeName();
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = Utils.getObjectStatus(monitor, this, ObjectType.VIEW);
	}

	public String getViewText() {
		return viewText;
	}

	public void setViewText(String viewText) {
		this.viewText = viewText;
	}

	@Override
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		return new DBEPersistAction[] { new ObjectPersistAction(ObjectType.VIEW, "Compile view",
				"ALTER VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " RECOMPILE") };
	}
}
