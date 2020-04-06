package com.kenshoo.pl.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.kenshoo.pl.entity.internal.*;
import com.kenshoo.pl.entity.internal.changelog.EntityChangeLoggableFieldsResolver;
import com.kenshoo.pl.entity.internal.changelog.EntityChangeRecordGenerator;
import com.kenshoo.pl.entity.spi.*;
import com.kenshoo.pl.entity.spi.helpers.EntityChangeCompositeValidator;
import com.kenshoo.pl.entity.spi.helpers.ImmutableFieldValidatorImpl;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kenshoo.pl.entity.Feature.AutoIncrementSupport;
import static com.kenshoo.pl.entity.spi.PersistenceLayerRetryer.JUST_RUN_WITHOUT_CHECKING_DEADLOCKS;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toCollection;

public class ChangeFlowConfig<E extends EntityType<E>> {

    private final static Label NonExcludebale = new Label() {
    };

    private final E entityType;
    private final List<PostFetchCommandEnricher<E>> postFetchCommandEnrichers;
    private final List<OutputGenerator<E>> outputGenerators;
    private final List<ChangesValidator<E>> validators;
    private final Set<EntityField<E, ?>> requiredRelationFields;
    private final Set<EntityField<E, ?>> requiredFields;

    private final List<ChangeFlowConfig<? extends EntityType<?>>> childFlows;
    private final List<ChangesFilter<E>> postFetchFilters;
    private final List<ChangesFilter<E>> postSupplyFilters;
    private final PersistenceLayerRetryer retryer;
    private final EntityChangeRecordGenerator<E> entityChangeRecordGenerator;
    private final FeatureSet features;


    private ChangeFlowConfig(E entityType,
                             List<PostFetchCommandEnricher<E>> postFetchCommandEnrichers,
                             List<ChangesValidator<E>> validators,
                             List<OutputGenerator<E>> outputGenerators,
                             Set<EntityField<E, ?>> requiredRelationFields,
                             Set<EntityField<E, ?>> requiredFields,
                             List<ChangeFlowConfig<? extends EntityType<?>>> childFlows,
                             PersistenceLayerRetryer retryer,
                             EntityChangeRecordGenerator<E> entityChangeRecordGenerator,
                             FeatureSet features) {
        this.entityType = entityType;
        this.postFetchCommandEnrichers = postFetchCommandEnrichers;
        this.outputGenerators = outputGenerators;
        this.validators = validators;
        this.requiredRelationFields = requiredRelationFields;
        this.requiredFields = requiredFields;
        this.childFlows = childFlows;
        this.postFetchFilters = ImmutableList.of(new MissingParentEntitiesFilter<>(entityType.determineForeignKeys(requiredRelationFields)), new MissingEntitiesFilter<>(entityType));
        this.postSupplyFilters = ImmutableList.of(new RequiredFieldsChangesFilter<>(requiredFields));
        this.retryer = retryer;
        this.entityChangeRecordGenerator = entityChangeRecordGenerator;
        this.features = features;
    }

    public E getEntityType() {
        return entityType;
    }

    public PersistenceLayerRetryer retryer() {
        return retryer;
    }

    public Optional<EntityChangeRecordGenerator<E>> optionalChangeRecordGenerator() {
        return Optional.ofNullable(entityChangeRecordGenerator);
    }

    public List<PostFetchCommandEnricher<E>> getPostFetchCommandEnrichers() {
        return postFetchCommandEnrichers;
    }

    public List<ChangesValidator<E>> getValidators() {
        return validators;
    }

    public List<OutputGenerator<E>> getOutputGenerators() {
        return outputGenerators;
    }

    public Stream<CurrentStateConsumer<E>> currentStateConsumers() {
        return Stream.of(postFetchFilters,
                         postSupplyFilters,
                         postFetchCommandEnrichers,
                         validators,
                         outputGenerators,
                         singletonList(entityChangeRecordGenerator))
                     .flatMap(List::stream);
    }

    static <E extends EntityType<E>> Builder<E> builder(E entityType) {
        return new Builder<>(entityType);
    }

    public Set<EntityField<E, ?>> getRequiredRelationFields() {
        return requiredRelationFields;
    }

    public Set<EntityField<E, ?>> getRequiredFields() {
        return requiredFields;
    }

    public List<ChangeFlowConfig<? extends EntityType<?>>> childFlows() {
        return childFlows;
    }

    public List<ChangesFilter<E>> getPostFetchFilters() {
        return postFetchFilters;
    }

    public List<ChangesFilter<E>> getPostSupplyFilters() {
        return postSupplyFilters;
    }

    public Optional<EntityField<E, Object>> getPrimaryIdentityField() {
        return features.isEnabled(AutoIncrementSupport)
                ? getEntityType().getPrimaryIdentityField()
                : Optional.empty();
    }

    public FeatureSet getFeatures() {
        return this.features;
    }


    public static class Builder<E extends EntityType<E>> {
        private final E entityType;
        private final List<Labeled<? extends PostFetchCommandEnricher<E>>> postFetchCommandEnrichers = new ArrayList<>();
        private final List<Labeled<ChangesValidator<E>>> validators = new ArrayList<>();
        private final List<OutputGenerator<E>> outputGenerators = new ArrayList<>();
        private final Set<EntityField<E, ?>> requiredRelationFields = new HashSet<>();
        private final Set<EntityField<E, ?>> requiredFields = new HashSet<>();
        private Optional<PostFetchCommandEnricher<E>> falseUpdatesPurger = Optional.empty();
        private final List<ChangeFlowConfig.Builder<? extends EntityType<?>>> flowConfigBuilders = new ArrayList<>();
        private PersistenceLayerRetryer retryer = JUST_RUN_WITHOUT_CHECKING_DEADLOCKS;
        private final EntityChangeRecordGenerator<E> entityChangeRecordGenerator;
        private FeatureSet features = FeatureSet.EMPTY;

        public Builder(E entityType) {
            this(entityType, EntityChangeLoggableFieldsResolver.INSTANCE);
        }

        @VisibleForTesting
        Builder(final E entityType,
                final EntityChangeLoggableFieldsResolver entityChangeLoggableFieldsResolver) {
            this.entityType = entityType;
            this.entityChangeRecordGenerator = entityChangeLoggableFieldsResolver.resolve(entityType)
                                                                                 .map(EntityChangeRecordGenerator::new)
                                                                                 .orElse(null);
        }

        public Builder<E> with(FeatureSet features) {
            this.features = features;
            this.flowConfigBuilders.forEach(builder -> builder.with(features));
            return this;
        }

        public Builder<E> withLabeledPostFetchCommandEnricher(PostFetchCommandEnricher<E> enricher, Label label) {
            postFetchCommandEnrichers.add(new Labeled<>(enricher, label));
            return this;
        }

        public Builder<E> withPostFetchCommandEnricher(PostFetchCommandEnricher<E> enricher) {
            postFetchCommandEnrichers.add(new Labeled<>(enricher, NonExcludebale));
            return this;
        }

        public Builder<E> withLabeledPostFetchCommandEnrichers(Collection<? extends PostFetchCommandEnricher<E>> enrichers, Label label) {
            enrichers.forEach(e -> postFetchCommandEnrichers.add(new Labeled<>(e, label)));
            return this;
        }

        public Builder<E> withPostFetchCommandEnrichers(Collection<? extends PostFetchCommandEnricher<E>> enrichers) {
            enrichers.forEach(e -> postFetchCommandEnrichers.add(new Labeled<>(e, NonExcludebale)));
            return this;
        }

        /* not public */ void withFalseUpdatesPurger(FalseUpdatesPurger<E> falseUpdatesPurger) {
            this.falseUpdatesPurger = Optional.of(falseUpdatesPurger);
        }

        public Builder<E> withoutFalseUpdatesPurger() {
            this.falseUpdatesPurger = Optional.empty();
            this.flowConfigBuilders.forEach(Builder::withoutFalseUpdatesPurger);
            return this;
        }

        public Builder<E> withValidator(ChangesValidator<E> validator) {
            this.validators.add(new Labeled<>(validator, NonExcludebale));
            return this;
        }

        public Builder<E> withValidators(Collection<ChangesValidator<E>> validators) {
            validators.forEach(validator -> this.validators.add(new Labeled<>(validator, NonExcludebale)));
            return this;
        }

        public Builder<E> withLabeledValidator(ChangesValidator<E> validator, Label label) {
            this.validators.add(new Labeled<>(validator, label));
            return this;
        }

        public Builder<E> withLabeledValidators(Collection<ChangesValidator<E>> validators, Label label) {
            validators.forEach(validator -> this.validators.add(new Labeled<>(validator, label)));
            return this;
        }

        public Builder<E> withoutValidators() {
            this.validators.clear();
            this.flowConfigBuilders.forEach(Builder::withoutValidators);
            return this;
        }

        public Builder<E> withoutLabeledElements(Label label) {
            this.withoutLabeledElements(ImmutableList.of(label));
            return this;
        }

        public Builder<E> withoutLabeledElements(List<Label> labels) {
            if (!labels.isEmpty()) {
                this.validators.removeIf(validator -> labels.contains(validator.label()));
                this.postFetchCommandEnrichers.removeIf(enricher -> labels.contains(enricher.label()));
                this.flowConfigBuilders.forEach(builder -> builder.withoutLabeledElements(labels));
            }
            return this;
        }

        public Builder<E> withOutputGenerator(OutputGenerator<E> outputGenerator) {
            outputGenerators.add(outputGenerator);
            return this;
        }

        public Builder<E> withOutputGenerators(Collection<? extends OutputGenerator<E>> outputGenerators) {
            this.outputGenerators.addAll(outputGenerators);
            return this;
        }

        public Builder<E> withoutOutputGenerators() {
            this.outputGenerators.clear();
            this.flowConfigBuilders.forEach(Builder::withoutOutputGenerators);
            return this;
        }

        public Builder<E> withChildFlowBuilder(ChangeFlowConfig.Builder<? extends EntityType<?>> flowConfigBuilder) {
            this.flowConfigBuilders.add(flowConfigBuilder);
            return this;
        }

        /* not public */ void withRequiredRelationFields(Stream<EntityField<E, ?>> requiredRelationFields) {
            requiredRelationFields.collect(toCollection(() -> this.requiredRelationFields));
        }

        /* not public */ void withRequiredFields(Stream<EntityField<E, ?>> requiredFields) {
            requiredFields.collect(toCollection(() -> this.requiredFields));
        }

        /* not public */ void withImmutableFields(Stream<EntityField<E, ?>> immutableFields) {
            EntityChangeCompositeValidator<E> compositeValidator = new EntityChangeCompositeValidator<>();
            immutableFields.forEach(immutableField -> compositeValidator.register(entityType, new ImmutableFieldValidatorImpl<>(immutableField, Errors.FIELD_IS_IMMUTABLE)));
            this.withValidator(compositeValidator);
        }

        public Builder<E> withRetryer(PersistenceLayerRetryer retryer) {
            this.retryer = retryer;
            return this;
        }

        public ChangeFlowConfig<E> build() {
            ImmutableList.Builder<PostFetchCommandEnricher<E>> enrichers = ImmutableList.builder();
            postFetchCommandEnrichers.forEach(excludableElement -> enrichers.add(excludableElement.element()));
            ImmutableList.Builder<ChangesValidator<E>> validatorList = ImmutableList.builder();
            validators.forEach(validator -> validatorList.add(validator.element()));
            falseUpdatesPurger.ifPresent(enrichers::add);
            return new ChangeFlowConfig<>(entityType,
                                          enrichers.build(),
                                          validatorList.build(),
                                          ImmutableList.copyOf(outputGenerators),
                                          ImmutableSet.copyOf(requiredRelationFields),
                                          ImmutableSet.copyOf(requiredFields),
                                          flowConfigBuilders.stream().map(Builder::build).collect(Collectors.toList()),
                                          retryer,
                                          entityChangeRecordGenerator,
                                          features
            );
        }

        static private class Labeled<Element> {

            private final Element element;
            private final Label label;

            Labeled(Element element, Label label) {
                this.element = element;
                this.label = label;
            }

            Element element() {
                return element;
            }

            Label label() {
                return label;
            }
        }
    }

}
