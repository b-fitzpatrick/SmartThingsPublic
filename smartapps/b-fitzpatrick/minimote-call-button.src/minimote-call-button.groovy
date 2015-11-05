/**
 *  Minimote Call Button
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
    name: "Minimote Call Button",
    namespace: "b-fitzpatrick",
    author: "Brian Fitzpatrick",
    description: "Notifies when a Minimote button is pushed or held.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
		input "minimote", "capability.button", title: "Which device?", multiple: false
        input "buttonNum", "number", title: "Which button?", range: "1..4"
        input "pushedOrHeld", "text", title: "When pushed or held?", options: ["pushed", "held"]
        input "message", "text", title: "Message to send?"
        input "phone1", "phone", title: "Send an SMS to this number? (optional)", required: false
        input "phone2", "phone", title: "Send another SMS to this number? (optional)", required: false
        input "ifttt", "text", title: "Fire IFTTT maker event named? (optional)", required: false
        input "ifttt-key", "text", title: "IFTTT maker api key? (if used)", required: false
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
	log.debug "Initialized with button.${pushedOrHeld}"
	subscribe(minimote, "button.${pushedOrHeld}", "buttonHandler")
}

def buttonHandler(evt) {
	log.debug "Running buttonHandler"
    log.debug "evt.data: " + evt.data
    log.debug "compare: " + '{"buttonNumber":' + buttonNum + '}'
	if (evt.data == ('{"buttonNumber":' + buttonNum + '}')) {
    	log.debug "Button ${buttonNum} was ${pushedOrHeld}"
        if (phone1) {
        	sendSms(phone1, message)
        }
        if (phone2) {
        	sendSms(phone2, message)
        }
        if (ifttt && ifttt-key) {
        	activateCall()
        	runIn(30, "activateCall")
        }
    }
}

def activateCall() {
	try {
        httpPost("https://maker.ifttt.com/trigger/${ifttt}/with/key/${ifttt-key}", "foo=bar") { resp ->
            log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.debug "Error sending http POST: $e"
    }
}