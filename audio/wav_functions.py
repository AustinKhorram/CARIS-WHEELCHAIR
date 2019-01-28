import numpy as np
import matplotlib.pyplot as plt
import wave
import struct

def get_freq(in_file):
    # Open our file and read out all of its frames
    wav_file = wave.open(in_file, 'r')

    n_chan = wav_file.getnchannels() # Have to double the number of reads if stereo
    n_frames = wav_file.getnframes(); # Entire num frames of the file

    in_data = wav_file.readframes(n_frames)
    wav_file.close()

    # FFT data and return it
    data = np.array(struct.unpack('{n}h'.format(n=n_frames*n_chan), in_data))
    data_fft = np.abs(np.fft.fft(data)) # abs() converts complex to real valued

    return data_fft

def write_freq(in_file, freq, samp_time, samp_rate, amp):
    n_samp = int(samp_rate*samp_time) # Sample_rate * time

    # [] Puts the sine wave into a list
    sine_wave = [np.sin(2 * np.pi * freq * x / samp_rate) for x in range(n_samp)]

    # .wav constants
    n_frame = n_samp
    c_type = "NONE" # Compression type
    c_name = "not compressed" # Compression name
    n_chan = 1
    samp_width = 2

    # Write to a .wav file
    wav_file = wave.open(in_file, 'w')
    wav_file.setparams((n_chan, samp_width, samp_rate, n_frame, c_type, c_name))

    for s in sine_wave:
        wav_file.writeframes(struct.pack('h', int(s * amp)))


# Test reading some files
gravel_file = "gravel.wav"
pavement_file = "pavement.wav"
test_file = "test.wav"

# Plot a fft'd wav file
pavement_data = get_freq(pavement_file)
gravel_data = get_freq(gravel_file)
plt.plot(pavement_data, label=pavement_file)
plt.plot(gravel_data[0:pavement_data.size], label=gravel_data)
plt.title("FFT of .wav Files")
plt.xlabel("f")
plt.ylabel("A")
plt.show()

# Test writing a 1 kHz signal

t = 10 # Seconds
f = 1000 # Sine constants
amp = 16000 # 16 bit max
samp_rate = 48000 # Commonly used in audio recordings

# Write a wav file
write_freq(test_file, f, t, samp_rate, amp)
