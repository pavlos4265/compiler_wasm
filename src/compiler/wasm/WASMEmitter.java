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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import compiler.enums.LiteralType;
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
import compiler.nodes.UnaryOperationNode;
import compiler.nodes.VariableDeclarationNode;
import compiler.nodes.WasmStatementNode;
import compiler.nodes.WhileStatementNode;

public class WASMEmitter {
	private Map<String, Integer> OPCODES;

	private DataOutputStream output;
	private ByteArrayOutputStream baos;

	private Map<String, FunctionStatementNode> funcSymbolTable;
	private Map<String, Map<String, VariableDeclarationNode>> localSymbolTables;

	private WASMVector currentVector;
	private String currentFunctioName;

	// use this to keep the order of the functions in which they appear in the
	// binary
	private Map<String, Integer> orderedFunctions;
	private Map<String, Integer> orderedLocalVars;

	public WASMEmitter(Node node, Map<String, FunctionStatementNode> funcSymbolTable,
			Map<String, Map<String, VariableDeclarationNode>> localSymbolTables) {
		loadOPCodes();

		this.funcSymbolTable = Collections.unmodifiableMap(funcSymbolTable);
		this.localSymbolTables = Collections.unmodifiableMap(localSymbolTables);

		this.orderedFunctions = new HashMap<>();
	}

	public byte[] emit() throws IOException {
		output = new DataOutputStream((baos = new ByteArrayOutputStream()));

		// wasm magic number
		output.write(0x00);
		output.write(0x61);
		output.write(0x73);
		output.write(0x6D);

		// wasm version
		output.write(0x01);
		output.write(0x00);
		output.write(0x00);
		output.write(0x00);

		writeBinaryHeader();
		writeCodeSection();

		return baos.toByteArray();
	}

	private void writeBinaryHeader() throws IOException {
		// TODO: have each unique function signature only once in the type section

		// type section
		output.write(0x01); // type section id - 1
		WASMVector typeVector = new WASMVector(funcSymbolTable.size());
		for (FunctionStatementNode functionNode : funcSymbolTable.values()) {
			WASMVector funcTypeVector = new WASMVector();
			funcTypeVector.addByte(0x60); // funcType id

			WASMVector paramsVector = new WASMVector(functionNode.getArguments().size());
			for (VariableDeclarationNode arg : functionNode.getArguments()) {
				paramsVector.addByte(WASMUtils.GetValType(arg.getType()));
			}
			funcTypeVector.addVector(paramsVector);

			boolean isVoid = LiteralType.getLiteralTypeFromString(functionNode.getType()) == LiteralType._void_;
			WASMVector returnVector = isVoid ? new WASMVector(0) : new WASMVector(1);
			if (!isVoid)
				returnVector.addByte(WASMUtils.GetValType(functionNode.getType()));
			funcTypeVector.addVector(returnVector);

			typeVector.addVector(funcTypeVector);
		}

		WASMUtils.WriteUnsignedLeb128(output, typeVector.getBytes().length);
		output.write(typeVector.getBytes());
		// end of type section

		// function section
		output.write(0x03);
		WASMVector functionVector = new WASMVector(funcSymbolTable.size());
		int i = 0;
		for (FunctionStatementNode functionNode : funcSymbolTable.values()) {
			WASMUtils.WriteUnsignedLeb128(functionVector.getStream(), i);
			orderedFunctions.put(functionNode.getName(), i);
			i++;
		}

		WASMUtils.WriteUnsignedLeb128(output, functionVector.getBytes().length);
		output.write(functionVector.getBytes());
		// end of function section

		// memory section
		output.write(0x05);
		// TODO: setup the memory through compiler options
		WASMVector memoryVector = new WASMVector(1);
		memoryVector.addByte(0x01);
		WASMUtils.WriteUnsignedLeb128(memoryVector.getStream(), 1);
		WASMUtils.WriteUnsignedLeb128(memoryVector.getStream(), 10);

		WASMUtils.WriteUnsignedLeb128(output, memoryVector.getBytes().length);
		output.write(memoryVector.getBytes());
		// end of memory section

		// export section
		output.write(0x07);
		WASMVector exportVector = new WASMVector(funcSymbolTable.size() + 1); // +1 for the memory export

		i = 0;
		for (FunctionStatementNode functionNode : funcSymbolTable.values()) {
			byte[] funcName = functionNode.getName().getBytes();
			WASMVector nameVector = new WASMVector(funcName.length);
			nameVector.getStream().write(funcName);
			exportVector.addVector(nameVector);
			exportVector.addByte(0x00); // func id
			WASMUtils.WriteUnsignedLeb128(exportVector.getStream(), i);

			i++;
		}

		// TODO: setup the memory through compiler options
		String memoryName = "memory";
		WASMVector memNameVector = new WASMVector(memoryName.length());
		memNameVector.getStream().write(memoryName.getBytes());
		exportVector.addVector(memNameVector);
		exportVector.addByte(0x02); // memory export
		WASMUtils.WriteSignedLeb128(exportVector.getStream(), 0);

		WASMUtils.WriteUnsignedLeb128(output, exportVector.getBytes().length);
		output.write(exportVector.getBytes());
		// end of exports section
	}

	private void writeCodeSection() throws IOException {
		// code section
		output.write(0x0A);
		WASMVector codeVector = new WASMVector(funcSymbolTable.size());
		for (FunctionStatementNode functionNode : funcSymbolTable.values()) {
			orderedLocalVars = new HashMap<>();
			WASMVector codeData = new WASMVector();

			currentVector = codeData;
			currentFunctioName = functionNode.getName();
			emitFunctionNode(functionNode);

			codeData.addByte(0x0B); // end of instructions
			WASMUtils.WriteUnsignedLeb128(codeVector.getStream(), codeData.getBytes().length);
			codeVector.addVector(codeData);
		}

		WASMUtils.WriteUnsignedLeb128(output, codeVector.getBytes().length);
		output.write(codeVector.getBytes());
	}

	private LiteralType emitNode(Node node) throws IOException {
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
		} else if (node instanceof CastingExpressionNode) {
			CastingExpressionNode castingNode = (CastingExpressionNode) node;

			return emitCastingNode(castingNode);
		} else {
			System.out.println("Unknown node " + node.getClass().getName());
		}

		return null;
	}

	private LiteralType emitCastingNode(CastingExpressionNode castingNode) throws IOException {
		LiteralType exprType = emitNode(castingNode.getExpression());
		LiteralType castType = LiteralType.getLiteralTypeFromString(castingNode.getType());

		if (exprType == castType)
			return castType;

		if (castType == LiteralType.i64 && exprType == LiteralType.i32)
			currentVector.addByte(OPCODES.get("i64.extend_i32_s"));
		else if (castType == LiteralType.i32 && exprType == LiteralType.i64)
			currentVector.addByte(OPCODES.get("i32.wrap_i64"));
		else if (castType == LiteralType.f32 && exprType == LiteralType.i32)
			currentVector.addByte(OPCODES.get("f32.convert_i32_s"));
		else if (castType == LiteralType.i32 && exprType == LiteralType.f32)
			currentVector.addByte(OPCODES.get("i32.trunc_f32_s"));

		return castType;
	}

	private void emitWasmNode(WasmStatementNode wasmNode) throws IOException {
		String wasmCommand = wasmNode.getWasmCommand().trim();
		String wasm = wasmCommand.substring(1, wasmCommand.length() - 1);

		String cmd = wasm.contains(" ") ? wasm.split(" ")[0] : wasm;
		currentVector.addByte(OPCODES.get(cmd));

		if (wasm.contains(" ")) {
			String[] params = wasm.split(" ");
			for (int i = 1; i < params.length; i++) {
				String param = params[i];

				try {
					int val = Integer.parseInt(param);
					WASMUtils.WriteSignedLeb128(currentVector.getStream(), val);
				} catch (Exception e) {
					WASMUtils.WriteUnsignedLeb128(currentVector.getStream(), orderedLocalVars.get(param.substring(1)));
				}
			}
		}
	}

	private LiteralType emitBooleanNode(BooleanExpressionNode booleanNode) throws IOException {
		currentVector.addByte(OPCODES.get("i32.const"));
		WASMUtils.WriteSignedLeb128(currentVector.getStream(), booleanNode.getValue() ? 1 : 0);

		return LiteralType.i32;
	}

	private void emitReturnNode(ReturnStatementNode returnNode) throws IOException {
		emitNode(returnNode.getExpression());

		currentVector.addByte(OPCODES.get("return"));
	}

	private void emitFunctionNode(FunctionStatementNode functionNode) throws IOException {
		WASMVector varsVector = new WASMVector(
				localSymbolTables.get(functionNode.getName()).size() - functionNode.getArguments().size());
		int i = 0;
		for (VariableDeclarationNode localVar : localSymbolTables.get(functionNode.getName()).values()) {
			orderedLocalVars.put(localVar.getVarName(), i);

			if (i >= functionNode.getArguments().size()) {
				WASMUtils.WriteUnsignedLeb128(varsVector.getStream(), 1);
				varsVector.addByte(WASMUtils.GetValType(localVar.getType()));
			}

			i++;
		}

		currentVector.addVector(varsVector);

		emitNode(functionNode.getStatementBlock());
	}

	private void emitVariableDeclarationNode(VariableDeclarationNode varNode) throws IOException {
		if (varNode.getExpression() != null) {
			emitNode(varNode.getExpression());

			currentVector.addByte(OPCODES.get("local.set"));
			WASMUtils.WriteUnsignedLeb128(currentVector.getStream(), orderedLocalVars.get(varNode.getVarName()));
		}
	}

	private void emitAssignmentNode(AssignExpressionNode assignNode) throws IOException {
		emitNode(assignNode.getRightExpression());

		currentVector.addByte(OPCODES.get("local.set"));
		WASMUtils.WriteUnsignedLeb128(currentVector.getStream(), orderedLocalVars.get(assignNode.getVarName()));
	}

	private LiteralType emitNumberNode(NumberExpressionNode numberNode) throws IOException {
		if (!numberNode.isFloat()) {
			currentVector.addByte(OPCODES.get("i32.const"));
			WASMUtils.WriteSignedLeb128(currentVector.getStream(), (int) numberNode.getNumber());
			return LiteralType.i32;
		}

		currentVector.addByte(OPCODES.get("f32.const"));
		output.writeFloat((float) numberNode.getNumber());
		return LiteralType.f32;
	}

	private void emitWhileNode(WhileStatementNode whileNode) throws IOException {
		currentVector.addByte(OPCODES.get("loop"));
		currentVector.addByte(0x40);
		emitNode(whileNode.getConditionNode());
		currentVector.addByte(OPCODES.get("if"));
		currentVector.addByte(0x40);
		emitNode(whileNode.getStatementBlock());
		currentVector.addByte(OPCODES.get("br"));
		WASMUtils.WriteUnsignedLeb128(currentVector.getStream(), 1);
		currentVector.addByte(OPCODES.get("end"));
		currentVector.addByte(OPCODES.get("end"));
	}

	private void emitIfNode(IfStatementNode ifNode) throws IOException {
		for (int i = 0; i < ifNode.getConditions().size(); i++) {
			if (i != 0)
				currentVector.addByte(OPCODES.get("else"));
			emitNode(ifNode.getConditions().get(i));
			currentVector.addByte(OPCODES.get("if"));
			currentVector.addByte(0x40);
			emitNode(ifNode.getStatementBlocks().get(i));
		}

		// if there is an else block
		if (ifNode.getStatementBlocks().size() > ifNode.getConditions().size()) {
			currentVector.addByte(OPCODES.get("else"));
			emitNode(ifNode.getStatementBlocks().get(ifNode.getStatementBlocks().size() - 1));
		}

		// close the else blocks
		for (int i = 0; i < ifNode.getConditions().size(); i++) {
			currentVector.addByte(OPCODES.get("end"));
		}
	}

	private LiteralType emitFunctionCallNode(FunctionCallExpressionNode funcCallNode) throws IOException {
		for (ExpressionNode arg : funcCallNode.getArguments()) {
			emitNode(arg);
		}

		String funcName = funcCallNode.getFunctionName();
		currentVector.addByte(OPCODES.get("call"));
		WASMUtils.WriteUnsignedLeb128(currentVector.getStream(), orderedFunctions.get(funcName));

		FunctionStatementNode funcNode = funcSymbolTable.get(funcName);
		assert (funcNode != null);

		return getEmittedLiteralType(funcNode.getType());
	}

	private LiteralType emitIdentifierNode(IdentifierExpressionNode idNode) throws IOException {
		currentVector.addByte(OPCODES.get("local.get"));
		WASMUtils.WriteUnsignedLeb128(currentVector.getStream(), orderedLocalVars.get(idNode.getName()));

		return getEmittedLiteralType(localSymbolTables.get(currentFunctioName).get(idNode.getName()).getType());
	}

	private LiteralType emitUnaryOperationNode(UnaryOperationNode unaryNode) throws IOException {
		LiteralType exprType = emitNode(unaryNode.getExpression());

		switch (unaryNode.getUnaryOperationType()) {
		case PLUS:
			break;
		case MINUS:
			currentVector.addByte(OPCODES.get("i32.const"));
			WASMUtils.WriteSignedLeb128(currentVector.getStream(), -1);
			currentVector.addByte(OPCODES.get("i32.mul"));
			break;
		case COMPLEMENT:
			// toggle 0 to 1 or 1 to 0
			currentVector.addByte(OPCODES.get("i32.const"));
			WASMUtils.WriteSignedLeb128(currentVector.getStream(), 1);
			currentVector.addByte(OPCODES.get("i32.xor"));
		}

		return exprType;
	}

	private LiteralType emitBinaryOperationNode(BinaryOperationNode binaryNode) throws IOException {
		LiteralType leftType = emitNode(binaryNode.getLeftExpression());
		LiteralType rightType = emitNode(binaryNode.getRightExpression());

		assert (leftType == rightType);

		LiteralType literalType = leftType;

		switch (binaryNode.getBinaryOperationType()) {
		case ADDITION:
			currentVector.addByte(OPCODES.get(literalType + ".add"));
			break;
		case SUBTRACTION:
			currentVector.addByte(OPCODES.get(literalType + ".sub"));
			break;
		case MULTIPLICATION:
			currentVector.addByte(OPCODES.get(literalType + ".mul"));
			break;
		case DIVISION:
			currentVector.addByte(OPCODES.get(literalType + ".div"));
			break;
		case BIT_AND:
			currentVector.addByte(OPCODES.get(literalType + ".and"));
			break;
		case BIT_OR:
			currentVector.addByte(OPCODES.get(literalType + ".or"));
			break;
		case BIT_XOR:
			currentVector.addByte(OPCODES.get(literalType + ".xor"));
			break;
		case LESS_THAN:
			currentVector.addByte(OPCODES.get(literalType + ".lt"
					+ ((literalType == LiteralType.i32 || literalType == LiteralType.i64) ? "_s" : "")));
			break;
		case LESS_EQUAL:
			currentVector.addByte(OPCODES.get(literalType + ".le"
					+ ((literalType == LiteralType.i32 || literalType == LiteralType.i64) ? "_s" : "")));
			break;
		case GREATER_THAN:
			currentVector.addByte(OPCODES.get(literalType + ".gt"
					+ ((literalType == LiteralType.i32 || literalType == LiteralType.i64) ? "_s" : "")));
			break;
		case GREATER_EQUAL:
			currentVector.addByte(OPCODES.get(literalType + ".ge"
					+ ((literalType == LiteralType.i32 || literalType == LiteralType.i64) ? "_s" : "")));
			break;
		case EQUALS:
			currentVector.addByte(OPCODES.get(literalType + ".eq"));
			break;
		case NOT_EQUAL:
			currentVector.addByte(OPCODES.get(literalType + ".ne"));
			break;
		case AND:
			currentVector.addByte(OPCODES.get(literalType + ".and"));
			break;
		case OR:
			currentVector.addByte(OPCODES.get(literalType + ".or"));
			break;
		}

		return literalType;
	}

	private LiteralType getEmittedLiteralType(String type) {
		if (type.equals("bool"))
			return LiteralType.i32;

		return LiteralType.getLiteralTypeFromString(type);
	}

	private void loadOPCodes() {
		OPCODES = new HashMap<>();

		OPCODES.put("loop", 0x03);
		OPCODES.put("if", 0x04);
		OPCODES.put("else", 0x05);
		OPCODES.put("end", 0x0B);
		OPCODES.put("br", 0X0C);
		OPCODES.put("return", 0X0F);
		OPCODES.put("call", 0x10);

		OPCODES.put("local.get", 0x20);
		OPCODES.put("local.set", 0x21);

		OPCODES.put("i32.load", 0x28);
		OPCODES.put("f32.load", 0x2A);
		OPCODES.put("i32.load8_s", 0x2C);
		OPCODES.put("i32.load16_s", 0x2E);
		OPCODES.put("i32.store", 0x36);
		OPCODES.put("f32.store", 0x38);
		OPCODES.put("i32.store8", 0x3A);
		OPCODES.put("i32.store16", 0x3B);

		OPCODES.put("i32.const", 0x41);
		OPCODES.put("f32.const", 0x43);

		OPCODES.put("i32.eq", 0x46);
		OPCODES.put("i32.ne", 0x47);
		OPCODES.put("i32.lt_s", 0x48);
		OPCODES.put("i32.gt_s", 0x4A);
		OPCODES.put("i32.le_s", 0x4C);
		OPCODES.put("i32.ge_s", 0x4E);

		OPCODES.put("i64.eq", 0x51);
		OPCODES.put("i64.ne", 0x52);
		OPCODES.put("i64.lt_s", 0x53);
		OPCODES.put("i64.gt_s", 0x55);
		OPCODES.put("i64.le_s", 0x57);
		OPCODES.put("i64.ge_s", 0x59);

		OPCODES.put("f32.eq", 0x5B);
		OPCODES.put("f32.ne", 0x5C);
		OPCODES.put("f32.lt", 0x5D);
		OPCODES.put("f32.gt", 0x5E);
		OPCODES.put("f32.le", 0x5F);
		OPCODES.put("f32.ge", 0x60);

		OPCODES.put("i32.add", 0x6A);
		OPCODES.put("i32.sub", 0x6B);
		OPCODES.put("i32.mul", 0x6C);
		OPCODES.put("i32.div", 0x6D); // signed
		OPCODES.put("i32.and", 0x71);
		OPCODES.put("i32.or", 0x72);
		OPCODES.put("i32.xor", 0x73);

		OPCODES.put("i64.add", 0x7C);
		OPCODES.put("i64.sub", 0x7D);
		OPCODES.put("i64.mul", 0x7E);
		OPCODES.put("i64.div", 0x7F); // signed
		OPCODES.put("i64.and", 0x83);
		OPCODES.put("i64.or", 0x84);
		OPCODES.put("i64.xor", 0x85);

		OPCODES.put("f32.add", 0x92);
		OPCODES.put("f32.sub", 0x93);
		OPCODES.put("f32.mul", 0x94);
		OPCODES.put("f32.div", 0x95);

		OPCODES.put("i32.wrap_i64", 0xA7);
		OPCODES.put("i32.trunc_f32_s", 0xA8);
		OPCODES.put("i64.extend_i32_s", 0xAC);
		OPCODES.put("f32.convert_i32_s", 0xB3);
	}
}
