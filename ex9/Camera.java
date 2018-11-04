package ex9;

/*
   holds the viewing data and
   generates frustum and lookAt
   matrices
 */
import java.util.Scanner;
import java.nio.FloatBuffer;

public class Camera {

	// human modeling of viewing setup
	private Triple e;
	private double azi, alt;

	// frustum data
	private double l, r, b, t, n, f;
	private Mat4 frustum, orientation;

	private FloatBuffer fBuff, lBuff;

	private double speed;  // camera's current speed

	public Camera(double left, double right, double bottom, double top,
			double near, double far,
			Triple eye, double azimuth, double altitude) {
		l = left;
		r = right;
		b = bottom;
		t = top;
		n = near;
		f = far;

		e = eye;
		azi = azimuth;
		alt = altitude;

		fBuff = Util.createFloatBuffer(16);
		lBuff = Util.createFloatBuffer(16);

		speed = 0;

		update();
	}
	
	public void moveRelative(Triple movement) {
		double alpha = Math.toRadians(azi); // yaw
		double beta = Math.toRadians(alt); // pitch
		Triple c = new Triple(e.x + Math.cos(beta) * n * Math.cos(alpha),
				e.y + Math.cos(beta) * n * Math.sin(alpha),
				e.z + n * Math.sin(beta)
		);
		
		// x = right/left
		// y = forward/backward????
		// z = up/down??????
		
		orientation = Mat4.lookAtOrientation(e, c, Triple.zAxis);
		Triple forward = orientation.forward().normalize().scale(movement.y);
		Triple right = orientation.right().normalize().scale(movement.x);
		Triple up  = orientation.up().normalize().scale(movement.z);
		
		e = new Triple(
			e.x + forward.x + right.x + up.x,
			e.y + forward.y + right.y + up.y,
			e.z + forward.z + right.z + up.z
		);
		
		
	}

	// from core data update frustum and lookAt
	public void update() {

		// compute c:
		double alpha = Math.toRadians(azi);  // azi, alt are in degrees
		double beta = Math.toRadians(alt);
		Triple c = new Triple(e.x + Math.cos(beta) * n * Math.cos(alpha),
				e.y + Math.cos(beta) * n * Math.sin(alpha),
				e.z + n * Math.sin(beta)
		);

		orientation = Mat4.lookAt(e, c, Triple.zAxis);

		frustum = Mat4.frustum(l, r, b, t, n, f);

		// fill buffers
		frustum.sendData(fBuff);
		orientation.sendData(lBuff);

	}

	public void move() {
		double angle = Math.toRadians(azi);
		e = e.add(new Triple(speed * Math.cos(angle),
				speed * Math.sin(angle),
				0));
		update();
	}

	public void go() {
		speed = 0.25;
	}

	public void stop() {
		speed = 0;
	}

	public FloatBuffer getFrustumBuffer() {
		return fBuff;
	}

	public FloatBuffer getLookAtBuffer() {
		return lBuff;
	}

	public void shift(double dx, double dy, double dz) {
		e = e.add(new Triple(dx, dy, dz));
		update();
	}

	public void rotate(double amount) {
		azi += amount;

		// adjust to stay in [0,360) range
		if (azi < 0) {
			azi += 360;
		}
		if (azi >= 360) {
			azi -= 360;
		}

		update();
	}

	public void tilt(double amount) {
		alt += amount;

		update();
	}

	public String toString() {
		return "eye: " + e;
	}

}
