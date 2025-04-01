package org.jkiss.dbeaver.ext.xugu.model;

import java.sql.ResultSet;
import java.sql.Types;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class ViewColumn extends TableColumn {

	private int dbId ;
	private int viewId ;
	private int colNo ;
	private String colName;
	private String timeStampT;
	private String typeName;
	private Boolean varying;
	private Integer scale;
	
	private DataType type;
	private DataTypeModifier typeMod;
	
	public ViewColumn(DBRProgressMonitor monitor, BaseTable table, ResultSet dbResult) throws DBException {
		super(monitor, table, dbResult);
		// TODO Auto-generated constructor stub
		this.dbId = JDBCUtils.safeGetInt(dbResult, "DB_ID");
		this.viewId = JDBCUtils.safeGetInt(dbResult, "VIEW_ID");
		this.colNo = JDBCUtils.safeGetInt(dbResult, "COL_NO");
		this.scale = JDBCUtils.safeGetInt(dbResult, "SCALE");
		this.colName = JDBCUtils.safeGetString(dbResult, "COL_NAME");
		this.timeStampT = JDBCUtils.safeGetString(dbResult, "TIMESTAMP_T");
		this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
		this.varying = JDBCUtils.safeGetBoolean(dbResult, "VARYING");
		setName(this.colName);
		setOrdinalPosition(this.colNo);
		
		// 对数据类型、精度标度进行统一处理
		final String typeDatetime = "DATETIME";
		final String typeChar = "CHAR";
		final String typeNumeric = "NUMERIC";
		final String typeVarchar = "VARCHAR";
		final String typeIntervalRegex = "INTERVAL(.*)";
		final String typeTimestamp = "TIMESTAMP";

		if (typeDatetime.equals(this.typeName)) {
			String t = JDBCUtils.safeGetString(dbResult, "TIMESTAMP_T");
			final String flag = "n";
			if (flag.equals(t)) {
				this.typeName = "DATETIME";
			} else {
				this.typeName = "TIMESTAMP";
			}
		} else if (typeChar.equals(this.typeName)) {
			boolean v = JDBCUtils.safeGetBoolean(dbResult, "VARYING");
			if (v) {
				this.typeName = "VARCHAR";
			} else {
				this.typeName = "CHAR";
			}
		}
		this.type = new DataType(this, this.typeName, true);

		if (this.type != null) {
			this.typeName = type.getFullyQualifiedName(DBPEvaluationContext.DDL);
			this.valueType = type.getTypeID();
			if (typeNumeric.equals(this.typeName)) {
				this.scale = JDBCUtils.safeGetInt(dbResult, "SCALE") % 65536;
				this.precision = (JDBCUtils.safeGetInt(dbResult, "SCALE") - this.scale) / 65536;
				this.maxLength = this.precision;
			} else if (typeChar.equals(this.typeName) || typeVarchar.equals(this.typeName)) {
				this.precision = JDBCUtils.safeGetInt(dbResult, "SCALE");
				this.scale = null;
			} else if (this.typeName.matches(typeIntervalRegex)) {
				this.scale = JDBCUtils.safeGetInt(dbResult, "SCALE") % 65536;
				this.precision = (JDBCUtils.safeGetInt(dbResult, "SCALE") - this.scale) / 65536;
			} else if (this.typeName.matches(typeTimestamp)) {
				this.precision = JDBCUtils.safeGetInt(dbResult, "SCALE") % 65536;
				this.maxLength = this.precision;
				this.scale = null;
			} else {
				this.precision = this.type.getPrecision();
				this.scale = this.type.getMaxScale();
			}
		}
		if (typeMod == DataTypeModifier.REF) {
			this.valueType = Types.REF;
		}
		if (typeNumeric.equals(this.typeName)) {
			setScale(this.scale);
			setPrecision(this.precision);
		} else {
			setScale(this.scale == null || this.scale == 0 ? null : this.scale);
			setPrecision(this.precision);
		}
	}


}
