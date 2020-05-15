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
package io.xream.x7.reyc.internal;

import io.xream.x7.common.bean.KV;
import io.xream.x7.reyc.ReyClient;
import io.xream.x7.reyc.api.GroupRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientParser {

    private static Logger logger = LoggerFactory.getLogger(ClientParser.class);

    private final static Map<String, ClientParsed> map = new HashMap<>();

    public static ClientParsed get(String intfName) {

        return map.get(intfName);
    }

    public static void parse(Class<?> clz, Environment environment) {

        Annotation reyClientAnno = clz.getAnnotation(ReyClient.class);
        if (reyClientAnno == null)
            return;

        ReyClient reyClient = (ReyClient) reyClientAnno;

        String url = reyClient.value();
        url = environment.resolvePlaceholders(url);

        ClientParsed parsed = new ClientParsed();
        parsed.setObjectType(clz);
        parsed.setUrl(url);

        map.put(clz.getName(),parsed);


        /*
         * fallback
         */
        Class<?> fallbackClz = reyClient.fallback();
        if (fallbackClz != null && fallbackClz != void.class) {
            Method[] fallbackMethodArr = fallbackClz.getMethods();
            for (Method fm : fallbackMethodArr) {
                parsed.getFallbackMethodMap().put(fm.getName(), fm);
            }
            try {
                parsed.setFallback(fallbackClz.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /*
         * groupRouter
         */
        Class<? extends GroupRouter> groupRouterClz = reyClient.groupRouter();
        if (groupRouterClz != GroupRouter.class) {
            try{
                parsed.setGroupRouter(groupRouterClz.newInstance());
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        Method[] arr = clz.getDeclaredMethods();

        for (Method method : arr) {

            String methodName = method.getName();
            Class<?> returnType = method.getReturnType();

            Annotation mappingAnno = method.getAnnotation(RequestMapping.class);
            if (mappingAnno == null) {
                logger.error(clz.getName() + "." + methodName + ", Not Found Annotation: " + RequestMapping.class.getName());
                System.exit(0);
            }

            RequestMapping requestMapping = (RequestMapping) mappingAnno;
            if (requestMapping.value() == null || requestMapping.value().length ==0) {
                logger.error(clz.getName() + "." + methodName + " RequestMapping, no mapping value");
                System.exit(0);
            }

            String mapping = requestMapping.value()[0];

            RequestMethod rm = RequestMethod.POST;

            RequestMethod[] rmArr = requestMapping.method();
            if (rmArr == null || rmArr.length == 0) {
                if (mapping != null && mapping.contains("{")&&mapping.contains("}")){
                    rm = RequestMethod.GET;
                }
            }else{
                rm = rmArr[0];
            }

            List<KV> hearderList = null;
            String[] headers = requestMapping.headers();
            if (headers != null && headers.length > 0){
                hearderList = new ArrayList<>();
                for (String header : headers){
                    int i = header.indexOf("=");
                    String key = header.substring(0,i);
                    String value = header.substring(i+1);
                    KV kv = new KV(key,value);
                    hearderList.add(kv);
                }
            }

            if (returnType == Map.class) {
                logger.info("ReyClient not support  genericReturnType of Map，while parsing " + method);
                logger.error("ReyClient not support  genericReturnType of Map，while parsing " + method);
                System.exit(0);
            }

            Class gtc = null;
            if (returnType == List.class) {
                Type gt = method.getGenericReturnType();
                ParameterizedType pt = (ParameterizedType)gt;
                Type t = pt.getActualTypeArguments()[0];
                if (t instanceof ParameterizedType) {
                    logger.error("ReyClient not support complex genericReturnType, like List<List<?>>, or" +
                            "List<Map>，while parsing " + method);
                    System.exit(0);
                }
                gtc = (Class)t;
            }

            MethodParsed methodParsed = new MethodParsed();
            methodParsed.setRequestMapping(mapping);
            methodParsed.setReturnType(returnType);
            methodParsed.setGeneType(gtc);
            methodParsed.setRequestMethod(rm);
            methodParsed.setHeaderList(hearderList);

            parsed.getMap().put(methodName,methodParsed);
        }

    }

}
