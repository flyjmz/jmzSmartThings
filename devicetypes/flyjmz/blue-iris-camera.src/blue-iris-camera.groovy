/*
Blue Iris Camera Device Type Handler

Copyright 2017 FLYJMZ (flyjmz230@gmail.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
for the specific language governing permissions and limitations under the License.
*/


//////////////////////////////////////////////////////////////////////////////////////////////
///										App Info											//
//////////////////////////////////////////////////////////////////////////////////////////////
/*
This is the Camera device for Blue Iris Software, and must be used with the BI Fusion smartapp and Blue Iris Server DTH (see below).  
It cannot function on its own.

SmartThings Community Thread:   
		https://community.smartthings.com/t/release-bi-fusion-v3-0-adds-blue-iris-device-type-handler-blue-iris-camera-dth-motion-sensing/103032

Github Code: 
		https://github.com/flyjmz/jmzSmartThings/tree/master/devicetypes/flyjmz/blue-iris-camera.src

Version History:
v1.0 26Oct17	Initial commit

To Do:
-Video stream and image capture
*/

def appVersion() {"1.0"}

metadata {
    definition (name: "Blue Iris Camera", namespace: "flyjmz", author: "flyjmz230@gmail.com") {
        capability "Motion Sensor"  //To treat cameras as a motion sensor for other apps (e.g. BI camera senses motion, setting this device to active so an alarm can subscribe to it and go off
        capability "Switch"  //To trigger camera recording for other smartapps that may not accept momentary
        capability "Momentary" //To trigger camera recording w/momentary on
        capability "Image Capture"
        attribute "cameraShortName", "string"
        attribute "errorMessage", "String"
        attribute "Image", "string"
        command "active"
        command "inactive"
        command "on"
        command "off"
        command "take"
    }


    simulator {
    }

    tiles (scale: 2) {
        standardTile("motion", "device.motion", width: 4, height: 2, canChangeIcon: false, canChangeBackground: true) {
            state "inactive", label: 'No Motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
            state "active", label: 'Motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
        }
        standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: false, canChangeBackground: true) {
            state "off", label: 'Record', action: "switch.on", icon: "st.switch.switch.off", backgroundColor: "#ffffff"
            state "on", label: 'Recording', icon: "st.switch.switch.on", backgroundColor: "#53a7c0"  //no action because you can't untrigger a camera
        }
        main (["motion"])
        details(["motion","button"])
    }

    preferences {
        section("Device Settings"){	//Can't have paragraphs in a DTH, but I also need to warn not to change the name here.
            //paragraph "WARNING: DO NOT CHANGE SETTINGS HERE (except to uninstall the device)."
            //paragraph "To change settings, go to the BI Fusion Smartapp."
            input "nothing", "text", title: "WARNING: DO NOT CHANGE SETTINGS HERE (except to uninstall the device)", description: "Change settings in BI Fusion.", required: false, displayDuringSetup: false 
        }
    }
}

def parse(String description) {  //Don't need to parse anything because it's all to/from server device then service manager app.
	log.debug "Parsing '${description}'"
}

def on() {   //Trigger to start recording with BI Camera
	log.info "Executing 'on'"
    sendEvent(name: "switch", value: "on", descriptionText: "Recording Triggered", displayed: true)
    runIn(10,off)
}

def off() {  //Can't actually turn off recording, the trigger is for a defined period in Blue Iris Settings for each camera and profile, this just puts the tile back to normal.
	log.info "Executing 'off'"
    sendEvent(name: "switch", value: "off", descriptionText: "Recording Trigger Ended", displayed: true)
}

def active() {  //BI Camera senses motion
	log.info "Motion 'active'"
	sendEvent(name: "motion", value: "active", descriptionText: "Camera Motion Active", displayed: true)
}

def inactive() {  //BI Camera no longer senses motion
	log.info "Motion 'inactive'"
    sendEvent(name: "motion", value: "inactive", descriptionText: "Camera Motion Inactive", displayed: true)
}

def push() {
	log.info "Executing 'push'"
	on()
}

def take() {
	log.info "Executing 'take'"
    //todo - add image capture
}