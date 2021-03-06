package edu.cg.menu;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import edu.cg.RGBWeights;
import edu.cg.ImageProcessor;
import edu.cg.Logger;
import edu.cg.SeamsCarver;
import edu.cg.UnimplementedMethodException;
import edu.cg.menu.components.ActionsController;
import edu.cg.menu.components.ColorMixer;
import edu.cg.menu.components.ImagePicker;
import edu.cg.menu.components.LogField;
import edu.cg.menu.components.ScaleSelector;
import edu.cg.menu.components.ScaleSelector.ResizingOperation;

@SuppressWarnings("serial")
public class MenuWindow extends JFrame implements Logger {
	// MARK: fields
	private BufferedImage workingImage;
	private boolean[][] imageMask;
	private String imageTitle;

	// MARK: GUI fields
	private ImagePicker imagePicker;
	private ColorMixer colorMixer;
	private ScaleSelector scaleSelector;
	private ActionsController actionsController;
	private LogField logField;

	public MenuWindow() {
		super();

		setTitle("Ex1: Image Processing Application");
		// The following line makes sure that all application threads are terminated
		// when this window is closed.
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		imagePicker = new ImagePicker(this);
		colorMixer = new ColorMixer();
		scaleSelector = new ScaleSelector();
		actionsController = new ActionsController(this);
		logField = new LogField();

		contentPane.add(imagePicker, BorderLayout.NORTH);

		JPanel panel1 = new JPanel();
		contentPane.add(panel1, BorderLayout.CENTER);
		panel1.setLayout(new GridLayout(0, 1, 0, 0));

		JPanel panel2 = new JPanel();
		panel1.add(panel2, BorderLayout.CENTER);
		panel2.setLayout(new GridLayout(0, 1, 0, 0));

		JPanel panel3 = new JPanel();
		panel2.add(panel3, BorderLayout.CENTER);
		panel3.setLayout(new GridLayout(0, 1, 0, 0));

		panel3.add(colorMixer);
		panel3.add(scaleSelector);
		panel2.add(actionsController);
		panel1.add(logField);

		workingImage = null;
		imageMask = null;
		imageTitle = null;

		pack();
	}

	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
		log("Application started.");
	}

	public void changeHue() {
		int outWidth = scaleSelector.width();
		int outHeight = scaleSelector.height();
		RGBWeights rgbWeights = colorMixer.getRGBWeights();
		BufferedImage img = new ImageProcessor(this, duplicateImage(), rgbWeights, outWidth, outHeight).changeHue();
		present(img, "Change hue");
	}

	public void greyscale() {
		BufferedImage img = new ImageProcessor(this, duplicateImage(), colorMixer.getRGBWeights()).greyscale();
		present(img, "Grey scale");
	}

	public void resize() {
		int outWidth = scaleSelector.width();
		int outHeight = scaleSelector.height();
		ResizingOperation op = scaleSelector.resizingOperation();
		RGBWeights rgbWeights = colorMixer.getRGBWeights();
		BufferedImage img;
		switch (op) {
		case NEAREST_NEIGHBOR:
			img = new ImageProcessor(this, duplicateImage(), rgbWeights, outWidth, outHeight).nearestNeighbor();
			break;

		default: // seam carving
			SeamsCarver sc = new SeamsCarver(this, duplicateImage(), outWidth, rgbWeights, duplicateMask());
			img = sc.resize();
			boolean[][] new_mask = sc.getMaskAfterSeamCarving();
			img = new SeamsCarver(this, rotateClockwise(img), outHeight, rgbWeights, rotateMaskClockwise(new_mask))
					.resize();
			img = rotateCounterclockwise(img);
			break;
		}

		present(img, "Resize: " + op.title + " [" + outWidth + "][" + outHeight + "]");
	}

	public void showSeamsVertical() {
		int outWidth = scaleSelector.width();
		RGBWeights rgbWeights = colorMixer.getRGBWeights();
		BufferedImage vertical = new SeamsCarver(this, duplicateImage(), outWidth, rgbWeights, duplicateMask())
				.showSeams(Color.RED.getRGB());
		present(vertical, "Show seams vertical");
	}

	public void showSeamsHorizontal() {
		int outHeight = scaleSelector.height();
		RGBWeights rgbWeights = colorMixer.getRGBWeights();

		BufferedImage horizontal = new SeamsCarver(this, rotateClockwise(workingImage), outHeight, rgbWeights,
				rotateMaskClockwise(imageMask)).showSeams(Color.BLACK.getRGB());

		horizontal = rotateCounterclockwise(horizontal);

		present(horizontal, "Show seams horizontal");
	}

	private void present(BufferedImage img, String title) {
		if (img == null)
			throw new NullPointerException("Can not present a null image.");

		new ImageWindow(img, imageTitle + "; " + title, this).setVisible(true);
	}

	private static BufferedImage rotateClockwise(BufferedImage img) {
		int imgWidth = img.getWidth();
		int imgHeight = img.getHeight();
		BufferedImage ans = new BufferedImage(imgHeight, imgWidth, img.getType());
		for (int y = 0; y < imgWidth; ++y)
			for (int x = 0; x < imgHeight; ++x) {
				int imgX = y;
				int imgY = imgHeight - 1 - x;
				ans.setRGB(x, y, img.getRGB(imgX, imgY));
			}

		return ans;
	}

	private static boolean[][] rotateMaskClockwise(boolean[][] mask) {
		int height = mask.length;
		int width = mask[0].length;
		boolean[][] ans = new boolean[width][height];
		for (int y = 0; y < height; ++y)
			for (int x = 0; x < width; ++x) {
				int X = y;
				int Y = width - 1 - x;
				ans[Y][X] = mask[y][x];
			}

		return ans;
	}

	private static BufferedImage rotateCounterclockwise(BufferedImage img) {
		int imgWidth = img.getWidth();
		int imgHeight = img.getHeight();
		BufferedImage ans = new BufferedImage(imgHeight, imgWidth, img.getType());
		for (int y = 0; y < imgWidth; ++y)
			for (int x = 0; x < imgHeight; ++x) {
				int imgX = imgWidth - 1 - y;
				int imgY = x;
				ans.setRGB(x, y, img.getRGB(imgX, imgY));
			}

		return ans;
	}

	private static BufferedImage duplicateImage(BufferedImage img) {
		BufferedImage dup = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
		for (int y = 0; y < dup.getHeight(); ++y)
			for (int x = 0; x < dup.getWidth(); ++x)
				dup.setRGB(x, y, img.getRGB(x, y));

		return dup;
	}

	private static boolean[][] duplicateMask(boolean[][] mask) {
		boolean[][] cpyMask = new boolean[mask.length][];
		for (int i = 0; i < mask.length; i++) {
			cpyMask[i] = Arrays.copyOf(mask[i], mask[i].length);
		}
		return cpyMask;
	}

	private BufferedImage duplicateImage() {
		return duplicateImage(workingImage);
	}

	private boolean[][] duplicateMask() {
		return duplicateMask(imageMask);
	}

	public void setWorkingImage(BufferedImage workingImage, String imageTitle) {
		this.imageTitle = imageTitle;
		this.workingImage = workingImage;
		log("Image: " + imageTitle + " has been selected as working image.");
		scaleSelector.setWidth(workingImage.getWidth());
		scaleSelector.setHeight(workingImage.getHeight());
		actionsController.activateButtons();
		imageMask = new boolean[workingImage.getHeight()][workingImage.getWidth()];
	}

	public void present() {
		new ImageWindow(workingImage, imageTitle, this).setVisible(true);
	}

	// MARK: Logger
	@Override
	public void log(String s) {
		logField.log(s);
	}

	public void setImageMask(boolean[][] srcMask) {
		imageMask = duplicateMask(srcMask);
	}

	public void removeObjectFromImage(boolean[][] srcMask) {

		int currentWidth = srcMask[0].length;
		int maxTrueVal = maxTrueInRows(srcMask);

		boolean[][] newMask = duplicateMask(srcMask);
		BufferedImage reducedImage = null;
		BufferedImage increasedImage = duplicateImage(); //in case no mask marked, return the original

		while(maxTrueVal > 0){
			//Δ = min (𝑾/𝟑,− 1, #𝑀𝑎𝑥𝑖𝑚𝑢𝑚 𝑛𝑢𝑚𝑏𝑒𝑟 𝑜𝑓 𝑡𝑟𝑢𝑒 𝑣𝑎𝑙𝑢𝑒𝑠 𝑖𝑛 𝑎 𝑟𝑜𝑤)
			int delta = Math.min((int)(currentWidth / 3) - 1, maxTrueVal);
			RGBWeights rgbWeights = colorMixer.getRGBWeights();

			//Remove Δ vertical seams using seam carving.
			SeamsCarver scReduce = new SeamsCarver(this, duplicateImage(), (currentWidth-delta), rgbWeights, newMask);
			reducedImage = scReduce.resize();
			newMask = duplicateMask(scReduce.getMaskAfterSeamCarving());

			//Increase the image size to the original image’s size.
			SeamsCarver scIncrease = new SeamsCarver(this, reducedImage, currentWidth, rgbWeights, newMask);
			increasedImage = scIncrease.resize();
			newMask = duplicateMask(scIncrease.getMaskAfterSeamCarving());
			maxTrueVal = maxTrueInRows(newMask);
		}

		present(increasedImage, "Image After Object Removal");
	}

	private int maxTrueInRows (boolean [][] currentMask){

		int maxVal = 0;
		for(int i = 0; i < currentMask.length ; i++){
			int currentVal = 0;
			for(int j = 0; j < currentMask[0].length; j++){

				if(currentMask[i][j]){
					currentVal++;
				}
			}
			if(currentVal > maxVal){
				maxVal = currentVal;
			}
		}
		return maxVal;
	}

	public void maskImage() {
		new MaskPainterWindow(duplicateImage(), "Mask Painter", this).setVisible(true);
	}
}
