/**
 *  SmartPing's Automations and Notifications  (This is the child app)
 *
 *  Copyright 2016 flyjmz, based on previous work: Copyright 2016 Jason Botello
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
 */
 
definition(
	name: "SmartPing's Automations and Notifications",
	namespace: "flyjmz/SmartPing",
	author: "flyjmz",
	parent: "flyjmz/SmartPing:SmartPing with Notifications",
	description: "Monitor a website's uptime every 5 minutes and triggers SmartThings automations if it goes down. Sends notifications, and can be used to effect lights, switches, or alarms when monitoring an external or internal website.",
	category: "My Apps",
	iconUrl: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/smartping.png",
	iconX2Url: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/smartping@2x.png",
	iconX3Url: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/smartping@3x.png"
)

preferences {
	page name: "mainPage", title: "", install: true, uninstall: true
}

def installed() {
	initialize()
}

def updated() {
	initialize()
}

def initialize() {
	if (validateURL()) {
		state.downHost = false
		state.pollVerify = false
        state.timeDown = now()
        state.websiteDownMsgSent = false
        state.sentPeriodMsg = now()
		app.updateLabel("${state.website} Monitor")
		runEvery5Minutes(poll)
    	log.debug "initialized state.downHost = ${state.downHost}, state.pollVerify = ${state.pollVerify}, state.timeDown = ${state.timeDown}, state.websiteDownMsgSent = ${state.websiteDownMsgSent}, state.sentPeriodMsg = ${state.sentPeriodMsg}"
        }
}

def validateURL() {    //TO DO - make sure this will work for https
	state.website = website.toLowerCase()
    	if (state.website.contains(".com") || state.website.contains(".net") || state.website.contains(".org") || state.website.contains(".biz") || state.website.contains(".us") || state.website.contains(".info") || state.website.contains(".io") || state.website.contains(".ca") || state.website.contains(".co.uk") || state.website.contains(".tv") || state.website.contains(":")) {
    		state.website = state.website.trim()
    		if (state.website.startsWith("http://")) {
    			state.website = state.website.replace("http://", "")
        		state.website = state.website.replace("www.", "")
    		}
    		if (state.website.startsWith("https://")) {
    			state.website = state.website.replace("https://", "")
        		state.website = state.website.replace("www.", "")
    		}
    		if (state.website.startsWith("www.")) {
    			state.website = state.website.replace("www.", "")
    		}
    		state.validURL = "true"
            log.debug "state.validURL set to true"
    		return true
	} else {
    		state.validURL = "false"
        	return false
            log.debug "state.validURL set to false"
    	}
}

def mainPage() {
	return dynamicPage(name: "mainPage", title: "") {
    		section {
        		paragraph "URL you want to monitor. ex: google.com"    //TO DO - set this up to work for https, whether we need to type in https:// or not, and talk about putting in the port
            		input(name: "website", title:"URL", type: "text", required: true)
            		input(name: "threshold", title:"False Alarm Threshold (minutes)", type: "number", required: true, defaultValue:2)
        	}
        	section {
        		paragraph "If the URL goes offline. Note: These actions only work if the monitored URL is on a different connection than your Smartthings Hub (i.e. if it's your local webserver on the same internet connection as the hub, the hub may go offline with the website.)"
            		lightInputs()
            		lightActionInputs()
            		switchInputs()
            		switchActionInputs()
            		alarmInputs()
            		alarmActionInputs()
        	}
            section {
            	paragraph "Notifications. Note: Notificaitons work regardless whether the website is internal/external.  Monitoring takes place from the cloud, and notifications are sent from the cloud. As long as your phone has internet, notifications will work."
                    input("recipients", "contact", title: "Send notifications to") {
            		input("sendPushMessage", "enum", title: "Send a push notification?", options: ["Yes", "No"], required: true)
            		input("phone1", "phone", title: "Phone Number for Text Message: (leave blank for no SMS)", required: false)
                    paragraph "You will receive notifications when the website goes down and when it comes back up.  Optionally, you can set periodic notifications in between as well." 
                    input("periodicNotifications", "enum", title: "Recieve periodic notifications?", options: ["Yes", "No"], required: true)
                    input("waitminutes", "number", title: "Minutes between periodic notifications? (multiples of 5 only)", required: true)
                    }
            }
    }
}

def poll() {
	log.debug "poll started, states are: state.downHost = ${state.downHost}, state.pollVerify = ${state.pollVerify}, state.timeDown = ${state.timeDown}, state.websiteDownMsgSent = ${state.websiteDownMsgSent}, state.sentPeriodMsg = ${state.sentPeriodMsg}"
    def reqParams = [
            uri: "http://${state.website}"
    	]
    	if (state.validURL == "true") {
    		try {
        		httpGet(reqParams) { resp ->
            			if (resp.status == 200) {
                			if (state.downHost == true) {
            					turnOffHandler()
                    				log.info "successful response from ${state.website}, turning off handlers"
                			} else {
                    				log.info "successful response from ${state.website}, no handlers"
                			}
            			} else {
            				if (state.downHost == false) {
                				if (state.pollVerify == false) {
        							runIn(60*threshold, pollVerify)
            						state.pollVerify = true
            					}
                				log.info "poll request failed to ${state.website}, calling pollVerify with a ${threshold} minute threshold"
                			} else {
                                log.debug "periodicNotifications = ${periodicNotifications}"
                                if (periodicNotifications == "Yes") {
                                    if (now()-state.sentPeriodMsg >= waitminutes * 60000) {
                                    	log.debug "long enough period, sending periodic message"
                                        def timeSinceDown = (now() - state.timeDown) / 60000    //converts the milliseconds elapsed between timeDown until now to minutes
                                		log.debug "timeSinceDown is ${timeSinceDown} minutes"
                                        send("${state.website} is still down, it's been down for ${timeSinceDown} minutes")   //TO DO - Make this return timeSinceDown as an integer
                                        log.debug "sent still down message"
                                        state.sentPeriodMsg = now()
                                    }
                                    log.debug "not enough time to send periodic message"
                                }
                                log.info "poll already called pollVerify"
                			}
            			}
        		}
    		} catch (e) {
        		if (state.downHost == false) {
        			if (state.pollVerify == false) {
        				runIn(60*threshold, pollVerify)
            				state.pollVerify = true
            			}
            			log.info "poll catch request failed to ${state.website}, calling pollVerify with a ${threshold} minute threshold"
        		} else {
           			log.debug "catch periodicNotifications = ${periodicNotifications}"
                                if (periodicNotifications == "Yes") { 
                                	if (now()-state.sentPeriodMsg >= waitminutes * 60000) {
                                    	log.debug "catch long enough period, sending periodic message"
                                        def timeSinceDown = (now() - state.timeDown) / 60000    //converts the milliseconds elapsed between timeDown until now to minutes
                                		log.debug "catch timeSinceDown is ${timeSinceDown} minutes"
                                        send("${state.website} is still down, it's been down for ${timeSinceDown} minutes")  //TO DO - Make this return timeSinceDown as an integer
                                        log.debug "catch sent still down message"
                                        state.sentPeriodMsg = now()
                                    }
                                    log.debug "catch not enough time to send periodic message"
                                }
                    log.info "catch poll already called pollVerify"
        		}
    		}
    	}
}

def pollVerify() {
    def reqParams = [
		uri: "http://${state.website}"
	]
    	try {
        	httpGet(reqParams) { resp ->
            		if (resp.status == 200) {
                		state.downHost = false
                		state.pollVerify = false
                		turnOffHandler()
                		log.info "successful response from ${state.website}, false alarm avoided"
            		} else {
            			state.downHost = true
                		state.pollVerify = false
            			turnOnHandler()
                        state.timeDown = now()
                		log.info "pollVerify confirmed request failed to ${state.website}, turning on handlers, state.timeDown set to ${state.timeDown}"
            		}
        	}
    	} catch (e) {
        	state.downHost = true
        	state.pollVerify = false
        	turnOnHandler()
            state.timeDown = now()
        	log.info "catch pollVerify confirmed request failed to ${state.website}, missed turning on handlers, state.timeDown set to ${state.timeDown}"
    	}
}

def turnOnHandler() {
	log.debug "running turnOnHandler()"
    send("${state.website} is down")   //send notificiton that website is down
    state.websiteDownMsgSent = true
    state.sentPeriodMsg = now()  //this is also the "first" periodic notificaiton sent, so reset it's timer
    log.debug "website down message sent & state.websiteDownMsgSent set to true"
    if (lights) {
    		lights.each {
			if (it.hasCommand('setLevel')) {
				it.setLevel(level as Integer)
			} else {
				it.on()
			}
		}
    		lights?.on()
        	setColor()
        	setColorTemperature()
    		log.info "turning on lights"
    	}
	if (switches) {
    		switches.on()
    		log.info "turning on switches"
   	}
	if (alarms) {
    		alarms.each {
			if (it.hasCommand('both')) {
				it.both()
			} else if (it.hasCommand('siren')) {
				it.siren()
            		} else if (it.hasCommand('strobe')) {
            			it.strobe()
			}
		}
        	log.info "turning on siren(s)"
    	}
}

def turnOffHandler() {
	log.info "running turnOffHandler()"
	if (state.websiteDownMsgSent == true) {
    	send("${state.website} is back up")    //send notificiton that website is back up
        state.websiteDownMsgSent = false
    	log.info "website back up message sent &  state.websiteDownMsgSent set back to false"
    }
    if (lights) {
    		lights.off()
    		log.info "turning off light(s)"
    	}
    	if (switches) {
    		switches.off()
    		log.info "turning off switch(es)"
    	}
    	if (alarms) {
    		alarms.off()
		log.info "turning off siren(s)"
	}
}

private lightInputs() {
	input "lights", "capability.switch", title: "Control these lights", multiple: true, required: false, submitOnChange: true
}

private switchInputs() {
	input "switches", "capability.switch", title: "Control these switches", multiple: true, required: false, submitOnChange: true
}

private alarmInputs() {
	input "alarms", "capability.alarm", title: "Control these alarms", multiple: true, required: false, submitOnChange: true
}

private lightActionMap() {
	def map = [on: "Turn On", off: "Turn Off"]
	if (lights.find{it.hasCommand('setLevel')} != null) {
		map.level = "Turn On & Set Level"
	}
	if (lights.find{it.hasCommand('setColor')} != null) {
		map.color = "Turn On & Set Color"
	}
	map
}

private lightActionOptions() {
	lightActionMap().collect{[(it.key): it.value]}
}

private lightActionInputs() {
	if (lights) {
		def requiredInput = androidClient() || iosClient("1.7.0.RC1")
		input "action", "enum", title: "Perform this action", options: lightActionOptions(), required: requiredInput, submitOnChange: true
		if (action == "color") {
			input "color", "enum", title: "Color", required: false, multiple:false, options: [
				["Soft White":"Soft White - Default"],
				["White":"White - Concentrate"],
				["Daylight":"Daylight - Energize"],
				["Warm White":"Warm White - Relax"],
				"Red","Green","Blue","Yellow","Orange","Purple","Pink"]

		}
		if (action == "colorTemperature") {
			input "colorTemperature", "enum", title: "Color Temperature", options: [[2700: "Soft White (2700K)"], [3300: "White (3300K)"], [4100: "Moonlight (4100K)"], [5000: "Cool White (5000K)"], [6500: "Daylight (6500K)"]], defaultValue: "3300"
		}
		if (action == "level" || action == "color" || action == "colorTemperature") {
			input "level", "enum", title: "Dimmer Level", options: [[10:"10%"],[20:"20%"],[30:"30%"],[40:"40%"],[50:"50%"],[60:"60%"],[70:"70%"],[80:"80%"],[90:"90%"],[100:"100%"]], defaultValue: "80"
		}
	}
}

private switchActionMap() {
	def map = [on: "Turn On", off: "Turn Off"]
}

private switchActionOptions() {
	switchActionMap().collect{[(it.key): it.value]}
}

private switchActionInputs() {
	if (switches) {
		def requiredInput = androidClient() || iosClient("1.7.0.RC1")
		input "action", "enum", title: "Perform this action", options: switchActionOptions(), required: requiredInput, submitOnChange: true
	}
}

private alarmActionMap() {
	def map = [off: "Turn Off"]
	if (alarms.find{it.hasCommand('both')} != null) {
		map.both = "Turn On Siren & Strobe"
	}
    if (alarms.find{it.hasCommand('siren')} != null) {
		map.siren = "Turn On Siren"
	}
    if (alarms.find{it.hasCommand('strobe')} != null) {
		map.strobe = "Turn On Strobe"
	}
	map
}

private alarmActionOptions() {
	alarmActionMap().collect{[(it.key): it.value]}
}

private alarmActionInputs() {
	if (alarms) {
		def requiredInput = androidClient() || iosClient("1.7.0.RC1")
		input "action", "enum", title: "Perform this action", options: alarmActionOptions(), required: requiredInput, submitOnChange: true
	}
}

private setColor() {

	def hueColor = 0
	def saturation = 100

	switch(color) {
		case "White":
			hueColor = 52
			saturation = 19
			break;
		case "Daylight":
			hueColor = 53
			saturation = 91
			break;
		case "Soft White":
			hueColor = 23
			saturation = 56
			break;
		case "Warm White":
			hueColor = 20
			saturation = 80
			break;
		case "Blue":
			hueColor = 70
			break;
		case "Green":
			hueColor = 39
			break;
		case "Yellow":
			hueColor = 25
			break;
		case "Orange":
			hueColor = 10
			break;
		case "Purple":
			hueColor = 75
			break;
		case "Pink":
			hueColor = 83
			break;
		case "Red":
			hueColor = 100
			break;
	}

	def value = [switch: "on", hue: hueColor, saturation: saturation, level: level as Integer ?: 100]

	lights.each {
		if (it.hasCommand('setColor')) {
			it.setColor(value)
		}
		else if (it.hasCommand('setLevel')) {
			it.setLevel(level as Integer ?: 100)
		}
		else {
			it.on()
		}
	}
}

def setColorTemperature() {
	def tempValue = colorTemperature as Integer ?: 3300
	def levelValue = level as Integer ?: 100
	lights.each {
		if (it.hasCommand('setColorTemperature')) {
			it.setColorTemperature(tempValue)
			if (it.hasCommand('setLevel')) {
				it.setLevel(levelValue)
			}
			else {
				it.on()
			}
		}
		else if (it.hasCommand('setLevel')) {
			it.setLevel(levelValue)
		}
		else {
			it.on()
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