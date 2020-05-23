package com.kenshoo.pl.entity.annotation.audit;

import com.kenshoo.pl.entity.audit.AuditRecord;
import com.kenshoo.pl.entity.spi.audit.AncestorFieldsProvider;
import com.kenshoo.pl.entity.spi.audit.AncestorFieldsProvider.EmptyAncestorFieldsProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Whenever an entity or field has this annotation, it indicates that any changes to the entity / field
 * will be published (by the publisher belonging to the PersistenceLayer instance)
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * A provider for additional fields from ancestor entities that should be added to an {@link AuditRecord}.<br>
     * This attribute is valid for entity-level annotations only, and will be ignored if appearing on fields.
     */
    Class<? extends AncestorFieldsProvider> ancestorFieldsProvider() default EmptyAncestorFieldsProvider.class;
}