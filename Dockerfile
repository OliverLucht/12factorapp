## Create image from the Liberty Core image
FROM websphere-liberty:webProfile7

## set the maintainer
MAINTAINER oliver.lucht@de.ibm.com
LABEL maintainer "oliver.lucht@de.ibm.com"

## Copy local server.xml (with endpoint definition and *hosts) file to the config directory in the container
COPY ./12-factor-wlpcfg/servers/12FactorAppServer/server.xml /config/
## COPY ./12-factor-application/image.png /config/dropins/


## could be deleted?
ARG REPOSITORIES_PROPERTIES=""

## Use Liberty installUtilitiy to install some addones and accecpt the Liberty license
RUN if [ ! -z $REPOSITORIES_PROPERTIES ]; then echo $REPOSITORIES_PROPERTIES > /opt/ibm/wlp/etc/repositories.properties; fi \
    && installUtility install --acceptLicense appSecurityClient-1.0 javaee-7.0 javaeeClient-7.0 \
    && if [ ! -z $REPOSITORIES_PROPERTIES ] ; then rm /opt/ibm/wlp/etc/repositories.properties; fi \
    && rm -rf /output/workarea /output/logs

## Copy local .war file to the config/dropins directory in the container
ADD ./12-factor-wlpcfg/servers/12FactorAppServer/apps/12-factor-application.war /config/dropins/

## Set some environment vars in the container
ENV PATH=/opt/ibm/wlp/bin:$PATH
ENV LOG_DIR=/logs
ENV WLP_OUTPUT_DIR=/opt/ibm/wlp/output
ENV dbPassword=770a564648417076f49c7bb0275fbe7b1157aa842894ddb21bf208c74a948279
ENV dbUrl=https://0b563b02-1e03-481d-863d-c32a477aef8f-bluemix:770a564648417076f49c7bb0275fbe7b1157aa842894ddb21bf208c74a948279@0b563b02-1e03-481d-863d-c32a477aef8f-bluemix.cloudant.com
ENV dbUsername=0b563b02-1e03-481d-863d-c32a477aef8f-bluemix

## Expose / open port 9082 which is the port the microservices uses
EXPOSE 9082

## Start the web server
#CMD ["/opt/ibm/docker/docker-server", "run", "defaultServer"]
CMD ["/opt/ibm/wlp/bin/server", "run", "defaultServer"]
