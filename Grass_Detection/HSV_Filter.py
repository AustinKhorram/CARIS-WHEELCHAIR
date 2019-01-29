import cv2
import os 
import numpy as np

SOURCE_DIR = "grass_images"

# HSV has limits H: 0 - 180 
#                S: 0 - 255
#                V: 0 - 255
GREEN_MIN = np.array([21, 75,  75], np.uint8)
GREEN_MAX = np.array([53, 255, 255], np.uint8)

def processImages(directory):
	for root, dirnames, filenames in os.walk(SOURCE_DIR):
		for filename in filenames:
			tempName = os.path.splitext(filename)[0]
			print ("\n" + tempName + "_output.jpg" )

			try:
				# Read the image
				img = cv2.imread(root + "/" + filename)

				# Convert the image from a BGR pattern to an HSV
				hsv_img = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)

				# Increase the saturation of the image to accentuate
				# the colored components
				hsv_img[:,:,1] = hsv_img[:,:,1]*1.4

			 	# Now apply a bitmask to the image by blacking out those
			 	# pixels which do not meet the minimum HSV standards for 
			 	# green
				frame_threshed = cv2.inRange(hsv_img, GREEN_MIN, GREEN_MAX)
				
				# Save the image as
				cv2.imwrite(root + "/" + filename + "_output.jpg", frame_threshed)

			except:
				continue

if __name__ == '__main__':
	processImages(SOURCE_DIR)