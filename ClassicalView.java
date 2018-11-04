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

public class ClassicalView extends Basic {

   public static void main(String[] args) {
      ClassicalView app = new ClassicalView( "Classic Viewing of Several Triangles", 500, 500, 30, args[0] );
      app.start();
   }// main

   // instance variables 
 
   private Shader v1, f1;
   private int hp1;  // handle for the GLSL program
 
   private int frustumLoc, lookAtLoc;
   private FloatBuffer frustumBuffer, lookAtBuffer;
 
   private TriList tris;
 
   private Camera camera;
 
   // construct basic application with given title, pixel width and height
   // of drawing area, and frames per second
   public ClassicalView( String appTitle, int pw, int ph, int fps, String inputFile )
   {
     super( appTitle, pw, ph, (long) ((1.0/fps)*1000000000) );
 
     // read position and color data for all the triangles from inputFile:
     try {
        Scanner input = new Scanner( new File( inputFile ) );
        tris = new TriList( input );
        input.close();
     }
     catch(Exception e) {
        e.printStackTrace();
        System.exit(1);
     }
 
     // place camera somewhere at start
     camera = new Camera( -.1, .1, -.1, .1, 0.5, 300,
                           new Triple(50,-50,30), 90, -30 ); 
 
   }
 
   protected void init()
   {
     String vertexShaderCode =
 "#version 330 core\n"+
 "layout (location = 0 ) in vec3 vertexPosition;\n"+
 "layout (location = 1 ) in vec3 vertexColor;\n"+
 "out vec3 color;\n"+
 "uniform mat4 frustum;\n"+
 "uniform mat4 lookAt;\n"+
 "void main(void)\n"+
 "{\n"+
 "  color = vertexColor;\n"+
 "  gl_Position = frustum * lookAt * vec4( vertexPosition, 1.0 );\n"+
 "}\n";
 
     System.out.println("Vertex shader:\n" + vertexShaderCode + "\n\n" );
 
     v1 = new Shader( "vertex", vertexShaderCode );
 
     String fragmentShaderCode =
 "#version 330 core\n"+
 "in vec3 color;\n"+
 "layout (location = 0 ) out vec4 fragColor;\n"+
 "void main(void)\n"+
 "{\n"+
 "  fragColor = vec4(color, 1.0 );\n"+
 "}\n";
 
     System.out.println("Fragment shader:\n" + fragmentShaderCode + "\n\n" );
 
     f1 = new Shader( "fragment", fragmentShaderCode );
 
     hp1 = GL20.glCreateProgram();
          Util.error("after create program");
          System.out.println("program handle is " + hp1 );
 
     GL20.glAttachShader( hp1, v1.getHandle() );
          Util.error("after attach vertex shader to program");
 
     GL20.glAttachShader( hp1, f1.getHandle() );
          Util.error("after attach fragment shader to program");
 
     GL20.glLinkProgram( hp1 );
          Util.error("after link program" );
 
     GL20.glUseProgram( hp1 );
          Util.error("after use program");
 
     // get location of uniforms 
     frustumLoc = GL20.glGetUniformLocation( hp1, "frustum" );
     lookAtLoc = GL20.glGetUniformLocation( hp1, "lookAt" );
     System.out.println("locations of frustum and lookAt uniforms are: " + 
                          frustumLoc + " " + lookAtLoc );
 
     // once and for all (it's never changing), get
     // buffer for frustum
     frustumBuffer = camera.getFrustumBuffer();
     // Util.showBuffer("frustum from camera: ", frustumBuffer );
 
     // set background color to white
     GL11.glClearColor( 1.0f, 1.0f, 1.0f, 0.0f );
 
     // turn on depth testing
     GL11.glEnable( GL11.GL_DEPTH_TEST );
     // clearing the depth buffer means setting all spots to this value (1)
     GL11.glClearDepth( 2.0f );
     // an incoming fragment overwrites the existing fragment if its depth
     // is less
     GL11.glDepthFunc( GL11.GL_LESS );
 
   }
 
    protected void processInputs()
    {
       // process all waiting input events
 
       while( InputInfo.size() > 0 ) {
          InputInfo info = InputInfo.get();
 
          if( info.kind == 'k' && (info.action == GLFW_PRESS || 
                                info.action == GLFW_REPEAT) ) {
 
             // store info values in more convenient variables
             int code = info.code;
             int mods = info.mods;
 
             final double amount = 1;  // amount to move
             final double angAmount = 5;
 
             // move the eye point of the camera
        
             if ( code == GLFW_KEY_X && mods == 0 ) {// x:  move left
                camera.shift( -amount, 0, 0 );
             }
             else if ( code == GLFW_KEY_X && mods == 1 ) {// X:  move right
                camera.shift( amount, 0, 0 );
             }
             else if ( code == GLFW_KEY_Y && mods == 0 ) {// y:  move smaller in y direction
                camera.shift( 0, -amount, 0 );
             }
             else if ( code == GLFW_KEY_Y && mods == 1 ) {// Y:  move bigger in y direction
 camera.shift( 0, amount, 0 );
             }
             else if ( code == GLFW_KEY_Z && mods == 0 ) {// z:  move smaller in z direction
                camera.shift( 0, 0, -amount );
             }
             else if ( code == GLFW_KEY_Z && mods == 1 ) {// Z:  move bigger in Z direction
                camera.shift( 0, 0, amount );
             }
  
             // change angles
 
             else if ( code == GLFW_KEY_R && mods == 0 ) {// r:  rotate clockwise
                camera.rotate( -angAmount );
             }
             else if ( code == GLFW_KEY_R && mods == 1 ) {// R: rotate counterclockwise
                camera.rotate( angAmount );
             }
             else if ( code == GLFW_KEY_T && mods == 0 ) {// t:  tilt downward
                camera.tilt( -angAmount );
             }
             else if ( code == GLFW_KEY_T && mods == 1 ) {// T: tilt upward
                camera.tilt( angAmount );
             }
         
             else if ( code == GLFW_KEY_G && mods == 0 ) {// g to go
                camera.go();
             }
             else if ( code == GLFW_KEY_S && mods == 0 ) {// s to stop
                camera.stop();
             }
              
             else {
                // ignore bad keys
             }

          }// input event is a key
 
          else if ( info.kind == 'm' ) {// mouse moved
             //  System.out.println( info );
          }
 
          else if( info.kind == 'b' ) {// button action
             //  System.out.println( info );
          }
 
       }// loop to process all input events
 
    }
 
   protected void update() {

      camera.move();

   }
 
   protected void display() {
 // System.out.println( "Step: " + getStepNumber() + " camera: " + camera );
 
     System.out.println("camera is now " + camera );

     // send triangle data to GPU
     tris.sendData();
 
     // send possibly new values of frustum and lookAt to GPU
     // Util.showBuffer("frustum: ", frustumBuffer );
     GL20.glUniformMatrix4fv( frustumLoc, true, frustumBuffer ); 
     lookAtBuffer = camera.getLookAtBuffer();
     // Util.showBuffer("lookAt: ", lookAtBuffer );
     GL20.glUniformMatrix4fv( lookAtLoc, true, lookAtBuffer ); 
 
     // clear the color and depth buffers
     GL11.glClear( GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );
 
     // activate vao
     GL30.glBindVertexArray( tris.getVAO() );
            Util.error("after bind vao");
 
     // seems that if glViewport is not called, Mac retina display
     // is taken care of, but calling glViewport requires adjusting
     // by doubling number of pixels
 
     GL11.glViewport( 0, 0, 
                      Util.retinaDisplay*getPixelWidth(), 
                      Util.retinaDisplay*getPixelHeight() );
 
     // draw the buffers
     GL11.glDrawArrays( GL11.GL_TRIANGLES, 0, tris.size()*3 );
            Util.error("after draw arrays");
 
   }
 
}// ClassicalView
