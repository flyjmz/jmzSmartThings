/**
* 
*	File: ZP_Pushover_device.groovy
*	Last Modified: 2016-01-04 22:29:15
*
*  Zachary Priddy
*	 https://zpriddy.com
*  me@zpriddy.com
*
*  Copyright 2015 Zachary Priddy
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
*/

preferences {
  input("apiKey", "text", title: "API Key", description: "Pushover API Key")
  input("userKey", "text", title: "User Key", description: "Pushover User Key")
  input("deviceName", "text", title: "Device Name", description: "Pusherover Device Name")
  input("testMessageText", "text", title: "Test Message Text", description: "Test Message Text", defaultValue: "This is a test message from SmartThings!")
  input("testMessagePriority", "enum", title: "Test Message Priority", options: ["Low", "Normal", "High", "Emergency"], defaultValue:"Normal")
}

metadata {
  definition (name: "ZP Pushover Device", namespace: "zpriddy", author: "zpriddy") {
    capability "Notification"

    command "sendMessage"
    command "sendTestMessage"

  }

  simulator {
    //This is left blank for now
  }

  tiles {
    standardTile("sendTestMessage", "device.sendTestMessage", inactiveLabel: false, decoration: "flat", width: 2, height: 2)
    {
      state "sendTestMessage", label: '', action: "sendTestMessage", icon: "st.Kids.kids8"
    }
    main(["sendTestMessage"])
    details(["sendTestMessage"])
  }

}

def sendTestMessage() {
  log.debug "Send Test Message"
  sendMessage("${testMessageText}", "${testMessagePriority}")
}

def sendMessage(message, priority=0) {
  log.debug "Sending Message: ${message} Priority: ${priority}"

  // Define the initial postBody keys and values for all messages
  def postBody = [
    token: "$apiKey",
    user: "$userKey",
    message: "${message}",
    priority: 0
  ]

  switch ( priority ) {
    case "Low":
      postBody['priority'] = -1
      break

    case "High":
      postBody['priority'] = 1
      break

    case "Emergency":
      postBody['priority'] = 2
      postBody['retry'] = "60"
      postBody['expire'] = "3600"
      break
    }

  // We only have to define the device if we are sending to a single device
  if (deviceName)
  {
    log.debug "Sending Pushover to Device: $deviceName"
    postBody['device'] = "$deviceName"
  }
  else
  {
    log.debug "Sending Pushover to All Devices"
  }

  // Prepare the package to be sent
  def params = [
    uri: "https://api.pushover.net/1/messages.json",
    body: postBody
  ]

  log.debug postBody

  if ((apiKey =~ /[A-Za-z0-9]{30}/) && (userKey =~ /[A-Za-z0-9]{30}/))
  {
    log.debug "Sending Pushover: API key '${apiKey}' | User key '${userKey}'"
    httpPost(params){
    response ->
      if(response.status != 200)
      {
        sendPush("ERROR: 'Pushover Me When' received HTTP error ${response.status}. Check your keys!")
        log.error "Received HTTP error ${response.status}. Check your keys!"
      }
      else
      {
        log.debug "HTTP response received [$response.status]"
      }
    }
  }
  else {
    // Do not sendPush() here, the user may have intentionally set up bad keys for testing.
    log.error "API key '${apiKey}' or User key '${userKey}' is not properly formatted!"
  }
}
