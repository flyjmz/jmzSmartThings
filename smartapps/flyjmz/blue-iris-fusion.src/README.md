# BI Fusion README
[![Donate Button](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/donateButton.gif "Donate")](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=6T44ZPUKCMYL6&lc=US&item_name=FLYJMZ%20Custom%20SmartApps&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted)

* [SmartThings Community Thread](https://community.smartthings.com/t/release-bi-fusion-v3-0-adds-blue-iris-device-type-handler-blue-iris-camera-dth-motion-sensing/103032)
* [HTTPS/SSL/STUNNEL How-To for securing Blue Iris external (WAN) connections](https://github.com/flyjmz/jmzSmartThings/blob/master/smartapps/flyjmz/blue-iris-fusion.src/HTTPS:SSL:STUNNEL%20How-To.md)
* (upcoming) VPN How-To for securing Home Network & Blue Iris external (WAN) connections

**Server & Camera Device Pages**

![Blue Iris Server Device Type](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BIServerDevice.png)
![Blue Iris Camera Device Type](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BICameraDevice.jpeg)

## BI Fusion Features
* Simplified setup – Only enter your password, IP, etc. ONCE!  OAuth tokens only change when you want them too, and they can be found in multiple places to enable copy & paste!
* All the smartapp functions in one smartapp: SmartThings events trigger Blue Iris recording, SmartThings modes control Blue Iris profiles, PTZ to presets (and back) based on SmartThings events.
* Blue Iris Server Device – You can manually control Blue Iris mode’s and traffic light from SmartThings, monitor online/offline status, get alerts.
* Local LAN connection status updates and error notifications – no more need to use port forwarding, etc.  But still works with external connections as before.
* Blue Iris Camera Devices – Act like switches so other automations can manually trigger recording, and like motion sensors so other automations (like SHM or [Super Notifier](https://community.smartthings.com/t/release-super-notifier-all-your-alerts-in-one-place/59707)) can react and alert you to motion sensed by your BI cameras.  Plus you can manually trigger recording from the device’s page in the app.
* Works with webCoRE so you can create your own automations
* Limited live stream capability - only work when your phone is on the same LAN as your Blue Iris computer, and is limited by SmartThings' live stream capabilities.  No image capture due to the way BI Fusion builds all the capabilities into one set of smartapps and DTHs.  I strongly encourage you to use the [Blue Iris Mobile apps](http://blueirissoftware.com/mobile/) for video and image viewing. It has all the capabilities now and works better than non-SmartThings-branded video will ever work within SmartThings.

**Plus BI Fusion still does (and now does better):**
* Profile Integration – Sync’s ST Modes with BI’s Profiles so they match
* BI Triggers – Triggers BI camera’s to record based on motion/contact sensors/etc.  This can now be done from other smartapps as well through the Blue Iris Camera DTH.
* WAN/External connection control if you need it.

**If you’re unfamiliar with Blue Iris (BI):**
Blue Iris is an outstanding Video Security/Capture NVR (Network Video Recorder) program that works with virtually every IP camera out there, creating a web server that manages them all and monitors them for motion, triggering recording. [http://blueirissoftware.com/](http://blueirissoftware.com/) Big shout out to Ken, the program is great and his support is outstanding.

**Clarification of the Camera DTH's Intent & Purpose:**
* The Camera Device can be selected as a switch to turn 'on' from other smartapps, this triggers the Blue Iris Camera to start recording for the preset length you setup in Blue Iris.
* The Camera Device can also be selected as a Motion Sensor, which becomes active/inactive based on the Blue Iris motion sensing settings you set up in Blue Iris.
* You don't want to have your Camera Device's motion sensing trigger the Camera Device's switch to enable recording, because you'll create an endless loop of recording...  Treat the two function separately.  
    * Example: Front door contact opening triggers the Porch Camera Device's switch 'on' to ensure the event is recorded.  Separately, you can have SHM monitor the Porch Camera DTH's motion and alert you when it is active.

## Install Instructions
1.	In the SmartThings API, Create a new Smartapp. Either from code and copy the code from the links below, or add my Github repository to your Github Integration settings:
    * Owner: flyjmz
    * Name: jmzSmartThings
    * Branch: master
2.	You need to add two smartapps and two Device Type Handlers (DTH): 
    1. [BI Fusion SmartApp](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/smartapps/flyjmz/blue-iris-fusion.src/blue-iris-fusion.groovy)  *(Publish this one)*
    2. [BI Fusion – Trigger SmartApp](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/smartapps/flyjmz/blue-iris-fusion-trigger.src/blue-iris-fusion-trigger.groovy) *(Do not publish this one, it only needs to be installed in the IDE)*
    3. [Blue Iris Server DTH](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/devicetypes/flyjmz/blue-iris-server.src/blue-iris-server.groovy) *(Publish this one)*
    4. [Blue Iris Camera DTH](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/devicetypes/flyjmz/blue-iris-camera.src/blue-iris-camera.groovy) *(Publish this one)*

**Make sure you enable OAuth for the "BI Fusion" smartapp** (Once you've created the new app in your API, find it in the Smart Apps list and click "Edit Properties." Then scroll down to OAuth and enable it.)

3.	One you’ve added them in the API, open the SmartThings app on your phone, add an automation, and add the “Blue Iris Fusion” app under the “My Apps” category. Only install the “Blue Iris Fusion” app, do not install the “Blue Iris Fusion - Trigger” app (it will install itself).
4.	Go through and enter your settings.  Some gotchas:
    * For the Server Device, the ST Hub and BI Computer need to be on the “same” network with either static IP’s or DHCP with address reservations so that the IP isn’t changing.
    * If using the Server Device or a local/LAN connection, only enter the IP address, nothing extra
    * The Camera Short Name is the “Short Name” set in Blue Iris Camera Properties.  
    * Make sure there are no spaces or special characters in the username, password, and camera short name (I can’t guarantee it’ll work if there are).
    * The username for Blue Iris login needs to be an Admin username to make profile changing work.  For security reasons, I recommend creating a new username just for this integration, so you can look at the logs and tell when it was you or the app that was logging into Blue Iris.
    * If you want to use the BI Fusion Triggers to have ST trigger BI camera recording, and use the Blue Iris Camera DTH (which I recommend), you need to complete the initial setup through BI Fusion settings once first.  Click 'done' through the pages, then reenter BI Fusion settings and set up the Triggers (because that first iteration of settings creates the devices, so if you didn't close out once first, they wouldn't be created and available for selection).
    * In Blue Iris’s Advanced Webserver settings, “Use Secure Session Keys and Login Page” must NOT be checked:
![Blue Iris Web Server Settings](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/biwebserversettings.jpg)

***NOTE*** 
*While this is a reduced security setting in Blue Iris, it is only being used when using a local only connection (i.e. computer and hub are on the same network that is secured from intrusion).  So as long as you've taken reasonable steps to secure your home network, this is not reducing security.  If you are using the external options for setup (i.e. the computer and hub are not on the same network), then you don't need to do this step and can leave it with the higher security setting.  (Local and external use different protocols to communicate between computer and hub, and require different settings to work).*

5.	If you’re installing camera devices and want motion triggers, you’ll have to use the access token found during setup and enter it in Blue Iris for each camera.  I’m trying to make this better, but for now we have to use OAuth.  One improvement I made is the ability to “View Only” the access token URLs, so that they don’t change every time you open BI Fusion’s settings.  You can also choose to have the URLs displayed in the API's Live Logging tab so you can copy & paste into Blue Iris too!
    * From the BI Fusion Settings, first generate new tokens and continue with the other settings.  We’ll set up Blue Iris in bit.
6.	Once done entering settings, click “done” until exited out of settings, go to the Blue Iris Server device on your phone, and tap the refresh button to have it do it’s first update.
7.	Now we can set up Motion Alerts in Blue Iris.
    * Go to Blue Iris and open Camera Properties for your first camera.
    * Follow the directions in these screenshots and enter the URLs from the BI Fusion Setting Screenshot you took or from the debug logs earlier:
![Blue Iris Alert Settings](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/bialertsettings.jpg)

![Blue Iris Alert Settings Detail](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/BIalertsettingsdetail.jpg)

    * Make sure you enter the URL correctly, not including the “https://” but selecting it from the dropdown menu.
    * Replace “cameraShortNameHere” with your camera’s short name
    * The only difference between the active and inactive URLs is the “in” for “inactive,” so once you type the URL in for the first camera, you can just copy and paste for the rest of the cameras, ensuring the short name is correct for each camera and you use active and inactive for “when triggered” and “Request again when trigger is reset”
8.	You’re set!  Just remember, **don’t change any settings from within the device settings themselves**, make required updates within BI Fusion’s settings.

**If you want to learn more:**
These are old threads if you want to see the comments, but all the funcitonality has been incorporated and improved in this new app.
[The old BI Fusion Thread is here if you want to look over other’s comments](https://community.smartthings.com/t/release-blue-iris-fusion-integrate-smartthings-and-blue-iris/54226).

[The v1.0 beta of the Blue Iris Device Handler Thread is here.](https://community.smartthings.com/t/release-blue-iris-device-handler/101765)

## FAQ
**What settings are important in Blue Iris?**
Go to Blue Iris Options, the Web server tab, click Advanced, and make sure "Use Secure Session Keys and Login Page" is unchecked. This enables the login to work, but also uses a less secure method of login. If you are using it LAN only and do not port-forward or otherwise expose your Blue Iris server to the Internet, then there is no need to worry. If you do have it set up for external logins (i.e. to log into the BI server when you’re away), then please make sure you have secured that connection in another way. I recommend two different options:
1.	VPN. You can set up a VPN server on your computer, a network device like a Synology server, or on your router using [DD-WRT firmware](http://www.dd-wrt.com/) (which is a relatively simple and doesn’t cost anything if you have a good router already). The VPN will take care of all the security, it’s as if you don’t expose it to the internet but you can still reach the server! I have a not-yet-ready-for-publishing How-To.
2.	HTTPS. Blue Iris has [stunnel](https://www.stunnel.org/index.html) integration. It’s not to difficult to set this up either. You port forward the BI server but secure the connection so your login and viewing are all encrypted. I have an extensive how-to here: [HTTPS/SSL/STUNNEL How-To](https://github.com/flyjmz/jmzSmartThings/blob/master/smartapps/flyjmz/blue-iris-fusion.src/HTTPS:SSL:STUNNEL%20How-To.md).

**What is the difference between WAN and LAN?**
LAN = Local area network, WAN = Wide area network. For BI Fusion setup, LAN specifically means all traffic will stay on your home network, nothing out to the cloud (the SmartThings hub sends commands directly to the computer running Blue Iris). The WAN setup differs by having the SmartThings cloud send the commands to your Blue Iris computer.

With BI Fusion v3.0+, using the Blue Iris Server DTH is the best option, but if you don’t want to use it, there is a functional difference between the Local and External options in setup.  The WAN option provides feedback if Blue Iris didn’t actually receive the command (no way to make this happen within this app alone when using LAN), and the WAN setup requires opening a port so the cloud can reach your Blue Iris computer.  Again, the Blue Iris Server DTH does not have any limits to functionality, and it provides more.

I can think of few situations that WAN would be a better fit, primarily if you have your SmartThings hub on a different network than your Blue Iris computer. I would imagine most people would use LAN.

## Current Version Changes:
* Custom Polling improved
* Added ability for triggers to move PTZ cameras to a preset, and then move them back (if desired)
                            
Older Major Changes:
* Added triggers for door knocking and mode changes
* Added ability to set your own icon for the Server and Camera Devices
* Expanded error checking to lock status (hold/temporary profile change mode)
* Improved settings and operation when not using profile<>mode integration.
* Added ability to display motion URLs in API Live Logging so you can copy & paste into Blue Iris
* BI Fusion updated to allow user to change Camera Device Names after installation.
* Added Video Live Stream
* Added triggers for: acceleration, presence, shock, smoke, carbonMonoxide, and water sensors.
* Added ability to send preset commands to cameras when triggering.
* Enabled full Camera Device DTH support even when not using Server DTH (works for both local and external connections).


[![Donate Button](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/donateButton.gif "Donate")](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=6T44ZPUKCMYL6&lc=US&item_name=FLYJMZ%20Custom%20SmartApps&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted)
*Thank You!  This is purely optional, no guarantees this will work perfectly, but brew money helps keep me going!*
