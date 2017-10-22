/*
Blue Iris Camera Device Type Handler

This is the Camera device for Blue Iris Software, and must be used with the BI Fusion smartapp (see below).  Cannot funciton on its own.

Copyright 2017 FLYJMZ (flyjmz230@gmail.com)

Smartthings Community Thread:   
	//todo - update when moved to a new thread
Github: 
	https://github.com/flyjmz/jmzSmartThings/tree/master/devicetypes/flyjmz

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
for the specific language governing permissions and limitations under the License.

Version History:
v1.0 xxOct17	Initial commit

*/

def appVersion() {"2.6"}  //todo-add the rest

metadata {
	definition (name: "Blue Iris Camera", namespace: "flyjmz", author: "flyjmz230@gmail.com") {
		capability "Motion Sensor"  //To treat cameras as a motion sensor for other apps (e.g. BI camera senses motion, setting this device to active so an alarm can subscribe to it and go off
		capability "Refresh"
		capability "Switch"  //To trigger camera recording for other smartapps that may not accept momentary
        capability "Momentary" //To trigger camera recording w/momentary on

		attribute "cameraShortName", "string"
        
        command "active"
		command "inactive"
        command "on"
        command "off"
        
        /*		todo - for image capture if we get there, check out the template smartsense camera dth
        capability "Image Capture"
        attributes "Image", "string"
        command "take"
        */
	}


	simulator {
		// TODO: define status and reply messages here
	}

    tiles (scale: 2) {
        valueTile("motion", "device.motion", width: 4, height: 2, canChangeIcon: false, decoration: "flat", canChangeBackground: true) {
        	state "inactive", label: 'No Motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
            state "active", label: 'Motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
        }
        standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: false, decoration: "flat", canChangeBackground: true) {
            state "off", label: 'Record', action: "switch.on", icon: "st.switch.switch.off", backgroundColor: "#ffffff"
            state "on", label: 'Recording', icon: "st.switch.switch.on", backgroundColor: "#53a7c0"  //no action because you can't untrigger a camera
        }
        main (["button"])
        details(["button"])
    }
}

// parse events into attributes
def parse(String description) {  //shouldn't need any parse, because it's all to/from server device then service manager, then to here
	log.debug "Parsing '${description}'"
	// TODO: handle 'motion' attribute
	// TODO: handle 'switch' attribute
	// TODO: handle 'Blue Iris Camera' attribute

}

// handle commands
def refresh() {
	log.debug "Executing 'refresh'"
	// TODO: handle 'refresh' command
}

def on() {
	log.debug "Executing 'on'"
    sendEvent(name: "switch", value: "on")
    runIn(10,off)
    // TODO: send camera trigger code to Fusion to server device to BI
}

def off() {
	log.debug "Executing 'off'"
    sendEvent(name: "switch", value: "off")
}

def active() {  //BI Camera senses motion
	//sendEvent(name: "motion", value: "active") //todo- this would be done by BI Fusion from BI server:  child.sendEvent(name: "motion", value: "active")
}

def inactive() {  //BI Camera no longer senses motion
    //sendEvent(name: "motion", value: "inactive")  //same as active
}

def push() {
	log.debug "Executing 'push'"
	on()
}