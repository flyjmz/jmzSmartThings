/*
Smart Alert - Delayed Alert
   
https://github.com/flyjmz/jmzSmartThings


   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
   in compliance with the License. You may obtain a copy of the License at:
 
       http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
   for the specific language governing permissions and limitations under the License.
 
Version History:
	1.0 - 5Sep2016, Initial Commit
	1.1 - 10Oct2016, all tweaks rolled into public release
 
*/
 
definition(
    name: "Smart Alert - Delayed Alert",
    namespace: "flyjmz",
    author: "flyjmz230@gmail.com",
    parent: "flyjmz:Smart Alert",
    description: "Child app for Smart Alert to notifiy when a contact is left open or closed, or a switch is left on or off.",
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
            input "monitorType", "enum", title: "Monitor a switch or contact sensor?", required: true, options: ["Contact Sensor", "Switch"], submitOnChange: true
            if (monitorType == "Contact Sensor") {
            	input "myContact", "capability.contactSensor", title: "Which contact?", required: true
            	input "openClosed", "enum", title: "When left open or closed?", required: true, options: ["Open", "Closed"]
            }
            if (monitorType == "Switch") {
            	input "mySwitch", "capability.switch", title: "Which switch?", required: true
            	input "onOff", "enum", title: "When left on or off?", required: true, options: ["On", "Off"]
            }
        }
        section("For more than this many minutes") {
            input "waitThreshold", "number", description: "Number of minutes", required: true
        }

        section("Send this custom message (optional, sends standard status message if not specified)"){
                input "messageText", "text", title: "Message Text", required: false
        }

        section("Periodic Notificaitons") {
            paragraph "You'll receive an alert when it is left that way after your defined time period (above) and also onces it returns to normal.  Optionally, you can set periodic notifications for in between as well." 
            input "periodicNotifications", "enum", title: "Receive periodic notifications?", options: ["Yes", "No"], required: true, submitOnChange: true
            if (periodicNotifications == "Yes") input "waitMinutes", "number", title: "Minutes between periodic notifications?", required: true
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
    initialize()
}

def initialize () {
	log.trace "initialize()"
	subscribeToEvents()
    atomicState.msgSent = false
}

def subscribeToEvents() {
    if (openClosed && openClosed == "Open") {
    	subscribe(myContact, "contact.open", eventHandler)
        subscribe(myContact, "contact.closed", okHandler)
    } else if (openClosed && openClosed == "Closed") {
    	subscribe(myContact, "contact.closed", eventHandler)
        subscribe(myContact, "contact.open", okHandler)
    } else if (onOff && onOff == "On") {
		subscribe(mySwitch, "switch.on", eventHandler)
        subscribe(mySwitch, "switch.off", okHandler)
    } else if (onOff && onOff == "Off"){
    	subscribe(mySwitch, "switch.off", eventHandler)
		subscribe(mySwitch, "switch.on", okHandler)
    } else log.debug "Not subscribing to any events"
}

def eventHandler(evt) {
	log.trace "eventHandler has ${evt.displayName}: ${evt.name}: ${evt.value}, scheduling tooLong() in ${waitThreshold} minutes"
    runIn((waitThreshold * 60), tooLong)
}

def tooLong() { 
	def myContactState = myContact?.currentState("contact")
    def mySwitchState = mySwitch?.currentState("switch")
    if (openClosed) {
    	if (openClosed == "Open") {
			if (myContactState.value == "open") {
            	log.debug "Contact is still open"
                tooLongMsger()
            } else log.debug "the contact is closed and you want it that way"  //okHandler will send the it's ok messages
        } else {
        	if (myContactState.value == "closed") {
            	log.debug "Contact is still closed"
                tooLongMsger()
            } else log.debug "the contact is open and you want it that way"
    	}
    } 
    if (onOff) {
    	if (onOff == "On") {
			if (mySwitchState.value == "on") {
            	log.debug "Switch is still on"
				tooLongMsger()
            } else log.debug "the switch is off and you want it that way"
        } else {
        	if (mySwitchState.value == "off") {
	            log.debug "Switch is still on"
				tooLongMsger()
            } else log.debug "the switch is on and you want it that way"
    	}
    }
}

def tooLongMsger() {
	def myContactState2 = myContact?.currentState("contact")
    def mySwitchState2 = mySwitch?.currentState("switch")
    if (allOk) {
        log.debug "event within time/day/mode constraints"
    	if (atomicState.msgSent == false) {
            if (myContact) sendMessage("${myContact?.displayName} is still ${myContactState2?.value}!")
            if (mySwitch) sendMessage("${mySwitch?.displayName} is still ${mySwitchState2?.value}!")
            atomicState.msgSent = true
            log.debug "sending first message, set atomicState.msgSent to ${atomicState.msgSent}"
       		if (periodicNotifications == "Yes") {
            	runIn((waitMinutes * 60), tooLong)
                log.debug "periodic notifications is on, scheduling tooLong() to run again in ${waitMinutes} minutes"
            }
        } else if (periodicNotifications == "Yes") {
        	log.debug "sending periodic notification"
        	if (myContact) sendMessage("Periodic Alert: ${myContact?.displayName} is still ${myContactState2?.value}!")
            if (mySwitch) sendMessage("Periodic Alert: ${mySwitch?.displayName} is still ${mySwitchState2?.value}!")
            runIn((waitMinutes * 60), tooLong)
            log.debug "periodic notifications is scheduling the next tooLong() to run in ${waitMinutes} minutes"
        }
    } else {
    	log.debug "event outside of time/day/mode conditions, ignoring"
    	runIn((waitThreshold * 60), tooLong) //keep checking status regardless of allOk() so that the message will happen once allOk() is true
    }
}

def okHandler(evt) {
	if (atomicState.msgSent) {
    	//def evtDeviceState = evt.value
    	sendMessage("${evt.device.displayName} is now ${evt.value}.")
        atomicState.msgSent = false
        log.debug "okHandler() evoked, message sent, atomicState.msgSent is now: ${atomicState.msgSent}"
        /*   This is what it used to be, see if the above works...
        if (evt.value == "closed") sendMessage("${myContact.displayName} is now closed.")
        if (evt.value == "open") sendMessage("${myContact.displayName} is now open.")
        if (evt.value == "off") sendMessage("${mySwitch.displayName} is now off.")
        if (evt.value == "on") sendMessage("${mySwitch.displayName} is now on.")
        */
    } else log.debug "it's okay now, and never sent left open/closed/on/off message, so no need to send an 'ok' message"
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
	/*    ----some of this is wrong
    log.debug "!modes is ${!modes}"
    log.debug "modes.contains(location.mode) is ${modes.contains(location.mode)}"
    log.debug "result is ${result}"
    log.trace "modeOk = $result"
	*/
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
}