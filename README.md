Learning ChatBot
================

My submission for ["who is this chatbot"](http://codegolf.stackexchange.com/questions/20914/who-is-this-chatbot) popularity contest.

I decided to embrace this challenge holistically. My chatbot knows very few things starting out -- no words, no syntax, no nothing. It knows how to parse standard English into words, and how to identify non-word characters as punctuation. That's it. Everything it knows it learns from interaction with the user. As you interact with it, it pays attention to the connections between words, and constructs sentences using that information. Of course, reference the source for more information. I've greatly exceeded the recommended length-of-program expectations of this challenge, but to a good purpose. Here are some highlights of the program:

 * Chatbot starts with no knowledge (follows **"Rules":3**)
 * Frequency of word occurrence is tracked
 * Word frequencies are "decayed" so that conversation can move from topic to topic (follows **"Bonus":3 and 4**)
 * The arrangement of words in observed sentences is recorded, so "phrases" are implicitly kept track of (e.g. if you use a lot of prepositional phrases when chatting with the bot, the bot will use a lot of them too!)
 * Sentences are built by preferring to follow most frequently observed connections between words, with random factors to inject variation
 * The sentence construction algorithm is a Depth First Search, that attempts to maximize occurrence of topic words in the output sentence, with a small preference for ending sentences (this follows **"Bonus":1** -- a pretty damn cool learning algorithm, that shifts over time and retains knowledge of harvested word connections)
     * Topic words are now drawn from both global knowledge of reoccurring words, and from the most recent sentence
     * Words weights are now computed using log base 4 of the word length, so longer words are weighted more strongly, and shorter words, more weakly -- this is to make up for the lack of a true corpus to use in both weighting and eliminating high-frequency, low value words as one can easily do with a corpus.
     * There is a built-in depth maximum to prevent too much looping and too much time spent because of my use of word precedent to build a sentence
     * Loops are detected directly while building a sentence, and while they are technically allowed, there is a high chance that loops will be avoided
     * Tune-able timeout is used to encourage both branch pruning and statement finalization, and also to prevent going past the 5-10 second "acceptable delay" in the rules

To summarize my connection to the rules:

 * For **"Rules":1**, I chose Java, which is verbose, so be gentle.
 * For **"Rules":2**, user input alone is leveraged, although I have some stub code to add brain saving/loading for the future
 * For **"Rules":3**, there is absolutely no pre-set vocabulary. The ChatBot knows how to parse English, but that's it. Starting out, it knows absolutely nothing.
 * For **"Mandatory Criteria":1**, my program is longer, but packs a lot of awesome. I hope you'll overlook.
 * For **"Mandatory Criteria":2**, I have a timeout on my sentence construction algorithm to explicitly prevent more than 5-6 seconds search time. The best sentence so far is returned on timeout.
 * For **"Mandatory Criteria":3**, Topics generally solidify in about 10 sentences, so the Bot will be on-topic by then, and by 20 sentences will be responding to statements with some fascinating random constructs that actually make a bit of sense.
 * For **"Mandatory Criteria":4**, I borrowed nothing from the reference code. This is an entirely unique construction.
 * For **"Bonus":1**, I like to think this bot is quite exceptional. It won't be as convincing as scripted bots, but it has absolutely no limitations on topics, and will move gracefully (with persistence) from conversation topic to topic.
 * For **"Bonus":2**, this is strictly round-robin, so no bonus here. Yet. There's no requirement within my algorithm for response, so I'm planning a Threaded version that will address this bonus.
 * For **"Bonus":3**, initially this bot will mimic, but as the conversation progresses beyond the first few sentences, mimicing will clearly end.
 * For **"Bonus":4**, "moods" aren't processed in any meaningful way, but as the bot preferences topic following, it will shift moods.
 * For **"Bonus":5**, saving and loading brain is not currently in place.

So, I've met all base rules, all mandatory rules, and provisionally bonus rules 1, 3, and 4.

Enjoy!
