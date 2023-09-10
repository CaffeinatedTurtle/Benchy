#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

#include <Servo.h>

#include <Preferences.h>

Preferences preferences;

#define MAX_THROTTLE 100
#define MIN_THROTTLE 0

#define MAX_RUDDER 120
#define MIN_RUDDER 0

#define MODE_UNI 0
#define MODE_BI 1
#define MODE_PROGRAM 2

int rudderPin = 13;
int throttlePin = 12;

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
BLECharacteristic *pRudderCharacteristic;   // 0 -180 degrees
BLECharacteristic *pThrottleCharacteristic; //  0 - 100
BLECharacteristic *pModeCharacteristic;     // 0= unidirectional, 1 = bidirectional 2 = programming mode
BLECharacteristic *pLEDCharacteristic;   // flags for up to 8 leds

uint8_t ledFlags = 0;
int rudderAngle;      // +/-  60
int throttlePosition; // +/-  100
int throttleServoPosition;   // 0 - 100
int rudderServoPosition; // 0-180
uint8_t mode = 0;

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

int setThrottle(int value)
{
  int position = value;
  if (value > MAX_THROTTLE)
    position= MAX_THROTTLE;
  if (value < MIN_THROTTLE)
    position = MIN_THROTTLE;

   pThrottleCharacteristic->setValue(position);
  return position;
}

void updateThrottleControl(int value){
  int position=value;
    if (mode = MODE_BI)
  {
    if (value < 0)
    {
      position = 50 - (value * -1);
    }
    else
    {
      position = value + 50; // bi direction 50 is mid point // -50 = full reverse(0) +50= full throttle(100)
    }
  }
  throttle.write(position);
  throttleServoPosition = position;
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

void setMode(uint8_t newMode)
{
  mode = newMode;
  pModeCharacteristic->setValue(&mode, 1);
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

void loadPreferences()
{
  mode = preferences.getUChar("mode", 0);
  Serial.printf("loaded preferences mode = %d\n", mode);
  setMode(mode);
}

void setup()
{

  preferences.begin("CTBenchy", false);

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
      THROTTLE_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE);
  pModeCharacteristic = pService->createCharacteristic(
      MODE_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE);
   pLEDCharacteristic = pService->createCharacteristic(
      LED_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ |
          BLECharacteristic::PROPERTY_WRITE);



  loadPreferences();
  setRudderAngle(0);

  // bidirectional set throttle at midpoint when starting
  // program mode set throttle at max when starting
  // unidirectional mode set throttle a 0 when starting

  switch (mode)
  {
  case MODE_UNI:
    setThrottle(0);
    break;
  case MODE_BI:
    setThrottle(0);
    break;
  case MODE_PROGRAM:
    setThrottle(100);
    break;
  default:
    setThrottle(0);
  }

  rudder.attach(rudderPin);
  throttle.attach(throttlePin, Servo::CHANNEL_NOT_ATTACHED, 0, 100, 544, 2400);

  pService->start();
  BLEDevice::setPower(ESP_PWR_LVL_P9,ESP_BLE_PWR_TYPE_DEFAULT);
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true);
  pAdvertising->setMinPreferred(0x06); // functions that help with iPhone connections issue
  pAdvertising->setMinPreferred(0x12);
 
  BLEDevice::startAdvertising();
  // pAdvertising->start();
  Serial.println("Characteristic defined! Now you can read it in the Client!");
}

void updateMode(){
  uint8_t newMode = getMode();
  if (newMode != mode){
    mode = newMode;
    saveMode();

  }
}

void loop()
{
  updateMode();
  throttlePosition = getThrottle();
  rudderAngle = getRudderAngle();
  rudder.write(rudderAngle + (MAX_RUDDER / 2));
  long t1 = millis();
  if (t1 % 500 == 0)
  { // print a debug every 1/2 second
    if (pServer->getConnectedCount() <= 0 && connected){
      connected = false;
      setThrottle(0);
      pServer->startAdvertising();
     
    } else {
      connected=true;
    }
    Serial.printf("rudder angle %03d throttle position %03d duty cycle %03d rudder %d  led: %2x mode:%d  connected%d\r", rudderAngle, throttlePosition, throttleServoPosition, rudderAngle,getLed(), mode, pServer->getConnectedCount());
  }
}