# Garadget connector for SmartThings using MQTT

Using a bridge:
mqtt2rest -- An MQTT to REST Bridge for Garadget


Usage:
    # run bridge
    node mqtt2rest.js

Diagram:

    ----------------------------    ---------------    -------------     ---------------
    | Garadget (MQTT Client)   |<-->| MQTT Broker |<-->| mqtt2rest |<--->| SmartThings |
    ----------------------------    ---------------    -------------     ---------------

.. and SmartThings composite DTH

1. Install and configure MQTT broker
2. Setup mqttrest - can be run standalone or on RB Pi within a Docker container, Dockerfile is included

    config.yml contents:
    
        mqtt:
          host: "192.168.1.39"  <------- host of MQTT broker
          port: 1883            <------- port of MQTT broker
          doors: ["Garage1", "Garage2"]   <------- door names that are configured in Garadget app - not mandatory
          prefix: "garadget"    <------- prefix used for Garadget topics
        http:
          host: localhost       <------- IP address of the network host this application is running on
          port: 8081            <------- port of the network host this application is running on
        application:
          logLevel: "debug"
          logLevelHttp: "info"  <------- - log level to be used by the express

    To "Dockerize this":
    
        a. Go to the folder with the Dockerfile
        b. Run >docker build .
        c. Tag the image using >docker tag <id of built image> garadget-mqttrest
        d. Run the container  >docker run -d --name mqtt2rest --network=host --restart=always -w /usr/src/mqtt2rest garadget-mqtt2rest
        e. Check the logs >docker logs mqtt2rest
    To use pm2 (assuming pm2 is installed and integrated with systemd for automated startup - refer to pm2 user guide):
    
        a. Go to the folder with the mqtt2rest.js file
        b. execute the following "pm2 start ecosystem.config.js"
        c. check logs using "pm2 logs mqtt2rest"
3. Verify in mqttrest log that you're getting updates from the MQTT broker Garadget topics
4. Install parent (garadget-mqtt.groovy) and child (garadget-mqtt-door.groovy) DTH via Github integration or via code in the ST IDE
4. In the IDE (easiest way) manually a device - make sure that the Device Network Id matches the mac address of the network port
that mqttrest.js uses
![alt text](https://github.com/thecrazymonkey/Garadget-mqtt/blob/master/pics/dthcreate.png)
5. Once created the new device needs additional configuration - again can be provided via Web IDE
    a. MQTT2REST IP address
    b. MQTT2REST port
    c. MQTT2REST MAC address
6. Once configured hit a refresh button on the newly created device in Things list
7. The devuce should then query the mqttrest process and create child devices for all detected doors
8. Individual doors should be listed in Things

