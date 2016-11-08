FROM tomcat:8-jre8

ENV CATALINA_HOME /usr/local/tomcat
WORKDIR $CATALINA_HOME
ADD usr/local/tomcat /usr/local/tomcat

EXPOSE 8080 8000
CMD CATALINA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n $CATALINA_OPTS -DjvmRoute=$HOSTNAME" catalina.sh run

