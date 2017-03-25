package com.vedsoft.objective.droid;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.vedsoft.futures.runnables.ThreeParameterAction;
import com.vedsoft.lazyj.AbstractSynchronousLazy;

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

/**
 * Created by david on 12/13/15.
 */
public class ObjectiveDroid {
	private final SQLiteDatabase database;
	private final String sqlQuery;
	private final HashMap<String, String> parameters = new HashMap<>();

	public ObjectiveDroid(SQLiteDatabase database, String sqlQuery) {
		this.database = database;
		this.sqlQuery = sqlQuery;
	}

	public ObjectiveDroid addParameter(String parameter, String value) {
		parameters.put(parameter, value);
		return this;
	}

	public <E extends Enum<E>> ObjectiveDroid addParameter(String parameter, Enum<E> value) {
		return addParameter(parameter, value != null ? value.name() : null);
	}

	public ObjectiveDroid addParameter(String parameter, short value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public ObjectiveDroid addParameter(String parameter, int value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public ObjectiveDroid addParameter(String parameter, long value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public ObjectiveDroid addParameter(String parameter, float value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public ObjectiveDroid addParameter(String parameter, double value) {
		return addParameter(parameter, String.valueOf(value));
	}

	public ObjectiveDroid addParameter(String parameter, boolean value) {
		return addParameter(parameter, value ? 1 : 0);
	}

	public ObjectiveDroid addParameter(String parameter, Object value) {
		return addParameter(parameter, value.toString());
	}

	public ObjectiveDroid addParameters(Map<String, Object> parameters) {
		for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
			final String key = parameter.getKey();
			final Object value = parameter.getValue();
			final Class<?> valueClass = value.getClass();

			if (Boolean.TYPE.equals(valueClass)) {
				addParameter(key, (boolean)value);
				continue;
			}

			if (Short.TYPE.equals(valueClass)) {
				addParameter(key, (short)value);
				continue;
			}

			if (Integer.TYPE.equals(valueClass)) {
				addParameter(key, (int)value);
				continue;
			}

			if (Long.TYPE.equals(valueClass)) {
				addParameter(key, (long)value);
				continue;
			}

			if (Float.TYPE.equals(valueClass)) {
				addParameter(key, (float)value);
				continue;
			}

			if (Double.TYPE.equals(valueClass)) {
				addParameter(key, (double)value);
				continue;
			}

			addParameter(key, value);
		}

		return this;
	}

	public <T> List<T> fetch(Class<T> cls) throws SQLException {
		final Cursor cursor = getCursorForQuery();
		try {
			if (!cursor.moveToFirst()) return new ArrayList<>();

			final ArrayList<T> returnObjects = new ArrayList<>(cursor.getCount());
			do {
				returnObjects.add(mapDataFromCursorToClass(cursor, cls));
			} while (cursor.moveToNext());

			return returnObjects;
		} finally {
			cursor.close();
		}
	}

	public <T> T fetchFirst(Class<T> cls) {
		final Cursor cursor = getCursorForQuery();
		try {
			if (!cursor.moveToFirst() || cursor.getCount() == 0) return null;

			return mapDataFromCursorToClass(cursor, cls);
		} finally {
			cursor.close();
		}
	}

	private Cursor getCursorForQuery() {
		final Map.Entry<String, String[]> compatibleSqlQuery = QueryCache.getSqlQuery(sqlQuery, parameters);

		return database.rawQuery(compatibleSqlQuery.getKey(), compatibleSqlQuery.getValue());
	}

	private static <T> T mapDataFromCursorToClass(Cursor cursor, Class<T> cls) {
		final ClassReflections reflections = ClassCache.getReflections(cls);

		final T newObject;
		try {
			newObject = cls.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}

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

		return newObject;
	}

	public long execute() throws SQLException {
		final Map.Entry<String, String[]> compatibleSqlQuery = QueryCache.getSqlQuery(sqlQuery, parameters);

		final String sqlQuery = compatibleSqlQuery.getKey();

		final SQLiteStatement sqLiteStatement = database.compileStatement(sqlQuery);
		try {
			final String[] args = compatibleSqlQuery.getValue();
			for (int i = 0; i < args.length; i++) {
				final String arg = args[i];
				if (arg != null)
					sqLiteStatement.bindString(i + 1, arg);
				else
					sqLiteStatement.bindNull(i + 1);
			}

			return executeSpecial(sqLiteStatement, sqlQuery);
		} finally {
			sqLiteStatement.close();
		}
	}

	private static long executeSpecial(SQLiteStatement sqLiteStatement, String sqlQuery) {
		final String sqlQueryType = sqlQuery.trim().substring(0, 3).toLowerCase(Locale.ROOT);
		if (sqlQueryType.equals("upd") || sqlQueryType.equals("del"))
			return sqLiteStatement.executeUpdateDelete();
		if (sqlQueryType.equals("ins"))
			return sqLiteStatement.executeInsert();
		return sqLiteStatement.simpleQueryForLong();
	}

	private static class QueryCache {
		private static final Map<String, Map.Entry<String, String[]>> queryCache = new HashMap<>();

		static synchronized Map.Entry<String, String[]> getSqlQuery(String sqlQuery, Map<String, String> parameters) {
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
				newParameters[i] = parameterValue;
			}

			return new AbstractMap.SimpleImmutableEntry<>(cachedQuery.getKey(), newParameters);
		}
	}

	private static class ClassCache {
		private static final Map<Class<?>, ClassReflections> classCache = new HashMap<>();

		static synchronized <T extends Class<?>> ClassReflections getReflections(T cls) {
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
					setters.getObject().get(type).getObject().runWith(field, object, value);
					break;
				}
				currentType = type.getSuperclass();
			}
		}

		private static final AbstractSynchronousLazy<HashMap<Type, AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>>> setters = new AbstractSynchronousLazy<HashMap<Type, AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>>>() {
			@Override
			protected final HashMap<Type, AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>> initialize() {
				final HashMap<Type, AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>> newHashMap = new HashMap<>();

				newHashMap.put(Boolean.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.setBoolean(parameterTwo, parseSqlBoolean(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Boolean.class, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.set(parameterTwo, !isSqlValueNull(parameterThree) ? parseSqlBoolean(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Short.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.setShort(parameterTwo, Short.parseShort(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Short.class, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.set(parameterTwo, !isSqlValueNull(parameterThree) ? Short.parseShort(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Integer.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.setInt(parameterTwo, Integer.parseInt(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Integer.class, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.set(parameterTwo, !isSqlValueNull(parameterThree) ? Integer.parseInt(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Long.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.setLong(parameterTwo, Long.parseLong(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Long.class, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.set(parameterTwo, !isSqlValueNull(parameterThree) ? Long.parseLong(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Float.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.setFloat(parameterTwo, Float.parseFloat(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Float.class, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.set(parameterTwo, !isSqlValueNull(parameterThree) ? Float.parseFloat(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Double.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.setDouble(parameterTwo, Double.parseDouble(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Double.class, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.set(parameterTwo, !isSqlValueNull(parameterThree) ? Double.parseDouble(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(String.class, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.set(parameterTwo, parameterThree);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Enum.class, new AbstractSynchronousLazy<ThreeParameterAction<Field, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Field, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.set(parameterTwo, Enum.valueOf((Class<? extends Enum>) parameterOne.getType(), parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				return newHashMap;
			}
		};
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
					setters.getObject().get(currentType).getObject().runWith(method, object, value);
					break;
				}
				currentType = type.getSuperclass();
			}
		}

		private static final AbstractSynchronousLazy<HashMap<Class<?>, AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>>> setters = new AbstractSynchronousLazy<HashMap<Class<?>, AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>>>() {
			@Override
			protected final HashMap<Class<?>, AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>> initialize() {
				final HashMap<Class<?>, AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>> newHashMap = new HashMap<>();

				newHashMap.put(Boolean.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.invoke(parameterTwo, parseSqlBoolean(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Boolean.class, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.invoke(parameterTwo, !isSqlValueNull(parameterThree) ? parseSqlBoolean(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Short.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.invoke(parameterTwo, Short.parseShort(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Short.class, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.invoke(parameterTwo, !isSqlValueNull(parameterThree) ? Short.parseShort(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Integer.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.invoke(parameterTwo, Integer.parseInt(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Integer.class, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.invoke(parameterTwo, !isSqlValueNull(parameterThree) ? Integer.parseInt(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Long.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.invoke(parameterTwo, Long.parseLong(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Long.class, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.invoke(parameterTwo, !isSqlValueNull(parameterThree) ? Long.parseLong(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Float.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.invoke(parameterTwo, Float.parseFloat(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Float.class, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.invoke(parameterTwo, !isSqlValueNull(parameterThree) ? Float.parseFloat(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Double.TYPE, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.invoke(parameterTwo, Double.parseDouble(parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Double.class, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.invoke(parameterTwo, !isSqlValueNull(parameterThree) ? Double.parseDouble(parameterThree) : null);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(String.class, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								parameterOne.invoke(parameterTwo, parameterThree);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				newHashMap.put(Enum.class, new AbstractSynchronousLazy<ThreeParameterAction<Method, Object, String>>() {
					@Override
					protected final ThreeParameterAction<Method, Object, String> initialize() {
						return (parameterOne, parameterTwo, parameterThree) -> {
							try {
								if (!isSqlValueNull(parameterThree))
									parameterOne.invoke(parameterTwo, Enum.valueOf((Class<? extends Enum>) parameterOne.getParameterTypes()[0], parameterThree));
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							} catch (InvocationTargetException e) {
								throw new RuntimeException(e);
							}
						};
					}
				});

				return newHashMap;
			}
		};
	}

	private static boolean parseSqlBoolean(String booleanValue) {
		return Integer.parseInt(booleanValue) != 0;
	}

	private static boolean isSqlValueNull(String sqlValue) {
		return sqlValue == null || sqlValue.equals("NULL");
	}
}
