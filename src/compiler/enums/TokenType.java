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
package compiler.enums;

public enum TokenType {
	WHITESPACE, LEFT_CURLY_BRACKET, // {
	RIGHT_CURLY_BRACKET, // }
	SEMICOLON, // ;
	COLON, // :
	EQUALS_SIGN, // =
	COMMA, // ,
	EXCLAMATION, // !
	LEFT_SQUARE_BRACKET, // [
	RIGHT_SQUARE_BRACKET, // ]
	LEFT_PARENTHESIS, // (
	RIGHT_PARENTHESIS, // )
	PLUS_SIGN, // +
	MINUS_SIGN, // -
	ASTERISK_SIGN, // *
	SLASH_SIGN, // /
	PLUSPLUS, // ++
	MINUSMINUS, // --
	LESS_THAN, // <
	GREATER_THAN, // >
	LESS_EQUALS, // <=
	GREATER_EQUALS, // >=
	DOUBLE_EQUALS, // ==
	NOT_EQUAL, // !=
	AND, // &&
	OR, // ||
	BIT_AND, // &
	BIT_OR, // |
	BIT_XOR, // ^
	TRUE, // true
	FALSE, // false
	TYPE, IDENTIFIER, NUMBER, STRING, WASM, IF, // if
	ELSE, // else
	ELSEIF, // eif
	WHILE, // while
	RETURN, // return
	COMMENT
}
