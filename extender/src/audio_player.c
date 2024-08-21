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
#define MAX_AUDIO_BUFFERS 3

#define TRACK_BACKGROUND 0
#define TRACK_TOOT 1
#define TRACK_TONE 2

#define MODE_SINGLE 2
#define MODE_CYCLE 1
#define MODE_STOP 0

typedef struct
{
    uint8_t *data;
    size_t size;
} audio_buffer_t;

typedef struct
{
    audio_buffer_t buffers[MAX_AUDIO_BUFFERS];
    uint8_t mode;
    uint8_t track;
    dac_continuous_handle_t dac_handle;
} audio_data_t;

static bool play_audio_control = false;
static dac_continuous_handle_t dac_handle;
static QueueHandle_t audio_queue;
static audio_data_t audio_data;

void play_audio(int mode, int sample_rate);

static bool IRAM_ATTR dac_on_convert_done_callback(dac_continuous_handle_t handle, const dac_event_data_t *event, void *user_data)
{
    QueueHandle_t que = (QueueHandle_t)user_data;
    BaseType_t need_awoke;
    if (xQueueIsQueueFullFromISR(que))
    {
        dac_event_data_t dummy;
        xQueueReceiveFromISR(que, &dummy, &need_awoke);
    }
    xQueueSendFromISR(que, event, &need_awoke);
    return need_awoke;
}

static void dac_write_data_asynchronously(dac_continuous_handle_t handle, QueueHandle_t que, uint8_t *data, size_t data_size)
{
    ESP_LOGI(TAG, "Audio size %d bytes", data_size);
    uint32_t cnt = 1;
    while (1)
    {
        printf("Play count: %" PRIu32 "\n", cnt++);
        dac_event_data_t evt_data;
        size_t byte_written = 0;
        while (byte_written < data_size)
        {
            xQueueReceive(que, &evt_data, portMAX_DELAY);
            size_t loaded_bytes = 0;
            ESP_ERROR_CHECK(dac_continuous_write_asynchronously(handle, evt_data.buf, evt_data.buf_size,
                                                                data + byte_written, data_size - byte_written, &loaded_bytes));
            byte_written += loaded_bytes;
        }
        for (int i = 0; i < 4; i++)
        {
            xQueueReceive(que, &evt_data, portMAX_DELAY);
            memset(evt_data.buf, 0, evt_data.buf_size);
        }
        vTaskDelay(pdMS_TO_TICKS(1000));
    }
}

static void dac_write_data_asynchronously_cycle(dac_continuous_handle_t handle, QueueHandle_t que, int mode)
{
    int bufferIndex = audio_data.track;
    ESP_LOGI(TAG, "Start Audio %d %d size %d bytes", bufferIndex, mode, audio_data.buffers[bufferIndex].size);
    uint32_t cnt = 1;
    while (audio_data.mode != MODE_STOP)
    {

    printf("Play count: %" PRIu32 "\n", cnt++);
    dac_event_data_t evt_data;
    while (audio_data.mode != MODE_STOP)
    {
        bufferIndex = audio_data.track;
        uint8_t *data = audio_data.buffers[bufferIndex].data;
        size_t data_size = audio_data.buffers[bufferIndex].size;
        size_t truncated_size = (data_size / 1024) * 1024;
        data_size = truncated_size;
        ESP_LOGI(TAG, "cycle Audio %d %d size %d bytes", bufferIndex, audio_data.mode, data_size);  
        if (audio_data.mode == MODE_SINGLE)
        {
            audio_data.mode = MODE_CYCLE;
            audio_data.track = TRACK_BACKGROUND;
        } 
       
        size_t byte_written = 0;
        while (byte_written < data_size)
        {
            xQueueReceive(que, &evt_data, portMAX_DELAY);
            size_t loaded_bytes = 0;
            ESP_ERROR_CHECK(dac_continuous_write_asynchronously(handle, evt_data.buf, evt_data.buf_size,
                                                                data + byte_written, data_size - byte_written, &loaded_bytes));
            byte_written += loaded_bytes;
      
        }

       
     
  
    }
    for (int i = 0; i < 4; i++)
    {
        xQueueReceive(que, &evt_data, portMAX_DELAY);
        memset(evt_data.buf, 0, evt_data.buf_size);
    }
    }
    ESP_ERROR_CHECK(dac_continuous_start_async_writing(audio_data.dac_handle));
    vTaskDelay(pdMS_TO_TICKS(1000));
}


static void dac_write_data_synchronously(dac_continuous_handle_t handle, uint8_t *data, size_t data_size)
{
    ESP_ERROR_CHECK(dac_continuous_write(handle, data, data_size, NULL, -1));
    ESP_ERROR_CHECK(dac_continuous_disable(handle));
}


static void dac_dma_write_task(void *args)
{
    audio_data_t *audio = (audio_data_t *)args;
    play_audio(audio->mode, 44100);
    while (1)
    {

     

        vTaskDelay(pdMS_TO_TICKS(10));
    }
}

void start_play(int track,int mode)
{
    audio_data.track = track;
    audio_data.mode = mode;

}

void switch_play(int track, int mode)
{
    audio_data.track = track;
    audio_data.mode = mode;
}

void stop_play()
{
    audio_data.mode = 0;
    ESP_ERROR_CHECK(dac_continuous_disable(audio_data.dac_handle));
}

void interrupt_play()
{
    audio_data.mode = 0;
}

void play_audio(int mode, int sample_rate)
{

    dac_continuous_config_t cont_cfg = {
        .chan_mask = DAC_CHANNEL_MASK_ALL,
        .desc_num = 4,
        .buf_size = 2048,
        .freq_hz = sample_rate,
        .offset = 0,
        .clk_src = DAC_DIGI_CLK_SRC_APLL,
        .chan_mode = DAC_CHANNEL_MODE_SIMUL,
    };

    ESP_ERROR_CHECK(dac_continuous_new_channels(&cont_cfg, &audio_data.dac_handle));

    audio_queue = xQueueCreate(10, sizeof(dac_event_data_t));
    assert(audio_queue);

    dac_event_callbacks_t cbs = {
        .on_convert_done = dac_on_convert_done_callback,
        .on_stop = NULL,
    };

    ESP_ERROR_CHECK(dac_continuous_register_event_callback(audio_data.dac_handle, &cbs, audio_queue));
    ESP_LOGI(TAG, "continuous mode enable");
    ESP_ERROR_CHECK(dac_continuous_enable(audio_data.dac_handle));
    ESP_LOGI(TAG, "Starting asynchronous write start");
    ESP_ERROR_CHECK(dac_continuous_start_async_writing(audio_data.dac_handle));
    ESP_LOGI(TAG, "Starting asynchronous write");
    audio_data.mode = 1;
    dac_write_data_asynchronously_cycle(audio_data.dac_handle, audio_queue, mode);

}

static void control_task(void *param)
{
    ESP_LOGI(TAG, "start play 1 for 2 second");

    start_play(TRACK_BACKGROUND, MODE_CYCLE);
    vTaskDelay(pdMS_TO_TICKS(2000));
    ESP_LOGI(TAG, "swicth single toot");
    switch_play(TRACK_TOOT, MODE_SINGLE);
    vTaskDelay(pdMS_TO_TICKS(5000));
    ESP_LOGI(TAG, "start play 3 for .5  second");

    switch_play(TRACK_TONE, MODE_CYCLE);
    vTaskDelay(pdMS_TO_TICKS(1500));

ESP_LOGI(TAG, "start play 1 for 2 second");
    switch_play(TRACK_BACKGROUND, MODE_CYCLE);
      vTaskDelay(pdMS_TO_TICKS(2000));
    ESP_LOGI(TAG, "stop play");



    stop_play();
    vTaskDelete(NULL);
    ESP_LOGI(TAG, "control complete");
}

void test_audio()
{
    size_t data_size_tractor;
    uint32_t sample_rate_tractor;
    uint8_t *data_tractor = process_wave("/spiffs/tractor.wav", 8, &data_size_tractor, &sample_rate_tractor);

    size_t data_size_toot;
    uint32_t sample_rate_toot;
    uint8_t *data_toot = process_wave("/spiffs/toot.wav", 8, &data_size_toot, &sample_rate_toot);
     size_t data_size_tone;
    uint32_t sample_rate_tone;
    uint8_t *data_tone = process_wave("/spiffs/tone.wav", 8, &data_size_tone, &sample_rate_tone);

    audio_data.buffers[TRACK_BACKGROUND].data = data_tractor;
    audio_data.buffers[TRACK_BACKGROUND].size = data_size_tractor;
    audio_data.buffers[TRACK_TOOT].data = data_toot;
    audio_data.buffers[TRACK_TOOT].size = data_size_toot;
    audio_data.buffers[TRACK_TONE].data = data_tone;
    audio_data.buffers[TRACK_TONE].size = data_size_tone;
    audio_data.mode = 1;

    ESP_LOGI(TAG, "Allocated memory for audio data: %d bytes %" PRIu32 " sample_rate", data_size_tractor, sample_rate_tractor);
    ESP_LOGI(TAG, "Allocated memory for audio data: %d bytes %" PRIu32 " sample_rate", data_size_toot, sample_rate_toot);
    ESP_LOGI(TAG, "Allocated memory for audio data: %d bytes %" PRIu32 " sample_rate", data_size_tone, sample_rate_tone);

    xTaskCreate(control_task, "control_task", 2048, NULL, 5, NULL);
    xTaskCreate(dac_dma_write_task, "dac_dma_write_task", 2048, &audio_data, 5, NULL);
}

esp_err_t play_wav(const char *file_path)
{
    size_t data_size;
    uint32_t sample_rate;
    uint8_t *data = process_wave(file_path, 8, &data_size, &sample_rate);
    audio_data.buffers[0].data = data;
    audio_data.buffers[0].size = data_size;
    audio_data.buffers[1].data = data;
    audio_data.buffers[1].size = data_size;
    audio_data.mode = 1;

    if (data == NULL)
    {
        ESP_LOGE(TAG, "Failed to process WAV file: %s", file_path);
        return ESP_FAIL;
    }

    ESP_LOGI(TAG, "Allocated memory for audio data: %d bytes %" PRIu32 " sample_rate", data_size, sample_rate);

    play_audio(1, sample_rate);

    free(data);
    ESP_LOGI(TAG, "Finished playing WAV file: %s", file_path);
    return ESP_OK;
}
