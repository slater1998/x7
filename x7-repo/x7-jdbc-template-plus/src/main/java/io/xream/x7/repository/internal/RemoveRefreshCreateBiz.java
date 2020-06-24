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
package io.xream.x7.repository.internal;

import io.xream.x7.common.bean.Parsed;
import io.xream.x7.common.bean.Parser;
import io.xream.x7.common.bean.condition.RemoveRefreshCreate;
import io.xream.x7.common.repository.X;
import io.xream.x7.common.util.ExceptionUtil;
import io.xream.x7.common.util.JsonX;
import io.xream.x7.common.util.StringUtil;
import io.xream.x7.repository.KeyOne;
import io.xream.x7.repository.Repository;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.*;

public final class RemoveRefreshCreateBiz {

    protected static <T> boolean doIt(Class<T> clz, Repository repository, RemoveRefreshCreate wrapper) {

        Assert.notNull(wrapper, "removeRefreshCreate(wrapper),wrapper is null");

        if (wrapper.getList() == null || wrapper.getList().isEmpty())
            return false;
        List<T> list = null;
        Object obj = wrapper.getList().get(0);
        if (obj.getClass() != clz){
            list = new ArrayList<>();
            for (Object o : wrapper.getList()){
                T t = JsonX.toObject(o,clz);
                list.add(t);
            }
        }else{
            list = wrapper.getList();
        }

        Object[] ins = wrapper.getIns();

        List<String> inList = new ArrayList<>();
        if (ins != null ) {
            for (Object in : ins){
                if (in == null)
                    continue;
                String strId = String.valueOf(in);
                if (StringUtil.isNullOrEmpty(strId))
                    continue;
                inList.add(strId);
            }
        }

        final Parsed parsed = Parser.get(clz);
        Field f = parsed.getKeyField(X.KEY_ONE);
        Map<String,T> map = new HashMap<>();
        try {
            for (T t : list) {
                Object id = f.get(t);
                map.put(String.valueOf(id), t);
            }
        }catch (Exception e){
            throw new RuntimeException(ExceptionUtil.getMessage(e));
        }


        /*
         * remove
         */
        Iterator<String> existIte = inList.iterator();
        while (existIte.hasNext()) {
            Object id = existIte.next();
            if (map.containsKey(id))
                continue;
            existIte.remove();
            repository.remove(new KeyOne<Object>() {
                @Override
                public Object get() {
                    return id;
                }

                @Override
                public Class<Object> getClzz() {
                    return parsed.getClz();
                }
            });
        }

        /*
         * refreshOrCreate
         */
        for (Map.Entry<String,T> entry : map.entrySet()) {
            String id = entry.getKey();
            T t = entry.getValue();
            if (inList.contains(id)) {
                repository.refresh(t);
            }else {
                repository.create(t);
            }

        }

        return true;
    }
}
