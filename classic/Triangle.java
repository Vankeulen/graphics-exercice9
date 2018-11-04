
import java.util.Scanner;
import java.nio.FloatBuffer;

public class Triangle {

	private Vertex a, b, c;

	public Triangle(Scanner input) {
		a = new Vertex(input);
		b = new Vertex(input);
		c = new Vertex(input);
	}

	public void sendData(FloatBuffer posBuffer, FloatBuffer colBuffer) {
		// render three Vertex instances
		a.sendData(posBuffer, colBuffer);
		b.sendData(posBuffer, colBuffer);
		c.sendData(posBuffer, colBuffer);
	}

}
