## **HTTPS/SSL/STUNNEL How-To**

**stunnel:**

* Download and install [stunnel](https://www.stunnel.org/index.html)
* Once installed, follow the SSL Certificate Generation Steps to complete setup.

**SSL Certificate Generation**

1. Setup your host on [no-ip.com](http://no-ip.com/). (Or other services)
a) Create an account, and sign up for the "No-IP Plus" service level (enhanced might work), no need for registering a domain name unless you want your own, just use one of their included domains (e.g. [mydomain.ddns.net](http://mydomain.ddns.net/)).

b) Set up your hostname: Host Type - "DNS Host (A)", IP address will auto populate, Assign Group – No Group, Enable Wildcard off.

2. Go to:[zerossl.com ](http://zerossl.com/) > click on Online Tools > Click on Start for the Free SSL Certificate Wizard

3. Enter Email Address, check DNS Verification box, check both terms of service boxes to agree

4. For New setups:

a) In "Domains...", type in your domain name (e.g. [mydomain.com](http://mydomain.com/) or [mydomain.ddns.net](http://mydomain.ddns.net/)).
b) Leave the "Paste your Let's Encrypt Key..." and "Paste your CSR..." fields blank.
c) It'll ask if you want to include the www version (if yes, you'll get it for [www.mydomain.ddns.net](http://www.mydomain.ddns.net/) as well). Click next.
d) It'll generate the domain-csr.txt file, then generate the Key (account-key.txt). Be sure to save both files and put in a secure location.
e) Then it'll take to you the verification step, use the DNS verification (not http, which has you put specific stuff on your server, which you can't do in blue iris).
f) Log into your [No-ip.com](http://no-ip.com/) account > manage domains > Modify > Advanced Records > TXT, and copy the TXT record into it and click update
g) Wait 15-30 minutes (per directions), and click verify. When it's good, it'll take you to your last step.
h) Download the domain-crt.txt and domain-key.txt files, and save your account ID. Keep all the files in a safe place!
i) Delete the TXT records from your hostnames on your [no-IP.com](http://no-ip.com/) account.
5. For Renewals:
a) Leave "Domains..." blank
b) In "Paste your Let's Encrypt Key...", copy the "account-key.txt" file contents so you can skip verification.
c) In "Paste your CSR...", copy in your "domain-csr.txt" file contents you created the first time you set it up.
d) Click Next.
e) As long as it wasn't expired (so create a reminder somewhere to renew this before it expires each time), it'll take you straight to a new certificate (domain-crt.txt). Download, save a copy in a safe place.
6. Configure Stunnel, first stop the stunnel service and make sure the GUI is closed.
7. For new setups:
a) Edit the stunnel.conf file to read:
[Blue-Iris]
accept = xxx.xxx.xxx.xxx:xxxx
connect = xxx.xxx.xxx.xxx:xxxx
cert = blueiris.pem
TIMEOUTclose = 0
b) Save stunnel.conf and close it. Save a backup copy to a safe place.
c) Create a new .txt file and call it "blueiris.pem" this is your new certificate file to replace the default stunnel.pem.
d) Open the blueiris.pem file you just created and paste the contents of domain-key.txt into it. After the domain-key.txt contents, paste the contents of domain-crt.txt and save the file. Save a backup copy to a safe place.
8. For Renewals:
a) Open the blueiris.pem file and delete the old certificate (so everything after "-----END PRIVATE KEY-----")
b) Paste the new certificate to the end (all of the new domain-crt.txt you just downloaded)
c) Save the file and close it. Save a backup copy to a safe place.
9. Start the Stunnel service, then open the Stunnel GUI and reload configuration. Test it out!
a) There are several stunnel links in start menu, if you choose to run as a service it'll just open a service and not have a GUI or anything running.  You can go to Windows Processes and find the stunnel service and make it auto-start on computer boot so that even after restarts you don't ever have to start stunnel again.

**Blue Iris HTTPS Setup**

* In the Web Server tab within Blue Iris Options, enable HTTPS and fill out the other settings based on your Stunnel.conf settings above and how you would like it to operate.  The Blue Iris help files are pretty good for this topic.
