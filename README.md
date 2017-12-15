# WordGuess

WordGuess is a word-guessing party game for 2 to 6 players in an Android app.

## Usage

Before building and running, please:

1. Configure Firebase.

    * Create a new project named "WordGuess" using the [Firebase console][1].

    * Add Firebase to a new Android app using the package name `com.example.jiunwei.wordguess`. Download the generated `google-services.json` file and copy it into the `app` directory.

    * Under "Authentication" and "Sign-In Method", enable "Anonymous" as a sign-in provider. 

2. Optionally provide a valid Wordnik API key in the static `API_KEY` field of class `com.example.jiunwei.wordguess.util.WordUtil`. API keys can be requested from the [Wordnik developer site][2]. If `API_KEY` is set to `null`, the app will still function by using Wordnik's demo API key.

## License

The MIT License (MIT)

Copyright (c) 2017 Jiun Wei Chia

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

[1]: https://console.firebase.google.com/
[2]: http://developer.wordnik.com/
