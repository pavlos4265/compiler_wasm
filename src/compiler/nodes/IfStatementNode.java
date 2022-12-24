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
import java.util.List;

public class IfStatementNode extends StatementNode {

	private List<StatementNode> statementBlocks;
	private List<ExpressionNode> conditions;

	public List<StatementNode> getStatementBlocks() {
		return statementBlocks;
	}

	public void setStatementBlocks(List<StatementNode> statementBlocks) {
		this.statementBlocks = statementBlocks;
	}

	public List<ExpressionNode> getConditions() {
		return conditions;
	}

	public void setConditions(List<ExpressionNode> conditions) {
		this.conditions = conditions;
	}

	@Override
	public void print(PrintStream out, int depth) {
		String tabs = "";
		for (int i = 0; i < depth; i++)
			tabs += "\t";

		out.println(tabs + this);

		for (int i = 0; i < statementBlocks.size(); i++) {
			if (i != conditions.size())
				conditions.get(i).print(out, depth + 1);

			statementBlocks.get(i).print(out, depth + 1);
		}
	}
}
