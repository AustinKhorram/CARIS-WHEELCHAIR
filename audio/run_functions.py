# Implemented functions
from wav_functions import get_freq
from wav_functions import write_freq
import matplotlib.pyplot as plt

# Test reading some files
gravel_file = "gravel.wav"
pavement_file = "pavement.wav"
test_file = "test.wav"

# Plot a fft'd wav file
pavement_data = get_freq(pavement_file)
gravel_data = get_freq(gravel_file)
plt.plot(gravel_data[0:pavement_data.size], label=gravel_file)
plt.plot(pavement_data, label=pavement_file)
plt.plot(gravel_data[0:pavement_data.size] - pavement_data, label="difference")
plt.title("FFT of .wav Files {} and {}".format(gravel_file, pavement_file))
plt.xlabel("f")
plt.ylabel("A")
plt.legend()
plt.show()

# Test writing a 1 kHz signal
t = 10 # Seconds
f = 1000 # Sine constants
amp = 16000 # 16 bit max
samp_rate = 48000 # Commonly used in audio recordings

# Write a wav file
write_freq(test_file, f, t, samp_rate, amp)
