/*
Blue Iris Fusion - Trigger 
(Child app for camera triggering.  Parent app is: "Blue Iris Fusion") 

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
Smartthings Community Thread: https://community.smartthings.com/t/release-bi-fusion-v3-0-adds-blue-iris-device-type-handler-blue-iris-camera-dth-motion-sensing/103032

Github: https://github.com/flyjmz/jmzSmartThings/tree/master/smartapps/flyjmz/blue-iris-fusion-trigger.src

PARENT APP CAN BE FOUND ON GITHUB: https://github.com/flyjmz/jmzSmartThings/tree/master/smartapps/flyjmz/blue-iris-fusion.src

Version History:
Version 1.0 - 30July2016    Initial release
Version 1.1 - 3August2016   Cleaned up code
Version 1.2 - 4August2016   Added Alarm trigger capability from rayzurbock
Version 2.0 - 14Dec2016     Added ability to restrict triggering to defined time periods
Version 2.1 - 17Jan2017     Added preference to turn debug logging on or off
Version 2.2 - 22Jan2017     Added trigger notifications
Version 2.3 - 23Jan2017     Slight tweak to notifications, now receving notifications in the app is user defined instead of always on.
Version 2.4 - 30May2017     Added button push to trigger options
Version 2.5 - 5Oct2017		Added Contact Closing and Switch turning off to trigger options
Version 3.0 - 26Oct2017		Added Blue Iris Server and Camera Device Type Integration, and App Update Notifications
Version 3.0.1 - 28Oct2017 	Enabled full Camera DTH support regardless of command method to Blue Iris (BI Server DTH/Local/External)
Version 3.0.2 - ??			Added triggers for: acceleration, presence, shock, smoke, carbonMonoxide, and water sensors.
							Added ability to send preset commands to cameras when triggering.
                            Remind Users to click on each Trigger app instance to confirm settings & tap 'done' to ensure initialize() runs.
BETA 3.0.2.b

To Do:
-see todos
*/

def appVersion() {"3.0.2"}

definition(
    name: "Blue Iris Fusion - Trigger",
    namespace: "flyjmz",
    author: "flyjmz230@gmail.com",
    parent: "flyjmz:Blue Iris Fusion",
    description: "Child app to 'Blue Iris Fusion.' Install that app, it will call this during setup.",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo.png",
    iconX2Url: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo%402x.png")

preferences {
    page(name: "mainPage", title: "BI Fusion Custom Camera Triggers", install: true, uninstall: true)
    page(name: "certainTime")
}

def mainPage() {
    return dynamicPage(name: "mainPage", title: "BI Fusion Custom Camera Triggers", submitOnChange: true) {
        section(""){
            paragraph "Select the Camera(s) to control by either selecting from your BI Camera Devices list or type in a camera's short name"
            input "usingCameraDTH", "bool", title: "Select from list?", submitOnChange: true
            paragraph "NOTE: To select cameras, first complete the initial BI Fusion setup. (The camera devices aren't created until you click 'Done' the first time)."
        }
        if (usingCameraDTH) {
            section("Select Blue Iris Camera(s) to Trigger") {   
                input "biCamerasSelected", "capability.imageCapture", title: "Blue Iris Cameras", required: false, multiple: true  
                paragraph "NOTE: Be sure to only select Blue Iris cameras."
            }
        } else {
            section("Blue Iris Camera Name") {  
                paragraph "Enter the Blue Iris short name for the Camera (case-sensitive)."
                input "biCamera", "text", title: "Camera Name", required: false
            }
        }
        section("Select Trigger Events"){   
            input "myMotion", "capability.motionSensor", title: "Motion Sensors Active", required: false, multiple: true
            input "myContactOpen", "capability.contactSensor", title: "Contact Sensors Opening", required: false, multiple: true
            input "myContactClosed", "capability.contactSensor", title: "Contact Sensors Closing", required: false, multiple: true
            input "mySwitchOn", "capability.switch", title: "Switches Turning On", required: false, multiple: true
            input "mySwitchOff", "capability.switch", title: "Switches Turning Off", required: false, multiple: true
            input "myAlarm", "capability.alarm", title: "Alarm Activated", required: false, multiple: true
            input "myButton", "capability.button", title: "Button Pushed", required: false, multiple: true
            input "myAccel", "capability.accelerationSensor", title: "Acceleration Active", required: false, multiple: true
            input "myPresence", "capability.presenceSensor", title: "Presence Arrived", required: false, multiple: true
            input "myShock", "capability.shockSensor", title: "Shock Detected", required: false, multiple: true
            input "mySmoke", "capability.smokeDetector", title: "Smoke/CO Detected", required: false, multiple: true
            input "myWater", "capability.waterSensor", title: "Water Detected", required: false, multiple: true
        }
        section("Camera Trigger and/or PTZ") {
            paragraph "This is trigger camera(s) to record by default, you can also choose to move the camera(s) to a preset:"
            input "usePreset", "bool", title: "Move to Preset?", required: true, submitOnChange: true
            if (usePreset) {
                input "disableRecording", "bool", title: "Disable Recording (and only move to preset)?", required: false
                if (usingCameraDTH) {
                    biCamerasSelected.each { camera ->
                        def cameraName = camera.displayName  
                        input "preset-${cameraName}", "number", title: "Preset # for Camera: ${cameraName}", required: true
                    }
                } else {
                    input "biPreset", "number", title: "Preset # for Camera: ${biCamera}", required: true
                }
            }
        }
        section(title: "More options", hidden: hideOptionsSection(), hideable: true) {
            def timeLabel = timeIntervalLabel()
            href "certainTime", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
        }
        section("Notifications") {
            def receiveAlerts = false
            input "receiveAlerts", "bool", title: "Receive Push/SMS Alerts When Triggered?"
            if (!parent.localOnly && !parent.usingBIServer) {
                paragraph "You can also receive error SMS/PUSH notifications for this trigger since you are using 'WAN/External' connections.  Message delivery matches your settings in the main BI Fusion app."
                def receiveNotifications = false
                input "receiveNotifications", "bool", title: "Receive Error Push/SMS Notifications?"  //todo -- this would also work for usingBIServer, just need to code it out.
            }
        }
        section("") {
            input "customTitle", "text", title: "Assign a Name", required: true
        }
    }
}

def certainTime() {
    dynamicPage(name:"certainTime",title: "Only during a certain time", uninstall: false) {
        section() {
            input "startingX", "enum", title: "Starting at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: "A specific time", submitOnChange: true
            if(startingX in [null, "A specific time"]) input "starting", "time", title: "Start time", required: false
            else {
                if(startingX == "Sunrise") input "startSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
                else if(startingX == "Sunset") input "startSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
                    }
        }

        section() {
            input "endingX", "enum", title: "Ending at", options: ["A specific time", "Sunrise", "Sunset"], defaultValue: "A specific time", submitOnChange: true
            if(endingX in [null, "A specific time"]) input "ending", "time", title: "End time", required: false
            else {
                if(endingX == "Sunrise") input "endSunriseOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
                else if(endingX == "Sunset") input "endSunsetOffset", "number", range: "*..*", title: "Offset in minutes (+/-)", required: false
                    }
        }
    }
}

def installed() {
    if (parent.loggingOn) log.debug "Installed with settings: ${settings}"
    initialize()
    app.updateLabel("${customTitle}")
}

def updated() {
    if (parent.loggingOn) log.debug "Updated with settings: ${settings}"
    unsubscribe()
    initialize()
    app.updateLabel("${customTitle}")
}

def initialize() {
    subscribe(myMotion, "motion.active", eventHandlerBinary)
    subscribe(myContactOpen, "contact.open", eventHandlerBinary)
    subscribe(myContactClosed, "contact.closed", eventHandlerBinary)
    subscribe(mySwitchOn, "switch.on", eventHandlerBinary)
    subscribe(mySwitchOff, "switch.off", eventHandlerBinary)
    subscribe(myAlarm, "alarm.strobe", eventHandlerBinary)
    subscribe(myAlarm, "alarm.siren", eventHandlerBinary)
    subscribe(myAlarm, "alarm.both", eventHandlerBinary)
    subscribe(myButton, "button.pushed", eventHandlerBinary)
    subscribe(myAccel, "acceleration.active", eventHandlerBinary)
    subscribe(myPresence, "presence.active", eventHandlerBinary)
    subscribe(myShock, "shock.detected", eventHandlerBinary)
    subscribe(mySmoke, "smoke.detected", eventHandlerBinary)
    subscribe(mySmoke, "carbonMonoxide.detected", eventHandlerBinary)
    subscribe(myWater, "water.detected", eventHandlerBinary)
    def names = []  //todo - make externalAction use these lists to reduce code length
    def presets = []   
    if (usingCameraDTH) { 
        biCamerasSelected.each { camera ->
            names += camera.name
            if (usePreset) {
                def presetInput = "preset-${camera.displayName}"
                presets += settings[presetInput].toInteger()
            }
        }
    } else {
        names += biCamera
        presets +=biPreset.toInteger()
    }
    state.shortNameList = names
    state.presetList = presets
    state.listSize =  state.shortNameList.size()
    log.info "intialized, listSize is $state.listSize, cameras are $state.shortNameList, and presets are $state.presetList"
}

def eventHandlerBinary(evt) {
    if (parent.loggingOn) log.debug "processed event ${evt.name} from device ${evt.displayName} with value ${evt.value} and data ${evt.data}"
    if (allOk) {
        log.info "Event occured within the desired timing conditions, sending commands"
        def actionName = ""
        if (usePreset && !disableRecording) {
            actionName = " Moving and Triggering "
        } else if (usePreset && disableRecording) {
            actionName = " Moving "
        } else if (!usePreset && disableRecording) {
            actionName = " Doing nothing to "
        } else if (!usePreset && !disableRecording) {
			actionname = " Triggering "
        }
        if (usingCameraDTH) {
            if (!receiveAlerts) sendNotificationEvent("${evt.displayName} is ${evt.value}, BI Fusion is" + actionName + "Cameras: ${biCamerasSelected}")
            if (receiveAlerts) parent.send("${evt.displayName} is ${evt.value}, BI Fusion is" + actionName + "Cameras: ${biCamerasSelected}")
        } else {
            if (!receiveAlerts) sendNotificationEvent("${evt.displayName} is ${evt.value}, BI Fusion is" + actionName + "Camera: ${biCamera}")
            if (receiveAlerts) parent.send("${evt.displayName} is ${evt.value}, BI Fusion is" + actionName + "Camera: ${biCamera}")
        }
        if (parent.localOnly || parent.usingBIServer) {  //The trigger runs it's own local/external code, so we need to know which BI Fusion is use (and localOnly is the same as using the server in this case)
            localAction()
        } else externalAction()
    } else if (parent.loggingOn) log.debug "event did not occur within the desired timing conditions, not triggering"
        }

def localAction() {
    def triggerCommand = ""
    def presetCommand = ""
    for (int i = 0; i < state.listSize; i++) {
        if (usePreset) {
        	log.info "Moving ${state.shortNameList[i]} to preset ${state.presetList[i]}"
            def presetString = 7 + state.presetList[i]  //todo - if preset is 1, do I need to make this "7+1" or "8".  Also, it says unauthorized for ptz if I don't include the user&pass, but when I do the returned body is  null and I don't get a reaction.
            presetCommand = "/cam/${state.shortNameList[i]}/pos=${presetString}&user=${parent.username}&pw=${parent.password}" //todo - find out what the parse message that comes back looks like
            talkToHub(presetCommand)
        }
        if (!disableRecording) {
            log.info "Triggering: ${state.shortNameList[i]}"
            triggerCommand = "/admin?camera=${state.shortNameList[i]}&trigger&user=${parent.username}&pw=${parent.password}"
            talkToHub(triggerCommand)
        }
    }
}

def talkToHub(commandPath) {  //todo can I use a 'callback' function to parse results?  Otherwise the trigger app really isn't confirming it worked...
    def biHost = "${parent.host}:${parent.port}"
    if (parent.loggingOn) log.debug "sending GET to URL $biHost$commandPath"
    def httpMethod = "GET"
    def httpRequest = [
        method:     httpMethod,
        path:       commandPath,
        headers:    [
            HOST:       biHost,
            Accept:     "*/*",
        ]
    ]
    def hubAction = new physicalgraph.device.HubAction(httpRequest)
    sendHubCommand(hubAction)
    log.trace hubAction
}
//todo - do the log.info, log.error and errormsg changes in bi fusion too
def externalAction() {
    log.info "Running externalAction"
    try {
        httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"login"]) { response ->
            if (parent.loggingOn) log.debug response.data
            if (parent.loggingOn) log.debug "logging in"
            if (response.data.result == "fail") {
                if (parent.loggingOn) log.debug "BI_Inside initial call fail, proceeding to login"
                def session = response.data.session
                def hash = parent.username + ":" + response.data.session + ":" + parent.password
                hash = hash.encodeAsMD5()
                httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"login","session":session,"response":hash]) { response2 ->
                    if (response2.data.result == "success") {
                        if (parent.loggingOn) log.debug ("BI_Logged In")
                        if (parent.loggingOn) log.debug response2.data
                        httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"status","session":session]) { response3 ->
                            if (response3.data.result == "success"){
                                if (parent.loggingOn) log.debug ("BI_Retrieved Status")
                                if (parent.loggingOn) log.debug response3.data

                                ////////////////////Trigger to Record////////////////////////////////////////////////////
                                if (!disableRecording) {
                                    if (usingCameraDTH) {  //If using multiple cameras:
                                        log.info "Triggering: ${biCamerasSelected}"
                                        for (cameraDevice in biCamerasSelected) {
                                            def shortName = cameraDevice.name
                                            httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"trigger","camera":shortName,"session":session]) { response4 ->
                                                if (parent.loggingOn) log.debug response4.data
                                                if (response4.data.result == "success") {
                                                    log.info "Blue Iris Fusion triggered camera '${shortName}'"
                                                } else {
                                                    log.error "BI Fusion Failure: ${shortName} not triggered"
                                                    if (parent.loggingOn) log.debug(response4.data.data.reason)
                                                    if (!receiveNotifications) sendNotificationEvent("BI Fusion Failure: ${shortName} not triggered")
                                                    if (receiveNotifications) parent.send("BI Fusion Failure: ${shortName} not triggered")
                                                }
                                            }
                                        }
                                    } else {        //If using a single camera:
                                        log.info "Triggering ${biCamera}"
                                        httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"trigger","camera":biCamera,"session":session]) { response4 ->
                                            if (parent.loggingOn) log.debug response4.data
                                            if (response4.data.result == "success") {
                                                log.info "Blue Iris Fusion triggered camera '${biCamera}'"
                                            } else {
                                                log.error "BI Fusion Failure, not triggered"
                                                if (parent.loggingOn) log.debug(response4.data.data.reason)
                                                if (!receiveNotifications) sendNotificationEvent("BI Fusion Failure: ${biCamera} not triggered")
                                                if (receiveNotifications) parent.send("BI Fusion Failure: ${biCamera} not triggered")
                                            }
                                        }
                                    }
                                }

                                ////////////////////Move to Preset Position//////////////////////////////////////////////
                                if (usePreset) {
                                    if (usingCameraDTH) {
                                        for (cameraDevice in biCamerasSelected) {
                                            def shortName = cameraDevice.name
                                            def presetInput = "preset-${cameraDevice.displayName}"
                                            def presetNumber = settings[presetInput].toInteger() + 100  //Blue Iris JSON command for preset is 101...120 for preset 1...20
                                            httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"ptz","camera":shortName,"button":presetNumber, "session":session]) { response4 -> 
                                                if (parent.loggingOn) log.debug response4.data
                                                if (response4.data.result == "success") {
                                                    log.info "BI Fusion moved $shortName to preset '$presetString'"
                                                } else {
                                                    log.error "BI Fusion Failure: preset '$presetString' not sent to $shortName"
                                                    if (parent.loggingOn) log.debug(response4.data.data.reason)
                                                    if (!receiveNotifications) sendNotificationEvent("BI Fusion Failure: preset '$presetString' not sent to $shortName")
                                                    if (receiveNotifications) parent.send("BI Fusion Failure: preset '$presetString' not sent to $shortName")
                                                }
                                            }
                                        }
                                    } else {
                                        def presetNumber = biPreset.toInteger() + 100
                                        httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"ptz","camera":biCamera,"button":presetString, "session":session]) { response4 -> 
                                            if (parent.loggingOn) log.debug response4.data
                                            if (response4.data.result == "success") {
                                                log.info "Moved ${biCamera} to preset ${biPreset}"
                                            } else {
                                                log.error "BI Fusion Failure: preset '$presetString' not sent to $biCamera"
                                                if (parent.loggingOn) log.debug(response4.data.data.reason)
                                                if (!receiveNotifications) sendNotificationEvent("BI Fusion Failure: preset '$presetString' not sent to $biCamera")
                                                if (receiveNotifications) parent.send("BI Fusion Failure: preset '$presetString' not sent to $biCamera")
                                            }
                                        } 
                                    }
                                }

                                ////////////////////Logout///////////////////////////////////////////////////////////////
                                httpPostJson(uri: parent.host + ':' + parent.port, path: '/json',  body: ["cmd":"logout","session":session]) { response5 ->
                                    if (parent.loggingOn) log.debug response5.data
                                    if (parent.loggingOn) log.debug "Logged out"
                                }
                                
							//////////////////////////Continue with code//////////////////////////////////////////
                            } else {
                                log.error "BI Fusion Failure: didn't receive status"
                                if (parent.loggingOn) log.debug(response3.data.data.reason)
                                if (!receiveNotifications) sendNotificationEvent("BI Fusion Failure: Couldn't Login to Blue Iris")
                                if (receiveNotifications) parent.send("BI Fusion Failure: Couldn't Login to Blue Iris")
                            }
                        }
                    } else {
                        log.error "BI Fusion Failure: Couldn't Login to Blue Iris"
                        if (parent.loggingOn) log.debug(response2.data.data.reason)
                        if (!receiveNotifications) sendNotificationEvent("BI Fusion Failure: Couldn't Login to Blue Iris")
                        if (receiveNotifications) parent.send("BI Fusion Failure: Couldn't Login to Blue Iris")
                    }
                }
            } else {
                log.error "BI Fusion Failure: Couldn't Login to Blue Iris"
                if (parent.loggingOn) log.debug(response.data.data.reason)
                if (!receiveNotifications) sendNotificationEvent("BI Fusion Failure: Couldn't Login to Blue Iris")
                if (receiveNotifications) parent.send("BI Fusion Failure: Couldn't Login to Blue Iris")
            }
        }
    } catch(Exception e) {
        log.error "BI Fusion Failure: $e"
        if (!receiveNotifications) sendNotificationEvent("BI Fusion Failure, turn on debugging and check logs")
        if (receiveNotifications) parent.send("BI Fusion Failure, turn on debugging and check logs")
    }
}

private getAllOk() {
    modeOk && daysOk && timeOk
}

private getDaysOk() {
    def result = true
    if (days) {
        def df = new java.text.SimpleDateFormat("EEEE")
        if (location.timeZone) {
            df.setTimeZone(location.timeZone)
        }
        else {
            df.setTimeZone(TimeZone.getTimeZone("America/New_York"))
        }
        def day = df.format(new Date())
        result = days.contains(day)
    }
    return result
}

private getTimeOk() {
    def result = true
    if ((starting && ending) ||
        (starting && endingX in ["Sunrise", "Sunset"]) ||
        (startingX in ["Sunrise", "Sunset"] && ending) ||
        (startingX in ["Sunrise", "Sunset"] && endingX in ["Sunrise", "Sunset"])) {
        def currTime = now()
        def start = null
        def stop = null
        def s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: startSunriseOffset, sunsetOffset: startSunsetOffset)
        if(startingX == "Sunrise") start = s.sunrise.time
        else if(startingX == "Sunset") start = s.sunset.time
            else if(starting) start = timeToday(starting,location.timeZone).time
                s = getSunriseAndSunset(zipCode: zipCode, sunriseOffset: endSunriseOffset, sunsetOffset: endSunsetOffset)
            if(endingX == "Sunrise") stop = s.sunrise.time
            else if(endingX == "Sunset") stop = s.sunset.time
                else if(ending) stop = timeToday(ending,location.timeZone).time
                    result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
            }
    return result
}

private getModeOk() {
    def result = !modes || modes.contains(location.mode)
    return result
}

private hhmm(time, fmt = "h:mm a") {
    def t = timeToday(time, location.timeZone)
    def f = new java.text.SimpleDateFormat(fmt)
    f.setTimeZone(location.timeZone ?: timeZone(time))
    f.format(t)
}

private hideOptionsSection() {
    (starting || ending || days || modes || startingX || endingX) ? false : true
}

private offset(value) {
    def result = value ? ((value > 0 ? "+" : "") + value + " min") : ""
}

private timeIntervalLabel() {
    def result = ""
    if (startingX == "Sunrise" && endingX == "Sunrise") result = "Sunrise" + offset(startSunriseOffset) + " to " + "Sunrise" + offset(endSunriseOffset)
    else if (startingX == "Sunrise" && endingX == "Sunset") result = "Sunrise" + offset(startSunriseOffset) + " to " + "Sunset" + offset(endSunsetOffset)
        else if (startingX == "Sunset" && endingX == "Sunrise") result = "Sunset" + offset(startSunsetOffset) + " to " + "Sunrise" + offset(endSunriseOffset)
            else if (startingX == "Sunset" && endingX == "Sunset") result = "Sunset" + offset(startSunsetOffset) + " to " + "Sunset" + offset(endSunsetOffset)
                else if (startingX == "Sunrise" && ending) result = "Sunrise" + offset(startSunriseOffset) + " to " + hhmm(ending, "h:mm a z")
                    else if (startingX == "Sunset" && ending) result = "Sunset" + offset(startSunsetOffset) + " to " + hhmm(ending, "h:mm a z")
                        else if (starting && endingX == "Sunrise") result = hhmm(starting) + " to " + "Sunrise" + offset(endSunriseOffset)
                            else if (starting && endingX == "Sunset") result = hhmm(starting) + " to " + "Sunset" + offset(endSunsetOffset)
                                else if (starting && ending) result = hhmm(starting) + " to " + hhmm(ending, "h:mm a z")
                                    }
