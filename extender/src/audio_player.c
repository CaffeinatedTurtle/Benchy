#include "audio_player.h"
#include "spiffs_manager.h"
#include "wave_processor.h"
#include <stdio.h>
#include <string.h>
#include <inttypes.h>
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "driver/dac_continuous.h"
#include "esp_log.h"
#include "toot.h"

#define TAG "AUDIO_PLAYER"

typedef struct
{
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

typedef struct audio_data_t{
    uint8_t *data1;
    size_t size1;
    uint8_t *data2;
    size_t size2;
    uint8_t mode;
    dac_continuous_handle_t dac_handle;
} audio_data_t;

static bool play_audio_control = false;
static dac_continuous_handle_t dac_handle;
static QueueHandle_t audio_queue;
static audio_data_t audio_data;

static bool IRAM_ATTR dac_on_convert_done_callback(dac_continuous_handle_t handle, const dac_event_data_t *event, void *user_data)
{
    QueueHandle_t que = (QueueHandle_t)user_data;
    BaseType_t need_awoke;
    /* When the queue is full, drop the oldest item */
    if (xQueueIsQueueFullFromISR(que))
    {
        dac_event_data_t dummy;
        xQueueReceiveFromISR(que, &dummy, &need_awoke);
    }
    /* Send the event from callback */
    xQueueSendFromISR(que, event, &need_awoke);
    return need_awoke;
}

static void dac_write_data_asynchronously(dac_continuous_handle_t handle, QueueHandle_t que, uint8_t *data, size_t data_size)
{
    while (play_audio_control)
    {
        printf("Playing audio\n");
        dac_event_data_t evt_data;
        size_t byte_written = 0;
        /* Receive the event from callback and load the data into the DMA buffer until the whole audio loaded */
        while (byte_written < data_size && play_audio_control)
        {
            xQueueReceive(que, &evt_data, portMAX_DELAY);
            size_t loaded_bytes = 0;
            ESP_ERROR_CHECK(dac_continuous_write_asynchronously(handle, evt_data.buf, evt_data.buf_size,
                                                                data + byte_written, data_size - byte_written, &loaded_bytes));
            byte_written += loaded_bytes;
        }
        /* Clear the legacy data in DMA, clear times equal to the 'dac_continuous_config_t::desc_num' */
        for (int i = 0; i < 4; i++)
        {
            xQueueReceive(que, &evt_data, portMAX_DELAY);
            memset(evt_data.buf, 0, evt_data.buf_size);
        }
    }
    ESP_ERROR_CHECK(dac_continuous_disable(handle));
    ESP_ERROR_CHECK(dac_continuous_del_channels(handle));
}

static void dac_write_data_synchronously(dac_continuous_handle_t handle, uint8_t *data, size_t data_size)
{
    while (play_audio_control)
    {
        printf("Playing audio\n");
        ESP_ERROR_CHECK(dac_continuous_write(handle, data, data_size, NULL, -1));
    }
    ESP_ERROR_CHECK(dac_continuous_disable(handle));
    ESP_ERROR_CHECK(dac_continuous_del_channels(handle));
}

static void dac_dma_write_task(void *args)
{
    audio_data_t *audio = (audio_data_t *)args;  // Properly cast args to audio_data_t pointer
    while (1)
    {
        /* The wave in the buffer will be converted cyclically */
        switch (audio->mode)
        {
        case 0:
            break;
        case 1:
            ESP_ERROR_CHECK(dac_continuous_write_cyclically(audio->dac_handle, audio->data1, audio->size1, NULL));
            break;
        case 2:
            ESP_ERROR_CHECK(dac_continuous_write_cyclically(audio->dac_handle, audio->data2, audio->size2, NULL));
            break;
        default:
            break;
        }
    }
}


void start_play(int mode)
{
    audio_data.mode = mode;
}

void stop_play()
{
    audio_data.mode = 0;
    ESP_ERROR_CHECK(dac_continuous_disable(audio_data.dac_handle));
    
}

void play_continuous_audio(audio_data_t *audio, uint32_t frequency)
{
    ESP_LOGI(TAG, "DAC audio example start");
    ESP_LOGI(TAG, "--------------------------------------");

    dac_continuous_config_t cont_cfg = {
        .chan_mask = DAC_CHANNEL_MASK_ALL,
        .desc_num = 4,
        .buf_size = 2048,
        .freq_hz = frequency,
        .offset = 0,
        .clk_src = DAC_DIGI_CLK_SRC_APLL, // Using APLL as clock source to get a wider frequency range
        .chan_mode = DAC_CHANNEL_MODE_SIMUL,
    };
   
    /* Allocate continuous channels */
    ESP_ERROR_CHECK(dac_continuous_new_channels(&cont_cfg, audio->dac_handle));
   
}




void play_audio(size_t *audio_size, uint8_t *audio_data, uint32_t frequency)
{
    ESP_LOGI(TAG, "DAC audio example start");
    ESP_LOGI(TAG, "--------------------------------------");

    dac_continuous_config_t cont_cfg = {
        .chan_mask = DAC_CHANNEL_MASK_ALL,
        .desc_num = 4,
        .buf_size = 2048,
        .freq_hz = frequency,
        .offset = 0,
        .clk_src = DAC_DIGI_CLK_SRC_APLL, // Using APLL as clock source to get a wider frequency range
        .chan_mode = DAC_CHANNEL_MODE_SIMUL,
    };

    /* Allocate continuous channels */
    ESP_ERROR_CHECK(dac_continuous_new_channels(&cont_cfg, &dac_handle));

    /* Create a queue to transport the interrupt event data */
    audio_queue = xQueueCreate(10, sizeof(dac_event_data_t));
    assert(audio_queue);

    dac_event_callbacks_t cbs = {
        .on_convert_done = dac_on_convert_done_callback,
        .on_stop = NULL,
    };

    /* Must register the callback if using asynchronous writing */
    ESP_ERROR_CHECK(dac_continuous_register_event_callback(dac_handle, &cbs, audio_queue));

    /* Enable the continuous channels */
    ESP_ERROR_CHECK(dac_continuous_enable(dac_handle));
    ESP_LOGI(TAG, "DAC initialized successfully, DAC DMA is ready");

    ESP_ERROR_CHECK(dac_continuous_start_async_writing(dac_handle));
    dac_write_data_asynchronously(dac_handle, audio_queue, audio_data, audio_size);
}

static void control_task(void *param)
{
    start_play(1);
    vTaskDelay(pdMS_TO_TICKS(5000)); // Wait for 5 seconds
    start_play(2);
    vTaskDelay(pdMS_TO_TICKS(500)); // Wait for 5 seconds
    start_play(1);
    vTaskDelay(pdMS_TO_TICKS(5000)); // Wait for 5 seconds
    stop_play(); // Set the control bit to false
    vTaskDelete(NULL); // Delete this task
}

void test_audio()
{
    size_t data_size_tractor;
    uint32_t sample_rate_tractor;
    uint8_t *data_tractor = process_wave("/spiffs/tractor.wav", 8, &data_size_tractor, &sample_rate_tractor);

    size_t data_size_toot;
    uint32_t sample_rate_toot;
    uint8_t *data_toot = process_wave("/spiffs/toot.wav", 8, &data_size_toot, &sample_rate_toot);

    audio_data.data1 = data_tractor;
    audio_data.size1 = data_size_tractor;
    audio_data.data2 = data_toot;
    audio_data.size2 = data_size_toot;
    audio_data.mode = 1;
    xTaskCreate(control_task, "control_task", 2048, NULL, 5, NULL);  
    // Start the DMA task
    xTaskCreate(dac_dma_write_task, "dac_dma_write_task", 2048, &audio_data, 5, NULL);

}

esp_err_t play_wav(const char *file_path)
{
    size_t data_size;
    uint32_t sample_rate;
    uint8_t *data = process_wave(file_path, 8, &data_size, &sample_rate);

    if (data == NULL)
    {
        ESP_LOGE(TAG, "Failed to process WAV file: %s", file_path);
        return ESP_FAIL;
    }

    ESP_LOGI(TAG, "Allocated memory for audio data: %d bytes %" PRIu32 " sample_rate", data_size, sample_rate);

    play_audio(&data_size, data, sample_rate);

    free(data);
    ESP_LOGI(TAG, "Finished playing WAV file: %s", file_path);
    return ESP_OK;
}


