/*
 * Copyright 2009 Tom Gibara
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.tomgibara.stupp;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.WeakHashMap;

import com.tomgibara.pronto.util.Reflect;

public class StuppType {

	private static final Class<?>[] CONS_PARAMS = new Class<?>[] { InvocationHandler.class };

	private static final WeakHashMap<Definition, StuppType> instances = new WeakHashMap<Definition, StuppType>();
	
	private static ClassLoader nonNullClassLoader(ClassLoader classLoader, Class<?> clss) {
		if (classLoader != null) return classLoader;
		classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader != null) return classLoader;
		if (classLoader == null) classLoader = clss.getClassLoader();
		return classLoader;
	}
	
	//TODO rename static methods?
	
	public static Definition newDefinition(Class<?> clss) {
		return newDefinition(null, clss);
	}
	
	public static Definition newDefinition(Class<?>... classes) {
		return newDefinition(null, classes);
		
	}
	
	public static Definition newDefinition(ClassLoader classLoader, Class<?> clss) {
		classLoader = nonNullClassLoader(classLoader, clss);
		final Class<?> proxyClass = Proxy.getProxyClass(classLoader, clss);
		return new Definition(proxyClass);
	}
	
	public static Definition newDefinition(ClassLoader classLoader, Class<?>... classes) {
		//TODO unpleasant change of behaviour here
		classLoader = nonNullClassLoader(classLoader, StuppType.class);
		final Class<?> proxyClass = Proxy.getProxyClass(classLoader, classes);
		return new Definition(proxyClass);
	}

	//convenience method
	public static StuppType getInstance(Class<?> clss) {
		return newDefinition(clss).getType();
	}
	
	private static StuppType getInstance(Definition def) {
		synchronized (instances) {
			StuppType type = instances.get(def);
			if (type == null) {
				type = new StuppType(def);
				instances.put(def.clone(), type);
			}
			return type;
		}
	}
	
	private final Class<?> proxyClass;

	final HashSet<String> propertyNames;
	final HashMap<Method, String> methodPropertyNames;
	final HashMap<String, Class<?>> propertyClasses;
	final StuppProperties equalityProperties;
	final HashMap<String, StuppProperties> indexProperties = new HashMap<String, StuppProperties>();
	
	private StuppType(Definition def) {
		proxyClass = def.proxyClass;
		methodPropertyNames = def.methodPropertyNames;
		propertyClasses = def.propertyClasses;
		propertyNames = new HashSet<String>(methodPropertyNames.values());
		equalityProperties = properties(def.equalityProperties);
		for (Map.Entry<String, ArrayList<String>> entry : def.indexProperties.entrySet()) {
			final ArrayList<String> propertyNames = entry.getValue();
			indexProperties.put(entry.getKey(), properties((String[]) propertyNames.toArray(new String[propertyNames.size()])));
		}
	}

	public boolean instanceImplements(Class<?> clss) {
		return clss.isAssignableFrom(proxyClass);
	}
	
	public StuppProperties properties(String... propertyNames) {
		return new StuppProperties(this, propertyNames);
	}

	public StuppProperties getIndexProperties() {
		return indexProperties.get(StuppIndexed.PRIMARY_INDEX_NAME);
	}

	//TODO include index type in annotation
	public Class<? extends StuppIndex<?>> getIndexClass() {
		return getIndexClass(StuppIndexed.PRIMARY_INDEX_NAME);
	}
	
	public StuppProperties getIndexProperties(String indexName) {
		return indexProperties.get(indexName);
	}
	
	//TODO include index type in annotation
	public Class<? extends StuppIndex<?>> getIndexClass(String indexName) {
		return StuppUniqueIndex.class;
	}
	
	public Object newInstance() {
		try {
			return proxyClass.getConstructor(CONS_PARAMS).newInstance(new Object[] { new StuppHandler(this) });
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append('<');
		for (Class<?> clss : proxyClass.getInterfaces()) {
			if (sb.length() > 1) sb.append(", ");
			sb.append(clss.getName());
		}
		sb.append('>');
		return sb.toString();
	}

	// package methods

	Collection<? extends StuppIndex<?>> createIndices() {
		if (indexProperties.isEmpty()) return Collections.emptySet();
		ArrayList<StuppIndex<?>> list = new ArrayList<StuppIndex<?>>(indexProperties.size());
		for (Map.Entry<String, StuppProperties> entry : indexProperties.entrySet()) {
			final StuppIndex<?> index = new StuppUniqueIndex(entry.getValue(), entry.getKey(), true);
			list.add(index);
		}
		return list;
	}
	
	// inner classes
	
	public static class Definition implements Cloneable {
		
		final Class<?> proxyClass;
		final HashMap<Method, String> methodPropertyNames;
		final HashMap<String, Class<?>> propertyClasses;
		String[] equalityProperties = null;
		final HashMap<String, ArrayList<String>> indexProperties = new HashMap<String, ArrayList<String>>();
		
		private Definition(Definition that) {
			this.proxyClass = that.proxyClass;
			this.methodPropertyNames = that.methodPropertyNames;
			this.propertyClasses = that.propertyClasses;
			this.indexProperties.putAll(that.indexProperties);
			this.equalityProperties = that.equalityProperties.clone();
		}
		
		private Definition(Class<?> proxyClass) {
			//generate method property name map and type map
			HashMap<Method, String> methodPropertyNames = new HashMap<Method, String>();
			HashMap<String, Class<?>> propertyClasses = new HashMap<String, Class<?>>();
			final Class<?>[] interfaces = proxyClass.getInterfaces();
			for (Class<?> i : interfaces) {
				for (Method method : i.getMethods()) {
					final boolean setter = Reflect.isSetter(method);
					final boolean getter = Reflect.isGetter(method);
					if (setter || getter) {
						final String propertyName = Reflect.propertyName(method.getName());
						methodPropertyNames.put(method, propertyName);
						final Class<?> c = setter ? method.getParameterTypes()[0] : method.getReturnType();
						final Class<?> k = propertyClasses.get(propertyName);
						if (c != k) {
							if (k == null) {
								propertyClasses.put(propertyName, c);
							} else {
								boolean cek = k.isAssignableFrom(c);
								boolean kec = c.isAssignableFrom(k);
								if (!cek && !kec) {
									throw new IllegalArgumentException("Incompatible setter/getter types: " + propertyName);
								} else if (getter && cek || setter && kec) {
									throw new IllegalArgumentException("Incompatible setter type too general: " + propertyName);
								} else if (getter) {
									propertyClasses.put(propertyName, c);
								}
							}
						}
					}
				}
			}
			
			//assign values
			this.proxyClass = proxyClass;
			this.methodPropertyNames = methodPropertyNames;
			this.propertyClasses = propertyClasses;
			
			//default other state based on annotations
			processAnnotations();
		}
		
		//TODO introduce index class
		public Definition addIndex(String indexName, String... keyProperties) {
			if (keyProperties == null) throw new IllegalArgumentException("null indexProperties");
			StuppIndex.checkName(indexName);
			if (this.indexProperties.containsKey(indexName)) throw new IllegalArgumentException("duplicate index name: " + indexName);
			
			//check key properties
			final int length = keyProperties.length;
			final HashSet<String> set = new HashSet<String>();
			//TODO doesn't actually check if there is a corresponding setter - does that matter?
			for (int i = 0; i < length; i++) {
				final String property = keyProperties[i];
				if (property == null) throw new IllegalArgumentException("Null key property at index " + i);
				Class<?> clss = propertyClasses.get(property);
				if (clss == null) throw new IllegalArgumentException("Unknown property " + property + " at index " + i);
				if (!set.add(property)) throw new IllegalArgumentException("Duplicate key property " + property + " at index " + i);
			}

			this.indexProperties.put(indexName, new ArrayList<String>(Arrays.asList(keyProperties)));
			return this;
		}
		
		public Definition removeIndex(String indexName) {
			indexProperties.remove(indexName);
			return this;
		}
		
		public Definition setEqualityProperties(String... equalityProperties) {
			if (equalityProperties == null) throw new IllegalArgumentException("null equalityProperties");
			
			final int length = equalityProperties.length;
			final LinkedHashSet<String> set = new LinkedHashSet<String>();
			//TODO doesn't actually check if there is a corresponding setter - does that matter?
			for (int i = 0; i < length; i++) {
				final String property = equalityProperties[i];
				if (property == null) throw new IllegalArgumentException("Null equality property at index " + i);
				if (!propertyClasses.containsKey(property)) throw new IllegalArgumentException("Unknown property " + property + " at index " + i);
				if (!set.add(property)) throw new IllegalArgumentException("Duplicate equality property " + property + " at index " + i);
				
			}

			this.equalityProperties = (String[]) set.toArray(new String[set.size()]);
			return this;
		}

		//TODO consider removing this method
		private void checkComplete() {
		}

		public StuppType getType() {
			checkComplete();
			return getInstance(this);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof Definition)) return false;
			final Definition that = (Definition) obj;
			if (this.proxyClass != that.proxyClass) return false;
			if (!Arrays.equals(equalityProperties, that.equalityProperties)) return false;
			if (!this.indexProperties.equals(that.indexProperties)) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			return proxyClass.hashCode() ^ indexProperties.hashCode() ^ Arrays.hashCode(equalityProperties);
		}
		
		@Override
		protected Definition clone() {
			return new Definition(this);
		}
		
		private void processAnnotations() {
			HashMap<String, ArrayList<Method>> indexMethods = new HashMap<String, ArrayList<Method>>();
			ArrayList<Method> equalityMethods = new ArrayList<Method>();
			final Class<?>[] interfaces = proxyClass.getInterfaces();
			for (Class<?> i : interfaces) {
				for (Method method : i.getMethods()) {
					if (!Reflect.isSetter(method)) continue;
					final StuppIndexed indexed = method.getAnnotation(StuppIndexed.class);
					final StuppEquality equality = method.getAnnotation(StuppEquality.class);
					if (indexed != null) {
						final String indexName = indexed.name();
						ArrayList<Method> methods = indexMethods.get(indexName);
						if (methods == null) {
							StuppIndex.checkName(indexName);
							methods = new ArrayList<Method>();
							indexMethods.put(indexName, methods);
						}
						int index = indexed.index();
						final int size = methods.size();
						if (index < 0 || index == size) {
							methods.add(method);
						} else if (index < size) {
							methods.set(index, method);
						} else {
							while (index > size) {
								methods.add(null);
								index --;
							}
							methods.add(method);
						}
					}
					//TODO weaken this
					if (equality != null || indexed != null) {
						equalityMethods.add(method);
					}
				}
			}
			//create key arrays
			for (Map.Entry<String, ArrayList<Method>> entry : indexMethods.entrySet()) {
				final String indexName = entry.getKey();
				final ArrayList<Method> methods = entry.getValue();
				//ensure key properties have no gaps
				while (methods.remove(null));
				{
					final int length = methods.size();
					final ArrayList<String> properties = new ArrayList<String>(length);
					for (int i = 0; i < length; i++) {
						final Method method = methods.get(i);
						final String propertyName = Reflect.propertyName(method.getName());
						properties.add(propertyName);
					}
					this.indexProperties.put(indexName, properties);
				}
			}
			//create equality array
			{
				final int length = equalityMethods.size();
				final String[] equalityProperties = new String[length];
				for (int i = 0; i < length; i++) {
					Method method = equalityMethods.get(i);
					equalityProperties[i] = Reflect.propertyName(method.getName());
				}
				this.equalityProperties = equalityProperties;
			}
		}
		
	}
	
}