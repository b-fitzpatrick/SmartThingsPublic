/**
 *  Notify of Open Sensors When Entering Away Mode
 *
 *  Copyright 2015 Brian Fitzpatrick
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
 */
definition(
    name: "Notify of Open Sensors When Entering Away Mode",
    namespace: "b-fitzpatrick",
    author: "Brian Fitzpatrick",
    description: "Subscribes to the 'Away' mode. When entering 'Away' mode, checks each sensor and sends a notification if it is open.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Check these sensors:") {
    	input "sensors", "capability.contactSensor", multiple: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(location, "mode", checkSensors)
}

def checkSensors(evt) {
	log.debug "Mode changed"
    if (location.currentMode.name == "Away") {
        log.debug "Entered 'Away' mode, checking sensors"
        def openSensor = false
        sensors.each {
            log.debug "Checking sensor ${it.name}: ${it.currentContact}"
            if (it.currentContact != "closed") {
                sendPush("Away: ${it.name} is open.")
                openSensor = true
            }
        }
    }
}