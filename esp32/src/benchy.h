#ifndef BENCHY_H
#define BENCHY_H

#include <Arduino.h>
#include <BLECharacteristic.h>

// LED bit flags
#define LED_GREEN 1
#define LED_RED 2
#define LED_WHITE 4
#define HORN 8
#define MOTOR 16

// Data structures
typedef struct {
    uint8_t mode;
    uint8_t mac_address[6];
} Configuration_t;

typedef struct {
    uint8_t switch_value;
    uint8_t servo_values[4];
} Operation_t;

typedef struct {
    uint8_t message_type;
    union {
        Configuration_t config;
        Operation_t op;
    } payload;
} BenchyMsg_t;

typedef struct __attribute__((packed)) {
    Configuration_t config;
    Operation_t op;
} Benchy_t;

// External global state (must be defined in one .cpp or .ino)
extern Benchy_t benchy;
void printBenchy(const Benchy_t &b, const char *prefix );

// Callback class declaration
class BenchyCallbacks : public BLECharacteristicCallbacks {
  void onRead(BLECharacteristic *pCharacteristic) override;
  void onWrite(BLECharacteristic *pCharacteristic) override;
  void onNotify(BLECharacteristic *pCharacteristic) override;
  void onStatus(BLECharacteristic *pCharacteristic, Status s, uint32_t code) override;
};

#endif  // BENCHY_H
