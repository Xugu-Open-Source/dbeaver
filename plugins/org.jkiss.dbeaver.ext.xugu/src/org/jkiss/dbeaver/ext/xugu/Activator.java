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
package org.jkiss.dbeaver.ext.xugu;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.osgi.framework.BundleContext;

/**
 * 此插件激活类，控制插件生命周期
 */
public class Activator extends AbstractUIPlugin {
	/**
	 * 插件唯一标识
	 */
	public static final String PLUGIN_ID = "org.jkiss.dbeaver.ext.xugu";

	/**
	 * 插件共享实例
	 */
	private static Activator plugin;

	/**
	 * 插件默认构造函数
	 */
	public Activator() {
	}

	/**
	 * 插件启动函数
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		PrefUtils.setDefaultPreferenceValue(DBeaverCore.getGlobalPreferenceStore(), Constants.PREF_SUPPORT_ROWID, true);
		PrefUtils.setDefaultPreferenceValue(DBeaverCore.getGlobalPreferenceStore(), Constants.PREF_DBMS_OUTPUT, true);
		PrefUtils.setDefaultPreferenceValue(DBeaverCore.getGlobalPreferenceStore(),
				Constants.PREF_DBMS_READ_ALL_SYNONYMS, true);
		PrefUtils.setDefaultPreferenceValue(DBeaverCore.getGlobalPreferenceStore(),
				Constants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING, true);
	}

	/**
	 * 插件停止函数
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * 获取插件共享实例
	 *
	 * @return 共享实例
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * 
	 * 通过给定的相对路径获取图像描述符
	 *
	 * @param 图像文件相对路径
	 * @return 图像描述符
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
