metadata {
    definition(name: "Garadget Door", namespace: "thecrazymonkey", author: "Ivan Kunz") {
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
        command "statusCommand"
        command "setConfigCommand"
        command "doorConfigCommand"
        command "netConfigCommand"
    }
    preferences {
        input("prdt", "text", title: "sensor scan interval in mS (default: 1000)")
        input("pmtt", "text", title: "door moving time in mS(default: 10000)")
        input("prlt", "text", title: "button press time mS (default: 300)")
        input("prlp", "text", title: "delay between consecutive button presses in mS (default: 1000)")
        input("psrr", "text", title: "number of sensor reads used in averaging (default: 3)")
        input("psrt", "text", title: "reflection threshold below which the door is considered open (default: 25)")
        input("paot", "text", title: "alert for open timeout in seconds (default: 320)")
        input("pans", "text", title: " alert for night time start in minutes from midnight (default: 1320)")
        input("pane", "text", title: " alert for night time end in minutes from midnight (default: 360)")
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
        valueTile("ip", "ip", decoration: "flat", width: 2, height: 1) {
            state "ip", label: 'IP Address\r\n${currentValue}'
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
        details(["status", "contact", "reflection", "ver", "configure", "lastAction", "rssi", "stop", "ip", "ssid", "refresh"])
    }
}

void on() {
}

void off() {
}

void open() {
}

void close() {
}

def installed() {

}