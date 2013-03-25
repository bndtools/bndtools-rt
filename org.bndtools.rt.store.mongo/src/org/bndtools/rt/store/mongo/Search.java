package org.bndtools.rt.store.mongo;

import java.text.*;
import java.util.*;
import java.util.regex.*;

public class Search {
	private static final Pattern	SKIPWORDS	= Pattern
														.compile("am|are|is|was|were|be|been|have|has|had|can|could|will|in|the|of|to|end|is|it|you|that|this|he|for|on|with|as|his|they|at|one|from|or|by|not|but|some|what|there|we|out|other|all|your|when|up|use|how|an|if|do|then|so|and|very|org|com|net|java");
	static Pattern					pattern		= Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	final Set<String>				set;

	public Search(Set<String> set) {
		this.set = set;
	}

	public Search() {
		this(new HashSet<String>());
	}

	public Set<String> set() {
		return set;
	}

	public void addAll(String description) {
		if (description == null)
			return;

		int l = description.length();
		int start = 0;
		while (start < l) {
			for (; start < l; start++) {
				char c = description.charAt(start);
				if (Character.isLetter(c) || Character.isDigit(c))
					break;
			}
			int end = start;
			for (; end < l; end++) {
				char c = description.charAt(end);
				if (!(Character.isLetter(c) || Character.isDigit(c)))
					break;
			}
			if (end - start > 1)
				add(description.substring(start, end));
			start = end;
		}
	}

	/**
	 * Terribly inefficient TODO
	 * 
	 * @param str
	 */
	public void add(String str) {
		if (str == null)
			return;

		str = str.trim();
		if (str.isEmpty())
			return;

		str = str.toLowerCase();
		if (SKIPWORDS.matcher(str).matches())
			return;

		String s = Normalizer.normalize(str, Normalizer.Form.NFD);
		s = pattern.matcher(s).replaceAll("");
		set.add(s);
	}

	public void getEncodedKeywords(StringBuilder sb) {
		String del = "";
		for (String s : set) {
			sb.append(del).append(s);
			del = "%20";
		}
	}
}
