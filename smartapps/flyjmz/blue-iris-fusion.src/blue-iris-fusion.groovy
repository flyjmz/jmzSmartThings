/*
Blue Iris Fusion  (parent app, child app is Blue Iris Camera Triggers - Trigger)

Created by FLYJMZ (flyjmz230@gmail.com)

Based on work by:
Tony Gutierrez in "Blue Iris Profile Integration"
jpark40 at https://community.smartthings.com/t/blue-iris-profile-trigger/17522/76
luma at https://community.smartthings.com/t/blue-iris-camera-trigger-from-smart-things/25147/9

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
for the specific language governing permissions and limitations under the License.
*/


//////////////////////////////////////////////////////////////////////////////////////////////
///										App Info											//
//////////////////////////////////////////////////////////////////////////////////////////////
/*
SmartThings Community Thread: 
	NEW:  https://community.smartthings.com/t/release-bi-fusion-v3-0-adds-blue-iris-device-type-handler-blue-iris-camera-dth-motion-sensing/103032
    OLD:  https://community.smartthings.com/t/release-blue-iris-fusion-integrate-smartthings-and-blue-iris/54226

Github Code: 
		https://github.com/flyjmz/jmzSmartThings/tree/master/smartapps/flyjmz/blue-iris-fusion.src

Child app can be found on Github: 
		https://github.com/flyjmz/jmzSmartThings/tree/master/smartapps/flyjmz/blue-iris-fusion-trigger.src

Blue Iris Server Device Type Handler on Github: 
		https://github.com/flyjmz/jmzSmartThings/tree/master/devicetypes/flyjmz/blue-iris-server.src

Server DTH SmartThings Community Thread:  
		https://community.smartthings.com/t/release-blue-iris-device-handler/

Blue Iris Camera Device Type Handler on Github: 
		https://github.com/flyjmz/jmzSmartThings/tree/master/devicetypes/flyjmz/blue-iris-camera.src

Version 1.0 - 30July2016    Initial release
Version 1.1 - 3August2016   Cleaned up Code
Version 2.0 - 16Oct2016     Added Profile integration.  Also set up option for local connections, but doesn't work.  Standby for updates to make it work.
Version 2.1 - 14Dec2016     Got local connection to work!  If you have issues, try external.  External is very stable.
Version 2.2 - 2Jan2017		Found out the local connection issue, "Local Only" setting in Blue Iris Webserver Settings cannot be checked.
Version 2.3 - 17Jan2017     Added preference to turn debug logging on or off.
Version 2.4 - 22Jan2017     Fixed error in profile change notifications (they all said temporary even if it was a hold change)
Version 2.5 - 23Jan2017     Slight tweak to notifications.
Version 2.6 - 17Jun2017		Fixed Profile names when using in LAN (localAction was throwing NULL). Thanks Zaxxon!
Version 3.0 - 26Oct2017		Added Blue Iris Server and Camera Device Type Integration with motion, profile integration, manual triggering and manual profile switching.
							Also added App Update Notifications, cleaned up notifications, added OAuth for motion alerts

TODO:
-Try to get motion alerts from BI to Camera Devices without using OAuth.  Some example code in here already (lanEventHandler), and look at:
https://community.smartthings.com/t/smartthings-labs-and-lan-devices/1100/11
https://community.smartthings.com/t/poll-or-subscribe-example-to-network-events/72862/15
and maybe:
https://community.smartthings.com/t/help-receiving-http-events-from-raspberry-pi/3629/14
https://community.smartthings.com/t/tutorial-creating-a-rest-smartapp-endpoint/4331
*/

def appVersion() {"3.0"}

mappings {
    path("/active/:camera") {
        action: [GET: "cameraActiveHandler"]
    }
    path("/inactive/:camera") {
        action: [GET: "cameraInactiveHandler"]
    }
}

definition(
    name: "Blue Iris Fusion",
    namespace: "flyjmz",
    author: "flyjmz230@gmail.com",
    description: "Full SmartThings mode integration with Blue Iris Profiles, plus SmartThings can trigger Blue Iris Camera recording, and use Blue Iris Cameras as motion sensors.",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo.png",
    iconX2Url: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo%402x.png",
    singleInstance: true
)

preferences {
    page(name:"BIFusionSetup")
    page(name:"BIServerSetup")
    page(name:"integrationSetup")
    page(name:"cameraDeviceSetup")
    page(name:"oauthSetup")
    page(name:"oauthView")
}

def BIFusionSetup() {
    dynamicPage(name:"BIFusionSetup", title: "BI Fusion Setup", install: true, uninstall: true, submitOnChange: true) {
        section("Blue Iris Server Settings") {
            href(name: "BIServerSetup", title: "Blue Iris Server Settings", required: false, page: "BIServerSetup")
        }
        section("Blue Iris Profile <=> SmartThings Mode Integration") {
            href(name: "integrationSetup", title: "Blue Iris Profile <=> SmartThings Mode Integration", required: false, page: "integrationSetup")
        }
        section("Blue Iris Camera Installation") {
            href(name: "cameraDeviceSetup", title: "Blue Iris Camera Installation", required: false, page: "cameraDeviceSetup")
        }
        section("Blue Iris Camera Triggers") {
            app(name: "Blue Iris Fusion - Trigger", appName: "Blue Iris Fusion - Trigger", namespace: "flyjmz", title: "Add Camera Trigger", multiple: true)
        }
        section("Notification Delivery Settings", hidden: false, hideable: true) {
            input("recipients", "contact", title: "Send notifications to") {
                input "pushAndPhone", "enum", title: "Also send SMS? (optional, it will always send push)", required: false, options: ["Yes", "No"]      
                input "phone", "phone", title: "Phone Number (only for SMS)", required: false
                paragraph "If outside the US please make sure to enter the proper country code"
            }
            def receiveUpdateAlerts = true
            input "receiveUpdateAlerts", "bool", title: "Do you want to recieve software update alerts?", required:false
        }
        section("Debug", hidden: true, hideable: true){
            paragraph "You can turn on debug logging, viewed in Live Logging on the API website."
            def loggingOn = false
            input "loggingOn", "bool", title: "Debug Logging On?"
        }
    }
}

def BIServerSetup() {
    dynamicPage(name:"BIServerSetup", title: "BI Server Setup", submitOnChange: true) {
        section("Blue Iris Server Device Type") {
            paragraph "Blue Iris Server provides more complete BI control through ST.  See https://community.smartthings.com/t/release-blue-iris-device-handler/101765"
            input "usingBIServer", "bool", title: "Use/Install Blue Iris Server?", required: true, submitOnChange: true
            paragraph "NOTE: If you want to remove the device but keep BI Fusion installed, just turn this off."
            paragraph "NOTE: The Blue Iris Server Device Type requires a 'local' connection between the ST hub and BI server.  BI Fusion can handle external connections, but not when using the Server Device."
            if(usingBIServer) {
                paragraph "NOTE: If you already have the Server Device installed, you'll need to remove it before continuing. Apologies."
                paragraph "NOTE: Ensure the Blue Iris Server Device Type Handler is already added to your account on the SmartThings API."
                paragraph "NOTE: Once installed, do not edit the server device settings from within the device's preferences page. Make all changes within this BI Fusion app."
            }
        }
        section("Blue Iris Server Login Settings") {
            input "username", "text", title: "Blue Iris Username", required: true
            input "password", "password", title: "Blue Iris Password", required: true
            paragraph "Note: Blue Iris only allows Admin Users to toggle profiles."
            if(usingBIServer) {

                input "host", "text", title: "Blue Iris Server IP (only include the IP)", description: "e.g. 192.168.0.14", required:true
                input "port", "number", title: "Blue Iris Server Port", description: "e.g. 81", required:true
                paragraph "NOTE: Ensure 'Secure Only' is not checked in Blue Iris Webserver settings."
                double waitThreshold = 5
                input "waitThreshold", "number", title: "Blue Iris Server Health Monitor: Enter the max server response time:", description: "Default: 5 seconds", required:false, displayDuringSetup: true 
            } else {
                paragraph "Local or External Connection to Blue Iris Server (i.e. LAN vs WAN)?"
                paragraph "(External requires port forwarding/VPN/etc so the SmartThings Cloud can reach your BI server.)"
                paragraph "(Local does not support notifications confirming the changes were actually made.)"
                input "localOnly", "bool", title: "Local connection?", required: true, submitOnChange: true
                if (localOnly) {
                    paragraph "NOTE: When using a local connection, you need to ensure 'Secure Only' is not checked in Blue Iris' Webserver settings."
                    paragraph "Use the local IP address for Host, do not include http:// or anything but the IP address."
                    input "host", "text", title: "BI Webserver IP Address", description: "e.g. 192.168.0.14", required:true
                } else {
                    paragraph "Since you're using an external connection, use the external IP address for Webserver Host, and be sure to include the full address (i.e. include http:// or https://, .com, etc)."
                    paragraph "If you are using Stunnel, ensure the SSL certificate is from a Certificate Authority (CA), it cannot be self-signed. You can create a free CA signed certificate at www.letsencrypt.org"
                    input "host", "text", title: "BI Webserver Host (include http(s)://)", required:true
                }
                input "port", "number", title: "BI Webserver Port (e.g. 81)", required:true
            }
        }
    }
}

def integrationSetup() {
    dynamicPage(name:"integrationSetup", title: "Blue Iris Profile <=> SmartThings Mode Integration", submitOnChange: true) {
        section("Blue Iris Profile/SmartThings Mode Integration") {
            paragraph "You can have BI Fusion update Blue Iris Profiles based on SmartThings Modes."
            input "autoModeProfileSync", "bool", title: "Auto Sync Profile to Mode?", required: true
            paragraph "Enter your Blue Iris Profile Number (0-7) for the matching SmartThings mode. To ignore a mode leave it blank. (Remember '0' normally sets Blue Iris to 'Inactive')."
            location.modes.each { mode ->
                def modeId = mode.id.toString()  
                input "mode-${modeId}", "number", title: "Mode ${mode}", required: false, submitOnChange: true
            }
            if (usingBIServer) {
                paragraph "To give remaining BI Profiles a custom name that are not linked to a SmartThings mode, enter the name under the available Profiles below (leave blank to ignore a profile or use the default)."
                def takenProfiles = [9,10] //make sure it creates a list, we know 9 and 10 are garbage, so it won't matter.
                location.modes.each { mode ->
                    def checkMode = "mode-${mode.id.toString()}"
                    if (settings[checkMode]) {
                        takenProfiles += settings[checkMode].toInteger()
                    }
                }
                if (loggingOn) log.debug "takenProfiles is ${takenProfiles}"
                if (!takenProfiles.contains(0)) input "profile0", "text", title: "ST Mode for BI Inactive (Profile 0)", description: "Default: Inactive", required:false
                if (!takenProfiles.contains(1)) input "profile1", "text", title: "ST Mode for BI Profile #1", description: "Default: Profile 1", required:false
                if (!takenProfiles.contains(2)) input "profile2", "text", title: "ST Mode for BI Profile #2", description: "Default: Profile 2", required:false
                if (!takenProfiles.contains(3)) input "profile3", "text", title: "ST Mode for BI Profile #3", description: "Default: Profile 3", required:false
                if (!takenProfiles.contains(4)) input "profile4", "text", title: "ST Mode for BI Profile #4", description: "Default: Profile 4", required:false
                if (!takenProfiles.contains(5)) input "profile5", "text", title: "ST Mode for BI Profile #5", description: "Default: Profile 5", required:false
                if (!takenProfiles.contains(6)) input "profile6", "text", title: "ST Mode for BI Profile #6", description: "Default: Profile 6", required:false
                if (!takenProfiles.contains(7)) input "profile7", "text", title: "ST Mode for BI Profile #7", description: "Default: Profile 7", required:false
            }
            paragraph "You can make the profile changes either 'Hold' or 'Temporary.'"
            paragraph "Hold changes remain until the next change is made, even through computer/server restart.  Temporary changes will only be in effect for the 'Temp Time' duration set for each profile in Blue Iris Settings > Profiles. At the end of that time, Blue Iris will change profiles according to your schedule."
            paragraph "Note: if Blue Iris restarts while a temporary profile is set, it will set the profile according to it's schedule."
            input "holdTemp", "bool", title: "Make Hold changes?", required: true
            paragraph "Profile changes will display in SmartThings Notifications Feed.  Do you also want to receive PUSH/SMS notifications?"
            input "receiveAlerts", "enum", title: "Receive PUSH/SMS on Profile Change?", options: ["Yes", "Errors Only", "No"], required: true
        }
    }
}

def cameraDeviceSetup() {
    dynamicPage(name:"cameraDeviceSetup", title: "Blue Iris Camera Installation", submitOnChange: true) {
        if (usingBIServer) {
			section("Blue Iris Camera Device Creation") {
                paragraph "You can install devices for each of your cameras to act as motion sensor devices in SmartThings and to allow SmartThings to trigger them to record."
                input "installCamaraDevices", "bool", title: "Install Cameras?", required: true, submitOnChange: true 
                paragraph "NOTE: To uninstall the camera devices but keep BI Fusion installed, just set cameras to install to '0' and turn off this switch"
                if (installCamaraDevices) {
                    paragraph "Ensure the Blue Iris Camera Device Type Handler is already added to your account on the SmartThings API."
                    input "howManyCameras", "number", title: "How many cameras do you want to install?", required: true, submitOnChange: true 
                    paragraph "Create a new device for each camera by entering the Blue Iris short name (case-sensitive, recommend no spaces or special characters)."
                    paragraph "Display Names are optional. They default to 'BI Cam - [short name]'."
                    paragraph "NOTE: You have to click 'Done' to complete BI Fusion setup prior to re-entering settings to create any any triggers."
                    for (int i = 0; i < howManyCameras; i++) {
                        input "camera${i}shortName", "text", title: "Camera ${i} Short Name", description: "e.g. frontporch", required: true
                        input "camera${i}displayName", "text", title: "Camera ${i} Display Name", description: "e.g. Front Porch", required: false
                    }
                }
            }
        }
        section ("Blue Iris Motion Alert Setup") {
            paragraph "You will need to copy the addresses and change the camera names for each camera you want to get motion from. The addresses will display after clicking to open the page below."
            def createNewAddresses = false
            input "createNewAddresses", "bool", title: "Do you want to (re)create the URLs?", required: false, submitOnChange: true
            paragraph "(Leave Off in Order to VIEW ONLY...Not Change)"
        }
        if (createNewAddresses) {
            section("CHANGE & View Motion Alert URLs") {
                href(name: "oauthSetup", title: "CHANGE & View Motion Alert URLs", required: false, page: "oauthSetup")
            }
        } else {
            section("View Only Motion Alert URLs") {
                href(name: "oauthView", title: "View Only Motion Alert URLs", required: false, page: "oauthView")
            }
        }
    }
}

def oauthSetup() {
    dynamicPage(name:"oauthSetup", title: "CHANGED Blue Iris Alert URLs") {
        createBIFusionToken()
        section("") {
            paragraph "Take a screenshot of this page, then enter the addresses following the directions on the previous page."
            paragraph "WARNING: THESE ADDRESSES ARE NEW!"
        }
        section("Motion Active URL") {
            def activeURL = apiServerUrl("/api/smartapps/installations/${app.id}/active/cameraShortNameHere?access_token=${state.accessToken}")
            paragraph "Motion Active URL is: ${activeURL}"
        }
        section("Motion Inactive URL") {
            def inactiveURL = apiServerUrl("/api/smartapps/installations/${app.id}/inactive/cameraShortNameHere?access_token=${state.accessToken}")
            paragraph "Motion Inactive URL is: ${inactiveURL}"
        }
    }
}

def oauthView() {
    dynamicPage(name:"oauthView", title: "CURRENT Blue Iris Alert URLs") {
        section("") {
            paragraph "Take a screenshot of this page, then enter the addresses following the directions on the previous page."
        }
        section("Motion Active URL") {
            def activeURL = apiServerUrl("/api/smartapps/installations/${app.id}/active/cameraShortNameHere?access_token=${state.accessToken}")
            paragraph "Motion Active URL is: ${activeURL}"
        }
        section("Motion Inactive URL") {
            def inactiveURL = apiServerUrl("/api/smartapps/installations/${app.id}/inactive/cameraShortNameHere?access_token=${state.accessToken}")
            paragraph "Motion Inactive URL is: ${inactiveURL}"
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unsubscribe()
    unschedule()
    initialize()
}

def uninstalled() {
    removeChildDevices(getChildDevices(true))
    unschedule()
    revokeAccessToken()
}

private removeChildDevices(delete) {
    delete.each {
        deleteChildDevice(it.deviceNetworkId)
    }
}

def initialize() {
	log.info "initialized"
    createInfoMaps()
    if (autoModeProfileSync) subscribe(location, modeChange)
    if (loggingOn) log.debug "Initialized with settings: ${settings}"
    if (installCamaraDevices) subscribe(location, null, lanEventHandler, [filterEvents:false])  //for new motion...todo - test
    makeDevices()
	schedule(now(), checkForUpdates)
	checkForUpdates()
}

///////////////////////////////////////////////////////////////////////////
//					BI FUSION 3.X Code (Uses Device Type Handlers)		///
///////////////////////////////////////////////////////////////////////////
def createInfoMaps() {
    //First create Profile:
    if (state.profileModeMap != null) state.profileModeMap.clear()  //wipes it clean to prevent weird carryover
    state.profileModeMap = [[modeName: "Inactive"],
                            [modeName: "Profile 1"], 
                            [modeName: "Profile 2"],
                            [modeName: "Profile 3"],
                            [modeName: "Profile 4"],
                            [modeName: "Profile 5"],
                            [modeName: "Profile 6"],
                            [modeName: "Profile 7"]]  //Don't need the numbers, to get Profile 1's Mode's Name use: state.profileModeMap[1].modeName
    if (profile0 != null) state.profileModeMap[0].modeName = profile0
    if (profile1 != null) state.profileModeMap[1].modeName = profile1
    if (profile2 != null) state.profileModeMap[2].modeName = profile2
    if (profile3 != null) state.profileModeMap[3].modeName = profile3
    if (profile4 != null) state.profileModeMap[4].modeName = profile4
    if (profile5 != null) state.profileModeMap[5].modeName = profile5
    if (profile6 != null) state.profileModeMap[6].modeName = profile6
    if (profile7 != null) state.profileModeMap[7].modeName = profile7
    location.modes.each { mode ->
        def checkMode = "mode-${mode.id.toString()}"
        if (settings[checkMode]) {
            state.profileModeMap[settings[checkMode].toInteger()].modeName = "${mode.name}"	//For each ST mode, it determines if the user made profile number for it in settings, then uses that profile number as the map value number and fills the name.
        }
    }
    if (loggingOn) log.debug "state.profileModeMap map: ${state.profileModeMap}"
    
    //Second create BI Server Settings Map:
    if (state.blueIrisServerSettings != null) state.blueIrisServerSettings.clear()  //wipes it clean to prevent weird carryover
    state.blueIrisServerSettings = [:]
    state.blueIrisServerSettings.host = host
    state.blueIrisServerSettings.port = port
    state.blueIrisServerSettings.username = username
    state.blueIrisServerSettings.password = password
    state.blueIrisServerSettings.profileModeMap = state.profileModeMap
    //the network ID needs to be the hex ip:port or mac address for the BI Server Computer (I use IP because it's easier for user, but mac would be easier to code):
    def hosthex = convertIPtoHex(host).toUpperCase()  //Note: it needs to be set to uppercase for the new deviceNetworkId to work in SmartThings
    def porthex = convertPortToHex(port).toUpperCase()
   	if (usingBIServer) state.blueIrisServerSettings.DNI = "$hosthex:$porthex"
    else state.blueIrisServerSettings.DNI = null
    state.blueIrisServerSettings.waitThreshold = waitThreshold
    state.blueIrisServerSettings.holdTemp = holdTemp
    state.blueIrisServerSettings.loggingOn = loggingOn
    if (loggingOn) log.debug "state.blueIrisServerSettings map: ${state.blueIrisServerSettings}"

    //Third create the Camera Devices Map:
    if (state.biCamera != null) state.biCamera.clear()  //wipes it clean to prevent weird carryover
    state.biCamera = [[:]]		//[[deviceDNI: "", shortName: "", displayName: ""], [deviceDNI: "", shortName: "", displayName: ""]]

    //First update the map:
    if (installCamaraDevices) {
        for (int i = 0; i < howManyCameras; i++) {
            def cameraShortNameInput = "camera${i}shortName"
            def cameraDisplayNameInput = "camera${i}displayName"
            state.biCamera[i].deviceDNI = "bicamera" + i + settings[cameraShortNameInput].toString()
            state.biCamera[i].shortName = settings[cameraShortNameInput].toString()
            if (settings[cameraDisplayNameInput]?.toString() == null) state.biCamera[i].displayName = "BI Cam - " + settings[cameraShortNameInput].toString()
            else state.biCamera[i].displayName = settings[cameraDisplayNameInput].toString()
        }
        if (loggingOn) log.debug "state.biCamera map: ${state.biCamera}"
    }
}

def makeDevices() {
    //First, delete any old devices not in the settings any more:
    def installedChildDevices = getChildDevices(true)
    def wantedChildDevices = []
    if (wantedChildDevices != null) wantedChildDevices.clear()
    def serverDNI = state.blueIrisServerSettings.DNI.toString()
    wantedChildDevices += serverDNI
    state.biCamera?.deviceDNI.each {
        if (it != null) {
            wantedChildDevices += it
        }
    }
    if (loggingOn) log.debug "installedChildDevices found: $installedChildDevices, and wantedChildDevices are: $wantedChildDevices"
    installedChildDevices.each {
        def childDNI = it.deviceNetworkId
        if (it != null && !wantedChildDevices.contains(childDNI)) {
            deleteChildDevice(it.deviceNetworkId) 
        } //else not deleting since we want it
    }
    //Then install devices if user wants:
    if (usingBIServer) createBlueIrisServerDevice()
    if (installCamaraDevices) createCameraDevice()   
}


//////////////////////   	Server Device Creation 		////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////

def createBlueIrisServerDevice() {
    def serverDevice = getChildDevice(state.blueIrisServerSettings.DNI)       
    try {
        if (!serverDevice) { //double check that it isn't already installed
            serverDevice = addChildDevice("flyjmz", "Blue Iris Server", state.blueIrisServerSettings.DNI, location.hubs[0].id, [name: "Blue Iris Server", label: "Blue Iris Server", completedSetup: true])
            log.info "Blue Iris Server Created"
        } else {
            log.info "Blue Iris Server already created"
        }
    } catch (e) {
        log.error "Error creating Blue Iris Server device: ${e}"
    }

    //Update the Server Settings regardless of whether it was just created or not, and subscribe to it's events:
    serverDevice.initializeServer(state.blueIrisServerSettings)
    if (receiveAlerts == "Errors Only") {
        subscribe(serverDevice, "errorMessage", serverDeviceErrorMessageHandler)  //returns a message string
    } else if (receiveAlerts == "Yes") {
        subscribe(serverDevice, "errorMessage", serverDeviceErrorMessageHandler)  //returns a message string
        subscribe(serverDevice, "blueIrisProfile", serverDeviceProfileHandler) //returns profile number
        subscribe(serverDevice, "stoplight", serverDeviceStopLightHandler)  //returns ["red", "green", "yellow"]
    } else if (receiveAlerts == "No") {
        //no server events to subscribe to, because we don't receive any that are just for messaging
    }
    
    //Code for motion active/inactive from BI Server Device.  OAuth setup overrode this, but I'd like to go back (todo):
    //subscribe(serverDevice, "cameraMotionActive", cameraActiveHandler)	
    //subscribe(serverDevice, "cameraMotionInactive", cameraInactiveHandler)
}					

def serverDeviceProfileHandler(evt) {
    if (loggingOn) log.debug "serverDeviceProfileHandler() received {$evt}"
    send("Blue Iris Profile set to ${evt.value}")
}

def serverDeviceStopLightHandler(evt) {
    if (loggingOn) log.debug "serverDeviceStopLightHandler() received {$evt}"
    send("Blue Iris Stoplight set to ${evt.value}")
}

def serverDeviceErrorMessageHandler(evt) {
    if (loggingOn) log.debug "serverDeviceErrorMessageHandler() received {$evt}"
    send("${evt.descriptionText}")
}

/*		//Code for motion active/inactive from BI Server Device.  OAuth setup overrode this, but I'd like to go back (todo).
def cameraActiveHandler(evt) {  //receives triggered status from BI through BI Server Device, and sends it to the Camera device
    if (loggingOn) log.debug "cameraActiveHandler() got event: '${evt.displayName}'. Camera '${evt.value}' is active."
    log.trace "cameraActiveHandler() got event: '${evt.displayName}'. Camera '${evt.value}' is active."
    def cameraDNI = ""
    def biCameraSize = state.biCamera.size()
    for (int i = 0; i < biCameraSize; i++) {
        if (state.biCamera[i].shortName == evt.value) cameraDNI = state.biCamera[i].deviceDNI
    }
    log.trace "cameraDNI is $cameraDNI"
    def cameraDevice = getChildDevice(cameraDNI)
    cameraDevice.active()
}

def cameraInactiveHandler(evt) {  //receives triggered status from BI through BI Server Device, and sends it to the Camera device
    if (loggingOn) log.debug "cameraInactiveHandler() got event: '${evt.displayName}'. Camera '${evt.value}' is inactive."
    def cameraDNI = ""
    def biCameraSize = state.biCamera.size()
    for (int i = 0; i < biCameraSize; i++) {
        if (state.biCamera[i].shortName == evt.value) cameraDNI = state.biCamera[i].deviceDNI
    }
    log.trace "cameraDNI is $cameraDNI"
    def cameraDevice = getChildDevice(cameraDNI)
    cameraDevice.inactive()
}
*/

//////////////////////   	Camera Device Creation 		////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////
def createCameraDevice() {
    for (int i = 0; i < howManyCameras; i++) {  
        def cameraDevice = getChildDevice(state.biCamera[i].deviceDNI)
        if (!cameraDevice) {	//double check that it isn't already installed
            try {
                cameraDevice = addChildDevice("flyjmz", "Blue Iris Camera", state.biCamera[i].deviceDNI, location.hubs[0].id, [name: "${state.biCamera[i].shortName}", label: "${state.biCamera[i].displayName}", completedSetup: true])
                if (loggingOn) log.debug "'${state.biCamera[i].displayName}' Device Created"
                subscribe(cameraDevice, "switch.on", cameraTriggerHandler)
            } catch (e) {
                log.error "Error creating '${state.biCamera[i].displayName}' Device: ${e}"
            }
        } else {
            if (loggingOn) log.debug "Camera with dni of '${state.biCamera[i].deviceDNI}' already exists."
            subscribe(cameraDevice, "switch.on", cameraTriggerHandler) //still have to subscribe (intialize() wiped old subscriptions)
        }
    }
}

def cameraTriggerHandler(evt) {  //sends command to camera through the BI Server Device to start recording whenever the camera device is 'turned on'.
    if (loggingOn) log.debug "cameraTriggerHandler() got event ${evt.displayName} is ${evt.value}"
    def shortName = ""
    def biCameraSize = state.biCamera.size()
    for (int i = 0; i < biCameraSize; i++) {
        if (state.biCamera[i].displayName == evt.displayName) shortName = state.biCamera[i].shortName
    }
    def serverDevice = getChildDevice(state.blueIrisServerSettings.DNI)
    serverDevice.triggerCamera(shortName)
}


/////   				Camera Motion Code (Using OAuth) 						   /////
////////////////////////////////////////////////////////////////////////////////////////

def createBIFusionToken() {
    try {
        if (state.accessToken) revokeAccessToken()
        createAccessToken()
		log.info "created new token"
    } catch (Exception e) {
        log.error "Error: Can't create access token, is OAuth enabled in the SmartApp Settings? Error: $e"
        send("Error: Can't create access token, is OAuth enabled in the SmartApp Settings?")
        return
    }
}

def lanEventHandler(evt) {  //todo -- see if i can make this work
	def msg = parseLanMessage(evt.value)
	def body = msg.body
    log.debug "lanEventHandler() got msg $msg and body $body"
    //def headerString = new String(parsedEvent.headers.decodeBase64())		
	//def bodyString = new String(parsedEvent.body.decodeBase64())
}

def cameraActiveHandler() {
    def cameraShortName = params.camera
    log.info "cameraActiveHandler() got $cameraShortName is active."
    try {
        def cameraDNI = ""
        for (int i = 0; i < state.biCamera.size(); i++) {
            if (state.biCamera[i].shortName == cameraShortName) cameraDNI = state.biCamera[i].deviceDNI
        }
        def cameraDevice = getChildDevice(cameraDNI)
        cameraDevice.active()
    } catch (Exception e) {
        log.error "error 30: Active Camera Motion Received but failed to send motion to ST device. Error: $e"
        sendEvent(name: "errorMessage", value: "Active Camera Motion Received but failed to send motion to ST device, check settings", descriptionText: "Active Camera Motion Received but failed to send motion to ST device, check settings", displayed: true)
    }
}

def cameraInactiveHandler() {
    def cameraShortName = params.camera
    log.info "cameraInactiveHandler() got $cameraShortName is inactive."
    try {
        def cameraDNI = ""
        for (int i = 0; i < state.biCamera.size(); i++) {
            if (state.biCamera[i].shortName == cameraShortName) cameraDNI = state.biCamera[i].deviceDNI
        }
        def cameraDevice = getChildDevice(cameraDNI)
        cameraDevice.inactive()
    } catch (Exception e) {
        log.error "error 31: Camera Motion Stopped but failed to update ST device. Error: $e"
        sendEvent(name: "errorMessage", value: "Camera Motion Stopped but failed to update ST device, check settings", descriptionText: "Camera Motion Stopped but failed to update ST device, check settings", displayed: true)
    }
}

private String convertIPtoHex(ipAddress) {
    try {
        String hex = ipAddress.tokenize('.').collect {String.format('%02x', it.toInteger())}.join()
        return hex
    } catch (Exception e) {
        log.error "error 12: Invalid IP Address $ipAddress, check settings. Error: $e"
        sendEvent(name: "errorMessage", value: "Invalid IP Address $ipAddress, check settings", descriptionText: "Invalid Blue Iris Server IP Address $ipAddress, check settings", displayed: true)
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


///////////////////////////////////////////////////////////////////////////
//					BI FUSION 2.X Code (No devices required)
///////////////////////////////////////////////////////////////////////////
def modeChange(evt) {
    if (evt.name != "mode") {return;}
    log.info "BI_modeChange detected mode now: " + evt.value
    def checkMode = ""

    location.modes.each { mode ->
        if (mode.name == evt.value){
            checkMode = "mode-" + mode.id
            if (loggingOn) log.debug "BI_modeChange matched to " + mode.name
        }
    }

    if (checkMode != "" && settings[checkMode]){
        if (loggingOn) log.debug "BI_Found profile " + settings[checkMode]
        if (usingBIServer) {
            def device = getChildDevice(state.blueIrisServerSettings.DNI)
            device.setProfile(settings[checkMode].toInteger())  //sends profile change through device
        } else {
            if(localOnly){
                localAction(settings[checkMode].toInteger())	//sends profile change through local lan (like device) except through the app
            } else externalAction(settings[checkMode].toInteger())  //sends profile change from SmartThings cloud to BI server
        }  
    }
}

def localAction(profile) {
    def biHost = "${host}:${port}"
    log.info "Changing Blue Iris Profile to ${profile} via local command"
    def lock = 2  //Blue Iris Param "&lock=0/1/2" makes profile changes as: run/temp/hold
    if(holdTemp) {
        if(receiveAlerts == "No" || receiveAlerts == "Errors Only") sendNotificationEvent("Blue Iris Fusion hold changed Blue Iris to profile ${profile}")
        if(receiveAlerts == "Yes") send("Blue Iris Fusion hold changed Blue Iris to profile ${profile}")
    } else {
        lock = 1
        if(receiveAlerts == "No" || receiveAlerts == "Errors Only") sendNotificationEvent("Temporarily changed Blue Iris to profile ${profile}")
        if(receiveAlerts == "Yes") send("Temporarily changed Blue Iris to profile ${profile}")
    }
    def biRawCommand = "/admin?profile=${profile}&lock=${lock}&user=${username}&pw=${password}"
    def httpMethod = "GET"
    def httpRequest = [
        method:     httpMethod,
        path:       biRawCommand,
        headers:    [
            HOST:       biHost,
            Accept:     "*/*",
        ]
    ]
    def hubAction = new physicalgraph.device.HubAction(httpRequest)
    sendHubCommand(hubAction)
    if (loggingOn) log.debug hubAction
}

def externalAction(profile) {
	log.info "Changing Blue Iris Profile to ${profile} via external command"
    def errorMsg = "Blue Iris Fusion could not adjust Blue Iris Profile"
    if (!holdTemp) {
        try {
            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login"]) { response ->
                if (loggingOn) log.debug response.data

                if (response.data.result == "fail") {
                    if (loggingOn) log.debug "BI_Inside initial call fail, proceeding to login"
                    def session = response.data.session
                    def hash = username + ":" + response.data.session + ":" + password
                    hash = hash.encodeAsMD5()

                    httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login","session":session,"response":hash]) { response2 ->
                        if (response2.data.result == "success") {
                            def BIprofileNames = response2.data.data.profiles
                            if (loggingOn) log.debug ("BI_Logged In")
                            if (loggingOn) log.debug response2.data
                            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","session":session]) { response3 ->
                                if (loggingOn) log.debug ("BI_Retrieved Status")
                                if (loggingOn) log.debug response3.data
                                if (response3.data.result == "success"){
                                    if (response3.data.data.profile != profile){
                                        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","profile":profile,"session":session]) { response4 ->
                                            if (loggingOn) log.debug response4.data
                                            if (response4.data.result == "success") {
                                                if (response4.data.data.profile.toInteger() == profile.toInteger()) {
                                                    if (loggingOn) log.debug ("Blue Iris to profile ${profileName(BIprofileNames,profile)}!")
                                                    if(receiveAlerts == "No" || receiveAlerts == "Errors Only") sendNotificationEvent("Blue Iris Fusion temporarily changed Blue Iris to profile ${profileName(BIprofileNames,profile)}")
                                                    if (receiveAlerts == "Yes") send("Blue Iris Fusion temporarily changed Blue Iris to profile ${profileName(BIprofileNames,profile)}")
                                                } else {
                                                    if (loggingOn) log.debug ("Blue Iris ended up on profile ${profileName(BIprofileNames,response4.data.data.profile)}? Temp change to ${profileName(BIprofileNames,profile)}. Check your user permissions.")
                                                    if (receiveAlerts == "No") sendNotificationEvent("Blue Iris Fusion failed to change Profiles, it is in ${profileName(BIprofileNames,response4.data.data.profile)}? Check your user permissions.")
                                                    if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send("Blue Iris Fusion failed to change Profiles, it is in ${profileName(BIprofileNames,response4.data.data.profile)}? Check your user permissions.")
                                                }
                                                httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"logout","session":session]) { response5 ->
                                                    if (loggingOn) log.debug response5.data
                                                    if (loggingOn) log.debug "Logged out"
                                                }
                                            } else {
                                                if (loggingOn) log.debug "BI_FAILURE"
                                                if (loggingOn) log.debug(response4.data.data.reason)
                                                if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                                                if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
                                            }
                                        }
                                    } else {
                                        if (loggingOn) log.debug ("Blue Iris is already at profile ${profileName(BIprofileNames,profile)}.")
                                        sendNotificationEvent("Blue Iris is already in profile ${profileName(BIprofileNames,profile)}.")
                                    }
                                } else {
                                    if (loggingOn) log.debug "BI_FAILURE"
                                    if (loggingOn) log.debug(response3.data.data.reason)
                                    if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                                    if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
                                }
                            }
                        } else {
                            if (loggingOn) log.debug "BI_FAILURE"
                            if (loggingOn) log.debug(response2.data.data.reason)
                            if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                            if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
                        }
                    }
                } else {
                    if (loggingOn) log.debug "FAILURE"
                    if (loggingOn) log.debug(response.data.data.reason)
                    if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                    if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
                }
            }
        } catch(Exception e) {
            if (loggingOn) log.debug(e)
            if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
            if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
        }
    } else {
        try {
            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login"]) { response ->
                if (loggingOn) log.debug response.data

                if (response.data.result == "fail") {
                    if (loggingOn) log.debug "BI_Inside initial call fail, proceeding to login"
                    def session = response.data.session
                    def hash = username + ":" + response.data.session + ":" + password
                    hash = hash.encodeAsMD5()

                    httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login","session":session,"response":hash]) { response2 ->
                        if (response2.data.result == "success") {
                            def BIprofileNames = response2.data.data.profiles
                            if (loggingOn) log.debug ("BI_Logged In")
                            if (loggingOn) log.debug response2.data
                            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","session":session]) { response3 ->
                                if (loggingOn) log.debug ("BI_Retrieved Status")
                                if (loggingOn) log.debug response3.data
                                if (response3.data.result == "success"){
                                    if (response3.data.data.profile != profile){
                                        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","profile":profile,"session":session]) { response4 ->
                                            if (loggingOn) log.debug response4.data
                                            if (response4.data.result == "success") {
                                                if (loggingOn) log.debug "Set profile to ${profileName(BIprofileNames,profile)} via temp change, trying to set via hold"
                                                if (response4.data.data.profile.toInteger() == profile.toInteger()) {
                                                    httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","profile":profile,"session":session]) { response5 ->
                                                        if (loggingOn) log.debug response5.data
                                                        if (response5.data.result == "success") {
                                                            if (loggingOn) log.debug ("Set profile to ${profileName(BIprofileNames,profile)} with a hold change!")
                                                            if (receiveAlerts == "No" || receiveAlerts == "Errors Only") sendNotificationEvent("Blue Iris Fusion hold changed Blue Iris to profile ${profileName(BIprofileNames,profile)}")
                                                            if (receiveAlerts == "Yes") send("Blue Iris Fusion hold changed Blue Iris to profile ${profileName(BIprofileNames,profile)}")
                                                        } else {
                                                            if (loggingOn) log.debug ("Blue Iris Fusion failed to hold profile, it is in ${profileName(BIprofileNames,response5.data.data.profile)}? but is only temporarily changed.")
                                                            if (receiveAlerts == "No") sendNotificationEvent("Blue Iris Fusion failed to hold profile, it is in ${profileName(BIprofileNames,response5.data.data.profile)}? but is only temporarily changed.")
                                                            if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send("Blue Iris Fusion failed to hold profile, it is in ${profileName(BIprofileNames,response5.data.data.profile)}? but is only temporarily changed.")
                                                        }
                                                    }
                                                } else {
                                                    if (loggingOn) log.debug ("Blue Iris ended up on profile ${profileName(BIprofileNames,response4.data.data.profile)}? Attempt to set ${profileName(BIprofileNames,profile)} failed, also unable to attempt hold. Check your user permissions.")
                                                    if (receiveAlerts == "No") sendNotificationEvent("Blue Iris Fusion failed to change Profiles, it is in ${profileName(BIprofileNames,response4.data.data.profile)}? Check your user permissions.")
                                                    if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send("Blue Iris Fusion failed to change Profiles, it is in ${profileName(BIprofileNames,response4.data.data.profile)}? Check your user permissions.")
                                                }
                                                httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"logout","session":session]) { response6 ->
                                                    if (loggingOn) log.debug response6.data
                                                    if (loggingOn) log.debug "Logged out"
                                                }
                                            } else {
                                                if (loggingOn) log.debug "BI_FAILURE"
                                                if (loggingOn) log.debug(response4.data.data.reason)
                                                if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                                                if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
                                            }
                                        }
                                    } else {
                                        if (loggingOn) log.debug ("Blue Iris is already at profile ${profileName(BIprofileNames,profile)}.")
                                        sendNotificationEvent("Blue Iris is already in profile ${profileName(BIprofileNames,profile)}.")
                                    }
                                } else {
                                    if (loggingOn) log.debug "BI_FAILURE"
                                    if (loggingOn) log.debug(response3.data.data.reason)
                                    if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                                    if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
                                }
                            }
                        } else {
                            if (loggingOn) log.debug "BI_FAILURE"
                            if (loggingOn) log.debug(response2.data.data.reason)
                            if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                            if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
                        }
                    }
                } else {
                    if (loggingOn) log.debug "FAILURE"
                    if (loggingOn) log.debug(response.data.data.reason)
                    if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
                    if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
                }
            }
        } catch(Exception e) {
            if (loggingOn) log.debug(e)
            if (receiveAlerts == "No") sendNotificationEvent(errorMsg)
            if (receiveAlerts == "Yes" || receiveAlerts == "Errors Only") send(errorMsg)
        }
    }
}

def profileName(names, num) {
    if (names[num.toInteger()]) {
        names[num.toInteger()] + " (#${num})"
    } else {
        '#' + num
    }
}

def checkForTriggerUpdates() {
	log.info "Checking for Trigger app updates"
    def childApps = getChildApps()
	def installedVersion = "0.0"
    def installed = false
    if (childApps[0] != null) {
        installedVersion = childApps[0].appVersion()
        installed = true
    } else  //no triggers installed
    if (loggingOn) log.debug "Trigger child app installedVersion is $installedVersion"
    def name = "BI Fusion - Trigger"
    def website = "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/smartapps/flyjmz/blue-iris-fusion-trigger.src/version.txt"
    if (installed) checkUpdates(name, installedVersion, website)
}

def checkForFusionUpdates() {
	log.info "Checking for BI Fusion app updates"
    def installedVersion = appVersion()
    def name = "BI Fusion"
    def website = "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/smartapps/flyjmz/blue-iris-fusion.src/version.txt"
    checkUpdates(name, installedVersion, website)
}

def checkForCameraUpdates() {
	log.info "Checking for Camera device code updates"
    def name = "Blue Iris Camera Device Type Handler"
    def website = "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/devicetypes/flyjmz/blue-iris-camera.src/version.txt"
    def installedVersion = getChildDevice(state.biCamera[0].deviceDNI).appVersion()
    if (installed) checkUpdates(name, installedVersion, website)
}

def checkForServerUpdates() {
    log.info "Checking for Server device code updates"
    def name = "Blue Iris Server Device Type Handler"
    def website = "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/devicetypes/flyjmz/blue-iris-server.src/version.txt"
    def installedVersion = getChildDevice(state.blueIrisServerSettings.DNI).appVersion()
    if (installedVersion) checkUpdates(name, installedVersion, website)
}

def checkForUpdates() {
    checkForFusionUpdates()
    checkForTriggerUpdates()
    if (installCamaraDevices) checkForCameraUpdates()
    if (usingBIServer) checkForServerUpdates()
}

def checkUpdates(name, installedVersion, website) {
    if (loggingOn) log.debug "${name} running checkForUpdates() with an installedVersion of $installedVersion, at website $website"
    def publishedVersion = "0.0"
    try {
        httpGet([uri: website, contentType: "text/plain; charset=UTF-8"]) { resp ->
            if(resp.data) {
                publishedVersion= resp?.data?.text.toString()   //For some reason I couldn't add .trim() to this line, or make another variable and add it there.  The rest of the code would run, but notifications failed. Just having .trim() in the notification below failed a bunch, then started working again...
                if (loggingOn) log.debug "publishedVersion found is $publishedVersion"
                return publishedVersion
            } else  log.error "checkUpdates retrievePublishedVersion response from httpGet was ${resp} for ${name}"
        }
    }
    catch (Exception e) {
        log.error "checkForUpdates() couldn't get the current ${name} code verison. Error:$e"
        publishedVersion = "0.0"
    }
    if (loggingOn) log.debug "${name} publishedVersion from web is ${publishedVersion}, installedVersion is ${installedVersion}"
    def instVerNum = 0			    
    def webVerNum = 0
    if (publishedVersion && installedVersion) {  //make sure no null
        def instVerMap = installedVersion.tokenize('.')  //makes a map of each level of the version
        def webVerMap = publishedVersion.tokenize('.')
        def instVerMapSize = instVerMap.size()
        def webVerMapSize = webVerMap.size()
        if (instVerMapSize > webVerMapSize) {   //handles mismatched sizes (e.g. they have v1.3 installed but v2 is out and didn't write it as v2.0)
            def sizeMismatch = instVerMapSize - webVerMapSize
            for (int i = 0; i < sizeMismatch; i++) {
                def newMapPosition = webVerMapSize + i  //maps' first postion is [0], but size would count it, so the next map position is i in the loop because i starts with 0
                webVerMap[newMapPosition] = 0  //just make it a zero, the actual goal is to increase the size of the map
            }
        } else if (instVerMapSize < webVerMapSize) {
            def sizeMismatch = webVerMapSize - instVerMapSize
            for (int i = 0; i < sizeMismatch; i++) {
                def newMapPosition = instVerMapSize + i  
                instVerMap[newMapPosition] = 0
            }
        }
        instVerMapSize = instVerMap.size() //update the sizes incase we just changed the maps
        webVerMapSize = webVerMap.size()
        if (loggingOn) log.debug "${name} instVerMapSize is $instVerMapSize and the map is $instVerMap and webVerMapSize is $webVerMapSize and the map is $webVerMap"
        for (int i = 0; i < instVerMapSize; i++) {
            instVerNum = instVerNum + instVerMap[i]?.toInteger() *  Math.pow(10, (instVerMapSize - i))  //turns the version segments into one big additive number: 1.2.3 becomes 100+20+3 = 123
        }
        for (int i = 0; i < webVerMapSize; i++) {
            webVerNum = webVerNum + webVerMap[i]?.toInteger() * Math.pow(10, (webVerMapSize - i)) 
        }
        if (loggingOn) log.debug "${name} publishedVersion is now ${webVerNum}, installedVersion is now ${instVerNum}"
        if (webVerNum > instVerNum) {
            def msg = "${name} Update Available. Update in IDE. v${installedVersion} installed, v${publishedVersion.trim()} available."
            if (!receiveUpdateAlerts) sendNotificationEvent(msg)  //Message is only displayed in Smartthings app notifications feed
            if (receiveUpdateAlerts) send(msg) //Message sent to push/SMS per user, plus the Smartthings app notifications feed
        } else if (webVerNum == instVerNum) {
            log.info "${name} is current."
        } else if (webVerNum < instVerNum) {
            log.error "Your installed version of ${name} seems to be higher than the published version."
        }
    } else if (!publishedVersion) {log.error "Cannot get published app version for ${name} from the web."}
}

private send(msg) {
    if (location.contactBookEnabled) {
        if (loggingOn) log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        Map options = [:]
        if (phone) {
            options.phone = phone
            if (loggingOn) log.debug 'sending SMS'
        } else if (pushAndPhone == 'Yes') {
            options.method = 'both'
            options.phone = phone
        } else options.method = 'push'
        sendNotification(msg, options)
    }
    if (loggingOn) log.debug msg
}