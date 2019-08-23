# compiler

The complier allows **RML** specifications to be translated into Prolog programs.
The compiler is written in ANTLR, Java and Kotlin.

To build the compiler, type:

    $ ./gradlew build

An executable JAR will be produced in `build/libs`.
To run the compiler:

    $ java -jar build/libs/rml-compiler.jar

By default it will read a specification from standard input and print Prolog code to standard output.
Run with `--help` for options.
