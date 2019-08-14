package com.kenshoo.pl.entity.spi;

import com.kenshoo.pl.entity.ChangeOperation;
import com.kenshoo.pl.entity.Entity;
import com.kenshoo.pl.entity.EntityField;
import com.kenshoo.pl.entity.EntityType;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A "delayed" supplier of the new value for a field that decides on the value given the current state. For example,
 * a logic to increment the current value by 10% can be implemented by implementing this interface. The {@link #supply(Entity)}
 * method is going to be called after the current state has been fetched from the database.
 *
 * @param <T> type of the field whose value is being supplied
 * @see com.kenshoo.pl.entity.ChangeEntityCommand#set(EntityField, FieldValueSupplier)
 */
public interface FieldValueSupplier<T> extends FetchEntityFields {

    /**
     * Returns the new value for a field given an existing entity
     *
     * @param entity entity before the change
     * @return new field value
     * @throws ValidationException if the supposed change is invalid
     * @throws NotSuppliedException if the supplier doesn't want to change the current value
     */
    T supply(Entity entity) throws ValidationException, NotSuppliedException;

    static <E extends EntityType<E>, T> FieldValueSupplier<T> fromOldValue(EntityField<E, T> field, Function<T, T> func) {
        return new FieldValueSupplier<T>() {
            @Override
            public T supply(Entity oldState) throws ValidationException, NotSuppliedException {
                return func.apply(oldState.get(field));
            }
            @Override
            public Stream<EntityField<?, ?>> fetchFields(ChangeOperation changeOperation) {
                return Stream.of(field);
            }
        };
    }


}
