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
package compiler.enums;

public enum LiteralType {
	i32, i64, f32, f64, _void_, bool;

	public static LiteralType getLiteralTypeFromString(String type) {
		if (type.equals("i32"))
			return LiteralType.i32;
		else if (type.equals("i64"))
			return LiteralType.i64;
		else if (type.equals("f32"))
			return LiteralType.f32;
		else if (type.equals("f64"))
			return LiteralType.f64;
		else if (type.equals("void"))
			return LiteralType._void_;
		else if (type.equals("bool"))
			return LiteralType.bool;
		else
			return null;
	}
}
