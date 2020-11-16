[![Build Status](https://travis-ci.org/alindhorst/tomcat-persistable-session-manager.svg?branch=master)](https://travis-ci.org/alindhorst/tomcat-persistable-session-manager)
![CodeQL](https://github.com/alindhorst/tomcat-persistable-session-manager/workflows/CodeQL/badge.svg)
# Tomcat Persistable Session Manager
Attempt at creating a SessionManager capable of running distributed web applications by persisting session state to a global backend.

## Project structure
The project consists of several subprojects which are:
* tomcat-persistable-session-manager-shared: Core part, shared with each other subproject
* tomcat-persistable-session-manager-testutils: Utilities used in other subprojects for Unit-Tests, not distributed
* tomcat-persistable-session-manager-riak-backend: The backend for Riak; include this project if you want to store your session data in Riak
* (more tomcat-persistable-session-manager-*-backend to come)

## Configuration

### Mode1: No jvmroute
Example `context.xml`
```
<?xml version="1.0" encoding="UTF-8"?>
<Context path="/sessiontest">
    <Manager
        className="de.alexanderlindhorst.tomcat.session.manager.PersistableSessionManager"
        serviceImplementationClassName="de.alexanderlindhorst.tomcat.session.access.riak.SynchronousRiakService"
        serviceBackendAddress="riak.service.consul:8087"
        sessionExpiryThreshold="30000">
        <SessionIdGenerator className="org.apache.catalina.util.StandardSessionIdGenerator"/>
    </Manager>
</Context>

```

This will use a SessionManager that stores its session data in the Riak backend available under `riak.service.consul`. No JVM route is configured, so the session manager has no ways of knowing if its local data is up to date. Every read will fire back to the Riak backend.


### Mode 2: With jvmroute, but no route rewriting
Example `context.xml`
```
<?xml version="1.0" encoding="UTF-8"?>
<Context path="/sessiontest">
    <Manager
        className="de.alexanderlindhorst.tomcat.session.manager.PersistableSessionManager"
        serviceImplementationClassName="de.alexanderlindhorst.tomcat.session.access.riak.SynchronousRiakService"
        serviceBackendAddress="riak.service.consul:8087"
        sessionExpiryThreshold="30000">
        <SessionIdGenerator className="org.apache.catalina.util.StandardSessionIdGenerator" jvmRoute="myhost"/>
    </Manager>
</Context>
```

This will use a SessionManager that stores its session data in the Riak backend available under `riak.service.consul`. Furthermore it will try to look up any data locally first if the configured `jvmRoute` value is the same as the value found in the session cookie or the session indicated in a link. If these values differ it will **not** rewrite the session cookie.


### Mode 3: With jvmroute and Valve to handle route rewriting
Example `context.xml`
```
<?xml version="1.0" encoding="UTF-8"?>
<Context path="/sessiontest">
    <Manager
        className="de.alexanderlindhorst.tomcat.session.manager.PersistableSessionManager"
        serviceImplementationClassName="de.alexanderlindhorst.tomcat.session.access.riak.SynchronousRiakService"
        serviceBackendAddress="riak.service.consul:8087"
        sessionExpiryThreshold="30000">
        <SessionIdGenerator className="org.apache.catalina.util.StandardSessionIdGenerator" jvmRoute="${HOSTNAME}"/>
    </Manager>
    <Valve className="de.alexanderlindhorst.tomcat.session.valve.AdjustSessionIdToJvmRouteValve"/>
</Context>
```

This will use a SessionManager that stores its session data in the Riak backend available under `riak.service.consul`. Furthermore it will try to look up any data locally first if the configured `jvmRoute` value is the same as the value found in the session cookie or the session indicated in a link. The Valve configured here will try to find local session. If configured `jvmRoute` and jvm route value as found in the current request are the same this is the same as Mode 2. If they differ, before the request is passed on to any other logic, the session data will be fetched from the backend and the `route` part of the session cookie will be updated. If cookies are not used then the `Response.encodeUrl` method will encode all links to point to a local session. Thus all follow up can be routed to the same instance and benefit from locally available data.
