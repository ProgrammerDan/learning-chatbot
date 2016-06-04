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

/**
 * Utility class to help with frequency maps.
 */
class FrequencyMap<F,V> implements Serializable {
	private static final long serialVersionUID = 4L;
	private NavigableMap<F, Collection<V>> frequencyMap;
	
	private Map<V,F> frequencyLookup;

	private int size;

	private int remCount;
	private int comThresh;
	/** After how many removals should compact be called. */
	public static final int COMPACT_THRESHOLD = 100;

	/** 
	 * Constructor that sets up a FrequencyMap using default configuration
	 */
	public FrequencyMap() {
		frequencyMap = new TreeMap<F, Collection<V>>();
		frequencyLookup = new HashMap<V, F>();
		size = 0;
		remCount = 0;
		comThresh = COMPACT_THRESHOLD;
	}

	/**
	 * Public method that puts a value into a frequency bucket.
	 * If that value already exists in the map, it is moved from
	 * its prior frequency bucket to the new frequency bucket.
	 */
	public void put(F frequency, V value) {
		if (frequency == null || value == null) {
			throw new IllegalArgumentException("Frequency and values must not be null");
		}
		if (frequencyLookup.containsKey(value)) {
			update(frequency, value);
		} else {
			add(frequency, value);
		}
	}

	/**
	 * Removes a value from the map.
	 */
	public F remove(V value) {
		if (value == null) {
			throw new IllegalArgumentException("Value must not be null");
		}
		F freq = frequencyLookup.get(value);
		delete(value);
		return freq;
	}

	/**
	 * Checks if a value is in the Map.
	 */
	public boolean containsValue(V value) {
		if (value == null) {
			return false;
		}
		return frequencyLookup.containsKey(value);
	}

	/**
	 * Checks if a frequency is in the Map.
	 */
	public boolean containsFrequency(F freq) {
		if (freq == null) {
			return false;
		}
		return frequencyMap.containsKey(freq);
	}

	/**
	 * Gets the frequency of a value in the Map.
	 */
	public F getFrequency(V value) {
		if (value == null) {
			throw new IllegalArgumentException("Value must not be null");
		}
		return frequencyLookup.get(value);
	}

	/**
	 * Gets the collection of values mapping to a specific frequency.
	 */
	public Collection<V> getValues(F freq) {
		if (freq == null) {
			throw new IllegalArgumentException("Frequency must not be null");
		}
		return frequencyMap.get(freq);
	}

	/**
	 * KeySet of the underlying Map.
	 */
	public Set<V> valueKeySet() {
		return frequencyLookup.keySet();
	}

	/**
	 * Descending KeySet of the underlying Navigable Map.
	 */
	public NavigableSet<F> descendingKeySet() {
		return frequencyMap.descendingKeySet();
	}
	
	public Iterable<V> descendingValues() {
		return new Iterable<V>() {
			public Iterator<V> iterator() {
				return new Iterator<V>() {
					// unwind into an ArrayList
					List<V> backing;
					Iterator<V> backingIterator;
					{
						backing = new ArrayList<V>();
						for (F freq : frequencyMap.descendingKeySet()){
							for (V val : frequencyMap.get(freq)){
								backing.add(val);
							}
						}
						backingIterator = backing.iterator();
					}

					public boolean hasNext() {
						return backingIterator.hasNext();
					}

					public V next() {
						return backingIterator.next();
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}

					@Override
					public void finalize() {
						 backing.clear();
					}
				};
			}
		};
	}

	/**
	 * Last frequency (highest value) of the map
	 */
	public F lastKey() {
		return frequencyMap.lastKey();
	}

	/**
	 * Returns the number of Values in the map.
	 */
	public int size() {
		return size;
	}

	/**
	 * Clears the map of all values.
	 */
	public void clear() {
		for (F freq : frequencyMap.keySet()) {
			Collection<V> set = frequencyMap.get(freq);
			set.clear();
		}
		frequencyMap.clear();
		frequencyLookup.clear();
		remCount=0;
		size=0;
	}

	/**
	 * Update a value. Nearly duplicates delete, then add.
	 * Does not delete the entry from lookup to prevent
	 * concurrency problems.
	 */
	private void update(F frequency, V value) {
		F curFrequency = frequencyLookup.get(value);
		Collection<V> curSet = frequencyMap.get(curFrequency);
		curSet.remove(value);
		size--;
		remCount++;
		add(frequency, value);
	}
	/**
	 * Deletes a value from the map.
	 */
	private void delete(V value) {
		F curFrequency = frequencyLookup.get(value);
		Collection<V> curSet = frequencyMap.get(curFrequency);
		curSet.remove(value);
		frequencyLookup.remove(value);
		size--;
		remCount++;
		if (remCount>=comThresh) compact();
	}
	/**
	 * Adds a value to the map.
	 */
	private void add(F frequency, V value) {
		frequencyLookup.put(value, frequency);
		Collection<V> curSet;
		if (frequencyMap.containsKey(frequency)) {
			curSet = frequencyMap.get(frequency);
		} else {
			curSet = new HashSet<V>();
			frequencyMap.put(frequency, curSet);
		}
		curSet.add(value);
		size++;
	}

	/**
	 * Gets rid of empty frequency buckets.
	 */
	private void compact() {
		Set<F> toRemove = new HashSet<F>();
		for(F freq : frequencyMap.keySet()) {
			Collection<V> curSet = frequencyMap.get(freq);
			if (curSet.size() == 0) {
				toRemove.add(freq);
			}
		}
		for(F freq : toRemove) {
			frequencyMap.remove(freq);
		}
		toRemove.clear();
		remCount=0;
	}
}

