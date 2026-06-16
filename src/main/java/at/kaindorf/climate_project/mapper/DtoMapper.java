package at.kaindorf.climate_project.mapper;

import at.kaindorf.climate_project.annotations.ToDto;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static Map<String, Object> map(Object source) {
        Map<String, Object> dto = new LinkedHashMap<>();
        Field[] fields = source.getClass().getDeclaredFields();

        for (Field field : fields) {
            if (!field.isAnnotationPresent(ToDto.class)) {
                continue;
            }

            field.setAccessible(true);

            try {
                dto.put(field.getName(), field.get(source));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not map field to DTO: " + field.getName(), e);
            }
        }

        return dto;
    }
}

