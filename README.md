# mqtt2rest -- An MQTT to REST Bridge for Garadget

Allows connecting traditional REST web services with MQTT.

Usage:
    # run bridge
    node mqtt2rest.js

Diagram:

    ----------------------------    ---------------    -------------     ---------------
    | Garadget (MQTT Client)   |----| MQTT Broker |----| mqtt2rest |-----| SmartThings |
    ----------------------------    ---------------    -------------     ---------------

