BrowserKit: Web Applications on the Desktop
===========================================

BrowserKit is a simple framework for developing desktop applications using Web technologies in OSGi. It wraps a native browser control to render and execute web content in a desktop window.

The BrowserKit framework performs the following very simple operations:

* Waits for Endpoint service(s) to be published in the OSGi service registry, with a "uri" property matching either `http:*` or `https:*`.
* Opens a browser window for each Endpoint service, connected to the specified URL.
* When the last Endpoint service is unregistered, or when the last browser window is closed by the user, terminates the application.

Application bundles have zero dependencies on BrowserKit. Therefore it is entirely possible to reuse exactly the same bundles in the desktop and in a "real" server-side web application.


Motivation
----------

Modern web technologies -- HTML5 and JavaScript -- are powerful enough for almost any purpose, and many Java developers are familiar with using these technologies to develop applications for the web. However it is sometimes necessary to deploy standalone applications to the desktop, and existing approaches to developing such applications with Java (e.g. Swing, JavaFX, SWT, AWT) are substantially different from HTML5/JS and require learning a new, large and complex API.

BrowserKit simply allows developers to carry over their web skills to desktop applications. It is also possible to reuse functionality in both the desktop and server, though this requires bundles that are designed to work correctly in both environments.


Developing with BrowserKit
--------------------------

Note that BrowserKit does *not* include an HttpService or any other kind of web server. There are many and various ways of developing web applications in OSGi, and BrowserKit does not tie you to any specific way.

Therefore to develop an application, it is your responsibility to ensure a web server is running. For example you could deploy an existing bundle such as an HttpService implementation. Or you could use a Web Container such as Gemini Web. Or you could simply embed Jetty, Netty or some other HTTP engine directly in your bundle... it's up to you! Once the web server is started you need to publish its existence using the `org.bndtools.service.endpoint.Endpoint` interface. For example:

        Properties props = new Properties();
        props.put(Endpoint.URI, "https://localhost:8081/example");
        context.registerService(Endpoint.class.getName(), new Endpoint() {}, props);

N.B.: you should always try to use a dynamically assigned, free TCP port. If a static port is used then your application may conflict with other services on the computer, and with other BrowserKit-based applications.
