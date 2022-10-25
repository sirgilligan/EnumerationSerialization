/*------------------------------------------------------------------------------------------------
* org.example.EnumSerializationComparisonTests
* 10/25/22
------------------------------------------------------------------------------------------------*/

package org.example;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.time.StopWatch;
import org.example.EnumJson.Projection;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class EnumSerializationComparisonTests
{

	ObjectMapper mapper;
	private final int LOOP_MAX = 10000;

	@BeforeTest
	public void setUp()
	{
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}


	static enum MatchTypeTraditional
	{
		UNKNOWN(0, "unknown"), EXACT(1, "exact"), WILDCARD(2, "wildcard");

		private static final Map<Integer, MatchTypeTraditional> VALUE_MATCH_TYPE_MAP = Stream.of(MatchTypeTraditional.values())
				.collect(Collectors.toMap(s -> s.value, Function.identity()));
		private static final Map<String, MatchTypeTraditional> STRING_MATCH_TYPE_MAP = Stream.of(MatchTypeTraditional.values())
				.collect(Collectors.toMap(s -> s.type, Function.identity()));

		private final int value;
		private final String type;

		MatchTypeTraditional(final int value, final String type)
		{
			this.value = value;
			this.type = type;
		}

		@JsonCreator
		public static MatchTypeTraditional fromString(final String s)
		{
			return Optional.ofNullable(STRING_MATCH_TYPE_MAP.get(s))
					.orElseThrow(() -> new IllegalArgumentException(String.format("Illegal match type '%s' specified from string", s)));
		}

		@JsonCreator
		public static MatchTypeTraditional fromValue(final Integer value)
		{
			return Optional.ofNullable(VALUE_MATCH_TYPE_MAP.get(value))
					.orElseThrow(() -> new IllegalArgumentException(String.format("Illegal match type '%s' specified from value", value)));
		}

		@JsonValue
		public int getValue()
		{
			return value;
		}

		@Override
		public String toString()
		{
			return type;
		}
	}


	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = EnumerationDeserializer.class)
	@EnumJson(serializeProjection = Projection.ALIAS,
	          deserializationClass = MatchTypeNewApproach.class,
	          deserializationValueFieldName = "value",
	          deserializationAliasFieldName = "type")
	public enum MatchTypeNewApproach
	{
		UNKNOWN(0, "unknown"), EXACT(1, "exact"), WILDCARD(2, "wildcard");

		private final int value;
		private final String type;

		MatchTypeNewApproach(final int value, final String type)
		{
			this.value = value;
			this.type = type;
		}

		@Override
		public String toString()
		{
			return type;
		}
	}


	@Test
	public void speedTest() throws Exception
	{

		String json;
		int errorCount = 0;

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < LOOP_MAX; i++) {
			for (MatchTypeTraditional traditional : MatchTypeTraditional.values()) {
				json = mapper.writeValueAsString(traditional);
				MatchTypeTraditional x = mapper.convertValue(json, MatchTypeTraditional.class);
				if (traditional != x) {
					errorCount++;
				}
			}
		}
		stopWatch.stop();

		long traditionalMS = stopWatch.getTime();

		assertEquals(errorCount, 0);

		stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < LOOP_MAX; i++) {
			for (MatchTypeNewApproach newApproach : MatchTypeNewApproach.values()) {
				json = mapper.writeValueAsString(newApproach);
				MatchTypeNewApproach x = mapper.convertValue(json, MatchTypeNewApproach.class);
				if (newApproach != x) {
					errorCount++;
				}
			}
		}
		stopWatch.stop();

		long newApproachMS = stopWatch.getTime();

		assertEquals(errorCount, 0);
	}

	@Test
	public void crossTest() throws Exception
	{

		for (MatchTypeTraditional traditional : MatchTypeTraditional.values()) {
			String json = mapper.writeValueAsString(traditional);
			MatchTypeNewApproach newApproach = mapper.convertValue(json, MatchTypeNewApproach.class);
			assertEquals(newApproach.type, traditional.type);
			assertEquals(newApproach.value, traditional.value);
		}

		//This will fail, the traditional can not deserialize by its type field, only its value field.
		for (MatchTypeNewApproach newApproach : MatchTypeNewApproach.values()) {
			String json = mapper.writeValueAsString(newApproach);

			MatchTypeTraditional traditional = null;
			try {
				traditional = mapper.convertValue(json, MatchTypeTraditional.class);
			}
			catch (IllegalArgumentException ignored) {
				//Ignored
			}
			assertNull(traditional);
		}
	}

	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = EnumerationDeserializer.class)
	@EnumJson(serializeProjection = Projection.VALUE,
	          deserializationClass = Compound.class,
	          deserializationValueFieldName = "value")
	public enum Compound
	{
		AB("A B"), ONE_TWO("1 2");

		private final String value;

		Compound(String value)
		{
			this.value = value;
		}
	}

	@Test
	public void testCompoundEnum() throws Exception
	{

		for (Compound c : Compound.values()) {
			String json = mapper.writeValueAsString(c);
			Compound nc = mapper.convertValue(json, Compound.class);
			assertEquals(c, nc);
		}
	}

	static class CompoundContainer
	{

		public Compound first = Compound.AB;
		public Compound second = Compound.ONE_TWO;

		public CompoundContainer()
		{

		}
	}

	@Test
	public void testCompoundContainer() throws Exception
	{
		CompoundContainer compoundContainer = new CompoundContainer();
		String json = mapper.writeValueAsString(compoundContainer);
		CompoundContainer another = mapper.readValue(json, CompoundContainer.class);
		assertNotNull(another);
		assertEquals(compoundContainer.first, another.first);
		assertEquals(compoundContainer.second, another.second);
	}

	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = EnumerationDeserializer.class)
	@EnumJson(serializeProjection = Projection.VALUE,
	          deserializationClass = SecretCode.class,
	          deserializationValueFieldName = "value")
	public enum SecretCode
	{
		CODE_ONE(1), CODE_TWO(2);

		private final int value;

		SecretCode(int value)
		{
			this.value = value;
		}
	}

	@Test
	public void testCodeOne() throws Exception
	{

		for (SecretCode c1 : SecretCode.values()) {
			String json = mapper.writeValueAsString(c1);
			SecretCode c2 = mapper.readValue(json, SecretCode.class);
			assertEquals(c1, c2);
		}
	}

}
