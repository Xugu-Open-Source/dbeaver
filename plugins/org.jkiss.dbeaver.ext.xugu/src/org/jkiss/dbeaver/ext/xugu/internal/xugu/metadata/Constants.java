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

    public static final String KEYWORDS =  " ABORT,ABOVE,ABSOLUTE,ACCESS,ACCOUNT,ACTION,ADD,AFTER,AGGREGATE,ALL,ALTER,ANALYSE,ANALYZE,AND,ANY,AOVERLAPS,APPEND,ARCHIVELOG," +
            "ARE,ARRAY,AS,ASC,AT,AUDIT, AUDITOR,AUTHID,AUTHORIZATION,AUTO,BACKUP,BACKWARD,BADFILE,BCONTAINS,BEFORE, BEGIN,BETWEEN,BINARY,BINTERSECTS," +
            "BIT,BLOCK, BLOCKS,	BODY,	BOTH,	BOUND,	BOVERLAPS,	BREAK,	BUFFER_POOL,	BUILD,	BULK,	BWITHIN,	BY,	CACHE,	CALL,	CASCADE," +
            "CASE,	CAST,	CATCH,	CATEGORY,	CHAIN,	CHAR,	CHARACTER,	CHARACTERISTICS,	CHECK,	CHECKPOINT,	CHUNK,	CLOSE,	CLUSTER,	COALESCE," +
            "COLLATE,	COLLECT,	COLUMN,	COMMENT,	COMMIT,	COMMITTED,	COMPLETE,		COMPRESS,	COMPUTE,	CONNECT,	CONSTANT,	CONSTRAINT," +
            "CONSTRAINTS,	CONSTRUCTOR,	CONTAINS,	CONTEXT,	CONTINUE,	COPY,	CORRESPONDING,	CREATE,	CREATEDB,	CREATEUSER,	CROSS,	CROSSES," +
            "CUBE,	CURRENT,	CURSOR,	CYCLE,	DATABASE,	DATAFILE,	DATE,	DATETIME,	DAY,	DBA,	DEALLOCATE,	DEC,	DECIMAL,	DECLARE,	DECODE," +
            "DECRYPT,	DEFAULT,	DEFERRABLE,	DEFERRED,	DELETE,	DELIMITED,	DELIMITERS,	DEMAND,	DESC,	DESCRIBE,	DETERMINISTIC,	DIR,	DISABLE," +
            "DISASSEMBLE,	DISCORDFILE,	DISJOINT,	DISTINCT,	DO,	DOMAIN,	DOUBLE,	DRIVEN,	DROP,	EACH,	ELEMENT,	ELSE,	ELSEIF,	ELSIF,	ENABLE," +
            "ENCODING,	ENCRYPT,	ENCRYPTOR,	END,	ENDCASE,	ENDFOR,	ENDIF,	ENDLOOP,	EQUALS,		ESCAPE,	EVERY,	EXCEPT,	EXCEPTION,	EXCEPTIONS," +
            "EXCLUSIVE,	EXEC,	EXECUTE,	EXISTS,	EXIT,	EXPIRE,	EXPLAIN,	EXPORT,	EXTEND,	EXTERNAL,	EXTRACT,	FALSE,	FAST,	FETCH,	FIELD,	FIELDS," +
            "FILTER, FINAL,	FINALLY,	FIRST,	FLOAT,	FOLLOWING,	FOR, FORALL,	FORCE,	FOREIGN,	FORWARD,	FOUND,	FREELIST,	FREELISTS,	FROM,	FULL," +
            "FUNCTION,	GENERATED,	GET,	GLOBAL,	GOTO,	GRANT,	GREATEST,	GROUP,	GROUPING,	GROUPS,	HANDLER,	HASH,	HAVING,	HEAP,	HIDE,	HOTSPOT," +
            "HOUR,	IDENTIFIED,	IDENTIFIER,	IDENTITY,	IF,	ILIKE,	IMMEDIATE,	IMPORT,	IN,	INCLUDE,	INCREMENT,	INDEX,	INDEXTYPE,	INDICATOR,	INDICES," +
            "INHERITS,	INIT,	INITIAL,	INITIALLY,	INITRANS,	INNER,	INOUT,	INSENSITIVE,	INSERT,	INSTANTIABLE,	INSTEAD,	INTERSECT,	INTERSECTS," +
            "INTERVAL,	INTO,	IO,	IS,	ISNULL,	ISOLATION,	ISOPEN,	JOB,	JOIN,	K,	KEEP,	KEY,	KEYSET,	LABEL,	LANGUAGE,	LAST,	LEADING,	LEAST," +
            "LEAVE,	LEFT,	LEFTOF,	LENGTH,	LESS,		LEVEL,	LEVELS,	LEXER,	LIBRARY,	LIKE,	LIMIT,	LINK,	LIST,	LISTEN,	LOAD,	LOB, 	LOCAL,	LOCATION," +
            "LOCATOR,	LOCK,	LOGFILE,	LOGGING,	LOGIN,	LOGOUT,	LOOP,	LOVERLAPS,		M,	MATCH,	MATERIALIZED,	MAX,	MAXEXTENTS,	MAXSIZE,	MAXTRANS," +
            "MAXVALUE,	MAXVALUES,	MEMBER,	MEMORY,	MERGE,	MINEXTENTS,	MINUS,	MINUTE,	MINVALUE,	MISSING,	MODE,	MODIFY,	MONTH,	MOVEMENT,	NAME,	NAMES,	" +
            "NATIONAL,	NATURAL,	NCHAR,	NESTED,	NEW,	NEWLINE,	NEXT,	NO,	NOARCHIVELOG,	NOAUDIT,	NOCACHE,	NOCOMPRESS,	NOCREATEDB,	NOCREATEUSER," +
            "NOCYCLE,	NODE,	NOFORCE,	NOFOUND,	NOLOGGING,	NONE,	NOORDER,	NOPARALLEL,	NOT,	NOTFOUND,	NOTHING,	NOTIFY,	NOTNULL,	NOVALIDATE," +
            "NOWAIT,	NULL,	NULLIF,	NULLS,	NUMBER,	NUMERIC,	NVARCHAR,	NVARCHAR2,	NVL,	NVL2,	OBJECT,	OF,	OFF,	OFFLINE,	OFFSET,		OIDINDEX," +
            "OIDS,	OLD,	ON,	ONLINE,	ONLY,	OPEN,	OPERATOR,	OPTION,	OR,	ORDER,	ORGANIZATION,	OTHERVALUES,	OUT,	OUTER,	OVER,	OVERLAPS,	OWNER," +
            "PACKAGE,	PARALLEL,	PARAMETERS,	PARTIAL,	PARTITION,	PARTITIONS,	PASSWORD,	PCTFREE,	PCTINCREASE,	PCTUSED,	PCTVERSION,	PERIOD,	POLICY," +
            "PRAGMA,	PREBUILT,	PRECEDING, 	PRECISION,	PREPARE,	PRESERVE,	PRIMARY,	PRIOR,	PRIORITY,	PRIVILEGES,	PROCEDURAL,	PROCEDURE,	PROTECTED," +
            "PUBLIC,	QUERY,	QUOTA,	RAISE,	RANGE,	RAW,	READ,	READS,	REBUILD,	RECOMPILE,	RECORD,	RECORDS,	RECYCLE,	REDUCED,	REF,	REFERENCES," +
            "REFERENCING,	REFRESH, 	REINDEX,	RELATIVE,	RENAME,	REPEATABLE,	REPLACE,	REPLICATION,	RESOURCE,	RESTART,	RESTORE,	RESTRICT,	RESULT," +
            "RETURN,	RETURNING,	REVERSE,	REVOKE,	REWRITE,	RIGHT,	RIGHTOF,	ROLE,	ROLLBACK,	ROLLUP,	ROVERLAPS,	ROW,	ROWCOUNT,	ROWID,	ROWS," +
            "ROWTYPE,	RULE,	RUN,	SAVEPOINT,	SCHEMA,	SCROLL,	SECOND,	SEGMENT,	SELECT,	SELF,	SEQUENCE,	SERIALIZABLE,	SESSION,	SET,	SETOF,	SETS," +
            "SHARE,	SHOW,	SHUTDOWN,	SIBLINGS,	SIZE,	SLOW,	SNAPSHOT,	SOME,	SPATIAL,	SPLIT,	SSO,	STANDBY,	START,	STATEMENT,	STATIC,	STATISTICS," +
            "STEP,	STOP,	STORAGE,	STORE,	STREAM,	SUBPARTITION,	SUBPARTITIONS,	SUBTYPE,	SUCCESSFUL,	SYNONYM,	SYSTEM,	TABLE,	TABLESPACE,		TEMP,	TEMPLATE," +
            "TEMPORARY,	TERMINATED,	THAN,	THEN,	THROW,	TIME,	TIMESTAMP,	TO,	TOP,	TOPOVERLAPS, TOUCHES, TRACE, TRAILING,	TRAN,	TRANSACTION,	TRIGGER,	TRUE," +
            "TRUNCATE,	TRUSTED,	TRY,	TYPE,	UNBOUNDED,	UNDER,	UNDO,	UNIFORM,	UNION,	UNIQUE,	UNLIMITED,	UNLISTEN,	UNLOCK,	UNPROTECTED,	UNTIL,	UOVERLAPS," +
            "UPDATE,	USE,	USER,	USING,	VACUUM,	VALID,	VALIDATE,	VALUE,	VALUES,	VARCHAR,	VARCHAR2,	VARRAY,	VARYING,	VERBOSE,	VERSION,	VIEW,	VOCABLE," +
            "WAIT,	WHEN,	WHENEVER,	WHERE,	WHILE,	WITH,	WITHIN, 	WITHOUT,	WORK,	WRITE,	XML,	YEAR,	ZONE";

}
