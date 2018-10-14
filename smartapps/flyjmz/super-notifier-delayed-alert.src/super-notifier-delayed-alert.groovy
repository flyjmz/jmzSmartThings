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
    1.6 - 1Feb2018, added timestamp to messages and debug logging option
   	1.7 - 21Feb2018, bugfix - fixed timestamp so hours are in 24-hour time since there isn't an AM/PM
    1.8 - 5Mar2018, bugfix - fixed waitThreshold title in preferences
    1.9 - 17Apr2018, added power meter monitoring per @ErnieG request
    1.9.1 - 20Apr2018, fixed power meter monitoring, added "temp now ok" message (apparently I forgot it...)
    1.9.2 - 24Jul2018, added contact book like feature to ease SmartThings' depricating the real contact book
    1.9.3 - 6Aug2018, fixed bug that forced you to enter a SMS phone number in the parent app no matter what
    1.9.4 - 13Oct2018, added audio notifications for speech synthesis devices, added "only when switch on/off" to More Options settings, user can now set if the snooze switch snoozes when it is on or off.

To Do:
-None!
*/

def appVersion() {"1.9.4"}
 
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
            input "monitorType", "enum", title: "Monitor what?", required: true, options: ["Contact Sensor", "Switch", "Temperature", "Lock", "Power"], submitOnChange: true
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
            if (monitorType == "Power") {
                input "myPower", "capability.powerMeter", title: "Which Power Meter?", required: true
                input "powerTooHigh", "number", title: "Power Too High When Above:", range: "*..*", required: false
                input "powerTooLow", "number", title: "Power Too Low When Below:", range: "*..*", required: false
            }
        }
        section("Message Details") {
            input "waitThreshold", "number", title: "Delay time before alerting (minutes):", required: true
            input "messageText", "text", title: "Custom Message Text (optional)", required: false
            input "useTimeStamp", "bool", title: "Add timestamp to messages?", required: false
        }

        section("Periodic Notificaitons") {
            paragraph "You'll receive an alert when it is left that way after your defined time period (above) and also onces it returns to normal.  Optionally, you can set periodic notifications for times in between as well." 
            input "periodicNotifications", "bool", title: "Receive periodic notifications?", required: false, submitOnChange: true
            if (periodicNotifications) {
            	input "waitMinutes", "number", title: "Time between periodic notifications? (minutes)", required: false
                input "modeChange", "bool", title: "Notify on mode change?", required: false
                input "sunChange", "bool", title: "Notify at sunrise/sunset?", required: false
                paragraph "Periodic Notifications can be snoozed easily with a virtual switch device type.  This is useful when you are unable to resolve an issue and the notifications become irritable.  A 'snooze' switch in your Things is easier to hit than changing settings." 
                input "snoozeSwitch", "capability.switch", title: "Snooze periodic notifications when this switch is...?", required: false, submitOnChange: true
				if (snoozeSwitch) input "snoozeSwitchOnOrOff","enum", title: "...On or Off?", multiple: false, required: true, options: ["On", "Off"]
        	}      
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
                input name: "speechDevices", type: "capability.speechSynthesis", title: "Which Speakers?", required: true, multiple: true
            }
        }

        section() {
            label title: "Assign a name", required: true
        }

        section(title: "Execution Restrictions", hidden: false, hideable: true) {
            def timeLabel = timeIntervalLabel()
            href "certainTime", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
            input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
            input "modes", "mode", title: "Only when mode is", multiple: true, required: false
            input "controlSwitch", "capability.switch", title: "Only when this switch is...?", required: false, submitOnChange: true
            if (controlSwitch) input "controlSwitchOnOrOff","enum", title: "...On or Off?", multiple: false, required: true, options: ["On", "Off"]
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
	log.info "installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.info "updated with settings: ${settings}"
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
    } else if (myPower) {
        subscribe(myPower, "power", powerHandler)
    } else {if (parent.loggingOn) log.debug "Not subscribing to any device events"}
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
    def tempState1 = temp.currentState("temperature").doubleValue  //trigger is based on the event subcription, but the temp value for notifications is a direct state pull    
    log.info "tempHandler found ${evt.displayName} is ${tempState1} ${location.temperatureScale}."
    if (!atomicState.msgSent) {  //need this check because temperatures report at intervals on a scale of values (unlike switches and contact sensors), if we didn't check this, every time it recieved a new temp that was still out of limits, it'd reset the event time and start the periodic notifcations over.
       if (tempState1 > tooHot || tempState1 < tooCold) {
            if (parent.loggingOn) log.debug "Temp out of limits and haven't sent a message yet, sending to eventHandler."
            eventHandler(evt)
        } else {
            if (parent.loggingOn) log.debug "Temp within limits and no messages sent, doing nothing."
        }
    } else {
        if (tempState1 <= tooHot && tempState1 >= tooCold) {
            if (parent.loggingOn) log.debug "Temp within limits and messages sent, sending to okHandler()."
            okHandler(evt)
        } else {
            if (parent.loggingOn) log.debug "Temp still out of limits and messages sent, stillWrongMsger() will handle this."
        }
    }
}

def gettooHigh() {
    def power1 = powerTooHigh
    if (power1 == null) power1 = 2000000.0
    return power1
}

def gettooLow() {
    def power2 = powerTooLow 
    if (power2 == null) power2 = -3000000.0
    return power2
}

def powerHandler(evt) {
    log.info "powerHandler found ${evt.displayName} is outputting ${evt.value} W, want it between $tooLow W & $tooHigh W"
    def powerValue = evt.value.toDouble()
    if (!atomicState.msgSent) {
        if (powerValue > tooHigh || powerValue < tooLow) {
            if (parent.loggingOn) log.debug "Power out of limits and haven't sent a message yet, sending to eventHandler."
            eventHandler(evt)
        } else {
            if (parent.loggingOn) log.debug "Power within limits and no messages sent, doing nothing."
        }
    } else {
    	if (powerValue <= tooHigh && powerValue >= tooLow) {
    		if (parent.loggingOn) log.debug "Power within limits and messages sent, sending to okHandler()."
            okHandler(evt)
        } else {
        	if (parent.loggingOn) log.debug "Power still out of limits and messages sent, stillWrongMsger() will handle this."
        }
    }
}

def eventHandler(evt) {
    def newWaitThreshold = (waitThreshold > 0) ? waitThreshold : 0.1
    log.info "eventHandler has ${evt.displayName}: ${evt.name}: ${evt.value}, scheduling stillWrong() in ${newWaitThreshold} minutes"
    runIn((newWaitThreshold * 60), stillWrong)
    atomicState.problemTime = now()
}

def stillWrong() { 
	if (parent.loggingOn) log.debug "stillWrong() started"
	def myContactState = myContact?.currentState("contact")?.value
    def mySwitchState = mySwitch?.currentState("switch")?.value
    def tempState2 = temp?.currentState("temperature")?.doubleValue
    def myLockState = myLock?.currentState("lock")?.value
    def myPowerState = myPower?.currentState("power")?.doubleValue
    if (parent.loggingOn) log.debug "myContactState is $myContactState, mySwitchState is $mySwitchState, tempState2 is $tempState2, myLockState is $myLockState, myPowerState is $myPowerState"
    if (openClosed) {
    	if (openClosed == "Open") {
			if (myContactState == "open") {
            	if (parent.loggingOn) log.debug "Contact is still open"
                stillWrongMsger()
            } else log.debug "the contact is closed and you want it that way"  //okHandler will send the it's ok messages for all these cases
        } else {
        	if (myContactState == "closed") {
            	if (parent.loggingOn) log.debug "Contact is still closed"
                stillWrongMsger()
            } else {if (parent.loggingOn) log.debug "the contact is open and you want it that way"}
    	}
    }
    if (onOff) {
    	if (onOff == "On") {
			if (mySwitchState == "on") {
            	if (parent.loggingOn) log.debug "Switch is still on"
				stillWrongMsger()
            } else log.debug "the switch is off and you want it that way"
        } else {
        	if (mySwitchState == "off") {
	            if (parent.loggingOn) log.debug "Switch is still off"
				stillWrongMsger()
            } else {if (parent.loggingOn) log.debug "the switch is on and you want it that way"}
    	}
    }
    if (lockedUnlocked) {
        if (lockedUnlocked == "locked") {
            if (myLockState == "locked") {
                if (parent.loggingOn) log.debug "Lock is still locked"
                stillWrongMsger()
            } else {if (parent.loggingOn) log.debug "the lock is unlocked and you want it that way"}
        } else {
            if (myLockState == "unlocked") {
                if (parent.loggingOn) log.debug "Lock is still unlocked"
                stillWrongMsger()
            } else {if (parent.loggingOn) log.debug "the lock is lcoked and you want it that way"}
        }
    }
    if (temp) {
        if (tempState2 > tooHot || tempState2 < tooCold) {
            if (parent.loggingOn) log.debug "Temperature is still out of limits (${tempState2} ${location.temperatureScale})"
            stillWrongMsger()
        } else {if (parent.loggingOn) log.debug "Temp is within limits, no action taken."}
    }
    if (myPower) {
        if (myPowerState > tooHigh || myPowerState < tooLow) {
            if (parent.loggingOn) log.debug "Power is still out of limits (${myPowerState} W)"
            stillWrongMsger()
        } else {if (parent.loggingOn) log.debug "Power is within limits, no action taken."}
    }
}

def stillWrongMsger() {
	if (parent.loggingOn) log.debug "stillWrongMsger() started"
	def myContactState2 = myContact?.currentState("contact")
    def mySwitchState2 = mySwitch?.currentState("switch")
    def myLockState2 = myLock?.currentState("lock")
    def tempState3 = temp?.currentState("temperature")
    def myPowerState2 = myPower?.currentState("power")
    if (allOk) {
        if (parent.loggingOn) log.debug "Event within time/day/mode/switch constraints"
    	if (!atomicState.msgSent) {
            if (myContact) sendMessage("${myContact?.displayName} is still ${myContactState2?.value}!")
            if (mySwitch) sendMessage("${mySwitch?.displayName} is still ${mySwitchState2?.value}!")
            if (myLock) sendMessage("${myLock?.displayName} is still ${myLockState2?.value}!")
            if (temp) sendMessage("${temp?.displayName} is still ${tempState3?.value} ${location.temperatureScale}!")
            if (myPower) sendMessage("${myPower?.displayName} is still ${myPowerState2?.value} W!")
            atomicState.msgSent = true
            if (parent.loggingOn) log.debug "sent first message, set atomicState.msgSent to ${atomicState.msgSent}"
       		if (periodicNotifications) {
            	if (waitMinutes != null) {
                	def newWaitMinutes = (waitMinutes > 0) ? waitMinutes : 0.1 //if user entered '0' then it'd break, so adjusting it for them
                	runIn((newWaitMinutes * 60), stillWrong)
                	if (parent.loggingOn) log.debug "periodic notifications is on, scheduled stillWrong() to run again in ${newWaitMinutes} minutes"
            	}
            }
        } else if (periodicNotifications && atomicState.msgSent) {
            def snooze = false
            if (snoozeSwitch && snoozeSwitchOnOrOff) {
                if (snoozeSwitchOnOrOff == "On" && snoozeSwitch?.currentState("switch")?.value == "on") {
                    snooze = true
                    if (parent.loggingOn) log.debug "snoozing"
                } else if (snoozeSwitchOnOrOff == "Off" && snoozeSwitch?.currentState("switch")?.value == "off") {
                    snooze = true
                    if (parent.loggingOn) log.debug "snoozing"
                } else {
                    if (parent.loggingOn) log.debug "Snooze switch is not on/off for snoozing, not snoozing."
                }
            } else if (snoozeSwitch && snoozeSwitchOnOrOff == null) {
            	log.error "You have a snooze switch selected but haven't updated your settings for whether want to snooze when it is on or off. Assuming you want to snooze when the switch is on. Please update your settings."
            	snooze = true
            }
            if (!snooze) log.debug "sending periodic notification"
            int timeSince = ((now() - atomicState.problemTime) / 60000) //time since issue occured in whole minutes
            if (!snooze && timeSince > 180) {  //determines whether to report in hours or minutes (longer than 180 minutes is reported in hours), and ensures alerts aren't snoozed.
            	int timeMsg = timeSince / 60
                if (myContact) sendMessage("Periodic Alert: ${myContact?.displayName} has been ${myContactState2?.value} for ${timeMsg} hours!")
            	if (mySwitch) sendMessage("Periodic Alert: ${mySwitch?.displayName} has been ${mySwitchState2?.value} for ${timeMsg} hours!")
                if (myLock) sendMessage("Periodic Alert: ${myLock?.displayName} has been ${myLockState2?.value} for ${timeMsg} hours!")
                if (temp) sendMessage("Periodic Alert: ${temp?.displayName} has been out of limits for ${timeMsg} hours! (Currently $tempState3.value ${location.temperatureScale}).")
                if (myPower) sendMessage("Periodic Alert: ${myPower?.displayName} has been out of limits for ${timeMsg} hours! (Currently $myPowerState2.value W).")
            } 
            if (!snooze && timeSince < 180) {
        		if (myContact) sendMessage("Periodic Alert: ${myContact?.displayName} has been ${myContactState2?.value} for ${timeSince} minutes!")
            	if (mySwitch) sendMessage("Periodic Alert: ${mySwitch?.displayName} has been ${mySwitchState2?.value} for ${timeSince} minutes!")
                if (myLock) sendMessage("Periodic Alert: ${myLock?.displayName} has been ${myLockState2?.value} for ${timeSince} minutes!")
                if (temp) sendMessage("Periodic Alert: ${temp?.displayName} has been out of limits for ${timeSince} minutes! (Currently $tempState3.value ${location.temperatureScale})")
                if (myPower) sendMessage("Periodic Alert: ${myPower?.displayName} has been out of limits for ${timeSince} minutes! (Currently $myPowerState2.value W).")
            }
            if (waitMinutes != null) {
            	def newWaitMinutes = (waitMinutes > 0) ? waitMinutes : 0.1
            	runIn((newWaitMinutes * 60), stillWrong)
            	if (parent.loggingOn) log.debug "periodic notifications is scheduling the next stillWrong() to run in ${newWaitMinutes} minutes"
        	}
        } else if (!periodicNotifications && atomicState.msgSent) {
        	if (parent.loggingOn) log.debug "message already sent once, not using periodic notifcations, so not sending another message"
        }
    } else {
    	if (parent.loggingOn) log.debug "event is outside of time/day/mode/switch conditions, no message sent, but monitoring in case it doesn't return to normal before it is within those time/day/mode/switch conditions"
    	def newWaitThreshold = (waitThreshold > 0) ? waitThreshold : 0.1
        runIn((waitThreshold * 60), stillWrong)
    }
}

def okHandler(evt) {
	if (atomicState.msgSent) {
    	sendMessage("${evt.device.displayName} is OK. Now ${evt.value}.")
        atomicState.msgSent = false
        if (parent.loggingOn)log.debug "okHandler() evoked, message sent, atomicState.msgSent is now: ${atomicState.msgSent}"
    } else {if (parent.loggingOn) log.debug "it's okay now, and never sent left open/closed/on/off message, so no need to send an 'ok' message"}
}

def periodicNotifier(evt) {
	if (parent.loggingOn) log.debug "periodic notifier got ${evt.descriptionText}, sending to stillWrong()"
    stillWrong()
}

private getAllOk() {
	modeOk && daysOk && timeOk && switchOk
}

private getSwitchOk() {
    def result = true
    if (controlSwitch) {
		if (controlSwitchOnOrOff == "On" && controlSwitch?.currentState("switch")?.value != "on") {
        result = false
        } else if (controlSwitchOnOrOff == "Off" && controlSwitch?.currentState("switch")?.value != "off") {
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
	if (parent.loggingOn) log.debug "timeOk = $result"
	return result
}

private getModeOk() {
	def result = !modes || modes.contains(location.mode)
    if (parent.loggingOn) log.debug "modeOk = $result"
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
    if (startingX == "Sunrise" && endingX == "Sunrise") {result = "Sunrise" + offset(startSunriseOffset) + " to " + "Sunrise" + offset(endSunriseOffset)}
    else if (startingX == "Sunrise" && endingX == "Sunset") {result = "Sunrise" + offset(startSunriseOffset) + " to " + "Sunset" + offset(endSunsetOffset)}
    else if (startingX == "Sunset" && endingX == "Sunrise") {result = "Sunset" + offset(startSunsetOffset) + " to " + "Sunrise" + offset(endSunriseOffset)}
    else if (startingX == "Sunset" && endingX == "Sunset") {result = "Sunset" + offset(startSunsetOffset) + " to " + "Sunset" + offset(endSunsetOffset)}
    else if (startingX == "Sunrise" && ending) {result = "Sunrise" + offset(startSunriseOffset) + " to " + hhmm(ending, "h:mm a z")}
    else if (startingX == "Sunset" && ending) {result = "Sunset" + offset(startSunsetOffset) + " to " + hhmm(ending, "h:mm a z")}
    else if (starting && endingX == "Sunrise") {result = hhmm(starting) + " to " + "Sunrise" + offset(endSunriseOffset)}
    else if (starting && endingX == "Sunset") {result = hhmm(starting) + " to " + "Sunset" + offset(endSunsetOffset)}
    else if (starting && ending) {result = hhmm(starting) + " to " + hhmm(ending, "h:mm a z")}
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
                    log.info "sent '$msg' via SMS to ${SMSContactsSplit[i]}"
                    sendSmsMessage(SMSContactsSplit[i], msg)
                }
            }
        }
    }
}