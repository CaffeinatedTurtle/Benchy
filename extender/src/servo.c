#include "servo.h"
#include "esp_log.h"

static const char *TAG = "servo";

static uint32_t angle_to_pulsewidth(servo_t *servo, uint32_t angle) {
    return (servo->min_pulsewidth + ((servo->max_pulsewidth - servo->min_pulsewidth) * angle) / SERVO_MAX_ANGLE);
}

esp_err_t servo_init(servo_t *servo, gpio_num_t gpio_num, ledc_channel_t channel, ledc_timer_t timer_sel) {
    servo->channel = channel;
    servo->gpio_num = gpio_num;
    servo->timer_sel = timer_sel;
    servo->min_pulsewidth = SERVO_MIN_PULSEWIDTH;
    servo->max_pulsewidth = SERVO_MAX_PULSEWIDTH;

    ledc_timer_config_t ledc_timer = {
        .duty_resolution = LEDC_TIMER_16_BIT, // resolution of PWM duty
        .freq_hz = 50,                        // frequency of PWM signal
        .speed_mode = LEDC_HIGH_SPEED_MODE,   // timer mode
        .timer_num = timer_sel,               // timer index
        .clk_cfg = LEDC_AUTO_CLK              // Auto select the source clock
    };
    ESP_ERROR_CHECK(ledc_timer_config(&ledc_timer));

    ledc_channel_config_t ledc_channel = {
        .channel = channel,
        .duty = 0,
        .gpio_num = gpio_num,
        .speed_mode = LEDC_HIGH_SPEED_MODE,
        .hpoint = 0,
        .timer_sel = timer_sel
    };
    ESP_ERROR_CHECK(ledc_channel_config(&ledc_channel));

    return ESP_OK;
}

esp_err_t servo_set_angle(servo_t *servo, uint32_t angle) {
    if (angle > SERVO_MAX_ANGLE) {
        ESP_LOGE(TAG, "Angle out of range");
        return ESP_ERR_INVALID_ARG;
    }

    uint32_t pulsewidth = angle_to_pulsewidth(servo, angle);
    ESP_ERROR_CHECK(ledc_set_duty(LEDC_HIGH_SPEED_MODE, servo->channel, pulsewidth));
    ESP_ERROR_CHECK(ledc_update_duty(LEDC_HIGH_SPEED_MODE, servo->channel));

    return ESP_OK;
}

esp_err_t servo_deinit(servo_t *servo) {
    ESP_ERROR_CHECK(ledc_stop(LEDC_HIGH_SPEED_MODE, servo->channel, 0));
    return ESP_OK;
}
