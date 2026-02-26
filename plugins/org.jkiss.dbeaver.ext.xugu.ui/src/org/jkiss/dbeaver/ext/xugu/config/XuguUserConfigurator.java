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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Role;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.ext.xugu.views.WarningDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;
import java.util.Map;

/**
 * User配置
 */
public class XuguUserConfigurator implements DBEObjectConfigurator<User> {
    /**
     * 系统角色信息
     */
    public static Collection<Role> roleList;


    public static Collection<Role> getRolesList() {
        return roleList;
    }

    @Override
    public User configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                @Nullable Object container, @NotNull User user, @NotNull Map<String, Object> options) {
        DataSource parent = (DataSource) container;
        return UITask.run(() -> {
            CreateUserDialog dialog = new CreateUserDialog(UIUtils.getActiveWorkbenchShell(), parent, monitor);
            if (dialog.open() != IDialogConstants.OK_ID) {
                return null;
            }
            user.setName(dialog.getUserName());
            return user;
        });
    }


    static class CreateUserDialog extends Dialog{
        private DataSource dataSource;
        private Combo roleCombo;
        private Text userNameText;
        private Text passwordText;
        private Text roleText;
        private Text isLockedText;
        private Text expiredText;
        private Text validUntilText;


        private Role role;
        private User user;
        private String roleName;
        private String userName;
        private String password;
        private String isLocked;
        private String expired;
        private String validUntil;


        private DBRProgressMonitor monitor;

        public CreateUserDialog(Shell shell, DataSource dataSource, DBRProgressMonitor monitor) {
            super(shell);
            this.user = new User(dataSource, monitor, false);
            this.dataSource = dataSource;
            this.monitor = monitor;
        }

        public Role getRole() {
            return role;
        }



        public User user() {
            return user;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }

        public String getRoleName() {
            return roleName;
        }

        public String getIsLocked() {
            return isLocked;
        }

        public String getExpired() {
            return expired;
        }

        public String getValidUntil() {
            return validUntil;
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
            getShell().setText(Messages.dialog_user_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 3);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            userNameText = UIUtils.createLabelText(composite, Messages.dialog_connection_user_name, null);
            userNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

//			passwordText = UIUtils.createLabelText(composite, Messages.dialog_connection_password, null);
//			passwordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
// 			isLockedText = UIUtils.createLabelText(composite, Messages.dialog_connection_islocked, null);
// 			isLockedText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
// 			expiredText = UIUtils.createLabelText(composite, Messages.dialog_connection_expired, null);
// 			expiredText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
// 			validUntilText = UIUtils.createLabelText(composite, Messages.dialog_connection_valid_until, null);
// 			validUntilText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
//			roleCombo = UIUtils.createLabelCombo(composite, Messages.dialog_connection_role, 8);
//			roleCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            try {
                roleList = this.dataSource.roleCache.getAllObjects(monitor, this.dataSource);
//				Iterator<Role> it = roleList.iterator();
//				while(it.hasNext()) {
//					Role role = it.next();
//					roleCombo.add(role.getName());
//				}
            } catch (DBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            UIUtils.createInfoLabel(composite, Messages.dialog_user_create_info, GridData.FILL_HORIZONTAL, 4);
            return parent;
        }

        @Override
        protected void okPressed() {
            if (Utils.checkString(userNameText.getText())) {
//				user = new User(null,DBObjectNameCaseTransformer.transformObjectName(user, schemaOwner.getText()));
//				user.setName(DBObjectNameCaseTransformer.transformObjectName(user, schemaOwner.getText()));
//				userNameString = DBObjectNameCaseTransformer.transformObjectName(schema, schemaOwner.getText());
//				schema.setName(DBObjectNameCaseTransformer.transformObjectName(schema, nameText.getText()));
//				schema.setUser(user);
                userName = DBObjectNameCaseTransformer.transformName(dataSource, userNameText.getText());
//				password = DBObjectNameCaseTransformer.transformName(dataSource, passwordText.getText());
// 				isLocked = DBObjectNameCaseTransformer.transformName(dataSource, isLockedText.getText());
// 				validUntil = DBObjectNameCaseTransformer.transformName(dataSource, validUntilText.getText());
// 				expired = DBObjectNameCaseTransformer.transformName(dataSource, expiredText.getText());
//				roleName = DBObjectNameCaseTransformer.transformName(dataSource, roleCombo.getText());
                user.setName(userName);
//				user.setPassword(password);
//				user.setLocked(Boolean.getBoolean(isLocked));
//				user.setUntilTime(validUntil);
//				user.setExpired(Boolean.getBoolean(expired));
//				user.setRoleList(roleName);
                super.okPressed();
            } else {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "user name can not be empty");
                warnDialog.open();
            }
        }
    }
}
