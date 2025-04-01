package org.jkiss.dbeaver.ext.cae.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.data.gis.handlers.GISGeometryValueHandler;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * MySQLGeometryValueHandler
 */
public class GeometryValueHandler extends GISGeometryValueHandler {

    static final GeometryValueHandler INSTANCE = new GeometryValueHandler();

    public GeometryValueHandler() {
        setInvertCoordinates(true);
        setLeadingSRID(true);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (format == DBDDisplayFormat.NATIVE) {
            return value.toString();
        }
        return super.getValueDisplayString(column, value, format);
    }
}
