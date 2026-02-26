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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.model.View;
import org.jkiss.dbeaver.ext.xugu.views.WarningDialog;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

/**
 * View配置
 */
public class XuguViewConfigurator implements DBEObjectConfigurator<View> {


    @Override
    public View configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                @Nullable Object container, @NotNull View view, @NotNull Map<String, Object> options) {
        return UITask.run(() -> {
            NewViewDialog dialog = new NewViewDialog(UIUtils.getActiveWorkbenchShell(), view);
            if (dialog.open() != IDialogConstants.OK_ID) {
                return null;
            }

            View newView = dialog.getView();
            boolean replace = dialog.getViewReplace();
            boolean force = dialog.getViewRorce();
            newView.setViewText("CREATE " + (replace ? "OR REPLACE " : "") + (force ? "FORCE " : "") + "VIEW "
                    + newView.getFullyQualifiedName(DBPEvaluationContext.DDL) + " AS\nSELECT");
            return newView;
        });
    }

    static class NewViewDialog extends Dialog {
        private View view;
        private Text nameText;
        private boolean viewReplace;
        private boolean viewRorce;
        private Button replaceCheck;
        private Button forceCheck;

        public NewViewDialog(Shell parentShell, View view) {
            super(parentShell);
            this.view = view;
        }

        public View getView() {
            return view;
        }

        public boolean getViewReplace() {
            return viewReplace;
        }

        public void setViewReplace(boolean viewReplace) {
            this.viewReplace = viewReplace;
        }

        public boolean getViewRorce() {
            return viewRorce;
        }

        public void setViewRorce(boolean viewRorce) {
            this.viewRorce = viewRorce;
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
            getShell().setText(Messages.dialog_view_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, Messages.dialog_view_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
//			replaceCheck = UIUtils.createLabelCheckbox(composite, Messages.dialog_view_replace, false);
//			replaceCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
//			forceCheck = UIUtils.createLabelCheckbox(composite, Messages.dialog_view_force, false);
//			forceCheck.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            return parent;
        }

        @Override
        protected void okPressed() {

            if (Utils.checkString(nameText.getText())) {
                view.setName(DBObjectNameCaseTransformer.transformObjectName(view, nameText.getText()));
//				this.viewReplace = replaceCheck.getSelection();
//				this.viewRorce = forceCheck.getSelection();
                super.okPressed();
            } else {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
                        "View name cannot be null");
                warnDialog.open();
            }
        }
    }
}
