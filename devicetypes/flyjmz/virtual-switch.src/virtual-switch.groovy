/**
* Virtual Switch
*
* TO DO - Add GITHUB WEBSITE
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at:
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
* for the specific language governing permissions and limitations under the License.
*
* Version 1.0 - 3August2016 Initial Release
*
*/

metadata {
	definition (name: "Virtual Switch", namespace: "flyjmz", author: "flyjmz230@gmail.com") {
    	capability "Switch"
	}

	tiles {
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: false,  canChangeBackground: true) {
			state "off", label: 'Off', action: "switch.on", icon:"st.switch.switch.off", backgroundColor:"#ffffff"     //  , nextState: "on"
			state "on", label: 'On', action: "switch.off", icon:"st.switch.switch.on", backgroundColor:"#53a7c0"  			//  , nextState: "off"
		}
		main (["button"])
		details(["button"])
	}
}

def parse(String description) {
	def pair = description.split(":")
	createEvent(name: pair[0].trim(), value: pair[1].trim())
}

def on() {
	sendEvent(name: "switch", value: "on")
}

def off() {
	sendEvent(name: "switch", value: "off")
}