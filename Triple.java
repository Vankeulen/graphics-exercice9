/*
  this class is immutable
  (as long as you don't directly change
   x, y, z)
*/

import java.util.Scanner;
import java.nio.FloatBuffer;

public class Triple {

   public final static Triple xAxis = new Triple(1,0,0);
   public final static Triple yAxis = new Triple(0,1,0);
   public final static Triple zAxis = new Triple(0,0,1);

   // public, but absolutely do not change them directly!
   public double x, y, z;

   public Triple( double xIn, double yIn, double zIn ) {
      x = xIn;
      y = yIn;
      z = zIn;
   }

   public Triple( Scanner input ) {
      x = input.nextDouble();
      y = input.nextDouble();
      z = input.nextDouble();
      input.nextLine();
   }

   // compute the vector from this Triple to v
   public Triple vectorTo( Triple v ) {
      return new Triple( v.x - x, v.y - y, v.z - z );
   }

   // compute this vector minus v
   public Triple minus( Triple v ) {
      return new Triple( x - v.x, y - v.y, z - v.z );
   }

   // add the other Triple to this Triple
   public Triple add( Triple v ) {
      return new Triple( x + v.x, y + v.y, z + v.z );
   }

   // multiply this Triple by scalar
   public Triple mult( double s ) {
      return new Triple( s*x, s*y, s*z );
   }

   // compute the dot product of this Triple and v
   public double dot( Triple v  ) {
      return x*v.x + y*v.y + z*v.z;
   }

   // compute the norm of this Triple
   public double norm() {
      return Math.sqrt( x*x + y*y + z*z );
   }

   // return the normalized version of this Triple
   public Triple normalize() {
      double temp = 1 / norm();    
      return new Triple( temp*x, temp*y, temp*z );
   }

   // return the cross product of this Triple and v
   public Triple cross( Triple v ) {
      return new Triple( y*v.z - v.y*z, v.x*z - x*v.z, x*v.y - v.x*y );
   }

   // send this Triple's data to a buffer
   public void sendData( FloatBuffer buffer ) {
      buffer.put( (float) x );
      buffer.put( (float) y );
      buffer.put( (float) z );
   }

   public String toString() {
      return "[" + x + " " + y + " " + z + "]";
   }

   public static void main( String[] args ) {
      Triple v = new Triple( 1, 2, 3 );
      Triple w = new Triple( -4, 5, 7 );
      Triple u = v.cross(w);
      System.out.println( v.dot(u) + " " + w.dot(u) );
   }

}
