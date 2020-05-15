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
package io.xream.x7.common.util;

import io.xream.x7.common.bean.*;
import io.xream.x7.common.repository.SqlFieldType;
import io.xream.x7.common.repository.X;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;


public class BeanUtilX extends BeanUtil {

    public final static String SQL_KEYWORD_MARK = "`";

    private BeanUtilX() {
        super();
    }

    @SuppressWarnings("rawtypes")
    public static List<BeanElement> getElementList(Class clz) {

        List<Field> fl = new ArrayList<Field>();

        if (clz.getSuperclass() != Object.class) {
            fl.addAll(Arrays.asList(clz.getSuperclass().getDeclaredFields()));
        }
        fl.addAll(Arrays.asList(clz.getDeclaredFields()));

        /*
         * 排除transient
         */
        Map<String, Field> filterMap = new HashMap<String, Field>();
        Map<String, Field> allMap = new HashMap<String, Field>();
        for (Field f : fl) {
            allMap.put(f.getName(), f);

            if (f.getModifiers() >= 128) {
                filterMap.put(f.getName(), f);
            }

            /*
             * ignored anno
             */
            X.Ignore p = f.getAnnotation(X.Ignore.class);
            if (p != null) {
                filterMap.put(f.getName(), f);
            }
        }

        Set<String> mns = new HashSet<String>();
        List<Method> ml = new ArrayList<Method>();
        if (clz.getSuperclass() != Object.class) {
            ml.addAll(Arrays.asList(clz.getSuperclass().getDeclaredMethods()));
        }
        ml.addAll(Arrays.asList(clz.getDeclaredMethods())); // 仅仅XxxMapped子类

        for (Method m : ml) {
            mns.add(m.getName());
        }

        List<BeanElement> filterList = new ArrayList<BeanElement>();
        for (Method m : ml) {
            String name = m.getName();
            if (!(name.startsWith("set") || name.startsWith("get") || name.startsWith("is")))
                continue;

            String key = getProperty(name);
            BeanElement be = null;
            for (BeanElement b : filterList) {
                if (b.getProperty().equals(key)) {
                    be = b;
                    break;
                }
            }
            if (be == null) {
                be = new BeanElement();
                be.setProperty(key);
                filterList.add(be);
            }
            if (name.startsWith("set")) {
                be.setter = name;
            } else if (name.startsWith("get")) {
                be.getter = name;
                be.clz = m.getReturnType();
            } else if (name.startsWith("is")) {
                be.getter = name;
                be.clz = m.getReturnType();
                be.setProperty(name);
                String setter = getSetter(name); // FIXME 可能有BUG
                if (mns.contains(setter)) {
                    be.setter = setter;
                }
            }

        }

        /*
         * 找出有setter 和 getter的一对
         */
        Iterator<BeanElement> ite = filterList.iterator();
        while (ite.hasNext()) {// BUG, 这里去掉了boolen属性
            BeanElement be = ite.next();
            if (!be.isPair()) {
                ite.remove();
            }
        }

        /*
         * 去掉transient
         */
        for (String key : filterMap.keySet()) {
            Iterator<BeanElement> beIte = filterList.iterator();
            while (beIte.hasNext()) {
                BeanElement be = beIte.next();
                if (be.getProperty().equals(key)) {
                    beIte.remove();
                    break;
                }
            }
        }

        List<BeanElement> list = new ArrayList<BeanElement>();

        for (BeanElement element : filterList) {

            parseAnno(clz, element, allMap.get(element.getProperty()));

            Class ec = element.clz;
            if (element.sqlType == null) {
                if (ec == int.class || ec == Integer.class) {
                    element.sqlType = SqlFieldType.INT;
                    element.length = 11;
                } else if (ec == long.class || ec == Long.class) {
                    element.sqlType = SqlFieldType.LONG;
                    element.length = 13;
                } else if (ec == double.class || ec == Double.class) {
                    element.sqlType = SqlFieldType.DOUBLE;
                    element.length = 13;
                } else if (ec == float.class || ec == Float.class) {
                    element.sqlType = SqlFieldType.FLOAT;
                    element.length = 13;
                } else if (ec == boolean.class || ec == Boolean.class) {
                    element.sqlType = SqlFieldType.BYTE;
                    element.length = 1;
                } else if (ec == Date.class || ec == java.sql.Date.class || ec == Timestamp.class) {
                    element.sqlType = SqlFieldType.DATE;
                } else if (ec == String.class) {
                    element.sqlType = SqlFieldType.VARCHAR;
                    if (element.length == 0)
                        element.length = 60;
                } else if (ec == BigDecimal.class) {
                    element.sqlType = SqlFieldType.DECIMAL;
                } else if (BeanUtil.isEnum(ec)) {
                    element.sqlType = SqlFieldType.VARCHAR;
                    if (element.length == 0)
                        element.length = 40;
                } else {
                    element.isJson = true;
                    if (ec == List.class) {
                        Field field = null;
                        try {
                            field = clz.getDeclaredField(element.getProperty());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        ParameterizedType pt = (ParameterizedType) field.getGenericType();

                        Class geneType = (Class) pt.getActualTypeArguments()[0];
                        element.geneType = geneType;
                    }
                    element.sqlType = SqlFieldType.VARCHAR;
                    if (element.length == 0)
                        element.length = 512;
                }
            } else if (element.sqlType.contains(SqlFieldType.TEXT)) {
                element.length = 0;
            } else {
                element.sqlType = SqlFieldType.VARCHAR;
            }

            list.add(element);
        }

        try {
            for (BeanElement be : list) {
                try {
                    be.setMethod = clz.getDeclaredMethod(be.setter, be.clz);
                } catch (NoSuchMethodException e) {
                    be.setMethod = clz.getSuperclass().getDeclaredMethod(be.setter, be.clz);
                }
                try {
                    be.getMethod = clz.getDeclaredMethod(be.getter);
                } catch (NoSuchMethodException e) {
                    be.getMethod = clz.getSuperclass().getDeclaredMethod(be.getter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void parseCacheableAnno(Class clz, Parsed parsed) {
        X.NoCache p = (X.NoCache) clz.getAnnotation(X.NoCache.class);
        if (p != null) {
            parsed.setNoCache(true);
        }
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void parseAnno(Class clz, BeanElement ele, Field f) {

        Method m = null;
        try {
            m = clz.getDeclaredMethod(ele.getter);
        } catch (NoSuchMethodException e) {

        }
        if (m != null) {
            X p = m.getAnnotation(X.class);
            if (p != null) {
                ele.sqlType = p.type();
                ele.length = p.length();
            }
        }

        if (f != null) {
            X p = f.getAnnotation(X.class);
            if (p != null) {
                ele.sqlType = p.type();
                ele.length = p.length();
            }

            X.Mapping mapping = (X.Mapping) f.getAnnotation(X.Mapping.class);
            if (mapping != null) {
                if (StringUtil.isNotNull(mapping.value()))
                    ele.mapper = mapping.value();
            }

        }

    }

    @SuppressWarnings({"rawtypes"})
    public static void parseKey(Parsed parsed, Class clz) {

        Map<Integer, String> map = parsed.getKeyMap();
        Map<Integer, Field> keyFieldMap = parsed.getKeyFieldMap();
        List<Field> list = new ArrayList<Field>();

        try {

            list.addAll(Arrays.asList(clz.getDeclaredFields()));
            Class sc = clz.getSuperclass();
            if (sc != Object.class) {
                list.addAll(Arrays.asList(sc.getDeclaredFields()));
            }
        } catch (Exception e) {

        }

        for (Field f : list) {
            X.Key a = f.getAnnotation(X.Key.class);
            if (a != null) {
                map.put(X.KEY_ONE, f.getName());
                f.setAccessible(true);
                keyFieldMap.put(X.KEY_ONE, f);
            }

        }
    }


    public static <T> void sort(Class<T> clz, List<T> list, String property, boolean isAsc) {
        list.sort(
                (a, b) -> compare(clz, property, isAsc, a, b)
        );
    }

    private static <T> int compare(Class clz, String orderBy, boolean isAsc, T a, T b) {
        try {
            int scValue = isAsc ? 1 : -1;
            Field field = clz.getDeclaredField(orderBy);
            field.setAccessible(true);
            Object valueA = field.get(a);
            Object valueB = field.get(b);
            if (field.getType() == String.class) {
                int intA = valueA.toString().charAt(0);
                int intB = valueB.toString().charAt(0);
                if (intA > intB)
                    return 1 * scValue;
                if (intA < intB)
                    return -1 * scValue;
                return 0;
            } else {
                BigDecimal bdA = new BigDecimal(valueA.toString().toCharArray());
                BigDecimal bdB = new BigDecimal(valueB.toString().toCharArray());
                return bdA.compareTo(bdB) * scValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void sort(List<Map<String, Object>> list, String property, boolean isAsc) {
        list.sort(
                (a, b) -> compare(property, isAsc, a, b)
        );
    }

    private static int compare(String property, boolean isAsc, Map<String, Object> a, Map<String, Object> b) {

        int scValue = isAsc ? 1 : -1;

        Object valueA = a.get(property);
        Object valueB = b.get(property);

        if (valueA instanceof String) {
            int intA = valueA.toString().charAt(0);
            int intB = valueB.toString().charAt(0);
            if (intA > intB)
                return 1 * scValue;
            if (intA < intB)
                return -1 * scValue;
            return 0;
        } else {
            BigDecimal bdA = new BigDecimal(valueA.toString().toCharArray());
            BigDecimal bdB = new BigDecimal(valueB.toString().toCharArray());
            return bdA.compareTo(bdB) * scValue;
        }

    }


    public static String filterSQLKeyword(String mapper) {
        for (String keyWord : SqlScript.KEYWORDS) {
            if (keyWord.equals(mapper.toLowerCase())) {
                return SQL_KEYWORD_MARK + mapper + SQL_KEYWORD_MARK;
            }
        }
        return mapper;
    }


    private static Set<String> opSet = new HashSet() {
        {
            add("=");
            add("!");
            add(">");
            add("<");
            add("+");
            add("-");
            add("*");
            add("/");
            add("(");
            add(")");
            add(";");
        }
    };

    public static String normalizeSql(final String manuSql) {
        StringBuilder valueSb = new StringBuilder();

        boolean ignore = false;
        int length = manuSql.length();
        for (int j = 0; j < length; j++) {
            String strEle = String.valueOf(manuSql.charAt(j));
            if (SqlScript.SPACE.equals(strEle)) {
                ignore = true;
                continue;
            }
            if (opSet.contains(strEle)) {

                valueSb.append(SqlScript.SPACE);

                valueSb.append(strEle);
                if (j + 1 < length) {
                    String nextOp = String.valueOf(manuSql.charAt(j + 1));
                    if (opSet.contains(nextOp)) {
                        valueSb.append(nextOp);
                        j++;
                    }
                }
                valueSb.append(SqlScript.SPACE);
            } else {
                if (ignore)
                    valueSb.append(SqlScript.SPACE);
                valueSb.append(strEle);
            }
            ignore = false;
        }

        return valueSb.toString();

    }


    public static String getClzName(String alia, CriteriaCondition criteria) {
        if (criteria.getAliaMap() != null) {
            String a = criteria.getAliaMap().get(alia);
            if (StringUtil.isNotNull(a))
                return a;
        }
        return alia;
    }

    public static void aliaToClzzForMapResult(Criteria.ResultMappedCriteria resultMapped, List<Map<String, Object>> mapList) {
        Map<String, String> aliaMap = resultMapped.getAliaMap();

        for (Map.Entry<String, String> entry : aliaMap.entrySet()) {
            if (entry.getKey().equals(entry.getValue()))
                return;
        }

        for (Map<String, Object> aliaKeyMap : mapList) {

            Map<String, Object> clzKeyMap = new HashMap<>();

            for (Map.Entry<String, Object> entry : aliaKeyMap.entrySet()) {
                String alia = entry.getKey();
                Object obj = entry.getValue();
                String clzName = aliaMap.get(alia);
                clzKeyMap.put(clzName, obj);
            }

            aliaKeyMap.clear();
            aliaKeyMap.putAll(clzKeyMap);
        }

    }

    private static BeanElement getBeanElement(String property, Parsed parsed) {

        String str = null;
        if (property.contains(SqlScript.SPACE)) {
            String[] arr = property.split(SqlScript.SPACE);
            str = arr[0];
        } else {
            str = property;
        }
        if (str.contains(SqlScript.POINT)) {
            String[] xxx = str.split("\\.");
            if (xxx.length == 1)
                property = xxx[0];
            else
                property = xxx[1];
        } else {
            property = str;
        }

        BeanElement be = parsed.getElement(property);

        return be;

    }


    public static boolean isBaseType_0(String property, Object v, Parsed parsed) {

        if (v instanceof String)
            return false;

        double d = 0;
        try {
            d = Double.valueOf(v.toString());
            if (d != 0) return false;
        } catch (Exception e) {
            return false;
        }

        BeanElement be = getBeanElement(property, parsed);

        if (be == null) {
            return false; //FIXME
        }

        Class<?> vType = be.clz;


        if (vType == int.class || vType == long.class || vType == float.class
                || vType == double.class
                || vType == short.class
                || vType == byte.class) {

            return true;
        }


        return false;
    }

    public static <T> Object tryToGetId(T t, Parsed parsed) {

        Field f = parsed.getKeyField(X.KEY_ONE);
        Object id = null;
        try {
            id = f.get(t);
        } catch (Exception e) {

        }
        if (id == null)
            throw new IllegalArgumentException("obj keyOne = " + id + ", " + t);
        return id;
    }

    public static String getCacheKey(Object obj, Parsed parsed) {
        try {
            Object keyOneObj = tryToGetId(obj, parsed);
            if (keyOneObj != null)
                return keyOneObj.toString();

        } catch (Exception e) {
        }
        return null;
    }

}
