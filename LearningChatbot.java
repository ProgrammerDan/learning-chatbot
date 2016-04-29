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
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Invocation method.
	 */
	public void beginConversation() {
		ChatbotBrain cb = new ChatbotBrain();

		Scanner dialog = new Scanner(System.in);

		boolean more = true;

		while (more) {
			System.out.print("    You? ");
			String input = dialog.nextLine();

			if (input.equals("++done")) {
				System.exit(0);
			} else if (input.equals("++save")) {
				System.out.println("Saving not yet implemented, sorry!");
				System.exit(0);
			} else if (input.equals("++help")) {
				getHelp();
			}else {
				cb.decay();
				cb.digestSentence(input);
			}

			System.out.print("Chatbot? ");
			System.out.println(cb.buildSentence());
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
			System.out.printf("Using %s as brain file, if possible.", args[0]);
			lc = new LearningChatbot(args[0]);
		} else {
			lc = new LearningChatbot();
		}
		lc.beginConversation();
	}

	/**
	 * The ChatbotBrain holds references to all ChatWords and has various
	 * methods to decompose and reconstruct sentences.
	 */
	static class ChatbotBrain {
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
		private Map<ChatWord, Double> wordFrequencyLookup;

		/**
		 * This holds the actual word frequencies, for quick isolation of
		 * highest frequency words.
		 */
		private NavigableMap<Double, Collection<ChatWord>> wordFrequency;

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
		private NavigableMap<Double,Collection<ChatWord>> lastSentence;

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
			observedWords.put("\n",ENDWORD);
			startWord = new ChatWord("");
			observedWords.put("",startWord);

			wordFrequencyLookup = new HashMap<ChatWord, Double>();
			wordFrequency = new TreeMap<Double, Collection<ChatWord>>();
			decayRate = 0.10;
			wordCount = 0;
			wordValues = 0.0;
			random = new Random();

			lastSentence = new TreeMap<Double, Collection<ChatWord>>();
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
			clearLastSentence();
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
				prior.addDescendent(ENDWORD);
			}
		}

		/** Helper to clear lastSentence. */
		private void clearLastSentence() {
			for (Double key : lastSentence.keySet()) {
				lastSentence.get(key).clear();
			}
			lastSentence.clear();
		}

		/** Helper to add a word to the last sentence collection */
		private void addToLastSentence(ChatWord cw) {
			Double value = valueWord(cw);
			Collection<ChatWord> words;
			if (lastSentence.containsKey(value)) {
				words = lastSentence.get(value);
			} else {
				words = new HashSet<ChatWord>();
				lastSentence.put(value, words);
			}
			words.add(cw);
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
			Double curValue;
			Double nextValue;
			Collection<ChatWord> freqMap;
			if (wordFrequencyLookup.containsKey(word)) {
				curValue = wordFrequencyLookup.get(word);
				freqMap = wordFrequency.get(curValue);
				freqMap.remove(word);
			} else {
				curValue = 0.0;
			}
			nextValue=curValue+valueWord(word);
			wordFrequencyLookup.put(word, nextValue);

			freqMap = wordFrequency.get(nextValue);
			if (freqMap == null) {
				freqMap = new HashSet<ChatWord>();
				wordFrequency.put(nextValue, freqMap);
			}

			freqMap.add(word);
			wordCount++;
			wordValues++;
		}
		
		/**
		 * Decays a particular word by decay rate.
		 */
		public void decayWord(ChatWord word) {
			Double curValue;
			Double nextValue;
			Collection<ChatWord> freqMap;
			if (wordFrequencyLookup.containsKey(word)) {
				curValue = wordFrequencyLookup.get(word);
				freqMap = wordFrequency.get(curValue);
				freqMap.remove(word);
			} else {
				return;
			}
			wordValues-=curValue; // remove old decay value
			nextValue=curValue-(curValue*decayRate);
			wordValues+=nextValue; // add new decay value
			wordFrequencyLookup.put(word, nextValue);

			freqMap = wordFrequency.get(nextValue);
			if (freqMap == null) {
				freqMap = new HashSet<ChatWord>();
				wordFrequency.put(nextValue, freqMap);
			}

			freqMap.add(word);
		}

		/**
		 * Decay all word's frequency values. This allows changes
		 * in the bot's perceptions of conversation topics
		 */
		public void decay() {
			for (ChatWord cw : wordFrequencyLookup.keySet()) {
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
			//System.out.println("Topics:");
			for (Double weight: wordFrequency.descendingKeySet()) {
				for (ChatWord word: wordFrequency.get(weight)) {
					if (topicSkip <= 0) { 
						topics.add(word);
						//System.out.printf("\t%2f %s (global)", weight, word.getWord());
						nTopics++;
						if (nTopics == maxGlobalTopics) break;
					} else {
						topicSkip--;
					}
				}
				if (nTopics == maxGlobalTopics) break;
			}
			//System.out.println();
			for (Double weight: lastSentence.descendingKeySet()) {
				for (ChatWord word: lastSentence.get(weight)) {
					topics.add(word);
						//System.out.printf("\t%2f %s (last)", wordFrequencyLookup.get(word), word.getWord());
					nTopics++;
					if (nTopics == maxSentenceTopics) break;
				}
				if (nTopics == maxSentenceTopics) break;
			}
			//System.out.printf("\nFinal count: %d\n", topics.size());
			return topics;
		}

		/**
		 * Uses word frequency records to prefer to build on-topic
		 * sentences.
		 * Feature highlights:
		 *  - There is a built-in depth maximum to prevent too much looping
		 *  - Loops are detected directly within the recursive function, and
		 *    while they are technically allowed, there is a high chance that
		 *    loops will be avoided.
		 *  - This is a depth-first search, so the depth maximum and timeout
		 *    together help encourage branch pruning.
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
			double bestValue = buildSentence(cs, topicWords(TOPICS), 0.0, 0, maxDepth, timeout);
			return cs.toString();
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
						if (curWord.equals(ENDWORD)) {
							if (chance>=SKIP_CHANCE) {
								double endValue = random.nextDouble() * wordFrequency.lastKey();
								/* The endword's value is a random portion of
								 * the highest frequency word's value, so it's
								 * comparable, also gives a slight preference
								 * to ending sentences.*/
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
								double wordValue = topics.contains(curWord)?
										wordFrequencyLookup.get(curWord):0.0;
								ChatSentence branchSentence = new ChatSentence(sentence);
								branchSentence.addWord(curWord);
								addPunctuation(branchSentence);
								double branchValue = buildSentence(branchSentence,
										topics, curValue+wordValue, curDepth+1,
										maxDepth, timeout);
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
				sb.append(wordFrequencyLookup.get(cw.getValue()));
				sb.append("\t");
				sb.append(cw.getValue());
			}
			return sb.toString();
		}

	}

	/**
	 * Useful helper class to construct sentences.
	 */
	static class ChatSentence implements Cloneable {
		/**
		 * List of words.
		 */
		private List<Object> words;
		/**
		 * Quick search construct to have O(ln) lookup times.
		 */
		private Set<Object> contains;

		/**
		 * Starts to build a sentence with a single word as anchor
		 */
		public ChatSentence(ChatWord anchor) {
			if (anchor == null) {
				throw new IllegalArgumentException("Anchor must not be null");
			}
			words = new ArrayList<Object>();
			contains = new HashSet<Object>();
			words.add(anchor);
			contains.add(anchor);
		}

		/** 
		 * Starts a sentence using an existing ChatSentence. Also used for
		 * cloning.
		 */
		public ChatSentence(ChatSentence src) {
			words = new ArrayList<Object>();
			contains = new HashSet<Object>();
			appendSentence(src);
		}

		/**
		 * Adds a word to a sentence
		 */
		public ChatSentence addWord(ChatWord word) {
			if (word == null) {
				throw new IllegalArgumentException("Can't add null word");
			}
			words.add(word);
			contains.add(word);
			return this;
		}

		/**
		 * Adds a character to a sentence.
		 */
		public ChatSentence addCharacter(Character punc) {
			if (punc == null) {
				throw new IllegalArgumentException("Can't add null punctuation");
			}
			words.add(punc);
			contains.add(punc);
			return this;
		}

		/**
		 * Replace a sentence with some other sentence.
		 * Useful to preserve references.
		 */
		public ChatSentence replaceSentence(ChatSentence src) {
			words.clear();
			contains.clear();
			appendSentence(src);
			return this;
		}

		public ChatSentence appendSentence(ChatSentence src) {
			words.addAll(src.getWords());
			contains.addAll(src.getWords());
			return this;
		}

		/**
		 * Get last word of the sentence.
		 */
		public ChatWord getLastWord() {
			for (int i=words.size()-1; i>=0; i--) {
				if (words.get(i) instanceof ChatWord) {
					return (ChatWord) words.get(i);
				}
			}
			throw new IllegalStateException("No ChatWords found!");
		}

		/**
		 * Checks if the sentence has a word
		 */
		public boolean hasWord(ChatWord word) {
			return contains.contains(word);
		}

		/**
		 * Counts the number of words in a sentence.
		 */
		public int countWords() {
			int cnt = 0;
			for (Object o : words) {
				if (o instanceof ChatWord) {
					cnt++;
				}
			}
			return cnt;
		}

		/**
		 * Gets all the words of the sentence
		 */
		private List<Object> getWords() {
			return words;
		}

		/**
		 * Returns the sentence as a string.
		 */
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (Object o : words) {
				if (o instanceof ChatWord) {
					ChatWord cw = (ChatWord) o;
					sb.append(" ");
					sb.append( cw.getWord() );
				} else {
					sb.append(o);
				}
			}
			return sb.toString().trim();
		}

		/**
		 * Clones this sentence.
		 */
		@Override
		public Object clone() {
			return new ChatSentence(this);
		}
	}

	/**
	 * ChatWord allows the creation of words that track how they are
	 * connected to other words in a forward fashion. In this way it is
	 * possible to construct arbitrary length sentences involving a set
	 * of keywords harvested from statements. Trust me, it's possible.
	 */
	static class ChatWord {
		/** The word. */
		private String word;
		/** Collection of punctuation observed after this word */
		private NavigableMap<Integer, Collection<Character>> punctuation;
		/** Lookup linking observed punctuation to where they are in ordering */
		private Map<Character, Integer> punctuationLookup;
		/** Punctionation observation count */
		private Integer punctuationCount;
		
		/** Collection of ChatWords observed after this word */
		private NavigableMap<Integer, Collection<ChatWord>> firstOrder;
		/** Lookup linking observed words to where they are in ordering */
		private Map<ChatWord, Integer> firstOrderLookup;
		/** First order antecedent word count */
		private Integer firstOrderCount;

		/**
		 * Creates a new ChatWord that is aware of punctuation that
		 * follows it, and also ChatWords that follow it.
		 */
		public ChatWord(String word){
			this.word = word;

			this.firstOrder = new TreeMap<Integer, Collection<ChatWord>>();
			this.firstOrderLookup = new HashMap<ChatWord, Integer>();
			this.firstOrderCount = 0;

			this.punctuation = new TreeMap<Integer, Collection<Character>>();
			this.punctuationLookup = new HashMap<Character, Integer>();
			this.punctuationCount = 0;
		}

		/**
		 * Including this for now, but I don't like it -- it returns all
		 * descendents wholesale. I think what would be better is some
		 * function that returns a descendent based on some characteristic.
		 */
		protected NavigableMap<Integer, Collection<ChatWord>> getDescendents() {
			return firstOrder;
		}

		/**
		 * Returns how many descendents this word has seen.
		 */
		protected int getDescendentCount() {
			return firstOrderCount;
		}

		/**
		 * Gets the lookup map for descendents
		 */
		protected Map<ChatWord, Integer> getDescendentsLookup() {
			return firstOrderLookup;
		}

		/** As conversation progresses, word orderings will be encountered.
		 * The descendent style of "learning" basically weights how often
		 * words are encountered together, and is strongly biased towards
		 * encountered ordering.
		 * Thus, when constructing a "reply", the bot can use what it
		 * has "learned" about precedence and ordering to construct
		 * phrases that may or may not make sense. It's a grand adventure!
		 * This function allows recording a new occurence of a word after 
		 * this ChatWord. For word recording purposes, it is recommended
		 * that all but [-a-zA-Z0-9] be removed. This isn't required, but
		 * as we're discussing speech and not high forms of communication it
		 * should be sufficient.
		 */
		public void addDescendent(ChatWord next) {
			if(next != null){
				firstOrderCount++;
				int nextCount = 1;
				Collection<ChatWord> obs = null;
				// If we've already seen this word, clean up prior membership.
				if(firstOrderLookup.containsKey(next)){
					nextCount = firstOrderLookup.remove(next);
					obs = firstOrder.get(nextCount);
					// Remove from prior obs count order
					obs.remove(next);
					nextCount++;
				}
				obs = firstOrder.get(nextCount);
				if (obs == null) { // we don't have this order yet
					obs = new HashSet<ChatWord>();
					firstOrder.put(nextCount, obs);
				}
				firstOrderLookup.put(next, nextCount);
				obs.add(next);
			}
		}

		/**
		 * Some words have punctuation after them more often than not. 
		 * This allows the ChatBrain to record occurrences of punctuation
		 * after a word.
		 */
		public void addPunctuation(Character punc) {
			if(punc != null){
				punctuationCount++;
				int puncCount = 1;
				Collection<Character> obs = null;
				// If we've already seen this punc, clean up prior membership.
				if(punctuationLookup.containsKey(punc)){
					puncCount = punctuationLookup.remove(punc);
					obs = punctuation.get(puncCount);
					// Remove from prior obs count order
					obs.remove(punc);
					puncCount++;
				}
				obs = punctuation.get(puncCount);
				if (obs == null) { // we don't have this order yet
					obs = new HashSet<Character>();
					punctuation.put(puncCount, obs);
				}
				punctuationLookup.put(punc, puncCount);
				obs.add(punc);
			}
		}

		/**
		 * Including this for now, but I don't like it -- it returns all
		 * punctuation wholesale. I think what would be better is some
		 * function that returns punctuation based on some characteristic.
		 */
		protected NavigableMap<Integer, Collection<Character>> getPunctuation() {
			return punctuation;
		}

		/**
		 * Gets count of punctuation encountered.
		 */
		protected int getPunctuationCount() {
			return punctuationCount;
		}

		/**
		 * Gets lookup of punctuations encountered.
		 */
		protected Map<Character, Integer> getPunctuationLookup() {
			return punctuationLookup;
		}

		/**
		 * Gets the String backing this ChatWord.
		 */
		public String getWord() {
			return word;
		}

		/**
		 * ChatWords are equivalent with the String they wrap.
		 */
		@Override
		public int hashCode() {
			return word.hashCode();
		}

		/**
		 * ChatWord equality is that ChatWords that wrap the same String
		 * are equal, and a ChatWord is equal to the String that it contains.
		 */
		@Override
		public boolean equals(Object o){
			if (o == this) {
				return true;
			}
			if (o instanceof ChatWord) {
				return ((ChatWord)o).getWord().equals(this.getWord());
			}
			if (o instanceof String) {
				return ((String)o).equals(this.getWord());
			}

			return false;
		}

		/**
		 * Returns this ChatWord as a String.
		 */
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("ChatWord[");
			sb.append(word);
			sb.append("]desc{");
			for (Integer key : firstOrder.keySet() ) {
				Collection<ChatWord> value = firstOrder.get(key);
				sb.append(key);
				sb.append(":[");
				for (ChatWord cw : value) {
					sb.append(cw.getWord());
					sb.append(",");
				}
				sb.append("],");
			}
			sb.append("}punc{");
			for (Integer key : punctuation.keySet() ) {
				Collection<Character> value = punctuation.get(key);
				sb.append(key);
				sb.append(":[");
				for (Character c : value) {
					sb.append("\"");
					sb.append(c);
					sb.append("\",");
				}
				sb.append("],");
			}
			sb.append("}");
			return sb.toString();
		}
	}
}


