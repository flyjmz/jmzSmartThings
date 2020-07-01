# HTTPS-SSL-STUNNEL How-To
*Current As of 8 Sep 2018*

[![Donate Button](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/donateButton.gif "Donate")](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=6T44ZPUKCMYL6&lc=US&item_name=FLYJMZ%20Custom%20SmartApps&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted)

* This how-to explains a method to increase security for Blue Iris connections.  
* Normal port-forwarding without setting up any extra layers of security isn't necessarily bad, as long as the Blue Iris Web Server settings are set to require authentication and the "Use Secure Session Keys and login page" box is checked, however that is not compatible with BI Fusion.  You can use these instructions regardless of SmartThings, because they add security for Blue Iris itself.
* If you are using BI Fusion in SmartThings, then definitely follow these steps or set up a VPN in order to secure your Blue Iris server from the outside internet while still allowing BI Fusion access.
* Locally connecting BI Fusion requires that "Use Secure Session Keys and login page" box to be unchecked, slightly weakening Blue Iris' external security, but does not impose any risks locally as long as you secure your home network with a good password and encryption level.  Setting up this HTTPS connection or using a VPN will ensure all other (external) connections remain secure, so you can use your phone app to view video, etc.  
* I have rough draft VPN setup instructions available too, will post and link sometime in the future.

### Port Forwarding & DDNS
1. Set up Port Forwarding on you router's settings.  Refer to your router's manual, and use the IP address of your Blue Iris Server Computer and choose a port (not on of the common ones though, 4 digit ports can help keep you safe from picking a common one).  You'll likely want to set up a IP Reservation for your computer's IP on your server so that when the computer or router restarts it doesn't change the IP.  You could also use a static IP.
2. Set up a DDNS service (lets you use a webpage address instead of your IP address to access Blue Iris, which is nice since your IP address changes periodically and you don't want to update your settings all the time).  I use [no-ip.com](http://no-ip.com/), the examples will follow their site, but it should be simple to interpret for other services.
   1. Create an account, and sign up for the "No-IP Plus" service level (enhanced might work), no need for registering a domain name unless you want your own, just use one of their included domains (e.g. [mydomain.ddns.net](http://mydomain.ddns.net/)).
   2. hostname settings: Host Type - "DNS Host (A)", IP address will auto populate, Assign Group – No Group, Enable Wildcard off.
   3. Set up their DDNS program on your router or computer running Blue Iris (this program checks for IP address changes and then updates the DDNS service so it knows your new IP address.  Easiest to just run the DDNS' program on your Blue Iris computer, but most routers can do this too.

### Stunnel Setup
*Stunnel is a program that secures the WAN (internet) side of the connection, and connects it to the LAN (local) side.  You port forward one port, but have Blue Iris on another.  So you are port forwarding Stunnel, which then connects to Blue Iris). For example, you could have Blue Iris’s web server port set to 4000, but for your router’s port forwarding you'd use a different port, like 4002. Stunnel accepts the 4002 from the router and connects you to the 4000 port, which is your BI Server.*

1. Download and install [stunnel](https://www.stunnel.org/index.html)
2. Edit the stunnel.conf file to read:
```
[Blue-Iris]
accept = xxx.xxx.xxx.xxx:xxxx
connect = xxx.xxx.xxx.xxx:xxxx
cert = blueiris.pem
TIMEOUTclose = 0
```
2. The 'accept' IP:PORT address is the IP address of the computer running Stunnel (likely the same computer as the one running Blue Iris), and the Port is the one forwarded in your router settings that your DDNS service connects through. For example: 192.168.0.15:4002
3. The 'connect' IP:PORT address is the IP address of the computer running Blue Iris (again, the IP for both may be the same if it's the same computer), and the Port is a different one that you'll set up in Blue Iris. For example: 192.168.0.15:4000
3. The 'cert' part is the certificate file used to encrypt your connection.  Stunnel can/will create a self-signed certificate for you and put it in a file called 'stunnel.pem'. You can use this and skip the SSL Certificate Generation section if you don't plan to use the external mode in BI Fusion.  If you do that, then leave the .conf file as is, with 'cert = stunnel.pem' otherwise change it to "cert = blueiris.pem" and complete the SSL Certificate Generation steps below before running stunnel.
4. Save stunnel.conf and close it. Save a backup copy to a safe place. 

### Blue Iris HTTPS Setup

1. Open the Web Server tab within Blue Iris Options
2. Enable HTTPS and fill out the other settings based on your Stunnel.conf settings above and how you would like it to operate.  The Blue Iris help files are pretty good for this topic.

### SSL Certificate Generation
**This is to create a real HTTPS certificate from a Certificate Authority (CA), as opposed to a self-signed one.  SmartThings needs a real one from a CA in order to use the external mode of BI Fusion.**

*NOTE: This step may be omitted if you are going to use BI Fusion in local mode, and are only setting this up for external access to Blue Iris for viewing videos, etc.  But if you are using BI Fusion in external mode, you must follow these steps*

1. Go to: [zerossl.com ](http://zerossl.com/) > click on Online Tools > Click on Start for the Free SSL Certificate Wizard
2. Enter Email Address, check DNS Verification box, check both terms of service boxes to agree

###### Initial Setup:
1. In "Domains...", type in your domain name (e.g. [mydomain.com](http://mydomain.com/) or [mydomain.ddns.net](http://mydomain.ddns.net/)).
2. Leave the "Paste your Let's Encrypt Key..." and "Paste your CSR..." fields blank.
3. It'll ask if you want to include the www version (if yes, you'll get it for [www.mydomain.ddns.net](http://www.mydomain.ddns.net/) as well). Click next.
4. It'll generate the domain-csr.txt file, then generate the Key (account-key.txt). Be sure to save both files and put in a secure location.
5. Then it'll take to you the verification step, use the DNS verification (not http, which has you put specific stuff on your server, which you can't do in blue iris).
6. Log into your [No-ip.com](http://no-ip.com/) account > manage domains > Modify > Advanced Records > TXT, and copy the TXT record into it and click update
7. Wait 15-30 minutes (per directions), and click verify. When it's good, it'll take you to your last step.
8. Download the domain-crt.txt and domain-key.txt files, and save your account ID. Keep all the files in a safe place!
9. Delete the TXT records from your hostnames on your [no-IP.com](http://no-ip.com/) account.
10. Create a new .txt file and call it "blueiris.pem" (.pem is the file extension, delete the txt part) this is your new certificate file to replace the default stunnel.pem.
11. Open the blueiris.pem file you just created and paste the contents of domain-key.txt into it. After the domain-key.txt contents, paste the contents of domain-crt.txt and save the file. Save a backup copy to a safe place.
12. Put your new blueiris.pem file into the same folder as stunnel.pem, and delete stunnel.pem.
13. Start the Stunnel service, then open the Stunnel GUI and reload configuration. Test it out!
    - There are several stunnel links in start menu, if you choose to run as a service it'll just open a service and not have a GUI or anything running.  You can go to Windows Processes and find the stunnel service and make it auto-start on computer boot so that even after restarts you don't ever have to start stunnel again.
###### Renewals:
1. Leave "Domains..." blank
2. In "Paste your Let's Encrypt Key...", copy the "account-key.txt" file contents so you can skip verification.
3. In "Paste your CSR...", copy in your "domain-csr.txt" file contents you created the first time you set it up.
4. Click Next.
5. As long as it wasn't expired (so create a reminder somewhere to renew this before it expires each time), it'll take you straight to a new certificate (domain-crt.txt). Download, save a copy in a safe place.
6. Open the blueiris.pem file and delete the old certificate (so everything after "-----END PRIVATE KEY-----")
7. Paste the new certificate to the end (all of the new domain-crt.txt you just downloaded)
8. Save the file and close it. Save a backup copy to a safe place.
9. Start the Stunnel service, then open the Stunnel GUI and reload configuration. Test it out!
    - There are several stunnel links in start menu, if you choose to run as a service it'll just open a service and not have a GUI or anything running.  You can go to Windows Processes and find the stunnel service and make it auto-start on computer boot so that even after restarts you don't ever have to start stunnel again.
    
### Computer Interface, Mobile Blue Iris app, and SmartThings Considerations
* Depending on your connection, either local or external to your home network, you'll use a different address to connect to Blue Iris:
    * Locally it'd be 192.168.0.15:4000
    * Externally it’d be mydomain.ddns.org:4002
* Remember you'll have to include the port in the address as shown above, :80 is assumed if you don't type a port, and it won't work
* For the iOS/Android app, you'll have to edit the connection, ensuring you have the LAN and WAN addresses entered as shown above with the different ports and addresses.  Also, don't forget to use the dropdown and set http/https as required (https for WAN, for LAN it'd most likely be http unless you set up Blue Iris to use the secure HTTPs for LAN as well, which means you'd also need to use the stunnel port, 4002 in this example, for both).
* For SmartThings, none of this matters if you use the local connection in BI Fusion (either with or without the server device) and the computer and your SmartThings hub are on the same network.  You'd just use the local IP and Port.  If they aren't on the same network, or you choose to use the external connection method for some other reason, then you'd use the external address and port, and also must use a real CA SSL certificate.

[![Donate Button](https://raw.githubusercontent.com/flyjmz/jmzSmartThings/master/resources/donateButton.gif "Donate")](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=6T44ZPUKCMYL6&lc=US&item_name=FLYJMZ%20Custom%20SmartApps&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted)
