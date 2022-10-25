/*------------------------------------------------------------------------------------------------
* org.example.EnumerationSerializer
* 10/21/22
------------------------------------------------------------------------------------------------*/

package org.example;

/*
	EnumerationSerializer serializes an Enum looking for EnumJson annotations.
	If no EnumJson annotation is found the enum is serialized by name.

	If the enum is a member variable of some class then the EnumJson annotation
	at the member variable level is used and takes priority over any Enum class annotation.

	If there are no member variable EnumJson annotation then if there is an Enum class
	annotation it will be used.
 */

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.lang.reflect.Field;
import org.example.EnumJson.Projection;

public class EnumerationSerializer<T extends Enum<T>> extends StdSerializer<Enum<T>>
{

	private static final long serialVersionUID = 1L;
	private transient EnumJson classAnnotation = null;

	public EnumerationSerializer()
	{
		this(null);
	}

	protected EnumerationSerializer(Class<Enum<T>> t)
	{
		super(t);
	}


	@SuppressWarnings("unchecked")
	@Override
	public void serialize(Enum<T> value, JsonGenerator gen, SerializerProvider provider) throws IOException
	{

		//Looking for data to write as json
		String dataToWrite = null;

		//Get the class annotation
		try {
			classAnnotation = value.getClass().getAnnotation(EnumJson.class);
		}
		catch (Exception ignored) {
			//Ignored
		}

		//Looking for a specific annotation
		//Field annotation has priority over class annotation
		EnumJson enumJsonAnnotation = null;

		//If this value is a field/member variable contained by another object, get the field name.
		String fieldName = gen.getOutputContext().getCurrentName();

		if (null != fieldName) {
			//Does the field have an annotation?

			try {
				enumJsonAnnotation = gen.getOutputContext().getCurrentValue().getClass().getField(fieldName).getAnnotation(EnumJson.class);
			}
			catch (NoSuchFieldException ignored) {
				//ignored
			}

		}

		if (null == enumJsonAnnotation) {
			//There wasn't a field level annotation
			//Is there an annotation on the enum class?
			enumJsonAnnotation = classAnnotation;
		}

		ObjectMapper mapper = (ObjectMapper) gen.getCodec();

		if (null != enumJsonAnnotation) {
			switch (enumJsonAnnotation.serializeProjection()) {
				case ORDINAL: {
					dataToWrite = mapper.writeValueAsString(value.ordinal());
				}
				break;

				case NAME: {
					dataToWrite = mapper.writeValueAsString(value.name());
				}
				break;

				case ALIAS: {
					Field[] enumFields = value.getClass().getDeclaredFields();
					Field field = findClassAnnotation(enumFields, Projection.ALIAS, (Class<T>) value.getClass());
					if (null == field) {
						field = findAnnotatedField(enumFields, Projection.ALIAS);
						if (null == field) {
							field = findFieldByName(Projection.ALIAS.name().toLowerCase(), (Class<T>) value.getClass());
						}
					}
					dataToWrite = getData(field, value, mapper);
				}
				break;

				case VALUE: {
					Field[] enumFields = value.getClass().getDeclaredFields();
					Field field = findClassAnnotation(enumFields, Projection.VALUE, (Class<T>) value.getClass());
					if (null == field) {
						field = findAnnotatedField(enumFields, Projection.VALUE);
						if (null == field) {
							field = findFieldByName(Projection.VALUE.name().toLowerCase(), (Class<T>) value.getClass());
						}
					}
					dataToWrite = getData(field, value, mapper);
				}
				break;

				default:
					break;
			}
		}
		else {
			//There was not any EnumJson annotation or known field such as value or alias.
			// Write enum as name
			dataToWrite = mapper.writeValueAsString(value.name());
		}

		if (null != dataToWrite) {
			gen.writeRawValue(dataToWrite);
		}
	}

	private Field findClassAnnotation(Field[] enumFields, Projection projection, Class<T> enumClass)
	{
		String fieldName = null;
		Field result = null;

		if (null != classAnnotation) {
			if ((projection.equals(Projection.VALUE)) && (!"".equals(classAnnotation.deserializationValueFieldName()))) {
				fieldName = classAnnotation.deserializationValueFieldName();
			}
			else if ((projection.equals(Projection.ALIAS)) && (!"".equals(classAnnotation.deserializationAliasFieldName()))) {
				fieldName = classAnnotation.deserializationAliasFieldName();
			}
		}

		if (null != fieldName) {
			result = findFieldByName(fieldName, enumClass);
		}

		return result;

	}

	protected Field findAnnotatedField(Field[] enumFields, EnumJson.Projection projection)
	{
		Field result = null;
		for (Field f : enumFields) {
			EnumJson annie = f.getAnnotation(EnumJson.class);
			if ((null != annie) && (annie.serializeProjection() == projection)) {
				result = f;
				break;
			}
		}

		return result;
	}

	private Field findFieldByName(String fieldName, Class<T> enumClass)
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

	@java.lang.SuppressWarnings("java:S3011")
	protected String getData(Field field, Enum<T> value, ObjectMapper mapper)
	{
		String dataToWrite = null;

		Object foundValue = null;
		if (null != field) {
			try {
				field.setAccessible(true);
				foundValue = field.get(value);
			}
			catch (IllegalAccessException ignored) {
				//ignored
			}
		}

		if (null != foundValue) {
			try {
				dataToWrite = mapper.writeValueAsString(foundValue);
			}
			catch (JsonProcessingException ignored) {
				//ignored
			}
		}

		return dataToWrite;
	}
}

