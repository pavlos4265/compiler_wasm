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
import java.util.List;
import java.util.regex.Pattern;

import compiler.enums.BinaryOperationType;
import compiler.enums.TokenType;
import compiler.enums.UnaryOperationType;
import compiler.exceptions.ParseException;
import compiler.nodes.AssignExpressionNode;
import compiler.nodes.BinaryOperationNode;
import compiler.nodes.BooleanExpressionNode;
import compiler.nodes.CastingExpressionNode;
import compiler.nodes.ExpressionNode;
import compiler.nodes.FunctionCallExpressionNode;
import compiler.nodes.FunctionStatementNode;
import compiler.nodes.IdentifierExpressionNode;
import compiler.nodes.IfStatementNode;
import compiler.nodes.Node;
import compiler.nodes.NumberExpressionNode;
import compiler.nodes.ReturnStatementNode;
import compiler.nodes.StatementBlockNode;
import compiler.nodes.StatementNode;
import compiler.nodes.StringExpressionNode;
import compiler.nodes.UnaryOperationNode;
import compiler.nodes.VariableDeclarationNode;
import compiler.nodes.WasmStatementNode;
import compiler.nodes.WhileStatementNode;

public class Parser {

	private Lexer lexer;

	public Parser(Lexer lexer) {
		this.lexer = lexer;
	}

	private StatementNode parseStatement() throws ParseException {
		Token token = lexer.peek();

		if (token == null)
			return null;

		switch (token.getType()) {
		case WHILE:
			matchToken(TokenType.WHILE);
			matchToken(TokenType.LEFT_PARENTHESIS);
			ExpressionNode whileConditionNode = parseExpression(0);
			matchToken(TokenType.RIGHT_PARENTHESIS);
			matchToken(TokenType.LEFT_CURLY_BRACKET);
			StatementBlockNode whileStatementBlock = parseStatementBlock();
			matchToken(TokenType.RIGHT_CURLY_BRACKET);

			WhileStatementNode whileNode = new WhileStatementNode();
			whileNode.setConditionNode(whileConditionNode);
			whileNode.setStatementBlock(whileStatementBlock);
			whileNode.setLine(token.getLine());
			whileNode.setPos(token.getPos());

			return whileNode;
		case IF:
			List<StatementNode> statementBlocks = new ArrayList<>();
			List<ExpressionNode> conditions = new ArrayList<>();

			boolean isFirstLoop = true;
			while (true) {
				Token peekedToken = lexer.peek();

				boolean isElse = false;
				if (peekedToken.getType() == TokenType.ELSEIF)
					matchToken(TokenType.ELSEIF);
				else if (peekedToken.getType() == TokenType.IF && isFirstLoop)
					matchToken(TokenType.IF);
				else if (peekedToken.getType() == TokenType.ELSE && lexer.peek(2).getType() == TokenType.IF) {
					matchToken(TokenType.ELSE);
					matchToken(TokenType.IF);
				} else if (peekedToken.getType() == TokenType.ELSE) {
					matchToken(TokenType.ELSE);
					isElse = true;
				} else
					break;

				if (!isElse) {
					matchToken(TokenType.LEFT_PARENTHESIS);
					conditions.add(parseExpression(0));
					matchToken(TokenType.RIGHT_PARENTHESIS);
				}

				if (lexer.peek().getType() == TokenType.LEFT_CURLY_BRACKET) {
					matchToken(TokenType.LEFT_CURLY_BRACKET);
					statementBlocks.add(parseStatementBlock());
					matchToken(TokenType.RIGHT_CURLY_BRACKET);
				} else
					statementBlocks.add(parseStatement());

				if (isElse)
					break;

				isFirstLoop = false;
			}

			IfStatementNode ifNode = new IfStatementNode();
			ifNode.setConditions(conditions);
			ifNode.setStatementBlocks(statementBlocks);
			ifNode.setLine(token.getLine());
			ifNode.setPos(token.getPos());
			return ifNode;
		case WASM:
			matchToken(TokenType.WASM);
			Token wasmCommandToken = matchToken(TokenType.STRING);
			matchToken(TokenType.SEMICOLON);

			WasmStatementNode wasmStatementNode = new WasmStatementNode();
			wasmStatementNode.setWasmCommand(wasmCommandToken.getToken());
			return wasmStatementNode;
		case IDENTIFIER:
			matchToken(TokenType.IDENTIFIER);
			switch (lexer.peek().getType()) {
			case EQUALS_SIGN: // assignment
				matchToken(TokenType.EQUALS_SIGN);

				ExpressionNode rightExpression = parseExpression(0);

				AssignExpressionNode assignNode = new AssignExpressionNode();
				assignNode.setVarName(token.getToken());
				assignNode.setRightExpression(rightExpression);
				assignNode.setLine(token.getLine());
				assignNode.setPos(token.getPos());

				matchToken(TokenType.SEMICOLON);

				return assignNode;
			case LEFT_PARENTHESIS:
				matchToken(TokenType.LEFT_PARENTHESIS);

				FunctionCallExpressionNode functionCallNode = new FunctionCallExpressionNode();
				functionCallNode.setFunctionName(token.getToken());
				functionCallNode.setArguments(parseCallArguments());
				functionCallNode.setLine(token.getLine());
				functionCallNode.setPos(token.getPos());

				matchToken(TokenType.RIGHT_PARENTHESIS);
				matchToken(TokenType.SEMICOLON);
				return functionCallNode;

			default:
				throw new ParseException("Bad statement.", token);
			}
		case TYPE:
			switch (lexer.peek(3).getType()) {
			case LEFT_PARENTHESIS:
				Token functionTypeToken = matchToken(TokenType.TYPE);
				Token functionNameToken = matchToken(TokenType.IDENTIFIER);
				matchToken(TokenType.LEFT_PARENTHESIS);
				List<VariableDeclarationNode> argumentsList = parseArguments();
				matchToken(TokenType.RIGHT_PARENTHESIS);
				matchToken(TokenType.LEFT_CURLY_BRACKET);

				StatementBlockNode functionStatementBlock = parseStatementBlock();

				matchToken(TokenType.RIGHT_CURLY_BRACKET);

				FunctionStatementNode functionNode = new FunctionStatementNode();
				functionNode.setName(functionNameToken.getToken());
				functionNode.setType(functionTypeToken.getToken());
				functionNode.setArguments(argumentsList);
				functionNode.setStatementBlock(functionStatementBlock);
				functionNode.setLine(token.getLine());
				functionNode.setPos(token.getPos());

				return functionNode;
			default:
				VariableDeclarationNode variableDeclNode = parseVariableDeclaration();
				matchToken(TokenType.SEMICOLON);
				return variableDeclNode;
			}

		case RETURN:
			matchToken(TokenType.RETURN);
			ExpressionNode exprNode = parseExpression(0);
			ReturnStatementNode returnStatementNode = new ReturnStatementNode();
			returnStatementNode.setLine(token.getLine());
			returnStatementNode.setPos(token.getPos());
			returnStatementNode.setExpression(exprNode);
			matchToken(TokenType.SEMICOLON);
			return returnStatementNode;
		default:
			return null;
		}
	}

	private VariableDeclarationNode parseVariableDeclaration() throws ParseException {
		Token varTypeToken = matchToken(TokenType.TYPE);

		if (lexer.peek().getType() == TokenType.LEFT_SQUARE_BRACKET) {
			matchToken(TokenType.LEFT_SQUARE_BRACKET);
			matchToken(TokenType.RIGHT_SQUARE_BRACKET);
		}

		VariableDeclarationNode variableDeclNode = new VariableDeclarationNode();
		variableDeclNode.setType(varTypeToken.getToken());
		variableDeclNode.setLine(varTypeToken.getLine());
		variableDeclNode.setPos(varTypeToken.getPos());

		Token identifierToken = matchToken(TokenType.IDENTIFIER);

		if (lexer.peek().getType() == TokenType.EQUALS_SIGN) {
			matchToken(TokenType.EQUALS_SIGN);
			variableDeclNode.setExpression(parseExpression(0));
		}

		variableDeclNode.setVarName(identifierToken.getToken());
		return variableDeclNode;
	}

	private List<ExpressionNode> parseCallArguments() throws ParseException {
		List<ExpressionNode> arguments = new ArrayList<>();

		while (lexer.peek().getType() != TokenType.RIGHT_PARENTHESIS) {
			ExpressionNode exprNode = parseExpression(0);
			arguments.add(exprNode);

			if (lexer.peek().getType() != TokenType.COMMA)
				break;

			matchToken(TokenType.COMMA);
		}

		return arguments;
	}

	private List<VariableDeclarationNode> parseArguments() throws ParseException {
		List<VariableDeclarationNode> arguments = new ArrayList<>();

		while (lexer.peek().getType() != TokenType.RIGHT_PARENTHESIS) {
			VariableDeclarationNode varDeclNode = parseVariableDeclaration();
			arguments.add(varDeclNode);

			if (lexer.peek().getType() != TokenType.COMMA)
				break;

			matchToken(TokenType.COMMA);
		}

		return arguments;
	}

	private ExpressionNode parseExpression(int parentPrecedence) throws ParseException {
		Token token = lexer.peek();

		ExpressionNode node;
		switch (token.getType()) {
		case TRUE:
		case FALSE:
			lexer.nextToken();
			BooleanExpressionNode booleanNode = new BooleanExpressionNode();
			booleanNode.setValue(Boolean.valueOf(token.getToken()));
			node = booleanNode;
			break;
		case NUMBER:
			matchToken(TokenType.NUMBER);
			Pattern hexPattern = Pattern.compile("^[+-]?0x");
			Pattern binPattern = Pattern.compile("^[+-]?0b");
			double num;
			if (hexPattern.matcher(token.getToken()).find())
				num = Integer.decode(token.getToken());
			else if (binPattern.matcher(token.getToken()).find())
				num = Integer.parseInt(token.getToken().replace("0b", ""), 2);
			else
				num = Double.parseDouble(token.getToken());

			NumberExpressionNode numberNode = new NumberExpressionNode();
			numberNode.setNumber(num);
			numberNode.setFloat(token.getToken().contains("."));
			node = numberNode;
			break;
		case STRING:
			matchToken(TokenType.STRING);
			StringExpressionNode stringNode = new StringExpressionNode();
			stringNode.setValue(token.getToken());
			node = stringNode;
			break;
		case IDENTIFIER:
			matchToken(TokenType.IDENTIFIER);
			ExpressionNode exprNode = null;
			// is it a function call?
			if (lexer.peek().getType() == TokenType.LEFT_PARENTHESIS) {
				matchToken(TokenType.LEFT_PARENTHESIS);

				FunctionCallExpressionNode functionCallNode = new FunctionCallExpressionNode();
				functionCallNode.setFunctionName(token.getToken());
				functionCallNode.setArguments(parseCallArguments());

				matchToken(TokenType.RIGHT_PARENTHESIS);
				exprNode = functionCallNode;
			} else {
				IdentifierExpressionNode identiferNode = new IdentifierExpressionNode();
				identiferNode.setName(token.getToken());
				exprNode = identiferNode;
			}
			node = exprNode;
			break;
		case PLUS_SIGN:
			Token plusToken = matchToken(TokenType.PLUS_SIGN);
			UnaryOperationNode plusNode = new UnaryOperationNode();
			plusNode.setUnaryOperationType(UnaryOperationType.PLUS);
			plusNode.setExpression(parseExpression(getUnaryPrecedence(getUnaryOperationType(plusToken.getType()))));
			node = plusNode;
			break;
		case MINUS_SIGN:
			Token minusToken = matchToken(TokenType.MINUS_SIGN);
			UnaryOperationNode minusNode = new UnaryOperationNode();
			minusNode.setUnaryOperationType(UnaryOperationType.MINUS);
			minusNode.setExpression(parseExpression(getUnaryPrecedence(getUnaryOperationType(minusToken.getType()))));
			node = minusNode;
			break;
		case EXCLAMATION:
			Token complementToken = matchToken(TokenType.EXCLAMATION);
			UnaryOperationNode complementNode = new UnaryOperationNode();
			complementNode.setUnaryOperationType(UnaryOperationType.COMPLEMENT);
			complementNode.setExpression(
					parseExpression(getUnaryPrecedence(getUnaryOperationType(complementToken.getType()))));
			node = complementNode;
			break;
		case LEFT_PARENTHESIS:
			matchToken(TokenType.LEFT_PARENTHESIS);
			ExpressionNode expression = null;
			switch (lexer.peek().getType()) {
			case TYPE:
				Token castType = matchToken(TokenType.TYPE);
				matchToken(TokenType.RIGHT_PARENTHESIS);

				CastingExpressionNode castingNode = new CastingExpressionNode();
				castingNode.setExpression(parseExpression(6));
				castingNode.setType(castType.getToken());
				expression = castingNode;

				break;
			default:
				expression = parseExpression(0);
				matchToken(TokenType.RIGHT_PARENTHESIS);
			}

			node = expression;
			break;
		default:
			throw new ParseException("Bad expression.", token);
		}

		node.setLine(token.getLine());
		node.setPos(token.getPos());

		while (true) {
			Token symbolToken = lexer.peek();

			BinaryOperationType operationType = getBinaryOperationType(symbolToken.getType());
			if (operationType != null) {
				int precedence = getBinaryPrecedence(operationType);
				if (precedence == 0 || precedence <= parentPrecedence)
					break;
				lexer.nextToken();

				BinaryOperationNode operationNode = new BinaryOperationNode();
				operationNode.setBinaryOperationType(operationType);
				operationNode.setLeftExpression(node);
				operationNode.setRightExpression(parseExpression(precedence));
				operationNode.setLine(symbolToken.getLine());
				operationNode.setPos(symbolToken.getPos());

				node = operationNode;
			} else
				return node;
		}

		return node;
	}

	private UnaryOperationType getUnaryOperationType(TokenType tokenType) {
		switch (tokenType) {
		case MINUS_SIGN:
			return UnaryOperationType.MINUS;
		case PLUS_SIGN:
			return UnaryOperationType.PLUS;
		case EXCLAMATION:
			return UnaryOperationType.COMPLEMENT;
		default:
			return null;
		}
	}

	private BinaryOperationType getBinaryOperationType(TokenType tokenType) {
		switch (tokenType) {
		case PLUS_SIGN:
			return BinaryOperationType.ADDITION;
		case MINUS_SIGN:
			return BinaryOperationType.SUBTRACTION;
		case ASTERISK_SIGN:
			return BinaryOperationType.MULTIPLICATION;
		case SLASH_SIGN:
			return BinaryOperationType.DIVISION;
		case BIT_AND:
			return BinaryOperationType.BIT_AND;
		case BIT_OR:
			return BinaryOperationType.BIT_OR;
		case AND:
			return BinaryOperationType.AND;
		case OR:
			return BinaryOperationType.OR;
		case GREATER_THAN:
			return BinaryOperationType.GREATER_THAN;
		case LESS_THAN:
			return BinaryOperationType.LESS_THAN;
		case GREATER_EQUALS:
			return BinaryOperationType.GREATER_EQUAL;
		case LESS_EQUALS:
			return BinaryOperationType.LESS_EQUAL;
		case DOUBLE_EQUALS:
			return BinaryOperationType.EQUALS;
		case NOT_EQUAL:
			return BinaryOperationType.NOT_EQUAL;
		default:
			return null;
		}
	}

	private int getUnaryPrecedence(UnaryOperationType unaryType) {
		switch (unaryType) {
		case MINUS:
		case PLUS:
		case COMPLEMENT:
			return 7;
		default:
			return 0;
		}
	}

	private int getBinaryPrecedence(BinaryOperationType binaryType) {
		switch (binaryType) {
		case MULTIPLICATION:
		case DIVISION:
			return 5;
		case ADDITION:
		case SUBTRACTION:
			return 4;
		case LESS_THAN:
		case GREATER_THAN:
		case LESS_EQUAL:
		case GREATER_EQUAL:
		case NOT_EQUAL:
		case EQUALS:
			return 3;
		case BIT_AND:
		case BIT_OR:
			return 2;
		case AND:
		case OR:
			return 1;
		default:
			return 0;
		}
	}

	public StatementBlockNode parseStatementBlock() throws ParseException {
		List<StatementNode> statements = new ArrayList<>();

		StatementNode node;
		while ((node = parseStatement()) != null) {
			statements.add(node);
		}

		StatementBlockNode statementBlock = new StatementBlockNode();
		statementBlock.setStatements(statements);
		return statementBlock;
	}

	private Token matchToken(TokenType type) throws ParseException {
		Token token;
		if ((token = lexer.nextToken()) == null || token.getType() != type) {
			throw new ParseException("Wrong token" + ((token != null) ? " (" + token.getType() + ")" : "")
					+ ", was expecting " + type + ".", token);
		}

		return token;
	}

	public void printTree(Node node) {
		node.print(System.out, 0);
	}
}
