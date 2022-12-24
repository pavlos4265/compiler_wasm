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
import java.io.IOException;

public class WASMVector {

	private ByteArrayOutputStream baos;

	public WASMVector(int size) throws IOException {
		this();

		WASMUtils.WriteUnsignedLeb128(baos, size);
	}

	public WASMVector() throws IOException {
		this.baos = new ByteArrayOutputStream();
	}

	public void addVector(WASMVector v) throws IOException {
		this.baos.write(v.getBytes());
	}

	public void addByte(int b) {
		baos.write(b);
	}

	public ByteArrayOutputStream getStream() {
		return baos;
	}

	public byte[] getBytes() {
		return baos.toByteArray();
	}
}
