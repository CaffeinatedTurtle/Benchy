import os
import struct

def get_wave_array_str(filename, target_bits):
    try:
        with open(filename, 'rb') as wave_file:
            # Read and parse WAV header
            riff = wave_file.read(4)
            if riff != b'RIFF':
                raise ValueError("Not a WAV file (missing RIFF header)")

            size = struct.unpack('<I', wave_file.read(4))[0]
            wave_format = wave_file.read(4)
            if wave_format != b'WAVE':
                raise ValueError("Not a WAV file (missing WAVE header)")

            fmt_chunk = wave_file.read(4)
            while fmt_chunk != b'fmt ':
                wave_file.seek(-3, 1)
                fmt_chunk = wave_file.read(4)

            fmt_size = struct.unpack('<I', wave_file.read(4))[0]
            audio_format = struct.unpack('<H', wave_file.read(2))[0]
            channels = struct.unpack('<H', wave_file.read(2))[0]
            sample_rate = struct.unpack('<I', wave_file.read(4))[0]
            byte_rate = struct.unpack('<I', wave_file.read(4))[0]
            block_align = struct.unpack('<H', wave_file.read(2))[0]
            bits_per_sample = struct.unpack('<H', wave_file.read(2))[0]

            # Seek past any extra data in the fmt chunk
            if fmt_size > 16:
                wave_file.read(fmt_size - 16)

            # Find data chunk
            data_chunk = wave_file.read(4)
            while data_chunk != b'data':
                wave_file.seek(-3, 1)
                data_chunk = wave_file.read(4)

            data_size = struct.unpack('<I', wave_file.read(4))[0]

            # Read and process audio data
            array_str = ''
            bytes_per_sample = bits_per_sample // 8
            scale_val = (1 << target_bits) - 1

            for i in range(data_size // bytes_per_sample):
                raw_data = wave_file.read(bytes_per_sample)
                if len(raw_data) < bytes_per_sample:
                    raise ValueError("Unexpected end of audio data")
                val = int.from_bytes(raw_data, byteorder='little', signed=(bits_per_sample < 8))
                val = val * scale_val / ((1 << bits_per_sample) - 1)
                val = int(val + ((scale_val + 1) // 2)) & scale_val
                array_str += '0x%x, ' % (val)
                if (i + 1) % 16 == 0:
                    array_str += '\n'

            return array_str

    except Exception as e:
        print(f"Error processing {filename}: {str(e)}")
        return ''


def gen_wave_tables(wav_file_list, output_dir, scale_bits=8):
    try:
        os.makedirs(output_dir, exist_ok=True)
        for wav in wav_file_list:
            output_filename = os.path.join(output_dir, os.path.splitext(wav)[0] + '.h')
            with open(output_filename, 'w') as audio_table:
                print('#include <stdio.h>', file=audio_table)
                print('const unsigned char audio_table[] = {', file=audio_table)
                print(get_wave_array_str(filename=wav, target_bits=scale_bits), file=audio_table)
                print('};\n', file=audio_table)
            print(f'Generated {output_filename}')
    except Exception as e:
        print(f"Error generating wave tables: {str(e)}")


if __name__ == '__main__':
    print('Generating audio arrays...')
    wav_list = [wavefile for wavefile in os.listdir('./') if wavefile.endswith('.wav')]
    gen_wave_tables(wav_file_list=wav_list, output_dir='output_directory', scale_bits=8)

