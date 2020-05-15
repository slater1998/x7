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

import io.xream.x7.reyc.api.GroupRouter;
import org.springframework.web.bind.annotation.RequestMethod;
import io.xream.x7.common.bean.KV;

import java.util.Arrays;
import java.util.List;

public class R {
    private String url;
    private Class<?> returnType;
    private Class<?> geneType;
    private Object[] args;
    private RequestMethod requestMethod;
    private List<KV> headerList;
    private GroupRouter router;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public Class<?> getGeneType() {
        return geneType;
    }

    public void setGeneType(Class<?> geneType) {
        this.geneType = geneType;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(RequestMethod requestMethod) {
        this.requestMethod = requestMethod;
    }

    public List<KV> getHeaderList() {
        return headerList;
    }

    public void setHeaderList(List<KV> headerList) {
        this.headerList = headerList;
    }

    public GroupRouter getRouter() {
        return router;
    }

    public void setRouter(GroupRouter router) {
        this.router = router;
    }

    @Override
    public String toString() {
        return "R{" +
                "url='" + url + '\'' +
                ", returnType=" + returnType +
                ", geneType=" + geneType +
                ", args=" + Arrays.toString(args) +
                ", requestMethod=" + requestMethod +
                ", headerList=" + headerList +
                '}';
    }
}
