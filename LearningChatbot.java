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
		ChatWord test1 = new ChatWord("and");
		ChatWord test2 = new ChatWord("because");
		ChatWord test3 = new ChatWord("never");
		ChatWord test4 = new ChatWord("heavenly");

		test2.addDescendent(test3);
		test3.addPunctuation('.');
		test1.addPunctuation(',');
		test1.addDescendent(test3);
		test1.addDescendent(test3);
		test1.addDescendent(test2);
		test3.addDescendent(test2);
		test3.addDescendent(test3);
		test3.addDescendent(test4);
		test4.addDescendent(test1);
		test4.addDescendent(test1);
		test4.addDescendent(test2);
		test4.addDescendent(test2);
		test4.addDescendent(test1);
		test4.addDescendent(test3);

		System.out.println(test1);
		System.out.println(test2);
		System.out.println(test3);
		System.out.println(test4);
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

	//class ChatbotBrain {


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


