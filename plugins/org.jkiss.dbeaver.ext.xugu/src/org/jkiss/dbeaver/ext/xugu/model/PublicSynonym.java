package org.jkiss.dbeaver.ext.xugu.model;

 
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.xugu.model.DataSource.UserRoleFlag;
import org.jkiss.dbeaver.ext.xugu.model.source.SourceObject;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import com.xugu.parser.DatabaseParsing;
import com.xugu.parser.Parsing;
import com.xugu.parser.Parsing.TableType;

/**
 * 
 * @author zkun
 *
 */
public class PublicSynonym  extends BaseObject<DBSObject> implements DBSEntity, DBPQualifiedObject, DBPScriptObject{
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
	
	public PublicSynonym(DataSource dataSource, ResultSet dbResult) {
		super(dataSource, JDBCUtils.safeGetString(dbResult, "SYNO_NAME"), true);
		this.objectDbId = JDBCUtils.safeGetInt(dbResult, "DB_ID");
		this.objectSchemaId = JDBCUtils.safeGetInt(dbResult, "SCHEMA_ID");
		this.objectSchemaName = JDBCUtils.safeGetString(dbResult, "CURR_SC");
		this.objectUserId = JDBCUtils.safeGetInt(dbResult, "USER_ID");
		this.objectName = JDBCUtils.safeGetString(dbResult, "SYNO_NAME");
		this.targetSchemaName = JDBCUtils.safeGetString(dbResult, "TARG_SC");
		this.targetName = JDBCUtils.safeGetString(dbResult, "TARG_NAME");
		this.isPublic = JDBCUtils.safeGetBoolean(dbResult, "IS_PUBLIC");
		this.valid = JDBCUtils.safeGetBoolean(dbResult, "VALID");
		this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
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

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		String objectFullName = DBUtils.getObjectFullName(this, DBPEvaluationContext.DDL);
		monitor.beginTask("Load sources for public synonym '" + objectFullName + "'...", 1);
		try (Connection conn = DBUtils.openUtilSession(monitor, this, "Get " + this.name + "DDL")) {
			String roleFlag = getDataSource().getRoleFlag();
			TableType tableType;

			if (UserRoleFlag.SYS.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.SYS;
			} else if (UserRoleFlag.DBA.name().equalsIgnoreCase(roleFlag)) {
				tableType = TableType.DBA;
			} else {
				tableType = TableType.ALL;
			}

			Parsing parsing = new Parsing();
			return parsing.getObjectDDL(conn, "0", getName(), "SEQUENCE", tableType);
		} catch (SQLException e) {
			throw new DBException("Close connection of DDL failed", e);
		}
	}


	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DBSEntityType getEntityType() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public List<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DBSEntityAttribute getAttribute(DBRProgressMonitor monitor, String attributeName) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Collection<? extends DBSEntityConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}
}
