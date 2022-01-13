package com.kenshoo.pl.entity.internal.audit.entitytypes;

import com.kenshoo.pl.entity.EntityField;
import com.kenshoo.pl.entity.annotation.Id;
import com.kenshoo.pl.entity.annotation.audit.Audited;
import com.kenshoo.pl.entity.internal.audit.MainTable;
import com.kenshoo.pl.entity.internal.audit.formatters.CustomAuditFieldValueFormatter1;
import com.kenshoo.pl.entity.internal.audit.formatters.CustomAuditFieldValueFormatter2;

@Audited(valueFormatter = CustomAuditFieldValueFormatter1.class)
public class AuditedWithEntityValueFormatterOverrideType extends AbstractType<AuditedWithEntityValueFormatterOverrideType> {

    public static final AuditedWithEntityValueFormatterOverrideType INSTANCE = new AuditedWithEntityValueFormatterOverrideType();

    @Id
    public static final EntityField<AuditedWithEntityValueFormatterOverrideType, Long> ID = INSTANCE.field(MainTable.INSTANCE.id);
    @Audited
    public static final EntityField<AuditedWithEntityValueFormatterOverrideType, String> NAME = INSTANCE.field(MainTable.INSTANCE.name);
    @Audited(valueFormatter = CustomAuditFieldValueFormatter2.class)
    public static final EntityField<AuditedWithEntityValueFormatterOverrideType, String> DESC = INSTANCE.field(MainTable.INSTANCE.desc);
    public static final EntityField<AuditedWithEntityValueFormatterOverrideType, String> DESC2 = INSTANCE.field(MainTable.INSTANCE.desc2);

    private AuditedWithEntityValueFormatterOverrideType() {
        super("AuditedWithEntityValueFormatterOverride");
    }
}
