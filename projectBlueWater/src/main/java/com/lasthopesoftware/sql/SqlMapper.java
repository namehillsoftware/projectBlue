package com.lasthopesoftware.sql;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.lasthopesoftware.runnables.IThreeParameterRunnable;
import com.lasthopesoftware.threading.Lazy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by david on 12/13/15.
 */
public class SqlMapper {
	private final SQLiteDatabase database;
	private final String sqlQuery;
	private final HashMap<String, String> parameters = new HashMap<>();

	public SqlMapper(SQLiteDatabase database, String sqlQuery) {
		this.database = database;
		this.sqlQuery = sqlQuery;
	}

	public SqlMapper addParameter(String parameter, String value) {
		parameters.put(parameter, value);
		return this;
	}

	public <E extends Enum<E>> SqlMapper addParameter(String parameter, Enum<E> value) {
		return addParameter(parameter, value != null ? value.name() : null);
	}

	public SqlMapper addParameter(String parameter, int value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public SqlMapper addParameter(String parameter, long value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public SqlMapper addParameter(String parameter, boolean value) {
		return addParameter(parameter, value ? 1 : 0);
	}

	public SqlMapper addParameter(String parameter, Object value) {
		return addParameter(parameter, value.toString());
	}

	public SqlMapper addParameters(Map<String, Object> parameters) {
		for (Map.Entry<String, Object> parameter : parameters.entrySet())
			addParameter(parameter.getKey(), parameter.getValue());

		return this;
	}

	public <T> List<T> fetch(Class<T> cls) throws SQLException {
		final Map.Entry<String, String[]> compatibleSqlQuery = QueryCache.getSqlQuery(sqlQuery, parameters);

		try {
			final Cursor cursor = database.rawQuery(compatibleSqlQuery.getKey(), compatibleSqlQuery.getValue());
			try {
				if (!cursor.moveToFirst()) return new ArrayList<>();

				final ClassReflections reflections = ClassCache.getReflections(cls);

				final ArrayList<T> returnObjects = new ArrayList<>(cursor.getCount());
				do {
					final T newObject;
					try {
						newObject = cls.newInstance();

						for (int i = 0; i < cursor.getColumnCount(); i++) {
							String colName = cursor.getColumnName(i).toLowerCase(Locale.ROOT);

							if (reflections.setterMap.containsKey(colName)) {
								reflections.setterMap.get(colName).set(newObject, cursor.getString(i));
								continue;
							}

							if (!colName.startsWith("is")) continue;

							colName = colName.substring(2);
							if (reflections.setterMap.containsKey(colName))
								reflections.setterMap.get(colName).set(newObject, cursor.getString(i));
						}

						returnObjects.add(newObject);
					} catch (InstantiationException e) {
						throw new RuntimeException(e);
					} catch (IllegalAccessException e) {
						throw new RuntimeException(e);
					}
				} while (cursor.moveToNext());

				return returnObjects;
			} finally {
				cursor.close();
			}
		} catch (SQLException se) {
			throw se;
		}
	}

	public <T> T fetchFirst(Class<T> cls) {
		final List<T> results = fetch(cls);

		return results.size() > 0 ? results.get(0) : null;
	}

	public long execute() throws SQLException {
		final Map.Entry<String, String[]> compatibleSqlQuery = QueryCache.getSqlQuery(sqlQuery, parameters);

		final String sqlQuery = compatibleSqlQuery.getKey();

		try {
			final SQLiteStatement sqLiteStatement = database.compileStatement(sqlQuery);
			sqLiteStatement.bindAllArgsAsStrings(compatibleSqlQuery.getValue());

			final String sqlQueryType = sqlQuery.substring(0, 3).toLowerCase(Locale.ROOT);
			if (sqlQueryType.equals("upd") || sqlQueryType.equals("del"))
				return sqLiteStatement.executeUpdateDelete();
			if (sqlQueryType.equals("ins"))
				return sqLiteStatement.executeInsert();

			return sqLiteStatement.simpleQueryForLong();
		} catch (SQLException se) {
			throw se;
		}
	}

	private static class QueryCache {
		private static final Map<String, Map.Entry<String, String[]>> queryCache = new HashMap<>();

		public static synchronized Map.Entry<String, String[]> getSqlQuery(String sqlQuery, Map<String, String> parameters) {
			sqlQuery = sqlQuery.trim();
			if (queryCache.containsKey(sqlQuery))
				return getOrderedSqlParameters(queryCache.get(sqlQuery), parameters);

			final ArrayList<String> sqlParameters = new ArrayList<>();
			final StringBuilder sqlQueryBuilder = new StringBuilder(sqlQuery);
			int paramIndex;

			for (int i = 0; i < sqlQueryBuilder.length(); i++) {
				final char queryChar = sqlQueryBuilder.charAt(i);

				if (queryChar == '\'') {
					i = sqlQueryBuilder.indexOf("'", ++i);

					if (i < 0) break;

					continue;
				}

				if (queryChar != '@') continue;

				paramIndex = i;
				final StringBuilder paramStringBuilder = new StringBuilder();
				while (++paramIndex < sqlQueryBuilder.length()) {
					final char paramChar = sqlQueryBuilder.charAt(paramIndex);

					// A parameter needs to look like a Java identifier
					if (paramIndex == i + 1 && !Character.isJavaIdentifierStart(paramChar)) break;
					if (!Character.isJavaIdentifierPart(paramChar)) break;

					paramStringBuilder.append(paramChar);
				}

				sqlParameters.add(paramStringBuilder.toString());
				sqlQueryBuilder.replace(paramIndex - paramStringBuilder.length() - 1, paramIndex, "?");
			}

			final Map.Entry<String, String[]> entry = new AbstractMap.SimpleImmutableEntry<>(sqlQueryBuilder.toString(), sqlParameters.toArray(new String[sqlParameters.size()]));

			queryCache.put(sqlQuery, entry);

			return getOrderedSqlParameters(entry, parameters);
		}

		private static Map.Entry<String, String[]> getOrderedSqlParameters(Map.Entry<String, String[]> cachedQuery, Map<String, String> parameters) {
			final String[] parameterHolders = cachedQuery.getValue();
			final String[] newParameters = new String[parameterHolders.length];
			for (int i = 0; i < parameterHolders.length; i++) {
				final String parameterName = parameterHolders[i];
				if (!parameters.containsKey(parameterName)) continue;

				final String parameterValue = parameters.get(parameterName);
				newParameters[i] = parameterValue != null ? parameterValue : "NULL";
			}

			return new AbstractMap.SimpleImmutableEntry<>(cachedQuery.getKey(), newParameters);
		}
	}

	private static class ClassCache {
		private static final Map<Class<?>, ClassReflections> classCache = new HashMap<>();

		public static synchronized <T extends Class<?>> ClassReflections getReflections(T cls) {
			if (!classCache.containsKey(cls))
				classCache.put(cls, new ClassReflections(cls));

			return classCache.get(cls);
		}
	}

	private interface ISetter {
		void set(Object object, String value);
	}

	private static class ClassReflections {
		public final Map<String, ISetter> setterMap = new HashMap<>();

		public <T extends Class<?>> ClassReflections(T cls) {

			for (final Field f : cls.getFields()) {
				setterMap.put(f.getName().toLowerCase(Locale.ROOT), new FieldSetter(f));
			}

			// prepare methods. Methods will override fields, if both exists.
			for (Method m : cls.getMethods()) {
				if (m.getParameterTypes().length == 1 && m.getName().startsWith("set"))
					setterMap.put(m.getName().substring(3).toLowerCase(Locale.ROOT), new MethodSetter(m));
			}
		}
	}

	private static class FieldSetter implements ISetter {
		private final Field field;
		private final Class<?> type;

		public FieldSetter(Field field) {
			this.field = field;
			type = field.getType();
		}

		public void set(Object object, String value) {
			Class<?> currentType = type;
			while (currentType != Object.class) {
				if (setters.getObject().containsKey(currentType)) {
					setters.getObject().get(type).getObject().run(field, object, value);
					break;
				}
				currentType = type.getSuperclass();
			}
		}

		private static final Lazy<HashMap<Type, Lazy<IThreeParameterRunnable<Field, Object, String>>>> setters = new Lazy<>(new Callable<HashMap<Type, Lazy<IThreeParameterRunnable<Field, Object, String>>>>() {
			@Override
			public HashMap<Type, Lazy<IThreeParameterRunnable<Field, Object, String>>> call() throws Exception {
				final HashMap<Type, Lazy<IThreeParameterRunnable<Field, Object, String>>> newHashMap = new HashMap<>();

				newHashMap.put(Boolean.TYPE, new Lazy<>(new Callable<IThreeParameterRunnable<Field, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Field, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Field, Object, String>() {
							@Override
							public void run(Field parameterOne, Object parameterTwo, String parameterThree) {
								try {
									if (parameterThree != null)
										parameterOne.setBoolean(parameterTwo, parseSqlBoolean(parameterThree));
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				newHashMap.put(Integer.TYPE, new Lazy<>(new Callable<IThreeParameterRunnable<Field, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Field, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Field, Object, String>() {
							@Override
							public void run(Field parameterOne, Object parameterTwo, String parameterThree) {
								try {
									if (parameterThree != null)
										parameterOne.setInt(parameterTwo, Integer.parseInt(parameterThree));
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				newHashMap.put(Long.TYPE, new Lazy<>(new Callable<IThreeParameterRunnable<Field, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Field, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Field, Object, String>() {
							@Override
							public void run(Field parameterOne, Object parameterTwo, String parameterThree) {
								try {
									if (parameterThree != null)
										parameterOne.setLong(parameterTwo, Long.parseLong(parameterThree));
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				newHashMap.put(String.class, new Lazy<>(new Callable<IThreeParameterRunnable<Field, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Field, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Field, Object, String>() {
							@Override
							public void run(Field parameterOne, Object parameterTwo, String parameterThree) {
								try {
									parameterOne.set(parameterTwo, parameterThree);
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				newHashMap.put(Enum.class, new Lazy<>(new Callable<IThreeParameterRunnable<Field, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Field, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Field, Object, String>() {
							@Override
							public void run(Field parameterOne, Object parameterTwo, String parameterThree) {
								try {
									parameterOne.set(parameterTwo, Enum.valueOf((Class<? extends Enum>) parameterOne.getType(), parameterThree));
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				return newHashMap;
			}
		});
	}

	private static class MethodSetter implements ISetter {
		private final Method method;
		private final Class<?> type;

		public MethodSetter(Method method) {
			this.method = method;
			type = method.getParameterTypes()[0];
		}

		public void set(Object object, String value) {
			Class<?> currentType = type;
			while (currentType != Object.class) {
				if (setters.getObject().containsKey(currentType)) {
					setters.getObject().get(currentType).getObject().run(method, object, value);
					break;
				}
				currentType = type.getSuperclass();
			}
		}

		private static final Lazy<HashMap<Class<?>, Lazy<IThreeParameterRunnable<Method, Object, String>>>> setters = new Lazy<>(new Callable<HashMap<Class<?>, Lazy<IThreeParameterRunnable<Method, Object, String>>>>() {
			@Override
			public HashMap<Class<?>, Lazy<IThreeParameterRunnable<Method, Object, String>>> call() throws Exception {
				final HashMap<Class<?>, Lazy<IThreeParameterRunnable<Method, Object, String>>> newHashMap = new HashMap<>();

				newHashMap.put(Boolean.TYPE, new Lazy<>(new Callable<IThreeParameterRunnable<Method, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Method, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Method, Object, String>() {
							@Override
							public void run(Method parameterOne, Object parameterTwo, String parameterThree) {
								try {
									if (parameterThree != null)
										parameterOne.invoke(parameterTwo, parseSqlBoolean(parameterThree));
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								} catch (InvocationTargetException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				newHashMap.put(Integer.TYPE, new Lazy<>(new Callable<IThreeParameterRunnable<Method, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Method, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Method, Object, String>() {
							@Override
							public void run(Method parameterOne, Object parameterTwo, String parameterThree) {
								try {
									if (parameterThree != null)
										parameterOne.invoke(parameterTwo, Integer.parseInt(parameterThree));
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								} catch (InvocationTargetException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				newHashMap.put(Long.TYPE, new Lazy<>(new Callable<IThreeParameterRunnable<Method, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Method, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Method, Object, String>() {
							@Override
							public void run(Method parameterOne, Object parameterTwo, String parameterThree) {
								try {
									if (parameterThree != null)
										parameterOne.invoke(parameterTwo, Long.parseLong(parameterThree));
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								} catch (InvocationTargetException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				newHashMap.put(String.class, new Lazy<>(new Callable<IThreeParameterRunnable<Method, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Method, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Method, Object, String>() {
							@Override
							public void run(Method parameterOne, Object parameterTwo, String parameterThree) {
								try {
									parameterOne.invoke(parameterTwo, parameterThree);
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								} catch (InvocationTargetException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				newHashMap.put(Enum.class, new Lazy<>(new Callable<IThreeParameterRunnable<Method, Object, String>>() {
					@Override
					public IThreeParameterRunnable<Method, Object, String> call() throws Exception {
						return new IThreeParameterRunnable<Method, Object, String>() {
							@Override
							public void run(Method parameterOne, Object parameterTwo, String parameterThree) {
								try {
									parameterOne.invoke(parameterTwo, Enum.valueOf((Class<? extends Enum>) parameterOne.getParameterTypes()[0], parameterThree));
								} catch (IllegalAccessException e) {
									throw new RuntimeException(e);
								} catch (InvocationTargetException e) {
									throw new RuntimeException(e);
								}
							}
						};
					}
				}));

				return newHashMap;
			}
		});
	}

	private static boolean parseSqlBoolean(String booleanValue) {
		return Integer.parseInt(booleanValue) != 0;
	}
}
