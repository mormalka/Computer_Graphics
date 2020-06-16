package edu.cg.models.Car;

import com.jogamp.opengl.GL2;
import edu.cg.algebra.Point;
import edu.cg.algebra.Vec;
import edu.cg.models.BoundingSphere;
import edu.cg.models.IIntersectable;
import edu.cg.models.IRenderable;

import java.util.LinkedList;
import java.util.List;

public class Front implements IRenderable, IIntersectable {
	private FrontHood hood = new FrontHood();
	private PairOfWheels wheels = new PairOfWheels();

	//new front bumper feature
	private FrontBumber frontBumber = new FrontBumber();


	@Override
	public void render(GL2 gl) {
		gl.glPushMatrix();
		// Render hood - Use Red Material.
		gl.glTranslated(-Specification.F_LENGTH / 2.0 + Specification.F_HOOD_LENGTH / 2.0, 0.0, 0.0);
		hood.render(gl);

		// Render the wheels.
		gl.glTranslated(Specification.F_HOOD_LENGTH / 2.0 - 1.25 * Specification.TIRE_RADIUS,
				0.5 * Specification.TIRE_RADIUS, 0.0);
		wheels.render(gl);

		// Render front bumber
		gl.glTranslated(1.25*Specification.TIRE_RADIUS + Specification.F_BUMPER_LENGTH / 2.0, -0.5*Specification.TIRE_RADIUS, 0.0);
		frontBumber.render(gl);

		gl.glPopMatrix();

	}

	@Override
	public void init(GL2 gl) {
	}

	@Override
	public List<BoundingSphere> getBoundingSpheres() {
		// where:
		// s1 - sphere bounding the car front
		LinkedList<BoundingSphere> res = new LinkedList<BoundingSphere>();
		Point center = new Point(0, Specification.F_HEIGHT / 2, 0);
		double radius = new Vec(Specification.F_LENGTH / 2, Specification.F_HEIGHT / 2, Specification.F_DEPTH / 2).norm();
		BoundingSphere boundingSphere = new BoundingSphere(radius, center);
		boundingSphere.setSphereColore3d(0.8, 0, 0.4);
		res.add(boundingSphere);
		return res;
	}

	@Override
	public String toString() {
		return "CarFront";
	}
}
