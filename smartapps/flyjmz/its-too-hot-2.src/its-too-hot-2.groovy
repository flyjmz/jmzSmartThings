/**
 *  Copyright 2016 flyjmz
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
 *  It's Too Hot 2
 *
 *  Author: flyjmz
 */
definition(
    name: "It's Too Hot 2",
    namespace: "flyjmz",
    author: "flyjmz",
    description: "Monitor the temperature and when it rises above your setting get a notification and/or turn on a switch.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/its-too-hot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/its-too-hot@2x.png"
)

preferences {
	section("Monitor the temperature...") {
		input "temperatureSensor1", "capability.temperatureMeasurement"
	}
	
    section("When the temperature rises above...") {
		input "temperature1", "number", title: "Temperature?"
	}
   
   section("Notifications") {
        	input("recipients", "contact", title: "Send notifications to") {
    		input("sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: true)
    		input("phone1", "phone", title: "Phone Number for Text Message: (leave blank for no SMS)", required: false)
            	paragraph "You will receive notifications when the website goes down and when it comes back up.  Optionally, you can set periodic notifications in between as well." 
            	input("periodicNotifications", "enum", title: "Recieve periodic notifications?", options: ["Yes", "No"], required: true)
                input("waitminutes", "number", title: "Minutes between periodic notifications?", required: true)
                }
        }
    
    section("Turn on which A/C or fan...") {
		input "switch1", "capability.switch", required: false
	}
}

def installed() {
	log.debug "installed"
	unschedule()
    unsubscribe()
    subscribe(temperatureSensor1, "temperature", temperatureHandler)
    state.msgalreadysent = false
    state.msgsenttime = now()
}

def updated() {
	log.debug "updated"
	unschedule()
    unsubscribe()
    subscribe(temperatureSensor1, "temperature", temperatureHandler)
    state.msgalreadysent = false
    state.msgsenttime = now()
}

def temperatureHandler(evt) {
	log.debug "temperature handler launched, msgalreadysent is ${state.msgalreadysent}"
    def tempState = temperatureSensor1.currentState("temperature")  //trigger is based on the event subcription, but the temp value for notifications is a direct state pull
	log.debug "temperature: ${tempState.doubleValue}, want it below ${temperature1}"
	def tooHot = temperature1
	def mySwitch = settings.switch1
    if(tempState.doubleValue >= tooHot) {
    	if (state.msgalreadysent == false) {
        	state.msgsenttime = now()
        	log.debug "Too hot at time ${state.msgsenttime}, sending too hot message, turning switch on."
    		send("${temperatureSensor1.displayName} is too hot: ${tempState.value}${tempState.unit?:"F"}")
		switch1?.on()
		state.msgalreadysent = true
        	log.debug "message sent, msgalreadysent is now ${state.msgalreadysent}"
        } else {				
         	if(periodicNotifications == "Yes") {
         		if(now()-state.msgsenttime >= waitminutes * 60000) {     //changes waitminutes to milliseconds for how now() uses time
         			log.debug "It is still too hot, sending still too hot message"
    				send("${temperatureSensor1.displayName} is still too hot, now: ${tempState.value}${tempState.unit?:"F"}")
       				state.msgalreadysent = true
            			state.msgsenttime = now()  //resets the sent time to the lastest message
        			log.debug "still too hot message sent, msgalreadysent is now ${state.msgalreadysent}"
         		} else {
            			log.debug "too soon since last message, waiting..."
         		}
            	}
        }
    } else {
    	if(state.msgalreadysent == true) {
        	log.debug "temp is okay after sending message, sending ok message and turning off switch."
            send("${temperatureSensor1.displayName} is OK: temp now ${tempState.value}${tempState.unit?:"F"}")
			switch1?.off()
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
