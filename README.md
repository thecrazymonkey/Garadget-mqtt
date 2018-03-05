# mqtt2rest -- An MQTT to REST Bridge for Garadget


Usage:
    # run bridge
    node mqtt2rest.js

Diagram:

    ----------------------------    ---------------    -------------     ---------------
    | Garadget (MQTT Client)   |<-->| MQTT Broker |<-->| mqtt2rest |<--->| SmartThings |
    ----------------------------    ---------------    -------------     ---------------


1. Install and configure MQTT broker
2. Setup mqttrest - can be run standalone or on RB Pi within a Docker container, Dockerfile is included
3. Verify in mqttrest log that you're getting updates from the MQTT broker Garadget topics
4. Install parent (garadget-mqtt.groovy) and child (garadget-mqtt-door.groovy) DTH via Github integration or via code in the ST IDE
4. In the IDE (easiest way) manually a device - make sure that the Device Network Id matches the mac address of the network port
that mqttrest.js uses
5. Once created the new device should query the mqttrest process and create child devices for all detected doors
6. Individual doors should be listed in Things

