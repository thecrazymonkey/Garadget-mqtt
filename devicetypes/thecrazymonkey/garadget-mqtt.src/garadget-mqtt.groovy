/**
 *  Garadget MQTT device
 *
 * 	Author
 *   - ivan.kunz@gmail.com
 *
 *  Copyright 2018
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */

import groovy.json.JsonSlurper
import groovy.json.JsonOutput


metadata {
    definition(name: "Garadget MQTT", namespace: "thecrazymonkey", author: "Ivan Kunz") {
        capability "Switch"
        capability "Contact Sensor"
        capability "Signal Strength"
        capability "Actuator"
        capability "Sensor"
        capability "Refresh"
        capability "Polling"
        capability "Configuration"
        capability "Garage Door Control"
    }
    simulator {
    }
    tiles {
        valueTile("basic", "device.ip", width: 3, height: 2) {
            state("basic", label:'OK')
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'Refresh', action: "refresh.refresh", icon: "st.secondary.refresh-icon"
        }
        main "refresh"
    }

}

def sync(ip, port) {
    def existingIp = getDataValue("ip")
    def existingPort = getDataValue("port")
    if (ip && ip != existingIp) {
        updateDataValue("ip", ip)
    }
    if (port && port != existingPort) {
        updateDataValue("port", port)
    }
}

def refresh() {
    log.debug "Executing 'refresh'"
    getDoors()
}

def configure() {
    log.debug "Resetting Sensor Parameters to SmartThings Compatible Defaults"
//    SetConfigCommand()
}

private getDeviceDetails() {
    def fullDni = device.deviceNetworkId
    return fullDni
}

def on(String dni) {

    log.debug("Executing - on()")
    openCommand(dni)
}

def off(String dni) {

    log.debug("Executing - off()")
    closeCommand(dni)
}

def stop(String dni) {
    log.debug "Executing - stop() - 'sendCommand.setState'"
    def jsonbody = new groovy.json.JsonOutput().toJson(arg: "stop")

    doorNotification("setState", [jsonbody])
}

def statusCommand(String dni) {
    log.debug "Executing - statusCommand() - 'sendCommand.statusCommand'"
    doorNotification("doorStatus", [])
}

def getDoors() {
    log.debug "Executing - getDoors()"
    def jsonbody = new groovy.json.JsonOutput().toJson(path: "/gmqtt/doors")
    doorNotification(jsonbody)
}

def openCommand(String dni) {
    log.debug "Executing - openCommand() - 'sendCommand.setState'"
    def jsonbody = new groovy.json.JsonOutput().toJson(arg: "open")
    doorNotification("setState", [jsonbody])
}

def closeCommand(String dni) {
    log.debug "Executing - closeCommand() - 'sendCommand.setState'"
    def jsonbody = new groovy.json.JsonOutput().toJson(arg: "close")
    doorNotification("setState", [jsonbody])
}

def open(String dni) {
    log.debug "Executing - open() - 'on'"
    on(dni)
}

def close(String dni) {
    log.debug "Executing - close() - 'off'"
    off(dni)
}

def doorConfigCommand() {
    log.debug "Executing doorConfigCommand() - 'sendCommand.doorConfig'"
    doorNotification("doorConfig", [])
}


def netConfigCommand() {
    log.debug "Executing 'sendCommand.netConfig'"
    doorNotification("netConfig", [])
}


def installed() {
    log.debug "Called installed"
//    createChildDevices()
//    response(refresh() + configure())
}

private void createChildDevices() {
    // send query to the bridge to find garage doors list
    // create devices per response
//    def json = new groovy.json.JsonOutput().toJson([
//            path: "/doors",
//    ])
//    log.debug "Getting dooors: ${json}"
//    deviceNotification(json)
    log.debug("creating child devices")
    for (i in 1..2) {
        addChildDevice("Garadget door", "${device.deviceNetworkId}-${i}", null, [completedSetup: true, label: "${device.displayName} (CH${i})", isComponent: true, componentName: "ch$i", componentLabel: "Channel $i"])
    }
}

// Store the MAC address as the device ID so that it can talk to SmartThings
def setNetworkAddress() {
    // Setting Network Device Id
    def hex = "$settings.mac".toUpperCase().replaceAll(':', '')
    if (device.deviceNetworkId != "$hex") {
        device.deviceNetworkId = "$hex"
        log.debug "Device Network Id set to ${device.deviceNetworkId}"
    }
}

// Parse events from the Bridge
def parse(String description) {
    setNetworkAddress()

    log.debug "Parsing '${description}'"
    def msg = parseLanMessage(description)
    log.debug "Pared '${msg}'"

//    return createEvent(name: "message", value: new JsonOutput().toJson(msg.data))
}

// Send message to the Bridge
def doorNotification(message) {
    if (device.hub == null) {
        log.error "Hub is null, must set the hub in the device settings so we can get local hub IP and port"
        return
    }

    log.debug "Sending '${message}' to device"
    setNetworkAddress()

    def slurper = new JsonSlurper()
    def parsed = slurper.parseText(message)

    parsed.body.callback = "http://" + device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")

    def hubAction = new physicalgraph.device.HubAction(
            method: "POST",
            path: parsed.path,
            body: parsed.body,
            headers: [
                    HOST          : "$ip:$port",
                    "Content-Type": "application/json"
            ]
    )
    hubAction
}