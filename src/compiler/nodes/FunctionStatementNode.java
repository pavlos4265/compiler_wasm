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

public class FunctionStatementNode extends StatementNode {

	private String name, type;
	private StatementBlockNode statementBlock;
	private List<VariableDeclarationNode> arguments;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StatementBlockNode getStatementBlock() {
		return statementBlock;
	}

	public void setStatementBlock(StatementBlockNode statementBlock) {
		this.statementBlock = statementBlock;
	}

	public List<VariableDeclarationNode> getArguments() {
		return arguments;
	}

	public void setArguments(List<VariableDeclarationNode> arguments) {
		this.arguments = arguments;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public void print(PrintStream out, int depth) {
		String tabs = "";
		for (int i = 0; i < depth; i++)
			tabs += "\t";

		out.println(tabs + this + " (" + name + " " + type + ")");

		for (VariableDeclarationNode arg : arguments)
			arg.print(out, depth + 1);

		statementBlock.print(out, depth + 1);
	}
}
