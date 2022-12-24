/*   
 * Copyright 2022 pavlos4265
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import compiler.exceptions.AnalysisException;
import compiler.exceptions.LexerException;
import compiler.exceptions.ParseException;
import compiler.exceptions.PreprocessException;
import compiler.nodes.Node;
import compiler.wasm.WASMEmitter;

public class MainCompiler {

	public static void main(String[] args) throws AnalysisException {
		try {
			new MainCompiler(args);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PreprocessException e) {
			e.printStackTrace();
		} catch (LexerException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}

	private MainCompiler(String[] args)
			throws IOException, PreprocessException, LexerException, ParseException, AnalysisException {
		Map<String, String> options = parseOptions(args);

		if (options.get("input") == null)
			throw new IllegalArgumentException("An input file is not provided.");

		File f = new File(options.get("input"));

		if (!f.exists() || f.isDirectory())
			throw new IllegalArgumentException("The input file does not exist.");

		List<String> fileLines = Files.readAllLines(f.toPath());

		Preprocessor preprocessor = new Preprocessor();
		fileLines = preprocessor.process(fileLines);

		Lexer lexer = new Lexer(fileLines);

		if (options.get("showTokens") != null) {
			Token token;
			while ((token = lexer.nextToken()) != null) {
				System.out.println("token: " + token.getToken() + " type: " + token.getType());
			}
			lexer.reset();
		}

		Parser parser = new Parser(lexer);
		Node node = parser.parseStatementBlock();

		if (options.get("showTree") != null)
			parser.printTree(node);

		Analyzer analyzer = new Analyzer();
		analyzer.analyzeTree(node);

		/*
		 * if (options.get("showWat") != null) { WATEmitter emitter = new
		 * WATEmitter(node, analyzer.getFuncSymbolTable(),
		 * analyzer.getLocalSymbolTables()); List<String> instructions = emitter.emit();
		 * printInstructions(System.out, instructions); }
		 */

		if ((options.get("output") != null)) {
			WASMEmitter wasmEmitter = new WASMEmitter(node, analyzer.getFuncSymbolTable(),
					analyzer.getLocalSymbolTables());
			byte[] bin = wasmEmitter.emit();
			OutputStream out = new FileOutputStream(options.get("output"));
			out.write(bin);
			out.close();
		}
	}

	/*
	 * private void printInstructions(PrintStream out, List<String> instructions) {
	 * int depth = 0; for(String instruction: instructions) { if
	 * (instruction.startsWith(")")) depth--;
	 * 
	 * String tabs = ""; for (int i = 0; i < depth; i++) tabs += "\t";
	 * out.println(tabs+instruction);
	 * 
	 * if (!instruction.startsWith("(func") && instruction.startsWith("(") &&
	 * instruction.endsWith(")")) continue;
	 * 
	 * if (instruction.startsWith("(")) depth++; } }
	 */

	private Map<String, String> parseOptions(String[] args) {
		Map<String, String> options = new HashMap<>();

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (arg.equalsIgnoreCase("-i")) {
				String value = args[++i];

				options.put("input", value);
			} else if (arg.equalsIgnoreCase("-o")) {
				String value = args[++i];

				options.put("output", value);
			} else if (arg.equalsIgnoreCase("-tree")) {
				options.put("showTree", "");
			} else if (arg.equalsIgnoreCase("-tokens")) {
				options.put("showTokens", "");
			} else if (arg.equalsIgnoreCase("-wat")) {
				options.put("showWat", "");
			}
		}

		return options;
	}
}
