#include "espnow_manager.h"
#include "esp_log.h"
#include "esp_now.h"
#include "esp_wifi.h"
#include <string.h>  // Include for memcpy

static const uint8_t peer_mac[ESP_NOW_ETH_ALEN] = {0x24, 0x6F, 0x28, 0xAD, 0x34, 0xCE};

typedef struct struct_message {
    char command[32];
} struct_message;

static struct_message my_data;

static void on_data_sent(const uint8_t *mac_addr, esp_now_send_status_t status) {
    ESP_LOGI("ESP-NOW", "Last Packet Send Status: %s", status == ESP_NOW_SEND_SUCCESS ? "Delivery Success" : "Delivery Fail");
}

void init_espnow() {
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));
    ESP_ERROR_CHECK(esp_wifi_set_storage(WIFI_STORAGE_RAM));
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_start());

    ESP_ERROR_CHECK(esp_now_init());
    ESP_ERROR_CHECK(esp_now_register_send_cb(on_data_sent));

    esp_now_peer_info_t peer_info = {};
    memcpy(peer_info.peer_addr, peer_mac, ESP_NOW_ETH_ALEN);
    peer_info.channel = 0;
    peer_info.encrypt = false;

    ESP_ERROR_CHECK(esp_now_add_peer(&peer_info));
}

void send_espnow_data(const char *value) {
    strncpy(my_data.command, value, sizeof(my_data.command) - 1);
    my_data.command[sizeof(my_data.command) - 1] = '\0';
    esp_err_t result = esp_now_send(peer_mac, (uint8_t *)&my_data, sizeof(my_data));

    if (result == ESP_OK) {
        ESP_LOGI("ESP-NOW", "Sent with success");
    } else {
        ESP_LOGI("ESP-NOW", "Error sending the data");
    }
}
