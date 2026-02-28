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
package org.jkiss.dbeaver.ext.xugu.config;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.internal.XuguUIMessages;
import org.jkiss.dbeaver.ext.xugu.model.ProcedureStandalone;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.CreateProcedurePage;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Map;

/**
 * Procedure配置
 */
public class XuguProcedureConfigurator implements DBEObjectConfigurator<ProcedureStandalone> {

    @Override
    public ProcedureStandalone configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                               @Nullable Object container, @NotNull ProcedureStandalone procedure, @NotNull Map<String, Object> options) {
        return UITask.run(() -> {
            CreateProcedurePage editPage = new CreateProcedurePage(procedure) {
                @Override
                public DBSProcedureType getDefaultProcedureType() {
                    if (XuguUIMessages.tree_procedures_node_name.equals(options.get("container").toString())) {
                        return DBSProcedureType.PROCEDURE;
                    } else {
                        return DBSProcedureType.FUNCTION;
                    }
                }
            };
            if (!editPage.edit()) {
                return null;
            }
            procedure.setProcedureType(editPage.getProcedureType());

            StringBuilder desc = new StringBuilder(100);
            desc.append("CREATE OR REPLACE ");
            desc.append(editPage.getProcedureType());
            desc.append(" ");
            desc.append(procedure.getSchema().getName());
            desc.append(".");
            desc.append(editPage.getProcedureName());
            if (editPage.getProcedureType().equals(DBSProcedureType.PROCEDURE)) {
                desc.append(GeneralUtils.getDefaultLineSeparator());
                desc.append("IS ");
                desc.append(GeneralUtils.getDefaultLineSeparator());
                desc.append("BEGIN ");
            } else {
                desc.append(GeneralUtils.getDefaultLineSeparator());
                desc.append("-- Return DataType --");
                desc.append(GeneralUtils.getDefaultLineSeparator());
                desc.append("RETURN ");
                desc.append(GeneralUtils.getDefaultLineSeparator());
                desc.append("AS ");
                desc.append(GeneralUtils.getDefaultLineSeparator());
                desc.append("-- Variable Declaration --");
                desc.append(GeneralUtils.getDefaultLineSeparator());
                desc.append("BEGIN ");
            }
            desc.append(GeneralUtils.getDefaultLineSeparator());
            desc.append("-- Procedure/Function body --");
            desc.append(GeneralUtils.getDefaultLineSeparator());
            desc.append("END ");
            desc.append(";");

            procedure.setName(editPage.getProcedureName());
            procedure.setObjectDefinitionText(desc.toString());
            procedure.setValid(true);
            return procedure;
        });
    }

}
