import os 
import numpy as np
import tensorflow as tf 
from matplotlib import pyplot as plt
from scipy.fftpack import fft 
from scipy.signal import butter, lfilter, find_peaks, filtfilt, lfilter_zi
from tensorflow.keras.datasets import cifar10
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, Activation, Flatten
from tensorflow.keras.layers import Conv2D, MaxPooling2D

SOURCE_DIR = "."
TITLES          = ["X Acceleration", "Y Acceleration", "Z Acceleration", \
                   "X Linear Acceleration", "Y Linear Acceleration", "Z Linear Acceleration", \
                   "X Gyroscope", "Y Gyroscope", "Z Gyroscope"]


# Path to directory detailing the location of the IMU data 
# The data is organized according to a comma separate values (csv)
# The first column represents the time stamp, the subsequent rows represent
# the linear accelerations sampled in the x, y, and z axes
def parseCSVAndRotate(pathToFile, noOfColumns, delimiter, readFromLine):
	
	# Final return value is a rotated matrix of the CSVC
	# i.e. the columns are mapped as rows and columns as rows
	#   
	# time:  [[t1 t2  ... tn],
	# x acc:  [x1 x2  ... xn],

	# y acc:  [y1 y2  ... yn],
	# z acc:  [z1 z2  ... zn]]

	#Create the matrix
	data = []
	for column in range(noOfColumns):
		data.append([])

	textfile = open(pathToFile)
	for i, line in enumerate(textfile):
		if (i < readFromLine):
			continue 
		# Separate the line around the comma ',' and then append the 
		# row data to the overall data set
		row_data = line.strip("\n").split(delimiter)
		# Ignore the last element in the list because it is empty
		for i, dataPoint in enumerate(row_data[:-1]):
			data[i].append(dataPoint)

	return data

def splitArrayIntoNEvenSubArrays(data_set, no_of_groups=1):
    length = len(data_set)
    return [ data_set[i*length // no_of_groups: (i+1)*length // no_of_groups] 
             for i in range(no_of_groups) ]

### FFT-Algorithm Relevant Functions ###

def dataSetIsSizePowerOf2(data_set):
	size = len(data_set)
	return size > 0 and ((size & (size - 1)) == 0)

def padZeros(data_set, size):
	"""
	If the data_set has a size that's not a power of 2 then 
	pad it with an equal number of zeros in the front and back 
	"""
	zeros_left   = tf.zeros([int(data_set.get_shape()[0]), int((N - input_length+1) / 2)])
	zeros_right  = tf.zeros([int(data_set.get_shape()[0]), int((N - input_length) / 2)])
	input_padded = tf.concat([zeros_left, input, zeros_right], axis=1)

	fftbuffer_left  = tf.slice(data_set, [0, int(N/2)], [-1, -1])
	fftbuffer_right = tf.slice(data_set, [0, 0],   [-1, int(N/2)])
	fftbuffer       = tf.concat([fftbuffer_left, fftbuffer_right], axis=1)

def butter_lowpass(cutoff, fs, order=5):
    nyq = 0.5 * fs
    cut = cutoff / nyq
    b, a = butter(N=order, Wn=cut, btype='low')
    return b, a


def butter_lowpass_filter(data, cutoff, fs, order=5):
    b, a = butter_lowpass(cutoff, fs, order=order)
    y = lfilter(b, a, data)
    return y

def averageValsFilter(data, sampleSize):
	subX = splitArrayIntoNEvenSubArrays(data, len(data)//sampleSize)
	subX = [sum(subArr)/len(subArr) for subArr in subX]

	return subX

### MAIN ###
def analyze():

	# Take the average of all the frequency components
	frequencies_per_terrain_type   = {}
	no_of_terrain_data_sets        = {}

	for root, dirnames, filenames in os.walk(SOURCE_DIR):
		for filename in filenames:
			if filename.endswith(('.txt','.csv')):
				
				# The terrain type should be appended as the first word, delimited
				# by "_", of the file name
				terrainType = filename.split("_")[0]
				print(terrainType)

				# If the type of terrain hasn't been encountered before initialize
				# the frequency sets, index maps, and counters
				if terrainType not in no_of_terrain_data_sets:
					frequencies_per_terrain_type[terrainType]   = [] 
					no_of_terrain_data_sets[terrainType]        = 1
				# The terrain type has been previously encountered 
				else:
					no_of_terrain_data_sets[terrainType] += 1

				filenameWithoutExt = filename.split(".")[0]

				IMU_data = parseCSVAndRotate(filename,       \
					                         noOfColumns=10, \
					                         delimiter=";",  \
					                         readFromLine=2)
				
				for i, data_set in enumerate(IMU_data[:-1]):
					data_set = [float(i) for i in data_set]

					# Create a butterworth filter to clean up the noise in the IMU data
					CUTOFF_FREQ   = 40  # Hz
					ORDER         = 6
					SAMPLING_RATE = 200 # Hz
					AVERAGE_SAMP  = 10
					filter_data_set = butter_lowpass_filter (data    = data_set,     \
						                                     cutoff  = CUTOFF_FREQ,  \
						                                     fs      = SAMPLING_RATE,\
						                                     order   = ORDER)

					#average_data_set = averageValsFilter(data_set, AVERAGE_SAMP)
					data_to_plot = filter_data_set
					
					N = len(data_to_plot)

					# Make sure that the data set isn't empty
					if (N != 0):
						yr = fft(data_to_plot)
						y  = 2 * np.abs(yr[0:np.int(N/2)]) / N
						x = np.linspace(0.0, SAMPLING_RATE/2, int(N/2))
						
						print(len(y))
						print(len(x))
						"""
						fig, ax  = plt.subplots()
						lines = ax.plot(x, y) 
						
						ax.set_xlabel('Frequency (Hz)')
						ax.set_ylabel('Amplitude')

						outputFileName = TITLES[i] + ' ' + filenameWithoutExt
						plotName = terrainType + ' ' + TITLES[i]
						
						if i in list(range(6,9)):
							ax.set_ybound(lower=0, upper=0.1)
						else:
							ax.set_ybound(lower=0, upper=1)

						ax.set_title(plotName)
						fig.savefig(outputFileName + ".png")

						plt.close()
						"""


if __name__ == '__main__':
	analyze()	