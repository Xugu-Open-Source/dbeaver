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
package org.jkiss.dbeaver.ext.cae.model;

import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * 角色权限信息类
 */
public class RoleAuthority extends BaseAuthority {
	boolean isDatabase;
	boolean isSubObject;

	protected RoleAuthority(DBSObject parent, String name, String targetName, boolean isDatabase, boolean persisted) {
		super(parent, name, targetName, isDatabase, persisted);
		this.isDatabase = isDatabase;
//		this.isSubObject = isSubObject;
	}

	@Override
	public boolean isDatabase() {
		return this.isDatabase;
	}
	
	
	public boolean isSubObject() {
		return this.isSubObject;
	}
	
	

	@Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
	public String getTargetName() {
		return this.targetName;
	}
}
