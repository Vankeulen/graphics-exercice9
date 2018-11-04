/*  
   useful methods
   and constants
   that don't fit in any specific
   class
*/

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.util.Scanner;
import java.io.*;

public class Util
{
  // change this to 1 if not a retinaDisplay 
  // and 2 if is a a retinaDisplay
  // (or probably other fancy monitors?)
  public final static int retinaDisplay = 1;

  // this is the one big, frequently reused app
  // buffer, direct allocated outside usual heap, apparently
  public static FloatBuffer appDataBuffer;
  private static int bufferSize = 100000000; // as big as possible

  public static void init(){
    appDataBuffer = createFloatBuffer( bufferSize );
    System.out.println("Util.init done");
  }

  // store given value a as a float in appDataBuffer
  public static void bufferPut( double a ){
    appDataBuffer.put( (float) a );
  }

  // clear appDataBuffer for next use
  public static void bufferClear(){
    appDataBuffer.clear();
  }

  // rewind appDataBuffer for next use
  public static void bufferRewind(){
    appDataBuffer.rewind();
  }

  // set limit to position and 
  // then set position to 0,
  // ready to be read
  public static void bufferFlip(){
    appDataBuffer.flip();
  }

  // connect appDataBuffer to currently active
  // GPU buffer
  public static void sendBufferToGPU( int style ){
    // System.out.println("send " + appDataBuffer.limit() + " to GPU");
    GL15.glBufferData( GL15.GL_ARRAY_BUFFER, appDataBuffer, style );
       error("after send appDataBuffer to GPU buffer");

    bufferClear();  // clear the appDataBuffer
  }

  public static void error( String message ){
    int error = glGetError();
    if( error != 0 )
    {
      System.out.println("Got OpenGL error " + error + " at " + message );
    }
  }

  // create an empty byte buffer with given space
  public static ByteBuffer createByteBuffer( int num )
  {
    // make byte buffer big enough to hold the entire bytes
    ByteBuffer bb = ByteBuffer.allocateDirect( num );
    // make sure that the order of the bytes in a single float is correct
    bb.order(ByteOrder.nativeOrder());
    return bb;
  }

  // create an empty int buffer with given space
  public static IntBuffer createIntBuffer( int num )
  {
    // make byte buffer big enough to hold the entire bytes
    ByteBuffer bb = ByteBuffer.allocateDirect( num*4 );
    // make sure that the order of the bytes in a single float is correct
    bb.order(ByteOrder.nativeOrder());
    // create int buffer from these bytes
    IntBuffer ib = bb.asIntBuffer();
    // System.out.println("new buffer has capacity of " + ib.capacity() + " limit of " + ib.limit() );
    return ib;
  }

  // create an empty float buffer with space for
  // num float's
  public static FloatBuffer createFloatBuffer( int num )
  {
    // make byte buffer big enough to hold the entire bytes
    ByteBuffer bb = ByteBuffer.allocateDirect( num*4 );
    // make sure that the order of the bytes in a single float is correct
    bb.order(ByteOrder.nativeOrder());
    // create float buffer from these bytes
    FloatBuffer fb = bb.asFloatBuffer();
    return fb;
  }

  public static void showBuffer( String message, ByteBuffer ib )
  {
    System.out.println( message );
    ib.rewind();
    while( ib.hasRemaining() )
    {
      byte x = ib.get();
      System.out.println( "next item from buffer: " + x );
    }
    ib.rewind();
  }

  public static void showBuffer( String message, IntBuffer ib )
  {
    System.out.println( message );
    ib.rewind();
    while( ib.hasRemaining() )
    {
      int x = ib.get();
      System.out.println( "next item from buffer: " + x );
    }    
    ib.rewind();
  }

  public static void showBuffer( String message, FloatBuffer ib )
  {
    System.out.println( message );
    ib.rewind();
    while( ib.hasRemaining() )
    {
      float x = ib.get();
      System.out.println( "next item from buffer: " + x );
    }
    ib.rewind();
  }

  // create a float buffer holding contents of array of doubles
  // (philosophy is to work with doubles in CPU, only convert to
  //  floats when sending to GPU)
  public static FloatBuffer arrayToBuffer( double[] array )
  {
    // make byte buffer big enough to hold the entire array bytes
    ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
    // make sure that the order of the bytes in a single float is correct
    bb.order(ByteOrder.nativeOrder());
    // create float buffer from these bytes
    FloatBuffer fb = bb.asFloatBuffer();
    // put the bytes of array into fb
    for( int k=0; k<array.length; k++ )
      fb.put( (float) array[k] );
    fb.rewind();

    return fb;
  }

  // create a float buffer holding contents of array of floats
  public static FloatBuffer arrayToBuffer( float[] array )
  {
    // make byte buffer big enough to hold the entire array bytes
    ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
    // make sure that the order of the bytes in a single float is correct
    bb.order(ByteOrder.nativeOrder());
    // create float buffer from these bytes
    FloatBuffer fb = bb.asFloatBuffer();
    // put the bytes of array into fb
    fb.put( array );
    fb.rewind();

    return fb;
  }

  // create an int buffer holding contents of array of ints
  public static IntBuffer arrayToBuffer( int[] array )
  {
    // make byte buffer big enough to hold the entire array bytes
    ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
    // make sure that the order of the bytes in a single float is correct
    bb.order(ByteOrder.nativeOrder());
    // create int buffer from these bytes
    IntBuffer ib = bb.asIntBuffer();
    // put the bytes of array into ib
    ib.put( array );
    ib.rewind();

    return ib;
  }

  // create a byte buffer holding contents of array of byte
  public static ByteBuffer arrayToBuffer( byte[] array )
  {
    // make byte buffer big enough to hold the entire array bytes
    ByteBuffer bb = ByteBuffer.allocateDirect( array.length );
    // make sure that the order of the bytes in a single float is correct
    bb.order(ByteOrder.nativeOrder());

    return bb;
  }

  // create a float buffer holding 4 given floats
  public static FloatBuffer makeBuffer4( float r, float g, float b, float a )
  {
    float[] array = new float[4];
    array[0]=r; array[1]=g; array[2]=b; array[3]=a;
    return arrayToBuffer( array );
  }

  // do the OpenGL calls to set up the given data as an
  // array buffer  VBO
  // with given attribute number and number of floats per item
  // and return its handle
  public static int setupVBO( FloatBuffer data, 
                               int attribIndex, int number )
  {
    int handle = GL15.glGenBuffers();
       Util.error("after generate buffer handle");
    GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER, handle );
             Util.error("after bind handle");
    GL15.glBufferData( GL15.GL_ARRAY_BUFFER,
                                     data, GL15.GL_STATIC_DRAW );
             Util.error("after set data");
    GL20.glEnableVertexAttribArray( attribIndex );
             Util.error("after enable attrib " + attribIndex );
    GL20.glVertexAttribPointer( attribIndex, number, 
                                 GL11.GL_FLOAT, false, 0, 0 );
             Util.error("after do attrib pointer");

    return handle;

  }

  // do the OpenGL calls to set up the given data as an
  // array buffer  VBO
  // with given attribute number and number of ints per item
  // and return its handle
  public static int setupVBO( IntBuffer data,
                               int attribIndex, int number )
  {
    int handle = GL15.glGenBuffers();
       Util.error("after generate buffer handle");
    GL15.glBindBuffer( GL15.GL_ARRAY_BUFFER, handle );
             Util.error("after bind handle");
    GL15.glBufferData( GL15.GL_ARRAY_BUFFER,
                                     data, GL15.GL_STATIC_DRAW );
             Util.error("after set data");
    GL20.glEnableVertexAttribArray( attribIndex );
             Util.error("after enable attrib " + attribIndex );
    GL20.glVertexAttribPointer( attribIndex, number,
                                 GL11.GL_INT, false, 0, 0 );
             Util.error("after do attrib pointer");

    return handle;

  }

  // represent x using given total width and number of  decimals
  public static String nice( double x, int width, int decimals )
  {
    return String.format("%" + width + "." + decimals + "f", x );
  }

}
