# Executor bundle

## Description
This bundle provides an implementation of the [*java.util.concurrent.Executor*](http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/Executor.html) interface as an OSGi service. The service instances obtained with this bundle are shared between all the bundles in the framework. Depending on the configuration(s) provided to this bundle via the [*Configuration Admin*](http://www.osgi.org/javadoc/r4v42/org/osgi/service/cm/ConfigurationAdmin.html) service, the underlying thread pool(s) can be a single thread pool, a cached thread pool or a fixed thread pool, whose number of threads can be fixed.

## Benefits
* Shared thread pool across bundles within an OSGi framework
* Lifecycle of the underlying thread pools handled by the framework
* Task submitted by stopped bundle are not getting executed
* Running tasks submitted by a stopping bundle are stopped

## Requirements
Each instance of the service must be configured. The configuration may set:

* *type*: The type of the underlying thread pool (defaults to FIXED), amongst:
	* FIXED
	* SINGLE
	* CACHED
* *size*: The number of threads in the (fixed or cached) thread pool (defaults to 2)