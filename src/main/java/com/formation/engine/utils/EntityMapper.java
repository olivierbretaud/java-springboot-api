package com.formation.engine.utils;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class EntityMapper {

    public <E, D> D toDto(E entity, Function<E, D> mapper) {
        return entity == null ? null : mapper.apply(entity);
    }

    public <E, D> List<D> toDtoList(List<E> entities, Function<E, D> mapper) {
        if (entities == null) return List.of();
        return entities.stream().map(mapper).collect(Collectors.toList());
    }

    public <D, E> E toEntity(D dto, Function<D, E> mapper) {
        return dto == null ? null : mapper.apply(dto);
    }
}
