BONUS showSeams function:

First, we create duplication of the working image.
We call reduceImageWidth() in order to fill trueBestSeamsIndices field with the best k seams.
(Note that the indexes matched to the original image)
In the 'for' loop, we iterate the seams in trueBestSeamsIndices, and for each pixel(in the seam) we set a new color -given seamColorRGB

The function returns a new Image in the same size with all the chosen seams marked in red (as describe above).