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
package org.jkiss.dbeaver.ext.xugu.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ext.xugu.Utils;
import com.xugu.permission.LoadPermission;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * 角色信息类，包含名称、角色权限等具体信息
 */
public class Role extends BaseGlobalObject implements DBARole, DBPRefreshableObject {
	private String name;
	private int id;
	private String authentication;
	private Collection<RoleAuthority> roleAuthorities;
	private String userDesc;
	private Connection conn;
	private Database parent;
	DBRProgressMonitor monitor;

	public Role(DataSource dataSource, DBRProgressMonitor monitor, ResultSet resultSet) {
		super(dataSource, true);
		try {
			this.monitor = monitor;
			if (resultSet != null) {
				conn = resultSet.getStatement().getConnection();
				if (resultSet.getMetaData().getColumnCount() != 1) {
					String roleNameString = JDBCUtils.safeGetString(resultSet, "USER_NAME");
					this.name =  roleNameString;
					if(User.roleNames.size()==0) {
						User.roleNames.add(roleNameString);
					}else {
						Boolean isHaveBoolean = false;
						for (int i = 0; i < User.roleNames.size(); i++) {
							if(User.roleNames.get(i).equals(roleNameString)) {
								 isHaveBoolean = true ;
							}
						}
						if(!isHaveBoolean) {
							//加入全部角色列表中
							User.roleNames.add(roleNameString);
						}
					}
					this.id = JDBCUtils.safeGetInt(resultSet, "USER_ID");
					this.authentication = JDBCUtils.safeGetString(resultSet, "PASSWORD");
				}
				this.reloadAuthrities();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		this.parent = dataSource.getDatabase();
	}

	@NotNull
	@Override
	@Property(viewable = true,editable = false ,order = 2)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthentication() {
		return authentication;
	}

	public int getId() {
		return this.id;
	}

	public String getUserDesc() {
		return userDesc;
	}

	public void setUserDesc(String desc) {
		userDesc = desc;
	}

	@Nullable
	@Override
	public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
		Role role = this.getDataSource().roleCache.refreshObject(monitor, this.getDataSource(), this);
		role.reloadAuthrities();
		return role;
	}

	public String getObjectList(String schemaName, String type, String tableName) {
		return Utils.getObjectList(this.getDataSource(), monitor, schemaName, type, tableName);
	}

	public void reloadAuthrities() {
		// 加载权限
		Connection conn;
		try {
			conn = this.getDataSource().getDefaultInstance().getDefaultContext(true).getConnection(new LoggingProgressMonitor());
		} catch (SQLException e) {
			throw new RuntimeException("获取用户权限查询连接失败", e);
		}
		Vector<Object> authorities = new LoadPermission().loadPermission(conn, this.name, 0);
		Iterator<Object> it = authorities.iterator();
		roleAuthorities = new ArrayList<>();
		while (it.hasNext()) {
			String temp = it.next().toString();
			// 对象级权限
			if (temp.indexOf("\"") != -1) {
				String targetName = temp.substring(temp.indexOf("\""));
				RoleAuthority one = new RoleAuthority(this, temp, targetName, false, true);
				roleAuthorities.add(one);
			}
			// 库级权限
			else {
				RoleAuthority one = new RoleAuthority(this, temp, null, true, true);
				roleAuthorities.add(one);
			}
		}
	}

	public Collection<RoleAuthority> getRoleAuthorities() {
		return roleAuthorities;
	}

	public Collection<RoleAuthority> getRoleDatabaseAuthorities() {
		Collection<RoleAuthority> res = new ArrayList<>();
		if (roleAuthorities != null) {
			Iterator<RoleAuthority> it = roleAuthorities.iterator();
			while (it.hasNext()) {
				RoleAuthority authority = it.next();
				if (authority.isDatabase) {
					res.add(authority);
				}
			}
		}
		return res;
	}

	public Collection<RoleAuthority> getRoleObjectAuthorities() {
		Collection<RoleAuthority> res = new ArrayList<>();
		if (roleAuthorities != null) {
			Iterator<RoleAuthority> it = roleAuthorities.iterator();
			while (it.hasNext()) {
				RoleAuthority authority = it.next();
				final boolean isContainColumnOrTriggerAuth = !authority.getName().contains("列")
						&& !authority.getName().contains("触发器");
				if (!authority.isDatabase && isContainColumnOrTriggerAuth) {
					res.add(authority);
				}
			}
		}
		return res;
	}

	public Collection<RoleAuthority> getRoleSubObjectAuthorities() {
		Collection<RoleAuthority> res = new ArrayList<>();
		if (roleAuthorities != null) {
			Iterator<RoleAuthority> it = roleAuthorities.iterator();
			while (it.hasNext()) {
				RoleAuthority authority = it.next();
				final boolean isContainColumnOrTriggerAuth = authority.getName().contains("列")
						|| authority.getName().contains("触发器");
				if (!authority.isDatabase && isContainColumnOrTriggerAuth) {
					res.add(authority);
				} 
			}
		}
		return res;
	}

	public Database getParent() {
		return parent;
	}
}
