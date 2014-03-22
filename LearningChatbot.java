import java.util.*;

public class LearningChatbot {
	/**
	 * Static definition of final word in a statement. It never has 
	 * any descendents, and concludes all statements. This is the only
	 * "starting knowledge" granted the bot.
	 */
	public static final ChatWord ENDWORD = new ChatWord("\n");

	/**
	 * Starts LearningChatbot with a new brain
	 */
	public LearningChatbot() {
		
	}

	/**
	 * Starts LearningChatbot with restored brain.
	 */
	public LearningChatbot(String filename) {

	}

	/**
	 * Invocation method.
	 */
	public void beginConversation() {
		ChatbotBrain cb = new ChatbotBrain();

		cb.digestSentence("This is a test");
		cb.digestSentence("This was never going to work");
		cb.digestSentence("I am a real boy now");
		cb.digestSentence("If you were a real boy you would know");
		cb.digestSentence("Does this make sense to you now");
		cb.digestSentence("You are so smart and real");
		cb.digestSentence("All boys are real boys");
		cb.digestSentence("This is not a test");
		System.out.println(cb);
		System.out.println(cb.buildSentence());
		System.out.println(cb.buildSentence());

		cb.digestSentence("What have you done with my real boy");
		cb.digestSentence("You can not be a real boy now");
		System.out.println(cb);
		System.out.println(cb.buildSentence());
		System.out.println(cb.buildSentence());
	}

	public static void main(String[] args) {
		System.out.println("Welcome to the Learning Chatbot");
		System.out.println("At any time during the conversation, type");
		System.out.println("   ++done");
		System.out.println("to exit without saving.");
		System.out.println("Or type");
		System.out.println("   ++save");
		System.out.println("to exit while saving the brain.");

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
		private Map<String,ChatWord> observedWords;
		private ChatWord startWord = null;

		public ChatbotBrain() {
			observedWords = new HashMap<String,ChatWord>();
			observedWords.put("\n",ENDWORD);
			startWord = new ChatWord("");
			observedWords.put("",startWord);
		}

		/**
		 * Simple digest method (first edition) that takes a sentence,
		 * cuts it up, and links up the words based on ordering.
		 * Also supports anchoring at beginning and ending to help
		 * the sentence builder pick good sentences.
		 */
		public void digestSentence(String sentence) {
			Scanner scan = new Scanner(sentence);

			ChatWord prior = null;
			ChatWord current = null;
			String currentStr = null;
			while (scan.hasNext()) {
				currentStr = scan.next();
				// first draft, take "words" as is. Punctuation is considered
				// part of the word. Capitalization matters.
				if (observedWords.containsKey(currentStr)) {
					current = observedWords.get(currentStr);
				} else {
					current = new ChatWord(currentStr);
					observedWords.put(currentStr, current);
				}

				if (prior != null) {
					prior.addDescendent(current);
				}
				if (prior == null){
					startWord.addDescendent(current);
				}

				prior = current;
			}

			if (prior != null) { // finalize.
				prior.addDescendent(ENDWORD);
			}
		}

		/**
		 * Uses randomness to pick certain characteristics of the sentence
		 * it will build.
		 */
		public String buildSentence(String hintWord) {
			return hintWord;
		}

		/** 
		 * No hint, pure random.
		 */
		public String buildSentence() {
			int maxDepth = 10+(new Random()).nextInt(15); // Simple cycle prevention ...
			return buildSentence(startWord, 0, maxDepth);
		}

		public String buildSentence(ChatWord word, int curDepth, int maxDepth) {
			SortedMap<Integer, Collection<ChatWord>> roots = word.getDescendents();
			Integer rootMax = roots.lastKey();

			Collection<ChatWord> rootWords = roots.get(rootMax);

			ChatWord pick = null;

			if (rootWords.size() == 1) {
				pick = rootWords.iterator().next(); // first one
			} else {
				//It's late, don't judge.
				while (pick == null) {
					for (ChatWord cw : rootWords) {
						boolean chance = (new Random()).nextBoolean();
						if (chance) {
							pick = cw;
						}
					}
				}
			}

			if (pick.equals(ENDWORD) || curDepth >= maxDepth) {
				return word.getWord();
			} else {
				return word.getWord() + " " + buildSentence(pick, curDepth++, maxDepth);
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
				sb.append(cw.getValue());
			}
			return sb.toString();
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
		private SortedMap<Integer, Collection<Character>> punctuation;
		/** Lookup linking observed punctuation to where they are in ordering */
		private Map<Character, Integer> punctuationLookup;
		/** Punctionation observation count */
		private Integer punctuationCount;
		
		/** Collection of ChatWords observed after this word */
		private SortedMap<Integer, Collection<ChatWord>> firstOrder;
		/** Lookup linking observed words to where they are in ordering */
		private Map<ChatWord, Integer> firstOrderLookup;
		/** First order antecedent word count */
		private Integer firstOrderCount;

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
		protected SortedMap<Integer, Collection<ChatWord>> getDescendents() {
			return firstOrder;
		}

		protected int getDescendentCount() {
			return firstOrderCount;
		}

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

		public String getWord() {
			return word;
		}

		@Override
		public int hashCode() {
			return word.hashCode();
		}

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


