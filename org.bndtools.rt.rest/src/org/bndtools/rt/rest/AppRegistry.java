package org.bndtools.rt.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Application;

public class AppRegistry {

	private final Map<String, ImmutableApplication> appMap = new HashMap<String, ImmutableApplication>();
	
	public synchronized Application addClassesForAlias(String alias, Collection<Class<?>> classes) {
		ImmutableApplication app = appMap.get(alias);
		if (app == null) {
			app = ImmutableApplication.empty().addClasses(classes);
		} else {
			app = app.addClasses(classes);
		}
		appMap.put(alias, app);
		return app;
	}
	
	public synchronized Application addSingletonsForAlias(String alias, Collection<Object> singletons) {
		ImmutableApplication app = appMap.get(alias);
		if (app == null) {
			app = ImmutableApplication.empty().addSingletons(singletons);
		} else {
			app = app.addSingletons(singletons);
		}
		appMap.put(alias, app);
		return app;
	}
	
	public synchronized Application removeSingletonsForAlias(String alias, Collection<Object> singletons) {
		ImmutableApplication app = appMap.get(alias);
		if (app == null) {
			return null;
		}
		
		app = app.removeSingletons(singletons);
		if (app.isEmpty())
			return null;
		
		return app;
	}
	
}
