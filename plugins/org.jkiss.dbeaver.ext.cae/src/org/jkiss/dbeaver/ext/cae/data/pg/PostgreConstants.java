package org.jkiss.dbeaver.ext.cae.data.pg;

public class PostgreConstants {
	// 
    public static final String PG_OBJECT_CLASS = "com.cae.cloudjdbc.util.DBobject";
    
    public static final String PG_ARRAY_CLASS = "org.postgresql.jdbc.PgArray";
    public static final String PG_INTERVAL_CLASS = "org.postgresql.util.PGInterval";
    
    public static final String PG_GEOMETRY_CLASS = "com.cae.gis.DBgeometry";
    // Workaround for Redshift 2.x
//    public static final String RS_OBJECT_CLASS = "com.amazon.redshift.util.RedshiftObject";
    // Workaround for EnterpriseDB
//    public static final String EDB_OBJECT_CLASS = "com.edb.util.PGobject";
}
