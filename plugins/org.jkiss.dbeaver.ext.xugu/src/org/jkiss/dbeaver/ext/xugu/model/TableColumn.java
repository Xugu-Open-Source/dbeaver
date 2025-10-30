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

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPHiddenObject;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.runtime.DBWorkbench;

/**
 * 表字段信息类，包含字段相关的基本信息
 */
public class TableColumn extends JDBCTableColumn<BaseTable>
		implements DBSTableColumn, DBSTypedObjectEx, DBPHiddenObject, DBPNamedObject2 {
	private static final Log log = Log.getLog(TableColumn.class);

	private DataType type;
	private DataTypeModifier typeMod;
	private String comment;
	private boolean hidden;

	private int dbId;
	private int tableId;
	private boolean varying;
	private boolean isSerial;
	private int serialId;
	private String timeStampT;
	private String collator;
	private String colHistory;
	private int domainId;
	private boolean deleted;
	private boolean isVirtual;
	private double repetRate;
	private double dispersion;
	private String maxVal;
	private String minVal;

	private int colNo;
	private String colName;
	private boolean notNull;
	
	/**
	 * 自增属性
	 */
	private Boolean isIdenBoolean;
	private Integer minInteger;
//	private Integer maxInteger;
	private Integer stepInteger;
	
	/**
	 * 离散采样
	 */
	private Integer samplingInterval;

	public TableColumn(BaseTable table) {
		super(table, false);
	}

	public TableColumn(DBRProgressMonitor monitor, BaseTable table, ResultSet dbResult) throws DBException {
		super(table, true);
		// Read default value first because it is of LONG type and has to be read before others
		// 根据表和视图区分要获取的字段
		if (dbResult != null) {
			// type=0时为表 type=1时为视图
			if (table.getType().getTypeName().equals(ObjectType.TABLE.getTypeName())) {
				this.dbId = JDBCUtils.safeGetInt(dbResult, "DB_ID");
				this.tableId = JDBCUtils.safeGetInt(dbResult, "TABLE_ID");
				this.colNo = JDBCUtils.safeGetInt(dbResult, "COL_NO");
				this.colName = JDBCUtils.safeGetString(dbResult, "COL_NAME");
				this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
				this.varying = JDBCUtils.safeGetBoolean(dbResult, "VARYING");
				this.notNull = JDBCUtils.safeGetBoolean(dbResult, "NOT_NULL");
				this.isSerial = JDBCUtils.safeGetBoolean(dbResult, "IS_SERIAL");
				this.serialId = JDBCUtils.safeGetInt(dbResult, "SERIAL_ID");
				this.timeStampT = JDBCUtils.safeGetString(dbResult, "TIMESTAMP_T");
				this.collator = JDBCUtils.safeGetString(dbResult, "COLLATOR");
				this.setDefaultValue(JDBCUtils.safeGetString(dbResult, "DEF_VAL"));
				this.domainId = JDBCUtils.safeGetInt(dbResult, "DOMAIN_ID");
				this.deleted = JDBCUtils.safeGetBoolean(dbResult, "DELETED");
				this.isVirtual = JDBCUtils.safeGetBoolean(dbResult, "IS_VIRTUAL");
				this.comment = JDBCUtils.safeGetString(dbResult, "COMMENTS");
				this.repetRate = JDBCUtils.safeGetDouble(dbResult, "REPET_RATE");
				this.dispersion = JDBCUtils.safeGetDouble(dbResult, "DISPERSION");
				this.maxVal = JDBCUtils.safeGetString(dbResult, "MAX_VAL");
				this.minVal = JDBCUtils.safeGetString(dbResult, "MIN_VAL");
				this.samplingInterval = null;
				this.minInteger = JDBCUtils.safeGetInteger(dbResult, "min");
//				this.maxInteger = JDBCUtils.safeGetInteger(dbResult, "max");
				this.stepInteger = JDBCUtils.safeGetInteger(dbResult, "step");
				if(this.stepInteger!=null) {
					this.isIdenBoolean = true;
				}else{
					this.isIdenBoolean = false;
				}
				setName(this.colName);
				setOrdinalPosition(this.colNo);
			}
			else {
				this.dbId = JDBCUtils.safeGetInt(dbResult, "DB_ID");
				this.tableId = JDBCUtils.safeGetInt(dbResult, "VIEW_ID");
				this.colNo = JDBCUtils.safeGetInt(dbResult, "COL_NO");
				this.colName = JDBCUtils.safeGetString(dbResult, "COL_NAME");
				this.timeStampT = JDBCUtils.safeGetString(dbResult, "TIMESTAMP_T");
				this.typeName = JDBCUtils.safeGetString(dbResult, "TYPE_NAME");
				this.varying = JDBCUtils.safeGetBoolean(dbResult, "VARYING");
				setName(this.colName);
				setOrdinalPosition(this.colNo);
			}
			// 对数据类型、精度标度进行统一处理
			final String typeDatetime = "DATETIME";
			final String typeChar = "CHAR";
			final String typeNumeric = "NUMERIC";
			final String typeVarchar = "VARCHAR";
			final String typeIntervalRegex = "INTERVAL(.*)";
			final String typeTimestamp = "TIMESTAMP";
            final String typeBit = "BIT";
            final String typeVarBit = "VARBIT";

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
			this.type = (DataType) table.getDataSource().getLocalDataType(this.typeName);

			if (this.type != null) {
				this.typeName = type.getFullyQualifiedName(DBPEvaluationContext.DDL);
				this.valueType = type.getTypeID();
				if (typeNumeric.equals(this.typeName)) {
					this.setScale(JDBCUtils.safeGetInt(dbResult, "SCALE") % 65536);
					this.setPrecision((JDBCUtils.safeGetInt(dbResult, "SCALE") - this.scale) / 65536);
					this.maxLength = this.precision;
				} else if (typeChar.equals(this.typeName) || typeVarchar.equals(this.typeName)
                    || typeBit.equals(this.typeName) || typeVarBit.equalsIgnoreCase(this.typeName)) {
					this.setPrecision(JDBCUtils.safeGetInt(dbResult, "SCALE"));
					this.setScale(null);
				} else if (this.typeName.matches(typeIntervalRegex)) {
					this.setScale(JDBCUtils.safeGetInt(dbResult, "SCALE") % 65536);
					this.setPrecision((JDBCUtils.safeGetInt(dbResult, "SCALE") - this.scale) / 65536);
				} else if (this.typeName.matches(typeTimestamp)) {
					this.setPrecision(JDBCUtils.safeGetInt(dbResult, "SCALE") % 65536);
					this.maxLength = this.precision;
					this.setScale(null);
				} else {
					this.setPrecision(this.type.getPrecision());
					this.setScale(this.type.getMaxScale());
				}
			}
			if (typeMod == DataTypeModifier.REF) {
				this.valueType = Types.REF;
			}
			// 设置非空
			setRequired(this.notNull);
			if (typeNumeric.equals(this.typeName)) {
				setScale(this.scale);
				setPrecision(this.precision);
			} else {
				setScale(this.scale == 0 ? null : this.scale);
				setPrecision(this.precision);
			}
		}
	}

	public TableColumn(DBRProgressMonitor monitor, BaseTable table, DBSEntityAttribute source) {
		super(table, source, false);
        TableColumn sourceColumn = (TableColumn) source;
		this.colName = sourceColumn.getName();
		this.type = sourceColumn.getDataType();
		this.setPrecision(sourceColumn.getPrecision());
		this.setScale(sourceColumn.getScale());
		this.notNull = sourceColumn.notNull;
		this.setDefaultValue(sourceColumn.getDefaultValue());
		this.samplingInterval = sourceColumn.getSamplingInterval();
		this.autoGenerated = sourceColumn.isAutoGenerated();
		this.minInteger = sourceColumn.getMinInteger();
		this.stepInteger = sourceColumn.getStepInteger();
		this.comment = sourceColumn.getDescription();
	}

	@NotNull
	@Override
	public DataSource getDataSource() {
		return getTable().getDataSource();
	}

	@Nullable
	@Override
	@Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = ColumnDataTypeListProvider.class)
	public DataType getDataType() {
		return type;
	}

	public void setDataType(DataType type) {
		this.type = type;
		this.typeName = type == null ? "" : type.getFullyQualifiedName(DBPEvaluationContext.DDL);
	}

	@Property(viewable = true, order = 30)
	public DataTypeModifier getTypeMod() {
		return typeMod;
	}

	@Override
	public String getTypeName() {
		return super.getTypeName();
	}

	@Property(viewable = false, editable = false, updatable = false, order = 40, hidden = true)
	@Override
	public long getMaxLength() {
		return super.getMaxLength();
	}

	@Override
	@Property(viewable = true, editable = true, updatable = true, order = 41)
	public Integer getPrecision() {
		return super.getPrecision();
	}

	@Override
	@Property(viewable = true, editable = true, updatable = true, order = 42)
	public Integer getScale() {
		return super.getScale();
	}

	@Property(viewable = true, editable = true, updatable = true, order = 50)
	@Override
	public boolean isRequired() {
		return super.isRequired();
	}
	

	@Property(viewable = true, editable = true, updatable = true, order = 70)
	@Override
	public String getDefaultValue() {
		return super.getDefaultValue();
	}

	@Property(viewable = true, order = 71)
	public double getRepetRate() {
		return repetRate;
	}
	
	@Property(viewable = true, order = 72)
	public double getDispersion() {
		return dispersion;
	}
	
	@Property(viewable = true, order = 73)
	public String getMaxVal() {
		return maxVal;
	}

	@Property(viewable = true, order = 74)
	public void setMaxVal(String maxVal) {
		this.maxVal = maxVal;
	}

	@Property(viewable = true, order = 75)
	public String getMinVal() {
		return minVal;
	}
		
	@Property(viewable = true, editable = true, updatable = true, order = 76)
	public Integer getSamplingInterval() {
		return samplingInterval;
	}
	
	@Override
	public boolean isAutoGenerated() {
		return false;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 77)
	public Boolean getIsIdenBoolean() {
		return isIdenBoolean;
	}

	public void setIsIdenBoolean(Boolean isIdenBoolean) {
		this.isIdenBoolean = isIdenBoolean;
	}

	@Property(viewable = true, editable = true, updatable = true, order = 78)
	public Integer getMinInteger() {
		return minInteger;
	}

	public void setMinInteger(Integer minInteger) {
		this.minInteger = minInteger;
	}

//	@Property(viewable = false, editable = true, updatable = true, order = 79)
//	public Integer getMaxInteger() {
//		return maxInteger;
//	}

//	public void setMaxInteger(Integer maxInteger) {
//		this.maxInteger = maxInteger;
//	}

	@Property(viewable = true, editable = true, updatable = true, order = 80)
	public Integer getStepInteger() {
		return stepInteger;
	}

	public void setStepInteger(Integer stepInteger) {
		this.stepInteger = stepInteger;
	}

	public static class CommentLoadValidator implements IPropertyCacheValidator<TableColumn> {
		@Override
		public boolean isPropertyCached(TableColumn object, Object propertyId) {
			return object.comment != null;
		}
	}

	@Property(viewable = true, editable = true, updatable = true, order = 100)
	@LazyProperty(cacheValidator = CommentLoadValidator.class)
	public String getComment(DBRProgressMonitor monitor) {
		if (isPersisted() && comment == null && !DBWorkbench.getPlatform().isUnitTestMode()) {
			// 加载表中所有列注释
			getTable().loadColumnComments(monitor);
		}
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	void cacheComment() {
		if (this.comment == null) {
			this.comment = "";
		}
	}

	@Nullable
	@Override
	public String getDescription() {
		return comment;
	}

	@Override
	public boolean isHidden() {
		return hidden;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}



	public void setRepetRate(double repetRate) {
		this.repetRate = repetRate;
	}


	public void setDispersion(double dispersion) {
		this.dispersion = dispersion;
	}

	public void setMinVal(String minVal) {
		this.minVal = minVal;
	}
	
	public void setSamplingInterval(Integer samplingInterval) {
		this.samplingInterval = samplingInterval;
	}

	public static class ColumnDataTypeListProvider implements IPropertyValueListProvider<TableColumn> {
		@Override
		public boolean allowCustomValue() {
			return false;
		}

		@Override
		public Object[] getPossibleValues(TableColumn column) {
			List<DBSDataType> dataTypes = new ArrayList<>(column.getTable().getDataSource().getLocalDataTypes());

			if (!dataTypes.contains(column.getDataType())) {
				DataType t = column.getDataType();
				if (t != null) {
					dataTypes.add(column.getDataType());
				}
			}
			Collections.sort(dataTypes, DBUtils.nameComparator());
			return dataTypes.toArray(new DBSDataType[dataTypes.size()]);
		}
	}
}
