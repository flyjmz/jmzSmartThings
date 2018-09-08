# **HTTPS/SSL/STUNNEL How-To**

### stunnel
* Download and install [stunnel](https://www.stunnel.org/index.html)

### Port Forwarding & DDNS
1. Set up Port Forwarding on you router for the port you specified in Blue Iris's Web Server Settings
2. Set up a DDNS service (lets you use a webpage address instead of your IP address to access Blue Iris, which is nice since your IP address changes periodically and you don't want to update your settings all the time).  I use [no-ip.com](http://no-ip.com/), the examples will follow their site, but it should be simple to interpret for other services.
   1. Create an account, and sign up for the "No-IP Plus" service level (enhanced might work), no need for registering a domain name unless you want your own, just use one of their included domains (e.g. [mydomain.ddns.net](http://mydomain.ddns.net/)).
   2. hostname settings: Host Type - "DNS Host (A)", IP address will auto populate, Assign Group – No Group, Enable Wildcard off.
   3. Set up their DDNS program on your router or computer running Blue Iris (this program checks for IP address changes and then updates the DDNS service so it knows your new IP address.  Easiest to just run the DDNS' program on your Blue Iris computer, but most routers can do this too.

### SSL Certificate Generation
**This is to create a real HTTPS certificate from a Certificate Authority (CA), as opposed to a self-certified one.  SmartThings needs a real one from a CA in order to use the external mode of BI Fusion.**

*NOTE: This step may be ommitted if you you are going to use BI Fusion in local mode, and are only setting this up for external access to Blue Iris for viewing videos, etc.  But if you are using BI Fusion in external mode, you must follow these steps*

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
###### Renewals:
1. Leave "Domains..." blank
2. In "Paste your Let's Encrypt Key...", copy the "account-key.txt" file contents so you can skip verification.
3. In "Paste your CSR...", copy in your "domain-csr.txt" file contents you created the first time you set it up.
4. Click Next.
5. As long as it wasn't expired (so create a reminder somewhere to renew this before it expires each time), it'll take you straight to a new certificate (domain-crt.txt). Download, save a copy in a safe place.

### Configure Stunnel
**(Stop the stunnel service and make sure the GUI is closed before making changes.)**
###### Initial Setup:
1. Edit the stunnel.conf file to read:
```
[Blue-Iris]
accept = xxx.xxx.xxx.xxx:xxxx
connect = xxx.xxx.xxx.xxx:xxxx
cert = blueiris.pem
TIMEOUTclose = 0
```
2. Save stunnel.conf and close it. Save a backup copy to a safe place.
3. Create a new .txt file and call it "blueiris.pem" this is your new certificate file to replace the default stunnel.pem.
4. Open the blueiris.pem file you just created and paste the contents of domain-key.txt into it. After the domain-key.txt contents, paste the contents of domain-crt.txt and save the file. Save a backup copy to a safe place.

###### Renewals:
1. Open the blueiris.pem file and delete the old certificate (so everything after "-----END PRIVATE KEY-----")
2. Paste the new certificate to the end (all of the new domain-crt.txt you just downloaded)
3. Save the file and close it. Save a backup copy to a safe place.
4. Start the Stunnel service, then open the Stunnel GUI and reload configuration. Test it out!
    - There are several stunnel links in start menu, if you choose to run as a service it'll just open a service and not have a GUI or anything running.  You can go to Windows Processes and find the stunnel service and make it auto-start on computer boot so that even after restarts you don't ever have to start stunnel again.

**Blue Iris HTTPS Setup**

* In the Web Server tab within Blue Iris Options, enable HTTPS and fill out the other settings based on your Stunnel.conf settings above and how you would like it to operate.  The Blue Iris help files are pretty good for this topic.
