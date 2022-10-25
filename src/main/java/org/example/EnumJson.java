/*------------------------------------------------------------------------------------------------
* org.example.EnumJson
* 10/21/22
------------------------------------------------------------------------------------------------*/
package org.example;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnumAnnotation
public @interface EnumJson
{

	enum Projection
	{
		ALIAS,   //Enum has a property for an alias or alternate name. E.g. Monday
		NAME,    //Enum.name. E.g. MONDAY
		ORDINAL, //Enum.ordinal. E.g. 0, 1, 3, 4, etc.
		VALUE    //Enum has property for some type of value. E.g. LUNES
	}

	Projection serializeProjection() default Projection.VALUE;

	boolean deserializeCaseInsensitive() default false;

	Class<?> deserializationClass() default Void.class;

	String deserializationValueFieldName() default "";

	String deserializationAliasFieldName() default "";
}