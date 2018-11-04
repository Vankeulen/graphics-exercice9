package ex9;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Thing {
	
	public String kind;
	public List<Triangle> model;
	
	
	public Triple position;
	public Triple rotation;
	
	public double speed;
	public double rotSpeed;
	
	public Thing() {
		init();
		model = new ArrayList<>();
	}

	public Thing(Scanner sc) {
		init();
		
		try {
			kind = sc.nextLine();
			position = new Triple(sc);
			rotation = new Triple(sc);
			
			speed = sc.nextDouble();
			rotSpeed = sc.nextDouble();
			sc.nextLine();
			
			int numTris = sc.nextInt();
			sc.nextLine();
			model = new ArrayList<>(numTris);
			
			
			for (int i = 0; i < numTris; i++) {
				model.add(new Triangle(sc));
			}
			
		} catch (Exception e) {
			System.out.println("Error loading thing kind = " + kind + " ex = " + e);
		}
		
	}
	
	private void init() {
		kind = "Basic Thing";
		
		
		position = new Triple(0,0,0);
		rotation = new Triple(0,0,0);
		speed = 0;
		rotSpeed = 0;
	}
	
	
}
