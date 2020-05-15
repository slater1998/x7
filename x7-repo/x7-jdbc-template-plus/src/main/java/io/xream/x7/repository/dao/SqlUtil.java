/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.xream.x7.repository.dao;

import io.xream.x7.common.bean.*;
import io.xream.x7.common.bean.condition.RefreshCondition;
import io.xream.x7.common.repository.X;
import io.xream.x7.common.util.BeanUtil;
import io.xream.x7.common.util.ExceptionUtil;
import io.xream.x7.common.util.StringUtil;
import io.xream.x7.repository.CriteriaParser;
import io.xream.x7.repository.DbType;
import io.xream.x7.repository.SqlParsed;
import io.xream.x7.repository.exception.PersistenceException;
import io.xream.x7.repository.mapper.DataObjectConverter;
import io.xream.x7.repository.mapper.Dialect;
import io.xream.x7.repository.util.SqlParserUtil;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SqlUtil {

    protected static void adpterSqlKey(PreparedStatement pstmt, String keyOne, Object obj, int i) {
        /*
         * 处理KEY
         */
        Method method = null;
        try {
            method = obj.getClass().getDeclaredMethod(BeanUtil.getGetter(keyOne));
        } catch (NoSuchMethodException e) {
            try {
                method = obj.getClass().getSuperclass().getDeclaredMethod(BeanUtil.getGetter(keyOne));
            } catch (Exception ee) {
                throw new RuntimeException(ExceptionUtil.getMessage(ee));
            }
        }
        try {
            Object value = method.invoke(obj);
            pstmt.setObject(i++, value);
        } catch (Exception e) {
            throw new PersistenceException(ExceptionUtil.getMessage(e));
        }
    }

    protected static String paged(String sql, int page, int rows, Dialect dialect) {
        int start = (page - 1) * rows;
        return dialect.match(sql, start, rows);
    }

    /**
     * 拼接SQL
     */
    protected static String concat(Parsed parsed, String sql, Map<String, Object> queryMap) {

        StringBuilder sb = new StringBuilder();

        boolean flag = (sql.contains(SqlScript.WHERE) || sql.contains(SqlScript.WHERE.toLowerCase()));

        for (String key : queryMap.keySet()) {

            String mapper = parsed.getMapper(key);
            if (flag) {
                sb.append(ConjunctionAndOtherScript.AND.sql()).append(mapper).append(SqlScript.EQ_PLACE_HOLDER);
            } else {
                sb.append(SqlScript.WHERE).append(mapper).append(SqlScript.EQ_PLACE_HOLDER);
                flag = true;
            }

        }

        sql += sb.toString();

        return sql;
    }


    protected static String buildRefresh(Parsed parsed, RefreshCondition refreshCondition, CriteriaParser criteriaParser) {

        return criteriaParser.parseRefresh(parsed,refreshCondition);
    }

    protected static String concatRefresh(StringBuilder sb, Parsed parsed, Map<String, Object> refreshMap) {

        sb.append(SqlScript.SET);
        int size = refreshMap.size();
        int i = 0;
        for (String key : refreshMap.keySet()) {

            BeanElement element = parsed.getElement(key);
            if (element.isJson && DbType.ORACLE.equals(DbType.value)){
                Object obj = refreshMap.get(key);
                Reader reader = new StringReader(obj.toString());
                refreshMap.put(key,reader);
            }

            String mapper = parsed.getMapper(key);
            sb.append(mapper);
            sb.append(SqlScript.EQ_PLACE_HOLDER);
            if (i < size - 1) {
                sb.append(SqlScript.COMMA);
            }
            i++;
        }

        String keyOne = parsed.getKey(X.KEY_ONE);

        sb.append(SqlScript.WHERE);
        String mapper = parsed.getMapper(keyOne);
        sb.append(mapper).append(SqlScript.EQ_PLACE_HOLDER);

        return sb.toString();
    }




    protected static String buildIn(String sql, String mapper, BeanElement be, List<? extends Object> inList) {

        StringBuilder sb = new StringBuilder();
        sb.append(sql).append(SqlScript.WHERE);
        sb.append(mapper).append(SqlScript.IN);//" IN "

        Class<?> keyType = be.getMethod.getReturnType();

        buildIn(sb,keyType,inList);

        return sb.toString();
    }

    protected static void buildIn(StringBuilder sb, Class clz,List<? extends Object> inList ){

        sb.append(SqlScript.LEFT_PARENTTHESIS).append(SqlScript.SPACE);//"( "

        int length = inList.size();
        if (clz == String.class) {

            for (int j = 0; j < length; j++) {
                Object value = inList.get(j);
                if (value == null || StringUtil.isNullOrEmpty(value.toString()))
                    continue;
                value = SqlUtil.filter(value.toString());
                sb.append(SqlScript.SINGLE_QUOTES).append(value).append(SqlScript.SINGLE_QUOTES);//'string'
                if (j < length - 1) {
                    sb.append(SqlScript.COMMA);
                }
            }

        }else if (BeanUtil.isEnum(clz)) {
            for (int j = 0; j < length; j++) {
                Object value = inList.get(j);
                if (value == null )
                    continue;
                String ev = ((Enum) value).name();
                sb.append(SqlScript.SINGLE_QUOTES).append(ev).append(SqlScript.SINGLE_QUOTES);//'string'
                if (j < length - 1) {
                    sb.append(SqlScript.COMMA);
                }
            }
        }else {
            for (int j = 0; j < length; j++) {
                Object value = inList.get(j);
                if (value == null)
                    continue;
                sb.append(value);
                if (j < length - 1) {
                    sb.append(SqlScript.COMMA);
                }
            }
        }

        sb.append(SqlScript.SPACE).append(SqlScript.RIGHT_PARENTTHESIS);
    }

    protected static SqlParsed fromCriteria(Criteria criteria, CriteriaParser criteriaParser, Dialect dialect) {
        SqlParsed sqlParsed = criteriaParser.parse(criteria);
        String sql = sqlParsed.getSql().toString();

        int page = criteria.getPage();
        int rows = criteria.getRows();

        int start = (page - 1) * rows;

        sql = dialect.match(sql, start, rows);

        StringBuilder sb = new StringBuilder();
        sb.append(sql);
        sqlParsed.setSql(sb);
        DataObjectConverter.log(criteria.getClz(), criteria.getValueList());

        return sqlParsed;
    }

    protected static String filter(String sql) {
        sql = sql.replace("drop", SqlScript.SPACE)
                .replace(";", SqlScript.SPACE);// 手动拼接SQL,
        return sql;
    }


    public static <T> Object[] refresh(T t, Class<T> clz) {

        Parsed parsed = Parser.get(clz);
        String tableName = parsed.getTableName();

        StringBuilder sb = new StringBuilder();
        sb.append(SqlScript.UPDATE).append(SqlScript.SPACE).append(tableName).append(SqlScript.SPACE);

        Map<String, Object> refreshMap = SqlParserUtil.getRefreshMap(parsed, t);

        String keyOne = parsed.getKey(X.KEY_ONE);
        Object keyOneValue = refreshMap.remove(keyOne);

        String sql = concatRefresh(sb, parsed, refreshMap);

        List<Object> valueList = new ArrayList<>();
        valueList.addAll(refreshMap.values());
        valueList.add(keyOneValue);

        return new Object[]{sql,valueList};
    }
}
