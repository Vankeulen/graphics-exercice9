package ex9;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class Thing {
	
	public String kind;
	public List<Triple> modelPositions;
	public List<Triple> modelColors;
	
	public Triple position;
	public Triple rotation;
	
	public double speed;
	public double rotSpeed;
	
	int pHandle;
	int cHandle;
	FloatBuffer pBuffer;
	FloatBuffer cBuffer;
	int vao;
	private int numTris;
	
	public Thing() {
		init();
		kind = "Basic Thing";
		
		modelPositions = new ArrayList<>();
		modelColors = new ArrayList<>();
	}

	public Thing(Scanner sc) {
		init();
		
		try {
			while (kind == null || kind.isEmpty()) {
				kind = sc.nextLine(); 
			}
			
			position = readV3(sc);
			rotation = readV3(sc);
			
			speed = sc.nextDouble();
			rotSpeed = sc.nextDouble();
			
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
	
	private static Triple readV3(Scanner sc) {
		double x = sc.nextDouble();
		double y = sc.nextDouble();
		double z = sc.nextDouble();
		return new Triple(x, y, z);
	}
	
	private void init() {
		
		position = new Triple(0,0,0);
		rotation = new Triple(0,0,0);
		speed = 0;
		rotSpeed = 0;
	}
	
	public int size() { return numTris; }
	
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
	
	public void sendData() {
		pHandle = GL15.glGenBuffers();
		cHandle = GL15.glGenBuffers();
		
		System.out.printf("Have handles %d, %d\n", pHandle, cHandle);
		
		pBuffer = makeBuffer();
		cBuffer = makeBuffer();
		
		System.out.println("Have buffers");
		
		for (int i = 0; i < numTris * 3; i++) {
			modelPositions.get(i).sendData(pBuffer);
			modelColors.get(i).sendData(cBuffer);
		}
		pBuffer.rewind();
		cBuffer.rewind();
		System.out.println("Piped " + numTris * 3 + " verts");
		
		// Set position as 'Dynamic Draw' since we won't update it
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, pHandle);
		Util.error("after bind pHandle");
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, pBuffer, GL15.GL_STATIC_DRAW);
		Util.error("after set position static draw");
		
		// Set color  as 'Dynamic Draw' since we won't update it
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, cHandle);
		Util.error("after bind cHandle");
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, cBuffer, GL15.GL_STATIC_DRAW);
		Util.error("after set color static draw");
		
		// Create VAO on graphics card
		vao = GL30.glGenVertexArrays();
		Util.error("after generate single vertex array");
		GL30.glBindVertexArray(vao);
		Util.error("after bind the vao");
		
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
	
	public void update() {
		
	}
	
	public void draw() {
		System.out.println("Thing.draw()");
		// Activate model data
		GL30.glBindVertexArray(vao);
		System.out.println("after bind");
		Util.error("after bind vao");
		
		
		// Draw data
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, numTris * 3);
		Util.error("after draw arrays");
		
	}
}
