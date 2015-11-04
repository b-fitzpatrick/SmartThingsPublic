/**
 *  Contact Sensor Left Open
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
    name: "Contact Sensor Left Open",
    namespace: "b-fitzpatrick",
    author: "Brian Fitzpatrick",
    description: "Notifies is a contact sensor is left open for a period of time.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
		input "sensor", "capability.contactSensor", title: "Which sensor?", multiple: false
        input "timeout", "number", title: "Notify if open how many minutes?"
        input "message", "text", title: "Notification text"
        input "button", "capability.button", title: "Button to cancel notification?", multiple: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
    unschedule()
	initialize()
}

def initialize() {
    subscribe(sensor, "contact.open", openHandler)
    subscribe(sensor, "contact.closed", closedHandler)
    subscribe(button, "button.pushed", buttonHandler)
}

def openHandler(evt) {
	state.cancelled = false
    runIn(60*timeout, timeoutHandler)
}

def timeoutHandler(evt) {
	if (!state.cancelled) sendPush(message)
}

def closedHandler(evt) {
	//unschedule()
    state.cancelled = true
}

def buttonHandler(evt) {
	//unschedule()
    state.cancelled = true
}