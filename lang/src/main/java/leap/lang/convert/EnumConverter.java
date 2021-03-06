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

import java.lang.reflect.Type;

import leap.lang.Enums;
import leap.lang.Out;

public class EnumConverter extends AbstractConverter<Enum<?>> implements Converter<Enum<?>> {

	@SuppressWarnings("unchecked")
	public boolean convertFrom(Object value, Class<?> targetType, Class<?> genericType, Out<Object> out) throws Throwable {
		Enum<?> enumObject = Enums.valueOf((Class<Enum<?>>)targetType, value);
		
		if(null != enumObject){
			out.set(enumObject);
			return true;
		}
		
	    return false;
    }
	
	@Override
    public boolean convertTo(Enum<?> value, Class<?> targetType, Type genericType, Out<Object> out) throws Throwable {
		Object realValue = Enums.getValue(value);
		
	    out.set(Converts.convert(realValue, targetType, genericType));
	    return true;
    }

	@Override
    public String convertToString(Enum<?> value) throws Throwable {
		Object realValue = Enums.getValue(value);
		
		return Converts.toString(realValue);
    }
}