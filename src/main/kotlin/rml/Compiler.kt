package rml

import prolog.PrologCompiler
import rml.ast.toProlog
import rml.parser.buildSpecificationAst
import rml.parser.rmlLexer
import rml.parser.rmlParser
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.DefaultHelpFormatter
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import org.antlr.v4.runtime.*
import java.io.*


class Args(parser: ArgParser) {
    val input by parser.storing("input file").default<String?>(null)
    val output by parser.storing("output file").default<String?>(null)
}

// see documentation https://github.com/xenomachina/kotlin-argparser
fun main(args: Array<String>) {
    try {
        return mainBody {
            // define help message
            val help = DefaultHelpFormatter("By default the program reads from standard input and writes to standard output")

            ArgParser(args, helpFormatter = help).parseInto(::Args).run {
                val inputStream = if (input != null) File(input).inputStream() else System.`in`
                val outputStream = if (output != null) File(output).outputStream() else System.out
                compile(inputStream, outputStream)
            }
        }
    } catch (e: FileNotFoundException) {
        System.err.println(e.message)
    }
}

fun compile(inputStream: InputStream, outputStream: OutputStream) {
    try {
        val input = CharStreams.fromStream(inputStream)
        val lexer = rmlLexer(input)
        val tokenStream = CommonTokenStream(lexer)
        val parser = rmlParser(tokenStream)
        parser.errorHandler = object: DefaultErrorStrategy() {
            override fun recover(recognizer: Parser?, e: RecognitionException?) {
                throw e!!
            }
        }
        val parseTree = parser.spec()
        val rmlAst = buildSpecificationAst(parseTree)
        val prologAst = toProlog(rmlAst)
        val writer = outputStream.bufferedWriter()
        PrologCompiler(writer).compile(prologAst)
        writer.close()
    } catch (e: IOException) {
        System.err.println(e.message)
    } catch (e: RecognitionException) {
        // error message has already been printed at this point
    }
}
