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
package io.xream.x7.repository.mapper;

import io.xream.x7.common.bean.BeanElement;
import io.xream.x7.common.bean.Parsed;
import io.xream.x7.common.bean.Parser;
import io.xream.x7.common.repository.X;
import io.xream.x7.common.util.BeanUtil;
import io.xream.x7.common.util.LoggerProxy;
import io.xream.x7.repository.DbType;
import io.xream.x7.repository.Mapped;
import io.xream.x7.repository.util.SqlParserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapperFactory implements Mapper {

	private static Map<Class, Map<String, String>> sqlsMap = new HashMap<>();

	public static Dialect Dialect;

	/**
	 * 返回SQL
	 * 
	 * @param clz
	 *            ? extends IAutoMapped
	 * @param type
	 *            (BeanMapper.CREATE|BeanMapper.REFRESH|BeanMapper.DROP|
	 *            BeanMapper.QUERY)
	 */
	@SuppressWarnings({ "rawtypes" })
	public static String getSql(Class clz, String type) {

		Map<String, String> sqlMap = sqlsMap.get(clz);
		if (sqlMap == null) {
			sqlMap = new HashMap<String, String>();
			sqlsMap.put(clz, sqlMap);
			parseBean(clz);
		}

		return sqlMap.get(type);

	}

	@SuppressWarnings({ "rawtypes" })
	public static String tryToCreate(Class clz) {

		Map<String, String> sqlMap = sqlsMap.get(clz);
		if (sqlMap == null) {
			sqlMap = new HashMap<>();
			sqlsMap.put(clz, sqlMap);
			parseBean(clz);
			return sqlMap.remove(CREATE_TABLE);
		}

		return "";

	}

	/**
	 * 
	 * @param clz
	 */
	public static List<BeanElement> getElementList(Class clz) {
		return Parser.get(clz).getBeanElementList();
	}

	@SuppressWarnings({ "rawtypes" })
	public static void parseBean(Class clz) {

		String dbType = DbType.value;
		switch (dbType) {
		default:
			StandardSql sql = new StandardSql();
			sql.getTableSql(clz);
			sql.getRefreshSql(clz);
			sql.getRemoveSql(clz);
			sql.getOneSql(clz);
			sql.getQuerySql(clz);
			sql.getLoadSql(clz);
			sql.getCreateSql(clz);
			sql.getTagSql(clz);
			return;
		}

	}

	public static class StandardSql implements Interpreter {
		public String getRefreshSql(Class clz) {

			Parsed parsed = Parser.get(clz);

			List<BeanElement> list = Parser.get(clz).getBeanElementList();

			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("UPDATE ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);
			sb.append("SET ");

			String keyOne = parsed.getKey(X.KEY_ONE);

			List<BeanElement> tempList = new ArrayList<BeanElement>();
			for (BeanElement p : list) {
				String column = p.property;
				if (column.equals(keyOne))
					continue;

				tempList.add(p);
			}

			int size = tempList.size();
			for (int i = 0; i < size; i++) {
				String column = tempList.get(i).property;

				sb.append(column).append(" = ?");
				if (i < size - 1) {
					sb.append(", ");
				}
			}

			sb.append(" WHERE ");

			parseKey(sb, clz);

			String sql = sb.toString();

			sql = SqlParserUtil.mapper(sql, parsed);

			sqlsMap.get(clz).put(REFRESH, sql);

			LoggerProxy.debug(clz, sb);

			return sql;

		}

		public String getRemoveSql(Class clz) {
			Parsed parsed = Parser.get(clz);
			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("DELETE FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);
			sb.append("WHERE ");

			parseKey(sb, clz);

			String sql = sb.toString();

			sql = SqlParserUtil.mapper(sql, parsed);

			sqlsMap.get(clz).put(REMOVE, sql);

			LoggerProxy.debug(clz, sb);

			return sql;

		}

		public String getOneSql(Class clz) {
			Parsed parsed = Parser.get(clz);
			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);
			sb.append("WHERE ");

			parseKey(sb, clz);

			String sql = sb.toString();

			sql = SqlParserUtil.mapper(sql, parsed);

			sqlsMap.get(clz).put(GET_ONE, sql);

			LoggerProxy.debug(clz, sb);

			return sql;

		}

		public void parseKey(StringBuilder sb, Class clz) {
			Parsed parsed = Parser.get(clz);

			sb.append(parsed.getKey(X.KEY_ONE));
			sb.append(" = ?");

		}

		public String getQuerySql(Class clz) {

			Parsed parsed = Parser.get(clz);
			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);
			sb.append("WHERE ");

			sb.append(parsed.getKey(X.KEY_ONE));
			sb.append(" = ?");

			String sql = sb.toString();
			sql = SqlParserUtil.mapper(sql, parsed);

			sqlsMap.get(clz).put(QUERY, sql);

			LoggerProxy.debug(clz, sb);

			return sql;

		}

		public String getLoadSql(Class clz) {

			Parsed parsed = Parser.get(clz);
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT * FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName()));

			String sql = sb.toString();

			sql = SqlParserUtil.mapper(sql, parsed);

			sqlsMap.get(clz).put(LOAD, sql);

			LoggerProxy.debug(clz, sb);

			return sql;

		}



		public String getCreateSql(Class clz) {

			List<BeanElement> list = Parser.get(clz).getBeanElementList();

			Parsed parsed = Parser.get(clz);

			List<BeanElement> tempList = new ArrayList<BeanElement>();
			for (BeanElement p : list) {

				tempList.add(p);
			}

			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("INSERT INTO ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);

			sb.append("(");
			int size = tempList.size();
			for (int i = 0; i < size; i++) {
				String p = tempList.get(i).property;

				sb.append(" ").append(p).append(" ");
				if (i < size - 1) {
					sb.append(",");
				}
			}
			sb.append(") VALUES (");

			for (int i = 0; i < size; i++) {

				sb.append("?");
				if (i < size - 1) {
					sb.append(",");
				}
			}
			sb.append(")");

			String sql = sb.toString();
			sql = SqlParserUtil.mapper(sql, parsed);
			sqlsMap.get(clz).put(CREATE, sql);

			LoggerProxy.debug(clz, sb);

			return sql;

		}

		public String getTableSql(Class clz) {

			List<BeanElement> temp = Parser.get(clz).getBeanElementList();
			Map<String, BeanElement> map = new HashMap<String, BeanElement>();
			List<BeanElement> list = new ArrayList<BeanElement>();
			for (BeanElement be : temp) {
				if (be.sqlType != null && be.sqlType.equals("text")) {
					list.add(be);
					continue;
				}
				map.put(be.property, be);
			}
			Parsed parsed = Parser.get(clz);

			final String keyOne = parsed.getKey(X.KEY_ONE);

			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE IF NOT EXISTS ").append(BeanUtil.getByFirstLower(parsed.getClzName())).append(" (")
					.append("\n");

			sb.append("   ").append(keyOne);

			BeanElement be = map.get(keyOne);
			String sqlType = Mapper.getSqlTypeRegX(be);

			if (sqlType.equals(Dialect.INT)) {
				sb.append(Dialect.INT + " NOT NULL");
			} else if (sqlType.equals(Dialect.LONG)) {
				sb.append(Dialect.LONG + " NOT NULL");
			} else if (sqlType.equals(Dialect.STRING)) {
				sb.append(Dialect.STRING).append("(").append(be.length).append(") NOT NULL");
			}

			sb.append(", ");// FIXME ORACLE

			sb.append("\n");
			map.remove(keyOne);

			for (BeanElement bet : map.values()) {
				sqlType = Mapper.getSqlTypeRegX(bet);
				sb.append("   ").append(bet.property).append(" ");

				sb.append(sqlType);

				if (sqlType.equals(Dialect.BIG)) {
					sb.append(" DEFAULT 0.00 ");
				} else if (sqlType.equals(Dialect.DATE)) {
					sb.append(" NULL");

				}else if (BeanUtil.isEnum(bet.clz)) {
					sb.append("(").append(bet.length).append(") NOT NULL");
				} else if (sqlType.equals(Dialect.STRING)) {
					sb.append("(").append(bet.length).append(") NULL");
				} else {
					if (bet.clz == Boolean.class || bet.clz == boolean.class || bet.clz == Integer.class
							|| bet.clz == int.class || bet.clz == Long.class || bet.clz == long.class) {
						sb.append(" DEFAULT 0");
					} else {
						sb.append(" DEFAULT NULL");
					}
				}
				sb.append(",").append("\n");
			}

			for (BeanElement bet : list) {
				sqlType = Mapper.getSqlTypeRegX(bet);
				sb.append("   ").append(bet.property).append(" ").append(sqlType).append(",").append("\n");
			}

			sb.append("   PRIMARY KEY ( ").append(keyOne).append(" )");

			sb.append("\n");
			sb.append(") ").append(Dialect.ENGINE).append(";");

			String sql = sb.toString();
			sql = Dialect.match(sql, CREATE_TABLE);
			sql = SqlParserUtil.mapper(sql, parsed);
			sqlsMap.get(clz).put(CREATE_TABLE, sql);

			return sql;
		}

		public String getTagSql(Class clz) {
			Parsed parsed = Parser.get(clz);
			String space = " ";
			StringBuilder sb = new StringBuilder();
			sb.append("SELECT " + Mapped.TAG + " FROM ");
			sb.append(BeanUtil.getByFirstLower(parsed.getClzName())).append(space);

			String sql = sb.toString();

			sql = SqlParserUtil.mapper(sql, parsed);
			sqlsMap.get(clz).put(TAG, sql);

			LoggerProxy.debug(clz, sb);

			return sql;
		}
	}

}
