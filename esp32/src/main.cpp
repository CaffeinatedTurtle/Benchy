#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

#include <Servo.h>

#include <Preferences.h>

Preferences preferences;

#define MAX_THROTTLE 1024
#define MIN_THROTTLE -1024

#define MAX_RUDDER 120
#define MIN_RUDDER 0

int rudderPin = 13;
int throttlePin = 12;


Servo rudder;
Servo throttle;

#define SERVICE_UUID "7507cee3-db32-4e5a-bd6b-96b62887129e"
#define RUDDER_CHARACTERISTIC_UUID "d7c1861c-beff-430f-9a72-fc05c6cc997d"
#define THROTTLE_CHARACTERISTIC_UUID "87607759-37d1-41b5-b2c8-c44b7c746083"
#define MODE_CHARACTERISTIC_UUID "16d68508-2fd4-40a9-ba61-aac41cb81e45"

BLEServer *pServer;
BLEService *pService;
BLECharacteristic *pRudderCharacteristic;   // 0 -180 degrees
BLECharacteristic *pThrottleCharacteristic; // +/-  0 - 100
BLECharacteristic *pModeCharacteristic; // true = bidirectional, false= unidirectional

int rudderAngle;      // +/-  60
int throttlePosition; // +/-  1024
bool mode = false;

void setRudderAngle(int angle)
{
  if (angle > MAX_RUDDER)
    angle = MAX_RUDDER;
  if (angle < MIN_RUDDER)
    angle = MIN_RUDDER;
  pRudderCharacteristic->setValue(angle);
}

uint8_t getRudderAngle()
{
  uint8_t *dataPtr = pRudderCharacteristic->getData();
  size_t datalength = pRudderCharacteristic->getLength();
  int rudder;
  if (datalength < sizeof(rudder))
    return rudderAngle;
  memcpy(&rudder, dataPtr, sizeof(rudder));
  if (rudder > MAX_RUDDER)
  {
    rudder = MAX_RUDDER;
    pRudderCharacteristic->setValue(rudder);
  }
  if (rudder < MIN_RUDDER)
  {
    rudder = MIN_RUDDER;
    pRudderCharacteristic->setValue(rudder);
  }

  return rudder;
}

void setThrottle(int value)
{
  if (value > MAX_THROTTLE)
    value = MAX_THROTTLE;
  if (value < MIN_THROTTLE)
    value = MIN_THROTTLE;
  pThrottleCharacteristic->setValue(value);
}

int getThrottle()
{
  uint8_t *dataPtr = pThrottleCharacteristic->getData();
  size_t datalength = pThrottleCharacteristic->getLength();
  int position;
  memcpy(&position, dataPtr, sizeof(position));

  if (position > MAX_THROTTLE)
  {
    position = MAX_THROTTLE;
    pThrottleCharacteristic->setValue(position);
  }

  if (position < MIN_THROTTLE)
  {
    position = MIN_THROTTLE;
    pThrottleCharacteristic->setValue(position);
  }

  return position;
}

void setMode(bool newMode)
{
  int mode = 0;
 if (newMode) mode = 1;
 pModeCharacteristic->setValue(mode);
  preferences.putBool("mode",newMode);
}

bool getMode()
{
  uint8_t *dataPtr = pModeCharacteristic->getData();
  size_t datalength = pModeCharacteristic->getLength();
  int mode;
  memcpy(&mode, dataPtr, sizeof(mode));
  return mode==1;
}

void loadPreferences(){
 bool mode = preferences.getBool("mode",false);
//setMode(mode);
}





void setup()
{

  preferences.begin("CTBenchy", false); 
  loadPreferences();

  Serial.begin(115200);
  Serial.println("Starting BLE Server!");

  BLEDevice::init("CTBenchy");
  pServer = BLEDevice::createServer();
  pService = pServer->createService(SERVICE_UUID);
  pRudderCharacteristic = pService->createCharacteristic(
      RUDDER_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE);
  pThrottleCharacteristic = pService->createCharacteristic(
      RUDDER_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE);
  pModeCharacteristic = pService->createCharacteristic(
      MODE_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE);

  /* BLEServer *pServer = BLEDevice::createServer();
  BLEService *pService = pServer->createService(SERVICE_UUID);
  BLECharacteristic *pCharacteristic = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );*/

  setRudderAngle(0);

  // bidirectional set throttle at midpoint when starting

  if (mode) setThrottle(50);
  else  setThrottle(0);


  rudder.attach(rudderPin);
  throttle.attach(throttlePin, Servo::CHANNEL_NOT_ATTACHED, 0, 100, 1000, 2000);

  pService->start();
  // BLEAdvertising *pAdvertising = pServer->getAdvertising();
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06); // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
  BLEDevice::startAdvertising();
  // pAdvertising->start();
  Serial.println("Characteristic defined! Now you can read it in the Client!");
}

void loop()
{
  throttlePosition = getThrottle();
  rudderAngle = getRudderAngle();
  rudder.write(rudderAngle + (MAX_RUDDER / 2));
  long t1 = millis();
  if (t1 % 500 == 0)
  { // print a debug every 1/2 second
    Serial.printf("rudder angle %d throttle position %d duty cycle rudder %d  mode:%d\r", rudderAngle, getThrottle(), rudderAngle, mode);
  }
}