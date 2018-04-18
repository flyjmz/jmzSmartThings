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

*/

def appVersion() {"1.3"}
 
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
        section("") {
            app(name: "Instant Alert", appName: "Super Notifier - Instant Alert", namespace: "flyjmz", title: "Add instant alert", description: "Add an instant alert to notify as soon as something happens", multiple: true)
            app(name: "Delayed Alert", appName: "Super Notifier - Delayed Alert", namespace: "flyjmz", title: "Add delayed alert", description: "Add a delayed alert to notify when something has been left open/closed or on/off",multiple: true)
        }
        section("") {
          input "loggingOn", "bool", title: "Turn on Debug Logging?", required: false
            input "updateAlertsOff", "bool", title: "Disable software update alerts?", required:false, submitOnChange: true
            if (!updateAlertsOff) {
                input("recipients", "contact", title: "Send notifications to") {
                    input "pushAndPhone", "enum", title: "Also send SMS? (optional, it will always send push)", required: false, options: ["Yes", "No"]      
                    input "phone", "phone", title: "Phone Number (only for SMS)", required: false
                    paragraph "If outside the US please make sure to enter the proper country code"
                }
            }
        }
    }
}

def installed() {
    schedule(now(), checkForUpdates)
    checkForUpdates()
}

def updated() {
    unsubscribe()
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
    if (location.contactBookEnabled) {
        if (loggingOn) log.debug("sending notifications to: ${recipients?.size()}")
        sendNotificationToContacts(msg, recipients)
    }
    else {
        Map options = [:]
        if (phone) {
            options.phone = phone
            if (loggingOn) log.debug 'sending SMS'
        } else if (pushAndPhone == 'Yes') {
            options.method = 'both'
            options.phone = phone
        } else options.method = 'push'
        sendNotification(msg, options)
    }
    if (loggingOn) log.debug msg
}