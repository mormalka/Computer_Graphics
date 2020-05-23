package edu.cg.scene;

import edu.cg.Logger;
import edu.cg.algebra.Point;
import edu.cg.algebra.*;
import edu.cg.scene.camera.PinholeCamera;
import edu.cg.scene.lightSources.Light;
import edu.cg.scene.objects.Surface;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Scene {
	private String name = "scene";
	private int maxRecursionLevel = 1;
	private int antiAliasingFactor = 1; // gets the values of 1, 2 and 3
	private boolean renderRefarctions = false;
	private boolean renderReflections = false;

	private PinholeCamera camera;
	private Vec ambient = new Vec(1, 1, 1); // white
	private Vec backgroundColor = new Vec(0, 0.5, 1); // blue sky
	private List<Light> lightSources = new LinkedList<>();
	private List<Surface> surfaces = new LinkedList<>();

	// MARK: initializers
	public Scene initCamera(Point eyePoistion, Vec towardsVec, Vec upVec, double distanceToPlain) {
		this.camera = new PinholeCamera(eyePoistion, towardsVec, upVec, distanceToPlain);
		return this;
	}

	public Scene initAmbient(Vec ambient) {
		this.ambient = ambient;
		return this;
	}

	public Scene initBackgroundColor(Vec backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}

	public Scene addLightSource(Light lightSource) {
		lightSources.add(lightSource);
		return this;
	}

	public Scene addSurface(Surface surface) {
		surfaces.add(surface);
		return this;
	}

	public Scene initMaxRecursionLevel(int maxRecursionLevel) {
		this.maxRecursionLevel = maxRecursionLevel;
		return this;
	}

	public Scene initAntiAliasingFactor(int antiAliasingFactor) {
		this.antiAliasingFactor = antiAliasingFactor;
		return this;
	}

	public Scene initName(String name) {
		this.name = name;
		return this;
	}

	public Scene initRenderRefarctions(boolean renderRefarctions) {
		this.renderRefarctions = renderRefarctions;
		return this;
	}

	public Scene initRenderReflections(boolean renderReflections) {
		this.renderReflections = renderReflections;
		return this;
	}

	// MARK: getters
	public String getName() {
		return name;
	}

	public int getFactor() {
		return antiAliasingFactor;
	}

	public int getMaxRecursionLevel() {
		return maxRecursionLevel;
	}

	public boolean getRenderRefarctions() {
		return renderRefarctions;
	}

	public boolean getRenderReflections() {
		return renderReflections;
	}

	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Camera: " + camera + endl + "Ambient: " + ambient + endl + "Background Color: " + backgroundColor + endl
				+ "Max recursion level: " + maxRecursionLevel + endl + "Anti aliasing factor: " + antiAliasingFactor
				+ endl + "Light sources:" + endl + lightSources + endl + "Surfaces:" + endl + surfaces;
	}

	private transient ExecutorService executor = null;
	private transient Logger logger = null;

	private void initSomeFields(int imgWidth, int imgHeight, Logger logger) {
		this.logger = logger;
		// TODO: initialize your additional field here.
	}

	public BufferedImage render(int imgWidth, int imgHeight, double viewAngle, Logger logger)
			throws InterruptedException, ExecutionException, IllegalArgumentException {

		initSomeFields(imgWidth, imgHeight, logger);

		BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
		camera.initResolution(imgHeight, imgWidth, viewAngle);
		int nThreads = Runtime.getRuntime().availableProcessors();
		nThreads = nThreads < 2 ? 2 : nThreads;
		this.logger.log("Intitialize executor. Using " + nThreads + " threads to render " + name);
		executor = Executors.newFixedThreadPool(nThreads);

		@SuppressWarnings("unchecked")
		Future<Color>[][] futures = (Future<Color>[][]) (new Future[imgHeight][imgWidth]);

		this.logger.log("Starting to shoot " + (imgHeight * imgWidth * antiAliasingFactor * antiAliasingFactor)
				+ " rays over " + name);

		for (int y = 0; y < imgHeight; ++y)
			for (int x = 0; x < imgWidth; ++x)
				futures[y][x] = calcColor(x, y);

		this.logger.log("Done shooting rays.");
		this.logger.log("Wating for results...");

		for (int y = 0; y < imgHeight; ++y)
			for (int x = 0; x < imgWidth; ++x) {
				Color color = futures[y][x].get();
				img.setRGB(x, y, color.getRGB());
			}

		executor.shutdown();

		this.logger.log("Ray tracing of " + name + " has been completed.");

		executor = null;
		this.logger = null;

		return img;
	}

	private Future<Color> calcColor(int x, int y) {
		return executor.submit(() -> {
			// TODO: You need to re-implement this method if you want to handle
			// super-sampling. You're also free to change the given implementation if you
			// want.
			Point centerPoint = camera.transform(x, y);
			Ray ray = new Ray(camera.getCameraPosition(), centerPoint);
			Vec color = calcColor(ray, 0);
			return color.toColor();
		});
	}



	private Vec calcColor(Ray ray, int recusionLevel) {
//		this.logger.log("calc color");
		// This is the recursive method in RayTracing.
		Hit closestHit = surfaces.get(0).intersect(ray); // initial to the intersection with the first item in surfaces
		Surface closestSurface = surfaces.get(0);
		for(Surface s: this.surfaces){ // search for the closest hit
			Hit currentHit = s.intersect(ray);
			if(currentHit != null){
				if(closestHit != null){
					if(closestHit.compareTo(currentHit) == 1){
						closestHit = currentHit;
						closestSurface = s;
					}
				} else {
					closestHit = currentHit;
					closestSurface = s;
				}
			}
		}

		if(closestHit == null){ // in case no intersection was found
			return this.backgroundColor;
		}
		closestHit.setSurface(closestSurface);

		Vec color = this.ambient.mult(closestHit.getSurface().Ka()); // add the ambient parameter
		Vec normal = closestHit.getNormalToSurface();
		Point hitPoint = ray.getHittingPoint(closestHit);

		for(Light L : this.lightSources){ // diffuse and specular
			double shadow = calcShadow(hitPoint, L);
			if(shadow == 1){
				Ray rayToLight = L.rayToLight(hitPoint);
				Vec diffuse = calcDiffuse(normal, closestHit, rayToLight);
				Vec d_n_s = diffuse.add(calcSpecular(normal, closestHit, rayToLight, ray));
				Vec I_L = L.intensity(hitPoint, rayToLight);

				color = color.add(d_n_s.mult(I_L));
			}
		}

		recusionLevel++;
		if (recusionLevel > this.maxRecursionLevel){
			return color;
		}
		if(this.renderReflections) {
			// reflective implementation
			if (closestHit.getSurface().reflectionIntensity() > 0) {
				Vec r_v = ray.direction().add(normal.mult(ray.direction().dot(normal)).mult(-2));
				Ray ReflectiveRay = new Ray(hitPoint, r_v);
				double K_R = closestHit.getSurface().reflectionIntensity();
				Vec temp_color = calcColor(ReflectiveRay, recusionLevel);

				color = color.add(temp_color.mult(K_R));
			}
		}
		if(this.renderRefarctions) {
			// reflective implementation-BONUS
			if(closestHit.getSurface().isTransparent()){
				Vec refractiveVec = Ops.refract(ray.direction(), normal, closestHit.getSurface().n1(closestHit), closestHit.getSurface().n2(closestHit));
				Ray refractiveRay = new Ray(hitPoint, refractiveVec);
				double K_T = closestHit.getSurface().refractionIntensity();
				Vec temp_color = calcColor(refractiveRay, recusionLevel);

				color = color.add(temp_color.mult(K_T));
			}
		}
		return color;
	}

	private Vec calcDiffuse(Vec normal, Hit closestHit, Ray rayToLight){
		Vec diffuseLight = closestHit.getSurface().Kd();
		double dotProd = normal.dot(rayToLight.direction());
		return diffuseLight.mult(Math.max(dotProd, 0));
	}

	private Vec calcSpecular (Vec normal, Hit closestHit, Ray rayToLight, Ray currentRay){
		Vec specularLight = closestHit.getSurface().Ks();
		int n = closestHit.getSurface().shininess();
		Vec u = rayToLight.direction();
		Vec R = u.add(normal.mult(u.dot(normal)).mult(-2)); //reflective vector
		Vec V = currentRay.direction();
		double dotProd = V.dot(R);

		if (dotProd < 0) return new Vec();
		return specularLight.mult(Math.pow(dotProd, n));

	}

	private double calcShadow(Point hitPoint, Light L){
		for(Surface s: this.surfaces){
			if(L.isOccludedBy(s, L.rayToLight(hitPoint))){
				return 0;
			}
		}
		return 1;
	}
}
