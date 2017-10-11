/*
Super Notifier - Delayed Alert
   
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
	1.1 - 10Oct2016, added mode changes & sunrise/sunset to periodic notifications, public release
    1.2 - 23Oct2016, found & corrected error when using periodic notifications for mode/sun changes but not timed ones.  Added hours to notifications.
    1.3 - 29Aug2017, added ability to snooze periodic notifications.  Just add a virtual switch device.  (I have a virtual switch device type in my Github repository, linked above).
	1.4 - 5Oct2017, added temperature sensor monitoring.
    1.5 - 10Oct2017, added lock monitoring.
*/
 
definition(
    name: "Super Notifier - Delayed Alert",
    namespace: "flyjmz",
    author: "flyjmz230@gmail.com",
    parent: "flyjmz:Super Notifier",
    description: "Child app for Super Notifier to notifiy when a contact is left open or closed, or a switch is left on or off.",
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
        section("") {
            input "monitorType", "enum", title: "Monitor what?", required: true, options: ["Contact Sensor", "Switch", "Temperature", "Lock"], submitOnChange: true
            if (monitorType == "Contact Sensor") {
            	input "myContact", "capability.contactSensor", title: "Which contact?", required: true
            	input "openClosed", "enum", title: "When left open or closed?", required: true, options: ["Open", "Closed"]
            }
            if (monitorType == "Switch") {
            	input "mySwitch", "capability.switch", title: "Which switch?", required: true
            	input "onOff", "enum", title: "When left on or off?", required: true, options: ["On", "Off"]
            }
            if (monitorType == "Temperature") {
            	input "temp", "capability.temperatureMeasurement", title: "Which Temp Sensor?", required: true
                input "tempTooHot", "number", title: "Too Hot When Temp is Above:", range: "*..*", required: false
                input "tempTooCold", "number", title: "Too Cold When Temp is Below:", range: "*..*", required: false
            }
            if (monitorType == "Lock") {
            	input "myLock", "capability.lock", title: "Which Lock?", required: true
                input "lockedUnlocked", "enum", title: "When left locked or left unlocked?", required: true, options: ["Locked", "Unlocked"]
            }
        }
        section("For more than this many minutes") {
            input "waitThreshold", "number", description: "Number of minutes", required: true
        }

        section("Send this custom message (optional, sends standard status message if not specified)"){
                input "messageText", "text", title: "Message Text", required: false
        }

        section("Periodic Notificaitons") {
            paragraph "You'll receive an alert when it is left that way after your defined time period (above) and also onces it returns to normal.  Optionally, you can set periodic notifications for times in between as well." 
            input "periodicNotifications", "bool", title: "Receive periodic notifications?", required: false, submitOnChange: true
            if (periodicNotifications) {
            	input "waitMinutes", "number", title: "Timed periodic notifications? (minutes in-between)", required: false
                input "modeChange", "bool", title: "Notify on mode change?", required: false
                input "sunChange", "bool", title: "Notify at sunrise/sunset?", required: false
                paragraph "Periodic Notifications can be snoozed easily with a virtual switch device type.  This is useful when you are unable to resolve an issue and the notifications become irritable.  A 'snooze' switch in your Things is easier to hit than changeing these settings." 
                input "snoozeSwitch", "capability.switch", title: "Which switch controls snoozing periodic notifications?", required: false
        	}      
        }

        section("Notification Type"){
            input("recipients", "contact", title: "Send notifications to") {
                input "pushAndPhone", "enum", title: "Also send SMS? (optional, it will always send push)", required: false, options: ["Yes", "No"]		
                input "phone", "phone", title: "Phone Number (only for SMS)", required: false
                paragraph "If outside the US please make sure to enter the proper country code"
            }
    	}
        
        section() {
            label title: "Assign a name", required: true
        }

        section(title: "More options", hidden: hideOptionsSection(), hideable: true) {
            def timeLabel = timeIntervalLabel()
            href "certainTime", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
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
	log.trace "installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.trace "updated with settings: ${settings}"
	unsubscribe()
    unschedule()
    initialize()
}

def initialize () {
    if (openClosed && openClosed == "Open") {
    	subscribe(myContact, "contact.open", eventHandler)
        subscribe(myContact, "contact.closed", okHandler)
    } else if (openClosed && openClosed == "Closed") {
    	subscribe(myContact, "contact.closed", eventHandler)
        subscribe(myContact, "contact.open", okHandler)
    } else if (onOff && onOff == "On") {
		subscribe(mySwitch, "switch.on", eventHandler)
        subscribe(mySwitch, "switch.off", okHandler)
    } else if (onOff && onOff == "Off") {
    	subscribe(mySwitch, "switch.off", eventHandler)
		subscribe(mySwitch, "switch.on", okHandler)
    } else if (lockedUnlocked && lockedUnlocked == "Locked") {
    	subscribe(myLock, "lock.locked", eventHandler)
		subscribe(myLock, "lock.unlocked", okHandler)
    } else if (lockedUnlocked && lockedUnlocked == "Unlocked") {
        subscribe(myLock, "lock.unlocked", eventHandler)
        subscribe(myLock, "lock.locked", okHandler)
    } else if (temp) {
    	subscribe(temp, "temperature", tempHandler)
    } else log.debug "Not subscribing to any device events"
    if (modeChange) subscribe(location, "mode", periodicNotifier) //checks status every time mode changes (in case it missed it)
    if (sunChange) {				//checks status every sun rises/sets (in case it missed it)
        subscribe(location, "sunrise", periodicNotifier)
    	subscribe(location, "sunset", periodicNotifier)
    }
    atomicState.msgSent = false
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
	log.debug "tempHandler has ${evt.displayName} and ${temp?.currentState("temperature")}"
    if (!atomicState.msgSent) {  //need this check because temperatures report at intervals on a scale of values (unlike switches and contact sensors), if we didn't check this, every time it recieved a new temp that was still out of limits, it'd reset the event time and start the periodic notifcations over.
        def tempState1 = temp.currentState("temperature")  //trigger is based on the event subcription, but the temp value for notifications is a direct state pull
        if (tempState1.doubleValue > tooHot || tempState1.doubleValue < tooCold) {
            log.debug "Temp out of limits, sending to eventHandler"
            eventHandler(evt)
        } else log.debug "Temp within limits, no action taken."
    }
}

def eventHandler(evt) {
	log.trace "eventHandler has ${evt.displayName}: ${evt.name}: ${evt.value}, scheduling stillWrong() in ${waitThreshold} minutes"
    runIn((waitThreshold * 60), stillWrong)
    atomicState.problemTime = now()
}

def stillWrong() { 
	def myContactState = myContact?.currentState("contact")
    def mySwitchState = mySwitch?.currentState("switch")
    def tempState2 = temp?.currentState("temperature")
    def myLockState = myLock?.currentState("lock")
    if (openClosed) {
    	if (openClosed == "Open") {
			if (myContactState.value == "open") {
            	log.debug "Contact is still open"
                stillWrongMsger()
            } else log.debug "the contact is closed and you want it that way"  //okHandler will send the it's ok messages for all these cases
        } else {
        	if (myContactState.value == "closed") {
            	log.debug "Contact is still closed"
                stillWrongMsger()
            } else log.debug "the contact is open and you want it that way"
    	}
    }
    if (onOff) {
    	if (onOff == "On") {
			if (mySwitchState.value == "on") {
            	log.debug "Switch is still on"
				stillWrongMsger()
            } else log.debug "the switch is off and you want it that way"
        } else {
        	if (mySwitchState.value == "off") {
	            log.debug "Switch is still off"
				stillWrongMsger()
            } else log.debug "the switch is on and you want it that way"
    	}
    }
        if (lockedUnlocked) {
    	if (lockedUnlocked == "locked") {
			if (myLockState.value == "locked") {
            	log.debug "Lock is still locked"
				stillWrongMsger()
            } else log.debug "the lock is unlocked and you want it that way"
        } else {
        	if (myLockState.value == "unlocked") {
	            log.debug "Lock is still unlocked"
				stillWrongMsger()
            } else log.debug "the lock is lcoked and you want it that way"
    	}
    }
    if (temp) {
        if (tempState2.doubleValue > tooHot || tempState2.doubleValue < tooCold) {
            log.debug "Temperature is still out of limits"
            stillWrongMsger()
        } else log.debug "Temp within limits, no action taken."
    }
}

def stillWrongMsger() {
	def myContactState2 = myContact?.currentState("contact")
    def mySwitchState2 = mySwitch?.currentState("switch")
    def myLockState2 = myLock?.currentState("lock")
    def tempState3 = temp?.currentState("temperature")
    if (allOk) {
        log.debug "Event within time/day/mode constraints"
    	if (!atomicState.msgSent) {
            if (myContact) sendMessage("${myContact?.displayName} is still ${myContactState2?.value}!")
            if (mySwitch) sendMessage("${mySwitch?.displayName} is still ${mySwitchState2?.value}!")
            if (myLock) sendMessage("${myLock?.displayName} is still ${myLockState2?.value}!")
            if (temp) sendMessage("${temp?.displayName} is still ${tempState3?.value}!")
            atomicState.msgSent = true
            log.debug "sending first message, set atomicState.msgSent to ${atomicState.msgSent}"
       		if (periodicNotifications) {
            	if (waitMinutes) {
                	runIn((waitMinutes * 60), stillWrong)
                	log.debug "periodic notifications is on, scheduling stillWrong() to run again in ${waitMinutes} minutes"
            	}
            }
        } else if (periodicNotifications && atomicState.msgSent) {
        	def snooze = false
        	if (snoozeSwitch?.currentState("switch")?.value == "on") snooze = true
            if (!snooze) log.debug "sending periodic notification"
            int timeSince = ((now() - atomicState.problemTime) / 60000) //time since issue occured in whole minutes
            if (!snooze && timeSince > 180) {  //determines whether to report in hours or minutes (longer than 180 minutes is reported in hours), and ensures alerts aren't snoozed.
            	int timeMsg = timeSince / 60
                if (myContact) sendMessage("Periodic Alert: ${myContact?.displayName} has been ${myContactState2?.value} for ${timeMsg} hours!")
            	if (mySwitch) sendMessage("Periodic Alert: ${mySwitch?.displayName} has been ${mySwitchState2?.value} for ${timeMsg} hours!")
                if (myLock) sendMessage("Periodic Alert: ${myLock?.displayName} has been ${myLockState2?.value} for ${timeMsg} hours!")
                if (temp) sendMessage("Periodic Alert: ${temp?.displayName} has been out of limits for ${timeMsg} hours!")
            } 
            if (!snooze && timeSince < 180) {
        		if (myContact) sendMessage("Periodic Alert: ${myContact?.displayName} has been ${myContactState2?.value} for ${timeSince} minutes!")
            	if (mySwitch) sendMessage("Periodic Alert: ${mySwitch?.displayName} has been ${mySwitchState2?.value} for ${timeSince} minutes!")
                if (myLock) sendMessage("Periodic Alert: ${myLock?.displayName} has been ${myLockState2?.value} for ${timeSince} minutes!")
                if (temp) sendMessage("Periodic Alert: ${temp?.displayName} has been out of limits for ${timeSince} minutes!")
            }
            if (waitMinutes) {
            	runIn((waitMinutes * 60), stillWrong)
            	log.debug "periodic notifications is scheduling the next stillWrong() to run in ${waitMinutes} minutes"
        	}
        } else if (!periodicNotifications && atomicState.msgSent) {
        	log.debug "message already sent once, not using periodic notifcations, so not sending another message"
        }
    } else {
    	log.debug "event is outside of time/day/mode conditions, no message sent, but monitoring in case it doesn't return to normal before it is within those time/day/mode conditions"
    	runIn((waitThreshold * 60), stillWrong)
    }
}

def okHandler(evt) {
	if (atomicState.msgSent) {
    	sendMessage("${evt.device.displayName} is now ${evt.value}.")
        atomicState.msgSent = false
        log.debug "okHandler() evoked, message sent, atomicState.msgSent is now: ${atomicState.msgSent}"
    } else log.debug "it's okay now, and never sent left open/closed/on/off message, so no need to send an 'ok' message"
}

def periodicNotifier(evt) {
	log.debug "periodic notifier got ${evt.descriptionText}, sending to stillWrong()"
    stillWrong()
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
	log.trace "daysOk = $result"
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
	log.trace "timeOk = $result"
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

private sendMessage(msg) {
	if (location.contactBookEnabled) {
		sendNotificationToContacts(msg, recipients)
	} else {
		Map options = [:]
        if (phone) {
			options.phone = phone
			log.debug 'sending SMS'
		} else if (pushAndPhone == 'Yes') {
        	options.method = 'both'
            options.phone = phone
        } else options.method = 'push'
		sendNotification(msg, options)
	}
    log.debug "sent message: ${msg}"
}