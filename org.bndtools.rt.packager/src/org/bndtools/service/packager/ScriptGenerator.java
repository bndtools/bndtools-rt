package org.bndtools.service.packager;

import java.io.File;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * @since 1.1
 */
@ProviderType
public interface ScriptGenerator {
	
	String generate(Map<String, Object> properties, File data);
	
}
