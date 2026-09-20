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

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;


/**
 * 
 * @author zkun
 *
 */
public class NewDataTypeMethod implements DBSObject {

	private String name;
	private String methodType;
	
	
	public 	NewDataTypeMethod(String name, String methodType) {
		this.name =name;
		this.methodType = methodType;
	} 
	
	
	@Property(viewable = true,order = 3)
	public String getMethodType() {
		return methodType;
	}

	public void setMethodType(String methodType) {
		this.methodType = methodType;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	@Property(viewable = true,order = 4)
	public String getName() {
		// TODO Auto-generated method stub
		return name;
	}
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public boolean isPersisted() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public DBSObject getParentObject() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public DBPDataSource getDataSource() {
		// TODO Auto-generated method stub
		return null;
	}
	

	
}
