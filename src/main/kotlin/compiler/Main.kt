package compiler

import compiler.prolog.PrologCompiler
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.DefaultHelpFormatter
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import compiler.calculus.Compiler
import compiler.prolog.ast.Program
import compiler.rml.ast.*
import compiler.rml.parser.buildSpecification
import org.antlr.v4.runtime.*
import rml.parser.RMLLexer
import rml.parser.RMLParser
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
        val lexer = RMLLexer(input)
        val tokenStream = CommonTokenStream(lexer)
        val parser = RMLParser(tokenStream)
        parser.errorHandler = object: DefaultErrorStrategy() {
            override fun recover(recognizer: Parser?, e: RecognitionException?) {
                throw e!!
            }
        }
        val parseTree = parser.specification()
        val rmlAst = buildSpecification(parseTree)
        val calculusAst = CalculusCompiler.compile(rmlAst)
        val declarationsClauses = compile(rmlAst.eventTypeDeclarations)
        val calculusCompiler = Compiler<EventType, DataExpression>(::compile) { de -> compile(de) }
        val specificationClauses = calculusCompiler.compile(calculusAst, "Main")
        val prologAst = Program(directives, declarationsClauses + specificationClauses)
        val writer = outputStream.bufferedWriter()
        PrologCompiler(writer).compile(prologAst)
        writer.close()
    } catch (e: IOException) {
        System.err.println(e.message)
    } catch (e: RecognitionException) {
        // error message has already been printed at this point
    }
}
