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
import org.jkiss.dbeaver.ext.xugu.model.Package;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EntityEditPage;

import java.util.Map;

/**
 * Package配置
 */
public class XuguPackageConfigurator implements DBEObjectConfigurator<Package> {

    @Override
    public Package configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                   @Nullable Object container, @NotNull Package pkg, @NotNull Map<String, Object> options) {
        return UITask.run(() -> {
            EntityEditPage editPage = new EntityEditPage(pkg.getDataSource(), DBSEntityType.PACKAGE);
            if (!editPage.edit()) {
                return null;
            }
            String packName = editPage.getEntityName();
            pkg.setName(packName);
            pkg.setObjectDefinitionText("CREATE OR REPLACE PACKAGE " + pkg.getSchema().getName() + "." + packName + "\n"
                    + "AS\n" + "-- Package header\n" + "END;");
            pkg.setExtendedDefinitionText("CREATE OR REPLACE PACKAGE BODY " + pkg.getSchema().getName() + "." + packName
                    + "\n" + "AS\n" + "-- Package body\n" + "END;");
            pkg.setValid(true);
            return pkg;
        });
    }

}
