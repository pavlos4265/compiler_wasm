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

public class WhileStatementNode extends StatementNode {

	private ExpressionNode conditionNode;
	private StatementBlockNode statementBlock;

	public ExpressionNode getConditionNode() {
		return conditionNode;
	}

	public void setConditionNode(ExpressionNode conditionNode) {
		this.conditionNode = conditionNode;
	}

	public StatementBlockNode getStatementBlock() {
		return statementBlock;
	}

	public void setStatementBlock(StatementBlockNode statementBlock) {
		this.statementBlock = statementBlock;
	}

	@Override
	public void print(PrintStream out, int depth) {
		String tabs = "";
		for (int i = 0; i < depth; i++)
			tabs += "\t";

		out.println(tabs + this);

		conditionNode.print(out, depth + 1);
		statementBlock.print(out, depth + 1);
	}
}
