#ifndef SERVO_H
#define SERVO_H

#include "driver/ledc.h"
#include "esp_err.h"

#define SERVO_MIN_PULSEWIDTH 500     // Minimum pulse width in microsecond
#define SERVO_MAX_PULSEWIDTH 2500    // Maximum pulse width in microsecond
#define SERVO_MAX_ANGLE 180          // Maximum angle in degree

typedef struct {
    ledc_channel_t channel;
    gpio_num_t gpio_num;
    ledc_timer_t timer_sel;
    uint32_t min_pulsewidth;
    uint32_t max_pulsewidth;
} servo_t;

esp_err_t servo_init(servo_t *servo, gpio_num_t gpio_num, ledc_channel_t channel, ledc_timer_t timer_sel);
esp_err_t servo_set_angle(servo_t *servo, uint32_t angle);
esp_err_t servo_deinit(servo_t *servo);
uint32_t byteToAngle(uint8_t byte);

#endif // SERVO_H
