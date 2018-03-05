/**
 *  Garadget MQTT device - parent gateway
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

metadata {
    definition(name: "Garadget MQTT", namespace: "thecrazymonkey", author: "Ivan Kunz") {
        capability "Refresh"
        capability "Polling"
    }
    simulator {
    }
    preferences {
        input "ip", "text", title: "MQTT Gateway IP Address", description: "IP Address in form 192.168.1.226", required: true, displayDuringSetup: true
        input "port", "text", title: "MQTT Gateway Port", description: "port in form of 8090", required: true, displayDuringSetup: true
        input "mac", "text", title: "MQTT Gateway MAC Addr", description: "MAC Address in form of 02A1B2C3D4E5", required: true, displayDuringSetup: true
    }
    tiles(scale: 2) {
//        childDeviceTiles("all")
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", height: 2, width: 2) {
            state "default", label: 'Refresh', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        main("refresh")
    }
}


// used with ssdp - TODO fixing if ssdp is going to be used
def sync(ip, port, mac) {
    log.debug "Executing 'sync()': ${ip}:${port}:${mac}"
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

def getStatus(String dni) {
    log.debug "Executing - 'getStatus()'"
    def json = new groovy.json.JsonOutput().toJson([
            path: "/gmqtt/command",
            body: [
                    "command": "get-status",
                    "name": "${dni}"
            ]
    ])
    log.debug "Executing - 'getStatus()' - '${json}'"
    doorNotification(json)
}

def getConfig(String dni) {
    log.debug "Executing - 'getConfig()'"
    def json = new groovy.json.JsonOutput().toJson([
            path: "/gmqtt/command",
            body: [
                    "command": "get-config",
                    "name": "${dni}"
            ]
    ])
    log.debug "Executing - 'getConfig()' - '${json}'"
    doorNotification(json)
}

def getDoors() {
    log.debug "Executing - 'getDoors()'"
    def json = new groovy.json.JsonOutput().toJson([
            path: "/gmqtt/doors",
            body: [
                    "callback": "http://" + device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
            ]
    ])
    log.debug "Executing - 'getDoors()' - '${json}'"
    doorNotification(json)
}

def setConfig(String dni, prdt, pmtt, prlt, prlp, psrt){
    def crdt = prdt ?: 1000
    def cmtt = pmtt ?: 10000
    def crlt = prlt ?: 300
    def crlp = prlp ?: 1000
    def csrt = psrt ?: 25
    log.debug "Executing - 'setConfig()'"
    def json = new groovy.json.JsonOutput().toJson([
            path: "/gmqtt/set-config",
            body: [
                    "name": "${dni}",
                    "value": [
                        "rdt": "${crdt}",
                        "mtt": "${cmtt}",
                        "rlt": "${crlt}",
                        "rlp": "${crlp}",
                        "srt": "${csrt}"
                    ]
            ]
    ])
    log.debug "Executing - 'setConfig()' - '${json}'"
    doorNotification(json)
}


def openCommand(String dni) {
    log.debug "Executing - 'openCommand()'"
    def json = new groovy.json.JsonOutput().toJson([
            path: "/gmqtt/command",
            body: [
                    "command": "open",
                    "name": "${dni}"
            ]
    ])
    log.debug "Executing - 'setConfig()' - '${json}'"
    doorNotification(json)
}

def closeCommand(String dni) {
    log.debug "Executing - 'closeCommand()'"
    def json = new groovy.json.JsonOutput().toJson([
            path: "/gmqtt/command",
            body: [
                    "command": "close",
                    "name": "${dni}"
            ]
    ])
    doorNotification(json)
}


def installed() {
    log.debug "Executing - 'installed()'"
    configure()
}

private void createChildDevices(List<String> doors) {
    log.debug "Executing - 'createChildDevices()'"
    def children = getChildDevices()
    def oldDoors = []
    children.each { child ->
        if (doors.contains(child.deviceNetworkId))
            oldDoors.add(child.deviceNetworkId)
    }
    def newDoors = doors.minus(oldDoors)
    log.debug "createChildDevices():To create: '${newDoors}'"

    for (String door : newDoors) {
        log.debug "createChildDevices():Adding door: '${door}'"
        addChildDevice("Garadget MQTT door", "${door}", null, [completedSetup: true, label: "${door} (MQTT)", isComponent: false, componentName: "${door}", componentLabel: "${door} (MQTT)"])
    }
}

// Store the MAC address as the device ID so that it can talk to SmartThings
def setNetworkAddress() {
    // Setting Network Device Id
    if (device.deviceNetworkId != mac) {
        device.deviceNetworkId = mac
        log.debug "setNetworkAddress():Device Network Id set to ${device.deviceNetworkId}"
    }
}

// Parse events from the Bridge
def parse(String description) {
    def results = []
    log.debug "Executing - 'parse()'"
    def msg = parseLanMessage(description)
    log.debug "parse():Parsed '${msg}'"
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
                }
            }
            childDevice.generateEvent(payloadType, receivedJson?.value)
            break
        case "doors":
            // received list of doors
            log.debug "parse():Doors '${receivedJson?.doors}'"
            createChildDevices(receivedJson?.doors)
            break
        default:
            // ignore anything else
            break
    }
    return results
}

// Send message to the Bridge
def doorNotification(message) {
    if (device.hub == null) {
        log.error "doorNotification():Hub is null, must set the hub in the device settings so we can get local hub IP and port"
        return
    }

    log.debug "Executing - 'doorNotification()'"
    setNetworkAddress()

    def slurper = new JsonSlurper()
    def parsed = slurper.parseText(message)
    log.debug "doorNotification():Sending '${parsed}' to device '${ip}':'${port}'; mac:'${mac}'"

    sendHubCommand(new physicalgraph.device.HubAction(
            method: "POST",
            path: parsed.path,
            body: parsed.body,
            headers: [
                    "HOST"        : "$ip:$port",
                    "Content-Type": "application/json"
            ],
            dni: mac
    ))
}
