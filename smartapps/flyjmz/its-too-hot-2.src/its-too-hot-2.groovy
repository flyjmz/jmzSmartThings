/**
 *  It's Too Hot 2
 *
 *	https://github.com/flyjmz/jmzSmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Version 1.1 - 4 August 2016		Cleaned up code
 *	Version 1.2 - 3 September 2016	Corrected typo in Preferences>Notifications
 *  Version 1.3 - 4 October 2017	Add capability for "temp too cold" and to turn the switch on or off.
 *	Version 1.4 - 5 October 2017	Fixed Preferences Page bug, added ability to enter negative numbers for the temps.
 *
 *  To do:
 *	(none)
 */
 
definition(
    name: "It's Too Hot 2",
    namespace: "flyjmz",
    author: "flyjmz230@gmail.com",
    description: "Monitor the temperature and when it rises above or below your desired range recieve a notification and turn a switch on or off.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/its-too-hot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/its-too-hot@2x.png"
)

preferences {
    page(name:"tooHot2")
}

def tooHot2() {
    dynamicPage(name:"tooHot2", install: true, uninstall: true, submitOnChange: true) {
        section("Monitor the temperature...") {
            input "temperatureSensor1", "capability.temperatureMeasurement"
        }

        section("When the temperature rises above...") {
            input "tempTooHot", "number", title: "Too Hot?", range: "*..*", required: false
        }

        section("When the temperature dips below...") {
            input "tempTooCold", "number", title: "Too Cold?", range: "*..*", required: false
        }

        section("Notifications") {
            input("recipients", "contact", title: "Send notifications to") {
                input("sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: true)
                input("phone1", "phone", title: "Phone Number for Text Message: (leave blank for no SMS)", required: false)
            }   
            paragraph "You will receive notifications when the temperature is out of your set limits and also when it returns within limits.  Optionally, you can set periodic notifications to occur in between as well." 
            input("periodicNotifications", "enum", title: "Receive periodic notifications?", options: ["Yes", "No"], required: true, submitOnChange: true)
            if (periodicNotifications == "Yes") input("waitminutes", "number", title: "Minutes between periodic notifications? (multiples of 5 only)", required: true)       
        }

        section("Turn a switch (e.g. A/C or fan) on or off?") {
            input "switch1", "capability.switch", title: "Which Switch?", required: false, submitOnChange: true
            if (switch1 != null) input "onOrOff", "enum", title: "Turn it on or off?", options: ["On", "Off"], required: true //todo - make this work in the code now
        }
    }
}

def installed() {
	log.debug "installed"
	intialize()
}

def updated() {
	log.debug "updated"
	intialize()
}

def intialize() {
    unschedule()
    unsubscribe()
    subscribe(temperatureSensor1, "temperature", temperatureHandler)
    state.msgalreadysent = false
    state.msgsenttime = now()
    log.debug "Want temperature between ${tooCold} and ${tooHot}."
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

def switchSwitcher(control) {
	log.debug "Turning ${switch1?.device} ${control}"
	if (control == "On") switch1?.on()
    if (control == "Off") switch1?.off()
}

def temperatureHandler(evt) {
    def tempState = temperatureSensor1.currentState("temperature")  //trigger is based on the event subcription, but the temp value for notifications is a direct state pull
    log.debug "temperature handler launched, msgalreadysent is ${state.msgalreadysent}, the temperature is ${tempState.doubleValue}, want temperature between ${tooCold} and ${tooHot}"
    if(tempState.doubleValue > tooHot || tempState.doubleValue < tooCold) {
    	if (state.msgalreadysent == false) {
            state.msgsenttime = now()
            log.debug "Temp out of limits at ${state.msgsenttime}, sending message."
            send("${temperatureSensor1.displayName} is out of limits at ${tempState.value}${tempState.unit?:"F"}")
			switchSwitcher(onOrOff)
			state.msgalreadysent = true
        	log.debug "message sent, msgalreadysent is now ${state.msgalreadysent}"
        } else {				
         	if(periodicNotifications == "Yes") {
         		if(now()-state.msgsenttime >= waitminutes * 60000) {     //changes waitminutes to milliseconds for how now() uses time
         			log.debug "It is still out of limits, now: ${tempState.value}.  Sending still out of limits message."
    				send("${temperatureSensor1.displayName} is still out of limits, now: ${tempState.value}${tempState.unit?:"F"}")
       				state.msgalreadysent = true
                    state.msgsenttime = now()  //resets the sent time to the lastest message
        			log.debug "still out of limits message sent, msgsenttime is now ${state.msgsenttime}"
         		} else {
                    log.debug "too soon since last message, waiting..."
         		}
            } else log.debug "It is still out of limits, now: ${tempState.value}.  Did not select periodicNotifications, no need to do anything."
        }
    } else {
    	if(state.msgalreadysent == true) {
        	log.debug "temp is okay after sending message, sending ok message."
            send("${temperatureSensor1.displayName} is OK: temp now ${tempState.value}${tempState.unit?:"F"}")
			def oppositeAction = "On"
            if (onOrOff == "On") {
            	oppositeAction = "Off"
            } else oppositeAction = "On"
            switchSwitcher(oppositeAction)
        	state.msgalreadysent = false
            log.debug "temp became ok, msgalreadysent changed to ${state.msgalreadysent}"
        } else {
            log.debug "temp is ok: ${tempState.doubleValue}, msgalreadysent is: ${state.msgalreadysent}"
            state.msgalreadysent = false
        }	
    }
}

private send(msg) {
    if (location.contactBookEnabled) {
        log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        if (sendPushMessage != "No") {
            log.debug("sending push message")
            sendPush(msg)
        }

        if (phone1) {
            log.debug("sending text message")
            sendSms(phone1, msg)
        }
    }

    log.debug msg
}