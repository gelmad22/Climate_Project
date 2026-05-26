package at.kaindorf.climate_project.mapper;

import at.kaindorf.climate_project.annotations.ToDto;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DtoMapper {
    public static Map<String, Object> map(Object o) {
        Map<String, Object> dto = new HashMap<>();
        Field[] fields = o.getClass().getDeclaredFields();

        for (Field field : fields) {
            if(!field.isAnnotationPresent(ToDto.class)){
                continue;
            }

            field.setAccessible(true);

            try {
                dto.put(field.getName(), field.get(o));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return dto;
    }
}

