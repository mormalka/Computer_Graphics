package edu.cg.scene.objects;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.Hit;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;

public class Sphere extends Shape {
	private Point center;
	private double radius;
	
	public Sphere(Point center, double radius) {
		this.center = center;
		this.radius = radius;
	}
	
	public Sphere() {
		this(new Point(0, -0.5, -6), 0.5);
	}
	
	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Sphere:" + endl + 
				"Center: " + center + endl +
				"Radius: " + radius + endl;
	}
	
	public Sphere initCenter(Point center) {
		this.center = center;
		return this;
	}
	
	public Sphere initRadius(double radius) {
		this.radius = radius;
		return this;
	}
	
	@Override
	public Hit intersect(Ray ray) {
		Vec sourceSubCenter = ray.source().sub(this.center);
		// a = 1 because ray's direction is normalized
		double b = ray.direction().mult(2).dot(sourceSubCenter);
		double c = sourceSubCenter.dot(sourceSubCenter) - (this.radius*this.radius);
		double sqrt_arg = Math.pow(b,2) -4 * c;
		Vec normal;
		if(sqrt_arg < 0){
			return null; // no solution
		}
		double t_1 = (-1*b - Math.sqrt(sqrt_arg)) / 2;
		double t_2 = (-1*b + Math.sqrt(sqrt_arg)) / 2;
		normal = ray.add(t_1).sub(this.center).normalize();
		if(t_2 <= 0){
			return null; // t_1 <= t_2 so both are non positive
		}
		if (t_1 <= 0){
			normal = ray.add(t_2).sub(this.center).normalize();
			return new Hit(t_2,normal);
		}
		return new Hit(t_1,normal);
	}
}
