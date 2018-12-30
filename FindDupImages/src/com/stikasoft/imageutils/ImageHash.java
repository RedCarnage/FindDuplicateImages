package com.stikasoft.imageutils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/***
 * ImageHash
 * 
 * Class to create a hash for the image.
 * This is a simple hash. It is quick and good enough for my application. This hash will handle some scaling and color changes.
 * 
 * @author Carl Stika
 *
 */
public class ImageHash {

	/**
	 * Simple average hash. 
	 * 
	 * 1) Reduce size. Scale Image to 8x8 and make it gray scale.
 	 * 2) Reduce color. The tiny 8x8 picture is converted to a grayscale. This changes the hash from 64 pixels (64 red, 64 green, and 64 blue) to 64 total colors.
	 * 3) Average the colors. Compute the mean value of the 64 colors.
	 * 4) Compute the bits. This is the fun part. Each bit is simply set based on whether the color value is above or below the mean.
	 * 5) Construct the hash. Set the 64 bits into a 64-bit integer. The order does not matter, just as long as you are consistent. (I set the bits from left to right, top to bottom using big-endian.)
	 * @param image
	 * @return
	 */
	public String AverageHash(BufferedImage image) {
		long time = System.currentTimeMillis();
		String hashString = "";
		
		//steps 1 and 2
		BufferedImage scaleImage = scaleImage(image, 8, 8);
		
		float[] pixels = new float[64];
		int index = 0;
		float average = 0;
		for(int j=0;j<8;j++) {
			for(int i=0;i<8;i++) {
				int color = scaleImage.getRGB(i, j);
				//convert to gray scale
				float gray = convertToGray(color);

				pixels[index] = gray;
				average += pixels[index];
				index++;
			}
		}
		average /= 64;
		
		hashString = createHashString(pixels, (int)average);
//		System.out.println("average = " + average);
//		System.out.println("AhashString = " + hashString + " time = " + (System.currentTimeMillis()-time) + " ms");
		
		return hashString;
	}




	/**
	 * phash
	 * 
	 * 1) reduce the size to 32x32
	 * 2) turn to gray scale
	 * 3) create a 32x32 DCT but take the 8x8 top corner
	 * 4) compute average value
	 * 5) turn each pixel into a 0 or 1.
	 * @param image
	 * @return
	 */
	public String PerceptualHash(BufferedImage image) {
		long time = System.currentTimeMillis();
		String hashString = "";
		BufferedImage scaleImage = scaleImage(image, 32, 32);
		
		float[][] imageMatrix = new float[32][32];
		for(int j=0;j<32;j++) {
			for(int i=0;i<32;i++) {
				int color = scaleImage.getRGB(i, j);
				int gray = (int)( (0.2126f*(float)(((color>>16)&255)) + 
								  (0.7512f*(float)((color>>8)&255)) + 
								  (0.0722f*(float)(color&255))));
					
				imageMatrix[j][i] = ((float)gray) - 127.5f; //convert to -127 to 127
				
			}
		}
		
		float[][] dctMatrix = new float[32][32];
		DCT(dctMatrix, imageMatrix);

		

		//---------------------------------------------
		//test inverse the dct to see the new test image
		InverseDCT(imageMatrix, dctMatrix);
		//put it back into the image for display
		for(int j=0;j<32;j++) {
			for(int i=0;i<32;i++) {
				int gray = ((int)(imageMatrix[j][i]+127.5f))&255;
				scaleImage.setRGB(i,  j,  ((255<<24)|(gray<<16)|(gray<<8)|gray));
			}
		}
		
		//---------------------------------------------
		
		
		//take the top 8x8 matrix
		float[] pixels = new float[64];
		int index = 0;
		float average = 0;
		for(int j=0;j<8;j++) {
			for(int i=0;i<8;i++) {
				pixels[index] = dctMatrix[j][i];
				if(i!=0) //do not use the first value for the average 
				{
					average += pixels[index];
				}
				index++;
			}
		}
		average /= 63; //not include the first value
		
		//Covert the pixels to hashstring
		System.out.println("average = " + average);
		hashString = createHashString(pixels, average);
		System.out.println("PhashString = " + hashString + " time = " + (System.currentTimeMillis()-time) + " ms");
		

		return hashString;
	}

	/**
	 * use the difference between two pixels to generate the hash.
	 * 
	 * @param image
	 * @return
	 */
	public String DifferenceHash(BufferedImage image) {
		long time = System.currentTimeMillis();
		String hashString = "";
		
		//steps 1 and 2
		BufferedImage scaleImage = scaleImage(image, 9, 8);
		
		float[] pixels = new float[64];
		int index = 0;
		for(int j=0;j<8;j++) {
			for(int i=0;i<9;i++) {
				int color = scaleImage.getRGB(i, j);
				//convert to gray scale
				float gray = convertToGray(color);
				
				//Do difference of the previous value
				if(i>0) {
					//Subtract from the previous
					pixels[index-1] -= gray;
				}

				//If we are not at the end of a column store the current value
				if(i<8) {
					pixels[index] = gray;
					index++;
				}
				
				
			}
		}

		hashString = createHashString(pixels, 0.0f);
		
		return hashString;
	}

	private void showMatrix(float[][] dctMatrix) {
		for(int j=0;j<dctMatrix.length;j++) {
			for(int i=0;i<dctMatrix[j].length;i++) {
				System.out.print("" + ((int)dctMatrix[j][i]) + ", ");
			}
			System.out.println("");
		}
		
	}

	public void DCT(float[][] DCTMatrix, float[][] Matrix) {

		int N = Matrix[0].length;
		int M = Matrix.length;
		
		float fN = (float)N;
		float fM = (float)M;
		for (int u = 0; u < N; ++u) {
	        for (int v = 0; v < M; ++v) {
	        	DCTMatrix[u][v] = 0;
	        	for (float i = 0; i < N; i++) {
	        		for (float j = 0; j < M; j++) {
	        			DCTMatrix[u][v] += Matrix[(int)i][(int)j] * 
	        							Math.cos((Math.PI/fN)*(i+0.5f)*u) *
	        							Math.cos((Math.PI/fM)*(j+0.5f)*v);
	        		}               
	        	}
	        }  
		}
	}

	/**
	 * Giving the DCT Matrix it will do the inverse DCT
	 * @param Matrix
	 * @param DCTMatrix
	 */
	public void InverseDCT(float[][] Matrix, float[][] DCTMatrix){
		int N = DCTMatrix[0].length;
		int M = DCTMatrix.length;
	    for (int u = 0; u < N; ++u) {
	        for (int v = 0; v < M; ++v) {
//	          Matrix[u][v] = 1.0f/4.0f*DCTMatrix[0][0];
		          Matrix[u][v] = 0.25f*DCTMatrix[0][0];
	          for(int i = 1; i < N; i++){
	        	  Matrix[u][v] += 0.5f*DCTMatrix[i][0];
	          }
	          for(int j = 1; j < M; j++){
	        	  Matrix[u][v] += 0.5f*DCTMatrix[0][j];
	          }

	          for (int i = 1; i < N; i++) {
	        	  for (int j = 1; j < M; j++) {
	        		  Matrix[u][v] += DCTMatrix[i][j] * (float)Math.cos((Math.PI/N)*(u+0.5f)*i)*Math.cos((Math.PI/M)*(v+0.5f)*j);
	              }               
	          }
	          Matrix[u][v] *= 2.0f/((float)N)*2.0f/((float)M);
	        }
	    }  
	 }

	private String createHashString(float[] pixels, float average) {
		String hashString = "";
		
		char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		int buffer = 0;
		int numDigits = 0;
		for(int i=0;i<64;i++) {
			int bit = 0;
			if(pixels[i]<average) {
				pixels[i] = 0;
			}
			else { 
				pixels[i] = 255;
				bit = 1;
			}
			buffer = (buffer<<1)|bit;
			numDigits++;
			if(numDigits==4) {
				hashString += digits[buffer];
				buffer = 0;
				numDigits = 0;
			}
		}
		
		return hashString;

	}
	/**
	 * Hamming distance between two hashes.
	 * 
	 * @param hash1
	 * @param hash2
	 * @return
	 */
	public int distanceBetweenHash(String hash1, String hash2) {
		int diff = 0;
		
		for(int i=0;i<hash1.length();i++) {
			int d1 = hexToInt(hash1.charAt(i));
			int d2 = hexToInt(hash2.charAt(i));
			
			//find difference between hashs
			int combits = d1^d2;
			for(int j=0;j<4;j++) {
				if((combits&1)==1) diff++;
				combits>>=1;
			}
		}
		
		return diff;
	}
	
	private int hexToInt(char c) {
		if(c>='0' || c<='9') return (c-'0');
		if(c>='a' || c<='f') return (c-'a')+10;
		if(c>='A' || c<='F') return (c-'A')+10;
		
		return 0;
	}

	/**
	 * Scale the image.
	 * I could have the scale operation change to gray scale but I would rather do it as a 
	 * Separate step so I can control the grayscale conversion
	 * @param image
	 * @param newWidth
	 * @param newHeight
	 * @return
	 */
	private BufferedImage scaleImage(BufferedImage image, int newWidth, int newHeight) {
		BufferedImage output = new BufferedImage(newWidth,  newHeight,  BufferedImage.TYPE_4BYTE_ABGR);
		
		Graphics2D g2d = (Graphics2D)output.getGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		
		g2d.drawImage(image, 0, 0, newWidth, newHeight, 0, 0, image.getWidth(), image.getHeight(), null);
		g2d.dispose();
		
		return output;
	}
	private float convertToGray(int color) {
/*		float gray = ( (0.2126f*(float)(((color>>16)&255)) + 
				  (0.7512f*(float)((color>>8)&255)) + 
				  (0.0722f*(float)(color&255))));*/
/*				int gray = (int)( (0.299f*(float)(((color>>16)&255)) + 
		  (0.587f*(float)((color>>8)&255)) + 
		  (0.114f*(float)(color&255))));
*/					
		
		float gray = (float)(color&255);
		return gray;
	}
}