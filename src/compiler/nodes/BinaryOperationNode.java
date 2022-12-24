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
package compiler.nodes;

import java.io.PrintStream;

import compiler.enums.BinaryOperationType;

public class BinaryOperationNode extends ExpressionNode {

	private BinaryOperationType binaryOperationType;

	private ExpressionNode leftExpression;
	private ExpressionNode rightExpression;

	public ExpressionNode getLeftExpression() {
		return leftExpression;
	}

	public void setLeftExpression(ExpressionNode leftExpression) {
		this.leftExpression = leftExpression;
	}

	public ExpressionNode getRightExpression() {
		return rightExpression;
	}

	public void setRightExpression(ExpressionNode rightExpression) {
		this.rightExpression = rightExpression;
	}

	public BinaryOperationType getBinaryOperationType() {
		return binaryOperationType;
	}

	public void setBinaryOperationType(BinaryOperationType binaryOperationType) {
		this.binaryOperationType = binaryOperationType;
	}

	public String toString() {
		return super.toString() + " (" + binaryOperationType + ")";
	}

	@Override
	public void print(PrintStream out, int depth) {
		String tabs = "";
		for (int i = 0; i < depth; i++)
			tabs += "\t";

		out.println(tabs + this);

		leftExpression.print(out, depth + 1);
		rightExpression.print(out, depth + 1);
	}
}
