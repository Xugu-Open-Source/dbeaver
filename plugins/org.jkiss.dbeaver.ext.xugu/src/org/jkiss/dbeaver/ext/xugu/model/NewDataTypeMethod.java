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
