/**
 *  Turn on Light When Sensor Opened at Night
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
    name: "Turn on Light When Sensor Opened at Night",
    namespace: "b-fitzpatrick",
    author: "Brian Fitzpatrick",
    description: "If off, turns on a light when any of the associated Contact Sensors are opened between sunset and sunrise. It then turns it back off after a defined delay",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
		input "sensors", "capability.contactSensor", title: "When any of these are opened?", multiple:true
        input "light", "capability.switch", title: "Turn on which light?", multiple:false
        input "level", "number", title: "Light level (if dimmable)?", required:false, range:"1..100"
        input "delay", "number", title: "Turn off after how many minutes?"
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
	subscribe(sensors, "contact.open", openHandler)
}

def openHandler(evt) {
	// Only act if it is dark
    def sunriseAndSunset = getSunriseAndSunset()
    if (evt.date > sunriseAndSunset.sunset || evt.date < sunriseAndSunset.sunrise) {
        // Only act if the light is currently off
        if (light.currentSwitch != "on") {
            if (light.hasCapability("Switch Level") && level != null) {
                light.setLevel(level)
            } else {
                light.on()
            }
            runIn(60 * delay, lightOff)
        }
    }
}

def lightOff() {
	light.off()
}