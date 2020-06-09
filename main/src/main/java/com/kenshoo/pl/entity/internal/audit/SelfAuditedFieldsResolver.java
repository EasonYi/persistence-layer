package com.kenshoo.pl.entity.internal.audit;

import com.kenshoo.pl.entity.EntityField;
import com.kenshoo.pl.entity.EntityType;
import com.kenshoo.pl.entity.annotation.audit.Audited;
import com.kenshoo.pl.entity.annotation.audit.NotAudited;
import com.kenshoo.pl.entity.audit.AuditTrigger;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kenshoo.pl.entity.audit.AuditTrigger.ALWAYS;
import static com.kenshoo.pl.entity.audit.AuditTrigger.ON_CHANGE;
import static com.kenshoo.pl.entity.internal.EntityTypeReflectionUtil.getFieldAnnotation;
import static com.kenshoo.pl.entity.internal.EntityTypeReflectionUtil.isAnnotatedWith;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.jooq.lambda.Seq.seq;

class SelfAuditedFieldsResolver implements AuditedFieldsResolver {

    static final SelfAuditedFieldsResolver INSTANCE = new SelfAuditedFieldsResolver();

    @Override
    public <E extends EntityType<E>> Optional<AuditedFieldSet<E>> resolve(final E entityType) {
        requireNonNull(entityType, "entityType is required");
        return entityType.getIdField()
                         .flatMap(idField -> resolve(entityType, idField));
    }

    private <E extends EntityType<E>> Optional<AuditedFieldSet<E>> resolve(final E entityType, final EntityField<E, ? extends Number> idField) {
        final Stream<? extends EntityField<E, ?>> nonIdFields = entityType.getFields()
                                                                          .filter(field -> !idField.equals(field));

        final boolean entityLevelAudited = entityType.getClass().isAnnotationPresent(Audited.class);
        final Collection<AuditedFieldTrigger<E>> fieldTriggers = resolveFieldTriggers(nonIdFields, entityLevelAudited);

        final AuditedFieldSet<E> fieldSet = AuditedFieldSet.builder(idField)
                                                           .withSelfMandatoryFields(filterFieldsByTrigger(fieldTriggers, ALWAYS))
                                                           .withOnChangeFields(filterFieldsByTrigger(fieldTriggers, ON_CHANGE))
                                                           .build();

        if (entityLevelAudited || fieldSet.hasSelfFields()) {
            return Optional.of(fieldSet);
        }
        return empty();
    }

    private <E extends EntityType<E>> Collection<AuditedFieldTrigger<E>> resolveFieldTriggers(
        final Stream<? extends EntityField<E, ?>> nonIdFields,
        final boolean entityLevelAudited) {

        return nonIdFields.filter(field -> !isAnnotatedWith(field.getEntityType(), NotAudited.class, field))
                          .map(field -> resolveFieldTrigger(field, entityLevelAudited))
                          .filter(Optional::isPresent)
                          .map(Optional::get)
                          .collect(toList());
    }

    private <E extends EntityType<E>> Optional<AuditedFieldTrigger<E>> resolveFieldTrigger(final EntityField<E, ?> field,
                                                                                           final boolean entityLevelAudited) {
        final Optional<AuditTrigger> optionalEntityTrigger = entityLevelAudited ? Optional.of(ON_CHANGE) : Optional.empty();
        return Stream.of(extractFieldTrigger(field),
                         optionalEntityTrigger)
                     .filter(Optional::isPresent)
                     .map(Optional::get)
                     .map(trigger -> new AuditedFieldTrigger<>(field, trigger))
                     .findFirst();
    }

    private <E extends EntityType<E>> Optional<AuditTrigger> extractFieldTrigger(final EntityField<E, ?> field) {
        return Optional.ofNullable(getFieldAnnotation(field.getEntityType(), field, Audited.class))
                       .map(Audited::trigger);
    }

    private <E extends EntityType<E>> Iterable<? extends EntityField<E, ?>> filterFieldsByTrigger(final Collection<AuditedFieldTrigger<E>> fieldTriggers,
                                                                                                  final AuditTrigger trigger) {
        return seq(fieldTriggers.stream()
                                .filter(fieldTrigger -> fieldTrigger.getTrigger().equals(trigger))
                                .map(AuditedFieldTrigger::getField));
    }

    private SelfAuditedFieldsResolver() {
        // Singleton
    }
}
