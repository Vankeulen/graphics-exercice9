package ex9;

/*  
   classical viewing of some
   triangles   
   with moving camera
 */
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

// import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;   // just for the key constants
// import static org.lwjgl.system.MemoryUtil.*;

import java.util.Scanner;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClassicalView extends Basic {

	public static void main(String[] args) {
		ClassicalView app = new ClassicalView("kinda fancy car thing", 1000, 500, 60, "src/ex9/carworld.txt");
		app.start();
	}// main

	// toggle to preserve Jerry's controls.
	public static final boolean JERRY_CONTROLS = false;
	
	// instance variables 
	private Shader v1, f1;
	private int hp1;  // handle for the GLSL program
	private int blendColorLoc;

	private int frustumLoc, lookAtLoc, modelLoc;
	private FloatBuffer frustumBuffer, lookAtBuffer, modelBuffer;

	// Need to store at least 256 states... might as well 2x it
	// ints are 32 bits... this type stores massive bitmasks
	private IntFlags keyStates = new IntFlags(512 / 32);
	// Kinda silly, but works.
	private IntFlags mouseButtons = new IntFlags(2);
	private int mouseX, mouseY;
	private int lastMouseX, lastMouseY;
	
	/** Entity list */
	private List<Thing> things;

	/** Time (seconds) between last frame and new frame */
	private double deltaTime;
	/** Last Time (seconds) */
	private double lastTime;
	/** Current Time (seconds) */
	private double time;

	/** offset for camera following player car */
	private Triple cameraPositionOffset = new Triple(0, -15, 3);
	/** offset for camera rotation following player car*/
	private Triple cameraRotationOffset = new Triple(-11, 0, 0);
	private Camera camera;
	private Camera topCamera;
	

	// construct basic application with given title, pixel width and height
	// of drawing area, and frames per second
	public ClassicalView(String appTitle, int pw, int ph, int fps, String inputFile) {
		super(appTitle, pw, ph, (long) ((1.0 / fps) * 1000000000));
		
		// Read "things" from input file.
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
				System.out.println("Loaded thing " + t.kind + " at " + t.position + " with " + t.numTris() + " tris.");
			}
			
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// Primary camera
		double w = .55;
		camera = new Camera(-w, w, -w, w, 0.5, 1300,
				new Triple(0, 0, 5), 90, -30);
		
		w = 1.5;
		// Secondary camera, wider view, from above.
		topCamera = new Camera(-w, w, -w, w, 0.5, 1300,
				new Triple(0, 0, 100), 0, Camera.MIN_PITCH);
		

	}
	
	/** Prepares locations for data on the graphics card, and sets up various data. */
	protected void init() {
		// Shader code, typically loaded from a file.
		// This is the vertex shader, which applies the MVP matrix:
		//		Model View Projection
		String vertexShaderCode
				= "#version 330 core\n"
				+ "layout (location = 0 ) in vec3 vertexPosition;\n"
				+ "layout (location = 1 ) in vec3 vertexColor;\n"
				+ "out vec3 color;\n"
				+ "uniform mat4 frustum;\n"
				+ "uniform mat4 lookAt;\n"
				+ "uniform mat4 model;\n"
				+ "void main(void)\n"
				+ "{\n"
				+ "  color = vertexColor;\n"
				+ "  gl_Position = frustum * lookAt * model * vec4( vertexPosition, 1.0 );\n"
				+ "}\n";
		
		v1 = new Shader("vertex", vertexShaderCode);

		// This is the fragment shader, used to actually render the pixels on the screen
		String fragmentShaderCode
				= "#version 330 core\n"
				+ "in vec3 color;\n"
				+ "layout (location = 0 ) out vec4 fragColor;\n"
				+ "uniform vec4 blendColor;\n"
				+ "void main(void)\n"
				+ "{\n"
				+ "  fragColor = blendColor * vec4(color, 1.0 );\n"
				+ "}\n";

		f1 = new Shader("fragment", fragmentShaderCode);
		
		// Create shader program
		hp1 = GL20.glCreateProgram();
		Util.error("after create program");
		System.out.println("program handle is " + hp1);

		// Attach shader vert/frag shaders together 
		GL20.glAttachShader(hp1, v1.getHandle());
		Util.error("after attach vertex shader to program");
		GL20.glAttachShader(hp1, f1.getHandle());
		Util.error("after attach fragment shader to program");
		GL20.glLinkProgram(hp1);
		Util.error("after link program");

		// Activate shader program, 
		//		we're not using any others, 
		//		so it will stay active for the rest of the program
		GL20.glUseProgram(hp1);
		Util.error("after use program");

		// get location of uniform properties in shader
		frustumLoc = GL20.glGetUniformLocation(hp1, "frustum");
		lookAtLoc = GL20.glGetUniformLocation(hp1, "lookAt");
		modelLoc = GL20.glGetUniformLocation(hp1, "model");
		blendColorLoc = GL20.glGetUniformLocation(hp1, "blendColor");
		
		// The above pointers let us pipe data into the shader program.
		// For example, prepare the model matrix with the identity matrix
		// To do this, we need a buffer...
		modelBuffer = Util.createFloatBuffer(16);
		// ...then put the data into it...
		Mat4.IDENTITY.sendData(modelBuffer);
		// ...and then attach it to the uniform.
		GL20.glUniformMatrix4fv(modelLoc, true, modelBuffer);
		
		// We can also send data directly for uniforms we know the size of
		// for example, we set blendColor to be a vec4, so it can be set 
		//		directly with 4 floats. (1,1,1,1) = White, Opaque
		GL20.glUniform4f(blendColorLoc, 1,1,1,1);
		
		// Print out pointers because why not.
		System.out.println("locations of frustum and lookAt and model uniforms are: "
				+ frustumLoc + " " + lookAtLoc + " " + modelLoc);

		// Set clear color/depth
		//		these are used every frame to reset rendering buffers.
		// Set background color to something more pleasant than pure white.
		GL11.glClearColor(.08f, .4f, .65f, 1);
		// Set the default depth value to use when clearing
		GL11.glClearDepth(2.0f);
		
		// Turn on depth testing
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		// an incoming fragment overwrites the existing fragment if its depth
		//		is less (closer to the camera)
		GL11.glDepthFunc(GL11.GL_LESS);
		
		// Finally, pipe all data from all entities
		// (only needs to be done once, not every frame)
		for (Thing thing : things) {
			thing.sendData();
		}

	}

	/** Handle moving to the next frame of input */
	protected void nextFrame() {
		// begin next frame.
		
		// Copy data for inputs from last frame
		keyStates.next();
		mouseButtons.next();
		// Update lastMouse for delta tracking
		lastMouseX = mouseX;
		lastMouseY = mouseY;
		
		// Update timing information 
		lastTime = time;
		time = getTime() / 1000.0;
		deltaTime = (time - lastTime);
		// Capped so too much time never elapses in one frame
		if (deltaTime > .1) {
			deltaTime = .1;
		}

		Time.time = time;
		Time.deltaTime = deltaTime;
	}
		
	/** Called by Basic every frame before update */
	protected void processInputs() {
		nextFrame();
		
		// process all waiting input events
		while (InputInfo.size() > 0) {
			InputInfo info = InputInfo.get();
			
			// Jerry's controls.
			if (JERRY_CONTROLS) {


				// store info values in more convenient variables
				int code = info.code;

				final double amount = 1;  // amount to move
				final double angAmount = 5;
				Thing player = findThing("PlayerCar");

				if (info.kind == 'k' &&
						(info.action == GLFW_PRESS || info.action == GLFW_REPEAT)) {

					if (player != null) {
						if (code == GLFW_KEY_L) {
							player.rotation = new Triple(
									player.rotation.x, player.rotation.y,
									player.rotation.z + 5
							);
						} else if (code == GLFW_KEY_R) {
							player.rotation = new Triple(
									player.rotation.x, player.rotation.y,
									player.rotation.z - 5
							);
						} else if (code == GLFW_KEY_G) {
							player.speed = 40;
						} else if (code == GLFW_KEY_S) {
							player.speed = 0;
						} else if (code == GLFW_KEY_D) {
							cameraRotationOffset = new Triple(
									clamp(cameraRotationOffset.x - 5, -89.99, 89.99), 0, 0
							);
						} else if (code == GLFW_KEY_U) {
							cameraRotationOffset = new Triple(
									clamp(cameraRotationOffset.x + 5, -89.99, 89.99), 0, 0
							);
						} else {
							// ignore bad keys
						}
					}
				}// input event is a key press
				else if (info.kind == 'm') {// mouse moved
					//  System.out.println( info );
				} else if (info.kind == 'b') {// button action
					//  System.out.println( info );
				}
			} else {
				// Not-jerry controls.
				
				// Update keystates with each key (would be best to do inside
				//		GLFWKeyCallback/GLFWCursorPosCallback/GLFWMouseButtonCallback
				// keyStates/mouseButtons tracks the state of keys/buttons (in bitfields)
				//		for the current and the last frame, so we know if a key is held,
				//		as well as if it was pressed/released when we want to know.
				// mouseY/mouseY hold the current mouse position
				//		we compare with lastMouseX/lastMouseY to check mouse movement.
				if (info.kind == 'k' && info.action == GLFW_PRESS) {
					keyStates.set(info.code, true);
				} else if (info.kind == 'k' && info.action == GLFW_RELEASE) {
					keyStates.set(info.code, false);
				} else if (info.kind == 'b' && info.action == GLFW_PRESS) {
					mouseButtons.set(info.code, true);
				} else if (info.kind == 'b' && info.action == GLFW_RELEASE) {
					mouseButtons.set(info.code, false);
				} else if (info.kind == 'm') {
					mouseX = info.mouseX;
					mouseY = info.mouseY;
				}
					
					
			}
		
			
		}// loop to process all input events
		
		
	}

	/** Called by Basic every frame */
	protected void update() {
		
		// Check if escape was pressed and exit if it was.
		// (I had to make window protected instead of private for this.
		if (keyStates.checkPressed(GLFW_KEY_ESCAPE)) {
			glfwSetWindowShouldClose(window, true);
			return;
		}
		
		for (Thing thing : things) {
			thing.update();
		}
		
		// Call nice camera controls (can disable to control the car)
		Thing player = findThing("PlayerCar");
		if (player != null) {
			controlPlayer(player);

			camera.e = player.position;
			camera.azi = player.rotation.z + cameraRotationOffset.z;
			camera.alt = player.rotation.x + cameraRotationOffset.x;

			camera.moveRelative(cameraPositionOffset);

			// Update camera matricies
			camera.move();

		} else {
			niceCameraControls();
		}



		
	}
		
	/** Called by Basic at the end of every frame to render the image */
	protected void display() {
		// Clear screen (backbuffer)
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		
		
		// Pipes data for View/Projection matrix for camera to shader
		activate(camera);
		
		// Sets up view on left half of screen
		GL11.glViewport(0, 0,
				(int)(Util.retinaDisplay * getPixelWidth() * .5) ,
				Util.retinaDisplay * getPixelHeight());

		// Draws all objects
		for (Thing thing : things) {
			draw(thing);
		}
		
		// Pipes data for View/Projection matrix for other camera to shader
		activate(topCamera);
		
		// Sets up view on right half of screen
		GL11.glViewport((int)(Util.retinaDisplay * getPixelWidth() * .5), 0,
				(int)(Util.retinaDisplay * getPixelWidth() * .5),
				Util.retinaDisplay * getPixelHeight());
				
		// Draws all objects again in the other view
		for (Thing thing : things) {
			draw(thing);
		}
	}

	private void rainbowBlendColor() {
		double t = time * 3.3;
		// Silly method used to test the blendColor shader property.
		float s1 = .5f + .5f * (float)Math.sin(t + 0.0 * Math.PI / 2.0);
		float s2 = .5f + .5f * (float)Math.sin(t + 1.0 * Math.PI / 2.0);
		float s3 = .5f + .5f * (float)Math.sin(t + 2.0 * Math.PI / 2.0);
		GL20.glUniform4f(blendColorLoc, s1, s2, s3, 1);
	}
	
	/** Activates the camera by piping the camera's view and projection matrix 
	 into the relevant buffers and shader properties */
	private void activate(Camera camera) {
		
		// Get the buffers for the camera...
		frustumBuffer = camera.getFrustumBuffer();
		lookAtBuffer = camera.getLookAtBuffer();
		
		// ...then attach them to the shader properites
		GL20.glUniformMatrix4fv(frustumLoc, true, frustumBuffer);
		GL20.glUniformMatrix4fv(lookAtLoc, true, lookAtBuffer);
		
	}
	
	/** Draws the Thing */
	private void draw(Thing thing) {
		// Get Tranlate Rotate Scale (modelView property) matrix
		Mat4 trs = thing.getTRS();
		// Get blendColor property
		Triple color = thing.getColor();
		
		// Update the modelView property to use the thing's modelView
		trs.sendData(modelBuffer);
		GL20.glUniformMatrix4fv(modelLoc, true, modelBuffer);
		// Update the blendColor property
		GL20.glUniform4f(blendColorLoc, (float)color.x, (float)color.y, (float)color.z, 1f);
		
		// rainbowBlendColor();
		
		thing.draw();
	}

	private void controlPlayer(Thing player) {

		if (!JERRY_CONTROLS) {
			if (keyStates.check(GLFW_KEY_A)) {
				player.rotSpeed = 120;
			} else if (keyStates.check(GLFW_KEY_D)) {
				player.rotSpeed = -120;
			} else {
				player.rotSpeed = 0;
			}

			if (keyStates.check(GLFW_KEY_W)) {
				player.speed = 65;
			} else if (keyStates.check(GLFW_KEY_S)) {
				player.speed = -65;
			} else {
				player.speed = 0;
			}
		}

	}


	private int frames_held = 0;
	/** Nicer, more intuitive camera controls */
	private void niceCameraControls() {

		// Mousemove while rightclick held
		if (mouseButtons.check(GLFW_MOUSE_BUTTON_2)) {
			if (mouseX != lastMouseX) {
				camera.rotate(lastMouseX - mouseX);
			}
			if (mouseY != lastMouseY) {
				camera.tilt(lastMouseY - mouseY);
			}
		}

		// Map W/S to forward/back, A/D to left/right, Q/E to down/up
		// Movement applied every frame keys are held...
		int dx=0,dy=0,dz=0;
		if (keyStates.check(GLFW_KEY_W)) { dy += 1; }
		if (keyStates.check(GLFW_KEY_S)) { dy -= 1; }
		if (keyStates.check(GLFW_KEY_A)) { dx -= 1; }
		if (keyStates.check(GLFW_KEY_D)) { dx += 1; }
		if (keyStates.check(GLFW_KEY_Q)) { dz -= 1; }
		if (keyStates.check(GLFW_KEY_E)) { dz += 1; }

		// Move at 30 units/second
		double speed = 30 * deltaTime;
		if (keyStates.check(GLFW_KEY_LEFT_SHIFT)) {
			speed += frames_held / 30;
			frames_held++;
		} else {
			frames_held = 0;
		}
		// And apply movement relative to camera facing
		camera.moveRelative(new Triple(dx*speed,dy*speed,dz*speed));

		dx = dy = 0;
		// Rotation applied every frame keys are held...
		if (keyStates.check(GLFW_KEY_J)) { dx += 1; }
		if (keyStates.check(GLFW_KEY_L)) { dx -= 1; }
		if (keyStates.check(GLFW_KEY_I)) { dy += 1; }
		if (keyStates.check(GLFW_KEY_K)) { dy -= 1; }
		// Rotate at 65 degrees/second
		double angAmount = 65 * deltaTime;
		camera.rotate(dx * angAmount);
		camera.tilt(dy * angAmount);

		// Update the camera's matrixes based on movement applied.
		camera.move();
	}

	private Thing findThing(String name) {
		for (Thing t : things) {
			if (t.kind.equals(name)) {
				return t;
			}
		}
		return null;
	}

	private static double clamp(double val, double min, double max) {
		if (val < min) { return min; }
		if (val > max) { return max; }
		return val;
	}



}// ClassicalView
