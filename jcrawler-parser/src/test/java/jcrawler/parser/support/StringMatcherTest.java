package jcrawler.parser.support;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jcrawler.parser.support.StringMatcher;

public class StringMatcherTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPattern() {
		String input = "window.sr2_lat = '22.2002222721014'; window.sr2_lng = '113.546796441078';";
		Pattern p = Pattern.compile("\\d+\\.\\d+");
		Double[] numbers = StringMatcher.from(input).pattern(p).toDoubles();
		assertTrue(numbers[0] == 22.2002222721014);
		assertTrue(numbers[1] == 113.546796441078);
	}
	
	@Test
	public void testPatternFirst() {
		String input = "(10)[15.66]";
		Pattern p = Pattern.compile("\\d+(\\.\\d+)?");
		Integer number = StringMatcher.from(input).patternFirst(p).toInteger();
		assertTrue(number.intValue() == 10);
		Double number2 = StringMatcher.from(input).patternLast(p).toDouble();
		assertTrue(number2.doubleValue() == 15.66);
	}
	
	@Test
	public void testPatternGroups() {
		String input = "(10.567)(20.345)[0.789]";
		Pattern p = Pattern.compile("(\\d+)\\.(\\d+)");
		Integer[] numbers = StringMatcher.from(input).pattern(p, 2).toIntegers();
		printArray(numbers);
		Double[] values = StringMatcher.from(input).pattern(p, 0).toDoubles();
		printArray(values);
	}
	
	@Test
	public void testPatternGroup() {
		String input = "(10.567)(20.345)[0.789]";
		Pattern p = Pattern.compile("(\\d+)\\.(\\d+)");
		Integer number = StringMatcher.from(input).patternFirst(p, 1).toInteger();
		assertTrue(number.intValue() == 10);
		number = StringMatcher.from(input).patternFirst(p, 2).toInteger();
		assertTrue(number.intValue() == 567);
		number = StringMatcher.from(input).patternLast(p, 1).toInteger();
		assertTrue(number.intValue() == 0);
		number = StringMatcher.from(input).patternLast(p, 2).toInteger();
		assertTrue(number.intValue() == 789);
	}
	
	@Test
	public void testMatchFirst() {
		String input = "(10.567)(20.345)[0.789]";
		Pattern p = Pattern.compile("(\\d+)\\.(\\d+)");
		String[] numbers = StringMatcher.from(input).matchFirst(p).toStrings();
		printArray(numbers);
		Integer[] values = StringMatcher.from(input).matchFirst(p, 1, 2).toIntegers();
		printArray(values);
	}
	
	private <T> void printArray(T[] inputs) {
		if (inputs != null) {
			System.out.println();
			for (T value : inputs) {
				System.out.print(value + "\t\t");
			}
		}
	}
	
	@Test
	public void testSubstring() {
		String input = "abcurl=xyzurl=opq";
		String open = "url=";
		String close = null;
		String result = StringMatcher.from(input).substring(open, open).toString();
		assertTrue("xyz".equals(result));
		result = StringMatcher.from(input).substringLast(open, close).toString();
		assertTrue("opq".equals(result));
	}

}
