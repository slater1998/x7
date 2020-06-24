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
package io.xream.x7.common.support;

import java.sql.Timestamp;
import java.util.Date;

public class TimestampSupport {

    public static boolean testNumberValueToDate(Class clzz, io.xream.x7.common.bean.X x){
        if (clzz == Date.class) {
            Object v = x.getValue();
            if (v instanceof Long || v instanceof Integer) {
                if (Long.valueOf(v.toString()) == 0){
                    x.setValue(null);
                }else {
                    x.setValue(new Date(((Long)v).longValue()));
                }
            }
            return true;
        } else if (clzz == Timestamp.class) {
            Object v = x.getValue();
            if (v instanceof Long || v instanceof Integer) {
                if (Long.valueOf(v.toString()) == 0){
                    x.setValue(null);
                }else {
                    x.setValue(new Timestamp(((Long) v).longValue()));
                }
            }
            return true;
        }

        return false;
    }


}
