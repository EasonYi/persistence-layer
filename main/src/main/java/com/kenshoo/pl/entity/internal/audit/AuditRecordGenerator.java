package com.kenshoo.pl.entity.internal.audit;

import com.google.common.annotations.VisibleForTesting;
import com.kenshoo.pl.entity.*;
import com.kenshoo.pl.entity.internal.EntityIdExtractor;
import com.kenshoo.pl.entity.spi.CurrentStateConsumer;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.kenshoo.pl.entity.ChangeOperation.UPDATE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class AuditRecordGenerator<E extends EntityType<E>> implements CurrentStateConsumer<E> {

    private final AuditedFieldSet<E> auditedFieldSet;
    private final EntityIdExtractor entityIdExtractor;

    public AuditRecordGenerator(final AuditedFieldSet<E> auditedFieldSet) {
        this(auditedFieldSet,
             EntityIdExtractor.INSTANCE);
    }

    @VisibleForTesting
    AuditRecordGenerator(final AuditedFieldSet<E> auditedFieldSet,
                         final EntityIdExtractor entityIdExtractor) {
        this.auditedFieldSet = requireNonNull(auditedFieldSet, "An audited field set is required");
        this.entityIdExtractor = entityIdExtractor;
    }

    @Override
    public Stream<? extends EntityField<?, ?>> requiredFields(final Collection<? extends EntityField<E, ?>> fieldsToUpdate,
                                                              final ChangeOperation operator) {
        return auditedFieldSet.intersectWith(fieldsToUpdate.stream())
                              .getAllFields();
    }

    public Optional<? extends AuditRecord<E>> generate(final EntityChange<E> entityChange,
                                                       final Entity entity,
                                                       final Collection<? extends AuditRecord<?>> childRecords) {
        final AuditRecord<E> auditRecord = generateInner(entityChange,
                                                         entity,
                                                         childRecords);

        if (entityChange.getChangeOperation() == UPDATE && auditRecord.hasNoChanges()) {
            return Optional.empty();
        }
        return Optional.of(auditRecord);
    }

    private AuditRecord<E> generateInner(final EntityChange<E> entityChange,
                                         final Entity entity,
                                         final Collection<? extends AuditRecord<?>> childRecords) {
        requireNonNull(entityChange, "entityChange is required");
        requireNonNull(entity, "entity is required");

        final String entityId = extractEntityId(entityChange, entity);

        final Set<? extends EntityField<E, ?>> candidateDataFields = auditedFieldSet.intersectWith(entityChange.getChangedFields())
                                                                                    .getDataFields();

        final Collection<? extends FieldAuditRecord<E>> fieldRecords = generateFieldRecords(entityChange,
                                                                                            entity,
                                                                                            candidateDataFields);

        return new AuditRecord.Builder<E>()
            .withEntityType(entityChange.getEntityType())
            .withEntityId(entityId)
            .withOperator(entityChange.getChangeOperation())
            .withFieldRecords(fieldRecords)
            .withChildRecords(childRecords)
            .build();
    }

    private Collection<? extends FieldAuditRecord<E>> generateFieldRecords(final EntityChange<E> entityChange,
                                                                           final Entity entity,
                                                                           final Collection<? extends EntityField<E, ?>> candidateFields) {
        return candidateFields.stream()
                              .filter(field -> fieldWasChanged(entityChange, entity, field))
                              .map(field -> buildFieldChangeRecord(entityChange, entity, field))
                              .collect(toList());
    }

    private FieldAuditRecord<E> buildFieldChangeRecord(final EntityChange<E> entityChange,
                                                       final Entity entity,
                                                       final EntityField<E, ?> field) {
        return new FieldAuditRecord<>(field,
                                      entity.containsField(field) ? entity.get(field) : null,
                                      entityChange.get(field));
    }

    private String extractEntityId(final EntityChange<E> entityChange,
                                   final Entity entity) {
        return entityIdExtractor.extract(entityChange, entity)
                                .orElseThrow(() -> new IllegalStateException("Could not extract the entity id for entity type '" + entityChange.getEntityType() + "' " +
                                                                                 "from either the EntityChange or the Entity, so the audit record cannot be generated."));
    }

    private boolean fieldWasChanged(final EntityChange<E> entityChange,
                                    final Entity entity,
                                    final EntityField<E, ?> field) {
        return !fieldStayedTheSame(entityChange, entity, field);
    }

    private boolean fieldStayedTheSame(final EntityChange<E> entityChange,
                                       final Entity entity,
                                       final EntityField<E, ?> field) {
        return entity.containsField(field) && fieldValuesEqual(entityChange, entity, field);
    }

    private <T> boolean fieldValuesEqual(final EntityChange<E> entityChange,
                                         final Entity entity,
                                         final EntityField<E, T> field) {
        return field.valuesEqual(entityChange.get(field), entity.get(field));
    }

    @VisibleForTesting
    public AuditedFieldSet<E> getAuditedFieldSet() {
        return auditedFieldSet;
    }
}