/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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


import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.ext.xugu.conf.OemConfig;
import org.jkiss.dbeaver.ext.xugu.internal.XuguConstants;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.utils.CommonUtils;

public class XuguDataSourceProvider  extends GenericDataSourceProvider {

//    @Override
//    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
//        XuguConstants.ConnectionType connectionType;
//        String conTypeProperty = connectionInfo.getProviderProperty(XuguConstants.PROP_CONNECTION_TYPE);
//        if (conTypeProperty != null) {
//            connectionType = XuguConstants.ConnectionType.valueOf(CommonUtils.toString(conTypeProperty));
//        } else {
//            connectionType = XuguConstants.ConnectionType.BASIC;
//        }
//        if (connectionType == XuguConstants.ConnectionType.CUSTOM) {
//            return connectionInfo.getUrl();
//        }
//        StringBuilder url = new StringBuilder(100);
//        url.append(String.format("jdbc:%s://", OemConfig.OEM_NAME_EN_LOWER));
//        if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
//            url.append(connectionInfo.getHostName());
//        }
//        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
//            url.append(":");
//            url.append(connectionInfo.getHostPort());
//        }
//        if  (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
//            url.append("/");
//            url.append(connectionInfo.getDatabaseName());
//        }
//
//        return url.toString();
//    }
}
