/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.xugu.internal.xugu.parser;


import org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata.Constants.DatabaseObjectType;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata.Constants.PartitionType;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata.IndexMeta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;


/**
 * @author
 * 该类主要为对象级（指定对象名），模式级（指定具体模式），用户级（指定用户）的对象ddl语句的收集对象方法与组装语句方法。
 * 登录库为当前用户库
 */

@SuppressWarnings("unchecked")
public class ObjectParsing {

    /**
     * 日志
     */
//    private Logger log = LoggerFactory.getLogger(DatabaseParsing.class);

    static final String MARK_SIN_QUOTATION = "'";

    static final String MARK_DUB_QUOTATION = "\"";

    static final String MARK_MYSQL_QUOTATION = "`";

    static final String MARK_BGN_CURVES = "(";

    static final String MARK_END_CURVES = ")";

    static final String MARK_DOT = ".";

    static final String MARK_COMMA = ",";

    static final String SEMICOLON = ";";

    static final String MARK_WRAP = "\r\n";

    protected LinkedHashMap<String, Vector<Object>> taskObject;

     private  static   ObjectParsing objectParsing ;

    /**
     * 初始化任务队列
     * @return void
     */
    protected synchronized void initSearchTaskObject() {
        if (this.taskObject != null) {
            taskObject.clear();
        } else {
            taskObject = new LinkedHashMap<String, Vector<Object>>();
        }
    }

    /**
     * 添加任务对象到任务队列。
     *
     * @param key
     * @param obj
     */
    protected synchronized void addSearchTaskObject(String key, Vector<Object> obj) {
        if (this.taskObject == null) {
            taskObject = new LinkedHashMap<String, Vector<Object>>();
        }
        if (obj == null || obj.size() != 19) {
            return;
        }
        this.taskObject.put(key, obj);
    }

    /**
     * 获取已有的任务对象
     *
     * @return
     */
    public synchronized LinkedHashMap<String, Vector<Object>> getTaskObject() {
        if (this.taskObject == null) {
            this.taskObject = new LinkedHashMap<String, Vector<Object>>();
        }
        return taskObject;
    }

    /**
     * 获取数据库对象条件使用串(单字段)
     * 返回为将String对象里存在单引号的地方替换为两个单引号 并在对象前后加上单引号。
     */
    public static String quotaDatabaseObjectSin(Object objectName) {
        return MARK_SIN_QUOTATION + objectName.toString().replace(MARK_SIN_QUOTATION, MARK_SIN_QUOTATION + MARK_SIN_QUOTATION) + MARK_SIN_QUOTATION;
    }

    /**
     * 去除对象中的单引号
     * @param objectName
     * @return
     */
    public static String quotaDatabaseObjectSin1(Object objectName) {
        return objectName.toString().replace(MARK_SIN_QUOTATION, "");
    }

    /**
     * 获取数据库对象包裹串
     * 去除schemaName,objectname里的引号 返回前后加上的双引号schemaName.objectName 对象。
     */
    public static String quotaDatabaseObjectDub(Object schemaName, Object objectName) {
        return removeDatabaseObjectQuota(schemaName.toString()) + "." + removeDatabaseObjectQuota(objectName.toString());
    }

    /**
     * 获取数据库对象包裹串
     * 去除objectName的引号 返回前后加上双引号的字符串
     */
    public static String quotaDatabaseObjectDub(Object objectName) {
        return MARK_DUB_QUOTATION + removeDatabaseObjectQuota(objectName.toString()) + MARK_DUB_QUOTATION;
    }

    /**
     * 去除单引号，双引号
     * 返回为：将string对象里的引号去除后的字符串
     */
    public static String removeDatabaseObjectQuota(Object objectString) {
        if (objectString == null) {
            return null;
        }
        return objectString.toString().replace(MARK_DUB_QUOTATION, "").replace(MARK_SIN_QUOTATION, "");
    }

    /**
     * 只除去双引号
     *
     * @param objectString
     * @return
     */
    public static String removeDatabaseObjectQuota1(Object objectString) {
        if (objectString == null) {
            return null;
        }
        return objectString.toString().replace(MARK_DUB_QUOTATION, "");
    }

    /**
     * 只除去单引号
     *
     * @param objectString
     * @return
     */
    public static String removeDatabaseObjectQuota2(Object objectString) {
        if (objectString == null) {
            return null;
        }
        return objectString.toString().replace(MARK_SIN_QUOTATION, "");
    }

    /**
     * 获取表对应的表分区类型
     */
    public static PartitionType getTabPartitionType(Object partition) {
        if (partition == null) {
            return PartitionType.unknown;
        }
        if (partition.toString().toLowerCase().contains("range")) {
            return PartitionType.range;
        } else if (partition.toString().toLowerCase().contains("list")) {
            return PartitionType.list;
        } else if (partition.toString().toLowerCase().contains("hash")) {
            return PartitionType.hash;
        } else {
            return PartitionType.unknown;
        }
    }


//    /**
//     * 获取源数据库对应表自增信息
//     * @param from_Connection
//     * @param db_id
//     * @param parameters
//     * @param table_type
//     * @return
//     */
//    public Vector<Vector<Object>> CollectTableIdentity( com.xugu.cloudjdbc.Connection from_Connection,Integer db_id,  Object[] parameters,String table_type) {
//        Vector<Object> columnIdentity_row = null;
//        Vector<Vector<Object>> identity = new Vector<Vector<Object>>();
//        MessageFormat messageFormat = null;
//        String primaryKeyCol = "";
//        ResultSet rs;
//        String sql;
//        Statement stmt;
//        parameters = new String[]{parameters[0].toString(), parameters[1].toString()};
//        try {
//            /*************************************************获取主键列名************************************************/
//            // 解决主键与自增字段共生的情况,获取表主键信息
//            rs = from_Connection.getMetaData().getPrimaryKeys(from_Connection.getCatalog(), removeDatabaseObjectQuota(parameters[0]), removeDatabaseObjectQuota(parameters[1]));
//            while (rs.next()) {
//                //可能是多字段主键(自增仅为单字段，用于判断共生)
//                primaryKeyCol += rs.getString(4);
//            }
//            rs.close();
//            /**********************************************************************************************************/
//
//            messageFormat = new MessageFormat("select s.schema_name,t.table_name,c.col_name,c.type_name,q.min_val,q.max_val,q.curr_val,q.step_val,q.is_cycle as iscycle "
//                    + "from user_schemas s join user_tables t on s.schema_id=t.schema_id and s.db_id=t.db_id join user_columns c on c.table_id = t.table_id and c.db_id=t.db_id "
//                    + "join user_sequences q on q.seq_id=c.serial_id and q.db_id=c.db_id where s.schema_name={0} and t.table_name={1} and c.is_serial = ''T'' and c.db_id=current_db_id");
//            sql = messageFormat.format(parameters);
//            stmt = from_Connection.createStatement();
//            rs = stmt.executeQuery(sql);
//
//            while (rs.next()) {
//                columnIdentity_row = new Vector<Object>();
//                // 表模式
//                columnIdentity_row.add(rs.getString(1));
//                // 表名称
//                columnIdentity_row.add(rs.getString(2));
//                // 表列名称
//                columnIdentity_row.add(rs.getString(3));
//                // 表列数据类型
//                columnIdentity_row.add(rs.getString(4));
//                // 表列自增起始值
//                columnIdentity_row.add(rs.getString(5));
//                // 表列自增最大值
//                columnIdentity_row.add(rs.getString(6));
//                // 表列自增当前值
//                columnIdentity_row.add(rs.getString(7));
//                // 表列自增步长
//                columnIdentity_row.add(rs.getString(8));
//                // 自增是否循环Cycle
//                columnIdentity_row.add(rs.getBoolean(9));
//                // 是否为主键
//                columnIdentity_row.add(rs.getString(3).equals(primaryKeyCol) ? true : false);
//                identity.add(columnIdentity_row);
//            }
//            rs.close();
//            stmt.close();
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//        }
//        /* **************************************************************************************** */
//        return identity;
//    }
//
//
//    /**
//     *  获取源数据库对应表索引信息
//     * @param from_Connection
//     * @param db_id
//     * @param parameters
//     * @param table_type
//     * @return
//     */
//    public Vector<DDL_INDEX_CREATE> CollectTableIndex(com.xugu.cloudjdbc.Connection from_Connection, Integer db_id, String[] parameters,String table_type) {
//        Vector<DDL_INDEX_CREATE> indexs = new Vector<DDL_INDEX_CREATE>();
//        ResultSet rs = null;
//        String sql;
//        String primaryKey = "";
//        Statement stmt;
//        try {
//            DatabaseMetaData dbmd = from_Connection.getMetaData();
//
//            /*************************************************获取主键约束名***********************************************/
//            rs = dbmd.getPrimaryKeys(from_Connection.getCatalog(), removeDatabaseObjectQuota(parameters[0]), removeDatabaseObjectQuota(parameters[1]));  //获取表主键信息
//            while (rs.next()) {
//                primaryKey = rs.getString(6);
//            }
//            rs.close();
//            /**********************************************************************************************************/
//            /*************************************************获取索引信息************************************************/
//            MessageFormat messageFormat = null;
//            messageFormat = new MessageFormat("select i.index_name,i.index_type,i.field_num,i.keys,i.is_primary,i.is_unique,i.create_time "
//                    + "from user_indexes i,user_schemas s,user_tables t where i.table_id=t.table_id and t.schema_id=s.schema_id and s.schema_name={0} and t.table_name={1} "
//                    + "and s.db_id=current_db_id and t.db_id=current_db_id and i.db_id=current_db_id order by i.index_name asc");
//            sql = messageFormat.format(parameters);
//            stmt =  from_Connection.createStatement();
//            rs = stmt.executeQuery(sql);
//            while (rs.next()) {
//                //去除无效索引及主键索引
//                if (rs.getString(1) == null || rs.getString(6).equalsIgnoreCase(primaryKey)) {
//                    continue;
//                }
//                indexs.add(CollectTableIndexPartition(from_Connection, new String[]{parameters[0], parameters[1], QuotaDatabaseObjectSin(rs.getString(1))},table_type));
//            }
//            rs.close();
//            /**********************************************************************************************************/
//        } catch (SQLException e) {
//            // TODO Auto-generated catch block
//
//        }
//        return indexs;
//    }
//

    /**
     * 收集源数据库对象信息
     *
     * @param fromConnection
     * @param object
     * @param databaseObjectType
     * @param tableType
     */
    public void collectDatabaseObject(Connection fromConnection, String[] object, DatabaseObjectType databaseObjectType, Parsing.TableType tableType) {
        /**
         * 用于分割文本文件
         * 1.文件编码方式 2.列分割符3.行分隔符4.第一行是否为列名
         */
        String [] midFileRule = {"GBK", "垂直条{|}", "{CR}{LF}", "TRUE"};
        Vector<Object> objectRow = null;
        Vector<Object> fileSplit = null;
        String[] parameters = new String[]{
                quotaDatabaseObjectSin(object[0]),
                quotaDatabaseObjectSin(object[1]),
                quotaDatabaseObjectSin(object[0]),
                quotaDatabaseObjectSin(object[1])};

        objectRow = new Vector<Object>();
//		objectRow.add(databaseObjectType);                                           // 类型
        //源对象
        objectRow.add(XuguDDLUtils.schemaAndTable(quotaDatabaseObjectDub(object[0], object[1])));
        //目的对象
        objectRow.add(XuguDDLUtils.schemaAndTable(quotaDatabaseObjectDub(object[0], object[1])));
        // 转换为目的数据库表对象后的表列信息
        objectRow.add(fileSplit);
        // 源表列信息
        objectRow.add(collectTableColumn(fromConnection, parameters, tableType));
        // 约束：主键，外键，唯一值，值检查
        objectRow.add(collectTableConstraint(fromConnection, parameters, tableType));
        // 表分区
        objectRow.add(collectTablePartition(fromConnection, parameters, tableType));
        // 表自增
        objectRow.add(collectTableIdentity(fromConnection, parameters, tableType));
        // 索引
        objectRow.add(collectTableIndex(fromConnection, parameters, tableType));
        //视图
        objectRow.add(collectView(fromConnection, parameters, tableType));
        //序列值
        objectRow.add(collectSequence(fromConnection, parameters, tableType));
        //包
        objectRow.add(collectPackage(fromConnection, parameters, tableType));
        //存储过程
        objectRow.add(collectProcedure(fromConnection, parameters, tableType));
        //存储函数
        objectRow.add(collectFunction(fromConnection, parameters, tableType));
        //触发器
        objectRow.add(collectTrigger(fromConnection, parameters, tableType));
        // 同义词
        objectRow.add(collectSynonym(fromConnection, parameters, tableType));
        //自定义数据类型
        objectRow.add(collectUdt(fromConnection, parameters, tableType));
        //定时作业
        objectRow.add(collectJob(fromConnection, parameters, tableType));
        //表类型
        objectRow.add(collectTableType(fromConnection, parameters, tableType));
        // 副本数 copy number
        objectRow.add(collectTableCopyNumber(fromConnection, parameters, tableType));
        //添加到任务对象列表
        addSearchTaskObject(quotaDatabaseObjectDub(object[0], object[1]), objectRow);

    }

    /**
     * 获取源数据库表副本数信息
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectTableCopyNumber(Connection fromConnection, String[] parameters, Parsing.TableType tableType) {
        Vector<Vector<Object>> vectors = new Vector<Vector<Object>>();
        Vector<Object> vector;
        ResultSet rsm = null;
        StringBuilder sqlBuilder;
        Statement sta = null;
        try {
            sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT COPY_NUM FROM ");
            sqlBuilder.append(tableType+"_TABLES xt"); //  拼接权限类型
            sqlBuilder.append(" JOIN ");
            sqlBuilder.append(tableType+"_SCHEMAS xs");
            sqlBuilder.append(" ON ");
            sqlBuilder.append(" xt.SCHEMA_ID = xs.SCHEMA_ID");
            sqlBuilder.append(" WHERE ");
            sqlBuilder.append(" SCHEMA_NAME = " + parameters[0]);
            sqlBuilder.append(" AND ");
            sqlBuilder.append(" TABLE_NAME = " + parameters[1]);
            sta = fromConnection.createStatement();
            rsm = sta.executeQuery(sqlBuilder.toString());
            while (rsm.next()) {
                vector = new Vector<Object>();
                vector.add(rsm.getInt("COPY_NUM"));
                vectors.add(vector);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            if (sta != null){
                try {
                    sta.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (rsm != null){
                try {
                    rsm.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return vectors;
    }

    /**
     * 获取源数据库对应表列信息
     *
     * @param parameters
     * @return
     */
    public Vector<Vector<Object>> collectTableColumn(Connection fromConnection, String[] parameters, Parsing.TableType tableType) {
        Vector<Vector<Object>> colSource = new Vector<Vector<Object>>();
        Vector<Object> colSrcRow;
        ResultSet rs = null;
        MessageFormat messageFormat = null;
        ResultSet rsm = null;
        String sql;
        Statement stmt = null;
        Statement sta = null;
        try {
            DatabaseMetaData dbmd = fromConnection.getMetaData();
            // **********************************************源数据库表COMMENT信息**********************************************

//            rs = dbmd.getTables(null, removeDatabaseObjectQuota(parameters[0]), removeDatabaseObjectQuota(parameters[1]), new String[]{"TABLE", "GLOBAL TEMPORARY"});
//            String tableComment = null;
//            if (rs.next()) {
//                tableComment = rs.getString(5) == null ? "" : rs.getString(5);
//            }
//            rs.close();

            String tableComment = null;
            messageFormat = new MessageFormat("select t.comments from " + tableType + "_tables t," + tableType + "_schemas s where  s.schema_id = t.schema_id and s.schema_name={0} and t.table_name={1}");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                tableComment = rs.getString(1) == null ? "" : rs.getString(1);
            }
            if(rs!=null){
                rs.close();
            }
            if(stmt!=null){
                stmt.close();
            }
            // **************************************************************************************************************

            // **********************************************源数据库表列信息****************************************************
            rs = dbmd.getColumns(null, removeDatabaseObjectQuota(parameters[0]), removeDatabaseObjectQuota(parameters[1]), null);
            String sqlStr = "select distinct col_name,type_name,\"VARYING\",NOT_NULL,IS_SERIAL,TIMESTAMP_T,DEF_VAL,COMMENTS,SCALE,COL_NO FROM " + tableType + "_COLUMNS A  "
                    + "WHERE table_id=(select table_id from " + tableType + "_tables where "+"db_id = (select db_id from "+tableType+"_DATABASES WHERE DB_NAME = '"+fromConnection.getCatalog()+"') AND"+" table_name =" + parameters[1]
                    + "and schema_id in (select schema_id from " + tableType + "_schemas where schema_name =" + parameters[0] + ")) "+"and db_id = (select db_id from "+tableType+"_DATABASES WHERE DB_NAME = '"+fromConnection.getCatalog()+"')"+"ORDER BY COL_NO";
            sta = fromConnection.createStatement();
            rsm = sta.executeQuery(sqlStr);
            while (rs.next()) {
                if(rsm.next()) {
                    colSrcRow = new Vector<Object>();
                    // 源表列名
                    colSrcRow.add(rs.getString(4));
                    if ("DATETIME".equalsIgnoreCase(rsm.getString(2)) && "i".equalsIgnoreCase(rsm.getString(6))) {
                        colSrcRow.add("TIMESTAMP");
                    } else if ("DATETIME".equalsIgnoreCase(rsm.getString(2)) && "u".equalsIgnoreCase(rsm.getString(6))) {
                        colSrcRow.add("TIMESTAMP AUTO UPDATE");
                    } else {
                        // 源表数据类型
                        colSrcRow.add(rs.getString(6));
                    }
                    // 源表精度
                    colSrcRow.add(rs.getInt(7) < 0 ? "" : rs.getString(7));
                    // 源表标度
                    colSrcRow.add(rs.getString(9));
                    // 源表允许空值
                    colSrcRow.add(!rs.getBoolean(11));
                    // 源表默认值
                    String columnType = rs.getString(6);
                    objectParsing = new ObjectParsing();
                    //根据表字段数据类型判断对不同默认值的引号处理
                    switch (columnType) {
                        //字符首尾只有单引号保持原样，首位单引号
                        case "VARCHAR":
                        case "CLOB":
                        case "CHAR":
                            //暂不做处理
                            colSrcRow.add(rs.getString(13));
                            break;
                        case "INTEGER":
                        case "BIGINT":
                        case "FLOAT":
                        case "DOUBLE":
                        case "TINYINT":
                        case "SMALLINT":
                        case "NUMERIC":
                            String defaultValue = rs.getString(13);
                            //去除单双引号
                            colSrcRow.add(ObjectParsing.removeDatabaseObjectQuota(defaultValue));
                            break;
                        case "GUID":
                            String defaultValue1 = rs.getString(13);
                            //去除双引号
                            colSrcRow.add(ObjectParsing.removeDatabaseObjectQuota1(defaultValue1));
                            break;
                        case "BOOLEAN":
                            String defaultValue2 = rs.getString(13);
                            //去除单引号
                            colSrcRow.add(ObjectParsing.removeDatabaseObjectQuota2(defaultValue2));
                            break;
                        case "DATE":
                        case "DATETIME":
                        case "DATETIME WITH TIME ZONE":
                        case "TIME":
                        case "TIME WITH TIME ZONE":
                        case "INTERVAL YEAR":
                        case "INTERVAL MONTH":
                        case "INTERVAL DAY":
                        case "INTERVAL HOUR":
                        case "INTERVAL MINUTE":
                        case "INTERVAL SECOND":
                        case "INTERVAL YEAR TO MONTH":
                        case "INTERVAL DAY TO HOUR":
                        case "INTERVAL DAY TO MINUTE":
                        case "INTERVAL DAY TO SECOND":
                        case "INTERVAL HOUR TO MINUTE":
                        case "INTERVAL HOUR TO SECOND":
                        case "INTERVAL MINUTE TO SECOND":
                            if (rs.getString(13) != null && rs.getString(13).contains(MARK_SIN_QUOTATION)) {
                                colSrcRow.add(rs.getString(13));
                            } else {
                                String defaultValue3 = rs.getString(13);
                                //去除双引号
                                colSrcRow.add(ObjectParsing.removeDatabaseObjectQuota1(defaultValue3));
                            }
                            break;
                        default:
                            colSrcRow.add(rs.getString(13));
                    }
                    // 源表列Comment
                    colSrcRow.add(rs.getString(12));
                    // 源表Comment
                    colSrcRow.add(tableComment);
                    colSource.add(colSrcRow);
                }
            }
            // **************************************************************************************************************
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(rsm !=null){
                try {
                    rsm.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(sta !=null){
                try {
                    sta.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return colSource;
    }

    /**
     * 获取源数据库对应表外键信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectTableConstraint(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        Vector<Vector<Object>> tableConstraintVector = new Vector<Vector<Object>>();
        Vector<Object> consRow;
        MessageFormat messageFormat = null;
        ResultSet rs = null;
        String sql;
        Statement stmt = null;

        try {
            // **********************************************收集表约束信息***************************************************
            messageFormat = new MessageFormat("select sch.schema_name,tab.table_name,ac.cons_name,ac.cons_type,replace(decode(lower(ac.cons_type),''f'',substr(ac.define,2,position('')'' in ac.define)-2),ac.define),''\"'','''') "
                    + "as constraint_col,rsch.schema_name,rtab.table_name,replace(replace(decode(lower(ac.cons_type),''f'',substr(ac.define,position('')'' in ac.define)+2,length(ac.define)),ac.define),''\"'',''''),'')'','''') "
                    + "as ref_constraint_col,case lower(ac.update_action) when ''n'' then ''no action'' when ''c'' then ''cascade'' when ''u'' then ''set null'' when ''r'' then ''restrict'' when ''d'' then ''set default'' else ''no action'' end,"
                    + "case lower(ac.delete_action) when ''n'' then ''no action'' when ''c'' then ''cascade'' when ''u'' then ''set null'' when ''r'' then ''restrict'' when ''d'' then ''set default'' else ''no action'' end,decode(lower(ac.cons_type),''c'',ac.define,'''') "
                    + "as check_define,(case lower(ac.cons_type) when ''p'' then 0 when ''f'' then 1 when ''u'' then 2 when ''c'' then 3 else 4 end) as order_seq from " + tableType + "_tables tab left join " + tableType + "_schemas sch on tab.schema_id=sch.schema_id and tab.db_id=sch.db_id "
                    + "left join " + tableType + "_constraints ac on tab.table_id=ac.table_id and ac.db_id=tab.db_id left join " + tableType + "_tables rtab on rtab.table_id=ac.ref_table_id and  rtab.db_id=ac.db_id left join " + tableType + "_schemas rsch on rsch.schema_id=rtab.schema_id  and rsch.db_id=rtab.db_id where sch.schema_name={0} and tab.table_name={1} and tab.db_id=current_db_id order by order_seq");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                consRow = new Vector<Object>();
                // 表模式
                consRow.add(rs.getString(1));
                // 表名称
                consRow.add(XuguDDLUtils.isKeyWordAddQuote(rs.getString(2)));
                // 约束名
                consRow.add(XuguDDLUtils.isKeyWordAddQuote(rs.getString(3)));
                // 约束类型
                consRow.add(rs.getString(4));
                // 约束字段
                consRow.add(XuguDDLUtils.isKeyWordAddQuote(rs.getString(5)));
                // 主键表模式
                consRow.add(rs.getString(6));
                // 主键表名称
                consRow.add(XuguDDLUtils.isKeyWordAddQuote(rs.getString(7)));
                // 主键表字段
                consRow.add(XuguDDLUtils.isKeyWordAddQuote(rs.getString(8)));
                // 级联UPDATE RULE
                consRow.add(rs.getString(9));
                // 级联DELETE RULE
                consRow.add(rs.getString(10));
                // check约束定义
                consRow.add(rs.getString(11));
                // 约束类型序：0,主键;1,外键;2,唯一值;3,值检查;4,其他
                consRow.add(rs.getString(12));
                tableConstraintVector.add(consRow);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return tableConstraintVector;
    }

    /**
     * 获取源数据库对应表索引信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<IndexMeta> collectTableIndex(Connection fromConnection, String[] parameters, Parsing.TableType tableType) {
        Vector<IndexMeta> indexs = new Vector<IndexMeta>();
        ResultSet rs = null;
        String sql;
        String primaryKey = "";
        Statement stmt = null;

        try {
            DatabaseMetaData dbmd = fromConnection.getMetaData();

            /*************************************************获取主键约束名***********************************************/
            //获取表主键信息
            rs = dbmd.getPrimaryKeys(fromConnection.getCatalog(), removeDatabaseObjectQuota(parameters[0]), removeDatabaseObjectQuota(parameters[1]));
            while (rs.next()) {
                primaryKey = rs.getString(6);
            }
            rs.close();
            /**********************************************************************************************************/
            /*************************************************获取索引信息************************************************/
            MessageFormat messageFormat = null;
            messageFormat = new MessageFormat("select i.index_name,i.index_type,i.field_num,i.keys,i.is_primary,i.is_unique,i.create_time,i.copy_num "
                    + "from " + tableType + "_indexes i," + tableType + "_schemas s," + tableType + "_tables t where i.table_id=t.table_id and t.schema_id=s.schema_id and s.schema_name={0} "
                    + "and t.table_name={1} and s.db_id=current_db_id and t.db_id=current_db_id and i.db_id=current_db_id order by i.index_name asc");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                //去除无效索引及主键索引
                if (rs.getString(1) == null || rs.getString(6).equalsIgnoreCase(primaryKey)) {
                    continue;
                }
                indexs.add(collectTableIndexPartition(fromConnection, new String[]{parameters[0], parameters[1], quotaDatabaseObjectSin(rs.getString(1))}, tableType));
            }

        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return indexs;
    }

    /**
     * 获取源数据库对应表索引分区信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public IndexMeta collectTableIndexPartition(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        IndexMeta ddl = new IndexMeta();
        Vector<Object> rowdata;
        ResultSet  rs = null;
        String sql;
        Statement stmt = null;
        MessageFormat messageFormat = null;
        MessageFormat messageFormatPartition = null;
        MessageFormat messageFormatSubPartition = null;

        try {
            messageFormat = new MessageFormat("select idx.* from " + tableType + "_indexes idx left join " + tableType + "_tables tab on tab.table_id=idx.table_id "
                    + "left join " + tableType + "_schemas sch on tab.schema_id=sch.schema_id where sch.schema_name={0} and tab.table_name={1} "
                    + "and idx.index_name={2}");
            messageFormatPartition = new MessageFormat("select t1.* from " + tableType + "_idx_partis t1 where exists(select idx.index_id "
                    + "from " + tableType + "_indexes idx left join " + tableType + "_tables tab on tab.table_id=idx.table_id left join " + tableType + "_schemas sch on tab.schema_id=sch.schema_id "
                    + "where sch.schema_name={0} and tab.table_name={1} and idx.index_name={2} and t1.index_id=idx.index_id)");
            messageFormatSubPartition = new MessageFormat("select t1.* from " + tableType + "_idx_subpartis t1 where exists(select idx.index_id from " + tableType + "_indexes idx "
                    + "left join " + tableType + "_tables tab on tab.table_id=idx.table_id left join " + tableType + "_schemas sch on tab.schema_id=sch.schema_id "
                    + "where sch.schema_name={0} and tab.table_name={1} and idx.index_name={2} and t1.index_id=idx.index_id)");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                ddl.setSchemaName(removeDatabaseObjectQuota(parameters[0].toString()));
                ddl.setTableName(removeDatabaseObjectQuota(parameters[1].toString()));
                ddl.setIndexName(removeDatabaseObjectQuota(parameters[2].toString()));
//                ddl.setIndex_Columns(rs.getString("KEYS"));
                //索引键
                ddl.setIndexColumns(XuguDDLUtils.isKeyWordAddQuote(removeDatabaseObjectQuota(rs.getString("KEYS"))));
                //索引类型
                ddl.setIndexType(rs.getInt("INDEX_TYPE"));
                //是否为唯一值索引
                ddl.setUnique(rs.getBoolean("IS_UNIQUE"));
                //是否为主键索引
                ddl.setPrimaryKey(rs.getBoolean("IS_PRIMARY"));
                //是否为局部索引
                ddl.setLocal(rs.getBoolean("IS_LOCAL"));
                //字段数
                ddl.setFieldNum(rs.getInt("FIELD_NUM"));
                ddl.setKeys(rs.getString("KEYS"));
                //词表名
                ddl.setVocable(rs.getString("VOCABLE"));
                //词分割长度
                ddl.setWordLen(rs.getInt("WORD_LEN"));
                //分区类型（0：无分区，1：范围分区，2：列表分区）
                ddl.setPartType(rs.getInt("PARTI_TYPE"));
                //分区数
                ddl.setPartNum(rs.getInt("PARTI_NUM"));
                //分区键
                ddl.setPartKeys(rs.getString("PARTI_KEY"));
                //子分区类型（0：无分区，1：范围分区，2：列表分区）
                ddl.setSubPartType(rs.getInt("SUBPARTI_TYPE"));
                //子分区数
                ddl.setSubPartNum(rs.getInt("SUBPARTI_NUM"));
                //子分区键
                ddl.setSubPartKeys(rs.getString("SUBPARTI_KEY"));

                ddl.setCopyNum(rs.getInt("COPY_NUM"));
            }
            if(rs!=null){
                rs.close();
            }
            if(stmt!=null){
                stmt.close();
            }
            /**************************************************索引一级分区*************************************************/
            if (ddl.getPartKeys() != null) {
                sql = messageFormatPartition.format(parameters);
                stmt = fromConnection.createStatement();
                rs = stmt.executeQuery(sql);
                Vector<Vector<Object>> indexPart = new Vector<Vector<Object>>();
                while (rs.next()) {
                    rowdata = new Vector<Object>();
                    //分区名
                    rowdata.add(rs.getString("PARTI_NAME"));
                    //分区条件值
                    rowdata.add(rs.getString("PARTI_VAL"));
                    indexPart.add(rowdata);
                }
                ddl.setPartInfo(indexPart);
                if(rs!=null){
                    rs.close();
                }
                if(stmt!=null){
                    stmt.close();
                }
            }
            /*****************************************************************************************************************/
            /**************************************************索引二级分区*************************************************/
            if (ddl.getSubPartKeys() != null) {
                sql = messageFormatSubPartition.format(parameters);
                stmt = fromConnection.createStatement();
                rs = stmt.executeQuery(sql);
                Vector<Vector<Object>> indexSubpart = new Vector<Vector<Object>>();
                while (rs.next()) {
                    rowdata = new Vector<Object>();
                    rowdata.add(rs.getString("SUBPARTI_NAME"));
                    rowdata.add(rs.getString("SUBPARTI_VAL"));
                    indexSubpart.add(rowdata);
                }
                ddl.setSubPartInfo(indexSubpart);
            }
            /*****************************************************************************************************************/
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return ddl;
    }

    /**
     * 获取源数据库对应表分区信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Object> collectTablePartition(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        Vector<Object> tabpart = null;
        ResultSet rs = null;
        String sql;
        MessageFormat messageFormat = null;
        MessageFormat messageFormatPartition = null;
        MessageFormat messageFormatSubPartition = null;
        Statement stmt = null;
        try {
            messageFormat = new MessageFormat("select s.schema_name,t.table_name,case parti_type "
                    + "when 1 then ''range'' when 2 then ''list'' when 3 then ''hash'' else null "
                    + "end as parti_type,case subparti_type when 1 then ''range'' when 2 then ''list'' "
                    + "when 3 then ''hash'' else null end as subparti_type,parti_key,subparti_key,parti_num,subparti_num,case auto_parti_type "
                    + "when 1 then ''year'' when 2 then ''month'' when 3 then ''day'' when 4 then ''hour'' end as auto_parti_type, auto_parti_span "
                    + "from " + tableType + "_tables t left join " + tableType + "_schemas s on t.schema_id=s.schema_id "
                    + "left join " + tableType +"_databases d on d.db_id = s.db_id "
                    + "where s.schema_name={0} and t.table_name={1} and d.db_name = {4}  and t.db_id = d.db_id and s.db_id = d.db_id and t.parti_type is not null");
            messageFormatPartition = new MessageFormat("select s.schema_name,t.table_name,part.parti_name,part.parti_val from " + tableType + "_tables t "
                    + "left join " + tableType + "_schemas s on t.schema_id=s.schema_id left join " + tableType + "_partis part on t.table_id=part.table_id "
                    + "left join " + tableType +"_databases d on d.db_id = s.db_id "
                    + "where s.schema_name={0} and t.table_name={1} and d.db_name = {4} and t.db_id = d.db_id and s.db_id = d.db_id order by part.parti_no");
            messageFormatSubPartition = new MessageFormat("select s.schema_name,t.table_name,part.subparti_name,part.subparti_val "
                    + "from " + tableType + "_tables t left join " + tableType + "_schemas s on t.schema_id=s.schema_id left join " + tableType + "_subpartis part on t.table_id=part.table_id "
                    + "left join " + tableType +"_databases d on d.db_id = s.db_id "
                    + "where s.schema_name={0} and t.table_name={1} and d.db_name = {4}  and t.db_id = d.db_id and s.db_id = d.db_id order by part.subparti_no");

            ResultSet resultSet = fromConnection.createStatement().executeQuery("select current_database()");
            String dbName = "";

            while (resultSet.next()){
                dbName = resultSet.getString(1);

            }
            String string ="'"+ dbName+"'";
            String[] newParameters = (String[]) Arrays.copyOf(parameters, parameters.length + 1);
            newParameters[newParameters.length - 1] = string;
            parameters = newParameters;

            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            ArrayList<Integer> number7 = new ArrayList<>();
            ArrayList<Integer> number8 = new ArrayList<>();
            while (rs.next()) {
                tabpart = new Vector<Object>();
                //分区表模式
                tabpart.add(rs.getString(1));
                //分区表名称
                tabpart.add(rs.getString(2));
                //分区表主分区类型
                tabpart.add(rs.getString(3));
                //分区表子分区类型
                tabpart.add(rs.getString(4));
                //分区表主分区键
                tabpart.add(rs.getString(5));
                //分区表子分区键
                tabpart.add(rs.getString(6));

                number7.add(rs.getInt(7));

                number8.add(rs.getInt(8));

                //是否自动扩展分区
                tabpart.add(rs.getString(9));
                //自动扩展分区间隔
                tabpart.add(rs.getString(10));
            }
            rs.close();
            for (Integer i : number7) {
                if (i > 0) {
                    Vector<Object> partVal;
                    ResultSet rs1;
                    Vector<Object> mainpart = new Vector<Object>();
                    sql = messageFormatPartition.format(parameters);
                    stmt = (Statement) fromConnection.createStatement();
                    rs1 = stmt.executeQuery(sql);
                    while (rs1.next()) {
                        partVal = new Vector<Object>();
                        //分区表主分区模式
                        partVal.add(rs1.getString(1));
                        //分区表主分区表名
                        partVal.add(rs1.getString(2));
                        //分区表主分区名称
                        partVal.add(rs1.getString(3));
                        //分区表主分区键值
                        partVal.add(rs1.getString(4) == null ? null : rs1.getString(4).replace("TIMESTAMP", ""));
                        //分区表主分区信息
                        mainpart.add(partVal);
                    }
                    tabpart.add(mainpart);
                    rs1.close();
                } else {
                    tabpart.add(null);
                }
            }

            for (Integer i : number8) {
                if (i > 0) {
                    Vector<Object> partVal;
                    Vector<Object> subpart = new Vector<Object>();
                    ResultSet rs2;
                    sql = messageFormatSubPartition.format(parameters);
                    stmt = fromConnection.createStatement();
                    rs2 = stmt.executeQuery(sql);
                    while (rs2.next()) {
                        partVal = new Vector<Object>();
                        //分区表子分区模式
                        partVal.add(rs2.getString(1));
                        //分区表子分区表名
                        partVal.add(rs2.getString(2));
                        //分区表子分区名称
                        partVal.add(rs2.getString(3));
                        //分区表主分区键值
                        partVal.add(rs2.getString(4) == null ? null : rs2.getString(4).replace("TIMESTAMP", ""));
                        //分区表子分区信息
                        subpart.add(partVal);
                    }
                    tabpart.add(subpart);
                    rs2.close();
                } else {
                    tabpart.add(null);
                }
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return tabpart;
    }


    /**
     * 获取源数据库对应表自增信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectTableIdentity(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        Vector<Object> columnIdentityRow = null;
        Vector<Vector<Object>> identity = new Vector<Vector<Object>>();
        MessageFormat messageFormat = null;
        String primaryKeyCol = "";
        ResultSet rs = null;
        String sql;
        Statement stmt = null;
        parameters = new String[]{parameters[0].toString(), parameters[1].toString()};
        try {
            /*************************************************获取主键列名************************************************/
            // 解决主键与自增字段共生的情况，获取表主键信息
            rs = fromConnection.getMetaData().getPrimaryKeys(fromConnection.getCatalog(), removeDatabaseObjectQuota(parameters[0]), removeDatabaseObjectQuota(parameters[1]));
            while (rs.next()) {
                //可能是多字段主键(自增仅为单字段，用于判断共生)
                primaryKeyCol += rs.getString(4);
            }
            if(rs!=null){
                rs.close();
            }
            /************************************************************************************** ********************/

            messageFormat = new MessageFormat("select s.schema_name,t.table_name,c.col_name,c.type_name,q.min_val,q.max_val,q.curr_val,q.step_val,q.is_cycle as iscycle "
                    + "from " + tableType + "_schemas s join " + tableType + "_tables t on s.schema_id=t.schema_id and s.db_id=t.db_id join " + tableType + "_columns c on c.table_id = t.table_id and c.db_id=t.db_id "
                    + "join " + tableType + "_sequences q on q.seq_id=c.serial_id and q.db_id=c.db_id where s.schema_name={0} and t.table_name={1} and c.is_serial = ''T'' and c.db_id=current_db_id");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                columnIdentityRow = new Vector<Object>();
                // 表模式
                columnIdentityRow.add(rs.getString(1));
                // 表名称
                columnIdentityRow.add(XuguDDLUtils.isKeyWordAddQuote(rs.getString(2)));
                // 表列名称
                columnIdentityRow.add(XuguDDLUtils.isKeyWordAddQuote(rs.getString(3)));
                // 表列数据类型
                columnIdentityRow.add(rs.getString(4));
                // 表列自增起始值
                columnIdentityRow.add(rs.getString(5));
                // 表列自增最大值
                columnIdentityRow.add(rs.getString(6));
                // 表列自增当前值
                columnIdentityRow.add(rs.getString(7));
                // 表列自增步长
                columnIdentityRow.add(rs.getString(8));
                // 自增是否循环Cycle
                columnIdentityRow.add(rs.getBoolean(9));
                // 是否为主键
                columnIdentityRow.add(rs.getString(3).equals(primaryKeyCol) ? true : false);
                identity.add(columnIdentityRow);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        /******************************************************************************************/
        return identity;
    }


    /**
     * 获取源数据库对应表自增信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Object collectTableType(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        int tempType=0;
        MessageFormat messageFormat = null;
        ResultSet rs = null;
        String sql;
        Statement stmt = null;
        try{
        parameters = new String[]{parameters[0].toString(), parameters[1].toString()};
            messageFormat = new MessageFormat("select t.temp_type from "+tableType +"_tables t  join "+tableType +"_schemas s on t.schema_id = s.schema_id  where s.schema_name={0} and t.table_name={1} and db_id = current_db_id ");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                tempType=rs.getInt(1);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        /******************************************************************************************/
        return tempType;
    }


    /**
     * 获取源数据库对应视图信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectView(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        Vector<Vector<Object>> view = new Vector<Vector<Object>>();
        Vector<Object> viewRow;
        MessageFormat messageFormat = null;
        ResultSet rs = null;
        String sql;
        Statement stmt = null;
        try {
            // ************************************迁出视图结构***************************************************
            messageFormat = new MessageFormat("select s.schema_name,v.view_name,define,option as check_option,v.comments "
                    + "from " + tableType + "_views v left join " + tableType + "_schemas s on v.schema_id=s.schema_id where s.schema_name={0} and v.view_name={1}");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                viewRow = new Vector<Object>();
                // 模式视图
                viewRow.add(rs.getString(1));
                // 视图名称
                viewRow.add(rs.getString(2));
                // 视图定义
                viewRow.add(rs.getString(3));
                // 视图严格检查WHERE条件：WITH CHECK OPTION
                viewRow.add(rs.getString(4));
                viewRow.add(rs.getString(5));
                view.add(viewRow);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return view;
    }


    /**
     * 获取源数据库对应序列值信息
     *
     * @param fromConnection
     * @param parameters
     * @param table_type
     * @return
     */
    public Vector<Vector<Object>> collectSequence(Connection fromConnection, Object[] parameters, Parsing.TableType table_type) {
        MessageFormat messageFormat = null;
        ResultSet rs =null;
        String sql;
        Vector<Vector<Object>> sequence = new Vector<Vector<Object>>();
        Vector<Object> val;
        Statement stmt = null;
        try {
            messageFormat = new MessageFormat("select q.curr_val,q.step_val,q.min_val,q.max_val,q.is_cycle ,q.comments "
                    + "from " + table_type + "_schemas s," + table_type + "_sequences q where s.schema_id=q.schema_id and s.schema_name={0} and q.seq_name={1}");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                val = new Vector<Object>();
                // 当前值
                val.add(rs.getString(1));
                // 步长
                val.add(rs.getString(2));
                // 最小值
                val.add(rs.getString(3));
                BigDecimal oracleSequenceMax = new BigDecimal(rs.getString(4));
                BigDecimal sequenceMax = new BigDecimal(Long.MAX_VALUE);
                if (oracleSequenceMax.compareTo(sequenceMax) == -1) {
                    // 最大值
                    val.add(rs.getString(4));
                } else {
                    val.add(String.valueOf(Long.MAX_VALUE));
                }
                // 是否循环
                val.add(Boolean.valueOf(rs.getBoolean(5)));
                //注释信息
                val.add(rs.getString(6));
                sequence.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return sequence;
    }

    /**
     * 获取源数据库对应包信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectPackage(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> define = new Vector<Vector<Object>>();
        Vector<Object> val;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            messageFormat = new MessageFormat("select p.spec,p.body ,p.comments from " + tableType + "_schemas s," + tableType + "_packages p "
                    + "where s.schema_id=p.schema_id and s.schema_name={0} and p.pack_name={1}");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                val = new Vector<Object>();
                //包头，包体
                val.add(rs.getString(1) + "\n\n" + rs.getString(2));
                //包注释
                val.add(rs.getString(3));
                define.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return define;
    }

    /**
     * 获取源数据库对应存储过程信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectProcedure(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> define = new Vector<Vector<Object>>();
        Vector<Object> val;
        Statement stmt =null;
        ResultSet rs = null;
        try {
            messageFormat = new MessageFormat("select p.define ,p.comments from " + tableType + "_schemas s," + tableType + "_procedures p "
                    + "where s.schema_id=p.schema_id  and s.schema_name={0} and p.proc_name={1}");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                val = new Vector<Object>();
                //存储过程定义
                val.add(rs.getString(1));
                //存储过程注释
                val.add(rs.getString(2));
                define.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return define;
    }


    /**
     * 获取源数据库对应函数信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectFunction(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> define = new Vector<Vector<Object>>();
        Vector<Object> val;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            messageFormat = new MessageFormat("select p.define,p.comments from " + tableType + "_schemas s," + tableType + "_procedures p "
                    + "where s.schema_id=p.schema_id and p.ret_type is not null and s.schema_name={0} and p.proc_name={1}");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
             rs = stmt.executeQuery(sql);
            if (rs.next()) {
                val = new Vector<Object>();
                //函数定义
                val.add(rs.getString(1));
                //函数注释
                val.add(rs.getString(2));
                define.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return define;
    }

    /**
     * 获取源数据库对应触发器信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectTrigger(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> define = new Vector<Vector<Object>>();
        Vector<Object> val;
        ResultSet rs = null;
        Statement stmt = null;
        try {
            messageFormat = new MessageFormat("select t.define ,t.comments from " + tableType + "_schemas s," + tableType + "_triggers t "
                    + "where s.schema_id=t.schema_id and s.schema_name={0} and t.trig_name={1}");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                val = new Vector<Object>();
                //触发器定义
                val.add(rs.getString(1));
                //触发器注释
                val.add(rs.getString(2));
                define.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return define;
    }

    /**
     * 获取源数据库对应同义词信息
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectSynonym(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> define = new Vector<Vector<Object>>();
        Vector<Object> val;
        ResultSet rs = null;
        Statement stmt = null;
        try {
            messageFormat = new MessageFormat("select * from (select y.is_public,y.targ_sche_id,y.targ_name "
                    + "from " + tableType + "_synonyms y join " + tableType + "_schemas s on y.schema_id=s.schema_id and y.db_id=s.db_id "
                    + "where s.schema_name={0} and y.syno_name={1} and y.db_id=current_db_id) a1 join "
                    + "(select schema_name as targ_sche_name,schema_id from " + tableType + "_schemas) a2 on a1.targ_sche_id=a2.schema_id");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                val = new Vector<Object>();
                //是否为全局同义词
                val.add(rs.getBoolean(1));

                //目标对象名
                val.add(rs.getString(3));

                //目标模式名
                val.add(rs.getString(4));

                define.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return define;
    }

    /**
     * 获取源数据库对应的自定义数据类型
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectUdt(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> define = new Vector<Vector<Object>>();
        Vector<Object> val;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            messageFormat = new MessageFormat("select t.spec,t.body ,t.comments from " + tableType + "_types t ," + tableType + "_schemas s " +
                    "where s.schema_id = t.schema_id and s.schema_name={0} and t.type_name={1} ");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            if (rs.next()) {
                val = new Vector<Object>();
                //头部定义
                val.add(rs.getString(1));
                //成员过程定义
                val.add(rs.getString(2));
                //注释
                val.add(rs.getString(3));
                define.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return define;
    }

    /**
     * 收集定时作业
     *
     * @param fromConnection
     * @param parameters
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectJob(Connection fromConnection, Object[] parameters, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> define = new Vector<Vector<Object>>();
        Vector<Object> val;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            messageFormat = new MessageFormat("select j.job_name,j.job_type,j.job_action,j.job_param_num, " +
                    "j.begin_t,j.repet_interval,j.end_t,j.enable,j.auto_drop,j.comments " +
                    "from " + tableType + "_jobs j where j.job_name={1}");
            sql = messageFormat.format(parameters);
            stmt = fromConnection.createStatement();
             rs = stmt.executeQuery(sql);
            while (rs.next()) {
                val = new Vector<Object>();
                //定时作业名
                val.add(rs.getString(1));
                //定时作业类型
                val.add(rs.getString(2));
                //动作语句（z作业被执行时将执行该语句）
                val.add(rs.getString(3));
                //参数个数
                val.add(rs.getString(4));
                //开始时间
                val.add(rs.getString(5));
                //频度描述字串
                val.add(rs.getString(6));
                //结束时间
                val.add(rs.getString(7));
                //该作业是否被允许
                val.add(rs.getString(8));
                //作业到达结束时间受否自动删除
                val.add(rs.getString(9));
                //作业注释
                val.add(rs.getString(10));
                define.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return define;
    }

    /**
     * 根据用户名获取所有对象
     *
     * @param fromConnection
     * @param userName
     * @return
     */
    public Vector<Vector<Object>> collectObjectsByUser(Connection fromConnection, String[] userName, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> objects = new Vector<Vector<Object>>();
        Vector<Object> val;
        Statement stmt = null;
        ResultSet rs =null;
        try {
            messageFormat = new MessageFormat("select s.schema_name,o.obj_name,o.obj_type,o.is_sys from " +
                    tableType+"_objects o left join "+tableType+"_schemas s on o.schema_id=s.schema_id" +
                    " where o.user_id=(select u.user_id from "+tableType+"_users u where u.user_name={0}) ");
            sql = messageFormat.format(userName);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                val = new Vector<Object>();
                //模式名
                val.add(rs.getString(1));
                //对象名
                val.add(rs.getString(2));
                //对象类型
                val.add(rs.getInt(3));
                //是否内建
                val.add(rs.getBoolean(4));
                objects.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return objects;
    }

    /**
     * 通过模式名获取模式下的同义词对象
     */
    public Vector<Vector<Object>> collectSynonymBySchema(Connection conn ,String [] schemaNames, Parsing.TableType tableType) {
        Vector<Vector<Object>> define = new Vector<Vector<Object>>();
        MessageFormat messageFormat ;
        String sql = "";
        ResultSet rs = null;
        Statement stmt = null;
        try {
            messageFormat = new MessageFormat("select y.is_public,y.syno_name,y.targ_name "
                    + "from " + tableType + "_synonyms y join " + tableType + "_schemas s on y.schema_id=s.schema_id and y.db_id=s.db_id "
                    + "where s.schema_name={0} and y.db_id=current_db_id");
            sql = messageFormat.format(schemaNames);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                Vector<Object> synonyms = new Vector<Object>();
                //是否为全局同义词
                synonyms.add(rs.getBoolean(1));
                //目标对象名
                synonyms.add(rs.getString(2));
                //目标模式名
                synonyms.add(rs.getString(3));
                define.add(synonyms);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return  define;
    }

    /**
     * 通过模式名获取模式下的对象
     *
     * @param fromConnection
     * @param schemaName
     * @param tableType
     * @return
     */
    public Vector<Vector<Object>> collectobjectsbyschema(Connection fromConnection, String[] schemaName, Parsing.TableType tableType){
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> objects = new Vector<Vector<Object>>();
        Vector<Object> val;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            messageFormat = new MessageFormat("select distinct s.schema_name,o.obj_name,o.obj_type,o.is_sys from " +
                    tableType + "_objects o left join " + tableType + "_schemas s on o.schema_id=s.schema_id" +
                    " where s.schema_id in (select schema_id from " + tableType + "_schemas  where schema_name={0}) ");
            sql = messageFormat.format(schemaName);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
        while (rs.next()) {
                val = new Vector<Object>();
                //模式名
                val.add(rs.getString(1));
                //对象名
                val.add(rs.getString(2));
                //对象类型
                val.add(rs.getInt(3));
                //是否内建
                val.add(rs.getBoolean(4));
                objects.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally{
            //释放result（结果集）资源
            if(rs != null){
                try{
                    rs.close();
                }catch (SQLException e){
                    e.printStackTrace();
                }
            }
            // 释放Statement资源
            if(stmt != null) {
                try {
                    stmt.close() ;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return objects;
    }

    public Vector<Vector<Object>> collectobjectsbyschema(Connection fromConnection, String databaseName, String[] schemaName, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<Vector<Object>> objects = new Vector<Vector<Object>>();
        Vector<Object> val;
        Statement stmt = null;
        ResultSet rs = null;
        String dbName = ObjectParsing.quotaDatabaseObjectSin(databaseName);
        String sName = ObjectParsing.removeDatabaseObjectQuota(schemaName[0]);
        try {
            messageFormat = new MessageFormat("select o.db_id,o.obj_name,o.obj_type,o.is_sys from sys_objects o" +
                    " where o.db_id=(select d.db_id from sys_databases d where d.db_name='" + dbName + "')" +
                    " and o.schema_id=(select s.schema_id  from sys_schemas s" +
                    " where s.db_id=(select d.db_id from sys_databases d where d.db_name='" + dbName + "') and s.schema_name={0}) and o.is_sys='false'");
            sql = messageFormat.format(schemaName);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                val = new Vector<Object>();
                val.add(sName);
                //库id
                val.add(rs.getInt(1));
                //对象名
                val.add(rs.getString(2));
                //对象类型
                val.add(rs.getInt(3));
                //是否内建
                val.add(rs.getBoolean(4));
                objects.add(val);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return objects;
    }

    /**
     * 获取指定数据库的模式
     *
     * @param fromConnection
     * @param databaseName
     * @param tableType
     * @return
     */
    public Vector<String> collectSchemasByDatabase(Connection fromConnection, String[] databaseName, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<String> val = new Vector<String>();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            messageFormat = new MessageFormat("select  schema_name from " + tableType + "_schemas  " +
                    "where  db_id=(select db_id from " + tableType + "_databases where db_name={0}) ");
            sql = messageFormat.format(databaseName);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                val.add(rs.getString(1));
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return val;
    }

    /**
     * 获取数据库id
     *
     * @param fromConnection
     * @param dataBaseName
     * @param tableType
     * @return
     */
    public int getDatabaseId(Connection fromConnection, String[] dataBaseName, Parsing.TableType tableType) {
        MessageFormat messageFormat;
        String sql = "";
        Statement stmt = null;
        ResultSet rs = null;
        int dbId = 0;
        try {
            messageFormat = new MessageFormat("select db_id from " + tableType + "_databases where db_name={0}");
            sql = messageFormat.format(dataBaseName);
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                dbId = rs.getInt(1);
            }
        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return dbId;
    }

    /**
     * 获取数据库中所有定时作业
     *
     * @param fromConnection
     * @param dbId
     * @param tableType
     * @return
     */
    public Vector<String> collectJobsByDatabase(Connection fromConnection, int dbId, Parsing.TableType tableType) {
        MessageFormat messageFormat = null;
        String sql = "";
        Vector<String> val = new Vector<String>();
        Statement stmt = null;
        ResultSet rs = null ;
        try {
            sql = "select j.job_name from " + tableType + "_jobs j where  j.db_id=" + dbId + "";
            stmt = fromConnection.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                val.add(rs.getString(1));
            }

        } catch (SQLException e) {
//		log.error(e.toString());
            System.out.println(e.getMessage());
        }finally {
            if(rs!=null){
                try {
                    rs.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            if(stmt!=null){
                try {
                    stmt.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
        }
        return val;
    }


    /**
     * 组装表创建语句
     *
     * @param objectName
     * @param column
     * @param partition
     * @param schemaName
     * @param tableName
     * @return
     */
    public String assembleXuGuTable(Object objectName, Vector<Vector<Object>> column, Vector<Object> partition, String schemaName, String tableName,Integer  tempType,Integer copyNumber) {
        if (column.size() == 0) {
            return " ";
        }
        String tabComment = null;
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("-- Create Table --");
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("create ");
        if(tempType==2){
            sqlBuffer.append("global temp ");
        }
        sqlBuffer.append("table ");
        sqlBuffer.append(objectName);
        sqlBuffer.append(MARK_BGN_CURVES);
        sqlBuffer.append(MARK_WRAP);
        for (int j = 0; j < column.size(); j++) {
            // 列名
            sqlBuffer.append(XuguDDLUtils.isKeyWordAddQuote(column.get(j).get(0).toString()));
            sqlBuffer.append(" ");
            if ("NUMERIC".equals(column.get(j).get(1).toString())) {
                // 数据类型
                sqlBuffer.append(column.get(j).get(1).toString().toLowerCase());
                if (!"".equals(column.get(j).get(2).toString()) && !"".equals(column.get(j).get(3).toString())) {
                    sqlBuffer.append(MARK_BGN_CURVES);
                    // 精度
                    sqlBuffer.append(column.get(j).get(2).toString());
                    sqlBuffer.append(",");
                    // 标度
                    sqlBuffer.append(column.get(j).get(3).toString());
                    sqlBuffer.append(MARK_END_CURVES);
                } else if (!"".equals(column.get(j).get(2).toString()) && "".equals(column.get(j).get(3).toString())) {
                    sqlBuffer.append(MARK_BGN_CURVES);
                    // 精度
                    sqlBuffer.append(column.get(j).get(2).toString());
                    sqlBuffer.append(",");
                    // 标度
                    sqlBuffer.append("0");
                    sqlBuffer.append(MARK_END_CURVES);
                } else if ("".equals(column.get(j).get(2).toString()) && !"".equals(column.get(j).get(3).toString())) {
                    sqlBuffer.append(MARK_BGN_CURVES);
                    // 精度
                    sqlBuffer.append("0");
                    sqlBuffer.append(",");
                    // 标度
                    sqlBuffer.append(column.get(j).get(3).toString());
                    sqlBuffer.append(MARK_END_CURVES);
                }
            } else if ("CHAR".equals(column.get(j).get(1).toString()) || "VARCHAR".equals(column.get(j).get(1).toString())) {
                // 数据类型
                sqlBuffer.append(column.get(j).get(1).toString().toLowerCase());
                if (!"".equals(column.get(j).get(2).toString())) {
                    sqlBuffer.append(MARK_BGN_CURVES);
                    // 精度
                    sqlBuffer.append(column.get(j).get(2).toString());
                    sqlBuffer.append(MARK_END_CURVES);
                }
            } else {
                // 数据类型
                sqlBuffer.append(column.get(j).get(1).toString().toLowerCase());
            }
            // 非空约束
            if (Boolean.valueOf(column.get(j).get(4).toString())) {
                sqlBuffer.append(" not null");
            }
            // 默认值
            if (column.get(j).get(5) != null) {
                if (column.get(j).get(5).toString().toLowerCase().contains("uuid()")) {
                    sqlBuffer.append(" default ");
                    sqlBuffer.append(column.get(j).get(5).toString().toLowerCase().replace("uuid()", "sys_guid()"));
                } else if ("(GETDATE())".equalsIgnoreCase(column.get(j).get(5).toString()) ||"sysdate".equalsIgnoreCase( column.get(j).get(5).toString().trim())) {
                    sqlBuffer.append(" default SYSDATE");
                } else if ("0000-00-00 00:00:00".equals(column.get(j).get(5).toString())) {
                    sqlBuffer.append(" default '1970-01-01 00:00:00'");
                } else if ("0000-00-00".equals(column.get(j).get(5).toString())) {
                    sqlBuffer.append(" default '1970-01-01'");
                } else {
                    sqlBuffer.append(" default ");
                    if (column.get(j).get(1).toString().toLowerCase().contains("time") && "SYSDATE".equals(removeDatabaseObjectQuota(column.get(j).get(5).toString()))) {
                        sqlBuffer.append(removeDatabaseObjectQuota(column.get(j).get(5).toString()));
                    } else if (column.get(j).get(1).toString().toLowerCase().contains("char")
                            || column.get(j).get(1).toString().toLowerCase().contains("date")
                            || column.get(j).get(1).toString().toLowerCase().contains("time")) {
                        sqlBuffer.append(column.get(j).get(5).toString());
                    } else {
                        sqlBuffer.append(column.get(j).get(5));
                    }
                }
            }
//            if (column.get(j).get(6) != null && column.get(j).get(6).toString().length() > 1) { // 表列备注
//                sqlBuffer.append(", comment on column"+tableName+"."+column.get(j).get(0).toString()+" is ");
//                sqlBuffer.append(QuotaDatabaseObjectSin(column.get(j).get(6)));
//            }
//            if (column.get(j).get(7) != null) { // 表备注
//                tabComment = column.get(j).get(7).toString();
//            }
            if (j != column.size() ) {
                // 字段分割
                if (column.get(j).get(6) != null && column.get(j).get(6).toString().length() > 1) {
                    sqlBuffer.append(" comment ");
                    sqlBuffer.append(quotaDatabaseObjectSin(column.get(j).get(6)));
                }
                //最后一列不加“,”
                if(j != column.size()-1){
                    sqlBuffer.append(MARK_COMMA);
                }
            }
            sqlBuffer.append(MARK_WRAP);
        }
        sqlBuffer.append(MARK_END_CURVES);
        //*************************************表分区**********************************************
        if (partition != null && partition.size() > 0) {
            Vector<Object> mainPart = (Vector<Object>) partition.get(6);
            Vector<Object> subPart = (Vector<Object>) partition.get(7);
            switch (getTabPartitionType(partition.get(2))) {
                //范围分区
                case range:
                    if (removeDatabaseObjectQuota(partition.get(8)) == null) {
                        sqlBuffer.append(" partition by " + PartitionType.range.getKey());
                        sqlBuffer.append(MARK_BGN_CURVES + removeDatabaseObjectQuota(partition.get(4)) + MARK_END_CURVES + " partitions");
                        sqlBuffer.append(MARK_BGN_CURVES);
                        sqlBuffer.append(MARK_WRAP);
                        for (int m = 0; m < mainPart.size(); m++) {
                            sqlBuffer.append(((Vector<Object>) mainPart.get(m)).get(2));
                            sqlBuffer.append(" values less than ");
                            sqlBuffer.append(MARK_BGN_CURVES);
                            sqlBuffer.append("maxvalue".equalsIgnoreCase(((Vector<Object>) mainPart.get(m)).get(3).toString()) ? "maxvalues" : ((Vector<Object>) mainPart.get(m)).get(3));
                            sqlBuffer.append(MARK_END_CURVES);
                            if (m != mainPart.size() - 1) {
                                sqlBuffer.append(MARK_COMMA);
                            }
                            sqlBuffer.append(MARK_WRAP);
                        }
                        sqlBuffer.append(MARK_END_CURVES);
                    } else {
                        sqlBuffer.append(" partition by " + PartitionType.range.getKey());
                        sqlBuffer.append(MARK_BGN_CURVES + removeDatabaseObjectQuota(partition.get(4)) + MARK_END_CURVES + " interval " + removeDatabaseObjectQuota(partition.get(9)) + " " + removeDatabaseObjectQuota(partition.get(8)) + " partitions");
                        sqlBuffer.append(MARK_BGN_CURVES);
                        sqlBuffer.append(MARK_WRAP);
                        if (!mainPart.isEmpty()){
                            for (int m = 0; m < mainPart.size(); m++) {
                                sqlBuffer.append(((Vector<Object>) mainPart.get(m)).get(2));
                                sqlBuffer.append(" values less than ");
                                sqlBuffer.append(MARK_BGN_CURVES);
                                sqlBuffer.append("maxvalue".equalsIgnoreCase(((Vector<Object>) mainPart.get(m)).get(3).toString()) ? "maxvalues" : ((Vector<Object>) mainPart.get(m)).get(3));
                                sqlBuffer.append(MARK_END_CURVES);
                                if (m != mainPart.size() - 1) {
                                    sqlBuffer.append(MARK_COMMA);
                                }
                                sqlBuffer.append(MARK_WRAP);
                            }
                            sqlBuffer.append(MARK_END_CURVES);
                        }else {
                            sqlBuffer.append(quotaDatabaseObjectDub(((Vector<Object>) mainPart.get(0)).get(2)));
                            sqlBuffer.append(" values less than ");
                            sqlBuffer.append(MARK_BGN_CURVES);
                            sqlBuffer.append(((Vector<Object>) mainPart.get(0)).get(3));
                            sqlBuffer.append(MARK_END_CURVES);
                            sqlBuffer.append(MARK_WRAP);
                            sqlBuffer.append(MARK_END_CURVES);
                        }
                    }
                    break;
                //列表分区
                case list:
                    sqlBuffer.append(" partition by " + PartitionType.list.getKey());
                    sqlBuffer.append(MARK_BGN_CURVES + removeDatabaseObjectQuota(partition.get(4)) + MARK_END_CURVES + " partitions");
                    sqlBuffer.append(MARK_BGN_CURVES);
                    sqlBuffer.append(MARK_WRAP);
                    for (int m = 0; m < mainPart.size(); m++) {
                        sqlBuffer.append(((Vector<Object>) mainPart.get(m)).get(2));
                        sqlBuffer.append(" values ");
                        sqlBuffer.append(MARK_BGN_CURVES);
                        sqlBuffer.append("default".equalsIgnoreCase(((Vector<Object>) mainPart.get(m)).get(3).toString()) ? "othervalues" : ((Vector<Object>) mainPart.get(m)).get(3));
                        sqlBuffer.append(MARK_END_CURVES);
                        if (m != mainPart.size() - 1) {
                            sqlBuffer.append(MARK_COMMA);
                        }
                        sqlBuffer.append(MARK_WRAP);
                    }
                    sqlBuffer.append(MARK_END_CURVES);
                    break;
                //哈希分区
                case hash:
                    sqlBuffer.append(" partition by " + PartitionType.hash.getKey());
                    sqlBuffer.append(MARK_BGN_CURVES);
                    sqlBuffer.append(removeDatabaseObjectQuota(partition.get(4)));
                    sqlBuffer.append(MARK_END_CURVES);
                    sqlBuffer.append(" partitions " + mainPart.size());
                    break;
                default:
                    break;
            }
            switch (getTabPartitionType(partition.get(3))) {
                case range:
                    sqlBuffer.append(" subpartition by " + PartitionType.range.getKey());
                    sqlBuffer.append(MARK_BGN_CURVES + removeDatabaseObjectQuota(partition.get(5)) + MARK_END_CURVES + " subpartitions");
                    sqlBuffer.append(MARK_BGN_CURVES);
                    sqlBuffer.append(MARK_WRAP);
                    for (int m = 0; m < subPart.size(); m++) {
                        sqlBuffer.append(((Vector<Object>) subPart.get(m)).get(2));
                        sqlBuffer.append(" values less than ");
                        sqlBuffer.append(MARK_BGN_CURVES);
                        sqlBuffer.append("maxvalue".equalsIgnoreCase(((Vector<Object>) subPart.get(m)).get(3).toString()) ? "maxvalues" : ((Vector<Object>) subPart.get(m)).get(3));
                        sqlBuffer.append(MARK_END_CURVES);
                        if (m != subPart.size() - 1) {
                            sqlBuffer.append(MARK_COMMA);
                        }
                        sqlBuffer.append(MARK_WRAP);
                    }
                    sqlBuffer.append(MARK_END_CURVES);
                    break;
                case list:
                    sqlBuffer.append(" subpartition by " + PartitionType.list.getKey());
                    sqlBuffer.append(MARK_BGN_CURVES + removeDatabaseObjectQuota(partition.get(5)) + MARK_END_CURVES + " subpartitions");
                    sqlBuffer.append(MARK_BGN_CURVES);
                    sqlBuffer.append(MARK_WRAP);
                    for (int m = 0; m < subPart.size(); m++) {
                        sqlBuffer.append(((Vector<Object>) subPart.get(m)).get(2));
                        sqlBuffer.append(" values ");
                        sqlBuffer.append(MARK_BGN_CURVES);
                        sqlBuffer.append("default".equalsIgnoreCase(((Vector<Object>) subPart.get(m)).get(3).toString()) ? "othervalues" : ((Vector<Object>) subPart.get(m)).get(3));
                        sqlBuffer.append(MARK_END_CURVES);
                        if (m != subPart.size() - 1) {
                            sqlBuffer.append(MARK_COMMA);
                        }
                        sqlBuffer.append(MARK_WRAP);
                    }
                    sqlBuffer.append(MARK_END_CURVES);
                    break;
                case hash:
                    sqlBuffer.append(" subpartition by " + PartitionType.hash.getKey());
                    sqlBuffer.append(MARK_BGN_CURVES);
                    sqlBuffer.append(removeDatabaseObjectQuota(partition.get(5)));
                    sqlBuffer.append(MARK_END_CURVES);
                    sqlBuffer.append(" subpartitions " + subPart.size());
                    break;
                default:
                    break;
            }
        }
        if (copyNumber > 0){
            sqlBuffer.append(" copy number " + copyNumber);
        }
        sqlBuffer.append(SEMICOLON);
        for (int j = 0; j < column.size(); j++) {
            // 表列备注
//            if (column.get(j).get(6) != null && column.get(j).get(6).toString().length() > 1) {
//                sqlBuffer.append(MARK_WRAP + "comment on column " + tableName + "." + column.get(j).get(0).toString() + " is ");
//                sqlBuffer.append(quotaDatabaseObjectSin(column.get(j).get(6)));
//                sqlBuffer.append(SEMICOLON);
//            }
            // 表备注
            if (column.get(j).get(7) != null) {
                tabComment = column.get(j).get(7).toString();
            }

        }
        //表注释
        if (tabComment != null && tabComment.length() > 1) {
            sqlBuffer.append(MARK_WRAP + "comment on table " + tableName + " is ");
            sqlBuffer.append(quotaDatabaseObjectSin(tabComment));
            sqlBuffer.append(SEMICOLON);
        }

        return sqlBuffer.toString();
    }

    /**
     * 组装约束执行语句
     *
     * @param objectName
     * @param constraint
     * @param consType
     * @return
     */
    public String assembleTableConstraint(Object objectName, Vector<Object> constraint, DatabaseObjectType consType, List<Object> identityCols) {
        StringBuffer sqlBuffer = new StringBuffer();
        String[] pkCol;//主键列/唯一值列
        String[] fkCol;//外键列
        String str = "";
        switch (consType) {
            //主键约束
            case PrimaryKey:
                if (isIdentityPrimaryKey(constraint, identityCols)) {
                    break;
                }
                sqlBuffer.append(MARK_WRAP);
                sqlBuffer.append("-- Alter Table Add PrimaryKey Constraint --");
                sqlBuffer.append(MARK_WRAP);
                sqlBuffer.append("alter table " + objectName)
                        .append(" add constraint " + "\""+constraint.get(2)+"\"")
                        .append(" primary key" + MARK_BGN_CURVES);
                // 作用列
                pkCol = constraint.get(4).toString().split(MARK_COMMA);
                for (int p = 0; p < pkCol.length; p++) {
                    sqlBuffer.append(pkCol[p]);
                    if (p != pkCol.length - 1) {
                        sqlBuffer.append(MARK_COMMA);
                    }
                }
                sqlBuffer.append(MARK_END_CURVES);
                sqlBuffer.append(SEMICOLON);
            //    sqlBuffer.append(MARK_WRAP);
                break;
            //外键约束
            case ForeignKey:
                sqlBuffer.append(MARK_WRAP);
                sqlBuffer.append("-- Alter Table Add ForeignKey Constraint --");
                sqlBuffer.append(MARK_WRAP);
                sqlBuffer.append("alter table " + objectName)
                        .append(" add constraint " + "\""+constraint.get(2)+"\"")
                        .append(" foreign key" + MARK_BGN_CURVES);
                // 作用列
                fkCol = constraint.get(4).toString().split(MARK_COMMA);
                for (int f = 0; f < fkCol.length; f++) {
                    sqlBuffer.append(fkCol[f]);
                    if (f != fkCol.length - 1) {
                        sqlBuffer.append(MARK_COMMA);
                    }
                }
                sqlBuffer.append(MARK_END_CURVES);
                sqlBuffer.append(" references " + quotaDatabaseObjectDub(constraint.get(5), constraint.get(6)))
                        .append(MARK_BGN_CURVES);
                // 引用列
                pkCol = constraint.get(7).toString().split(MARK_COMMA);
                for (int p = 0; p < pkCol.length; p++) {
                    sqlBuffer.append(pkCol[p]);
                    if (p != pkCol.length - 1) {
                        sqlBuffer.append(MARK_COMMA);
                    }
                }
                sqlBuffer.append(MARK_END_CURVES);
                if (constraint.get(8) != null) {
                    sqlBuffer.append(" on update " + constraint.get(8));
                }
                if (constraint.get(9) != null) {
                    sqlBuffer.append(" on delete " + constraint.get(9));
                }
                sqlBuffer.append(SEMICOLON);
        //        sqlBuffer.append(MARK_WRAP);
                break;
            case UniqueConstraint:
                sqlBuffer.append(str);
                break;
            //值检查约束
            case CheckConstraint:
                sqlBuffer.append(MARK_WRAP);
                sqlBuffer.append("-- Alter Table Add Check Constraint --");
                sqlBuffer.append(MARK_WRAP);
                sqlBuffer.append("alter table " + objectName)
                        .append(" add constraint " + "\""+constraint.get(2)+"\"")
                        .append(" check" + MARK_BGN_CURVES)
                        //CHECK约束定义
                        .append(removeDatabaseObjectQuota1(constraint.get(10)))
                        .append(MARK_END_CURVES);
                sqlBuffer.append(SEMICOLON);
        //        sqlBuffer.append(MARK_WRAP);
                break;
            default:
                break;
        }
        return sqlBuffer.toString();
    }
    private boolean isIdentityPrimaryKey(Vector<Object> constraint, List<Object> identityCols) {
        if (identityCols == null || identityCols.isEmpty() || constraint == null || constraint.size() <= 4 || constraint.get(4) == null) {
            return false;
        }
        String[] pkCols = constraint.get(4).toString().split(MARK_COMMA);
        if (pkCols.length != 1) {
            return false;
        }
        String pkCol = normalizeColumnName(pkCols[0]);
        for (Object identityCol : identityCols) {
            if (identityCol != null && pkCol.equals(normalizeColumnName(identityCol.toString()))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeColumnName(String columnName) {
        return removeDatabaseObjectQuota(columnName == null ? "" : columnName.trim()).toUpperCase();
    }


    /**
     * 组装自增创建语句
     *
     * @param objectName
     * @param identity
     * @return
     */
    public String assembleTableIdentity(Object objectName, Vector<Object> identity) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append("-- Alter Table Add Identity --");
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("alter table ").append(objectName);
        sqlBuffer.append(" alter column ");
        //列名
        sqlBuffer.append(identity.get(2));
        //数据类型(需将源数据类型转换为目标数据类型)
        sqlBuffer.append(" " + identity.get(3).toString());
        sqlBuffer.append(" identity(");
        //初值
        sqlBuffer.append(identity.get(4)).append(",");
        //步长
        sqlBuffer.append(identity.get(7));
        sqlBuffer.append(MARK_END_CURVES);
        //是否主键
        if (Boolean.valueOf(identity.get(9).toString())) {
            sqlBuffer.append(" primary key");
        }
        sqlBuffer.append(SEMICOLON);
        sqlBuffer.append(MARK_WRAP);
       // sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }

    /**
     * 组装视图创建语句
     *
     * @param objectName
     * @param view
     * @return
     */
    public String assembleView(Object objectName, Vector<Object> view) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("-- Create View --");
        sqlBuffer.append(MARK_WRAP);
        //添加视图定义
        sqlBuffer.append(removeDatabaseObjectQuota1(view.get(2).toString()));
        sqlBuffer.append(SEMICOLON);
        sqlBuffer.append(MARK_WRAP);
        //添加视图注释
        if (!(view.get(4) == null)) {
            sqlBuffer.append("comment on view " + objectName + " is " + "'" + view.get(4) + "'");
            sqlBuffer.append(SEMICOLON);
        }
        sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }

    /**
     * 组装序列值创建语句
     *
     * @param objectName
     * @param sequence
     * @return
     */
    public String assembleSequence(Object objectName, Vector<Object> sequence) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("-- Create Sequence --");
        sqlBuffer.append(MARK_WRAP);
        //添加序列值名
        sqlBuffer.append("create sequence " + objectName);
        //添加序列值最小值
        sqlBuffer.append(" minvalue " + sequence.get(2));
        //添加最大值
        sqlBuffer.append(" maxvalue " + sequence.get(3));
        //起始值
        sqlBuffer.append(" start with " + sequence.get(0));
        //步长
        sqlBuffer.append(" increment by " + sequence.get(1));
        //是否循环
        if (Boolean.valueOf(sequence.get(4).toString())) {
            sqlBuffer.append(" cycle");
        } else {
            sqlBuffer.append(" no cycle");
        }
        sqlBuffer.append(SEMICOLON);
        //添加序列值注释
        if (!(sequence.get(5) == null)) {
            sqlBuffer.append(MARK_WRAP + " comments on sequence " + objectName + " is " + "'" + sequence.get(5).toString() + "'");
            sqlBuffer.append(SEMICOLON);
        }
        sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }


    /**
     * 组装包的DDL语句
     *
     * @param objectName
     * @param backage
     * @return
     */
    public String assemblePackage(Object objectName, Vector<Object> backage) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("-- Create Package --");
        sqlBuffer.append(MARK_WRAP);
        //添加包头，包体定义
        sqlBuffer.append(backage.get(0).toString());
        sqlBuffer.append(MARK_WRAP);
        //添加注释
        if (!(backage.get(1) == null)) {
            sqlBuffer.append("comment on package " + objectName + " is " + "'" + backage.get(1).toString() + "'");
            sqlBuffer.append(SEMICOLON);
        }
        sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }

    /**
     * 组装存储过程的DDl语句
     *
     * @param objectName
     * @param procedure
     * @return
     */
    public String assembleProcedure(Object objectName, Vector<Object> procedure) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("-- Create Procedure --");
        sqlBuffer.append(MARK_WRAP);
        //添加存储过程定义
        sqlBuffer.append(procedure.get(0).toString());
        sqlBuffer.append(MARK_WRAP);
        //添加存储过程注释
        if (!(procedure.get(1) == null)) {
            sqlBuffer.append("comment on procedure " + objectName + " is " + "'" + procedure.get(1).toString() + "'");
            sqlBuffer.append(SEMICOLON);
        }
        sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }


    /**
     * 组装存储函数的DDL语句
     *
     * @param objectName
     * @param function
     * @return
     */
    public String assembleFunction(Object objectName, Vector<Object> function) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("-- Create Function --");
        sqlBuffer.append(MARK_WRAP);
        //添加函数定义
        sqlBuffer.append(function.get(0).toString());
        sqlBuffer.append(MARK_WRAP);
        //添加函数注释
        if (!(function.get(1) == null)) {
            sqlBuffer.append("comment on function " + objectName + "'" + function.get(1).toString() + "'");
            sqlBuffer.append(SEMICOLON);
        }
        sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }

    /**
     * 组装触发器的DDL语句
     *
     * @param objectName
     * @param trigger
     * @return
     */

    public String assembleTrigger(Object objectName, Vector<Object> trigger) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("-- Create Trigger --");
        sqlBuffer.append(MARK_WRAP);
        //添加触发器定义
        sqlBuffer.append(trigger.get(0).toString());
        sqlBuffer.append(MARK_WRAP);
        //添加触发器注释
        if (!(trigger.get(1) == null)) {
            sqlBuffer.append("comment on trigger " + objectName + " is " + "'" + trigger.get(1).toString() + "'");
            sqlBuffer.append(SEMICOLON);
        }
        sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }


    /**
     * 组装同义词DDl语句
     *
     * @param objectName
     * @param synonym
     * @return
     */
    public String assembleSynonym(String[] objectName, Vector<Object> synonym) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("-- Create Synonym --");
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("create ");
        // 是否公有同义词
        sqlBuffer.append(Boolean.valueOf(synonym.get(0).toString()) ? "public " : "");
        // 同义词所属模式
        sqlBuffer.append("synonym ");
        sqlBuffer.append(objectName[0]);
        sqlBuffer.append(MARK_DOT);
        // 同义词名称
        sqlBuffer.append(objectName[1]);
        sqlBuffer.append(" for ");
        // 等价对象所属模式
        sqlBuffer.append(synonym.get(2));
        sqlBuffer.append(MARK_DOT);
        // 等价对象名
        sqlBuffer.append(synonym.get(1));
        sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }

    /**
     * 组装 udt DDl语句
     *
     * @param objectName
     * @param udt
     * @return
     */
    public String AssembleUdt(String[] objectName, Vector<Object> udt) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append(" --Create udt --");
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append(udt.get(0));
        //添加自定义数据类型定义
        sqlBuffer.append(MARK_WRAP + udt.get(1));
        sqlBuffer.append(MARK_WRAP);
        //添加注释
        if (!(udt.get(2) == null)) {
            sqlBuffer.append("comment on object " + objectName[1] + " is " + "'" + udt.get(2) + "'");
            sqlBuffer.append(SEMICOLON);
        }
        sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }

    /**
     * 组装job ddl语句
     *
     * @param objectName
     * @param job
     * @return
     */
    public String assembleJob(String[] objectName, Vector<Object> job) {
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append(" --Create Job --");
        sqlBuffer.append(MARK_WRAP);
        sqlBuffer.append("dbms_scheduler.create_job" + MARK_BGN_CURVES);
        sqlBuffer.append(MARK_WRAP);
        //作业名
        sqlBuffer.append("job_name                  =>" + MARK_SIN_QUOTATION + job.get(0) + MARK_SIN_QUOTATION + MARK_COMMA);
        sqlBuffer.append(MARK_WRAP);
        //作业类型
        sqlBuffer.append("job_type                  =>" + MARK_SIN_QUOTATION + job.get(1) + MARK_SIN_QUOTATION + MARK_COMMA);
        sqlBuffer.append(MARK_WRAP);
        //作业动作
        sqlBuffer.append("job_action                =>" + MARK_SIN_QUOTATION + job.get(2) + MARK_SIN_QUOTATION + MARK_COMMA);
        sqlBuffer.append(MARK_WRAP);
        //参数个数
        sqlBuffer.append("job_param_num             =>" + job.get(3) + MARK_COMMA);
        sqlBuffer.append(MARK_WRAP);
        //开始时间
        sqlBuffer.append("begin_t                   =>" + MARK_SIN_QUOTATION + job.get(4) + MARK_SIN_QUOTATION + MARK_COMMA);
        sqlBuffer.append(MARK_WRAP);
        //频度描述字串
        sqlBuffer.append("repet_interval            =>" + MARK_SIN_QUOTATION + job.get(5) + MARK_SIN_QUOTATION + MARK_COMMA);
        sqlBuffer.append(MARK_WRAP);
        //结束时间
        sqlBuffer.append("end_t                     =>" + job.get(6) + MARK_COMMA);
        sqlBuffer.append(MARK_WRAP);
        //该作业是否允许
        sqlBuffer.append("enable                    =>" + job.get(7) + MARK_COMMA);
        sqlBuffer.append(MARK_WRAP);
        //作业到达结束时间后是否删除
        sqlBuffer.append("auto_drop                 =>" + job.get(8));
        sqlBuffer.append(MARK_WRAP);
        //作业注释
        if (!(job.get(2) == null)) {
            sqlBuffer.append("comments                  =>" + "comment on job " + objectName[1] + " is " + "'" + job.get(2) + "'");
            sqlBuffer.append(MARK_WRAP);
            sqlBuffer.append(MARK_END_CURVES + SEMICOLON);
        }
        sqlBuffer.append(MARK_WRAP);
        return sqlBuffer.toString();
    }


}




