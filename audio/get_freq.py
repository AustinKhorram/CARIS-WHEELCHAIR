import numpy as np
import matplotlib.pyplot as plt
import wave
import struct

# Constants
t = 10 # Number of seconds to sample
n_samp = 48000*t # This is common for audio files
frame_rate = "48000.0"

# Open a .wav file
in_file = "gravel.wav"
wav_file = wave.open(in_file, 'r') # Line ending r
in_data = wav_file.readframes(n_samp)
wav_file.close()

# Process and FFT it
data = np.array(struct.unpack('{n}h'.format(n=n_samp*2), in_data))
data_fft = np.abs(np.fft.fft(data)) # abs() converts complex to real valued

# Plot it
plt.plot(data_fft)
plt.title("FFT of .wav file \"%s\"" % in_file)
plt.xlabel("f")
plt.ylabel("A")
plt.show()
