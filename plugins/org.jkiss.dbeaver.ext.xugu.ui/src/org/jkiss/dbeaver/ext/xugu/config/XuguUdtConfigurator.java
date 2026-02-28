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
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Udt;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.Map;

/**
 * Procedure配置
 */
public class XuguUdtConfigurator implements DBEObjectConfigurator<Udt> {

    @Override
    public Udt configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                               @Nullable Object container, @NotNull Udt udt, @NotNull Map<String, Object> options) {
        Schema schema = (Schema) container;
        return UITask.run(() -> {
            EntityEditPage page = new EntityEditPage(udt.getDataSource(), DBSEntityType.TYPE);
            if (!page.edit()) {
                return null;
            }
            udt.setName(page.getEntityName());
            udt.setTypeName(page.getEntityName());
            udt.setObjectDefinitionText(
                    "CREATE TYPE " + schema.getName() + "." + page.getEntityName() + " AS OBJECT");
            udt.setExtendedDefinitionText(
                    "-- CREATE TYPE BODY " + schema.getName() + "." + page.getEntityName() + " AS ");
            udt.setValid(true);
            return udt;
        });
    }

}
