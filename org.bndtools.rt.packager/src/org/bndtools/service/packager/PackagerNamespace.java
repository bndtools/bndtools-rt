package org.bndtools.service.packager;

import org.osgi.resource.Namespace;

public class PackagerNamespace extends Namespace {

	/**
	 * <p>This is the namespace used by implementations of the {@link PackagerManager}
	 * to indicate that they provide a packager implementation, or by {@link PackageType}
	 * and {@link ProcessGuard} implementations to indicate that they require a 
	 * packager implementation
	 * </p>
	 * 
	 * <p>When providing a packager implementation the provided capability should specify
	 * a uses constraint for the org.bndtools.service.packager package. For example:
	 * </p>
	 * <pre>Provide-Capability: packager.manager;uses:="org.bndtools.service.packager";effective:="active"</pre>
	 */
	public static final String PACKAGER_MANAGER_NAMESPACE = "packager.manager";
	
	/**
	 * This is the namespace used by implementations of {@link PackageType} to indicate
	 * that they provide a packaged function, or by implementations of {@link ProcessGuard}
	 * to indicate that they require a suitable {@link PackageType} implementation.
	 * 
	 * <p>When providing a packager package type the provided capability should specify
	 * a uses constraint for the org.bndtools.service.packager package. For example:
	 * </p>
	 * <pre>Provide-Capability: packager.type;uses:="org.bndtools.service.packager";
	 *  effective:="active";package.type="myType";version:Version="1.2.3"</pre>
	 */
	public static final String PACKAGER_PACKAGE_TYPE_NAMESPACE = "packager.type";
	
	/**
	 * This is the namespace used by implementations of {@link ProcessGuard} to indicate that
	 * they can configure and manage a packaged function. This can also be used by bundles
	 * to indicate that they require the third party function
	 */
	public static final String PACKAGER_PROCESS_GUARD_NAMESPACE = "packager.guard";
	
	/**
	 * The version attribute is used by both the {@link #PACKAGER_PACKAGE_TYPE_NAMESPACE}
	 * and the {@link #PACKAGER_PROCESS_GUARD_NAMESPACE} to specify the version of the 
	 * packaged function that they support.
	 */
	public static final String CAPABILITY_VERSION_ATTRIBUTE = "version";
	
	/**
	 * The package type attribute is used by both the {@link #PACKAGER_PACKAGE_TYPE_NAMESPACE}
	 * and the {@link #PACKAGER_PROCESS_GUARD_NAMESPACE} to indicate the name of the packaged
	 * function.
	 */
	public static final String CAPABILITY_PACKAGE_TYPE_ATTRIBUTE = "package.type";
}
