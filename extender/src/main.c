#include "esp_log.h"
#include "nvs_flash.h"
#include "esp_err.h"
#include "esp_coexist.h"
#include "espnow_manager.h"  // Include the ESP-NOW header file
#include "ble_manager.h"     // Include the BLE manager header file
#include "servo.h"
#include "audio_player.h"
#include "spiffs_manager.h"
#include "esp_rom_sys.h"  // Include this header for esp_rom_delay_us
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

void app_main() {
      servo_t servo;
    // Initialize ESP-NOW
    init_espnow();

    // Initialize BLE
    init_ble();

    

    ESP_ERROR_CHECK(init_spiffs());

    const char *file_path = "/spiffs/toot.wav";
    ESP_ERROR_CHECK(play_wav(file_path));

   
    servo_init(&servo, GPIO_NUM_18, LEDC_CHANNEL_0, LEDC_TIMER_0);

    while (1) {
        for (int angle = 0; angle <= 180; angle += 10) {
            servo_set_angle(&servo, angle);
            vTaskDelay(pdMS_TO_TICKS(500));
        }
        for (int angle = 180; angle >= 0; angle -= 10) {
            servo_set_angle(&servo, angle);
            vTaskDelay(pdMS_TO_TICKS(500));
        }
    }
}
