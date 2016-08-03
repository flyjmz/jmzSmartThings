/**
 *  Power Is Out
 *
 *  Copyright 2014 flyjmz
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
 *	Version 1.1    2 August 2016 - updated notes within app to clarify.
 *	To do: give options when power is back on to turn off/on lights, switches, etc (like cree bulbs, etc)
 */
definition(
    name: "Power Is Out",
    namespace: "flyjmz",
    author: "flyjmz",
    description: "Alert me of power loss using SmartSense Motion v1's change from wired-power to battery-power.  Note: SmartThings hub and internet connection must be working! You can connect the hub and internet connection device (e.g. modem, router, etc.) to a UPS (uniteruptable power supply) so they stay powered even when the house looses power.  Then the motion detector can detect the loss and the hub and router will still have enough power to get the message out before they fail as well.",
    category: "Safety & Security",
    iconX2Url: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/home2-icn@2x.png"
)


preferences {
	section("When there is wired-power loss on...") {
			input "motion1", "capability.motionSensor", title: "Where?"
            paragraph "Must be a SmartSense Motion v1.  Also, SmartThings Hub and internet connection (modem/router) must retain power for this to work (e.g. connect them to a UPS)."
	}
	section("Enter Phone number if you want a text message (optional) as well as a push notificaiton."){
    	input "pushAndPhone", "enum", title: "Send SMS?", required: false, metadata: [values: ["Yes","No"]]
		input "phone1", "phone", title: "Phone Number (only for SMS, optional)", required: false
	}
}

def installed() {
	unsubscribe()
    subscribe(motion1, "powerSource.battery", onBatteryPowerHandler)
    subscribe(motion1, "powerSource.powered", PoweredPowerHandler)
}

def updated() {
	unsubscribe()
	subscribe(motion1, "powerSource.battery", onBatteryPowerHandler)
    subscribe(motion1, "powerSource.powered", PoweredPowerHandler)
}

def onBatteryPowerHandler(evt) {
	log.trace "$evt.value: $evt"
	def msg = "${motion1.label ?: motion1.name} sensed Power is Out!"
    
	log.debug "sending push for power is out"
	sendPush(msg)
    
    if ( phone1 && pushAndPhone ) {
    	log.debug "sending SMS to ${phone1}"
   	sendSms(phone1, msg)
	}
}

def PoweredPowerHandler(evt) {
	log.trace "$evt.value: $evt"
	def msg = "${motion1.label ?: motion1.name} sensed Power is Back On!"
    
	log.debug "sending push for power is back on"
	sendPush(msg)
    
    if ( phone1 && pushAndPhone ) {
    	log.debug "sending SMS to ${phone1}"
    	sendSms(phone1, msg)
	}
}