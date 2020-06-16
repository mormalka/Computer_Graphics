package edu.cg.models.Car;

import java.util.LinkedList;
import java.util.List;

import com.jogamp.opengl.GL2;

import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import edu.cg.algebra.Point;
import edu.cg.algebra.Vec;
import edu.cg.models.BoundingSphere;
import edu.cg.models.IIntersectable;
import edu.cg.models.IRenderable;
import edu.cg.models.SkewedBox;

public class Center implements IRenderable, IIntersectable {

	private SkewedBox bodyBase = new SkewedBox(Specification.C_BASE_LENGTH, Specification.C_BASE_HEIGHT,
			Specification.C_BASE_HEIGHT, Specification.C_DEPTH, Specification.C_DEPTH);
	private SkewedBox backBox = new SkewedBox(Specification.C_BACK_LENGTH, Specification.C_BACK_HEIGHT_1,
			Specification.C_BACK_HEIGHT_2, Specification.C_BACK_DEPTH, Specification.C_BACK_DEPTH);
	private SkewedBox frontBox = new SkewedBox(Specification.C_FRONT_LENGTH, Specification.C_FRONT_HEIGHT_1,
			Specification.C_FRONT_HEIGHT_2, Specification.C_FRONT_DEPTH_1, Specification.C_FRONT_DEPTH_2);
	private SkewedBox sideBox = new SkewedBox(Specification.C_SIDE_LENGTH, Specification.C_SIDE_HEIGHT_1,
			Specification.C_SIDE_HEIGHT_2, Specification.C_SIDE_DEPTH_1, Specification.C_SIDE_DEPTH_2);

	@Override
	public void render(GL2 gl) {
		gl.glPushMatrix();
		Materials.SetBlackMetalMaterial(gl);
		bodyBase.render(gl);
		Materials.SetRedMetalMaterial(gl);
		gl.glTranslated(Specification.C_BASE_LENGTH / 2.0 - Specification.C_FRONT_LENGTH / 2.0,
				Specification.C_BASE_HEIGHT, 0.0);
		frontBox.render(gl);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glTranslated(-Specification.C_BASE_LENGTH / 2.0 + Specification.C_FRONT_LENGTH / 2.0,
				Specification.C_BASE_HEIGHT, 0.0);
		gl.glRotated(180, 0.0, 1.0, 0.0);
		frontBox.render(gl);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glTranslated(0.0, Specification.C_BASE_HEIGHT,
				Specification.C_SIDE_LENGTH / 2 + Specification.C_FRONT_DEPTH_1 / 2.0);
		gl.glRotated(90, 0.0, 1.0, 0.0);
		sideBox.render(gl);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glTranslated(0.0, Specification.C_BASE_HEIGHT,
				-Specification.C_SIDE_LENGTH / 2 - Specification.C_FRONT_DEPTH_1 / 2.0);
		gl.glRotated(-90, 0.0, 1.0, 0.0);
		sideBox.render(gl);
		gl.glPopMatrix();
		Materials.SetBlackMetalMaterial(gl);
		gl.glPushMatrix();
		gl.glTranslated(
				-Specification.C_BASE_LENGTH / 2.0 + Specification.C_FRONT_LENGTH + Specification.C_BACK_LENGTH / 2.0,
				Specification.C_BASE_HEIGHT, 0.0);
		backBox.render(gl);
		gl.glPopMatrix();

		//Add designing
		//right mirror
		GLU glu = new GLU();
		gl.glColor3d(0.66, 0.66, 0.66);
		GLUquadric quad = glu.gluNewQuadric();
		gl.glPushMatrix();
		gl.glTranslated(Specification.C_BASE_LENGTH / 2.0, Specification.C_HIEGHT / 2.0, Specification.C_DEPTH / 2.0);
		gl.glRotated(90.0, 0.0, 0.0, 1.0);
		glu.gluCylinder(quad, Specification.S_ROD_RADIUS, Specification.S_ROD_RADIUS, Specification.S_ROD_HIEGHT, 8, 2);
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glTranslated(Specification.C_BASE_LENGTH / 2.0, Specification.C_HIEGHT / 2.0 -0.5*Specification.S_ROD_RADIUS, Specification.C_DEPTH / 2.0 + 1.5*Specification.S_ROD_HIEGHT);
		gl.glRotated(-90.0, -1.0, 3.0, -1.0);
		double h1 = Specification.F_BUMPER_HEIGHT_1 / 2.0;
		double h2 = Specification.F_BUMPER_HEIGHT_1 / 2.0;
		double length = Specification.S_ROD_HIEGHT;
		double depth = Specification.S_RODS_SIZE / 2.0;

		new SkewedBox(length,h1,h2,depth,depth).render(gl);
		gl.glPopMatrix();

		//left mirror
		gl.glPushMatrix();
		gl.glTranslated(Specification.C_BASE_LENGTH / 2.0, Specification.C_HIEGHT / 2.0, -Specification.C_DEPTH / 2.0 -Specification.S_ROD_HIEGHT);
		gl.glRotated(90.0, 0.0, 0.0, 1.0);
		glu.gluCylinder(quad, Specification.S_ROD_RADIUS, Specification.S_ROD_RADIUS, Specification.S_ROD_HIEGHT, 8, 2);
		gl.glPopMatrix();

		gl.glPushMatrix();
		gl.glTranslated(Specification.C_BASE_LENGTH / 2.0, Specification.C_HIEGHT / 2.0 -0.5*Specification.S_ROD_RADIUS, -Specification.C_DEPTH / 2.0 - 1.5*Specification.S_ROD_HIEGHT);
		gl.glRotated(-90.0, -1.0, 3.0, -1.0);
		h1 = Specification.F_BUMPER_HEIGHT_1 / 2.0;
		h2 = Specification.F_BUMPER_HEIGHT_1 / 2.0;
		length = Specification.S_ROD_HIEGHT;
		depth = Specification.S_RODS_SIZE / 2.0;

		new SkewedBox(length,h1,h2,depth,depth).render(gl);
		gl.glPopMatrix();

		glu.gluDeleteQuadric(quad); //Clear from memory
	}

	@Override
	public void init(GL2 gl) {

	}

	@Override
	public List<BoundingSphere> getBoundingSpheres() {
		// Return a list of bounding spheres the list structure is as follow:
		// s1
		// where:
		// s1 - sphere bounding the car front
		LinkedList<BoundingSphere> res = new LinkedList<BoundingSphere>();
		Point center = new Point(0, Specification.C_HIEGHT / 2.0, 0.0);
		double radius = new Vec(Specification.C_LENGTH / 2, Specification.C_HIEGHT / 2, Specification.C_DEPTH / 2).norm();
		BoundingSphere boundingSphere = new BoundingSphere(radius, center);
		boundingSphere.setSphereColore3d(0, 0, 0.5);
		res.add(boundingSphere);
		return res;
	}
}
