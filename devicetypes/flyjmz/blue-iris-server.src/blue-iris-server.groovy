/*
Blue Iris Server
SmartThings Custom Device Type Handler for Blue Iris Webserver: http://blueirissoftware.com/

Copyright 2017 FLYJMZ (flyjmz230@gmail.com)

Smartthings Community Thread:  https://community.smartthings.com/t/release-blue-iris-fusion-integrate-smartthings-and-blue-iris/54226      //todo - update when moved to a new thread
Github: https://github.com/flyjmz/jmzSmartThings/tree/master/devicetypes/flyjmz

Shoutout and big thanks to Ken from Blue Iris! His great support made this happen.  Plus Blue Iris is an outstanding camera platform.


Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
for the specific language governing permissions and limitations under the License.


Version History:
1.0		14Oct2017	Initial Commit of Standalone DTH

To Do:
-Try having camera's motion alert from BI send a get to the hub's ip, parse it out to figure out which camera, and make it's motion active.  Likely need to use subscribe as described at the bottom of this code.  Might have to be inside individual camera devices...
-Add another layer of server health check or a better method if the user forwards their server out & the cloud could see it: Use the cloud-based gethttp or whatever to send a check for current profile, and if the response is not 200, then we know we're offline

For manager app:
-DTHs can't send notifications.  So manager app needs to subscribe to 'errorMessage' and pass the value as a notification.
-Want to install the device from the manager app so you don't have to keep entering settings, the tile labels can persist, and the profile names can be site directly from ST modes (like BI Fusion).
-Can we use null profile numbers to make fewer tiles on the app (i.e. don't display tiles for unused profiles)?

Wish List:
-"Commands that take parameters" cannot be called with their parameters from tile action, resulting in a different command for each profile.  Some solutions may exist using child device buttons/switches.
-Label wishes:
--The label can't be centered within the tile (which prevents making 2-line labels (using '/n' or '/r'), because right now the second line just goes below the tile).
--The label's text color can't be specified
--The 'on' status for each tile lets me have the background change but then the label says on instead of the profile's name
*/
metadata {
    definition (name: "Blue Iris Server", namespace: "flyjmz", author: "flyjmz230@gmail.com") {
        capability "Refresh"
        attribute "blueIrisProfile", "Number"
        attribute "stoplight", "enum", ["red", "green", "yellow"]
        attribute "errorMessage", "String"
        command "setBlueIrisProfile0"
        command "setBlueIrisProfile1"
        command "setBlueIrisProfile2"
        command "setBlueIrisProfile3"
        command "setBlueIrisProfile4"
        command "setBlueIrisProfile5"
        command "setBlueIrisProfile6"
        command "setBlueIrisProfile7"
        command "syncBIprofileToSmarthThings"
        command "setBlueIrisStopLight"
        command "changeHoldTemp"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {	//todo -- this icon is not working, just want it on the "my home" page in the app, not on the actual device page
        valueTile("blueIrisProfile", "device.blueIrisProfile", width: 4, height: 2, icon: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo.png", canChangeIcon: false, decoration: "flat") {
            state("default", label: '${currentValue}')  
        }
        standardTile("refresh", "device.refresh", width: 2, height: 1, inactiveLabel: false, decoration: "flat") {
            state("default", label:'', action: "refresh.refresh", icon:"st.secondary.refresh", nextState:"refreshing")
            state("refreshing", label:"Refreshing", backgroundColor: "#00a0dc")
        } 
        standardTile("sync", "device.sync", width: 2, height: 1, decoration: "flat") {
            state("default", label:'Sync BI to ST', action: "syncBIprofileToSmarthThings", nextState:"syncing")
            state("syncing", label:"Syncing", backgroundColor: "#00a0dc")
        } 
        standardTile("profile0", "device.profile0mode", width: 2, height: 1, decoration: "flat") {
            state("default", label:'${currentValue}', action: "setBlueIrisProfile0", backgroundColor: "#ffffff", nextState:"turningOn")
            state("on", label:'${currentValue}', backgroundColor: "#00a0dc")
            state("turningOn", label:"Turning On", backgroundColor: "#00a0dc")
        }
        standardTile("holdTemp", "device.holdTemp", width: 2, height: 1, decoration: "flat") {
            state("Hold", label:'${currentValue}', action: "changeHoldTemp", backgroundColor: "#ffffff", nextState:"changing")
            state("Temporary", label:'${currentValue}', action: "changeHoldTemp", backgroundColor: "#ffffff", nextState:"changing")
            state("changing", label:"Changing...", backgroundColor: "#ffffff")
        }
        standardTile("profile1", "device.profile1mode", width: 2, height: 2, decoration: "flat") {
            state("default", label:'${currentValue}', action: "setBlueIrisProfile1", backgroundColor: "#ffffff", nextState:"turningOn")
            state("on", label:'${currentValue}', backgroundColor: "#00a0dc")
            state("turningOn", label:"Turning On", backgroundColor: "#00a0dc")
        }
        standardTile("profile2", "device.profile2mode", width: 2, height: 2, decoration: "flat") {
            state("default", label:'${currentValue}', action: "setBlueIrisProfile2", backgroundColor: "#ffffff", nextState:"turningOn")
            state("on", label:'${currentValue}', backgroundColor: "#00a0dc")
            state("turningOn", label:"Turning On", backgroundColor: "#00a0dc")
        }
        standardTile("profile3", "device.profile3mode", width: 2, height: 2, decoration: "flat") {
            state("default", label:'${currentValue}', action: "setBlueIrisProfile3", backgroundColor: "#ffffff", nextState:"turningOn")
            state("on", label:'${currentValue}', backgroundColor: "#00a0dc")
            state("turningOn", label:"Turning On", backgroundColor: "#00a0dc")
        }
        standardTile("profile4", "device.profile4mode", width: 2, height: 2, decoration: "flat") {
            state("default", label:'${currentValue}', action: "setBlueIrisProfile4", backgroundColor: "#ffffff", nextState:"turningOn")
            state("on", label:'${currentValue}', backgroundColor: "#00a0dc")
            state("turningOn", label:"Turning On", backgroundColor: "#00a0dc")
        }
        standardTile("profile5", "device.profile5mode", width: 2, height: 2, decoration: "flat") {
            state("default", label:'${currentValue}', action: "setBlueIrisProfile5", backgroundColor: "#ffffff", nextState:"turningOn")
            state("on", label:'${currentValue}', backgroundColor: "#00a0dc")
            state("turningOn", label:"Turning On", backgroundColor: "#00a0dc")
        }
        standardTile("profile6", "device.profile6mode", width: 2, height: 2, decoration: "flat") {
            state("default", label:'${currentValue}', action: "setBlueIrisProfile6", backgroundColor: "#ffffff", nextState:"turningOn")
            state("on", label:'${currentValue}', backgroundColor: "#00a0dc")
            state("turningOn", label:"Turning On", backgroundColor: "#00a0dc")
        }
        standardTile("profile7", "device.profile7mode", width: 2, height: 2, decoration: "flat") {
            state("default", label:'${currentValue}', action: "setBlueIrisProfile7", backgroundColor: "#ffffff", nextState:"turningOn")
            state("on", label:'${currentValue}', backgroundColor: "#00a0dc")
            state("turningOn", label:"Turning On", backgroundColor: "#00a0dc")
        }
        standardTile("stoplight", "device.stoplight", width: 2, height: 2, decoration: "flat") {
            state("red", label:'Traffic Light', action: "setBlueIrisStopLight", backgroundColor: "#bc2323", nextState:"changing")
            state("yellow", label:'Traffic Light', action: "setBlueIrisStopLight", backgroundColor: "#f1d801", nextState:"changing")
            state("green", label:'Traffic Light', action: "setBlueIrisStopLight", backgroundColor: "#44b621", nextState:"changing")
            state("changing", label:'Changing...', backgroundColor: "#ffffff")
        }
        main ('blueIrisProfile')
        details('blueIrisProfile','refresh','sync','profile0','profile1','profile2','holdTemp','profile3','profile4','profile5','profile6','profile7','stoplight')
    }

    preferences {
        section("Blue Iris Server Login Settings:"){
            paragraph "This only functions on a Local Connection to the Blue Iris Server (i.e. LAN, the SmartThings hub is on the same network as the BI Server)?"
            paragraph "Ensure 'Secure Only' is not checked in Blue Iris' Webserver settings."
            input "host", "text", title: "BI Webserver IP", description: "Don't include http://, etc", required:true, displayDuringSetup: true
            input "port", "number", title: "BI Webserver Port (e.g. 81)", required:true, displayDuringSetup: true
            input "username", "text", title: "BI Username", description: "Must be an Admin User", required: true, displayDuringSetup: true
            input "password", "password", title: "BI Password", required: true, displayDuringSetup: true
        }
        section("Blue Iris Profile <=> SmartThings Mode Matching:"){
            paragraph "Enter the SmartThings Mode name for each of the Blue Iris Profiles (Inactive, and #1-7). Be sure to enter it exactly. To ignore a profile number, leave it blank."
            input "profile0", "text", title: "ST Mode for BI Inactive (Profile 0)", required:false, displayDuringSetup: true
            input "profile1", "text", title: "ST Mode for BI Profile #1", required:false, displayDuringSetup: true
            input "profile2", "text", title: "ST Mode for BI Profile #2", required:false, displayDuringSetup: true
            input "profile3", "text", title: "ST Mode for BI Profile #3", required:false, displayDuringSetup: true
            input "profile4", "text", title: "ST Mode for BI Profile #4", required:false, displayDuringSetup: true
            input "profile5", "text", title: "ST Mode for BI Profile #5", required:false, displayDuringSetup: true
            input "profile6", "text", title: "ST Mode for BI Profile #6", required:false, displayDuringSetup: true
            input "profile7", "text", title: "ST Mode for BI Profile #7", required:false, displayDuringSetup: true
        }
        section("Blue Iris Server Health Monitor"){
            double waitThreshold = 10
            input "waitThreshold", "number", title: "Enter how many seconds to wait after the server request is made before deaming it offline:", description: "Default: 10sec", required:false, displayDuringSetup: true 
        }
        section("Debug"){
            paragraph "You can turn on debug logging, viewed in Live Logging on the API website."
            def loggingOn = false
            input "loggingOn", "bool", title: "Debug Logging?"
        }
    }
}

def updated() {
	unschedule()
    initialize()
}

def initialize() { 
	state.serverResponseThreshold = (waitThreshold != null) ? waitThreshold : 10
    log.debug "initialize() called, debug logging is ${loggingOn}, serverResponseThreshold is ${state.serverResponseThreshold}"
    //set actual network ID so SmartThings knows what to listen to for Parse()
    def hosthex = convertIPtoHex(host).toUpperCase()  //Note: it needs to be set to uppercase for the new deviceNetworkId to work in SmartThings
    def porthex = convertPortToHex(port).toUpperCase()
    device.deviceNetworkId = "$hosthex:$porthex"
    
    //initialize variables and set the profile names to the tiles
    if (state.holdChange == null) {
        state.holdChange = true
        sendEvent(name: "holdTemp", value: "Hold", displayed: false)
    }
    state.hubCommandReceivedTime = now()
    state.hubCommandSentTime = now()
    state.profile0mode = (profile0 != null) ? profile0 : "Inactive"
    state.profile1mode = (profile1 != null) ? profile1 : "Profile 1"
    state.profile2mode = (profile2 != null) ? profile2 : "Profile 2"
    state.profile3mode = (profile3 != null) ? profile3 : "Profile 3"
    state.profile4mode = (profile4 != null) ? profile4 : "Profile 4"
    state.profile5mode = (profile5 != null) ? profile5 : "Profile 5"
    state.profile6mode = (profile6 != null) ? profile6 : "Profile 6"
    state.profile7mode = (profile7 != null) ? profile7 : "Profile 7"
    sendEvent(name: "profile0mode", value: "${state.profile0mode}", displayed: false)
    sendEvent(name: "profile1mode", value: "${state.profile1mode}", displayed: false)
    sendEvent(name: "profile2mode", value: "${state.profile2mode}", displayed: false)
    sendEvent(name: "profile3mode", value: "${state.profile3mode}", displayed: false)
    sendEvent(name: "profile4mode", value: "${state.profile4mode}", displayed: false)
    sendEvent(name: "profile5mode", value: "${state.profile5mode}", displayed: false)
    sendEvent(name: "profile6mode", value: "${state.profile6mode}", displayed: false)
    sendEvent(name: "profile7mode", value: "${state.profile7mode}", displayed: false)
    if(loggingOn) log.debug "profile0mode is ${state.profile0mode}, profile1mode is ${state.profile1mode}, profile2mode is ${state.profile2mode}, profile3mode is ${state.profile3mode}, profile4mode is ${state.profile4mode}, profile5mode is ${state.profile5mode}, profile6mode is ${state.profile6mode}, profile7mode is ${state.profile7mode}"
	
    //Get current Blue Iris Server Status
    retrieveCurrentStatus()
    runEvery15Minutes(refresh) //this is the "polling" to update the device.  todo: Should be able to delete once we have a service manager app (poll() runs every 10min in the app automatically).
}

def parse(description) {
    if(loggingOn) log.debug "Parsing got something"   
	state.hubCommandReceivedTime = now()
    
    //Parse Blue Iris Server Response to command from SmartThings
    def msg = parseLanMessage(description)
    def headersAsString = msg.header // => headers as a string
    def headerMap = msg.headers      // => headers as a Map
    def body = msg.body              // => request body as a string
    def status = msg.status          // => http status code of the response
    if(loggingOn) {
        log.debug "msg: ${msg}"
        log.debug "headersAsString: ${headersAsString}"
        log.debug "headerMap: ${headerMap}"
        log.debug "body: ${body}"
        log.debug "status: ${status}"
    }
    
    //Modify the received messages to be useful
    def bodyjustvalues = body - "signal=" - "profile="
    def bodyArray = bodyjustvalues.split()
    def newSignal = bodyArray[0]
    def newProfileNum = bodyArray[1].toInteger()
    def newProfileName = getprofileName(newProfileNum)
    if(loggingOn) log.debug "msg.body results: profile is $newProfileNum, signal is $newSignal"
    if(loggingOn) log.debug "status returned ${status.toInteger()}"

	//Update Tiles
    sendEvent(name: "blueIrisProfile", value: "${newProfileName}")
    sendEvent(name: "stoplight", value: "$newSignal")
    sendEvent(name: "sync", value: "default", displayed: false)
    sendEvent(name: "refresh", value: "default", displayed: false)
    if (newProfileNum ==0) sendEvent(name: "profile0mode", value: "on", displayed: false)
    if (newProfileNum ==1) sendEvent(name: "profile1mode", value: "on", displayed: false)
    if (newProfileNum ==2) sendEvent(name: "profile2mode", value: "on", displayed: false)
    if (newProfileNum ==3) sendEvent(name: "profile3mode", value: "on", displayed: false)
    if (newProfileNum ==4) sendEvent(name: "profile4mode", value: "on", displayed: false)
    if (newProfileNum ==5) sendEvent(name: "profile5mode", value: "on", displayed: false)
    if (newProfileNum ==6) sendEvent(name: "profile6mode", value: "on", displayed: false)
    if (newProfileNum ==7) sendEvent(name: "profile7mode", value: "on", displayed: false)
    if (newProfileNum !=0) sendEvent(name: "profile0mode", value: "${state.profile0mode}", displayed: false)
    if (newProfileNum !=1) sendEvent(name: "profile1mode", value: "${state.profile1mode}", displayed: false)
    if (newProfileNum !=2) sendEvent(name: "profile2mode", value: "${state.profile2mode}", displayed: false)
    if (newProfileNum !=3) sendEvent(name: "profile3mode", value: "${state.profile3mode}", displayed: false)
    if (newProfileNum !=4) sendEvent(name: "profile4mode", value: "${state.profile4mode}", displayed: false)
    if (newProfileNum !=5) sendEvent(name: "profile5mode", value: "${state.profile5mode}", displayed: false)
    if (newProfileNum !=6) sendEvent(name: "profile6mode", value: "${state.profile6mode}", displayed: false)
    if (newProfileNum !=7) sendEvent(name: "profile7mode", value: "${state.profile7mode}", displayed: false)

    //Notify if there are errors
    if(state?.desiredNewProfile && state?.desiredNewProfile != newProfileName) {
        log.error "error 1: ${state.desiredNewProfile} != ${newProfileName}"
        sendEvent(name: "errorMessage", value: "Error! BI profile should be ${state.desiredNewProfile}, but it is ${newProfileName}")
    }
    if(state?.desiredNewStoplightColor && state?.desiredNewStoplightColor != newSignal) {
        if(state.desiredNewStoplightColor == 'yellow' && newSignal == 'green'){
            //Do nothing, not a problem, if the user has Blue Iris to skip yellow, or the yellow delay is a very short time, it'll end up on green
        } else {
            log.error "error 2: ${state.desiredNewStoplightColor} != ${newSignal}"
            sendEvent(name: "errorMessage", value: "Error! BI Traffic Light should be ${state.desiredNewStoplightColor}, but it is ${newSignal}")
        }
    }
    if(status.toInteger() != 200){   //200 is ok, 100-300 series should be ok usually, 400 & 500 series are definitely problems
        if (status.toInteger() >= 400){
            log.error "error 3: msg.status returned ${status.toInteger()}"
            sendEvent(name: "errorMessage", value: "Error! BI server returned http status code ${status.toInteger()}, an error.")
        } else {
            log.error "error 4: msg.status returned ${status.toInteger()}"
            sendEvent(name: "errorMessage", value: "BI server returned http status code ${status.toInteger()}, which may indicate an error.")
        }
    }

    /////////////////////Testing Block////////////////////
    //log.debug "11 ${device.currentState("blueIrisProfile").value}"  //returns attribute value, eg Away
    //log.debug "12 ${device.currentState("blueIrisProfile").name}" //returns attribute name, eg blueIrisProfile
    //////////////////////////////////////////////////////
}

def changeHoldTemp() {
    if(loggingOn) log.debug "changeHoldTemp() called, state.holdChange is ${state.holdChange}"
    if(state.holdChange) {
        state.holdChange = false
        sendEvent(name: "holdTemp", value: "Temporary", displayed: false)
    }
    else {
        state.holdChange = true
        sendEvent(name: "holdTemp", value: "Hold", displayed: false)
    }
    if(loggingOn) log.debug "changeHoldTemp() done, state.holdChange is now ${state.holdChange}"
}

def retrieveCurrentStatus() {
    if(loggingOn) log.debug "Executing 'retrieveCurrentStatus'"
    def retrieveProfileCommand = "/admin&user=${username}&pw=${password}"
    hubTalksToBI(retrieveProfileCommand)
}

def refresh() {
    if(loggingOn) log.debug "Executing 'refresh'" 
    retrieveCurrentStatus()
}

def setBlueIrisStopLight() {
    if(loggingOn) log.debug "Executing 'setBlueIrisStopLight' with stoplight currently ${device.currentState("stoplight").value}"  
    //Blue Iris http command "/admin?signal=x" Changes the traffic signal state and returns the current state.  
    //x=0 for red (not recording), x=1 for green (recording, x=2 for yellow (pause before starting to record).  This requires admin authentication.   
    def stopligthNow = device.currentState("stoplight").value
    def newStoplight = 2
    if(stopligthNow == 'red') {
        newStoplight = 2
        state.desiredNewStoplightColor = 'yellow'
    } else if(stopligthNow == 'yellow'){
        newStoplight = 1
        state.desiredNewStoplightColor = 'green'
    } else if(stopligthNow == 'green'){
        newStoplight = 0
        state.desiredNewStoplightColor = 'red'
    }
    def changeStopLightCommand = "/admin?signal=${newStoplight}&user=${username}&pw=${password}"
    hubTalksToBI(changeStopLightCommand)
    if(loggingOn) log.debug "'setBlueIrisStopLight' complete, changing stoplight to ${state.desiredNewStoplightColor}"
}

def syncBIprofileToSmarthThings() {
    def smartThingsMode = location.currentMode.name
    def profileToSet = getprofileNumber(smartThingsMode)
    if(loggingOn) log.debug "Executing 'syncBIprofileToSmarthThings', mode is ${location.mode}, sending profile $profileToSet"
    setProfile(profileToSet)
}

def setBlueIrisProfile0() {
    if(loggingOn) log.debug "Executing 'setBlueIrisProfile0'"
    setProfile(0)
}

def setBlueIrisProfile1() {
    if(loggingOn) log.debug "Executing 'setBlueIrisProfile1'"
    setProfile(1)
}

def setBlueIrisProfile2() {
    if(loggingOn) log.debug "Executing 'setBlueIrisProfile2'"
    setProfile(2)
}

def setBlueIrisProfile3() {
    if(loggingOn) log.debug "Executing 'setBlueIrisProfile3'"
    setProfile(3)
}

def setBlueIrisProfile4() {
    if(loggingOn) log.debug "Executing 'setBlueIrisProfile4'"
    setProfile(4)
}

def setBlueIrisProfile5() {
    if(loggingOn) log.debug "Executing 'setBlueIrisProfile5'"
    setProfile(5)
}

def setBlueIrisProfile6() {
    if(loggingOn) log.debug "Executing 'setBlueIrisProfile6'"
    setProfile(6)
}

def setBlueIrisProfile7() {
    if(loggingOn) log.debug "Executing 'setBlueIrisProfile7'"
    setProfile(7)
}

def setProfile(profile) {
    if (loggingOn) "setprofile() received $profile"
    //For future expansion...  todo -- can you call 'device.setProfile(2)' from other apps?
    def name = getprofileName(profile)
    if (loggingOn) log.debug "Changing Blue Iris Profile to ${profile}, named '${name}'"
    state.desiredNewProfile = name
    //Blue Iris Param "&lock=0/1/2" makes profile changes as: run/temp/hold
    def lock = 1
    if(state.holdChange) {lock = 2}
    def changeProfileCommand = "/admin?profile=${profile}&lock=${lock}&user=${username}&pw=${password}"
    hubTalksToBI(changeProfileCommand)
}

def hubTalksToBI(command) {
	state.hubCommandSentTime = now()
    def biHost = "${host}:${port}"
    def sendHTTP = new physicalgraph.device.HubAction(
        method: "GET",
        path: command,
        headers:    [
            HOST:       biHost,
            Accept:     "*/*",
        ]
    )
    if (loggingOn) log.debug sendHTTP
    state.hubCommandSentTime = now()
    runIn(state.serverResponseThreshold, serverOfflineChecker)
    sendHubCommand(sendHTTP)
}

def serverOfflineChecker() {  //todo: Note - this won't work if I figure out how to subscribe to motion events, probably should fix my SmartPing app.
    double responseTime = (state.hubCommandReceivedTime - state.hubCommandSentTime) / 1000  //response time in seconds
    if (loggingOn) log.debug "serverOfflineChecker() found server response time was ${responseTime}"
    if (responseTime > state.serverResponseThreshold) {
        log.error "error 9: Server Response time was ${responseTime} seconds, the server is offline."
        sendEvent(name: "errorMessage", value: "Error! Blue Iris Server is offline!")  //It has to be the BI server or the SmartThing hub's connection to the BI Server (because otherwise you're hub would be offline too and the SmartThings app would tell you.)
    }
}

def getprofileName(number) {
    if (loggingOn) log.debug "getprofileName got ${number}"
    def name = 'Away'
    if (number == 0) {name = state.profile0mode}
    else if (number == 1) {name = state.profile1mode}
    else if (number == 2) {name = state.profile2mode}
    else if (number == 3) {name = state.profile3mode}
    else if (number == 4) {name = state.profile4mode}
    else if (number == 5) {name = state.profile5mode}
    else if (number == 6) {name = state.profile6mode}
    else if (number == 7) {name = state.profile7mode}
    else {
        log.error "error 10: getprofileName(number) got a profile number outside of the 0-7 range, check the settings of what you passed it."
        sendEvent(name: "errorMessage", value: "Error! A profile number was passed outside of the 0-7 range. Check settings.")
    }
    if (loggingOn) log.debug "getprofileName returning ${name}"
    return name
}

def getprofileNumber(name) {
    if (loggingOn) log.debug "getprofileNumber got ${name}"
    def number = 1
    if (name == state.profile0mode) {number = 0}
    else if (name == state.profile1mode) {number = 1}
    else if (name == state.profile2mode) {number = 2}
    else if (name == state.profile3mode) {number = 3}
    else if (name == state.profile4mode) {number = 4}
    else if (name == state.profile5mode) {number = 5}
    else if (name == state.profile6mode) {number = 6}
    else if (name == state.profile7mode) {number = 7}
    else {
        log.error "error 11: getprofileNumber(name) got a name that isn't one of the user defined profiles, check profile name settings"
        sendEvent(name: "errorMessage", value: "Error! A profile name was passed that isn't one of the user defined profiles. Check settings.")
    }
    if (loggingOn) log.debug "getprofileNumber returning ${number}"
    return number
}

private String convertIPtoHex(ipAddress) {
    try {
        String hex = ipAddress.tokenize('.').collect {String.format('%02x', it.toInteger())}.join()
        return hex
    } catch (Exception e) {
        log.error "error 12: Invalid IP Address $ipAddress, check settings. Error: $e"
    }
}

private String convertPortToHex(port) {
    if (!port || (port == 0)) {
        log.error "error 13: Invalid port $port, check settings."
    }

    try {
        String hexport = port.toString().format('%04x', port.toInteger())
        return hexport
    } catch (Exception e) {
        log.error "error 14: Invalid port $port, check settings. Error: $e"
    }
}



/* todo: see if this can work for collecting motion alerts (have BI alerts send to the hub for this to catch??)
Subscribing to device Events
If you’d like to hear back from a LAN-connected device upon a particular Event, you can subscribe using a HubAction. The parse method will be called when this Event is fired on the device.

Here’s an example using UPnP:

def someCommand() {
subscribeAction("/path/of/event")
}

private subscribeAction(path, callbackPath="") {
log.trace "subscribe($path, $callbackPath)"
def address = getCallBackAddress()
def ip = getHostAddress()

def result = new physicalgraph.device.HubAction(
method: "SUBSCRIBE",
path: path,
headers: [
HOST: ip,
CALLBACK: "<http://${address}/notify$callbackPath>",
NT: "upnp:event",
TIMEOUT: "Second-28800"
]
)

log.trace "SUBSCRIBE $path"

return result
}
*/