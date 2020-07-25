package com.kenshoo.pl.entity.internal.audit;

import com.google.common.collect.ImmutableSet;
import com.kenshoo.pl.entity.EntityField;
import com.kenshoo.pl.entity.internal.audit.entitytypes.AuditedType;
import com.kenshoo.pl.entity.internal.audit.entitytypes.NotAuditedAncestorType;
import org.junit.Test;

import java.util.Set;

import static com.kenshoo.pl.entity.internal.audit.AuditedFieldSet.builder;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

public class AuditedFieldSetTest {

    @Test
    public void getAllFields_IdOnly() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID).build();

        final Set<EntityField<?, ?>> expectedAllFields = singleton(AuditedType.ID);

        assertThat(auditedFieldSet.getAllFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllFields_IdAndExternalMandatory() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withExternalMandatoryFields(ImmutableSet.of(NotAuditedAncestorType.NAME, NotAuditedAncestorType.DESC))
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(AuditedType.ID,
                            NotAuditedAncestorType.NAME,
                            NotAuditedAncestorType.DESC);

        assertThat(auditedFieldSet.getAllFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllFields_IdAndSelfMandatory() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withSelfMandatoryFields(AuditedType.NAME, AuditedType.DESC)
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(AuditedType.ID,
                            AuditedType.NAME,
                            AuditedType.DESC);

        assertThat(auditedFieldSet.getAllFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllFields_IdAndOnChange() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withOnChangeFields(ImmutableSet.of(AuditedType.NAME, AuditedType.DESC))
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(AuditedType.ID,
                            AuditedType.NAME,
                            AuditedType.DESC);

        assertThat(auditedFieldSet.getAllFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllFields_AllTypes() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withExternalMandatoryFields(NotAuditedAncestorType.NAME, NotAuditedAncestorType.DESC)
                .withSelfMandatoryFields(AuditedType.NAME)
                .withOnChangeFields(AuditedType.DESC, AuditedType.DESC2)
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(AuditedType.ID,
                            NotAuditedAncestorType.NAME,
                            NotAuditedAncestorType.DESC,
                            AuditedType.NAME,
                            AuditedType.DESC,
                            AuditedType.DESC2);

        assertThat(auditedFieldSet.getAllFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllSelfFields_WhenHasOnChange() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withOnChangeFields(AuditedType.NAME, AuditedType.DESC)
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(AuditedType.NAME,
                            AuditedType.DESC);

        assertThat(auditedFieldSet.getAllSelfFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllSelfFields_WhenHasSelfMandatory() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withSelfMandatoryFields(AuditedType.NAME, AuditedType.DESC)
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(AuditedType.NAME,
                            AuditedType.DESC);

        assertThat(auditedFieldSet.getAllSelfFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllSelfFields_WhenHasOnChangeAndSelfMandatory() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withSelfMandatoryFields(AuditedType.NAME)
                .withOnChangeFields(AuditedType.DESC, AuditedType.DESC2)
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(AuditedType.NAME,
                            AuditedType.DESC,
                            AuditedType.DESC2);

        assertThat(auditedFieldSet.getAllSelfFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllSelfFields_WhenHasNone() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID).build();

        assertThat(auditedFieldSet.getAllSelfFields().collect(toSet()), is(empty()));
    }

    @Test
    public void getAllMandatoryFields_WhenHasExternalMandatory() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withExternalMandatoryFields(NotAuditedAncestorType.NAME, NotAuditedAncestorType.DESC)
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(NotAuditedAncestorType.NAME, NotAuditedAncestorType.DESC);

        assertThat(auditedFieldSet.getAllMandatoryFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllMandatoryFields_WhenHasSelfMandatory() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withExternalMandatoryFields(NotAuditedAncestorType.NAME, NotAuditedAncestorType.DESC)
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(NotAuditedAncestorType.NAME, NotAuditedAncestorType.DESC);

        assertThat(auditedFieldSet.getAllMandatoryFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllMandatoryFields_WhenHasExternalAndSelfMandatory() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withExternalMandatoryFields(NotAuditedAncestorType.NAME, NotAuditedAncestorType.DESC)
                .withSelfMandatoryFields(AuditedType.NAME,AuditedType.DESC)
                .build();

        final Set<EntityField<?, ?>> expectedAllFields =
            ImmutableSet.of(NotAuditedAncestorType.NAME,
                            NotAuditedAncestorType.DESC,
                            AuditedType.NAME,
                            AuditedType.DESC);

        assertThat(auditedFieldSet.getAllMandatoryFields().collect(toSet()), is(expectedAllFields));
    }

    @Test
    public void getAllMandatoryFields_WhenHasNoMandatory() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withOnChangeFields(AuditedType.NAME, AuditedType.DESC)
                .build();

        assertThat(auditedFieldSet.getAllMandatoryFields().collect(toSet()), is(empty()));
    }

    @Test
    public void hasSelfFields_WhenHasOnChangeFields_ShouldReturnTrue() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withOnChangeFields(ImmutableSet.of(AuditedType.NAME,
                                                    AuditedType.DESC))
                .build();

        assertThat(auditedFieldSet.hasSelfFields(), is(true));
    }

    @Test
    public void hasSelfFields_WhenHasSelfMandatoryFields_ShouldReturnTrue() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withSelfMandatoryFields(ImmutableSet.of(AuditedType.NAME,
                                                         AuditedType.DESC))
                .build();

        assertThat(auditedFieldSet.hasSelfFields(), is(true));
    }

    @Test
    public void hasSelfFields_WhenHasIdOnly_ShouldReturnFalse() {
        final AuditedFieldSet<AuditedType> auditedFieldSet = builder(AuditedType.ID).build();

        assertThat(auditedFieldSet.hasSelfFields(), is(false));
    }

    @Test
    public void hasSelfFields_WhenHasIdAndExternalMandatoryOnly_ShouldReturnFalse() {
        final AuditedFieldSet<AuditedType> auditedFieldSet =
            builder(AuditedType.ID)
                .withExternalMandatoryFields(NotAuditedAncestorType.NAME)
                .build();

        assertThat(auditedFieldSet.hasSelfFields(), is(false));
    }
}