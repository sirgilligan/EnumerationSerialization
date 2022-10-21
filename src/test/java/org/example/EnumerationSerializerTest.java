package org.example;

import static org.testng.Assert.*;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.example.EnumJson.Projection;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
/*------------------------------------------------------------------------------------------------
* org.example.EnumerationSerializerTest
* 10/21/22
------------------------------------------------------------------------------------------------*/

public class EnumerationSerializerTest
{

	//Enum without EnumJson annotation
	@JsonSerialize(using = EnumerationSerializer.class)
	enum Answer {
		YES,
		NO
	}

	//Enum serialized by Identifier
	@JsonSerialize(using = EnumerationSerializer.class)
	@EnumJson(serializeProjection = Projection.NAME)
	enum RGB {
		RED,
		GREEN,
		BLUE
	}

	//Enum serialized by Ordinal
	@JsonSerialize(using = EnumerationSerializer.class)
	@EnumJson(serializeProjection = Projection.ORDINAL)
	enum RYB {
		RED,
		YELLOW,
		BLUE
	}

	//Enum serialized by Value
	@JsonSerialize(using = EnumerationSerializer.class)
	@EnumJson(serializeProjection = Projection.VALUE)
	enum SomeDays {
		MONDAY("Lunes", "Monday"),
		TUESDAY("Martes", "Tuesday"),
		WEDNESDAY("Miercoles", "Wednesday");

		@EnumJson(serializeProjection = Projection.VALUE)
		final String value;

		@EnumJson(serializeProjection = Projection.ALIAS)
		final String alias;

		SomeDays(String v, String a) {
			this.value = v;
			this.alias = a;
		}
	}

	//Enum serialized by Value, and the value is an integer
	@JsonSerialize(using = EnumerationSerializer.class)
	@EnumJson(serializeProjection = Projection.VALUE)
	enum SomeNums {
		ONE(1),
		TWO(2),
		THREE(3);

		@EnumJson(serializeProjection = Projection.VALUE)
		final int value;

		SomeNums(int v) {
			this.value = v;
		}
	}

	ObjectMapper mapper;

	@BeforeTest
	public void setUp() {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Test
	public void testSerializationOfEnums() throws Exception {

		// NO EnumJson annotation.
		String json = mapper.writeValueAsString(Answer.NO);
		assertEquals(json, "\"NO\"");

		//EnumJson Identifier
		json = mapper.writeValueAsString(RGB.BLUE);
		assertEquals(json, "\"BLUE\"");

		//EnumJson Ordinal
		json = mapper.writeValueAsString(RYB.YELLOW);
		assertEquals(json, "1");

		//EnumJson Value
		json = mapper.writeValueAsString(SomeDays.TUESDAY);
		assertEquals(json, "\"Martes\"");

		//EnumJson Value
		json = mapper.writeValueAsString(SomeNums.TWO);
		assertEquals(json, "2");

	}


	static class SomeStuff {

		//Serialize as the Ordinal
		@EnumJson(serializeProjection = Projection.ORDINAL)
		public SomeDays someDay = SomeDays.MONDAY;

		//Serialize as the Identifier
		@EnumJson(serializeProjection = Projection.NAME)
		public SomeDays nextDay = SomeDays.values()[someDay.ordinal() + 1];

		//Serialize as the Value
		@EnumJson(serializeProjection = Projection.VALUE)
		public SomeDays middleDay = SomeDays.WEDNESDAY;

		//Serialize as the Alias
		@EnumJson(serializeProjection = Projection.ALIAS)
		public SomeDays tuesday = SomeDays.TUESDAY;

		//Serialize as the Ordinal
		@EnumJson(serializeProjection = Projection.ORDINAL)
		public SomeNums aNum = SomeNums.ONE;

		//Serialize as the Identifier
		@EnumJson(serializeProjection = Projection.NAME)
		public SomeNums anotherNum = SomeNums.TWO;

	}

	@Test
	public void testSerializationOfClassWithMemberEnums001() throws Exception {

		SomeStuff someStuff = new SomeStuff();

		String json = mapper.writeValueAsString(someStuff);
		assertEquals(json,
		             "{\"someDay\":0,\"nextDay\":\"TUESDAY\",\"middleDay\":\"Miercoles\",\"tuesday\":\"Tuesday\",\"aNum\":0,\"anotherNum\":\"TWO\"}");
	}

	static class SameStuffMultipleWays {

		@EnumJson(serializeProjection = Projection.ORDINAL)
		public SomeDays ordinalDay = SomeDays.MONDAY;

		@EnumJson(serializeProjection = Projection.NAME)
		public SomeDays nameDay = SomeDays.MONDAY;

		@EnumJson(serializeProjection = Projection.VALUE)
		public SomeDays valueDay = SomeDays.MONDAY;

		@EnumJson(serializeProjection = Projection.ALIAS)
		public SomeDays aliasDay = SomeDays.MONDAY;

	}

	@Test
	public void testSerializationOfClassWithMemberEnums002() throws Exception {

		SameStuffMultipleWays stuff = new SameStuffMultipleWays();

		String json = mapper.writeValueAsString(stuff);
		assertEquals(json,
		             "{\"ordinalDay\":0,\"nameDay\":\"MONDAY\",\"valueDay\":\"Lunes\",\"aliasDay\":\"Monday\"}");
	}

	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = EnumerationDeserializer.class)
	@EnumJson(serializeProjection = Projection.VALUE, deserializeCaseInsensitive = false, deserializationClass = DogBreeds.class)
	static enum DogBreeds {
		POODLE("poodle"),
		LABRADOR("labrador"),
		MUTT("mutt");

		final String value;

		DogBreeds(String value) {
			this.value = value;
		}
	}

	static class MyDogs {

		public DogBreeds first = DogBreeds.POODLE;
		public DogBreeds favorite = DogBreeds.MUTT;
		public DogBreeds wanted = DogBreeds.LABRADOR;
	}

	@Test
	public void testKnownFieldNamedValue() throws Exception {
		MyDogs myDogs = new MyDogs();
		String json = mapper.writeValueAsString(myDogs);
		assertEquals(json, "{\"first\":\"poodle\",\"favorite\":\"mutt\",\"wanted\":\"labrador\"}");
	}

	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = EnumerationDeserializer.class)
	@EnumJson(serializeProjection = Projection.ALIAS, deserializeCaseInsensitive = false, deserializationClass = CatBreeds.class)
	static enum CatBreeds {
		TABBY("tabby"),
		PERSIAN("persian"),
		STRAY("stray");

		final String alias;

		CatBreeds(String alias) {
			this.alias = alias;
		}
	}

	static class MyCats {

		public CatBreeds first = CatBreeds.TABBY;
		public CatBreeds favorite = CatBreeds.STRAY;
		public CatBreeds wanted = CatBreeds.PERSIAN;
	}

	@Test
	public void testKnownFieldNamedAlias() throws Exception {
		MyCats myCats = new MyCats();
		String json = mapper.writeValueAsString(myCats);
		assertEquals(json, "{\"first\":\"tabby\",\"favorite\":\"stray\",\"wanted\":\"persian\"}");
	}

}