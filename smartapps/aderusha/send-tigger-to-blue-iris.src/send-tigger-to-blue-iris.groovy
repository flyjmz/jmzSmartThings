/**
 *	Send Tigger to Blue Iris
 *	
 *	Trigger Blue Iris in response to SmartThings events
 *	
 *	https://github.com/aderusha/SmartThings/blob/master/Send-Trigger-to-Blue-Iris.groovy
 *	Copyright 2015 aderusha
 *	Version	0.0.1 - 2015-12-06 - Initial test release
 *		0.0.2 - 2015-12-06 - Only trigger on "motion" or "open", added more debug logging
 *		0.0.3 - 2015-12-10 - Actually tested this against Blue Iris and made it work.
 *
 *	This SmartApp will send selected events to a Blue Iris server on the local network.
 *
 *	This requires the Blue Iris web server to allow un-authenticated connections.  In
 *	settings > Web Server > Advanced > Authentication select "Non-LAN only" (preferred)
 *	or "No" to disable authentication altogether.
 *	
 *	TODO:
 *	- Add device types
 *	- Add configurable conditions
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
	name: "Send Tigger to Blue Iris",
	namespace: "aderusha",
	author: "aderusha",
	description: "Trigger Blue Iris in response to SmartThings events",
	category: "Convenience",
	iconUrl: "https://raw.githubusercontent.com/aderusha/SmartThings/master/resources/BlueIris_logo.png",
	iconX2Url: "https://raw.githubusercontent.com/aderusha/SmartThings/master/resources/BlueIris_logo%402x.png"
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
		def biRawCommand = "/admin?camera=${settings.biCamera}&trigger&user=${settings.biUser}&pw=${settings.biPass}"
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
}