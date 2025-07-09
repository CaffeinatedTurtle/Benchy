/*
Benchy esp32 BLE controller for CT Benchy
This version is for the CT Benchy with a single motor and rudder controlled by a  4 channel
remote control,  channel 2 is the throttle, channel 1 is the rudder. channel 3 is a toggle switch
on and off for the motor sound, channel 4 is momentary switch short press to sound horn, long press to
toggle LEDs on and off.

*/


#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>


#include <Preferences.h>
#include "driver/gpio.h"
#include "driver/rmt.h"

#include "XT_DAC_Audio.h"
#include "soundMotor.h"
#include "toot.h"
#include "benchy.h"

Preferences preferences;

// RMT configuration for receiving button presses on GPIOs 12 and 13
// GPIO 12: connected to CHAN_3 (toggle ON/OFF, double click)
// GPIO 13: connected to CHAN_4 (short/long press)
#define RMT_RX_CHANNEL_1 RMT_CHANNEL_0
#define RMT_RX_GPIO_NUM_1 12

#define RMT_RX_CHANNEL_2 RMT_CHANNEL_1
#define RMT_RX_GPIO_NUM_2 13

#define RMT_CLK_DIV 80  // 1 μs per tick (80 MHz / 80)

#define PRESS_THRESHOLD_FREQ 750.0
#define DOUBLE_CLICK_THRESHOLD_MS 1000
#define LONG_PRESS_DURATION_MS 1500

#define LED_GREEN_PIN GPIO_NUM_5
#define LED_RED_PIN GPIO_NUM_21
#define LED_WHITE_PIN GPIO_NUM_19

#define LED_GREEN 1
#define LED_RED 2
#define LED_WHITE 4
#define HORN 8
#define MOTOR 16



enum ButtonState {
  BUTTON_OFF,
  BUTTON_ON,              
  BUTTON_PRESSED_SHORT,   
  BUTTON_PRESSED_LONG
};

boolean connected = false;



#define SERVICE_UUID "7507cee3-db32-4e5a-bd6b-96b62887129e"
#define BENCHY_CHARACTERISTIC_UUID "d7c1861c-beff-430f-9a72-fc05c6cc997d"


BLEServer *pServer;
BLEService *pService;
BLEUUID ClientNotifyDescriptorUuid = BLEUUID((uint16_t)0x2902);
BLECharacteristic *pBenchyCharacteristic; // characteristic for Benchy data
BLEDescriptor benchyNotifyDescriptor = BLEDescriptor(ClientNotifyDescriptorUuid, 100);


uint8_t ledFlags = 0;

uint8_t mode = 0;
uint8_t led = 0;


bool soundHorn = true;
bool soundingHorn = false;
bool motorSound = false;

bool ledOn = false;

bool rcConnected = false;

XT_Wav_Class MotorSound(motor_wav);

XT_Wav_Class HornSound(horn_wav);

void setBenchy(uint8_t *newValue)
{

  pBenchyCharacteristic->setValue(newValue, sizeof(Benchy_t));
  pBenchyCharacteristic->indicate();
}


uint8_t getBenchy()
{
  uint8_t *dataPtr = pBenchyCharacteristic->getData();
  size_t datalength = pBenchyCharacteristic->getLength();
  return *dataPtr;
}

void writeLed(uint8_t value)
{
  led = value;
  if (led & LED_RED)
  {
    gpio_set_level((gpio_num_t)LED_RED_PIN, 1);
  }
  else
  {
    gpio_set_level((gpio_num_t)LED_RED_PIN, 0);
  }
  if (led & LED_GREEN)
  {
    gpio_set_level((gpio_num_t)LED_GREEN_PIN, 1);
  }
  else
  {
    gpio_set_level((gpio_num_t)LED_GREEN_PIN, 0);
  }
  if (led & LED_WHITE)
  {
    gpio_set_level((gpio_num_t)LED_WHITE_PIN, 1);
  }
  else
  {
    gpio_set_level((gpio_num_t)LED_WHITE_PIN, 0);
  }
}

void setLed(uint8_t value)
{
  writeLed(value);
  
}

void loadPreferences()
{
  mode = preferences.getUChar("mode", 0);
  Serial.printf("loaded preferences mode = %d\n", mode);
}

void initLed()
{
  gpio_pad_select_gpio(LED_RED);
  gpio_set_direction((gpio_num_t)LED_RED_PIN, GPIO_MODE_OUTPUT);
  gpio_pad_select_gpio((gpio_num_t)LED_GREEN_PIN);
  gpio_set_direction((gpio_num_t)LED_GREEN_PIN, GPIO_MODE_OUTPUT);
  gpio_pad_select_gpio(LED_WHITE_PIN);
  gpio_set_direction((gpio_num_t)LED_WHITE_PIN, GPIO_MODE_OUTPUT);
}
const char* stateToString(ButtonState state) {
  switch (state) {
    case BUTTON_OFF: return "OFF";
    case BUTTON_ON: return "ON";
    case BUTTON_PRESSED_SHORT: return "SHORT_PRESS";
    case BUTTON_PRESSED_LONG: return "LONG_PRESS";
    default: return "UNKNOWN";
  }
}

struct ButtonTracker {
  bool wasBelowThreshold = true;
  unsigned long pressStartTime = 0;
  ButtonState state = BUTTON_OFF;
  ButtonState prevState = BUTTON_OFF;
};

ButtonTracker button12, button13;

// Store latest raw pulse durations
volatile uint16_t gpio12_high_us = 0, gpio12_low_us = 0;
volatile uint16_t gpio13_high_us = 0, gpio13_low_us = 0;

void setupRMT(gpio_num_t gpio, rmt_channel_t channel) {
  rmt_config_t config = RMT_DEFAULT_CONFIG_RX(gpio, channel);
  config.clk_div = RMT_CLK_DIV;
  config.rx_config.filter_en = true;
  config.rx_config.filter_ticks_thresh = 100;
  config.rx_config.idle_threshold = 10000;
  rmt_config(&config);
  rmt_driver_install(config.channel, 2048, 0);
}

void handleClick() {
  Serial.println("Button clicked");

  if (motorSound){
    motorSound= false;
  } else {
    motorSound = true;
  }
  // Add your click handling logic here
}
void handleShortPress() {
  Serial.println("Button short pressed");
  rcConnected=true;

  // Add your short press handling logic here
  if (soundHorn == true  && !soundingHorn) {
    soundHorn = false;
  } else {
    soundHorn = true; 
  }
  
}
void handleLongPress() {
  Serial.println("Button long pressed");
  rcConnected=true;
  uint8_t value  =0;
  if (ledOn == false){
    ledOn = true;
    value = LED_RED | LED_GREEN | LED_WHITE; // Turn on all LEDs
    
  } else {
    ledOn = false;
    value = 0 ;// Turn off all LEDs
  }

  writeLed(value); // Toggle all LEDs
  led=value;
  // Add your long press handling logic here
}
void handleDoubleClick() {
  Serial.println("Button double clicked leds on ");
  rcConnected=true;
  uint8_t value  =led ^ (LED_RED | LED_GREEN | LED_WHITE);
  writeLed(value); // Toggle all LEDs
  led=value;
  // Add your double click handling logic here
}

// CHAN_3: toggle ON/OFF, double click
void handleToggleFrequency(float freq, ButtonTracker &tracker)
{
    unsigned long now = millis();
    unsigned long pressDuration = now - tracker.pressStartTime;
    ButtonState newState = freq >= PRESS_THRESHOLD_FREQ ? BUTTON_ON : BUTTON_OFF;
    if (newState != tracker.state)
    {
       printf("chan 4 button pressed %s %s %u %u %u \n", 
        stateToString(newState),stateToString(tracker.state), pressDuration,now,tracker.pressStartTime);
        if (pressDuration < DOUBLE_CLICK_THRESHOLD_MS)
        {
           handleDoubleClick();
            
        } else {
          handleClick();
          
         
        }      
        tracker.prevState = tracker.state; // Store previous state
        tracker.pressStartTime = now; // Update press start time
    }

    tracker.state = newState; 
  
}

// CHAN_4: short/long press
void handlePressFrequency(float freq, ButtonTracker &tracker) {
  unsigned long now = millis();
  
 
   if (freq >= PRESS_THRESHOLD_FREQ) {
     tracker.state = BUTTON_ON;  // Set to ON state
    
  
     if (tracker.wasBelowThreshold) {
     tracker.wasBelowThreshold = false;
      tracker.pressStartTime = now;
    }
    // Keep state unchanged while holding
  } else {
   
    if (!tracker.wasBelowThreshold) {
      // Falling edge — button released
      tracker.wasBelowThreshold = true;
      unsigned long pressDuration = now - tracker.pressStartTime;
      if (pressDuration < LONG_PRESS_DURATION_MS) {
       tracker.state = BUTTON_PRESSED_SHORT;
       handleShortPress();
      } else {
        tracker.state = BUTTON_PRESSED_LONG;
        handleLongPress();
      }
        tracker.pressStartTime = 0;  // Reset start time
  
    } else {
      // Signal still low — no press happening
       tracker.state = BUTTON_OFF;
    }
  }
}

void processRMT(rmt_channel_t channel, ButtonTracker &tracker, volatile uint16_t &high_us, volatile uint16_t &low_us, void (*handler)(float, ButtonTracker&)) {
  RingbufHandle_t rb = NULL;
  rmt_get_ringbuf_handle(channel, &rb);
  rmt_rx_start(channel, true);

  size_t rx_size;
  rmt_item32_t* item;
  while ((item = (rmt_item32_t*)xRingbufferReceive(rb, &rx_size, pdMS_TO_TICKS(10))) != NULL) {
    int high = item->duration0;
    int low  = item->duration1;
    int period = high + low;

    if (period == 0) {
      vRingbufferReturnItem(rb, (void*)item);
      continue;
    }

    float freq = 1000000.0 / period;

    high_us = high;
    low_us = low;

    handler(freq, tracker);

    vRingbufferReturnItem(rb, (void*)item);
  }
}

void printButtonStatesIfChanged() {
  if (button12.state != button12.prevState || button13.state != button13.prevState) {
    Serial.printf("[CHAN_3] State: %s\n", stateToString(button12.state));
    Serial.printf("[CHAN_4] State: %s\n", stateToString(button13.state));
    button12.prevState = button12.state;
    button13.prevState = button13.state;
  }
}

unsigned long lastPrintTime = 0;
void printRMTValues() {
  unsigned long now = millis();
  if (now - lastPrintTime >= 500) {
    lastPrintTime = now;

    float freq12 = (gpio12_high_us + gpio12_low_us) > 0 ? 1000000.0 / (gpio12_high_us + gpio12_low_us) : 0;
    float duty12 = (gpio12_high_us + gpio12_low_us) > 0 ? 100.0 * gpio12_high_us / (gpio12_high_us + gpio12_low_us) : 0;

    float freq13 = (gpio13_high_us + gpio13_low_us) > 0 ? 1000000.0 / (gpio13_high_us + gpio13_low_us) : 0;
    float duty13 = (gpio13_high_us + gpio13_low_us) > 0 ? 100.0 * gpio13_high_us / (gpio13_high_us + gpio13_low_us) : 0;

    Serial.printf("CHAN_3: High=%dus, Low=%dus, Freq=%.2fHz, Duty=%.2f%% state:%s prev:%s\n",
                 gpio12_high_us, gpio12_low_us, freq12, duty12, 
                 stateToString(button12.state),stateToString(button12.prevState));
    Serial.printf("CHAN_4: High=%dus, Low=%dus, Freq=%.2fHz, Duty=%.2f%% state:%s\n",
                  gpio13_high_us, gpio13_low_us, freq13, duty13,stateToString(button13.state));
  }
}


void setup()
{

  preferences.begin("CTBenchy", false);

  Serial.begin(115200);
  Serial.println("Starting BLE Server!");

  BLEDescriptor ClientNotifyDescriptor = BLEDescriptor(ClientNotifyDescriptorUuid, 100);
  BLEDevice::init("CTBenchy");
  pServer = BLEDevice::createServer();
  pService = pServer->createService(SERVICE_UUID);

  pBenchyCharacteristic = pService->createCharacteristic(
      BENCHY_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
      BLECharacteristic::PROPERTY_WRITE |
      BLECharacteristic::PROPERTY_NOTIFY |
      BLECharacteristic::PROPERTY_INDICATE);

  pBenchyCharacteristic->addDescriptor(&benchyNotifyDescriptor);
  pBenchyCharacteristic->setCallbacks(new BenchyCallbacks());



  pService->start();
  BLEDevice::setPower(ESP_PWR_LVL_P9, ESP_BLE_PWR_TYPE_DEFAULT);
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06); // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);

  printf("Advertising started!\n");

  BLEDevice::startAdvertising();

  Serial.println("Load Preferences!");

  loadPreferences();

  Serial.println("Init Led!");

  initLed();
  setLed(0);

  Serial.println("Characteristic defined! Now you can read it in the Client!");

  // Initialize RMT for button presses
  setupRMT((gpio_num_t)RMT_RX_GPIO_NUM_1, RMT_RX_CHANNEL_1);
  setupRMT((gpio_num_t)RMT_RX_GPIO_NUM_2, RMT_RX_CHANNEL_2);
}






int initCount = 0;
int ledFlash= 0;
long flashtime = 0;
long debugtime = 0;

// Main loop
void loop()
{

uint8_t newled;

processRMT(RMT_RX_CHANNEL_1, button12, gpio12_high_us, gpio12_low_us, handleToggleFrequency);
processRMT(RMT_RX_CHANNEL_2, button13, gpio13_high_us, gpio13_low_us, handlePressFrequency);
static XT_DAC_Audio_Class DacAudio(25, 3); // declare DacAudio object
 
if (connected){
 
  uint8_t newLed = benchy.op.switch_value;

  if (newLed != led)
  {
    printf("led changed from %0x to %0x\n", led, newLed);
    setLed(newLed);
    if (newLed & HORN && soundHorn == false)
      soundHorn = true;
    else
      soundHorn = false;

    if (newLed & MOTOR)
      motorSound = true;
    else
      motorSound = false;
    // acknowledge the change unless the horn is being sounded
    if (!soundHorn) setBenchy((uint8_t *)&benchy); // send the new value
    printBenchy(benchy, "Benchy updated in loop");
     
  }



}

  // play sound
   DacAudio.FillBuffer();                     // Fill the sound buffer with data
  if (motorSound && MotorSound.Playing == false)
  { // if not playing,
    MotorSound.Repeat;
    DacAudio.DacVolume=127;
    DacAudio.Play(&MotorSound);
  } 
  if (MotorSound.Playing == true  && !motorSound)
  {
    DacAudio.StopAllSounds(); // stop the sound if motorSound is false  
  }


  if (soundHorn && HornSound.Playing == false)
  {
    // start playing horn sound
    DacAudio.DacVolume=127;
    DacAudio.Play(&HornSound,true);
    soundHorn=false;
    soundingHorn = true;   
  }
  if (soundingHorn && HornSound.Playing == false)
{
    // sounding horn has finished
    soundingHorn = false;
    benchy.op.switch_value &= ~HORN; // clear the horn bit
  
    setBenchy((uint8_t *)&benchy); // send the new value
    //printBenchy(benchy);
}

  long t1 = millis();


  if ((t1 -debugtime)>500  && t1 != debugtime)
  { // print a debug every 1/2 second
  debugtime=t1;
    if (pServer->getConnectedCount() <= 0 && connected)
    {
      connected = false;
    
      printf("disconnected\n");
      pServer->startAdvertising();
    }
    else if (pServer->getConnectedCount() == 0)
    {
      connected = false;
    }
    else
    {
      connected = true;
    }
    if (connected)
    {
      if (initCount < 5 && initCount >= 0)
      {
        setLed(0x7);
        initCount++;
      }
      else
      {
        if (initCount >= 5)
        {
          initCount = -1;
          setLed(0x0);
        }
      }
      Serial.printf("ledRed: %1x ledWhite %1x ledGreen %1xmode:%03d  connected%02d\r", led & LED_RED, led & LED_WHITE, led & LED_GREEN, mode, pServer->getConnectedCount());
    }
  }
  if ((t1-flashtime) > 1000  && t1 != flashtime && !connected && !rcConnected)
  {
    flashtime = t1;

    if (!connected){
    if (ledFlash == 0)
      ledFlash = LED_GREEN;
    else if (ledFlash == LED_GREEN)
      ledFlash = LED_WHITE;
    else if (ledFlash == LED_WHITE)
      ledFlash = LED_RED;
    else if (ledFlash == LED_RED)
      ledFlash = LED_GREEN;
    Serial.printf("adverising :%s %d %d\r", "CTBenchy",ledFlash,t1);
    writeLed(ledFlash);
 
  } 
  }
 
}