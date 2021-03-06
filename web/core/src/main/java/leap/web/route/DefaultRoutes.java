/*
 * Copyright 2013 the original author or authors.
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
package leap.web.route;

import leap.core.annotation.Inject;
import leap.core.web.path.PathTemplateFactory;
import leap.lang.Args;
import leap.lang.collection.ArrayIterator;
import leap.lang.http.HTTP.Method;
import leap.web.Handler;
import leap.web.action.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

public class DefaultRoutes implements Routes {
	
	private @Inject PathTemplateFactory pathTemplateFactory;
	private @Inject ActionManager		actionManager;
	
	private Route[]    array = new Route[]{};
	private Set<Route> set   = new TreeSet<Route>(Route.COMPARATOR);
	
	@Override
    public int size() {
	    return array.length;
    }

	@Override
    public boolean isEmpty() {
	    return array.length == 0;
    }

	@Override
    public Iterator<Route> iterator() {
	    return new ArrayIterator<Route>(array);
    }
	
	@Override
    public RouteConfigurator create() {
		return new DefaultRouteConfigurator((c) -> {
			RouteBuilder rb = createRoute(c.getMethod(), c.getPath(), c.getHandler());
			
			rb.setSupportsMultipart(c.isSupportsMultipart());
			rb.setCorsEnabled(c.getCorsEnabled());
			rb.setCsrfEnabled(c.getCsrfEnabled());
			rb.setHttpsOnly(c.getHttpsOnly());

			Route r = rb.build();
			
			add(r);
			
			return r;
		});
    }

	@Override
    public Routes add(Method method, String path, Runnable handler) {
		Args.notNull(handler, "handler");
		return doAdd(method, path, () -> new RunnableAction(handler));
    }
	
	@Override
    public <T> Routes add(Method method, String path, Supplier<T> handler) {
		Args.notNull(handler, "handler");
		return doAdd(method, path, () -> new SupplierAction(handler));
    }

	@Override
    public Routes add(Method method, String path, Handler handler) {
		Args.notNull(handler, "handler");
		return doAdd(method, path, () -> new HandlerAction(handler));
    }
	
	protected Routes doAdd(Method method, String path, Supplier<Action> action) {
	    return add(createRoute(method, path, action).build());
	}
	
	protected RouteBuilder createRoute(Method method, String path, Supplier<Action> action) {
		return createRoute(method, path, action.get());
	}
	
	protected RouteBuilder createRoute(Method method, String path, Action action) {
		Args.notEmpty(path, "path");
		
		RouteBuilder r = new RouteBuilder();
		
		r.setPathTemplate(pathTemplateFactory.createPathTemplate(path));
		r.setAction(action);
		
		if(null != method) {
			r.setMethod(method.name());
		}else{
			r.setMethod("*");
		}
		
		actionManager.prepareAction(r);
		
	    return r;
	}

	@Override
    public synchronized Routes add(Route route) {
		Args.notNull(route,"route");
		set.add(route);
		this.setNewArray();
	    return this;
    }

	@Override
    public synchronized Routes addAll(Iterable<Route> routes) {
		Args.notNull(routes,"routes");
		for(Route route : routes){
			add(route);
		}
		return this;
    }

	@Override
    public Route match(String method, String path, Map<String,Object> inParameters,  Map<String, String> outVariables) {
		Route[] routes = this.array;
		
		for(int i=0;i<routes.length;i++){
			Route route = routes[i];
			
			if(null == method || route.getMethod().equals("*") || route.getMethod().equals(method)){
				
				if(!matchRequiredParameters(route.getRequiredParameters(), inParameters)){
					continue;
				}
				
				if(route.getPathTemplate().match(path, outVariables)){
					return route;
				}
			}
		}
		
		return null;
    }
	
	protected boolean matchRequiredParameters(Map<String, String> requiredParameters,Map<String, Object> inParameters) {
		if(requiredParameters.isEmpty()){
			return true;
		}
		
		for(Entry<String, String> entry : requiredParameters.entrySet()){
			
			Object v = inParameters.get(entry.getKey());
			
			if(null == v || !(v instanceof String)){
				return false;
			}
			
			if(!((String)v).equals(entry.getValue())){
				return false;
			}
		}
		
		return true;
	}

	protected void setNewArray(){
		this.array = set.toArray(new Route[set.size()]);
	}
}