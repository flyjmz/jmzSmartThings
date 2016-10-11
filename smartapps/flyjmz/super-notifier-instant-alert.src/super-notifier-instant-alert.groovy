/*
Super Notifier - Instant Alert
   
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
        }

        section("Send this custom message (optional, sends standard status message if not specified)") {
            input "messageText", "text", title: "Message Text", required: false
        }

        section("Notification Type"){
            input("recipients", "contact", title: "Send notifications to") {
                input "pushAndPhone", "enum", title: "Also send SMS? (optional, it will always send push)", required: false, options: ["Yes", "No"]		
                input "phone", "phone", title: "Phone Number (only for SMS)", required: false
                paragraph "If outside the US please make sure to enter the proper country code"
            }
    	}

        section("Message Timing") {
            paragraph "Minimum time between messages (optional, defaults to every message)"
            input "frequency", "decimal", title: "Minutes", required: false
        }

        section() {
                label title: "Assign a name", required: true
        }

        section(title: "More options", hidden: hideOptionsSection(), hideable: true) {
                def timeLabel = timeIntervalLabel()
                href "certainTime", title: "Only during a certain time", description: timeLabel ?: "Tap to set", state: timeLabel ? "complete" : null
                input "days", "enum", title: "Only on certain days of the week", multiple: true, required: false, options: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
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
	log.debug "Installed with settings: ${settings}"
	subscribeToEvents()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	subscribeToEvents()
}

def subscribeToEvents() {
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
}

def eventHandler(evt) {
	log.debug "Notify got event ${evt} from ${evt.displayName}"
	if (frequency) {
		def lastTime = state[evt.deviceId]
		if (lastTime == null || now() - lastTime >= frequency * 60000) {
        	log.debug "frequency used and it is time for new message, checking if within time & date period"
            if(allOk) createInstantMessage(evt)
            state[evt.deviceId] = now()
		}
        else {
        	log.debug "frequency used but it is too early to send a new message"
        }
	}
	else {
    	log.debug "frequency not used, checking if within time & date period"
		if(allOk) createInstantMessage(evt)
	}
}

def createInstantMessage(evt) {
	String msg = messageText
    def messageDefault = ""
    if (!messageText) {
		if (evt.name == 'presence') {
			if (evt.value == 'present') {
				messageDefault = "${evt.device} has arrived"
			} else {
				messageDefault = "${evt.device} has left"
			}
		} else {
			messageDefault = "${evt.device} is ${evt.value}"
		}
        msg = messageDefault
	}
	log.debug "created message to send. msg is ${msg}"
    sendMessage(msg)
}

private getAllOk() {
	daysOk && timeOk
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
	log.trace "TimeOk = $result"
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