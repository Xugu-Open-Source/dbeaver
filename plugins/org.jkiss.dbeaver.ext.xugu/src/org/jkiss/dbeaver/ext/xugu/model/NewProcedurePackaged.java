package org.jkiss.dbeaver.ext.xugu.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import com.alibaba.druid.sql.dialect.xugu.api.XuguParserApi;
import com.alibaba.druid.sql.dialect.xugu.api.bean.CreateFunctionBean;
import com.alibaba.druid.sql.dialect.xugu.api.bean.CreatePackageBean;
import com.alibaba.druid.sql.dialect.xugu.api.bean.CreateProcedureBean;


/**
 * 
 * @author zkun
 *
 */
public class NewProcedurePackaged   implements DBSObject{
	
	
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
}
//		ProcedureParameter procedureParameter;
//		procParams = new ArrayList<ProcedureParameter>();
//			//通过xugu-parser解析包解析存储过程参数。
//			XuguParserApi xuguParserApi = new XuguParserApi();
//			
//			CreatePackageBean createPackageBean = xuguParserApi.parseCreatePackage(pack.getSourceDeclaration()+pack.getSourceDefinition());
//			List<CreateProcedureBean> createProcedureBean  = createPackageBean.getCreateProcedureBeans();
//			List<CreateFunctionBean> createFunctionBeans = createPackageBean.getCreateFunctionBeans();
//			for(int i = 0; i<createProcedureBean.getParamSize();i++) {
//				paraName = createProcedureBean.getParams().get(i).get(0);
//				paraType = createProcedureBean.getParams().get(i).get(2);
//				dataType = createProcedureBean.getParams().get(i).get(1);
//				paraPosition = Integer.valueOf(createProcedureBean.getParams().get(i).get(3));
//				paraDefault = createProcedureBean.getParams().get(i).get(4);
//				if(paraDefault == null) {
//					paraDefault = "";
//				}
//				procedureParameter =  new ProcedureParameter(monitor, this,paraName,paraType,dataType,paraPosition,paraDefault);
//				//monitor,procedure实例，参数名，数据类型，参数模式，参数位置，默认值。
//				procParams.add(procedureParameter);
//			}
// 
//			XuguParserApi  xuguParserApi = new XuguParserApi();
//			CreateFunctionBean createFunctionBean = xuguParserApi.parseCreateFunction(sourceDeclaration);
//			for (int i = 0; i < createFunctionBean.getParamSize(); i++) {
//				 paraName = createFunctionBean.getParams().get(i).get(0);
//					paraType = createFunctionBean.getParams().get(i).get(2);
//					dataType = createFunctionBean.getParams().get(i).get(1);
//					paraPosition = Integer.valueOf(createFunctionBean.getParams().get(i).get(3));
//					paraDefault = createFunctionBean.getParams().get(i).get(4);
//					if(paraDefault == null) {
//						paraDefault = "";
//					}
//					procedureParameter =  new ProcedureParameter(monitor, this,paraName,paraType,dataType,paraPosition,paraDefault);
//					//monitor,procedure实例，参数名，数据类型，参数模式，参数位置，默认值。
//					procParams.add(procedureParameter);
//			}
//	}
