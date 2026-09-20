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
package org.jkiss.dbeaver.ext.xugu.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanSourceFormat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 计划分析
 */
public class PlanAnalyser implements DBCPlan {
	private static final Log log = Log.getLog(PlanAnalyser.class);
	private DataSource dataSource;
	private JDBCSession session;
	private String query;
	private List<PlanNode> rootNodes;

	public PlanAnalyser(DataSource dataSource, JDBCSession session, String query) {
		this.dataSource = dataSource;
		this.session = session;
		this.query = query;
	}

	@NotNull
	@Override
	public String getQueryString() {
		return query;
	}

	@NotNull
	@Override
	public String getPlanQueryString() throws DBException {
		return "EXPLAIN  VERBOSE " + query;
	}

	@NotNull
	@Override
	public DBCPlanSourceFormat getPlanSourceDataFormat() {
		return DBCPlanSourceFormat.JSON;
	}

	@Nullable
	@Override
	public Object getPlanSourceData() {
		return query;
	}

	public Collection<PlanNode> getPlanNodes() {
		return rootNodes != null ? rootNodes : List.of();
	}

	public void explain() throws DBException {
		String planQuery = getPlanQueryString();
		try {
			// Explain plan
			JDBCStatement dbStat = session.createStatement();
			// Read explained plan
			JDBCResultSet dbResult = dbStat.executeQuery(planQuery);
			rootNodes = new ArrayList<>();
			while (dbResult.next()) {
				String  pathStrigString = dbResult.getString(1);
				String [] paStrings = pathStrigString.split("\r\n");
				PlanNode node = null ;
				for(int i = 0 ; i<paStrings.length;i++) {
					node = new PlanNode(paStrings[i]);
					rootNodes.add(node);
				}
//				PlanNode node = new PlanNode(dataSource, dbResult);
			}
			dbStat.close();
		} catch (SQLException e) {
			throw new DBCException(e, session.getExecutionContext());
		}
	}

	@Nullable
	@Override
	public Object getPlanFeature(@NotNull String feature) {
		return null;
	}

	@NotNull
	@Override
	public List<? extends DBCPlanNode> getPlanNodes(@NotNull Map<String, Object> options) {
		return rootNodes != null ? rootNodes : List.of();
	}
}