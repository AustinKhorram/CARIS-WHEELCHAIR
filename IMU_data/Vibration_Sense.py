# TODO: 
# The expectation is that the variance will be so distinct for a 
# rough surface such as gravel or pavement that one can use a simple 
# threshold algorithm for detecting such terrains

# The other algorithm is to decompose the signal into its frequency components
# and then feed those components into a neural network to train 

from scipy.signal import find_peaks
import numpy as np
from tensorflow.keras.datasets import cifar10
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, Activation, Flatten
from tensorflow.keras.layers import Conv2D, MaxPooling2D
from matplotlib import pyplot as plt
import os 

# The current source directory is inside the folder housing this script
SOURCE_DIR = "."

TIMESTAMP_INDEX = 0
X_ACC_INDEX     = 1
Y_ACC_INDEX     = 2
Z_ACC_INDEX     = 3

# Path to directory detailing the location of the IMU data 
# The data is organized according to a comma separate values (csv)
# The first column represents the time stamp, the subsequent rows represent
# the linear accelerations sampled in the x, y, and z axes
def parseCSV(pathToFile, noOfColumns):
	
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
	for line in textfile:
		# Separate the line around the comma ',' and then append the 
		# row data to the overall data set
		row_data = line.strip("\n").split(",")
		# Ignore the last element in the list because it is empty
		for i, dataPoint in enumerate(row_data[:-1]):
			data[i].append(dataPoint)

	return data

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

def plotAcc (time, accX, accY, accZ, title):
	print(title)
	plt.subplot(2,2,1),plt.plot(time, accX, 'r')
	plt.title('X Acceleration') #plt.xticks([]), plt.yticks([])
	plt.subplot(2,2,2),plt.plot(time, accY, 'g')
	plt.title('Y Acceleration') 
	plt.subplot(2,2,3),plt.plot(time, accZ)
	plt.title('Z Acceleration') 
	plt.show()

for root, dirnames, filenames in os.walk(SOURCE_DIR):
	for filename in filenames:
		if filename.endswith(('.txt')):
			IMU_data = parseCSV(filename, 4)

			"""
			# Plot all three acceleration graphs 
			plotAcc(IMU_data[TIMESTAMP_INDEX], \
					IMU_data[X_ACC_INDEX],     \
				    IMU_data[Y_ACC_INDEX],     \
				    IMU_data[Z_ACC_INDEX],
				    filename)
			"""
			print(IMU_data[X_ACC_INDEX])
			print([str(float(x) * -1) for x in IMU_data[X_ACC_INDEX]])
			
			plt.show(block=True)
