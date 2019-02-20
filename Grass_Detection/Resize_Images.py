import cv2
import os, traceback 
from PIL import Image, ExifTags

SOURCE_DIR = "grass_images"
resolution = 504, 378

def processImages(directory):

    for root, dirnames, filenames in os.walk(SOURCE_DIR):
        for i, filename in enumerate(filenames):
            # Read the image
            location = os.path.join(root, filename)
            print ("\n" + location )
            img = Image.open(location)

            # most smartphones will store exf metadata 
            if hasattr(img, '_getexif'):
                for orientation in ExifTags.TAGS.keys():
                    if ExifTags.TAGS[orientation] == 'Orientation':
                        break
                e = img._getexif() # Returns NONE if no EXIF data
                if e is not None:
                    exif = dict(e.items())
                    orientation = exif[orientation]

                    if orientation == 3:
                        img = img.transpose(Image.ROTATE_180)
                    elif orientation == 6:
                        img = img.transpose(Image.ROTATE_270)
                    elif orientation == 8:
                        img = img.transpose(Image.ROTATE_90)

            img = img.resize(resolution)
            img.save("XKDgrass" + str(i) + ".jpg", "JPEG")

if __name__ == '__main__':
    processImages(SOURCE_DIR)