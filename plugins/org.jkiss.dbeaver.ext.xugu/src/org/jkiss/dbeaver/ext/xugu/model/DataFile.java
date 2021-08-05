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
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import java.math.BigDecimal;
import java.sql.ResultSet;

/**
 * 虚谷表空间文件
 */
public class DataFile extends BaseObject<Tablespace> {
	public enum OnlineStatus {
		/**
		 * 表空间文件在线状态
		 */
		SYSOFF, SYSTEM, OFFLINE, ONLINE, RECOVER,
	}

	private long nodeId;
	private long spaceId;
	private String path;
	private int fileNo;
	private BigDecimal maxSize;
	private BigDecimal stepSize;

	private final Tablespace tablespace;

	private boolean temporary;

	protected DataFile(Tablespace tablespace, ResultSet dbResult, boolean temporary) {
		super(tablespace, JDBCUtils.safeGetString(dbResult, "PATH")
				.substring(JDBCUtils.safeGetString(dbResult, "PATH").lastIndexOf("/") + 1), true);
		this.tablespace = tablespace;
		this.temporary = temporary;
		this.nodeId = JDBCUtils.safeGetLong(dbResult, "NODEID");
		this.spaceId = JDBCUtils.safeGetLong(dbResult, "SPACE_ID");
		this.path = JDBCUtils.safeGetString(dbResult, "PATH");
		this.maxSize = JDBCUtils.safeGetBigDecimal(dbResult, "MAX_SIZE");
		this.stepSize = JDBCUtils.safeGetBigDecimal(dbResult, "STEP_SIZE");
		this.fileNo = JDBCUtils.safeGetInt(dbResult, "FILE_NO");
	}

	public Tablespace getTablespace() {
		return tablespace;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, order = 1)
	public String getName() {
		return name;
	}

	@Property(order = 2)
	public long getNodeId() {
		return nodeId;
	}

	@Property(order = 3)
	public long getSpaceId() {
		return spaceId;
	}

	@Property(order = 4)
	public int getFileNo() {
		return fileNo;
	}

	@Property(viewable = true, order = 6)
	public BigDecimal getMaxSize() {
		return maxSize;
	}

	@Property(viewable = true, order = 7)
	public BigDecimal getStepSize() {
		return stepSize;
	}

	@Property(viewable = true, order = 8)
	public String getPath() {
		return path;
	}

	@Property(order = 14)
	public boolean isTemporary() {
		return temporary;
	}
}
