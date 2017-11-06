package com.loremachine.tools;
import java.io.*;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class DataExtractor {

	// Constant for space defining a tab.
    private static final String TAB = "    ";
    // The current number of tabs in we are.
    private static int tabLevel = 0;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("No mode given!");
            System.exit(1);
        }

        HashSet<File> files = loadLists(args[0] + "-");
        switch(args[0]) {
            case "spells":
                Spell.processSpells(files);
                break;
            default:
                break;
        }
    }

    /**
     * This method will generate data files for the given list of JSON objects.
     * It will write a file with the given name to ./output/filename.js containing
     * an exported array with all the data from the JSON objects formatted neatly.
     * 
     * It will also expect each object to contain an id and name field.
     * This allows it to also generate an ID file at ./output/ids/filenameIDs.js containing
     * an exported object that maps object names to their ids. This object can then be imported
     * to refer to object by ID much easier in code.
     * 
     * @param filename Basic file "title" no directory or extensions please.
     * @param dataTitle The name of the type of data to use for exported stuff.
     * @param data A list of all the JSON objects to write out.
     */
    protected static void generateFiles(String filename, String dataTitle, List<JSONObject> data) {
    	
    	// Preamble thanking PC gen
    	String preamble = "// Data scraped from PC gen's data files. All credit for hauling in this data goes to the PC gen team: http://pcgen.org/";
    	// The content string for the ID file
    	String idContents =  "export const " + dataTitle + "_ids = {\n";
    	// The content string for the data file
    	String dataContents = "import { " + dataTitle + "_ids } from \'./ids/" + filename + "IDs\';\n"
    						+ "export const " + dataTitle + "_data = \n[";
    	
    	// Start at tab level 1
    	tabLevel = 1;
    	for (JSONObject o : data) {
    		
    		// If the object has no name or id field ignore it
    		if (o.get("id") == null || o.get("name") == null)
    			continue;
    		// Get the name and ID
    		int id = (Integer) o.get("id");
    		// Ensure the name is all lowercase and alphanumeric with underscores instead of spaces
    		String name = nameify((String) o.get("name"));
    		
    		// Add the name/id to the ID file
    		idContents += TAB + name + ": " + id + ",\n";
    		// Stringify this object
    		dataContents += stringifyObject(o, dataTitle) + ",";
    	}
    	
    	// Fence-posting
    	idContents += "}\n";
    	dataContents += "\n]\n";
    	
    	
    	// Create new files for output
    	File fIDs = new File("output/ids/" + filename + "IDs.js");
    	File fData = new File("output/" + filename + ".js");
    	if (!fIDs.exists()) {
			try { fIDs.createNewFile(); }
			catch (IOException e) { e.printStackTrace(); }
    	}
    	if (!fData.exists()) {
			try { fData.createNewFile(); }
			catch (IOException e) { e.printStackTrace(); }
    	}
    	
    	// Write data to files
    	FileWriter fw = null;
    	try {
    		fw = new FileWriter(fIDs);
    		fw.write(preamble + "\n\n" + idContents + "\n\n");
    		fw.close();
    		fw = new FileWriter(fData);
    		fw.write(preamble + "\n\n" + dataContents + "\n\n");
    	}
    	catch (IOException e) { e.printStackTrace(); }
    	finally {
    		// Close filewriter
    		try { if (fw != null) fw.close(); }
    		catch (IOException e) { e.printStackTrace(); }
    	}
    }
    
    /**
     * This turns a JSON object into a pretty ordered string with appropriate indentation.
     * This will recursively work through JSON objects and arrays organizing them and
     * adding the right amount of indentation as it goes.
     * 
     * If an ID field is found it will map the value to a map entry with the given dataTile_ids name
     * as is defined in generateFiles.
     * @param o The object to stringify.
     * @param dataTitle The title of the data map.
     * @return A nice pretty string for this JSON object.
     */
    private static String stringifyObject(JSONObject o, String dataTitle) {
    	String ret = "";
    	ret += getTab() + "\n" + getTab() + "{\n";
    	tabLevel++;
    	for (Object field : o.keySet()) {
    		if (field instanceof String && field.equals("id")) {
    			if (o.get("name") != null) {
    				ret += getTab() + "id: " + dataTitle + "_ids." + nameify((String)o.get("name")) + ",\n";
    			}
    		}
    		else {
    			Object value = o.get(field);
    			ret += getTab() + field + ": ";
    			if (value instanceof JSONObject)
    				ret += stringifyObject((JSONObject) value, dataTitle) + ",\n";
    			else if (value instanceof JSONArray)
    				ret += stringifyArray((JSONArray) value, dataTitle) + ",\n";
    			else if (value instanceof String)
    				ret += "\'" + value + "\',\n";
    			else
    				ret += value + ",\n";
    		}
     	}
    	tabLevel--;
		ret += getTab() + "}";
		return ret;
    }
    
    /**
     * This functions very similarly to stringifyObject except it accepts a JSON array.
     * It will walk through the objects of the array and recursively stringify each entry.
     * @param a The JSON array to make pretty.
     * @param dataTitle The name of the data map as needed by stringifyObject and given by generateFiles.
     * @return A pretty string for the given array.
     */
    private static String stringifyArray(JSONArray a, String dataTitle) {
    	String ret = "";
    	ret += "\n" + getTab() + "[";
    	tabLevel++;
    	for (Object item : a) {
			ret += getTab();
			if (item instanceof JSONObject)
				ret += stringifyObject((JSONObject) item, dataTitle) + ",\n";
			else if (item instanceof JSONArray)
				ret += stringifyArray((JSONArray) item, dataTitle) + ",\n";
			else if (item instanceof String)
				ret += "\'" + item + "\',\n";
			else
				ret += item + ",\n";
     	}
    	tabLevel--;
    	ret += getTab() + "]";
    	return ret;
    }
    
    /**
     * Get the proper amount of spaces for the current tab level.
     * @return A string with the right amount of spaces for the current tab level.
     */
    private static String getTab() {
    	String ret = "";
    	for (int i = 0 ; i < tabLevel ; i++) {
    		ret += TAB;
    	}
    	return ret;
    }
    
    /**
     * This normalizes a name.
     * It turns it into an all lower-case alpha-numeric string with underscores instead of spaces.
     * @param name The name to normalize.
     * @return The normalized name.
     */
    private static String nameify(String name) {
    	return Normalizer.normalize(name, Form.NFD)
				.toLowerCase()
				.replaceAll(" ", "_")
				.replaceAll("[^A-Za-z0-9_]", "");
    }

    /**
     * This will load and pre-process all files in the input directory with the
     * given prefix.
     * @param prefix The prefix of the files we should load in.
     * @return A set of all the pre-processed files we found.
     */
    private static HashSet<File> loadLists(String prefix) {
        HashSet<File> inputFiles = new HashSet<File>();
        File inputDir = new File("input");
        if (!inputDir.isDirectory())
            return null;
        for (File f : inputDir.listFiles()) {
            if (f.getName().startsWith(prefix)) {
                inputFiles.add(preProcess(f));
            }
        }
        return inputFiles;
    }

    /**
     * A quick way of pre-processing the file before we pull data from it.
     * This will remove various types of comments from the file, and write a new one in
     * the current directory with a .proc extension.
     * @param f The file we are pre-processing.
     * @return The pre-processed file.
     */
    private static File preProcess(File f) {
        // .proc file contents
        String newFile = "";
        // Scan in file line by line
        try (Scanner fScan = new Scanner(f)) {
            boolean isInBlock = false;
            while (fScan.hasNextLine()) {
                String line = fScan.nextLine();
                // Check if we are in a block comment
                if (isInBlock) {
                    // Signal block end
                    if (line.startsWith("*/"))
                        isInBlock = false;
                    // Otherwise eat the line
                    continue;
                }
                // Look for block comment start, signal it and eat the line
                else if (line.startsWith("/*")) {
                    isInBlock = true;
                    continue;
                }
                // Look for line comment start and eat the line
                else if (line.startsWith("#")) {
                    continue;
                }
                // Otherwise add the contents for the line
                else {
                    newFile += line + "\n";
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Write .proc file
        File ret = new File(f.getName() + ".proc");
        try (FileWriter writer  = new FileWriter(ret)) {
            writer.write(newFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }
    
    protected static HashMap<String, String> tokenator(String firstTokenID, String fileLine) {
    	Scanner s = new Scanner(fileLine);
		HashMap<String, String> tagMap = new HashMap<String, String>();
		String token = "";
		String currentTokenName = firstTokenID;
		String currentTokenData = null;
		// Read in spell name
		while (s.hasNext()) {
			token = s.next();
			if (token.matches("[A-Z]*:.*")) {
				tagMap.put(currentTokenName, currentTokenData);
				currentTokenName = token.substring(0, token.indexOf(':'));
				currentTokenData = token.substring(token.indexOf(':') + 1);
			}
			else {
				if (currentTokenData != null)
					currentTokenData += " " + token;
				else
					currentTokenData = token;
			}
		}
		s.close();
		tagMap.put(currentTokenName, currentTokenData);
		return tagMap;
    }
    
    protected static String stringPurifier(String gross) {
    	if (gross != null && gross.length() > 0)
    		return gross.replace("'", "\\'").replace("\"", "\\\"");
    	else
    		return gross;
    }
    
    protected static String stringifyMap(HashMap<?, ?> map) {
    	String ret = "";
		for (Object key : map.keySet()) {
			ret += key.toString() + " : " + map.get(key).toString() + "\n";
		}
		return ret;
	}
    
}
