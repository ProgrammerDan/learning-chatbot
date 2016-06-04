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
import java.util.regex.*;
import java.io.Serializable;

/**
 * The ChatbotBrain holds references to all ChatWords and has various
 * methods to decompose and reconstruct sentences.
 */
class ChatbotBrain implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * A tracking of all observed words. Keyed by the String version of
	 * the ChatWord, to allow uniqueness across all ChatWords
	 */
	private Map<String,ChatWord> observedWords;

	/**
	 * This brain is going to be able to keep track of "topics" by way of
	 * a word frequency map. That way, it can generate sentences based
	 * on topic-appropriateness.
	 */
	private FrequencyMap<Double, ChatWord> wordFrequency;

	/**
	 * This holds the count of words observed total.
	 */
	private int wordCount;

	/**
	 * This holds the current "values" of all words.
	 */
	private double wordValues;

	/**
	 * A "word" that is arbitrarily the start of every sentence
	 */
	private ChatWord startWord;

	/**
	 * Rate of decay of "topics".
	 */
	private double decayRate;

	// These values configure various features of the recursive 
	// sentence construction algorithm.
	/** Nominal (target) length of sentences */
	public static final int NOMINAL_LENGTH = 10;
	/** Max length of sentences */
	public static final int MAX_LENGTH = 25;
	/** Sentence creation timeout */
	public static final long TIMEOUT = 5000;
	/** Topic words to match against */
	public static final int TOPICS = 7;
	/** Topic word split: % of global topic words, remainder sentence */
	public static final double TOPIC_SPLIT = 0.48;
	/** Minimum branches to consider for each word */
	public static final int MIN_BRANCHES = 2;
	/** Maximum branches to consider for each word */
	public static final int MAX_BRANCHES = 6;
	/** % chance as integer out of 100 to skip a word */
	public static final int SKIP_CHANCE = 30;
	/** % chance as integer to skip a word that would cause a loop */
	public static final int LOOP_CHANCE = 5;
	/** % chance that punctuation will happen at all */
	public static final int PUNCTUATION_CHANCE = 40;
	/** % chance that a particular punctuation will be skipped */
	public static final int PUNCTUATION_SKIP_CHANCE = 50;
	/** % of high frequency words to skip, to avoid "the, of" etc. */
	public static final int TOPIC_SKIP = 1;
	/** % chance that we'll examine all words in frequency list again
	 * if we fail to branch enough times the first time through our list*/
	public static final int BREADTH_ASSURANCE_CHANCE = 50;

	/** The last sentence observed by the bot, as a value map */
	private FrequencyMap<Double,ChatWord> lastSentence;

	/**
	 * Convenience parameter to use a common random source 
	 * throughout the brain.
	 */
	private Random random;

	/**
	 * Gets the Chatbot started, sets up data structures necessary
	 */
	public ChatbotBrain() {
		observedWords = new HashMap<String,ChatWord>();
		observedWords.put("\n",LearningChatbot.ENDWORD);
		startWord = new ChatWord("");
		observedWords.put("",startWord);

		wordFrequency = new FrequencyMap<Double, ChatWord>();
		decayRate = 0.10;
		wordCount = 0;
		wordValues = 0.0;
		random = new Random();

		lastSentence = new FrequencyMap<Double, ChatWord>();
	}

	/**
	 * More complex digest method (second edition) that takes a sentence,
	 * cuts it up, and links up the words based on ordering.
	 * It is sensitive to punctuation, and also simple typos (like
	 * forgetting to put spaces after punctuation, etc.).
	 * Note the character class is somewhat complex to deal with
	 * stupid English things like hyphenation, possessives, and
	 * abbreviations.
	 */
	public void digestSentence(String sentence) {
		Scanner scan = new Scanner(sentence);

		ChatWord prior = null;
		ChatWord current = null;
		String currentStr = null;
		String currentPnc = null;
		lastSentence.clear();
		while (scan.hasNext()) {
			currentStr = scan.next();
			Pattern wordAndPunctuation = 
					Pattern.compile("([a-zA-Z\\-_'0-9]+)([^a-zA-Z\\-_'0-9]?)[^a-zA-Z\\-_'0-9]*?");
			Matcher findWords = wordAndPunctuation.matcher(currentStr);
			//  Basically this lets us find words-in-word typos like this:
			//  So,bob left his clothes with me again.
			//  where "So,bob" becomes "So," "bob"
			while (findWords.find()) {
				currentStr = findWords.group(1);
				currentPnc = findWords.group(2);
				if (currentStr != null) {
					if (observedWords.containsKey(currentStr)) {
						current = observedWords.get(currentStr);
					} else {
						current = new ChatWord(currentStr);
						observedWords.put(currentStr, current);
					}

					addToLastSentence(current);

					incrementWord(current);
					
					if (currentPnc != null && !currentPnc.equals("")) {
						current.addPunctuation(currentPnc.charAt(0));
					}

					if (prior != null) {
						prior.addDescendent(current);
					}
					if (prior == null) {
						startWord.addDescendent(current);
					}

					prior = current;
				}
			}
		}
		if (prior != null) { // finalize.
			prior.addDescendent(LearningChatbot.ENDWORD);
		}
	}

	/** Helper to add a word to the last sentence collection */
	private void addToLastSentence(ChatWord cw) {
		lastSentence.put(valueWord(cw), cw);
	}

	/** Helper to value a word using a logarithmic valuation */
	private Double valueWord(ChatWord word) {
		if (word.getWord().length() > 0) {
			return (Math.log(word.getWord().length()) /	Math.log(4));
		} else {
			return 0.0; // empty words have no value.
		}
	}

	/**
	 * Increments the value of a word (catalogues a new sighting).
	 * I use a logarithmic value function (log base 4) computed against 
	 * the length of the word. In this way, long words are valued slightly
	 * higher. This is approximate to reality, although truthfully corpus
	 * frequency is a better measure of word value than word length.
	 */
	public void incrementWord(ChatWord word) {
		Double curValue = wordFrequency.getFrequency(word);
		Double nextValue;
		if (curValue == null) {
			curValue = 0.0;
		}
		nextValue=curValue+valueWord(word);
		wordFrequency.put(nextValue, word);

		wordCount++;
		wordValues++;
	}
	
	/**
	 * Decays a particular word by decay rate.
	 */
	public void decayWord(ChatWord word) {
		Double curValue = wordFrequency.getFrequency(word);
		Double nextValue;
		if (curValue == null) {
			return;
		}
		wordValues-=curValue; // remove old decay value
		nextValue=curValue-(curValue*decayRate);
		wordValues+=nextValue; // add new decay value
		
		wordFrequency.put(nextValue, word);
	}

	/**
	 * Decay all word's frequency values. This allows changes
	 * in the bot's perceptions of conversation topics
	 */
	public void decay() {
		for (ChatWord cw : wordFrequency.valueKeySet()) {
			decayWord(cw);
		}
	}

	/**
	 * Gets a set of words that appear to be "top" of the frequency
	 * list.
	 */
	public Set<ChatWord> topicWords(int maxTopics) {
		Set<ChatWord> topics = new HashSet<ChatWord>();
		int maxGlobalTopics = (int) (maxTopics * (double)TOPIC_SPLIT);
		int maxSentenceTopics = maxTopics;

		int nTopics = 0;
		int topicSkip = (int)(((float)wordCount * (float)TOPIC_SKIP)/100f);
		//System.out.print("Topics (global):");
		for (ChatWord word: wordFrequency.descendingValues()) {
			if (topicSkip <= 0) { 
				topics.add(word);
				//System.out.printf(" [%2f %s]", wordFrequency.getFrequency(word), word.getWord());
				nTopics++;
				if (nTopics == maxGlobalTopics) break;
			} else {
				topicSkip--;
			}
		}
		//System.out.print("\nTopics (local):");
		for (ChatWord word: lastSentence.descendingValues()) {
			topics.add(word);
			//System.out.printf(" [%2f %s]", wordFrequency.getFrequency(word), word.getWord());
			nTopics++;
			if (nTopics == maxSentenceTopics) break;
		}
		//System.out.println();
		//System.out.printf("\nFinal count: %d\n", topics.size());
		return topics;
	}

	/**
	 * Uses word frequency records to prefer to build on-topic
	 * sentences.
	 * Feature highlights:
	 *  - Loops are detected directly within the recursive function, and
	 *    while they are technically allowed, there is a high chance that
	 *    loops will be avoided.
	 *  - This is a breadth-first search, with branch pruning and
	 *    descending timeout as depth increases.
	 *  - The maximizing function is on-topic-ness, with a small preference
	 *    for ending sentences. Basically, sentences that don't involve
	 *    topic words are weighted very low, while sentences involving
	 *    as many topic words as possible are weighted high.
	 *  - ChatWords know which ChatWords they precede most often, so 
	 *    sentences are constructed making heavy use of this feature
	 */
	public String buildSentence() {
		int maxDepth = NOMINAL_LENGTH+
				random.nextInt(MAX_LENGTH - NOMINAL_LENGTH);
		ChatSentence cs = new ChatSentence(startWord);
		// We don't want to take too long to "think of an answer"
		long timeout = System.currentTimeMillis() + TIMEOUT;
		//System.out.printf("builder: md=%d to=%d\n",maxDepth,timeout);
		double bestValue = buildSentence(cs, topicWords(TOPICS), 0.0, 0, maxDepth, timeout);
		return cs.toString();
	}

	/** 
	 * Suppression function to reduce the value of words as the sentence
	 * grows.
	 */
	private double suppressWords(int curDepth, int maxDepth) {
		double half = (((double) (NOMINAL_LENGTH+maxDepth))/2.0);
		double cdep = (double) curDepth;
		double v=1.0/(1.0 + Math.exp(Math.E*(cdep-half)/half));
		return v;
	}

	/**
	 * Recursive portion of the buildSentence algorithm.
	 */
	public double buildSentence(ChatSentence sentence, 
			Set<ChatWord> topics, double curValue,
			int curDepth, int maxDepth, long timeout){
		if (curDepth==maxDepth || System.currentTimeMillis() > timeout) {
			return curValue;
		}
		// Determine how many branches to enter from this node
		int maxBranches = MIN_BRANCHES + random.nextInt(MAX_BRANCHES - MIN_BRANCHES);
		// Determine suppression for this depth.
		double suppress = suppressWords(curDepth, maxDepth);
		//System.out.printf("%2d %2d %5.2f\n",curDepth,maxBranches,suppress);
		// try a few "best" words from ChatWord's descendent list.
		ChatWord word = sentence.getLastWord();
		NavigableMap<Integer, Collection<ChatWord>> roots =
				word.getDescendents();
		// Going to keep track of current best encountered sentence
		double bestSentenceValue = curValue;
		ChatSentence bestSentence = null;
		int curBranches = 0;
		// This is to combat prematurely ended sentences.
		while (curBranches < MIN_BRANCHES) {
			for (Integer freq : roots.descendingKeySet()) {
				for (ChatWord curWord : roots.get(freq)) {
					int chance = random.nextInt(100);
					if (curWord.equals(LearningChatbot.ENDWORD)) {
						if (chance>=SKIP_CHANCE) {
							double endValue = random.nextDouble() * wordFrequency.lastKey() * suppress;
							/* The endword's value is a random portion of
							 * the highest frequency word's value, so it's
							 * comparable, also gives a slight preference
							 * to ending sentences.*/
							//if(curDepth==0)
							//System.out.printf("          %2d %2d %5.2f [%s]\n",curDepth,curBranches,curValue+endValue,sentence);
							if (curValue+endValue > bestSentenceValue) {
								bestSentenceValue = curValue+endValue;
								bestSentence = new ChatSentence(sentence);
								// Try to add punctuation if possible.
								addPunctuation(bestSentence);
								bestSentence.addWord(curWord); // then end.
							}
							curBranches++;
						}
					} else {
						boolean loop = sentence.hasWord(curWord);
						/* Include a little bit of chance in the inclusion
						 * of any given word, whether a loop or not.*/
						if ( (!loop&&chance>=SKIP_CHANCE) ||
								(loop&&chance<LOOP_CHANCE)) {
							double wordValue = suppress*wordFrequency.getFrequency(curWord)*
									(topics.contains(curWord)?1.0:0.25);
							ChatSentence branchSentence = new ChatSentence(sentence);
							branchSentence.addWord(curWord);
							addPunctuation(branchSentence);
							//System.out.printf("          %d %d %5.2f [%s]\n",curDepth,curBranches,curValue+wordValue,branchSentence);
							double branchValue = buildSentence(branchSentence,
									topics, curValue+wordValue, curDepth+1,
									maxDepth, timeout);
							//if (curDepth==0)System.out.printf("%2f [%s]\n",branchValue,branchSentence);
							if (branchValue > bestSentenceValue) {
								bestSentenceValue = branchValue;
								bestSentence = branchSentence;
							}
							curBranches++;
						}
					}
					if (curBranches == maxBranches) break;
				}
				if (curBranches == maxBranches) break;
			}
			if (random.nextInt()>=BREADTH_ASSURANCE_CHANCE)	break;
		}
		if (bestSentence != null) {
			sentence.replaceSentence(bestSentence);
		}
		return bestSentenceValue;
	}

	/**
	 * Adds punctuation to a sentence, potentially.
	 */
	public void addPunctuation(ChatSentence sentence) {
		ChatWord word = sentence.getLastWord();
		NavigableMap<Integer, Collection<Character>> punc = word.getPunctuation();
		if (punc.size()>0 && random.nextInt(100)<PUNCTUATION_CHANCE){
			Integer puncMax = punc.lastKey();
			Collection<Character> bestPunc = punc.get(puncMax);
			Character puncPick = null;
			for (Integer freq : punc.descendingKeySet()) {
				for (Character curPunc : punc.get(freq)) {
						if (random.nextInt(100)>=PUNCTUATION_SKIP_CHANCE) {
							puncPick = curPunc;
							break;
						}
				}
				if (puncPick != null) break;
			}
			if (puncPick != null) {
				sentence.addCharacter(puncPick);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ChatBrain[");
		sb.append(observedWords.size());
		sb.append("]:");
		for (Map.Entry<String,ChatWord> cw : observedWords.entrySet()) {
			sb.append("\n\t");
			sb.append(wordFrequency.getFrequency(cw.getValue()));
			sb.append("\t");
			sb.append(cw.getValue());
		}
		return sb.toString();
	}

/*	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
	}
	private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
	}
	private void readObjectNoData() throws ObjectStreamException {
	}
*/

}

