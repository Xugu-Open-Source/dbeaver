package org.jkiss.dbeaver.ext.cae.model;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import com.alibaba.druid.stat.TableStat.Name;

/**
 * 
 * @author zkun
 *
 */
public class NewDataTypeAttribute implements DBSObject{
	
	private String  name;
	private String typeName;
	
	public NewDataTypeAttribute(String name, String typeName) {
		this.name = name;
		this.typeName = typeName;
	}
	
	
	@Override
	@Property(viewable = true,order = 3)
	public String getName() {
		// TODO Auto-generated method stub
		return  name;
	}
	
	@Property(viewable = true,order = 4)
	public String getTypeName() {
		return typeName;
	}


	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}


	public void setName(String name) {
		this.name = name;
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
