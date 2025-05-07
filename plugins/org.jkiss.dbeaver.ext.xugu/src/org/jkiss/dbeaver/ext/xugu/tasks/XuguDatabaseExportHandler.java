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
package org.jkiss.dbeaver.ext.xugu.tasks;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class XuguDatabaseExportHandler extends NativeToolHandler<XuguExportSettings, DBSObject, DatabaseExportInfo>{




    @Override
    public Collection<DatabaseExportInfo> getRunInfo(XuguExportSettings settings) {
        return settings.getExportObjects();
    }

    @Override
    protected XuguExportSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        XuguExportSettings exportSettings = new XuguExportSettings(task.getProject());
        exportSettings.loadSettings(context, new TaskPreferenceStore(task));
        return exportSettings;
    }

    @Override
    protected List<String> getCommandLine(XuguExportSettings settings, DatabaseExportInfo databaseExportInfo) throws IOException {
      return null;
    }


    @Override
    public void fillProcessParameters(XuguExportSettings settings, DatabaseExportInfo databaseExportInfo, List<String> cmd) throws IOException {
    }

    @Override
    protected void startProcessHandler(
            DBRProgressMonitor monitor,
            DBTTask task,
            XuguExportSettings settings,
            final DatabaseExportInfo arg,
            ProcessBuilder processBuilder,
            Process process,
            Log log
    ) throws IOException, DBException {
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        String outFileStr = settings.getOutputFile(arg);
        Path outFile = DBFUtils.resolvePathFromString(monitor, task.getProject(), outFileStr);
        if (Files.exists(outFile)) {
            // Unlike pg_dump, mysqldump happily overrides files which can easily lead to a lost dump.
            // We prevent that with our manual check
            // https://github.com/dbeaver/dbeaver/issues/11532
            throw new IOException("Output file already exists");
        }

        log.debug("Dump database into " + outFile.toUri());
        Thread job = new DumpCopierJob(monitor, "Dump database", process.getInputStream(), outFile, log);
        job.start();
    }







}
