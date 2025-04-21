/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.xugu.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ext.xugu.internal.Messages;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * 警告对话框
 */
public class WarningDialog extends Dialog {
	private String warningInfo;

	public WarningDialog(Shell parentShell, String warningInfo) {
		super(parentShell);
		this.warningInfo = warningInfo;
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
		getShell().setText(Messages.dialog_warn_title);

		Control container = super.createDialogArea(parent);
		Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		UIUtils.createInfoLabel(composite, this.warningInfo, GridData.FILL_HORIZONTAL, 2);

		return parent;
	}
}
