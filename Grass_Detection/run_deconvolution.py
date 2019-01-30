from deconvolution_functions import read_img_file, deblur_wiener, deblur_lucy
import matplotlib.pyplot as plt

img = read_img_file("blurry_1.jpg")
wiener_img = deblur_wiener(img)
richardson_lucy_img = deblur_lucy(img)

fig, ax = plt.subplots(nrows=1, ncols=3, figsize=(8, 5))
plt.gray()

ax[0].imshow(img)
ax[1].imshow(wiener_img)
ax[2].imshow(richardson_lucy_img)

plt.show()
