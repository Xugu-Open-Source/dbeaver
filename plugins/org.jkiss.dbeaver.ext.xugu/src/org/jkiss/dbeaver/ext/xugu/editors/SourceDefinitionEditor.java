/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;

/**
 * Ô´¶¨Òå±à¼­Æ÷
 */
public class SourceDefinitionEditor extends SQLSourceViewer<SourceObject> {
	@Override
	protected String getCompileCommandId() {
		return Constants.CMD_COMPILE;
	}

	@Override
	protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
		return ((DBPScriptObjectExt) getSourceObject()).getExtendedDefinitionText(monitor);
	}

	@Override
	protected void setSourceText(DBRProgressMonitor monitor, String sourceText) {
		getInputPropertySource().setPropertyValue(monitor, Constants.PROP_OBJECT_BODY_DEFINITION, sourceText);
	}

	@Override
	protected boolean isReadOnly() {
		return false;
	}
}
