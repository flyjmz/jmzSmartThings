/*
Super Notifier - Instant Alert
   
Code: https://github.com/flyjmz/jmzSmartThings
Forum: https://community.smartthings.com/t/release-super-notifier-all-your-alerts-in-one-place/59707


   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
   in compliance with the License. You may obtain a copy of the License at:
 
       http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
   for the specific language governing permissions and limitations under the License.

Version History:
	1.0 - 5Sep2016, Initial Commit
    1.1 - 10Oct2016, all tweaks rolled into public release
 	1.2 - 5Oct2017, added temperature sensor alert capability
    1.3 - 10Oct2017, added lock locked/unlocked capability
    1.4 - 1Feb2018, added timestamp to messages and debug logging option
    1.5 - 21Feb2018, fixed timestamp so hours are in 24-hour time since there isn't an AM/PM
    1.6 - 17Apr2018, added door knock detection.  Added power metering per @ErnieG request
    1.6.1 - 20Apr2018, fixed power metering.
    1.6.2 - 24Jul2018, added contact book like feature to ease SmartThings' depricating the real contact book
    1.6.3 - 6Aug2018, fixed bug that forced you to enter a SMS phone number in the parent app no matter what
	1.6.4 - 13Oct2018, added audio notifications for speech synthesis devices, added "only when switch on/off" to More Options settings,
To Do:
*/

def appVersion() {"1.6.4"}
 
definition(
	name: "Super Notifier - Instant Alert",
	namespace: "flyjmz",
	author: "flyjmz230@gmail.com",
    parent: "flyjmz:Super Notifier",
	description: "Child app for Super Notifier that provides an instant alert whenever something happens",
	category: "My Apps",
	iconUrl: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/phone2x.png",
	iconX2Url: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/phone2x.png"
)

preferences {
    page(name: "settings")
    page(name: "certainTime")
}

def settings() {
    dynamicPage(name: "settings", title: "", install: true, uninstall: true) {
        section("Choose one or more, notify when..."){
            input "button", "capability.button", title: "Button Pushed", required: false, multiple: true
            input "motion", "capability.motionSensor", title: "Motion Here", required: false, multiple: true
            input "contact", "capability.contactSensor", title: "Contact Opens", required: false, multiple: true
            input "contactClosed", "capability.contactSensor", title: "Contact Closes", required: false, multiple: true
            input "acceleration", "capability.accelerationSensor", title: "Acceleration Detected", required: false, multiple: true
            input "mySwitch", "capability.switch", title: "Switch Turned On", required: false, multiple: true
            input "mySwitchOff", "capability.switch", title: "Switch Turned Off", required: false, multiple: true
            input "arrivalPresence", "capability.presenceSensor", title: "Arrival Of", required: false, multiple: true
            input "departurePresence", "capability.presenceSensor", title: "Departure Of", required: false, multiple: true
            input "smoke", "capability.smokeDetector", title: "Smoke Detected", required: false, multiple: true
            input "water", "capability.waterSensor", title: "Water Sensor Wet", required: false, multiple: true
            input "lockLocked", "capability.lock", title: "Lock Locked", required: false, multiple: true
            input "lockUnlocked", "capability.lock", title: "Lock Unlocked", required: false, multiple: true
            input "temp", "capability.temperatureMeasurement", title: "Temp Too Hot or Cold", required: false, multiple: false, submitOnChange: true
            if (temp != null) {
                input "tempTooHot", "number", title: "Too Hot When Temp is Above:", range: "*..*", required: false
                input "tempTooCold", "number", title: "Too Cold When Temp is Below:", range: "*..*", required: false
            }
            input "doorKnocker", "bool", title: "When someone knocks", required: false, multiple: false, submitOnChange: true
            if (doorKnocker) {
                input name: "knockSensor", type: "capability.accelerationSensor", title: "When Someone Knocks Where?"
                input name: "openSensor", type: "capability.contactSensor", title: "But not when they open this door?"
                input name: "knockDelay", type: "number", title: "Knock Delay (defaults to 5s)?", required: false
            }
            input "powerMeters", "capability.powerMeter", title: "Power Too High or Low", required: false, multiple: false, submitOnChange: true
            if (power != powerMeters) {
                input "powerTooHigh", "number", title: "Power Too High When Above:", range: "*..*", required: false
                input "powerTooLow", "number", title: "Power Too Low When Below:", range: "*..*", required: false
            }
        }

        section("Send this custom message (optional, sends standard status message if not specified)") {
            input "messageText", "text", title: "Message Text", required: false
        }

        section("Text/Push Notifications", hidden: false, hideable: true) {
            def SMSContactsSendSMS = []

            if (location.contactBookEnabled ==  true) {
                input("recipients", "contact", title: "Send notifications to")
            } 
            else { 
                input "wantsPush", "bool", title: "Send Push Notification? (pushes to all this location's users)", required: false
                if (parent.settings["SMSContacts"] != null) {
                    paragraph "Select Contacts to send SMS Notifications:"

                    def mapSize = parent.settings["SMSContacts"].split(';').size()
                    for (int i = 0; i < mapSize; i++) {
                        def contactInput = "contact-" + "${i}"
                        def contactName = parent.settings[contactInput]                   
                        input "phone-${i}", "bool", title: "${contactName}", required: false, submitOnChange: true
                        def contactValue = "phone-" + "${i}"
                        SMSContactsSendSMS += settings[contactValue]         
                    }
                    state.SMSContactsMap = SMSContactsSendSMS
                }
            }
        }
 
 		section("Audio Notifications", hidden: false, hideable: true) {
        	paragraph "Can choose to have the message spoken using a speech synthesis device (e.g. LANnouncer)"
			input "speakNotifications", "bool", title: "Use Audio Notifications?", required: false, submitOnChange: true
            if (speakNotifications) {
                input name: "speechDevices", type: "capability.speechSynthesis", title: "Which Speakers (e.g. LANnouncer)?", required: true, multiple: true
            }
        }

        section("Message Details") {
            paragraph "Minimum time between messages (optional, defaults to every message)"
            input "frequency", "decimal", title: "Minutes", required: false
            input "useTimeStamp", "bool", title: "Add timestamp to messages?", required: false
        }

        section() {
                label title: "Assign a name", required: true
        }

        section(title: "Execution Restrictions", hidden: hideOptionsSection(), hideable: true) {
            def timeLabel = timeIntervalLabel()
            href "certainTime", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "controlSwitch", "capability.switch", title: "Only when this switch is...?", required: false, submitOnChange: true
            if (controlSwitch) input "controlSwitchOnOrOff","enum", title: "...On or Off?", multiple: false, required: true, options: ["On", "Off"]
            mode(title: "Only during specific mode(s)")
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
	log.info "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.info "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(button, "button.pushed", eventHandler)
	subscribe(contact, "contact.open", eventHandler)
   	subscribe(contactClosed, "contact.closed", eventHandler)
	subscribe(acceleration, "acceleration.active", eventHandler)
	subscribe(motion, "motion.active", eventHandler)
	subscribe(mySwitch, "switch.on", eventHandler)
    subscribe(mySwitchOff, "switch.off", eventHandler)
	subscribe(arrivalPresence, "presence.present", eventHandler)
	subscribe(departurePresence, "presence.not present", eventHandler)
	subscribe(smoke, "smoke.detected", eventHandler)
	subscribe(smoke, "smoke.tested", eventHandler)
	subscribe(smoke, "carbonMonoxide.detected", eventHandler)
	subscribe(water, "water.wet", eventHandler)
    subscribe(temp, "temperature", tempHandler)
    subscribe(lockLocked,"lock.locked", eventHandler)
    subscribe(lockUnlocked,"lock.unlocked", eventHandler)
    subscribe(knockSensor, "acceleration.active", knockAcceleration)
    subscribe(openSensor, "contact.closed", doorClosed)
    state.lastClosed = 0
    subscribe(powerMeters, "power", powerHandler)
}

def gettooCold() {
	def temp1 = tempTooCold
    if (temp1 == null) temp1 = -460.0
    return temp1
}

def gettooHot() {
	def temp2 = tempTooHot 
    if (temp2 == null) temp2 = 3000.0
    return temp2
}

def tempHandler(evt) {
    def tempState = temp.currentState("temperature")  //trigger is based on the event subcription, but the temp value for notifications is a direct state pull
    if (tempState.doubleValue > tooHot || tempState.doubleValue < tooCold) {
    	eventHandler(evt)
    } else {if (parent.loggingOn) log.debug "Temp within limits, no action taken."}
}

def gettooHigh() {
	def power1 = powerTooHigh
    if (power1 == null) power1 = 2000.0
    return power1
}

def gettooLow() {
	def power2 = powerTooLow 
    if (power2 == null) power2 = -2000.0
    return power2
}

def powerHandler(evt) {
	if (parent.loggingOn) log.debug "Notify got event ${evt} from ${evt.displayName}"
    def powerValue = evt.value.toDouble()
    if (powerValue > tooHigh || powerValue < tooLow) {
    	eventHandler(evt)
    } else {if (parent.loggingOn) log.debug "Power within limits, no action taken."}
}

def knockAcceleration(evt) {
    def delay = knockDelay ?: 5
    runIn(delay, "doorKnock")
}

def doorClosed(evt) {
    state.lastClosed = now()
}

def doorKnock() {
    if ( (openSensor.latestValue("contact") == "closed") && (now() - (60 * 1000) > state.lastClosed) && allOk) {
        log.info "${knockSensor.label ?: knockSensor.name} detected a knock."
        createInstantMessage("knock","knocking","${knockSensor.label ?: knockSensor.name}")
    }
    else {
        if (parent.loggingOn) log.debug("${knockSensor.label ?: knockSensor.name} knocked, but looks like it was just someone opening the door.")
    }
}

def eventHandler(evt) {
	if (parent.loggingOn) log.debug "Notify got event ${evt} from ${evt.displayName}"
	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
        	if (parent.loggingOn) log.debug "frequency used and it is time for new message, checking if within time/day/mode/switch parameters"
            if (allOk) createInstantMessage(evt.name,evt.value,evt.device)
            state[evt.deviceId] = now()
		}
        else {
        	if (parent.loggingOn) log.debug "frequency used but it is too early to send a new message"
        }
	}
	else {
    	if (parent.loggingOn) log.debug "frequency not used, checking if within time/day/mode/switch parameters"
		if (allOk) createInstantMessage(evt.name,evt.value,evt.device)
	}
}

def createInstantMessage(name,value,device) {
	String msg = messageText
    def messageDefault = ""
    if (!messageText) {
		if (name == 'presence') {
			if (value == 'present') {
				messageDefault = "${device} has arrived"
			} else {
				messageDefault = "${device} has left"
			}
		} else {
			messageDefault = "${device} is ${value}"
		}
        msg = messageDefault
	}
    sendMessage(msg)
}

private getAllOk() {
	daysOk && timeOk && switchOk
}

private getSwitchOk() {
    def result = true
    if (controlSwitch) {
		if (controlSwitchOnOrOff == "On" && controlSwitch.currentState("switch")?.value != "on") {
        result = false
        } else if (controlSwitchOnOrOff == "Off" && controlSwitch.currentState("switch")?.value != "off") {
        result = false
        } else log.error "You're using a switch to control when this app will run, except the setting for when that switch is 'On' or 'Off' isn't set. Ignoring and allowing app to run regardless. Check your settings."
    }
    if (parent.loggingOn) log.debug "switchOk = $result"
    return result
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
	if (parent.loggingOn) log.debug "daysOk = $result"
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
	if (parent.loggingOn) log.debug "TimeOk = $result"
	return result
}

private hhmm(time, fmt = "h:mm a") {
	def t = timeToday(time, location.timeZone)
	def f = new java.text.SimpleDateFormat(fmt)
	f.setTimeZone(location.timeZone ?: timeZone(time))
	f.format(t)
}

private hideOptionsSection() {
	(starting || ending || days || modes || startingX || endingX || controlSwitch) ? false : true
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

private sendMessage(msg) {  
	//Speak Message
    if (speakNotifications) {
        speechDevices.each() {
    		it.speak(msg)
            log.info "Spoke '" + msg + "' with " + it.device.displayName
    	}
    }
    
    //Add time stamps for text/push messages (not for audio)
    if (useTimeStamp) {
        def stamp = new Date().format('yyyy-M-d HH:mm:ss',location.timeZone)
        msg = msg + " (" + stamp + ")"
    }
    
    //First try to use Contact Book (Depricated 30July2018)
    if (location.contactBookEnabled) {
        log.info "sent '$msg' notification to: ${recipients?.size()}"
        sendNotificationToContacts(msg, recipients)
    }

    //Otherwise use old school Push/SMS notifcations
    else {
        if (loggingOn) log.debug("sending message to app notifications tab: '$msg'")
        sendNotificationEvent(msg)  //First send to app notifications (because of the loop we're about to do, we need to use this version to avoid multiple instances) 
        if (wantsPush) {
            sendPushMessage(msg)  //Second, send the push notification if user wanted it
            log.info "sent '$msg' via push"
        }

        if (state.SMSContactsMap != null) {  //Third, send SMS messages if desired
            def SMSContactsSplit = parent.settings["SMSContacts"].split(';')
            for (int i = 0; i < state.SMSContactsMap.size(); i++) {
                if (state.SMSContactsMap[i]) {
                    sendSmsMessage(SMSContactsSplit[i], msg)
                    log.info "sent '$msg' via SMS to ${SMSContactsSplit[i]}"
                }
            }
        }
    }
}