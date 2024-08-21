#ifndef BENCHY_MANAGER_H
#define BENCHY_MANAGER_H

#include <stdint.h>
#include <stdbool.h>

// Include ESP-IDF logging
#include "esp_log.h"
#include "hal/gpio_types.h"


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
#define SWITCH_HORN 3
#define SWITCH_MOTOR 4


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
#define MOTOR 16

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
} BenchyMsg_t; // Renamed from ESPNowMessage


typedef struct __attribute__((packed)) {
    Configuration_t config;
    Operation_t op;
} Benchy_t;

void benchy_create_config(BenchyMsg_t *msg, const Configuration_t *config);
void benchy_create_op(BenchyMsg_t *msg, const Operation_t *op);
bool benchy_parse(BenchyMsg_t *msg, Benchy_t *data);
bool benchy_get_switch(uint8_t switch_value, int index);
void benchy_set_switch(uint8_t *switch_value, int index, bool value);
const char* benchy_get_switch_name(int index);
void benchy_print(const Benchy_t *msg);
void benchy_init(Benchy_t *data);
void benchy_print_raw(const Benchy_t *bench);

#endif // BENCHY_MANAGER_H
