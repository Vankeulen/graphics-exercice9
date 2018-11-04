/* 
  hold the data for a
  bunch of triangles
 */

import java.util.Scanner;
import java.util.ArrayList;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.opengl.*;

public class TriList {

	private ArrayList<Triangle> list;

	FloatBuffer positionBuffer, colorBuffer;

	int vao;

	public TriList(Scanner input) {

		list = new ArrayList<Triangle>();

		int numTris = input.nextInt();

		for (int k = 0; k < numTris; k++) {
			list.add(new Triangle(input));
		}
	}

	public int size() {
		return list.size();
	}

	public int getVAO() {
		return vao;
	}

	// take list of triangles and set up the GPU
	// to have all their data (position and color)
	public void sendData() {

		// create vertex buffer objects and their handles one at a time
		int positionHandle = GL15.glGenBuffers();
		int colorHandle = GL15.glGenBuffers();
//      System.out.println("have position handle " + positionHandle +
//                         " and color handle " + colorHandle );

		// create the buffers
		positionBuffer = makeBuffer();
		colorBuffer = makeBuffer();

		// connect triangle data to the VBO's
		// first turn the arrays into buffers
		for (int k = 0; k < list.size(); k++) {
			// process triangle k
			list.get(k).sendData(positionBuffer, colorBuffer);
		}

		positionBuffer.rewind();
		colorBuffer.rewind();

		// Util.showBuffer("position:", positionBuffer);
		// Util.showBuffer("color:", colorBuffer);
		// now connect the buffers
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionHandle);
		Util.error("after bind positionHandle");
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
				positionBuffer, GL15.GL_STATIC_DRAW);
		Util.error("after set position data");
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorHandle);
		Util.error("after bind colorHandle");
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER,
				colorBuffer, GL15.GL_STATIC_DRAW);
		Util.error("after set color data");

		// set up vertex array object
		// using convenience form that produces one vertex array handle
		vao = GL30.glGenVertexArrays();
		Util.error("after generate single vertex array");
		GL30.glBindVertexArray(vao);
		Util.error("after bind the vao");
//      System.out.println("vao is " + vao );

		// enable the vertex array attributes
		GL20.glEnableVertexAttribArray(0);  // position
		Util.error("after enable attrib 0");
		GL20.glEnableVertexAttribArray(1);  // color
		Util.error("after enable attrib 1");

		// map index 0 to the position buffer, index 1 to the color buffer
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionHandle);
		Util.error("after bind position buffer");
		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
		Util.error("after do position vertex attrib pointer");

		// map index 1 to the color buffer
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, colorHandle);
		Util.error("after bind color buffer");
		GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, 0);
		Util.error("after do color vertex attrib pointer");
	}

	private FloatBuffer makeBuffer() {
		// make byte buffer big enough to hold all the position data
		ByteBuffer bb = ByteBuffer.allocateDirect(size() * 3 * 3 * 4);
		// make sure that the order of the bytes in a single float is correct
		bb.order(ByteOrder.nativeOrder());
		// create float buffer from these bytes
		FloatBuffer fb = bb.asFloatBuffer();

		return fb;
	}

}
