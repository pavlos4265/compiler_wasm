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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import compiler.enums.LiteralType;
import compiler.exceptions.AnalysisException;
import compiler.nodes.AssignExpressionNode;
import compiler.nodes.BinaryOperationNode;
import compiler.nodes.BooleanExpressionNode;
import compiler.nodes.CastingExpressionNode;
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

public class Analyzer {

	private Map<String, FunctionStatementNode> funcSymbolTable;
	private Map<String, Map<String, VariableDeclarationNode>> localSymbolTables;
	private String currentFunctionName;

	public void analyzeTree(Node node) throws AnalysisException {
		funcSymbolTable = new HashMap<>();
		localSymbolTables = new HashMap<>();

		analyzeNode(node);
	}

	private LiteralType analyzeNode(Node node) throws AnalysisException {
		if (node instanceof StatementBlockNode) {
			StatementBlockNode statementBlock = (StatementBlockNode) node;
			for (StatementNode statement : statementBlock.getStatements())
				analyzeNode(statement);
		} else if (node instanceof FunctionStatementNode) {
			FunctionStatementNode functionNode = (FunctionStatementNode) node;

			analyzeFunctionNode(functionNode);
		} else if (node instanceof VariableDeclarationNode) {
			VariableDeclarationNode varDeclNode = (VariableDeclarationNode) node;

			analyzeVariableDeclNode(varDeclNode);
		} else if (node instanceof IfStatementNode) {
			IfStatementNode ifNode = (IfStatementNode) node;

			analyzeIfStatementNode(ifNode);
		} else if (node instanceof IdentifierExpressionNode) {
			IdentifierExpressionNode idNode = (IdentifierExpressionNode) node;

			return analyzeIdentifierNode(idNode);
		} else if (node instanceof NumberExpressionNode) {
			NumberExpressionNode numberNode = (NumberExpressionNode) node;

			return analyzeNumberNode(numberNode);
		} else if (node instanceof AssignExpressionNode) {
			AssignExpressionNode assignNode = (AssignExpressionNode) node;

			analyzeAssignmentNode(assignNode);
		} else if (node instanceof BinaryOperationNode) {
			BinaryOperationNode binaryNode = (BinaryOperationNode) node;

			return analyzeBinaryOperationNode(binaryNode);
		} else if (node instanceof UnaryOperationNode) {
			UnaryOperationNode unaryNode = (UnaryOperationNode) node;

			return analyzeUnaryOperationNode(unaryNode);
		} else if (node instanceof BooleanExpressionNode) {
			BooleanExpressionNode booleanNode = (BooleanExpressionNode) node;

			return analyzeBooleanNode(booleanNode);
		} else if (node instanceof ReturnStatementNode) {
			ReturnStatementNode returnNode = (ReturnStatementNode) node;

			analyzeReturnNode(returnNode);
		} else if (node instanceof FunctionCallExpressionNode) {
			FunctionCallExpressionNode functionCallNode = (FunctionCallExpressionNode) node;

			return analyzeFunctionCallNode(functionCallNode);
		} else if (node instanceof WhileStatementNode) {
			WhileStatementNode whileNode = (WhileStatementNode) node;

			analyzeWhileNode(whileNode);
		} else if (node instanceof WasmStatementNode) {
			WasmStatementNode wasmNode = (WasmStatementNode) node;

			analyzeWasmNode(wasmNode);
		} else if (node instanceof CastingExpressionNode) {
			CastingExpressionNode castingNode = (CastingExpressionNode) node;

			return analyzeCastingNode(castingNode);
		} else {
			throw new AnalysisException("Unknown tree node " + node, node);
		}

		return null;
	}

	private LiteralType analyzeCastingNode(CastingExpressionNode castingNode) throws AnalysisException {
		// TODO: analyze if the casting is legal
		analyzeNode(castingNode.getExpression());
		return LiteralType.getLiteralTypeFromString(castingNode.getType());
	}

	private void analyzeWasmNode(WasmStatementNode wasmNode) {
		// TODO: maybe somewhat analyze the wasm instruction
	}

	private void analyzeWhileNode(WhileStatementNode whileNode) throws AnalysisException {
		if (currentFunctionName == null)
			throw new AnalysisException("While statement cannot be declared outside a function body.", whileNode);

		LiteralType conditionType = analyzeNode(whileNode.getConditionNode());
		if (conditionType != LiteralType.bool)
			throw new AnalysisException("Condition was expected.", whileNode.getConditionNode());

		analyzeNode(whileNode.getStatementBlock());
	}

	private LiteralType analyzeFunctionCallNode(FunctionCallExpressionNode functionCallNode) throws AnalysisException {
		if (funcSymbolTable.get(functionCallNode.getFunctionName()) == null)
			throw new AnalysisException("Function " + functionCallNode.getFunctionName() + " is not defined.",
					functionCallNode);

		FunctionStatementNode funcNode = funcSymbolTable.get(functionCallNode.getFunctionName());
		if (funcNode.getArguments().size() != functionCallNode.getArguments().size())
			throw new AnalysisException("Wrong amount of arguments for function " + functionCallNode.getFunctionName(),
					functionCallNode);

		// TODO: check if argument types and function type are actual types

		for (int i = 0; i < funcNode.getArguments().size(); i++) {
			LiteralType argType = analyzeNode(functionCallNode.getArguments().get(i));

			if (argType != LiteralType.getLiteralTypeFromString(funcNode.getArguments().get(i).getType()))
				throw new AnalysisException("Argument of type " + funcNode.getArguments().get(i).getType()
						+ " cannot accept value of type " + argType, functionCallNode.getArguments().get(i));
		}

		return LiteralType.getLiteralTypeFromString(funcNode.getType());
	}

	private void analyzeReturnNode(ReturnStatementNode returnNode) throws AnalysisException {
		if (currentFunctionName == null)
			throw new AnalysisException("Return statement cannot be outside a function body.", returnNode);

		LiteralType exprType = analyzeNode(returnNode.getExpression());

		if (exprType != LiteralType.getLiteralTypeFromString(funcSymbolTable.get(currentFunctionName).getType()))
			throw new AnalysisException("Cannot return value of type " + exprType + " in function of type "
					+ funcSymbolTable.get(currentFunctionName).getType(), returnNode);
	}

	private LiteralType analyzeBooleanNode(BooleanExpressionNode booleanNode) {
		return LiteralType.bool;
	}

	private LiteralType analyzeUnaryOperationNode(UnaryOperationNode unaryNode) throws AnalysisException {
		LiteralType exprType = analyzeNode(unaryNode.getExpression());

		switch (unaryNode.getUnaryOperationType()) {
		case MINUS:
		case PLUS:
			if (exprType == LiteralType.bool)
				throw new AnalysisException(
						"Operation " + unaryNode.getUnaryOperationType() + " cannot be performed on bool type value.",
						unaryNode);
			break;
		case COMPLEMENT:
			if (exprType != LiteralType.bool)
				throw new AnalysisException(
						"Operation " + unaryNode.getUnaryOperationType() + " cannot be performed on type " + exprType,
						unaryNode);
			break;
		}

		return exprType;
	}

	private LiteralType analyzeBinaryOperationNode(BinaryOperationNode binaryNode) throws AnalysisException {
		LiteralType leftType = analyzeNode(binaryNode.getLeftExpression());
		LiteralType rightType = analyzeNode(binaryNode.getRightExpression());

		if (leftType != rightType)
			throw new AnalysisException("Left and right expressions are not the same type in "
					+ binaryNode.getBinaryOperationType() + " operation.", binaryNode);

		switch (binaryNode.getBinaryOperationType()) {
		case ADDITION:
		case SUBTRACTION:
		case DIVISION:
		case MULTIPLICATION:
		case BIT_AND:
		case BIT_OR:
		case BIT_XOR:
			if (leftType == LiteralType.bool)
				throw new AnalysisException(
						"Operation " + binaryNode.getBinaryOperationType() + " cannot be performed on bool type value.",
						binaryNode);

			return leftType;
		case GREATER_EQUAL:
		case GREATER_THAN:
		case LESS_EQUAL:
		case LESS_THAN:
			if (leftType == LiteralType.bool)
				throw new AnalysisException(
						"Operation " + binaryNode.getBinaryOperationType() + " cannot be performed on bool type value.",
						binaryNode);

			return LiteralType.bool;
		case AND:
		case OR:
			if (leftType != LiteralType.bool)
				throw new AnalysisException(
						"Operation " + binaryNode.getBinaryOperationType() + " cannot be performed on type " + leftType,
						binaryNode);

			return LiteralType.bool;
		case EQUALS:
		case NOT_EQUAL:
			return LiteralType.bool;
		}

		return null;
	}

	private void analyzeAssignmentNode(AssignExpressionNode assignNode) throws AnalysisException {
		if (currentFunctionName == null)
			throw new AnalysisException("Assignment cannot be outside a function body.", assignNode);

		Map<String, VariableDeclarationNode> localSymbolTable = localSymbolTables.get(currentFunctionName);

		if (localSymbolTable.get(assignNode.getVarName()) == null)
			throw new AnalysisException("Variable " + assignNode.getVarName() + " is not declared.", assignNode);

		VariableDeclarationNode varDeclNode = localSymbolTable.get(assignNode.getVarName());
		LiteralType exprType = analyzeNode(assignNode.getRightExpression());
		if (exprType != LiteralType.getLiteralTypeFromString(varDeclNode.getType()))
			throw new AnalysisException("Variable " + assignNode.getVarName() + " of type " + varDeclNode.getType()
					+ " cannot be assigned a value of type " + exprType, assignNode);
	}

	private LiteralType analyzeNumberNode(NumberExpressionNode numberNode) {
		// TODO: maybe handle i64/f64
		if (!numberNode.isFloat())
			return LiteralType.i32;

		return LiteralType.f32;
	}

	private LiteralType analyzeIdentifierNode(IdentifierExpressionNode idNode) throws AnalysisException {
		Map<String, VariableDeclarationNode> localSymbolTable = localSymbolTables.get(currentFunctionName);

		if (localSymbolTable.get(idNode.getName()) == null)
			throw new AnalysisException("Variable " + idNode.getName() + " is not declared.", idNode);

		VariableDeclarationNode varDeclNode = localSymbolTable.get(idNode.getName());

		// TODO: check if variable is initialized

		return LiteralType.getLiteralTypeFromString(varDeclNode.getType());
	}

	private void analyzeIfStatementNode(IfStatementNode ifNode) throws AnalysisException {
		if (currentFunctionName == null)
			throw new AnalysisException("If statement cannot be declared outside a function body.", ifNode);

		for (int i = 0; i < ifNode.getStatementBlocks().size(); i++) {
			if (i != ifNode.getConditions().size()) {
				LiteralType conditionType = analyzeNode(ifNode.getConditions().get(i));
				if (conditionType != LiteralType.bool)
					throw new AnalysisException("Condition was expected.", ifNode.getConditions().get(i));
			}

			analyzeNode(ifNode.getStatementBlocks().get(i));
		}
	}

	private void analyzeVariableDeclNode(VariableDeclarationNode varDeclNode) throws AnalysisException {
		if (currentFunctionName == null)
			throw new AnalysisException("A variable cannot be declared outside a function body.", varDeclNode);

		Map<String, VariableDeclarationNode> localSymbolTable = localSymbolTables.get(currentFunctionName);
		if (localSymbolTable.get(varDeclNode.getVarName()) != null)
			throw new AnalysisException("Variable " + varDeclNode.getVarName() + " is already declared.", varDeclNode);

		if (LiteralType.getLiteralTypeFromString(varDeclNode.getType()) == null)
			throw new AnalysisException("Unknown value type " + varDeclNode.getType(), varDeclNode);

		if (LiteralType.getLiteralTypeFromString(varDeclNode.getType()) == LiteralType._void_)
			throw new AnalysisException("Value of type " + varDeclNode.getType() + " cannot be declared.", varDeclNode);

		if (varDeclNode.getExpression() != null) {
			LiteralType exprType = analyzeNode(varDeclNode.getExpression());

			if (LiteralType.getLiteralTypeFromString(varDeclNode.getType()) != exprType)
				throw new AnalysisException("Variable " + varDeclNode.getVarName() + " of type " + varDeclNode.getType()
						+ " cannot be assigned a value of type " + exprType, varDeclNode);
		}

		localSymbolTable.put(varDeclNode.getVarName(), varDeclNode);
	}

	private void analyzeFunctionNode(FunctionStatementNode functionNode) throws AnalysisException {
		if (currentFunctionName != null)
			throw new AnalysisException("Function cannot be declared inside another function.", functionNode);

		if (funcSymbolTable.get(functionNode.getName()) != null)
			throw new AnalysisException("Function " + functionNode.getName() + " is already declared.", functionNode);

		funcSymbolTable.put(functionNode.getName(), functionNode);

		Map<String, VariableDeclarationNode> localSymbolTable = new LinkedHashMap<>();

		for (VariableDeclarationNode varDeclNode : functionNode.getArguments()) {
			if (localSymbolTable.get(varDeclNode.getVarName()) != null)
				throw new AnalysisException("There is already an argument with the name " + varDeclNode.getVarName(),
						varDeclNode);

			if (varDeclNode.getExpression() != null)
				throw new AnalysisException("A value cannot be assigned to an argument in a function signature.",
						varDeclNode);

			localSymbolTable.put(varDeclNode.getVarName(), varDeclNode);
		}

		String functionName = functionNode.getName();
		localSymbolTables.put(functionName, localSymbolTable);
		currentFunctionName = functionName;

		analyzeNode(functionNode.getStatementBlock());

		currentFunctionName = null;
	}

	public Map<String, Map<String, VariableDeclarationNode>> getLocalSymbolTables() {
		return localSymbolTables;
	}

	public Map<String, FunctionStatementNode> getFuncSymbolTable() {
		return funcSymbolTable;
	}
}
