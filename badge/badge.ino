// for cpu throttling
extern "C" {
#include "user_interface.h"
}

// high and low freq for processor switching
#define LOW_FREQ 80
#define HIGH_FREQ 160

// how long the main loop should delay, in ms
#define LOOP_DELAY 5

// on-esp SPIFFS filesystem
#include <FS.h>

// and a way to serialize/deserialize json (for config onto file system)
#include <ArduinoJson.h>

// generate true random numbers
#include <ESP8266TrueRandom.h>

/* e-paper display lib */
#include <GxEPD.h>

// correct display for b/w/r badgy
#include <GxGDEW029Z10/GxGDEW029Z10.h>    // 2.9" b/w/r
#include <GxIO/GxIO_SPI/GxIO_SPI.h>
#include <GxIO/GxIO.h>

/* WiFi and WiFi-related libs*/
#include <ESP8266WiFi.h>
#include <DNSServer.h>
#include <ESP8266WebServer.h>
#include <ESP8266HTTPUpdateServer.h>
#include <WiFiManager.h>

// uuid-ish
#include <base64.h>
#include <sha256.h>

// qr code generation
#include <qrcode.h>

// shadowman display
#include "shadowman.c"

// custom fonts
#include "OverpassMonoBold12pt.h"
#include "OverpassMonoBold10pt.h"
#include "OverpassMonoBold08pt.h"
#include "OverpassMono08pt.h"

// name of config file
const char* _configFile = "/shadowbadge.conf.json";

// get system chip id which is really the chip MAC address
// we don't want to leak this upstream but we can do something with it...
// like seeding a pRNG to create a UUID
// as a component of the UUID...
// uh, UUID stuff
const uint32_t _chipId = system_get_chip_id();
char salt[32] = ""; // if empty replaced by a randomly generated salt value and saved in the config
char secret[8] = ""; // secret must be presented to endpoint in order to get content if the badge was claimed with a secret
char uuid[64];

// application root
const char* applicationHost = "shadowbadge.apps.cluster.ruffalo.org";
const char* applicationRoot = "https://shadowbadge.apps.cluster.ruffalo.org";
const int applicationPort = 443;

/* where values are loaded from configuration */
const char* key_owner_id = "ownerId";
const char* key_display_heading = "heading";
const char* key_display_title = "title";
const char* key_display_loc = "location";
const char* key_display_group = "group";
const char* key_display_info = "tagline";
const char* key_qr_url = "url";
const char* key_qr_type = "qr_type";
const char* key_badge_status = "status";
const char* key_badge_icon = "icon";
const char* key_badge_style = "style";
const char* key_badge_hash = "hash";
const char* key_salt = "private_salt";
const char* key_secret = "private_secret";

// holders for calculated name/password for use in config callback step
char ap_name[21];
char ap_password[10];

char owner_id[128] = "";
char display_heading[34] = "Shadow Badge"; // this is waaaayyyyy too big
char display_title[25] = "Cloud Entity"; // more wayy tooo big
char display_loc[25] = "Earth"; // still too big
char display_group[25] = "Everywhere"; // still too big but more realistic
char display_info[25] = "RHCA XXXXXV"; // same
char badge_status[16] = "NONE";
char badge_icon[12] = "RED_HAT";
char badge_style[12] = "BADGE_RIGHT";
char badge_hash[50] = "NONE";
char qr_url[512] = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
char qr_type[16] = "RELATIVE";

// update server port
const int port = 8888;

/* Configure pins for display */
GxIO_Class io(SPI, SS, 0, 2);
GxEPD_Class display(io); // default selection of D4, D2

/* A single byte is used to store the button states for debouncing */
byte buttonState = 0;
byte lastButtonState = -1;   //the previous reading from the input pin
unsigned long lastDebounceTime = 0;  //the last time the output pin was toggled
unsigned long debounceDelay = 50;    //the debounce time

unsigned long lastRefreshTime = 0;  //the last time the output pin was toggled
unsigned long refreshDelay = 2000; // at least two seconds

const uint8_t minSslBuffer = 512;
const uint8_t maxSslBuffer = 2048;

// state machine variables that control loop
boolean needsClaim = false;
boolean needsRefresh = false;
boolean needsRedraw = true;
boolean needsConnectIconRedraw = false;

// this is our retry count for *CONFIGURED* status only
// it is only incremented with the status is already
// CONFIGURED and is basically used to prevent the 
// wifi refresh loop from running constantly in the 
// event that no change actually happened before or 
// shortly after the button is pressed
int maxConfiguredRetryCount = 50;
int configuredRetryCount = 0;

// single global wifi interfaces
WiFiManager wifiManager;
BearSSL::WiFiClientSecure client;

// reads configuration from any json serialized object
// could be web or not
void readJsonConfig(JsonObject& json, boolean readPrivate = false) {
  strlcpy(owner_id, json[key_owner_id] | owner_id, 128);
  strlcpy(display_heading, json[key_display_heading] | display_heading, 34); // with current font size 24 is the max length
  strlcpy(display_title, json[key_display_title] | display_title, 25);
  strlcpy(display_loc, json[key_display_loc] | display_loc, 25);
  strlcpy(display_group, json[key_display_group] | display_group, 25);
  strlcpy(display_info, json[key_display_info] | display_info, 25);
  strlcpy(badge_status, json[key_badge_status] | badge_status, 16);
  strlcpy(badge_icon, json[key_badge_icon] | badge_icon, 12);
  strlcpy(badge_style, json[key_badge_style] | badge_style, 12);
  strlcpy(badge_hash, json[key_badge_hash] | badge_hash, 50);
  strlcpy(qr_url, json[key_qr_url] | qr_url, 512);
  strlcpy(qr_type, json[key_qr_type] | qr_type, 16);

  // if the qr is relative modify qr_url if it does not start with http or https
  // in this case it will point to the application root with whatever
  // url was given by the UI. this means that the chip doesn't have to 
  // be reflashed to "move" the URL it **does** mean that either the
  // server or the chip needs to have a "/" at the end of the URL which
  // we can do on the server and always have no "/" at the end of the
  // applicationRoot
  if (strcmp("RELATIVE", qr_type) == 0) {
    String urlString = String(qr_url);
    if (!urlString.startsWith("http") && !urlString.startsWith("HTTP")) {
      sprintf(qr_url, "%s%s", applicationRoot, urlString.c_str()); 
    }
  }

  // use this block to read 'private' values that can't be set from the
  // web (like the salt, for example, or other sensitive info)
  if (readPrivate) {
    strlcpy(salt, json[key_salt] | "", 32);
    strlcpy(secret, json[key_secret] | "", 8);
  }
}

// writes configuration to disk
// WARNING: if you add a new field and don't write it here then it will not 
// be written and you will try and use it at boot and it won't exist and you
// will wonder why
void writeConfig() {
  DynamicJsonBuffer jsonBuffer;
  JsonObject& json = jsonBuffer.createObject();
  json[key_owner_id] = owner_id;
  json[key_display_heading] = display_heading;
  json[key_display_title] = display_title;
  json[key_display_loc] = display_loc;
  json[key_display_group] = display_group;
  json[key_display_info] = display_info;
  json[key_badge_icon] = badge_icon;
  json[key_badge_status] = badge_status;
  json[key_badge_style] = badge_style;
  json[key_badge_hash] = badge_hash;
  json[key_qr_url] = qr_url;
  json[key_qr_type] = qr_type;

  // save salt if created
  if (strlen(salt) > 0) {
    json[key_salt] = salt;
  }

  if (strlen(secret) > 0) {
    json[key_secret] = secret;
  }

  File configFile = SPIFFS.open(_configFile, "w");
  json.printTo(configFile);
  configFile.close();
}

void loadConfig() {
  if (SPIFFS.begin()) {
    if (SPIFFS.exists(_configFile)) {
      // open config file
      File configFile = SPIFFS.open(_configFile, "r");

      // read into buffer and parse out json
      size_t size = configFile.size();
      std::unique_ptr<char[]> buf(new char[size]);
      configFile.readBytes(buf.get(), size);
      DynamicJsonBuffer jsonBuffer;
      JsonObject& json = jsonBuffer.parseObject(buf.get());

      // if parsed, send to read function
      if (json.success()) {
        readJsonConfig(json, true);
      }
    } else {
      // write default config file
      writeConfig();
    }
  }
}

void bootstrapConfig() {
  // load configuration
  loadConfig();

  // only needed if config changed
  boolean configChanged = false;

  // determine if salt is present here
  if (strlen(salt) < 1) {
    // generate random salt
    byte randomSalt[16];
    ESP8266TrueRandom.uuid(randomSalt);
    String saltStr = ESP8266TrueRandom.uuidToString(randomSalt);

    // copy up to bytes to the buffer
    strlcpy(salt, saltStr.c_str(), 32);

    // set config changed after action
    configChanged = true;
  }

  // determine if secret is present and copy 8 bytes for the badge secret
  if (strlen(secret) < 1) {
    // generate random salt
    byte randomSecret[16];
    ESP8266TrueRandom.uuid(randomSecret);
    String secretString = ESP8266TrueRandom.uuidToString(randomSecret);
    String encoded = base64::encode(secretString);
    strlcpy(secret, encoded.c_str(), 8);
    
    // set config chagned after action
    configChanged = true;
  }

  // and write configuration if private values changed
  if (configChanged) {
    writeConfig();
  }

  // create chip hash from salt and chip id, this gives us the ability
  // to generate a "new" id by blowing away the salt
  SHA256 sha256;
  sha256.add(&salt, strlen(salt));
  sha256.add(&_chipId, 4);
  strlcpy(uuid, sha256.getHash(), 64);

  // create an AP name based on the UUID
  char ap_id[8];
  char ap_pass[10];
  strlcpy(ap_id, uuid, 8);
  strlcpy(ap_pass, uuid + 8, 9);
  sprintf(ap_name, "Shadowbadge %s", ap_id);
  sprintf(ap_password, "%s", ap_pass);
}

void deleteConfig() {
  if (SPIFFS.begin()) {
    if (SPIFFS.exists(_configFile)) {
      SPIFFS.remove(_configFile);
    }
  }
}

void connectWifi() {
  /* WiFi Manager automatically connects using the saved credentials, if that fails it will go into AP mode */
  wifiManager.setMinimumSignalQuality(10);
  wifiManager.setAPCallback(configModeCallback);
  wifiManager.autoConnect(ap_name, ap_password);
}

void setup()
{
  pinMode(1, INPUT_PULLUP); //down
  pinMode(3, INPUT_PULLUP); //left
  pinMode(5, INPUT_PULLUP); //center
  pinMode(12, INPUT_PULLUP); //right
  pinMode(10, INPUT_PULLUP); //up

  byte reading =  (digitalRead(1)  == 0 ? 0 : (1 << 0)) | //down
                  (digitalRead(3)  == 0 ? 0 : (1 << 1)) | //left
                  (digitalRead(5)  == 0 ? 0 : (1 << 2)) | //center
                  (digitalRead(12) == 0 ? 0 : (1 << 3)) | //right
                  (digitalRead(10) == 0 ? 0 : (1 << 4)); //up

  
  Serial.begin(115200);
  Serial.println();
  Serial.println();
  Serial.println("Shadowbadge starting...");
  Serial.println(ESP.getFreeHeap());
  Serial.println();

  // client and https pieces
  client.setInsecure();
  client.setBufferSizes(minSslBuffer, maxSslBuffer);

  // override max power setting (20.5)
  WiFi.setOutputPower(16);

  // initid display
  display.init();

  /*
     CONTROLS:
     When turned on the position of the joystick represents what the badge will do:
     up: delete configuration
     right: keep configuration but reconfigure wifi
     left: wifi update server
     center: refresh information from app.shadowcloud.badge

     IF NO BUTTON IS PRESSED:
     - load configuration
     - if no configuration do wifi thing
     - if not claimed to claim thing
  */
  boolean pressUp = bitRead(reading, 4) == 0;
  boolean pressDown = bitRead(reading, 0) == 0; // does not appear to work right at start, may not be the right pin
  boolean pressLeft = bitRead(reading, 1) == 0;
  boolean pressRight = bitRead(reading, 3) == 0;
  boolean pressCenter = bitRead(reading, 2) == 0;
  
  // up: delete configuration
  if ( pressUp ) {
    deleteConfig();
  }

  // up or right: remove wifi settings
  if ( pressUp || pressRight ) {
    wifiManager.resetSettings();
  }

  // load initial configuration and salt
  bootstrapConfig();

  // if no owner id is present, go through claim logic
  // or if the status is NONE, CLAIMED, or UNCLAIMED
  if (strlen(owner_id) < 1 || strcmp("CLAIMED", badge_status) == 0 || strcmp("UNCLAIMED", badge_status) == 0 || strcmp("NONE", badge_status) == 0) {
    needsClaim = true;
  }

  // up center left right: need wifi
  if (pressUp || pressLeft || pressCenter || pressRight || needsClaim ) {
    // these modes require a configured wifi
    connectWifi();
  }

  // pressing left or down puts you in OTA update mode
  if (pressLeft || pressDown) {
    ESP8266WebServer httpServer(port);
    ESP8266HTTPUpdateServer httpUpdater;
    httpUpdater.setup(&httpServer);
    httpServer.begin();
    showIP();
    while (1) {
      httpServer.handleClient();
    }
  }   

  // pressing center starts refresh mode (this will also happen after initial claim if no claim is made)
  if (pressCenter || needsClaim) {
    needsRefresh = true;
  }

}

void updateBadgeInfo() {
  // leave and set status if the retry count is too high
  if (configuredRetryCount > maxConfiguredRetryCount) {
    needsRefresh = false;
    needsConnectIconRedraw = true;
    return;
  }
  
  // do not do if not connected to wifi
  if (WiFi.status() != WL_CONNECTED) {
    return;
  }

  // save old hash to compare with after update
  char old_hash[50];
  strlcpy(old_hash, badge_hash, 50);
  
  // no claim redraw once update has started
  needsClaim = false;

  //Serial.println("Created wifi client, connecting...");

  // since a connection is going to be attempted on an 
  // already-configured badge we start tracking the retry count
  if (strcmp("CONFIGURED", badge_status) == 0) {
    // increment configured retry count
    configuredRetryCount++;
  }

  // connect and bail if connection fails
  if (!client.connect(applicationHost, applicationPort)) {
    char buff[64];
    client.getLastSSLError(buff, 64);
    Serial.println(buff);
    
    client.stop();
    return;
  }
 
  // build full url
  char url[512];
  sprintf(url, "/badges/%s", uuid);

  Serial.printf("Connecting to %s with secret %s\n", url, secret);

  // send http get for url
  client.printf("GET %s HTTP/1.1\n", url);
  client.printf("Host: %s\n", applicationHost);
  client.printf("X-Shadowbadge-Secret: %s\n", secret);
  client.println("Connection: close\n");
  client.println();

  // read headers
  while (client.connected()) {
    String line = client.readStringUntil('\n');
    if (line == "\r") {
      break;
    }
  }

  // can't read payload
  if (!client.connected()) {
    client.stop();
    return;
  }

  // parse payload into json
  DynamicJsonBuffer jsonBuffer;
  JsonObject& root = jsonBuffer.parseObject(client);

  if(!root.success()){
    Serial.println("JSON parsing failed!");
    client.stop();
    return;
  } else {
    //root.printTo(Serial);
  }

  // read config from json object
  readJsonConfig(root, false);

  client.stop();

  // and write config to disk
  writeConfig();

  // set a single value for if the hash has changed
  boolean changed = strcmp(old_hash, badge_hash) != 0;  

  // set refresh done and redraw needed
  if (strcmp("CONFIGURED", badge_status) == 0) {
    needsRefresh = !changed;
    needsRedraw = changed; 
  } else if (strcmp("CLAIMED", badge_status) == 0) {
    needsRefresh = true;
    needsRedraw = changed;
  } else if (strcmp("UNCLAIMED", badge_status) == 0) {
    needsRedraw = true;
    needsRefresh = true;
  } else {
    needsRedraw = false;
    needsRefresh = true;
  }
}

void updateBadgeInfoProcessorBoost() {
  // clock to 160mhz
  system_update_cpu_freq(HIGH_FREQ);
  yield();

  // do badge thing
  updateBadgeInfo();

  // step back down
  system_update_cpu_freq(LOW_FREQ);
  yield();
}

void cycleIcon() {
  if (needsRedraw) {
    return;
  }
  
  if (strcmp("SHADOWMAN", badge_icon) == 0) {
    strcpy(badge_icon, "RED_HAT");
  } else {
    strcpy(badge_icon, "SHADOWMAN");
  }

  // config has deviated
  strcpy(badge_hash, "");
  
  writeConfig();
  
  needsRedraw = true;              
}

void cycleStyle() {
  if (needsRedraw) {
    return;
  }
  
  if (strcmp("ICON_LEFT", badge_style) == 0) {
    strcpy(badge_style, "ICON_RIGHT");
  } else {
    strcpy(badge_icon,"ICON_LEFT");
  }

  // config has deviated
  strcpy(badge_hash, "");
  
  writeConfig();
  
  needsRedraw = true;
}

void loop()
{
  byte reading =  (digitalRead(1)  == 0 ? 0 : (1 << 0)) | //down
                  (digitalRead(3)  == 0 ? 0 : (1 << 1)) | //left
                  (digitalRead(5)  == 0 ? 0 : (1 << 2)) | //center
                  (digitalRead(12) == 0 ? 0 : (1 << 3)) | //right
                  (digitalRead(10) == 0 ? 0 : (1 << 4)); //up

  if (reading != lastButtonState) {
    lastDebounceTime = millis();
  }

  // only read button presses if we aren't doing something already
  // and sufficient time has passed
  if ((millis() - lastDebounceTime) > debounceDelay) {
    // and the button state has changed
    if (reading != buttonState) {
      buttonState = reading;
      for (int i = 0; i < 5; i++) {
        if (bitRead(buttonState, i) == 0) {
          switch (i) {
            case 0:
              // don't use this, down is weird at runtime on this board
              break;
            case 1:
              // don't use this, left is weird at runtime on this board
              break;
            case 2:
              // if the user presses the center button add update to the loop
              lastRefreshTime = 0;
              configuredRetryCount = 0;
              needsRefresh = true;
              needsConnectIconRedraw = true;
              break;
            case 3:
              //do something when the user presses right
              break;
            case 4:
              //do something when the user presses up
              cycleIcon();
              break;
            default:
              break;
          }
        }
      }
    }
  }
  lastButtonState = reading;

  // =======================================
  // State machine based on global state
  // TODO:
  // - create state map
  // - refactor to use state transitions
  // =======================================

  if (needsClaim) {
    showClaimInfo();      
    needsClaim = false;
    needsRedraw = false;
    needsRefresh = true;
  }

  // draw connect icon if needed which causes a whole refresh
  // but has the benefit of letting you know that you actually
  // did something
  if (needsConnectIconRedraw && !needsRedraw) {
    wifiIcon(needsRefresh, true);
    needsConnectIconRedraw = false;
  }

  // update status on refresh
  if (needsRefresh && millis() - lastRefreshTime > refreshDelay) {
    // connect if needed
    if (WiFi.status() != WL_CONNECTED) {
      connectWifi();
    }

    // update badge info using processor boost
    updateBadgeInfoProcessorBoost();

    // update refresh time
    lastRefreshTime = millis();
  }

  // show badge info (finally!)
  if (needsRedraw) {
    if(strcmp("CLAIMED", badge_status) == 0) {
      showClaimed(); 
    } else {
      showBadgeInfo();
    }
    needsRedraw = false;
    needsConnectIconRedraw = false;
  }

  // disconnect from wifi and (maybe) some sort of sleep
  if (!needsRefresh) {
    // todo: implement additional power management somehow
  }

  // this should, at a bare minimum, save *some* power
  delay(LOOP_DELAY);
}

// i'm not sure this is necessary if you know how to do 
// better mask stuff in the writeBitmap method but since
// i can't think that way this was implemented
unsigned char reverse(unsigned char b) {
  b = (b & 0xF0) >> 4 | (b & 0x0F) << 4;
  b = (b & 0xCC) >> 2 | (b & 0x33) << 2;
  b = (b & 0xAA) >> 1 | (b & 0x55) << 1;
  return b;
}

void writeBitmap(const unsigned char *bitmap, uint16_t x, uint16_t y, uint16_t w, uint16_t h, int color, boolean invert) {
  uint16_t cx = 0;
  uint16_t cy = 0;

  unsigned char mask;

  int ib = 0;
  while(true) {
    mask = reverse(bitmap[ib++]);
    if (invert) {
      mask = ~mask;
    }
    for (unsigned char bit_index = 0; bit_index < 8; bit_index++) {
      if (mask & 1 > 0) {
        display.drawPixel(cx + x, cy + y, color);
      }
      // move one right
      cx++;
      if (cx >= w) {
        cy++;
        cx = 0;
        break;
      }
      // shift mask
      mask >>= 1;
    }
    if (h <= cy) {
      break;
    }
  }
}

// draws two bitmaps into the same space so that one can overlay the other without two draw calls
void writeTwo(const unsigned char *bitmap1, const unsigned char *bitmap2, uint16_t x, uint16_t y, uint16_t w, uint16_t h, uint16_t color1, uint16_t color2, boolean invert) {
  uint16_t cx = 0;
  uint16_t cy = 0;

  unsigned char mask1;
  unsigned char mask2;
  
  int ib = 0;
  while(true) {
    mask1 = reverse(bitmap1[ib]);
    mask2 = reverse(bitmap2[ib]);
    ib++;
    if (invert) {
      mask1 = ~mask1;
      mask2 = ~mask2;
    }
    for (unsigned char bit_index = 0; bit_index < 8; bit_index++) {
      if (mask2 & 1 > 0) {
        display.drawPixel(cx + x, cy + y, color2);
      } else if (mask1 & 1 > 0) {
        display.drawPixel(cx + x, cy + y, color1);
      }
      // move one right
      cx++;
      if (cx >= w) {
        cy++;
        cx = 0;
        break;
      }
      // shift masks
      mask1 >>= 1;
      mask2 >>= 1;
    }
    if (h <= cy) {
      break;
    }
  }
}

// show shadowman at x/y in
void showShadowmanIcon(uint16_t x, uint16_t y) {
  writeTwo(bwshadowman, oldredhat, x, y, 50, 38, GxEPD_BLACK, GxEPD_RED, true);
}

void showRedHatIcon(uint16_t x, uint16_t y) {
  writeBitmap(newredhat, x, y, 45, 38, GxEPD_RED, false);
}

void showQR(uint16_t ix, uint16_t iy, uint16_t box_size, uint16_t qr_version, uint16_t err_correction, const char *data, bool forcewrite) {
  uint16_t box_x = ix;
  uint16_t box_y = iy;
  uint16_t init_x = box_x;

  // The structure to manage the QR code
  QRCode qrcode;

  // Allocate a chunk of memory to store the QR code
  uint8_t qrcodeBytes[qrcode_getBufferSize(qr_version)];

  qrcode_initText(&qrcode, qrcodeBytes, qr_version, err_correction, data);

  // display bitmap
  for (uint8_t y = 0; y < qrcode.size; y++) {
    // Each horizontal module
    for (uint8_t x = 0; x < qrcode.size; x++) {
      if (qrcode_getModule(&qrcode, x, y)) {
        display.fillRect(box_x, box_y, box_size, box_size, GxEPD_BLACK);
      } else if (forcewrite) {
        display.fillRect(box_x, box_y, box_size, box_size, GxEPD_WHITE);
      }
      box_x = box_x + box_size;
    }
    box_y = box_y + box_size;
    box_x = init_x;
  }
}

void configModeCallback (WiFiManager *myWiFiManager) {
  display.setRotation(3); //even = portrait, odd = landscape
  display.fillScreen(GxEPD_WHITE);

  // red hat icon in bottom left
  showRedHatIcon(5, 85);

  // print what's going on
  const GFXfont* f = &overpass_mono_bold8pt7b ;
  display.setTextColor(GxEPD_BLACK);
  display.setFont(f);
  display.setCursor(5, 15);
  display.printf("SSID: %s", ap_name);
  display.setCursor(5, 35);
  display.printf("Pass: %s", ap_password);

  // create QR code for access using this nifty text code
  char wifi[256];
  sprintf(wifi, "WIFI:T:WPA;S:%s;P:%s;;", ap_name, ap_password);
  showQR(200, 38, 2, 7, ECC_QUARTILE, wifi, false); 
  
  display.update();
}

void showText(const char *text)
{
  display.setRotation(3); //even = portrait, odd = landscape
  display.fillScreen(GxEPD_WHITE);
  const GFXfont* f = &overpass_mono_bold8pt7b ;
  display.setTextColor(GxEPD_BLACK);
  display.setFont(f);
  display.setCursor(10, 70);
  display.println(text);
  display.update();
}

void showIP() {
  display.setRotation(3); //even = portrait, odd = landscape
  display.fillScreen(GxEPD_WHITE);
  const GFXfont* f = &overpass_mono_bold8pt7b ;
  display.setTextColor(GxEPD_BLACK);
  display.setFont(f);
  display.setCursor(0, 10);

  String url = WiFi.localIP().toString() + ":" + String(port) + "/update";
  byte charArraySize = url.length() + 1;
  char urlCharArray[charArraySize];
  url.toCharArray(urlCharArray, charArraySize);

  display.println("You are now connected!");
  display.println("");
  display.println("Go to:");
  display.println(urlCharArray);
  display.println("to upload firmware.");
  display.update();
}

// this is used to print individual lines and unbolds them 
// if and when they are over 21 characters to squeeze a little
// more space out
void printBadgeLine(int x, int y, char* display_line, uint8_t color) {

  // the threshold size is 18 characters
  const GFXfont* f = &overpass_mono_bold8pt7b;
  if (strlen(display_line) >= 18) {
    f = &overpass_mono_regular8pt7b;
  }

  display.setFont(f);
  display.setTextColor(color);
  display.setCursor(x, y);
  display.print(display_line);
}

void showBadgeInfo()
{
  display.setRotation(3); //even = portrait, odd = landscape
  display.fillScreen(GxEPD_WHITE);

  int heading_x = 3;
  int icon_x = display.width() - 53;
  int icon_y = 0;

  if (strcmp("ICON_LEFT", badge_style) == 0) {
    // write curved header thing
    writeBitmap(headercurve, 45, 0, 21, 37, GxEPD_RED, true);
    // and draw header box
    display.fillRect(45 + 21, 0, display.width(), 37, GxEPD_RED);
    // move icon and heading
    icon_x = 3;
    heading_x = 60;
  } else {
    // write curved header thing
    writeBitmap(headercurve2, display.width() - 50 - 21, 0, 21, 37, GxEPD_RED, true);
    // and draw header box
    display.fillRect(0, 0, display.width() - 50 - 21, 37, GxEPD_RED);
  }

  // draw icon as needed but without update beacuse this method handles that
  wifiIcon(needsRefresh, false);

  int heading_y = 26;
  int head_len = strlen(display_heading);
  const GFXfont* f = &overpass_mono_bold12pt7b;

  // this might be a little janky but attempting to adjust the name field for UP TO 20 character names
  if (head_len >= 20) {
    f = &overpass_mono_bold8pt7b;
    heading_y -= 4;
  } else if (head_len >= 14) {
    f = &overpass_mono_bold10pt7b;
    heading_y -= 2;
  }
  
  display.setTextColor(GxEPD_WHITE);
  display.setFont(f);
  display.setCursor(heading_x, heading_y);
  display.println(display_heading);

  // custom print function that can somewhat auto-adjust the font size  
  printBadgeLine(3, 63, display_title, GxEPD_BLACK);
  printBadgeLine(3, 83, display_group, GxEPD_BLACK);
  printBadgeLine(3, 103, display_loc, GxEPD_BLACK);
  printBadgeLine(3, 123, display_info, GxEPD_BLACK);
  
  // qr code, allow it to be turned off in the web ui
  if (strcmp("NONE", qr_type) != 0) {
    int x = 222;
    int y = 54;    
    
    // qr version 5 can store 106 bytes at ECC_LOW
    //                        84 at ECC_MEDIUM
    //                        60 at ECC_QUARTILE
    //                        44 at ECC_HIGH
    // we auto-adjust because higher error correction
    // also makes it easier to read on the small screen
    int url_len = strlen(qr_url);
    uint8_t ecc = ECC_LOW;
    uint8_t qr_ver = 5;
    uint8_t qr_block_size = 2;

    // qr version 5 supports up to 106 characters
    if (url_len < 106) {
      if (url_len < 44) {
        ecc = ECC_HIGH;
      } else if (url_len < 60) {
        ecc = ECC_QUARTILE;
      } else if (url_len < 84) {
        ecc = ECC_MEDIUM;
      }
    } else {
      // otherwise we double the size of the qr code and half
      // the block size
      qr_ver = 14;
      qr_block_size = 1;

      // it's also 73x73 instead of 74x74 (ver=5, size=2)
      // so we adjust the x/y
      x += 1;
      y += 1;

      if (url_len < 194) {
        ecc = ECC_HIGH;
      } else if (url_len < 258) {
        ecc = ECC_QUARTILE;
      } else if (url_len < 362) {
        ecc = ECC_MEDIUM;
      }      
    }
    
    // show the QR code
    showQR(x, y, qr_block_size, qr_ver, ecc, qr_url, false);
  }

  // show shadowman
  if (strcmp("SHADOWMAN", badge_icon) == 0) {
    showShadowmanIcon(icon_x, 0);
  } else {
    showRedHatIcon(icon_x, 0);
  }

  // update/draw
  display.update();
}

void showClaimed() {
  display.setRotation(3); //even = portrait, odd = landscape
  display.fillScreen(GxEPD_WHITE);

  // drawing it but black so it is faster
  writeBitmap(newredhat, display.width() - 53, display.height() - 43, 45, 38, GxEPD_BLACK, false);

  const GFXfont* f = &overpass_mono_bold8pt7b;
  display.setTextColor(GxEPD_BLACK);
  display.setFont(f);
  display.setCursor(3, 10);
  display.print(ap_name);
  display.setCursor(3, 35);
  display.println("Waiting for update...");

  // update/draw
  display.update();
}

void wifiIcon(boolean show, boolean doUpdate) {
  int x = display.width() - 23;
  
  // decide color based on show status, a !show just overwrites a show
  int color = GxEPD_BLACK;
  if (!show) {
    color = GxEPD_WHITE;
  }
  writeBitmap(connecticon, x, 35, 21, 21, color, true);

  if (doUpdate) {
    // values here were discovered experimentally until it looked right
    display.update();
  }
}

void showClaimInfo() {
  display.setRotation(3); //even = portrait, odd = landscape
  display.fillScreen(GxEPD_WHITE);

  // red hat icon in bottom left, drawing it black so it is faster
  writeBitmap(newredhat, 5, 85, 45, 38, GxEPD_BLACK, false);
  
  // create url
  char url[2048];
  sprintf(url, "%s/badges/%s/claimAction?secret=%s", applicationRoot, uuid, secret);
  showQR(200, 38, 2, 7, ECC_LOW, url, false);

  const GFXfont* f = &overpass_mono_bold8pt7b;
  display.setTextColor(GxEPD_BLACK);
  display.setFont(f);
  display.setCursor(5, 15);
  display.println("Scan QR to claim badge");

  // update/draw
  display.update();
}
