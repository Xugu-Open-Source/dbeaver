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

package org.jkiss.dbeaver.ext.xugu.model.source;

import org.jkiss.dbeaver.ext.xugu.model.SourceType;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * 源对象
 */
public interface SourceObject extends DBPScriptObject, StatefulObject {
	/**
	 * 设置名称
	 * 
	 * @param name 名称
	 */
	void setName(String name);

	/**
	 * 设置对象定义文本
	 * 
	 * @param source 定义文本
	 */
	void setObjectDefinitionText(String source);

	/**
	 * 获取源类型
	 * 
	 * @return 源类型
	 */
	SourceType getSourceType();

	/**
	 *  * 获取编译动作数组
	 * 
	 * @param monitor
	 * @return 编译动作数组
	 */
	DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor);
}
