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
package org.jkiss.dbeaver.ext.xugu.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.ObjectType;
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.ext.xugu.model.source.StatefulObject;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLog;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 基本任务处理器，加载相关日志信息
 * 
 * @author Xugu
 */
public abstract class BaseTaskHandler extends AbstractHandler implements IElementUpdater {
	private static final Log LOG = Log.getLog(BaseTaskHandler.class);

	protected List<SourceObject> getSourceObjects(UIElement element) {
		List<SourceObject> objects = new ArrayList<>();
		IWorkbenchPartSite partSite = UIUtils.getWorkbenchPartSite(element.getServiceLocator());
		if (partSite != null) {
			final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
			if (selectionProvider != null) {
				ISelection selection = selectionProvider.getSelection();
				if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
					for (Iterator<?> iter = ((IStructuredSelection) selection).iterator(); iter.hasNext();) {
						final Object item = iter.next();
						final SourceObject sourceObject = RuntimeUtils.getObjectAdapter(item, SourceObject.class);
						if (sourceObject != null) {
							objects.add(sourceObject);
						}
					}
				}
			}
			if (objects.isEmpty()) {
				final IWorkbenchPart activePart = partSite.getPart();
				final SourceObject sourceObject = RuntimeUtils.getObjectAdapter(activePart, SourceObject.class);
				if (sourceObject != null) {
					objects.add(sourceObject);
				}
			}
		}
		return objects;
	}

	/**
	 * 记录对象错误日志，只对 SYS 用户开放错误日志信息
	 */
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
				LOG.error("Can't read user errors", e);
				return false;
			}
		}
		return false;
	}

}