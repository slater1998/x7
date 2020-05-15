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
package io.xream.x7.common.cache;

import io.xream.x7.common.bean.Criteria;
import io.xream.x7.common.web.Page;
import io.xream.x7.repository.QueryForCache;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 
 * 缓存<br>
 * @author sim
 *
 */
public interface L2CacheResolver extends Protection{

	void setL2CacheConsistency(L2CacheConsistency l2CacheConsistency);
	void setCacheStorage(L2CacheStorage cacheStorage);

	boolean isEnabled();
	/**
	 * 标记缓存要更新
	 * @param clz
	 * @return nanuTime_String
	 */
	@SuppressWarnings("rawtypes")
	String markForRefresh(Class clz);

	boolean refresh(Class clz, String key);
	boolean refresh(Class clz);


	<T> List<T> listUnderProtection(Class<T> clz, Object conditionObj,  QueryForCache queryForCache,  Callable<List<T>> callable);
	<T> T getUnderProtection(Class<T> clz, Object conditonObj,Callable<T> callable);
	<T> Page<T> findUnderProtection(Criteria criteria, QueryForCache queryForCache, Callable<Page<T>> findCallable, Callable<List<T>> listCallable);

}
