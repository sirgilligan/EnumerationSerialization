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
* org.example.EnumerationDeserializerTest
* 10/21/22
------------------------------------------------------------------------------------------------*/

public class EnumerationDeserializerTest
{

	//Enum without EnumJson annotation
	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = Answer.Deserializer.class)
	enum Answer {
		YES,
		NO;

		public static class Deserializer extends EnumerationDeserializer<Answer> {

			private static final long serialVersionUID = 1L;

			public Deserializer() {
				super(Answer.class);
			}
		}
	}

	//Enum serialized by Identifier
	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = RGB.Deserializer.class)
	@EnumJson(serializeProjection = Projection.NAME)
	enum RGB {
		RED,
		GREEN,
		BLUE;

		public static class Deserializer extends EnumerationDeserializer<RGB> {

			private static final long serialVersionUID = 1L;

			public Deserializer() {
				super(RGB.class);
			}
		}
	}

	//Enum serialized by Ordinal
	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = RYB.Deserializer.class)
	@EnumJson(serializeProjection = Projection.ORDINAL)
	enum RYB {
		RED,
		YELLOW,
		BLUE;

		public static class Deserializer extends EnumerationDeserializer<RYB> {

			private static final long serialVersionUID = 1L;

			public Deserializer() {
				super(RYB.class);
			}
		}
	}

	//Enum serialized by Value
	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = SomeDays.Deserializer.class)
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

		public static class Deserializer extends EnumerationDeserializer<SomeDays> {
			private static final long serialVersionUID = 1L;
			public Deserializer() {
				super(SomeDays.class);
			}
		}
	}

	//Enum serialized by Value, and the value is an integer
	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = SomeNums.Deserializer.class)
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

		public static class Deserializer extends EnumerationDeserializer<SomeNums> {

			private static final long serialVersionUID = 1L;

			public Deserializer() {
				super(SomeNums.class);
			}
		}
	}


	ObjectMapper mapper;

	@BeforeTest
	public void setUp() {
		mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Test
	public void testDeserializationOfEnums() throws Exception {

		// Enum Value
		String json = "\"NO\"";
		Answer answer = mapper.readValue(json, Answer.class);
		assertEquals(answer, Answer.NO);

		//Enum Identifier
		json = "\"BLUE\"";
		RGB rgb = mapper.readValue(json, RGB.class);
		assertEquals(rgb, RGB.BLUE);

		//Enum Ordinal
		json = "1";
		RYB ryb = mapper.readValue(json, RYB.class);
		assertEquals(ryb, RYB.YELLOW);

		//Enum Value
		json = "\"Martes\"";
		SomeDays someDay = mapper.readValue(json, SomeDays.class);
		assertEquals(someDay, SomeDays.TUESDAY);

		//Enum Value
		json = "2";
		SomeNums someNum = mapper.readValue(json, SomeNums.class);
		assertEquals(someNum, SomeNums.TWO);

	}

	@Test
	public void testDeserializationOfBogusEnums() throws Exception {

		// Enum Value
		String json = "\"FRED\"";
		Answer answer = mapper.readValue(json, Answer.class);
		assertNull(answer);

		//Enum Identifier
		json = "\"DOG\"";
		RGB rgb = mapper.readValue(json, RGB.class);
		assertNull(rgb);

		//Enum Ordinal
		json = "13";
		RYB ryb = mapper.readValue(json, RYB.class);
		assertNull(ryb);

		//Enum Value
		json = "\"MAYO\"";
		SomeDays someDay = mapper.readValue(json, SomeDays.class);
		assertNull(someDay);

		//Enum Value
		json = "2.0";
		SomeNums someNum = mapper.readValue(json, SomeNums.class);
		assertNull(someNum);

	}

	static class SomeStuff {

		public SomeDays someDay;
		public SomeDays nextDay;
		public SomeDays middleDay;
		public SomeDays tuesday;
		public SomeNums aNum;
		public SomeNums anotherNum;

	}

	@Test
	public void testDeserializationOfClassWithMemberEnums001() throws Exception {

		String json = "{\"someDay\":0,\"nextDay\":\"TUESDAY\",\"middleDay\":\"Miercoles\",\"tuesday\":\"Tuesday\",\"aNum\":0,\"anotherNum\":\"TWO\"}";
		SomeStuff stuff = mapper.readValue(json, SomeStuff.class);
		assertNotNull(stuff);
		assertEquals(stuff.someDay, SomeDays.MONDAY);
		assertEquals(stuff.nextDay, SomeDays.TUESDAY);
		assertEquals(stuff.middleDay, SomeDays.WEDNESDAY);
		assertEquals(stuff.tuesday, SomeDays.TUESDAY);
		assertEquals(stuff.aNum, SomeNums.ONE);
		assertEquals(stuff.anotherNum, SomeNums.TWO);
	}

	static class SameStuffMultipleWays {

		public SomeDays ordinalDay = SomeDays.MONDAY;
		public SomeDays nameDay = SomeDays.MONDAY;
		public SomeDays valueDay = SomeDays.MONDAY;
		public SomeDays aliasDay = SomeDays.MONDAY;

	}

	@Test
	public void testDeserializationOfClassWithMemberEnums002() throws Exception {

		String json = "{\"ordinalDay\":0,\"nameDay\":\"MONDAY\",\"valueDay\":\"Lunes\",\"aliasDay\":\"Monday\"}";
		SameStuffMultipleWays stuff = mapper.readValue(json, SameStuffMultipleWays.class);
		assertNotNull(stuff);
		assertEquals(stuff.ordinalDay, SomeDays.MONDAY);
		assertEquals(stuff.nameDay, SomeDays.MONDAY);
		assertEquals(stuff.valueDay, SomeDays.MONDAY);
		assertEquals(stuff.aliasDay, SomeDays.MONDAY);
	}

	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = Fish.Deserializer.class)
	@EnumJson(serializeProjection = Projection.NAME, deserializeCaseInsensitive = true)
	static enum Fish {
		TROUT,
		BASS;

		public static class Deserializer extends EnumerationDeserializer<Fish> {

			private static final long serialVersionUID = 1L;

			public Deserializer() {
				super(Fish.class);
			}
		}
	}

	@Test
	public void testCaseInsensitive001() throws Exception {

		String json = "\"TROUT\"";

		Fish fish = mapper.readValue(json, Fish.class);
		assertEquals(fish, Fish.TROUT);

		json = "\"trout\"";
		fish = mapper.readValue(json, Fish.class);
		assertEquals(fish, Fish.TROUT);
	}

	static class Food {

		//override the setting on the Fish enum for case sensitivity
		@EnumJson(serializeProjection = Projection.NAME, deserializeCaseInsensitive = false)
		public Fish fish;

		@EnumJson(serializeProjection = Projection.NAME, deserializeCaseInsensitive = true)
		public Fish anotherFish;
	}

	@Test
	public void testCaseInsensitive002() throws Exception {

		String json =	"{\"fish\":\"bAsS\",\"anotherFish\":\"tROUt\"}";

		Food food = mapper.readValue(json, Food.class);
		assertNotNull(food);
		assertNull(food.fish);

		json = "{\"fish\":\"BASS\",\"anotherFish\":\"tROUt\"}";
		food = mapper.readValue(json, Food.class);
		assertNotNull(food);
		assertNotNull(food.fish);

	}

	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = EnumerationDeserializer.class)
	@EnumJson(serializeProjection = Projection.NAME, deserializeCaseInsensitive = true, deserializationClass = Cars.class)
	static enum Cars {
		Corvette,
		Camaro,
		Chevelle
	}

	static class MyCars {

		public Cars first = Cars.Camaro;
		public Cars favorite = Cars.Chevelle;
		public Cars wanted = Cars.Corvette;
	}

	@Test
	public void testBaseDeserializer001() throws Exception {

		String json = "{\"first\":\"Camaro\",\"favorite\":\"Chevelle\",\"wanted\":\"Corvette\"}";

		MyCars myCars = mapper.readValue(json, MyCars.class);
		assertNotNull(myCars);
		assertEquals(myCars.first, Cars.Camaro);
		assertEquals(myCars.favorite, Cars.Chevelle);
		assertEquals(myCars.wanted, Cars.Corvette);
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
	public void testBaseDeserializer002() throws Exception {

		String json = "{\"first\":\"poodle\",\"favorite\":\"mutt\",\"wanted\":\"labrador\"}";

		MyDogs myDogs = mapper.readValue(json, MyDogs.class);
		assertNotNull(myDogs);
		assertEquals(myDogs.first, DogBreeds.POODLE);
		assertEquals(myDogs.favorite, DogBreeds.MUTT);
		assertEquals(myDogs.wanted, DogBreeds.LABRADOR);
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
	public void testBaseDeserializer003() throws Exception {

		String json = "{\"first\":\"tabby\",\"favorite\":\"stray\",\"wanted\":\"persian\"}";

		MyCats myCats = mapper.readValue(json, MyCats.class);
		assertNotNull(myCats);
		assertEquals(myCats.first, CatBreeds.TABBY);
		assertEquals(myCats.favorite, CatBreeds.STRAY);
		assertEquals(myCats.wanted, CatBreeds.PERSIAN);
	}


}