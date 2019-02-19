import cv2
import os 
import numpy as np
from math import atan2, cos, sin, sqrt, pi
from matplotlib import pyplot as plt

SOURCE_DIR = "grass_images"

# HSV has limits H: 0 - 180 
#                S: 0 - 255
#                V: 0 - 255
GREEN_MIN = np.array([21, 80,  80], np.uint8)
GREEN_MAX = np.array([53, 255, 255], np.uint8)
SATURATION_FACTOR = 1.1

def showGradient (orig, lap, sobx, soby):
	plt.subplot(2,2,1),plt.imshow(orig, cmap = 'gray')
	plt.title('Original'), plt.xticks([]), plt.yticks([])
	plt.subplot(2,2,2),plt.imshow(lap, cmap = 'gray')
	plt.title('Laplacian'), plt.xticks([]), plt.yticks([])
	plt.subplot(2,2,3),plt.imshow(sobx,cmap = 'gray')
	plt.title('Sobel X'), plt.xticks([]), plt.yticks([])
	plt.subplot(2,2,4),plt.imshow(soby,cmap = 'gray')
	plt.title('Sobel Y'), plt.xticks([]), plt.yticks([])
	plt.show()

def drawAxis(img, p_, q_, colour, scale):
    p = list(p_)
    q = list(q_)
    
    angle = atan2(p[1] - q[1], p[0] - q[0]) # angle in radians
    hypotenuse = sqrt((p[1] - q[1]) * (p[1] - q[1]) + (p[0] - q[0]) * (p[0] - q[0]))
    # Here we lengthen the arrow by a factor of scale
    q[0] = p[0] - scale * hypotenuse * cos(angle)
    q[1] = p[1] - scale * hypotenuse * sin(angle)
    cv2.line(img, (int(p[0]), int(p[1])), (int(q[0]), int(q[1])), colour, 1, cv2.LINE_AA)
    # create the arrow hooks
    p[0] = q[0] + 9 * cos(angle + pi / 4)
    p[1] = q[1] + 9 * sin(angle + pi / 4)
    cv2.line(img, (int(p[0]), int(p[1])), (int(q[0]), int(q[1])), colour, 1, cv2.LINE_AA)
    p[0] = q[0] + 9 * cos(angle - pi / 4)
    p[1] = q[1] + 9 * sin(angle - pi / 4)
    cv2.line(img, (int(p[0]), int(p[1])), (int(q[0]), int(q[1])), colour, 1, cv2.LINE_AA)
    
def getOrientation(pts, img):
    
    sz = len(pts)
    data_pts = np.empty((sz, 2), dtype=np.float64)
    for i in range(data_pts.shape[0]):
        data_pts[i,0] = pts[i,0,0]
        data_pts[i,1] = pts[i,0,1]
    # Perform PCA analysis
    mean = np.empty((0))
    mean, eigenvectors, eigenvalues = cv2.PCACompute(data_pts, mean)
    # Store the center of the object
    cntr = (int(mean[0,0]), int(mean[0,1]))
    
    
    cv2.circle(img, cntr, 3, (255, 0, 255), 2)
    p1 = (cntr[0] + 0.02 * eigenvectors[0,0] * eigenvalues[0,0], cntr[1] + 0.02 * eigenvectors[0,1] * eigenvalues[0,0])
    p2 = (cntr[0] - 0.02 * eigenvectors[1,0] * eigenvalues[1,0], cntr[1] - 0.02 * eigenvectors[1,1] * eigenvalues[1,0])
    drawAxis(img, cntr, p1, (0, 255, 0), 1)
    drawAxis(img, cntr, p2, (255, 255, 0), 5)
    angle = atan2(eigenvectors[0,1], eigenvectors[0,0]) # orientation in radians
    
    return angle

def processImages(directory):
	for root, dirnames, filenames in os.walk(SOURCE_DIR):
		for filename in filenames:
			# Read the image
			location = os.path.join(root, filename)
			print ("\n" + location )
			img = cv2.imread(location)

			# Convert the image from a BGR pattern to an HSV
			hsv_img = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
			# Convert the image from a gray scale
			gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
			
			# Use blind deconvolution to deblur images and compensate
			# for the acceleration that the wheel chair regularly
			# undergoes
			# TODO

			# Increase the saturation of the image to accentuate
			# the colored components
			hsv_img[:,:,1] = hsv_img[:,:,1] * SATURATION_FACTOR

		 	# Now apply a bitmask to the image by blacking out those
		 	# pixels which do not meet the minimum HSV standards for 
		 	# green and grab the binary output
			frame_threshed = cv2.inRange(hsv_img, GREEN_MIN, GREEN_MAX)

			# Identify regions of interest and calculate the subimage's 
			# gradient
			gray_threshed = cv2.bitwise_and(gray, gray, mask = frame_threshed)
			laplacian = cv2.Laplacian(gray_threshed, cv2.CV_64F)
			sobelx = cv2.Sobel(gray_threshed, cv2.CV_64F, 1, 0, ksize=5)
			sobely = cv2.Sobel(gray_threshed, cv2.CV_64F, 0, 1, ksize=5)

			# Analyze the images for some directional or chaotic component:
			# grass patches viewed from above or close are never smooth
			# and always evince some texture
			mean = np.empty((0))
			meanLap, eigVecLap   = cv2.PCACompute(laplacian, mean)
			meanSobx, eigVecSobx = cv2.PCACompute(sobelx, mean)
			meanSoby, eigVecSoby = cv2.PCACompute(sobely, mean)
			
			print (np.linalg.norm(eigVecLap))
			print (np.linalg.norm(eigVecSobx))
			print (np.linalg.norm(eigVecSoby))
			# Save the images 
			#cv2.imwrite(location + "_output_thresh.jpg", frame_threshed)
			#cv2.imwrite(location + "_contours.jpg", img)

if __name__ == '__main__':
	processImages(SOURCE_DIR)