# Built for Badgy / Shadowbadge

# Build Requirements
* Arduino IDE 1.8+
* Followed instructions for envrionment from: https://github.com/sqfmi/badgy/tree/master/examples
  * ESP8266 Arduinio Core (v1.10.10)
  * Adafruit GFX v1.10.x
  * GxEPD v3.x
  * ArduinoJSON v5.x
  * WifiManager v0.16.x
  * Time v1.5.x
* Additional Libraries:
  * AWS-SDK-ESP8266 (sha256 functionality)
  * QRCode (by Richard Moore, v0.0.1)
  * Manually Install [ESP8266TrueRandom](https://github.com/marvinroger/ESP8266TrueRandom)
* [Install](https://github.com/esp8266/arduino-esp8266fs-plugin) the SPIFFS Tool

# Build Process
* Plug in Badgy via USB
* Select approprite board and port (NodeMCU 1.0, /dev/ttyUSB0)
* Under 'Tools' menu: 
    * Select Flash Size: 4M (1M SPIFFS) (required for saving settings, etc)
       - In newer versions of Arduino IDE this may say FS:1MB
    * Select 160mhz (required for ssl or the watchdog will think the ssl is taking too long and hard reset)
* Compile and upload sketch

Alternatively you can create the binary using the steps above and update the software using the OTA update tool.
