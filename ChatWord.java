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
import java.io.Serializable;
import java.io.IOException;

/**
 * ChatWord allows the creation of words that track how they are
 * connected to other words in a forward fashion. In this way it is
 * possible to construct arbitrary length sentences involving a set
 * of keywords harvested from statements. Trust me, it's possible.
 */
class ChatWord implements Serializable {
	private static final long serialVersionUID = 3L;
	/** The word. */
	private String word;
	/** Collection of punctuation observed after this word */
	private transient NavigableMap<Integer, Collection<Character>> punctuation;
	/** Lookup linking observed punctuation to where they are in ordering */
	private Map<Character, Integer> punctuationLookup;
	/** Punctionation observation count */
	private Integer punctuationCount;
	
	/** Collection of ChatWords observed after this word */
	private transient NavigableMap<Integer, Collection<ChatWord>> firstOrder;
	/** Lookup linking observed words to where they are in ordering */
	private Map<ChatWord, Integer> firstOrderLookup;
	/** First order antecedent word count */
	private Integer firstOrderCount;

	public ChatWord() {
		this.word = ""; // placeholder.

		this.firstOrder = new TreeMap<Integer, Collection<ChatWord>>();
		this.firstOrderLookup = new HashMap<ChatWord, Integer>();
		this.firstOrderCount = 0;

		this.punctuation = new TreeMap<Integer, Collection<Character>>();
		this.punctuationLookup = new HashMap<Character, Integer>();
		this.punctuationCount = 0;
	}

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

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.writeObject(word);
		out.writeObject(punctuationCount);
		out.writeObject(punctuationLookup);
		out.writeObject(firstOrderCount);
		out.writeObject(firstOrderLookup);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		word = (String) in.readObject();
		punctuationCount = (Integer) in.readObject();
		punctuationLookup = (Map<Character, Integer>) in.readObject();
		firstOrderCount = (Integer) in.readObject();
		firstOrderLookup = (Map<ChatWord, Integer>) in.readObject();

		punctuation = new TreeMap<Integer, Collection<Character>>();
		firstOrder = new TreeMap<Integer, Collection<ChatWord>>();
		// Reconstitute Character occurance map
		for (Map.Entry<Character, Integer> c : punctuationLookup.entrySet()) {
			Collection<Character> set = punctuation.get(c.getValue());
			if (set == null) {
				set = new HashSet<Character>();
				punctuation.put(c.getValue(), set);
			}
			set.add(c.getKey());
		}
		
		// Reconstitute ChatWord occurance map
		for (Map.Entry<ChatWord, Integer> c : firstOrderLookup.entrySet()) {
			Collection<ChatWord> set = firstOrder.get(c.getValue());
			if (set == null) {
				set = new HashSet<ChatWord>();
				firstOrder.put(c.getValue(), set);
			}
			set.add(c.getKey());
		}
	}
}
