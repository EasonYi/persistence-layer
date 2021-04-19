package com.kenshoo.pl.entity.internal.audit;

import com.kenshoo.pl.entity.Entity;
import com.kenshoo.pl.entity.Triptional;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class AuditFieldValueResolver {

    public static final AuditFieldValueResolver INSTANCE = new AuditFieldValueResolver();

    public <T> Triptional<T> resolve(final AuditedField<?, T> auditedField,
                                     final Entity entity) {
        requireNonNull(entity, "entity is required");
        return entity.safeGet(auditedField.getField());
    }

    public <T> Triptional<String> resolveToString(final AuditedField<?, T> auditedField,
                                                  final Entity entity) {

        return resolve(auditedField, entity)
            .map(value -> valueToString(auditedField, value));
    }

    private <T> String valueToString(final AuditedField<?, T> auditedField,
                                     final T value) {
        return Optional.ofNullable(auditedField.getStringValueConverter())
                       .map(converter -> converter.convertTo(value))
                       .orElse(String.valueOf(value));
    }

    private AuditFieldValueResolver() {
        // singleton
    }
}
