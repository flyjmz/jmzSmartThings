/**
 *  Blue Iris Integration
 *
 *  Copyright 2014 Tony Gutierrez
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
 *  
 *
 *  Author: pursual
 *  Date: 2016-03-04
 *
 *  Sets Blue Iris to a profile that corresponds to the Smartthings "mode". 
 *
 *  From Github: https://github.com/pursual/SmartThingsPublic/blob/master/smartapps/pursual/blue-iris-profile-integration.src/blue-iris-profile-integration.groovy
 *  #1.65   Mar 25, 2016
 *  #1.66   Jul 4, 2016 flyjmz fixed typo in line 46, changed password input type to password (now encrypted within the app and cloud)
 *  #1.67   Jul 30, 2016 flyjmz changed icons to Blue Iris icon
 *  #1.68   Aug 31, 2016 flyjmz added push/sms notification option for errors
 *	#1.69	Sep 4, 2016 flyjmz add option to make a hold or temp change to profile, also cleaned up notifications and other code
 */

definition(
    name: "Blue Iris Profile Integration",
    namespace: "pursual",
    author: "Tony Gutierrez",
    description: "Integration with Blue Iris JSON Interface for purposes of toggling recording/alert profiles.",
    category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo.png",
	iconX2Url: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo%402x.png")

preferences {
    page(name:"selectModes")
    page(name:"BISettings")
}

def selectModes() {  
  dynamicPage(name: "selectModes", title: "Mode and Profile Matching", nextPage:"BISettings", uninstall:true) {    
    section("") {
        paragraph "Numbers 1-7 correspond to Blue Iris profile numbers. To ignore a mode leave it blank. A profile of 0 sets Blue Iris to 'inactive.'"
        location.modes.each { mode ->
            def modeId = mode.id.toString()  
            input "mode-${modeId}", "number", title: "Mode ${mode}", required: false
        }
    }
    section ("Make temporary or hold profile changes?") {
    	paragraph "Temporary changes will only be in effect for the 'Temp Time' duration set for each profile in Blue Iris Settings > Profiles. At the end of that time, Blue Iris will change profiles according to your schedule.  Hold changes will remain until a new change is made (e.g. you change it or this app does).  Note: if Blue Iris restarts with a temp change, it will start in a profile set by your schedule. A hold change will remain even after a restart."
    	input "holdTemp", "enum", title: "", required: true, options: ["Temporary","Hold"], default: "Hold"
    }
  }
}

def BISettings() {
    dynamicPage(name:"BISettings", "title":"", uninstall:true, install:true) {
        section("Blue Iris Login Info") {
            input "host", "string", title: "BI External Webserver Host (include http://)", required:true
            input "port", "number", title: "BI External Webserver Port (81?)", required:true, default:81
            input "username", "string", title: "BI Username", required: true
            input "password", "password", title: "BI Password", required: true
            paragraph "Note: BI only allows Admin Users to toggle profiles.  Also, you must use the external address of your server (the app accesses your BI server from the Smartthings cloud).  Use either your external IP or a DDNS service (like www.noip.com).  If using https, the certificate cannot be self-signed.  You can create a free Let's Encrypt certificate at www.zerossl.com"
        }
        section("Notification Type") {
			paragraph "You can choose to receive push notifications or a SMS when there is an error.  Regardless, you will always receive status notifications within the SmartThings Notifications tab."
            input "receiveAlerts", "enum", title: "Receive Notifications?", options: ["Yes", "No"], required: true, submitOnChange: true
            if (receiveAlerts == "Yes") {
            	input("recipients", "contact", title: "Send notifications to") {
					input "phone1", "phone", title: "Phone Number for Text Message: (leave blank for no SMS)", required: false
					paragraph "If outside the US please make sure to enter the proper country code"
				}
            }
		}
    }
}

def installed() {
    //log.debug "Installed with settings: ${settings}"
    unsubscribe()
    subscribe(location, modeChange)
}

def updated() {
    //log.debug "Updated with settings: ${settings}"
    unsubscribe()
    subscribe(location, modeChange)    
}

def modeChange(evt)
{
    if (evt.name != "mode") {return;}
    log.debug "BI_modeChange detected. " + evt.value
    def checkMode = ""
    
    location.modes.each { mode ->
        if (mode.name == evt.value){
            checkMode = "mode-" + mode.id
            log.debug "BI_modeChange matched to " + mode.name
        }
    }
    
    if (checkMode != "" && settings[checkMode]){
        log.debug "BI_Found profile " + settings[checkMode]
        takeAction(settings[checkMode].toInteger());
    }
}

def takeAction(profile) {
    def errorMsg = "Could not adjust Blue Iris Profile"
if (holdTemp == "Temporary") {
    try {
        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login"]) { response ->
            //log.debug response.data

            if (response.data.result == "fail") {
               //log.debug "BI_Inside initial call fail, proceeding to login"
               def session = response.data.session
               def hash = username + ":" + response.data.session + ":" + password
               hash = hash.encodeAsMD5()

               httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login","session":session,"response":hash]) { response2 ->
                    if (response2.data.result == "success") {
                        def BIprofileNames = response2.data.data.profiles;
                        //log.debug ("BI_Logged In")
                        //log.debug response2.data
                        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","session":session]) { response3 ->
                            //log.debug ("BI_Retrieved Status");
                            //log.debug response3.data
                            if (response3.data.result == "success"){
                                if (response3.data.data.profile != profile){
                                    httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","profile":profile,"session":session]) { response4 ->
                                        //log.debug response4.data
                                        if (response4.data.result == "success") {
                                            if (response4.data.data.profile.toInteger() == profile.toInteger()) {
                                                //log.debug ("Blue Iris to profile ${profileName(BIprofileNames,profile)}!")
                                                sendNotificationEvent("Blue Iris temp changed to profile ${profileName(BIprofileNames,profile)}!")
                                            } else {
                                                //log.debug ("Blue Iris ended up on profile ${profileName(BIprofileNames,response4.data.data.profile)}? Temp change to ${profileName(BIprofileNames,profile)}. Check your user permissions.")
                                                sendNotificationEvent("Blue Iris Integration failed to change Profiles, it is in ${profileName(BIprofileNames,response4.data.data.profile)}? Temp change to ${profileName(BIprofileNames,profile)} failed. Check your user permissions.")
                                            	send("Blue Iris failed to change profiles, it is in '${profileName(BIprofileNames,response4.data.data.profile)}'. Temp change to '${profileName(BIprofileNames,profile)}' failed.")
                                            }
                                            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"logout","session":session]) { response5 ->
                                                //log.debug response5.data
                                                //log.debug "Logged out"
                                            }
                                        } else {
                                            //log.debug "BI_FAILURE"
                                            //log.debug(response4.data.data.reason)
                                            sendNotificationEvent(errorMsg)
                                        }
                                    }
                                } else {
                                    //log.debug ("Blue Iris is already at profile ${profileName(BIprofileNames,profile)}.")
                                    sendNotificationEvent("Blue Iris is already in profile ${profileName(BIprofileNames,profile)}.")
                                }
                            } else {
                                //log.debug "BI_FAILURE"
                                //log.debug(response3.data.data.reason)
                                sendNotificationEvent(errorMsg)
                                send(errorMsg)
                            }
                        }
                    } else {
                        //log.debug "BI_FAILURE"
                        //log.debug(response2.data.data.reason)
                        sendNotificationEvent(errorMsg)
                        send(errorMsg)
                    }
                }
            } else {
                //log.debug "FAILURE"
                //log.debug(response.data.data.reason)
                sendNotificationEvent(errorMsg)
                send(errorMsg)
            }
        }
    } catch(Exception e) {
        //log.debug(e)
        sendNotificationEvent(errorMsg);
        send(errorMsg)
    }
} else {
    try {
        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login"]) { response ->
            //log.debug response.data

            if (response.data.result == "fail") {
               //log.debug "BI_Inside initial call fail, proceeding to login"
               def session = response.data.session
               def hash = username + ":" + response.data.session + ":" + password
               hash = hash.encodeAsMD5()

               httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"login","session":session,"response":hash]) { response2 ->
                    if (response2.data.result == "success") {
                        def BIprofileNames = response2.data.data.profiles;
                        //log.debug ("BI_Logged In")
                        //log.debug response2.data
                        httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","session":session]) { response3 ->
                            //log.debug ("BI_Retrieved Status");
                            //log.debug response3.data
                            if (response3.data.result == "success"){
                                if (response3.data.data.profile != profile){
                                    httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","profile":profile,"session":session]) { response4 ->
                                        //log.debug response4.data
                                        if (response4.data.result == "success") {
                                        	//log.debug "Set profile to ${profileName(BIprofileNames,profile)} via temp change, trying to set via hold"
                                            if (response4.data.data.profile.toInteger() == profile.toInteger()) {
                                                httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"status","profile":profile,"session":session]) { response5 ->
                                                	//log.debug response5.data
                                                	if (response5.data.result == "success") {
                                                		//log.debug ("Set profile to ${profileName(BIprofileNames,profile)} with a hold change!")
                                                		sendNotificationEvent("Blue Iris is holding profile ${profileName(BIprofileNames,profile)}!")
                                            		} else {
                                                		//log.debug ("Blue Iris failed to hold profile, it is in ${profileName(BIprofileNames,response5.data.data.profile)}? but is only temporary. Check your user permissions.")
                                                		sendNotificationEvent("Blue Iris ended up on profile ${profileName(BIprofileNames,response5.data.data.profile)}? I tried to hold ${profileName(BIprofileNames,profile)}. Check your user permissions.");
                                            			send("Blue Iris failed to hold profile, it is in '${profileName(BIprofileNames,response5.data.data.profile)}' but is only temporary.")
                                                	}
                                               }
                                            } else {
                                                //log.debug ("Blue Iris ended up on profile ${profileName(BIprofileNames,response4.data.data.profile)}? Attempt to set ${profileName(BIprofileNames,profile)} failed, also unable to attempt hold. Check your user permissions.")
                                                sendNotificationEvent("Blue Iris ended up on profile ${profileName(BIprofileNames,response4.data.data.profile)}? Attempt to set ${profileName(BIprofileNames,profile)} failed, also unable to attempt hold. Check your user permissions.");
                                            	send("Blue Iris failed to change Profiles, it is in '${profileName(BIprofileNames,response4.data.data.profile)}' but should have been switched to '${profileName(BIprofileNames,profile)}.'")
                                            }
                                            httpPostJson(uri: host + ':' + port, path: '/json',  body: ["cmd":"logout","session":session]) { response6 ->
                                                //log.debug response6.data
                                                //log.debug "Logged out"
                                            }
                                        } else {
                                            //log.debug "BI_FAILURE"
                                            //log.debug(response4.data.data.reason)
                                            sendNotificationEvent(errorMsg)
                                        }
                                    }
                                } else {
                                    //log.debug ("Blue Iris is already at profile ${profileName(BIprofileNames,profile)}.")
                                    sendNotificationEvent("Blue Iris is already in profile ${profileName(BIprofileNames,profile)}.")
                                    }
                            } else {
                                //log.debug "BI_FAILURE"
                                //log.debug(response3.data.data.reason)
                                sendNotificationEvent(errorMsg)
                                send(errorMsg)
                            }
                        }
                    } else {
                        //log.debug "BI_FAILURE"
                        //log.debug(response2.data.data.reason)
                        sendNotificationEvent(errorMsg)
                        send(errorMsg)
                    }
                }
            } else {
                //log.debug "FAILURE"
                //log.debug(response.data.data.reason)
                sendNotificationEvent(errorMsg)
                send(errorMsg)
            }
        }
    } catch(Exception e) {
        //log.debug(e)
        sendNotificationEvent(errorMsg);
        send(errorMsg)
    }
}
}

def profileName(names, num) {
    if (names[num.toInteger()]) {
        names[num.toInteger()] + " (#${num})"
    } else {
        '#' + num
    }
}

private send(msg) {
    if (receiveAlerts == "Yes") {
    	if (location.contactBookEnabled) {
        	log.debug("sending notifications to: ${recipients?.size()}")
        	sendNotificationToContacts(msg, recipients)
    	}
    	else {
            log.debug("sending push message")
            sendPush(msg)
        	if (phone1) {
            	log.debug("sending text message")
            	sendSms(phone1, msg)
        	}
    	}
	}
	log.debug msg
}