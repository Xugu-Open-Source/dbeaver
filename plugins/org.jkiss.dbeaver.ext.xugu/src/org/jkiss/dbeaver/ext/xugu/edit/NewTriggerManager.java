package org.jkiss.dbeaver.ext.xugu.edit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.config.OemConfig;

import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.ext.xugu.model.NewTrigger;
import org.jkiss.dbeaver.ext.xugu.model.ObjectType;
import org.jkiss.dbeaver.ext.xugu.model.ObjectValidateAction;
import org.jkiss.dbeaver.ext.xugu.model.Schema;
import org.jkiss.dbeaver.ext.xugu.model.Trigger;
import org.jkiss.dbeaver.ext.xugu.model.Udt;
import org.jkiss.dbeaver.ext.xugu.model.View;
import org.jkiss.dbeaver.ext.xugu.views.WarningDialog;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTriggerManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;

public class NewTriggerManager  extends SQLObjectEditor<NewTrigger,Schema>{

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		// TODO Auto-generated method stub
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Override
	public DBSObjectCache<? extends DBSObject, NewTrigger> getObjectsCache(NewTrigger object) {
		// TODO Auto-generated method stub
		return object.getSchema().triggerCache;
	}

	@Override
	protected NewTrigger createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {
		
	
//			return new UITask<NewTrigger>() {
//				@Override
//				protected NewTrigger runTask() {
//					NewTriggerDialog dialog = new NewTriggerDialog(UIUtils.getActiveWorkbenchShell(), monitor);
//					if (dialog.open() != IDialogConstants.OK_ID) {
//						return null;
//					}
//					NewTrigger newTrigger = dialog.getTrigger();
//					return newTrigger;
//				}
//			}.execute();

//			return new UITask<NewTrigger>() {
//				@Override
//				protected NewTrigger runTask() {
//					WarningDialog dialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(), "You must create table first");
//					if (dialog.open() != IDialogConstants.OK_ID) {
//						return null;
//					}
//					return null;
//				}
//			}.execute(); 
 
		return null;
	}
	
	protected  void addObjectChangeActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<NewTrigger, Schema>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
		NewTrigger trigger = command.getObject();
		String sql = null;
		sql = trigger.getObjectDefinitionText(monitor, options);
		String bodyDefine = trigger.getExtendedDefinitionText(monitor);
		if (!(bodyDefine == null || bodyDefine.trim().isEmpty())) {
			sql += "\n" + bodyDefine;
		}
		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct create trigger sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Create trigger", sql));
	}
	
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand objectChangeCommand, Map<String, Object> options)
    {
    	try {
			addObjectChangeActions(monitor,executionContext, actionList, objectChangeCommand,options);
		} catch (DBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<NewTrigger, Schema>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
		final String commentKey = "comment";
		if (command.getProperty(commentKey) != null) {
			StringBuilder desc = new StringBuilder(100);
			desc.append("COMMENT ON TRIGGER ");
			desc.append(command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL));
			desc.append(" IS ");
			desc.append(SQLUtils.quoteString(command.getObject(), command.getObject().getCommentSting()));

			log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct add trigger comment sql: " + desc.toString());
			actions.add(new SQLDatabasePersistAction("Comment Trigger", desc.toString()));
		}
		
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<NewTrigger, Schema>.ObjectDeleteCommand command,
			Map<String, Object> options) {
		
		String sql = "DROP TRIGGER " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL);
		log.debug("[" + OemConfig.OEM_NAME_EN + "] Construct drop trigger sql: " + sql);
		actions.add(new SQLDatabasePersistAction("Drop trigger", sql));
		
	}

//	static class NewTriggerDialog extends Dialog {
//		private DBRProgressMonitor monitor;
//		private NewTrigger NewTrigger;
//		private Text nameText;
//		private Text parentTypeText;
//		private Text parentNameText;
//		private Combo triggerTypeCombo;
//		private Button triggerEventInsert;
//		private Button triggerEventUpdate;
//		private Button triggerEventDelete;
//		private Combo triggerTimingCombo;
//		private Text triggerConditionText;
//		private Table colListTable;
//		private Collection<org.jkiss.dbeaver.ext.xugu.model.TableColumn> colList;
//
//		public NewTriggerDialog(Shell parentShell , DBRProgressMonitor monitor) {
//			super(parentShell);
//			this.monitor = monitor;
//			colList = new ArrayList<>();
//		}
//
//		public NewTrigger getTrigger() {
//			
//			return NewTrigger;
//		}
//
//		@Override
//		protected boolean isResizable() {
//			return true;
//		}
//
//		@Override
//		protected Point getInitialSize() {
//			return new Point(450, 550);
//		}
//
//		@Override
//		protected Control createDialogArea(Composite parent) {
//			getShell().setText(Messages.dialog_trigger_create_title);
//
//			Control container = super.createDialogArea(parent);
//			Composite composite = UIUtils.createPlaceholder((Composite) container, 1, 5);
//			composite.setLayoutData(new GridData(GridData.FILL_BOTH));
//
//			nameText = UIUtils.createLabelText(composite, Messages.dialog_trigger_name, null);
//			nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			nameText.setEditable(true);
////
////			parentTypeText = UIUtils.createLabelText(composite, Messages.dialog_trigger_parent_type, null);
////			parentTypeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
////			parentTypeText.setText(ObjectType.TRIGGER.name());
////			parentTypeText.setEditable(false);
////
////			parentNameText = UIUtils.createLabelText(composite, Messages.dialog_trigger_parent_name, null);
////			parentNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
////			parentNameText.setText(table.getFullyQualifiedName(DBPEvaluationContext.DDL));
////			parentNameText.setEditable(false);
//
//			Composite eventBox = UIUtils.createPlaceholder(composite, 4, 1);
//			eventBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			CLabel eventLabel = UIUtils.createInfoLabel(eventBox, Messages.dialog_trigger_event);
//			eventLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			triggerEventInsert = UIUtils.createCheckbox(eventBox, Messages.dialog_trigger_event_insert, false);
//			triggerEventInsert.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			triggerEventUpdate = UIUtils.createCheckbox(eventBox, Messages.dialog_trigger_event_update, false);
//			triggerEventUpdate.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			triggerEventDelete = UIUtils.createCheckbox(eventBox, Messages.dialog_trigger_event_delete, false);
//			triggerEventDelete.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//
//			triggerTypeCombo = UIUtils.createLabelCombo(composite, Messages.dialog_trigger_type, 0);
//			triggerTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			triggerTypeCombo.add(Messages.dialog_trigger_type_row);
//			triggerTypeCombo.add(Messages.dialog_trigger_type_statement);
//			triggerTypeCombo.addSelectionListener(new SelectionListener() {
//				@Override
//				public void widgetSelected(SelectionEvent e) {
//					if (triggerTypeCombo.getSelectionIndex() == 0) {
//						// 元祖(行)级触发
//						triggerConditionText.setEditable(true);
//					} else {
//						// 语句级触发
//						triggerConditionText.setText("");
//						triggerConditionText.setEditable(false);
//					}
//				}
//
//				@Override
//				public void widgetDefaultSelected(SelectionEvent e) {
//				}
//			});
//
//			triggerTimingCombo = UIUtils.createLabelCombo(composite, Messages.dialog_trigger_timing, 0);
//			triggerTimingCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			triggerTimingCombo.add("BEFORE");
//			triggerTimingCombo.add("AFTER");
//			triggerTimingCombo.add("INSTEAD OF");
////			// 当创建视图触发器时，不展示timing界面
////			if (ObjectType.VIEW.equals(table.getType())) {
////				triggerTimingCombo.setText("INSTEAD OF");
////				triggerTimingCombo.setEnabled(false);
////			}
//
//			triggerConditionText = UIUtils.createLabelText(composite, Messages.dialog_trigger_condition, null);
//			triggerConditionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//			triggerConditionText.setEditable(false);
//
//			colListTable = new Table(composite, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
//			colListTable.setLayoutData(new GridData(GridData.FILL_BOTH));
//			colListTable.setHeaderVisible(true);
//			colListTable.setLinesVisible(true);
//			colListTable.setHeaderVisible(true);
//			colListTable.setLinesVisible(true);
//			String[] tableHeader = { "Column Name", "Data Type", "Precision", "Scale", "Default Value" };
//			for (int i = 0; i < tableHeader.length; i++) {
//				TableColumn tableColumn = new TableColumn(colListTable, SWT.NONE);
//				tableColumn.setText(tableHeader[i]);
//				// 设置表头可移动，默认为false
//				tableColumn.setMoveable(true);
//			}
//			// 动态加载所有列信息
//			triggerEventUpdate.addSelectionListener(new SelectionListener() {
//				@Override
//				public void widgetSelected(SelectionEvent e) {
//					String objType = parentTypeText.getText();
//					String objName = parentNameText.getText();
//					if (!triggerEventUpdate.getSelection()) {
//						// 清空列信息
//						colListTable.removeAll();
//					} else {
//						if (objType != null && !"".equals(objType) && objName != null && !"".equals(objName)) {
//							try {
//								colList = table.getAttributes(monitor);
//							} catch (DBException e1) {
//								e1.printStackTrace();
//							}
//							// 重新加载数据
//							if (colList.size() != 0) {
//								Iterator<org.jkiss.dbeaver.ext.xugu.model.TableColumn> it = colList.iterator();
//								while (it.hasNext()) {
//									org.jkiss.dbeaver.ext.xugu.model.TableColumn col = it.next();
//									TableItem item = new TableItem(colListTable, SWT.NONE);
//									item.setText(new String[] { col.getName(),
//											col.getDataType().toString() == null ? col.getTypeName()
//													: col.getDataType().toString(),
//											col.getPrecision() == null ? "" : String.valueOf(col.getPrecision()),
//											col.getScale() == null ? "" : String.valueOf(col.getScale()),
//											col.getDefaultValue() });
//								}
//							}
//							// 调整表格大小
//							for (int i = 0; i < tableHeader.length; i++) {
//								colListTable.getColumn(i).pack();
//							}
//						}
//					}
//				}
//
//				@Override
//				public void widgetDefaultSelected(SelectionEvent e) {
//					// TODO 小部件默认已选择事件
//				}
//			});
//			UIUtils.createInfoLabel(composite, Messages.dialog_trigger_label, GridData.FILL_HORIZONTAL, 2);
//
//			return parent;
//		}
//
//		@Override
//		protected void okPressed() {
//			if (!Utils.checkString(nameText.getText())) {
//				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
//						Messages.dialog_trigger_name_warn);
//				warnDialog.open();
//				return;
//			}
//			if (!triggerEventInsert.getSelection() && !triggerEventUpdate.getSelection()
//					&& !triggerEventDelete.getSelection()) {
//				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
//						Messages.dialog_trigger_event_warn);
//				warnDialog.open();
//				return;
//			}
//			if (triggerTypeCombo.getSelectionIndex() == -1) {
//				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
//						Messages.dialog_trigger_type_warn);
//				warnDialog.open();
//				return;
//			}
//			if (!Utils.checkString(triggerTimingCombo.getText())) {
//				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
//						Messages.dialog_trigger_timing_warn);
//				warnDialog.open();
//				return;
//			}
//			if (triggerTypeCombo.getSelectionIndex() == 0 && !Utils.checkString(triggerConditionText.getText())) {
//				WarningDialog warnDialog = new WarningDialog(UIUtils.getActiveWorkbenchShell(),
//						Messages.dialog_trigger_condition_warn);
//				warnDialog.open();
//				return;
//			}
//			String source = "\nBEGIN\n\nEND";
//			// 设置父对象信息
//			this.trigger = new Trigger(table, "");
//			trigger.setName(DBObjectNameCaseTransformer.transformObjectName(trigger, nameText.getText()));
//			trigger.setObjectType(parentTypeText.getText());
//			// 当创建视图触发器时，timing自动设为instead of
//			if (ObjectType.VIEW.equals(table.getType())) {
//				trigger.setTriggerTime(2);
//			} else {
//				trigger.setTriggerTime(triggerTimingCombo.getText());
//			}
//
//			trigger.setTriggerCondition(triggerConditionText.getText());
//			if (triggerTypeCombo.getSelectionIndex() > -1) {
//				trigger.setTriggerType(triggerTypeCombo.getSelectionIndex() + 1);
//			}
//			int event = 0;
//			if (triggerEventInsert.getSelection()) {
//				event += 1;
//			}
//			if (triggerEventUpdate.getSelection()) {
//				event += 2;
//			}
//			if (triggerEventDelete.getSelection()) {
//				event += 4;
//			}
//			trigger.setTriggeringEvent(event);
//			trigger.setObjectDefinitionText(source);
//			// 加载列信息
//			TableItem[] cols = colListTable.getItems();
//			if (cols != null) {
//				int sum = cols.length;
//				int i = 0;
//				ArrayList<String> includeCols = new ArrayList<String>();
//				while (i < sum) {
//					if (cols[i].getChecked()) {
//						includeCols.add(cols[i].getText(0));
//					}
//					i++;
//				}
//				trigger.setIncludeColumns(includeCols);
//			}
//			super.okPressed();
//		}
//	}
 

}
