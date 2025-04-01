package org.jkiss.dbeaver.ext.cae.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.cae.model.DataSource.SchemaCache;
import org.jkiss.dbeaver.ext.cae.model.Table;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.utils.CommonUtils;

public class ExportSettings extends AbstractImportExportSettings<DBSObject> {

	private static final Log log = Log.getLog(ExportSettings.class);
	
	public List<DatabaseExportInfo> exportObjects = new ArrayList<>();
	
    public void setExportObjects(List<DatabaseExportInfo> exportObjects) {
        this.exportObjects = exportObjects;
    }

    public List<DatabaseExportInfo> getExportObjects() {
        return exportObjects;
    }
	
	public void fillExportObjectsFromInput() {
		Map<SchemaCache, List<Table>> objMap = new LinkedHashMap<>();
		
		for(DBSObject object : getDatabaseObjects()){
			SchemaCache schemaCache = null;
			if(object instanceof SchemaCache) {
				schemaCache =(SchemaCache) object;
			}else if (object instanceof SchemaCache) {
				 
            }
            if (schemaCache == null) {
                log.error("Can't determine export catalog");
                continue;
            }
            List<Table> tables = objMap.computeIfAbsent(schemaCache, mySQLCatalog -> new ArrayList<>());
            if (object instanceof Table) {
                tables.add((Table) object);
            }
        }
        for (Map.Entry<SchemaCache, List<Table>> entry : objMap.entrySet()) {
            getExportObjects().add(new DatabaseExportInfo(entry.getKey(), entry.getValue()));
        }
        updateDataSourceContainer();
		}

 

//	@Override
//	public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
//			super.loadSettings(runnableContext,store);
//		      if (store instanceof DBPPreferenceMap) {
//		            // Save input objects to task properties
//		            List<Map<String, Object>> objectList = ((DBPPreferenceMap) store).getObject("exportObjects");
//		            if (!CommonUtils.isEmpty(objectList)) {
//		                for (Map<String, Object> object : objectList) {
//		                    String catalogId = CommonUtils.toString(object.get("catalog"));
//		                    if (!CommonUtils.isEmpty(catalogId)) {
//		                        List<String> tableNames = (List<String>) object.get("tables");
//		                        DatabaseExportInfo exportInfo = loadDatabaseExportInfo(runnableContext, catalogId, tableNames);
//		                        if (exportInfo != null) {
//		                            exportObjects.add(exportInfo);
//		                        }
//		                    }
//		                }
//		            }
//		        }
//	}
	
//	 private DatabaseExportInfo loaDatabaseExportInfo(DBRRunnableContext runnableContext, String catalogId, List<String> tableNames) {
//		 
//	 }
	
	
	
}
