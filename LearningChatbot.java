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

import java.util.Scanner;

public class LearningChatbot {
	/**
	 * Static definition of final word in a statement. It never has 
	 * any descendents, and concludes all statements. This is the only
	 * "starting knowledge" granted the bot.
	 */
	public static final ChatWord ENDWORD = new ChatWord("\n");

	/**
	 * The Brain of this operation.
	 */
	private ChatbotBrain brain;

	/**
	 * Starts LearningChatbot with a new brain
	 */
	public LearningChatbot() {
		brain = new ChatbotBrain();
	}

	/**
	 * Starts LearningChatbot with restored brain.
	 */
	public LearningChatbot(String filename) {
		brain = BrainMuxer.loadBrain(filename);
		if (brain == null) {
			throw new RuntimeException("That brain file is missing or invalid!");
		}
	}

	/**
	 * Invocation method.
	 */
	public void beginConversation() {
		Scanner dialog = new Scanner(System.in);

		boolean more = true;

		while (more) {
			System.out.print("    You? ");
			String input = dialog.nextLine();

			if (input.equals("++done")) {
				more = false;
				continue;
			} else if (input.equals("++save")) {
				System.out.println("Filename to save to? (\"cancel\" to abort save)");
				String filename = dialog.nextLine();
				if (!filename.equalsIgnoreCase("cancel")) {
					if (BrainMuxer.saveBrain(brain, filename)) {
						System.out.println("Brain saved successfully!");
					} else {
						System.out.println("There was a problem saving the brain.");
					}
				} else {
					System.out.println("Save cancelled.");
				}
				continue;
			} else if (input.equals("++help")) {
				getHelp();
				continue;
			}else {
				brain.decay();
				brain.digestSentence(input);
			}

			System.out.print("Chatbot? ");
			System.out.println(brain.buildSentence());
		}
	}

	/**
	 * Help display
	 */
	public static void getHelp() {
		System.out.println("At any time during the conversation, type");
		System.out.println("   ++done");
		System.out.println("to exit without saving.");
		System.out.println("Or type");
		System.out.println("   ++save");
		System.out.println("to exit and save the brain.");
		System.out.println();
	}

	/**
	 * Get things started.
	 */
	public static void main(String[] args) {
		System.out.println("Welcome to the Learning Chatbot");
		System.out.println();
		getHelp();

		LearningChatbot lc = null;
		if (args.length > 0) {
			System.out.printf("Using %s as brain file, if possible.\n\n", args[0]);
			lc = new LearningChatbot(args[0]);
		} else {
			lc = new LearningChatbot();
		}
		lc.beginConversation();
	}
}


