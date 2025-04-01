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

import org.jkiss.dbeaver.model.meta.Property;

/**
 * 二级表分区信息类
 */
public class TableSubPartition extends BasePartition<BaseTablePhysical> {

	public TableSubPartition(BaseTablePhysical table, boolean subpartition, String name) {
		super(table, subpartition, name);
	}

	public TableSubPartition(BaseTablePhysical table, boolean subpartition, ResultSet dbResult) {
		super(table, subpartition, dbResult);
	}

	public TableSubPartition(BaseTablePhysical table, boolean subpartition, TableSubPartition srcSubPartition) {
		super(table, subpartition, srcSubPartition);
	}

	@Override
	@Property(viewable = true, order = 5, updatable = false, editable = true)
	public boolean isOnline() {
		return online;
	}

	@Override
	public void setOnline(boolean flag) {
		this.online = flag;
	}
}
