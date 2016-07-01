/**
 *	Change Blue Iris Profile
 *	
 *	https://github.com/
 *	Copyright 2016 flyjmz
 *	Version	0.0.1 - 2016-06-30 - Initial test release
 *		
 *
 *	This SmartApp will send profile changes to a Blue Iris server on the local network.
 *
 *	This requires the Blue Iris web server to allow un-authenticated connections.  In
 *	settings > Web Server > Advanced > Authentication select "Non-LAN only"
 *	
 *	TODO:
 *	- 
 *	
 *	ISSUES:
 *	-
 *	
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *	
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *	
 */

definition(
	name: "Change Blue Iris Profile",
	namespace: "flyjmz",
	author: "flyjmz",
	description: "Change Blue Iris profile to match Smartthings Mode changes (on the local network)",
	category: "Convenience",
	iconUrl: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo.png",
	iconX2Url: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo%402x.png"
)

preferences {
	section("Blue Iris server details"){
		input "biServer", "text", title: "Server", description: "Blue Iris web server IP", required: true
		input "biPort", "number", title: "Port", description: "Blue Iris web server port", required: true
		input "biUser", "text", title: "User name", description: "Blue Iris user name", required: true
		input "biPass", "password", title: "Password", description: "Blue Iris password", required: true
		}
	section("Blue Iris Camera Name"){
		input "biCamera", "text", title: "Camera Name", required: true
	}
	section("Select events to be sent to Blue Iris"){
		input "myMotion", "capability.motionSensor", title: "Motion Sensors", required: false, multiple: true
		input "myContact", "capability.contactSensor", title: "Contact Sensors", required: false, multiple: true
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
	subscribe(myMotion, "motion", eventHandlerBinary)
	subscribe(myContact, "contact", eventHandlerBinary)
}

def eventHandlerBinary(evt) {
	if ((evt.value == "active") || (evt.value == "open")) {
		log.debug "processed event ${evt.name} from device ${evt.displayName} with value ${evt.value} and data ${evt.data}"
		def biHost = "${settings.biServer}:${settings.biPort}"
		def biRawCommand = "/admin?profile=1"    //TO DO - change the 1 to the profile, then repeat the command (to hold it)
        log.debug "sending GET to URL http://$biHost/$biRawCommand"
		def httpMethod = "GET"
		def httpRequest = [
			method:		httpMethod,
			path: 		biRawCommand,
			headers:	[
        				HOST:		biHost,
						Accept: 	"*/*",
                    ]
		]
		def hubAction = new physicalgraph.device.HubAction(httpRequest)
		sendHubCommand(hubAction)
	}
}   //TO DO - Check the current profile to make sure it set it...how??
