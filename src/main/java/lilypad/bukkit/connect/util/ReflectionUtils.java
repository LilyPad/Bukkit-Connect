package lilypad.bukkit.connect.util;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public class ReflectionUtils {

	private static final Field FIELD_MODIFIERS;
	private static final Field FIELD_ACCESSSOR;
	private static final Field FIELD_ACCESSSOR_OVERRIDE;
	private static final Field FIELD_ROOT;

	static {
		if (!System.getProperty("java.version").startsWith("1.8")) {
			try {
				getFilterMap(Field.class).clear();
			} catch (Exception exception) {
				exception.printStackTrace();
			}
		}

		Field fieldModifiers = null;
		Field fieldAccessor = null;
		Field fieldAccessorOverride = null;
		Field fieldRoot = null;
		try {
			fieldModifiers = Field.class.getDeclaredField("modifiers");
			fieldModifiers.setAccessible(true);
			fieldAccessor = Field.class.getDeclaredField("fieldAccessor");
			fieldAccessor.setAccessible(true);
			fieldAccessorOverride = Field.class.getDeclaredField("overrideFieldAccessor");
			fieldAccessorOverride.setAccessible(true);
			fieldRoot = Field.class.getDeclaredField("root");
			fieldRoot.setAccessible(true);
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		FIELD_MODIFIERS = fieldModifiers;
		FIELD_ACCESSSOR = fieldAccessor;
		FIELD_ACCESSSOR_OVERRIDE = fieldAccessorOverride;
		FIELD_ROOT = fieldRoot;
	}

	public static void setFinalField(Class<?> objectClass, Object object, String fieldName, Object value) throws Exception {
		Field field = objectClass.getDeclaredField(fieldName);
		field.setAccessible(true);

		if (Modifier.isFinal(field.getModifiers())) {
			FIELD_MODIFIERS.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			Field currentField = field;
			do {
				FIELD_ACCESSSOR.set(currentField, null);
				FIELD_ACCESSSOR_OVERRIDE.set(currentField, null);
			} while((currentField = (Field) FIELD_ROOT.get(currentField)) != null);
		}
		
		field.set(object, value);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getPrivateField(Class<?> objectClass, Object object, Class<T> fieldClass, String fieldName) throws Exception {
		Field field = objectClass.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(object);
	}

	@SuppressWarnings("unchecked")
	public static <T extends AccessibleObject & Member> Map<Class<?>, String[]> getFilterMap(Class<T> clazz) throws Exception {
		if (Constructor.class.isAssignableFrom(clazz)) {
			return null;
		}
		Method getDeclaredFields0M = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
		getDeclaredFields0M.setAccessible(true);
		Field[] fields = (Field[]) getDeclaredFields0M.invoke(Class.forName("jdk.internal.reflect.Reflection"), false);
		Field field = null;
		for (Field f : fields) {
			if (f.getName().equals(clazz.getSimpleName().toLowerCase() + "FilterMap")) {
				field = f;
			}
		}
		Method setAccessible0M = AccessibleObject.class.getDeclaredMethod("setAccessible0", boolean.class);
		setAccessible0M.setAccessible(true);
		setAccessible0M.invoke(field, true);
		return (Map<Class<?>, String[]>) field.get(null);
	}

}
