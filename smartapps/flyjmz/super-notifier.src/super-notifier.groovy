/*
Super Notifier
   
Code: https://github.com/flyjmz/jmzSmartThings
Forum: https://community.smartthings.com/t/release-super-notifier-all-your-alerts-in-one-place/597


Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
   in compliance with the License. You may obtain a copy of the License at:
 
       http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
   for the specific language governing permissions and limitations under the License.
 
   
Credits, based on work from:  
  "Notify Me When" by SmartThings dated 2013-03-20
  "Turn off after some minutes with options" by Bruce Ravenel dated 2015
  "Left It Open" by SmartThings dated 2013-05-09
  "Door Knocker" by brian@bevey.org dated 9/10/13
  
Version History:
  	1.0 - 5Sep2016, Initial Commit
    1.1 - 10Oct2016, public release
    1.2 - 1Feb2018, added update notifications and debug logging option
    1.3 - 17Apr2017, updated with Door Knocker monitoring
    1.4 - 24Jul2018, added contact book like feature to ease SmartThings' depricating the real contact book

*/

def appVersion() {"1.4"}
 
definition(
  name: "Super Notifier",
  namespace: "flyjmz",
  author: "flyjmz230@gmail.com",
  description: "One stop shop for alerts",
  category: "My Apps",
  iconUrl: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/phone2x.png",
  iconX2Url: "https://github.com/flyjmz/jmzSmartThings/raw/master/resources/phone2x.png",
    singleInstance: true
)

preferences {
    page(name:"superNotifierSetup")
}


def superNotifierSetup() {
    dynamicPage(name: "superNotifierSetup", title: "Super Notifier", install: true, uninstall: true, submitOnChange: true) {
        section("Alerts") {
            app(name: "Instant Alert", appName: "Super Notifier - Instant Alert", namespace: "flyjmz", title: "Add instant alert", description: "Add an instant alert to notify as soon as something happens", multiple: true)
            app(name: "Delayed Alert", appName: "Super Notifier - Delayed Alert", namespace: "flyjmz", title: "Add delayed alert", description: "Add a delayed alert to notify when something has been left open/closed or on/off",multiple: true)
        }

        section("Notifications", hidden: false, hideable: true) {
            def SMSContactsSplit
            if (!location.contactBookEnabled) {
                paragraph "Contact Book Workaround:  Enter phone numbers if you are using SMS notifications. (Push notifications are selected elsewhere and effect all users of this Location)"
                input "SMSContacts", "string", title: "Enter up to 10 Phone Numbers: (include country code) Separate entries with a semi-colon (;). Do not use spaces or special characters.", required: false, submitOnChange: true
                if (SMSContacts != null) {
                    paragraph "Name each contact:"
                    SMSContactsSplit = SMSContacts.split(";")
                    for (int i = 0; i < SMSContactsSplit.size(); i++) {
                        input "contact-${i}", "string", title: "Contact-${i} '${SMSContactsSplit[i]}' Name:", required: true
                    }
                    paragraph "NOTE: If you add/remove/reorder users, you'll need to update the Notification Settings in each instant/delayed alert app. You do not need to do anything if you only change the phone number and don't want to change anything else." 
                }
            }
        }

        section("Advanced", hidden: true, hideable: true){
            paragraph "You can turn on debug logging, viewed in Live Logging on the API website."
            def loggingOn = false
            input "loggingOn", "bool", title: "Debug Logging On?"
            paragraph "New software version notifications are sent automatically, but can be disabled."
            input "updateAlertsOff", "bool", title: "Disable software update alerts?", required:false
            if (updateAlertsOff) {
                input("recipients", "contact", title: "Send update notifications to") {
                    input "wantsPush", "bool", title: "Send update Push Notification? (pushes to all this location's users)", required: false
                    paragraph "If you want SMS Notifications, enter phone numbers including the country code (1 for USA), otherwise leave blank. Separate multiple numbers with a semi-colon (;). Only enter the numbers, no spaces or special characters."
                    input "phoneNumbers", "string", title: "Enter Phone Numbers for SMS Notifications:", required: false
                }
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
    unschedule()
	initialize()
}

def initialize() {
    schedule(now(), checkForUpdates)
   	checkForUpdates()
}

def checkForUpdates() {
    checkForParentUpdates()
    checkForChildUpdates()
    log.info "Checking for Super Notifier updates"
}

def checkForParentUpdates() {
    def installedVersion = appVersion()
    def name = "Super Notifier"
    def website = "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/smartapps/flyjmz/super-notifier.src/version.txt"
    checkUpdates(name, installedVersion, website)
}

def checkForChildUpdates() {
    def childApps = getChildApps()
    def installedVersion = "0.0"
    def installed = false
    if (childApps[0] != null) {
        def instantcheckdone = false
        def delayedcheckdone = false
        for (int i = 0; i < childApps.size(); i++) {
            if (instantcheckdone && delayedcheckdone) {i=1000}
            if (!instantcheckdone && childApps[i].getName() == "Super Notifier - Instant Alert") {
                installedVersion = childApps[i].appVersion()
                def name = "Super Notifier - Instant Alert"
                def website = "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/smartapps/flyjmz/super-notifier-instant-alert.src/version.txt"
                checkUpdates(name, installedVersion, website)
                if (loggingOn) log.debug "Instant child app installedVersion is $installedVersion"
                instantcheckdone = true
            }
            if (!delayedcheckdone && childApps[i].getName() == "Super Notifier - Delayed Alert") {
                installedVersion = childApps[i].appVersion()
                def name = "Super Notifier - Delayed Alert"
                def website = "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/smartapps/flyjmz/super-notifier-delayed-alert.src/version.txt"
                checkUpdates(name, installedVersion, website)
                if (loggingOn) log.debug "Delayed child app installedVersion is $installedVersion"
                delayedcheckdone = true
            }
        }

    } else {} //no childs installed
}

def checkUpdates(name, installedVersion, website) {
    if (loggingOn) log.debug "${name} running checkForUpdates() with an installedVersion of $installedVersion, at website $website"
    def publishedVersion = "0.0"
    try {
        httpGet([uri: website, contentType: "text/plain; charset=UTF-8"]) { resp ->
            if(resp.data) {
                publishedVersion= resp?.data?.text.toString()   //For some reason I couldn't add .trim() to this line, or make another variable and add it there.  The rest of the code would run, but notifications failed. Just having .trim() in the notification below failed a bunch, then started working again...
                if (loggingOn) log.debug "publishedVersion found is $publishedVersion"
                return publishedVersion
            } else  log.error "checkUpdates retrievePublishedVersion response from httpGet was ${resp} for ${name}"
        }
    }
    catch (Exception e) {
        log.error "checkForUpdates() couldn't get the current ${name} code verison. Error:$e"
        publishedVersion = "0.0"
    }
    if (loggingOn) log.debug "${name} publishedVersion from web is ${publishedVersion}, installedVersion is ${installedVersion}"
    def instVerNum = 0          
    def webVerNum = 0
    if (publishedVersion && installedVersion) {  //make sure no null
        def instVerMap = installedVersion.tokenize('.')  //makes a map of each level of the version
        def webVerMap = publishedVersion.tokenize('.')
        def instVerMapSize = instVerMap.size()
        def webVerMapSize = webVerMap.size()
        if (instVerMapSize > webVerMapSize) {   //handles mismatched sizes (e.g. they have v1.3 installed but v2 is out and didn't write it as v2.0)
            def sizeMismatch = instVerMapSize - webVerMapSize
            for (int i = 0; i < sizeMismatch; i++) {
                def newMapPosition = webVerMapSize + i  //maps' first postion is [0], but size would count it, so the next map position is i in the loop because i starts with 0
                webVerMap[newMapPosition] = 0  //just make it a zero, the actual goal is to increase the size of the map
            }
        } else if (instVerMapSize < webVerMapSize) {
            def sizeMismatch = webVerMapSize - instVerMapSize
            for (int i = 0; i < sizeMismatch; i++) {
                def newMapPosition = instVerMapSize + i  
                instVerMap[newMapPosition] = 0
            }
        }
        instVerMapSize = instVerMap.size() //update the sizes incase we just changed the maps
        webVerMapSize = webVerMap.size()
        if (loggingOn) log.debug "${name} instVerMapSize is $instVerMapSize and the map is $instVerMap and webVerMapSize is $webVerMapSize and the map is $webVerMap"
        for (int i = 0; i < instVerMapSize; i++) {
            instVerNum = instVerNum + instVerMap[i]?.toInteger() *  Math.pow(10, (instVerMapSize - i))  //turns the version segments into one big additive number: 1.2.3 becomes 100+20+3 = 123
        }
        for (int i = 0; i < webVerMapSize; i++) {
            webVerNum = webVerNum + webVerMap[i]?.toInteger() * Math.pow(10, (webVerMapSize - i)) 
        }
        if (loggingOn) log.debug "${name} publishedVersion is now ${webVerNum}, installedVersion is now ${instVerNum}"
        if (webVerNum > instVerNum) {
            def msg = "${name} Update Available. Update in IDE. v${installedVersion} installed, v${publishedVersion.trim()} available."
            if (updateAlertsOff) sendNotificationEvent(msg)  //Message is only displayed in Smartthings app notifications feed
            if (!updateAlertsOff) send(msg) //Message sent to push/SMS per user, plus the Smartthings app notifications feed
        } else if (webVerNum == instVerNum) {
            log.info "${name} is current."
        } else if (webVerNum < instVerNum) {
            log.error "Your installed version of ${name} seems to be higher than the published version."
        }
    } else if (!publishedVersion) {log.error "Cannot get published app version for ${name} from the web."}
}

private send(msg) {  
	//First try to use Contact Book (Depricated 30July2018)
    if (location.contactBookEnabled) {
        if (loggingOn) log.debug("sending '$msg' notification to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    //Otherwise use old school Push/SMS notifcations
    else {
        if (loggingOn) log.debug("sending message to app notications tab: '$msg'")
        sendNotificationEvent(msg)  //First send to app notifications (because of the loop we're about to do, we need to use this version to avoid multiple instances) 
        if (wantsPush) {
            sendPushMessage(msg)  //Second, send the push notification if user wanted it
            if (loggingOn) log.debug("sending push message")
        }

        if (phoneNumbers) {	//Third, send SMS messages if desired
            if (phoneNumbers.indexOf(";") > 1) {	//Code block for multiple phone numbers
                def phones = phoneNumbers.split(";")
                for (int i = 0; i < phones.size(); i++) {
                    if (loggingOn) log.debug("sending SMS to ${phones[i]}")
                    sendSmsMessage(phones[i], msg)
                }
            } else {	//Code block for single phone number
                if (loggingOn) log.debug("sending SMS to ${phoneNumbers}")
                sendSmsMessage(phoneNumbers, msg)
            }
        }
    }
}