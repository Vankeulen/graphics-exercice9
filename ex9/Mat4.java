package ex9;

/*  4 by 4 matrices
 */
import java.nio.FloatBuffer;

public class Mat4 {

	public static final Mat4 IDENTITY = new Mat4(1,0,0,0,
												0,1,0,0,
												0,0,1,0,
												0,0,0,1);
	
	private double[][] data;
	
	public Mat4(double a11, double a12, double a13, double a14,
			double a21, double a22, double a23, double a24,
			double a31, double a32, double a33, double a34,
			double a41, double a42, double a43, double a44) {

		data = new double[4][4];

		data[0][0] = a11;
		data[0][1] = a12;
		data[0][2] = a13;
		data[0][3] = a14;
		data[1][0] = a21;
		data[1][1] = a22;
		data[1][2] = a23;
		data[1][3] = a24;
		data[2][0] = a31;
		data[2][1] = a32;
		data[2][2] = a33;
		data[2][3] = a34;
		data[3][0] = a41;
		data[3][1] = a42;
		data[3][2] = a43;
		data[3][3] = a44;

	}

	public static Mat4 frustum(double l, double r, double b, double t,
			double n, double f) {
		return new Mat4(2 * n / (r - l), 0, (r + l) / (r - l), 0,
				0, 2 * n / (t - b), (t + b) / (t - b), 0,
				0, 0, -(f + n) / (f - n), -2 * f * n / (f - n),
				0, 0, -1, 0);
	}

	// given eyepoint e, look at point c, and up direction,
	// form the look at matrix
	public static Mat4 lookAt(Triple e, Triple c, Triple up) {

		Triple n = c.minus(e).normalize();
		Triple r = n.cross(up).normalize();
		Triple w = r.cross(n);  // note: ||r X n || = 
		//        |sin(angle between)| ||r|| || n||

		// System.out.println("n=" + n + " r= " + r + " w= " + w );
		Mat4 translate = new Mat4(1, 0, 0, -e.x,
				0, 1, 0, -e.y,
				0, 0, 1, -e.z,
				0, 0, 0, 1);

		Mat4 rotate = new Mat4(r.x, r.y, r.z, 0,
				w.x, w.y, w.z, 0,
				-n.x, -n.y, -n.z, 0,
				0, 0, 0, 1);

		Mat4 look = rotate.mult(translate);

		//System.out.println("look at matrix is \n" + look);

		//System.out.println(" lookAt * e = " + look.multAndPerspDiv(e));
		//System.out.println(" lookAt * c = " + look.multAndPerspDiv(c));
		/*
		System.out.println("Rn = " + rotate.multAndPerspDiv(n)
				+ "Rr = " + rotate.multAndPerspDiv(r)
				+ "Rw = " + rotate.multAndPerspDiv(w));
			//*/
		
		return look;
	}
	// given eyepoint e, look at point c, and up direction,
	// form just the orientation matrix
	public static Mat4 lookAtOrientation (Triple e, Triple c, Triple up) {
		Triple n = c.minus(e).normalize();
		Triple r = n.cross(up).normalize();
		Triple w = r.cross(n);  // note: ||r X n || = 
		//        |sin(angle between)| ||r|| || n||

		Mat4 rotate = new Mat4(r.x, r.y, r.z, 0,
				w.x, w.y, w.z, 0,
				-n.x, -n.y, -n.z, 0,
				0, 0, 0, 1);
		return rotate;
	}
	
	
	
	// orientation matrix 'n' row should be backward direction
	public Triple forward() { return new Triple(-data[2][0], -data[2][1], -data[2][2]); }
	// orientation matrix 'w' row should be upward direction
	public Triple up() { return new Triple(data[1][0], data[1][1], data[1][2]); }
	// orientation matrix 'r' row should be rightward direction
	public Triple right() { return new Triple(data[0][0], data[0][1], data[0][2]); }
	
	public static Mat4 trs(Triple tr, Triple rot, Triple sc) {
		Mat4 t = translate(tr.x, tr.y, tr.z);
		Mat4 rx = rotate(1, 0, 0, rot.x);
		Mat4 ry = rotate(0, 1, 0, rot.y);
		Mat4 rz = rotate(0, 0, 1, rot.z);
		Mat4 s = scale(sc.x, sc.y, sc.z);
		
		Mat4 trs = s;
		trs = trs.mult(rz);
		trs = trs.mult(ry);
		trs = trs.mult(rx);
		trs = trs.mult(t);
		
		return trs;
	}
	
	public static Mat4 translate(double x, double y, double z) {
		return new Mat4(1, 0, 0, x,
						0, 1, 0, y, 
						0, 0, 1, z,
						0, 0, 0, 1);
	}
	
	public static Mat4 scale(double x, double y, double z) {
		return new Mat4(x, 0, 0, 0,
						0, y, 0, 0,
						0, 0, z, 0,
						0, 0, 0, 1);
	}

	// produce the general rotation matrix for
	// given axis (x,y,z) and angle in degrees
	public static Mat4 rotate(double x, double y, double z,
			double angle) {
		double len = Math.sqrt(x * x + y * y + z * z);
		x /= len;
		y /= len;
		z /= len;
		double c = Math.cos(Math.toRadians(angle));
		double s = Math.sin(Math.toRadians(angle));

		// for efficency and convenience:
		double onec = 1 - c,
				xy = x * y, xz = x * z, yz = y * z,
				sx = s * x, sy = s * y, sz = s * z;

		return new Mat4(
				x * x * onec + c, xy * onec - sz, xz * onec + sy, 0,
				xy * onec, y * y * onec + c, yz * onec - sx, 0,
				xz * onec - sy, yz * onec + sx, z * z * onec + c, 0,
				0, 0, 0, 1);

	}

	// multiply this 4 by 4 matrix by the other one
	// (like  this * other)
	public Mat4 mult(Mat4 other) {

		Mat4 result = new Mat4(0, 0, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 0,
				0, 0, 0, 0);

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				for (int k = 0; k < 4; k++) {
					result.data[row][col] += data[row][k] * other.data[k][col];
				}
			}
		}

		return result;
	}

	public String toString() {
		String s = "";

		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				s += String.format(" %10.3f", data[row][col]);
			}
			s += "\n";
		}

		return s;
	}

	// multiply Triple v with 1 appended in the 4th spot, 
	// do perspective divide and return the resulting triple
	public Triple multAndPerspDiv(Triple v) {

		double x = data[0][0] * v.x + data[0][1] * v.y + data[0][2] * v.z + data[0][3];
		double y = data[1][0] * v.x + data[1][1] * v.y + data[1][2] * v.z + data[1][3];
		double z = data[2][0] * v.x + data[2][1] * v.y + data[2][2] * v.z + data[2][3];
		double w = data[3][0] * v.x + data[3][1] * v.y + data[3][2] * v.z + data[3][3];

		// System.out.println("before persp div: " + x + " " + y + " " + z + " " + w);
		// do perspective division:
		x /= w;
		y /= w;
		z /= w;

		return new Triple(x, y, z);

	}

	// copy the 16 numbers for this matrix (as floats)
	// into the given buffer
	// (note: row major)
	public void sendData(FloatBuffer buffer) {
		buffer.rewind();
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				buffer.put((float) data[row][col]);
			}
		}
		buffer.rewind();
	}

	// multiply this matrix by the given vector v
	public double[] mult(double[] v) {
		double[] r = new double[4];
		for (int row = 0; row < 4; row++) {
			for (int col = 0; col < 4; col++) {
				r[row] += data[row][col] * v[col];
			}
		}
		return r;
	}

	public static void main(String[] args) {

		Mat4 frustum = Mat4.frustum(-2, 2, -2, 2, 10, 50);
		frustum.data[2][3] = -5;
		Mat4 look = (new Mat4(1, 0, 0, 0,
				0, 1, 0, -10,
				0, 0, 1, -10,
				0, 0, 0, 1)).mult(
				new Mat4(1, -3, 0, 0,
						0, 0, 1, 0,
						-3, -1, 0, 0,
						0, 0, 0, 1));

		System.out.println("F:\n" + frustum);
		System.out.println("L:\n" + look);

		Mat4 tran = look.mult(frustum);
		System.out.println("LF = \n" + tran);

		Triple a = new Triple(10, 20, 5);
		Triple b = new Triple(20, 10, 5);
		Triple c = new Triple(15, 15, 20);

		System.out.println(tran.multAndPerspDiv(a));
		System.out.println(tran.multAndPerspDiv(b));
		System.out.println(tran.multAndPerspDiv(c));

		/*
      double l = 2, r = 5, b = 1, t = 3, n = 1, f = 100;

      Mat4 frustum = Mat4.frustum( l, r, b, t, n, f );
      System.out.println("frustum is: \n" + frustum );

      Triple nllc = new Triple( l, b, -n );
      Triple fllc = new Triple( f*l/n, f*b/n, -f );

      Triple v = frustum.multAndPerspDiv( nllc );
      System.out.println("near lower left corner projects to " + v );

      v = frustum.multAndPerspDiv( fllc );
      System.out.println("far lower left corner projects to " + v );

      Triple p = new Triple( 7, 8, -91 );
      v = frustum.multAndPerspDiv( p );
      System.out.println("randomish point projects to " + v );

      System.out.println("do some look at testing");

//      Mat4 look = Mat4.lookAt( new Triple(3,2,1), new Triple(-1, 4, 3),
//                               Triple.zAxis );

      Mat4 look = Mat4.lookAt( new Triple(3,1,10), new Triple(3, 1, 5),
                               Triple.xAxis );
      System.out.println("look * a = " + look.multAndPerspDiv( new Triple(2,1,5) ) );
      System.out.println("look * b = " + look.multAndPerspDiv( new Triple(3,3,5) ) );
		 */
	}

	private static String show(double[] x) {
		String r = "";
		for (int k = 0; k < x.length; k++) {
			r += x[k] + " ";
		}
		return r;
	}

}
