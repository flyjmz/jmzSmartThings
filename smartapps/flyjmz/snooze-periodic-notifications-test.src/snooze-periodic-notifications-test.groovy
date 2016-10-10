/*
Snooze Periodic Notifications Test
   
https://github.com/flyjmz/jmzSmartThings


   Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
   in compliance with the License. You may obtain a copy of the License at:
 
       http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
   for the specific language governing permissions and limitations under the License.
 
Version History:
	1.0 - 10Oct16, started
 
*/
 
definition(
    name: "Snooze Periodic Notifications Test",
    namespace: "flyjmz",
    author: "flyjmz230@gmail.com",
    description: "Test bed",
    category: "My Apps",
	iconUrl: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/phone2x.png",
	iconX2Url: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/phone2x.png"
)
preferences {
    page(name: "settings")
}

def settings() {
    dynamicPage(name: "settings", title: "", install: true, uninstall: true) {
        section("") {
		 	input "mySwitch", "capability.switch", title: "Monitor which switch?", required: true
            input "notificationSwitch", "capability.switch", title: "Select Virtual Snooze Switch", required: true
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
	subscribe(mySwitch, "switch", eventHandler)
}

def eventHandler(evt) {
	if (notificationSwitch == "on") {
    	sendMessage("${mySwitch.displayName} is now on. Snooze")    //To do: add a hyperlink in message.  Then user can click hyperlink, which would go to ifttt/smarttiles/?? to turn off a notifications virtual switch device (or actually, just turn the switch itself off too!)
	}
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