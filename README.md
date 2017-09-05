# CleanTweets

This project includes a UIMA Pipeline and a UIMA CasAnnotator to clean Tweets.

# CleanTokens 

A UIMA CasAnnotator. It detects different kinds of tokens WORD, USER, HASHTAG, URL, RT, EMOJI, COLON, DOTS 
and manages them according to its location in the tweet.

* For emojis it replaces them by the description (in English , different languages should be checked) it uses the emoji-java parser
https://github.com/vdurmont/emoji-java

* For users, it removes the @  and capitalizes the first letter

* For hashtags, it removes the # and it splits it in several words, when in CamelCase

All the changes are done in a secondary CAS: "TargetView"

#TwitterCleaner 
It just reads a file with a text (a specific tweets reader may be necessary) processes it and writes it back to and XMI file



