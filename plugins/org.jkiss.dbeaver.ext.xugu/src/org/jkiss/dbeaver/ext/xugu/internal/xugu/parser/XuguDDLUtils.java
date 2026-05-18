/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.xugu.internal.xugu.parser;

import com.alibaba.druid.util.StringUtils;
import org.jkiss.dbeaver.ext.xugu.internal.xugu.metadata.Constants;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author jiangnan
 * @date 2025/12/04
 * @description XuguDDLUtils
 */
public class XuguDDLUtils {
    public static final String[] XUGU_QUOTE_STRINGS = {"`", "`"};
    public static final String keyWordsSql = "SELECT keyword from ALL_KEYWORDS";
    public static final Map<String, List<String>> concurrentHashMap= new ConcurrentHashMap<>();
    public static final String MAP_KEY = "keyWords";
    public static final String MARK_DOT = "\\.";
    public static final String NOT_ESCAPED_MARK_DOT = ".";
    public static final String MARK_COMMA = ",";

    public static void initKeyWords(Connection conn) {
        concurrentHashMap.clear();
//      不做存在检查  if (concurrentHashMap.containsKey(MAP_KEY)) return;

        Integer dbKernelVersion = 0;
        // 考虑V11 情况不存在 ALL_KEYWORDS 系统表
        try (ResultSet resultSet = conn.createStatement().executeQuery("SHOW VERSION;");){
            while (resultSet.next()) {
                String dbKernel = resultSet.getString(1);
                String[] split = dbKernel.split(" ");
                dbKernelVersion = Integer.parseInt(split[split.length-1].split("\\.")[0]);
            }
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
        if (dbKernelVersion < 12) {
            initKeyWordsV11(conn);
        }else {
            initKeyWordsV12(conn);

        }

    }
    private static void initKeyWordsV12(Connection conn) {
        // 正常V12 情况
        try(Statement statement = conn.createStatement()) {
            ResultSet resultSet = statement.executeQuery(keyWordsSql);
            List<String> keyWords = new  ArrayList<>();
            while (resultSet.next()) {
                String keyword = resultSet.getString(1);
                keyWords.add( keyword );
            }
            concurrentHashMap.put(MAP_KEY, keyWords);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    //V11 手动维护
    private static void initKeyWordsV11(Connection conn) {
        List<String> keywordsSet = Arrays.stream(Constants.KEYWORDS.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        concurrentHashMap.put(MAP_KEY, keywordsSet);
    }

    /**
     * TABLE -> `TABLE`
     * @param keyword
     * @return
     */
    public static String isKeyWordAddQuote(String keyword) {
        if (!concurrentHashMap.containsKey(MAP_KEY)) {
            return keyword;
        }
        List<String> keyWords = concurrentHashMap.get(MAP_KEY);
        // TABLE,TABLE1,TABLE2 -> `TABLE`,`TABLE1`,`TABLE2`
        if (!StringUtils.isEmpty( keyword) && keyword.contains(MARK_COMMA)){
            List<String> split = Arrays.stream(keyword.split(MARK_COMMA)).toList();
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 1; i <= split.size(); i++) {
                stringBuffer.append(isKeyWordAddQuote(split.get(i-1)));
                if (i != split.size()){
                    stringBuffer.append(MARK_COMMA);
                }
            }
            return stringBuffer.toString();
        }
        if (!StringUtils.isEmpty(keyword)&&(keyWords.contains(keyword.toUpperCase()) || keyword.contains(keyword.toLowerCase()))){
            keyword = XUGU_QUOTE_STRINGS[0] + keyword + XUGU_QUOTE_STRINGS[1];
        }
        return keyword;
    }

    /**
     * 添加schema和table的引号  SYSDBA.TESTTABLE -> SYSDBA.`TESTTABLE`
     * @param keyword
     * @return
     */
    public static String schemaAndTable(String keyword) {
        if (!concurrentHashMap.containsKey(MAP_KEY)) {
            return keyword;
        }
        try {
            String[] split = keyword.split(MARK_DOT);
            if (split.length == 2) {
                String schema = split[0];
                String table = split[1];
                table = isKeyWordAddQuote(table);
                return schema + NOT_ESCAPED_MARK_DOT + table;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return keyword;

    }


}
