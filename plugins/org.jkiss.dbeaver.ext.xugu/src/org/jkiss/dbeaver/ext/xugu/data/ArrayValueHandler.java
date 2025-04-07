/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.xugu.data;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.StringTokenizer;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.xugu.Constants;
import org.jkiss.dbeaver.ext.xugu.Utils;
import org.jkiss.dbeaver.ext.xugu.model.DataSource;
import org.jkiss.dbeaver.ext.xugu.model.DataType;
import org.jkiss.dbeaver.ext.xugu.model.DataTypeAttribute;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructImpl;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollectionString;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCompositeStatic;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.utils.CommonUtils;

public class ArrayValueHandler extends JDBCArrayValueHandler {
    private static final Log log = Log.getLog(ArrayValueHandler.class);
	public static final ArrayValueHandler INSTANCE = new ArrayValueHandler();

	// Copied from pgjdbc array parser class
    // https://github.com/pgjdbc/pgjdbc/blob/master/pgjdbc/src/main/java/org/postgresql/jdbc/PgArray.java
    public static List<Object> parseArrayString(String fieldString, String delimiter) throws DBCException {
        List<Object> arrayList = new ArrayList<>();
        if (CommonUtils.isEmpty(fieldString)) {
            return arrayList;
        }

        int dimensionsCount = 1;
        char delim = delimiter.charAt(0);//connection.getTypeInfo().getArrayDelimiter(oid);

        if (fieldString != null) {
            int bracePairsCount = 0;
            char[] chars = fieldString.toCharArray();
            StringBuilder buffer = null;
            boolean insideString = false;
            boolean wasInsideString = false; // needed for checking if NULL
            // value occurred
            List<List<Object>> dims = new ArrayList<>(); // array dimension arrays
            List<Object> curArray = arrayList; // currently processed array

            // Starting with 8.0 non-standard (beginning index
            // isn't 1) bounds the dimensions are returned in the
            // data formatted like so "[0:3]={0,1,2,3,4}".
            // Older versions simply do not return the bounds.
            //
            // Right now we ignore these bounds, but we could
            // consider allowing these index values to be used
            // even though the JDBC spec says 1 is the first
            // index. I'm not sure what a client would like
            // to see, so we just retain the old behavior.
            int startOffset = 0;
            {
                if (chars[0] == '[') {
                    while (chars[startOffset] != '=') {
                        startOffset++;
                    }
                    startOffset++; // skip =
                }
            }

            for (int i = startOffset; i < chars.length; i++) {

                // escape character that we need to skip
                if (chars[i] == '\\') {
                    i++;
                } else if (!insideString && chars[i] == '{') {
                    // subarray start
                    if (dims.isEmpty()) {
                        dims.add(arrayList);
                    } else {
                        List<Object> a = new ArrayList<>();
                        List<Object> p = dims.get(dims.size() - 1);
                        p.add(a);
                        dims.add(a);
                    }
                    bracePairsCount++;
                    curArray = dims.get(dims.size() - 1);

                    // number of dimensions
                    {
                        for (int t = i + 1; t < chars.length; t++) {
                            if (Character.isWhitespace(chars[t])) {
                                continue;
                            } else if (chars[t] == '{') {
                                dimensionsCount++;
                            } else {
                                break;
                            }
                        }
                    }

                    buffer = new StringBuilder();
                    continue;
                } else if (chars[i] == '"') {
                    // quoted element
                    insideString = !insideString;
                    wasInsideString = true;
                    continue;
                } else if (!insideString && Character.isWhitespace(chars[i])) {
                    // white space
                    continue;
                } else if ((!insideString && (chars[i] == delim || chars[i] == '}'))
                    || i == chars.length - 1) {
                    // array end or element end
                    // when character that is a part of array element
                    if (chars[i] != '"' && chars[i] != '}' && chars[i] != delim && buffer != null) {
                        buffer.append(chars[i]);
                    }

                    String b = buffer == null ? null : buffer.toString();

                    // add element to current array
                    if (b != null && (!b.isEmpty() || wasInsideString)) {
                        curArray.add(!wasInsideString && b.equals("NULL") ? null : b);
                    }

                    wasInsideString = false;
                    buffer = new StringBuilder();

                    // when end of an array
                    if (chars[i] == '}') {
                        if (dims.isEmpty()) {
                            throw new DBCException("Redundant trailing bracket in " + fieldString);
                        }
                        dims.remove(dims.size() - 1);
                        bracePairsCount--;

                        // when multi-dimension
                        if (!dims.isEmpty()) {
                            curArray = dims.get(dims.size() - 1);
                        }

                        buffer = null;
                    }

                    continue;
                }

                if (buffer != null) {
                    buffer.append(chars[i]);
                }
            }
            if (bracePairsCount != 0) {
                throw new DBCException("Amount of array's braces is not equal");
            }
        }
        return arrayList;
    }
    
    private static Object startTransformListOfValuesIntoArray(
            DBCSession session,
            DataType itemType,
            List<?> list) throws DBException
        {
            //If array is one dimensional, we will return array of that type. If array is multidimensional we will return array of JDBCCollections.
            return transformListOfValuesIntoArray(session, itemType, list, true);
        }

    private static Object transformListOfValuesIntoStruct(
            DBCSession session,
            DataType itemType,
            List<?> list)
            throws DBException
        { //transform into struct
            List<Object> itemValues = new ArrayList<>();

            if (list.size() == 1 && list.get(0) instanceof List) {
                // Structs are represented as an array with one element
                list = (List<?>) list.get(0);
            }
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof String) {
                    itemValues.add(convertStringToValue(session, itemType, (String) item));
                } else if (item instanceof List) {
                    // Structs are represented as an array with one element
                    if (((List<?>) item).size() == 1) {
                        Object subItem = ((List<?>) item).get(0);
                        if (subItem instanceof String) {
                            itemValues.add(convertStringToValue(session, itemType, (String) subItem));
                        } else {
                            log.debug("Invalid sub item type: " + subItem.getClass().getName());
                        }
                    } else {
                        log.debug("Invalid struct list size: " + ((List<?>) item).size());
                    }
                } else {
                    log.debug("Invalid struct item type: " + item);
                }
            }
            Struct contents = new JDBCStructImpl(itemType.getTypeName(), itemValues.toArray(), list.toString());
            return new JDBCCompositeStatic(session, itemType, contents);
        }
    
    private static Object transformListOfValuesIntoArray(
        DBCSession session,
        DataType itemType,
        List<?> list,
        boolean firstAttempt)
        throws DBException
    { //transform into array
        Object[] values = new Object[list.size()];
        for (int index = 0; index < list.size(); index++) {
            Object item = list.get(index);
            if (item instanceof List) {
                Object parsedValue;
                if (itemType.getDataKind() == DBPDataKind.STRUCT) {
                    parsedValue = transformListOfValuesIntoStruct(session, itemType, (List<?>) item);
                } else {
                    parsedValue = transformListOfValuesIntoArray(session, itemType, (List<?>) item, false);
                }
                values[index] = parsedValue;
            } else {
                Object[] itemValues = new Object[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    itemValues[i] = convertStringToValue(session, itemType, (String) list.get(i));
                }
                if (firstAttempt){
                    return itemValues;
                } else {
                    return new JDBCCollection(
                        session.getProgressMonitor(),
                        itemType,
                        DBUtils.findValueHandler(session, itemType),
                        itemValues);
                }
            }
        }
        if (firstAttempt) {
            return values;
        } else {
            return new JDBCCollection(session.getProgressMonitor(), itemType, DBUtils.findValueHandler(session, itemType), values);
        }
    }

    private static Object prepareToParseArray(DBCSession session, DBSTypedObject arrayType, String string) throws DBCException {
        DBSDataType arrayDataType = arrayType instanceof DBSDataType ? (DBSDataType) arrayType : ((DBSTypedObjectEx) arrayType).getDataType();
        try {
            if (arrayDataType == null) {
                log.error("Can't get array type '" + arrayType.getFullTypeName() + "'");
                return string;
            }
            DBSDataType componentType = arrayDataType.getComponentType(session.getProgressMonitor());
            if (componentType == null) {
                log.error("Can't get component type from array '" + arrayType.getFullTypeName() + "'");
                return string;
            } else {
                if (componentType instanceof DataType) {
                    List<Object> itemStrings = parseArrayString(string, ",");
                    return startTransformListOfValuesIntoArray(session, (DataType) componentType, itemStrings);
                } else {
                    log.error("Incorrect type '" + arrayType.getFullTypeName() + "'");
                    return string;
                }
            }
        } catch (Exception e) {
            if (e instanceof DBCException) {
                throw (DBCException) e;
            }
            throw new DBCException("Error parsing array '" + arrayType.getFullTypeName() + "' items", e);
        }
    }
	
    public static Object convertStringToValue(DBCSession session, DBSTypedObject arrayType, String string) throws DBCException {
        if (arrayType.getDataKind() == DBPDataKind.ARRAY) {
            if (string != null && string.startsWith("{") && string.endsWith("}")) {
                try {
                    return prepareToParseArray(session, arrayType, string);
                } catch (Exception e) {
                    log.error("Array parsing failed " + e.getMessage());
                    return string;
                }
            } else {
                //log.error("Unsupported array string: '" + string + "'");
                // It can be already a string object as an element of parsed array
                return string;
            }
        }
        if (CommonUtils.isEmpty(string)) {
            return convertStringToSimpleValue(session, arrayType, string);
        }
        try {
            switch (arrayType.getTypeID()) {
                case Types.BOOLEAN:
                    return string.length() > 0 && Character.toLowerCase(string.charAt(0)) == 't'; //todo: add support of alternatives to "true/false"
                case Types.TINYINT:
                    return Byte.parseByte(string);
                case Types.SMALLINT:
                    return Short.parseShort(string);
                case Types.INTEGER:
                    return Integer.parseInt(string);
                case Types.BIGINT:
                    return Long.parseLong(string);
                case Types.FLOAT:
                    return Float.parseFloat(string);
                case Types.REAL:
                case Types.NUMERIC:
                case Types.DOUBLE:
                    return Double.parseDouble(string);
                default: {
                    return convertStringToSimpleValue(session, arrayType, string);
                }
            }
        } catch (NumberFormatException e) {
            return string;
        }
    }
    
    private static Object convertStringToSimpleValue(DBCSession session, DBSTypedObject itemType, String string) throws DBCException {
        DBDValueHandler valueHandler = DBUtils.findValueHandler(session, itemType);
        if (valueHandler != null) {
            return valueHandler.getValueFromObject(session, itemType, string, false, false);
        } else {
            return string;
        }
    }

    private JDBCCollection convertStringArrayToCollection(@NotNull DBCSession session, @NotNull DataType arrayType, @NotNull DataType itemType, @NotNull String strValue) throws DBCException {
        Object parsedArray = convertStringToValue(session, arrayType, strValue);
        if (parsedArray instanceof Object[]){
            return new JDBCCollection(session.getProgressMonitor(), itemType, DBUtils.findValueHandler(session, itemType), (Object[]) parsedArray);
        } else {
            log.error("Can't parse array");
            return new JDBCCollection(session.getProgressMonitor(), itemType, DBUtils.findValueHandler(session, itemType), new Object[]{parsedArray});
        }
    }
    
    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException {
        if (object != null) {
            final DataType arrayType = Utils.findDataType(session, (DataSource) session.getDataSource(), type);
            if (arrayType == null) {
                throw new DBCException("Can't resolve data type " + type.getFullTypeName());
            }

            DataType itemType = arrayType.getElementType(session.getProgressMonitor());
            if (itemType == null) {
                throw new DBCException("Array type " + arrayType.getFullTypeName() + " doesn't have a component type");
            }
            
            String className = object.getClass().getName();
            if (object instanceof String || className.equals(Constants.XG_ARRAY_CLASS))
            {
                if (className.equals(Constants.XG_ARRAY_CLASS)) {
                    // Convert arrays to string representation (#7468)
                    // Otherwise we may have problems with domain types decoding (as they come in form of PgObject)
                    String strValue = object.toString();
                    return convertStringArrayToCollection(session, arrayType, itemType, strValue);
                } else {
                    return convertStringArrayToCollection(session, arrayType, itemType, (String) object);
                }
            } else if (object instanceof Object[]) {
                return new JDBCCollection(
                    session.getProgressMonitor(),
                    itemType,
                    DBUtils.findValueHandler(session, itemType),
                    (Object[]) object
                );
            }
        }
        return super.getValueFromObject(session, type, object, copy, validateValue);
    }
	
	@Override
	protected void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType,
			int paramIndex, Object value) throws DBCException, SQLException {
        if (value == null) {
            statement.setNull(paramIndex, Types.ARRAY);
        } else if (value instanceof DBDCollection) {
            DBDCollection collection = (DBDCollection) value;
            if (collection.isNull()) {
                statement.setNull(paramIndex, Types.ARRAY);
            } else if (collection instanceof JDBCCollection) {
                final Array arrayValue = ((JDBCCollection) collection).getArrayValue();
                if (useSetArray(session, paramType)) {
                    statement.setArray(paramIndex, arrayValue);
                } else {
                    statement.setObject(paramIndex, arrayValue, Types.ARRAY);
                }
            } else {
                final Object arrayValue = collection.getRawValue();
                if (useSetArray(session, paramType) && arrayValue instanceof Array) {
                    statement.setArray(paramIndex, (Array) arrayValue);
                } else {
                    statement.setObject(paramIndex, arrayValue);
                }
            }
        } else {
            throw new DBCException("Array parameter type '" + value.getClass().getName() + "' not supported");
        }
	}

	@Override
	public String getValueDisplayString(DBSTypedObject column, Object value, DBDDisplayFormat format) {
        if (!DBUtils.isNullValue(value) && value instanceof DBDCollection) {
            final DBDCollection collection = (DBDCollection) value;
            final StringJoiner output = new StringJoiner(",", "{", "}");

            for (int i = 0; i < collection.getItemCount(); i++) {
                final Object item = collection.getItem(i);
                final String member;

                if (item instanceof DBDCollection) {
                    member = getArrayMemberDisplayString(column, this, item, format);
                } else {
                    final DataType componentType = (DataType) collection.getComponentType();
                    final DBDValueHandler componentHandler = collection.getComponentValueHandler();
                    member = getArrayMemberDisplayString(componentType, componentHandler, item, format);
                }

                output.add(member);
            }

            return output.toString();
        }

        return super.getValueDisplayString(column, value, format);
	}

    @NotNull
    private static String getArrayMemberDisplayString(
        @NotNull DBSTypedObject type,
        @NotNull DBDValueHandler handler,
        @Nullable Object value,
        @NotNull DBDDisplayFormat format
    ) {
        if (DBUtils.isNullValue(value)) {
            return SQLConstants.NULL_VALUE;
        }

        final String string = handler.getValueDisplayString(type, value, format);

        if (isQuotingRequired(type, string)) {
            return '"' + string.replaceAll("[\\\\\"]", "\\\\$0") + '"';
        }

        return string;
    }

    private static boolean isQuotingRequired(@NotNull DBSTypedObject type, @NotNull String value) {
        switch (type.getDataKind()) {
            case ARRAY:
            case NUMERIC:
                return false;
            default:
                break;
        }

        if (value.isEmpty() || value.equalsIgnoreCase(SQLConstants.NULL_VALUE)) {
            return true;
        }

        for (int index = 0; index < value.length(); index++) {
            switch (value.charAt(index)) {
                case '{':
                case '}':
                case '"':
                case ' ':
                case '\\':
                    return true;
                default:
                    break;
            }
        }

        return false;
    }
}
