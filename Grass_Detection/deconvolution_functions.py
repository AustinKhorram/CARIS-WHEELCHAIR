import numpy as np
import matplotlib.pyplot as plt

from skimage import io, restoration, color
from scipy.signal import convolve2d

# Read an image file and return it as a numpy array
def read_img_file(filename):
    img = io.imread(filename)
    return img

# Deblur an image file given an image array, using Wiener filter
# Will be converted to grayscale
def deblur_wiener(img):
    psf = np.ones((5, 5)) / 25 # Point spread function

    img_temp = color.rgb2gray(img)
    img_temp = convolve2d(img_temp, psf, 'same')
    img_temp += 0.1 * img_temp.std() * np.random.standard_normal(img_temp.shape)

    deconvolved, _ = restoration.unsupervised_wiener(img_temp, psf)

    return (deconvolved)

# Deblur an image file given an image array, using  filter
# Will be converted to grayscale
def deblur_lucy(img):
    psf = np.ones((5, 5)) / 25 # Point spread function

    img_temp = color.rgb2gray(img)
    img_temp = convolve2d(img_temp, psf, 'same')

    deconvolved = restoration.richardson_lucy(img_temp, psf, iterations=30)

    return deconvolved
