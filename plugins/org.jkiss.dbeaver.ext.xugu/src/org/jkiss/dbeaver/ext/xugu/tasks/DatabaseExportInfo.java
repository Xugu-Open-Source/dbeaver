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
package org.jkiss.dbeaver.ext.xugu.tasks;

import java.util.Collection;

import com.alibaba.druid.sql.dialect.xugu.api.Base;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Table;

public class DatabaseExportInfo {

	@NotNull
	private Schema schemaCache;
	
	@Nullable
	private Collection<Table> tables;
	
	
	public DatabaseExportInfo(Schema schemaCache,Collection<Table> tables) {
		this.schemaCache = schemaCache;
		this.tables = tables;	
	}


	
	public Schema getSchemaCache() {
		return schemaCache;
	}


	
	public Collection<Table> getTables(){
		return tables;
	}	
}
