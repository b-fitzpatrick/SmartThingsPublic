/**
 *  Temperature Datalogger
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
    name: "Temperature Datalogger",
    namespace: "b-fitzpatrick",
    author: "Brian Fitzpatrick",
    description: "Logs thermostat and weather data for fitting a thermodynamic model",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("Settings") {
		input "thermostats", "capability.thermostat", title: "Which thermostats?", multiple:true
        input "formId", "text", title: "Google Form ID"
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
	// Record data on thermostat temperature and state changes
    subscribe(thermostats, "temperature", tempHandler)
    subscribe(thermostats, "thermostatOperatingState", stateHandler)
    
    // Record data now and periodically
    log.debug "here"
    schedHandler()
    runEvery5Minutes(schedHandler)
}

def tempHandler(evt) {
	recordData(evt, "temp")
}

def stateHandler(evt) {
	recordData(evt, "state")
}

def schedHandler() {
	def now = new Date()
	def simEvent = [date: now, deviceId: ""]
    recordData(simEvent, "sched")
}

def recordData(evt, trigger) {
	// Get weather data
    def weatherTime
    def precipIntensity
    def temperature
    def dewPoint
    def humidity
    def windSpeed
    def windBearing
    def cloudCover
    
    def params = [
        uri:  'https://api.forecast.io/forecast/830033e4ae2f33ce8e8953f64b170948/33.175,-96.690/',
        contentType: 'application/json'
    ]
    try {
        httpGet(params) {resp ->
            //log.debug "resp data: ${resp.data}"
            weatherTime = resp.data.currently.time
            precipIntensity = resp.data.currently.precipIntensity
            temperature = resp.data.currently.temperature
            dewPoint = resp.data.currently.dewPoint
            humidity = resp.data.currently.humidity
            windSpeed = resp.data.currently.windSpeed
            windBearing = resp.data.currently.windBearing
            cloudCover = resp.data.currently.cloudCover
        }
    } catch (e) {
        log.error "forecast.io: $e"
    }
    
    // Thermostat event or scheduled event?
    def devices
    if (evt.deviceId == "") {
    	devices = thermostats //all thermostats - refresh first, as events don't seem reliable
        devices.each { thermostat ->
        	thermostat.refresh()
        }
    } else {
    	devices = [evt.device] //just the triggering thermostat, no need to refresh
    }
    
    devices.each { thermostat ->
        // Send data to Google Form
        def url = "https://docs.google.com/a/froxen.com/forms/d/${formId}/formResponse"
        def body = "entry.1554218875=${evt.date.getTime().toString()}&" +
                   "entry.1718537046=${URLEncoder.encode(thermostat.displayName, 'UTF-8')}&" +
                   "entry.2044143710=${trigger}&" +
                   "entry.969591355=${thermostat.currentTemperature}&" +
                   "entry.751884560=${thermostat.currentHumidity}&" +
                   "entry.1445938291=${thermostat.currentThermostatOperatingState}&" +
                   "entry.1792453546=${thermostat.currentThermostatFanState}&" +
                   "entry.490663172=${thermostat.currentCoolingSetpoint}&" +
                   "entry.922143888=${thermostat.currentHeatingSetpoint}&" +
                   "entry.741601175=${weatherTime}&" +
                   "entry.47662698=${temperature}&" +
                   "entry.1159520036=${humidity}&" +
                   "entry.133652532=${dewPoint}&" +
                   "entry.15313144=${precipIntensity}&" +
                   "entry.59753146=${windSpeed}&" +
                   "entry.829030705=${windBearing}&" +
                   "entry.983948422=${cloudCover}"

        log.debug "url: ${url}"
        log.debug "body: ${body}"

        try {
            httpPost(url, body) { resp ->
                resp.headers.each {
                    log.debug "${it.name} : ${it.value}"
                }
                log.debug "response contentType: ${resp.contentType}"
            }
        } catch (e) {
            log.error "Google form: ${e}"
        }
    }
}