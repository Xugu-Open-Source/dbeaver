package org.jkiss.dbeaver.ext.xugu.model;

 
import java.sql.Timestamp;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
 
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * 
 * @author zkun
 *
 */
public class PulbicSynonym  extends BaseGlobalObject implements DBSAlias{
	private int objectDbId;
	private int objectSchemaId;
	private String objectSchemaName;
	private int objectUserId;
	private String objectName;
	private String targetSchemaName;
	private String targetName;
	private boolean isPublic;
	private boolean valid;
	private Timestamp createTime;
	
	
	
	public PulbicSynonym(DataSource dataSource, String name) {
		super(dataSource, false);
		this.objectName = name;
	}


	@NotNull
	@Override
	@Property(hidden = true, viewable = false, editable = false, updatable = false, order = 1)
	public String getName() {
		return   objectName;
	}

	@NotNull
	@Property(viewable = true, editable = false, updatable = false, order = 1)
	public String getObjectName() {
		return objectName;
	}

	@Property(viewable = true, editable = false, updatable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 2)
	public String getTargetSchemaName() {
		return targetSchemaName;
	}

	@Property(viewable = true, editable = true, updatable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 3)
	public String getTargetName() {
		return targetName;
	}

	@Property(viewable = true, editable = false, updatable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 4)
	public Timestamp getCreateTime() {
		return createTime;
	}

	@Property(viewable = true, editable = false, updatable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 5)
	public boolean isPublic() {
		return isPublic;
	}

	@Property(viewable = true, order = 6)
	public boolean isValid() {
		return valid;
	}

	public Object getObjectOwner() {
		final Schema schema = getDataSource().schemaCache.getCachedObject(objectSchemaName);
		return schema == null ? objectSchemaName : schema;
	}

	@Override
	public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
		Object object = getObject(monitor);
		if (object instanceof DBSObject) {
			return (DBSObject) object;
		}
		return null;
	}

 
	public void setName(String name) {
		this.objectName = name;
	 
	}

	public int getObjectDbId() {
		return objectDbId;
	}

	public void setObjectDbId(int objectDbId) {
		this.objectDbId = objectDbId;
	}

	public int getObjectSchemaId() {
		return objectSchemaId;
	}

	public void setObjectSchemaId(int objectSchemaId) {
		this.objectSchemaId = objectSchemaId;
	}

	public String getObjectSchemaName() {
		return objectSchemaName;
	}

	public void setObjectSchemaName(String objectSchemaName) {
		this.objectSchemaName = objectSchemaName;
	}

	public int getObjectUserId() {
		return objectUserId;
	}

	public void setObjectUserId(int objectUserId) {
		this.objectUserId = objectUserId;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public void setTargetSchemaId(String targetSchemaName) {
		this.targetSchemaName = targetSchemaName;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void setCreateTime(Timestamp createTime) {
		this.createTime = createTime;
	}

	public void setTargetName(String name) {
		targetName = name;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public Object getObject(DBRProgressMonitor monitor) throws DBException {
		return ObjectType.resolveObject(monitor, getDataSource(), null, "SYNONYM", objectSchemaName, objectName);
	}

}
