import numpy as np
import wave
import struct

# Returns an array of data from a wav file
def get_array(in_file):
    wav_file = wave.open(in_file, 'r')
    
    n_chan = wav_file.getnchannels() # Have to double number of reads if stereo
    n_frames = wav_file.getnframes(); # Entire number of frames in the file
    in_data = wav_file.readframes(n_frames)
    
    wav_file.close()
    
    data = np.array(struct.unpack('{n}h'.format(n=n_frames*n_chan), in_data))
    return data

# Returns a fft of the input data with max amplitude = 1
def normalized_fft(data=[]):
    data_fft = np.abs(np.fft.rfft(data, 70000)) # abs() converts complex to real valued

    max_amp = np.amax(data_fft) # Normalize by max amplitude
    data_fft_norm = np.multiply(data_fft, 1.0/max_amp)

    return data_fft_norm

def average_amp(data=[]):
# TODO: Implement a threshold to filter out bckgrnd sound
    return

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
