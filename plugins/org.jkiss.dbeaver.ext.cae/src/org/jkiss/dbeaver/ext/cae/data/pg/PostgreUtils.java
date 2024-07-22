package org.jkiss.dbeaver.ext.cae.data.pg;

import org.jkiss.dbeaver.Log;

public class PostgreUtils {
	
    private static final Log log = Log.getLog(PostgreUtils.class);
    
    public static boolean isPGObject(Object object) {
        if (object == null) {
            return false;
        }
        String className = object.getClass().getName();
        return className.equals(PostgreConstants.PG_OBJECT_CLASS);
    }

    public static Object extractPGObjectValue(Object pgObject) {
        if (pgObject == null) {
            return null;
        }
        if (!isPGObject(pgObject)) {
            return pgObject;
        }
        try {
            return pgObject.getClass().getMethod("getValue").invoke(pgObject);
        } catch (Exception e) {
            log.debug("Can't extract value from " + pgObject.getClass().getName(), e);
        }
        return null;
    }
}
