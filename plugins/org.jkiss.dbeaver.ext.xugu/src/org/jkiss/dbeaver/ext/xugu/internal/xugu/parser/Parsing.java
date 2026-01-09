package org.jkiss.dbeaver.ext.xugu.internal.xugu.parser;

import org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata.Constants.DatabaseObjectType;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata.IndexMeta;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.DatabaseObjectParsing;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.DatabaseParsing;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.ObjectParsing;
import com.xugu.permission.LoadPermission;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map.Entry;


/**
 * @author
 * 加载数据库对象ddl语句类（主要为对象级，用户级，模式级加载）
 */
@SuppressWarnings("unchecked")
public class Parsing {

	/**
	 * 日志
	 */
//	private Logger log = LoggerFactory.getLogger(Parsing.class);
	
    /**
     * Table中行数据
     */    
    private Connection currentConn;

	/**
	 * 模式名
	 */
	private String schemaName;

	/**
	 * 表名
	 */
	private String tableName;

	/**
	 * 视图名
	 */
    private  String viewName;

	private org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.ObjectParsing objectParsing = new org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.ObjectParsing();

	private static  Parsing parsing = new Parsing();

	private static DatabaseObjectParsing databaseObjectParsing = new DatabaseObjectParsing();



	/**
	 * @author zhangkun
	 * @date 2020/4/24  9:03
	 * @description 系统表枚举
	 **/
	 public enum TableType{
		/**
		 * SYS系统表前缀
		 */
		SYS,

		/**
		 * DBA系统表前缀
		 */
		DBA,

		/**
		 * ALL系统表前缀
		 */
		ALL
	}

//	/**
//	 * 对象类型枚举
//	 */
//	public enum ObjectType{
//	 	TABLE,VIEW,SEQUENCE,PACKAGE,PROCEDURE,FUNCTION,TRIGGER,SYNONYM,UDT,JOB
//	}

	/**
	 * 获取指定用户库下的DDL 系统表类型默认为ALL
	 *
	 * @param conn
	 * @param dbName
	 * @return
	 */
	public String getDataBaseDDL(Connection conn, String dbName)  {
		//默认系统表类型为ALL
		TableType tableType = TableType.ALL;
		try {
			if (dbName == null) {
				throw new Exception("库名为空");
			}
		} catch (Exception e) {
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		try {
			if (dbName == "") {
				throw new Exception("库名为空串");
			}
		} catch (Exception e) {
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return parsing.getDataBaseDDL(conn, dbName, tableType);
	}


	/**
	 * 获取指定用户库下的DDL
	 * @param conn
	 * @param dbName
	 * @param tableType
	 * @return
	 */
	public String getDataBaseDDL(Connection conn ,String dbName,TableType tableType) {
		try{
			if(dbName == null){
				throw new Exception("库名为空");
			}
		}
		catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		try{
			if(dbName == ""){
				throw new Exception("库为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return parsing.loadDataBaseDDL(conn,dbName,tableType);
	}

	/**
	 *  获取指定模式下的DDL 默认系统表类型为ALL
	 * @param conn
	 * @param schemaName
	 * @return
	 */
	public String  getSchemaDDL(Connection conn,String schemaName){
		TableType tableType = TableType.ALL;
		try{
			if(schemaName == null){
				throw new Exception("模式名为空");
			}
		}
		catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		try{
			if(schemaName == ""){
				throw new Exception("模式名为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return parsing.getSchemaDDL(conn,schemaName,tableType);
	}

	/**
	 * 获取当前库指定模式下的对象DDL
	 * @param conn
	 * @param schemaName
	 * @param tableType
	 * @return
	 */
	public String getSchemaDDL(Connection conn, String schemaName,TableType tableType){
		try{
			if(schemaName == null){
				throw new Exception("模式名为空");
			}
		}
		catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		try{
			if(schemaName == ""){
				throw new Exception("模式名为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return parsing.loadSchemaDDL(conn,schemaName,tableType);
	}

	/**
	 * 获取指定用户名下的DDL,默认系统表类型为ALL
	 * @param conn
	 * @param userName
	 * @return
	 */
	public String getUserDDL(Connection conn,String userName){
		TableType tableType = TableType.ALL;
		try{
			if(userName == null){
				throw new Exception("用户名为空");
			}
		}
		catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		try{
			if(userName == ""){
				throw new Exception("用户名为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return parsing.getUserDDL(conn,userName,tableType);
	}


	/**
	 * 获取指定用户名下的DDL，指定tableType
	 * @param conn
	 * @param userName
	 * @param tableType
	 * @return
	 */
	public String getUserDDL(Connection conn,String userName,TableType tableType){
		 try{
		 	if(userName == null){
		 		throw new Exception("用户名为空");
			}
		 }
		 catch(Exception e){
//		log.error(e.toString());
			 System.out.println(e.getMessage());
		 }
		 try{
		 	if(userName == ""){
		 		throw new Exception("用户名为空串");
			}
		 }catch (Exception e){
//		log.error(e.toString());
			 System.out.println(e.getMessage());
		 }
		 return parsing.loadUserDDL(conn,userName,tableType);
	}

	/**
	 * 获取指定模式指定对象的DDL，默认系统表类型为ALL
	 * @param conn
	 * @param schemaName
	 * @param objectName
	 * @param objectType
	 * @return
	 */
	public String getObjectDDL(Connection conn,String schemaName,String objectName,String objectType){
		//默认系统表类型为ALL
		TableType tableType = TableType.ALL;
		try{
			if( schemaName == null || objectName == null|| objectType == null){
				throw new Exception("模式名或对象名或对象类型为空");
			}
		}catch (Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		try{
			if(schemaName==""||objectName==""||objectType==""){
				throw new Exception("模式名或对象名或对象类型为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return 	parsing.getObjectDDL(conn,schemaName,objectName,objectType,tableType);
	}

	/**
	 * 获取指定模式下指定对象的DDL
	 * @param conn
	 * @param schemaName
	 * @param objectName
	 * @param objectType
	 * @return
	 */
	public String getObjectDDL(Connection conn,String schemaName,String objectName,String objectType,TableType tableType){
		try{
			XuguDDLUtils.initKeyWords(conn);
			if( schemaName == null || objectName == null|| objectType == null){
				throw new Exception("模式名或对象名或对象类型为空");
			}
		}catch (Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		try{
			if(schemaName==""||objectName==""||objectType==""){
				throw new Exception("模式名或对象名或对象类型为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return parsing.loadObjectDDL(conn,schemaName,objectName,objectType,tableType);
	}

	/**
	 *获取指定模式下的所有表DDL,默认系统表类型为ALL
	 * @param conn
	 * @param schemaName
	 * @return
	 */
	public String  getTableDDL(Connection conn,String schemaName){
		//默认系统表类型为ALL
		TableType tableType = TableType.ALL;
		try{
			if(schemaName==null){
				throw  new Exception("模式名为空");
			}
		}catch (Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		try{
			if(schemaName==""){
				throw  new Exception("模式名为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		return parsing.getTableDDL(conn,schemaName,tableType);
	}

	/**
	 * 获取指定模式下的所有表定义，
	 * @param conn
	 * @param schemaName
	 * @param tableType
	 * @return
	 */
	public String  getTableDDL(Connection conn,String schemaName,TableType tableType){
		try{
			if(schemaName==null){
				throw  new Exception("模式名为空");
			}
		}catch (Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		try{
			if(schemaName==""){
				throw  new Exception("模式名为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		String[] schemaNames = new String[]{
				ObjectParsing.quotaDatabaseObjectSin(schemaName)
		};
		Vector<Vector<Object>> objects;
		// 收集模式下的数据库对象
		objects = objectParsing.collectobjectsbyschema(conn, schemaNames,tableType);
		String ddl ="--ddl--";
		if(objects.size()>0) {
			for (Vector<Object> object : objects) {
				if (object.get(2) == (Integer) 5 && object.get(3) == (Boolean) false) {
					//表
					ddl += this.loadTableDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
				}
			}
		}
		return ddl;
	}


	/**
	 * 获取指定模式下的所有表定义，
	 * @param conn
	 * @param schemaName
	 * @param tableType
	 * @return
	 */
	public Map<String,String> getTableDDLForMap(Connection conn, String schemaName, TableType tableType){
		try{
			if(schemaName==null){
				throw  new Exception("模式名为空");
			}
		}catch (Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		try{
			if(schemaName==""){
				throw  new Exception("模式名为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		String[] schemaNames = new String[]{
				ObjectParsing.quotaDatabaseObjectSin(schemaName)
		};
		Vector<Vector<Object>> objects;
		// 收集模式下的数据库对象
		objects = objectParsing.collectobjectsbyschema(conn, schemaNames,tableType);
//		String ddl ="--ddl--";
		Map map = new HashMap<String, String>();
		String tableName = null;
		if(objects.size()>0) {
			for (Vector<Object> object : objects) {
				if (object.get(2) == (Integer) 5 && object.get(3) == (Boolean) false) {
					tableName = object.get(0).toString()+"."+object.get(1).toString();
					//表
				map.put(tableName,this.loadTableDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
				}
			}
		}
		return map;
	}

	/**
	 *获取指定模式下的所有表DDL,默认系统表类型为ALL
	 * @param conn
	 * @param schemaName
	 * @return
	 */
	public Map<String,String>  getTableDDLForMap(Connection conn,String schemaName){
		//默认系统表类型为ALL
		TableType tableType = TableType.ALL;
		try{
			if(schemaName==null){
				throw  new Exception("模式名为空");
			}
		}catch (Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		try{
			if(schemaName==""){
				throw  new Exception("模式名为空串");
			}
		}catch (Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		return parsing.getTableDDLForMap(conn,schemaName,tableType);
	}




	/**
	 * 加载指定用户下对象的DDL
	 * @param conn
	 * @param userName
	 * @param tableType
	 * @return
	 */
	public String loadUserDDL(Connection conn,String userName,TableType tableType){
		Vector<Vector<Object>> objects;
		String ddl ="--ddl--";
		String[] userNames = new String[]{
				ObjectParsing.quotaDatabaseObjectSin(userName)
		};
		//收集user下的数据库对象
		objects = objectParsing.collectObjectsByUser(conn, userNames,tableType);
		if(objects.size()>0){
			for (Vector<Object> object : objects) {
				if (object.get(2) == (Integer) 5 && object.get(3) == (Boolean) false) {
					//表
					ddl += this.loadTableDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
				} else if (object.get(2) == (Integer) 9 && object.get(3) == (Boolean) false) {
					//视图
					ddl += this.loadViewDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
				} else if (object.get(2) == (Integer) 8 && object.get(3) == (Boolean) false) {
					//序列值
					ddl += this.loadSequenceDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
				} else if (object.get(2) == (Integer) 18 && object.get(3) == (Boolean) false) {
					//包
					ddl += this.loadPackageDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
				} else if (object.get(2) == (Integer) 7 && object.get(3) == (Boolean) false) {
					//存储过程或函数
					ddl += this.loadProcedureDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
				} else if (object.get(2) == (Integer) 11 && object.get(3) == (Boolean) false) {
					//触发器
					ddl += this.loadTriggerDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
				}
				else if (object.get(2) == (Integer) 19 && object.get(3) == (Boolean) false) {
					//UDT
					ddl += this.loadUDTDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
				}
			}
		}
		//获取用户同名模式下的所有同义词对象
		Vector<Vector<Object>> synonymes = objectParsing.collectSynonymBySchema(conn,userNames,tableType);
		if(synonymes.size()>0){
			//获取所有同义词定义语句
			for(int i =0; i<synonymes.size(); i++){
				ddl+=this.loadSynonymDDL(conn,schemaName,synonymes.get(i).get(1).toString(),tableType);
			}
		}
		return  ddl;
	}

	/**
	 * 加载指定模式下的DDL
	 * @param conn
	 * @param schemaName
	 * @param tableType
	 * @return
	 */
	public String loadSchemaDDL(Connection conn,String schemaName,TableType tableType){
		String ddl ="--ddl--";
		Vector<Vector<Object>> objects;
		List<String> ddlList = new ArrayList<>();
		String[] schemaNames = new String[]{
				ObjectParsing.quotaDatabaseObjectSin(schemaName)
		};
		// 收集模式下的数据库对象
		objects = objectParsing.collectobjectsbyschema(conn, schemaNames,tableType);
		if(objects.size()>0){
			for (Vector<Object> object : objects) {
				if (object.get(2) == (Integer) 5 && object.get(3) == (Boolean) false) {
					//表
					ddlList.add(this.loadTableDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
				} else if (object.get(2) == (Integer) 9 && object.get(3) == (Boolean) false) {
					//视图
					ddlList.add(this.loadViewDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
				} else if (object.get(2) == (Integer) 8 && object.get(3) == (Boolean) false) {
					//序列值
					ddlList.add(this.loadSequenceDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
				} else if (object.get(2) == (Integer) 18 && object.get(3) == (Boolean) false) {
					//包
					ddlList.add(this.loadPackageDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
				} else if (object.get(2) == (Integer) 7 && object.get(3) == (Boolean) false) {
					//存储过程或函数
					ddlList.add(this.loadProcedureDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
				} else if (object.get(2) == (Integer) 11 && object.get(3) == (Boolean) false) {
					//触发器
					ddlList.add(this.loadTriggerDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
				}
				else if (object.get(2) == (Integer) 19 && object.get(3) == (Boolean) false) {
					//UDT
					ddlList.add(this.loadUDTDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
				}
			}
		}
		//获取模式下的所有同义词对象
		Vector<Vector<Object>> synonyms = objectParsing.collectSynonymBySchema(conn,schemaNames,tableType);
		if(synonyms.size()>0){
			//获取所有同义词定义语句
			for(int i =0; i<synonyms.size(); i++){
				ddlList.add(this.loadSynonymDDL(conn,schemaName,synonyms.get(i).get(1).toString(),tableType));
			}
		}
		for (int i = 0; i <ddlList.size() ; i++) {
			ddl += ddlList.get(i);
		}
		return ddl;
	}

	/**
	 * 加载指定用户库下的DDL
 	 * @param conn
	 * @param dataBaseName
	 * @param tableType
	 * @return
	 */
	public String  loadDataBaseDDL(Connection conn,String dataBaseName,TableType tableType) {
		StringBuilder ddl = new StringBuilder("\r\n--ddl--\r\n");
		String sys="SYS";
		Vector<String> schemas;
		Vector<Vector<Object>> objects;
		Vector<String> jobs;
		List<String> ddlList = new ArrayList<>();
		int dbId=0;
		String[] databaseNames = new String[]{
				ObjectParsing.quotaDatabaseObjectSin(dataBaseName)
		};
		DatabaseParsing databaseParsing =new DatabaseParsing();
		//获取库名
		dbId=objectParsing.getDatabaseId(conn,databaseNames,tableType);
		//获取数据库的所有模式
		schemas=objectParsing.collectSchemasByDatabase(conn,databaseNames,tableType);
//		for(int a =0;a<schemas.size();a++){
//			log.info(schemas.get(a).toString());
//		}
		for (int i=0;i<schemas.size();i++) {
			String[] schema_name = new String[]{ObjectParsing.quotaDatabaseObjectSin(schemas.get(i))};
			//获取模式下的所有对象
			objects = objectParsing.collectobjectsbyschema(conn, dataBaseName, schema_name, tableType);
//			for (Vector<Object> object : objects){
//				log.info(object.toString());
//			}
			if(objects.size()>0) {
				for (Vector<Object> object : objects) {
					if (object.get(3) == (Integer) 5 && object.get(4) == (Boolean) false) {
						//表
						ddlList.add(databaseParsing.loadTableDdl(conn, (Integer) object.get(1), schemas.get(i), object.get(2).toString(), tableType));
					} else if (object.get(3) == (Integer) 9 && object.get(4) == (Boolean) false) {
						//视图
						ddlList.add(databaseParsing.loadViewDdl(conn, (Integer) object.get(1), schemas.get(i), object.get(2).toString(), tableType));
					} else if (object.get(3) == (Integer) 8 && object.get(4) == (Boolean) false) {
						//序列值
						ddlList.add(databaseParsing.loadSequenceDdl(conn, (Integer) object.get(1), schemas.get(i), object.get(2).toString(), tableType));
					} else if (object.get(3) == (Integer) 18 && object.get(4) == (Boolean) false) {
						//包
						ddlList.add(databaseParsing.loadPackageDdl(conn, (Integer) object.get(1), schemas.get(i), object.get(2).toString(), tableType));
					} else if (object.get(3) == (Integer) 7 && object.get(4) == (Boolean) false) {
						//存储过程或函数
						ddlList.add(databaseParsing.loadProcedureDdl(conn, (Integer) object.get(1), schemas.get(i), object.get(2).toString(), tableType));
					} else if (object.get(3) == (Integer) 11 && object.get(4) == (Boolean) false) {
						//触发器
						ddlList.add(databaseParsing.loadTriggerDdl(conn, (Integer) object.get(1), schemas.get(i), object.get(2).toString(), tableType));
					} else if (object.get(3) == (Integer) 19 && object.get(4) == (Boolean) false) {
						//UDT
						ddlList.add(databaseParsing.loadUdtDdl(conn, (Integer) object.get(1), schemas.get(i), object.get(2).toString(), tableType));
					}
				}
			}
			//获取模式下的所有同义词对象
			Vector<Vector<Object>> synonyms = databaseObjectParsing.collectSynonymBySchema(conn,dbId,schema_name,tableType);
			if(synonyms.size()>0){
				//获取所有同义词定义语句
				for(int j =0; j<synonyms.size(); j++){
					ddlList.add(databaseParsing.loadSynonymDdl(conn,dbId,schemas.get(i).toString(),synonyms.get(j).get(1).toString(),tableType));
				}
			}
		}
		//获取数据库中所有的定时作业
		jobs=objectParsing.collectJobsByDatabase(conn,dbId,tableType);
		if(jobs.size()>0){
			//定时作业
			for (String job: jobs) {
				ddlList.add(databaseParsing.loadJobDdl(conn, (Integer) dbId, job, tableType));
			}
		}
		for (int i = 0; i <ddlList.size() ; i++) {
			ddl.append(ddlList.get(i));
		}
		// 角色
		try (Statement statement = conn.createStatement()) {
			String sql = String.format("SELECT R.USER_NAME FROM %s_USERS R" +
					" JOIN %s_DATABASES D ON R.DB_ID=D.DB_ID" +
					" WHERE R.IS_ROLE=TRUE AND D.DB_NAME='%s'",
					tableType, tableType, dataBaseName);
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				ddl.append(loadTheRoleDDL(conn, resultSet.getString(1)));
			}
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
		// 用户
		try (Statement statement = conn.createStatement()) {
			String sql = String.format("SELECT R.USER_NAME FROM %s_USERS R" +
							" JOIN %s_DATABASES D ON R.DB_ID=D.DB_ID" +
							" WHERE R.IS_ROLE=FALSE AND D.DB_NAME='%s'",
					tableType, tableType, dataBaseName);
			ResultSet resultSet = statement.executeQuery(sql);
			while (resultSet.next()) {
				ddl.append(loadTheUserDDL(conn, resultSet.getString(1), "CHANGE_PASSWORD", tableType));
			}
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
		return ddl.toString();
	}

	/**
	 * 加载指定对象的DDL
	 * @param conn
	 * @param schemaName
	 * @param objectName
	 * @param objectType
	 * @param tableType
	 * @return
	 */
	public String loadObjectDDL(Connection conn,String schemaName,String objectName,String objectType, TableType tableType){
			switch (objectType) {
				case "TABLE":
				case "table":
					return this.loadTableDDL(conn, schemaName, objectName, tableType);
				case "VIEW":
				case "view":
					return this.loadViewDDL(conn, schemaName, objectName, tableType);
				case "SEQUENCE":
				case "sequence":
					return this.loadSequenceDDL(conn, schemaName, objectName, tableType);
				case "PACKAGE":
				case "package":
					return this.loadPackageDDL(conn, schemaName, objectName, tableType);
				case "PROCEDURE":
				case "procedure":
					return this.loadProcedureDDL(conn, schemaName, objectName, tableType);
				case "FUNCTION":
				case "function":
					return this.loadFunctionDDL(conn, schemaName, objectName, tableType);
				case "TRIGGER":
				case "trigger":
					return this.loadTriggerDDL(conn, schemaName, objectName, tableType);
				case "SYNONYM":
				case "synonym":
					return this.loadSynonymDDL(conn, schemaName, objectName, tableType);
				case "UDT":
				case "udt":
					return this.loadUDTDDL(conn, schemaName, objectName, tableType);
				case "JOB":
				case "job":
					return this.loadJobDDL(conn, schemaName, objectName, tableType);
				default:
					break;
			}
			return "该对象不存在";
	}



	/**
	 * @author zhangkun
	 * @date 2020/4/24 16:40
	 * @description 获取ddl
	 **/
	public String loadDDL( Connection conn,String schemaName,String objectName,String objectType, TableType tableType){
		//args[0] 当前连接，args[1] 模式名，args[2] 对象名，args[3] 对象类型,args[4] 系统表类型
		ObjectParsing objectParsing = new ObjectParsing();
		Vector<Vector<Object>> objects;
		Vector<String> schemas;
		Vector<String> jobs;
		List<String> ddlList = new ArrayList<>();
		String ddl = "---ddl---";
		switch (objectType) {
			case "TABLE":
			case "table":
				  return this.loadTableDDL(conn, schemaName, objectName, tableType);
			case "VIEW":
			case "view":
				 return this.loadViewDDL(conn, schemaName, objectName, tableType);
			case "SEQUENCE":
			case "sequence":
				return this.loadSequenceDDL(conn, schemaName, objectName, tableType);
			case "PACKAGE":
			case "package":
				return this.loadPackageDDL(conn, schemaName, objectName, tableType);
			case "PROCEDURE":
			case "procedure":
				return this.loadProcedureDDL(conn, schemaName, objectName, tableType);
			case "FUNCTION":
			case "function":
				return this.loadFunctionDDL(conn, schemaName, objectName, tableType);
			case "TRIGGER":
			case "trigger":
				return this.loadTriggerDDL(conn, schemaName, objectName, tableType);
			case "SYNONYM":
			case "synonym":
				return this.loadSynonymDDL(conn, schemaName, objectName, tableType);
			case "UDT":
			case "udt":
				return this.loadUDTDDL(conn, schemaName, objectName, tableType);
			case "JOB":
			case "job":
				return this.loadJobDDL(conn, schemaName, objectName, tableType);
			case "USER":
			case "user":
				String[] userName = new String[]{
						ObjectParsing.quotaDatabaseObjectSin(schemaName)
				};
				//收集user下的数据库对象
				objects = objectParsing.collectObjectsByUser(conn, userName,tableType);

				for (Vector<Object> object : objects) {
					if (object.get(2) == (Integer) 5 && object.get(3) == (Boolean) false) {
						//表
						ddl += this.loadTableDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
					} else if (object.get(2) == (Integer) 9 && object.get(3) == (Boolean) false) {
						//视图
						ddl += this.loadViewDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
					} else if (object.get(2) == (Integer) 8 && object.get(3) == (Boolean) false) {
						//序列值
						ddl += this.loadSequenceDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
					} else if (object.get(2) == (Integer) 18 && object.get(3) == (Boolean) false) {
						//包
						ddl += this.loadPackageDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
					} else if (object.get(2) == (Integer) 7 && object.get(3) == (Boolean) false) {
						//存储过程或函数
						ddl += this.loadProcedureDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
					} else if (object.get(2) == (Integer) 11 && object.get(3) == (Boolean) false) {
						//触发器
						ddl += this.loadTriggerDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
					}
//					else if (object.get(2) == (Integer) 15 && object.get(3) == (Boolean) false) {
//						//同义词
//						ddl += this.loadSynonymDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
//					}
					else if (object.get(2) == (Integer) 19 && object.get(3) == (Boolean) false) {
						//UDT
						ddl += this.loadUDTDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType);
					}
				}
				//获取用户同名模式下的所有同义词对象
				Vector<Vector<Object>> synonymes = objectParsing.collectSynonymBySchema(conn,userName,tableType);
				//获取所有同义词定义语句
				for(int i =0; i<synonymes.size(); i++){
					ddl+=this.loadSynonymDDL(conn,schemaName,synonymes.get(i).get(1).toString(),tableType);
				}
				return  ddl;
			case "SCHEMA":
			case "schema":
				String[] schemaNames = new String[]{
						ObjectParsing.quotaDatabaseObjectSin(schemaName)
				};
				// 收集模式下的数据库对象
				objects = objectParsing.collectobjectsbyschema(conn, schemaNames,tableType);
//				for (int i = 0; i <objects.size() ; i++) {
//					System.out.println("对象"+objects.get(i));
//				}
				for (Vector<Object> object : objects) {
					if (object.get(2) == (Integer) 5 && object.get(3) == (Boolean) false) {
						//表
						ddlList.add(this.loadTableDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					//	System.out.println(this.loadTableDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					} else if (object.get(2) == (Integer) 9 && object.get(3) == (Boolean) false) {
						//视图
						ddlList.add(this.loadViewDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					//	System.out.println(this.loadViewDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					} else if (object.get(2) == (Integer) 8 && object.get(3) == (Boolean) false) {
						//序列值
						ddlList.add(this.loadSequenceDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					//	System.out.println(this.loadSequenceDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					} else if (object.get(2) == (Integer) 18 && object.get(3) == (Boolean) false) {
						//包
						ddlList.add(this.loadPackageDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					//	System.out.println(this.loadPackageDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					} else if (object.get(2) == (Integer) 7 && object.get(3) == (Boolean) false) {
						//存储过程或函数
						ddlList.add(this.loadProcedureDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					//	System.out.println(this.loadProcedureDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					} else if (object.get(2) == (Integer) 11 && object.get(3) == (Boolean) false) {
						//触发器
						ddlList.add(this.loadTriggerDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					//	System.out.println(this.loadTriggerDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					}
//					else if (object.get(2) == (Integer) 15 && object.get(3) == (Boolean) false) {
//						//同义词
//						ddlList.add(this.loadSynonymDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
//					//	System.out.println(this.loadSynonymDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
//					}
					else if (object.get(2) == (Integer) 19 && object.get(3) == (Boolean) false) {
						//UDT
						ddlList.add(this.loadUDTDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					//	System.out.println(this.loadUDTDDL(conn, object.get(0).toString(), object.get(1).toString(), tableType));
					}
				}
				//获取模式下的所有同义词对象
				Vector<Vector<Object>> synonyms = objectParsing.collectSynonymBySchema(conn,schemaNames,tableType);
				//获取所有同义词定义语句
				for(int i =0; i<synonyms.size(); i++){
						ddlList.add(this.loadSynonymDDL(conn,schemaName,synonyms.get(i).get(1).toString(),tableType));
					}
				for (int i = 0; i <ddlList.size() ; i++) {
					ddl += ddlList.get(i);
				}
				return ddl;
			case "DATABASE":
			case "database":
				String sys="SYS";
				int db_id=0;
				String[] databaseName = new String[]{
						ObjectParsing.quotaDatabaseObjectSin(schemaName)
				};
				DatabaseParsing databaseParsing =new DatabaseParsing();
				//获取数据库的所有模式
				schemas=objectParsing.collectSchemasByDatabase(conn,databaseName,tableType);
				for (int i=0;i<schemas.size();i++) {
					String[] schema_name = new String[]{ObjectParsing.quotaDatabaseObjectSin(schemas.get(i))};

					//获取模式下的所有对象
					objects = objectParsing.collectobjectsbyschema(conn, schemaName, schema_name, tableType);
					for (Vector<Object> object : objects){
						if (object.get(3) == (Integer) 5 && object.get(4) == (Boolean) false) {
							//表
							return databaseParsing.loadTableDdl(conn,(Integer) object.get(1), schema_name[0], object.get(2).toString(), tableType);
						} else if (object.get(3) == (Integer) 9 && object.get(4) == (Boolean) false) {
							//视图
							return databaseParsing.loadViewDdl(conn,(Integer) object.get(1), schema_name[0], object.get(2).toString(), tableType);
						} else if (object.get(3) == (Integer) 8 && object.get(4) == (Boolean) false) {
							//序列值
							return databaseParsing.loadSequenceDdl(conn, (Integer) object.get(1),schema_name[0], object.get(2).toString(), tableType);
						} else if (object.get(3) == (Integer) 18 && object.get(4) == (Boolean) false) {
							//包
							return databaseParsing.loadPackageDdl(conn, (Integer) object.get(1),schema_name[0], object.get(2).toString(), tableType);
						} else if (object.get(3) == (Integer) 7 && object.get(4) == (Boolean) false) {
							//存储过程或函数
							return databaseParsing.loadProcedureDdl(conn, (Integer) object.get(1),schema_name[0], object.get(2).toString(), tableType);
						} else if (object.get(3) == (Integer) 11 && object.get(4) == (Boolean) false) {
							//触发器
							return databaseParsing.loadTriggerDdl(conn, (Integer) object.get(1),schema_name[0], object.get(2).toString(), tableType);
						}
//						else if (object.get(3) == (Integer) 15 && object.get(4) == (Boolean) false) {
//							//同义词
//							return databaseParsing.loadSynonymDDL(conn,(Integer) object.get(1), schema_name[0], object.get(2).toString(), sys);
//						}
						else if (object.get(3) == (Integer) 19 && object.get(4) == (Boolean) false) {
							//UDT
							return databaseParsing.loadUdtDdl(conn, (Integer) object.get(1),schema_name[0], object.get(2).toString(), tableType);
						}
					}

				}

				//获取库名
				db_id=objectParsing.getDatabaseId(conn,databaseName,tableType);
				//获取数据库中所有的定时作业
				jobs=objectParsing.collectJobsByDatabase(conn,db_id,tableType);
				//定时作业
				for (String job: jobs) {
					return databaseParsing.loadJobDdl(conn, (Integer) db_id, job, tableType);
				}
			default:
				break;
		}
		return null;
	}

	/**
	 *获取表的ddl语句
	 * @param currentConn
	 * @param schemaName
	 * @param tableName
	 * @return
	 */
	public String loadTableDDL(Connection currentConn, String schemaName, String tableName,TableType table_type) {
    this.currentConn = currentConn;
    this.schemaName = schemaName;
    this.tableName = tableName;
    String test = null;
    List<Object> identity_cols = new ArrayList<>();
    List<Object> unique_cols = new ArrayList<>();
    try {
    	objectParsing.initSearchTaskObject();
    	String[] object = new String[]{schemaName,tableName};
    	//收集数据库源对象
    	objectParsing.collectDatabaseObject(currentConn, object, null,table_type);
		//获取所有任务对象的键值对
        Set<Entry<String, Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
        //遍历任务对象键值对
    	for(Entry<String, Vector<Object>> task : taskHashMap) {
			//传入参数为目的对象，列信息，表分区
			test = objectParsing.assembleXuGuTable(task.getValue().get(1), (Vector<Vector<Object>>) task.getValue().get(3), (Vector<Object>) task.getValue().get(5), schemaName, tableName, (Integer)task.getValue().get(17), (Integer)((Vector) ((Vector) task.getValue().get(18)).get(0)).get(0)) + "\n";
			//表自增
			Vector<Vector<Object>> identityVec = (Vector<Vector<Object>>) task.getValue().get(6);
			for (Vector<Object> identity : identityVec) {
				identity_cols.add(identity.get(2));
				//自增
				test += objectParsing.assembleTableIdentity(task.getValue().get(1), identity);
			}
			//表约束
			Vector<Vector<Object>> tableConstraintVector = (Vector<Vector<Object>>) task.getValue().get(4);
			for (Vector<Object> constraint : tableConstraintVector) {
				switch (Integer.parseInt(constraint.get(11).toString())) {
					//主键
					case 0:
						// ******************************************组装表主键***************************************
						test += objectParsing.assembleTableConstraint(task.getValue().get(1), constraint, DatabaseObjectType.PrimaryKey, identity_cols) + "\n";
						// ****************************************************************************************
						break;
					//外键
					case 1:
						// ******************************************组装表外键***************************************
						test += objectParsing.assembleTableConstraint(task.getValue().get(1), constraint, DatabaseObjectType.ForeignKey, identity_cols) + "\n";
						// ****************************************************************************************
						break;
					//唯一值
					case 2:
						// ******************************************组装表唯一值*************************************
						test += objectParsing.assembleTableConstraint(task.getValue().get(1), constraint, DatabaseObjectType.UniqueConstraint, identity_cols) + "\n";
						// ****************************************************************************************
						break;
					//值检查
					case 3:
						// ******************************************组装表值检查*************************************
						test += objectParsing.assembleTableConstraint(task.getValue().get(1), constraint, DatabaseObjectType.CheckConstraint, identity_cols) + "\n";
						// ****************************************************************************************
						break;
					default:
						break;
				}
			}
			Vector<IndexMeta> indexs = (Vector<IndexMeta>) task.getValue().get(7);

			//当存在自增长字段时，遍历自增长字段集合，凡是索引字段等于自增长字段的不获取索引ddl
			if (identity_cols.size() > 0) {
				for (IndexMeta index : indexs) {
					for (Object identity_col : identity_cols) {

						if (index.getIndexColumns().equals(identity_col)) {
							test += "";
						}else{
							if (index.isPrimaryKey() == true) {
								test += "";
							} else {
								//执行语句
								test += index.getSql() + "\n";
							}
						}
					}
				}
			}
			else{
				for (IndexMeta index : indexs) {
					if (index.isPrimaryKey() == true) {
						test += "";
					} else {
						//执行语句
						test += index.getSql() + "\n";
					}
				}
			}
    		return test;
    	} 		
	} catch (Exception e) {
//		log.error(e.toString());
		System.out.println(e.getMessage());
	}
	return test;
    }

	/**
	 * 获取视图的ddl语句
	 * @param currentConn
	 * @param schemaName
	 * @param viewName
	 * @param table_type
	 * @return
	 */
	public  String loadViewDDL(Connection currentConn , String schemaName, String viewName,TableType table_type){
    	this.currentConn=currentConn;
    	this.schemaName=schemaName;
    	this.viewName=viewName;
    	String test = null;
    	try{
    		objectParsing.initSearchTaskObject();
    		String [] object = new String[]{schemaName,viewName};
    		//获取数据库源对象信息
    		objectParsing.collectDatabaseObject(currentConn,object,null,table_type);
    		//获取所有任务对象键值对
    		Set<Entry<String,Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
    		//遍历任务对象，组装库对象ddl
			for (Entry<String,Vector<Object>> task: taskHashMap) {
				Vector<Vector<Object>> viewVector = (Vector<Vector<Object>>) task.getValue().get(8);
				for (Vector<Object> view:viewVector) {
					test=objectParsing.assembleView(task.getValue().get(1),view);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
    	return test;
	}

	/**
	 * 获取序列值的ddl语句
	 * @param currentConn
	 * @param schemaName
	 * @param sequenceName
	 * @param table_type
	 * @return
	 */
	public String loadSequenceDDL(Connection currentConn, String schemaName, String sequenceName,TableType table_type){
		this.currentConn = currentConn;
		this.schemaName =  schemaName;
		String test = null;
		try{
			objectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,sequenceName};
			//获取数据库源对象信息
			objectParsing.collectDatabaseObject(currentConn,object,null,table_type);
			//获取所有任务对象键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
			//遍历任务对象，组装库对象ddl
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> sequenceVec = (Vector<Vector<Object>>) task.getValue().get(9);
				for (Vector<Object> sequence : sequenceVec){
					test = objectParsing.assembleSequence(task.getValue().get(1),sequence);
				}
			}

		}catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return  test;
	}


	/**
	 *  获取包的DDL
	 * @param currentConn
	 * @param schemaName
	 * @param packageName
	 * @param table_type
	 * @return
	 */
	public String loadPackageDDL(Connection currentConn, String schemaName, String packageName,TableType table_type){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			objectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,packageName};
			//获取数据库对象源信息
			objectParsing.collectDatabaseObject(currentConn,object,null,table_type);
			//获取所有任务对象键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
			//遍历任务对象，组装数据库对对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> packageVec = (Vector<Vector<Object>>) task.getValue().get(10);
				for (Vector<Object> backage : packageVec){
					test = objectParsing.assemblePackage(task.getValue().get(1),backage);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return test;
	}

	/**
	 * 获取存储过程的DDL
	 * @param currentConn
	 * @param schemaName
	 * @param procedureName
	 * @param table_type
	 * @return
	 */
	public String loadProcedureDDL(Connection currentConn, String schemaName, String procedureName,TableType table_type){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			objectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,procedureName};
			//获取数据库对象源信息
			objectParsing.collectDatabaseObject(currentConn,object,null,table_type);
			//获取所有任务对象键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
			//遍历任务对象，组装数据库对象ddl
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> procedureVec = (Vector<Vector<Object>>) task.getValue().get(11);
				for (Vector<Object> procedure : procedureVec){
					test = objectParsing.assembleProcedure(task.getValue().get(1),procedure);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return test;
	}

	/**
	 * 获取存储函数的DDL
	 * @param currentConn
	 * @param schemaName
	 * @param functionName
	 * @param table_type
	 * @return
	 */
	public String loadFunctionDDL(Connection currentConn, String schemaName, String functionName,TableType table_type){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			objectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,functionName};
			//获取数据库对象源信息
			objectParsing.collectDatabaseObject(currentConn,object,null,table_type);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
			//遍历任务对象，组装数据库对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> functionVec = (Vector<Vector<Object>>) task.getValue().get(12);
				for (Vector<Object> function : functionVec){
					test = objectParsing.assembleFunction(task.getValue().get(1),function);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return test;
	}

	/**
	 * 获取触发器的DDL
	 * @param currentConn
	 * @param schemaName
	 * @param triggerName
	 * @param table_type
	 * @return
	 */
	public String loadTriggerDDL(Connection currentConn, String schemaName, String triggerName,TableType table_type){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			objectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,triggerName};
			//获取数据库对象源信息
			objectParsing.collectDatabaseObject(currentConn,object,null,table_type);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
			//遍历任务对象，组装ddl语句。
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> triggerVec = (Vector<Vector<Object>>) task.getValue().get(13);
				for (Vector<Object> trigger : triggerVec){
					test = objectParsing.assembleTrigger(task.getValue().get(1),trigger);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return test;
	}


	/**
	 * 获取同义词的DDL
	 * @param currentConn
	 * @param schemaName
	 * @param synonymName
	 * @param table_type
	 * @return
	 */
	public String loadSynonymDDL(Connection currentConn, String schemaName, String synonymName,TableType table_type){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			objectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,synonymName};
			//获取数据库对象源信息
			objectParsing.collectDatabaseObject(currentConn,object,null,table_type);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
			//遍历任务对象，组装ddl语句。
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> synonymVec = (Vector<Vector<Object>>) task.getValue().get(14);
				for (Vector<Object> synonym : synonymVec){
					test = objectParsing.assembleSynonym(object,synonym);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return test;
	}

	/**
	 * 获取自定义数据类型的ddl
	 * @param currentConn
	 * @param schemaName
	 * @param UDTName
	 * @param table_type
	 * @return
	 */
	public String loadUDTDDL(Connection currentConn, String schemaName, String UDTName,TableType table_type){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			objectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,UDTName};
			//获取数据库对象源信息
			objectParsing.collectDatabaseObject(currentConn,object,null,table_type);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
			//遍历任务对象，组装ddl语句。
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> UDTVec = (Vector<Vector<Object>>) task.getValue().get(15);
				for (Vector<Object> UDT : UDTVec){
					test = objectParsing.AssembleUdt(object,UDT);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return test;
	}


	/**
	 *  获取定时作业的ddl
	 * @param currentConn
	 * @param schemaName
	 * @param JobName
	 * @param table_type
	 * @return
	 */
	public String loadJobDDL(Connection currentConn, String schemaName, String JobName,TableType table_type){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			objectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,JobName};
			//获取数据库对象源信息
			objectParsing.collectDatabaseObject(currentConn,object,null,table_type);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = objectParsing.getTaskObject().entrySet();
			//遍历任务对象，组装ddl语句。
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> JobVec = (Vector<Vector<Object>>) task.getValue().get(16);
				for (Vector<Object> job : JobVec){
					test = objectParsing.assembleJob(object,job);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
		System.out.println(e.getMessage());
		}
		return test;
	}

	/**
	 * 登录系统库获取指定库指定模式下的对象定义
	 * @param conn
	 * @param dataBaseName
	 * @param schemaName
	 * @param objectType
	 * @param objectName
	 * @param
	 * @return
	 */
	public String getDatabaseObjectDDL(Connection conn, String dataBaseName, String schemaName, String objectType, String objectName, TableType tableType) {
		int dbId=0;
		String[] databaseNames = new String[]{
				ObjectParsing.quotaDatabaseObjectSin(dataBaseName)
		};
		DatabaseParsing databaseParsing =new DatabaseParsing();
		//获取库ID
		dbId = objectParsing.getDatabaseId(conn, databaseNames, tableType);

		if ("TABLE".equals(objectType)||"table".equals(objectType)) {
			//表
			return databaseParsing.loadTableDdl(conn, dbId, schemaName, objectName, tableType);
		} else if ("VIEW".equals(objectType)||"view".equals(objectType)) {
			//视图
			return databaseParsing.loadViewDdl(conn, dbId, schemaName, objectName, tableType);
		} else if ("SEQUENCE".equals(objectType)||"sequence".equals(objectType)) {
			//序列值
			return databaseParsing.loadSequenceDdl(conn, dbId, schemaName, objectName, tableType);
		} else if ("PACKAGE".equals(objectType)||"package".equals(objectType)) {
			//包
			return databaseParsing.loadPackageDdl(conn, dbId, schemaName, objectName, tableType);
		} else if ("PROCEDURE".equals(objectType)||"procedure".equals(objectType)||"FUNCTION".equals(objectType)||"function".equals(objectType)) {
			//存储过程或函数
			return databaseParsing.loadProcedureDdl(conn, dbId, schemaName, objectName, tableType);
		} else if ("TRIGGER".equals(objectType)||"trigger".equals(objectType)) {
			//触发器
			return databaseParsing.loadTriggerDdl(conn, dbId, schemaName, objectName, tableType);
		} else if ("UDT".equals(objectType)||"udt".equals(objectType)) {
			//UDT
			return databaseParsing.loadUdtDdl(conn, dbId, schemaName, objectName, tableType);
		} else if("SYNONYM".equals(objectType)||"synonym".equals(objectType)){
			//同义词
			return databaseParsing.loadSynonymDdl(conn, dbId, schemaName, objectName, tableType);
		}else if("job".equals(objectType)||"JOB".equals(objectType)){
			//定时作业
			return databaseParsing.loadJobDdl(conn,   dbId, objectName, tableType);
		}
		return null;
	}

	/**
	 * 加载用户数据定义语句
	 *
	 * @param connection 连接
	 * @param userName   用户名称
	 * @param tableType  查询表类型
	 * @return 用户数据定义语句
	 */
	public String loadTheUserDDL(Connection connection, String userName, String password, TableType tableType) {
		StringBuilder builder = new StringBuilder("-- Create User --\r\n");
		String untilTime;
		try (Statement statement = connection.createStatement()) {
			String sql = String.format("SELECT UNTIL_TIME FROM %s_USERS WHERE USER_NAME='%s' AND IS_ROLE=FALSE", tableType, userName);
			ResultSet resultSet = statement.executeQuery(sql);
			if (resultSet.next()) {
				untilTime = resultSet.getString(1);
			} else {
				throw new IllegalStateException("用户不存在：" + userName);
			}
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
		// 创建用户对象（不含角色）
		builder.append(String.format("CREATE USER \"%s\" IDENTIFIED BY '%s' VALID UNTIL '%s';\r\n", userName, password, untilTime));
		// 授予用户权限（不含角色）
		if (tableType != TableType.ALL) {
			LoadPermission loadPermission = new LoadPermission();
			Map<String, String> userPermissions = loadPermission.loadPermissionSql(connection, userName);
			String userRoleSql = "";
			for (Entry<String, String> entry: userPermissions.entrySet()) {
				if ("role".equalsIgnoreCase(entry.getKey())) {
					userRoleSql = entry.getValue();
				} else {
					builder.append(entry.getValue());
				}
			}
			builder.append(userRoleSql);
		}
		return builder.toString();
	}

	/**
	 * 加载角色数据定义语句
	 *
	 * @param connection 连接
	 * @param roleName   角色名称
	 * @return 角色数据定义语句
	 */
	public String loadTheRoleDDL(Connection connection, String roleName) {
		StringBuilder builder = new StringBuilder("-- Create Role --\r\n");
		// 创建用户拥有的角色
		builder.append(String.format("CREATE ROLE \"%s\";\r\n", roleName));
		// 授予角色权限
		Map<String, String> rolePermissions = new LoadPermission().loadPermissionSql(connection, roleName);
		for (Entry<String, String> entry: rolePermissions.entrySet()) {
			builder.append(entry.getValue());
		}
		return builder.toString();
	}


	/**
	 * 加载模式数据定义语句
	 *
	 * @param connection 连接
	 * @param schemaName 模式名称
	 * @return 模式数据定义语句
	 */
	public String loadTheSchemaDDL(Connection connection, String schemaName, TableType tableType) {
		StringBuilder builder = new StringBuilder("-- Create Schema --\r\n");
		String ownerName;
		try (Statement statement = connection.createStatement()) {
			String sql = String.format("SELECT U.USER_NAME FROM %s_SCHEMAS S JOIN %s_USERS U ON S.USER_ID=U.USER_ID WHERE S.SCHEMA_NAME='%s'", tableType, tableType, schemaName);
			ResultSet resultSet = statement.executeQuery(sql);
			if (resultSet.next()) {
				ownerName = resultSet.getString(1);
			} else {
				throw new IllegalStateException("模式不存在：" + schemaName);
			}
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
		builder.append(String.format("CREATE SCHEMA \"%s\" AUTHORIZATION \"%s\";\r\n", schemaName, ownerName));
		return builder.toString();
	}


	/**
	 * 加载库数据定义语句
	 *
	 * @param connection   连接
	 * @param databaseName 库名称
	 * @return 库数据定义语句
	 */
	public String loadTheDatabaseDDL(Connection connection, String databaseName, TableType tableType) {
		StringBuilder builder = new StringBuilder("-- Create Database --\r\n");
		String charset;
		String timezone;
		try (Statement statement = connection.createStatement()) {
			String sql = String.format("SELECT CHAR_SET,TIME_ZONE FROM %s_DATABASES WHERE DB_NAME='%s'", tableType, databaseName);
			ResultSet resultSet = statement.executeQuery(sql);
			if (resultSet.next()) {
				charset = resultSet.getString(1);
				timezone = resultSet.getString(2);
			} else {
				throw new IllegalStateException("库不存在：" + databaseName);
			}
		} catch (SQLException e) {
			throw new IllegalStateException(e);
		}
		builder.append(String.format("CREATE DATABASE \"%s\" CHARACTER SET '%s' TIME ZONE '%s';\r\n", databaseName, charset, timezone));
		return builder.toString();
	}
}
