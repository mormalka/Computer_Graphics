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
	// TODO: Add some additional fields
	long [][] energyMatrix;
	BufferedImage greyScaleImage;
	long [][] costMatrix;
	int currentWidth;
	int [][] seamsIndexes;

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

		// TODO: You may initialize your additional fields and apply some preliminary
		// calculations.

		this.logger.log("starts calculations");
		this.currentWidth = inWidth;
		this.greyScaleImage = this.greyscale();
		this.seamsIndexes = new int[this.numOfSeams][this.inHeight]; // NEW
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
		// TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("increaseImageWidth");
	}

	public BufferedImage showSeams(int seamColorRGB) {
		// TODO: Implement this method (bonus), remove the exception.
		throw new UnimplementedMethodException("showSeams");
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
		System.out.println("calculateEnergyMatrix");
		this.pushForEachParameters();
		this.setForEachParameters(this.currentWidth, this.inHeight);
		this.energyMatrix = new long[this.inHeight][this.currentWidth];
		this.logger.log("started calculateEnergyMatrix()");
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
		System.out.println("calculateCostMatrix");
		this.pushForEachParameters();
		this.setForEachParameters(this.currentWidth, this.inHeight);
		this.costMatrix = new long[this.inHeight][this.currentWidth];
		this.logger.log("started calculate cost matrix: " + currentWidth);
		forEach((y, x) -> {
			long minVal = 0;
			if (y > 0 ) { // not the first row
				minVal = findMinNeighbor(y, x);
			}
			this.costMatrix[y][x] = this.energyMatrix[y][x] + minVal; // first row is identical to the energy matrix first row
		});

		this.popForEachParameters();
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
		// TODO - the order of x and y

		long neighborUp = this.costMatrix[y - 1][x]; // TODO ??
		long C_up = 0; // TODO ??
		long neighborLeft = Long.MAX_VALUE;
		long neighborRight = Long.MAX_VALUE;

		// NEW
		if (x > 0 && ((x + 1) < this.currentWidth)){
			C_up = calc_c_up(x,y);
			neighborUp = this.costMatrix[y - 1][x] + C_up;
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

		System.out.println("width: " + currentWidth);
		System.out.println("start index: " + startXIndex);

		for (int y = inHeight - 1; y > 0; y--){
			long c_up = 0; // NEW!!!!!!!! NOT SURE ABOUT THE VALUE
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
				System.out.println("cond 1");
				//do nothing - (x = x)
			} else if (this.costMatrix[y][x] == (this.energyMatrix[y][x] + neighbourLeft)){
				System.out.println("cond 2");
				x = x - 1;
			}else {
				System.out.println("cond 3");
				x = x + 1;
			}

			bestSeamIndexes[y - 1] = x;

		}
		return bestSeamIndexes;
	}

	/*
	updates these fields:
	this.currentWidth
	this.greyScaleImage
	this.imageMask
	 */

	public BufferedImage removeBestSeam(BufferedImage currentColorImage){
		int minXIndex = this.findMinCostInCostMatrix();
		int [] currentBestSeam = backtrackTheBestSeam(minXIndex);

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

		return newImage;
	}
}
