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
package io.xream.x7.reyc;

import io.xream.x7.reyc.api.GroupRouter;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface ReyClient {

    /**
     * dns or value
     */
    String value() default  "";

    /**
     * "" or configed backend name in application.properties
     */
    String circuitBreaker() default " ";

    /**
     * true | false
     */
    boolean retry() default false;

    /**
     * handle fallback for the important method <br>
     * Fallback class,  the method name must same as the method of service. and the parameters must same
     */
    Class<?> fallback() default void.class;

    /**
     * route to service grouped, like k8s namespace with suffix of sharding key
     */
    Class<? extends GroupRouter> groupRouter() default GroupRouter.class;
}
