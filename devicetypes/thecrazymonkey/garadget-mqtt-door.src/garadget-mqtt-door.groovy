/**
 *  Garadget MQTT door child device
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
 *
 *  Thanks to Stuart Buchanan for the original code for the "Garadget (Connect)"
 *  Thanks to Daniel Ogorchock for base code for child DTHs based on his ST_Anything solution
 */


import groovy.json.JsonSlurper

metadata {
    definition(name: "Garadget MQTT Door", namespace: "thecrazymonkey", author: "Ivan Kunz") {
        capability "Switch"
        capability "Contact Sensor"
        capability "Signal Strength"
        capability "Actuator"
        capability "Sensor"
        capability "Refresh"
        capability "Polling"
        capability "Configuration"
        capability "Garage Door Control"

        attribute "reflection", "string"
        attribute "status", "string"
        attribute "time", "string"
        attribute "lastAction", "string"
        attribute "reflection", "string"
        attribute "ver", "string"

        command "stop"
        command "generateEvent", ["string", "string"]
    }
    preferences {
        input("prdt", "text", title: "sensor scan interval in mS (default: 1000)")
        input("pmtt", "text", title: "door moving time in mS(default: 10000)")
        input("prlt", "text", title: "button press time mS (default: 300)")
        input("prlp", "text", title: "delay between consecutive button presses in mS (default: 1000)")
        input("psrt", "text", title: "reflection threshold below which the door is considered open (default: 25)")
    }
    tiles(scale: 2) {
        multiAttributeTile(name: "status", type: "generic", width: 6, height: 4) {
            tileAttribute("device.status", key: "PRIMARY_CONTROL") {
                attributeState "open", label: '${name}', action: "switch.off", icon: "st.doors.garage.garage-open", backgroundColor: "#ffa81e"
                attributeState "opening", label: '${name}', icon: "st.doors.garage.garage-opening", backgroundColor: "#ffa81e"
                attributeState "closing", label: '${name}', icon: "st.doors.garage.garage-closing", backgroundColor: "#6699ff"
                attributeState "closed", label: '${name}', action: "switch.on", icon: "st.doors.garage.garage-closed", backgroundColor: "#79b821"
            }
            tileAttribute("device.lastAction", key: "SECONDARY_CONTROL") {
                attributeState "default", label: 'Time In State: ${currentValue}'
            }
        }
        standardTile("contact", "device.contact", width: 1, height: 1) {
            state("open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e")
            state("closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821")
        }
        valueTile("reflection", "reflection", decoration: "flat", width: 2, height: 1) {
            state "reflection", label: 'Reflection\r\n${currentValue}%'
        }
        valueTile("rssi", "device.rssi", decoration: "flat", width: 1, height: 1) {
            state "rssi", label: 'Wifi\r\n${currentValue} dBm', unit: "", backgroundColors: [
                    [value: 16, color: "#5600A3"],
                    [value: -31, color: "#153591"],
                    [value: -44, color: "#1e9cbb"],
                    [value: -59, color: "#90d2a7"],
                    [value: -74, color: "#44b621"],
                    [value: -84, color: "#f1d801"],
                    [value: -95, color: "#d04e00"],
                    [value: -96, color: "#bc2323"]
            ]
        }
        standardTile("refresh", "refresh", inactiveLabel: false, decoration: "flat") {
            state "default", action: "polling.poll", icon: "st.secondary.refresh"
        }
        standardTile("stop", "stop") {
            state "default", label: "", action: "stop", icon: "http://cdn.device-icons.smartthings.com/sonos/stop-btn@2x.png"
        }
        valueTile("brightness", "brightness", decoration: "flat", width: 2, height: 1) {
            state "brightness", label: 'Brightness\r\n${currentValue}'
        }
        valueTile("ssid", "ssid", decoration: "flat", width: 2, height: 1) {
            state "ssid", label: 'Wifi SSID\r\n${currentValue}'
        }
        valueTile("ver", "ver", decoration: "flat", width: 1, height: 1) {
            state "ver", label: 'Version\r\n${currentValue}'
        }
        standardTile("configure", "device.button", width: 1, height: 1, decoration: "flat") {
            state "default", label: "", backgroundColor: "#ffffff", action: "configure", icon: "st.secondary.configure"
        }

        main "status"
        details(["status", "contact", "reflection", "ver", "configure", "lastAction", "rssi", "stop", "brightness", "ssid", "refresh"])
    }
}

void poll() {
    log.debug ("Executing - poll()")
    parent.getStatus(device.deviceNetworkId)
    parent.getConfig(device.deviceNetworkId)
}

void refresh() {
    log.debug ("Executing - refresh()")
    poll()
}

def on() {
    log.debug ("Executing - on()")
    openCommand()
}

def off() {
    log.debug ("Executing - off()")
    closeCommand()
}

def openCommand(){
    log.debug "Executing - 'openCommand()'"
    parent.openCommand(device.deviceNetworkId)
}

def closeCommand(){
    log.debug "Executing - 'closeCommand()'"
    parent.closeCommand(device.deviceNetworkId)
}

void open() {
    log.debug ("Executing - open()")
    openCommand()
}

void close() {
    log.debug ("Executing - close()")
    closeCommand()
}

def installed() {
    log.debug ("Executing - installed()")
    parent.getStatus(device.deviceNetworkId)
    parent.getConfig(device.deviceNetworkId)
}

def stop(){
    log.debug "Executing - 'stop()'"
    parent.stopCommand(device.deviceNetworkId)
}

def generateEvent(name, jsonValue) {
    // Update device - complex handler for all responses
    log.debug ("Executing - generateEvent()")
    def slurper = new JsonSlurper()
    def parsed = slurper.parseText(jsonValue)
    log.debug("generateEvent(): '${name}'; '${parsed}'")
    switch (name) {
        case "status":
            sendEvent(name: name, value: parsed.status)
            sendEvent(name: 'contact', value: parsed.status)
            sendEvent(name: 'lastAction', value: parsed.time)
            sendEvent(name: 'reflection', value: parsed.sensor)
            sendEvent(name: 'brightness', value: parsed.bright)
            sendEvent(name: 'rssi', value: parsed.signal)
            break
        case "config":
            sendEvent(name: 'ver', value: parsed.ver, displayed: false);
            sendEvent(name: 'ssid', value: parsed.ssid, displayed: false)
            break
    }
}

def configure() {
    log.debug ("Executing - configure()")
    parent.setConfig(device.deviceNetworkId, prdt, pmtt, prlt, prlp, psrt)
}
