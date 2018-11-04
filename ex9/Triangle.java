package ex9;

import java.util.Scanner;
import java.nio.FloatBuffer;

public class Triangle {

	private Vertex a, b, c;

	public Triangle(Scanner input) {
		a = new Vertex(input);
		b = new Vertex(input);
		c = new Vertex(input);
	}
	
	public Triangle(Vertex a, Vertex b, Vertex c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
	
	public Triangle(Triangle source, Triple translation, double rotation) {
		
		
		
		
	}
	

	public void sendData(FloatBuffer posBuffer, FloatBuffer colBuffer) {
		// render three Vertex instances
		a.sendData(posBuffer, colBuffer);
		b.sendData(posBuffer, colBuffer);
		c.sendData(posBuffer, colBuffer);
	}

}
