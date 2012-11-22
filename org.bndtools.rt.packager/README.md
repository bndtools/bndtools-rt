# The Packager Model
The BndtoolsRT packager is a subsystem that uses OSGi bundles and services to manage
external processes. The executables of the external processes are delivered through
bundles, the 'packages', and then managed by a _Packager Manager_ service. The 
Packager Manager finds _types_ of packages by listening to _Package Type_ services
and _Process Guard_ services. A Process Guard is a service that has the following functions:

* The configuration, through a getProperties method
* The desired life cycle, singled by its presence
* Exploiting the actual state of the external process

Once a matching pair is found (i.e. the package type
specified by the Process Guard is present) the Packager Manager creates the
external process. Since this is happens asynchronously with a surprising large
number of error and edge cases, the Process Guard is informed of the progress,
lack thereof, or regress. For each detected state change, the Packager Manager
informs the Process Guard of the assumed state, reflected in the ProcessGuard.State
enum. If the external process is _alive_, the Process Guard can do whatever it
wants, for example register an additional service reflecting the presence of
the external process. The Package Manager will sends heart beats, called _pings_
to the external process and inform the Process Guard appropriately.

The Packager Manager must ensure the external process is running if a matching
Package Type and Process Guard are registered. If the matching pair disappears,
it must ensure the external process is killed. 


## TODO
* Handle versions in package types
* User management (different in macos/linux/pc)
* rename (Package, Packager and can't we find another name?)
* Lifecyle of Package Type now kills the process, can ignore the unregister but should handle
  the case when a handler is then registered.
* Related, currently the process is killed when the guard goes. However, the WatchDogManager
  can actually leave the process running and reattach (or restart when the configuration has
  changed). 

