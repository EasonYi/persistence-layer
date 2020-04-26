package com.kenshoo.pl.entity;

import org.jooq.lambda.Seq;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class OverridingContext implements ChangeContext {

    private final ChangeContext original;
    private final Map<EntityChange, Entity> overrides = new IdentityHashMap<>();

    public OverridingContext(ChangeContext original) {
        this.original = original;
    }

    @Override
    public boolean isEnabled(Feature feature) {
        return original.isEnabled(feature);
    }

    @Override
    public Entity getEntity(EntityChange entityChange) {
        return ofNullable(overrides.get(entityChange))
                .orElseGet(() -> original.getEntity(entityChange));
    }

    @Override
    public void addEntity(EntityChange change, Entity entity) {
        overrides.put(change, new OverridingEntity(entity, original.getEntity(change)));
    }

    @Override
    public void addValidationError(EntityChange<? extends EntityType<?>> entityChange, ValidationError error) {
        original.addValidationError(entityChange, error);
    }

    @Override
    public boolean hasValidationErrors() {
        return original.hasValidationErrors();
    }

    @Override
    public Seq<ValidationError> getValidationErrors(EntityChange cmd) {
        return original.getValidationErrors(cmd);
    }

    @Override
    public boolean containsError(EntityChange entityChange) {
        return original.containsError(entityChange);
    }

    @Override
    public boolean containsErrorNonRecursive(EntityChange entityChange) {
        return original.containsErrorNonRecursive(entityChange);
    }

    @Override
    public PersistentLayerStats getStats() {
        return original.getStats();
    }

    @Override
    public Collection<FieldFetchRequest> getFetchRequests() {
        return original.getFetchRequests();
    }

    @Override
    public Hierarchy getHierarchy() {
        return original.getHierarchy();
    }

    private static class OverridingEntity implements Entity {

        private final Entity overriding;
        private final Entity original;


        private OverridingEntity(Entity overriding, Entity original) {
            this.overriding = overriding;
            this.original = original;
        }

        @Override
        public boolean containsField(EntityField<?, ?> field) {
            return overriding.containsField(field) || original.containsField(field);
        }

        @Override
        public <T> T get(EntityField<?, T> field) {
            return overriding.containsField(field)
                    ? overriding.get(field)
                    : original.get(field);
        }
    }
}
