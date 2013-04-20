package lilypad.bukkit.connect.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionUtils {

	public static void setFinalField(Class<?> objectClass, Object object, String fieldName, Object value) throws Exception {
		Field field = objectClass.getDeclaredField(fieldName);
		field.setAccessible(true);

		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

		field.set(object, value);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T getPrivateField(Object object, Class<T> fieldClass, String fieldName) throws Exception {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(object);
	}
	
}