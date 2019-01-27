import numpy as np
import wave
import struct

# Sine constants
freq = 1000
n_samp = 48000 # This is common for audio files
samp_rate = 48000.0
amp = 16000 # Amplitude of our wave

# [] Puts it into list
sine_wave = [np.sin(2 * np.pi * freq * x / samp_rate) for x in range(n_samp)]

# .wav constants
n_frame = n_samp
c_type = "NONE" # Compression type
c_name = "not compressed" # Compression name
n_chan = 1
samp_width = 2

# Test making a .wav file
file = "/wav/test.wav"
wav_file = wave.open(file, 'w')
wav_file.setparams((n_chan, samp_width, int(samp_rate), n_frame, c_type, c_name))

for s in sine_wave:
    wav_file.writeframes(struct.pack('h', int(s * amp)))
