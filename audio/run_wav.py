# Implemented functions
from wav_functions import get_freq
from wav_functions import write_freq
import matplotlib.pyplot as plt

# Test reading some files
gravel_file = "gravel.wav"
pavement_file = "pavement.wav"
test_file = "test.wav"
pavement_data = get_freq(pavement_file)
gravel_data = get_freq(gravel_file)

# Plot two fft'd wav file
fig, ax = plt.subplots(nrows=1, ncols=2)

ax[0].plot(gravel_data)
ax[0].set_xlabel("f")
ax[0].set_ylabel("A")
ax[0].set_title("FFT of .wav file \"%s\"" % gravel_file)

ax[1].plot(pavement_data)
ax[1].set_xlabel("f")
ax[1].set_ylabel("A")
ax[01].set_title("FFT of .wav file \"%s\"" % pavement_file)

plt.show()

# Test writing a 1 kHz signal
#t = 10 # Seconds
#f = 1000 # Sine constants
#amp = 16000 # 16 bit max
#samp_rate = 48000 # Commonly used in audio recordings

# Write a wav file
#write_freq(test_file, f, t, samp_rate, amp)
