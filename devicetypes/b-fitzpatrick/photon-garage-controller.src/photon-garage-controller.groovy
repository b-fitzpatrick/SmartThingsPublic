/**
 *  Photon Garage Controller
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

preferences {
    input("device_id", "text", title: "Device ID")
    input("access_token", "text", title: "Access Token")
    input("door_num", "text", title: "Door Number")
}

metadata {
	definition (name: "Photon Garage Controller", namespace: "b-fitzpatrick", author: "Brian Fitzpatrick") {
		capability "Actuator"
		capability "Door Control"
        capability "Contact Sensor"
		capability "Polling"
        capability "Refresh"
		capability "Sensor"
        capability "Switch"
        
        attribute "verifyClose", "string"
        
        command "doorChange"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
        standardTile("door", "device.door", width: 2, height: 2) {
			state("closed", label:'${name}', icon:"st.doors.garage.garage-closed", action: "open", backgroundColor:"#79b821", nextState:"opening")
			state("open", label:'${name}', icon:"st.doors.garage.garage-open", action: "close", backgroundColor:"#ffa81e", nextState:"closing")
			state("opening", label:'${name}', icon:"st.doors.garage.garage-opening", backgroundColor:"#ffe71e")
			state("closing", label:'${name}', icon:"st.doors.garage.garage-closing", backgroundColor:"#ffe71e")
		}
        standardTile("refresh", "device.status", inactiveLabel: false, decoration: "flat") {
            state "default", action:"refresh", icon:"st.secondary.refresh"
        }
        
		main "door"
		details(["door", "refresh"])
	}
}

// handle commands
def on() {
	open()
}

def off() {
	close()
}

def open() {
	log.debug "Executing 'open'"
	if (device.currentValue("door") != "open") {
    	log.debug "Sending actDoor${door_num}"
        actDoor()
    } else {
    	log.debug "Not opening door: already open"
    }
}

def close() {
	log.debug "Executing 'close'"
	if (device.currentValue("door") != "closed") {
    	log.debug "Sending actDoor${door_num}"
        actDoor()
        runIn(20, "verifyClose")
    } else {
    	log.debug "Not closing door: already closed"
    }
}

def verifyClose() {
	if (device.currentValue("door") != "closed") {
    	log.debug "Did not verify that door is closed, actuating a second time"
        actDoor()
        runIn(20, "verifyClose2")
    } else {
    	log.debug "Verified that door is closed"
        sendEvent(name: "verifyClose", value: "true", displayed: false)
    }
}

def verifyClose2() {
	if (device.currentValue("door") != "closed") {
		log.debug "Still did not verify that door is closed, sending event"
        sendEvent(name: "verifyClose", value: "false", displayed: false)
        sendEvent(name: "verifyClose", value: "true", displayed: false)
    } else {
    	log.debug "Verified that door is closed"
        sendEvent(name: "verifyClose", value: "true", displayed: false)
    }
}

def poll() {
	log.debug "Executing 'poll'"
	getStatus()
}

def refresh() {
	log.debug "Executing 'refresh'"
	getStatus()
}

def doorChange(String doorStatus) {
	log.debug "Executing doorChange('${doorStatus}')"
	sendEvent(name: "door", value: doorStatus, descriptionText: "Door is ${doorStatus}")
    sendEvent(name: "contact", value: doorStatus, displayed: false)
    sendEvent(name: "switch", value: (doorStatus == "closed") ? "off" : "on", displayed: false)
}

private getStatus() {
	def statusClosure = { response ->
    	log.debug "Status request was successful, $response.data"
        if (response.data.result == 1 && device.currentValue("door") != "closed") {
            doorChange("closed")
        } else if (response.data.result == 0 && device.currentValue("door") != "open") {
        	doorChange("open")
        }
    }
        
    def statusParams = [
    	uri: "https://api.particle.io/v1/devices/${device_id}/doorStatus${door_num}?access_token=${access_token}",
        success: statusClosure
    ]
    
    log.debug "url: $statusParams.uri"
    httpGet(statusParams)
}

private actDoor() {
	def actParams = [
    	uri: "https://api.particle.io/v1/devices/${device_id}/actDoor${door_num}",
        body: [access_token: access_token]
    ]
    
    log.debug "url: $actParams.uri"
	httpPost(actParams)
}