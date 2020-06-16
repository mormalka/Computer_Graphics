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
	// TODO : Add new design features to the car.
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
	public List<BoundingSphere> getBoundingSpheres() {
		// TODO: Return a list of bounding spheres the list structure is as follow:
		// s1 -> s2 -> s3 -> s4
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
//		double radius_s1 = new Vec(Specification.F_LENGTH / 2, Specification.F_HEIGHT / 2, Specification.F_DEPTH / 2).norm();
		double center_to_front = center_s1.dist(new Point(Specification.F_LENGTH + Specification.F_BUMPER_LENGTH, 0.0, 0.0));
		double center_to_back = center_s1.dist(new Point(Specification.B_LENGTH + Specification.S_LENGTH, 0.0, 0.0));
		double radius_s1 = Math.max(center_to_front, center_to_back); // TODO : ours 0.7666, t : 0.75208
		BoundingSphere boundingSphere = new BoundingSphere(radius_s1, center_s1);
		boundingSphere.setSphereColore3d(0.0, 0.0, 0.0);
		res.add(boundingSphere);

		// s2 - sphere bounding the car front
		List<BoundingSphere> front_bounding_spheres = carFront.getBoundingSpheres();
		double translate_front_x = Specification.F_LENGTH / 2.0 + Specification.C_BASE_LENGTH / 2.0;
		// translate relative to the car model coordinate system
		for(BoundingSphere current_bs: front_bounding_spheres){
			current_bs.translateCenter(translate_front_x, 0.0, 0.0);
			res.add(current_bs);
		}

		// s3 - sphere bounding the car center
		List<BoundingSphere> center_bounding_spheres = carCenter.getBoundingSpheres();
		for(BoundingSphere current_bs: center_bounding_spheres){
			// no need to translate, already relative to the car model coordinate system
			res.add(current_bs);
		}

		// s4 - sphere bounding the car back
		List<BoundingSphere> back_bounding_spheres = carBack.getBoundingSpheres();
		double translate_back_x = -Specification.B_LENGTH / 2.0 - Specification.C_BASE_LENGTH / 2.0;
		// translate relative to the car model coordinate system
		for(BoundingSphere current_bs: back_bounding_spheres){
			current_bs.translateCenter(translate_back_x, 0.0, 0.0);
			res.add(current_bs);
		}

		return res;
	}
}
