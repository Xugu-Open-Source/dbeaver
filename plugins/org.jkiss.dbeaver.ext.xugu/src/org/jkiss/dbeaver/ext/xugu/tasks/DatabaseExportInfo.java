package org.jkiss.dbeaver.ext.xugu.tasks;

import java.util.Collection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.model.Table;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.SchemaCache;
import org.jkiss.dbeaver.ext.xugu.model.Schema.TableCache;

public class DatabaseExportInfo {

	@NotNull
	private SchemaCache schemaCache;
	
	@Nullable
	private Collection<Table> tables;
	
	
	public DatabaseExportInfo(SchemaCache schemaCache,Collection<Table> tables) {
		this.schemaCache = schemaCache;
		this.tables = tables;	
	}
	
	
	public SchemaCache getSchemaCache() {
		return schemaCache;
	}
	
	public Collection<Table> getTables(){
		return tables;
	}	
}
