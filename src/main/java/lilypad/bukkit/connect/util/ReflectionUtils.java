package lilypad.bukkit.connect.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionUtils {

	private static final Field FIELD_MODIFIERS;

	static {
		Field field = null;
		try {
			field = Field.class.getDeclaredField("modifiers");
			field.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace(); // What? Why?
		}
		FIELD_MODIFIERS = field;
	}

	public static void setFinalField(Class<?> objectClass, Object object, String fieldName, Object value) throws Exception {
		Field field = objectClass.getDeclaredField(fieldName);
		field.setAccessible(true);

		if (Modifier.isFinal(field.getModifiers())) {
			FIELD_MODIFIERS.setInt(field, field.getModifiers() & ~Modifier.FINAL);
		}

		field.set(object, value);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getPrivateField(Object object, Class<T> fieldClass, String fieldName) throws Exception {
		Field field = object.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(object);
	}

}