# Built for Badgy / Shadowbadge

# Build Requirements
* Arduino IDE 1.8+
* Followed instructions for envrionment from: https://github.com/sqfmi/badgy/tree/master/examples
  * ESP8266 Arduinio Core
  * Adafruit GFX
  * GxEPD v3.x
  * ArduinoJSON v5.x
  * Wifi Manager Library
  * Time v1.5
* Additional Libraries:
  * AWS-SDK-ESP8266 (sha256 functionality)
  * QRCode (by Richard Moore, v0.0.1)

# Build Process
* Plug in Badgy via USB
* Select approprite board and port (NodeMCU 1.0, /dev/ttyUSB0)
* Under 'Tools' menu: 
    * Select Flash Size: 4M (1M SPIFFS) (required for saving settings, etc)
    * Select 160mhz (required for ssl or the watchdog will think the ssl is taking too long and hard reset)
* Compile and upload sketch

Alternatively you can create the binary using the steps above and update the software using the OTA update tool.