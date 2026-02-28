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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.internal.Constants;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.model.TableColumn;
import org.jkiss.dbeaver.ext.xugu.model.TableIndex;
import org.jkiss.dbeaver.ext.xugu.model.TableIndexColumn;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Index配置
 */
public class XuguIndexConfigurator implements DBEObjectConfigurator<TableIndex> {

    @Override
    public TableIndex configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                      @Nullable Object container, @NotNull TableIndex index, @NotNull Map<String, Object> options) {
        return UITask.run(() -> {
            List<DBSIndexType> indexTypes = new ArrayList<>();
            indexTypes.add(Constants.INDEX_TYPE_BTREE);
//				indexTypes.add(Constants.INDEX_TYPE_FULL_TEXT);
//				indexTypes.add(Constants.INDEX_TYPE_BITMAP);
            EditIndexPage editPage = new EditIndexPage(Messages.edit_index_manager_dialog_title, index, indexTypes);
            if (!editPage.edit()) {
                return null;
            }

            StringBuilder idxName = new StringBuilder(64);
            idxName.append(CommonUtils.escapeIdentifier(index.getTable().getName())).append("_")
                    .append(CommonUtils
                            .escapeIdentifier(editPage.getSelectedAttributes().iterator().next().getName()))
                    .append("_IDX");
            index.setName(DBObjectNameCaseTransformer.transformName(index.getTable().getDataSource(), idxName.toString()));
            index.setUnique(editPage.isUnique());
            index.setIndexType(editPage.getIndexType());
            index.setLocal(true);
            int colIndex = 1;
            for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                index.addColumn(new TableIndexColumn(index, (TableColumn) tableColumn, colIndex++,
                        !Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC)),
                        null));
            }
            return index;
        });
    }

    /**
     * 为了设置local属性实现的继承自EditIndexPage的界面类
     */
    private class InnerIndexPage extends EditIndexPage {
        private Combo globalCombo;
        private boolean flag;

        public InnerIndexPage(String title, DBSTableIndex index, Collection<DBSIndexType> indexTypes) {
            super(title, index, indexTypes);
            // TODO Auto-generated constructor stub
        }

        @Override
        protected void createContentsBeforeColumns(Composite panel) {
            super.createContentsBeforeColumns(panel);
            UIUtils.createControlLabel(panel, "Is Local");
            globalCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
            globalCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            globalCombo.add("GLOBAL");
            globalCombo.add("LOCAL");
            globalCombo.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    String text = globalCombo.getText();
                    final String flagGlobal = "GLOBAL";
                    if (flagGlobal.equals(text)) {
                        flag = false;
                    } else {
                        flag = true;
                    }
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                }

            });
        }

        protected boolean isLocal() {
            return flag;
        }
    }

}
