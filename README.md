# EnumerationSerialization
Java implementation for a generic enum serializer and deserializer

Uses annotations custom annotations.
The enum's deserializer can be specified as a subclass of the base deserializer, or instead the class of the enum can be specified via annotation.

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

OR

	@JsonSerialize(using = EnumerationSerializer.class)
	@JsonDeserialize(using = EnumerationDeserializer.class)
	@EnumJson(serializeProjection = Projection.NAME, deserializationClass = RGB.class)
	enum RGB {
		RED,
		GREEN,
		BLUE
	}

There are two special annotations to identify a value and an alias.

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
  
  The annotations can be applied to a class that has members of your enum.

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
