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
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.model.ObjectStatus;
import org.jkiss.dbeaver.ext.xugu.model.TableConstraint;
import org.jkiss.dbeaver.ext.xugu.model.TableForeignKey;
import org.jkiss.dbeaver.ext.xugu.model.TableForeignKeyColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLForeignKeyManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * ForeignKey配置
 */
public class XuguForeignKeyConfigurator implements DBEObjectConfigurator<TableForeignKey> {

    @Override
    public TableForeignKey configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                           @Nullable Object container, @NotNull TableForeignKey foreignKey, @NotNull Map<String, Object> options) {
        return UITask.run(() -> {
            EditForeignKeyPage editPage = new EditForeignKeyPage(Messages.edit_foreign_key_manager_dialog_title,
                    foreignKey,
                    new DBSForeignKeyModifyRule[]{DBSForeignKeyModifyRule.NO_ACTION,
                            DBSForeignKeyModifyRule.CASCADE, DBSForeignKeyModifyRule.SET_NULL,
                            DBSForeignKeyModifyRule.SET_DEFAULT}, options);
            if (!editPage.edit()) {
                return null;
            }
            foreignKey.setReferencedConstraint((TableConstraint) editPage.getUniqueConstraint());

            String customName = editPage.getName();
            if (CommonUtils.isNotEmpty(customName)) {
                foreignKey.setName(customName);
            } else {
                SQLForeignKeyManager.updateForeignKeyName(monitor, foreignKey);
            }
            foreignKey.setDeleteRule(editPage.getOnDeleteRule());
            foreignKey.setEnable(foreignKey.getStatus() == ObjectStatus.ENABLED);
            int colIndex = 1;
            for (EditForeignKeyPage.FKColumnInfo tableColumn : editPage.getColumns()) {
                foreignKey.addColumn(new TableForeignKeyColumn(foreignKey, tableColumn.getOwnColumn(),
                        colIndex++, tableColumn.getRefColumn()));
            }
            SQLForeignKeyManager.updateForeignKeyName(monitor, foreignKey);
            return foreignKey;
        });
    }

}
