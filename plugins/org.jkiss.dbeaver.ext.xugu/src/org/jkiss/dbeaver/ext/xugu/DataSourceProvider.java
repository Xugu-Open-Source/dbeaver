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
package org.jkiss.dbeaver.ext.xugu;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.ext.xugu.internal.Constants;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 加载数据源信息
 */
public class DataSourceProvider extends JDBCDataSourceProvider implements DBPNativeClientLocationManager {

	public DataSourceProvider() {
		super(DataSourceProvider.class);
	}

	@Nullable
	private static Map<String, DBPNativeClientLocation> localClients;

	@Override
	public long getFeatures() {
		return FEATURE_SCHEMAS;
	}

	@Override
	public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
		Constants.ConnectionType connectionType;
		String conTypeProperty = connectionInfo.getProviderProperty(Constants.PROP_CONNECTION_TYPE);
		if (conTypeProperty != null) {
			connectionType = Constants.ConnectionType.valueOf(CommonUtils.toString(conTypeProperty));
		} else {
			connectionType = Constants.ConnectionType.BASIC;
		}
		if (connectionType == Constants.ConnectionType.CUSTOM) {
			return connectionInfo.getUrl();
		}
		StringBuilder url = new StringBuilder(100);
		url.append(String.format("jdbc:%s://", OemConfig.OEM_NAME_EN_LOWER));
		if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
			url.append(connectionInfo.getHostName());
		}
		if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
			url.append(":");
			url.append(connectionInfo.getHostPort());
		}
		if  (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
			url.append("/");
			url.append(connectionInfo.getDatabaseName());
		}
			
		return url.toString();
	}

	@NotNull
	@Override
	public DBPDataSource openDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
			throws DBException {
		return new DataSource(monitor, container);
	}

	@Override
	public List<DBPNativeClientLocation> findLocalClientLocations() {
		return new ArrayList<>(findLocalClients().values());
	}

	@Override
	public DBPNativeClientLocation getDefaultLocalClientLocation() {
		return null;
	}

	@Override
	public String getProductName(DBPNativeClientLocation location) {
		return null;
	}

	@Override
	public String getProductVersion(DBPNativeClientLocation location) {
		return null;
	}


	@NotNull
	private static synchronized Map<String, DBPNativeClientLocation> findLocalClients() {
		if (localClients != null) {
			return localClients;
		}
		if (RuntimeUtils.isWindows()) {
			localClients = findWindowsLocalClients();
		} /*else {
			localClients = findUnixLocalClients();
		}*/
		return localClients;
	}


	@NotNull
	private static Map<String, DBPNativeClientLocation> findWindowsLocalClients() {
		Map<String, DBPNativeClientLocation> result = new HashMap<>();

		// read from path
		String path = System.getenv("XUGU_DB");
		if (path != null) {
			result.put(path, new LocalNativeClientLocation(path, path));
		}

//		searchInWindowsRegistry(result, REGISTRY_ROOT_MYSQL_64, SERER_LOCATION_KEY);
//		searchInWindowsRegistry(result, REGISTRY_ROOT_MARIADB, INSTALLDIR_KEY);

		return result;
	}
}
