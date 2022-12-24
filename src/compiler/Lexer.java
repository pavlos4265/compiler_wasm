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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import compiler.enums.TokenType;
import compiler.exceptions.LexerException;

public class Lexer {

	private Map<Pattern, TokenType> tokenPatterns;
	private List<Token> tokens;

	private int tokenPos = 0;

	public Lexer(List<String> fileLines) throws LexerException {
		tokenPatterns = loadTokenPatterns();
		tokens = new ArrayList<>();

		for (int i = 0; i < fileLines.size(); i++) {
			String line = fileLines.get(i);

			int charPos = 0;
			while (charPos < line.length()) {
				Token token = matchPattern(line.substring(charPos));
				// System.out.println((token != null) ?token.getType(): "");
				if (token == null)
					throw new LexerException("Unknown symbol", i + 1, charPos);

				token.setLine(i + 1);
				token.setPos(charPos);
				if (token.getType() != TokenType.WHITESPACE && token.getType() != TokenType.COMMENT)
					tokens.add(token);
				charPos += token.getToken().length();
			}
		}

		tokenPatterns.clear();
	}

	private Token matchPattern(String line) {
		for (Pattern pattern : tokenPatterns.keySet()) {
			Matcher matcher = pattern.matcher(line);

			if (matcher.find()) {
				int index = (matcher.groupCount() == 0) ? 0 : 1;
				String data = matcher.group(index);
				return new Token(data, tokenPatterns.get(pattern), 0, 0);
			}
		}

		return null;
	}

	private Map<Pattern, TokenType> loadTokenPatterns() {
		Map<Pattern, TokenType> patterns = new LinkedHashMap<>();

		patterns.put(Pattern.compile("^[\s\t]+"), TokenType.WHITESPACE);
		patterns.put(Pattern.compile("^//.*"), TokenType.COMMENT);

		patterns.put(Pattern.compile("^if"), TokenType.IF);
		patterns.put(Pattern.compile("^else"), TokenType.ELSE);
		patterns.put(Pattern.compile("^while"), TokenType.WHILE);
		patterns.put(Pattern.compile("^return"), TokenType.RETURN);

		patterns.put(Pattern.compile("^void"), TokenType.TYPE);
		patterns.put(Pattern.compile("^i32"), TokenType.TYPE);
		patterns.put(Pattern.compile("^i64"), TokenType.TYPE);
		patterns.put(Pattern.compile("^f32"), TokenType.TYPE);
		patterns.put(Pattern.compile("^f64"), TokenType.TYPE);
		patterns.put(Pattern.compile("^bool"), TokenType.TYPE);

		patterns.put(Pattern.compile("^true"), TokenType.TRUE);
		patterns.put(Pattern.compile("^false"), TokenType.FALSE);

		patterns.put(Pattern.compile("^_wasm"), TokenType.WASM);
		patterns.put(Pattern.compile("^\"[^\"]*\""), TokenType.STRING);
		patterns.put(Pattern.compile("^(0x[a-fA-F0-9]+)"), TokenType.NUMBER);
		patterns.put(Pattern.compile("^(0b[01]+)"), TokenType.NUMBER);
		patterns.put(Pattern.compile("^([0-9]+(\\.[0-9]+)?)"), TokenType.NUMBER);
		patterns.put(Pattern.compile("^[_a-zA-Z0-9]+"), TokenType.IDENTIFIER);

		patterns.put(Pattern.compile("^\\{"), TokenType.LEFT_CURLY_BRACKET);
		patterns.put(Pattern.compile("^\\}"), TokenType.RIGHT_CURLY_BRACKET);
		patterns.put(Pattern.compile("^\\("), TokenType.LEFT_PARENTHESIS);
		patterns.put(Pattern.compile("^\\)"), TokenType.RIGHT_PARENTHESIS);
		patterns.put(Pattern.compile("^,"), TokenType.COMMA);
		patterns.put(Pattern.compile("^;"), TokenType.SEMICOLON);

		patterns.put(Pattern.compile("^!="), TokenType.NOT_EQUAL);
		patterns.put(Pattern.compile("^!"), TokenType.EXCLAMATION);
		patterns.put(Pattern.compile("^=="), TokenType.DOUBLE_EQUALS);
		patterns.put(Pattern.compile("^="), TokenType.EQUALS_SIGN);

		patterns.put(Pattern.compile("^<="), TokenType.LESS_EQUALS);
		patterns.put(Pattern.compile("^<"), TokenType.LESS_THAN);
		patterns.put(Pattern.compile("^>="), TokenType.GREATER_EQUALS);
		patterns.put(Pattern.compile("^>"), TokenType.GREATER_THAN);

		patterns.put(Pattern.compile("^&&"), TokenType.AND);
		patterns.put(Pattern.compile("^&"), TokenType.BIT_AND);
		patterns.put(Pattern.compile("^\\|\\|"), TokenType.OR);
		patterns.put(Pattern.compile("^\\|"), TokenType.BIT_OR);
		patterns.put(Pattern.compile("^\\^"), TokenType.BIT_XOR);

		patterns.put(Pattern.compile("^\\+\\+"), TokenType.PLUSPLUS);
		patterns.put(Pattern.compile("^\\+"), TokenType.PLUS_SIGN);
		patterns.put(Pattern.compile("^--"), TokenType.MINUSMINUS);
		patterns.put(Pattern.compile("^-"), TokenType.MINUS_SIGN);

		patterns.put(Pattern.compile("^/"), TokenType.SLASH_SIGN);
		patterns.put(Pattern.compile("^\\*"), TokenType.ASTERISK_SIGN);

		return patterns;
	}

	public Token nextToken() {
		if (tokenPos >= tokens.size())
			return null;

		Token token = tokens.get(tokenPos);
		tokenPos++;
		return token;
	}

	public Token peek(int steps) {
		steps--;
		if (tokenPos + steps >= tokens.size())
			return null;

		return tokens.get(tokenPos + steps);
	}

	public Token peek() {
		return peek(1);
	}

	public void reset() {
		tokenPos = 0;
	}
}
