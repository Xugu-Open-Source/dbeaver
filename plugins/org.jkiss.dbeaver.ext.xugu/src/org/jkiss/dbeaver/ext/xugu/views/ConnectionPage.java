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
package org.jkiss.dbeaver.ext.xugu.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.xugu.Activator;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.dbeaver.ui.dialogs.connection.DriverPropertiesDialogPage;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;
import java.util.TimeZone;

/**
 * 创建连接页面
 */
public class ConnectionPage extends ConnectionPageAbstract implements ICompositeDialogPage {
	private Text hostText;
	private Text portText;
	private Text dbText;
	private Text usernameText;
	private Text passwordText;

	private boolean activated = false;

	private static ImageDescriptor LOGO_IMG = Activator.getImageDescriptor("icons/logo.png");
	private Combo serverTimezoneCombo;
	private Combo roleCombo;

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public void createControl(Composite composite) {
		ModifyListener textListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (activated) {
					site.updateButtons();
				}
			}
		};
		final int fontHeight = UIUtils.getFontHeight(composite);

		Composite addrGroup = UIUtils.createPlaceholder(composite, 2);
		GridLayout gl = new GridLayout(2, false);
		addrGroup.setLayout(gl);
		GridData gd = new GridData(GridData.FILL_BOTH);
		addrGroup.setLayoutData(gd);

		Label hostLabel = UIUtils.createControlLabel(addrGroup, Messages.dialog_connection_host);
		hostLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		hostText = new Text(addrGroup, SWT.BORDER);
		hostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		hostText.addModifyListener(textListener);

		Label portLabel = UIUtils.createControlLabel(addrGroup, Messages.dialog_connection_port);
		portLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		portText = new Text(addrGroup, SWT.BORDER);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint = fontHeight * 10;
		portText.setLayoutData(gd);
		portText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.getDefault()));
		portText.addModifyListener(textListener);

		Label dbLabel = UIUtils.createControlLabel(addrGroup, Messages.dialog_connection_database);
		dbLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		dbText = new Text(addrGroup, SWT.BORDER);
		dbText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		dbText.addModifyListener(textListener);

		Label usernameLabel = UIUtils.createControlLabel(addrGroup, Messages.dialog_connection_user_name);
		usernameLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		usernameText = new Text(addrGroup, SWT.BORDER);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint = fontHeight * 20;
		usernameText.setLayoutData(gd);
		usernameText.addModifyListener(textListener);

		Label passwordLabel = UIUtils.createControlLabel(addrGroup, Messages.dialog_connection_password);
		passwordLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

		Composite passPh = UIUtils.createPlaceholder(addrGroup, 2, 5);
		passPh.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		passwordText = new Text(passPh, SWT.BORDER | SWT.PASSWORD);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.widthHint = fontHeight * 20;
		passwordText.setLayoutData(gd);
		passwordText.addModifyListener(textListener);

		roleCombo = UIUtils.createLabelCombo(addrGroup, Messages.dialog_connection_server_role, SWT.READ_ONLY);
		roleCombo.add(Messages.dialog_connection_role_normal);
		roleCombo.add(Messages.dialog_connection_role_sysdba);
		roleCombo.add(Messages.dialog_connection_role_dba);
		UIUtils.createHorizontalLine(addrGroup, 2, 10);
		roleCombo.select(0);

		serverTimezoneCombo = UIUtils.createLabelCombo(addrGroup, Messages.dialog_connection_server_timezone,
				SWT.READ_ONLY);
		serverTimezoneCombo.add(Messages.dialog_connection_auto_detect);
		{
			String[] tzList = TimeZone.getAvailableIDs();
			for (String tzId : tzList) {
				serverTimezoneCombo.add(tzId);
			}
		}
		serverTimezoneCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

		createDriverPanel(addrGroup);
		setControl(addrGroup);
	}

	@Override
	public boolean isComplete() {
		return hostText != null && portText != null && !CommonUtils.isEmpty(hostText.getText())
				&& !CommonUtils.isEmpty(portText.getText());
	}

	@Override
	public void loadSettings() {
		super.loadSettings();

		DBPDriver driver = getSite().getDriver();
		if (!activated) {
			setImageDescriptor(LOGO_IMG);
		}

		// Load values from new connection info
		DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
		if (hostText != null) {
			if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
				hostText.setText(connectionInfo.getHostName());
			} else {
				hostText.setText(Constants.DEFAULT_HOST);
			}
		}
		if (portText != null) {
			if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
				portText.setText(String.valueOf(connectionInfo.getHostPort()));
			} else if (site.getDriver().getDefaultPort() != null) {
				portText.setText(site.getDriver().getDefaultPort());
			} else {
				portText.setText("");
			}
		}
		if (dbText != null) {
			dbText.setText(CommonUtils.notEmpty(connectionInfo.getDatabaseName()));
		}
		if (usernameText != null) {
			usernameText.setText(CommonUtils.notEmpty(connectionInfo.getUserName()));
		}
		if (passwordText != null) {
			passwordText.setText(CommonUtils.notEmpty(connectionInfo.getUserPassword()));
		}
		if (roleCombo != null) {
			roleCombo.setText(CommonUtils.notEmpty(connectionInfo.getServerName()));
		}
		if (serverTimezoneCombo != null) {
			String tzProp = connectionInfo.getProviderProperty(Constants.PROP_SERVER_TIMEZONE);
			if (CommonUtils.isEmpty(tzProp)) {
				serverTimezoneCombo.select(0);
			} else {
				serverTimezoneCombo.setText(tzProp);
			}
		}
	}

	@Override
	public void saveSettings(DBPDataSourceContainer dataSource) {
		DBPConnectionConfiguration connectionInfo = dataSource.getConnectionConfiguration();
		if (hostText != null) {
			connectionInfo.setHostName(hostText.getText().trim());
		}
		if (portText != null) {
			connectionInfo.setHostPort(portText.getText().trim());
		}
		if (dbText != null) {
			connectionInfo.setDatabaseName(dbText.getText().toUpperCase().trim());
		}
		if (usernameText != null) {
			connectionInfo.setUserName(usernameText.getText().toUpperCase().trim());
		}
		if (passwordText != null) {
			connectionInfo.setUserPassword(passwordText.getText());
		}
		connectionInfo.setServerName(roleCombo.getText());
		if (serverTimezoneCombo != null) {
			if (serverTimezoneCombo.getSelectionIndex() == 0 || CommonUtils.isEmpty(serverTimezoneCombo.getText())) {
				connectionInfo.removeProviderProperty(Constants.PROP_SERVER_TIMEZONE);
			} else {
				connectionInfo.setProviderProperty(Constants.PROP_SERVER_TIMEZONE, serverTimezoneCombo.getText());
			}
		}
		connectionInfo.setProviderProperty(Constants.PROP_INTERNAL_LOGON,
				roleCombo.getText().toUpperCase(Locale.ENGLISH));
		
		String url = String.format("jdbc:%s://%s:%s/%s",
				OemConfig.OEM_NAME_EN_LOWER,
				hostText.getText().trim(),
				portText.getText().trim(),
				dbText.getText().toUpperCase().trim());
		connectionInfo.setUrl(url);
	}

	static class WarningDialog extends Dialog {
		private String warningInfo;

		public WarningDialog(Shell parentShell, String info) {
			super(parentShell);
			this.warningInfo = info;
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			getShell().setText(Messages.dialog_connection_connection);

			Control container = super.createDialogArea(parent);
			Composite composite = UIUtils.createPlaceholder((Composite) container, 2, 5);
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			Label infoText = UIUtils.createLabel(composite, "Warning:" + this.warningInfo);
			infoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			return parent;
		}
	}

	private void updateUi() {
		if (activated) {
			site.updateButtons();
		}
	}

	private class ControlsListener implements ModifyListener, SelectionListener {
		@Override
		public void modifyText(ModifyEvent e) {
			updateUi();
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			updateUi();
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			updateUi();
		}
	}

	public IDialogPage[] getSubPages() {
		return new IDialogPage[] { new DriverPropertiesDialogPage(this) };
	}

	@Override
	public IDialogPage[] getSubPages(boolean extrasOnly, boolean forceCreate) {
		return new IDialogPage[] { new DriverPropertiesDialogPage(this) };
	}
}
