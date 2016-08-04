/**
 *  Blue Iris Trigger 
 *  (Child app.  Parent app is: "Blue Iris Camera Triggers") 
 *
 *  Copyright 2016 flyjmz, based on work by Tony Gutierrez in "Blue Iris Profile Integration"
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
 *  	Version 1.0 - 30July2016 	Initial release
 *	Version 1.1 - 3August2016	Cleaned up code
 * 	Version 1.2 - 4August2016	Added Alarm trigger capability from rayzurbock
 */

definition(
    name: "Blue Iris Trigger",
    namespace: "flyjmz",
    author: "flyjmz",
    parent: "flyjmz:Blue Iris Camera Triggers",
    description: "Child app to 'Blue Iris Camera Triggers.' Install that app, it will call this during setup.",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo.png",
    iconX2Url: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo%402x.png")

preferences {
	page(name: "mainPage", title: "", install: true, uninstall: true)
}

def mainPage() {
	return dynamicPage(name: "mainPage", title: "") {
    	section("Blue Iris Camera Name"){
			input "biCamera", "text", title: "Camera Name", required: true
        	paragraph "Use the Blue Iris short name for the camera, it is case-sensitive."
		}
		section("Select trigger events"){   
			input "myMotion", "capability.motionSensor", title: "Motion Sensors Active", required: false, multiple: true
			input "myContact", "capability.contactSensor", title: "Contact Sensors Opening", required: false, multiple: true
            		input "mySwitch", "capability.switch", title: "Switches Turning On", required: false, multiple: true
            		input "myAlarm", "capability.alarm", title: "Alarm Activated", required: false, multiple: true
            		paragraph "Note: Only the Active/Open/On events will send a trigger.  Motion stopping, Contacts closing, and Switches turning off will not send a trigger."
		}
        section("") {
        	input "customTitle", "text", title: "Assign a Name", required: true
            	mode(title: "Set for specific mode(s)")
        }
	}
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    unsubscribe()
    subscribeToEvents()
    app.updateLabel("${customTitle}")
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
    	app.updateLabel("${customTitle}")
}

def subscribeToEvents() {
	subscribe(myMotion, "motion.active", eventHandlerBinary)
	subscribe(myContact, "contact.open", eventHandlerBinary)
	subscribe(mySwitch, "switch.on", eventHandlerBinary)
	subscribe(myAlarm, "alarm.strobe", eventHandlerBinary)
	subscribe(myAlarm, "alarm.siren", eventHandlerBinary)
	subscribe(myAlarm, "alarm.both", eventHandlerBinary)
}

def eventHandlerBinary(evt) {
	def host = parent.host
    def port = parent.port
    def username = parent.username
    def password = parent.password
    def errorMsg = "Could not trigger Blue Iris :("
	log.debug "processed event ${evt.name} from device ${evt.displayName} with value ${evt.value} and data ${evt.data}"
    try {
        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login"]) { response ->
            //log.debug response.data
            if (response.data.result == "fail")
            {
               log.debug "BI_Inside initial call fail, proceeding to login"
               def session = response.data.session
               def hash = username + ":" + response.data.session + ":" + password
               hash = hash.encodeAsMD5()
               httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login","session":session,"response":hash]) { response2 ->
                    if (response2.data.result == "success") {
                        log.debug ("BI_Logged In")
                        //log.debug response2.data
                        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","session":session]) { response3 ->
                            log.debug ("BI_Retrieved Status");
                            //log.debug response3.data
                            if (response3.data.result == "success"){
                                    httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"trigger","camera":biCamera,"session":session]) { response4 ->
                                        //log.debug response4.data
                                        if (response4.data.result == "success") {
                                            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"logout","session":session]) { response5 ->
                                                //log.debug response5.data
                                                //log.debug "Logged out?"
                                            }
                                        } else {
                                            log.debug "BI_FAILURE"
                                            log.debug(response4.data.data.reason)
                                            sendNotificationEvent(errorMsg)
                                        }
                                    }
                            } else {
                                log.debug "BI_FAILURE"
                                log.debug(response3.data.data.reason)
                                sendNotificationEvent(errorMsg)
                            }
                        }
                    } else {
                        log.debug "BI_FAILURE"
                        log.debug(response2.data.data.reason)
                        sendNotificationEvent(errorMsg)
                    }
                }
            } else {
                log.debug "FAILURE"
                log.debug(response.data.data.reason)
                sendNotificationEvent(errorMsg)
            }
        }
    } catch(Exception e) {
        log.debug(e)
        sendNotificationEvent(errorMsg);
    }
}
