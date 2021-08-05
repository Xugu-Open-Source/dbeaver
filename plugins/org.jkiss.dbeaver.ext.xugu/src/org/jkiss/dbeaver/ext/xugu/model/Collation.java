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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ≈≈–Ú
 */
public class Collation extends BaseInformation {
	private Charset charset;
	private String name;
	private int maxBytes;
	private int minBytes;
	private String comment;

	public Collation(Charset charset, ResultSet dbResult) throws SQLException {
		super(charset.getDataSource());
		this.charset = charset;
		this.loadInfo(dbResult);
	}

	private void loadInfo(ResultSet dbResult) throws SQLException {
		this.name = JDBCUtils.safeGetString(dbResult, "COLLATE_NAME");
		this.maxBytes = JDBCUtils.safeGetInt(dbResult, "MAX_BYTES");
		this.minBytes = JDBCUtils.safeGetInt(dbResult, "MIN_BYTES");
		this.comment = JDBCUtils.safeGetString(dbResult, "COMMENT");
	}

	@Property(viewable = true, order = 2)
	public Charset getCharset() {
		return charset;
	}

	@NotNull
	@Override
	@Property(viewable = true, order = 1)
	public String getName() {
		return name;
	}

	@Property(viewable = true, order = 3)
	public int getMaxBytes() {
		return maxBytes;
	}

	@Property(viewable = true, order = 4)
	public int getMinBytes() {
		return minBytes;
	}

	@Property(viewable = true, order = 5)
	public String getComment() {
		return comment;
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public DBSObject getParentObject() {
		return charset;
	}
}
