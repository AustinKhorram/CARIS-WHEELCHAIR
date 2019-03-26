# TODO: 
# The expectation is that the variance will be so distinct for a 
# rough surface such as gravel or pavement that one can use a simple 
# threshold algorithm for detecting such terrains

# The other algorithm is to decompose the signal into its frequency components
# and then feed those components into a neural network to train 

from scipy.signal import find_peaks
import numpy as np
import tensorflow as tf 
from tensorflow.keras.datasets import cifar10
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, Activation, Flatten
from tensorflow.keras.layers import Conv2D, MaxPooling2D
from matplotlib import pyplot as plt
import os 

# The current source directory is inside the folder housing this script
SOURCE_DIR = "."

X_ACC_INDEX     = 0
Y_ACC_INDEX     = 1
Z_ACC_INDEX     = 2
TIMESTAMP_INDEX = 9
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


def findVarianceOfPositivePeaks(data_set):
	# Tuning parameters of the IMU analysis: 
	HEIGHT   = 0 # Required magnitude of peaks to qualify as a peak
	DISTANCE = 3 # Required minimal horizontal distance in samples between neighbouring peaks

	# Find all the peaks in the data
	peaks, _ = find_peaks(data_set)
	# Determine their variance 
	variance_peaks = np.var(peaks)
	return variance_peaks

def findVarianceOfNegativePeaks(data_set):
	# Tuning parameters of the IMU analysis: 
	HEIGHT   = 0 # Required magnitude of peaks to qualify as a peak
	DISTANCE = 3 # Required minimal horizontal distance in samples between neighbouring peaks

	# Since valleys are essentially flipped peaks, simply flip the 
	# data_set about the x-axis and return the peaks
	flipped_data_set = [str(float(x) * -1) for x in data_set]
	return findVarianceOfPositivePeaks(flipped_data_set) 

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

### Data Visualization Helper Functions ### 
def plotAcc (time, accX, accY, accZ):
	plt.subplot(2,2,1),plt.plot(time, accX, 'r')
	plt.title('X Acceleration') #plt.xticks([]), plt.yticks([])
	plt.subplot(2,2,2),plt.plot(time, accY, 'g')
	plt.title('Y Acceleration') 
	plt.subplot(2,2,3),plt.plot(time, accZ)
	plt.title('Z Acceleration') 
	plt.show()

### MAIN ###
def analyze():
	for root, dirnames, filenames in os.walk(SOURCE_DIR):
		for filename in filenames:
			if filename.endswith(('.txt','.csv')):
				print(filename)

				IMU_data = parseCSVAndRotate(filename,       \
					                         noOfColumns=10, \
					                         delimiter=";",  \
					                         readFromLine=2)

				for i in range(1,3):
					# Plot all three acceleration graphs 
					plotAcc(IMU_data[TIMESTAMP_INDEX],   \
							IMU_data[X_ACC_INDEX + i*3], \
						    IMU_data[Y_ACC_INDEX + i*3], \
						    IMU_data[Z_ACC_INDEX + i*3])

				# Now find the positive and negative variances of the peaks
				# for each discrete IMU data set and plot the two dimensional 
				# graph, noting any discrepancies
				# The last index carries the time stamp so don't find the variance
				# of that one  
				PEAK_VAR_SAMPLE_SIZE = 50

				for i, data_set in enumerate([IMU_data[5],IMU_data[8]]):
					# Take each variance to be a sample size of 50 and then plot
					# it accordingly 
					sub_data_sets = splitArrayIntoNEvenSubArrays(data_set, \
						                                        len(data_set) // PEAK_VAR_SAMPLE_SIZE)
					pos_variances = []
					neg_variances = []

					for sub_data_set in sub_data_sets:
						pos_variances.append(findVarianceOfPositivePeaks(sub_data_set))
						neg_variances.append(findVarianceOfNegativePeaks(sub_data_set))

					plt.plot(neg_variances, pos_variances, 'ro')
					plt.title(TITLES[i])
					plt.show()

					#Try to also run FFT on the data set
					



if __name__ == '__main__':
	analyze()	