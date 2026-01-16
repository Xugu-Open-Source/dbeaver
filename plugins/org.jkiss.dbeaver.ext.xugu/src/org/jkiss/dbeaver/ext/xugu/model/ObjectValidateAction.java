/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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


import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.ext.xugu.model.source.StatefulObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLog;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLogBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;

import java.sql.ResultSet;

/**
 * 对象验证动作
 */
public class ObjectValidateAction extends ObjectPersistAction {
    private static final Log log = Log.getLog(ObjectValidateAction.class);
	private final SourceObject object;

	public ObjectValidateAction(SourceObject object, ObjectType objectType, String title, String script) {
		super(objectType, title, script);
		this.object = object;
	}

	@Override
	public void afterExecute(DBCSession session, Throwable error) throws DBCException {
		if (error != null) {
			return;
		}
		DBCCompileLog log = new DBCCompileLogBase();
		// 获取jdbc返回的错误信息
		logObjectErrors((JDBCSession) session, log, object, getObjectType());
		if (!log.getErrorStack().isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append("Error during ").append(getObjectType().getTypeName()).append(" '").append(object.getName())
					.append("' validation:");
			for (DBCCompileError e : log.getErrorStack()) {
				message.append("\n");
				message.append(e.toString());
			}
			throw new DBCException(message.toString());
		}
	}

    public static boolean logObjectErrors(JDBCSession session, DBCCompileLog compileLog, StatefulObject schemaObject,
                                          ObjectType objectType) {
        final String roleSys = "SYS";
        if (schemaObject.getSchema().getRoleFlag() == roleSys) {
            try {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                        "SELECT * FROM SYS_ERROR_LOG WHERE USER='" + schemaObject.getDataSource().getName() + "'")) {
                    try (ResultSet dbResult = dbStat.executeQuery()) {
                        boolean hasErrors = false;
                        while (dbResult.next()) {
                            DBCCompileError error = new DBCCompileError(true, dbResult.getString("ERR_STR"),
                                    dbResult.getInt("ERR_CODE"), dbResult.getInt("ERR_NO"));
                            hasErrors = true;
                            if (error.isError()) {
                                compileLog.error(error);
                            } else {
                                compileLog.warn(error);
                            }
                        }
                        return !hasErrors;
                    }
                }
            } catch (Exception e) {
                log.error("Can't read user errors", e);
                return false;
            }
        }
        return false;
    }
}
