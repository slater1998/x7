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
package io.xream.x7.repository.redis.cache;

import io.xream.x7.cache.L3CacheStorage;
import io.xream.x7.common.cache.Protection;
import io.xream.x7.common.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.concurrent.*;

@Component
public final class DefaultL3CacheStorage implements L3CacheStorage {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private L3CacheStorage fallbackStorage;

    @Value("${circuitbreaker.l3cache.name:l3cache}")
    private String circuitBreakerL3cacheName;

    @Override
    public void setFallbackStorage(L3CacheStorage fallbackStorage) {
        this.fallbackStorage = fallbackStorage;
    }

    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean lock(String key, @NotNull Integer timeOut) {
        if (timeOut.intValue() == 0)
            timeOut = 60000;
        try {
            return this.stringRedisTemplate.opsForValue().setIfAbsent(key, "1", timeOut, TimeUnit.MILLISECONDS);
        }catch (Exception e) {
            return true;
        }
    }

    @Override
    public void unLock(String key) {
        try {
            this.stringRedisTemplate.delete(key);
        }catch (Exception e){
        }
    }

    @Override
    public void set(String key, String value, long expireTime, TimeUnit timeUnit) {
        
        value = (value == null ? Protection.DEFAULT_VALUE : value);

        PluginL1.putBeforeL3(key,value,expireTime,timeUnit);

        this.stringRedisTemplate.opsForValue().set(key,
                value,
                expireTime,
                timeUnit);
    }

    @Override
    public String get(String key,long expireTime, TimeUnit timeUnit) {
        String value = PluginL1.get(key);
        if (StringUtil.isNullOrEmpty(value)){
            value = this.stringRedisTemplate.opsForValue().get(key);
            if (StringUtil.isNotNull(value)){
                PluginL1.putBeforeL3(key,value,expireTime,timeUnit);
            }
        }
        
        return value;
    }


    public static class PluginL1 {

        private final static Map<String, Wrapper> map = new ConcurrentHashMap<>();

        private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();


        private static long convertToMillisForL1(long expireTime, TimeUnit timeUnit) {
            long time = TimeUnit.MILLISECONDS.convert(expireTime, timeUnit);
            if (time <= 2000)
                return time/2;
            if (time <= 6000)
                return time/3;
            if (time <= 16000)
                return time/4;
            if (time <= 30000)
                return time/5;
            return 6000;
        }

        public static void putBeforeL3(String key, String value, long expireTime, TimeUnit timeUnit ){
            long expire = convertToMillisForL1(expireTime,timeUnit);

            put(key,value,expire);
        }

        public static void put(String key, String value, long expire) {

            remove(key);

            if (expire > 0) {
                Future future = executor.schedule(() -> {
                    map.remove(key);
                }, expire, TimeUnit.MILLISECONDS);
                map.put(key, new Wrapper(value, future));
            } else {
                map.put(key, new Wrapper(value, null));
            }
        }

        public static String get(String key) {
            Wrapper Wrapper = map.get(key);
            return Wrapper == null ? null : Wrapper.getValue();
        }
        

        public static String remove(String key) {

            Wrapper Wrapper = map.remove(key);
            if (Wrapper == null)
                return null;

            Future future = Wrapper.getFuture();
            if (future != null)
                future.cancel(true);
            return Wrapper.getValue();
        }


        private static class Wrapper {

            private String value;
            private Future future;

            public Wrapper(String value, Future future) {
                this.value = value;
                this.future = future;
            }

            public String getValue() {
                return value;
            }

            public Future getFuture() {
                return future;
            }

            @Override
            public String toString() {
                return "Wrapper{" +
                        "value=" + value +
                        ", future=" + future +
                        '}';
            }
        }

    }
}
