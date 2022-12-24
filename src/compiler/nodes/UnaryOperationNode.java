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

import compiler.enums.UnaryOperationType;

public class UnaryOperationNode extends ExpressionNode {

	private ExpressionNode expression;
	private UnaryOperationType unaryOperationType;

	public ExpressionNode getExpression() {
		return expression;
	}

	public void setExpression(ExpressionNode expression) {
		this.expression = expression;
	}

	public UnaryOperationType getUnaryOperationType() {
		return unaryOperationType;
	}

	public void setUnaryOperationType(UnaryOperationType unaryOperationType) {
		this.unaryOperationType = unaryOperationType;
	}

	public String toString() {
		return super.toString() + " (" + unaryOperationType + ")";
	}

	@Override
	public void print(PrintStream out, int depth) {
		String tabs = "";
		for (int i = 0; i < depth; i++)
			tabs += "\t";

		out.println(tabs + this);
		expression.print(out, depth + 1);
	}
}
