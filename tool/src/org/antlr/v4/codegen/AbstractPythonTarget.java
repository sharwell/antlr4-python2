/*
 * [The "BSD license"]
 *  Copyright (c) 2012 Terence Parr
 *  Copyright (c) 2012 Sam Harwell
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 *  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 *  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.antlr.v4.codegen;

import java.util.Locale;
import java.util.Set;

import org.antlr.v4.tool.ast.GrammarAST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.StringRenderer;

/**
 *
 * @author Eric Vergnaud
 */
public abstract class AbstractPythonTarget extends Target {

	protected AbstractPythonTarget(CodeGenerator gen, String name) {
		super(gen, name);
	}


	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * For Java, this is the translation {@code 'a\n"'} &rarr; {@code "a\n\""}.
	 * Expect single quotes around the incoming literal. Just flip the quotes
	 * and replace double quotes with {@code \"}.</p>
	 * <p>
	 * Note that we have decided to allow people to use '\"' without penalty, so
	 * we must build the target string in a loop as {@link String#replace}
	 * cannot handle both {@code \"} and {@code "} without a lot of messing
	 * around.</p>
	 */
	@Override
	public String getTargetStringLiteralFromANTLRStringLiteral(
		CodeGenerator generator,
		String literal, boolean addQuotes)
	{
		StringBuilder sb = new StringBuilder();
		String is = literal;

		if ( addQuotes ) sb.append('"');

		for (int i = 1; i < is.length() -1; i++) {
			if  (is.charAt(i) == '\\') {
				// Anything escaped is what it is! We assume that
				// people know how to escape characters correctly. However
				// we catch anything that does not need an escape in Java (which
				// is what the default implementation is dealing with and remove
				// the escape. The C target does this for instance.
				//
				switch (is.charAt(i+1)) {
					// Pass through any escapes that Java also needs
					//
					case    '"':
					case    'n':
					case    'r':
					case    't':
					case    'b':
					case    'f':
					case    '\\':
						// Pass the escape through
						sb.append('\\');
						break;

					case    'u':    // Assume unnnn
						// Pass the escape through as double \\
						// so that Java leaves as \u0000 string not char
						sb.append('\\');
						sb.append('\\');
						break;

					default:
						// Remove the escape by virtue of not adding it here
						// Thus \' becomes ' and so on
						break;
				}

				// Go past the \ character
				i++;
			} else {
				// Characters that don't need \ in ANTLR 'strings' but do in Java
				if (is.charAt(i) == '"') {
					// We need to escape " in Java
					sb.append('\\');
				}
			}
			// Add in the next character, which may have been escaped
			sb.append(is.charAt(i));
		}

		if ( addQuotes ) sb.append('"');

		return sb.toString();
	}

	@Override
	public String encodeIntAsCharEscape(int v) {
		if (v < Character.MIN_VALUE || v > Character.MAX_VALUE) {
			throw new IllegalArgumentException(String.format("Cannot encode the specified value: %d", v));
		}

		if (v >= 0 && v < targetCharValueEscape.length && targetCharValueEscape[v] != null) {
			return targetCharValueEscape[v];
		}

		if (v >= 0x20 && v < 127 && (!Character.isDigit(v) || v == '8' || v == '9')) {
			return String.valueOf((char)v);
		}

		if ( v>=0 && v<=127 ) {
			String oct = Integer.toOctalString(v);
			return "\\"+ oct;
		}

		String hex = Integer.toHexString(v|0x10000).substring(1,5);
		return "\\u"+hex;
	}

	@Override
	public int getSerializedATNSegmentLimit() {
		// set to something stupid to avoid segmentation
		return 2 ^ 31;
	}
	
	public abstract Set<String> getBadWords();
	
	@Override
	protected boolean visibleGrammarSymbolCausesIssueInGeneratedCode(GrammarAST idNode) {
		return getBadWords().contains(idNode.getText());
	}

	@Override
	protected STGroup loadTemplates() {
		STGroup result = super.loadTemplates();
		result.registerRenderer(String.class, new PythonStringRenderer(), true);
		return result;
	}

	protected static class PythonStringRenderer extends StringRenderer {

		@Override
		public String toString(Object o, String formatString, Locale locale) {
			return super.toString(o, formatString, locale);
		}
	}

	@Override
	public boolean wantsBaseListener() {
		return false;
	}

	@Override
	public boolean wantsBaseVisitor() {
		return false;
	}

	@Override
	public boolean supportsOverloadedMethods() {
		return false;
	}
	
	
}
