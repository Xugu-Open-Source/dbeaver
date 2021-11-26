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
package org.jkiss.dbeaver.ext.xugu.edit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.ext.xugu.Messages;
import org.jkiss.dbeaver.ext.xugu.model.TableColumn;
import org.jkiss.dbeaver.ext.xugu.model.TableIndex;
import org.jkiss.dbeaver.ext.xugu.model.TableIndexColumn;
import org.jkiss.dbeaver.ext.xugu.model.BaseTable;
import org.jkiss.dbeaver.ext.xugu.model.BaseTablePhysical;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.object.struct.EditIndexPage;
import org.jkiss.utils.CommonUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 索引管理器， 进行索引的增加和删除
 */
public class IndexManager extends SQLIndexManager<TableIndex, BaseTablePhysical> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, TableIndex> getObjectsCache(TableIndex object) {
		return object.getParentObject().getSchema().indexCache;
	}

	@Override
	protected TableIndex createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			final Object container, Object from, Map<String, Object> options) {
		BaseTablePhysical table = (BaseTablePhysical) container;

		final TableIndex index = new TableIndex(table.getSchema(), table, "INDEX", true, DBSIndexType.UNKNOWN);
		return new UITask<TableIndex>() {
			@Override
			protected TableIndex runTask() {
				List<DBSIndexType> indexTypes = new ArrayList<>();
				indexTypes.add(Constants.INDEX_TYPE_BTREE);
//				indexTypes.add(Constants.INDEX_TYPE_FULL_TEXT);
//				indexTypes.add(Constants.INDEX_TYPE_BITMAP);
				EditIndexPage editPage = new EditIndexPage(Messages.edit_index_manager_dialog_title, index, indexTypes);
				if (!editPage.edit()) {
					return null;
				}

				StringBuilder idxName = new StringBuilder(64);
				idxName.append(CommonUtils.escapeIdentifier(table.getName())).append("_")
						.append(CommonUtils
								.escapeIdentifier(editPage.getSelectedAttributes().iterator().next().getName()))
						.append("_IDX");
				index.setName(DBObjectNameCaseTransformer.transformName(table.getDataSource(), idxName.toString()));
				index.setUnique(editPage.isUnique());
				index.setIndexType(editPage.getIndexType());
				index.setLocal(true);
				int colIndex = 1;
				for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
					index.addColumn(new TableIndexColumn(index, (TableColumn) tableColumn, colIndex++,
							!Boolean.TRUE.equals(editPage.getAttributeProperty(tableColumn, EditIndexPage.PROP_DESC)),
							null));
				}
				return index;
			}
		}.execute();
	}

	/**
	 * 重新组装创建 index 语句，增加 local 关键字
	 */
	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
		final BaseTablePhysical table = command.getObject().getTable();
		final TableIndex index = command.getObject();

		// Create index
		final String indexName = DBUtils.getQuotedIdentifier(index.getDataSource(), index.getName());
		index.setName(indexName);
		final String tableName = DBUtils.getEntityScriptName(table, options);
		StringBuilder decl = new StringBuilder(40);
		decl.append("CREATE");
		appendIndexModifiers(index, decl);
		decl.append(" INDEX ").append(indexName);
		decl.append(" ON ").append(tableName).append(" (");
		// Get columns using void monitor
		boolean firstColumn = true;
		for (DBSTableIndexColumn indexColumn : CommonUtils
				.safeCollection(command.getObject().getAttributeReferences(new VoidProgressMonitor()))) {
			if (!firstColumn) {
				decl.append(",");
			}
			firstColumn = false;
			decl.append(DBUtils.getQuotedIdentifier(indexColumn));
			appendIndexColumnModifiers(monitor, decl, indexColumn);
		}
		decl.append(")");
		appendIndexType(index, decl);

		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, decl.toString()));

		actions.remove(0);
		// 局部或全局索引
		if (index.isLocal()) {
			decl.append(" LOCAL");
		} else {
			decl.append(" GLOBAL");
		}
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_create_new_index, decl.toString()));
	}

	/**
	 * 重写虛谷索引数据类型语法
	 */
	@Override
	protected void appendIndexType(TableIndex index, StringBuilder decl) {
		decl.append(" INDEXTYPE IS ");
		decl.append(index.getIndexType().getName());
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
		actions.add(new SQLDatabasePersistAction(ModelMessages.model_jdbc_drop_index,
				getDropIndexPattern(command.getObject())
						.replace(PATTERN_ITEM_TABLE,
								command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
						.replace(PATTERN_ITEM_INDEX, command.getObject().getName())
						.replace(PATTERN_ITEM_INDEX_SHORT, DBUtils.getQuotedIdentifier(command.getObject()))));
		String t = getDropIndexPattern(command.getObject())
				.replace(PATTERN_ITEM_TABLE,
						command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL))
				.replace(PATTERN_ITEM_INDEX, command.getObject().getName())
				.replace(PATTERN_ITEM_INDEX_SHORT, DBUtils.getQuotedIdentifier(command.getObject()));
	}

	@Override
	protected String getDropIndexPattern(TableIndex index) {
		return "DROP INDEX " + PATTERN_ITEM_TABLE + "." + PATTERN_ITEM_INDEX;
	}

	/**
	 * 为了设置local属性实现的继承自EditIndexPage的界面类
	 */
	private class InnerIndexPage extends EditIndexPage {
		private Combo globalCombo;
		private boolean flag;

		public InnerIndexPage(String title, DBSTableIndex index, Collection<DBSIndexType> indexTypes) {
			super(title, index, indexTypes);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void createContentsBeforeColumns(Composite panel) {
			super.createContentsBeforeColumns(panel);
			UIUtils.createControlLabel(panel, "Is Local");
			globalCombo = new Combo(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
			globalCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			globalCombo.add("GLOBAL");
			globalCombo.add("LOCAL");
			globalCombo.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					String text = globalCombo.getText();
					final String flagGlobal = "GLOBAL";
					if (flagGlobal.equals(text)) {
						flag = false;
					} else {
						flag = true;
					}
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}

			});
		}

		protected boolean isLocal() {
			return flag;
		}
	}

	@Override
	protected StringBuilder getNestedDeclaration(DBRProgressMonitor monitor, BaseTablePhysical owner,
			DBECommandAbstract<TableIndex> command, Map<String, Object> options) {
		return null;
	}
}
