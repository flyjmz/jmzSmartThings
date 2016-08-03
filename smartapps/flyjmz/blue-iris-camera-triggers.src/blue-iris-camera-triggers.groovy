/**
 *  Blue Iris Camera Triggers
 *
 *  Copyright 2016 flyjmz, based on work by Tony Gutierrez in "Blue Iris Profile Integration"
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
 *  Version 1.0 - 30July2016 	Initial release
 *	Version 1.1 - 3August2016 	Cleaned up Code
 */

definition(
	name: "Blue Iris Camera Triggers",
	namespace: "flyjmz",
	author: "flyjmz",
    description: "Parent App that sses Smartthings motion or contact sensors and/or switches to trigger camera recording in Blue Iris via JSON Interface.",
	category: "Safety & Security",
    iconUrl: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo.png",
    iconX2Url: "https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BlueIris_logo%402x.png",
	singleInstance: true
)

preferences {
    page(name:"BITriggers")
}

def BITriggers() {
	dynamicPage(name:"BITriggers", title: "Triggers", install: true, uninstall: true, submitOnChange: true) {
    	section("") {
            app(name: "Blue Iris Trigger", appName: "Blue Iris Trigger", namespace: "flyjmz", title: "Add trigger", multiple: true)
		}
        section("Blue Iris Server Login Settings") {
            paragraph "Blue Iris only allows Admin Users to toggle profiles.  Also, if using https://, the SSL certificate must be from a Certificate Authority (CA), it cannot be self-signed."
            input "host", "string", title: "BI Webserver Host (include http://)", required:true
            input "port", "number", title: "BI Webserver Port (e.g. 81)", required:true
            input "username", "string", title: "BI Username", required: true
            input "password", "password", title: "BI Password", required: true
		}
    }
}

def installed() {
}

def updated() {
	unsubscribe()
}
