/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.stream.importer;

import au.com.bytecode.opencsv.CSVReader;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.impl.local.LocalStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.stream.*;
import org.jkiss.dbeaver.tools.transfer.stream.importer.DataImporterCSV.HeaderPosition;
import org.jkiss.dbeaver.tools.transfer.stream.model.StreamDataSource;
import org.jkiss.dbeaver.tools.transfer.stream.model.StreamExecutionContext;
import org.jkiss.dbeaver.tools.transfer.stream.model.StreamTransferSession;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * CSV importer
 */
public class DataImporterSQL extends StreamImporterAbstract {

    private static final Log log = Log.getLog(DataImporterSQL.class);

    private static final String PROP_ENCODING = "encoding";

    public DataImporterSQL() {
    }

	@Override
	public List<StreamDataImporterColumnInfo> readColumnsInfo(InputStream inputStream) throws DBException {
		return Collections.emptyList();
	}

    private InputStreamReader openStreamReader(InputStream inputStream, Map<Object, Object> processorProperties) throws UnsupportedEncodingException {
        String encoding = CommonUtils.toString(processorProperties.get(PROP_ENCODING), GeneralUtils.UTF8_ENCODING);
        return new InputStreamReader(inputStream, encoding);
    }

	@Override
	public void runImport(DBRProgressMonitor monitor, InputStream inputStream, IDataTransferConsumer consumer)
			throws DBException {
		IStreamDataImporterSite site = getSite();
        Map<Object, Object> properties = site.getProcessorProperties();

        try (DBCSession session = consumer.getDatabaseObject()
        		.getDataSource()
        		.getDefaultInstance()
        		.getDefaultContext(monitor, false)
        		.openSession(monitor, DBCExecutionPurpose.UTIL, "Transfer stream data")) {
        	String fileString;
        	String[] originSqls;
        	try (InputStreamReader reader = openStreamReader(inputStream, properties);
            		BufferedReader bReader = new BufferedReader(reader)) {
        		fileString = IOUtils.readToString(bReader);
            } catch (IOException e) {
                throw new DBException("IO error reading SQL", e);
            }
        	originSqls = fileString.split(";");
        	List<String> resultSqls = new ArrayList<>();
            for (String sql : originSqls) {
                String tempSql = sql.trim();
                if (tempSql.length() == 0) {
                	continue;
                } else {
                	resultSqls.add(tempSql);
                }
            }
            for (String sql : resultSqls) {
            	try (DBCStatement statement = session.prepareStatement(DBCStatementType.QUERY, sql, false, false, false)) {
                	statement.executeStatement();
                }
            }
        }
	}
}
