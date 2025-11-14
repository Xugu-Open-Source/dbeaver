package org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata;

/**
 * @author zhangkun
 */
public interface Constants {

    /**
     * '符号常量
     */
    static final String MARK_SIN_QUOTATION = "'";

    /**
     * " 符号常量
     */
    static final String MARK_DUB_QUOTATION = "\"";

    /**
     * `符号常量
     */
    static final String MARK_MYSQL_QUOTATION = "`";

    /**
     * ( 符号常量
     */
    static final String MARK_BGN_CURVES = "(";

    /**
     * ） 符号常量
     */
    static final String MARK_END_CURVES = ")";

    /**
     * . 符号常量
     */
    static final String MARK_DOT = ".";

    /**
     * , 符号常量
     */
    static final String MARK_COMMA = ",";

    /**
     * ; 符号常量
     */
    static final String SEMICOLON = ";";

    /**
     *  换行回车
     */
    static final String MARK_WRAP = "\r\n";

    /**
     * 支持数据库对象类型
     */
    static enum DatabaseObjectType {
        //数据库
        Database("Database", "数据库"),
        //用户
        User("User", "用户"),
        //模式
        Schema("Schema", "模式"),
        //定时作业
        Job("Job", "定时作业"),
        //表
        Table("Table", "表"),
        //全局临时表
        GlobalTempTable("Global Template Table", "全局临时表"),
        //表字段
        TableColumn("Table Column", "表字段"),
        //表分区
        TablePartition("Table Partition", "表分区"),
        //外键
        ForeignKey("Foreign Key", "外键"),
        //主键
        PrimaryKey("Primary Key", "主键"),
        //唯一值约束
        UniqueConstraint("Unique Constraint", "唯一值约束"),
        //值检查约束
        CheckConstraint("Check Constraint", "值检查约束"),
        //视图
        View("View", "视图"),
        //视图字段
        ViewColumn("View Column", "视图字段"),
        //序列值
        Sequence("Sequence", "序列值"),
        //索引
        Index("Index", "索引"),
        //包
        Package("Package", "包"),
        //存储过程
        Procedure("Procedure", "存储过程"),
        //存储函数
        Function("Function", "存储函数"),
        //触发器
        Trigger("Trigger", "触发器"),
        //同义词
        Synonym("Synonym", "同义词"),
        //自定义类型
        UDT("UDT", "自定义数据类型"),
        //簇集
        Cluster("Cluster", "簇集"),
        //文本文件
        Txt("Txt", "文本文件"),
        //excel表格
        Execl("Execl", "Execl表格"),
        //xml文件
        XML("XML", "XML文件"),
        //未知数据类型
        Unknown("Unknown", "Unknown Database Type");

        /**
         * 对象名
         */
        private String objectName;

        private String value;

        private DatabaseObjectType(String objectName, String value) {
            this.objectName = objectName;
            this.value = value;
        }

        public String getObjectName() {
            return objectName;
        }

        public void setObjectName(String objectName) {
            this.objectName = objectName;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    /**
     * 表分区类型
     */
    static enum PartitionType {
        //范围分区
        range("range", "范围分区"),
        //列表分区
        list("list", "列表分区"),
        //哈希分区
        hash("hash", "哈希分区"),
        //无分区
        unknown("unknown", "无分区");

        private String key;

        private String value;

        private PartitionType(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
