/**
 *  SmartPing
 *
 *  Copyright 2016 Jason Botello
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
	name: "SmartPing",
	namespace: "jasonbio/smartping",
	author: "jasonbio",
	description: "Monitor your website uptime and trigger SmartThings automations if it goes down.",
	category: "SmartThings Labs",
	iconUrl: "https://raw.githubusercontent.com/jasonbio/icons/master/smartping.png",
	iconX2Url: "https://raw.githubusercontent.com/jasonbio/icons/master/smartping@2x.png",
	iconX3Url: "https://raw.githubusercontent.com/jasonbio/icons/master/smartping@3x.png",
	singleInstance: true
)

preferences {
	page(name: "setup", title: "SmartPing", install: false, uninstall: true, submitOnChange: true) {
    		section("") {
            		app(name: "SmartPing", appName: "SmartPing Automation", namespace: "jasonbio/smartping", title: "Add website", multiple: true)
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
    // placeholder
}