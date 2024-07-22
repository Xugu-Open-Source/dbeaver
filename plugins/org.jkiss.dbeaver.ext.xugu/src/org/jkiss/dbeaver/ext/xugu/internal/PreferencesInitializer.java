package org.jkiss.dbeaver.ext.xugu.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;

public class PreferencesInitializer extends AbstractPreferenceInitializer {

	public PreferencesInitializer() {}

	@Override
	public void initializeDefaultPreferences() {
		DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
		PrefUtils.setDefaultPreferenceValue(store, Constants.PREF_SUPPORT_ROWID, true);
		PrefUtils.setDefaultPreferenceValue(store, Constants.PREF_DBMS_OUTPUT, true);
		PrefUtils.setDefaultPreferenceValue(store, Constants.PREF_DBMS_READ_ALL_SYNONYMS, true);
		PrefUtils.setDefaultPreferenceValue(store, Constants.PREF_DISABLE_SCRIPT_ESCAPE_PROCESSING, true);
		PrefUtils.setPreferenceDefaultValue(store, ModelPreferences.RESULT_SET_MAX_ROWS_USE_SQL, true);
	}

}
