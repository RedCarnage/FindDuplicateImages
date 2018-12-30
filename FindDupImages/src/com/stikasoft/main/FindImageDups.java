package com.stikasoft.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;

import com.stikasoft.imageutils.ImageHash;
import com.stikasoft.imageutils.ImageUtils;

/***
 * FindImageDups
 *  
 * @author Carl Stika
 *
 *
 * Find duplicate Images in the current directory or sub directories.
 * It will output a list of duplicate names. use the -m option to move the duplicates to a directory.
 * 
 * 
 */
public class FindImageDups {

	/***
	 * ImageHashInfo
	 * 
	 * Class to hold the filename and hash of the image.
	 * 
	 *
	 */
	class ImageHashInfo {
		public String filename; //full path ogf image
		public String imageHash;
		
		public ImageHashInfo(String filename, String imageHash) {
			super();
			this.filename = filename;
			this.imageHash = imageHash;
		}

		public List<ImageHashInfo> possibleDups = new ArrayList<>();
	}
	
	private boolean recursive = false;
	private String directoryToSearch = "";
	private String[] imageList;
	private List<ImageHashInfo> listImageHashInfo = new ArrayList<>(); 
	private ImageHash imageHash = new ImageHash();
	private int imageDistance = 5;
	private boolean abortProgram = false;
	private String directoryToMoveFilesTo = "";
	
	
	public static void main(String[] args) {
		FindImageDups program = new FindImageDups();
		
		program.start(args);
		
	}

	/**
	 * start
	 * 
	 * main code of the app.
	 * 
	 * @param args
	 */
	private void start(String[] args) {
		if(args.length>0) {
			if(processArgs(args)) {
				if(!abortProgram) {
					findDupsImages();
					printDups();
				}
			}
			else {
				showUsage();
			}
		}
		else {
			showUsage();
		}
	}

	/**
	 * print a list of duplicate files
	 */
	private void printDups() {
		for(ImageHashInfo hashInfo : listImageHashInfo) {
			if(hashInfo.possibleDups.size()>0) {
				System.out.println(hashInfo.filename);
				for(ImageHashInfo dupHashInfo : hashInfo.possibleDups) {
					System.out.println("\t> " + dupHashInfo .filename);
				}
			}
		}
		
	}

	/***
	 * showUsage
	 * 
	 */
	public void showUsage() {
		
		System.out.println("Duplicate Image finder");
		System.out.println("Usage : DupImageFinder [-rh] [-A=0|1|2|3] [-M=directory] [--help] <dir to search>");
		System.out.println("Options : ");

		System.out.println("\nArgument : directory to search for duplicat files");
		System.out.println("\t-h, show help.");
		System.out.println("\t-r, do recursive find.");
		System.out.println("\t-A=acc,\tImage Accuracy. 0 - is most accurate,  3 is least. (Default is 1)");
		System.out.println("\t-M=directory,\tDirectory to move dups to.");
		
		System.out.println("\nInfo : ");
		System.out.println("\tProgram searches the directory to find Perceptial same images");
		System.out.println("\tYou can specify the accuracy to find the images.\n\tThe images can be slightly change and it can still find a close candidate.");
		System.out.println("\n\tIf the -M option is used then the duplicate images will be moved to the directory specified.");
		
		imageExtsSupported();
	}

	/**
	 * processArgs
	 * 
	 * @param args  -  Arguments from the command line
	 * @return - True if parameters are correct. False if there is a problem. 
	 */
	private boolean processArgs(String[] args) {
		boolean processingSuccessful = true;		

		//search for options
		for(String word : args) {
			if(word.indexOf("--")==0) {
				processingSuccessful = processLongOption(word.substring(2));
			}
			if(word.charAt(0)=='-') {
				processingSuccessful = processShortOption(word.substring(1));
			}
			else {
				//assume argument
				if(directoryToSearch.length()==0) {
					directoryToSearch = word;
				}
				else {
					processingSuccessful = false;
				}
			}
			
			if(processingSuccessful==false | abortProgram) break;
		}
		
		return processingSuccessful;
	}


	private boolean processShortOption(String param) {
		boolean result = true;
		
		int equalsSign = param.indexOf('=');
		if(equalsSign>-1) {
			//Short option with a argument
			String shortOption = param.substring(0, equalsSign).trim();
			String argument = param.substring(equalsSign+1).trim();
			
			switch(shortOption.charAt(0)) {
				case 'A':
					{
						try {
							int accuracy = Integer.parseInt(argument);
							
							if(accuracy>=0 && accuracy<=3) {
								imageDistance = 2 + accuracy*3; 
							}
						}
						catch(NumberFormatException ex) {
							System.out.printf("%s is not a number.\n", argument);
						}
						
					}
					break;
				case 'M':
					if(!isValidDirectory(argument)) {
						System.out.printf("\nError : directory '%s' does not exist\n", argument);
						result = false;
					}
					break;
				default:
					result = false; //bad option
					break;
			}
			
			
		}
		else {
			//short option without a argument. Can be multiple options together.
			for(char l : param.toCharArray()) {
				switch(l) {
				case 'r':
					recursive = true;
					break;
				default:
					result = false;
					break;
				}
				
				if(result==false) break; //stop processing
			}
		}
		return result;
	}

	/**
	 * Checks if directory is valid.
	 * @param argument
	 * @return
	 */
	private boolean isValidDirectory(String argument) {
		File directory = new File(argument);
		
		if(directory.exists() && directory.isDirectory()) {
			directoryToMoveFilesTo = directory.getAbsolutePath();

			return true;
		}
		
		return false;
	}

	/**
	 * 
	 * @param option
	 * @return
	 */
	private boolean processLongOption(String option) {
		boolean result = false;
		
		if(option.equalsIgnoreCase("help")) {
			showUsage();
			result = true;
			abortProgram = true;
		}
		return result;
	}


	/**
	 * crawlImageDirectory
	 * 
	 * Crawls the image directory and class the fileFunc for each image file found.
	 * 
	 * @param fileFunc
	 * @return
	 */
	private int crawlImageDirectory(Consumer<File> fileFunc) {
		File startDir = new File(directoryToSearch);

		MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap();
		List<File> dirToSearch = new ArrayList<>();
		dirToSearch.add(startDir);
		int numImages =0;
		
		while(dirToSearch.size()>0) {
			File dir = dirToSearch.get(0);
			dirToSearch.remove(0);
			
			File[] files = dir.listFiles();
			
			for(File file : files) {
				if(file.isDirectory()) {
					if(recursive) dirToSearch.add(file);  
				}
				else {
					String mimetype = mimeTypes.getContentType(file);
					if(mimetype.contains("image/")) {
						fileFunc.accept(file);
					}
				}
			}
		}

		return numImages;
	}

	private int totalImages = 0;
	private int currentImageNum = 0;
	
	/**
	 * findDupsImages
	 * 
	 * Find all duplicate images
	 */
	private void findDupsImages() {
		File startDir = new File(directoryToSearch);

		//Make sure the directory exists.
		if(startDir.exists() && startDir.isDirectory()) {
			System.out.printf("Searching for dups in directory %s\nUsing distance %d\n",  directoryToSearch, imageDistance);
			if(!directoryToMoveFilesTo.isEmpty()) {
				System.out.printf("Copying duplicate images to %s\n", directoryToMoveFilesTo); 
			}
		}
		else {
			System.out.printf("%s is does not exists or is not a directory\n", directoryToSearch);
			return;
		}
		
		//Count the number of images in the ddirectorys
		crawlImageDirectory(file->totalImages++);
		System.out.println("totalImages To check = " + totalImages);

		if(totalImages>0) {
			crawlImageDirectory(file->addPictureToList(file));
		}
		
	}
	
	/**
	 * addPictureToList
	 * 
	 * loads the picture and creates the hash for the image.
	 *  
	 * @param file
	 */
	private void addPictureToList(File file) {
    	BufferedImage displayImage = ImageUtils.loadImage(file.getAbsolutePath());
    	if(displayImage!=null) {
    		String imageDHashString = imageHash.DifferenceHash(displayImage);
    		currentImageNum++;
    		printProgressBar(currentImageNum, totalImages);
    		addHashToList(file.getAbsolutePath(), imageDHashString);
    	}
	}

	/**
	 * 
	 * Adds the hash to the hash list. If it is close
	 * @param fileFullPath
	 * @param imageHashString
	 */
	private void addHashToList(String fileFullPath, String imageHashString) {
		boolean foundDup = false;

		//see if the hash is already there.
		for(ImageHashInfo imageInfo : listImageHashInfo) {
			if(imageHash.distanceBetweenHash(imageInfo.imageHash, imageHashString)<imageDistance) {
				foundDup = true;
				imageInfo.possibleDups.add(new ImageHashInfo(fileFullPath, imageHashString));
				
				moveDuplicateImage(fileFullPath);
			}
		}
		
		if(!foundDup) {
			listImageHashInfo.add(new ImageHashInfo(fileFullPath, imageHashString));
		}
		
	}

	/***
	 * moveDuplicateImage
	 * 
	 * Moves the image file to the directory. It will lose the full path so it might collide with a already moved file.
	 * 
	 * @param fileFullPath
	 */
	private void moveDuplicateImage(String fileFullPath) {
		if(!directoryToMoveFilesTo.isEmpty()) {
			try {
				Files.move(Paths.get(fileFullPath), Paths.get(directoryToMoveFilesTo).resolve(Paths.get(fileFullPath).getFileName()));
			} catch (FileAlreadyExistsException e) {
				System.err.printf("Error : Moving %s to %s. File Exists\n", Paths.get(fileFullPath), Paths.get(directoryToMoveFilesTo).resolve(Paths.get(fileFullPath).getFileName()));
			} catch (IOException e) {
				System.err.printf("Error : Moving %s to %s\n", Paths.get(fileFullPath), Paths.get(directoryToMoveFilesTo).resolve(Paths.get(fileFullPath).getFileName()));
			}
		}
	}

	/**
	 * imageExtsSupported
	 * 
	 * Show support image files.
	 * @return
	 */
	private String imageExtsSupported() {
		String exts = "";
		
		System.out.println("\n\tImages supported:");
		imageList = ImageIO.getReaderMIMETypes();
		for(String type : imageList) {
			System.out.println("\t\t" + type);
		}
		
		return exts;
	}

	/**
	 * Show progress
	 * @param index
	 * @param maxIndex
	 */
	private void printProgressBar(int index, int maxIndex) {
		String cursorAnimation = "|/-\\|/-\\";
		int maxTicks = 60; //Number of ticks for the progress bar
		char tickChar = '*';
		String cursorLeft = ""+(char)(8); //Ascii backspace

		int percent = (index*100)/maxIndex; 

		int numStars = (index*maxTicks)/maxIndex; 
		char cursorChar = cursorAnimation.charAt((index)%cursorAnimation.length());
		
		if(percent==100) cursorChar=tickChar;
		
		String precentStr = "Images Processed : [" + repeat(""+tickChar, numStars) + cursorChar + repeat(" ", maxTicks-numStars-1)+"] " + percent + "%";
		System.out.print(precentStr);
		
		//Move the cursor to the beginning of the line. This seems to be platform agnostic. Tried on windows cmd, linux, windows cygwin and window power shell. 
		System.out.print(repeat(cursorLeft, precentStr.length()));
		
	}
	
	/**
	 * 
	 * Simple repeat function to build string.
	 * @param string
	 * @param numRepeat
	 * @return
	 */
	private String repeat(String string, int numRepeat) {
		String out = "";
		for(int i=0;i<numRepeat;i++) {
			out += string;
		}
		return out;
	}



}
