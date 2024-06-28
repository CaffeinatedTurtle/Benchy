#ifndef AUDIO_PLAYER_H
#define AUDIO_PLAYER_H

#include "esp_err.h"

esp_err_t play_wav(const char *file_path);
void test_audio();
void start_play();
void stop_play();

#endif // AUDIO_PLAYER_H