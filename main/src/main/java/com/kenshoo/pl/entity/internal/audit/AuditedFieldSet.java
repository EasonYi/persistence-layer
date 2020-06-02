package com.kenshoo.pl.entity.internal.audit;

import com.google.common.collect.ImmutableSet;
import com.kenshoo.pl.entity.EntityField;
import com.kenshoo.pl.entity.EntityType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jooq.lambda.Seq;

import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

public class AuditedFieldSet<E extends EntityType<E>> {

    private final EntityField<E, ? extends Number> idField;
    // Fields included always in the audit record with their current values (not necessarily from current entityType)
    private final Set<? extends EntityField<?, ?>> mandatoryFields;
    // Fields included in the audit record only when changed, with their old and new values (current entityType only)
    private final Set<? extends EntityField<E, ?>> onChangeFields;

    private AuditedFieldSet(final EntityField<E, ? extends Number> idField,
                            final Set<? extends EntityField<?, ?>> mandatoryFields,
                            final Set<? extends EntityField<E, ?>> onChangeFields) {
        this.idField = idField;
        this.mandatoryFields = mandatoryFields;
        this.onChangeFields = onChangeFields;
    }

    public EntityField<E, ? extends Number> getIdField() {
        return idField;
    }

    public Set<? extends EntityField<?, ?>> getMandatoryFields() {
        return mandatoryFields;
    }

    public Set<? extends EntityField<E, ?>> getOnChangeFields() {
        return onChangeFields;
    }

    public Stream<? extends EntityField<?, ?>> getAllFields() {
        return Stream.of(singleton(idField),
                         mandatoryFields,
                         onChangeFields)
                     .flatMap(Set::stream);
    }

    public AuditedFieldSet<E> intersectWith(final Stream<? extends EntityField<E, ?>> fields) {
        return builder(idField)
            .withMandatoryFields(mandatoryFields)
            .withOnChangeFields(Seq.seq(fields).filter(onChangeFields::contains))
            .build();
    }

    public static <E extends EntityType<E>> Builder<E> builder(final EntityField<E, ? extends Number> idField) {
        return new Builder<>(idField);
    }

    public static class Builder<E extends EntityType<E>> {
        private final EntityField<E, ? extends Number> idField;
        private Set<? extends EntityField<?, ?>> mandatoryFields = emptySet();
        private Set<? extends EntityField<E, ?>> onChangeFields = emptySet();

        public Builder(EntityField<E, ? extends Number> idField) {
            this.idField = requireNonNull(idField, "idField is required");
        }

        public Builder<E> withMandatoryFields(final EntityField<?, ?>... mandatoryFields) {
            this.mandatoryFields = mandatoryFields == null ? emptySet() : ImmutableSet.copyOf(mandatoryFields);
            return this;
        }

        public Builder<E> withMandatoryFields(final Iterable<? extends EntityField<?, ?>> mandatoryFields) {
            this.mandatoryFields = mandatoryFields == null ? emptySet() : ImmutableSet.copyOf(mandatoryFields);
            return this;
        }

        public Builder<E> withOnChangeFields(final Iterable<? extends EntityField<E, ?>> onChangeFields) {
            this.onChangeFields = onChangeFields == null ? emptySet() : ImmutableSet.copyOf(onChangeFields);
            return this;
        }

        public AuditedFieldSet<E> build() {
            return new AuditedFieldSet<>(idField, mandatoryFields, onChangeFields);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AuditedFieldSet<?> that = (AuditedFieldSet<?>) o;

        return new EqualsBuilder()
            .append(idField, that.idField)
            .append(mandatoryFields, that.mandatoryFields)
            .append(onChangeFields, that.onChangeFields)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(idField)
            .append(mandatoryFields)
            .append(onChangeFields)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("idField", idField)
            .append("mandatoryFields", mandatoryFields)
            .append("onChangeFields", onChangeFields)
            .toString();
    }

}
