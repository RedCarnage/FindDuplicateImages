package com.stikasoft.imageutils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Imageutils
 * 
 * Utility class I created to help with some simple image loading and saving.
 *  
 * @author Carl Stika
 *
 */
public class ImageUtils {
	
	
	public static boolean savePngImage(BufferedImage image, String imageName) {
		boolean result = false;
		
		try {
			File imageFile = new File(imageName);
			result = ImageIO.write(image, "png", imageFile);
		} catch (IOException e) {
			System.out.println("Could not save image file " + imageName);
		}
		
		return result;
	}

	public static BufferedImage loadImage(String imageName) {
		BufferedImage result = null;
		
		File imageFile = new File(imageName);
		if(imageFile.exists()) {
			try {
				result = ImageIO.read(imageFile);
			} catch (IOException e) {
				System.out.println("Could not load image file " + imageName);
			}
		}
		else {
			System.out.println("File " + imageName + " does not exist");
		}

		return result;
	}

	public static BufferedImage loadImageFromResource(String imageName) {
		BufferedImage result = null;
		
		URL imageURL = ImageUtils.class.getClassLoader().getResource(imageName);
		if(imageURL!=null) {
			File imageFile = new File(imageURL.getFile());
			if(imageFile.exists()) {
				try {
					result = ImageIO.read(imageFile);
				} catch (IOException e) {
					System.out.println("Could not load image file " + imageName);
				}
			}
			else {
				System.out.println("File " + imageName + " does not exist");
			}
		}
		else {
			System.out.println("Could not find file " + imageName + " in resource");
		}
		
		return result;
	}
	
	public static byte[] loadBinaryFile(String fileName) {
		byte[] result = null;
		
		URL fileURL = ImageUtils.class.getClassLoader().getResource(fileName);
		if(fileURL!=null) {
			Path path;
			try {
				path = Paths.get(fileURL.toURI());
				result = Files.readAllBytes(path);
			} 
			catch (URISyntaxException e) {
				System.out.println("File " + fileName + " error : " + e.getMessage());
			} 
			catch (IOException e) {
				System.out.println("File " + fileName + " error : " + e.getMessage());
			}
		}
		else {
			System.out.println("Could not find file " + fileName + " in resource");
		}
		
		return result;
	}

	public static List<String> loadTextFile(String fileName) {
		List<String> result = new ArrayList<>();
		
		URL fileURL = ImageUtils.class.getClassLoader().getResource(fileName);
		if(fileURL!=null) {
			Path path;
			try {
				path = Paths.get(fileURL.toURI());
				result = Files.readAllLines(path);
			} 
			catch (URISyntaxException e) {
				System.out.println("File " + fileName + " error : " + e.getMessage());
			} 
			catch (IOException e) {
				System.out.println("File " + fileName + " error : " + e.getMessage());
			}
		}
		else {
			System.out.println("Could not find file " + fileName + " in resource");
		}
		
		return result;
	}
}
