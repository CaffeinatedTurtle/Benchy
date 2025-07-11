#ifndef BLE_MANAGER_H
#define BLE_MANAGER_H

#include <stdint.h>
#include "esp_gap_ble_api.h"
#include "esp_gatts_api.h"
#include "benchy_manager.h"

// Function prototypes
void init_ble(const Benchy_t *benchy_ptr);
void ble_gap_event_handler(esp_gap_ble_cb_event_t event, esp_ble_gap_cb_param_t *param);
void gatts_profile_event_handler(esp_gatts_cb_event_t event, esp_gatt_if_t gatts_if, esp_ble_gatts_cb_param_t *param);
void send_ble_data(const char *data, size_t len);

#endif // BLE_MANAGER_H
