package org.bndtools.service.mosquitto;

import org.bndtools.service.packager.PackagerStandardProperties;

import aQute.bnd.annotation.metatype.Meta;



public interface MosquittoProperties extends PackagerStandardProperties {
	
	@Meta.AD(required = false, deflt = "1883")
	int port();
	
}
