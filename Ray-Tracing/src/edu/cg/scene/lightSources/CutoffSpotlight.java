package edu.cg.scene.lightSources;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.*;
import edu.cg.scene.objects.Surface;

public class CutoffSpotlight extends PointLight {
	private Vec direction;
	private double cutoffAngle;

	public CutoffSpotlight(Vec dirVec, double cutoffAngle) {
		this.direction = dirVec;
		this.cutoffAngle = cutoffAngle;
	}

	public CutoffSpotlight initDirection(Vec direction) {
		this.direction = direction;
		return this;
	}

	public CutoffSpotlight initCutoffAngle(double cutoffAngle) {
		this.cutoffAngle = cutoffAngle;
		return this;
	}

	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Spotlight: " + endl + description() + "Direction: " + direction + endl;
	}

	@Override
	public CutoffSpotlight initPosition(Point position) {
		return (CutoffSpotlight) super.initPosition(position);
	}

	@Override
	public CutoffSpotlight initIntensity(Vec intensity) {
		return (CutoffSpotlight) super.initIntensity(intensity);
	}

	@Override
	public CutoffSpotlight initDecayFactors(double q, double l, double c) {
		return (CutoffSpotlight) super.initDecayFactors(q, l, c);
	}

	@Override
	public boolean isOccludedBy(Surface surface, Ray rayToLight) {
		return super.isOccludedBy(surface, rayToLight); //TODO : CHECK
	}

	@Override
	public Vec intensity(Point hittingPoint, Ray rayToLight) {
		double cos_a = rayToLight.direction().mult(-1).dot(this.direction.normalize());
		double a = Math.acos(cos_a);
		if(Math.toDegrees(a) > this.cutoffAngle){
			return new Vec(); // intensity = 0
		}
		return super.intensity(hittingPoint, rayToLight).mult(cos_a);
	}
}