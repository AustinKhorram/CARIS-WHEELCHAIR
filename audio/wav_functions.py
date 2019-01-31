import numpy as np
import wave
import struct
import normalized_fft

# Return the FFT of a .wav file with amplitudes scaled such that the max is one
def get_freq(in_file):
    wav_file = wave.open(in_file, 'r')

    n_chan = wav_file.getnchannels() # Have to double number of reads if stereo
    n_frames = wav_file.getnframes(); # Entire number of frames in the file
    in_data = wav_file.readframes(n_frames)

    wav_file.close()

    data = np.array(struct.unpack('{n}h'.format(n=n_frames*n_chan), in_data))
    data_fft = normalized_fft.normalized_fft(data)

    return data_fft

# Write a signal of specified frequency to a .wav file, with other audio
# paramaters specified
def write_freq(in_file, freq, samp_time, samp_rate=48000, amp=16000):
    n_samp = samp_rate*samp_time # [] puts into a list
    sine_wave = [np.sin(2 * np.pi * freq * x / samp_rate) for x in range(n_samp)]

    c_type = "NONE" # Compression type
    c_name = "not compressed" # Compression name
    n_chan = 1
    samp_width = 2

    wav_file = wave.open(in_file, 'w')
    wav_file.setparams((n_chan, samp_width, samp_rate, n_samp, c_type, c_name))

    for s in sine_wave:
        wav_file.writeframes(struct.pack('h', int(s * amp)))
