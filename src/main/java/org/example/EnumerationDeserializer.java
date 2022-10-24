/*------------------------------------------------------------------------------------------------
* org.example.EnumerationDeserializer
* 10/21/22
------------------------------------------------------------------------------------------------*/

package org.example;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.lang.reflect.Field;
import org.example.EnumJson.Projection;

public class EnumerationDeserializer<T extends Enum<T>> extends StdDeserializer<Enum<T>> implements ContextualDeserializer
{

	private static final long serialVersionUID = 1L;
	private transient Class<T> enumClass;
	private transient EnumJson classAnnotation = null;
	private transient EnumJson fieldAnnotation = null;

	protected EnumerationDeserializer()
	{
		this(null);
	}

	protected EnumerationDeserializer(Class<T> vc)
	{
		super(vc);
		this.enumClass = vc;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException
	{

		if (null != property) {
			//This enum is a bean or a member variable.
			fieldAnnotation = property.getAnnotation(EnumJson.class);

			//enumClass could have been set in a constructor.
			if (null == enumClass) {
				enumClass = (Class<T>) property.getType().getRawClass();
			}
		}
		else {
			enumClass = (Class<T>) ctxt.getContextualType().getRawClass();
		}

		if (null != enumClass) {
			classAnnotation = enumClass.getAnnotation(EnumJson.class);
		}

		return this;
	}

	/*-------------------------------------------------------------------------------------------
		deserialize will check for four different matches on an enum.
			1) If the json string matches the enum.name
			2) If the enum nas an annotation for an EnumJson Projection = ALIAS
			3) If the enum has an annotation for an EnumJson Projection = VALUE
			4) If the json string matches the enum.ordinal
	 -------------------------------------------------------------------------------------------*/
	@Override
	public Enum<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
	{

		Enum<T> result = null;
		final String jsonValue = p.getText();

		boolean caseInsensitive = false;

		if (fieldAnnotation != null) {
			caseInsensitive = fieldAnnotation.deserializeCaseInsensitive();
		}
		else if (classAnnotation != null) {
			caseInsensitive = classAnnotation.deserializeCaseInsensitive();
		}

		//-------------------------------------------------------------------------------------------
		//Check if json matches the Name
		for (final T enumValue : enumClass.getEnumConstants()) {
			if (enumValue.name().equals(jsonValue) || ((caseInsensitive) && enumValue.name().equalsIgnoreCase(jsonValue))) {
				result = enumValue;
			}
		}

		//-------------------------------------------------------------------------------------------
		//Check if the enum has an EnumJson Projection Annotation of ALIAS
		if (null == result) {
			Field[] enumFields = enumClass.getDeclaredFields();
			result = enumByAnnotatedField(enumFields, Projection.ALIAS, jsonValue, caseInsensitive);
		}

		//-------------------------------------------------------------------------------------------
		//Check if the enum has an EnumJson Projection Annotation of VALUE
		if (null == result) {
			Field[] enumFields = enumClass.getDeclaredFields();
			result = enumByAnnotatedField(enumFields, Projection.VALUE, jsonValue, caseInsensitive);
		}

		//-------------------------------------------------------------------------------------------
		//Check if json matches the Ordinal
		if (null == result) {
			for (final T enumValue : enumClass.getEnumConstants()) {
				if (Integer.toString(enumValue.ordinal()).equals(jsonValue)) {
					result = enumValue;
				}
			}
		}

		return result;
	}

	@java.lang.SuppressWarnings("java:S3011")
	protected Enum<T> enumByAnnotatedField(Field[] enumFields, EnumJson.Projection projection, String jsonValue, boolean caseInsensitive)
	{

		Enum<T> result = null;

		Field valueField = null;
		for (Field f : enumFields) {
			EnumJson annie = f.getAnnotation(EnumJson.class);
			if ((null != annie) && (annie.serializeProjection() == projection)) {
				valueField = f;
				break;
			}
		}

		if (null != valueField) {
			//The enum has a EnumJson Projection that matches
			valueField.setAccessible(true);
			try {
				for (final T enumValue : enumClass.getEnumConstants()) {

					//Get the projected value from the enum.
					Object projectedValue = valueField.get(enumValue);

					if ((null != projectedValue) &&
					    ((projectedValue.toString().equals(jsonValue)) ||
					     ((caseInsensitive) && projectedValue.toString().equalsIgnoreCase(jsonValue)))) {
						result = enumValue;
					}
				}
			}
			catch (IllegalAccessException ignored) {
				//ignored
			}
		}
		else {
			//Look for a field by named value or alias.
			if (projection == Projection.VALUE) {
				result = getFromKnownField(Projection.VALUE.name().toLowerCase(), jsonValue, caseInsensitive);
			}
			else if (projection == Projection.ALIAS) {
				result = getFromKnownField(Projection.ALIAS.name().toLowerCase(), jsonValue, caseInsensitive);
			}

		}

		return result;
	}

	protected Enum<T> getFromKnownField(String fieldName, String jsonValue, boolean caseInsensitive)
	{
		Enum<T> result = null;

		try {
			Field knownField = enumClass.getDeclaredField(fieldName);
			for (final T enumValue : enumClass.getEnumConstants()) {
				Object value = knownField.get(enumValue);

				if ((null != value) &&
				    ((value.toString().equals(jsonValue)) || ((caseInsensitive) && value.toString().equalsIgnoreCase(jsonValue)))) {
					result = enumValue;
					break;
				}
			}
		}
		catch (NoSuchFieldException | IllegalAccessException ignored) {
			//ignored
		}

		return result;
	}
}
