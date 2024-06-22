#include "esp_log.h"
#include "nvs_flash.h"
#include "esp_err.h"
#include "esp_coexist.h"
#include "espnow_manager.h"  // Include the ESP-NOW header file
#include "ble_manager.h"     // Include the BLE manager header file

void app_main() {
    // Initialize ESP-NOW
    init_espnow();

    // Initialize BLE
    init_ble();
}
