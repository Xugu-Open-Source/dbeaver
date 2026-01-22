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
import org.jkiss.dbeaver.ext.xugu.edit.WarningDialog;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Role;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

/**
 * Role配置
 */
public class XuguRoleConfigurator implements DBEObjectConfigurator<Role> {


    @Override
    public Role configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                @Nullable Object container, @NotNull Role role, @NotNull Map<String, Object> options) {
        DataSource parent = (DataSource) container;
        return UITask.run(() -> {
            InnerDialog dialog = new InnerDialog(UIUtils.getActiveWorkbenchShell(), monitor, parent);
            if (dialog.open() != IDialogConstants.OK_ID) {
                return null;
            }
            Role newRole = dialog.getRole();
            role.setName(newRole.getName());
            role.setUserDesc(newRole.getUserDesc());
            return role;
        });
    }

    static class InnerDialog extends Dialog {
        private Role role;
        private Text roleText;
        private Text userNameText;

        public InnerDialog(Shell parentShell, DBRProgressMonitor monitor, DataSource dataSource) {
            super(parentShell);
            this.role = new Role(dataSource, monitor, null);
        }

        public Role getRole() {
            return role;
        }

        @Override
        protected boolean isResizable() {
            return true;
        }

        @Override
        protected Point getInitialSize() {
            return new Point(300, 200);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            getShell().setText(Messages.dialog_role_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            roleText = UIUtils.createLabelText(composite, Messages.dialog_role_name, null);
            roleText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            userNameText = UIUtils.createLabelText(composite, Messages.dialog_role_user, null);
            userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            return parent;
        }

        @Override
        protected void okPressed() {
            if (Utils.checkString(roleText.getText())) {
                role.setName(DBObjectNameCaseTransformer.transformObjectName(role, roleText.getText()));
                role.setUserDesc(DBObjectNameCaseTransformer.transformObjectName(role, userNameText.getText()));
                super.okPressed();
            } else {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
                        "Role name cannot be null");
                warnDialog.open();
            }
        }
    }
}
