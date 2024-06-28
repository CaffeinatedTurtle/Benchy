#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>

uint8_t *process_wave(const char *filename, int target_bits, size_t *data_size, uint32_t *sample_rate) {
    FILE *wave_file = fopen(filename, "rb");
    if (wave_file == NULL) {
        fprintf(stderr, "Could not open wave file %s\n", filename);
        return NULL;
    }

    char chunk_id[4];
    fread(chunk_id, 1, 4, wave_file);
    if (strncmp(chunk_id, "RIFF", 4) != 0) {
        fprintf(stderr, "Not a valid RIFF file: %s\n", filename);
        fclose(wave_file);
        return NULL;
    }

    fseek(wave_file, 4, SEEK_CUR); // Skip ChunkSize

    char format[4];
    fread(format, 1, 4, wave_file);
    if (strncmp(format, "WAVE", 4) != 0) {
        fprintf(stderr, "Not a valid WAVE file: %s\n", filename);
        fclose(wave_file);
        return NULL;
    }

    char subchunk1_id[4];
    fread(subchunk1_id, 1, 4, wave_file);
    if (strncmp(subchunk1_id, "fmt ", 4) != 0) {
        fprintf(stderr, "Invalid subchunk1 ID: %s\n", filename);
        fclose(wave_file);
        return NULL;
    }

    uint32_t subchunk1_size;
    fread(&subchunk1_size, 4, 1, wave_file);

    uint16_t audio_format;
    fread(&audio_format, 2, 1, wave_file);

    uint16_t num_channels;
    fread(&num_channels, 2, 1, wave_file);

    fread(sample_rate, 4, 1, wave_file);
    printf("Sample rate: %" PRIu32 " Hz\n", *sample_rate);

    fseek(wave_file, 6, SEEK_CUR); // Skip ByteRate and BlockAlign

    uint16_t bits_per_sample;
    fread(&bits_per_sample, 2, 1, wave_file);

    char subchunk2_id[4];
    fread(subchunk2_id, 1, 4, wave_file);
    while (strncmp(subchunk2_id, "data", 4) != 0) {
        uint32_t subchunk_size;
        fread(&subchunk_size, 4, 1, wave_file);
        fseek(wave_file, subchunk_size, SEEK_CUR);
        fread(subchunk2_id, 1, 4, wave_file);
    }

    uint32_t subchunk2_size;
    fread(&subchunk2_size, 4, 1, wave_file);

    int sample_size = bits_per_sample / 8;
    int num_samples = subchunk2_size / sample_size;

    uint32_t scale_val = (1 << target_bits) - 1;
    uint32_t cur_lim = (1 << bits_per_sample) - 1;

    *data_size = num_samples * (target_bits / 8);
    printf("Data size: %zu\n", *data_size);
    uint8_t *data = (uint8_t *)malloc(*data_size);
    if (data == NULL) {
        fprintf(stderr, "Memory allocation failed\n");
        fclose(wave_file);
        return NULL;
    }

    for (int i = 0; i < num_samples; i++) {
        uint32_t sample = 0;
        fread(&sample, sample_size, 1, wave_file);

        sample = sample * scale_val / cur_lim;
        sample = (sample + (scale_val + 1) / 2) & scale_val;

        if (target_bits == 8) {
            data[i] = (uint8_t)sample;
        } else if (target_bits == 16) {
            ((uint16_t *)data)[i] = (uint16_t)sample;
        } else if (target_bits == 32) {
            ((uint32_t *)data)[i] = sample;
        }
    }

    fclose(wave_file);
    return data;
}
