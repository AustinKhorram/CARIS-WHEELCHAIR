import numpy as np

# Returns a fft of the input data with max amplitude = 1
def normalized_fft(data=[]):
    data_fft = np.abs(np.fft.fft(data)) # abs() converts complex to real valued

    max_amp = np.amax(data_fft) # Normalize by max amplitude
    data_fft_norm = np.multiply(data_fft, 1.0/max_amp)

    return data_fft_norm
