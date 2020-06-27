package edu.cg.models.Car;

import java.util.LinkedList;
import java.util.List;

import com.jogamp.opengl.*;

import edu.cg.algebra.Point;
import edu.cg.algebra.Vec;
import edu.cg.models.BoundingSphere;
import edu.cg.models.IIntersectable;
import edu.cg.models.IRenderable;

/**
 * A F1 Racing Car.
 *
 */
public class F1Car implements IRenderable, IIntersectable {
	// Remember to include a ReadMe file specifying what you implemented.
	Center carCenter = new Center();
	Back carBack = new Back();
	Front carFront = new Front();

	@Override
	public void render(GL2 gl) {
		carCenter.render(gl);
		gl.glPushMatrix();
		gl.glTranslated(-Specification.B_LENGTH / 2.0 - Specification.C_BASE_LENGTH / 2.0, 0.0, 0.0);
		carBack.render(gl);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glTranslated(Specification.F_LENGTH / 2.0 + Specification.C_BASE_LENGTH / 2.0, 0.0, 0.0);
		carFront.render(gl);
		gl.glPopMatrix();

	}

	@Override
	public String toString() {
		return "F1Car";
	}

	@Override
	public void init(GL2 gl) {

	}

	@Override
	public void destroy(GL2 gl) {

	}

	@Override
	public List<BoundingSphere> getBoundingSpheres() {
		// return s1 -> s2 -> s3 -> s4
		// where:
		// s1 - sphere bounding the whole car
		// s2 - sphere bounding the car front
		// s3 - sphere bounding the car center
		// s4 - sphere bounding the car back
		//
		// * NOTE:
		// All spheres should be adapted so that they are place relative to
		// the car model coordinate system.
		LinkedList<BoundingSphere> res = new LinkedList<BoundingSphere>();

		// s1 - sphere bounding the whole car
		Point center_s1 = new Point(0, Specification.B_HEIGHT / 2.0, 0);
		double center_to_front = new Vec(Specification.C_LENGTH / 2 + Specification.F_LENGTH, Specification.F_DEPTH / 2, 0.0).norm();
		double center_to_back = new Vec(Specification.C_LENGTH / 2 + Specification.B_LENGTH, Specification.B_HEIGHT / 2, Specification.B_DEPTH / 2).norm();

		double radius_s1 = Math.max(center_to_front, center_to_back);
		BoundingSphere boundingSphere = new BoundingSphere(radius_s1, center_s1);
		boundingSphere.setSphereColore3d(0.0, 0.0, 0.0);
		res.add(boundingSphere);

		// s2 - sphere bounding the car front
		BoundingSphere front_bounding_sphere = carFront.getBoundingSpheres().get(0);
		double translate_front_x = Specification.F_LENGTH / 2.0 + Specification.C_BASE_LENGTH / 2.0;
		// translate relative to the car model coordinate system
		front_bounding_sphere.translateCenter(translate_front_x, 0.0, 0.0);
		res.add(front_bounding_sphere);

		// s3 - sphere bounding the car center
		BoundingSphere center_bounding_sphere = carCenter.getBoundingSpheres().get(0);
		// no need to translate, already relative to the car model coordinate system
		res.add(center_bounding_sphere);

		// s4 - sphere bounding the car back
		BoundingSphere back_bounding_sphere = carBack.getBoundingSpheres().get(0);
		double translate_back_x = -Specification.B_LENGTH / 2.0 - Specification.C_BASE_LENGTH / 2.0;
		// translate relative to the car model coordinate system
		back_bounding_sphere.translateCenter(translate_back_x, 0.0, 0.0);
		res.add(back_bounding_sphere);

		return res;
	}
}
