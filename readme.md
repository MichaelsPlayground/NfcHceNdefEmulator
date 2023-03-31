# Nfc HCE Ndef Emulator and Reader

This app emulates an NFC Forum Type 4 Tag on an Android device using **Host-based Card Emulation** ("HCE").

To be compliant with the specification a service is running independent on the app opened that serves 
an **Application Identifier** ("AID") with the value **D2760000850101**.

The app is running up to Android 13 (SDK 33, the actual one when app was created) and is tested on real 
Android devices with Android 9 and Android 13.

There are 3 main functionalities:

1) Send a message using HCE to another device in NDEF format
2) read a message using the Android's NFC reader and reading an incoming intent with NDEF data using NDEF protocol (high level API)
3) read a message using the Android's NFC reader and run the complete reading sequence using IsoDep protocol (low level API)

As there are very less articles and source code for NDEF emulation via HCE available on the Internet I was very happy 
to find one repository on GitHub containing a running service class, so all credits go to the original author.  
I just made the class a little more "handy" to accept individual messages.

The code is from the author "TechBooster" and is available in the repository "C85-Android-4.4-Sample"; the code for the 
"NdefHostApduService.java" class is available here:

https://github.com/TechBooster/C85-Android-4.4-Sample/blob/master/chapter08/NdefCard/src/com/example/ndefcard/NdefHostApduService.java

As the source code of the service class is under **Apache License 2.0** my app got the same license.

Note: please keep in mind that **the app is active since installation** as it runs a **HostApduService**. If there are other apps 
on your phone providing data for NFC - AID "**D2760000850101**" (NDEF access) you can run into problems ("routing conflict") that needs 
to get resolved each time a card reader is tapped to the device and asking for an NDEF message. On your device is under the "Settings menu" an entry 
"Connections" - "NFC" - "Payments" (or similar) and a sub-tab "others" - there you can choose which services are active at 
one time.




