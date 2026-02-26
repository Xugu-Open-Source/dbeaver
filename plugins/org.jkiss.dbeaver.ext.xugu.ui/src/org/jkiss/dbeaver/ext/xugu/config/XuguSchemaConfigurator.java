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
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.ext.xugu.views.WarningDialog;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * 模式配置
 */
public class XuguSchemaConfigurator implements DBEObjectConfigurator<Schema> {


    @Override
    public Schema configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object container, @NotNull Schema newSchema, @NotNull Map<String, Object> options) {
        DataSource parent = (DataSource) container ;
        return UITask.run(() -> {
            NewUserDialog dialog = new NewUserDialog(UIUtils.getActiveWorkbenchShell(), parent, monitor);
            if (dialog.open() != IDialogConstants.OK_ID) {
                return null;
            }
            newSchema.setName(dialog.getSchema().getName());
			newSchema.setUser(dialog.getUser());
            return newSchema;
        });
    }

    static class NewUserDialog extends Dialog {

        private Schema schema;
        private User user;
        private Text nameText;
        private Text passwordText;
        private DataSource dataSource;
        private Combo schemaOwner;
        private DBRProgressMonitor monitor;
        private static String userNameString ;

        public NewUserDialog(Shell parentShell, DataSource dataSource, DBRProgressMonitor monitor) {
            super(parentShell);
            this.schema = new Schema(dataSource, -1, null);
            this.user = new User(dataSource, userNameString, false);
            this.dataSource = dataSource;
            this.monitor = monitor;
        }



        public User getUser() {
            return user;
        }

        public Schema getSchema() {
            return schema;
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
            getShell().setText(Messages.dialog_schema_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 3);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, Messages.dialog_schema_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            schemaOwner = UIUtils.createLabelCombo(composite, Messages.dialog_schema_user, 8);
            schemaOwner.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            try {
                Collection<User> userList = this.dataSource.userCache.getAllObjects(monitor, this.dataSource);
                Iterator<User> it = userList.iterator();
                while (it.hasNext()) {
                    User user = it.next();
                    schemaOwner.add(user.getName());
                }
            } catch (DBException e) {
                e.printStackTrace();
            }

            UIUtils.createInfoLabel(composite, Messages.dialog_schema_create_info, GridData.FILL_HORIZONTAL, 4);

            return parent;
        }

        @Override
        protected void okPressed() {
            if (Utils.checkString(nameText.getText())) {
                userNameString = DBObjectNameCaseTransformer.transformObjectName(schema, schemaOwner.getText());
                user.setName(userNameString);
                schema.setName(DBObjectNameCaseTransformer.transformObjectName(schema, nameText.getText()));
                super.okPressed();
            } else {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "Schema name can not be empty");
                warnDialog.open();
            }
        }

    }

}
