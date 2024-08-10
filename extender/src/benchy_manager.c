#include "benchy_manager.h"
#include <string.h>

// Switch names for specific switch index values
static const char *SWITCH_NAMES[] = {
    "stbd",
    "port",
    "aft",
    "motor",
    "horn"
};

void benchy_create_config(BenchyMsg_t *msg, const Configuration_t *config) {
    msg->message_type = BENCHY_CONFIG;
    memcpy(&msg->payload.config, config, sizeof(Configuration_t));
}

void benchy_create_op(BenchyMsg_t *msg, const Operation_t *op) {
    msg->message_type = BENCHY_OP;
    memcpy(&msg->payload.op, op, sizeof(Operation_t));
}

#include <stdbool.h>

bool benchy_parse(BenchyMsg_t *msg, Benchy_t *data) {

     
    if (msg->message_type == BENCHY_CONFIG) {
        data->config.mode = msg->payload.config.mode;
        memcpy(data->config.mac_address, msg->payload.config.mac_address, 6);
        return true;
    } else if (msg->message_type == BENCHY_OP) {
        data->op.switch_value = msg->payload.op.switch_value;   
        memcpy(data->op.servo_values, msg->payload.op.servo_values, 4);
        return true;
    }

    return false; // Unknown message type
}


bool benchy_get_switch(uint8_t switch_value, int index) {
    return (switch_value & (1 << index)) != 0;
}

void benchy_set_switch(uint8_t *switch_value, int index, bool value) {
    if (value) {
        *switch_value |= (1 << index);
    } else {
        *switch_value &= ~(1 << index);
    }
}

const char* benchy_get_switch_name(int index) {
    if (index >= 0 && index < sizeof(SWITCH_NAMES) / sizeof(SWITCH_NAMES[0])) {
        return SWITCH_NAMES[index];
    }
    return "unknown";
}

void benchy_print_msg(const BenchyMsg_t *msg) {
    if (msg->message_type == BENCHY_CONFIG) {
        ESP_LOGI("benchy_manager", "Message Type: Configuration_t");
        ESP_LOGI("benchy_manager", "Mode: %d", msg->payload.config.mode);
        ESP_LOGI("benchy_manager", "MAC Address: %02X:%02X:%02X:%02X:%02X:%02X",
               msg->payload.config.mac_address[0], msg->payload.config.mac_address[1],
               msg->payload.config.mac_address[2], msg->payload.config.mac_address[3],
               msg->payload.config.mac_address[4], msg->payload.config.mac_address[5]);
    } else if (msg->message_type == BENCHY_OP) {
        ESP_LOGI("benchy_manager", "Message Type: Operation_t");
        ESP_LOGI("benchy_manager", "Switch Value: %d", msg->payload.op.switch_value);
        for (int i = 0; i < 8; ++i) {
            ESP_LOGI("benchy_manager", "%s: %s", benchy_get_switch_name(i), benchy_get_switch(msg->payload.op.switch_value, i) ? "true" : "false");
        }
        ESP_LOGI("benchy_manager", "Servo Values: %d, %d, %d, %d",
               msg->payload.op.servo_values[0], msg->payload.op.servo_values[1],
               msg->payload.op.servo_values[2], msg->payload.op.servo_values[3]);
    } else {
        ESP_LOGI("benchy_manager", "Unknown Message Type");
    }
}

void benchy_print(const Benchy_t *msg) {
        ESP_LOGI("benchy_manager", "MAC Address: %02X:%02X:%02X:%02X:%02X:%02X",
               msg->config.mac_address[0], msg->config.mac_address[1],
               msg->config.mac_address[2], msg->config.mac_address[3],
               msg->config.mac_address[4], msg->config.mac_address[5]);
        ESP_LOGI("benchy_manager", "Switch Value: %d", msg->op.switch_value);
        for (int i = 0; i < 8; ++i) {
            ESP_LOGI("benchy_manager", "%s: %s", benchy_get_switch_name(i), benchy_get_switch(msg->op.switch_value, i) ? "true" : "false");
        }
        ESP_LOGI("benchy_manager", "Servo Values: %d, %d, %d, %d",
               msg->op.servo_values[0], msg->op.servo_values[1],
               msg->op.servo_values[2], msg->op.servo_values[3]);
   
}


void benchy_init(Benchy_t *data) {
    Operation_t op = {
        .switch_value = 0x0f,
        .servo_values = {10, 20, 30, 40}
    };
    Configuration_t config = {
        .mode = MODE_UNIDIRECTIONAL,
        .mac_address = {1, 2, 3, 4, 5, 6}
    };
    data->op = op;
    data->config = config;
}
