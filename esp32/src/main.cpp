#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

#include <Servo.h>

#include <Preferences.h>
#include "driver/gpio.h"


#include "XT_DAC_Audio.h"
#include "soundMotor.h"
#include "soundHorn.h"


Preferences preferences;

#define MAX_THROTTLE 180
#define MIN_THROTTLE 0

#define MAX_RUDDER 110
#define RUDDER_OFFSET 7
#define MIN_RUDDER 0

#define MODE_UNI 0
#define MODE_BI 1
#define MODE_PROGRAM 2

#define RUDDER_PIN 13
#define THROTTLE_PIN 12

#define LED_GREEN_PIN GPIO_NUM_5
#define LED_RED_PIN GPIO_NUM_21
#define LED_WHITE_PIN GPIO_NUM_19

#define LED_GREEN 1
#define LED_RED 2
#define LED_WHITE 4
#define HORN 8

int rudderPin = RUDDER_PIN;
int throttlePin = THROTTLE_PIN;

boolean connected = false;

Servo rudder;
Servo throttle;

#define SERVICE_UUID "7507cee3-db32-4e5a-bd6b-96b62887129e"
#define RUDDER_CHARACTERISTIC_UUID "d7c1861c-beff-430f-9a72-fc05c6cc997d"
#define THROTTLE_CHARACTERISTIC_UUID "87607759-37d1-41b5-b2c8-c44b7c746083"
#define MODE_CHARACTERISTIC_UUID "16d68508-2fd4-40a9-ba61-aac41cb81e45"
#define LED_CHARACTERISTIC_UUID "3a84a192-d522-46ef-b7c8-36b9fc062490"

BLEServer *pServer;
BLEService *pService;
BLEUUID ClientNotifyDescriptorUuid = BLEUUID((uint16_t)0x2902);
BLECharacteristic *pRudderCharacteristic; // 0 -180 degrees
BLEDescriptor rudderNotifyDescriptor = BLEDescriptor(ClientNotifyDescriptorUuid, 100);
BLECharacteristic *pThrottleCharacteristic; //  0 - 100
BLEDescriptor throttleNotifyDescriptor = BLEDescriptor(ClientNotifyDescriptorUuid, 100);
BLECharacteristic *pModeCharacteristic; // 0= unidirectional, 1 = bidirectional 2 = programming mode
BLEDescriptor modeNotifyDescriptor = BLEDescriptor(ClientNotifyDescriptorUuid, 100);
BLECharacteristic *pLEDCharacteristic; // flags for up to 8 leds
BLEDescriptor LEDNotifyDescriptor = BLEDescriptor(ClientNotifyDescriptorUuid, 100);

uint8_t ledFlags = 0;

int throttlePosition;      // +/-  100
int throttleServoPosition; // 0 - 180
int rudderAngle;           // +/-  60
int rudderServoPosition;   // 0-180
uint8_t mode = 0;
uint8_t led = 0;


XT_Wav_Class MotorSound(motor_wav); 
XT_Wav_Class HornSound(horn_wav); 
XT_DAC_Audio_Class DacAudio(25,3);





void updateRudderServo(int angle)
{

  rudderServoPosition = angle + RUDDER_OFFSET+ (MAX_RUDDER / 2);
  if (rudderServoPosition > MAX_RUDDER)
    rudderServoPosition = MAX_RUDDER;
  if (rudderServoPosition < MIN_RUDDER)
    rudderServoPosition = MIN_RUDDER;

  rudder.write(rudderServoPosition);
  rudderAngle = rudderServoPosition - (MAX_RUDDER / 2) - RUDDER_OFFSET;
}

void setRudderAngle(int angle)
{
  pRudderCharacteristic->setValue(angle);
  pRudderCharacteristic->indicate();

  rudderAngle = angle;
  updateRudderServo(angle);
}

int32_t getRudderAngle()
{
  uint8_t *dataPtr = pRudderCharacteristic->getData();
  size_t datalength = pRudderCharacteristic->getLength();
  int32_t rudderInput;
  if (datalength < sizeof(rudderInput))
  {
    return rudderAngle;
  }
  memcpy(&rudderInput, dataPtr, sizeof(rudderInput));

  return rudderInput;
}
void updateThrottleControl(int value)
{
  int position = value;
  if (mode == MODE_BI)
  {
    if (value < 0)
    {
      position = 90 - (value * -1);
    }
    else
    {
      position = value + 90; // bi direction 90 is mid point // -90 = full reverse(0) +90= full throttle(100)
    }
  }
  if (position > MAX_THROTTLE)
    position = MAX_THROTTLE;
  if (position < MIN_THROTTLE)
    position = MIN_THROTTLE;
  throttle.write(position);
  throttleServoPosition = position;
  throttlePosition = value;
}

int setThrottle(int value)
{
  int32_t position = value;

  pThrottleCharacteristic->setValue(position);
  pThrottleCharacteristic->indicate();
  throttlePosition = position;
  updateThrottleControl(position);
  return position;
}

int getThrottle()
{
  uint8_t *dataPtr = pThrottleCharacteristic->getData();
  size_t datalength = pThrottleCharacteristic->getLength();
  int position;
  memcpy(&position, dataPtr, sizeof(position));

  return position;
}

void setMode(uint8_t newMode)
{
  mode = newMode;
  pModeCharacteristic->setValue(&mode, 1);
  pModeCharacteristic->indicate();
}

void saveMode()
{
  preferences.putUChar("mode", mode);
}

uint8_t getMode()
{
  uint8_t *dataPtr = pModeCharacteristic->getData();
  size_t datalength = pModeCharacteristic->getLength();
  return *dataPtr;
}

uint8_t getLed()
{
  uint8_t *dataPtr = pLEDCharacteristic->getData();
  size_t datalength = pLEDCharacteristic->getLength();
  return *dataPtr;
}

void setLed(uint8_t value)
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
  pLEDCharacteristic->setValue(&led, 1);
  pLEDCharacteristic->indicate();
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

void setup()
{


  preferences.begin("CTBenchy", false);

  Serial.begin(115200);
  Serial.println("Starting BLE Server!");

  BLEDescriptor ClientNotifyDescriptor = BLEDescriptor(ClientNotifyDescriptorUuid, 100);
  BLEDevice::init("CTBenchy");
  pServer = BLEDevice::createServer();
  pService = pServer->createService(SERVICE_UUID);
  pRudderCharacteristic = pService->createCharacteristic(
      RUDDER_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE |
          BLECharacteristic::PROPERTY_NOTIFY |
          BLECharacteristic::PROPERTY_INDICATE);
  pRudderCharacteristic->addDescriptor(&rudderNotifyDescriptor);
  pThrottleCharacteristic = pService->createCharacteristic(
      THROTTLE_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE |
          BLECharacteristic::PROPERTY_NOTIFY|
          BLECharacteristic::PROPERTY_INDICATE);
  pThrottleCharacteristic->addDescriptor(&throttleNotifyDescriptor);
  pModeCharacteristic = pService->createCharacteristic(
      MODE_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE |
          BLECharacteristic::PROPERTY_NOTIFY|
          BLECharacteristic::PROPERTY_INDICATE);
  pModeCharacteristic->addDescriptor(&modeNotifyDescriptor);
  pLEDCharacteristic = pService->createCharacteristic(
      LED_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE |
          BLECharacteristic::PROPERTY_NOTIFY|
          BLECharacteristic::PROPERTY_INDICATE);
  pLEDCharacteristic->addDescriptor(&LEDNotifyDescriptor);

  pService->start();
  BLEDevice::setPower(ESP_PWR_LVL_P9, ESP_BLE_PWR_TYPE_DEFAULT);
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06); // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);

  BLEDevice::startAdvertising();



  Serial.println("Load Preferences!");

  loadPreferences();

  Serial.println("Init Led!");

  initLed();

  Serial.println("Attach rudder");

  // bidirectional set throttle at midpoint when starting
  // program mode set throttle at max when starting
  // unidirectional mode set throttle a 0 when starting

  rudder.attach(rudderPin);
  throttle.attach(throttlePin,Servo::CHANNEL_NOT_ATTACHED, 0, 180,1000,2000);

  setMode(mode);
  setRudderAngle(0);
  switch (mode)
  {
  case MODE_UNI:
    setThrottle(0);
    break;
  case MODE_BI:
    setThrottle(0);
    break;
  case MODE_PROGRAM:
    setThrottle(180);
    break;
  default:
    setThrottle(0);
  }
  setLed(0);

  Serial.println("Characteristic defined! Now you can read it in the Client!");
}

void updateMode()
{
  uint8_t newMode = getMode();
  if (newMode != mode)
  {
    mode = newMode;
    saveMode();
    setMode(newMode);
  }
}




int initCount = 0;
void loop()
{
  bool soundHorn = false;
  updateMode();
  int newThrottlePosition = getThrottle();
  if (newThrottlePosition != throttlePosition)
  {
    setThrottle(newThrottlePosition);
  }

  int newRudderAngle = getRudderAngle();
  if (newRudderAngle != rudderAngle)
  {
    setRudderAngle(newRudderAngle);
  }

  uint8_t newled = getLed();
  if (newled != led)
  {
    setLed(newled);
    if (newled & HORN) soundHorn=true;
    else soundHorn=false;
  }


/*
  Serial.printf("play sound MotorSound.Playing %d\r",MotorSound.Playing);
  // play sound
  DacAudio.FillBuffer();                // Fill the sound buffer with data
  if(MotorSound.Playing==false)  {     // if not playing,
    DacAudio.Play(&MotorSound); 
  }

  if (newled && HornSound.Playing==false){
    DacAudio.Play(&HornSound);
  }
  */

  long t1 = millis();
  if (t1 % 500 == 0)
  { // print a debug every 1/2 second
    if (pServer->getConnectedCount() <= 0 && connected)
    {
      connected = false;
      if (throttlePosition != 0)
        setThrottle(0);
      pServer->startAdvertising();
    }
    else if (pServer->getConnectedCount() == 0){
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
    }
    Serial.printf("rudder angle %d rudderServo %d throttle position %d throttle servo %03d  ledRed: %1x ledWhite %1x ledGreen %1xmode:%d  connected%d\r", rudderAngle, rudderServoPosition, throttlePosition, throttleServoPosition, led & LED_RED, led & LED_WHITE, led & LED_GREEN, mode, pServer->getConnectedCount());
  }
}