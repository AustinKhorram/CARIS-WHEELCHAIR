# Implemented functions
from wav_functions import get_array
from wav_functions import write_freq
from wav_functions import normalized_fft
import matplotlib.pyplot as plt
import numpy as np

# Test reading some files
gravel_file = "gravel.wav"
pavement_file = "pavement.wav"
test_file = "test.wav"

pavement_data = get_array(pavement_file)
gravel_data = get_array(gravel_file)
pavement_fft = normalized_fft(pavement_data)
gravel_fft = normalized_fft(gravel_data)
pavement_mean = 100*np.abs(np.average(pavement_data))
gravel_mean = np.abs(np.average(gravel_data))

# Plot two fft'd wav file
fig_f, ax = plt.subplots(nrows=2, ncols=2)

ax[0][0].plot(gravel_data, label="Mean A = %s" % gravel_mean)
ax[0][0].legend()
ax[0][0].set_xlabel("t [ms]")
ax[0][0].set_ylabel("A")
ax[0][0].set_title("Audio \"%s\"" % gravel_file)

ax[0][1].plot(pavement_data, label="Mean A = %s" % pavement_mean)
ax[0][1].legend()
ax[0][1].set_xlabel("t [ms]")
ax[0][1].set_ylabel("A")
ax[0][1].set_title("Audio \"%s\"" % pavement_file)

ax[1][0].plot(gravel_fft)
ax[1][0].set_xlabel("f [Hz]")
ax[1][0].set_ylabel("A")
ax[1][0].set_title("Normalized FFT of \"%s\"" % gravel_file)

ax[1][1].plot(pavement_fft)
ax[1][1].set_xlabel("f [Hz]")
ax[1][1].set_ylabel("A")
ax[1][1].set_title("Normalized FFT of \"%s\"" % pavement_file)

plt.show()

# Test writing a 1 kHz signal
#t = 10 # Seconds
#f = 1000 # Sine constants
#amp = 16000 # 16 bit max
#samp_rate = 48000 # Commonly used in audio recordings

# Write a wav file
#write_freq(test_file, f, t, samp_rate, amp)
