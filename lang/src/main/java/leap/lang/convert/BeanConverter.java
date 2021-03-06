/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.lang.convert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import leap.lang.Objects2;
import leap.lang.Out;
import leap.lang.annotation.Name;
import leap.lang.beans.BeanType;
import leap.lang.beans.BeanProperty;
import leap.lang.reflect.ReflectClass;
import leap.lang.reflect.ReflectConstructor;
import leap.lang.reflect.ReflectParameter;
import leap.lang.serialize.Serialize;
import leap.lang.serialize.Serializer;
import leap.lang.serialize.Serializes;

@SuppressWarnings({"rawtypes"})
public class BeanConverter extends AbstractConverter<Object>{

	@Override
    public boolean convertFrom(Object value, Class<?> targetType, Type genericType, Out<Object> out) throws Throwable {
		if(Modifier.isAbstract(targetType.getModifiers()) || Modifier.isInterface(targetType.getModifiers())){
			return false;
		}
		
		if(value instanceof Map){
			out.set(convertFromMap(targetType, genericType, (Map)value));
			return true;
		}
		
		return false;
    }
	
	@Override
    public boolean convertTo(Object value, Class<?> targetType, Type genericType, Out<Object> out) throws Throwable {
		if(Map.class.isAssignableFrom(targetType)){
			out.set(convertToMap(value));
			return true;
		}
		return false;
    }

	protected Object convertFromMap(Class<?> targetType, Type genericType,Map map) {
		BeanType bt = BeanType.of(targetType);
		
		@SuppressWarnings("unchecked")
        Object bean = newInstance(bt, map);
		
		for(BeanProperty prop : bt.getProperties()){
			String name = prop.getName();
			
			boolean multiNames = false;
			
			for(Annotation a : prop.getAnnotations()){
				Name nameAnnotation = a.annotationType().getAnnotation(Name.class);
				
				if(null != nameAnnotation){
					name = (String)ReflectClass.of(a.getClass()).getMethod(nameAnnotation.value()).invoke(a);
					multiNames = true;
					break;
				}
			}
			
			Serializer serializer = Serializes.getSerializer(prop.getAnnotation(Serialize.class));
			
	        for(Object entryObject : map.entrySet()){
	            Entry entry = (Entry)entryObject;
	            
	            String key = Objects2.toStringOrEmpty(entry.getKey());
	            
	            if(name.equalsIgnoreCase(key) || (multiNames && prop.getName().equalsIgnoreCase(key))){
		            Object param = entry.getValue();
		            
		            if(null != serializer && null != param && param instanceof String){
		            	param = serializer.tryDeserialize((String)param);
		            }
		            
		            if(prop.isWritable()){
		                prop.setValue(bean, Converts.convert(param,prop.getType(),prop.getGenericType()));
		                break;
		            }
	            }
	        }
		}
		
		return bean;
	}
	
	protected Object newInstance(BeanType bt,Map<String, Object> map){
		ReflectClass cls = bt.getReflectClass();
		
		if(cls.hasDefaultConstructor()){
			return bt.newInstance();
		}else{
			ReflectConstructor c  = cls.getConstructors()[0];
			ReflectParameter[] ps = c.getParameters();
			
			Object[] args = new Object[c.getParameters().length];
			
			for(int i=0;i<args.length;i++){
				ReflectParameter p = ps[i];
				args[i] = Converts.convert(map.get(p.getName()), p.getType(), p.getGenericType());
			}
			
			return c.newInstance(args);
		}
	}

	protected Map<String,Object> convertToMap(Object bean) {
		BeanType beanType = BeanType.of(bean.getClass());
		
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		
		for(BeanProperty prop : beanType.getProperties()){
			if(prop.isReadable()){
				map.put(prop.getName(), prop.getValue(bean));
			}
		}
		
		return map;
	}
}