/*  
  draw many triangles from a data file

  Error log:

    The weird jittery display happened because
    we (okay, I) hadn't put in the code to clear
    the frame buffer at the start of each display() call

    Discovered by painful experimentation that calling
    glViewport creates a problem with Mac retina display
    (like my laptop has), need to double the number of
    pixels for a retina display (change Util.retinaDisplay
    between 1 and 2)

    In glDrawArrays I foolishly put the number triangles in 
    the last parameter spot instead of the number of vertices

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

public class ManyTris extends Basic {

  public static void main(String[] args) {
    ManyTris app = new ManyTris( "Many Triangles", 500, 500, 30, args[0] );
    app.start();
  }// main

  // instance variables 

  private Shader v1, f1;
  private int hp1;  // handle for the GLSL program

  private int vao;  // handle to the vertex array object

  private int numTris;
  private float[] positionData;
  private float[] colorData;

  // construct basic application with given title, pixel width and height
  // of drawing area, and frames per second
  public ManyTris( String appTitle, int pw, int ph, int fps, String inputFile )
  {
    super( appTitle, pw, ph, (long) ((1.0/fps)*1000000000) );

    // read position and color data for all the triangles from inputFile:
    try {
       Scanner input = new Scanner( new File( inputFile ) );
        
       numTris = input.nextInt();
       positionData = new float[ 3*3*numTris ];
       colorData = new float[ 3*3*numTris ];

       // read position data
       for (int k=0; k<positionData.length; k++) {
          positionData[k] = input.nextFloat();
       }

       // read color data
       for (int k=0; k<colorData.length; k++) {
          colorData[k] = input.nextFloat();
       }

       input.close();
    }
    catch(Exception e) {
       e.printStackTrace();
       System.exit(1);
    }

  }

  protected void init()
  {
    String vertexShaderCode =
"#version 330 core\n"+
"layout (location = 0 ) in vec3 vertexPosition;\n"+
"layout (location = 1 ) in vec3 vertexColor;\n"+
"out vec3 color;\n"+
"void main(void)\n"+
"{\n"+
"  color = vertexColor;\n"+
"  gl_Position = vec4( vertexPosition, 1.0);\n"+
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

    // set background color to white
    GL11.glClearColor( 1.0f, 1.0f, 1.0f, 0.0f );

    // create vertex buffer objects and their handles one at a time
    int positionHandle = GL15.glGenBuffers();
    int colorHandle = GL15.glGenBuffers();
    System.out.println("have position handle " + positionHandle +
                       " and color handle " + colorHandle );

    // connect data to the VBO's
        // first turn the arrays into buffers
        FloatBuffer positionBuffer = Util.arrayToBuffer( positionData );
        FloatBuffer colorBuffer = Util.arrayToBuffer( colorData );

Util.showBuffer("position buffer: ", positionBuffer );  positionBuffer.rewind();
Util.showBuffer("color buffer: ", colorBuffer );  colorBuffer.rewind();

       // now connect the buffers
       GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER, positionHandle );
             Util.error("after bind positionHandle");
       GL15.glBufferData( GL15.GL_ARRAY_BUFFER, 
                                     positionBuffer, GL15.GL_STATIC_DRAW );
             Util.error("after set position data");
       GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER, colorHandle );
             Util.error("after bind colorHandle");
       GL15.glBufferData( GL15.GL_ARRAY_BUFFER, 
                                     colorBuffer, GL15.GL_STATIC_DRAW );
             Util.error("after set color data");

    // set up vertex array object

      // using convenience form that produces one vertex array handle
      vao = GL30.glGenVertexArrays();
           Util.error("after generate single vertex array");
      GL30.glBindVertexArray( vao );
           Util.error("after bind the vao");
      System.out.println("vao is " + vao );

      // enable the vertex array attributes
      GL20.glEnableVertexAttribArray(0);  // position
             Util.error("after enable attrib 0");
      GL20.glEnableVertexAttribArray(1);  // color
             Util.error("after enable attrib 1");
  
      // map index 0 to the position buffer, index 1 to the color buffer
      GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER, positionHandle );
             Util.error("after bind position buffer");
      GL20.glVertexAttribPointer( 0, 3, GL11.GL_FLOAT, false, 0, 0 );
             Util.error("after do position vertex attrib pointer");

      // map index 1 to the color buffer
      GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER, colorHandle );
             Util.error("after bind color buffer");
      GL20.glVertexAttribPointer( 1, 3, GL11.GL_FLOAT, false, 0, 0 );
             Util.error("after do color vertex attrib pointer");

    // turn on depth testing
    GL11.glEnable( GL11.GL_DEPTH_TEST );
    // clearing the depth buffer means setting all spots to this value (-1)
    GL11.glClearDepth( -1.0f );
    // an incoming fragment overwrites the existing fragment if its depth
    // is greater
    GL11.glDepthFunc( GL11.GL_GREATER );

  }

  protected void processInputs()
  {
    // process all waiting input events
    while( InputInfo.size() > 0 )
    {
      InputInfo info = InputInfo.get();

      if( info.kind == 'k' && (info.action == GLFW_PRESS || 
                               info.action == GLFW_REPEAT) )
      {
        int code = info.code;

      }// input event is a key

      else if ( info.kind == 'm' )
      {// mouse moved
      //  System.out.println( info );
      }

      else if( info.kind == 'b' )
      {// button action
       //  System.out.println( info );
      }

    }// loop to process all input events

  }

  protected void update() {
  }

  protected void display() {
System.out.println( getStepNumber() );

    // clear the color and depth buffers
    GL11.glClear( GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT );

    // activate vao
    GL30.glBindVertexArray( vao );
           Util.error("after bind vao");

    // seems that if glViewport is not called, Mac retina display
    // is taken care of, but calling glViewport requires adjusting
    // by doubling number of pixels

    GL11.glViewport( 0, 0, 
                     Util.retinaDisplay*getPixelWidth(), 
                     Util.retinaDisplay*getPixelHeight() );

    // draw the buffers
    GL11.glDrawArrays( GL11.GL_TRIANGLES, 0, numTris*3 );
           Util.error("after draw arrays");

  }

}// ManyTris
