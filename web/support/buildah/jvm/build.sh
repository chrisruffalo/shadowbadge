#!/bin/bash

# work as root for install steps
buildah config --user root ${CONTAINER}

# copy in jvm artifacts
mkdir -p ${MOUNTPOINT}/deployments
cp -r quarkus-app/* ${MOUNTPOINT}/deployments/
mv ${MOUNTPOINT}/deployments/quarkus-run.jar ${MOUNTPOINT}/deployments/app.jar

# set permissions for arjuna transactions
buildah run ${CONTAINER} chown -R jboss:0 /deployments

# configure image
buildah config --env JAVA_OPTIONS="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager" \
               --env AB_ENABLED=jmx_exporter \
               --user jboss \
               --cmd  "/deployments/run-java.sh" \
               ${CONTAINER}