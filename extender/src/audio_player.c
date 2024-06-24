#include "audio_player.h"
#include "spiffs_manager.h"
#include <stdio.h>
#include <string.h>
#include <inttypes.h>
#include "driver/dac_continuous.h"
#include "esp_log.h"
#include "esp_rom_sys.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

#define TAG "AUDIO_PLAYER"

typedef struct {
    char chunk_id[4];
    uint32_t chunk_size;
    char format[4];
    char subchunk1_id[4];
    uint32_t subchunk1_size;
    uint16_t audio_format;
    uint16_t num_channels;
    uint32_t sample_rate;
    uint32_t byte_rate;
    uint16_t block_align;
    uint16_t bits_per_sample;
    char subchunk2_id[4];
    uint32_t subchunk2_size;
} wav_header_t;

dac_continuous_handle_t dac_handle;
dac_continuous_config_t cont_cfg = {
    .chan_mask = DAC_CHANNEL_MASK_ALL,
    .desc_num = 4,
    .buf_size = 2048,
    .freq_hz = 48000,
    .offset = 0,
    .clk_src = DAC_DIGI_CLK_SRC_APLL,
    .chan_mode = DAC_CHANNEL_MODE_SIMUL,
};

static bool IRAM_ATTR  dac_on_convert_done_callback(dac_continuous_handle_t handle, const dac_event_data_t *event, void *user_data)
{
    QueueHandle_t que = (QueueHandle_t)user_data;
    BaseType_t need_awoke;
    /* When the queue is full, drop the oldest item */
    if (xQueueIsQueueFullFromISR(que)) {
        dac_event_data_t dummy;
        xQueueReceiveFromISR(que, &dummy, &need_awoke);
    }
    /* Send the event from callback */
    xQueueSendFromISR(que, event, &need_awoke);
    return need_awoke;
}


esp_err_t play_wav(const char *file_path) {
    /* Create a queue to transport the interrupt event data */
    QueueHandle_t que = xQueueCreate(10, sizeof(dac_event_data_t));
    assert(que);
    dac_event_callbacks_t cbs = {
        .on_convert_done = dac_on_convert_done_callback,
        .on_stop = NULL,
    };
    /* Must register the callback if using asynchronous writing */
    ESP_ERROR_CHECK(dac_continuous_register_event_callback(dac_handle, &cbs, que));

    FILE *file = fopen(file_path, "rb");
    if (!file) {
        ESP_LOGE(TAG, "Failed to open file: %s", file_path);
        return ESP_FAIL;
    }

    wav_header_t header;
    fread(&header, sizeof(header), 1, file);

    if (strncmp(header.chunk_id, "RIFF", 4) != 0 || strncmp(header.format, "WAVE", 4) != 0) {
        ESP_LOGE(TAG, "Invalid WAV file format");
        fclose(file);
        return ESP_FAIL;
    }

    ESP_LOGI(TAG, "Playing WAV file: %s", file_path);
    ESP_LOGI(TAG, "Sample Rate: %" PRIu32, header.sample_rate);
    ESP_LOGI(TAG, "Bits per Sample: %" PRIu16, header.bits_per_sample);
    ESP_LOGI(TAG, "Number of Channels: %" PRIu16, header.num_channels);

    if (header.bits_per_sample != 8) {
        ESP_LOGE(TAG, "Only 8-bit WAV files are supported");
        fclose(file);
        return ESP_FAIL;
    }

    if (header.num_channels != 1) {
        ESP_LOGE(TAG, "Only mono WAV files are supported");
        fclose(file);
        return ESP_FAIL;
    }

    // Allocate buffer for audio data
    size_t audio_data_size = header.subchunk2_size;
    uint8_t *audio_data = (uint8_t *)malloc(audio_data_size);
    if (!audio_data) {
        ESP_LOGE(TAG, "Failed to allocate memory for audio data");
        fclose(file);
        return ESP_FAIL;
    }

    // Read the audio data into buffer
    size_t read_size = fread(audio_data, 1, audio_data_size, file);
    if (read_size != audio_data_size) {
        ESP_LOGE(TAG, "Failed to read audio data");
        free(audio_data);
        fclose(file);
        return ESP_FAIL;
    }

    fclose(file);

    // Play the audio data
    size_t offset = 0;
    while (offset < audio_data_size) {
        size_t chunk_size = (audio_data_size - offset) > cont_cfg.buf_size ? cont_cfg.buf_size : (audio_data_size - offset);
        ESP_ERROR_CHECK(dac_continuous_write(dac_handle, audio_data + offset, chunk_size, NULL, -1));
        offset += chunk_size;
        vTaskDelay(pdMS_TO_TICKS(1000 * chunk_size / header.byte_rate));
    }

    free(audio_data);
    ESP_LOGI(TAG, "Finished playing WAV file: %s", file_path);
    return ESP_OK;
}
