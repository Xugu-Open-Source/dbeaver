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
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;

import java.util.ArrayList;
import java.util.List;

/**
 * 约束信息基类
 */
public abstract class BaseTableConstraint extends JDBCTableConstraint<BaseTable,TableConstraintColumn> {
	private ObjectStatus status;
	private List<TableConstraintColumn> columns = new ArrayList<>();

	public BaseTableConstraint(BaseTable table, String name, DBSEntityConstraintType constraintType,
			ObjectStatus status, boolean persisted) {
		super(table, name, null, constraintType, persisted);
		this.status = status;
	}

	protected BaseTableConstraint(BaseTable tableBase, String name, String description,
			DBSEntityConstraintType constraintType, boolean persisted) {
		super(tableBase, name, description, constraintType, persisted);
	}
	
	public BaseTableConstraint(BaseTable table, BaseTableConstraint source) {
		super(table, source.getName(), source.getDescription(), source.getConstraintType(), false);
	}

	@NotNull
	@Override
	public DataSource getDataSource() {
		return getTable().getDataSource();
	}

	@NotNull
	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
	@Override
	public DBSEntityConstraintType getConstraintType() {
		return constraintType;
	}

	@Property(viewable = false, editable = false, order = 7)
	public ObjectStatus getStatus() {
		return status;
	}

	@Override
	public List<TableConstraintColumn> getAttributeReferences(DBRProgressMonitor monitor) {
		return columns;
	}

	public List<TableConstraintColumn> getColumns() {
		return columns;
	}

	public void addColumn(TableConstraintColumn column) {
		if (columns == null) {
			columns = new ArrayList<>();
		}
		this.columns.add(column);
	}

	void setColumns(List<TableConstraintColumn> columns) {
		this.columns = columns;
	}

	public void setAttributeReferences(List<TableConstraintColumn> columns) {
		this.columns.clear();
		this.columns.addAll(columns);
	}

	@Override
	public void addAttributeReference(DBSTableColumn column) throws DBException {
		this.columns.add(new TableConstraintColumn(this, (TableColumn) column, columns.size()));
	}


}
