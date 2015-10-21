/**
 *  Garage Door Event
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
    name: "Garage Door Event",
    namespace: "b-fitzpatrick",
    author: "Brian Fitzpatrick",
    description: "The Photon-based garage door controller will notify SmartThings of door status changes via a call to the API provided by this SmartApp",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    oauth: true)


preferences {
	section ("Select door #1") {
    	input "door1", "capability.doorControl", multiple: false, required: true
    }
    section ("Select door #2") {
        input "door2", "capability.doorControl", multiple: false, required: true
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
	// TODO: subscribe to attributes, devices, locations, etc.
    subscribe(door1, "verifyClose.false", failHandler)
    subscribe(door2, "verifyClose.false", failHandler)
}

def failHandler(evt) {
	sendPush("${evt.displayName} failed to close")
}

mappings {
	path("/doorchange") {
		action: [
			POST: "doorChange"
		]
	}
}

void doorChange() {
	log.debug "running 'doorChange()'"
    def data = request.JSON?.data
    def doorNum = data.getAt(0..0)
    def doorStatus = data.substring(2)
    log.debug "doorNum: ${doorNum}, doorStatus: ${doorStatus}"
    if (doorNum == "1") {
    	door1.doorChange(doorStatus)
    } else {
    	door2.doorChange(doorStatus)
    }
}