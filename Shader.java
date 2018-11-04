/*  
  an instance of this class provides interface to
  a GLSL shader
*/


import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.Scanner;
import java.io.*;

public class Shader
{
  private int handle; 
  private String kind;  // handy user-readable version of kind of shader

  // construct and compile a Shader instance of specified kind 
  // from given String
  // (can only be called when OpenGL context is active)
  public Shader( String shaderType, String shaderCode )
  {
    if( shaderType.equals( "vertex" ) )
      handle = GL20.glCreateShader( GL20.GL_VERTEX_SHADER );
    else if( shaderType.equals( "fragment" ) )
      handle = GL20.glCreateShader( GL20.GL_FRAGMENT_SHADER );
    // for later versions, add other shader options

         Util.error("create shader");
         System.out.println("shader handle is " + handle );

    kind = shaderType;
    
    // using glShaderSource( int, CharSequence ) version of this method
    GL20.glShaderSource( handle, shaderCode );
         Util.error("after attach " + kind + " shader to handle");

    GL20.glCompileShader( handle );
         Util.error("after compile " + kind + " shader");

    String log = GL20.glGetShaderInfoLog( handle, 10000 );
    System.out.println( kind + " shader info log:\n" + log );

  }// construct a shader

  public int getHandle()
  {
    return handle;
  }

  public void delete()
  {
    GL20.glDeleteShader( handle );
         Util.error("after delete " + kind + " shader");
  }

  // read file with given name and produce a single String holding that
  // file (intended to be used for shader programs) with end-of-line
  // inserted carefully
  // Is in this class only as convenience
  public static String readFile( String fileName )
  {
    String result = "";
    try{
      Scanner input = new Scanner( new File( fileName ) );
      while( input.hasNext() )
      {
        String line = input.nextLine();
        result += line + "\n";
      }
      input.close();
    }
    catch(Exception e)
    {
      System.out.println("could not read file named [" + fileName + "]" );
      e.printStackTrace();
      System.exit(1);
    }

    return result;
  }

}
