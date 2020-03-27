package com.kenshoo.pl.changelog.entity;

import com.kenshoo.pl.entity.EntityField;
import com.kenshoo.pl.entity.EntityType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class EntityFieldChangeRecord<E extends EntityType<E>> {
    private final EntityField<E, ?> field;
    private final Object oldValue;
    private final Object newValue;

    public EntityFieldChangeRecord(final EntityField<E, ?> field,
                                   final Object oldValue,
                                   final Object newValue) {
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public EntityField<E, ?> getField() {
        return field;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        @SuppressWarnings("unchecked")
        final EntityFieldChangeRecord<E> that = (EntityFieldChangeRecord<E>) o;

        return new EqualsBuilder()
            .append(field, that.field)
            .append(oldValue, that.oldValue)
            .append(newValue, that.newValue)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(field)
            .append(oldValue)
            .append(newValue)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("fieldName", field)
            .append("oldValue", oldValue)
            .append("newValue", newValue)
            .toString();
    }
}
