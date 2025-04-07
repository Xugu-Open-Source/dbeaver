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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;

import java.sql.SQLException;
import java.util.*;

/**
 * 结构辅助
 */
public class StructureAssistant implements DBSStructureAssistant<ExecutionContext> {
	static protected final Log log = Log.getLog(StructureAssistant.class);

	private final DataSource dataSource;

	public StructureAssistant(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public DBSObjectType[] getSupportedObjectTypes() {
		return new DBSObjectType[] { ObjectType.TABLE, ObjectType.PACKAGE, ObjectType.CONSTRAINT,
				ObjectType.FOREIGN_KEY, ObjectType.INDEX, ObjectType.PROCEDURE, ObjectType.SEQUENCE,
				ObjectType.TRIGGER, };
	}

	@Override
	public DBSObjectType[] getHyperlinkObjectTypes() {
		return new DBSObjectType[] { ObjectType.TABLE, ObjectType.PACKAGE, ObjectType.PROCEDURE, };
	}

	@Override
	public DBSObjectType[] getAutoCompleteObjectTypes() {
		return new DBSObjectType[] { ObjectType.TABLE, ObjectType.PACKAGE, ObjectType.PROCEDURE, };
	}

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(@NotNull DBRProgressMonitor monitor, @NotNull ExecutionContext executionContext,
                                                      @NotNull ObjectsSearchParams params) throws DBException {
        Schema schema = params.getParentObject() instanceof Schema ? (Schema) params.getParentObject() : null;

        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, "Find objects by name")) {
            List<DBSObjectReference> objects = new ArrayList<>();

            if (ArrayUtils.containsAny(params.getObjectTypes(), ObjectType.CONSTRAINT, ObjectType.FOREIGN_KEY)) {
                // Search constraints
                findConstraintsByMask(session, schema, params, objects);
                if (!containsOnlyConstraintOrFK(params.getObjectTypes())) {
                    searchAllObjects(session, schema, params, objects);
                }
            } else {
                // Search all objects
                searchAllObjects(session, schema, params, objects);
            }
            if (params.isSearchInComments()) {
                searchInTableComments(session, schema, params, objects);
            }

            // Sort objects. Put ones in the current schema first
            final Schema activeSchema = executionContext.getContextDefaults().getDefaultSchema();
            objects.sort((o1, o2) -> {
                if (CommonUtils.equalObjects(o1.getContainer(), o2.getContainer())) {
                    return o1.getName().compareTo(o2.getName());
                }
                if (o1.getContainer() == null || o1.getContainer() == activeSchema) {
                    return -1;
                }
                if (o2.getContainer() == null || o2.getContainer() == activeSchema) {
                    return 1;
                }
                return o1.getContainer().getName().compareTo(o2.getContainer().getName());
            });

            return objects;
        }
        catch (SQLException ex) {
            throw new DBException(ex.getMessage(), ex);
        }
    }

    private void findConstraintsByMask(JDBCSession session, final Schema schema, @NotNull ObjectsSearchParams params,
            List<DBSObjectReference> objects) throws SQLException, DBException {
		DBRProgressMonitor monitor = session.getProgressMonitor();

        List<DBSObjectType> objectTypesList = Arrays.asList(params.getObjectTypes());
        final boolean hasFK = objectTypesList.contains(ObjectType.FOREIGN_KEY);
		final boolean hasConstraints = objectTypesList.contains(ObjectType.CONSTRAINT);

		// Load tables
		StringBuilder decl = new StringBuilder(100);
		decl.append(
				"SELECT USR.USER_NAME AS OWNER, TAB.TABLE_NAME,CONS.CONS_NAME AS CONSTRAINT_NAME,CONS.CONS_TYPE AS CONSTRAINT_TYPE");
		decl.append(" FROM ");
		decl.append(schema.getRoleFlag());
		decl.append("_CONSTRAINTS CONS ");
		decl.append(" LEFT JOIN ");
		decl.append(schema.getRoleFlag());
		decl.append("_TABLES TAB ON CONS.TABLE_ID=TAB.TABLE_ID ");
		decl.append(" LEFT JOIN ");
		decl.append(schema.getRoleFlag());
		decl.append("_USERS USR ON USR.USER_ID=TAB.USER_ID");
		decl.append(" WHERE CONS.DB_ID=CURRENT_DB_ID ");
		decl.append(" AND CONSTRAINT_NAME LIKE ?");
		decl.append((!hasFK ? " AND CONSTRAINT_TYPE<>'R'" : ""));
		decl.append((schema != null ? " AND OWNER=?" : ""));

		log.debug("[" + OemConfig.OEM_NAME_EN + "] Constraints Information: " + decl.toString());
		try (JDBCPreparedStatement dbStat = session.prepareStatement(decl.toString())) {
			dbStat.setString(1, params.isCaseSensitive() ? params.getMask() : params.getMask().toUpperCase());
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next() && objects.size() < params.getMaxResults()) {
                    final String schemaName = JDBCUtils.safeGetString(dbResult, Constants.COL_OWNER);
                    final String tableName = JDBCUtils.safeGetString(dbResult, Constants.COL_TABLE_NAME);
                    final String constrName = JDBCUtils.safeGetString(dbResult, Constants.COL_CONSTRAINT_NAME);
                    final String constrType = JDBCUtils.safeGetString(dbResult, Constants.COL_CONSTRAINT_TYPE);
                    final DBSEntityConstraintType type = TableConstraint.getConstraintType(constrType);
                    objects.add(new AbstractObjectReference(
                        constrName,
                        dataSource.getSchema(session.getProgressMonitor(), schemaName),
                        null,
                        type == DBSEntityConstraintType.FOREIGN_KEY ? TableForeignKey.class : TableConstraint.class,
                        type == DBSEntityConstraintType.FOREIGN_KEY ? ObjectType.FOREIGN_KEY : ObjectType.CONSTRAINT) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            Schema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                            if (tableSchema == null) {
                                throw new DBException("Constraint schema '" + schemaName + "' not found");
                            }
                            Table table = tableSchema.getTable(monitor, tableName);
                            if (table == null) {
                                throw new DBException("Constraint table '" + tableName + "' not found in catalog '" + tableSchema.getName() + "'");
                            }
                            DBSObject constraint = null;
                            if (hasFK && type == DBSEntityConstraintType.FOREIGN_KEY) {
                                constraint = table.getForeignKey(monitor, constrName);
                            }
                            if (hasConstraints && type != DBSEntityConstraintType.FOREIGN_KEY) {
                                constraint = table.getConstraint(monitor, constrName);
                            }
                            if (constraint == null) {
                                throw new DBException("Constraint '" + constrName + "' not found in table '" + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            }
                            return constraint;
                        }
                    });
                }
            }
		}
	}

    private void searchAllObjects(final JDBCSession session, final Schema schema, @NotNull ObjectsSearchParams params,
            List<DBSObjectReference> objects) throws SQLException, DBException {
		StringBuilder objectTypeClause = new StringBuilder(100);
		final List<ObjectType> objectTypeList = new ArrayList<>(params.getObjectTypes().length + 2);
		for (DBSObjectType objectType : params.getObjectTypes()) {
			if (objectType instanceof ObjectType) {
				objectTypeList.add((ObjectType) objectType);
				if (objectType == ObjectType.PROCEDURE) {
					objectTypeList.add(ObjectType.FUNCTION);
				} else if (objectType == ObjectType.TABLE) {
					objectTypeList.add(ObjectType.VIEW);
					objectTypeList.add(ObjectType.MATERIALIZED_VIEW);
				}
			} else if (DBSProcedure.class.isAssignableFrom(objectType.getTypeClass())) {
				objectTypeList.add(ObjectType.FUNCTION);
			}
		}
		for (ObjectType objectType : objectTypeList) {
			if (objectTypeClause.length() > 0) {
				objectTypeClause.append(",");
			}
			objectTypeClause.append("'").append(objectType.getTypeName()).append("'");
		}
		if (objectTypeClause.length() == 0) {
			return;
		}
		// Always search for synonyms
		objectTypeClause.append(",'").append(ObjectType.SYNONYM.getTypeName()).append("'");
	}

	@Override
	public DBSObjectType[] getSearchObjectTypes() {
		// TODO 获取搜索对象类型列表
		return null;
	}

    private boolean containsOnlyConstraintOrFK(DBSObjectType[] objectTypes) {
        for (DBSObjectType objectType : objectTypes) {
            if (!(objectType == ObjectType.CONSTRAINT || objectType == ObjectType.FOREIGN_KEY)) {
                return false;
            }
        }
        return true;
    }
    
	private void searchInTableComments(@NotNull JDBCSession session, @Nullable Schema schema,
			@NotNull ObjectsSearchParams params, @NotNull List<DBSObjectReference> objects)
			throws SQLException, DBException {
		if (objects.size() >= params.getMaxResults()
				|| !ArrayUtils.contains(params.getObjectTypes(), ObjectType.TABLE)) {
			return;
		}
		StringBuilder sql = new StringBuilder(
				"SELECT atc.OWNER, atc.TABLE_NAME, atc.TABLE_TYPE FROM ALL_TAB_COMMENTS atc WHERE ");
		String mask = params.getMask();
		if (params.isCaseSensitive()) {
			sql.append("atc.COMMENTS ");
		} else {
			sql.append("UPPER(atc.COMMENTS) ");
			mask = mask.toUpperCase();
		}
		sql.append("LIKE ? ");
		if (schema != null) {
			sql.append("AND atc.OWNER = ? ");
		}
		sql.append("ORDER BY atc.TABLE_NAME");

		try (JDBCPreparedStatement preparedStatement = session.prepareStatement(sql.toString())) {
			preparedStatement.setString(1, mask);
			if (schema != null) {
				preparedStatement.setString(2, schema.getName());
			}
			try (JDBCResultSet resultSet = preparedStatement.executeQuery()) {
				while (!session.getProgressMonitor().isCanceled() && objects.size() < params.getMaxResults()
						&& resultSet.next()) {
					String owner = JDBCUtils.safeGetString(resultSet, "OWNER");
					String tableName = JDBCUtils.safeGetString(resultSet, "TABLE_NAME");
					String tableType = JDBCUtils.safeGetString(resultSet, "TABLE_TYPE");
					ObjectType oracleObjectType = ObjectType.getByType(tableType);
					if (oracleObjectType == null || !oracleObjectType.isBrowsable() || tableName == null) {
						continue;
					}
					Schema objectSchema = dataSource.getSchema(session.getProgressMonitor(), owner);
					if (objectSchema == null) {
						log.debug("Schema '" + owner + "' not found. Probably was filtered");
						continue;
					}
					addObjectReference(objects, tableName, objectSchema, oracleObjectType, tableType, owner, session);
				}
			}
		}
	}
	
	private void addObjectReference(@NotNull Collection<DBSObjectReference> references, String objectName,
			@NotNull DBSObject objectSchema, @NotNull ObjectType objectType, String objectTypeName,
			String schemaName, @NotNull JDBCSession session) {
		references.add(
				new AbstractObjectReference(objectName, objectSchema, null, objectType.getTypeClass(), objectType) {
					@Override
					public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
						Schema tableSchema = (Schema) getContainer();
						DBSObject object = objectType.findObject(session.getProgressMonitor(), tableSchema, objectName);
						if (object == null) {
							throw new DBException(objectTypeName + " '" + objectName + "' not found in schema '"
									+ tableSchema.getName() + "'");
						}
						return object;
					}

					@NotNull
					@Override
					public String getFullyQualifiedName(DBPEvaluationContext context) {
						if (objectType == ObjectType.SYNONYM && Constants.USER_PUBLIC.equals(schemaName)) {
							return DBUtils.getQuotedIdentifier(dataSource, objectName);
						}
						return super.getFullyQualifiedName(context);
					}
				});
	}
}
