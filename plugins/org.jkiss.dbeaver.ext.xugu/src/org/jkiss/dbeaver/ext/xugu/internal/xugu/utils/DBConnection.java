package org.jkiss.dbeaver.ext.xugu.internal.xugu.utils;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Properties;


/**
 * @author
 * 数据库连接类，用户管理维护数据库连接。
 */
@SuppressWarnings("unchecked")
public class DBConnection {

    /**
     * 日志
     */
//    private Logger log = LoggerFactory.getLogger(DatabaseObjectParsing.class);

    /**
     * 当前连接对象
     */
    private Connection conn;

    /**
     * 当前操作对象
     */
    private Statement stmt;

    /**
     * 当前结果集对象
     */
    private ResultSet rs;

    /**
     * 连接的数据库驱动
     */
    private String dbDriver;

    /**
     * 连接的url
     */
    private String dbUrl;

    /**
     * 数据库用户名
     */
    private String userName;

    /**
     * 密码
     */
    private String password;


    /**
     * 从配置文件取数据库链接参数
     * @param dbConnFile
     * @throws IOException
     */
    private void loadConnProperties(String dbConnFile) throws IOException {
        Properties props = new Properties();

        try {
//               props.load(new FileInputStream(new File(dbConnFile)));
//            根据配置文件路径Conf加载配置文件
           props.load(new InputStreamReader(DBConnection.class.getClassLoader().getResourceAsStream(dbConnFile)));
        } catch (FileNotFoundException e) {
            //		log.error(e.toString());
		System.out.println(e.getMessage());
        } catch (IOException e) {
            //		log.error(e.toString());
		System.out.println(e.getMessage());
        }
        //从配置文件中取得相应的参数并设置类变量
        this.dbDriver = props.getProperty("driver");
        this.dbUrl = props.getProperty("url");
        this.userName = props.getProperty("username");
        this.password = props.getProperty("password");
        System.out.println(dbDriver);
        System.out.println(dbUrl);
        System.out.println(userName);
        System.out.println(password);
    }

    /**
     * 建立连接
     * @param dbConnFile
     * @return
     */
    public boolean openConnection(String dbConnFile){
        try {
            loadConnProperties(dbConnFile);
            Class.forName(dbDriver);
            this.conn = DriverManager.getConnection(dbUrl,userName,password);
            return true;
        } catch(ClassNotFoundException classnotfoundexception) {
            classnotfoundexception.printStackTrace();
//            log.info("db: " + classnotfoundexception.getMessage());
            System.out.println("db: " + classnotfoundexception.getMessage());
        } catch(SQLException | IOException sqlexception) {
//            log.info("db.getconn(): " + sqlexception.getMessage());
            System.out.println("db.getconn(): " + sqlexception.getMessage());
        }
        return  false;
    }

    /**
     * 获取数据库连接
     * @return
     */
    public Connection getConn() {
        System.out.println("数据库已连接");
        return conn;
    }
    
    /**
     * 存储过程和查询
     * 
     * @param sql
     * @param clobState
     * @throws SQLException
     */
    public void query(String sql, int clobState) throws SQLException {
	stmt = conn.createStatement();
	Statement xgStmt =  stmt;
	rs = xgStmt.executeQuery(sql);
    }
    
    /**
     * 关闭当前Connection
     * @throws SQLException
     */
    public void closeConn() throws SQLException {
	if(conn != null){
	    conn.close();
	}
	conn = null;
    }

    /**
     * 关闭当前ResultSet和Statement
     * @throws SQLException
     */
    public void closeRsAndStmt() throws SQLException {
	rs.close();
	stmt.close();
    }
   
    /**
     * 取得当前结果集
     * @return ResultSet
     * @throws SQLException
     */
    public ResultSet getRs() throws SQLException {
	return rs;
    }
}
