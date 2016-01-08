/**
 *  Thermostat Fan Recirculate
 *
 *  Copyright 2016 Brian Fitzpatrick
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
    name: "Thermostat Fan Recirculate",
    namespace: "b-fitzpatrick",
    author: "Brian Fitzpatrick",
    description: "Switch thermostat fan mode from \"on\" to \"auto\" for X minutes every hour.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
		input "thermostats", "capability.thermostat", title: "Which thermostats?", multiple:true
        input "runFan", "number", title: "Run for how many minutes?", range: "05..55"
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
	// Switch fan to "on" every hour at 2 minutes after the hour
    schedule("0 2 * * * ?", fanOn)
    // Switch fan to "auto" after runFan minutes
    schedule("0 " + (2 + runFan) + " * * * ?", fanAuto)
}

def fanOn() {
	thermostats.each {
    	it.fanOn()
    }
}

def fanAuto() {
	thermostats.each {
    	it.fanAuto()
    }
}