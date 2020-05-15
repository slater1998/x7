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
package io.xream.x7.repository.dialect;

import io.xream.x7.common.bean.BeanElement;
import io.xream.x7.common.bean.Criteria;
import io.xream.x7.common.bean.SqlScript;
import io.xream.x7.common.util.BeanUtil;
import io.xream.x7.common.util.JsonX;
import io.xream.x7.repository.mapper.Dialect;

import java.math.BigDecimal;
import java.util.*;

public class MySqlDialect implements Dialect {

    private final Map<String, String> map = new HashMap<String, String>() {
        {

            put(DATE, "timestamp");
            put(BYTE, "tinyint(1)");
            put(INT, "int(11)");
            put(LONG, "bigint(13)");
            put(BIG, "decimal(15,2)");
            put(STRING, "varchar");
            put(TEXT, "text");
            put(LONG_TEXT, "longtext");
            put(INCREAMENT, "AUTO_INCREMENT");
            put(ENGINE, "ENGINE=InnoDB DEFAULT CHARSET=utf8");

        }

    };


    public String match(String sql, long start, long rows) {

        if (rows == 0)
            return sql;
        StringBuilder sb = new StringBuilder();
        sb.append(sql);
        sb.append(SqlScript.LIMIT).append(start).append(",").append(rows);
        return sb.toString();

    }

    public String match(String sql, String sqlType) {
        String dateV = map.get(DATE);
        String byteV = map.get(BYTE);
        String intV = map.get(INT);
        String longV = map.get(LONG);
        String bigV = map.get(BIG);
        String textV = map.get(TEXT);
        String longTextV = map.get(LONG_TEXT);
        String stringV = map.get(STRING);
        String increamentV = map.get(INCREAMENT);
        String engineV = map.get(ENGINE);

        return sql.replace(DATE.trim(), dateV).replace(BYTE.trim(), byteV).replace(INT.trim(), intV)
                .replace(LONG.trim(), longV).replace(BIG.trim(), bigV).replace(TEXT.trim(), textV)
                .replace(LONG_TEXT.trim(), longTextV).replace(STRING.trim(), stringV)
                .replace(INCREAMENT.trim(), increamentV).replace(ENGINE.trim(), engineV);
    }

    @Override
    public Object mappingToObject( Object obj, BeanElement element) {
        if (obj == null)
            return null;
        Class ec = element.clz;

        if (BeanUtil.isEnum(ec)) {
            return Enum.valueOf(ec, obj.toString());
        } else if (element.isJson) {
            if (ec == List.class) {
                Class geneType = element.geneType;
                return JsonX.toList(obj.toString(), geneType);
            } else if (ec == Map.class) {
                return JsonX.toMap(obj);
            } else {
                return JsonX.toObject(obj.toString(), ec);
            }
        } else if (ec == BigDecimal.class) {
            return new BigDecimal(String.valueOf(obj));
        } else if (ec == double.class || ec == Double.class) {
            return Double.valueOf(obj.toString());
        }

        return obj;
    }

    @Override
    public String createOrReplaceSql(String sql) {
        return sql.replaceFirst("INSERT","REPLACE");
    }

    @Override
    public String transformAlia(String mapper,Map<String, String> aliaMap,  Map<String, String> resultKeyAliaMap) {

        if (!resultKeyAliaMap.isEmpty()) {
            if (resultKeyAliaMap.containsKey(mapper)) {
                mapper = resultKeyAliaMap.get(mapper);
            }
        }
//        if (aliaMap.isEmpty())
//            return mapper;

//        if (mapper.contains(".")) {
//            String[] arr = mapper.split("\\.");
//            String alia = arr[0];
//            String p = arr[1];
//            String clzName = aliaMap.get(alia);
//            if (StringUtil.isNullOrEmpty(clzName)){
//                clzName = alia;
//            }
//            return clzName+"."+p;
//        }

        return mapper;

    }

    public Object filterValue(Object value) {

        if (value instanceof String) {
            String str = (String) value;
            value = str.replace("<", "&lt").replace(">", "&gt");
        }
        if (Objects.nonNull(value) && BeanUtil.isEnum(value.getClass()))
            return ((Enum)value).name();

        return value;
    }


    @Override
    public String resultKeyAlian(String mapper, Criteria.ResultMappedCriteria criteria) {

        if (mapper.contains(".") && (!mapper.contains(SqlScript.SPACE) || !mapper.contains(SqlScript.AS) )) {
            Map<String, String> resultKeyAliaMap = criteria.getResultKeyAliaMap();
            String alian = "c" + resultKeyAliaMap.size();
            resultKeyAliaMap.put(alian, mapper);
            String target = mapper + SqlScript.AS + alian;
            return target;
        }
        return mapper;
    }

    @Override
    public Object[] toArr(Collection<Object> list) {

        if (list == null || list.isEmpty())
            return null;
        int size = list.size();
        Object[] arr = new Object[size];
        int i =0;
        for (Object obj : list) {
            obj = filterValue(obj);
            arr[i++] = obj;
        }

        return arr;
    }


}
