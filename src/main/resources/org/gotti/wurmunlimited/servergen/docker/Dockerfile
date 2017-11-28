ARG baseimage
FROM $baseimage

ARG servername=test
COPY $servername $SERVERSDIR/$servername

COPY mods $DATADIR/mods

ENV SERVERNAME=$servername
ENTRYPOINT ["/bin/bash", "-c", "/wurmunlimited/launcher.sh $SERVERNAME"]
WORKDIR /wurmunlimited

VOLUME $SERVERSDIR
VOLUME $DATADIR
