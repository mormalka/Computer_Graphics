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

		this.currentWidth = inWidth;
		greyScaleImage = this.greyscale();
		this.calculateEnergyMatrix();
		this.calculateCostMatrix();


		this.logger.log("preliminary calculations were ended.");
	}

	public BufferedImage resize() {
		return resizeOp.resize();
	}

	private BufferedImage reduceImageWidth() {
		// TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("reduceImageWidth");
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
		// TODO: Implement this method, remove the exception.
		// This method should return the mask of the resize image after seam carving.
		// Meaning, after applying Seam Carving on the input image,
		// getMaskAfterSeamCarving() will return a mask, with the same dimensions as the
		// resized image, where the mask values match the original mask values for the
		// corresponding pixels.
		// HINT: Once you remove (replicate) the chosen seams from the input image, you
		// need to also remove (replicate) the matching entries from the mask as well.
		throw new UnimplementedMethodException("getMaskAfterSeamCarving");
	}

	public void calculateEnergyMatrix(){
		this.setForEachParameters(this.inWidth, this.inHeight);
		this.energyMatrix = new long[this.inHeight][this.inWidth];
		this.logger.log("started calculateEnergyMatrix()");
		forEach((y, x) -> {
					int p1, p2;
					long E1, E2, E3;
					p1 = new Color(this.greyScaleImage.getRGB(x, y)).getBlue();
					if (x < (this.inWidth - 1)) { // forward differencing
						p2 = new Color(this.greyScaleImage.getRGB(x + 1, y)).getBlue();
					} else { // backwards differencing
						p2 = new Color(this.greyScaleImage.getRGB(x - 1, y)).getBlue();
					}
					E1 = Math.abs(p1 - p2);

					if (x < (this.inHeight - 1)) { // forward differencing
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
	}


	public void calculateCostMatrix(){
		this.setForEachParameters(this.inWidth, this.inHeight);
		this.costMatrix = new long[this.inHeight][this.currentWidth];
		this.logger.log("started calculate cost matrix: " + currentWidth);
		forEach((y, x) -> {
			long minVal = 0;
			if (y > 0 ) { // not the first row
				minVal = findMinNeighbor(y, x);
			}
			this.costMatrix[y][x] = this.energyMatrix[y][x] + minVal;
		});
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
//		long C_up = calc_c_up(x,y);

		// TODO - the order of x and y
//		long neighborUp = this.costMatrix[y - 1][x] + C_up;
		long neighborUp = Long.MAX_VALUE; // MAX?
		long C_up = 255; // TODO ??
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

		return Math.min(neighborUp , Math.min(neighborLeft, neighborRight));
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

	public int[][] backtrackTheBestSeam(int startXIndex){
		int [][] bestSeamIndexes = new int[this.inHeight][2]; // store all indexes of the seam
		int [] currentIndexes = {this.inHeight - 1, startXIndex};
		bestSeamIndexes[this.inHeight - 1] = currentIndexes;
		int x = startXIndex;


		for (int y = inHeight - 1; y > 0; y--){
//			long c_up = calc_c_up(x,y);
			long c_up = 255; // NEW!!!!!!!! NOT SURE ABOUT THE VALUE
			long c_left;

			if (x > 0 && ((x + 1) < this.currentWidth)){ // NEW
				c_up = calc_c_up(x,y);
			}

			if (x > 0) {
				c_left = calc_c_left(x, y);
			} else { c_left  = -1;}

			if(this.costMatrix[y][x] == (this.energyMatrix[y][x] + this.costMatrix[y-1][x] + c_up)) {  // not finished
				//do nothing - (x = x)
			} else if (this.costMatrix[y][x] == (this.energyMatrix[y][x] + this.costMatrix[y-1][x-1] + c_left)){
				x = x-1;
			}else {
				x = x + 1;
			}

			currentIndexes[0] = y + 1;
			currentIndexes[1] = x;
			bestSeamIndexes[y] = currentIndexes;

		}
		return bestSeamIndexes;
	}

	// TODO : i adi added
	public void removeBestSeam(){
		int minXIndex = this.findMinCostInCostMatrix();
		int [][] currentBestSeam = backtrackTheBestSeam(minXIndex);

		// remove it and save it's values

	}
}
