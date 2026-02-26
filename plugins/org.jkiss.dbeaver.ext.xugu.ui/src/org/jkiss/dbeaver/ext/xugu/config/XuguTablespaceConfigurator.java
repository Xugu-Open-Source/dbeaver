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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Tablespace;
import org.jkiss.dbeaver.ext.xugu.views.WarningDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.sql.SQLException;
import java.util.Map;

/**
 * OracleConstraintConfigurator
 */
public class XuguTablespaceConfigurator implements DBEObjectConfigurator<Tablespace> {


    @Override
    public Tablespace configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                      @Nullable Object container, @NotNull Tablespace tablespace, @NotNull Map<String, Object> options) {
        DataSource parent = (DataSource) container;
        return UITask.run(() -> {
            NewTablespaceDialog dialog = new NewTablespaceDialog(UIUtils.getActiveWorkbenchShell(), parent);
            if (dialog.open() != IDialogConstants.OK_ID) {
                return null;
            }
            Tablespace newTablespace = dialog.getTableSpace();
            tablespace.setName(newTablespace.getName());
            tablespace.setFilePath(newTablespace.getFilePath());
            return tablespace;
        });
    }

    static class NewTablespaceDialog extends Dialog {
        private Tablespace space;
        private Text nameText;
        private Text fileText;
        private Text nodeText;

        public NewTablespaceDialog(Shell parentShell, DataSource dataSource) {
            super(parentShell);
            try {
                this.space = new Tablespace(dataSource, null);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public Tablespace getTableSpace() {
            return space;
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Point getInitialSize() {
            return new Point(300, 250);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            getShell().setText(Messages.dialog_tablespace_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, Messages.dialog_tablespace_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            nodeText = UIUtils.createLabelText(composite, Messages.dialog_tablespace_nodeID, null);
            nodeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            fileText = UIUtils.createLabelText(composite, Messages.dialog_tablespace_filePath, null);
            fileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createInfoLabel(composite, Messages.dialog_tablespace_create_info, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed() {
            if (Utils.checkString(nameText.getText())) {
                space.setName(DBObjectNameCaseTransformer.transformObjectName(space, nameText.getText()));
                space.setFilePath(DBObjectNameCaseTransformer.transformObjectName(space, fileText.getText()));
                if (nodeText.getText() != null) {
                    space.setNodeId(Integer
                            .parseInt(DBObjectNameCaseTransformer.transformObjectName(space, nodeText.getText())));
                } else {
                    space.setNodeId(0);
                }
                super.okPressed();
            } else {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Table space name can't be empty");
                warnDialog.open();
            }
        }
    }
}
