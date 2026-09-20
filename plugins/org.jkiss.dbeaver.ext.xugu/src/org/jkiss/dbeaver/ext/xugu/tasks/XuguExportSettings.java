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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Table;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceMap;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.dbeaver.tasks.nativetool.ExportSettingsExtension;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class XuguExportSettings extends AbstractImportExportSettings<DBSObject> implements ExportSettingsExtension<DatabaseExportInfo> {

	private static final Log log = Log.getLog(XuguExportSettings.class);

    public XuguExportSettings() {
        super();
    }

    public XuguExportSettings(@NotNull DBPProject project) {
        super(project);
    }
	
	public List<DatabaseExportInfo> exportObjects = new ArrayList<>();
	
    public void setExportObjects(List<DatabaseExportInfo> exportObjects) {
        this.exportObjects = exportObjects;
    }

    public List<DatabaseExportInfo> getExportObjects() {
        return exportObjects;
    }


	public void fillExportObjectsFromInput() {
		Map<Schema, List<Table>> objMap = new LinkedHashMap<>();
		
		for(DBSObject object : getDatabaseObjects()){
            Schema schemaCache = null;
			if(object instanceof Schema) {
				schemaCache = (Schema) object;
			}
            if (schemaCache == null) {
                log.error("Can't determine export catalog");
                continue;
            }
            List<Table> collect = null;
            if (schemaCache.tableCache != null){
                collect = schemaCache.tableCache.getCachedObjects().stream().map(baseTable -> (Table) baseTable).collect(Collectors.toList());
            }

            objMap.put(schemaCache, collect);


        }
        for (Map.Entry<Schema, List<Table>> entry : objMap.entrySet()) {
            getExportObjects().add(new DatabaseExportInfo((Schema) entry.getKey(), entry.getValue()));
        }
        updateDataSourceContainer();
		}

    @Override
    public String getOutputFile(DatabaseExportInfo databaseExportInfo) {
        Collection<DBSSchema> dbsSchemas = new ArrayList<>();
        dbsSchemas.add(databaseExportInfo.getSchemaCache());
        DataSource dataSource = databaseExportInfo.getSchemaCache().getDataSource();

        String outFileName = resolveVars(dataSource, dbsSchemas, databaseExportInfo.getTables(), getOutputFilePattern());
        return makeOutFilePath(getOutputFolder(databaseExportInfo), outFileName);
    }

    @Override
    public String getOutputFolder(DatabaseExportInfo databaseExportInfo) {
        Collection<DBSSchema> dbsSchemas = new ArrayList<>();
        dbsSchemas.add(databaseExportInfo.getSchemaCache());
        return resolveVars(null, dbsSchemas, databaseExportInfo.getTables(), getOutputFolderPattern());

    }

 

	@Override
	public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
			super.loadSettings(runnableContext,store);
		      if (store instanceof DBPPreferenceMap) {
		            // Save input objects to task properties
		            List<Map<String, Object>> objectList = ((DBPPreferenceMap) store).getObject("exportObjects");
		            if (!CommonUtils.isEmpty(objectList)) {
		                for (Map<String, Object> object : objectList) {
		                    String schemaId = CommonUtils.toString(object.get("schema"));
		                    if (!CommonUtils.isEmpty(schemaId)) {
		                        List<String> tableNames = (List<String>) object.get("tables");
		                        DatabaseExportInfo exportInfo = loadDatabaseExportInfo(runnableContext, schemaId, tableNames);
		                        if (exportInfo != null) {
		                            exportObjects.add(exportInfo);
		                        }
		                    }
		                }
		            }
		        }
	}
	
	 private DatabaseExportInfo loadDatabaseExportInfo(DBRRunnableContext runnableContext, String schemaId, List<String> tableNames) {


         DatabaseExportInfo[] exportInfo = new DatabaseExportInfo[1];
         try {
             runnableContext.run(false, true, monitor -> {
                 try {
                     Schema schema = (Schema) DBUtils.findObjectById(monitor, getProject(), schemaId);
                     if (schema == null) {
                         throw new DBException("Catalog " + schemaId + " not found");
                     }
                     List<Table> tables = null;
                     if (!CommonUtils.isEmpty(tableNames)) {
                         tables = new ArrayList<>();
                         for (String tableName : tableNames) {
                             Table table = (Table) schema.tableCache.getObject(monitor, schema, tableName);
                             if (table != null) {
                                 tables.add(table);
                             }
                         }
                     }
                     exportInfo[0] = new DatabaseExportInfo( schema, tables);
                 } catch (Throwable e) {
                     throw new InvocationTargetException(e);
                 }
             });
         } catch (InvocationTargetException e) {
             log.error("Error loading objects configuration", e);
         } catch (InterruptedException e) {
             // Ignore
         }
         return exportInfo[0];
	 }

     private boolean showViews;
     private boolean addDropStatements;
     private boolean binariesInHex;
     private boolean noData;

    public void setShowViews(boolean showViews) {
        this.showViews = showViews;
    }


    // 添加drop语句
    public boolean isAddDropStatements() {
        return addDropStatements;
    }

    public boolean isBinariesInHex() {
        return binariesInHex;
    }


    public boolean isNoData() {
        return noData;
    }

    public void setAddDropStatements(boolean addDropStatements) {
        this.addDropStatements = addDropStatements;
    }

    public void setBinariesInHex(boolean binariesInHex) {
        this.binariesInHex = binariesInHex;
    }

    public void setNoData(boolean noData) {
        this.noData = noData;
    }


    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) {
        super.saveSettings(runnableContext, store);
//        store.setValue("MySQL.export.method", method.name());
//        store.setValue("MySQL.export.noCreateStatements", noCreateStatements);
//        store.setValue("MySQL.export.addDropStatements", addDropStatements);
//        store.setValue("MySQL.export.disableKeys", disableKeys);
//        store.setValue("MySQL.export.extendedInserts", extendedInserts);
//        store.setValue("MySQL.export.dumpEvents", dumpEvents);
//        store.setValue("MySQL.export.comments", comments);
//        store.setValue("MySQL.export.removeDefiner", removeDefiner);
//        store.setValue("MySQL.export.binariesInHex", binariesInHex);
//        store.setValue("MySQL.export.noData", noData);
//        store.setValue("MySQL.export.noRoutines", noRoutines);
//        store.setValue("MySQL.export.showViews", showViews);
//        store.setValue(MySQLNativeCredentialsSettings.PREFERENCE_NAME, overrideCredentials);

        if (store instanceof DBPPreferenceMap && !CommonUtils.isEmpty(exportObjects)) {
            // Save input objects to task properties
            List<Map<String, Object>> objectList = new ArrayList<>();
            for (DatabaseExportInfo object : exportObjects) {
                Map<String, Object> objInfo = new LinkedHashMap<>();
                objInfo.put("schema", DBUtils.getObjectFullId(object.getSchemaCache()));
                if (!CommonUtils.isEmpty(object.getTables())) {
                    List<String> tableList = new ArrayList<>();
                    for (BaseTable table : object.getTables()) {
                        tableList.add(table.getName());
                    }
                    objInfo.put("tables", tableList);
                }
                objectList.add(objInfo);
            }

            ((DBPPreferenceMap) store).getPropertyMap().put("exportObjects", objectList);
        }
    }
}
