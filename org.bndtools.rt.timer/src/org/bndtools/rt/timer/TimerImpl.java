package org.bndtools.rt.timer;

import aQute.bnd.annotation.component.*;

/**
 * Provides java.util.Timer as a service in an OSGi environment.
 * 
 * This service is instantiated once for every requesting bundle by the service
 * factory. When the requesting bundle ungets this service (either explicitly 
 * or due to this bundle's leaving the active state), the underlying timer
 * is canceled and the service instance can be reclaimed by the garbage
 * collector. 
 * 
 * @see <a href="http://docs.oracle.com/javase/1.5.0/docs/api/java/util/Timer.html">java.util.Timer</a> 
 */
@Component(provide = java.util.Timer.class, servicefactory = true)
public class TimerImpl extends java.util.Timer {

	@Deactivate
	void deactivate() {
		super.cancel();
	}

}
