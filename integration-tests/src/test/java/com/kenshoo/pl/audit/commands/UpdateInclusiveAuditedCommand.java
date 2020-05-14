package com.kenshoo.pl.audit.commands;

import com.kenshoo.pl.entity.EntityCommandExt;
import com.kenshoo.pl.entity.Identifier;
import com.kenshoo.pl.entity.SingleUniqueKeyValue;
import com.kenshoo.pl.entity.UpdateEntityCommand;
import com.kenshoo.pl.entity.internal.audit.entitytypes.InclusiveAuditedType;

public class UpdateInclusiveAuditedCommand extends UpdateEntityCommand<InclusiveAuditedType, Identifier<InclusiveAuditedType>> implements EntityCommandExt<InclusiveAuditedType, UpdateInclusiveAuditedCommand> {

    public UpdateInclusiveAuditedCommand(final long id) {
        super(InclusiveAuditedType.INSTANCE, new SingleUniqueKeyValue<>(InclusiveAuditedType.ID, id));
    }
}