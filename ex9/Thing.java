package ex9;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

/** Simple 'Entity' like class. */
public class Thing {
	public static double worldSize = 100;

	/** Kind of thing this thing is  */
	public String kind;
	/** Vert positions in the model to show */
	public List<Triple> modelPositions;
	/** Vert colors in the model to show */
	public List<Triple> modelColors;
	
	/** Entity position stores position of entity in 3d space.*/
	public Triple position;
	/** Entity rotation, stores angle in degrees of rotation on
	 * each x, y, and z axis */
	public Triple rotation;
	/** Entity Scale stores size of entity, along x,y,z axis*/
	public Triple scale;
	/** Entity Color */
	public Triple color;
	
	/** Entity movement */
	public double speed;
	/** Entity Rotation speed (y-axis) */
	public double rotSpeed;
	
	/** position buffer handle */
	private int pHandle;
	/** color buffer handle */
	private int cHandle;
	/** position buffer */
	private FloatBuffer pBuffer;
	/** color buffer */
	private FloatBuffer cBuffer;
	/** Vertex-Array-Object pointer */
	private int vaoLocation;
	/** Count of triangles loaded into model */
	private int numTris;
	
	/** Empty thing constructor, if we want to generate a thing. */
	public Thing() {
		init();
		kind = "Basic Thing";
		
		modelPositions = new ArrayList<>();
		modelColors = new ArrayList<>();
	}
	
	/** Load a thing from a file. */
	public Thing(Scanner sc) {
		init();
		
		try {
			// skip emptys until we get something in the kind variable...
			while (kind == null || kind.isEmpty()) {
				kind = sc.nextLine().trim(); 
			}
			
			// Read entity information 
			position = readV3(sc);
			rotation = readV3(sc);
			scale = readV3(sc);
			color = readV3(sc);
			
			speed = sc.nextDouble();
			rotSpeed = sc.nextDouble();
			
			// Read model information
			numTris = sc.nextInt();
			modelPositions = new ArrayList<>(numTris * 3);
			modelColors = new ArrayList<>(numTris * 3);
			
			for (int i = 0; i < numTris*3; i++) {
				Triple position = readV3(sc);
				Triple color = readV3(sc);
				modelPositions.add(position);
				modelColors.add(color);
			}
			
		} catch (Exception e) {
			System.out.println("Error loading thing kind = " + kind + " ex = " + e);
			throw new RuntimeException(e);
		}
		
	}
	
	/** Read 3 numbers, and don't require a newline. */
	private static Triple readV3(Scanner sc) {
		double x = sc.nextDouble();
		double y = sc.nextDouble();
		double z = sc.nextDouble();
		return new Triple(x, y, z);
	}

	public void moveRelative(Triple movement) {
		double alpha = Math.toRadians(rotation.z);
		double beta = Math.toRadians(rotation.x);
		double n = 1;
		Triple e = position;
		Triple c = new Triple(e.x + Math.cos(beta) * n * Math.cos(alpha),
				e.y + Math.cos(beta) * n * Math.sin(alpha),
				e.z + n * Math.sin(beta)
		);

		// x = right/left
		// y = forward/backward
		// z = up/down
		Mat4 orientation = Mat4.lookAtOrientation(e, c, Triple.zAxis);
		Triple forward = orientation.forward().normalize().scale(movement.y);
		Triple right = orientation.right().normalize().scale(movement.x);
		Triple up  = orientation.up().normalize().scale(movement.z);

		position = new Triple(
				position.x + forward.x + right.x + up.x,
				position.y + forward.y + right.y + up.y,
				position.z + forward.z + right.z + up.z
		);

	}
	
	private void init() {
		position = new Triple(0,0,0);
		rotation = new Triple(0,0,0);
		scale = new Triple(1,1,1);
		color = new Triple(1,1,1);
		speed = 0;
		rotSpeed = 0;
	}
	
	public void update() {
		
		rotation = new Triple(
				rotation.x,
				rotation.y,
				rotation.z + rotSpeed * Time.deltaTime
		);

		moveRelative(new Triple(0, speed * Time.deltaTime, 0));

		// Match "Car" exactly
		if (kind.equals("Car")) {
			carLogic();
		}
		// Contains "Car" ( PlayerCar and Car )
		if (kind.contains("Car")) {
			clampToWorld();
		}
	}

	// really dumb way of keeping things in the world, but works
	public void clampToWorld() {
		double x = position.x, y = position.y, z = position.z;
		if (x > worldSize) { x = worldSize; }
		if (x < -worldSize) { x = -worldSize; }
		if (y > worldSize) { y = worldSize; }
		if (y < -worldSize) { y = -worldSize; }
		position = new Triple(x, y, z);
	}
	
	/** Gets the number of triangles in the model. */
	public int numTris() { return numTris; }
	
	/** Make a buffer big enough to hold all of the present triangles */
	private FloatBuffer makeBuffer() {
		// make byte buffer big enough to hold all the position data
		// tris * (verts per tri) * (x,y,z) * (bytes per float)
		ByteBuffer bb = ByteBuffer.allocateDirect(numTris * 3 * 3 * 4);
		
		// make sure that the order of the bytes in a single float is correct
		bb.order(ByteOrder.nativeOrder());
		
		// create float buffer from these bytes
		FloatBuffer fb = bb.asFloatBuffer();

		return fb;
	}
	
	/** Send this entity's model data into the graphics card. 
	 Retains handles of the locations of the information.
	 Doing this is only required once, unless the model data changes. */
	public void sendData() {
		
		// Create buffer handles on graphics card 
		pHandle = GL15.glGenBuffers();
		cHandle = GL15.glGenBuffers();
		
		// Create buffers with the right amount of space 
		pBuffer = makeBuffer();
		cBuffer = makeBuffer();
		
		// Put all the data in the buffers 
		for (int i = 0; i < numTris * 3; i++) {
			modelPositions.get(i).sendData(pBuffer);
			modelColors.get(i).sendData(cBuffer);
		}
		// Reset buffer positions 
		pBuffer.rewind();
		cBuffer.rewind();
		
		// Set position as 'Static Draw' since we won't update it
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, pHandle);
		Util.error("after bind pHandle");
		// Just a hint, typically doesn't change behavior
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, pBuffer, GL15.GL_STATIC_DRAW);
		Util.error("after set position static draw");
		
		// Set color  as 'Static Draw' since we won't update it
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, cHandle);
		Util.error("after bind cHandle");
		// Just a hint, typically doesn't change behavior
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, cBuffer, GL15.GL_STATIC_DRAW);
		Util.error("after set color static draw");
		
		// Create VAO object on graphics card, get pointer to it
		vaoLocation = GL30.glGenVertexArrays();
		Util.error("after generate single vertex array");
		
		// Just binds the Vertex-Array-Object,
		// this activates it so any changes to or usage of VAOs targets it.
		GL30.glBindVertexArray(vaoLocation);
		Util.error("after bind the vao");
		mapPositionColorArrays();
	}

	
	public Mat4 getTRS() {
		return Mat4.trs(position, rotation, scale);
	}
	
	public Triple getColor() {
		return color;
	}
	
	/** Draws the model */
	public void draw() {
		
		// Just binds the Vertex-Array-Object,
		// this activates it so any changes to or usage of VAOs targets it.
		GL30.glBindVertexArray(vaoLocation);
		Util.error("after bind the vao");
		
		// Then tell the pipeline to draw all of the triangles.
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, numTris * 3);
		Util.error("after draw arrays");
		
	}
	
	
	/** Enables position/color arrays/buffers in the VAO, and assigns them. */
	private void mapPositionColorArrays() {
		
		// Prepare VAO to hold index 0/1
		GL20.glEnableVertexAttribArray(0);  // position
		Util.error("after enable attrib 0");
		GL20.glEnableVertexAttribArray(1);  // color
		Util.error("after enable attrib 1");
		
		// Map Position buffer to index 0
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, pHandle);
		Util.error("after bind position buffer");
		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
		Util.error("after do position vertex attrib pointer");
		
		// Map Color buffer to index 1
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, cHandle);
		Util.error("after bind color buffer");
		GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 0, 0);
		Util.error("after do color vertex attrib pointer");
	}

	//////////////////////////////////////////////
	// car logic
	static Random rand = new Random();
	double turnTimeout = 0;
	double gasTimeout = 0;
	// Function called every frame to update NPCs (Non-Player-Cars)
	public void carLogic() {
		// Step each timer towards zero
		turnTimeout -= Time.deltaTime;
		gasTimeout -= Time.deltaTime;

		// Did turn time finish?
		if (turnTimeout <= 0) {

			// Are we turning?
			if (rotSpeed != 0) {
				// if so, stop
				rotSpeed = 0;
			} else {
				// otherwise, pick a new turning rate
				// Turn range between (-120, +120)
				rotSpeed = -120.0 + rand.nextDouble() * 240.0;
			}

			// reset timer so it can fire again later
			turnTimeout = 2 + rand.nextDouble() * 3;
		}

		// Did gas time finish?
		if (gasTimeout <= 0) {
			// are we moving?
			if (speed != 0) {
				// If so stop
				speed = 0;
			} else {
				// otherwise choose a random speed
				// Range (10, 40)
				speed = 10 + rand.nextDouble() * 30;
			}

			// Reset timer so it can fire again later
			gasTimeout = 1 + rand.nextDouble() * 2;
		}

	}

}
