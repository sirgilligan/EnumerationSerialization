/*------------------------------------------------------------------------------------------------
* org.example.EnumerationDeserializer
* 10/21/22
------------------------------------------------------------------------------------------------*/

package org.example;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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

		if (null == enumClass) {
			//enum class wasn't set in constructor and the property was null.
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
	@java.lang.SuppressWarnings("java:S3011")
	@Override
	public Enum<T> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
	{

		Enum<T> result = null;
		String jsonValue = p.getText();

		if ((null == p.getParsingContext().getParent()) && (p.getCurrentToken().equals(JsonToken.VALUE_STRING))) {
			//This is the root context. That means the data could be in nested double quotes.
			char[] chars = jsonValue.toCharArray();
			if ((chars[0] == '"') && (chars[chars.length - 1] == '"')) {
				jsonValue = jsonValue.substring(1, chars.length - 1);
			}
		}

		boolean caseInsensitive = false;

		if (fieldAnnotation != null) {
			caseInsensitive = fieldAnnotation.deserializeCaseInsensitive();
		}
		else if (classAnnotation != null) {
			caseInsensitive = classAnnotation.deserializeCaseInsensitive();
		}

		boolean isDigit = isDigit(jsonValue);

		Field[] enumFields = enumClass.getDeclaredFields();

		//-------------------------------------------------------------------------------------------
		// Find the alias and value field just once.
		Field valueField = null;
		Field aliasField = null;

		if (null != classAnnotation) {
			if (!"".equals(classAnnotation.deserializationValueFieldName())) {
				valueField = getDeclaredField(classAnnotation.deserializationValueFieldName());
			}

			if (!"".equals(classAnnotation.deserializationAliasFieldName())) {
				aliasField = getDeclaredField(classAnnotation.deserializationAliasFieldName());
			}
		}

		if ((null == valueField) || (null == aliasField)) {
			for (Field f : enumFields) {
				EnumJson annie = f.getAnnotation(EnumJson.class);
				if (null != annie) {
					if ((null == aliasField) && (annie.serializeProjection() == Projection.ALIAS)) {
						aliasField = f;
					}
					else if ((null != valueField) && (annie.serializeProjection() == Projection.VALUE)) {
						valueField = f;
					}
				}
				if ((null != aliasField) && (null != valueField)) {
					break;
				}
			}
		}

		if (null == aliasField) {
			//Look for field by field name, not by annotation
			aliasField = getDeclaredField(Projection.ALIAS.name().toLowerCase());
		}

		if (null == valueField) {
			//Look for field by field name, not by annotation
			valueField = getDeclaredField(Projection.VALUE.name().toLowerCase());
		}

		if (null != aliasField) {
			aliasField.setAccessible(true);
		}

		if (null != valueField) {
			valueField.setAccessible(true);
		}

		//-------------------------------------------------------------------------------------------

		//-------------------------------------------------------------------------------------------
		//Loop to go through the enum values only one time.
		for (final T enumValue : enumClass.getEnumConstants()) {

			//-------------------------------------------------------------------------------------------
			//Check if json matches the Name
			if ((!isDigit) && (enumValue.name().equals(jsonValue) || ((caseInsensitive) && enumValue.name().equalsIgnoreCase(jsonValue)))) {
				result = enumValue;
				break;
			}

			//-------------------------------------------------------------------------------------------
			//Check if the enum has an EnumJson Projection Annotation of ALIAS
			if ((null != aliasField) && ((!isDigit) || (fieldCanDeserializeDigit(aliasField)))) {
				Object valueOfField = getFieldValue(aliasField, enumValue);
				if ((null != valueOfField) &&
				    ((valueOfField.toString().equals(jsonValue)) ||
				     ((caseInsensitive) && valueOfField.toString().equalsIgnoreCase(jsonValue)))) {
					result = enumValue;
					break;
				}
			}

			//-------------------------------------------------------------------------------------------
			//Check if the enum has an EnumJson Projection Annotation of VALUE
			if ((null != valueField) && ((!isDigit) || (fieldCanDeserializeDigit(valueField)))) {
				Object valueOfField = getFieldValue(valueField, enumValue);
				if ((null != valueOfField) &&
				    ((valueOfField.toString().equals(jsonValue)) ||
				     ((caseInsensitive) && valueOfField.toString().equalsIgnoreCase(jsonValue)))) {
					result = enumValue;
					break;
				}
			}
		}

		//-------------------------------------------------------------------------------------------
		//Check if json matches the Ordinal
		//This can't be checked in the previous loop over the enum constants
		//because there could be a conflict with value or alias being a digit
		if ((isDigit) && (null == result)) {
			int index = parseInt(jsonValue, -1);
			if ((index >= 0) && (index < enumClass.getEnumConstants().length)) {
				result = enumClass.getEnumConstants()[index];
			}
		}

		return result;
	}

	protected int parseInt(String s, int defaultResult)
	{
		int result = defaultResult;

		try {
			result = Integer.parseInt(s);
		}
		catch (NumberFormatException ignored) {
			//Ignored
		}

		return result;
	}

	protected Field getDeclaredField(String fieldName)
	{
		Field result = null;

		try {
			result = enumClass.getDeclaredField(fieldName);
		}
		catch (NoSuchFieldException ignored) {
			//Ignored
		}

		return result;
	}

	protected Object getFieldValue(Field field, T enumValue)
	{
		Object result = null;

		try {
			result = field.get(enumValue);
		}
		catch (IllegalAccessException ignored) {
			//Ignored
		}

		return result;

	}

	protected boolean isDigit(String value)
	{
		boolean result = true;

		final char[] chars = value.toCharArray();
		for (Character c : chars) {
			if (c < '0' || c > '9') {
				result = false;
				break;
			}
		}

		return result;
	}

	protected boolean fieldCanDeserializeDigit(Field field)
	{
		boolean result = false;
		Class<?> valueFieldType = field.getType();
		if ((String.class.equals(valueFieldType)) ||
		    (int.class.equals(valueFieldType)) ||
		    (long.class.equals(valueFieldType)) ||
		    (Integer.class.equals(valueFieldType)) ||
		    (Long.class.equals(valueFieldType))) {
			result = true;
		}
		return result;
	}
}
