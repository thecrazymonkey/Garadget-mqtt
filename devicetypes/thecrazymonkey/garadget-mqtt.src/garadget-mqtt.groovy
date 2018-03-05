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
    preferences {
        input "ip", "text", title: "MQTT Gateway IP Address", description: "IP Address in form 192.168.1.226", required: true, displayDuringSetup: true
        input "port", "text", title: "MQTT Gateway Port", description: "port in form of 8090", required: true, displayDuringSetup: true
        input "mac", "text", title: "MQTT Gateway MAC Addr", description: "MAC Address in form of 02A1B2C3D4E5", required: true, displayDuringSetup: true
    }
    tiles(scale: 2) {
        childDeviceTiles("all")
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", height: 2, width: 2) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "configure", label:'Configure', action:"configuration.configure", icon:"st.secondary.tools"
        }
        main("configure")
    }
}


// used with ssdp
def sync(ip, port, mac) {
    log.debug "Executing 'sync': ${ip}:${port}:${mac}"
    def existingIp = getDataValue("ip")
    def existingPort = getDataValue("port")
    log.debug "Executing 'sync' existing : ${existingIp}:${existingPort}"
    if (ip && ip != existingIp) {
        updateDataValue("ip", ip)
    }
    if (port && port != existingPort) {
        updateDataValue("port", port)
    }
    def existingMac = getDataValue("mac")
    if (mac && mac!= existingMac) {
        updateDataValue("mac", mac)
    }
}

def refresh() {
    log.debug "Executing 'refresh'"
    getDoors()
}

def configure() {
    log.debug "Resetting configure"
    getDoors()
//    SetConfigCommand()
}

def updateDeviceNetworkID() {
    log.debug "Executing 'updateDeviceNetworkID'"
    if(device.deviceNetworkId!=mac) {
        log.debug "setting deviceNetworkID = ${mac}"
        device.setDeviceNetworkId("${mac}")
    }
    refresh()
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

def getStatus(String dni) {
    log.debug "Executing - getStatus()"
    def jsonbody = new groovy.json.JsonOutput().toJson([path: "/gmqtt/command", body: ["command": "get-status","name": ${dni}]])
    log.debug "Executing - getStatus() - ${jsonbody}"
    doorNotification(jsonbody)
}

def getDoors() {
    log.debug "Executing - getDoors()"
    def jsonbody = new groovy.json.JsonOutput().toJson([path: "/gmqtt/doors", body: {}])
    log.debug "Executing - getDoors() - ${jsonbody}"
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
    configure()
}

private void createChildDevices(List<String> doors) {
    log.debug "Setting doors: '${doors}'"
    def children = getChildDevices()
    def oldDoors = []
    children.each { child ->
        if (doors.contains(child.deviceNetworkId))
            oldDoors.add(child.deviceNetworkId)
    }
    def newDoors = doors.minus(oldDoors)
    log.debug "To create: '${newDoors}'"

    for (String door : newDoors) {
        log.debug "Adding door: '${door}'"
        addChildDevice("Garadget door", "${door}", null, [completedSetup: true, label: "${door} (MQTT)", isComponent: false, componentName: "${door}", componentLabel: "${door} (MQTT)"])
    }
}

// Store the MAC address as the device ID so that it can talk to SmartThings
def setNetworkAddress() {
    // Setting Network Device Id
    if (device.deviceNetworkId != mac) {
        device.deviceNetworkId = mac
        log.debug "Device Network Id set to ${device.deviceNetworkId}"
    }
}

// Parse events from the Bridge
def parse(String description) {
    log.debug "Parsing '${description}'"
    def msg = parseLanMessage(description)
    log.debug "Parsed '${msg}'"
    def receivedData = msg.data
    def receivedJson = msg.json
    def childId = receivedData?.name
    def payloadType = receivedData?.type
    def childInfo = receivedData?.value
    log.debug "childId:'${childId}'; type:'${payloadType}'; info:'${childInfo}'; json:'${receivedJson}'"
    switch (payloadType) {
        case "status":
        case "config":
            def childDevice = null
            childDevices.each {
                if (it.deviceNetworkId == childId) {
                    childDevice = it
                    log.debug "Found a match!!!"
                }
            }
            childDevice.generateEvent(payloadType, receivedJson)
            break
        case "doors":
            // received list of doors
            log.debug "Doors '${receivedJson?.doors}'"
            createChildDevices(receivedJson?.doors)
            break
        default:
            log.error "Unknown request type:'${payloadType}';json:'${receivedJson}'"
    }
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
    log.debug "Sending '${parsed}' to device '${ip}':'${port}'; mac:'${mac}'"

    def hubAction = new physicalgraph.device.HubAction(
            method: "POST",
            path: parsed.path,
            body: parsed.body,
            headers: [
                    HOST          : "$ip:$port",
                    "Content-Type": "application/json"
            ],
            dni: mac
    )
    hubAction
}

private Integer convertHexToInt(hex) {
    return Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    return [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}