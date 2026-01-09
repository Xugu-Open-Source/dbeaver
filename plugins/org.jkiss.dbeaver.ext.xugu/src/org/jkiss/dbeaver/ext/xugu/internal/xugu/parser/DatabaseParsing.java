package org.jkiss.dbeaver.ext.xugu.internal.xugu.parser;

import org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata.Constants.DatabaseObjectType;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata.IndexMeta;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.DatabaseObjectParsing;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.Parsing;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;


/**
 * 此类为库级对象获取加载对象ddl。
 */
@SuppressWarnings("unchecked")
public class DatabaseParsing {
	
	
    /**
     * 连接
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

	private org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.DatabaseObjectParsing databaseObjectParsing = new org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.DatabaseObjectParsing();

	/**
	 * 日志log
	 */
//	private Logger log = LoggerFactory.getLogger(DatabaseParsing.class);


	/**
	 * 获取表的ddl语句
	 * @param currentConn
	 * @param dbId
	 * @param schemaName
	 * @param tableName
	 * @param tableType
	 * @return
	 */
	public String loadTableDdl(Connection currentConn, Integer dbId, String schemaName, String tableName, Parsing.TableType tableType) {
    this.currentConn = currentConn;
    this.schemaName = schemaName;
    this.tableName = tableName;
    String test = null;
    List<Object> identityCols = new ArrayList<>();
    List<Object> uniqueCols = new ArrayList<>();
    try {
		databaseObjectParsing.initSearchTaskObject();
    	String[] object = new String[]{schemaName,tableName};
    	//收集数据库源对象
		databaseObjectParsing.collectDatabaseObject(currentConn, dbId, object, null,tableType);
		//获取所有任务对象的键值对
        Set<Entry<String, Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
        //遍历任务对象键值对
    	for(Entry<String, Vector<Object>> task : taskHashMap) {
			//传入参数为目的对象，列信息，表分区
			test = databaseObjectParsing.assembleXuGuTable(task.getValue().get(1), (Vector<Vector<Object>>) task.getValue().get(3), (Vector<Object>) task.getValue().get(5), schemaName, tableName) + "\n";
			//表自增
			Vector<Vector<Object>> identityVec = (Vector<Vector<Object>>) task.getValue().get(6);
			for (Vector<Object> identity : identityVec) {
				identityCols.add(identity.get(2));
				//自增
				test += databaseObjectParsing.assembleTableIdentity(task.getValue().get(1), identity);
			}
			//表约束
			Vector<Vector<Object>> tableConstraintVector = (Vector<Vector<Object>>) task.getValue().get(4);

			for (Vector<Object> constraint : tableConstraintVector) {

				switch (Integer.parseInt(constraint.get(11).toString())) {
					//主键
					case 0:
						// ******************************************组装表主键***************************************
						test += databaseObjectParsing.assembleTableConstraint(task.getValue().get(1), constraint, DatabaseObjectType.PrimaryKey, identityCols) + "\n";
						// ****************************************************************************************
						break;
					//外键
					case 1:
						// ******************************************组装表外键***************************************
						test += databaseObjectParsing.assembleTableConstraint(task.getValue().get(1), constraint, DatabaseObjectType.ForeignKey, identityCols) + "\n";
						// ****************************************************************************************
						break;
					//唯一值
					case 2:
						// ******************************************组装表唯一值*************************************
						test += databaseObjectParsing.assembleTableConstraint(task.getValue().get(1), constraint, DatabaseObjectType.UniqueConstraint, identityCols) + "\n";
						// ****************************************************************************************
						break;
					//值检查
					case 3:
						// ******************************************组装表值检查*************************************
						test += databaseObjectParsing.assembleTableConstraint(task.getValue().get(1), constraint, DatabaseObjectType.CheckConstraint, identityCols) + "\n";
						// ****************************************************************************************
						break;
					default:
						break;
				}
			}
			Vector<IndexMeta> indexs = (Vector<IndexMeta>) task.getValue().get(7);

			//当存在自增长字段时，遍历自增长字段集合，凡是索引字段等于自增长字段的不获取索引ddl
			if (identityCols.size() > 0) {
				for (IndexMeta index : indexs) {
					for (Object identityCol : identityCols) {

						if (index.getIndexColumns().equals(identityCol)) {
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
	 * @param dbId
	 * @param schemaName
	 * @param viewName
	 * @param tableType
	 * @return
	 */
	public  String loadViewDdl(Connection currentConn , Integer dbId, String schemaName, String viewName, Parsing.TableType tableType){
    	this.currentConn=currentConn;
    	this.schemaName=schemaName;
    	this.viewName=viewName;
    	String test = null;
    	try{
    		databaseObjectParsing.initSearchTaskObject();
    		String [] object = new String[]{schemaName,viewName};
			//收集数据库源对象
    		databaseObjectParsing.collectDatabaseObject(currentConn, dbId, object,null,tableType);
			//获取所有任务对象的键值对
    		Set<Entry<String,Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
			//遍历任务对象键值对，组装数据库对象ddl语句
			for (Entry<String,Vector<Object>> task: taskHashMap) {
				Vector<Vector<Object>> viewVector = (Vector<Vector<Object>>) task.getValue().get(8);
				for (Vector<Object> view:viewVector) {
					test=databaseObjectParsing.assembleView(task.getValue().get(1),view);
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
	 * @param dbId
	 * @param schemaName
	 * @param sequenceName
	 * @param tableType
	 * @return
	 */
	public String loadSequenceDdl(Connection currentConn, Integer dbId, String schemaName, String sequenceName, Parsing.TableType tableType){
		this.currentConn = currentConn;
		this.schemaName =  schemaName;
		String test = null;
		try{
			databaseObjectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,sequenceName};
			//收集数据库源对象
			databaseObjectParsing.collectDatabaseObject(currentConn, dbId,object,null,tableType);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
			//遍历任务对象键值对，组装数据库对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> sequenceVec = (Vector<Vector<Object>>) task.getValue().get(9);
				for (Vector<Object> sequence : sequenceVec){
					test = databaseObjectParsing.assembleSequence(task.getValue().get(1),sequence);
				}
			}

		}catch(Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		return  test;
	}


	/**
	 * 获取包的DDL
	 * @param currentConn
	 * @param dbId
	 * @param schemaName
	 * @param packageName
	 * @param tableType
	 * @return
	 */
	public String loadPackageDdl(Connection currentConn, Integer dbId, String schemaName, String packageName, Parsing.TableType tableType){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			databaseObjectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,packageName};
			//收集数据库源对象
			databaseObjectParsing.collectDatabaseObject(currentConn, dbId,object,null,tableType);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
			//遍历任务对象键值对，组装数据库对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> packageVec = (Vector<Vector<Object>>) task.getValue().get(10);
				for (Vector<Object> backage : packageVec){
					test = databaseObjectParsing.assemblePackage(task.getValue().get(1),backage);
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
	 * @param dbId
	 * @param schemaName
	 * @param procedureName
	 * @param tableType
	 * @return
	 */
	public String loadProcedureDdl(Connection currentConn, Integer dbId, String schemaName, String procedureName, Parsing.TableType tableType){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			databaseObjectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,procedureName};
			//收集数据库源对象
			databaseObjectParsing.collectDatabaseObject(currentConn, dbId,object,null,tableType);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
			//遍历任务对象键值对，组装数据库对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> procedureVec = (Vector<Vector<Object>>) task.getValue().get(11);
				for (Vector<Object> procedure : procedureVec){
					test = databaseObjectParsing.assembleProcedure(task.getValue().get(1),procedure);
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
	 * @param dbId
	 * @param schemaName
	 * @param functionName
	 * @param tableType
	 * @return
	 */
	public String loadFunctionDdl(Connection currentConn, Integer dbId, String schemaName, String functionName, Parsing.TableType tableType){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			databaseObjectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,functionName};
			//收集数据库源对象
			databaseObjectParsing.collectDatabaseObject(currentConn, dbId,object,null,tableType);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
			//遍历任务对象键值对，组装数据库对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> functionVec = (Vector<Vector<Object>>) task.getValue().get(12);
				for (Vector<Object> function : functionVec){
					test = databaseObjectParsing.assembleFunction(task.getValue().get(1),function);
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
	 * @param dbId
	 * @param schemaName
	 * @param triggerName
	 * @param tableType
	 * @return
	 */
	public String loadTriggerDdl(Connection currentConn, Integer dbId, String schemaName, String triggerName, Parsing.TableType tableType){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			databaseObjectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,triggerName};
			//收集数据库源对象
			databaseObjectParsing.collectDatabaseObject(currentConn, dbId,object,null,tableType);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
			//遍历任务对象键值对，组装数据库对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> triggerVec = (Vector<Vector<Object>>) task.getValue().get(13);
				for (Vector<Object> trigger : triggerVec){
					test = databaseObjectParsing.assembleTrigger(task.getValue().get(1),trigger);
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
	 * @param dbId
	 * @param schemaName
	 * @param synonymName
	 * @param tableType
	 * @return
	 */
	public String loadSynonymDdl(Connection currentConn, Integer dbId, String schemaName, String synonymName, Parsing.TableType tableType){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			databaseObjectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,synonymName};
			//收集数据库源对象
			databaseObjectParsing.collectDatabaseObject(currentConn, dbId,object,null,tableType);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
			//遍历任务对象键值对，组装数据库对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> synonymVec = (Vector<Vector<Object>>) task.getValue().get(14);
				if(synonymVec != null){
					for (Vector<Object> synonym : synonymVec){
						test = databaseObjectParsing.assembleSynonym(object,synonym);
					}
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
	 * @param dbId
	 * @param schemaName
	 * @param udtName
	 * @param tableType
	 * @return
	 */
	public String loadUdtDdl(Connection currentConn, Integer dbId, String schemaName, String udtName, Parsing.TableType tableType){
		this.currentConn = currentConn;
		this.schemaName = schemaName;
		String  test = null;
		try{
			databaseObjectParsing.initSearchTaskObject();
			String [] object = new String[]{schemaName,udtName};
			//收集数据库源对象
			databaseObjectParsing.collectDatabaseObject(currentConn, dbId,object,null,tableType);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
			//遍历任务对象键值对，组装数据库对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> udtVec = (Vector<Vector<Object>>) task.getValue().get(15);
				for (Vector<Object> udt : udtVec){
					test = databaseObjectParsing.assembleUdt(object,udt);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		return test;
	}


	/**
	 * 获取定时作业的ddl
	 * @param currentConn
	 * @param dbId
	 * @param jobName
	 * @param tableType
	 * @return
	 */
	public String loadJobDdl(Connection currentConn, Integer dbId, String jobName, Parsing.TableType tableType){
		this.currentConn = currentConn;
		String  test = null;
		try{
			databaseObjectParsing.initSearchTaskObject();
			String [] object = new String[]{jobName,jobName};
			//收集数据库源对象
			databaseObjectParsing.collectDatabaseObject(currentConn, dbId,object,null,tableType);
			//获取所有任务对象的键值对
			Set<Entry<String,Vector<Object>>> taskHashMap = databaseObjectParsing.getTaskObject().entrySet();
			//遍历任务对象键值对，组装数据库对象ddl语句
			for(Entry<String,Vector<Object>> task: taskHashMap){
				Vector<Vector<Object>> jobVec = (Vector<Vector<Object>>) task.getValue().get(16);
				for (Vector<Object> job : jobVec){
					test = databaseObjectParsing.assembleJob(object,job);
				}
			}
		}catch(Exception e){
//		log.error(e.toString());
			System.out.println(e.getMessage());
		}
		return test;
	}

}
