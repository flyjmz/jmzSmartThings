/**
 *	Change Blue Iris Profile
 *	
 *	https://github.com/flyjmz/jmzSmartThings/blob/master/smartapps/flyjmz/change-blue-iris-profile.src/change-blue-iris-profile.groovy
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

/**preferences {
*	section("Blue Iris server details"){
*		input "biServer", "text", title: "Server", description: "Blue Iris web server IP", required: true
*		input "biPort", "number", title: "Port", description: "Blue Iris web server port", required: true
*		input "biUser", "text", title: "User name", description: "Blue Iris user name", required: true
*		input "biPass", "password", title: "Password", description: "Blue Iris password", required: true
*		}
*}

preferences {
    page(name:"selectModes")
    page(name:"BISettings")
}

def selectModes() {  
  dynamicPage(name: "selectModes", title: "Mode and Profile Matching", nextPage:"BISettings", uninstall:true) {    
    section("") {
        paragraph "Numbers 1-7 correspond to Blue Iris profile numbers. To ignore a mode leave it blank. A profile of 0 sets Blue Iris to 'inactive.'"
        location.modes.each { mode ->
            def modeId = mode.id.toString()  
            input "mode-${modeId}", "number", title: "Mode ${mode}", required: false
        }
    }
  }
}

def BISettings() {
    dynamicPage(name:"BISettings", "title":"Blue Iris Login Info", uninstall:true, install:true) {
        section( "" ) {
            input "biServer", "string", title: "BI Webserver Host(include http://)", required:true
            input "biPort", "number", title: "BI Webserver Port", required:true
            input "biUser", "string", title: "BI Username", required: true
            input "biPass", "string", title: "BI Password", required: true
        }
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	unsubscribe()
    	subscribe(location, modeChange)
}

def updated() {
	log.debug "Updated with settings: ${settings}"
    	unsubscribe()
    	subscribe(location, modeChange)   
}

def modeChange(evt)
{
    if (evt.name != "mode") {return;}
    log.debug "BI_modeChange detected. " + evt.value
    def checkMode = ""
    
    //easiest way to get mode by id. Didnt want to use names.
    location.modes.each { mode ->
        if (mode.name == evt.value){
            checkMode = "mode-" + mode.id
            log.debug "BI_modeChange matched to " + mode.name
        }
    }
    
    if (checkMode != "" && settings[checkMode]){
        log.debug "BI_Found profile " + settings[checkMode]
        takeAction(settings[checkMode].toInteger());
    }
}

def takeAction(profile) {
	def biHost = "${settings.biServer}:${settings.biPort}"
	def biRawCommand = "/admin?profile=1"    //TODO - change the 1 to the profile number, then repeat the command (to hold it)
        log.debug "sending GET to URL $biHost$biRawCommand"
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
}  //TODO - Check the current profile to make sure it set it...how??

def profileName(names, num) {
    if (names[num.toInteger()]) {
        names[num.toInteger()] + " (#${num})"
    } else {
        '#' + num
    }
}
