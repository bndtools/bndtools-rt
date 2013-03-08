package org.bndtools.rt.timer.test.b1;

import java.util.Timer;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

/**
 * Bundle requesting a timer service
 *
 */
@Component
public class B1 {
	
	Timer timer;
	
	@Reference
	void setTimer(Timer timer) {this.timer = timer;}
	
}
