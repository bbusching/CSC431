import cfg.LLVMEmitter;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.*;

public class MiniCompiler {

    private static String _inputFile = null;
    private static boolean llvm = false;

    public static void main(String[] args) throws TypeChecker.TypeCheckerException {
        parseParameters(args);

        CommonTokenStream tokens = new CommonTokenStream(createLexer());
        MiniParser parser = new MiniParser(tokens);
        ParseTree tree = parser.program();

        if (parser.getNumberOfSyntaxErrors() == 0) {
         /*
            This visitor will build an object representation of the AST
            in Java using the provided classes.
         */
            MiniToAstProgramVisitor programVisitor =
                    new MiniToAstProgramVisitor();
            ast.Program program = programVisitor.visit(tree);
            TypeChecker.checkSemantics(program);
            if (llvm) {
                String filename = _inputFile.lastIndexOf('.') >= 0 ?
                        String.format("%s.ll", _inputFile.substring(0, _inputFile.lastIndexOf('.')))
                        : String.format("%s.ll", _inputFile);
                LLVMEmitter.writeLlvm(program, filename);
            }
        }
    }

    private static void parseParameters(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if ("llvm".equals(args[i].substring(1))) {
                    llvm = true;
                } else {
                    System.err.println("unexpected option: " + args[i]);
                    System.exit(1);
                }
            } else if (_inputFile != null) {
                System.err.println("too many files specified");
                System.exit(1);
            } else {
                _inputFile = args[i];
            }
        }
    }

    private static void error(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    private static MiniLexer createLexer() {
        try {
            ANTLRInputStream input;
            if (_inputFile == null) {
                input = new ANTLRInputStream(System.in);
            } else {
                input = new ANTLRInputStream(
                        new BufferedInputStream(new FileInputStream(_inputFile)));
            }
            return new MiniLexer(input);
        } catch (java.io.IOException e) {
            System.err.println("file not found: " + _inputFile);
            System.exit(1);
            return null;
        }
    }
}
