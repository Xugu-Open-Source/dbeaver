/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * 表空间信息类，包含表空间基本信息
 * 
 * @author Xugu
 */
public class Tablespace extends BaseGlobalObject implements DBPRefreshableObject {
	private static final Log log = Log.getLog(Tablespace.class);

	public enum Status {
		/**
		 * 表空间状态枚举
		 */
		ONLINE, OFFLINE, READ_ONLY
	}

	public enum Contents {
		/**
		 * 表空间内容枚举
		 */
		PERMANENT, TEMPORARY
	}

	public enum Logging {
		/**
		 * 表空间日志枚举
		 */
		LOGGING, NOLOGGING,
	}

	public enum ExtentManagement {
		/**
		 * 表空间拓展管理枚举
		 */
		DICTIONARY, LOCAL
	}

	public enum AllocationType {
		/**
		 * 表空间分配类型枚举
		 */
		SYSTEM, UNIFORM, USER,
	}

	public enum SegmentSpaceManagement {
		/**
		 * 表空间段空间管理枚举
		 */
		MANUAL, AUTO
	}

	public enum Retention {
		/**
		 * 表空间保留枚举
		 */
		GUARANTEE, NOGUARANTEE, NOT_APPLY
	}

	private String name;
	private DataFile file;
	final FileCache fileCache = new FileCache();

	private int nodeId;
	private long spaceId;
	private int dataFileNum;
	private String spaceType;
	private boolean mediaError;
	private String filePath;

	public Tablespace(DataSource dataSource, String tsName) throws SQLException {
		super(dataSource, true);
		this.name = tsName;
	}

	protected Tablespace(DataSource dataSource, ResultSet dbResult) throws SQLException {
		super(dataSource, true);
		this.name = JDBCUtils.safeGetString(dbResult, "SPACE_NAME");
		this.nodeId = JDBCUtils.safeGetInt(dbResult, "NODEID");
		this.spaceId = JDBCUtils.safeGetLong(dbResult, "SPACE_ID");
		this.dataFileNum = JDBCUtils.safeGetInt(dbResult, "DATAFILE_NUM");
		this.spaceType = JDBCUtils.safeGetString(dbResult, "SPACE_TYPE");
		this.mediaError = JDBCUtils.safeGetBoolean(dbResult, "MEDIA_ERROR");
	}

	static class FileCache extends JDBCObjectCache<Tablespace, DataFile> {
		@Override
		protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull Tablespace owner)
				throws SQLException {
			StringBuilder desc = new StringBuilder(100);
			desc.append("SELECT * FROM ");
			desc.append(owner.getDataSource().getRoleFlag());
			desc.append("_DATAFILES");
			SQLUtils.appendFirstClause(desc, true);
			desc.append("SPACE_ID=");
			desc.append(owner.getSpaceId());

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct view tablespace sql: " + desc.toString());
			JDBCPreparedStatement dbStat = session.prepareStatement(desc.toString());

			return dbStat;
		}

		@Override
		protected DataFile fetchObject(@NotNull JDBCSession session, @NotNull Tablespace owner,
				@NotNull JDBCResultSet resultSet) throws SQLException, DBException {
			return new DataFile(owner, resultSet, "TEMP_SPACE".equals(owner.getSpaceType()));
		}
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = false, order = 3)
	public String getName() {
		return name;
	}

	@Property(viewable = true, editable = false, order = 1)
	public int getNodeId() {
		return nodeId;
	}

	@Property(viewable = true, editable = false, order = 2)
	public long getSpaceId() {
		return spaceId;
	}

	@Property(viewable = true, editable = false, order = 4)
	public int getDataFileNum() {
		return dataFileNum;
	}

	@Property(viewable = true, editable = false, order = 5)
	public String getSpaceType() {
		return spaceType;
	}

	@Property(viewable = true, editable = false, order = 6)
	public boolean getMediaError() {
		return mediaError;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String path) {
		filePath = path;
	}

	public void setNodeId(int id) {
		this.nodeId = id;
	}

	@Association
	public Collection<DataFile> getFiles(DBRProgressMonitor monitor) throws DBException {
		return fileCache.getAllObjects(monitor, this);
	}

	public DataFile getFile() {
		return file;
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		fileCache.clearCache();
		return this;
	}
}
