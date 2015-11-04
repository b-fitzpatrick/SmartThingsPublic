/**
 *  Porch Light Control
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
    name: "Porch Light Control",
    namespace: "b-fitzpatrick",
    author: "Brian Fitzpatrick",
    description: "Controls a light switch. Turns it on/off on a schedule, and turns it on at night when a garage door is opened.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
		input "light", "capability.switch", title: "Turn on which light?", multiple:false
        input "level", "number", title: "Light level (if dimmable)?", required:false, range: "1..100"
        input "afterSunset", "number", title: "Turn on how many minutes after sunset?"
        input "offTime", "time", title: "Turn off at what time?"
		input "sensors", "capability.contactSensor", title: "Also turn on at night when which sensors are opened?", multiple:true
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
    unschedule("offSchedule")
	initialize()
}

def initialize() {
	state.openOn = false
	subscribe(sensors, "contact.open", openHandler)
    schedule(offTime, offSchedule)
    setOnSchedule()
}

def onSchedule() {
    if (state.openOn) {
    	unschedule("lightOff") // handle case where light is on and 'lightOff' has been scheduled
        state.openOn = false
    }
    light.on()
    setOnSchedule()
}

def offSchedule() {
	light.off() // It could happen that the light will turn off early if previously activated, but this seems not worth handling.
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
            state.openOn = true
        }
    }
}

def lightOff() {
	light.off()
    state.openOn = false
}

def setOnSchedule() {
	def sunriseAndSunset = getSunriseAndSunset()
    def onTime = new Date(sunriseAndSunset.sunset.getTime() + afterSunset * 60 * 1000)
    def now = new Date()
    if (now > onTime) {
    	onTime = new Date(onTime.getTime() + 24 * 60 * 60 * 1000)
    }
    runOnce(onTime, onSchedule)    
}