package ex9;

/*  
   classical viewing of some
   triangles   
   with moving camera
 */
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

// import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;   // just for the key constants
// import static org.lwjgl.system.MemoryUtil.*;

import java.util.Scanner;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class ClassicalView extends Basic {

	public static void main(String[] args) {
		ClassicalView app = new ClassicalView("kinda fancy car thing", 1000, 500, 60, "src/ex9/carworld.txt");
		app.start();
	}// main

	// instance variables 
	public static final boolean JERRY_CONTROLS = false;
	private Shader v1, f1;
	private int hp1;  // handle for the GLSL program
	private int blendColorLoc;

	private int frustumLoc, lookAtLoc, modelViewLoc;
	private FloatBuffer frustumBuffer, lookAtBuffer, modelViewBuffer;

	// private TriList tris;
	// Need to store at least 256 states... might as well 2x it
	// ints are 32 bits... this type stores massive bitmasks
	private IntFlags keyStates = new IntFlags(512 / 32);
	// Kinda silly, but works.
	private IntFlags mouseButtons = new IntFlags(2);
	private int mouseX, mouseY;
	private int lastMouseX, lastMouseY;
	
	private List<Thing> things;
	private double deltaTime;
	private double lastTime;
	private double time;

	private Camera camera;
	private Camera topCamera;
	

	// construct basic application with given title, pixel width and height
	// of drawing area, and frames per second
	public ClassicalView(String appTitle, int pw, int ph, int fps, String inputFile) {
		super(appTitle, pw, ph, (long) ((1.0 / fps) * 1000000000));
		
		// read position and color data for all the triangles from inputFile:
		// tris = new TriList();
		try {
			System.out.println("Loading input file " + inputFile);
			Scanner input = new Scanner(new File(inputFile));
			int numThings = input.nextInt();
			System.out.println(numThings + " things");
			input.nextLine();
			
			things = new ArrayList<>(numThings);
			for (int i = 0; i < numThings; i++) {
				Thing t = new Thing(input);
				things.add(t);
				System.out.println("Loaded thing " + t.kind + " at " + t.position + " with " + t.size() + " tris.");
			}
			
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// place camera somewhere at start
		double w = .5;
		camera = new Camera(-w, w, -w, w, 0.5, 1300,
				new Triple(0, -0, 5), 90, -30);
		
		w = 1.5;
		topCamera = new Camera(-w, w, -w, w, 0.5, 1300,
				new Triple(0, -0, 50), 0, -89);
		
		modelViewBuffer = Util.createFloatBuffer(16);

	}
	
	protected void init() {
		String vertexShaderCode
				= "#version 330 core\n"
				+ "layout (location = 0 ) in vec3 vertexPosition;\n"
				+ "layout (location = 1 ) in vec3 vertexColor;\n"
				+ "out vec3 color;\n"
				+ "uniform mat4 frustum;\n"
				+ "uniform mat4 lookAt;\n"
				+ "uniform mat4 modelView;\n"
				+ "void main(void)\n"
				+ "{\n"
				+ "  color = vertexColor;\n"
				+ "  gl_Position = frustum * lookAt * modelView * vec4( vertexPosition, 1.0 );\n"
				+ "}\n";

		System.out.println("Vertex shader:\n" + vertexShaderCode + "\n\n");

		v1 = new Shader("vertex", vertexShaderCode);

		String fragmentShaderCode
				= "#version 330 core\n"
				+ "in vec3 color;\n"
				+ "layout (location = 0 ) out vec4 fragColor;\n"
				+ "uniform vec4 blendColor;\n"
				+ "void main(void)\n"
				+ "{\n"
				+ "  fragColor = blendColor * vec4(color, 1.0 );\n"
				+ "}\n";

		System.out.println("Fragment shader:\n" + fragmentShaderCode + "\n\n");

		f1 = new Shader("fragment", fragmentShaderCode);

		hp1 = GL20.glCreateProgram();
		Util.error("after create program");
		System.out.println("program handle is " + hp1);

		GL20.glAttachShader(hp1, v1.getHandle());
		Util.error("after attach vertex shader to program");

		GL20.glAttachShader(hp1, f1.getHandle());
		Util.error("after attach fragment shader to program");

		GL20.glLinkProgram(hp1);
		Util.error("after link program");

		GL20.glUseProgram(hp1);
		Util.error("after use program");

		// get location of uniforms 
		frustumLoc = GL20.glGetUniformLocation(hp1, "frustum");
		lookAtLoc = GL20.glGetUniformLocation(hp1, "lookAt");
		modelViewLoc = GL20.glGetUniformLocation(hp1, "modelView");
		blendColorLoc = GL20.glGetUniformLocation(hp1, "blendColor");
		Mat4.IDENTITY.sendData(modelViewBuffer);
		GL20.glUniformMatrix4fv(modelViewLoc, true, modelViewBuffer);
		GL20.glUniform4f(blendColorLoc, 1,1,1,1);
		
		System.out.println("locations of frustum and lookAt and modelview uniforms are: "
				+ frustumLoc + " " + lookAtLoc + " " + modelViewLoc);

		// once and for all (it's never changing), get
		// buffer for frustum
		frustumBuffer = camera.getFrustumBuffer();
		// Util.showBuffer("frustum from camera: ", frustumBuffer );

		// set background color to something a bit more artsy fartsy
		GL11.glClearColor(.08f, .4f, .55f, 1);

		// turn on depth testing
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		// clearing the depth buffer means setting all spots to this value (1)
		GL11.glClearDepth(2.0f);
		// an incoming fragment overwrites the existing fragment if its depth
		// is less
		GL11.glDepthFunc(GL11.GL_LESS);
		
		
		for (Thing thing : things) {
			thing.sendData();
		}

	}

	protected void nextFrame() {
		// begin next frame.
		keyStates.next();
		mouseButtons.next();
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		
		lastTime = time;
		time = getTime();
		deltaTime = (time - lastTime) / 1000.0;
	}
		
	protected void processInputs() {
		nextFrame();
		
		// process all waiting input events
		while (InputInfo.size() > 0) {
			InputInfo info = InputInfo.get();
				
			if (JERRY_CONTROLS) {
				if (info.kind == 'k' && (info.action == GLFW_PRESS
						|| info.action == GLFW_REPEAT)) {

					// store info values in more convenient variables
					int code = info.code;
					int mods = info.mods;

					final double amount = 1;  // amount to move
					final double angAmount = 5;

					// move the eye point of the camera
					if (code == GLFW_KEY_X && mods == 0) {// x:  move left
						camera.shift(-amount, 0, 0);
					} else if (code == GLFW_KEY_X && mods == 1) {// X:  move right
						camera.shift(amount, 0, 0);
					} else if (code == GLFW_KEY_Y && mods == 0) {// y:  move smaller in y direction
						camera.shift(0, -amount, 0);
					} else if (code == GLFW_KEY_Y && mods == 1) {// Y:  move bigger in y direction
						camera.shift(0, amount, 0);
					} else if (code == GLFW_KEY_Z && mods == 0) {// z:  move smaller in z direction
						camera.shift(0, 0, -amount);
					} else if (code == GLFW_KEY_Z && mods == 1) {// Z:  move bigger in Z direction
						camera.shift(0, 0, amount);
					} // change angles
					else if (code == GLFW_KEY_R && mods == 0) {// r:  rotate clockwise
						camera.rotate(-angAmount);
					} else if (code == GLFW_KEY_R && mods == 1) {// R: rotate counterclockwise
						camera.rotate(angAmount);
					} else if (code == GLFW_KEY_T && mods == 0) {// t:  tilt downward
						camera.tilt(-angAmount);
					} else if (code == GLFW_KEY_T && mods == 1) {// T: tilt upward
						camera.tilt(angAmount);
					} else if (code == GLFW_KEY_G && mods == 0) {// g to go
						camera.go();
					} else if (code == GLFW_KEY_S && mods == 0) {// s to stop
						camera.stop();
					} else {
						// ignore bad keys
					}
				}// input event is a key
				else if (info.kind == 'm') {// mouse moved
					//  System.out.println( info );
				} else if (info.kind == 'b') {// button action
					//  System.out.println( info );
				}
			} else {
				
				if (info.kind == 'k' && info.action == GLFW_PRESS) {
					keyStates.set(info.code, true);
				} else if (info.kind == 'k' && info.action == GLFW_RELEASE) {
					keyStates.set(info.code, false);
				} else if (info.kind == 'm') {
					mouseX = info.mouseX;
					mouseY = info.mouseY;
					
				} else if (info.kind == 'b' && info.action == GLFW_PRESS) {
					mouseButtons.set(info.code, true);
				} else if (info.kind == 'b' && info.action == GLFW_RELEASE) {
					mouseButtons.set(info.code, false);
				}
					
			}
		
			
		}// loop to process all input events
		
		
	}

	protected void update() {
		
		if (keyStates.checkPressed(GLFW_KEY_ESCAPE)) {
			glfwSetWindowShouldClose(window, true);
			return;
		}
		
		niceCameraControls();
						
		camera.move();
		
	}
		

	protected void display() {
		
		// System.out.println( "Step: " + getStepNumber() + " camera: " + camera );

		// System.out.println("camera is now " + camera);

		// send triangle data to GPU
		
		// tris.sendData();
		// send possibly new values of frustum and lookAt to GPU
		// Util.showBuffer("frustum: ", frustumBuffer );
		activate(camera);
		//rainbowBlendColor();
		
		// clear the color and depth buffers
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		// activate vao
		// GL30.glBindVertexArray(tris.getVAO());
		// Util.error("after bind vao");

		// seems that if glViewport is not called, Mac retina display
		// is taken care of, but calling glViewport requires adjusting
		// by doubling number of pixels
		GL11.glViewport(0, 0,
				(int)(Util.retinaDisplay * getPixelWidth() * .5) ,
				Util.retinaDisplay * getPixelHeight());

		// draw the buffers
		for (Thing thing : things) {
			draw(thing);
		}
		
		//GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, tris.size() * 3);
		// Util.error("after draw arrays");
		activate(topCamera);
		GL11.glViewport((int)(Util.retinaDisplay * getPixelWidth() * .5), 0,
				(int)(Util.retinaDisplay * getPixelWidth() * .5),
				Util.retinaDisplay * getPixelHeight());
				
		
		// GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, tris.size() * 3);
			
		for (Thing thing : things) {
			draw(thing);
		}
	}

	private void rainbowBlendColor() {
		double time = getStepNumber() * .1;
		float s1 = .5f + .5f * (float)Math.sin(time + 0.0 * Math.PI / 2.0);
		float s2 = .5f + .5f * (float)Math.sin(time + 1.0 * Math.PI / 2.0);
		float s3 = .5f + .5f * (float)Math.sin(time + 2.0 * Math.PI / 2.0);
		GL20.glUniform4f(blendColorLoc, s1, s2, s3, 1);
	}
	
	private void activate(Camera camera) {
		
		frustumBuffer = camera.getFrustumBuffer();
		GL20.glUniformMatrix4fv(frustumLoc, true, frustumBuffer);
		lookAtBuffer = camera.getLookAtBuffer();
		GL20.glUniformMatrix4fv(lookAtLoc, true, lookAtBuffer);
		
	}

	private void draw(Thing thing) {
		Mat4 trs = thing.getTRS();
		Triple color = thing.getColor();
		thing.sendData();
		trs.sendData(modelViewBuffer);
		GL20.glUniformMatrix4fv(modelViewLoc, true, modelViewBuffer);
		GL20.glUniform4f(blendColorLoc, (float)color.x, (float)color.y, (float)color.z, 1f);
		thing.draw();
	}
	
	private void niceCameraControls() {
		
		final double angAmount = 5;
		if (mouseButtons.check(GLFW_MOUSE_BUTTON_2)) {
			if (mouseX != lastMouseX) {
				camera.rotate(lastMouseX - mouseX);
			}
			if (mouseY != lastMouseY) {
				camera.tilt(lastMouseY - mouseY);
			}
		}
		
		
		int dx=0,dy=0,dz=0;
		if (keyStates.check(GLFW_KEY_W)) { dy += 1; } 
		if (keyStates.check(GLFW_KEY_S)) { dy -= 1; } 
		if (keyStates.check(GLFW_KEY_A)) { dx -= 1; } 
		if (keyStates.check(GLFW_KEY_D)) { dx += 1; } 
		if (keyStates.check(GLFW_KEY_Q)) { dz -= 1; }
		if (keyStates.check(GLFW_KEY_E)) { dz += 1; }
		double speed = 30 * deltaTime;
		camera.moveRelative(new Triple(dx*speed,dy*speed,dz*speed));
		
		if (keyStates.check(GLFW_KEY_J)) {
			camera.rotate(angAmount);
		} 
		if (keyStates.check(GLFW_KEY_L)) {
			camera.rotate(-angAmount);
		}
		if (keyStates.check(GLFW_KEY_I)) {
			camera.tilt(angAmount);
		} 
		if (keyStates.check(GLFW_KEY_K)) {
			camera.tilt(-angAmount);
		}
	}

}// ClassicalView
