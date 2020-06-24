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
package io.xream.x7.common.filter;

import io.xream.x7.common.bean.*;

public class BaseTypeFilter {

    public static boolean beanWithoutBaseType = false;

    public static boolean isBaseType_0(String key, Object v, CriteriaCondition criteria) {
        if (beanWithoutBaseType)
            return false;
        String[] arr = key.split("\\.");
        String alia = arr[0];
        String clzName = criteria.getAliaMap().get(alia);
        if (clzName == null)
            clzName = alia;
        Parsed parsed = Parser.get(clzName);

        return BaseTypeFilter.isBaseType_0(arr[1],v,parsed);
    }

    public static boolean isBaseType_0(String property, Object v, Parsed parsed) {

        if (beanWithoutBaseType)
            return false;

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

    private static BeanElement getBeanElement(String property, Parsed parsed) {

        String str = null;
        if (property.contains(SqlScript.SPACE)) {
            String[] arr = property.split(SqlScript.SPACE);
            str = arr[0];
        } else {
            str = property;
        }
        if (str.contains(SqlScript.DOT)) {
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

}
