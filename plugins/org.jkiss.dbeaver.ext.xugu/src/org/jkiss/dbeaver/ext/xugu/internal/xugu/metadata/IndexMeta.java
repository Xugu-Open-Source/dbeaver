package org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata;

import com.xugu.metadata.Constants;
import com.xugu.parser.ObjectParsing;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.parser.XuguDDLUtils;

import java.util.Vector;

/**
 * @author zk
 *
 * 创建索引类，用户对表索引的定义与组装。
 */
@SuppressWarnings("unchecked")
public class IndexMeta implements Constants {

	/**
	 * 是否为唯一值
	 */
	private boolean unique;

	/**
	 * 是否为主键
	 */
	private boolean primaryKey;

	/**
	 * 模式名
	 */
	private String schemaName;

	/**
	 * 表名
	 */
	private String tableName;

	/**
	 * 索引名
	 */
	private String indexName;

	/**
	 * 索引列
	 */
	private String indexColumns;

	/**
	 * 索引类型码
	 */
	private int indexTypeCode;

	/**
	 * 是否在线
	 */
	private boolean online;

	/**
	 * 是否为本地
	 */
	private boolean local;

	/**
	 * 字段数量
	 */
	private int fieldNum;

	private String keys;
	
	private String vocable;

	private int wordLen;

	/**
	 * 分区类型
	 */
	private int partType;

	/**
	 * 分区数量
	 */
	private int partNum;

	/**
	 * 分区键
	 */
	private String partKeys;

	/**
	 * 分区信息
	 */
	private Vector<Vector<Object>> partInfo;

	/**
	 * 二级分区类型
	 */
	private int subpartType;

	/**
	 * 二级分区数量
	 */
	private int subpartNum;

	/**
	 * 二级分区键
	 */
	private String subpartKeys;

	/**
	 * 索引副本数
	 */
	private int copyNum;

	/**
	 * 二级分区信息
	 */
	private Vector<Vector<Object>> subpartInfo;

	/**
	 * sql字符
	 */
	private String sql;

	private enum IndexType {
		/**
		 * b树索引
		 */
		BTREE,

		/**
		 * r树索引
		 */
		RTREE,

		/**
		 * 全文索引
		 */
		FULL_TEXT,

		/**
		 * 位图索引
		 */
		BITMAP
	}

	private enum PartType {
		/**
		 * 范围分区
		 */
		RANGE,

		/**
		 * 列表分区
		 */
		LIST,

		/**
		 * 哈希分区
		 */
		HASH
	}


	public boolean isUnique() {
		return unique;
	}
	public int getCopyNum() {
		return copyNum;
	}
	public int setCopyNum(int copyNum) {
		return this.copyNum = copyNum;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isPrimaryKey(){
		return primaryKey;
	}

	public void setPrimaryKey(boolean primaryKey){
		this.primaryKey = primaryKey;
	}
	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public String getRelationTable() {
		return getSchemaName() + MARK_DOT +  getTableName() ;
	}

	public String getIndexColumns() {
		return indexColumns;
	}

	public void setIndexColumns(String indexColumns) {
		this.indexColumns = indexColumns;
	}

	public IndexType getIndexType() {
		if (indexTypeCode == 0) {
			return IndexType.BTREE;
		} else if (indexTypeCode == 1) {
			return IndexType.RTREE;
		} else if (indexTypeCode == 2) {
			return IndexType.FULL_TEXT;
		} else if (indexTypeCode == 3) {
			return IndexType.BITMAP;
		}
		else {
			return null;
		}
	}

	public PartType getPartType() {
		if (getPart_Type() == 1) {
			return PartType.RANGE;
		} else if (getPart_Type() == 2) {
			return PartType.LIST;
		} else if (getPart_Type() == 3) {
			return PartType.HASH;
		}
		return null;
	}

	public PartType getSubPartType() {
		if (getSubPart_Type() == 1) {
			return PartType.RANGE;
		} else if (getSubPart_Type() == 2) {
			return PartType.LIST;
		} else if (getSubPart_Type() == 3) {
			return PartType.HASH;
		}
		return null;
	}

	public void setIndexType(int indexTypeCode) {
		this.indexTypeCode = indexTypeCode;
	}

	public String getIndexOptItem() {
		String sql = "";
		if(getIndexType() == IndexType.FULL_TEXT){
			if(getVocable() != null){
				sql += "using vocable table ";
				sql += MARK_SIN_QUOTATION + getVocable() + MARK_SIN_QUOTATION;
			} else if(getWordLen() > 0){
				sql += "for every ";
				sql += getWordLen();
				sql += " vocable";
			}
			sql += MARK_WRAP;
		}
		return sql;
	}

	public String getIndexPartSql(PartType partType, Vector<Vector<Object>> partInfo) {
		if (partInfo == null) {
			return "";
		}
		String partsql = "";
		String sql = "";
		switch (partType) {
		case RANGE:
			partsql = " values less than";
			break;
		case LIST:
			partsql = " values";
			break;
		case HASH:
			break;
		default:
			return "";
		}
		sql += MARK_BGN_CURVES + MARK_WRAP;
		for (int i = 0; i < partInfo.size(); i++) {
			sql +=  partInfo.get(i).get(0) ;
			sql += partsql;
			while (partType != PartType.HASH) {
				sql += MARK_BGN_CURVES;
				sql += partInfo.get(i).get(1);
				sql += MARK_END_CURVES;
				break;
			}
			if (i != partInfo.size() - 1) {
				sql += MARK_COMMA;
			}
			sql += MARK_WRAP;
		}
		sql += MARK_END_CURVES;
		return sql;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	public int getFieldNum() {
		return fieldNum;
	}

	public void setFieldNum(int fieldNum) {
		this.fieldNum = fieldNum;
	}

	public String getKeys() {
		return keys;
	}

	public void setKeys(String keys) {
		this.keys = keys;
	}

	public String getVocable() {
		return vocable;
	}

	public void setVocable(String vocable) {
		this.vocable = vocable;
	}

	public int getWordLen() {
		return wordLen;
	}

	public void setWordLen(int wordLen) {
		this.wordLen = wordLen;
	}

	public int getPart_Type() {
		return partType;
	}

	public void setPartType(int partType) {
		this.partType = partType;
	}

	public int getPartNum() {
		return partNum;
	}

	public void setPartNum(int partNum) {
		this.partNum = partNum;
	}

	public String getPartKeys() {
		return partKeys;
	}

	public void setPartKeys(String partKeys) {
		this.partKeys = partKeys;
	}

	public Vector<Vector<Object>> getPartInfo() {
		return partInfo;
	}

	public void setPartInfo(Vector<Vector<Object>> partInfo) {
		this.partInfo = partInfo;
	}

	public int getSubPart_Type() {
		return subpartType;
	}

	public void setSubPartType(int subPartType) {
		this.subpartType = subPartType;
	}

	public int getSubPartNum() {
		return subpartNum;
	}

	public void setSubPartNum(int subPartNum) {
		this.subpartNum = subPartNum;
	}

	public String getSubPartKeys() {
		return subpartKeys;
	}

	public void setSubPartKeys(String subPartKeys) {
		this.subpartKeys = subPartKeys;
	}

	public Vector<Vector<Object>> getSubPartInfo() {
		return subpartInfo;
	}

	public void setSubPartInfo(Vector<Vector<Object>> subPartInfo) {
		this.subpartInfo = subPartInfo;
	}

	ObjectParsing objectParsing =new ObjectParsing();
	public String getSql() {
		String sql = "";
		sql += "-- Create Table Index --";
		sql += MARK_WRAP;
		sql += "create ";
		sql += isUnique() == true ? "unique " : "";

			sql += "index ";
			sql += "\""+getIndexName()+"\"";
			sql += " on ";
			sql += XuguDDLUtils.schemaAndTable(getRelationTable());
			sql += MARK_BGN_CURVES;
			sql += getIndexColumns();
			sql += MARK_END_CURVES;
			sql += " indextype is ";
			switch (getIndexType()) {
				case BTREE:
					sql += "btree ";
					break;
				case RTREE:
					sql += "rtree ";
					break;
				case FULL_TEXT:
					sql += "full_text ";
					break;
				case BITMAP:
					sql += "bitmap ";
					break;
				default:
					sql += "btree ";
					break;
			}
			sql += getIndexOptItem();
			if (isLocal()) {
				sql += "local ";
			} else {
				sql += "global ";
			}
			if (getPartType() != null && getPartKeys() != null) {
				sql += MARK_WRAP;
				switch (getPartType()) {
					case RANGE:
						sql += "partition by range";
						sql += MARK_BGN_CURVES;
						sql += ObjectParsing.removeDatabaseObjectQuota(getPartKeys());
						sql += MARK_END_CURVES;
						sql += " partitions";
						break;
					case LIST:
						sql += "partition by list";
						sql += MARK_BGN_CURVES;
						sql += ObjectParsing.removeDatabaseObjectQuota(getPartKeys());
						sql += MARK_END_CURVES;
						sql += " partitions";
						break;
					case HASH:
						sql += "partition by hash";
						sql += MARK_BGN_CURVES;
						sql += ObjectParsing.removeDatabaseObjectQuota(getPartKeys());
						sql += MARK_END_CURVES;
						sql += " partitions";
						break;
				}
			}
			sql += getIndexPartSql(getPartType(), getPartInfo());
			if (getSubPartType() != null && getSubPartKeys() != null) {
				sql += MARK_WRAP;
				switch (getSubPartType()) {
					case RANGE:
						sql += "subpartition by range";
						sql += MARK_BGN_CURVES;
						sql += ObjectParsing.removeDatabaseObjectQuota(getSubPartKeys());
						sql += MARK_END_CURVES;
						sql += " subpartitions";
						break;
					case LIST:
						sql += "subpartition by list";
						sql += MARK_BGN_CURVES;
						sql += ObjectParsing.removeDatabaseObjectQuota(getSubPartKeys());
						sql += MARK_END_CURVES;
						sql += " subpartitions";
						break;

					case HASH:
						sql += "subpartition by hash";
						sql += MARK_BGN_CURVES;
						sql += ObjectParsing.removeDatabaseObjectQuota(getSubPartKeys());
						sql += MARK_END_CURVES;
						sql += " subpartitions";
						break;
				}
			}
			sql += getIndexPartSql(getSubPartType(), getSubPartInfo());
			if (getCopyNum()>0){
				sql += " copy number " + getCopyNum();
			}
			sql += SEMICOLON;
			this.sql = sql;
			return this.sql;
		}

	public void setSql(String sql) {
		this.sql = sql;
	}
}
