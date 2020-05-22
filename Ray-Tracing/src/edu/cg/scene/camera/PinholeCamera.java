package edu.cg.scene.camera;

import edu.cg.algebra.Point;
import edu.cg.algebra.Vec;

public class PinholeCamera {
	/**
	 * Initializes a pinhole camera model with default resolution 200X200 (RxXRy)
	 * and View Angle 90.
	 * 
	 * @param cameraPosition  - The position of the camera.
	 * @param towardsVec      - The towards vector of the camera (not necessarily
	 *                        normalized).
	 * @param upVec           - The up vector of the camera.
	 * @param distanceToPlain - The distance of the camera (position) to the center
	 *                        point of the image-plain.
	 * 
	 */

	Point cameraPosition;
	Vec towardsVec;
	Vec upVec;
	double distanceToPlain;
	double viewAngle = 90;
	int[] resolution = {200,200};
	Point p0 = new Point(0,0,0);

	public PinholeCamera(Point cameraPosition, Vec towardsVec, Vec upVec, double distanceToPlain) {

		this.cameraPosition = cameraPosition;
		this.towardsVec = towardsVec.normalize();
		this.upVec = upVec.normalize();
		this.distanceToPlain = distanceToPlain;
	}

	/**
	 * Initializes the resolution and width of the image.
	 * 
	 * @param height    - the number of pixels in the y direction.
	 * @param width     - the number of pixels in the x direction.
	 * @param viewAngle - the view Angle.
	 */
	public void initResolution(int height, int width, double viewAngle) {

		this.resolution[0] = height;
		this.resolution[1] = width;
		this.viewAngle = viewAngle;
	}

	/**
	 * Transforms from pixel coordinates to the center point of the corresponding
	 * pixel in model coordinates.
	 * 
	 * @param x - the pixel index in the x direction.
	 * @param y - the pixel index in the y direction.
	 * @return the middle point of the pixel (x,y) in the model coordinates.
	 */
	public Point transform(int x, int y) {
		int R_x = this.resolution[1];
		int R_y = this.resolution[0];
		double w = 2 * this.distanceToPlain *Math.tan(Math.toRadians(this.viewAngle/2)); //plain width
		double R = w / R_x; // Ratio (pixel width)
		Vec d_towardsVec = this.towardsVec.mult(this.distanceToPlain);
		Point p_center = new Point(p0.x +d_towardsVec.x ,p0.y +d_towardsVec.y ,p0.z +d_towardsVec.z);
		Vec rightVec = (this.towardsVec.cross(this.upVec)).normalize();
		Vec tilda_upVec = (rightVec.cross(this.towardsVec)).normalize();

		Point P = new Point();
		P = P.add(p_center);
		Vec eq_right = tilda_upVec.mult(R*(y -Math.floor(R_y/2))).mult(-1);
		Vec eq_left = rightVec.mult(R*(x -Math.floor(R_x/2)));
		P = P.add(eq_right.add(eq_left));

		return P;
		//throw new UnimplementedMethodException("PinholeCamera.transform is not implemented.");
	}

	/**
	 * Returns the camera position
	 * 
	 * @return a new point representing the camera position.
	 */
	public Point getCameraPosition() {

		return this.cameraPosition;
		//throw new UnimplementedMethodException("PinholeCamera.getCameraPosition");
	}
}
