/* "Zero"-knowledge Learning ChatBot  Copyright (C) 2014-2016 Daniel Boston (ProgrammerDan)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the 
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

import java.util.*;
import java.io.*;

/**
 * Brain Muxer handle demuxing and remuxing the brain from a savefile.
 */
class BrainMuxer {

	public static boolean saveBrain(ChatbotBrain brain, String filename) {
		try {
			// Check if file exists.
			File file = new File(filename);
			if (!file.createNewFile()) {
				System.err.println("File already exists: " + filename);
				return false;
			}

			ObjectOutputStream saveOut = new ObjectOutputStream(new FileOutputStream(file));
			saveOut.writeObject(brain);
			saveOut.flush();
			saveOut.close();
		} catch (IOException iof) {
			System.err.println("Could not access file: " + filename);
			iof.printStackTrace();
			return false;
		}

		return true;
	}

	public static ChatbotBrain loadBrain(String filename) {
		try {
			// Check if file exists.
			File file = new File(filename);
			if (!file.exists()) {
				System.err.println("File does not exist: " + filename);
				return null;
			}

			// load
			ObjectInputStream loadIn = new ObjectInputStream(new FileInputStream(file));
			ChatbotBrain chatbrain = (ChatbotBrain) loadIn.readObject();
			loadIn.close();

			return chatbrain;
		} catch (IOException iof) {
			System.err.println("Could not access file: " + filename);
			iof.printStackTrace();
			return null;
		} catch (ClassNotFoundException cnfe) {
			System.err.println("That file does not contain a valid brain: " + filename);
			cnfe.printStackTrace();
			return null;
		}
	}

}

