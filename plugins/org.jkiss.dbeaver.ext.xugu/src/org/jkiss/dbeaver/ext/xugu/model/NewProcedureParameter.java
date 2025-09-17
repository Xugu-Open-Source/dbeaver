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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author zkun
 *
 */
public class NewProcedureParameter implements DBSProcedureParameter, DBSTypedObject{
	
	private   NewProcedurePackaged procedure;
	private String  procedureName;
	private String name;
	private int position=0;
	private int dataLevel;
	private int sequence;
	private ParameterMode mode;
	private DataType type;
	private DataType dataType;
	private String packageTypeName;
	private int dataLength;
	private int dataScale;
	private int dataPrecision;
	private String defalutValue;
	private String define;
	private List<NewProcedureParameter> params;

	public NewProcedureParameter(NewProcedurePackaged procedure,DBRProgressMonitor monitor,   String name, String datatype,
			String mode,int position, String defalutValue,Integer precision,Integer scale) {
		
		this.procedureName = procedure.getName();
		this.procedure = procedure;
		this.name = name;
		// 对int做转化
		final String typeInt = "INT";
		if (typeInt.equals(datatype.toUpperCase())) {
			datatype = "INTEGER";
		}
		this.dataType = new DataType(this, datatype.toUpperCase(), true);
		
		if(scale !=null) {
			this.dataScale = scale;
		}else {
			this.dataScale = this.dataType.getMaxScale();
		}
		if(precision!=null) {
			this.dataPrecision = precision;
		}else {
			this.dataPrecision = this.dataType.getPrecision();
		}
	
		this.dataLength = this.dataType.getLength();
		this.position = position;
		this.mode = ParameterMode.getMode(mode);
		this.defalutValue = defalutValue;
	}
	
	public NewProcedureParameter(NewProcedurePackaged procedure,   String name, String datatype,
			String mode,int position, String defalutValue,Integer precision,Integer scale) {
		
		this.procedureName = procedure.getName();
		this.procedure = procedure;
		this.name = name;
		// 对int做转化
		final String typeInt = "INT";
		if (typeInt.equals(datatype.toUpperCase())) {
			datatype = "INTEGER";
		}
		this.dataType = new DataType(this, datatype.toUpperCase(), true);
		
		if(scale !=null) {
			this.dataScale = scale;
		}else {
			this.dataScale = this.dataType.getMaxScale();
		}
		if(precision!=null) {
			this.dataPrecision = precision;
		}else {
			this.dataPrecision = this.dataType.getPrecision();
		}
		
		this.dataLength = this.dataType.getLength();
		this.position = position;
		this.mode = ParameterMode.getMode(mode);
		this.defalutValue = defalutValue;
	}
	
	public NewProcedureParameter( DBRProgressMonitor monitor,   String name, String datatype,
			String mode,int position, String defalutValue) {
	
		this.name = name;
		// 对int做转化
		final String typeInt = "INT";
		if (typeInt.equals(datatype.toUpperCase())) {
			datatype = "INTEGER";
		}
		this.dataType = new DataType(this, datatype.toUpperCase(), true);
		
		this.dataScale = this.dataType.getMaxScale();
		this.dataPrecision = this.dataType.getPrecision();
		
		this.dataLength = this.dataType.getLength();
		this.position = position;
		this.mode = ParameterMode.getMode(mode);
		this.defalutValue = defalutValue;
	}

 
	@Nullable
	@Override
	public String getDescription() {
		// TODO 获取描述符
		return null;
	}

 
 

	@Override
	public boolean isPersisted() {
		return true;
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 10)
	public String getName() {
		if (CommonUtils.isEmpty(name)) {
			if (dataLevel == 0) {
				// 函数结果集
				return "RESULT";
			} else {
				// 集合元素
				return "ELEMENT";
			}
		}
		return name;
	}

	public boolean isResultArgument() {
		return CommonUtils.isEmpty(name) && dataLevel == 0;
	}

	@Property(viewable = true, order = 11)
	public int getPosition() {
		return position+1;
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 20)
	public DBSProcedureParameterKind getParameterKind() {
		return mode == null ? DBSProcedureParameterKind.UNKNOWN : mode.getParameterKind();
	}

	@Property(viewable = true, order = 21)
	public Object getType() {
		return packageTypeName != null ? packageTypeName : dataType == null ? type : dataType;
	}

	@Override
	@Property(viewable = true, order = 30)
	public long getMaxLength() {
		return dataLength;
	}

	@Override
	public String getTypeName() {
		return type == null ? packageTypeName : type.getName();
	}

	@Override
	public String getFullTypeName() {
		return DBUtils.getFullTypeName(this);
	}

	@Override
	public int getTypeID() {
		return type == null ? 0 : type.getTypeID();
	}

	@Override
	public DBPDataKind getDataKind() {
		return type == null ? DBPDataKind.OBJECT : type.getDataKind();
	}

	@Override
	@Property(viewable = true, order = 40)
	public Integer getScale() {
		return dataScale;
	}

	@Override
	@Property(viewable = true, order = 50)
	public Integer getPrecision() {
		return dataPrecision;
	}

	public int getDataLevel() {
		return dataLevel;
	}

	public int getSequence() {
		return sequence;
	}

	
	   @Association
	    public Collection<NewProcedureParameter> getAttributes()
	    {
	        return  params;
	    }

	    void addAttribute(NewProcedureParameter attribute)
	    {
	        if (params == null) {
	        	params = new ArrayList<>();
	        }
	        params.add(attribute);
	    }
	    
	public boolean hasAttributes() {
		return !CommonUtils.isEmpty(params);
	}

	@NotNull
	@Override
	public DBSTypedObject getParameterType() {
		return this;
	}

	@Property(viewable = true, order = 60)
	public String getDefaultValue() {
		return defalutValue;
	}




	@Override
	public DBSObject getParentObject() {
		// TODO Auto-generated method stub
		return null;
	}




	public NewProcedurePackaged procedure() {
		return procedure;
	}




	@Override
	public DBPDataSource getDataSource() {
		// TODO Auto-generated method stub
		return this.procedure().getDataSource();
	}

	@Override
	public long getTypeModifiers() {
		return 0;
	}
}
