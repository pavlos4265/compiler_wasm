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

import java.io.IOException;
import java.io.OutputStream;

public class WASMUtils {
	public static void WriteUnsignedLeb128(OutputStream out, int value) throws IOException {
		int remaining = value >>> 7;
		while (remaining != 0) {
			out.write((byte) ((value & 0x7f) | 0x80));
			value = remaining;
			remaining >>>= 7;
		}

		out.write((byte) (value & 0x7f));
	}

	public static void WriteSignedLeb128(OutputStream out, int value) throws IOException {
		int remaining = value >> 7;
		boolean hasMore = true;
		int end = ((value & Integer.MIN_VALUE) == 0) ? 0 : -1;

		while (hasMore) {
			hasMore = (remaining != end) || ((remaining & 1) != ((value >> 6) & 1));

			out.write((byte) ((value & 0x7f) | (hasMore ? 0x80 : 0)));
			value = remaining;
			remaining >>= 7;
		}
	}

	public static byte GetValType(String name) {
		if (name.equalsIgnoreCase("i32")) {
			return 0x7F;
		} else if (name.equalsIgnoreCase("i64")) {
			return 0x7E;
		} else if (name.equalsIgnoreCase("f32")) {
			return 0x7D;
		} else if (name.equalsIgnoreCase("f64")) {
			return 0x7C;
		}

		return -1;
	}
}
