package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;

public class SeamsCarver extends ImageProcessor {

	// MARK: An inner interface for functional programming.
	@FunctionalInterface
	interface ResizeOperation {
		BufferedImage resize();
	}

	// MARK: Fields
	private int numOfSeams;
	private ResizeOperation resizeOp;
	boolean[][] imageMask;
	// added fields
	long [][] energyMatrix;
	BufferedImage greyScaleImage;
	long [][] costMatrix;
	int currentWidth;
	int [][] bestSeamsIndexesMatrix;
	int removedSeamCounter;
	boolean[][] originalMask;

	public SeamsCarver(Logger logger, BufferedImage workingImage, int outWidth, RGBWeights rgbWeights,
			boolean[][] imageMask) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, workingImage.getHeight());

		numOfSeams = Math.abs(outWidth - inWidth);
		this.imageMask = imageMask;
		if (inWidth < 2 | inHeight < 2)
			throw new RuntimeException("Can not apply seam carving: workingImage is too small");

		if (numOfSeams > inWidth / 2)
			throw new RuntimeException("Can not apply seam carving: too many seams...");

		// Setting resizeOp by with the appropriate method reference
		if (outWidth > inWidth)
			resizeOp = this::increaseImageWidth;
		else if (outWidth < inWidth)
			resizeOp = this::reduceImageWidth;
		else
			resizeOp = this::duplicateWorkingImage;

		// calculations.
		this.currentWidth = inWidth;
		this.greyScaleImage = this.greyscale();
		this.removedSeamCounter = 0;
		this.bestSeamsIndexesMatrix = new int[this.numOfSeams][this.inHeight];
		this.originalMask = imageMask;
		this.calculateEnergyMatrix();
		this.calculateCostMatrix();


		this.logger.log("preliminary calculations were ended.");
	}

	public BufferedImage resize() {
		return resizeOp.resize();
	}

	private BufferedImage reduceImageWidth() {
		BufferedImage reducedImage = this.workingImage;
		for (int i = 0; i < this.numOfSeams; i++){
			reducedImage = removeBestSeam(reducedImage);
			this.calculateEnergyMatrix();
			this.calculateCostMatrix();
		}
		return reducedImage;
	}

	private BufferedImage increaseImageWidth() {
		reduceImageWidth(); // finding the best k seams and store them in matrix
		this.currentWidth = this.inWidth;
		BufferedImage increasedImage = this.workingImage;
		this.imageMask = createCopyOfOriginalMask();
		for (int i = 0; i < this.numOfSeams; i++ ){
			increasedImage = duplicateBestSeam(increasedImage, i);
		}

		return increasedImage;
	}

	public BufferedImage showSeams(int seamColorRGB) {
		BufferedImage showSeamsImage = this.duplicateWorkingImage();
		increaseImageWidth();

		for (int i = 0; i < this.bestSeamsIndexesMatrix.length; i++) {
			int[] currentBestSeam =  this.bestSeamsIndexesMatrix[i];
			for(int j = 0; j < this.inHeight; j++){
				int x = currentBestSeam[j];
				showSeamsImage.setRGB(x, j, seamColorRGB);
			};
		}

		this.logger.log("showSeams Image done.");
		return showSeamsImage;
	}

	public boolean[][] getMaskAfterSeamCarving() {
		return this.imageMask;
	}

	/*
	Calculates the Energy Matrix by using:
	this.currentWidth
	this.greyScaleImage
	this.imageMask
	 */
	public void calculateEnergyMatrix(){
		this.pushForEachParameters();
		this.setForEachParameters(this.currentWidth, this.inHeight);
		this.energyMatrix = new long[this.inHeight][this.currentWidth];

		forEach((y, x) -> {
					int p1, p2;
					long E1, E2, E3;
					p1 = new Color(this.greyScaleImage.getRGB(x, y)).getBlue();
					if (x < (this.currentWidth - 1)) { // forward differencing
						p2 = new Color(this.greyScaleImage.getRGB(x + 1, y)).getBlue();
					} else { // backwards differencing
						p2 = new Color(this.greyScaleImage.getRGB(x - 1, y)).getBlue();
					}
					E1 = Math.abs(p1 - p2);

					if (y < (this.inHeight - 1)) { // forward differencing
						p2 = new Color(this.greyScaleImage.getRGB(x, y + 1)).getBlue();
					} else { // backwards differencing
						p2 = new Color(this.greyScaleImage.getRGB(x, y - 1)).getBlue();
					}
					E2 = Math.abs(p1 - p2);

					if (this.imageMask[y][x]) {
						E3 = Integer.MIN_VALUE;
					} else {
						E3 = 0;
					}

					this.energyMatrix[y][x] = E1 + E2 + E3;
				}
		);

		this.popForEachParameters();
	}

	public void calculateCostMatrix(){
		this.pushForEachParameters();
		this.setForEachParameters(this.currentWidth, this.inHeight);
		this.costMatrix = new long[this.inHeight][this.currentWidth];

		forEach((y, x) -> {
			long minVal = 0;
			if (y > 0 ) { // not the first row
				minVal = findMinNeighbor(y, x);
			}
			this.costMatrix[y][x] = this.energyMatrix[y][x] + minVal; // first row is identical to the energy matrix first row
		});

		this.popForEachParameters();
	}

	public boolean[][] createCopyOfOriginalMask(){
		boolean[][] maskCopy = new boolean[this.originalMask.length][this.originalMask[0].length];
		for(int i = 0; i < this.originalMask.length; i++){
			for(int j = 0; j < this.originalMask[0].length; j++){
				maskCopy[i][j] = this.originalMask[i][j];
			}
		}
		return maskCopy;
	}

	public int calc_c_up(int x,int y){
		int p1 = new Color(this.greyScaleImage.getRGB(x + 1, y)).getBlue();
		int p2 = new Color(this.greyScaleImage.getRGB(x - 1, y)).getBlue();

		return Math.abs(p1 - p2);
	}

	public int calc_c_left(int x,int y){
		int p1 = new Color(this.greyScaleImage.getRGB(x, y - 1)).getBlue();
		int p2 = new Color(this.greyScaleImage.getRGB(x - 1, y)).getBlue();

		return Math.abs(p1 - p2);
	}

	public int calc_c_right(int x,int y){
		int p1 = new Color(this.greyScaleImage.getRGB(x, y - 1)).getBlue();
		int p2 = new Color(this.greyScaleImage.getRGB(x + 1, y)).getBlue();

		return Math.abs(p1 - p2);
	}

	public long findMinNeighbor(int y, int x){
		long neighborUp = this.costMatrix[y - 1][x];
		long C_up = 0;
		long neighborLeft = Long.MAX_VALUE;
		long neighborRight = Long.MAX_VALUE;

		if (x > 0 && ((x + 1) < this.currentWidth)){
			C_up = calc_c_up(x,y);
			neighborUp += C_up;
		}
		if (x > 0){
			int C_left = calc_c_left(x,y);
			neighborLeft = this.costMatrix[y - 1][x - 1] + (C_up + C_left);
		}
		if (x + 1 < this.currentWidth) {
			int C_right = calc_c_right(x,y);
			neighborRight = this.costMatrix[y - 1][x + 1] + (C_up + C_right);
		}

		long minBetweenUpAndRight = Math.min(neighborUp, neighborRight);
		return Math.min(neighborLeft , minBetweenUpAndRight);
	}

	public int findMinCostInCostMatrix(){
		int minXIndex = 0;
		long minVal = this.costMatrix[this.inHeight - 1][0];
		for(int i = 1; i < this.currentWidth; i++){
			if(this.costMatrix[this.inHeight - 1][i] < minVal){
				minXIndex = i;
				minVal = this.costMatrix[this.inHeight - 1][i];
			}
		}
		return minXIndex;
	}

	public int[] backtrackTheBestSeam(int startXIndex){
		int [] bestSeamIndexes = new int[this.inHeight]; // store all indexes of the seam
		bestSeamIndexes[this.inHeight - 1] = startXIndex; // first indexes store in the last position in the array
		int x = startXIndex;

		for (int y = inHeight - 1; y > 0; y--){
			long c_up = 0;
			long c_left;
			long neighbourLeft = -1;

			if (x > 0 && ((x + 1) < this.currentWidth)){
				c_up = calc_c_up(x,y);
			}
			if (x > 0) {
				c_left = calc_c_left(x, y);
				neighbourLeft = this.costMatrix[y-1][x-1] + c_up + c_left;
			}

			// inverted formula
			if(this.costMatrix[y][x] == (this.energyMatrix[y][x] + this.costMatrix[y-1][x] + c_up)) {
				//do nothing - (x = x)
			} else if (this.costMatrix[y][x] == (this.energyMatrix[y][x] + neighbourLeft)){
				x = x - 1;
			}else {
				x = x + 1;
			}

			bestSeamIndexes[y - 1] = x;
		}

		return bestSeamIndexes;
	}

	public BufferedImage duplicateBestSeam(BufferedImage currentColorImage, int bestSeamIndexLocation) {
		int newWidth = currentColorImage.getWidth() + 1;
		BufferedImage newImage = new BufferedImage(newWidth, this.inHeight, this.workingImageType);
		boolean[][] newImageMask = new boolean[this.inHeight][newWidth];
		int[] currentSeam =  bestSeamsIndexesMatrix[bestSeamIndexLocation];

		for (int y = 0; y < this.inHeight; y++){
			int currentXIndex = currentSeam[y]; // the x index for the current row
			for (int x = 0; x < currentColorImage.getWidth(); x++){
				int currentRGB = currentColorImage.getRGB(x, y);;

				if (x < currentXIndex){ // pixels stays the same
					newImage.setRGB(x, y, currentRGB);
					newImageMask[y][x] = this.imageMask[y][x];
				} else if (x == currentXIndex){ // pixel duplication
					newImage.setRGB(x, y, currentRGB);
					newImageMask[y][x] = this.imageMask[y][x];

					newImage.setRGB(x + 1, y, currentRGB); // shift right the same value
					newImageMask[y][x + 1] = this.imageMask[y][x];
				} else if (x > currentXIndex){ // shift one pixel right
					newImage.setRGB((x + 1), y, currentRGB);
					newImageMask[y][x + 1] = this.imageMask[y][x];
				}
			}
		}

		// update fields with new values after adding seam
		this.currentWidth = this.currentWidth + 1;
		this.imageMask = newImageMask;
		this.updateBestSeamsIndexesMatrix(bestSeamIndexLocation);

		return newImage;
	}

	public void updateBestSeamsIndexesMatrix(int bestSeamIndexLocation){
		// increase the index for each seam so they will match the original image
		int[] currentSeam = this.bestSeamsIndexesMatrix[bestSeamIndexLocation];
		for(int i = bestSeamIndexLocation + 1; i < this.bestSeamsIndexesMatrix.length; i++){
			for(int j = 0; j < this.bestSeamsIndexesMatrix[0].length; j++){
				if(currentSeam[j] <= this.bestSeamsIndexesMatrix[i][j]) {
					this.bestSeamsIndexesMatrix[i][j]++;
				}
			}
		}
	}

	/*
	Updates the following fields:
	this.currentWidth
	this.greyScaleImage
	this.imageMask
	 */
	public BufferedImage removeBestSeam(BufferedImage currentColorImage){
		int minXIndex = this.findMinCostInCostMatrix();
		int [] currentBestSeam = backtrackTheBestSeam(minXIndex);
		this.bestSeamsIndexesMatrix[this.removedSeamCounter] = currentBestSeam;
		// remove it and save it's values
		int newWidth = this.currentWidth - 1;
		BufferedImage newImage = new BufferedImage(newWidth, this.inHeight, this.workingImageType);
		BufferedImage newGreyImage = new BufferedImage(newWidth, this.inHeight,this.workingImageType);
		boolean[][] newImageMask = new boolean[this.inHeight][newWidth];

		for (int y = 0; y < this.inHeight; y++){
			int currentXIndex = currentBestSeam[y];
			for (int x = 0; x < this.currentWidth; x++){
				if (x < currentXIndex){ // pixels stays the same
					newImage.setRGB(x, y, currentColorImage.getRGB(x, y));
					newGreyImage.setRGB(x, y, this.greyScaleImage.getRGB(x, y));
					newImageMask[y][x] = this.imageMask[y][x];
				} else if (x > currentXIndex){ // shift one pixel left
					newImage.setRGB((x - 1), y, currentColorImage.getRGB(x, y));
					newGreyImage.setRGB((x - 1), y, this.greyScaleImage.getRGB(x, y));
					newImageMask[y][x - 1] = this.imageMask[y][x];
				}
			}
		}
		// update fields with new values after removing seam
		this.currentWidth = this.currentWidth - 1;
		this.greyScaleImage = newGreyImage;
		this.imageMask = newImageMask;
		this.removedSeamCounter++;

		return newImage;
	}
}