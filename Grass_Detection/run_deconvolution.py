from deconvolution_functions import read_img_file, deblur_wiener, deblur_lucy
import matplotlib.pyplot as plt

img = read_img_file('blurry_images/blurry_1.jpg') # 2 kinds of filter
wiener_img = deblur_wiener(img)
richardson_lucy_img = deblur_lucy(img)

fig, ax = plt.subplots(nrows=1, ncols=3) # Plot comparision
plt.gray()

ax[0].imshow(img)
ax[0].axis('off')
ax[0].set_title('Original')
ax[1].imshow(wiener_img)
ax[1].axis('off')
ax[1].set_title('Wiener')
ax[2].imshow(richardson_lucy_img)
ax[2].axis('off')
ax[2].set_title('Richardson Lucy')

plt.show()
