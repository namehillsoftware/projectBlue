package com.lasthopesoftware.sql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.lasthopesoftware.runnables.IThreeParameterRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

	public SqlMapper addParameter(String parameter, int value) {
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

	public <T> List<T> fetch(Class<T> cls) {
		final Map.Entry<String, String[]> compatibleSqlQuery = QueryCache.getSqlQuery(sqlQuery, parameters);

		final Cursor cursor = database.rawQuery(compatibleSqlQuery.getKey(), compatibleSqlQuery.getValue());
		try {
			if (!cursor.moveToFirst()) return new ArrayList<>();

			final ClassReflections reflections = ClassCache.getReflections(cls);

			final ArrayList<T> returnObjects = new ArrayList<>();
			do {
				final T newObject;
				try {
					newObject = cls.newInstance();

					for (int i = 0; i < cursor.getColumnCount(); i++) {
						final String colName = cursor.getColumnName(i);

						if (reflections.setterMap.containsKey(colName))
							reflections.setterMap.get(colName).set(newObject, cursor.getString(i));
					}

					returnObjects.add(newObject);
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			} while (cursor.moveToNext());

			return returnObjects;
		} finally {
			cursor.close();
		}
	}

	public <T> T fetchFirst(Class<T> cls) {
		final List<T> results = fetch(cls);

		return results.size() > 0 ? results.get(0) : null;
	}

	public void execute() {
		final Map.Entry<String, String[]> compatibleSqlQuery = QueryCache.getSqlQuery(sqlQuery, parameters);

		database.execSQL(compatibleSqlQuery.getKey(), compatibleSqlQuery.getValue());
	}

	private static class QueryCache {
		private static final Map<String, Map.Entry<String, String[]>> queryCache = new ConcurrentHashMap<>();

		private static final Set<Character> endChars = new HashSet<>(Arrays.asList(';', '='));

		public static Map.Entry<String, String[]> getSqlQuery(String sqlQuery, Map<String, String> parameters) {
			if (queryCache.containsKey(sqlQuery))
				return getOrderedSqlParameters(queryCache.get(sqlQuery), parameters);

			final ArrayList<String> sqlParameters = new ArrayList<>();
			final StringBuilder sqlQueryBuilder = new StringBuilder(sqlQuery);
			int paramIndex;
			while ((paramIndex = sqlQueryBuilder.indexOf(":")) > -1) {
				final StringBuilder paramStringBuilder = new StringBuilder();
				while (++paramIndex < sqlQueryBuilder.length()) {
					final char paramChar = sqlQueryBuilder.charAt(paramIndex);

					if (!endChars.contains(paramChar) && !Character.isWhitespace(paramChar))
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
				if (parameters.containsKey(parameterHolders[i]))
					newParameters[i] = parameters.get(parameterHolders[i]);
			}

			return new AbstractMap.SimpleImmutableEntry<>(cachedQuery.getKey(), newParameters);
		}
	}

	private static class ClassCache {
		private static final Map<Class<?>, ClassReflections> classCache = new ConcurrentHashMap<>();

		public static <T extends Class<?>> ClassReflections getReflections(T cls) {
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
			Class<?> currentClass = cls;
			do {
				for (final Field f : currentClass.getDeclaredFields()) {
					setterMap.put(f.getName().toLowerCase(), new FieldSetter(f));
				}

				// prepare methods. Methods will override fields, if both exists.
				for (Method m : currentClass.getDeclaredMethods()) {
					if (m.getParameterTypes().length == 1 && m.getName().startsWith("set"))
						setterMap.put(m.getName().substring(3).toLowerCase(), new MethodSetter(m));
				}
				currentClass = cls.getSuperclass();
			} while (!currentClass.equals(Object.class));
		}
	}

	private static class FieldSetter implements ISetter {
		private final Field field;
		private final Type type;

		public FieldSetter(Field field) {
			this.field = field;
			type = field.getType();
		}

		public void set(Object object, String value) {
			setters.get(type).run(field, object, value);
		}

		private static final HashMap<Type, IThreeParameterRunnable<Field, Object, String>> setters;

		static {
			setters = new HashMap<>();
			setters.put(Boolean.TYPE, new IThreeParameterRunnable<Field, Object, String>() {
				@Override
				public void run(Field parameterOne, Object parameterTwo, String parameterThree) {
					try {
						parameterOne.setBoolean(parameterTwo, Boolean.parseBoolean(parameterThree));
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			});

			setters.put(Integer.TYPE, new IThreeParameterRunnable<Field, Object, String>() {
				@Override
				public void run(Field parameterOne, Object parameterTwo, String parameterThree) {
					try {
						parameterOne.setInt(parameterTwo, Integer.parseInt(parameterThree));
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			});

			setters.put(Object.class, new IThreeParameterRunnable<Field, Object, String>() {
				@Override
				public void run(Field parameterOne, Object parameterTwo, String parameterThree) {
					try {
						parameterOne.set(parameterTwo, parameterThree);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	private static class MethodSetter implements ISetter {
		private final Method method;
		private final Class<?> type;

		public MethodSetter(Method method) {
			this.method = method;
			type = method.getParameterTypes()[0];
		}

		public void set(Object object, String value) {
			setters.get(type).run(method, object, value);
		}

		private static final HashMap<Class<?>, IThreeParameterRunnable<Method, Object, String>> setters;

		static {
			setters = new HashMap<>();
			setters.put(Boolean.TYPE, new IThreeParameterRunnable<Method, Object, String>() {
				@Override
				public void run(Method parameterOne, Object parameterTwo, String parameterThree) {
					try {
						parameterOne.invoke(parameterTwo, Boolean.parseBoolean(parameterThree));
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			});

			setters.put(Integer.TYPE, new IThreeParameterRunnable<Method, Object, String>() {
				@Override
				public void run(Method parameterOne, Object parameterTwo, String parameterThree) {
					try {
						parameterOne.invoke(parameterTwo, Integer.parseInt(parameterThree));
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			});

			setters.put(String.class, new IThreeParameterRunnable<Method, Object, String>() {
				@Override
				public void run(Method parameterOne, Object parameterTwo, String parameterThree) {
					try {
						parameterOne.invoke(parameterTwo, parameterThree);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}
