package com.kenshoo.pl.entity;

import com.kenshoo.pl.entity.internal.audit.entitytypes.AuditedType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mockito.Mock;

import static com.kenshoo.pl.entity.ChangeOperation.CREATE;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class AuditRecordTest {

    private static final String ENTITY_ID_1 = "123";
    private static final String ENTITY_ID_2 = "456";

    private static final FieldAuditRecord<AuditedType> NAME_FIELD_RECORD =
        new FieldAuditRecord<>(AuditedType.NAME, null, "name");

    @Mock
    private AuditRecord<?> childRecord;

    @Test
    public void hasNoChanges_WithFieldRecords_WithChildRecords_ShouldReturnFalse() {
        final AuditRecord<AuditedType> auditRecord =
            new AuditRecord.Builder<AuditedType>()
                .withEntityType(AuditedType.INSTANCE)
                .withEntityId(ENTITY_ID_1)
                .withOperator(CREATE)
                .withFieldRecords(singleton(NAME_FIELD_RECORD))
                .withChildRecords(singleton(childRecord))
                .build();

        assertThat(auditRecord.hasNoChanges(), is(false));
    }

    @Test
    public void hasNoChanges_WithFieldRecords_WithoutChildRecords_ShouldReturnFalse() {
        final AuditRecord<AuditedType> auditRecord =
            new AuditRecord.Builder<AuditedType>()
                .withEntityType(AuditedType.INSTANCE)
                .withEntityId(ENTITY_ID_1)
                .withOperator(CREATE)
                .withFieldRecords(singleton(NAME_FIELD_RECORD))
                .build();

        assertThat(auditRecord.hasNoChanges(), is(false));
    }

    @Test
    public void hasNoChanges_WithoutFieldRecords_WithChildRecords_ShouldReturnFalse() {
        final AuditRecord<AuditedType> auditRecord =
            new AuditRecord.Builder<AuditedType>()
                .withEntityType(AuditedType.INSTANCE)
                .withEntityId(ENTITY_ID_1)
                .withOperator(CREATE)
                .withChildRecords(singleton(childRecord))
                .build();

        assertThat(auditRecord.hasNoChanges(), is(false));
    }

    @Test
    public void hasNoChanges_WithoutFieldRecords_WithoutChildRecords_ShouldReturnTrue() {
        final AuditRecord<AuditedType> auditRecord =
            new AuditRecord.Builder<AuditedType>()
                .withEntityType(AuditedType.INSTANCE)
                .withEntityId(ENTITY_ID_1)
                .withOperator(CREATE)
                .build();

        assertThat(auditRecord.hasNoChanges(), is(true));
    }

    @Test
    public void testToString_UnlimitedDepth_OneLevel() {
        final String auditRecordStr =
            new AuditRecord.Builder<AuditedType>()
                .withEntityType(AuditedType.INSTANCE)
                .withEntityId(ENTITY_ID_1)
                .withOperator(CREATE)
                .build()
                .toString();

        assertThat("The string representation must contain the entity id",
                   auditRecordStr, containsString(ENTITY_ID_1));
    }

    @Test
    public void testToString_UnlimitedDepth_TwoLevels() {
        final String auditRecordStr =
            new AuditRecord.Builder<AuditedType>()
                .withEntityType(AuditedType.INSTANCE)
                .withEntityId(ENTITY_ID_1)
                .withOperator(CREATE)
                .withChildRecords(singletonList(new AuditRecord.Builder<AuditedType>()
                                                    .withEntityType(AuditedType.INSTANCE)
                                                    .withEntityId(ENTITY_ID_2)
                                                    .withOperator(CREATE)
                                                    .build()))
                .build()
                .toString();

        assertThat("The string representation must contain the parent entity id",
                   auditRecordStr, containsString(ENTITY_ID_1));
        assertThat("The string representation must contain the child entity id",
                   auditRecordStr, containsString(ENTITY_ID_2));
    }

    @Test
    public void testToString_MaxDepthOne_OneLevel() {
        final String auditRecordStr =
            new AuditRecord.Builder<AuditedType>()
                .withEntityType(AuditedType.INSTANCE)
                .withEntityId(ENTITY_ID_1)
                .withOperator(CREATE)
                .withChildRecords(singletonList(new AuditRecord.Builder<AuditedType>()
                                                    .withEntityType(AuditedType.INSTANCE)
                                                    .withEntityId(ENTITY_ID_2)
                                                    .withOperator(CREATE)
                                                    .build()))
                .build()
                .toString(1);

        assertThat("The string representation must contain the parent entity id",
                   auditRecordStr, containsString(ENTITY_ID_1));
        assertThat("The string representation must NOT contain the child entity id",
                   auditRecordStr, not(containsString(ENTITY_ID_2)));
    }

    @Test
    public void testToString_MaxDepthOne_TwoLevels() {
        final String auditRecordStr =
            new AuditRecord.Builder<AuditedType>()
                .withEntityType(AuditedType.INSTANCE)
                .withEntityId(ENTITY_ID_1)
                .withOperator(CREATE)
                .build()
                .toString(1);

        assertThat("The string representation must contain the entity id",
                   auditRecordStr, containsString(ENTITY_ID_1));
    }

    @Test
    public void testToString_MaxDepthZero_ReturnsEmptyString() {
        final String auditRecordStr =
            new AuditRecord.Builder<AuditedType>()
                .withEntityType(AuditedType.INSTANCE)
                .withEntityId(ENTITY_ID_1)
                .withOperator(CREATE)
                .build()
                .toString(0);

        assertThat("The string representation must be empty when max depth is zero",
                   auditRecordStr, is(StringUtils.EMPTY));
    }
}