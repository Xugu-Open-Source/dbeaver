/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.xugu.editors;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ext.xugu.model.User;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;

/**
 * 基本用户编辑器
 */
public abstract class BaseUserEditor extends AbstractDatabaseObjectEditor<User> {
	@Override
	public void setFocus() {
		if (getPageControl() != null) {
			getPageControl().setFocus();
		}
	}

	/**
	 * 获取用户页面控制对象
	 * 
	 * @return 用户页面控制对象
	 */
	protected abstract UserPageControl getPageControl();

	protected class UserPageControl extends ObjectEditorPageControl {
		public UserPageControl(Composite parent) {
			super(parent, SWT.NONE, BaseUserEditor.this);
		}

		@Override
		public void fillCustomActions(IContributionManager contributionManager) {
			super.fillCustomActions(contributionManager);
			DatabaseEditorUtils.contributeStandardEditorActions(getSite(), contributionManager);
		}
	}
}