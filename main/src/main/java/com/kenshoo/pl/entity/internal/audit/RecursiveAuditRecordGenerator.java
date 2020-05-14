package com.kenshoo.pl.entity.internal.audit;

import com.kenshoo.pl.entity.*;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class RecursiveAuditRecordGenerator {

    public <E extends EntityType<E>> Stream<? extends AuditRecord<E>> generateMany(
        final ChangeFlowConfig<E> flowConfig,
        final Stream<? extends EntityChange<E>> entityChanges,
        final ChangeContext changeContext) {

        //noinspection RedundantTypeArguments
        return flowConfig.auditRecordGenerator()
                         .map(auditRecordGenerator -> this.<E>generateMany(flowConfig,
                                                                           auditRecordGenerator,
                                                                           entityChanges,
                                                                           changeContext))
                         .orElse(Stream.empty());
    }

    private <E extends EntityType<E>> Stream<? extends AuditRecord<E>> generateMany(
        final ChangeFlowConfig<E> flowConfig,
        final AuditRecordGenerator<E> auditRecordGenerator,
        final Stream<? extends EntityChange<E>> entityChanges,
        final ChangeContext changeContext) {

        //noinspection RedundantTypeArguments
        return entityChanges.map(entityChange -> this.<E>generateOne(flowConfig,
                                                                     auditRecordGenerator,
                                                                     entityChange,
                                                                     changeContext))
                            .filter(Optional::isPresent)
                            .map(Optional::get);
    }

    private <E extends EntityType<E>> Optional<? extends AuditRecord<E>> generateOne(
        final ChangeFlowConfig<E> flowConfig,
        final AuditRecordGenerator<E> auditRecordGenerator,
        final EntityChange<E> entityChange,
        final ChangeContext changeContext) {

        final Collection<? extends AuditRecord<?>> childAuditRecords =
            flowConfig.childFlows().stream()
                      .flatMap(childFlowConfig -> generateChildrenUntyped(childFlowConfig,
                                                                          entityChange,
                                                                          changeContext))
                      .collect(toList());

        return auditRecordGenerator.generate(entityChange,
                                             changeContext.getEntity(entityChange),
                                             childAuditRecords);
    }

    private <E extends EntityType<E>> Stream<? extends AuditRecord<? extends EntityType<?>>> generateChildrenUntyped(
        final ChangeFlowConfig<? extends EntityType<?>> childFlowConfig,
        final EntityChange<E> entityChange,
        final ChangeContext changeContext) {

        return generateChildrenTyped(childFlowConfig,
                                     entityChange,
                                     changeContext);
    }

    private <P extends EntityType<P>, C extends EntityType<C>> Stream<? extends AuditRecord<C>> generateChildrenTyped(
        final ChangeFlowConfig<C> childFlowConfig,
        final EntityChange<P> entityChange,
        final ChangeContext changeContext) {

        return generateMany(childFlowConfig,
                            entityChange.getChildren(childFlowConfig.getEntityType()),
                            changeContext);
    }
}