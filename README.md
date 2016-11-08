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
