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
package compiler.wasm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import compiler.enums.LiteralType;
import compiler.nodes.AssignExpressionNode;
import compiler.nodes.BinaryOperationNode;
import compiler.nodes.BooleanExpressionNode;
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
import compiler.nodes.UnaryOperationNode;
import compiler.nodes.VariableDeclarationNode;
import compiler.nodes.WasmStatementNode;
import compiler.nodes.WhileStatementNode;

@Deprecated
public class WATEmitter {

	private Node node;

	private List<String> instructions;

	private Map<String, FunctionStatementNode> funcSymbolTable;
	private Map<String, Map<String, VariableDeclarationNode>> localSymbolTables;

	private String currentFunctionName;

	public WATEmitter(Node node, Map<String, FunctionStatementNode> funcSymbolTable,
			Map<String, Map<String, VariableDeclarationNode>> localSymbolTables) {
		this.node = node;
		this.funcSymbolTable = Collections.unmodifiableMap(funcSymbolTable);
		this.localSymbolTables = Collections.unmodifiableMap(localSymbolTables);
	}

	public List<String> emit() {
		instructions = new ArrayList<String>();

		instructions.add("(module");
		emitNode(node);
		instructions.add(")");

		return instructions;
	}

	private LiteralType emitNode(Node node) {
		if (node instanceof StatementBlockNode) {
			StatementBlockNode statementBlock = (StatementBlockNode) node;

			for (StatementNode statement : statementBlock.getStatements()) {
				emitNode(statement);
			}
		} else if (node instanceof FunctionStatementNode) {
			FunctionStatementNode functionNode = (FunctionStatementNode) node;

			emitFunctionNode(functionNode);
		} else if (node instanceof VariableDeclarationNode) {
			VariableDeclarationNode varNode = (VariableDeclarationNode) node;

			emitVariableDeclarationNode(varNode);
		} else if (node instanceof AssignExpressionNode) {
			AssignExpressionNode assignNode = (AssignExpressionNode) node;

			emitAssignmentNode(assignNode);
		} else if (node instanceof NumberExpressionNode) {
			NumberExpressionNode numberNode = (NumberExpressionNode) node;

			return emitNumberNode(numberNode);
		} else if (node instanceof WhileStatementNode) {
			WhileStatementNode whileNode = (WhileStatementNode) node;

			emitWhileNode(whileNode);
		} else if (node instanceof IfStatementNode) {
			IfStatementNode ifNode = (IfStatementNode) node;

			emitIfNode(ifNode);
		} else if (node instanceof FunctionCallExpressionNode) {
			FunctionCallExpressionNode funcCallNode = (FunctionCallExpressionNode) node;

			return emitFunctionCallNode(funcCallNode);
		} else if (node instanceof IdentifierExpressionNode) {
			IdentifierExpressionNode idNode = (IdentifierExpressionNode) node;

			return emitIdentifierNode(idNode);
		} else if (node instanceof BinaryOperationNode) {
			BinaryOperationNode binaryNode = (BinaryOperationNode) node;

			return emitBinaryOperationNode(binaryNode);
		} else if (node instanceof BooleanExpressionNode) {
			BooleanExpressionNode booleanNode = (BooleanExpressionNode) node;

			return emitBooleanNode(booleanNode);
		} else if (node instanceof UnaryOperationNode) {
			UnaryOperationNode unaryNode = (UnaryOperationNode) node;

			return emitUnaryOperationNode(unaryNode);
		} else if (node instanceof ReturnStatementNode) {
			ReturnStatementNode returnNode = (ReturnStatementNode) node;

			emitReturnNode(returnNode);
		} else if (node instanceof WasmStatementNode) {
			WasmStatementNode wasmNode = (WasmStatementNode) node;

			emitWasmNode(wasmNode);
		} else {
			System.out.println("Unknown node " + node.getClass().getName());
		}

		return null;
	}

	private void emitWasmNode(WasmStatementNode wasmNode) {
		instructions.add(wasmNode.getWasmCommand().substring(1, wasmNode.getWasmCommand().length() - 1));
	}

	private LiteralType emitBooleanNode(BooleanExpressionNode booleanNode) {
		if (booleanNode.getValue())
			instructions.add("i32.const 1");
		else
			instructions.add("i32.const 0");

		return LiteralType.i32;
	}

	private void emitReturnNode(ReturnStatementNode returnNode) {
		emitNode(returnNode.getExpression());
		instructions.add("return");
	}

	private void emitFunctionNode(FunctionStatementNode functionNode) {
		String funcName = functionNode.getName();

		String instruction = "(func $" + funcName + " (export \"" + funcName + "\")";

		for (VariableDeclarationNode arg : functionNode.getArguments()) {
			instruction += " (param $" + arg.getVarName() + " " + getEmittedLiteralType(arg.getType()) + ")";
		}

		LiteralType funcType = getEmittedLiteralType(functionNode.getType());
		if (funcType != LiteralType._void_)
			instruction += " (result " + funcType + ")";
		instructions.add(instruction);

		int i = 0;
		for (String localVar : localSymbolTables.get(funcName).keySet()) {
			// ignore the arguments since they are already declared
			if (i < functionNode.getArguments().size()) {
				i++;
				continue;
			}

			VariableDeclarationNode varDeclNode = localSymbolTables.get(funcName).get(localVar);
			instructions.add(
					"(local $" + varDeclNode.getVarName() + " " + getEmittedLiteralType(varDeclNode.getType()) + ")");

			i++;
		}

		currentFunctionName = funcName;
		emitNode(functionNode.getStatementBlock());
		currentFunctionName = null;

		instructions.add(")");
	}

	private void emitVariableDeclarationNode(VariableDeclarationNode varNode) {
		assert (currentFunctionName != null);

		Map<String, VariableDeclarationNode> localSymbolTable = localSymbolTables.get(currentFunctionName);
		assert (localSymbolTable != null);

		if (varNode.getExpression() != null) {
			emitNode(varNode.getExpression());
			instructions.add("local.set $" + varNode.getVarName());
		}
	}

	private void emitAssignmentNode(AssignExpressionNode assignNode) {
		emitNode(assignNode.getRightExpression());
		instructions.add("local.set $" + assignNode.getVarName());
	}

	private LiteralType emitNumberNode(NumberExpressionNode numberNode) {
		if (!numberNode.isFloat()) {
			instructions.add("i32.const " + (int) numberNode.getNumber());
			return LiteralType.i32;
		}

		instructions.add("f32.const " + numberNode.getNumber());
		return LiteralType.f32;
	}

	private void emitWhileNode(WhileStatementNode whileNode) {
		// use the instruction count as a unique identifier for the while block
		int id = instructions.size();
		instructions.add("(loop $while" + id);

		emitNode(whileNode.getConditionNode());

		instructions.add("(if");
		instructions.add("(then");

		emitNode(whileNode.getStatementBlock());

		instructions.add("br $while" + id);
		instructions.add(")");
		instructions.add(")");

		instructions.add(")");
	}

	private void emitIfNode(IfStatementNode ifNode) {
		for (int i = 0; i < ifNode.getConditions().size(); i++) {
			if (i != 0)
				instructions.add("(else");
			emitNode(ifNode.getConditions().get(i));
			instructions.add("(if");
			instructions.add("(then");
			emitNode(ifNode.getStatementBlocks().get(i));
			instructions.add(")");
		}

		// if there is an else block
		if (ifNode.getStatementBlocks().size() > ifNode.getConditions().size()) {
			instructions.add("(else");
			emitNode(ifNode.getStatementBlocks().get(ifNode.getStatementBlocks().size() - 1));
			instructions.add(")");
		}

		// close the if/else blocks
		for (int i = 0; i < ifNode.getConditions().size(); i++) {
			if (i != 0)
				instructions.add(")");

			instructions.add(")");
		}
	}

	private LiteralType emitFunctionCallNode(FunctionCallExpressionNode funcCallNode) {
		for (ExpressionNode arg : funcCallNode.getArguments()) {
			emitNode(arg);
		}

		String funcName = funcCallNode.getFunctionName();
		instructions.add("call $" + funcName);

		FunctionStatementNode funcNode = funcSymbolTable.get(funcName);
		assert (funcNode != null);

		return getEmittedLiteralType(funcNode.getType());
	}

	private LiteralType emitIdentifierNode(IdentifierExpressionNode idNode) {
		instructions.add("local.get $" + idNode.getName());

		assert (currentFunctionName != null);

		Map<String, VariableDeclarationNode> localSymbolTable = localSymbolTables.get(currentFunctionName);
		assert (localSymbolTable != null);

		VariableDeclarationNode varNode = localSymbolTable.get(idNode.getName());
		assert (varNode != null);

		return getEmittedLiteralType(varNode.getType());
	}

	private LiteralType emitUnaryOperationNode(UnaryOperationNode unaryNode) {
		LiteralType exprType = emitNode(unaryNode.getExpression());

		switch (unaryNode.getUnaryOperationType()) {
		case PLUS:
			break;
		case MINUS:
			instructions.add(exprType + ".const -1");
			instructions.add(exprType + ".mul");
			break;
		case COMPLEMENT:
			// toggle 0 to 1 or 1 to 0
			instructions.add(exprType + ".const 1");
			instructions.add(exprType + ".xor");
		}

		return exprType;
	}

	private LiteralType emitBinaryOperationNode(BinaryOperationNode binaryNode) {
		LiteralType leftType = emitNode(binaryNode.getLeftExpression());
		LiteralType rightType = emitNode(binaryNode.getRightExpression());

		assert (leftType == rightType);

		LiteralType literalType = leftType;

		switch (binaryNode.getBinaryOperationType()) {
		case ADDITION:
			instructions.add(literalType + ".add");
			break;
		case SUBTRACTION:
			instructions.add(literalType + ".sub");
			break;
		case MULTIPLICATION:
			instructions.add(literalType + ".mul");
			break;
		case DIVISION:
			instructions.add(literalType + ".div");
			break;
		case BIT_AND:
			instructions.add(literalType + ".and");
			break;
		case BIT_OR:
			instructions.add(literalType + ".or");
			break;
		case BIT_XOR:
			instructions.add(literalType + ".xor");
			break;
		case LESS_THAN:
			instructions.add(literalType + ".lt" + ((literalType == LiteralType.i32) ? "_s" : ""));
			break;
		case LESS_EQUAL:
			instructions.add(literalType + ".le" + ((literalType == LiteralType.i32) ? "_s" : ""));
			break;
		case GREATER_THAN:
			instructions.add(literalType + ".gt" + ((literalType == LiteralType.i32) ? "_s" : ""));
			break;
		case GREATER_EQUAL:
			instructions.add(literalType + ".ge" + ((literalType == LiteralType.i32) ? "_s" : ""));
			break;
		case EQUALS:
			instructions.add(literalType + ".eq");
			break;
		case NOT_EQUAL:
			instructions.add(literalType + ".ne");
			break;
		case AND:
			instructions.add(literalType + ".and");
			break;
		case OR:
			instructions.add(literalType + ".or");
			break;
		}

		return literalType;
	}

	private LiteralType getEmittedLiteralType(String type) {
		if (type.equals("bool"))
			return LiteralType.i32;

		return LiteralType.getLiteralTypeFromString(type);
	}
}
