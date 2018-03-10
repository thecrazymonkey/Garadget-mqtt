FROM node
RUN apt update && apt upgrade -y 
ENV TZ=EST5EDT
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
RUN mkdir /usr/src/mqtt2rest
COPY mqtt2rest /usr/src/mqtt2rest/
RUN cd /usr/src/mqtt2rest && npm install
CMD node mqtt2rest.js

