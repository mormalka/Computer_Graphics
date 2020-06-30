package edu.cg;

import java.awt.Component;
import java.util.List;

import javax.swing.JOptionPane;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;

import edu.cg.algebra.Point;
import edu.cg.algebra.Vec;
import edu.cg.models.BoundingSphere;
import edu.cg.models.Track;
import edu.cg.models.TrackSegment;
import edu.cg.models.Car.F1Car;
import edu.cg.models.Car.Specification;

/**
 * An OpenGL 3D Game.
 *
 */
public class NeedForSpeed implements GLEventListener {
	private GameState gameState = null; // Tracks the car movement and orientation
	private F1Car car = null; // The F1 car we want to render
	private Vec carCameraTranslation = null; // The accumulated translation that should be applied on the car, camera
												// and light sources
	private Track gameTrack = null; // The game track we want to render
	private FPSAnimator ani; // This object is responsible to redraw the model with a constant FPS
	private Component glPanel; // The canvas we draw on.
	private boolean isModelInitialized = false; // Whether model.init() was called.
	private boolean isDayMode = true; // Indicates whether the lighting mode is day/night.
	private boolean isBirdseyeView = false; // Indicates whether the camera is looking from above on the scene or
											// looking
	// towards the car direction.
	// - Car initial position (should be fixed).
	// - Camera initial position (should be fixed)
	// - Different camera settings
	// - Light colors
	// Or in short anything reusable - this make it easier for your to keep track of your implementation.
	private Point carInitialPos;
	private Point birdInitialPos;
	private Point personInitialPos;
	private double scale;

	public NeedForSpeed(Component glPanel) {
		this.glPanel = glPanel;
		gameState = new GameState();
		gameTrack = new Track();
		carCameraTranslation = new Vec(0.0);
		car = new F1Car();
		// initial new fields
		this.scale = 4.0;
		this.carInitialPos = new Point( 0.0, 0.3, -4.5);
		this.birdInitialPos = new Point(0.0, 50.0, this.carInitialPos.z - 22.0 - 3.0);
		this.personInitialPos = new Point(0.0, 2.0, this.carInitialPos.z + 4.0 + 2.6);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		if (!isModelInitialized) {
			initModel(gl);
		}
		if (isDayMode) {
			gl.glClearColor(0.5f,0.7f, 1.0f ,1.0f);
		} else {
			gl.glClearColor(0.3f, 0.3f, 0.4f, 1.0f);
		}
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		// Step (1) Update the accumulated translation that needs to be
		// applied on the car, camera and light sources.
		updateCarCameraTranslation(gl);
		// Step (2) Position the camera and setup its orientation
		setupCamera(gl);
		// Step (3) setup the lights.
		setupLights(gl);
		// Step (4) render the car.
		renderCar(gl);
		// Step (5) render the track.
		renderTrack(gl);
		// Step (6) check collision. Note this has nothing to do with OpenGL.
		if (checkCollision()) {
			JOptionPane.showMessageDialog(this.glPanel, "Game is Over");
			this.gameState.resetGameState();
			this.carCameraTranslation = new Vec(0.0);
		}

	}

	/**
	 * @return Checks if the car intersects the one of the boxes on the track.
	 */
	private boolean checkCollision() {
		 List<BoundingSphere> trackBoundingSpheres = gameTrack.getBoundingSpheres();
		 List<BoundingSphere> carBoundingSpheres = car.getBoundingSpheres();
		// update car's spheres to car new size and position
		 for(BoundingSphere bs : carBoundingSpheres){
			 bs.translateCenter(this.carInitialPos.x + this.carCameraTranslation.x, this.carInitialPos.y + this.carCameraTranslation.y, this.carInitialPos.z + this.carCameraTranslation.z);
			 bs.setRadius(bs.getRadius()*this.scale);
		 }
		 for(BoundingSphere box : trackBoundingSpheres){
		 	//check intersection
		 	if(box.checkIntersection(carBoundingSpheres.get(0))){
				for (BoundingSphere carBoundingSphere : carBoundingSpheres){
					if(carBoundingSphere.checkIntersection(box) && (carBoundingSpheres.indexOf(carBoundingSphere) != 0)) return true;
				}
			}
		 }
		return false;
	}

	private void updateCarCameraTranslation(GL2 gl) {
		// Update the car and camera translation values (not the ModelView-Matrix).
		// - Always keep track of the car offset relative to the starting
		// point.
		// - Change the track segments here.
		Vec ret = gameState.getNextTranslation();
		carCameraTranslation = carCameraTranslation.add(ret);
		double dx = Math.max(carCameraTranslation.x, -TrackSegment.ASPHALT_TEXTURE_DEPTH / 2.0 - 2);
		carCameraTranslation.x = (float) Math.min(dx, TrackSegment.ASPHALT_TEXTURE_DEPTH / 2.0 + 2);
		if (Math.abs(carCameraTranslation.z) >= TrackSegment.TRACK_LENGTH + 10.0) {
			carCameraTranslation.z = -(float) (Math.abs(carCameraTranslation.z) % TrackSegment.TRACK_LENGTH);
			gameTrack.changeTrack(gl);
		}
	}

	private void setupCamera(GL2 gl) {
		GLU glu = new GLU();

		if (isBirdseyeView) {
			// Setup camera for Birds-eye view
			// 50 meters above, looking down -y direction, up vec -z direction
			glu.gluLookAt(this.birdInitialPos.x + this.carCameraTranslation.x, this.birdInitialPos.y + this.carCameraTranslation.y, this.birdInitialPos.z + this.carCameraTranslation.z,
					this.birdInitialPos.x + this.carCameraTranslation.x, this.birdInitialPos.y + this.carCameraTranslation.y - 1.0, this.birdInitialPos.z + this.carCameraTranslation.z,
					0.0, 0.0, -1.0);
		} else {
			// Setup camera for Third-Person view
			// 4 meters behind, 2 above, looking at -z direction
			glu.gluLookAt(this.personInitialPos.x + this.carCameraTranslation.x, this.personInitialPos.y + this.carCameraTranslation.y, this.personInitialPos.z + this.carCameraTranslation.z,
					this.personInitialPos.x + this.carCameraTranslation.x, this.personInitialPos.y + this.carCameraTranslation.y, this.personInitialPos.z + this.carCameraTranslation.z - 1.0,
						0.0, 1.0, 0.0);
		}

	}

	private void setupLights(GL2 gl) {
		if (isDayMode) {
			//switch-off any light sources that were used in night mode and are not use in day mode.
			gl.glDisable(GL2.GL_LIGHT1);

			float[] sunIntensity = {1.f, 1.f, 1.f, 1.f};
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, sunIntensity, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, sunIntensity, 0);
			float[] sunDirection = {0.f, 1.f, 1.f, 0.f};
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, sunDirection, 0);
			gl.glEnable(GL2.GL_LIGHT0);


		} else {
			// switch-off any light sources that are used in day mode
			gl.glDisable(GL2.GL_LIGHT0);

			float spotlight_offset_x_axis =(float)((Specification.F_BUMPER_WINGS_DEPTH / 2.0));

			float[] spotlight_right_pos = {this.carCameraTranslation.x +spotlight_offset_x_axis,
					this.carInitialPos.y + 1.0f,
					this.carCameraTranslation.z -7.0f,
					1.0f};
			float[] spotlight_left_pos = {this.carCameraTranslation.x -spotlight_offset_x_axis,
					this.carInitialPos.y + 1.0f,
					this.carCameraTranslation.z -7.0f,
					1.0f};

			double nextRotation = Math.toRadians(this.gameState.getCarRotation());

			float[] spotlightIntensity = {0.9f, 0.9f, 0.9f, 1.f};
			float[] spotlightDirection = {(float) Math.sin(nextRotation), 0.f,(float) -Math.cos(nextRotation)};

			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, spotlight_right_pos, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, spotlight_left_pos, 0);

			gl.glLightf(GL2.GL_LIGHT0, GL2.GL_SPOT_CUTOFF,90.0f);
			gl.glLightf(GL2.GL_LIGHT1, GL2.GL_SPOT_CUTOFF,90.0f);

			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPOT_DIRECTION, spotlightDirection, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, spotlightIntensity, 0);
			gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, spotlightIntensity, 0);

			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPOT_DIRECTION, spotlightDirection, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, spotlightIntensity, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, spotlightIntensity, 0);


			//  moon-light using ambient light.
			float[] moon = {0.5f, 0.5f, 0.5f, 1.f};
			gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT,moon, 0);

			gl.glEnable(GL2.GL_LIGHT0);
			gl.glEnable(GL2.GL_LIGHT1);
		}

	}

	private void renderTrack(GL2 gl) {
		// * Note: the track is not translated. It should be fixed.
		gl.glPushMatrix();
		gameTrack.render(gl);
		gl.glPopMatrix();
	}

	private void renderCar(GL2 gl) {
		// * Remember: the car position should be the initial position + the accumulated translation.
		//             This will simulate the car movement.
		// * Remember: the car was modeled locally, you may need to rotate/scale and translate the car appropriately.
		// * Recommendation: it is recommended to define fields (such as car initial position) that can be used during rendering.
		gl.glPushMatrix();
		gl.glTranslated(this.carInitialPos.x + this.carCameraTranslation.x, this.carInitialPos.y + this.carCameraTranslation.y, this.carInitialPos.z + this.carCameraTranslation.z);
		double nextRotation = this.gameState.getCarRotation();
		gl.glRotated(90 - nextRotation, 0.0, 1.0, 0.0);
		// scale
		gl.glScaled(this.scale, this.scale, this.scale);

		this.car.render(gl);
		gl.glPopMatrix();
	}

	public GameState getGameState() {
		return gameState;
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		// Initialize display callback timer
		ani = new FPSAnimator(30, true);
		ani.add(drawable);
		glPanel.repaint();

		initModel(gl);
		ani.start();
	}

	public void initModel(GL2 gl) {
		gl.glCullFace(GL2.GL_BACK);
		gl.glEnable(GL2.GL_CULL_FACE);

		gl.glEnable(GL2.GL_NORMALIZE);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_SMOOTH);

		car.init(gl);
		gameTrack.init(gl);
		isModelInitialized = true;
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		GLU glu = new GLU();
		double ratio = (double)(width)/height;
		glu.gluPerspective(60, ratio, 2, 500);
	}

	/**
	 * Start redrawing the scene with 30 FPS
	 */
	public void startAnimation() {
		if (!ani.isAnimating())
			ani.start();
	}

	/**
	 * Stop redrawing the scene with 30 FPS
	 */
	public void stopAnimation() {
		if (ani.isAnimating())
			ani.stop();
	}

	public void toggleNightMode() {
		isDayMode = !isDayMode;
	}

	public void changeViewMode() {
		isBirdseyeView = !isBirdseyeView;
	}

}
