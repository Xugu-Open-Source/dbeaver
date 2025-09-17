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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * 
 * @author zkun
 *
 */
public class NewProcedurePackaged   implements DBSObject, DBPUniqueObject {
	
	
	private Package package1;
	private String procedureName;
	private boolean valid;
	private String comment;
	private Timestamp createTime;
	private String sourceDeclaration;
	private String paraName;
	private String paraType;
	private String dataType;
	private Integer paraPosition;
	private String paraDefault;
	private List<NewProcedureParameter> procParams;
	
	public NewProcedurePackaged() {
		
	}
	
	public NewProcedurePackaged(Package package1 ,String procedureName,List<NewProcedureParameter> procParams) {
		this.procedureName = procedureName;
		this.procParams = procParams;
		this.package1 = package1;
	}

	public NewProcedurePackaged(Package package1 ,String procedureName ) {
		this.procedureName = procedureName;
		this.package1 = package1;
	}
	
 
	public List<NewProcedureParameter> getProcParams() {
		return procParams;
	}

	public void setProcParams(List<NewProcedureParameter> procParams) {
		this.procParams = procParams;
	}
 
	public Collection<NewProcedureParameter> getParameters() throws DBException {
		return this.procParams;
	}
	
	
	@Override
	@Property(viewable = true,order = 3)
	public String getName() {
		// TODO Auto-generated method stub
		return procedureName;
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



	public Package getPackage1() {
		return package1;
	}


	public void setPackage1(Package package1) {
		this.package1 = package1;
	}


	@Override
	public DBPDataSource getDataSource() {
		// TODO Auto-generated method stub
		return this.getPackage1().getDataSource();
	}
	
	
	   @Association
	    public Collection<NewProcedureParameter> getAttributes()
	    {
	        return  procParams;
	    }

	    void addAttribute(NewProcedureParameter attribute)
	    {
	        if (procParams == null) {
	        	procParams = new ArrayList<>();
	        }
	        procParams.add(attribute);
	    }
	    
	public boolean hasAttributes() {
		return !CommonUtils.isEmpty(procParams);
	}

	@Override
	public String getUniqueName() {
		StringBuilder builder = new StringBuilder(this.getName());
		builder.append("(");
		if (procParams != null) {
			for (int i=0; i < procParams.size(); ++i) {
				NewProcedureParameter param = procParams.get(i);
				builder.append(param.getType());
				builder.append(" ");
				builder.append(param.getName());
				if (i < procParams.size() - 1) {
					builder.append(",");
				}
			}
		}
		builder.append(")");
		return builder.toString();
	}
}
