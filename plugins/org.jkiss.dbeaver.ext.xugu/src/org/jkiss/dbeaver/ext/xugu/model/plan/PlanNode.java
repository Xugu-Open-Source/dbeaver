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
package org.jkiss.dbeaver.ext.xugu.model.plan;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.IntKeyMap;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 计划节点
 */
public class PlanNode implements DBCPlanNode {
/**
 * 	private final DataSource dataSource;
 */
	private PlanNode parent;
	private List<PlanNode> nested;
	private String planPath;
	private String objectName;

	static protected final Log log = Log.getLog(PlanNode.class);

//	public PlanNode(DataSource dataSource, ResultSet dbResult) {
//		this.dataSource = null;
//		this.dataSource = dataSource;
//		if (dbResult != null) {
//			this.planPath = JDBCUtils.safeGetString(dbResult, "plan_path");
//			this.objectName = "plan path";
//			this.parent = null;
//			this.nested = null;
//		}
//	}
	
	
	public PlanNode(String path) {
		this.planPath = path;
	}

//	public PlanNode(DataSource dataSource, IntKeyMap<PlanNode> prevNodes, ResultSet dbResult) throws SQLException {
//		this.dataSource = null;
//		this.dataSource = dataSource;
//		Integer parentId = JDBCUtils.safeGetInteger(dbResult, "parent_id");
//		if (parentId != null) {
//			parent = prevNodes.get(parentId);
//		}
//		if (parent != null) {
//			if (parent.nested == null) {
//				parent.nested = new ArrayList<>();
//			}
//			parent.nested.add(this);
//		}
//	}

	@Override
	public PlanNode getParent() {
		return parent;
	}

	@Override
	public Collection<PlanNode> getNested() {
		return nested;
	}

	@Property(name = "path", order = 0, viewable = true, description = "plan path")
	public String getPlanPath() {
		return planPath;
	}

	@Override
	public String getNodeName() {
		//TODO 获取节点名称
		return null;
	}

	@Override
	public String getNodeType() {
		//TODO 获取节点类型
		return null;
	}

	@Override
	public String getNodeCondition() {
		//TODO 获取节点条件
		return null;
	}

	@Override
	public String getNodeDescription() {
		//TODO 获取节点描述
		return null;
	}

	@Override
	public DBCPlanNodeKind getNodeKind() {
		//TODO 获取节点分类
		return null;
	}
}
