/*
 * Copyright 2015 the original author or authors.
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
package leap.web.api.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import leap.lang.Args;
import leap.lang.Arrays2;
import leap.lang.Collections2;
import leap.lang.naming.NamingStyle;
import leap.lang.path.Paths;
import leap.web.api.meta.OAuth2Scope;
import leap.web.route.Route;

public class DefaultApiConfig implements ApiConfig, ApiConfigurator {
	
	protected final String name;
	protected final String basePath;

    protected String        title;
    protected String        summary;
    protected String        description;
    protected String        version                     = DEFAULT_VERSION;
    protected String[]      protocols;
    protected String[]      produces;
    protected String[]      consumes;
    protected boolean       corsEnabled                 = true;
    protected boolean       oAuthEnabled                = false;
    protected String        oAuthAuthzEndpointUrl;
    protected String        oAuthTokenEndpointUrl;
    protected OAuth2Scope[] oAuthScopes;
    protected NamingStyle   parameterNamingStyle;
    protected NamingStyle   propertyNamingStyle;
    protected Set<String>   removalModelNamePrefixes    = new HashSet<String>();
    protected Set<String>   removalModelnamePrefixesImv = Collections.unmodifiableSet(removalModelNamePrefixes);
    protected Set<Route>    routes                      = new HashSet<>();
    protected Set<Route>    routesImv                   = Collections.unmodifiableSet(routes);
	
	public DefaultApiConfig(String name, String basePath) {
		Args.notEmpty(name, "name");
		Args.notEmpty(basePath, "basePath");
		Args.assertTrue(basePath.startsWith("/"), "The base path must be leading with a slash '/'");
		this.name     = name;
		this.title    = name;
		this.basePath = Paths.suffixWithoutSlash(basePath);
	}
	
	@Override
	public ApiConfig config() {
		return this;
	}

	@Override
	public String getBasePath() {
		return basePath;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
    public String getSummary() {
	    return summary;
    }

	@Override
    public String getDescription() {
	    return description;
    }

	@Override
    public String getTitle() {
	    return title;
    }
	
	@Override
    public String getVersion() {
	    return version;
    }

	public String[] getProtocols() {
		return protocols;
	}

	@Override
    public String[] getProduces() {
	    return produces;
    }

	@Override
    public String[] getConsumes() {
	    return consumes;
    }
	
	public boolean isCorsDisabled() {
		return corsEnabled;
	}
	
	@Override
    public boolean isOAuthEnabled() {
	    return oAuthEnabled;
    }

	@Override
    public Set<Route> getRoutes() {
	    return routesImv;
    }
	
	public NamingStyle getParameterNamingStyle() {
		return parameterNamingStyle;
	}
	
	public NamingStyle getPropertyNamingStyle() {
		return propertyNamingStyle;
	}
	
	public Set<String> getRemovalModelNamePrefixes() {
		return removalModelnamePrefixesImv;
	}
	
	public ApiConfigurator setTitle(String title) {
		Args.notEmpty(title, "title");
		this.title = title;
		return this;
	}
	
	@Override
    public ApiConfigurator setSummary(String s) {
		this.summary = s;
	    return this;
    }

	@Override
    public ApiConfigurator setDescription(String s) {
		this.description = s;
	    return this;
    }

	public ApiConfigurator setVersion(String v) {
		Args.notEmpty(v, "version");
		this.version = v;
		return this;
	}
	
	public ApiConfigurator setProtocols(String... protocols) {
		this.protocols = null == protocols ? Arrays2.EMPTY_STRING_ARRAY : protocols;
		return this;
	}

	@Override
    public ApiConfigurator setProduces(String... produces) {
		this.produces = null == produces ? Arrays2.EMPTY_STRING_ARRAY : produces;
	    return this;
    }

	@Override
    public ApiConfigurator setConsumes(String... consumes) {
		this.consumes = null == consumes ? Arrays2.EMPTY_STRING_ARRAY : consumes;
		return this;
    }
	
	public ApiConfigurator setParameterNamingStyle(NamingStyle ns) {
		this.parameterNamingStyle = ns;
		return this;
	}
	
	public ApiConfigurator setPropertyNamingStyle(NamingStyle namingStyle) {
		this.propertyNamingStyle = namingStyle;
		return this;
	}

	@Override
    public ApiConfigurator removeModelNamePrefixes(String... prefixes) {
		Collections2.addAll(removalModelNamePrefixes, prefixes);
	    return this;
    }
	
	@Override
    public ApiConfigurator setCorsEnabled(boolean enabled) {
		this.corsEnabled = enabled;
	    return this;
    }
	
	@Override
    public ApiConfigurator setOAuthEnabled(boolean enabled) {
		this.oAuthEnabled = enabled;
	    return this;
    }
	
	@Override
    public ApiConfigurator setOAuthAuthorizationUrl(String url) {
	    this.oAuthAuthzEndpointUrl = url;
        return this;
    }

    @Override
    public ApiConfigurator setOAuthTokenUrl(String url) {
        this.oAuthTokenEndpointUrl = url;
        return this;
    }

    @Override
    public String getOAuthAuthorizationUrl() {
        return oAuthAuthzEndpointUrl;
    }

    @Override
    public String getOAuthTokenUrl() {
        return oAuthTokenEndpointUrl;
    }
    
    @Override
    public ApiConfigurator setOAuthScopes(OAuth2Scope... scopes) {
        this.oAuthScopes = scopes;
        return this;
    }

    @Override
    public OAuth2Scope[] getOAuthScopes() {
        return oAuthScopes;
    }

    @Override
    public ApiConfigurator addRoute(Route route) {
		routes.add(route);
	    return this;
    }
}