package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

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
	int removedSeamCounter;
	boolean[][] originalMask;
	int [][] trueIndicesMatrix; // real indices matrix
	ArrayList<int[]> trueBestSeamsIndices; //remembering the real indices of the seams

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
		this.originalMask = imageMask;
		this.calculateEnergyMatrix();
		this.calculateCostMatrix();
		this.calculateTrueIndicesMatrix();
		this.trueBestSeamsIndices = new ArrayList<>();

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
		BufferedImage increasedImage = new BufferedImage(this.outWidth, this.outHeight, this.workingImageType);
		boolean [][] newMask = new boolean [this.outHeight][this.outWidth];
		int numOfShiftsRight = 0; // accumulate the num of pixels added in each row to indicate the shifts needed

		for(int row = 0; row < this.inHeight; row++){
			numOfShiftsRight = 0;
			for(int column = 0; column <this.inWidth; column++){
				int currentXIndex = column + numOfShiftsRight;
				int currentColor = this.workingImage.getRGB(column, row);
				increasedImage.setRGB(currentXIndex, row, currentColor);
				newMask[row][column] = this.originalMask[row][column];
				for(int seam = 0; seam < this.trueBestSeamsIndices.size(); seam++){
					if(this.trueBestSeamsIndices.get(seam)[row] == column){
						currentColor = this.workingImage.getRGB(column, row);
						increasedImage.setRGB(column + (++numOfShiftsRight), row, currentColor);
						newMask[row][column + numOfShiftsRight] = this.originalMask[row][column];
					}
				}
			}
		}

		this.imageMask = newMask;
		return increasedImage;
	}

	public BufferedImage showSeams(int seamColorRGB) {
		BufferedImage showSeamsImage = this.duplicateWorkingImage();
		reduceImageWidth();

		for (int i = 0; i < this.trueBestSeamsIndices.size(); i++) {
			int[] currentBestSeam =  this.trueBestSeamsIndices.get(i);
			for(int j = 0; j < this.inHeight; j++){
				int x = currentBestSeam[j];
				showSeamsImage.setRGB(x, j, seamColorRGB);
			};
		}

		this.logger.log("showSeams Image done.");
		return showSeamsImage;
	}

	public boolean[][] getMaskAfterSeamCarving() {
		return this.imageMask; // holds the updated mask of the current image
	}

	/*
	Calculates the Energy Matrix by using:
	this.currentWidth
	this.greyScaleImage
	this.imageMask
	 */
	private void calculateEnergyMatrix(){
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

	private void calculateCostMatrix(){
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

	private int calc_c_up(int x,int y){
		int p1 = new Color(this.greyScaleImage.getRGB(x + 1, y)).getBlue();
		int p2 = new Color(this.greyScaleImage.getRGB(x - 1, y)).getBlue();

		return Math.abs(p1 - p2);
	}

	private int calc_c_left(int x,int y){
		int p1 = new Color(this.greyScaleImage.getRGB(x, y - 1)).getBlue();
		int p2 = new Color(this.greyScaleImage.getRGB(x - 1, y)).getBlue();

		return Math.abs(p1 - p2);
	}

	private int calc_c_right(int x,int y){
		int p1 = new Color(this.greyScaleImage.getRGB(x, y - 1)).getBlue();
		int p2 = new Color(this.greyScaleImage.getRGB(x + 1, y)).getBlue();

		return Math.abs(p1 - p2);
	}

	private long findMinNeighbor(int y, int x){
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

	private int findMinCostInCostMatrix(){
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

	private int[] backtrackTheBestSeam(int startXIndex){
		int [] bestSeamIndexes = new int[this.inHeight]; // store all indexes of the seam
		int [] trueSeamIndexes = new int[this.inHeight];
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
			trueSeamIndexes[y - 1] = trueIndicesMatrix[y][x]; //retrieve the true value of the X index
		}
		this.trueBestSeamsIndices.add(trueSeamIndexes);
		return bestSeamIndexes;
	}

	/*
	Updates the following fields:
	this.currentWidth
	this.greyScaleImage
	this.imageMask
	 */
	private BufferedImage removeBestSeam(BufferedImage currentColorImage){
		int minXIndex = this.findMinCostInCostMatrix();
		int [] currentBestSeam = backtrackTheBestSeam(minXIndex);
		// remove it and save it's values
		int newWidth = this.currentWidth - 1;
		BufferedImage newImage = new BufferedImage(newWidth, this.inHeight, this.workingImageType);
		BufferedImage newGreyImage = new BufferedImage(newWidth, this.inHeight,this.workingImageType);
		boolean[][] newImageMask = new boolean[this.inHeight][newWidth];
		int[][] newTrueMatrix = new int[this.inHeight][newWidth]; //stores the true x values

		for (int y = 0; y < this.inHeight; y++){
			int currentXIndex = currentBestSeam[y];
			for (int x = 0; x < this.currentWidth; x++){
				if (x < currentXIndex){ // pixels stays the same
					newImage.setRGB(x, y, currentColorImage.getRGB(x, y));
					newGreyImage.setRGB(x, y, this.greyScaleImage.getRGB(x, y));
					newImageMask[y][x] = this.imageMask[y][x];
					newTrueMatrix[y][x] = this.trueIndicesMatrix[y][x];

				} else if (x > currentXIndex){ // shift one pixel left
					newImage.setRGB((x - 1), y, currentColorImage.getRGB(x, y));
					newGreyImage.setRGB((x - 1), y, this.greyScaleImage.getRGB(x, y));
					newImageMask[y][x - 1] = this.imageMask[y][x];
					newTrueMatrix[y][x - 1] = this.trueIndicesMatrix[y][x];
				}
			}
		}
		// update fields with new values after removing seam
		this.currentWidth = this.currentWidth - 1;
		this.greyScaleImage = newGreyImage;
		this.imageMask = newImageMask;
		this.removedSeamCounter++;
		this.trueIndicesMatrix = newTrueMatrix;

		return newImage;
	}

	private void calculateTrueIndicesMatrix(){
		this.trueIndicesMatrix = new int[this.inHeight][this.inWidth];
		for(int i = 0 ; i < this.inHeight ; i++){
			for(int j = 0; j < this.inWidth; j++){
				this.trueIndicesMatrix[i][j] = j;
			}
		}
	}
}