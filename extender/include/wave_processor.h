#ifndef WAVE_PROCESSOR_H
#define WAVE_PROCESSOR_H

#include <stdint.h>
#include <stdio.h>

uint8_t *process_wave(const char *filename, int target_bits, size_t *data_size,uint32_t *sample_rate);

#endif // WAVE_PROCESSOR_H
