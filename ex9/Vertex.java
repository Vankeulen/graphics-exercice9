package ex9;

import java.util.Scanner;
import java.nio.FloatBuffer;

public class Vertex {

	private Triple position;
	private Triple color;
	
	public Vertex(Triple pos, Triple col) {
		position = pos;
		color = col;
	}

	public Vertex(Scanner input) {
		position = new Triple(input);
		color = new Triple(input);
	}

	public void sendData(FloatBuffer posBuffer, FloatBuffer colBuffer) {
		position.sendData(posBuffer);
		color.sendData(colBuffer);
	}

}
