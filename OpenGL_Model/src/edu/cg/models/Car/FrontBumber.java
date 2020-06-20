package edu.cg.models.Car;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import edu.cg.models.IRenderable;
import edu.cg.models.SkewedBox;

public class FrontBumber implements IRenderable {
	SkewedBox f_b_base;
	SkewedBox f_b_wing;


	@Override
	public void render(GL2 gl) {
		// Remember the dimensions of the bumper, this is important when you
		// combine the bumper with the hood.

		gl.glPushMatrix(); //remember origin position
		// Initialize quad
		GLU glu = new GLU();
		GLUquadric quad = glu.gluNewQuadric();

		// Draw the center skewed box (the base without the wings)
		Materials.SetBlackMetalMaterial(gl);
		this.f_b_base  = createBase();
		this.f_b_base.render(gl);

		//Draw first wing
		Double depth = Specification.F_BUMPER_WINGS_DEPTH / 2 + Specification.F_BUMPER_DEPTH / 2;
		gl.glTranslated(0,0,depth);
		gl.glPushMatrix(); // remember the first wing position
		this.f_b_wing  = createWing(gl);
		this.f_b_wing.render(gl);

		//Draw sphere
		renderSphere(gl,glu,quad);
		gl.glPopMatrix(); //return to first wing position

		//Draw second wing
		gl.glTranslated(0,0,-2*depth);
		gl.glPushMatrix();// remember the second wing position
		Materials.SetBlackMetalMaterial(gl); // return to wing's color
		this.f_b_wing.render(gl);

		//Draw sphere
		renderSphere(gl,glu,quad);
		gl.glPopMatrix();//return to second wing position

		gl.glPopMatrix(); //return to origin
		glu.gluDeleteQuadric(quad); //Clear from memory
	}

	@Override
	public void init(GL2 gl) {
	}

	@Override
	public String toString() {
		return "FrontBumper";
	}


	private SkewedBox createBase(){
		double h1 = Specification.F_BUMPER_HEIGHT_1;
		double h2 = Specification.F_BUMPER_HEIGHT_2;
		double length = Specification.F_BUMPER_LENGTH;
		double depth = Specification.F_BUMPER_DEPTH;

		return new SkewedBox(length,h1,h2,depth,depth);
	}
	private SkewedBox createWing(GL2 gl){
		Materials.SetBlackMetalMaterial(gl);
		double h1 = Specification.F_BUMPER_WINGS_HEIGHT_1;
		double h2 = Specification.F_BUMPER_WINGS_HEIGHT_2;
		double length = Specification.F_BUMPER_LENGTH;
		double depth = Specification.F_BUMPER_WINGS_DEPTH;

		return new SkewedBox(length,h1,h2,depth,depth);
	}

	private void renderSphere(GL2 gl, GLU glu, GLUquadric quad) {
		gl.glColor3d(1, 1, 0);
		double sphere_radius = Specification.F_BUMPER_WINGS_DEPTH / 2.5;
		gl.glTranslated(0.0, Specification.F_BUMPER_WINGS_DEPTH / 2.0, 0.0);
		glu.gluSphere(quad, sphere_radius, 10, 10);
	}
}


