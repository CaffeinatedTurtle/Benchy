#include "audio_player.h"
#include <stdio.h>
#include <string.h>  // Include this header for string functions
#include <inttypes.h>  // Include this header for PRIu32
#include "driver/dac.h"
#include "esp_log.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_spiffs.h"
#include "esp_rom_sys.h"  // Include this header for esp_rom_delay_us

#define TAG "AUDIO_PLAYER"

typedef struct {
    char chunk_id[4];       // "RIFF"
    uint32_t chunk_size;    // Size of the file minus 8 bytes
    char format[4];         // "WAVE"
    char subchunk1_id[4];   // "fmt "
    uint32_t subchunk1_size;// 16 for PCM
    uint16_t audio_format;  // PCM = 1
    uint16_t num_channels;  // Mono = 1, Stereo = 2
    uint32_t sample_rate;   // 44100, 48000, etc.
    uint32_t byte_rate;     // SampleRate * NumChannels * BitsPerSample/8
    uint16_t block_align;   // NumChannels * BitsPerSample/8
    uint16_t bits_per_sample; // 8 bits = 8, 16 bits = 16
    char subchunk2_id[4];   // "data"
    uint32_t subchunk2_size;// NumSamples * NumChannels * BitsPerSample/8
} wav_header_t;

static esp_err_t init_spiffs() {
    esp_vfs_spiffs_conf_t conf = {
      .base_path = "/spiffs",
      .partition_label = NULL,
      .max_files = 5,
      .format_if_mount_failed = true
    };

    esp_err_t ret = esp_vfs_spiffs_register(&conf);
    if (ret != ESP_OK) {
        if (ret == ESP_FAIL) {
            ESP_LOGE(TAG, "Failed to mount or format filesystem");
        } else if (ret == ESP_ERR_NOT_FOUND) {
            ESP_LOGE(TAG, "Failed to find SPIFFS partition");
        } else {
            ESP_LOGE(TAG, "Failed to initialize SPIFFS (%s)", esp_err_to_name(ret));
        }
        return ret;
    }

    size_t total = 0, used = 0;
    ret = esp_spiffs_info(NULL, &total, &used);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to get SPIFFS partition information (%s)", esp_err_to_name(ret));
    } else {
        ESP_LOGI(TAG, "Partition size: total: %d, used: %d", total, used);
    }
    return ESP_OK;
}

esp_err_t play_wav(const char *file_path) {
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
    ESP_LOGI(TAG, "Sample Rate: %" PRIu32, header.sample_rate);  // Use PRIu32 for uint32_t
    ESP_LOGI(TAG, "Bits per Sample: %" PRIu16, header.bits_per_sample);  // Use PRIu16 for uint16_t
    ESP_LOGI(TAG, "Number of Channels: %" PRIu16, header.num_channels);  // Use PRIu16 for uint16_t

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

    dac_output_enable(DAC_CHANNEL_1);

    uint8_t sample;
    while (fread(&sample, sizeof(sample), 1, file) == 1) {
        dac_output_voltage(DAC_CHANNEL_1, sample);
        esp_rom_delay_us(1000000 / header.sample_rate);  // Use esp_rom_delay_us instead of ets_delay_us
    }

    dac_output_disable(DAC_CHANNEL_1);
    fclose(file);
    ESP_LOGI(TAG, "Finished playing WAV file: %s", file_path);
    return ESP_OK;
}
