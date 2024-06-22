#ifndef ESPNOW_MANAGER_H
#define ESPNOW_MANAGER_H

#include "esp_err.h"

// Function to initialize ESP-NOW
void init_espnow(void);

// Function to send data over ESP-NOW
void send_espnow_data(const char *value);

#endif /* ESPNOW_MANAGER_H */
