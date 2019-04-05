# TODO: 
# The expectation is that the variance will be so distinct for a 
# rough surface such as gravel or pavement that one can use a simple 
# threshold algorithm for detecting such terrains

# The other algorithm is to decompose the signal into its frequency components
# and then feed those components into a neural network to train 

from scipy.signal import find_peaks
import numpy as np
from matplotlib import pyplot as plt
import os 

# The current source directory is inside the folder housing this script
SOURCE_DIR = "."

X_ACC_INDEX     = 0
Y_ACC_INDEX     = 1
Z_ACC_INDEX     = 2
LIN_ACC_OFFSET  = 3
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
	peaks    = peaks[np.logical_not(np.isnan(peaks))]
	
	# Determine their variance 
	variance_peaks = 0
	if (len(peaks) != 0):
		variance_peaks = np.var(peaks)
	
	return variance_peaks

def findVarianceOfNegativePeaks(data_set):
	# Tuning parameters of the IMU analysis: 
	HEIGHT   = 0 # Required magnitude of peaks to qualify as a peak
	DISTANCE = 3 # Required minimal horizontal distance in samples between neighbouring peaks

	# Since valleys are espeaks    = peaks[np.logical_not(np.isnan(peaks))]sentially flipped peaks, simply flip the 
	# data_set about the x-axis and return the peaks
	flipped_data_set = [str(float(x) * -1) for x in data_set]
	return findVarianceOfPositivePeaks(flipped_data_set) 

def centroid(data_setX, data_setY):
	sumX = np.sum(data_setX)
	sumY = np.sum(data_setY)
	return [sumX / len(data_setX), sumY / len(data_setY)]

def averageValsFilter(data, sampleSize):
	subX = splitArrayIntoNEvenSubArrays(data, len(data)//sampleSize)
	subX = [sum(subArr)/len(subArr) for subArr in subX]

	return subX

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
def main():
	for root, dirnames, filenames in os.walk(SOURCE_DIR):
		for filename in filenames:
			if filename.endswith(('.txt','.csv')):
				print(filename)

				terrainType        = filename.split("_")[0]
				filenameWithoutExt = filename.split(".")[0]


				IMU_data = parseCSVAndRotate(filename,       \
					                         noOfColumns=10, \
					                         delimiter=";",  \
					                         readFromLine=2)

				"""
				for i in range(1,3):
					# Plot all three acceleration graphs 
					plotAcc(IMU_data[TIMESTAMP_INDEX],   \
							averageValsFilter(IMU_data[X_ACC_INDEX + i*3], 5), \
						    averageValsFilter(IMU_data[Y_ACC_INDEX + i*3], 5), \
						    averageValsFilter(IMU_data[Z_ACC_INDEX + i*3], 5))
				"""

				FILTER_SAMPLE_SIZE = 5; 

				time      = [float(i) for i in IMU_data[TIMESTAMP_INDEX]]
				time_filt = time[::FILTER_SAMPLE_SIZE]

				# Now find the positive and negative variances of the peaks
				# for each discrete IMU data set and plot the two dimensional 
				# graph, noting any discrepancies
				# The last index carries the time stamp so don't find the variance
				# of that one  
				PEAK_VAR_SAMPLE_SIZE = 30

				# main the linear and gyroscopic accelerations which are 
				# indexed from 3 to 8
				for i, data_set in enumerate(IMU_data[LIN_ACC_OFFSET:-1]):
					# Take each variance to be a sample size of 50 and then plot
					# it accordingly 
					
					data_set      = [float(i) for i in data_set]
					data_set_comp = data_set[::FILTER_SAMPLE_SIZE]
					data_set_filt = averageValsFilter(data_set, FILTER_SAMPLE_SIZE)
					data_set_len  = len(data_set_filt)

					plt.title(terrainType + ' ' + TITLES[LIN_ACC_OFFSET + i])

					plt.plot(time_filt, data_set_comp, c='r')
					#plt.plot(time, data_set)

					plt.show()
					plt.close()

					data_set_to_use = data_set_comp
					no_of_groups = len(data_set_to_use) // PEAK_VAR_SAMPLE_SIZE

					samples      = splitArrayIntoNEvenSubArrays(data_set_to_use, no_of_groups)
					
					pos_variances = []
					neg_variances = []

					for sample in samples:
						pos_variances.append(findVarianceOfPositivePeaks(sample))
						neg_variances.append(findVarianceOfNegativePeaks(sample))
					
					#fig, ax  = plt.subplots()
					#lines = ax.plot(neg_variances, pos_variances, 'ro')
					
					#ax.set_xlabel('Frequency (Hz)')
					#ax.set_ylabel('Amplitude')
					
					#ax.set_title(plotName)
					#fig.savefig(plotName + ' ' + tempName + '.png')
					
					plt.plot(neg_variances, pos_variances, 'ro')
					plt.title(terrainType + ' ' + TITLES[LIN_ACC_OFFSET + i])
					plt.xlabel("Variance of Acceleration Valleys")
					plt.ylabel("Variance of Acceleration Peaks")
					plt.show()

					plt.close()

					



if __name__ == '__main__':
	main()	