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
package compiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import compiler.exceptions.PreprocessException;

public class Preprocessor {

	private Pattern incPattern;

	public Preprocessor() {
		incPattern = Pattern.compile("#include \"([^\"]*)\"");
	}

	public List<String> process(List<String> fileLines) throws PreprocessException, IOException {
		List<String> finalLines = new ArrayList<>();

		for (String line : fileLines) {
			Matcher matcher = incPattern.matcher(line);
			if (!matcher.find()) {
				finalLines.add(line);
				continue;
			}

			String fileName = matcher.group(1);

			File includedFile = new File(fileName);
			if (!includedFile.exists())
				throw new PreprocessException("Included file " + fileName + " was not found.");

			List<String> incFileLines = process(Files.readAllLines(includedFile.toPath()));
			for (String incFileLine : incFileLines) {
				finalLines.add(incFileLine);
			}
		}

		return finalLines;
	}
}
