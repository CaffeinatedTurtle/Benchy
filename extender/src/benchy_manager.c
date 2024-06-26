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

void benchy_create_config(BenchMSG *msg, const Configuration *config) {
    msg->message_type = BENCHY_CONFIG;
    memcpy(&msg->payload.config, config, sizeof(Configuration));
}

void benchy_create_op(BenchMSG *msg, const Operation *op) {
    msg->message_type = BENCHY_OP;
    memcpy(&msg->payload.op, op, sizeof(Operation));
}

#include <stdbool.h>

bool benchy_parse(uint8_t *raw_message, BenchMSG *msg) {
    msg->message_type = raw_message[0];
    
    if (msg->message_type == BENCHY_CONFIG) {
        msg->payload.config.mode = raw_message[1];
        memcpy(msg->payload.config.mac_address, &raw_message[2], 6);
        return true;
    } else if (msg->message_type == BENCHY_OP) {
        msg->payload.op.switch_value = raw_message[1];
        memcpy(msg->payload.op.servo_values, &raw_message[2], 4);
        return true;
    }

    return false; // Unknown message type
}

int benchy_get_raw(const BenchMSG *msg, uint8_t *raw_message) {
    raw_message[0] = msg->message_type;

    if (msg->message_type == BENCHY_CONFIG) {
        raw_message[1] = msg->payload.config.mode;
        memcpy(&raw_message[2], msg->payload.config.mac_address, 6);
        return 8; // Length of the raw message for CONFIGURATION type
    } else if (msg->message_type == BENCHY_OP) {
        raw_message[1] = msg->payload.op.switch_value;
        memcpy(&raw_message[2], msg->payload.op.servo_values, 4);
        return 6; // Length of the raw message for OPERATION type
    }

    return 0; // Unknown message type, no valid message length
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

void benchy_print(const BenchMSG *msg) {
    if (msg->message_type == BENCHY_CONFIG) {
        ESP_LOGI("benchy_manager", "Message Type: CONFIGURATION");
        ESP_LOGI("benchy_manager", "Mode: %d", msg->payload.config.mode);
        ESP_LOGI("benchy_manager", "MAC Address: %02X:%02X:%02X:%02X:%02X:%02X",
               msg->payload.config.mac_address[0], msg->payload.config.mac_address[1],
               msg->payload.config.mac_address[2], msg->payload.config.mac_address[3],
               msg->payload.config.mac_address[4], msg->payload.config.mac_address[5]);
    } else if (msg->message_type == BENCHY_OP) {
        ESP_LOGI("benchy_manager", "Message Type: OPERATION");
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
