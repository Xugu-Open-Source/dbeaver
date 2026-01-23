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
import org.jkiss.dbeaver.ext.xugu.edit.WarningDialog;
import org.jkiss.dbeaver.ext.xugu.internal.Constants;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ext.xugu.internal.Utils;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Synonym;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.Map;

/**
 * 同义词配置
 */
public class XuguSynonymConfigurator implements DBEObjectConfigurator<Synonym> {
    @Override
    public Synonym configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext,
                                   @Nullable Object container, @NotNull Synonym synonym, @NotNull Map<String, Object> options) {
        return UITask.run(() -> {
            if (container instanceof DataSource dataSource) {
                return new UITask<Synonym>() {
                    @Override
                    protected Synonym runTask() {
                        NewSynonymDialog dialog = new NewSynonymDialog(UIUtils.getActiveWorkbenchShell(), dataSource.publicSchema);
                        if (dialog.open() != IDialogConstants.OK_ID) {
                            return null;
                        }
                        Synonym newSynonym = dialog.getSynonym();
                        synonym.setName(newSynonym.getName());
                        synonym.setTargetName(newSynonym.getTargetName());
                        synonym.setPublic(newSynonym.isPublic());
                        return synonym;
                    }
                }.execute();
            } else {
                return new UITask<Synonym>() {
                    @Override
                    protected Synonym runTask() {
                        NewSynonymDialog dialog = new NewSynonymDialog(UIUtils.getActiveWorkbenchShell(), (Schema) container);
                        if (dialog.open() != IDialogConstants.OK_ID) {
                            return null;
                        }
                        Synonym newSynonym = dialog.getSynonym();
                        synonym.setName(newSynonym.getName());
                        synonym.setTargetName(newSynonym.getTargetName());
                        synonym.setPublic(newSynonym.isPublic());
                        return synonym;
                    }
                }.execute();
            }
        });
    }

    static class NewSynonymDialog extends Dialog {
        private Synonym synonym;
        private Text nameText;
        private Text tarNameText;
        private Button isPublicButton;

        public NewSynonymDialog(Shell parentShell, Schema schema) {
            super(parentShell);
            this.synonym = new Synonym(schema, null);
            this.synonym.setPublic(Constants.USER_PUBLIC.equals(schema.getName()));
        }

        public Synonym getSynonym() {
            return synonym;
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
            getShell().setText(Messages.dialog_synonym_create_title);

            Control container = super.createDialogArea(parent);
            Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
            composite.setLayoutData(new GridData(GridData.FILL_BOTH));

            nameText = UIUtils.createLabelText(composite, Messages.dialog_synonym_name, null);
            nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            tarNameText = UIUtils.createLabelText(composite, Messages.dialog_synonym_target, null);
            tarNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            isPublicButton = UIUtils.createCheckbox(composite, "Is Public", synonym.isPublic());
            isPublicButton.setEnabled(false);

            UIUtils.createInfoLabel(composite, Messages.dialog_synonym_create_info, GridData.FILL_HORIZONTAL, 2);

            return parent;
        }

        @Override
        protected void okPressed() {
            if (Utils.checkString(nameText.getText())) {
                synonym.setName(DBObjectNameCaseTransformer.transformObjectName(synonym, nameText.getText()));
                synonym.setTargetName(DBObjectNameCaseTransformer.transformObjectName(synonym, tarNameText.getText()));
                synonym.setPublic(isPublicButton.getSelection());
                super.okPressed();
            } else {
                WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "同义词名称不能为空");
                warnDialog.open();
            }
        }
    }

}
