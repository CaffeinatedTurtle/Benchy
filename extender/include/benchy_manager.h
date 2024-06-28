#ifndef BENCHY_MANAGER_H
#define BENCHY_MANAGER_H

#include <stdint.h>
#include <stdbool.h>

// Include ESP-IDF logging
#include "esp_log.h"

// Define mode values
#define MODE_UNIDIRECTIONAL 0x01
#define MODE_BIDIRECTIONAL 0x02
#define MODE_PROGRAM 0x03

// Define servo channel indexes
#define SERVO_CHANNEL_1 0
#define SERVO_CHANNEL_2 1
#define SERVO_CHANNEL_3 2
#define SERVO_CHANNEL_4 3

// Define message types
#define BENCHY_CONFIG 0x10
#define BENCHY_OP 0x20

// Define switch names for specific switch index values
#define SWITCH_STBD 0
#define SWITCH_PORT 1
#define SWITCH_AFT 2
#define SWITCH_MOTOR 3
#define SWITCH_HORN 4

typedef struct {
    uint8_t mode;
    uint8_t mac_address[6];
} Configuration;

typedef struct {
    uint8_t switch_value;
    uint8_t servo_values[4];
} Operation;

typedef struct {
    uint8_t message_type;
    union {
        Configuration config;
        Operation op;
    } payload;
} BenchMSG; // Renamed from ESPNowMessage

void benchy_create_config(BenchMSG *msg, const Configuration *config);
void benchy_create_op(BenchMSG *msg, const Operation *op);
bool benchy_parse(uint8_t *raw_message, BenchMSG *msg);
int benchy_get_raw(const BenchMSG *msg, uint8_t *raw_message);
bool benchy_get_switch(uint8_t switch_value, int index);
void benchy_set_switch(uint8_t *switch_value, int index, bool value);
const char* benchy_get_switch_name(int index);
void benchy_print(const BenchMSG *msg);

#endif // BENCHY_MANAGER_H
