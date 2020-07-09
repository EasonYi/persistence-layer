package com.kenshoo.pl.entity.internal.audit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.kenshoo.pl.entity.CurrentEntityMutableState;
import com.kenshoo.pl.entity.CurrentEntityState;
import com.kenshoo.pl.entity.EntityField;
import com.kenshoo.pl.entity.audit.AuditRecord;
import com.kenshoo.pl.entity.internal.EntityIdExtractor;
import com.kenshoo.pl.entity.CurrentEntityState;
import com.kenshoo.pl.entity.internal.audit.entitytypes.AuditedType;
import com.kenshoo.pl.entity.internal.audit.entitytypes.NotAuditedAncestorType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAnd;
import static com.kenshoo.pl.entity.ChangeOperation.DELETE;
import static com.kenshoo.pl.entity.matchers.audit.AuditRecordMatchers.*;
import static com.kenshoo.pl.matchers.IterableStreamMatcher.eqStreamAsSet;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AuditRecordGeneratorForDeleteTest {

    private static final long ID = 1234;
    private static final String STRING_ID = String.valueOf(ID);
    private static final String ANCESTOR_NAME = "ancestorName";
    private static final String ANCESTOR_DESC = "ancestorDesc";

    @Mock
    private AuditedFieldSet<AuditedType> completeFieldSet;

    @Mock
    private EntityIdExtractor entityIdExtractor;

    @InjectMocks
    private AuditRecordGenerator<AuditedType> auditRecordGenerator;

    @Test
    public void generate_WithIdOnly_ShouldGenerateMandatoryData() {
        final AuditedCommand cmd = new AuditedCommand(ID, DELETE);

        final CurrentEntityMutableState currentState = new CurrentEntityMutableState();
        currentState.set(AuditedType.NAME, "oldName");
        currentState.set(AuditedType.DESC, "oldDesc");

        when(completeFieldSet.intersectWith(eqStreamAsSet(emptySet()))).thenReturn(AuditedFieldSet.builder(AuditedType.ID).build());
        doReturn(Optional.of(STRING_ID)).when(entityIdExtractor).extract(cmd, currentState);

        final Optional<? extends AuditRecord<AuditedType>> actualOptionalAuditRecord =
            auditRecordGenerator.generate(cmd, currentState, emptyList());

        assertThat(actualOptionalAuditRecord,
                   isPresentAnd(allOf(hasEntityType(AuditedType.INSTANCE),
                                      hasEntityId(STRING_ID),
                                      hasOperator(DELETE))));
    }

    @Test
    public void generate_WithMandatoryFieldsOnly_ShouldGenerateBasicDataAndMandatoryFields() {
        final AuditedCommand cmd = new AuditedCommand(ID, DELETE)
            .with(AuditedType.NAME, "name");
        final Set<? extends EntityField<AuditedType, ?>> cmdChangedFields = cmd.getChangedFields().collect(toSet());

        final CurrentEntityMutableState currentState = new CurrentEntityMutableState();
        currentState.set(NotAuditedAncestorType.NAME, ANCESTOR_NAME);
        currentState.set(NotAuditedAncestorType.DESC, ANCESTOR_DESC);

        final AuditedFieldSet<AuditedType> expectedIntersectionFieldSet =
            AuditedFieldSet.builder(AuditedType.ID)
                           .withOnChangeFields(singleton(AuditedType.NAME))
                           .build();

        when(completeFieldSet.intersectWith(eqStreamAsSet(cmdChangedFields))).thenReturn(expectedIntersectionFieldSet);
        //noinspection ResultOfMethodCallIgnored
        doReturn(ImmutableSet.of(NotAuditedAncestorType.NAME, NotAuditedAncestorType.DESC))
            .when(completeFieldSet).getExternalMandatoryFields();
        doReturn(Optional.of(STRING_ID)).when(entityIdExtractor).extract(cmd, currentState);

        final List<AuditRecord<?>> childRecords = ImmutableList.of(mockChildRecord(), mockChildRecord());

        final Optional<? extends AuditRecord<AuditedType>> actualOptionalAuditRecord =
            auditRecordGenerator.generate(cmd, currentState, childRecords);

        assertThat(actualOptionalAuditRecord,
                   isPresentAnd(allOf(hasEntityType(AuditedType.INSTANCE),
                                      hasEntityId(STRING_ID),
                                      hasOperator(DELETE),
                                      hasMandatoryFieldValue(NotAuditedAncestorType.NAME, ANCESTOR_NAME),
                                      hasMandatoryFieldValue(NotAuditedAncestorType.DESC, ANCESTOR_DESC))));
    }

    @Test
    public void generate_WithChildRecordsOnly_ShouldGenerateBasicDataAndChildRecords() {
        final AuditedCommand cmd = new AuditedCommand(ID, DELETE)
            .with(AuditedType.NAME, "name");
        final Set<? extends EntityField<AuditedType, ?>> cmdChangedFields = cmd.getChangedFields().collect(toSet());

        final CurrentEntityState currentState = CurrentEntityState.EMPTY;

        final AuditedFieldSet<AuditedType> expectedIntersectionFieldSet =
            AuditedFieldSet.builder(AuditedType.ID)
                           .withOnChangeFields(singleton(AuditedType.NAME))
                           .build();

        when(completeFieldSet.intersectWith(eqStreamAsSet(cmdChangedFields))).thenReturn(expectedIntersectionFieldSet);
        doReturn(Optional.of(STRING_ID)).when(entityIdExtractor).extract(cmd, currentState);

        final List<AuditRecord<?>> childRecords = ImmutableList.of(mockChildRecord(), mockChildRecord());

        final Optional<? extends AuditRecord<AuditedType>> actualOptionalAuditRecord =
            auditRecordGenerator.generate(cmd, currentState, childRecords);

        assertThat(actualOptionalAuditRecord,
                   isPresentAnd(allOf(hasEntityType(AuditedType.INSTANCE),
                                      hasEntityId(STRING_ID),
                                      hasOperator(DELETE),
                                      hasSameChildRecord(childRecords.get(0)),
                                      hasSameChildRecord(childRecords.get(1)))));
    }

    @Test
    public void generate_WithMandatoryFieldsAndChildRecords_ShouldGenerateBoth() {
        final AuditedCommand cmd = new AuditedCommand(ID, DELETE)
            .with(AuditedType.NAME, "name");
        final Set<? extends EntityField<AuditedType, ?>> cmdChangedFields = cmd.getChangedFields().collect(toSet());

        final CurrentEntityMutableState currentState = new CurrentEntityMutableState();
        currentState.set(NotAuditedAncestorType.NAME, ANCESTOR_NAME);
        currentState.set(NotAuditedAncestorType.DESC, ANCESTOR_DESC);

        final AuditedFieldSet<AuditedType> expectedIntersectionFieldSet =
            AuditedFieldSet.builder(AuditedType.ID)
                           .withOnChangeFields(singleton(AuditedType.NAME))
                           .build();

        when(completeFieldSet.intersectWith(eqStreamAsSet(cmdChangedFields))).thenReturn(expectedIntersectionFieldSet);
        //noinspection ResultOfMethodCallIgnored
        doReturn(ImmutableSet.of(NotAuditedAncestorType.NAME, NotAuditedAncestorType.DESC))
            .when(completeFieldSet).getExternalMandatoryFields();
        doReturn(Optional.of(STRING_ID)).when(entityIdExtractor).extract(cmd, currentState);

        final List<AuditRecord<?>> childRecords = ImmutableList.of(mockChildRecord(), mockChildRecord());

        final Optional<? extends AuditRecord<AuditedType>> actualOptionalAuditRecord =
            auditRecordGenerator.generate(cmd, currentState, childRecords);

        //noinspection unchecked
        assertThat(actualOptionalAuditRecord,
                   isPresentAnd(allOf(hasEntityType(AuditedType.INSTANCE),
                                      hasEntityId(STRING_ID),
                                      hasOperator(DELETE),
                                      hasMandatoryFieldValue(NotAuditedAncestorType.NAME, ANCESTOR_NAME),
                                      hasMandatoryFieldValue(NotAuditedAncestorType.DESC, ANCESTOR_DESC),
                                      hasSameChildRecord(childRecords.get(0)),
                                      hasSameChildRecord(childRecords.get(1)))));
    }

    private AuditRecord<?> mockChildRecord() {
        return mock(AuditRecord.class);
    }
}