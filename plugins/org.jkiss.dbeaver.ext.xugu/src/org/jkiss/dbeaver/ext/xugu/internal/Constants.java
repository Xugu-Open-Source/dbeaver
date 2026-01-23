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
package org.jkiss.dbeaver.ext.xugu.internal;

import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeType;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.Locale;
import java.util.ResourceBundle;
/**
 * 插件常量类
 */
public class Constants {
	
	
	public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.xugu";
	
	public static final String CMD_COMPILE = "org.jkiss.dbeaver.ext.xugu.code.compile";

	public static final int KEEP_ALIVE_INTERVAL_DEFAULT = 30;
	/**
	 * 连接类型枚举
	 */
	public enum ConnectionType {
		/**
		 * 基本类型
		 */

		BASIC,

		/**
		 * 自定义类型
		 */
		CUSTOM
	}
	
	/**
	 * 驱动中的类
	 * 驱动的 com.xugu.cloudjdbc.Connection#getObject
	 */
	public static final String XUGU_DBOBJECT_CLASS = "com.xugu.util.DBobject";
	public static final String XUGU_PATH_CLASS=  "class com.xugu.geometric.DBpath";
	public static final String XUGU_POLYGON_CLASS=  "class com.xugu.geometric.DBpolygon";
	public static final String XUGU_LINE_CLASS=  "class com.xugu.geometric.DBline";
	public static final String XUGU_LSEG_CLASS=  "class com.xugu.geometric.DBlseg";
	public static final String XUGU_BOX_CLASS=  "class com.xugu.geometric.DBbox";
	public static final String XUGU_BOX2D_CLASS=  "class com.xugugis.cloudjdbc.DBbox2d";
	public static final String XUGU_BOX3D_CLASS=  "class com.xugugis.cloudjdbc.DBbox3d";
	public static final String XUGU_POINT_CLASS=  "class com.xugu.geometric.DBpoint";
	public static final String XUGU_GEOGRAPHY_CLASS=  "class com.xugugis.cloudjdbc.DBgeography";
	public static final String XUGU_CIRCLE_CLASS=  "class com.xugu.geometric.DBcircle";
	public static final String XUGU_GEOMETRY_CLASS=  "class com.xugugis.cloudjdbc.DBgeometry";

	

	public static final int DEFAULT_PORT = 5138;
	public static final String DEFAULT_HOST = "localhost";
	public static final String SCHEMA_SYS = "SYS";
	public static final String VIEW_ALL_SOURCE = "ALL_SOURCE";
	public static final String VIEW_DBA_SOURCE = "DBA_SOURCE";
	public static final String VIEW_DBA_TAB_PRIVS = "DBA_TAB_PRIVS";

	public static final String[] SYSTEM_SCHEMAS = { "SYSDBA", "SYSAUDITOR", "SYSSSO", "GUEST" };
	/**
	 * 连接守护进程休眠时间默认1分钟
	 */
	public static final int DEFAULT_SLEEP_TIME = 60000;

	public static final String PROP_CONNECTION_TYPE = DBConstants.INTERNAL_PROP_PREFIX + "connection-type@";
	public static final String PROP_SERVER_TIMEZONE = DBConstants.INTERNAL_PROP_PREFIX + "serverTimezone@";

	public static final String[] TABLE_TYPES = new String[] { "TABLE", "VIEW" };

	public static final String TYPE_NAME_ENUM = "enum";
	public static final String TYPE_NAME_SET = "set";

	public static final String YES = "YES";

	public static final String COL_ID = "ID";
	public static final String COL_DEFAULT = "DEFAULT";
	public static final String COL_COMPILED = "COMPILED";
	public static final String COL_SORT_LENGTH = "SORTLEN";
	public static final String COL_COLLATION = "COLLATION";
	/**
	 * xfc 用户角色 SYSDBA DBA NORMAL
	 */
	public static final String PROP_INTERNAL_LOGON = "SYSDBA";
	public static final String PROP_USE_RULE_HINT = null;

	public static final int EC_FEATURE_NOT_SUPPORTED = 0;
	public static final String OS_AUTH_PROP = null;
	public static final String USER_PUBLIC = "GUEST";


	public static final String TYPE_NAME_JSON = "JSON";
	public static final String TYPE_NAME_XML = "XML";
	public static final String TYPE_NAME_XMLTYPE = "XMLTYPE";
	public static final String TYPE_NAME_BFILE = "BFILE";
	public static final String TYPE_NAME_TIMESTAMP = "TIMESTAMP";
	public static final String TYPE_NAME_GEOMETRY = "GEOMETRY";
	public static final String TYPE_NAME_BIT = "BIT";
	public static final String TYPE_NAME_VARBIT = "VARBIT";

	public static final DBSIndexType INDEX_TYPE_BTREE = new DBSIndexType("0", "BTree");
	public static final DBSIndexType INDEX_TYPE_RTREE = new DBSIndexType("1", "RTree");
/**public static final DBSIndexType INDEX_TYPE_FULL_TEXT = new DBSIndexType("2", "Full text");
 * 
 */
	public static final DBSIndexType INDEX_TYPE_BITMAP = new DBSIndexType("3", "Bitmap");

	public static final String COL_OWNER = "OWNER";
	public static final String COL_TABLE_NAME = "TABLE_NAME";
	public static final String COL_CONSTRAINT_NAME = "CONSTRAINT_NAME";
	public static final String COL_CONSTRAINT_TYPE = "CONSTRAINT_TYPE";

	public static final String PROP_OBJECT_DEFINITION = "objectDefinitionText";
	public static final String PROP_OBJECT_BODY_DEFINITION = "extendedDefinitionText";

	public static final String PREF_EXPLAIN_TABLE_NAME = OemConfig.OEM_NAME_EN_LOWER + ".explain.table";
	public static final String PREF_SUPPORT_ROWID = OemConfig.OEM_NAME_EN_LOWER + ".support.rowid";
	public static final String PREF_DBMS_OUTPUT = OemConfig.OEM_NAME_EN_LOWER + ".dbms.output";
	public static final String PREF_DBMS_READ_ALL_SYNONYMS = OemConfig.OEM_NAME_EN_LOWER + ".read.all.synonyms";
	public static final String PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING = OemConfig.OEM_NAME_EN_LOWER + ".disable.script.escape";
	public static final String PREF_KEY_DDL_FORMAT = OemConfig.OEM_NAME_EN_LOWER + ".ddl.format";

	/**
	 * 默认值约束
	 */
	public static final DBSEntityConstraintType CONSTRAINT_DEFAULT = new DBSEntityConstraintType(OemConfig.OEM_NAME_EN_LOWER + ".default",
			"DEFAULT", Messages.model_struct_default, false, false, false, false);

	/**
	 * 引用外键约束
	 */
	public static final DBSEntityConstraintType CONSTRAINT_REF_COLUMN = new DBSEntityConstraintType(OemConfig.OEM_NAME_EN_LOWER + ".ref.column",
			"Referential integrity", Messages.model_struct_ref_column, false, false, false, false);

	public static final int DATA_TYPE_TIMESTAMP_WITH_TIMEZONE = 101;
	public static final int DATA_TYPE_TIMESTAMP_WITH_LOCAL_TIMEZONE = 102;

	public static final String XMLTYPE_CLASS_NAME = "XMLType";
	public static final String[] DEFAULT_CHAR_SET = { "GBK", "GB2312", "UTF8" };
	public static final String DEF_PASSWORD_VALUE = "**********";
	public static final String DEF_UNTIL_TIME = "1970-1-1 07:00:00.933";

//	public static final String[] DEF_DATABASE_AUTHORITY_LIST = { "可创建任何数据库", "可修改任何数据库", "可删除任何数据库", "可创建任何模式",
//			"可修改任何模式", "可删除任何模式", "可创建任何表", "可修改任何表结构", "可删除任何表", "可引用任何表", "可查询任何表", "可插入记录，在任何表", "可删除记录，在任何表",
//			"可更新记录，在任何表", "可创建任何视图", "可修改任何视图结构", "可删除任何视图", "可查询任何视图", "可插入记录，在任何视图", "可删除记录，在任何视图", "可更新记录，在任何视图",
//			"可创建任何序列值", "可修改任何序列值", "可删除任何序列值", "可读任何序列值", "可更新任何序列值", "可引用任何序列值", "可创建任何包", "可修改任何包", "可删除任何包",
//			"可执行任何包", "可创建任何存储过程或函数", "可修改任何存储过程或函数", "可删除任何存储过程或函数", "可执行任何存储过程或函数", "可创建任何触发器", "可修改任何触发器",
//			"可删除任何触发器", "可创建任何索引", "可修改任何索引", "可删除任何索引", "可创建任何同义词", "可修改任何同义词", "可删除任何同义词", "可创建任何用户", "可修改任何用户",
//			"可删除任何用户", "可创建任何定时作业", "可修改任何定时作业", "可删除任何定时作业", "可创建任何角色", "可修改任何角色", "可删除任何角色", "可创建任何UDT", "可修改任何UDT",
//			"可删除任何UDT", "可创建表", "可创建视图", "可创建序列值", "可创建包", "可创建存储过程或函数", "可创建触发器", "可创建索引", "可创建同义词", "可创建UDT" };
//	public static final String[] DEF_TABLE_AUTHORITY_LIST = { "可修改表结构", "可删除表", "可引用表", "可查询表", "可插入记录，在表", "可删除记录，在表",
//	"可更新记录，在表" };

//public static final String[] DEF_VIEW_AUTHORITY_LIST = { "可修改视图结构", "可删除视图", "可读视图", "可插入记录，在视图", "可删除记录，在视图",
//	"可更新记录，在视图" };
//
//public static final String[] DEF_SEQUENCE_AUTHORITY_LIST = { "可修改序列值", "可删除序列值", "可读序列值", "可更新序列值", "可引用序列值" };
//
//public static final String[] DEF_PACKAGE_AUTHORITY_LIST = { "可修改包", "可删除包", "可执行包" };
//
//public static final String[] DEF_PROCEDURE_AUTHORITY_LIST = { "可修改存储过程或函数", "可删除存储过程或函数", "可执行存储过程或函数" };
//
//public static final String[] DEF_TRIGGER_AUTHORITY_LIST = { "可修改触发器", "可删除触发器" };
//
//public static final String[] DEF_COLUMN_AUTHORITY_LIST = { "可读列", "可更新列" };

public static final String[] DEF_OBJECT_TYPE_LIST = { "TABLE", "VIEW", "SEQUENCE", "TRIGGER", "PACKAGE",
	"PROCEDURE", "COLUMN" };

	public static String [] DEF_DATABASE_AUTHORITY_LIST = new String[68];
	public static String [] DEF_TABLE_AUTHORITY_LIST = new String[6];
	public static String [] DEF_VIEW_AUTHORITY_LIST = new String[5];
	public static String [] DEF_SEQUENCE_AUTHORITY_LIST = new String[5];
	public static String [] DEF_PACKAGE_AUTHORITY_LIST = new String[3];
	public static String [] DEF_PROCEDURE_AUTHORITY_LIST = new String[3];
	public static String [] DEF_TRIGGER_AUTHORITY_LIST = new String[2];
	public static String [] DEF_COLUMN_AUTHORITY_LIST = new String[2];
	
	static {
 
		ResourceBundle  resourceBundle = ResourceBundle.getBundle("org.jkiss.dbeaver.ext.xugu.internal.authority",Locale.CHINA);
		DEF_DATABASE_AUTHORITY_LIST[0] = resourceBundle.getString("can.create.any.database");
		DEF_DATABASE_AUTHORITY_LIST[1] = resourceBundle.getString("can.update.any.database");
		DEF_DATABASE_AUTHORITY_LIST[2] = resourceBundle.getString("can.drop.any.database");
		DEF_DATABASE_AUTHORITY_LIST[3] = resourceBundle.getString("can.create.any.schema");
		DEF_DATABASE_AUTHORITY_LIST[4] = resourceBundle.getString("can.update.any.schema");
		DEF_DATABASE_AUTHORITY_LIST[5] = resourceBundle.getString("can.drop.any.schema");
		DEF_DATABASE_AUTHORITY_LIST[6] = resourceBundle.getString("can.create.any.table");
		DEF_DATABASE_AUTHORITY_LIST[7] = resourceBundle.getString("can.update.any.table");
		DEF_DATABASE_AUTHORITY_LIST[8] = resourceBundle.getString("can.drop.any.table");
		DEF_DATABASE_AUTHORITY_LIST[9] = resourceBundle.getString("can.quote.any.table");
		DEF_DATABASE_AUTHORITY_LIST[10] = resourceBundle.getString("can.select.any.table");
		DEF_DATABASE_AUTHORITY_LIST[11] = resourceBundle.getString("can.insert.data.in.any.table");
		DEF_DATABASE_AUTHORITY_LIST[12] = resourceBundle.getString("can.drop.data.in.any.table");
		DEF_DATABASE_AUTHORITY_LIST[13] = resourceBundle.getString("can.update.data.in.any.table");
		DEF_DATABASE_AUTHORITY_LIST[14] = resourceBundle.getString("can.create.any.view");
		DEF_DATABASE_AUTHORITY_LIST[15] = resourceBundle.getString("can.update.any.view");
		DEF_DATABASE_AUTHORITY_LIST[16] = resourceBundle.getString("can.drop.any.view");
		DEF_DATABASE_AUTHORITY_LIST[17] = resourceBundle.getString("can.select.any.view");
		DEF_DATABASE_AUTHORITY_LIST[18] = resourceBundle.getString("can.insert.data.in.any.view");
		DEF_DATABASE_AUTHORITY_LIST[19] = resourceBundle.getString("can.drop.data.in.any.view");
		DEF_DATABASE_AUTHORITY_LIST[20] = resourceBundle.getString("can.update.data.in.any.view");
		DEF_DATABASE_AUTHORITY_LIST[21] = resourceBundle.getString("can.create.any.sequence");
		DEF_DATABASE_AUTHORITY_LIST[22] = resourceBundle.getString("can.update.any.sequence");
		DEF_DATABASE_AUTHORITY_LIST[23] = resourceBundle.getString("can.drop.any.sequence");
		DEF_DATABASE_AUTHORITY_LIST[24] = resourceBundle.getString("can.select.any.sequence");
		DEF_DATABASE_AUTHORITY_LIST[25] = resourceBundle.getString("can.update1.any.sequence");
		DEF_DATABASE_AUTHORITY_LIST[26] = resourceBundle.getString("can.quote.any.sequence");
		DEF_DATABASE_AUTHORITY_LIST[27] = resourceBundle.getString("can.create.any.package");
		DEF_DATABASE_AUTHORITY_LIST[28] = resourceBundle.getString("can.update.any.package");
		DEF_DATABASE_AUTHORITY_LIST[29] = resourceBundle.getString("can.drop.any.package");
		DEF_DATABASE_AUTHORITY_LIST[30] = resourceBundle.getString("can.excute.any.package");
		DEF_DATABASE_AUTHORITY_LIST[31] = resourceBundle.getString("can.create.any.procedure.or.function");
		DEF_DATABASE_AUTHORITY_LIST[32] = resourceBundle.getString("can.update.any.procedure.or.function");
		DEF_DATABASE_AUTHORITY_LIST[33] = resourceBundle.getString("can.drop.any.procedure.or.function");
		DEF_DATABASE_AUTHORITY_LIST[34] = resourceBundle.getString("can.excute.any.procedure.or.function");
		DEF_DATABASE_AUTHORITY_LIST[35] = resourceBundle.getString("can.create.any.trigger");
		DEF_DATABASE_AUTHORITY_LIST[36] = resourceBundle.getString("can.update.any.trigger");
		DEF_DATABASE_AUTHORITY_LIST[37] = resourceBundle.getString("can.drop.any.trigger");
		DEF_DATABASE_AUTHORITY_LIST[38] = resourceBundle.getString("can.create.any.index");
		DEF_DATABASE_AUTHORITY_LIST[39] = resourceBundle.getString("can.update.any.index");
		DEF_DATABASE_AUTHORITY_LIST[40] = resourceBundle.getString("can.drop.any.index");
		DEF_DATABASE_AUTHORITY_LIST[41] = resourceBundle.getString("can.create.any.synonym");
		DEF_DATABASE_AUTHORITY_LIST[42] = resourceBundle.getString("can.update.any.synonym");
		DEF_DATABASE_AUTHORITY_LIST[43] = resourceBundle.getString("can.drop.any.synonym");
		DEF_DATABASE_AUTHORITY_LIST[44] = resourceBundle.getString("can.create.any.user");
		DEF_DATABASE_AUTHORITY_LIST[45] = resourceBundle.getString("can.update.any.user");
		DEF_DATABASE_AUTHORITY_LIST[46] = resourceBundle.getString("can.drop.any.user");
		DEF_DATABASE_AUTHORITY_LIST[47] = resourceBundle.getString("can.create.any.job");
		DEF_DATABASE_AUTHORITY_LIST[48] = resourceBundle.getString("can.update.any.job");
		DEF_DATABASE_AUTHORITY_LIST[49] = resourceBundle.getString("can.drop.any.job");
		DEF_DATABASE_AUTHORITY_LIST[50] = resourceBundle.getString("can.create.any.role");
		DEF_DATABASE_AUTHORITY_LIST[51] = resourceBundle.getString("can.update.any.role");
		DEF_DATABASE_AUTHORITY_LIST[52] = resourceBundle.getString("can.drop.any.role");
		DEF_DATABASE_AUTHORITY_LIST[53] = resourceBundle.getString("can.create.any.udt");
		DEF_DATABASE_AUTHORITY_LIST[54] = resourceBundle.getString("can.update.any.udt");
		DEF_DATABASE_AUTHORITY_LIST[55] = resourceBundle.getString("can.drop.any.udt");
		DEF_DATABASE_AUTHORITY_LIST[56] = resourceBundle.getString("can.create.table");
		DEF_DATABASE_AUTHORITY_LIST[57] = resourceBundle.getString("can.create.view");
		DEF_DATABASE_AUTHORITY_LIST[58] = resourceBundle.getString("can.create.sequence");
		DEF_DATABASE_AUTHORITY_LIST[59] = resourceBundle.getString("can.create.package");
		DEF_DATABASE_AUTHORITY_LIST[60] = resourceBundle.getString("can.create.procedure.or.function");
		DEF_DATABASE_AUTHORITY_LIST[61] = resourceBundle.getString("can.create.trigger");
		DEF_DATABASE_AUTHORITY_LIST[62] = resourceBundle.getString("can.create.index");
		DEF_DATABASE_AUTHORITY_LIST[63] = resourceBundle.getString("can.create.synonym");
		DEF_DATABASE_AUTHORITY_LIST[64] = resourceBundle.getString("can.create.udt");
		DEF_DATABASE_AUTHORITY_LIST[65] = resourceBundle.getString("can.create.any.udt");
		DEF_DATABASE_AUTHORITY_LIST[66] = resourceBundle.getString("can.create.any.udt");
		DEF_DATABASE_AUTHORITY_LIST[67] = resourceBundle.getString("can.create.any.udt");
		
		DEF_TABLE_AUTHORITY_LIST[0] = resourceBundle.getString("can.update.table");
		DEF_TABLE_AUTHORITY_LIST[1] = resourceBundle.getString("can.drop.table");
		DEF_TABLE_AUTHORITY_LIST[2] = resourceBundle.getString("can.quote.table");
		DEF_TABLE_AUTHORITY_LIST[3] = resourceBundle.getString("can.select.table");
		DEF_TABLE_AUTHORITY_LIST[4] = resourceBundle.getString("can.insert.data.in.table");
		DEF_TABLE_AUTHORITY_LIST[5] = resourceBundle.getString("can.drop.data.in.table");
		
		DEF_VIEW_AUTHORITY_LIST[0] = resourceBundle.getString("can.update.view");
		DEF_VIEW_AUTHORITY_LIST[1] = resourceBundle.getString("can.drop.view");
		DEF_VIEW_AUTHORITY_LIST[2] = resourceBundle.getString("can.select.view");
		DEF_VIEW_AUTHORITY_LIST[3] = resourceBundle.getString("can.insert.data.view");
		DEF_VIEW_AUTHORITY_LIST[4] = resourceBundle.getString("can.drop.data.in.view");
		
		
		DEF_SEQUENCE_AUTHORITY_LIST[0] = resourceBundle.getString("can.update.sequence");
		DEF_SEQUENCE_AUTHORITY_LIST[1] = resourceBundle.getString("can.drop.sequence");
		DEF_SEQUENCE_AUTHORITY_LIST[2] = resourceBundle.getString("can.select.sequence");
		DEF_SEQUENCE_AUTHORITY_LIST[3] = resourceBundle.getString("can.update1.sequence");
		DEF_SEQUENCE_AUTHORITY_LIST[4] = resourceBundle.getString("can.quote.sequence");
		
		
		DEF_PACKAGE_AUTHORITY_LIST[0] = resourceBundle.getString("can.update.package");
		DEF_PACKAGE_AUTHORITY_LIST[1] = resourceBundle.getString("can.drop.package");
		DEF_PACKAGE_AUTHORITY_LIST[2] = resourceBundle.getString("can.execute.package");
		
		DEF_PROCEDURE_AUTHORITY_LIST[0] = resourceBundle.getString("can.update.procedure.or.function");
		DEF_PROCEDURE_AUTHORITY_LIST[1] = resourceBundle.getString("can.drop.procedure.or.function");
		DEF_PROCEDURE_AUTHORITY_LIST[2] = resourceBundle.getString("can.execute.procedure.or.function");
		
		DEF_TRIGGER_AUTHORITY_LIST[0] = resourceBundle.getString("can.update.trigger");
		DEF_TRIGGER_AUTHORITY_LIST[1] = resourceBundle.getString("can.drop.trigger");
		
		DEF_COLUMN_AUTHORITY_LIST[0] = resourceBundle.getString("can.select.column");
		DEF_COLUMN_AUTHORITY_LIST[1] = resourceBundle.getString("can.update.column");
		
		
	}
	
	
	
	
	
	
//	public static final String [] DEF_DATABASE_AUTHORITY_LIST = {"can create any database","can update any database","can drop any database","can create any schema",
//			"can update any schema","can drop any schema","can create any table","can update any table","can drop any table","can quote any table","can select any table",
//			"can insert data in any table","can drop data in any table","can update data in any table","can create any view","can update any view","can drop any view",
//			"can select any view","can insert data in any view","can drop data in any view","can update data in any view","can create any sequence","can update any sequence",
//			"can drop any sequence","can drop any sequence","can select any sequence","can update any sequence","can quote any sequence","can create any package",
//			"can update any package","can drop any package","can excute any package","can create any procedure or function","can update any procedure or function",
//			"can drop any procedure or function","can excute any procedure or function","can create any trigger","can update any trigger","can drop any trigger",
//			"can create any index","can update any index","can drop any index","can create any synonym","can update any synonym","can drop any synonym","can create any user",
//			"can update any user","can create any job","can update any job","can drop any job","can create any role","can update any role","can drop any role",
//			"can create any udt","can update any udt","can drop any udt","can create table","can create view","can create sequence","can create package",
//			"can create procedure or function","can create trigger","can create index","can create synonym","can create udt"};
	
	public static final DBDPseudoAttribute PSEUDO_ATTR_ROWID = new DBDPseudoAttribute(
	        DBDPseudoAttributeType.ROWID,
	        "ROWID",
	        "ROWID",
	        null,
	        "Unique row identifier",
	        true,
			DBDPseudoAttribute.PropagationPolicy.TABLE_LOCAL
	);


    public static final String XG_ARRAY_CLASS = "com.xugu.cloudjdbc.Array";
}
