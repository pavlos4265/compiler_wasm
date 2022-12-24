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

public class NumberExpressionNode extends ExpressionNode {

	private double number;

	private boolean isFloat;

	public double getNumber() {
		return number;
	}

	public void setNumber(double number) {
		this.number = number;
	}

	@Override
	public String toString() {
		return super.toString() + " (" + number + ")";
	}

	public boolean isFloat() {
		return isFloat;
	}

	public void setFloat(boolean isFloat) {
		this.isFloat = isFloat;
	}

	@Override
	public void print(PrintStream out, int depth) {
		String tabs = "";
		for (int i = 0; i < depth; i++)
			tabs += "\t";

		out.println(tabs + this);
	}
}
