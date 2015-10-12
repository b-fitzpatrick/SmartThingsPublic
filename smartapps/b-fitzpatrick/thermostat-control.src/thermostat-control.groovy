/**
 *  Thermostat Control
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
    name: "Thermostat Control",
    namespace: "b-fitzpatrick",
    author: "Brian Fitzpatrick",
    description: "Controls a thermostat",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	page(name: "setupPage", title: "General Setup", uninstall: true, nextPage: "setpointsPage") {
        section("Hold Settings") {
        	input "holdActive", "bool", title: "Activate hold?", default: false
            input "holdHeat", "number", title: "When heating?", range: "55..80"
            input "holdCool", "number", title: "When cooling?", range: "65..90"
            //input "holdHours", "number", title: "How many hours (optional)?", range: "1..744", required: false
        }

        section("General") {
        	input "numSetpoints", "number", title: "Number of setpoints?", range: "2..20", default: 2
            input "awayDelta", "number", title: "Adjust how many degrees when Away?", range: "0..10"
            input "thermostat", "capability.thermostat", title: "Which thermostat?", multiple:false
            label(name: "label", title: "Assign a name", required: false, multiple: false)
        }
    }
    page(name: "setpointsPage", title: "Setpoints Setup", uninstall: true, install: true)
}

def setpointsPage() {
	dynamicPage(name: "setpointsPage") {
    	(1..numSetpoints).each() { i ->
            section ("Setpoint ${i}") {
            	input "name${i}", "text", title: "Name of setpoint?"
				input "time${i}", "time", title: "Start time?"
                input "days${i}", "enum", title: "On which day(s)?", options: ["Everyday", "Monday - Friday", "Saturday & Sunday", "Sunday - Thursday", "Friday & Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
                input "heat${i}", "number", title: "Temperature when heating?", range: "55..80"
                input "cool${i}", "number", title: "Temperature when cooling?", range: "65..90"
                input "achieve${i}", "bool", title: "Try to achieve by start time?", default: false
            }
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    //sendNotificationEvent("Installed with settings: ${settings}")
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
    //sendNotificationEvent("Updated with settings: ${settings}")
    unsubscribe()
    initialize()
}

def initialize() {
	// Set temperature on virtual device
    //thermostat.setTemperature(75.5)
    
    subscribe(location, "mode", modeChange)
    applySetpoint()
}

def modeChange(evt) {
    log.debug "Received mode change event, applying setpoint"
    //sendNotificationEvent("Received mode change event, applying setpoint")
    applySetpoint()
}

def activateNow(setpoint) {
	// TODO: determine whether or not to activate the future setpoint now
    return false
}

def applySetpoint() {
	log.debug "Running applySetpoint()"
    //sendNotificationEvent("Running applySetpoint()")
    unschedule() // Clear any previously-scheduled setpoint change, as a new one will be created.
    def applyHeat
    def applyCool
    
    // Determine the current and next setpoints
    def curSetpoint = getSetpoint("prior")
    def nextSetpoint = getSetpoint("next")
    
    // If the next setpoint is "achieve by", should it be activated now?
    if (settings."achieve${nextSetpoint}") {
    	if (activateNow(nextSetpoint)) curSetpoint = nextSetpoint
    }
    
    // Determine the temperatures based on hold and "Away" settings
    if (holdActive) {
    	log.debug "Hold active, applying hold setpoint"
        applyHeat = settings.holdHeat
        applyCool = settings.holdCool
        sendNotificationEvent("Hold active, applying heat: ${applyHeat}, cool: ${applyCool}")
    } else if (location.currentMode == "Away") {
        log.debug "Applying setpoint ${curSetpoint} +/- ${awayDelta} for 'Away'"
        applyHeat = curSetpoint.heat - awayDelta
        applyCool = curSetpoint.cool + awayDelta
        sendNotificationEvent("'Away' active, applying heat: ${applyHeat}, cool: ${applyCool}")
        
    } else {
    	log.debug "Applying setpoint ${curSetpoint}"
        applyHeat = curSetpoint.heat
        applyCool = curSetpoint.cool
        sendNotificationEvent("Applying heat: ${applyHeat}, cool: ${applyCool}")
    }
    
	// Make sure thermostat is set to auto and set the temperatures
    if (thermostat.currentThermostatMode != "auto") thermostat.auto()
    log.debug "Setting heating temperature to ${applyHeat}"
    if (thermostat.currentHeatingSetpoint != applyHeat) thermostat.setHeatingSetpoint(applyHeat)
    log.debug "Setting cooling temperature to ${applyCool}"
    if (thermostat.currentCoolingSetpoint != applyCool) thermostat.setCoolingSetpoint(applyCool)
    
    // Schedule the next setpoint
	log.debug "Scheduling next setpoint at: " + nextSetpoint.cal.getTime().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone)
    sendNotificationEvent("Scheduling next setpoint at: " + nextSetpoint.cal.getTime().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ", location.timeZone))
    runOnce(nextSetpoint.cal.getTime().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ"), applySetpoint)
}

def getSetpoint(priorOrNext) {
    //log.debug "Starting: getSetpoint('${priorOrNext}')"
    def curCal = Calendar.getInstance(location.timeZone) // Current date and time
    long minSetpointToNow = (7 * 24 * 60 * 60 * 1000) // One week in milliseconds
    def foundSetpoint = [:]
    
    // Iterate forwards or backwards, starting at today
    for (d in (0..6)) {
    	if (priorOrNext == "prior") {
        	d *= -1
            minSetpointToNow *= -1
        }
    	def dCal = Calendar.getInstance(location.timeZone)
        dCal.add(Calendar.DAY_OF_WEEK, d)
        def daySetpoints = getSetpointsByDay(dCal.get(Calendar.DAY_OF_WEEK))
        
        // Iterate through the daily setpoints and find the one closest to now
        daySetpoints.each() { i ->
        	def iCal = Calendar.getInstance(location.timeZone)
            iCal.setTime(timeToday(settings."time${i}", location.timeZone))
            iCal.add(Calendar.DAY_OF_WEEK, d)
            //log.debug "Setpoint ${i} Time on Day ${d}: " + iCal.format("EEE MMM dd yyyy HH:mm z")
            def setpointToNow = iCal.getTimeInMillis() - curCal.getTimeInMillis()
            if (priorOrNext == "prior") {
                if (setpointToNow < 0 && setpointToNow > minSetpointToNow) {
                    minSetpointToNow = setpointToNow
                    foundSetpoint.index = i
                    foundSetpoint.cal = iCal
                }
            } else {
            	if (setpointToNow > 0 && setpointToNow < minSetpointToNow) {
                    minSetpointToNow = setpointToNow
                    foundSetpoint.index = i
                    foundSetpoint.cal = iCal
                }
            }
        }
        if (foundSetpoint != [:]) { // found a setpoint
        	foundSetpoint.name = settings."name${foundSetpoint.index}"
            foundSetpoint.days = settings."days${foundSetpoint.index}"
            foundSetpoint.heat = settings."heat${foundSetpoint.index}"
            foundSetpoint.cool = settings."cool${foundSetpoint.index}"
            foundSetpoint.achieve = settings."achieve${foundSetpoint.index}"
        	return foundSetpoint
        }
    }
    log.debug "Failed to find a '${priorOrNext}'setpoint"
}

def getSetpointsByDay(dayOfWeek) {
	def setpoints = []
    for (i in 1..numSetpoints) {
    	switch(settings."days${i}") {
        	case "Everyday":
            	setpoints.add(i)
                break
            case "Monday - Friday":
            	if (dayOfWeek in [2,3,4,5,6]) setpoints.add(i)
                break
            case "Saturday & Sunday":
            	if (dayOfWeek in [1,7]) setpoints.add(i)
                break
            case "Sunday - Thursday":
            	if (dayOfWeek in [1,2,3,4,5]) setpoints.add(i)
                break
            case "Friday & Saturday":
            	if (dayOfWeek in [6,7]) setpoints.add(i)
                break
            case "Sunday":
            	if (dayOfWeek == 1) setpoints.add(i)
            	break
            case "Monday":
            	if (dayOfWeek == 2) setpoints.add(i)
            	break
            case "Tuesday":
            	if (dayOfWeek == 3) setpoints.add(i)
                break
            case "Wednesday":
            	if (dayOfWeek == 4) setpoints.add(i)
                break
            case "Thursday":
            	if (dayOfWeek == 5) setpoints.add(i)
                break
            case "Friday":
            	if (dayOfWeek == 6) setpoints.add(i)
                break
            case "Saturday":
            	if (dayOfWeek == 7) setpoints.add(i)
                break
            default: // should never happen
            	setpoints.add(i)
                break
        }
    }
    return setpoints
}